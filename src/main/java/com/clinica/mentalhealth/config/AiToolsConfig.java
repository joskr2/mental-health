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
                        request.name(), request.email(), request.phone(), request.dni()
                ).block();
                return "ÉXITO: Paciente creado con ID " + p.id();
            } catch (Exception e) {
                return "ERROR: " + e.getMessage();
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
                        request.startTime()
                ).block();
                return "ÉXITO: Cita agendada con ID " + cita.id();
            } catch (Exception e) {
                return "ERROR AL AGENDAR: " + e.getMessage();
            }
        };
    }

    // --- HERRAMIENTAS DE INFRAESTRUCTURA/STAFF (SOLO ADMIN) ---

    @Bean
    @Description("Contratar un nuevo Psicólogo. Requiere nombre, especialidad, email, teléfono, DNI, username y password.")
    public Function<CreatePsychologistRequest, String> createPsychologistTool(PsychologistService service) {
        return request -> {
            try {
                var doc = service.createPsychologist(
                        request.name(),
                        request.specialty(),
                        request.email(),
                        request.phone(),
                        request.dni(),
                        request.username(),
                        request.password()
                ).block();
                return "ÉXITO: Psicólogo creado con ID " + doc.id();
            } catch (Exception e) {
                return "ERROR: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Crear una nueva Sala/Consultorio. Requiere el nombre de la sala.")
    public Function<CreateRoomRequest, String> createRoomTool(RoomService service) {
        return request -> {
            try {
                var room = service.createRoom(request.name()).block();
                return "ÉXITO: Sala creada con ID " + room.id();
            } catch (Exception e) {
                return "ERROR: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Listar todas las salas disponibles.")
    public Function<EmptyRequest, List<Room>> listRoomsTool(RoomService service) {
        return request -> service.findAll().collectList().block();
    }
}
