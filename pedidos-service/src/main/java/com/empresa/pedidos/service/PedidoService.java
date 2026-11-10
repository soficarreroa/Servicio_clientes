package com.empresa.pedidos.service;

import com.empresa.pedidos.dto.ItemPedidoRequest;
import com.empresa.pedidos.dto.PedidoRequest;
import com.empresa.pedidos.dto.PedidoResponse;
import com.empresa.pedidos.esb.EsbClient;
import com.empresa.pedidos.esb.ProductoNoDisponible;
import com.empresa.pedidos.esb.ResultadoVerificacion;
import com.empresa.pedidos.exception.ClienteNoExisteException;
import com.empresa.pedidos.exception.PedidoNoEncontradoException;
import com.empresa.pedidos.model.Pedido;
import com.empresa.pedidos.model.PedidoItem;
import com.empresa.pedidos.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Lógica de negocio del procesamiento de pedidos. No genera facturas, no envía notificaciones
 * ni descuenta stock: eso lo hacen sus propios servicios, coordinados por el ESB.
 */
@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    static final String MOTIVO_POR_DEFECTO = "No hay existencias suficientes para surtir el pedido";
    private static final String SEPARADOR_MOTIVOS = "; ";
    private static final String PUNTOS_SUSPENSIVOS = "...";

    private final PedidoRepository repositorio;
    private final EsbClient esb;

    public PedidoService(PedidoRepository repositorio, EsbClient esb) {
        this.repositorio = repositorio;
        this.esb = esb;
    }

    /**
     * Flujo de los pasos 3 a 6 del diseño de integración:
     * 1) valida que el cliente exista, 2) verifica el inventario, 3) guarda el pedido
     * como CONFIRMADO o RECHAZADO.
     *
     * <p>No es {@code @Transactional} a propósito: las llamadas al ESB pueden tardar hasta 7 s
     * y no deben mantener una conexión de base de datos abierta. Si cualquier llamada falla se lanza
     * una excepción antes de guardar, así que no queda nada a medias (RF-P13). El guardado final
     * ({@code save}) es una sola transacción: el pedido y sus items se guardan juntos o no se guardan.
     */
    public PedidoResponse crear(PedidoRequest solicitud) {
        Long clienteId = solicitud.clienteId();

        if (!esb.clienteExiste(clienteId)) {
            throw new ClienteNoExisteException(clienteId); // no se consulta inventario ni se guarda nada
        }

        ResultadoVerificacion verificacion = esb.verificarInventario(solicitud.items());

        List<PedidoItem> lineas = solicitud.items().stream()
                .map(item -> new PedidoItem(item.productoId(), item.cantidad()))
                .toList();

        Pedido pedido = verificacion.todoDisponible()
                ? Pedido.confirmado(clienteId, lineas)
                : Pedido.rechazado(clienteId, lineas, construirMotivo(verificacion));

        Pedido guardado = repositorio.save(pedido);
        log.info("Pedido {} {} (cliente {}, {} línea(s))",
                guardado.getId(), guardado.getEstado(), clienteId, lineas.size());
        return PedidoResponse.desde(guardado);
    }

    /** RF-P10 y RF-P11. */
    @Transactional(readOnly = true)
    public PedidoResponse consultar(Long id) {
        return repositorio.findById(id)
                .map(PedidoResponse::desde)
                .orElseThrow(() -> new PedidoNoEncontradoException(id));
    }

    /** RF-P12: todos los pedidos o solo los de un cliente. */
    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(Long clienteId) {
        List<Pedido> pedidos = clienteId == null
                ? repositorio.findAllByOrderByIdAsc()
                : repositorio.findByClienteIdOrderByIdAsc(clienteId);
        return pedidos.stream().map(PedidoResponse::desde).toList();
    }

    /** Ejemplo: "Producto 3: Stock insuficiente: hay 8 unidad(es)". Varios productos se separan con "; ". */
    private String construirMotivo(ResultadoVerificacion verificacion) {
        String motivo = verificacion.noDisponibles().stream()
                .map(this::describir)
                .collect(Collectors.joining(SEPARADOR_MOTIVOS));
        if (motivo.isBlank()) {
            motivo = MOTIVO_POR_DEFECTO;
        }
        return limitar(motivo);
    }

    private String describir(ProductoNoDisponible producto) {
        String mensaje = producto.mensaje() == null || producto.mensaje().isBlank()
                ? "No disponible"
                : producto.mensaje();
        return "Producto " + producto.productoId() + ": " + mensaje;
    }

    /** Evita que un motivo muy largo haga fallar el guardado (la columna tiene longitud máxima). */
    private String limitar(String motivo) {
        if (motivo.length() <= Pedido.MOTIVO_RECHAZO_MAX) {
            return motivo;
        }
        return motivo.substring(0, Pedido.MOTIVO_RECHAZO_MAX - PUNTOS_SUSPENSIVOS.length()) + PUNTOS_SUSPENSIVOS;
    }
}
