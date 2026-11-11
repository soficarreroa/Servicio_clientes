package com.empresa.facturacion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "facturas", indexes = {
    @Index(name = "idx_factura_pedido", columnList = "pedidoId", unique = true),
    @Index(name = "idx_factura_cliente", columnList = "clienteId")
})
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "pedido_id", nullable = false, unique = true)
    private Long pedidoId;

    @NotNull
    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @NotNull
    @DecimalMin("0.01")
    @Column(name = "monto_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "fecha_emision", nullable = false, updatable = false)
    private LocalDateTime fechaEmision;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoFactura estado;

    public Factura() {
    }

    public Factura(Long pedidoId, Long clienteId, BigDecimal montoTotal) {
        this.pedidoId = pedidoId;
        this.clienteId = clienteId;
        this.montoTotal = montoTotal;
        this.estado = EstadoFactura.GENERADA;
        this.fechaEmision = LocalDateTime.now();
    }

    public Factura(Long id, Long pedidoId, Long clienteId, BigDecimal montoTotal, LocalDateTime fechaEmision, EstadoFactura estado) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.clienteId = clienteId;
        this.montoTotal = montoTotal;
        this.fechaEmision = fechaEmision;
        this.estado = estado;
    }

    @PrePersist
    public void prePersist() {
        if (this.fechaEmision == null) {
            this.fechaEmision = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = EstadoFactura.GENERADA;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public EstadoFactura getEstado() {
        return estado;
    }

    public void setEstado(EstadoFactura estado) {
        this.estado = estado;
    }
}

