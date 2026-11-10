package com.empresa.pedidos.exception;

/** RF-P05: el cliente del pedido no existe (el ESB respondió 404 desde clientes). */
public class ClienteNoExisteException extends RuntimeException {
    public ClienteNoExisteException(Long clienteId) {
        super("El cliente con id " + clienteId + " no existe");
    }
}
