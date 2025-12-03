package com.clinica.mentalhealth.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.clinica.mentalhealth.domain.Patient;
import com.clinica.mentalhealth.domain.Role;
import com.clinica.mentalhealth.domain.User;
import com.clinica.mentalhealth.repository.PatientRepository;
import com.clinica.mentalhealth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.FetchSpec;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests para PatientService.
 *
 * Usa Mockito para simular las dependencias y StepVerifier
 * para verificar los flujos reactivos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PatientService Tests")
class PatientServiceTest {

  @Mock
  private PatientRepository patientRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private DatabaseClient databaseClient;

  @Mock
  private DatabaseClient.GenericExecuteSpec executeSpec;

  @Mock
  private FetchSpec<Object> fetchSpec;

  @InjectMocks
  private PatientService patientService;

  private Patient testPatient;
  private User testUser;

  @BeforeEach
  void setUp() {
    testPatient = new Patient(
      1L,
      "Juan Pérez",
      "juan@test.com",
      "+51999888777",
      "12345678"
    );
    testUser = new User(
      1L,
      "juan@test.com",
      "encodedPassword",
      Role.ROLE_PATIENT
    );
  }

  @Nested
  @DisplayName("findAll()")
  class FindAllTests {

    @Test
    @DisplayName("Debe retornar todos los pacientes")
    void shouldReturnAllPatients() {
      // Arrange
      Patient patient2 = new Patient(
        2L,
        "María García",
        "maria@test.com",
        "+51999777666",
        "87654321"
      );
      when(patientRepository.findAll()).thenReturn(
        Flux.just(testPatient, patient2)
      );

      // Act & Assert
      StepVerifier.create(patientService.findAll())
        .expectNext(testPatient)
        .expectNext(patient2)
        .verifyComplete();

      verify(patientRepository).findAll();
    }

    @Test
    @DisplayName("Debe retornar Flux vacío cuando no hay pacientes")
    void shouldReturnEmptyFluxWhenNoPatients() {
      // Arrange
      when(patientRepository.findAll()).thenReturn(Flux.empty());

      // Act & Assert
      StepVerifier.create(patientService.findAll()).verifyComplete();

      verify(patientRepository).findAll();
    }
  }

  @Nested
  @DisplayName("findById()")
  class FindByIdTests {

    @Test
    @DisplayName("Debe retornar paciente cuando existe")
    void shouldReturnPatientWhenExists() {
      // Arrange
      when(patientRepository.findById(1L)).thenReturn(Mono.just(testPatient));

      // Act & Assert
      StepVerifier.create(patientService.findById(1L))
        .expectNext(testPatient)
        .verifyComplete();

      verify(patientRepository).findById(1L);
    }

    @Test
    @DisplayName("Debe lanzar 404 cuando paciente no existe")
    void shouldThrow404WhenPatientNotFound() {
      // Arrange
      when(patientRepository.findById(999L)).thenReturn(Mono.empty());

      // Act & Assert
      StepVerifier.create(patientService.findById(999L))
        .expectErrorMatches(
          throwable ->
            throwable instanceof ResponseStatusException &&
            ((ResponseStatusException) throwable).getStatusCode() ==
            HttpStatus.NOT_FOUND &&
            throwable.getMessage().contains("Paciente no encontrado")
        )
        .verify();

      verify(patientRepository).findById(999L);
    }
  }

  @Nested
  @DisplayName("searchPatient()")
  class SearchPatientTests {

    @Test
    @DisplayName("Debe buscar por DNI cuando query son solo números")
    void shouldSearchByDniWhenQueryIsNumeric() {
      // Arrange
      when(patientRepository.findByDni("12345678")).thenReturn(
        Mono.just(testPatient)
      );

      // Act & Assert
      StepVerifier.create(patientService.searchPatient("12345678"))
        .expectNext(testPatient)
        .verifyComplete();

      verify(patientRepository).findByDni("12345678");
      verify(patientRepository, never()).findByNameLike(anyString());
    }

    @Test
    @DisplayName("Debe buscar por nombre (fuzzy) cuando query tiene letras")
    void shouldSearchByNameWhenQueryHasLetters() {
      // Arrange
      Patient patient2 = new Patient(
        2L,
        "Juanito Pérez",
        "juanito@test.com",
        "+51999666555",
        "11223344"
      );
      when(patientRepository.findByNameLike("Juan")).thenReturn(
        Flux.just(testPatient, patient2)
      );

      // Act & Assert
      StepVerifier.create(patientService.searchPatient("Juan"))
        .expectNext(testPatient)
        .expectNext(patient2)
        .verifyComplete();

      verify(patientRepository).findByNameLike("Juan");
      verify(patientRepository, never()).findByDni(anyString());
    }

    @Test
    @DisplayName("Debe retornar vacío cuando DNI no existe")
    void shouldReturnEmptyWhenDniNotFound() {
      // Arrange
      when(patientRepository.findByDni("99999999")).thenReturn(Mono.empty());

      // Act & Assert
      StepVerifier.create(
        patientService.searchPatient("99999999")
      ).verifyComplete();
    }

    @Test
    @DisplayName("Debe buscar por nombre con números mixtos")
    void shouldSearchByNameWhenQueryHasMixedContent() {
      // Arrange - Query con letras y números se trata como nombre
      when(patientRepository.findByNameLike("Juan123")).thenReturn(
        Flux.empty()
      );

      // Act & Assert
      StepVerifier.create(
        patientService.searchPatient("Juan123")
      ).verifyComplete();

      verify(patientRepository).findByNameLike("Juan123");
    }
  }

  @Nested
  @DisplayName("createPatient()")
  class CreatePatientTests {

    @Test
    @DisplayName("Debe fallar cuando DNI ya existe")
    void shouldFailWhenDniAlreadyExists() {
      // Arrange - Cuando el paciente ya existe, findByDni retorna el paciente
      // El switchIfEmpty necesita que userRepository.save esté mockeado aunque no se llame
      // debido a cómo funciona la evaluación de operadores en Reactor
      when(patientRepository.findByDni("12345678")).thenReturn(
        Mono.just(testPatient)
      );
      when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
      when(userRepository.save(any(User.class))).thenReturn(
        Mono.just(testUser)
      );

      // Act & Assert
      StepVerifier.create(
        patientService.createPatient(
          "Nuevo Paciente",
          "nuevo@test.com",
          "+51999111222",
          "12345678"
        )
      )
        .expectErrorMatches(
          throwable ->
            throwable instanceof IllegalArgumentException &&
            throwable
              .getMessage()
              .contains("Ya existe un paciente con DNI 12345678")
        )
        .verify();

      verify(patientRepository).findByDni("12345678");
    }

    @Test
    @DisplayName("Debe crear paciente exitosamente con nuevo DNI")
    @SuppressWarnings("unchecked")
    void shouldCreatePatientSuccessfully() {
      // Arrange
      String name = "Nuevo Paciente";
      String email = "nuevo@test.com";
      String phone = "+51999111222";
      String dni = "99998888";

      when(patientRepository.findByDni(dni)).thenReturn(Mono.empty());
      when(passwordEncoder.encode("123")).thenReturn("encodedPassword");

      User newUser = new User(5L, email, "encodedPassword", Role.ROLE_PATIENT);
      when(userRepository.save(any(User.class))).thenReturn(Mono.just(newUser));

      // Mock DatabaseClient chain
      when(databaseClient.sql(anyString())).thenReturn(executeSpec);
      when(executeSpec.bind(anyString(), any())).thenReturn(executeSpec);
      when(executeSpec.fetch()).thenReturn((FetchSpec) fetchSpec);
      when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(1L));

      // Act & Assert
      StepVerifier.create(patientService.createPatient(name, email, phone, dni))
        .expectNextMatches(
          patient ->
            patient.id().equals(5L) &&
            patient.name().equals(name) &&
            patient.email().equals(email) &&
            patient.dni().equals(dni)
        )
        .verifyComplete();

      verify(patientRepository).findByDni(dni);
      verify(userRepository).save(any(User.class));
    }
  }

  @Nested
  @DisplayName("deletePatient()")
  class DeletePatientTests {

    @Test
    @DisplayName("Debe eliminar paciente existente")
    void shouldDeleteExistingPatient() {
      // Arrange
      when(patientRepository.findById(1L)).thenReturn(Mono.just(testPatient));
      when(patientRepository.deleteById(1L)).thenReturn(Mono.empty());
      when(userRepository.deleteById(1L)).thenReturn(Mono.empty());

      // Act & Assert
      StepVerifier.create(patientService.deletePatient(1L)).verifyComplete();

      verify(patientRepository).findById(1L);
      verify(patientRepository).deleteById(1L);
      verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Debe lanzar 404 al eliminar paciente inexistente")
    void shouldThrow404WhenDeletingNonExistentPatient() {
      // Arrange
      when(patientRepository.findById(999L)).thenReturn(Mono.empty());

      // Act & Assert
      StepVerifier.create(patientService.deletePatient(999L))
        .expectErrorMatches(
          throwable ->
            throwable instanceof ResponseStatusException &&
            ((ResponseStatusException) throwable).getStatusCode() ==
            HttpStatus.NOT_FOUND
        )
        .verify();

      verify(patientRepository).findById(999L);
      verify(patientRepository, never()).deleteById(anyLong());
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCaseTests {

    @Test
    @DisplayName("findById con null debe fallar")
    void findByIdWithNullShouldFail() {
      // Arrange - Mock para evitar NPE del mock no configurado
      when(patientRepository.findById((Long) null)).thenReturn(Mono.empty());

      // Act & Assert - Puede lanzar NPE o ResponseStatusException dependiendo de la implementación
      StepVerifier.create(patientService.findById(null)).expectError().verify();
    }

    @Test
    @DisplayName("searchPatient con query vacío debe retornar vacío")
    void searchWithEmptyQueryShouldReturnEmpty() {
      // Arrange
      when(patientRepository.findByNameLike("")).thenReturn(Flux.empty());

      // Act & Assert
      StepVerifier.create(patientService.searchPatient("")).verifyComplete();
    }

    @Test
    @DisplayName("searchPatient con espacios solo números")
    void searchWithSpacesAndNumbers() {
      // Arrange - "123 456" tiene espacios, así que no es solo números
      when(patientRepository.findByNameLike("123 456")).thenReturn(
        Flux.empty()
      );

      // Act & Assert
      StepVerifier.create(
        patientService.searchPatient("123 456")
      ).verifyComplete();

      verify(patientRepository).findByNameLike("123 456");
    }
  }
}
