package com.clinica.mentalhealth.repository;

import com.clinica.mentalhealth.domain.Appointment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

/**
 * Test de integración para AppointmentRepository.
 * 
 * Usa Testcontainers para levantar un PostgreSQL real,
 * verificando que las queries SQL y los índices funcionan correctamente.
 * 
 * A diferencia de los tests con Mockito, estos tests detectan:
 * - Errores de sintaxis SQL
 * - Problemas con índices
 * - Incompatibilidades de tipos de datos
 * - Comportamiento real de las queries (nulls, ordenamiento, etc.)
 * 
 * Requiere Docker para ejecutarse. Ejecutar con:
 * ./mvnw test -Dtest=AppointmentRepositoryIntegrationTest
 * 
 * Para CI/CD, asegúrate de que Docker esté disponible o excluye este test.
 */
@DataR2dbcTest
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class AppointmentRepositoryIntegrationTest {

  // El ciclo de vida del contenedor es gestionado por @Container +
  // @Testcontainers
  @Container
  @SuppressWarnings("resource") // Testcontainers gestiona el cierre automáticamente
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("test_mental_clinic")
      .withUsername("test")
      .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // Convertir URL JDBC a R2DBC
    String jdbcUrl = postgres.getJdbcUrl();
    String r2dbcUrl = jdbcUrl.replace("jdbc:", "r2dbc:");

    registry.add("spring.r2dbc.url", () -> r2dbcUrl);
    registry.add("spring.r2dbc.username", postgres::getUsername);
    registry.add("spring.r2dbc.password", postgres::getPassword);
    registry.add("spring.sql.init.mode", () -> "always");
  }

  @Autowired
  private AppointmentRepository appointmentRepository;

  @Autowired
  private DatabaseClient databaseClient;

  /**
   * Helper para insertar datos de prueba necesarios para cumplir con las FKs.
   * En R2DBC no podemos usar @Sql, así que usamos DatabaseClient.
   */
  private void insertTestData() {
    // Insertar usuario para paciente
    databaseClient.sql("""
        INSERT INTO "users" (id, username, password, role)
        VALUES (1, 'patient_test', 'hashedpassword', 'PATIENT')
        ON CONFLICT (id) DO NOTHING
        """)
        .then()
        .block();

    // Insertar usuario para psicólogo
    databaseClient.sql("""
        INSERT INTO "users" (id, username, password, role)
        VALUES (2, 'psychologist_test', 'hashedpassword', 'PSYCHOLOGIST')
        ON CONFLICT (id) DO NOTHING
        """)
        .then()
        .block();

    // Insertar paciente
    databaseClient.sql("""
        INSERT INTO "patients" (id, name, email, phone, dni)
        VALUES (1, 'Juan García Test', 'juan@test.com', '+51999888777', '12345678')
        ON CONFLICT (id) DO NOTHING
        """)
        .then()
        .block();

    // Insertar psicólogo
    databaseClient.sql("""
        INSERT INTO "psychologists" (id, name, specialty, email, phone, dni)
        VALUES (2, 'Dra. María López', 'Psicología Clínica', 'maria@test.com', '+51999777666', '87654321')
        ON CONFLICT (id) DO NOTHING
        """)
        .then()
        .block();

    // Insertar sala
    databaseClient.sql("""
        INSERT INTO "rooms" (id, name)
        VALUES (1, 'Consultorio Test 1')
        ON CONFLICT (id) DO NOTHING
        """)
        .then()
        .block();
  }

  @Test
  void findPsychologistConflicts_ShouldReturnEmpty_WhenNoConflicts() {
    // Arrange
    var start = LocalDateTime.of(2025, 12, 1, 10, 0);
    var end = LocalDateTime.of(2025, 12, 1, 11, 0);
    Long psychologistId = 999L; // ID inexistente

    // Act & Assert
    StepVerifier.create(appointmentRepository.findPsychologistConflicts(psychologistId, start, end))
        .expectNextCount(0) // Sin conflictos porque no hay citas
        .verifyComplete();
  }

  @Test
  void findRoomConflicts_ShouldReturnEmpty_WhenNoConflicts() {
    // Arrange
    var start = LocalDateTime.of(2025, 12, 1, 14, 0);
    var end = LocalDateTime.of(2025, 12, 1, 15, 0);
    Long roomId = 999L;

    // Act & Assert
    StepVerifier.create(appointmentRepository.findRoomConflicts(roomId, start, end))
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  void findPatientConflicts_ShouldReturnEmpty_WhenNoConflicts() {
    // Arrange
    var start = LocalDateTime.of(2025, 12, 1, 16, 0);
    var end = LocalDateTime.of(2025, 12, 1, 17, 0);
    Long patientId = 999L;

    // Act & Assert
    StepVerifier.create(appointmentRepository.findPatientConflicts(patientId, start, end))
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  void save_ShouldPersistAppointment() {
    // Arrange - Insertar datos padre necesarios para cumplir con las FKs
    insertTestData();

    var appointment = new Appointment(
        null,
        LocalDateTime.of(2025, 12, 15, 10, 0),
        LocalDateTime.of(2025, 12, 15, 11, 0),
        1L, // patientId - insertado por insertTestData()
        2L, // psychologistId - insertado por insertTestData()
        1L // roomId - insertado por insertTestData()
    );

    // Act & Assert
    StepVerifier.create(appointmentRepository.save(appointment))
        .expectNextMatches(saved -> saved.id() != null)
        .verifyComplete();
  }
}
