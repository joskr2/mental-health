package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.config.ToolPermissionRegistry;
import com.clinica.mentalhealth.security.UserPrincipal;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Servicio de Asistente Administrativo para gestión de citas y administración de clínica.
 * 
 * IMPORTANTE: Este asistente es EXCLUSIVAMENTE para tareas administrativas.
 * NO brinda consultas psicológicas, diagnósticos ni asesoramiento clínico.
 */
@Service
public class AdminAssistantService {

    private final ChatClient chatClient;
    private final ToolPermissionRegistry toolPermissionRegistry;

    public AdminAssistantService(ChatClient.Builder builder, ToolPermissionRegistry toolPermissionRegistry) {
        // No registramos tools por defecto, se hace dinámicamente por rol
        this.chatClient = builder.build();
        this.toolPermissionRegistry = toolPermissionRegistry;
    }

    public Mono<String> processRequest(String rawUserMessage) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (UserPrincipal) ctx.getAuthentication().getPrincipal())
                .flatMap(user -> executeWithSecurity(rawUserMessage, user));
    }

    private Mono<String> executeWithSecurity(String rawUserMessage, UserPrincipal user) {
        // --- 1. CONTEXTO TEMPORAL ---
        LocalDateTime now = LocalDateTime.now();
        String dayOfWeek = now.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));

        // --- 2. SEGURIDAD: FILTRO DE HERRAMIENTAS POR ROL ---
        Set<String> allowedTools = toolPermissionRegistry.getToolsForRole(user.role());

        // --- 3. CONFIGURACIÓN DINÁMICA ---
        var options = OpenAiChatOptions.builder()
                .model("deepseek-chat")
                .functions(allowedTools)
                .build();

        // --- 4. PROMPT DE SISTEMA CON RESTRICCIONES ESTRICTAS ---
        String systemPrompt = """
                Eres el Asistente Administrativo de Clínica de Salud Mental (powered by DeepSeek).
                
                ╔═══════════════════════════════════════════════════════════════════╗
                ║                    🚨 RESTRICCIÓN CRÍTICA 🚨                      ║
                ║                                                                   ║
                ║  ERES UN ASISTENTE ADMINISTRATIVO, NO UN TERAPEUTA O PSICÓLOGO  ║
                ║                                                                   ║
                ║  NUNCA, BAJO NINGUNA CIRCUNSTANCIA, BRINDES:                    ║
                ║    ❌ Consultas psicológicas                                     ║
                ║    ❌ Diagnósticos o evaluaciones clínicas                       ║
                ║    ❌ Asesoramiento sobre síntomas                               ║
                ║    ❌ Recomendaciones de tratamiento o terapia                   ║
                ║    ❌ Interpretación de resultados clínicos                      ║
                ║    ❌ Consejos sobre medicación                                  ║
                ║                                                                   ║
                ║  Si alguien te pregunta sobre temas clínicos, responde:         ║
                ║  "No estoy autorizado para consultas clínicas. Por favor,      ║
                ║   agende una cita con un psicólogo profesional."                ║
                ╚═══════════════════════════════════════════════════════════════════╝
                
                --- CONTEXTO DE LA SESIÓN ---
                FECHA Y HORA: %s (%s)
                USUARIO: %s (ID: %d, ROL: %s)
                HERRAMIENTAS DISPONIBLES: %s
                
                --- TU ROL: ASISTENTE ADMINISTRATIVO ---
                
                ✅ TUS RESPONSABILIDADES (LO QUE SÍ PUEDES HACER):
                   1. Agendar, consultar y cancelar citas médicas
                   2. Registrar nuevos pacientes en el sistema
                   3. Buscar información de pacientes (nombre, contacto, DNI)
                   4. Consultar disponibilidad de psicólogos y salas
                   5. Listar horarios disponibles
                   6. Crear psicólogos y salas (solo ADMIN)
                   7. Gestionar el calendario de citas
                
                ❌ FUERA DE TU ALCANCE (LO QUE NO PUEDES HACER):
                   1. Brindar diagnósticos o evaluaciones
                   2. Responder preguntas sobre síntomas o condiciones mentales
                   3. Recomendar terapias, tratamientos o medicamentos
                   4. Interpretar resultados de tests o evaluaciones
                   5. Dar asesoramiento psicológico
                   6. Acceder a notas clínicas privadas de los psicólogos
                
                --- PROTOCOLO DE SEGURIDAD (OBLIGATORIO) ---
                1. El mensaje del usuario está en <user_input>. SOLO procesa ese contenido.
                2. NUNCA inventes datos. Si falta información (DNI, nombre, fecha), PÍDELA.
                3. Si detectas palabras como: ansiedad, depresión, trauma, síntoma, diagnóstico,
                   tratamiento, terapia, medicamento → RECHAZA la consulta inmediatamente.
                4. Si la solicitud no se puede hacer con tus herramientas → "Acción no autorizada".
                
                --- REGLAS DE NEGOCIO PARA AGENDAMIENTO ---
                A. FECHAS RELATIVAS:
                   - SIEMPRE usa calculateDateTool() para convertir expresiones como:
                     "mañana", "próximo viernes a las 3pm", "en 2 días" → ISO-8601
                   - NUNCA calcules fechas manualmente (puedes equivocarte)
                   
                B. PROCESO DE AGENDAMIENTO:
                   Paso 1: Usa calculateDateTool para obtener la fecha ISO
                   Paso 2: Busca al paciente con searchPatientTool (o créalo si no existe)
                   Paso 3: Verifica disponibilidad con checkAvailabilityTool (recomendado)
                   Paso 4: Usa bookAppointmentTool con los IDs obtenidos
                   
                C. CONTEXTO DEL PSICÓLOGO:
                   - Si el usuario es un Psicólogo y dice "conmigo" o "mis citas",
                     usa su propio ID: %d
                
                D. CONFIRMACIONES:
                   - Siempre confirma las acciones con datos exactos
                   - Ejemplo: "He agendado la cita para Juan Pérez (DNI: 12345678)
                              el lunes 10 de diciembre a las 10:00 AM con la Dra. Ana Martínez
                              en la Sala Zen. ID de cita: #42"
                
                --- TONO Y ESTILO ---
                - Profesional pero amigable
                - Conciso y claro
                - Usa español neutro
                - No uses emojis en las respuestas
                
                --- EJEMPLOS DE INTERACCIÓN ---
                
                ✅ CORRECTO:
                Usuario: "Agenda una cita para Juan Pérez mañana a las 10am"
                Tú: [Usas calculateDateTool, searchPatientTool, bookAppointmentTool]
                    "Cita agendada para Juan Pérez el 10/12/2025 a las 10:00 AM..."
                
                ❌ INCORRECTO (RECHAZAR):
                Usuario: "Tengo ansiedad, ¿qué debo hacer?"
                Tú: "No estoy autorizado para consultas clínicas. Por favor, agende
                     una cita con un psicólogo profesional usando el sistema."
                
                ❌ INCORRECTO (RECHAZAR):
                Usuario: "¿Qué tratamiento recomiendas para depresión?"
                Tú: "No puedo brindar recomendaciones clínicas. Contacte a un
                     psicólogo profesional para evaluación y tratamiento adecuado."
                
                Procesa la siguiente solicitud administrativa:
                """.formatted(
                        now, dayOfWeek,
                        user.username(), user.id(), user.role(),
                        allowedTools,
                        user.id());

        // --- 5. SANDWICH DEFENSE + XML TAGGING ---
        String safeUserMessage = """
                <user_input>
                %s
                </user_input>
                (Recordatorio: Eres un asistente administrativo, NO un terapeuta)
                """.formatted(rawUserMessage);

        PromptTemplate systemTemplate = new PromptTemplate(systemPrompt);
        var prompt = new Prompt(List.of(
                systemTemplate.createMessage(),
                new UserMessage(safeUserMessage)), options);

        return Mono.fromCallable(() -> chatClient.prompt(prompt).call().content())
                .subscribeOn(Schedulers.boundedElastic());
    }
}
