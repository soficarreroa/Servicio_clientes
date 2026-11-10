package com.empresa.pedidos.esb;

/** Producto que inventarios reportó como no disponible, con su mensaje (por ejemplo "Stock insuficiente: hay 8 unidad(es)"). */
public record ProductoNoDisponible(Long productoId, String mensaje) {
}
