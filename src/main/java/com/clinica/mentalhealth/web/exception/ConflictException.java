package com.clinica.mentalhealth.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción para conflictos de negocio (409 Conflict).
 * Se lanza cuando una operación no puede completarse debido a un conflicto
 * con el estado actual del recurso (ej: horarios superpuestos).
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {
  public ConflictException(String message) {
    super(message);
  }
}
