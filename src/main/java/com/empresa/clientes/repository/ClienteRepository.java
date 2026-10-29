package com.empresa.clientes.repository;

import com.empresa.clientes.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByDocumento(String documento);
    boolean existsByEmail(String email);
    Optional<Cliente> findByDocumento(String documento);
    Optional<Cliente> findByEmail(String email);
}
