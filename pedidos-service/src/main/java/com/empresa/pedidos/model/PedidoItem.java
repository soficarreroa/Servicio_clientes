package com.empresa.pedidos.model;

import jakarta.persistence.*;

/**
 * Línea de un pedido. {@code productoId} es solo una referencia:
 * el producto vive en db_inventarios (no hay llave foránea entre bases).
 */
@Entity
@Table(name = "pedido_items")
public class PedidoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    protected PedidoItem() {
        // requerido por JPA
    }

    public PedidoItem(Long productoId, Integer cantidad) {
        this.productoId = productoId;
        this.cantidad = cantidad;
    }

    /** Solo lo usa Pedido.agregarItem para mantener la relación en ambos sentidos. */
    void asignarPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Long getId() { return id; }
    public Long getProductoId() { return productoId; }
    public Integer getCantidad() { return cantidad; }
}
