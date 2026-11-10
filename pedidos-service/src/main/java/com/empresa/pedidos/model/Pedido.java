package com.empresa.pedidos.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pedido ya procesado: nace CONFIRMADO o RECHAZADO (no hay estados intermedios).
 * {@code clienteId} es solo una referencia: el cliente vive en db_clientes.
 */
@Entity
@Table(name = "pedidos",
        indexes = @Index(name = "idx_pedidos_cliente_id", columnList = "cliente_id"))
public class Pedido {

    /** Longitud máxima guardada para el motivo de rechazo. */
    public static final int MOTIVO_RECHAZO_MAX = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPedido estado;

    @Column(name = "motivo_rechazo", length = MOTIVO_RECHAZO_MAX)
    private String motivoRechazo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoItem> items = new ArrayList<>();

    protected Pedido() {
        // requerido por JPA
    }

    private Pedido(Long clienteId, EstadoPedido estado, String motivoRechazo, List<PedidoItem> items) {
        this.clienteId = clienteId;
        this.estado = estado;
        this.motivoRechazo = motivoRechazo;
        items.forEach(this::agregarItem);
    }

    public static Pedido confirmado(Long clienteId, List<PedidoItem> items) {
        return new Pedido(clienteId, EstadoPedido.CONFIRMADO, null, items);
    }

    public static Pedido rechazado(Long clienteId, List<PedidoItem> items, String motivoRechazo) {
        return new Pedido(clienteId, EstadoPedido.RECHAZADO, motivoRechazo, items);
    }

    private void agregarItem(PedidoItem item) {
        item.asignarPedido(this);
        items.add(item);
    }

    /** RF-P09: la fecha se asigna sola al guardar (sin fracciones de segundo, como en el contrato). */
    @PrePersist
    void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        }
    }

    public Long getId() { return id; }
    public Long getClienteId() { return clienteId; }
    public EstadoPedido getEstado() { return estado; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public List<PedidoItem> getItems() { return Collections.unmodifiableList(items); }
}
