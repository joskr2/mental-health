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
     * Búsqueda fuzzy usando pg_trgm con operador %
     * El threshold se configura a nivel de base de datos (0.1)
     * Encuentra pacientes aunque haya errores tipográficos en el nombre
     * Ejemplo: "Gonsales" encontrará "González"
     */
    @Query("""
            SELECT * FROM "patients"
            WHERE LOWER(name) % LOWER(:name)
            ORDER BY LOWER(name) <-> LOWER(:name) ASC
            LIMIT 20
            """)
    Flux<Patient> findByNameLike(String name);
}
