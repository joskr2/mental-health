package com.clinica.mentalhealth.repository;

// 1. IMPORTANTE: Debe apuntar a TU record
import com.clinica.mentalhealth.domain.Patient;

// 2. IMPORTANTE: Debe ser la versión REACTIVA, no la CRUD normal
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

// 3. Reactor Core
import reactor.core.publisher.Mono;

// 4. La definición
public interface PatientRepository extends ReactiveCrudRepository<Patient, Long> {
    Mono<Patient> findByEmail(String email);
}
