package com.empresa.inventario.dto;

import java.util.List;

public record VerificacionResponse(
        Boolean todoDisponible,
        List<DisponibilidadResponse> items) {
}
