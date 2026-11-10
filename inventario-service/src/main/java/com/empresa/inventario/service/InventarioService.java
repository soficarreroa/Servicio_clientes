package com.empresa.inventario.service;

import com.empresa.inventario.dto.*;
import com.empresa.inventario.exception.CantidadInvalidaException;
import com.empresa.inventario.exception.ProductoNoEncontradoException;
import com.empresa.inventario.model.Producto;
import com.empresa.inventario.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class InventarioService {

    private final ProductoRepository repositorio;

    public InventarioService(ProductoRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Lista todos los productos con su stock actual. */
    public List<ProductoResponse> listarProductos() {
        return repositorio.findAll().stream().map(ProductoResponse::desde).toList();
    }

    /** Consulta un producto por id. */
    public ProductoResponse consultarProducto(Long id) {
        return ProductoResponse.desde(buscar(id));
    }

    /** RF-02: hay disponibilidad de 'cantidad' unidades de este producto? */
    public DisponibilidadResponse consultarDisponibilidad(Long id, int cantidad) {
        if (cantidad < 1) {
            throw new CantidadInvalidaException("La cantidad debe ser mayor o igual a 1");
        }
        return evaluar(buscar(id), cantidad);
    }

    /**
     * RF-04: verifica las existencias de todos los productos de un pedido.
     * Si un producto se repite en la lista, sus cantidades se suman para no
     * aprobar mas unidades de las que realmente hay.
     * Un producto inexistente no lanza error: se reporta como no disponible
     * para que el servicio de pedidos reciba el detalle completo.
     */
    public VerificacionResponse verificarExistencias(VerificacionRequest solicitud) {
        Map<Long, Integer> totales = new LinkedHashMap<>();
        for (ItemSolicitud item : solicitud.items()) {
            totales.merge(item.productoId(), item.cantidad(), Integer::sum);
        }

        List<DisponibilidadResponse> resultado = new ArrayList<>();
        boolean todo = true;
        for (Map.Entry<Long, Integer> e : totales.entrySet()) {
            DisponibilidadResponse r = repositorio.findById(e.getKey())
                    .map(p -> evaluar(p, e.getValue()))
                    .orElseGet(() -> new DisponibilidadResponse(e.getKey(), null, e.getValue(),
                            0, false, "El producto no existe"));
            todo = todo && Boolean.TRUE.equals(r.disponible());
            resultado.add(r);
        }
        return new VerificacionResponse(todo, resultado);
    }

    // ----------------- metodos privados -----------------

    private Producto buscar(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new ProductoNoEncontradoException(id));
    }

    private DisponibilidadResponse evaluar(Producto p, int cantidad) {
        int stock = p.getStock();
        boolean disponible;
        String mensaje;

        if (!Boolean.TRUE.equals(p.getActivo())) {
            disponible = false;
            mensaje = "El producto no está activo";
        } else if (stock >= cantidad) {
            disponible = true;
            mensaje = "Disponible";
        } else {
            disponible = false;
            mensaje = "Stock insuficiente: hay " + stock + " unidad(es)";
        }
        return new DisponibilidadResponse(p.getId(), p.getNombre(), cantidad, stock, disponible, mensaje);
    }
}
