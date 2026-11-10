package com.empresa.pedidos.esb;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuración del ESB (prefijo "esb" en application.properties).
 * Pedidos solo conoce esta dirección: nunca la de clientes ni la de inventarios (RF-P14).
 */
@Validated
@ConfigurationProperties(prefix = "esb")
public record EsbProperties(
        @NotBlank String baseUrl,
        @NotNull @DefaultValue("2s") Duration connectTimeout,
        @NotNull @DefaultValue("5s") Duration readTimeout) {
}
