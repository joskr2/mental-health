package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.security.UserPrincipal;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@Service
public class ClinicalAgentService {

    private final ChatClient chatClient;

    public ClinicalAgentService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultFunctions("searchPatientTool", "createPatientTool", "bookAppointmentTool")
                .build();
    }

    public Mono<String> processRequest(String userMessage) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (UserPrincipal) ctx.getAuthentication().getPrincipal())
                .flatMap(user -> {

                    String systemPrompt = """
                    Eres un Asistente Clínico Inteligente (DeepSeek).
                    FECHA: %s
                    USUARIO: %s (ID: %d)
                    
                    CAPACIDADES:
                    1. Si piden agendar con un paciente, PRIMERO usa 'searchPatientTool'.
                    2. Si no existe, pregunta si crear o usa 'createPatientTool' si tienes datos.
                    3. Para agendar, usa 'bookAppointmentTool'. Doctor ID = Tu ID (%d).
                    
                    Responde en español conciso.
                    """.formatted(LocalDateTime.now(), user.username(), user.id(), user.id());

                    return Mono.fromCallable(() ->
                            chatClient.prompt()
                                    .system(systemPrompt)
                                    .user(userMessage)
                                    .call()
                                    .content()
                    ).subscribeOn(Schedulers.boundedElastic());
                });
    }
}