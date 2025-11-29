package com.clinica.mentalhealth.repository;

import com.clinica.mentalhealth.domain.Patient;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PatientRepository extends ReactiveCrudRepository<Patient, Long> {

    Mono<Patient> findByEmail(String email);

    Mono<Patient> findByDni(String dni);

    Mono<Boolean> existsByEmail(String email);

    Mono<Boolean> existsByDni(String dni);

    /**
     * Búsqueda fuzzy usando pg_trgm con similarity() explícito
     * Usamos threshold 0.1 hardcodeado para garantizar tolerancia
     * Encuentra pacientes aunque haya errores tipográficos en el nombre
     * Ejemplo: "Gonsales" encontrará "González"
     */
    @Query("""
            SELECT * FROM "patients"
            WHERE similarity(LOWER(name), LOWER(:name)) > 0.1
            ORDER BY LOWER(name) <-> LOWER(:name) ASC
            LIMIT 20
            """)
    Flux<Patient> findByNameLike(String name);
}
