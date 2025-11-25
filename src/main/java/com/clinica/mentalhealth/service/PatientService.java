package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.domain.Patient;
import com.clinica.mentalhealth.domain.Role;
import com.clinica.mentalhealth.domain.User;
import com.clinica.mentalhealth.repository.PatientRepository;
import com.clinica.mentalhealth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DatabaseClient databaseClient;

    /**
     * Búsqueda Híbrida:
     * - Si 'query' son solo números -> Busca por DNI.
     * - Si tiene letras -> Busca por Nombre.
     */
    public Flux<Patient> searchPatient(String query) {
        if (query.matches("\\d+")) { // Regex: Solo dígitos
            return patientRepository.findByDni(query).flux();
        } else {
            return patientRepository.findByNameLike(query);
        }
    }

    @Transactional
    public Mono<Patient> createPatient(String name, String email, String dni) {
        // Validar que no exista DNI antes de crear (Opcional, la BD también lo valida)
        return patientRepository.findByDni(dni)
                .flatMap(existing -> Mono
                        .<Patient>error(new IllegalArgumentException("Ya existe un paciente con DNI " + dni)))
                .switchIfEmpty(
                        // Flujo de creación si no existe
                        userRepository.save(new User(null, email, passwordEncoder.encode("123"), Role.ROLE_PATIENT))
                                .flatMap(savedUser -> {
                                    // El bloque de código problemático ha sido reemplazado por la lógica SQL
                                    String sql = "INSERT INTO \"patients\" (id, name, email, dni) VALUES (:id, :name, :email, :dni)";

                                    return databaseClient.sql(sql)
                                            .bind("id", Objects.requireNonNull(savedUser.id()))
                                            .bind("name", Objects.requireNonNull(name))
                                            .bind("email", Objects.requireNonNull(email))
                                            .bind("dni", Objects.requireNonNull(dni)) // <--- Bind del DNI
                                            .fetch()
                                            .rowsUpdated()
                                            .thenReturn(new Patient(savedUser.id(), name, email, dni));
                                }) // Cierra el flatMap
                ); // Cierra el switchIfEmpty
    }
}
