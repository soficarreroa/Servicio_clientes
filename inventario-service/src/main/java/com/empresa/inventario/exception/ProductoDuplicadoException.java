package com.empresa.inventario.exception;

public class ProductoDuplicadoException extends RuntimeException {
    public ProductoDuplicadoException(String codigo) {
        super("Ya existe un producto registrado con el código " + codigo);
    }
}

