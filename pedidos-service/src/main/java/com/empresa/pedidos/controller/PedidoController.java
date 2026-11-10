package com.empresa.pedidos.controller;

import com.empresa.pedidos.dto.PedidoRequest;
import com.empresa.pedidos.dto.PedidoResponse;
import com.empresa.pedidos.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService servicio;

    public PedidoController(PedidoService servicio) {
        this.servicio = servicio;
    }

    /**
     * POST /api/pedidos (RF-03). Responde 201 tanto si el pedido queda CONFIRMADO como RECHAZADO:
     * en ambos casos el pedido se registró y el resultado está en el campo "estado".
     */
    @PostMapping
    public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody PedidoRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(solicitud));
    }

    /** GET /api/pedidos/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.consultar(id));
    }

    /** GET /api/pedidos  o  GET /api/pedidos?clienteId=N */
    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listar(@RequestParam(required = false) Long clienteId) {
        return ResponseEntity.ok(servicio.listar(clienteId));
    }
}
