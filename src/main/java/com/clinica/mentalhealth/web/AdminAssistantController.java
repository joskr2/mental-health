package com.clinica.mentalhealth.web;

import com.clinica.mentalhealth.service.AdminAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Controller para el Asistente Administrativo de IA.
 * 
 * IMPORTANTE: Este asistente es SOLO para gestión administrativa.
 * NO brinda consultas psicológicas ni asesoramiento clínico.
 */
@RestController
@RequestMapping("/api/admin-assistant")
@RequiredArgsConstructor
@Tag(name = "Asistente Administrativo", 
     description = "Chat con el asistente administrativo de IA para gestión de citas, pacientes y recursos")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminAssistantController {

    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final AdminAssistantService assistantService;

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('ADMIN', 'PSYCHOLOGIST', 'PATIENT')")
    @Operation(
        summary = "Chat con Asistente Administrativo",
        description = """
            Envía consultas en lenguaje natural al asistente de IA para:
            
            ✅ PUEDE HACER:
            - Agendar, consultar o cancelar citas (Admin/Psicólogo)
            - Registrar y buscar pacientes (Admin/Psicólogo)
            - Consultar disponibilidad de psicólogos
            - Ver citas futuras (Todos los roles según permisos)
            - Gestionar psicólogos y salas (Solo Admin)
            
            ❌ NO PUEDE HACER:
            - Brindar consultas psicológicas
            - Diagnosticar síntomas
            - Recomendar tratamientos
            
            ROLES:
            - ADMIN: Acceso total a todas las funciones
            - PSYCHOLOGIST: Gestión de citas y pacientes
            - PATIENT: Solo consultar sus propias citas (lectura)
            
            Ejemplo: "Muestra mis citas de la próxima semana"
            Ejemplo: "¿Qué horarios tiene libre la Dra. Martínez mañana?"
            """
    )
    public Mono<Map<String, String>> chat(@RequestBody Map<String, String> body) {
        String text = body.get("text");

        // 1. Validación: no vacío
        if (text == null || text.isBlank()) {
            return Mono.error(new IllegalArgumentException("La consulta no puede estar vacía."));
        }

        // 2. Límite de seguridad (Evita inyecciones complejas y ataques de token)
        if (text.length() > MAX_MESSAGE_LENGTH) {
            return Mono.error(new IllegalArgumentException(
                    "Consulta demasiado larga (máx " + MAX_MESSAGE_LENGTH + " caracteres)."));
        }

        return assistantService.processRequest(text)
                .map(response -> Map.of("response", response));
    }
}
