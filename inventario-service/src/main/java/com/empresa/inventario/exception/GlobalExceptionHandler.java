package com.empresa.inventario.exception;

import com.empresa.inventario.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Convierte las excepciones en respuestas JSON claras (RNF-07)
 * sin exponer detalles internos (RNF-05).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProductoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(ProductoNoEncontradoException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CantidadInvalidaException.class)
    public ResponseEntity<ErrorResponse> cantidadInvalida(CantidadInvalidaException ex) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (detalle.isBlank()) {
            detalle = "Los datos enviados no son válidos";
        }
        return construir(HttpStatus.BAD_REQUEST, detalle);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> formatoInvalido(Exception ex) {
        return construir(HttpStatus.BAD_REQUEST,
                "La petición tiene un formato incorrecto (revise el JSON y los tipos de los datos)");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex) {
        // Errores de Spring MVC (ruta inexistente, metodo no permitido, falta un parametro...):
        // se responden con su codigo 4xx real en lugar de un 500.
        if (ex instanceof org.springframework.web.ErrorResponse er && er.getStatusCode().is4xxClientError()) {
            HttpStatus st = HttpStatus.valueOf(er.getStatusCode().value());
            return construir(st, "La petición no es válida para este servicio");
        }
        log.error("Error inesperado en inventario-service", ex); // el detalle solo queda en el log
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Intente nuevamente más tarde");
    }

    private ResponseEntity<ErrorResponse> construir(HttpStatus status, String mensaje) {
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(), status.value(),
                status.getReasonPhrase(), mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
