package com.empresa.pedidos.dto;

import com.empresa.pedidos.model.EstadoPedido;
import com.empresa.pedidos.model.Pedido;

import java.time.LocalDateTime;
import java.util.List;

/** Respuesta de pedidos: mismos campos y orden que el contrato del documento. */
public record PedidoResponse(
        Long id,
        Long clienteId,
        EstadoPedido estado,
        String motivoRechazo,
        LocalDateTime fechaCreacion,
        List<ItemPedidoResponse> items) {

    public static PedidoResponse desde(Pedido pedido) {
        List<ItemPedidoResponse> items = pedido.getItems().stream()
                .map(ItemPedidoResponse::desde)
                .toList();
        return new PedidoResponse(pedido.getId(), pedido.getClienteId(), pedido.getEstado(),
                pedido.getMotivoRechazo(), pedido.getFechaCreacion(), items);
    }
}
