package com.empresa.notificaciones.dto;

import java.time.LocalDateTime;

public record NotificacionResponse(
        String codigoSeguimiento,
        String estado,
        String mensaje,
        LocalDateTime timestamp
) {
}

