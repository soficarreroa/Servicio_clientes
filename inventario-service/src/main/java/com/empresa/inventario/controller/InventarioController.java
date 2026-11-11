package com.empresa.inventario.controller;

import com.empresa.inventario.dto.*;
import com.empresa.inventario.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
@CrossOrigin(origins = "*")
public class InventarioController {

    private final InventarioService servicio;

    public InventarioController(InventarioService servicio) {
        this.servicio = servicio;
    }

    /** GET /api/inventario/productos */
    @GetMapping("/productos")
    public ResponseEntity<List<ProductoResponse>> listar() {
        return ResponseEntity.ok(servicio.listarProductos());
    }

    /** GET /api/inventario/productos/{id} */
    @GetMapping("/productos/{id}")
    public ResponseEntity<ProductoResponse> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.consultarProducto(id));
    }

    /** POST /api/inventario/productos - Crear un nuevo producto en catálogo */
    @PostMapping("/productos")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearProducto(solicitud));
    }

    /** GET /api/inventario/disponibilidad/{id}?cantidad=3   (RF-02) */
    @GetMapping("/disponibilidad/{id}")
    public ResponseEntity<DisponibilidadResponse> disponibilidad(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int cantidad) {
        return ResponseEntity.ok(servicio.consultarDisponibilidad(id, cantidad));
    }

    /** POST /api/inventario/verificar   (RF-04, lo usa el servicio de pedidos via ESB) */
    @PostMapping("/verificar")
    public ResponseEntity<VerificacionResponse> verificar(@Valid @RequestBody VerificacionRequest solicitud) {
        return ResponseEntity.ok(servicio.verificarExistencias(solicitud));
    }

    /** POST /api/inventario/descontar - Descuenta stock de los productos tras un pedido confirmado */
    @PostMapping("/descontar")
    public ResponseEntity<List<ProductoResponse>> descontar(@Valid @RequestBody VerificacionRequest solicitud) {
        return ResponseEntity.ok(servicio.descontarStock(solicitud));
    }
}
