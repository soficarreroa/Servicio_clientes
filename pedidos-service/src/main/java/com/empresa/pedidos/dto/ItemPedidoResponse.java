package com.empresa.pedidos.dto;

import com.empresa.pedidos.model.PedidoItem;

public record ItemPedidoResponse(Long productoId, Integer cantidad) {

    public static ItemPedidoResponse desde(PedidoItem item) {
        return new ItemPedidoResponse(item.getProductoId(), item.getCantidad());
    }
}
