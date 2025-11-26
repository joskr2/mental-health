package com.clinica.mentalhealth.config;

import com.clinica.mentalhealth.ai.tools.*;
import com.clinica.mentalhealth.domain.Patient;
import com.clinica.mentalhealth.service.AppointmentService;
import com.clinica.mentalhealth.service.PatientService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;

@Configuration
public class AiToolsConfig {

    @Bean
    @Description("Buscar pacientes por Nombre o por DNI. Retorna la lista de coincidencias.")
    public Function<PatientSearchRequest, List<Patient>> searchPatientTool(PatientService patientService) {
        // La IA pasará el nombre o el número en el campo "name"
        return request -> patientService.searchPatient(request.name())
                .collectList()
                .block();
    }

    @Bean
    @Description("Crear un nuevo paciente. Requiere Nombre, Email, Teléfono y DNI (Documento de Identidad).")
    public Function<CreatePatientRequest, Patient> createPatientTool(PatientService patientService) {
        return request -> patientService.createPatient(request.name(), request.email(), request.phone(), request.dni())
                .block();
    }

    @Bean
    @Description("Reservar una cita médica. Requiere ID paciente, ID doctor y Fecha ISO.")
    public Function<BookingRequest, String> bookAppointmentTool(AppointmentService appointmentService) {
        return request -> {
            try {
                var cita = appointmentService.createFromAi(
                        request.patientId(),
                        request.psychologistId(),
                        request.startTime()).block();

                return "ÉXITO: Cita creada correctamente con ID " + cita.id();
            } catch (Exception e) {
                return "ERROR AL AGENDAR: " + e.getMessage();
            }
        };
    }
}
