package com.empresa.pedidos.exception;

import com.empresa.pedidos.dto.RespuestaError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Convierte cada excepción en una respuesta JSON clara (RNF-07)
 * sin mostrar detalles internos del sistema (RNF-05): el detalle solo queda en el log.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 404: cliente o pedido inexistente. */
    @ExceptionHandler({ClienteNoExisteException.class, PedidoNoEncontradoException.class})
    public ResponseEntity<RespuestaError> noEncontrado(RuntimeException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** 503: el ESB o un servicio detrás de él no respondió (RF-P13). */
    @ExceptionHandler(EsbNoDisponibleException.class)
    public ResponseEntity<RespuestaError> esbNoDisponible(EsbNoDisponibleException ex) {
        log.warn("No se pudo completar la comunicación con el ESB: {}", ex.getMessage(), ex.getCause());
        return construir(HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio no está disponible en este momento. Intente nuevamente más tarde");
    }

    /** 400: datos del pedido inválidos (RF-P02, RF-P03, RF-P04). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> validacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
        if (detalle.isBlank()) {
            detalle = "Los datos enviados no son válidos";
        }
        return construir(HttpStatus.BAD_REQUEST, detalle);
    }

    /** 400: JSON mal formado o con tipos incorrectos. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> jsonInvalido(HttpMessageNotReadableException ex) {
        return construir(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición no es un JSON válido o tiene datos con un tipo incorrecto");
    }

    /** 400: parámetro de ruta o de consulta con un valor del tipo equivocado (por ejemplo clienteId=abc). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespuestaError> tipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        return construir(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor no válido");
    }

    /** Cualquier otro error: 4xx propios de Spring MVC con su código real, o 500 genérico. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> inesperado(Exception ex) {
        // Ruta inexistente, método no permitido, tipo de contenido no soportado...
        if (ex instanceof org.springframework.web.ErrorResponse errorSpring
                && errorSpring.getStatusCode().is4xxClientError()) {
            HttpStatus status = HttpStatus.valueOf(errorSpring.getStatusCode().value());
            return construir(status, mensajeParaPeticionInvalida(status));
        }
        log.error("Error inesperado en pedidos-service", ex); // el detalle solo queda en el log
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Intente nuevamente más tarde");
    }

    private String mensajeParaPeticionInvalida(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "El recurso solicitado no existe";
            case METHOD_NOT_ALLOWED -> "El método HTTP no está permitido para este recurso";
            case UNSUPPORTED_MEDIA_TYPE -> "El tipo de contenido no es soportado: use application/json";
            default -> "La petición no es válida para este servicio";
        };
    }

    private ResponseEntity<RespuestaError> construir(HttpStatus status, String mensaje) {
        RespuestaError cuerpo = new RespuestaError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(), status.getReasonPhrase(), mensaje);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
