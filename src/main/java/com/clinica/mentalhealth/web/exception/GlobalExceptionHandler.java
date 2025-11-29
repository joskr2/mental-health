package com.clinica.mentalhealth.web.exception;

import com.clinica.mentalhealth.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Error 400: Datos inválidos o Reglas de Negocio simples (Ej: Citas en
    // domingo)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), exchange);
    }

    // 2. Error 409: Conflictos de Estado (Ej: Doctor ocupado, Sala ocupada)
    @ExceptionHandler({ IllegalStateException.class, ConflictException.class })
    public ResponseEntity<ErrorResponse> handleConflict(Exception ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), exchange);
    }

    // 3. Error 403: Acceso Denegado (Ej: Paciente intentando agendar por otro)
    // Capturamos tanto la nuestra (IllegalAccess) como la de Spring (AccessDenied)
    @ExceptionHandler({ IllegalAccessException.class, AccessDeniedException.class })
    public ResponseEntity<ErrorResponse> handleForbidden(Exception ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.FORBIDDEN,
                "No tienes permiso para realizar esta acción o acceder a estos datos.", exchange);
    }

    // 4. Error 401: Credenciales inválidas
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(BadCredentialsException ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos.", exchange);
    }

    // 5. Error 500: Cualquier otra cosa que no esperábamos
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, ServerWebExchange exchange) {
        ex.printStackTrace();
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado en el servidor.", exchange);
    }

    // Método auxiliar para construir el JSON
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, ServerWebExchange exchange) {
        var errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value());
        return ResponseEntity.status(status).body(errorResponse);
    }
}
