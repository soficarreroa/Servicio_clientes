package com.empresa.inventario.service;

import com.empresa.inventario.dto.*;
import com.empresa.inventario.exception.CantidadInvalidaException;
import com.empresa.inventario.exception.ProductoNoEncontradoException;
import com.empresa.inventario.model.Producto;
import com.empresa.inventario.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock
    private ProductoRepository repositorio;

    private InventarioService servicio;

    @BeforeEach
    void preparar() {
        servicio = new InventarioService(repositorio);
    }

    private Producto producto(Long id, int stock, boolean activo) {
        Producto p = new Producto();
        p.setId(id);
        p.setCodigo("PRD-" + id);
        p.setNombre("Producto " + id);
        p.setPrecio(new BigDecimal("1000.00"));
        p.setStock(stock);
        p.setActivo(activo);
        return p;
    }

    @Test
    void disponibilidad_hayStockSuficiente() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(producto(1L, 10, true)));
        DisponibilidadResponse r = servicio.consultarDisponibilidad(1L, 5);
        assertTrue(r.disponible());
        assertEquals(10, r.stockActual());
    }

    @Test
    void disponibilidad_stockInsuficiente() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(producto(1L, 2, true)));
        DisponibilidadResponse r = servicio.consultarDisponibilidad(1L, 5);
        assertFalse(r.disponible());
        assertTrue(r.mensaje().contains("insuficiente"));
    }

    @Test
    void disponibilidad_productoInactivo() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(producto(1L, 50, false)));
        assertFalse(servicio.consultarDisponibilidad(1L, 1).disponible());
    }

    @Test
    void disponibilidad_productoNoExiste() {
        when(repositorio.findById(9999L)).thenReturn(Optional.empty());
        assertThrows(ProductoNoEncontradoException.class,
                () -> servicio.consultarDisponibilidad(9999L, 1));
    }

    @Test
    void disponibilidad_cantidadInvalida() {
        assertThrows(CantidadInvalidaException.class,
                () -> servicio.consultarDisponibilidad(1L, 0));
    }

    @Test
    void verificar_todoDisponible() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(producto(1L, 10, true)));
        when(repositorio.findById(2L)).thenReturn(Optional.of(producto(2L, 3, true)));
        VerificacionResponse r = servicio.verificarExistencias(new VerificacionRequest(
                List.of(new ItemSolicitud(1L, 4), new ItemSolicitud(2L, 3))));
        assertTrue(r.todoDisponible());
        assertEquals(2, r.items().size());
    }

    @Test
    void verificar_unoSinStock_noEstaTodoDisponible() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(producto(1L, 10, true)));
        when(repositorio.findById(2L)).thenReturn(Optional.of(producto(2L, 1, true)));
        VerificacionResponse r = servicio.verificarExistencias(new VerificacionRequest(
                List.of(new ItemSolicitud(1L, 4), new ItemSolicitud(2L, 3))));
        assertFalse(r.todoDisponible());
    }

    @Test
    void verificar_sumaCantidadesRepetidas() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(producto(1L, 5, true)));
        // 3 + 3 = 6 > 5 -> no debe aprobarse
        VerificacionResponse r = servicio.verificarExistencias(new VerificacionRequest(
                List.of(new ItemSolicitud(1L, 3), new ItemSolicitud(1L, 3))));
        assertFalse(r.todoDisponible());
        assertEquals(1, r.items().size());
        assertEquals(6, r.items().get(0).cantidadSolicitada());
    }

    @Test
    void verificar_productoInexistente_seReportaNoDisponible() {
        when(repositorio.findById(777L)).thenReturn(Optional.empty());
        VerificacionResponse r = servicio.verificarExistencias(new VerificacionRequest(
                List.of(new ItemSolicitud(777L, 1))));
        assertFalse(r.todoDisponible());
        assertEquals("El producto no existe", r.items().get(0).mensaje());
    }
}
