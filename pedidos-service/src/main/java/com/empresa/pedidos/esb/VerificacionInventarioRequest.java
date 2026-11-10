package com.empresa.pedidos.esb;

import com.empresa.pedidos.dto.ItemPedidoRequest;

import java.util.List;

/** Mensaje que pedidos envía al ESB: POST /api/inventario/verificar. */
public record VerificacionInventarioRequest(List<ItemPedidoRequest> items) {
}
