package com.empresa.pedidos.esb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Respuesta de inventarios (vía ESB). Solo se declaran los campos que pedidos necesita;
 * los demás (nombre, cantidadSolicitada, stockActual...) se ignoran a propósito, para que
 * inventarios pueda agregar campos sin romper a pedidos.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VerificacionInventarioResponse(Boolean todoDisponible, List<Item> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(Long productoId, Boolean disponible, String mensaje) {
    }
}
