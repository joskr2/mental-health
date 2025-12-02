package com.clinica.mentalhealth.ai.tools;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para que la IA pueda crear un nuevo paciente.
 * Disponible para ADMIN y PSYCHOLOGIST.
 */
@Schema(description = "Datos para crear un nuevo paciente vía IA")
public record CreatePatientRequest(
  @Schema(
    description = "Nombre completo del paciente",
    example = "Juan Pérez García"
  )
  @NotBlank(message = "El nombre es requerido")
  @Size(
    min = 2,
    max = 255,
    message = "El nombre debe tener entre 2 y 255 caracteres"
  )
  String name,

  @Schema(
    description = "Correo electrónico del paciente",
    example = "juan.perez@email.com"
  )
  @NotBlank(message = "El correo electrónico es requerido")
  @Email(message = "El correo electrónico debe ser válido")
  @Size(max = 255, message = "El correo no puede exceder 255 caracteres")
  String email,

  @Schema(description = "Teléfono de contacto", example = "+51999888777")
  @NotBlank(message = "El teléfono es requerido")
  @Pattern(
    regexp = "^\\+?\\d{6,20}$",
    message = "El teléfono debe contener entre 6 y 20 dígitos"
  )
  String phone,

  @Schema(description = "DNI del paciente (8 dígitos)", example = "12345678")
  @NotBlank(message = "El DNI es requerido")
  @Pattern(
    regexp = "^\\d{8}$",
    message = "El DNI debe tener exactamente 8 dígitos"
  )
  String dni
) {}
