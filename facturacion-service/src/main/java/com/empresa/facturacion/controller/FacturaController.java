package com.empresa.facturacion.controller;

import com.empresa.facturacion.dto.FacturaRequest;
import com.empresa.facturacion.dto.FacturaResponse;
import com.empresa.facturacion.service.FacturacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facturas")
@CrossOrigin(origins = "*")
public class FacturaController {

    private final FacturacionService service;

    public FacturaController(FacturacionService service) {
        this.service = service;
    }

    /** POST /api/facturas - Genera la factura para un pedido confirmado */
    @PostMapping
    public ResponseEntity<FacturaResponse> crear(@Valid @RequestBody FacturaRequest request) {
        FacturaResponse response = service.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** GET /api/facturas/{id} - Consulta factura por ID */
    @GetMapping("/{id}")
    public ResponseEntity<FacturaResponse> consultarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.consultarPorId(id));
    }

    /** GET /api/facturas/pedido/{pedidoId} - Consulta factura asociada a un pedido */
    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<FacturaResponse> consultarPorPedidoId(@PathVariable Long pedidoId) {
        return ResponseEntity.ok(service.consultarPorPedidoId(pedidoId));
    }

    /** GET /api/facturas o GET /api/facturas?clienteId=N - Lista facturas */
    @GetMapping
    public ResponseEntity<List<FacturaResponse>> listar(@RequestParam(required = false) Long clienteId) {
        if (clienteId != null) {
            return ResponseEntity.ok(service.listarPorCliente(clienteId));
        }
        return ResponseEntity.ok(service.listarTodas());
    }
}

