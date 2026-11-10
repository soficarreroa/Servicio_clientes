package com.empresa.pedidos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Un producto del pedido (RF-P04). Tiene la misma forma que el item que espera inventarios. */
public record ItemPedidoRequest(
        @NotNull(message = "El productoId es obligatorio")
        @Positive(message = "El productoId debe ser un número positivo") Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor o igual a 1") Integer cantidad) {
}
