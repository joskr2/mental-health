package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.domain.Patient;
import com.clinica.mentalhealth.domain.Role;
import com.clinica.mentalhealth.domain.User;
import com.clinica.mentalhealth.repository.PatientRepository;
import com.clinica.mentalhealth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient; // Importar esto
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DatabaseClient databaseClient; // Inyectamos el cliente SQL nativo

    public Flux<Patient> searchByName(String name) {
        return patientRepository.findByNameLike(name);
    }

    @Transactional
    public Mono<Patient> createPatient(String name, String email) {
        // 1. Crear Usuario (ID nulo -> INSERT automático)
        User newUser = new User(null, email, passwordEncoder.encode("123"), Role.ROLE_PATIENT);

        return userRepository.save(newUser)
                .flatMap(savedUser -> {
                    // 2. Insertar Paciente forzando el INSERT con SQL nativo
                    // Esto evita que R2DBC intente hacer UPDATE por tener ID asignado
                    String sql = "INSERT INTO \"patients\" (id, name, email) VALUES (:id, :name, :email)";

                    return databaseClient.sql(sql)
                            .bind("id", savedUser.id())
                            .bind("name", name)
                            .bind("email", email)
                            .fetch()
                            .rowsUpdated()
                            .thenReturn(new Patient(savedUser.id(), name, email));
                });
    }
}