package com.empresa.inventario.exception;

public class CantidadInvalidaException extends RuntimeException {
    public CantidadInvalidaException(String mensaje) {
        super(mensaje);
    }
}
