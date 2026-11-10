package com.empresa.inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record VerificacionRequest(
        @NotEmpty(message = "Debe enviar al menos un producto en 'items'")
        List<@Valid ItemSolicitud> items) {
}
