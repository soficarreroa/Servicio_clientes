package com.empresa.facturacion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record FacturaRequest(
        @NotNull(message = "El id del pedido es obligatorio")
        @Positive(message = "El id del pedido debe ser un número positivo")
        Long pedidoId,

        @NotNull(message = "El id del cliente es obligatorio")
        @Positive(message = "El id del cliente debe ser un número positivo")
        Long clienteId,

        @NotNull(message = "El monto total es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto total debe ser mayor que cero")
        BigDecimal montoTotal
) {
}

