package com.empresa.pedidos.repository;

import com.empresa.pedidos.model.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Los métodos de lectura traen los items en la misma consulta (EntityGraph):
 * así no hay una consulta extra por pedido (problema N+1).
 */
@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Override
    @EntityGraph(attributePaths = "items")
    Optional<Pedido> findById(Long id);

    @EntityGraph(attributePaths = "items")
    List<Pedido> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = "items")
    List<Pedido> findByClienteIdOrderByIdAsc(Long clienteId);
}
