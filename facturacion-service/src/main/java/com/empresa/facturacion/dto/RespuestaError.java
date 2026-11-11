package com.empresa.facturacion.dto;

import java.time.LocalDateTime;

public record RespuestaError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message
) {
}

