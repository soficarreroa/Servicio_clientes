package com.empresa.facturacion.repository;

import com.empresa.facturacion.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {
    Optional<Factura> findByPedidoId(Long pedidoId);
    boolean existsByPedidoId(Long pedidoId);
    List<Factura> findByClienteIdOrderByFechaEmisionDesc(Long clienteId);
}

