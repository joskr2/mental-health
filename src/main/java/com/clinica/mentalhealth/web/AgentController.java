package com.clinica.mentalhealth.web;

import com.clinica.mentalhealth.service.ClinicalAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController // <--- ¡ASEGÚRATE QUE TENGA ESTO!
@RequestMapping("/api/agent") // <--- Y ESTO
@RequiredArgsConstructor
public class AgentController {

    private final ClinicalAgentService agentService;

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('ADMIN', 'ROLE_PSYCHOLOGIST')")
    public Mono<Map<String, String>> chat(@RequestBody Map<String, String> body) {
        return agentService.processRequest(body.get("text"))
                .map(response -> Map.of("response", response));
    }
}