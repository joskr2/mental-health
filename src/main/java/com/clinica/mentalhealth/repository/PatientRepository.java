package com.clinica.mentalhealth.repository;

// 1. IMPORTANTE: Debe apuntar a TU record
import com.clinica.mentalhealth.domain.Patient;

import org.springframework.data.r2dbc.repository.Query;
// 2. IMPORTANTE: Debe ser la versión REACTIVA, no la CRUD normal
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

// 3. Reactor Core
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// 4. La definición
public interface PatientRepository extends ReactiveCrudRepository<Patient, Long> {
    Mono<Patient> findByEmail(String email);

    // Búsqueda exacta por DNI (Prioritaria)
    Mono<Patient> findByDni(String dni);

    // Búsqueda difusa por nombre usando pg_trgm (tolerante a errores ortográficos)
    @Query("""
                SELECT * FROM "patients"
                WHERE LOWER(name) % LOWER(:name)
                ORDER BY LOWER(name) <-> LOWER(:name) ASC
                LIMIT 20
            """)
    Flux<Patient> findByNameLike(String name);
}
