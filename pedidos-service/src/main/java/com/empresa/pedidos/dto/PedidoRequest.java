package com.empresa.pedidos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/** Cuerpo de POST /api/pedidos (RF-P01, RF-P02, RF-P03). */
public record PedidoRequest(
        @NotNull(message = "El clienteId es obligatorio")
        @Positive(message = "El clienteId debe ser un número positivo") Long clienteId,

        @NotEmpty(message = "Debe enviar al menos un producto en 'items'")
        List<@NotNull(message = "Cada elemento de 'items' debe ser un producto válido") @Valid ItemPedidoRequest> items) {
}
