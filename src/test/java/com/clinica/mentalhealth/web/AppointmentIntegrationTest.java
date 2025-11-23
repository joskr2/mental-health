package com.clinica.mentalhealth.web;

import com.clinica.mentalhealth.domain.*;
import com.clinica.mentalhealth.repository.*;
import com.clinica.mentalhealth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.time.LocalDateTime;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AppointmentIntegrationTest {

    @LocalServerPort
    private int port;

    // Cliente manual (sin @Autowired para evitar conflictos)
    private WebTestClient webTestClient;

    @Autowired private DatabaseClient databaseClient; // SQL Nativo
    @Autowired private UserRepository userRepo;
    @Autowired private AppointmentRepository appointmentRepo;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder encoder;

    private String patientToken;
    private Long patientId;
    private Long psychologistId;
    private Long roomId;

    @BeforeEach
    void setUp() {
        // 1. Configurar el Cliente HTTP manualmente contra el puerto real
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(30))
                .build();

        // 2. Limpiar BD usando SQL Nativo con COMILLAS
        // H2 en modo Postgres es Case Sensitive si se usaron comillas al crear tablas.
        appointmentRepo.deleteAll().block(); // Borrado reactivo seguro

        // Borrado manual de tablas maestras para evitar errores de FK
        runSql("DELETE FROM \"appointments\"");
        runSql("DELETE FROM \"users\"");
        runSql("DELETE FROM \"patients\"");
        runSql("DELETE FROM \"psychologists\"");
        runSql("DELETE FROM \"rooms\"");

        // 3. Crear Usuarios (Identity)
        var uPat = new User(null, "pepe", encoder.encode("123"), Role.ROLE_PATIENT);
        var uDoc = new User(null, "doc", encoder.encode("123"), Role.ROLE_PSYCHOLOGIST);

        // Guardamos y bloqueamos para obtener los IDs generados
        var savedPatUser = userRepo.save(uPat).block();
        var savedDocUser = userRepo.save(uDoc).block();

        this.patientId = savedPatUser.id();
        this.psychologistId = savedDocUser.id();

        // 4. Insertar Entidades de Negocio sincronizadas con los Usuarios
        // Usamos SQL nativo para forzar el ID (para que coincida con el User ID)

        // Insertar Paciente (ID = User ID)
        databaseClient.sql("INSERT INTO \"patients\" (id, name, email) VALUES (:id, :name, :email)")
                .bind("id", patientId)
                .bind("name", "Pepe")
                .bind("email", "pepe@test.com")
                .fetch().rowsUpdated().block();

        // Insertar Psicólogo (ID = User ID)
        databaseClient.sql("INSERT INTO \"psychologists\" (id, name, specialty) VALUES (:id, :name, :spec)")
                .bind("id", psychologistId)
                .bind("name", "Doc Strange")
                .bind("spec", "Misticismo")
                .fetch().rowsUpdated().block();

        // Insertar Sala
        this.roomId = 1L;
        databaseClient.sql("INSERT INTO \"rooms\" (id, name) VALUES (:id, :name)")
                .bind("id", roomId)
                .bind("name", "Sala X")
                .fetch().rowsUpdated().block();

        // 5. Generar Token válido
        this.patientToken = jwtService.generateToken(savedPatUser);
    }

    // Método auxiliar para ejecutar SQL sin repetir código
    private void runSql(String sql) {
        databaseClient.sql(sql).fetch().rowsUpdated().block();
    }

    @Test
    void createAppointment_HappyPath() {
        // Fecha futura (Lunes 10 AM)
        var start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        if (start.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) start = start.plusDays(1);

        var cita = new Appointment(null, start, start.plusHours(1), patientId, psychologistId, roomId);

        webTestClient.post()
                .uri("/api/appointments")
                .header("Authorization", "Bearer " + patientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cita)
                .exchange()
                .expectStatus().isCreated() // 201 Created
                .expectBody()
                .jsonPath("$.patientId").isEqualTo(patientId);
    }

    @Test
    void createAppointment_Conflict() {
        // Fecha futura
        var start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        if (start.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) start = start.plusDays(1);

        var cita = new Appointment(null, start, start.plusHours(1), patientId, psychologistId, roomId);

        // 1. Insertamos la primera cita directo en BD (para simular que ya existe)
        // OJO: R2DBC save a veces falla si el ID es null en entidades nuevas manuales,
        // mejor insertamos vía Repository normal ya que el repository maneja eso.
        appointmentRepo.save(cita).block();

        // 2. Intentamos crear la misma cita vía API
        webTestClient.post()
                .uri("/api/appointments")
                .header("Authorization", "Bearer " + patientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cita)
                .exchange()
                .expectStatus().isEqualTo(409) // Esperamos 409 Conflict
                .expectBody()
                .jsonPath("$.error").isEqualTo("Conflict");
    }
}