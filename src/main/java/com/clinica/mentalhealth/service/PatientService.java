package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.domain.Patient;
import com.clinica.mentalhealth.domain.Role;
import com.clinica.mentalhealth.domain.User;
import com.clinica.mentalhealth.repository.PatientRepository;
import com.clinica.mentalhealth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientService {

    private static final String PATIENT_NOT_FOUND = "Paciente no encontrado con ID: ";
    private static final String BIND_EMAIL = "email";

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DatabaseClient databaseClient;

    /**
     * Obtiene todos los pacientes.
     */
    public Flux<Patient> findAll() {
        log.debug("Buscando todos los pacientes");
        return patientRepository.findAll();
    }

    /**
     * Obtiene un paciente por su ID.
     */
    public Mono<Patient> findById(@NonNull Long id) {
        log.debug("Buscando paciente con ID: {}", id);
        return patientRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, PATIENT_NOT_FOUND + id)));
    }

    /**
     * Búsqueda Híbrida:
     * - Si 'query' son solo números -> Busca por DNI.
     * - Si tiene letras -> Busca por Nombre.
     */
    public Flux<Patient> searchPatient(String query) {
        log.debug("Búsqueda híbrida de paciente con query: {}", query);
        if (query.matches("\\d+")) {
            return patientRepository.findByDni(query).flux();
        } else {
            return patientRepository.findByNameLike(query);
        }
    }

    @Transactional
    public Mono<Patient> createPatient(String name, String email, String dni) {
        log.info("Creando paciente: name={}, email={}, dni={}", name, email, dni);
        return patientRepository.findByDni(dni)
                .flatMap(existing -> Mono
                        .<Patient>error(new IllegalArgumentException("Ya existe un paciente con DNI " + dni)))
                .switchIfEmpty(
                        userRepository.save(new User(null, email, passwordEncoder.encode("123"), Role.ROLE_PATIENT))
                                .flatMap(savedUser -> {
                                    String sql = "INSERT INTO \"patients\" (id, name, email, dni) VALUES (:id, :name, :email, :dni)";

                                    return databaseClient.sql(sql)
                                            .bind("id", Objects.requireNonNull(savedUser.id()))
                                            .bind("name", Objects.requireNonNull(name))
                                            .bind(BIND_EMAIL, Objects.requireNonNull(email))
                                            .bind("dni", Objects.requireNonNull(dni))
                                            .fetch()
                                            .rowsUpdated()
                                            .doOnSuccess(rows -> log.info("Paciente creado exitosamente con ID: {}",
                                                    savedUser.id()))
                                            .thenReturn(new Patient(savedUser.id(), name, email, dni));
                                }));
    }

    @Transactional
    public Mono<Patient> updatePatient(@NonNull Long id, String name, String email, String dni) {
        log.info("Actualizando paciente con ID: {}", id);
        return patientRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, PATIENT_NOT_FOUND + id)))
                .flatMap(existing -> {
                    // Verificar si el nuevo DNI ya existe en otro paciente
                    if (!existing.dni().equals(dni)) {
                        return patientRepository.findByDni(dni)
                                .flatMap(other -> Mono.<Patient>error(
                                        new IllegalArgumentException("Ya existe otro paciente con DNI " + dni)))
                                .switchIfEmpty(performUpdate(id, name, email, dni));
                    }
                    return performUpdate(id, name, email, dni);
                });
    }

    private Mono<Patient> performUpdate(@NonNull Long id, String name, String email, String dni) {
        String sql = "UPDATE \"patients\" SET name = :name, email = :email, dni = :dni WHERE id = :id";
        return databaseClient.sql(sql)
                .bind("id", id)
                .bind("name", Objects.requireNonNull(name))
                .bind(BIND_EMAIL, Objects.requireNonNull(email))
                .bind("dni", Objects.requireNonNull(dni))
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> {
                    // También actualizar el email en la tabla users
                    String updateUserSql = "UPDATE \"users\" SET username = :email WHERE id = :id";
                    return databaseClient.sql(updateUserSql)
                            .bind("id", id)
                            .bind(BIND_EMAIL, email)
                            .fetch()
                            .rowsUpdated();
                })
                .doOnSuccess(rows -> log.info("Paciente actualizado exitosamente con ID: {}", id))
                .thenReturn(new Patient(id, name, email, dni));
    }

    @Transactional
    public Mono<Void> deletePatient(@NonNull Long id) {
        log.info("Eliminando paciente con ID: {}", id);
        return patientRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, PATIENT_NOT_FOUND + id)))
                .flatMap(existing -> patientRepository.deleteById(id)
                        .then(userRepository.deleteById(id))
                        .doOnSuccess(v -> log.info("Paciente eliminado exitosamente con ID: {}", id)));
    }
}
