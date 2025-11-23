package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.domain.Appointment;
import com.clinica.mentalhealth.repository.AppointmentRepository;
import com.clinica.mentalhealth.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class) // Usamos Mockito puro, sin Spring Context (¡Rapidísimo!)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void createAppointment_ShouldFail_WhenDateIsSunday() {
        // 1. Datos de prueba: Un Domingo cualquiera (Ej. 23 Nov 2025 es Domingo)
        var sunday = LocalDateTime.of(2025, 11, 23, 10, 0);

        var cita = new Appointment(null, sunday, sunday.plusHours(1), 3L, 2L, 1L);

        // 2. Simulamos el Contexto de Seguridad (Usuario Paciente ID 3)
        var principal = new UserPrincipal(3L, "pepe", "ROLE_PATIENT");
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));

        // 3. Ejecución
        // Como el servicio lee el contexto reactivo, debemos inyectarlo con .contextWrite
        Mono<Appointment> resultado = appointmentService.createAppointment(cita)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

        // 4. Verificación con StepVerifier
        StepVerifier.create(resultado)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("Cerrado los domingos.")
                )
                .verify(); // ¡Importante! Sin .verify(), nada se ejecuta.
    }
}