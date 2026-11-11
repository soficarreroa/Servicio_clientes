import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ESB LOCAL PARA PRUEBAS Y ORQUESTACIÓN ENTERPRISE (puerto 8280).
 *
 * Cumple con la regla RF-07: Ningún microservicio se llama directamente entre sí;
 * todo pasa a través del ESB en el puerto 8280.
 *
 * Rutas administradas:
 *   /api/clientes        -> 8081 (Gestión de Clientes)
 *   /api/inventario      -> 8082 (Inventarios)
 *   /api/pedidos         -> 8083 (Procesamiento de Pedidos)
 *   /api/facturas        -> 8084 (Facturación)
 *   /api/notificaciones  -> 8085 (Notificaciones)
 *
 * Orquestación del Flujo de Pedidos:
 *   - Si el pedido queda CONFIRMADO: Dispara Facturación (8084) y Notificaciones (8085).
 *   - Si el pedido queda RECHAZADO: Dispara Notificaciones (8085) con el motivo de rechazo.
 *
 * Incluye soporte nativo de CORS (OPTIONS 204 y headers) para el Frontend Dashboard.
 */
public class EsbLocal {

    private static final Map<String, Integer> RUTAS = new LinkedHashMap<>();
    private static final Map<String, String> ENV_CONFIG = new LinkedHashMap<>();

    static {
        cargarEnvSiExiste();
        RUTAS.put("/api/clientes", puerto("CLIENTES_PORT", 8081));
        RUTAS.put("/api/inventario", puerto("INVENTARIOS_PORT", 8082));
        RUTAS.put("/api/pedidos", puerto("PEDIDOS_PORT", 8083));
        RUTAS.put("/api/facturas", puerto("FACTURACION_PORT", 8084));
        RUTAS.put("/api/notificaciones", puerto("NOTIFICACIONES_PORT", 8085));
    }

    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public static void main(String[] args) throws IOException {
        int puertoEsb = puerto("ESB_PORT", 8280);
        HttpServer servidor = HttpServer.create(new InetSocketAddress(puertoEsb), 0);
        servidor.setExecutor(Executors.newFixedThreadPool(16));
        servidor.createContext("/", EsbLocal::enrutar);
        servidor.start();

        System.out.println("=================================================================");
        System.out.println("  ESB LOCAL CON ORQUESTADOR ENTERPRISE ACTIVO (: " + puertoEsb + ")");
        System.out.println("=================================================================");
        RUTAS.forEach((ruta, destino) -> System.out.println("  " + ruta + "/**  ->  localhost:" + destino));
        System.out.println("  Orquestación activa para pedidos CONFIRMADOS y RECHAZADOS.");
        System.out.println("  Soporte CORS habilitado para navegador web.");
        System.out.println("  Presione Ctrl+C para detenerlo.\n");
    }

    private static void enrutar(HttpExchange intercambio) throws IOException {
        long inicio = System.currentTimeMillis();
        String metodo = intercambio.getRequestMethod();
        URI uri = intercambio.getRequestURI();
        String ruta = uri.getPath();

        try (intercambio) {
            // Cabeceras CORS universales
            intercambio.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            intercambio.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            intercambio.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept, X-Requested-With");

            // Responder preflight CORS
            if ("OPTIONS".equalsIgnoreCase(metodo)) {
                intercambio.sendResponseHeaders(204, -1);
                return;
            }

            Integer destino = buscarDestino(ruta);
            if (destino == null) {
                responder(intercambio, 404, error(404, "Not Found", "El ESB no tiene una ruta para " + ruta));
                registrar(metodo, uri, "sin ruta", 404, inicio);
                return;
            }

            byte[] cuerpo = intercambio.getRequestBody().readAllBytes();
            HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + destino + uri))
                    .timeout(Duration.ofSeconds(10))
                    .method(metodo, cuerpo.length == 0
                            ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofByteArray(cuerpo));
            String tipo = intercambio.getRequestHeaders().getFirst("Content-Type");
            if (tipo != null) {
                peticion.header("Content-Type", tipo);
            }

            try {
                HttpResponse<byte[]> respuesta = CLIENTE.send(peticion.build(), HttpResponse.BodyHandlers.ofByteArray());
                String tipoRespuesta = respuesta.headers().firstValue("Content-Type").orElse("application/json");
                intercambio.getResponseHeaders().set("Content-Type", tipoRespuesta);
                enviar(intercambio, respuesta.statusCode(), respuesta.body());
                registrar(metodo, uri, "localhost:" + destino, respuesta.statusCode(), inicio);

                // Orquestación posterior a la creación de pedido (RF-07 / Flujo Enterprise)
                if ("POST".equalsIgnoreCase(metodo) && "/api/pedidos".equals(ruta) && respuesta.statusCode() == 201) {
                    String jsonRespuesta = new String(respuesta.body(), StandardCharsets.UTF_8);
                    CompletableFuture.runAsync(() -> orquestarFlujoPedido(jsonRespuesta));
                }

            } catch (ConnectException | HttpTimeoutException e) {
                responder(intercambio, 503, error(503, "Service Unavailable",
                        "El servicio de destino (puerto " + destino + ") no está disponible"));
                registrar(metodo, uri, "localhost:" + destino + " (NO RESPONDE)", 503, inicio);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                responder(intercambio, 503, error(503, "Service Unavailable", "El ESB fue interrumpido"));
            } catch (IOException e) {
                responder(intercambio, 503, error(503, "Service Unavailable",
                        "El servicio de destino (puerto " + destino + ") no está disponible"));
                registrar(metodo, uri, "localhost:" + destino + " (ERROR DE RED)", 503, inicio);
            }
        }
    }

    /**
     * Orquesta el flujo enterprise según el estado del pedido:
     * - CONFIRMADO: Calcula el monto total, genera la factura en 8084 y envía notificación en 8085.
     * - RECHAZADO: Envía notificación en 8085 con el motivo de rechazo.
     */
    private static void orquestarFlujoPedido(String json) {
        try {
            Long pedidoId = extraerLong(json, "\"id\":\\s*(\\d+)");
            Long clienteId = extraerLong(json, "\"clienteId\":\\s*(\\d+)");
            String estado = extraerTexto(json, "\"estado\":\\s*\"([^\"]+)\"");
            String motivoRechazo = extraerTexto(json, "\"motivoRechazo\":\\s*\"([^\"]+)\"");

            if (pedidoId == null || estado == null) {
                return;
            }

            System.out.println("\n[ESB ORQUESTADOR] Procesando evento de Pedido #" + pedidoId + " (Estado: " + estado + ")");

            // Obtener email del cliente desde Clientes (:8081)
            String emailCliente = obtenerEmailCliente(clienteId);

            if ("CONFIRMADO".equalsIgnoreCase(estado)) {
                // 1. Descontar existencias en Inventarios (:8082)
                dispararDescuentoStock(json);

                // 2. Calcular monto total consultando precios en Inventario (:8082)
                BigDecimal montoTotal = calcularMontoTotalPedido(json);

                // 3. Disparar generación de Factura en Facturación (:8084)
                Long facturaId = dispararFacturacion(pedidoId, clienteId, montoTotal);

                // 4. Disparar Notificación de confirmación en Notificaciones (:8085)
                String mensaje = String.format("Estimado cliente, su pedido #%d ha sido CONFIRMADO. Factura #%s generada por un total de $%s.",
                        pedidoId, (facturaId != null ? facturaId.toString() : "PENDIENTE"), montoTotal.toPlainString());
                dispararNotificacion(clienteId, emailCliente, "PEDIDO_CONFIRMADO", mensaje);

            } else if ("RECHAZADO".equalsIgnoreCase(estado)) {
                // Disparar Notificación de rechazo en Notificaciones (:8085)
                String razon = (motivoRechazo != null && !motivoRechazo.isBlank()) ? motivoRechazo : "Disponibilidad no superada";
                String mensaje = String.format("Estimado cliente, su pedido #%d ha sido RECHAZADO. Motivo: %s", pedidoId, razon);
                dispararNotificacion(clienteId, emailCliente, "PEDIDO_RECHAZADO", mensaje);
            }

        } catch (Exception e) {
            System.err.println("[ESB ORQUESTADOR] Error durante la orquestación: " + e.getMessage());
        }
    }

    private static String obtenerEmailCliente(Long clienteId) {
        if (clienteId == null) return "cliente@empresa.com";
        try {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create("http://localhost:8081/api/clientes/" + clienteId))
                    .timeout(Duration.ofSeconds(3))
                    .GET().build();
            HttpResponse<String> resp = CLIENTE.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                String email = extraerTexto(resp.body(), "\"email\":\\s*\"([^\"]+)\"");
                if (email != null && !email.isBlank()) return email;
            }
        } catch (Exception ignored) {
        }
        return "cliente" + clienteId + "@correo.com";
    }

    private static void dispararDescuentoStock(String jsonPedido) {
        try {
            Matcher m = Pattern.compile("\\{\\s*\"productoId\":\\s*(\\d+)\\s*,\\s*\"cantidad\":\\s*(\\d+)\\s*\\}").matcher(jsonPedido);
            StringBuilder itemsJson = new StringBuilder("[");
            boolean first = true;
            while (m.find()) {
                if (!first) itemsJson.append(",");
                itemsJson.append("{\"productoId\":").append(m.group(1)).append(",\"cantidad\":").append(m.group(2)).append("}");
                first = false;
            }
            itemsJson.append("]");

            if (!first) {
                String cuerpo = "{\"items\":" + itemsJson.toString() + "}";
                int puertoInventario = RUTAS.getOrDefault("/api/inventario", 8082);
                HttpRequest peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + puertoInventario + "/api/inventario/descontar"))
                        .timeout(Duration.ofSeconds(4))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> resp = CLIENTE.send(peticion, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    System.out.println("  [-> INVENTARIOS :" + puertoInventario + "] Stock descontado exitosamente en base de datos.");
                } else {
                    System.out.println("  [-> INVENTARIOS :" + puertoInventario + "] Respuesta al descontar: " + resp.statusCode());
                }
            }
        } catch (Exception e) {
            System.err.println("  [-> INVENTARIOS] Error al descontar stock: " + e.getMessage());
        }
    }

    private static BigDecimal calcularMontoTotalPedido(String jsonPedido) {
        BigDecimal total = BigDecimal.ZERO;
        Matcher m = Pattern.compile("\\{\\s*\"productoId\":\\s*(\\d+)\\s*,\\s*\"cantidad\":\\s*(\\d+)\\s*\\}").matcher(jsonPedido);
        boolean hayItems = false;
        while (m.find()) {
            hayItems = true;
            long productoId = Long.parseLong(m.group(1));
            int cantidad = Integer.parseInt(m.group(2));
            BigDecimal precio = obtenerPrecioProducto(productoId);
            total = total.add(precio.multiply(BigDecimal.valueOf(cantidad)));
        }
        if (!hayItems || total.compareTo(BigDecimal.ZERO) <= 0) {
            total = new BigDecimal("150000.00");
        }
        return total;
    }

    private static BigDecimal obtenerPrecioProducto(long productoId) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create("http://localhost:8082/api/inventario/productos/" + productoId))
                    .timeout(Duration.ofSeconds(3))
                    .GET().build();
            HttpResponse<String> resp = CLIENTE.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                String precioStr = extraerTexto(resp.body(), "\"precio\":\\s*([0-9.]+)");
                if (precioStr != null) {
                    return new BigDecimal(precioStr);
                }
            }
        } catch (Exception ignored) {
        }
        return new BigDecimal("50000.00");
    }

    private static Long dispararFacturacion(Long pedidoId, Long clienteId, BigDecimal montoTotal) {
        try {
            String cuerpo = String.format("{\"pedidoId\":%d,\"clienteId\":%d,\"montoTotal\":%s}",
                    pedidoId, clienteId, montoTotal.toPlainString());
            HttpRequest peticion = HttpRequest.newBuilder(URI.create("http://localhost:8084/api/facturas"))
                    .timeout(Duration.ofSeconds(4))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = CLIENTE.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 201) {
                Long facturaId = extraerLong(resp.body(), "\"id\":\\s*(\\d+)");
                System.out.println("  [-> FACTURACIÓN :8084] Factura #" + facturaId + " creada exitosamente.");
                return facturaId;
            } else {
                System.out.println("  [-> FACTURACIÓN :8084] Respuesta HTTP " + resp.statusCode() + ": " + resp.body());
            }
        } catch (Exception e) {
            System.err.println("  [-> FACTURACIÓN :8084] Error al conectar con facturación: " + e.getMessage());
        }
        return null;
    }

    private static void dispararNotificacion(Long clienteId, String email, String tipo, String mensaje) {
        try {
            String mensajeEscapado = mensaje.replace("\"", "\\\"");
            String cuerpo = String.format("{\"clienteId\":%d,\"email\":\"%s\",\"tipo\":\"%s\",\"mensaje\":\"%s\"}",
                    clienteId, email, tipo, mensajeEscapado);
            HttpRequest peticion = HttpRequest.newBuilder(URI.create("http://localhost:8085/api/notificaciones/enviar"))
                    .timeout(Duration.ofSeconds(4))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = CLIENTE.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                String tracking = extraerTexto(resp.body(), "\"codigoSeguimiento\":\\s*\"([^\"]+)\"");
                System.out.println("  [-> NOTIFICACIONES :8085] Notificación enviada. Tracking: " + tracking);
            } else {
                System.out.println("  [-> NOTIFICACIONES :8085] Respuesta HTTP " + resp.statusCode() + ": " + resp.body());
            }
        } catch (Exception e) {
            System.err.println("  [-> NOTIFICACIONES :8085] Error al conectar con notificaciones: " + e.getMessage());
        }
    }

    private static Long extraerLong(String texto, String patronRegex) {
        Matcher m = Pattern.compile(patronRegex).matcher(texto);
        return m.find() ? Long.parseLong(m.group(1)) : null;
    }

    private static String extraerTexto(String texto, String patronRegex) {
        Matcher m = Pattern.compile(patronRegex).matcher(texto);
        return m.find() ? m.group(1) : null;
    }

    private static Integer buscarDestino(String ruta) {
        for (Map.Entry<String, Integer> e : RUTAS.entrySet()) {
            String base = e.getKey();
            if (ruta.equals(base) || ruta.startsWith(base + "/")) {
                return e.getValue();
            }
        }
        return null;
    }

    private static void responder(HttpExchange intercambio, int estado, String json) throws IOException {
        intercambio.getResponseHeaders().set("Content-Type", "application/json");
        enviar(intercambio, estado, json.getBytes(StandardCharsets.UTF_8));
    }

    private static void enviar(HttpExchange intercambio, int estado, byte[] cuerpo) throws IOException {
        if (cuerpo.length == 0) {
            intercambio.sendResponseHeaders(estado, -1);
            return;
        }
        intercambio.sendResponseHeaders(estado, cuerpo.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(cuerpo);
        }
    }

    private static String error(int estado, String razon, String mensaje) {
        return "{\"timestamp\":\"" + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) + "\",\"status\":" + estado
                + ",\"error\":\"" + razon + "\",\"message\":\"" + mensaje + "\"}";
    }

    private static void registrar(String metodo, URI uri, String destino, int estado, long inicio) {
        System.out.printf("[ESB] %-6s %-45s -> %-34s %d (%d ms)%n",
                metodo, uri, destino, estado, System.currentTimeMillis() - inicio);
    }

    private static int puerto(String variable, int pordefecto) {
        String valor = System.getenv(variable);
        if (valor == null || valor.isBlank()) {
            valor = ENV_CONFIG.get(variable);
        }
        return valor == null || valor.isBlank() ? pordefecto : Integer.parseInt(valor.trim());
    }

    private static void cargarEnvSiExiste() {
        String[] posibles = { ".env", "../.env", "../../.env" };
        for (String ruta : posibles) {
            java.io.File archivo = new java.io.File(ruta);
            if (archivo.exists()) {
                try {
                    java.util.List<String> lineas = java.nio.file.Files.readAllLines(archivo.toPath(), StandardCharsets.UTF_8);
                    for (String linea : lineas) {
                        linea = linea.trim();
                        if (!linea.startsWith("#") && linea.contains("=")) {
                            int idx = linea.indexOf('=');
                            ENV_CONFIG.put(linea.substring(0, idx).trim(), linea.substring(idx + 1).trim());
                        }
                    }
                    break;
                } catch (Exception ignored) {
                }
            }
        }
    }
}
