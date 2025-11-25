package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.security.UserPrincipal;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;

@Service
public class ClinicalAgentService {

    private final ChatClient chatClient;

    public ClinicalAgentService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultFunctions("searchPatientTool", "createPatientTool", "bookAppointmentTool")
                .build();
    }

    public Mono<String> processRequest(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return Mono.error(new IllegalArgumentException("User message cannot be null"));
        }
        final String validatedMessage = userMessage;
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (UserPrincipal) ctx.getAuthentication().getPrincipal())
                .flatMap(user -> extracted(validatedMessage, user));
    }

    private Mono<String> extracted(String userMessage, UserPrincipal user) {
        // Datos auxiliares para ayudar a la IA a ubicarse en el tiempo
        LocalDateTime now = LocalDateTime.now();
        String dayOfWeek = now.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));

        // EL SECRETO ESTÁ AQUÍ: Instrucciones explícitas
        String systemPrompt = """
                Eres el Asistente Clínico Supremo (DeepSeek).

                --- CONTEXTO OBLIGATORIO ---
                FECHA ACTUAL: %s (Es %s).
                USUARIO ACTIVO: %s (ID: %d, ROL: %s).

                --- TUS HERRAMIENTAS ---
                1. 'searchPatientTool': Para buscar ID de pacientes (búsqueda por nombre o DNI).
                2. 'createPatientTool': Para registrar nuevos. (Pide DNI si falta).
                3. 'bookAppointmentTool': Para agendar.

                --- REGLAS DE RAZONAMIENTO (¡SÍGUELAS!) ---
                1. INFERENCIA DE FECHAS: Si el usuario dice "mañana", "el lunes", "pasado mañana", TÚ DEBES CALCULAR la fecha exacta en formato ISO (YYYY-MM-DDTHH:mm:ss) basándote en la FECHA ACTUAL. ¡No preguntes la fecha al usuario!
                2. INFERENCIA DE DOCTOR: Si el usuario activo es un Psicólogo (ROLE_PSYCHOLOGIST) y dice "agéndalo conmigo" o no especifica doctor, USA SU ID (%d) como 'psychologistId'. ¡No preguntes el ID del doctor!
                3. FLUJO: Si creas un paciente, usa el ID que te devuelve la herramienta 'createPatientTool' para agendar la cita inmediatamente en el siguiente paso.

                Responde confirmando la acción con los datos exactos (Nombre del paciente, fecha y hora).
                """
                .formatted(
                        now,
                        dayOfWeek,
                        user.username(), user.id(), user.role(),
                        user.id() // <--- Aquí inyectamos el ID del doctor para que la regla 2 funcione
                );

        return Mono.fromCallable(() -> chatClient.prompt()
                .system(Objects.requireNonNull(systemPrompt))
                .user(Objects.requireNonNull(userMessage))
                .call()
                .content()).subscribeOn(Schedulers.boundedElastic());
    }
}
