package com.clinica.mentalhealth.web;

import com.clinica.mentalhealth.domain.Psychologist;
import com.clinica.mentalhealth.service.PsychologistService;
import com.clinica.mentalhealth.web.dto.CreatePsychologistDto;
import com.clinica.mentalhealth.web.dto.UpdatePsychologistDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/psychologists")
@RequiredArgsConstructor
@Tag(name = "Psicólogos", description = "API para gestión de psicólogos (Solo Admin)")
public class PsychologistController {

  private final PsychologistService psychologistService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Listar todos los psicólogos", description = "Directorio completo de personal")
  @ApiResponse(responseCode = "200", description = "Lista de psicólogos obtenida exitosamente")
  @ApiResponse(responseCode = "403", description = "Acceso denegado - Solo administradores")
  public Flux<Psychologist> getAll() {
    return psychologistService.findAll();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Obtener psicólogo por ID", description = "Ver detalle de un psicólogo")
  @ApiResponse(responseCode = "200", description = "Psicólogo encontrado", content = @Content(schema = @Schema(implementation = Psychologist.class)))
  @ApiResponse(responseCode = "404", description = "Psicólogo no encontrado")
  @ApiResponse(responseCode = "403", description = "Acceso denegado")
  public Mono<Psychologist> getById(
      @Parameter(description = "ID del psicólogo") @PathVariable @NonNull Long id) {
    return psychologistService.findById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Contratar nuevo psicólogo", description = "Crea usuario (login) y perfil profesional con el mismo ID")
  @ApiResponse(responseCode = "201", description = "Psicólogo creado exitosamente", content = @Content(schema = @Schema(implementation = Psychologist.class)))
  @ApiResponse(responseCode = "400", description = "Datos inválidos")
  @ApiResponse(responseCode = "403", description = "Acceso denegado")
  public Mono<Psychologist> create(@RequestBody CreatePsychologistDto dto) {
    return psychologistService.createPsychologist(
        dto.name(),
        dto.specialty(),
        dto.email(),
        dto.phone(),
        dto.dni(),
        dto.username(),
        dto.password());
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Actualizar datos profesionales", description = "Modifica nombre, especialidad, contacto y/o DNI")
  @ApiResponse(responseCode = "200", description = "Psicólogo actualizado exitosamente")
  @ApiResponse(responseCode = "404", description = "Psicólogo no encontrado")
  @ApiResponse(responseCode = "403", description = "Acceso denegado")
  public Mono<Psychologist> update(
      @Parameter(description = "ID del psicólogo") @PathVariable @NonNull Long id,
      @RequestBody UpdatePsychologistDto dto) {
    return psychologistService.updatePsychologist(id, dto.name(), dto.specialty(), dto.email(), dto.phone(), dto.dni());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Despedir/Eliminar psicólogo", description = "Elimina el perfil profesional y el usuario de acceso")
  @ApiResponse(responseCode = "204", description = "Psicólogo eliminado exitosamente")
  @ApiResponse(responseCode = "404", description = "Psicólogo no encontrado")
  @ApiResponse(responseCode = "403", description = "Acceso denegado")
  public Mono<Void> delete(
      @Parameter(description = "ID del psicólogo") @PathVariable @NonNull Long id) {
    return psychologistService.deletePsychologist(id);
  }
}
