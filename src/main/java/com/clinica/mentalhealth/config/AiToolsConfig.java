package com.clinica.mentalhealth.config;

import com.clinica.mentalhealth.ai.tools.*;
import com.clinica.mentalhealth.domain.Appointment;
import com.clinica.mentalhealth.domain.Patient;
import com.clinica.mentalhealth.domain.Psychologist;
import com.clinica.mentalhealth.domain.Role;
import com.clinica.mentalhealth.domain.Room;
import com.clinica.mentalhealth.service.AppointmentService;
import com.clinica.mentalhealth.service.DateCalculationService;
import com.clinica.mentalhealth.service.PatientService;
import com.clinica.mentalhealth.service.PsychologistService;
import com.clinica.mentalhealth.service.RoomService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

@Configuration
public class AiToolsConfig {

    private static final String ERROR_PREFIX = "ERROR: ";

    // --- HERRAMIENTA DE CÁLCULO DE FECHAS (Evita alucinación del LLM) ---

    @Bean
    @Description("Calcular fecha exacta a partir de descripción relativa. USA SIEMPRE ESTA HERRAMIENTA para convertir "
            +
            "expresiones como 'próximo lunes', 'mañana a las 4', 'en 3 días' a formato ISO. " +
            "NUNCA calcules fechas tú mismo, SIEMPRE usa esta herramienta.")
    @AllowedRoles({ Role.ROLE_ADMIN, Role.ROLE_PSYCHOLOGIST, Role.ROLE_PATIENT })
    public Function<DateCalculationRequest, DateCalculationResponse> calculateDateTool(DateCalculationService service) {
        return service::calculate;
    }

    // --- HERRAMIENTAS DE PACIENTES ---

    @Bean
    @Description("Buscar pacientes por nombre o por DNI.")
    @AllowedRoles({ Role.ROLE_ADMIN, Role.ROLE_PSYCHOLOGIST })
    public Function<PatientSearchRequest, List<Patient>> searchPatientTool(PatientService service) {
        return request -> service.searchPatient(request.name()).collectList().block();
    }

    @Bean
    @Description("Crear un nuevo paciente. Requiere Nombre, Email, Teléfono y DNI.")
    @AllowedRoles({ Role.ROLE_ADMIN, Role.ROLE_PSYCHOLOGIST })
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
    @Description("Reservar una cita médica. Requiere ID paciente, ID doctor y Fecha ISO (usa calculateDateTool primero para obtener la fecha).")
    @AllowedRoles({ Role.ROLE_ADMIN, Role.ROLE_PSYCHOLOGIST })
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

    @Bean
    @Description("Listar citas futuras según filtros opcionales (paciente, psicólogo, rango de fechas). " +
            "Si no se especifican filtros, retorna citas del usuario actual según su rol. " +
            "SOLO retorna citas desde HOY en adelante.")
    @AllowedRoles({ Role.ROLE_ADMIN, Role.ROLE_PSYCHOLOGIST, Role.ROLE_PATIENT })
    public Function<ListAppointmentsRequest, List<Appointment>> listAppointmentsTool(AppointmentService service) {
        return request -> {
            try {
                LocalDateTime start = request.startDate() != null 
                    ? LocalDateTime.parse(request.startDate()) : null;
                LocalDateTime end = request.endDate() != null 
                    ? LocalDateTime.parse(request.endDate()) : null;
                
                return service.getFutureAppointments(
                    request.patientId(), 
                    request.psychologistId(), 
                    start, 
                    end
                ).collectList().block();
            } catch (Exception e) {
                return List.of(); // Retornar lista vacía en caso de error
            }
        };
    }

    @Bean
    @Description("Verificar disponibilidad de horarios para un psicólogo en una fecha específica. " +
            "Retorna lista de horarios 100% LIBRES en formato 'HH:mm' (ej: ['09:00', '10:00', '14:00']). " +
            "Solo muestra horarios completamente disponibles, sin conflictos.")
    @AllowedRoles({ Role.ROLE_ADMIN, Role.ROLE_PSYCHOLOGIST })
    public Function<CheckAvailabilityRequest, List<String>> checkAvailabilityTool(AppointmentService service) {
        return request -> {
            try {
                LocalDate date = LocalDate.parse(request.date().substring(0, 10));
                return service.getAvailableSlots(request.psychologistId(), date)
                             .collectList()
                             .block();
            } catch (Exception e) {
                return List.of(); // Retornar lista vacía en caso de error
            }
        };
    }

    @Bean
    @Description("Cancelar una cita existente. Requiere el ID de la cita. " +
            "Admin puede cancelar cualquier cita, Psicólogos solo sus propias citas.")
    @AllowedRoles({ Role.ROLE_ADMIN, Role.ROLE_PSYCHOLOGIST })
    public Function<CancelAppointmentRequest, String> cancelAppointmentTool(AppointmentService service) {
        return request -> {
            try {
                service.cancelAppointment(request.appointmentId()).block();
                return "ÉXITO: Cita #" + request.appointmentId() + " cancelada correctamente.";
            } catch (Exception e) {
                return ERROR_PREFIX + e.getMessage();
            }
        };
    }

    // --- HERRAMIENTAS DE PSICÓLOGOS ---

    @Bean
    @Description("Listar todos los psicólogos registrados con su especialidad y datos de contacto.")
    @AllowedRoles({ Role.ROLE_ADMIN, Role.ROLE_PSYCHOLOGIST, Role.ROLE_PATIENT })
    public Function<EmptyRequest, List<Psychologist>> listPsychologistsTool(PsychologistService service) {
        return request -> service.findAllCached().block();
    }

    // --- HERRAMIENTAS DE INFRAESTRUCTURA/STAFF (SOLO ADMIN) ---

    @Bean
    @Description("Contratar un nuevo Psicólogo. Requiere nombre, especialidad, email, teléfono, DNI y username. La contraseña se genera automáticamente.")
    @AllowedRoles({ Role.ROLE_ADMIN })
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
    @AllowedRoles({ Role.ROLE_ADMIN })
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
    @Description("Listar todas las salas/consultorios disponibles.")
    @AllowedRoles({ Role.ROLE_ADMIN, Role.ROLE_PSYCHOLOGIST })
    public Function<EmptyRequest, List<Room>> listRoomsTool(RoomService service) {
        // Usar versión cacheable que retorna Mono<List<Room>>
        return request -> service.findAllCached().block();
    }
}
