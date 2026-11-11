package com.empresa.notificaciones.model;

import java.time.LocalDateTime;

public record NotificacionAudit(
        String codigoSeguimiento,
        Long clienteId,
        String email,
        TipoNotificacion tipo,
        String mensaje,
        LocalDateTime fechaEnvio,
        String canal,
        String estado
) {
}

