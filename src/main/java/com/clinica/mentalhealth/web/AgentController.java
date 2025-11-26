package com.clinica.mentalhealth.web;

import com.clinica.mentalhealth.service.ClinicalAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Tag(name = "Agente IA", description = "Chat con el asistente clínico de inteligencia artificial")
@SecurityRequirement(name = "Bearer Authentication")
public class AgentController {

    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final ClinicalAgentService agentService;

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('ADMIN', 'PSYCHOLOGIST')")
    @Operation(summary = "Chat con IA", description = "Envía una consulta al agente clínico de IA y recibe una respuesta")
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

        return agentService.processRequest(text)
                .map(response -> Map.of("response", response));
    }
}
