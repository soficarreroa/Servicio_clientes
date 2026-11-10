package com.empresa.pedidos.esb;

import com.empresa.pedidos.dto.ItemPedidoRequest;
import com.empresa.pedidos.exception.EsbNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Prueba la comunicación con el ESB: qué mensajes se envían y cómo se interpretan las respuestas
 * de clientes e inventarios (con los mismos JSON del contrato del documento).
 */
class EsbClientTest {

    private static final String ESB = "http://esb.test:8280";

    private static final String VERIFICAR_SOLICITUD =
            "{\"items\":[{\"productoId\":1,\"cantidad\":2},{\"productoId\":3,\"cantidad\":20}]}";

    /** Respuesta real de inventarios cuando falta stock (PI-10), con todos sus campos. */
    private static final String VERIFICAR_SIN_STOCK = """
            {"todoDisponible":false,"items":[
            {"productoId":1,"nombre":"Teclado mecánico","cantidadSolicitada":2,"stockActual":25,"disponible":true,"mensaje":"Disponible"},
            {"productoId":3,"nombre":"Monitor 24 pulgadas","cantidadSolicitada":20,"stockActual":8,"disponible":false,"mensaje":"Stock insuficiente: hay 8 unidad(es)"}]}
            """;

    /** Respuesta de inventarios cuando todo está disponible (PI-09). */
    private static final String VERIFICAR_TODO_DISPONIBLE = """
            {"todoDisponible":true,"items":[
            {"productoId":1,"nombre":"Teclado mecánico","cantidadSolicitada":2,"stockActual":25,"disponible":true,"mensaje":"Disponible"},
            {"productoId":3,"nombre":"Monitor 24 pulgadas","cantidadSolicitada":20,"stockActual":80,"disponible":true,"mensaje":"Disponible"}]}
            """;

    private static final List<ItemPedidoRequest> ITEMS =
            List.of(new ItemPedidoRequest(1L, 2), new ItemPedidoRequest(3L, 20));

    private MockRestServiceServer servidorSimulado;
    private EsbClient esb;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(ESB);
        servidorSimulado = MockRestServiceServer.bindTo(builder).build();
        esb = new EsbClient(builder.build());
    }

    // ---------------- clientes ----------------

    @Test
    void clienteExiste_cuandoElEsbResponde200_devuelveTrue() {
        servidorSimulado.expect(requestTo(ESB + "/api/clientes/7"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":7,\"nombre\":\"Laura\"}", MediaType.APPLICATION_JSON));

        assertTrue(esb.clienteExiste(7L));
        servidorSimulado.verify();
    }

    @Test
    void clienteExiste_cuandoElEsbResponde404_devuelveFalse() {
        servidorSimulado.expect(requestTo(ESB + "/api/clientes/9999"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"mensaje\":\"No existe un cliente con id 9999\"}"));

        assertFalse(esb.clienteExiste(9999L));
        servidorSimulado.verify();
    }

    @Test
    void clienteExiste_cuandoElEsbResponde500_lanzaEsbNoDisponible() {
        servidorSimulado.expect(requestTo(ESB + "/api/clientes/7")).andRespond(withServerError());

        assertThrows(EsbNoDisponibleException.class, () -> esb.clienteExiste(7L));
    }

    @Test
    void clienteExiste_cuandoElEsbResponde503_lanzaEsbNoDisponible() {
        servidorSimulado.expect(requestTo(ESB + "/api/clientes/7"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(EsbNoDisponibleException.class, () -> esb.clienteExiste(7L));
    }

    @Test
    void clienteExiste_cuandoHayFalloDeRed_lanzaEsbNoDisponible() {
        servidorSimulado.expect(requestTo(ESB + "/api/clientes/7"))
                .andRespond(withException(new IOException("Connection refused")));

        assertThrows(EsbNoDisponibleException.class, () -> esb.clienteExiste(7L));
    }

    // ---------------- inventarios ----------------

    @Test
    void verificarInventario_enviaLosItemsTalCualAlEndpointDelContrato() {
        servidorSimulado.expect(requestTo(ESB + "/api/inventario/verificar"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(VERIFICAR_SOLICITUD))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andRespond(withSuccess(VERIFICAR_TODO_DISPONIBLE, MediaType.APPLICATION_JSON));

        ResultadoVerificacion r = esb.verificarInventario(ITEMS);

        assertTrue(r.todoDisponible());
        assertTrue(r.noDisponibles().isEmpty());
        servidorSimulado.verify();
    }

    @Test
    void verificarInventario_sinStock_devuelveSoloLosProductosNoDisponibles() {
        servidorSimulado.expect(requestTo(ESB + "/api/inventario/verificar"))
                .andRespond(withSuccess(VERIFICAR_SIN_STOCK, MediaType.APPLICATION_JSON));

        ResultadoVerificacion r = esb.verificarInventario(ITEMS);

        assertFalse(r.todoDisponible());
        assertEquals(1, r.noDisponibles().size());
        assertEquals(3L, r.noDisponibles().get(0).productoId());
        assertEquals("Stock insuficiente: hay 8 unidad(es)", r.noDisponibles().get(0).mensaje());
    }

    @Test
    void verificarInventario_ignoraCamposQueInventariosAgregueDespues() {
        String conCamposNuevos = "{\"todoDisponible\":true,\"version\":2,\"items\":["
                + "{\"productoId\":1,\"disponible\":true,\"mensaje\":\"Disponible\",\"bodega\":\"B-3\"}]}";
        servidorSimulado.expect(requestTo(ESB + "/api/inventario/verificar"))
                .andRespond(withSuccess(conCamposNuevos, MediaType.APPLICATION_JSON));

        assertTrue(esb.verificarInventario(ITEMS).todoDisponible());
    }

    @Test
    void verificarInventario_sinTodoDisponible_lanzaEsbNoDisponible() {
        // Sin ese campo no se puede decidir: nunca se acepta un pedido sin verificar el inventario.
        servidorSimulado.expect(requestTo(ESB + "/api/inventario/verificar"))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));

        assertThrows(EsbNoDisponibleException.class, () -> esb.verificarInventario(ITEMS));
    }

    @Test
    void verificarInventario_respuestaQueNoEsJson_lanzaEsbNoDisponible() {
        servidorSimulado.expect(requestTo(ESB + "/api/inventario/verificar"))
                .andRespond(withSuccess("esto no es json", MediaType.APPLICATION_JSON));

        assertThrows(EsbNoDisponibleException.class, () -> esb.verificarInventario(ITEMS));
    }

    @Test
    void verificarInventario_cuandoElEsbResponde500_lanzaEsbNoDisponible() {
        servidorSimulado.expect(requestTo(ESB + "/api/inventario/verificar")).andRespond(withServerError());

        assertThrows(EsbNoDisponibleException.class, () -> esb.verificarInventario(ITEMS));
    }

    @Test
    void verificarInventario_cuandoHayFalloDeRed_lanzaEsbNoDisponible() {
        servidorSimulado.expect(requestTo(ESB + "/api/inventario/verificar"))
                .andRespond(withException(new IOException("Read timed out")));

        assertThrows(EsbNoDisponibleException.class, () -> esb.verificarInventario(ITEMS));
    }

    // ---------------- ESB real apagado / lento (sin simulación) ----------------

    @Test
    void esbApagado_conexionRechazada_lanzaEsbNoDisponible() throws IOException {
        int puertoCerrado;
        try (ServerSocket socket = new ServerSocket(0)) {
            puertoCerrado = socket.getLocalPort();
        } // al cerrarse, nadie escucha en ese puerto: es como tener el ESB apagado (PP-10)

        EsbClient esbApagado = clienteReal("http://127.0.0.1:" + puertoCerrado, Duration.ofSeconds(1), Duration.ofSeconds(1));

        assertThrows(EsbNoDisponibleException.class, () -> esbApagado.clienteExiste(1L));
        assertThrows(EsbNoDisponibleException.class, () -> esbApagado.verificarInventario(ITEMS));
    }

    @Test
    void esbQueNoResponde_alSuperarElTiempoDeLectura_lanzaEsbNoDisponible() throws IOException {
        // El socket acepta la conexión pero nunca responde: se debe cortar por el tiempo de lectura.
        try (ServerSocket silencioso = new ServerSocket(0)) {
            EsbClient esbLento = clienteReal("http://127.0.0.1:" + silencioso.getLocalPort(),
                    Duration.ofSeconds(1), Duration.ofMillis(500));

            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                assertThrows(EsbNoDisponibleException.class, () -> esbLento.clienteExiste(1L));
            });
        }
    }

    private static EsbClient clienteReal(String baseUrl, Duration conexion, Duration lectura) {
        EsbProperties propiedades = new EsbProperties(baseUrl, conexion, lectura);
        return new EsbClient(new EsbClientConfig().esbRestClient(RestClient.builder(), propiedades));
    }
}
