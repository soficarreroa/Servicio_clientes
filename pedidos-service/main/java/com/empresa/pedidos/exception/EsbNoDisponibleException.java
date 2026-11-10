package com.empresa.pedidos.exception;

/**
 * RF-P13: el ESB (o un servicio detrás de él) no respondió o respondió algo inesperado.
 * El mensaje es solo para el log: al consumidor nunca se le muestran detalles internos (RNF-05).
 */
public class EsbNoDisponibleException extends RuntimeException {
    public EsbNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public EsbNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
