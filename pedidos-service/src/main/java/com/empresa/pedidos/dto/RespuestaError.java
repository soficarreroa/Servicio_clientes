package com.empresa.pedidos.dto;

import java.time.LocalDateTime;

/** Formato único de error del servicio (RNF-07). Ejemplo en el contrato: PP-03. */
public record RespuestaError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message) {
}
