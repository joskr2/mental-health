package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.domain.Patient;
import com.clinica.mentalhealth.domain.Role;
import com.clinica.mentalhealth.domain.User;
import com.clinica.mentalhealth.repository.PatientRepository;
import com.clinica.mentalhealth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

    /**
     * Busca pacientes por nombre (Wrapper del repositorio).
     */
    public Flux<Patient> searchByName(String name) {
        return patientRepository.findByNameLike(name);
    }

    /**
     * Crea un paciente Y su usuario de acceso en una sola transacción.
     * Esta es la lógica que antes tenías "hardcodeada" en la IA.
     */
    @Transactional
    public Mono<Patient> createPatient(String name, String email) {
        // 1. Crear Usuario (Pass por defecto '123')
        User newUser = new User(null, email, passwordEncoder.encode("123"), Role.ROLE_PATIENT);

        return userRepository.save(newUser)
                .flatMap(savedUser -> {
                    // 2. Crear Paciente vinculado al ID del usuario
                    Patient newPatient = new Patient(savedUser.id(), name, email);
                    return patientRepository.save(newPatient);
                });
    }
}