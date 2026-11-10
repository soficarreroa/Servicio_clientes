package com.empresa.pedidos.esb;

import java.util.List;

/**
 * Lo único que pedidos usa de la respuesta de inventarios: {@code todoDisponible}
 * y los mensajes de los productos no disponibles. El resto de campos se ignora.
 */
public record ResultadoVerificacion(boolean todoDisponible, List<ProductoNoDisponible> noDisponibles) {

    public ResultadoVerificacion {
        noDisponibles = noDisponibles == null ? List.of() : List.copyOf(noDisponibles);
    }

    public static ResultadoVerificacion disponible() {
        return new ResultadoVerificacion(true, List.of());
    }
}
