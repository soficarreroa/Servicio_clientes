package com.empresa.pedidos.exception;

/** RF-P11: el pedido consultado no existe. */
public class PedidoNoEncontradoException extends RuntimeException {
    public PedidoNoEncontradoException(Long id) {
        super("El pedido con id " + id + " no existe");
    }
}
