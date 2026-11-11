package com.empresa.facturacion.dto;

import com.empresa.facturacion.model.EstadoFactura;
import com.empresa.facturacion.model.Factura;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FacturaResponse(
        Long id,
        Long pedidoId,
        Long clienteId,
        BigDecimal montoTotal,
        LocalDateTime fechaEmision,
        EstadoFactura estado
) {
    public static FacturaResponse desdeEntidad(Factura factura) {
        return new FacturaResponse(
                factura.getId(),
                factura.getPedidoId(),
                factura.getClienteId(),
                factura.getMontoTotal(),
                factura.getFechaEmision(),
                factura.getEstado()
        );
    }
}

