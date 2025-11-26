package com.clinica.mentalhealth.web;

import com.clinica.mentalhealth.service.ClinicalAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final ClinicalAgentService agentService;

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('ADMIN', 'PSYCHOLOGIST')")
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