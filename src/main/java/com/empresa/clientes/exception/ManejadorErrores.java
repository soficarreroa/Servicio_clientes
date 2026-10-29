package com.empresa.clientes.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ManejadorErrores {

    // 400 por validaciones
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        // tomar primer error con mensaje claro
        String mensaje = "Datos inválidos";
        if (ex.getBindingResult() != null && ex.getBindingResult().getFieldError() != null) {
            FieldError fe = ex.getBindingResult().getFieldError();
            if (fe != null && fe.getDefaultMessage() != null) {
                mensaje = fe.getDefaultMessage();
            }
        }
        body.put("fecha", LocalDateTime.now());
        body.put("estado", 400);
        body.put("error", "Bad Request");
        body.put("mensaje", mensaje);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 404
    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(ClienteNoEncontradoException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("fecha", LocalDateTime.now());
        body.put("estado", 404);
        body.put("error", "Not Found");
        body.put("mensaje", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // 409
    @ExceptionHandler(ClienteDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> manejarDuplicado(ClienteDuplicadoException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("fecha", LocalDateTime.now());
        body.put("estado", 409);
        body.put("error", "Conflict");
        body.put("mensaje", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    // 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> manejarGeneral(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("fecha", LocalDateTime.now());
        body.put("estado", 500);
        body.put("error", "Internal Server Error");
        body.put("mensaje", "Ocurrió un error interno");
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
