package com.empresa.facturacion.service;

import com.empresa.facturacion.dto.FacturaRequest;
import com.empresa.facturacion.dto.FacturaResponse;
import com.empresa.facturacion.exception.FacturaDuplicadaException;
import com.empresa.facturacion.exception.FacturaNoEncontradaException;
import com.empresa.facturacion.exception.MontoInvalidoException;
import com.empresa.facturacion.model.EstadoFactura;
import com.empresa.facturacion.model.Factura;
import com.empresa.facturacion.repository.FacturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacturacionServiceTest {

    @Mock
    private FacturaRepository repository;

    @InjectMocks
    private FacturacionService service;

    private FacturaRequest requestValido;
    private Factura facturaGuardada;

    @BeforeEach
    void setUp() {
        requestValido = new FacturaRequest(10L, 5L, new BigDecimal("250000.00"));
        facturaGuardada = new Factura(1L, 10L, 5L, new BigDecimal("250000.00"), LocalDateTime.now(), EstadoFactura.GENERADA);
    }

    @Test
    @DisplayName("PF-01: Crear factura con datos válidos -> Generada exitosamente")
    void crearFactura_Valida_Exito() {
        when(repository.existsByPedidoId(10L)).thenReturn(false);
        when(repository.save(any(Factura.class))).thenReturn(facturaGuardada);

        FacturaResponse response = service.crear(requestValido);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(10L, response.pedidoId());
        assertEquals(5L, response.clienteId());
        assertEquals(new BigDecimal("250000.00"), response.montoTotal());
        assertEquals(EstadoFactura.GENERADA, response.estado());
        verify(repository).save(any(Factura.class));
    }

    @Test
    @DisplayName("PF-02: Intentar crear factura con pedido ya facturado -> FacturaDuplicadaException")
    void crearFactura_PedidoDuplicado_LanzaExcepcion() {
        when(repository.existsByPedidoId(10L)).thenReturn(true);

        assertThrows(FacturaDuplicadaException.class, () -> service.crear(requestValido));
        verify(repository, never()).save(any(Factura.class));
    }

    @Test
    @DisplayName("PF-03: Crear factura con monto inválido o cero -> MontoInvalidoException")
    void crearFactura_MontoInvalido_LanzaExcepcion() {
        FacturaRequest requestMontoCero = new FacturaRequest(11L, 5L, BigDecimal.ZERO);

        assertThrows(MontoInvalidoException.class, () -> service.crear(requestMontoCero));
        verify(repository, never()).save(any(Factura.class));
    }

    @Test
    @DisplayName("PF-04: Consultar factura por ID existente -> Retorna FacturaResponse")
    void consultarPorId_Existente_Exito() {
        when(repository.findById(1L)).thenReturn(Optional.of(facturaGuardada));

        FacturaResponse response = service.consultarPorId(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(10L, response.pedidoId());
    }

    @Test
    @DisplayName("PF-05: Consultar factura por ID inexistente -> FacturaNoEncontradaException")
    void consultarPorId_Inexistente_LanzaExcepcion() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(FacturaNoEncontradaException.class, () -> service.consultarPorId(99L));
    }

    @Test
    @DisplayName("PF-06: Consultar factura por pedidoId existente -> Retorna FacturaResponse")
    void consultarPorPedidoId_Existente_Exito() {
        when(repository.findByPedidoId(10L)).thenReturn(Optional.of(facturaGuardada));

        FacturaResponse response = service.consultarPorPedidoId(10L);

        assertNotNull(response);
        assertEquals(10L, response.pedidoId());
    }
}

