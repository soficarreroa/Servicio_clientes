package com.empresa.inventario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemSolicitud(
        @NotNull(message = "El productoId es obligatorio") Long productoId,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor o igual a 1") Integer cantidad) {
}
