package com.empresa.notificaciones.controller;

import com.empresa.notificaciones.dto.NotificacionRequest;
import com.empresa.notificaciones.dto.NotificacionResponse;
import com.empresa.notificaciones.model.NotificacionAudit;
import com.empresa.notificaciones.service.NotificacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@CrossOrigin(origins = "*")
public class NotificacionController {

    private final NotificacionService service;

    public NotificacionController(NotificacionService service) {
        this.service = service;
    }

    /** POST /api/notificaciones/enviar - Recibe el evento desde el ESB y despacha la notificación */
    @PostMapping("/enviar")
    public ResponseEntity<NotificacionResponse> enviar(@Valid @RequestBody NotificacionRequest request) {
        NotificacionResponse response = service.procesarNotificacion(request);
        return ResponseEntity.ok(response);
    }

    /** GET /api/notificaciones - Bitácora de auditoría de notificaciones enviadas */
    @GetMapping
    public ResponseEntity<List<NotificacionAudit>> listar() {
        return ResponseEntity.ok(service.listarHistorial());
    }

    /** GET /api/notificaciones/{codigo} - Consulta el detalle de una notificación por su tracking */
    @GetMapping("/{codigo}")
    public ResponseEntity<NotificacionAudit> consultar(@PathVariable String codigo) {
        return service.consultarPorCodigo(codigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

