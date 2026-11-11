package com.empresa.notificaciones;

import com.empresa.notificaciones.dto.NotificacionRequest;
import com.empresa.notificaciones.dto.NotificacionResponse;
import com.empresa.notificaciones.model.NotificacionAudit;
import com.empresa.notificaciones.model.TipoNotificacion;
import com.empresa.notificaciones.service.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NotificacionServiceTest {

    private NotificacionService service;

    @BeforeEach
    void setUp() {
        service = new NotificacionService();
    }

    @Test
    @DisplayName("PN-01: Procesar notificación de Pedido Confirmado exitosamente")
    void notificacionPedidoConfirmado_Exito() {
        NotificacionRequest request = new NotificacionRequest(
                1L,
                "cliente@correo.com",
                TipoNotificacion.PEDIDO_CONFIRMADO,
                "Su pedido #10 ha sido confirmado. Factura #5 generada por $150,000.00"
        );

        NotificacionResponse response = service.procesarNotificacion(request);

        assertNotNull(response);
        assertNotNull(response.codigoSeguimiento());
        assertTrue(response.codigoSeguimiento().startsWith("NOTIF-"));
        assertEquals("ENVIADA", response.estado());
        assertNotNull(response.timestamp());

        // Verificar que quedó en la bitácora
        List<NotificacionAudit> historial = service.listarHistorial();
        assertEquals(1, historial.size());
        assertEquals("cliente@correo.com", historial.get(0).email());
        assertEquals(TipoNotificacion.PEDIDO_CONFIRMADO, historial.get(0).tipo());
    }

    @Test
    @DisplayName("PN-02: Procesar notificación de Pedido Rechazado con motivo")
    void notificacionPedidoRechazado_Exito() {
        NotificacionRequest request = new NotificacionRequest(
                2L,
                "cliente2@correo.com",
                TipoNotificacion.PEDIDO_RECHAZADO,
                "Su pedido #11 fue rechazado: Stock insuficiente para el producto 3"
        );

        NotificacionResponse response = service.procesarNotificacion(request);

        assertNotNull(response);
        assertEquals("ENVIADA", response.estado());

        Optional<NotificacionAudit> audit = service.consultarPorCodigo(response.codigoSeguimiento());
        assertTrue(audit.isPresent());
        assertEquals(TipoNotificacion.PEDIDO_RECHAZADO, audit.get().tipo());
        assertTrue(audit.get().mensaje().contains("Stock insuficiente"));
    }

    @Test
    @DisplayName("PN-03: Consultar notificación por código de seguimiento existente")
    void consultarPorCodigo_Existente() {
        NotificacionRequest request = new NotificacionRequest(
                3L,
                "cliente3@correo.com",
                TipoNotificacion.PEDIDO_CONFIRMADO,
                "Pedido #12 confirmado."
        );
        NotificacionResponse response = service.procesarNotificacion(request);

        Optional<NotificacionAudit> encontrado = service.consultarPorCodigo(response.codigoSeguimiento());
        assertTrue(encontrado.isPresent());
        assertEquals(response.codigoSeguimiento(), encontrado.get().codigoSeguimiento());
    }

    @Test
    @DisplayName("PN-04: Bitácora almacena múltiples notificaciones")
    void listarHistorial_Multiples() {
        service.procesarNotificacion(new NotificacionRequest(1L, "a@correo.com", TipoNotificacion.PEDIDO_CONFIRMADO, "Msg 1"));
        service.procesarNotificacion(new NotificacionRequest(2L, "b@correo.com", TipoNotificacion.PEDIDO_RECHAZADO, "Msg 2"));

        List<NotificacionAudit> lista = service.listarHistorial();
        assertEquals(2, lista.size());
    }
}

