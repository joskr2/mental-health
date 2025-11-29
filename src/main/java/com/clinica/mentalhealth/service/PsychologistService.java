package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.config.ReactiveCache;
import com.clinica.mentalhealth.domain.Psychologist;
import com.clinica.mentalhealth.domain.Role;
import com.clinica.mentalhealth.domain.User;
import com.clinica.mentalhealth.repository.PsychologistRepository;
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

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PsychologistService {

        private static final String PSYCHOLOGIST_NOT_FOUND = "Psicólogo no encontrado con ID: ";
        private static final String CACHE_PREFIX = "psychologist:";
        private static final String CACHE_ALL = CACHE_PREFIX + "all";

        private final PsychologistRepository psychologistRepository;
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final DatabaseClient databaseClient;
        private final ReactiveCache reactiveCache;

        // --- LECTURA ---

        /**
         * Retorna todos los psicólogos como Flux (streaming, no cacheado).
         */
        public Flux<Psychologist> findAll() {
                log.debug("Buscando todos los psicólogos (stream)");
                return psychologistRepository.findAll();
        }

        /**
         * Versión cacheada reactiva que retorna Mono<List>.
         * Usa ReactiveCache para caché no bloqueante.
         */
        public Mono<List<Psychologist>> findAllCached() {
                return reactiveCache.cacheList(CACHE_ALL, () -> {
                        log.debug("Cache miss: buscando todos los psicólogos de BD");
                        return psychologistRepository.findAll().collectList();
                });
        }

        /**
         * Busca un psicólogo por ID con caché reactivo.
         */
        public Mono<Psychologist> findById(@NonNull Long id) {
                String cacheKey = CACHE_PREFIX + id;
                return reactiveCache.cacheMono(cacheKey, () -> {
                        log.debug("Cache miss: buscando psicólogo con ID: {}", id);
                        return psychologistRepository.findById(id)
                                        .switchIfEmpty(Mono.error(new ResponseStatusException(
                                                        HttpStatus.NOT_FOUND, PSYCHOLOGIST_NOT_FOUND + id)));
                });
        }

        // --- CREACIÓN (Transaccional: User + Psychologist) ---
        @Transactional
        public Mono<Psychologist> createPsychologist(String name, String specialty, String email, String phone,
                        String dni, String username, String password) {
                log.info("Creando psicólogo: name={}, specialty={}, email={}, dni={}, username={}",
                                name, specialty, email, dni, username);

                // 1. Crear Usuario con Rol de Psicólogo
                User newUser = new User(null, username, passwordEncoder.encode(password), Role.ROLE_PSYCHOLOGIST);

                return userRepository.save(newUser)
                                .flatMap(savedUser -> {
                                        // 2. Insertar Psicólogo forzando el ID del usuario (SQL Nativo)
                                        String sql = "INSERT INTO \"psychologists\" (id, name, specialty, email, phone, dni) "
                                                        +
                                                        "VALUES (:id, :name, :spec, :email, :phone, :dni)";

                                        return databaseClient.sql(sql)
                                                        .bind("id", Objects.requireNonNull(savedUser.id()))
                                                        .bind("name", Objects.requireNonNull(name))
                                                        .bind("spec", Objects.requireNonNull(specialty))
                                                        .bind("email", Objects.requireNonNull(email))
                                                        .bind("phone", Objects.requireNonNull(phone))
                                                        .bind("dni", Objects.requireNonNull(dni))
                                                        .fetch()
                                                        .rowsUpdated()
                                                        .doOnSuccess(rows -> log.info(
                                                                        "Psicólogo creado exitosamente con ID: {}",
                                                                        savedUser.id()))
                                                        .thenReturn(new Psychologist(savedUser.id(), name, specialty,
                                                                        email, phone, dni));
                                })
                                // Invalidar caché después de crear
                                .flatMap(p -> reactiveCache.evictByPrefix(CACHE_PREFIX).thenReturn(p));
        }

        // --- ACTUALIZACIÓN ---
        @Transactional
        public Mono<Psychologist> updatePsychologist(@NonNull Long id, String name, String specialty,
                        String email, String phone, String dni) {
                log.info("Actualizando psicólogo con ID: {}", id);
                return psychologistRepository.findById(id)
                                .switchIfEmpty(Mono.error(new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, PSYCHOLOGIST_NOT_FOUND + id)))
                                .flatMap(existing -> {
                                        String sql = "UPDATE \"psychologists\" SET name = :name, specialty = :spec, " +
                                                        "email = :email, phone = :phone, dni = :dni WHERE id = :id";
                                        return databaseClient.sql(sql)
                                                        .bind("id", id)
                                                        .bind("name", Objects.requireNonNull(name))
                                                        .bind("spec", Objects.requireNonNull(specialty))
                                                        .bind("email", Objects.requireNonNull(email))
                                                        .bind("phone", Objects.requireNonNull(phone))
                                                        .bind("dni", Objects.requireNonNull(dni))
                                                        .fetch()
                                                        .rowsUpdated()
                                                        .doOnSuccess(rows -> log.info(
                                                                        "Psicólogo actualizado exitosamente con ID: {}",
                                                                        id))
                                                        .thenReturn(new Psychologist(id, name, specialty, email, phone,
                                                                        dni));
                                })
                                // Invalidar caché después de actualizar
                                .flatMap(p -> reactiveCache.evictByPrefix(CACHE_PREFIX).thenReturn(p));
        }

        // --- ELIMINACIÓN (Transaccional: Psychologist + User) ---
        @Transactional
        public Mono<Void> deletePsychologist(@NonNull Long id) {
                log.info("Eliminando psicólogo con ID: {}", id);
                return psychologistRepository.findById(id)
                                .switchIfEmpty(Mono.error(new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, PSYCHOLOGIST_NOT_FOUND + id)))
                                .flatMap(p ->
                                // Borramos entidad de negocio primero, luego el usuario de acceso
                                psychologistRepository.deleteById(Objects.requireNonNull(p.id()))
                                                .then(userRepository.deleteById(id))
                                                .doOnSuccess(v -> log.info(
                                                                "Psicólogo eliminado exitosamente con ID: {}", id)))
                                // Invalidar caché después de eliminar
                                .then(reactiveCache.evictByPrefix(CACHE_PREFIX));
        }
}
