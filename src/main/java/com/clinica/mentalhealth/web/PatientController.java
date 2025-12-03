package com.clinica.mentalhealth.web;

import com.clinica.mentalhealth.domain.Patient;
import com.clinica.mentalhealth.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Pacientes", description = "API para gestión de pacientes")
public class PatientController {

  private final PatientService patientService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
    summary = "Listar todos los pacientes",
    description = "Solo accesible por administradores"
  )
  @ApiResponse(
    responseCode = "200",
    description = "Lista de pacientes obtenida exitosamente"
  )
  @ApiResponse(responseCode = "403", description = "Acceso denegado")
  public Flux<Patient> getAllPatients() {
    return patientService.findAll();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'PSYCHOLOGIST')")
  @Operation(
    summary = "Obtener paciente por ID",
    description = "Accesible por administradores y psicólogos"
  )
  @ApiResponse(
    responseCode = "200",
    description = "Paciente encontrado",
    content = @Content(schema = @Schema(implementation = Patient.class))
  )
  @ApiResponse(responseCode = "404", description = "Paciente no encontrado")
  @ApiResponse(responseCode = "403", description = "Acceso denegado")
  public Mono<Patient> getPatientById(
    @Parameter(description = "ID del paciente") @PathVariable @NonNull Long id
  ) {
    return patientService.findById(id);
  }

  @GetMapping("/search")
  @PreAuthorize("hasAnyRole('ADMIN', 'PSYCHOLOGIST')")
  @Operation(
    summary = "Buscar pacientes",
    description = "Búsqueda híbrida por DNI (números) o nombre (texto)"
  )
  @ApiResponse(responseCode = "200", description = "Resultados de búsqueda")
  public Flux<Patient> searchPatients(
    @Parameter(
      description = "Término de búsqueda (DNI o nombre)"
    ) @RequestParam @NotBlank(
      message = "El término de búsqueda no puede estar vacío"
    ) @Size(
      min = 2,
      max = 100,
      message = "El término de búsqueda debe tener entre 2 y 100 caracteres"
    ) String query
  ) {
    return patientService.searchPatient(query);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
    summary = "Crear nuevo paciente",
    description = "Solo accesible por administradores"
  )
  @ApiResponse(
    responseCode = "201",
    description = "Paciente creado exitosamente",
    content = @Content(schema = @Schema(implementation = Patient.class))
  )
  @ApiResponse(
    responseCode = "400",
    description = "Datos inválidos o DNI duplicado"
  )
  @ApiResponse(responseCode = "403", description = "Acceso denegado")
  public Mono<Patient> createPatient(
    @Valid @RequestBody CreatePatientRequest request
  ) {
    return patientService.createPatient(
      request.name(),
      request.email(),
      request.phone(),
      request.dni()
    );
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
    summary = "Actualizar paciente",
    description = "Solo accesible por administradores"
  )
  @ApiResponse(
    responseCode = "200",
    description = "Paciente actualizado exitosamente"
  )
  @ApiResponse(responseCode = "404", description = "Paciente no encontrado")
  @ApiResponse(
    responseCode = "400",
    description = "Datos inválidos o DNI duplicado"
  )
  @ApiResponse(responseCode = "403", description = "Acceso denegado")
  public Mono<Patient> updatePatient(
    @Parameter(description = "ID del paciente") @PathVariable @NonNull Long id,
    @Valid @RequestBody UpdatePatientRequest request
  ) {
    return patientService.updatePatient(
      id,
      request.name(),
      request.email(),
      request.phone(),
      request.dni()
    );
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
    summary = "Eliminar paciente",
    description = "Solo accesible por administradores"
  )
  @ApiResponse(
    responseCode = "204",
    description = "Paciente eliminado exitosamente"
  )
  @ApiResponse(responseCode = "404", description = "Paciente no encontrado")
  @ApiResponse(responseCode = "403", description = "Acceso denegado")
  public Mono<Void> deletePatient(
    @Parameter(description = "ID del paciente") @PathVariable @NonNull Long id
  ) {
    return patientService.deletePatient(id);
  }

  // DTOs internos para requests con validación completa
  @Schema(description = "Datos para crear un nuevo paciente")
  public record CreatePatientRequest(
    @Schema(
      description = "Nombre completo del paciente",
      example = "Juan Pérez García"
    ) @NotBlank(message = "El nombre es requerido") @Size(
      min = 2,
      max = 255,
      message = "El nombre debe tener entre 2 y 255 caracteres"
    ) String name,
    @Schema(
      description = "Correo electrónico",
      example = "juan@email.com"
    ) @NotBlank(message = "El correo electrónico es requerido") @Email(
      message = "El correo electrónico debe ser válido"
    ) @Size(
      max = 255,
      message = "El correo no puede exceder 255 caracteres"
    ) String email,
    @Schema(
      description = "Teléfono de contacto",
      example = "+51999888777"
    ) @NotBlank(message = "El teléfono es requerido") @Pattern(
      regexp = "^\\+?\\d{6,20}$",
      message = "El teléfono debe contener entre 6 y 20 dígitos"
    ) String phone,
    @Schema(
      description = "DNI del paciente (8 dígitos)",
      example = "12345678"
    ) @NotBlank(message = "El DNI es requerido") @Pattern(
      regexp = "^\\d{8}$",
      message = "El DNI debe tener exactamente 8 dígitos"
    ) String dni
  ) {}

  @Schema(description = "Datos para actualizar un paciente existente")
  public record UpdatePatientRequest(
    @Schema(
      description = "Nombre completo del paciente",
      example = "Juan Pérez García"
    ) @NotBlank(message = "El nombre es requerido") @Size(
      min = 2,
      max = 255,
      message = "El nombre debe tener entre 2 y 255 caracteres"
    ) String name,
    @Schema(
      description = "Correo electrónico",
      example = "juan.perez@email.com"
    ) @NotBlank(message = "El correo electrónico es requerido") @Email(
      message = "El correo electrónico debe ser válido"
    ) @Size(
      max = 255,
      message = "El correo no puede exceder 255 caracteres"
    ) String email,
    @Schema(
      description = "Teléfono de contacto",
      example = "+51999888777"
    ) @NotBlank(message = "El teléfono es requerido") @Pattern(
      regexp = "^\\+?\\d{6,20}$",
      message = "El teléfono debe contener entre 6 y 20 dígitos"
    ) String phone,
    @Schema(
      description = "DNI del paciente (8 dígitos)",
      example = "12345678"
    ) @NotBlank(message = "El DNI es requerido") @Pattern(
      regexp = "^\\d{8}$",
      message = "El DNI debe tener exactamente 8 dígitos"
    ) String dni
  ) {}
}
