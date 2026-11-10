package com.empresa.pedidos.service;

import com.empresa.pedidos.dto.ItemPedidoRequest;
import com.empresa.pedidos.dto.PedidoRequest;
import com.empresa.pedidos.dto.PedidoResponse;
import com.empresa.pedidos.esb.EsbClient;
import com.empresa.pedidos.esb.ProductoNoDisponible;
import com.empresa.pedidos.esb.ResultadoVerificacion;
import com.empresa.pedidos.exception.ClienteNoExisteException;
import com.empresa.pedidos.exception.EsbNoDisponibleException;
import com.empresa.pedidos.exception.PedidoNoEncontradoException;
import com.empresa.pedidos.model.EstadoPedido;
import com.empresa.pedidos.model.Pedido;
import com.empresa.pedidos.model.PedidoItem;
import com.empresa.pedidos.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Pruebas de la lógica de negocio, con el ESB y el repositorio simulados (no necesitan MySQL ni red). */
@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository repositorio;

    @Mock
    private EsbClient esb;

    private PedidoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new PedidoService(repositorio, esb);
    }

    private static PedidoRequest pedido(Long clienteId, ItemPedidoRequest... items) {
        return new PedidoRequest(clienteId, List.of(items));
    }

    private void elRepositorioDevuelveLoQueGuarda() {
        when(repositorio.save(any(Pedido.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    // ---------------- crear ----------------

    @Test
    void crear_conInventarioSuficiente_guardaConfirmado() {
        when(esb.clienteExiste(7L)).thenReturn(true);
        when(esb.verificarInventario(anyList())).thenReturn(ResultadoVerificacion.disponible());
        elRepositorioDevuelveLoQueGuarda();

        PedidoResponse r = servicio.crear(pedido(7L, new ItemPedidoRequest(1L, 2), new ItemPedidoRequest(2L, 3)));

        assertEquals(EstadoPedido.CONFIRMADO, r.estado());
        assertNull(r.motivoRechazo());
        assertEquals(7L, r.clienteId());
        assertEquals(2, r.items().size());
    }

    @Test
    void crear_sinStock_guardaRechazadoConElMotivoDeInventarios() {
        when(esb.clienteExiste(7L)).thenReturn(true);
        when(esb.verificarInventario(anyList())).thenReturn(new ResultadoVerificacion(false,
                List.of(new ProductoNoDisponible(3L, "Stock insuficiente: hay 8 unidad(es)"))));
        elRepositorioDevuelveLoQueGuarda();

        PedidoResponse r = servicio.crear(pedido(7L, new ItemPedidoRequest(3L, 20)));

        assertEquals(EstadoPedido.RECHAZADO, r.estado());
        assertEquals("Producto 3: Stock insuficiente: hay 8 unidad(es)", r.motivoRechazo());
        assertEquals(1, r.items().size()); // el pedido rechazado también queda registrado
    }

    @Test
    void crear_variosProductosNoDisponibles_unaTodosLosMotivos() {
        when(esb.clienteExiste(7L)).thenReturn(true);
        when(esb.verificarInventario(anyList())).thenReturn(new ResultadoVerificacion(false, List.of(
                new ProductoNoDisponible(3L, "Stock insuficiente: hay 8 unidad(es)"),
                new ProductoNoDisponible(5L, "El producto no está activo"))));
        elRepositorioDevuelveLoQueGuarda();

        PedidoResponse r = servicio.crear(pedido(7L, new ItemPedidoRequest(3L, 20), new ItemPedidoRequest(5L, 1)));

        assertEquals("Producto 3: Stock insuficiente: hay 8 unidad(es); Producto 5: El producto no está activo",
                r.motivoRechazo());
    }

    @Test
    void crear_inventarioRechazaSinDetalle_usaMotivoPorDefecto() {
        when(esb.clienteExiste(7L)).thenReturn(true);
        when(esb.verificarInventario(anyList())).thenReturn(new ResultadoVerificacion(false, List.of()));
        elRepositorioDevuelveLoQueGuarda();

        PedidoResponse r = servicio.crear(pedido(7L, new ItemPedidoRequest(1L, 1)));

        assertEquals(EstadoPedido.RECHAZADO, r.estado());
        assertEquals(PedidoService.MOTIVO_POR_DEFECTO, r.motivoRechazo());
    }

    @Test
    void crear_motivoMuyLargo_seRecortaParaQueCaigaEnLaColumna() {
        List<ProductoNoDisponible> muchos = IntStream.rangeClosed(1, 200)
                .mapToObj(i -> new ProductoNoDisponible((long) i, "Stock insuficiente: hay 0 unidad(es)"))
                .toList();
        when(esb.clienteExiste(7L)).thenReturn(true);
        when(esb.verificarInventario(anyList())).thenReturn(new ResultadoVerificacion(false, muchos));
        elRepositorioDevuelveLoQueGuarda();

        PedidoResponse r = servicio.crear(pedido(7L, new ItemPedidoRequest(1L, 1)));

        assertEquals(Pedido.MOTIVO_RECHAZO_MAX, r.motivoRechazo().length());
        assertTrue(r.motivoRechazo().endsWith("..."));
    }

    @Test
    void crear_productoRepetido_seEnviaYseGuardaTalCual() {
        // PP-09: pedidos no suma cantidades; eso lo hace inventarios al verificar.
        PedidoRequest solicitud = pedido(7L, new ItemPedidoRequest(1L, 1), new ItemPedidoRequest(1L, 1));
        when(esb.clienteExiste(7L)).thenReturn(true);
        when(esb.verificarInventario(solicitud.items())).thenReturn(ResultadoVerificacion.disponible());
        elRepositorioDevuelveLoQueGuarda();

        PedidoResponse r = servicio.crear(solicitud);

        verify(esb).verificarInventario(solicitud.items());
        assertEquals(2, r.items().size());
    }

    @Test
    void crear_clienteNoExiste_noConsultaInventarioNiGuarda() {
        when(esb.clienteExiste(9999L)).thenReturn(false);

        ClienteNoExisteException ex = assertThrows(ClienteNoExisteException.class,
                () -> servicio.crear(pedido(9999L, new ItemPedidoRequest(1L, 1))));

        assertEquals("El cliente con id 9999 no existe", ex.getMessage());
        verify(esb, never()).verificarInventario(anyList());
        verifyNoInteractions(repositorio);
    }

    @Test
    void crear_esbCaidoAlValidarElCliente_noGuardaNada() {
        when(esb.clienteExiste(7L)).thenThrow(new EsbNoDisponibleException("sin conexión"));

        assertThrows(EsbNoDisponibleException.class,
                () -> servicio.crear(pedido(7L, new ItemPedidoRequest(1L, 1))));

        verify(esb, never()).verificarInventario(anyList());
        verifyNoInteractions(repositorio);
    }

    @Test
    void crear_esbCaidoAlVerificarInventario_noGuardaNada() {
        when(esb.clienteExiste(7L)).thenReturn(true);
        when(esb.verificarInventario(anyList())).thenThrow(new EsbNoDisponibleException("timeout"));

        assertThrows(EsbNoDisponibleException.class,
                () -> servicio.crear(pedido(7L, new ItemPedidoRequest(1L, 1))));

        verifyNoInteractions(repositorio);
    }

    // ---------------- consultar y listar ----------------

    @Test
    void consultar_pedidoExistente_devuelveSusDatos() {
        Pedido guardado = Pedido.confirmado(1L, List.of(new PedidoItem(1L, 2)));
        when(repositorio.findById(15L)).thenReturn(Optional.of(guardado));

        PedidoResponse r = servicio.consultar(15L);

        assertEquals(EstadoPedido.CONFIRMADO, r.estado());
        assertEquals(1, r.items().size());
    }

    @Test
    void consultar_pedidoInexistente_lanzaNoEncontrado() {
        when(repositorio.findById(9999L)).thenReturn(Optional.empty());

        PedidoNoEncontradoException ex = assertThrows(PedidoNoEncontradoException.class,
                () -> servicio.consultar(9999L));

        assertEquals("El pedido con id 9999 no existe", ex.getMessage());
    }

    @Test
    void listar_sinFiltro_devuelveTodos() {
        when(repositorio.findAllByOrderByIdAsc()).thenReturn(List.of(
                Pedido.confirmado(1L, List.of(new PedidoItem(1L, 1))),
                Pedido.confirmado(2L, List.of(new PedidoItem(2L, 1)))));

        assertEquals(2, servicio.listar(null).size());
        verify(repositorio, never()).findByClienteIdOrderByIdAsc(any());
    }

    @Test
    void listar_conClienteId_filtraPorCliente() {
        when(repositorio.findByClienteIdOrderByIdAsc(1L)).thenReturn(List.of(
                Pedido.confirmado(1L, List.of(new PedidoItem(1L, 1)))));

        List<PedidoResponse> r = servicio.listar(1L);

        assertEquals(1, r.size());
        assertEquals(1L, r.get(0).clienteId());
        verify(repositorio, never()).findAllByOrderByIdAsc();
    }
}
