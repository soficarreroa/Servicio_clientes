package com.empresa.inventario.dto;

import com.empresa.inventario.model.Producto;
import java.math.BigDecimal;

public record ProductoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer stock,
        Boolean activo) {

    public static ProductoResponse desde(Producto p) {
        return new ProductoResponse(p.getId(), p.getCodigo(), p.getNombre(),
                p.getDescripcion(), p.getPrecio(), p.getStock(), p.getActivo());
    }
}
