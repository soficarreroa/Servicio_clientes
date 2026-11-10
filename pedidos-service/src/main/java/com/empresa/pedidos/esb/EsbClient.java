package com.empresa.pedidos.esb;

import com.empresa.pedidos.dto.ItemPedidoRequest;
import com.empresa.pedidos.exception.EsbNoDisponibleException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Objects;

/**
 * Única puerta de salida del servicio de pedidos (RF-07, RF-P14): todas las llamadas
 * a clientes e inventarios pasan por el ESB. Si el ESB no responde a tiempo, responde
 * con error o devuelve algo que no se entiende, lanza {@link EsbNoDisponibleException}.
 */
@Component
public class EsbClient {

    private final RestClient restClient;

    public EsbClient(RestClient esbRestClient) {
        this.restClient = esbRestClient;
    }

    /**
     * RF-P05: GET /api/clientes/{id} -> 200 (existe) o 404 (no existe).
     *
     * @return true si el cliente existe, false si el ESB responde 404
     * @throws EsbNoDisponibleException si no se pudo saber (ESB caído, timeout, 5xx...)
     */
    public boolean clienteExiste(Long clienteId) {
        try {
            restClient.get()
                    .uri("/api/clientes/{id}", clienteId)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (RestClientException e) {
            throw new EsbNoDisponibleException("Fallo al consultar el cliente " + clienteId + " por el ESB", e);
        }
    }

    /**
     * RF-P06: POST /api/inventario/verificar. Se envían los productos tal como llegaron en el pedido
     * (si uno se repite, inventarios suma las cantidades).
     *
     * @throws EsbNoDisponibleException si no se pudo verificar (ESB caído, timeout, 5xx, respuesta inválida...)
     */
    public ResultadoVerificacion verificarInventario(List<ItemPedidoRequest> items) {
        VerificacionInventarioResponse respuesta;
        try {
            respuesta = restClient.post()
                    .uri("/api/inventario/verificar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new VerificacionInventarioRequest(items))
                    .retrieve()
                    .body(VerificacionInventarioResponse.class);
        } catch (RestClientException e) {
            throw new EsbNoDisponibleException("Fallo al verificar el inventario por el ESB", e);
        }

        // Sin todoDisponible no se puede decidir: nunca se acepta un pedido sin verificar el inventario.
        if (respuesta == null || respuesta.todoDisponible() == null) {
            throw new EsbNoDisponibleException("La respuesta de inventarios no trae 'todoDisponible'");
        }
        return new ResultadoVerificacion(respuesta.todoDisponible(), noDisponibles(respuesta));
    }

    private List<ProductoNoDisponible> noDisponibles(VerificacionInventarioResponse respuesta) {
        if (respuesta.items() == null) {
            return List.of();
        }
        return respuesta.items().stream()
                .filter(Objects::nonNull)
                .filter(item -> Boolean.FALSE.equals(item.disponible()))
                .map(item -> new ProductoNoDisponible(item.productoId(), item.mensaje()))
                .toList();
    }
}
