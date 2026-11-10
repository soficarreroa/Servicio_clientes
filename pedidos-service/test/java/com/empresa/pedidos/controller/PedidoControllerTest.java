package com.empresa.pedidos.controller;

import com.empresa.pedidos.dto.ItemPedidoRequest;
import com.empresa.pedidos.esb.EsbClient;
import com.empresa.pedidos.esb.ProductoNoDisponible;
import com.empresa.pedidos.esb.ResultadoVerificacion;
import com.empresa.pedidos.exception.EsbNoDisponibleException;
import com.empresa.pedidos.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas PP-01 a PP-13 del documento, de punta a punta sobre HTTP: controlador, validaciones,
 * servicio, JPA y base de datos H2. El ESB se simula con un mock (clientes e inventarios no se
 * levantan). No son {@code @Transactional} a propósito: así se comprueba también que los pedidos
 * se leen bien en producción (sin sesión abierta), y cada prueba limpia la tabla al empezar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PedidoControllerTest {

    private static final String PEDIDO_VALIDO =
            "{\"clienteId\":1,\"items\":[{\"productoId\":1,\"cantidad\":2},{\"productoId\":2,\"cantidad\":3}]}";

    private static final String MENSAJE_503 = "El servicio no está disponible en este momento. Intente nuevamente más tarde";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PedidoRepository repositorio;

    @MockBean
    private EsbClient esb;

    @BeforeEach
    void limpiarBaseDeDatos() {
        repositorio.deleteAll();
    }

    private ResultActions crearPedido(String json) throws Exception {
        return mockMvc.perform(post("/api/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private void elClienteExiste(long id) {
        when(esb.clienteExiste(id)).thenReturn(true);
    }

    private void inventarioDisponible() {
        when(esb.verificarInventario(anyList())).thenReturn(ResultadoVerificacion.disponible());
    }

    private void inventarioNoDisponible(long productoId, String mensaje) {
        when(esb.verificarInventario(anyList())).thenReturn(
                new ResultadoVerificacion(false, List.of(new ProductoNoDisponible(productoId, mensaje))));
    }

    // ---------------- Crear pedido ----------------

    @Test
    void PP01_pedidoConInventarioSuficiente_201Confirmado() throws Exception {
        elClienteExiste(1L);
        inventarioDisponible();

        crearPedido(PEDIDO_VALIDO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.clienteId").value(1))
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
                .andExpect(jsonPath("$", hasKey("motivoRechazo")))      // el campo está, con valor null
                .andExpect(jsonPath("$.motivoRechazo", nullValue()))
                .andExpect(jsonPath("$.fechaCreacion").isNotEmpty())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].productoId").value(1))
                .andExpect(jsonPath("$.items[0].cantidad").value(2))
                .andExpect(jsonPath("$.items[1].productoId").value(2))
                .andExpect(jsonPath("$.items[1].cantidad").value(3));

        assertEquals(1, repositorio.count());
    }

    @Test
    void PP02_stockInsuficiente_201RechazadoConMotivo() throws Exception {
        elClienteExiste(1L);
        inventarioNoDisponible(3L, "Stock insuficiente: hay 8 unidad(es)");

        crearPedido("{\"clienteId\":1,\"items\":[{\"productoId\":3,\"cantidad\":20}]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.motivoRechazo").value("Producto 3: Stock insuficiente: hay 8 unidad(es)"))
                .andExpect(jsonPath("$.items", hasSize(1)));

        assertEquals(1, repositorio.count()); // el pedido rechazado también queda registrado
    }

    @Test
    void PP03_clienteQueNoExiste_404YNoConsultaInventario() throws Exception {
        when(esb.clienteExiste(9999L)).thenReturn(false);

        crearPedido("{\"clienteId\":9999,\"items\":[{\"productoId\":1,\"cantidad\":1}]}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("El cliente con id 9999 no existe"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        verify(esb, never()).verificarInventario(anyList());
        assertEquals(0, repositorio.count());
    }

    @Test
    void PP04_listaDeProductosVacia_400() throws Exception {
        crearPedido("{\"clienteId\":1,\"items\":[]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Debe enviar al menos un producto en 'items'"));

        verifyNoInteractions(esb);
        assertEquals(0, repositorio.count());
    }

    @Test
    void PP04b_sinListaDeProductos_400() throws Exception {
        crearPedido("{\"clienteId\":1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Debe enviar al menos un producto en 'items'"));

        verifyNoInteractions(esb);
    }

    @Test
    void PP05_cantidadInvalida_400() throws Exception {
        crearPedido("{\"clienteId\":1,\"items\":[{\"productoId\":1,\"cantidad\":0}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La cantidad debe ser mayor o igual a 1"));

        verifyNoInteractions(esb);
        assertEquals(0, repositorio.count());
    }

    @Test
    void PP05b_productoSinIdOSinCantidad_400() throws Exception {
        crearPedido("{\"clienteId\":1,\"items\":[{\"cantidad\":1},{\"productoId\":2}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("El productoId es obligatorio")))
                .andExpect(jsonPath("$.message", containsString("La cantidad es obligatoria")));

        verifyNoInteractions(esb);
    }

    @Test
    void PP06_pedidoSinCliente_400() throws Exception {
        crearPedido("{\"items\":[{\"productoId\":1,\"cantidad\":1}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El clienteId es obligatorio"));

        verifyNoInteractions(esb);
        assertEquals(0, repositorio.count());
    }

    @Test
    void PP07_productoInactivo_201Rechazado() throws Exception {
        elClienteExiste(1L);
        inventarioNoDisponible(5L, "El producto no está activo");

        crearPedido("{\"clienteId\":1,\"items\":[{\"productoId\":5,\"cantidad\":1}]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.motivoRechazo").value("Producto 5: El producto no está activo"));
    }

    @Test
    void PP08_productoQueNoExiste_201Rechazado() throws Exception {
        elClienteExiste(1L);
        inventarioNoDisponible(9999L, "El producto no existe");

        crearPedido("{\"clienteId\":1,\"items\":[{\"productoId\":9999,\"cantidad\":1}]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.motivoRechazo").value("Producto 9999: El producto no existe"));
    }

    @Test
    void PP09_productoRepetido_seEnviaTalCualAInventarios() throws Exception {
        elClienteExiste(1L);
        inventarioDisponible();

        crearPedido("{\"clienteId\":1,\"items\":[{\"productoId\":1,\"cantidad\":1},{\"productoId\":1,\"cantidad\":1}]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(2)));

        // Pedidos no suma: manda las dos líneas y es inventarios quien suma las cantidades.
        verify(esb).verificarInventario(List.of(new ItemPedidoRequest(1L, 1), new ItemPedidoRequest(1L, 1)));
    }

    @Test
    void PP10_esbCaidoAlValidarElCliente_503SinDetallesYNoGuarda() throws Exception {
        when(esb.clienteExiste(1L)).thenThrow(
                new EsbNoDisponibleException("Connection refused: localhost:8280", new RuntimeException("detalle interno")));

        crearPedido(PEDIDO_VALIDO)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value(MENSAJE_503))
                .andExpect(jsonPath("$.message", not(containsString("8280"))))
                .andExpect(jsonPath("$.message", not(containsString("refused"))));

        assertEquals(0, repositorio.count());
    }

    @Test
    void PP10b_esbCaidoAlVerificarInventario_503YNoGuarda() throws Exception {
        elClienteExiste(1L);
        when(esb.verificarInventario(anyList())).thenThrow(new EsbNoDisponibleException("Read timed out"));

        crearPedido(PEDIDO_VALIDO)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(MENSAJE_503));

        assertEquals(0, repositorio.count()); // no se acepta un pedido sin haber verificado el inventario
    }

    // ---------------- Consultar y listar ----------------

    @Test
    void PP11_consultarPedidoQueExiste_200ConSusProductos() throws Exception {
        elClienteExiste(1L);
        inventarioDisponible();
        crearPedido(PEDIDO_VALIDO).andExpect(status().isCreated());
        Long id = repositorio.findAll().get(0).getId();

        mockMvc.perform(get("/api/pedidos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.clienteId").value(1))
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    void PP12_consultarPedidoQueNoExiste_404() throws Exception {
        mockMvc.perform(get("/api/pedidos/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("El pedido con id 9999 no existe"));
    }

    @Test
    void PP13_listarPedidosDeUnCliente_200SoloConLosDeEseCliente() throws Exception {
        when(esb.clienteExiste(anyLong())).thenReturn(true);
        inventarioDisponible();
        String pedidoCliente1 = "{\"clienteId\":1,\"items\":[{\"productoId\":1,\"cantidad\":1}]}";
        String pedidoCliente2 = "{\"clienteId\":2,\"items\":[{\"productoId\":2,\"cantidad\":1}]}";
        crearPedido(pedidoCliente1).andExpect(status().isCreated());
        crearPedido(pedidoCliente2).andExpect(status().isCreated());
        crearPedido(pedidoCliente1).andExpect(status().isCreated());

        mockMvc.perform(get("/api/pedidos").param("clienteId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].clienteId").value(1))
                .andExpect(jsonPath("$[1].clienteId").value(1))
                .andExpect(jsonPath("$[0].items", hasSize(1)));
    }

    @Test
    void PP13b_listarTodosLosPedidos_200() throws Exception {
        when(esb.clienteExiste(anyLong())).thenReturn(true);
        inventarioDisponible();
        crearPedido("{\"clienteId\":1,\"items\":[{\"productoId\":1,\"cantidad\":1}]}").andExpect(status().isCreated());
        crearPedido("{\"clienteId\":2,\"items\":[{\"productoId\":2,\"cantidad\":1}]}").andExpect(status().isCreated());

        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].clienteId").value(1))
                .andExpect(jsonPath("$[1].clienteId").value(2));
    }

    @Test
    void PP13c_listarSinPedidos_200ConListaVacia() throws Exception {
        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ---------------- Errores generales (RNF-05, RNF-07) ----------------

    @Test
    void jsonMalFormado_400ConMensajeClaro() throws Exception {
        crearPedido("{clienteId: ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("no es un JSON válido")));

        verifyNoInteractions(esb);
    }

    @Test
    void clienteIdConTipoIncorrecto_400() throws Exception {
        crearPedido("{\"clienteId\":\"abc\",\"items\":[{\"productoId\":1,\"cantidad\":1}]}")
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/pedidos").param("clienteId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El parámetro 'clienteId' tiene un valor no válido"));
    }

    @Test
    void rutaInexistente_404() throws Exception {
        mockMvc.perform(get("/api/xyz"))
                .andExpect(status().isNotFound());
    }

    @Test
    void metodoNoPermitido_405ConMensajeClaro() throws Exception {
        mockMvc.perform(delete("/api/pedidos/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("El método HTTP no está permitido para este recurso"));
    }
}
