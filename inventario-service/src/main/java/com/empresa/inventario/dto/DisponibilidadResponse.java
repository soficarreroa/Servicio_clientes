package com.empresa.inventario.dto;

public record DisponibilidadResponse(
        Long productoId,
        String nombre,
        Integer cantidadSolicitada,
        Integer stockActual,
        Boolean disponible,
        String mensaje) {
}
