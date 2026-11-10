package com.empresa.clientes.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ManejadorErrores {

    // 400 por validaciones
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        // tomar primer error con mensaje claro
        String mensaje = "Datos inválidos";
        if (ex.getBindingResult() != null && ex.getBindingResult().getFieldError() != null) {
            FieldError fe = ex.getBindingResult().getFieldError();
            if (fe != null && fe.getDefaultMessage() != null) {
                mensaje = fe.getDefaultMessage();
            }
        }
        return construir(HttpStatus.BAD_REQUEST, mensaje);
    }

    // 400: JSON mal formado o con tipos incorrectos
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        return construir(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición no es un JSON válido o tiene datos con un tipo incorrecto");
    }

    // 400: parámetro de ruta con un valor del tipo equivocado (por ejemplo /api/clientes/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> manejarTipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        return construir(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor no válido");
    }

    // 404
    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(ClienteNoEncontradoException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 409
    @ExceptionHandler(ClienteDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> manejarDuplicado(ClienteDuplicadoException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Cualquier otro error: 4xx propios de Spring MVC con su código real, o 500 genérico
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> manejarGeneral(Exception ex) {
        // Ruta inexistente, método no permitido, tipo de contenido no soportado...
        if (ex instanceof org.springframework.web.ErrorResponse errorSpring
                && errorSpring.getStatusCode().is4xxClientError()) {
            HttpStatus status = HttpStatus.valueOf(errorSpring.getStatusCode().value());
            return construir(status, mensajeParaPeticionInvalida(status));
        }
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno");
    }

    private String mensajeParaPeticionInvalida(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "El recurso solicitado no existe";
            case METHOD_NOT_ALLOWED -> "El método HTTP no está permitido para este recurso";
            case UNSUPPORTED_MEDIA_TYPE -> "El tipo de contenido no es soportado: use application/json";
            default -> "La petición no es válida para este servicio";
        };
    }

    // Mismo formato de respuesta que ya usaba el servicio (fecha, estado, error, mensaje)
    private ResponseEntity<Map<String, Object>> construir(HttpStatus status, String mensaje) {
        Map<String, Object> body = new HashMap<>();
        body.put("fecha", LocalDateTime.now());
        body.put("estado", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("mensaje", mensaje);
        return new ResponseEntity<>(body, status);
    }
}
