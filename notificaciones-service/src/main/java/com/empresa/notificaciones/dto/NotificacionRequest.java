package com.empresa.notificaciones.dto;

import com.empresa.notificaciones.model.TipoNotificacion;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record NotificacionRequest(
        @NotNull(message = "El clienteId es obligatorio")
        @Positive(message = "El clienteId debe ser un número positivo")
        Long clienteId,

        @NotBlank(message = "El email no puede estar vacío")
        @Email(message = "El email debe tener un formato válido")
        String email,

        @NotNull(message = "El tipo de notificación es obligatorio (PEDIDO_CONFIRMADO o PEDIDO_RECHAZADO)")
        TipoNotificacion tipo,

        @NotBlank(message = "El mensaje no puede estar vacío")
        String mensaje
) {
}

