package com.empresa.facturacion.service;

import com.empresa.facturacion.dto.FacturaRequest;
import com.empresa.facturacion.dto.FacturaResponse;
import com.empresa.facturacion.exception.FacturaDuplicadaException;
import com.empresa.facturacion.exception.FacturaNoEncontradaException;
import com.empresa.facturacion.exception.MontoInvalidoException;
import com.empresa.facturacion.model.Factura;
import com.empresa.facturacion.repository.FacturaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FacturacionService {

    private static final Logger log = LoggerFactory.getLogger(FacturacionService.class);

    private final FacturaRepository repository;

    public FacturacionService(FacturaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public FacturaResponse crear(FacturaRequest request) {
        if (request.montoTotal() == null || request.montoTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new MontoInvalidoException("El monto total debe ser mayor que cero");
        }

        if (repository.existsByPedidoId(request.pedidoId())) {
            log.warn("Intento de generar factura duplicada para pedido {}", request.pedidoId());
            throw new FacturaDuplicadaException("Ya existe una factura registrada para el pedido " + request.pedidoId());
        }

        Factura factura = new Factura(request.pedidoId(), request.clienteId(), request.montoTotal());
        Factura guardada = repository.save(factura);
        log.info("Factura #{} generada para pedido #{} (cliente: {}, monto: {})",
                guardada.getId(), guardada.getPedidoId(), guardada.getClienteId(), guardada.getMontoTotal());

        return FacturaResponse.desdeEntidad(guardada);
    }

    @Transactional(readOnly = true)
    public FacturaResponse consultarPorId(Long id) {
        return repository.findById(id)
                .map(FacturaResponse::desdeEntidad)
                .orElseThrow(() -> new FacturaNoEncontradaException("Factura con ID " + id + " no encontrada"));
    }

    @Transactional(readOnly = true)
    public FacturaResponse consultarPorPedidoId(Long pedidoId) {
        return repository.findByPedidoId(pedidoId)
                .map(FacturaResponse::desdeEntidad)
                .orElseThrow(() -> new FacturaNoEncontradaException("No existe factura para el pedido con ID " + pedidoId));
    }

    @Transactional(readOnly = true)
    public List<FacturaResponse> listarTodas() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(FacturaResponse::desdeEntidad)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FacturaResponse> listarPorCliente(Long clienteId) {
        return repository.findByClienteIdOrderByFechaEmisionDesc(clienteId)
                .stream()
                .map(FacturaResponse::desdeEntidad)
                .toList();
    }
}

