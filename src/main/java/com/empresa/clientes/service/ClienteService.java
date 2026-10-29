package com.empresa.clientes.service;

import com.empresa.clientes.dto.ClienteRequest;
import com.empresa.clientes.exception.ClienteDuplicadoException;
import com.empresa.clientes.exception.ClienteNoEncontradoException;
import com.empresa.clientes.model.Cliente;
import com.empresa.clientes.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public Cliente registrar(ClienteRequest request) {
        // validar duplicados
        if (clienteRepository.existsByDocumento(request.getDocumento())) {
            throw new ClienteDuplicadoException("Ya existe un cliente con ese documento");
        }
        String emailLower = request.getEmail() == null ? null : request.getEmail().toLowerCase().trim();
        if (clienteRepository.existsByEmail(emailLower)) {
            throw new ClienteDuplicadoException("Ya existe un cliente con ese email");
        }

        Cliente cliente = new Cliente();
        cliente.setDocumento(request.getDocumento());
        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setEmail(emailLower);
        cliente.setTelefono(request.getTelefono());
        cliente.setDireccion(request.getDireccion());
        // fechaRegistro se pone en @PrePersist
        return clienteRepository.save(cliente);
    }

    @Transactional(readOnly = true)
    public Cliente obtener(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(id));
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }
}
