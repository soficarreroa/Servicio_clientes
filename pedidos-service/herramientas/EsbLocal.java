import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
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
import java.util.concurrent.Executors;

/**
 * ESB MÍNIMO PARA PRUEBAS LOCALES (no reemplaza a WSO2 ni a Apache Camel).
 *
 * Hace solo lo básico de un ESB: recibe las peticiones REST/JSON en el puerto 8280, decide a qué
 * servicio mandarlas según la ruta, devuelve la respuesta tal cual y registra cada paso en consola.
 * Sirve para probar la comunicación de pedidos con clientes e inventarios mientras el equipo
 * configura el ESB definitivo. No tiene dependencias: se ejecuta con solo Java 17 o superior.
 *
 * Uso:  java herramientas/EsbLocal.java
 *
 * Rutas (las mismas del documento):
 *   /api/clientes        -> 8081      /api/facturas        -> 8084
 *   /api/inventario      -> 8082      /api/notificaciones  -> 8085
 *   /api/pedidos         -> 8083
 *
 * Si el servicio de destino no responde, el ESB contesta 503 (igual que lo haría el ESB real).
 * Pendiente (lo hará el ESB definitivo): enviar el pedido a facturación y notificaciones según su estado.
 */
public class EsbLocal {

    private static final Map<String, Integer> RUTAS = new LinkedHashMap<>();

    static {
        RUTAS.put("/api/clientes", puerto("CLIENTES_PORT", 8081));
        RUTAS.put("/api/inventario", puerto("INVENTARIOS_PORT", 8082));
        RUTAS.put("/api/pedidos", puerto("PEDIDOS_PORT", 8083));
        RUTAS.put("/api/facturas", puerto("FACTURACION_PORT", 8084));
        RUTAS.put("/api/notificaciones", puerto("NOTIFICACIONES_PORT", 8085));
    }

    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public static void main(String[] args) throws IOException {
        int puertoEsb = puerto("ESB_PORT", 8280);
        HttpServer servidor = HttpServer.create(new InetSocketAddress(puertoEsb), 0);
        servidor.setExecutor(Executors.newFixedThreadPool(16));
        servidor.createContext("/", EsbLocal::enrutar);
        servidor.start();

        System.out.println("ESB local escuchando en http://localhost:" + puertoEsb);
        RUTAS.forEach((ruta, destino) -> System.out.println("  " + ruta + "/**  ->  localhost:" + destino));
        System.out.println("Ctrl+C para detenerlo.");
    }

    private static void enrutar(HttpExchange intercambio) throws IOException {
        long inicio = System.currentTimeMillis();
        String metodo = intercambio.getRequestMethod();
        URI uri = intercambio.getRequestURI();
        String ruta = uri.getPath();

        try (intercambio) {
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
        return valor == null || valor.isBlank() ? pordefecto : Integer.parseInt(valor.trim());
    }
}
