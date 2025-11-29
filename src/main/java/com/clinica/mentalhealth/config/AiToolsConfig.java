package com.clinica.mentalhealth.config;

import com.clinica.mentalhealth.ai.tools.*;
import com.clinica.mentalhealth.domain.Patient;
import com.clinica.mentalhealth.domain.Room;
import com.clinica.mentalhealth.service.AppointmentService;
import com.clinica.mentalhealth.service.PatientService;
import com.clinica.mentalhealth.service.PsychologistService;
import com.clinica.mentalhealth.service.RoomService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;

@Configuration
public class AiToolsConfig {

    private static final String ERROR_PREFIX = "ERROR: ";

    // --- HERRAMIENTAS DE PACIENTES ---

    @Bean
    @Description("Buscar pacientes por nombre o por DNI.")
    public Function<PatientSearchRequest, List<Patient>> searchPatientTool(PatientService service) {
        return request -> service.searchPatient(request.name()).collectList().block();
    }

    @Bean
    @Description("Crear un nuevo paciente. Requiere Nombre, Email, Teléfono y DNI.")
    public Function<CreatePatientRequest, String> createPatientTool(PatientService service) {
        return request -> {
            try {
                Patient p = service.createPatient(
                        request.name(), request.email(), request.phone(), request.dni()).block();
                return "ÉXITO: Paciente creado con ID " + p.id();
            } catch (Exception e) {
                return ERROR_PREFIX + e.getMessage();
            }
        };
    }

    // --- HERRAMIENTAS DE CITAS ---

    @Bean
    @Description("Reservar una cita médica. Requiere ID paciente, ID doctor y Fecha ISO.")
    public Function<BookingRequest, String> bookAppointmentTool(AppointmentService service) {
        return request -> {
            try {
                var cita = service.createFromAi(
                        request.patientId(),
                        request.psychologistId(),
                        request.startTime()).block();
                return "ÉXITO: Cita agendada con ID " + cita.id();
            } catch (Exception e) {
                return "ERROR AL AGENDAR: " + e.getMessage();
            }
        };
    }

    // --- HERRAMIENTAS DE INFRAESTRUCTURA/STAFF (SOLO ADMIN) ---

    @Bean
    @Description("Contratar un nuevo Psicólogo. Requiere nombre, especialidad, email, teléfono, DNI y username. La contraseña se genera automáticamente.")
    public Function<CreatePsychologistRequest, String> createPsychologistTool(PsychologistService service) {
        return request -> {
            try {
                // Generar contraseña temporal segura (no viene del LLM por seguridad)
                String tempPassword = generateSecureTemporaryPassword();
                
                var doc = service.createPsychologist(
                        request.name(),
                        request.specialty(),
                        request.email(),
                        request.phone(),
                        request.dni(),
                        request.username(),
                        tempPassword).block();
                
                // Retornar la contraseña temporal para que el admin la comunique
                return String.format(
                    "ÉXITO: Psicólogo '%s' creado con ID %d. " +
                    "IMPORTANTE: Contraseña temporal: %s (debe cambiarla en el primer inicio de sesión)",
                    request.name(), doc.id(), tempPassword);
            } catch (Exception e) {
                return ERROR_PREFIX + e.getMessage();
            }
        };
    }
    
    /**
     * Genera una contraseña temporal segura.
     * En producción, considerar usar SecureRandom con más entropía.
     */
    private String generateSecureTemporaryPassword() {
        // Caracteres permitidos (evitando ambiguos como 0/O, 1/l)
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder password = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    @Bean
    @Description("Crear una nueva Sala/Consultorio. Requiere el nombre de la sala.")
    public Function<CreateRoomRequest, String> createRoomTool(RoomService service) {
        return request -> {
            try {
                var room = service.createRoom(request.name()).block();
                return "ÉXITO: Sala creada con ID " + room.id();
            } catch (Exception e) {
                return ERROR_PREFIX + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Listar todas las salas disponibles.")
    public Function<EmptyRequest, List<Room>> listRoomsTool(RoomService service) {
        return request -> service.findAll().collectList().block();
    }
}
