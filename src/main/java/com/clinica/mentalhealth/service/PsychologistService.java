package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.domain.Psychologist;
import com.clinica.mentalhealth.domain.Role;
import com.clinica.mentalhealth.domain.User;
import com.clinica.mentalhealth.repository.PsychologistRepository;
import com.clinica.mentalhealth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
public class PsychologistService {

    private static final String PSYCHOLOGIST_NOT_FOUND = "Psicólogo no encontrado con ID: ";

    private final PsychologistRepository psychologistRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DatabaseClient databaseClient;

    // --- LECTURA ---

    @Cacheable(value = "psychologists", key = "'all'")
    public Flux<Psychologist> findAll() {
        log.debug("Buscando todos los psicólogos");
        return psychologistRepository.findAll();
    }

    @Cacheable(value = "psychologists", key = "#id")
    public Mono<Psychologist> findById(@NonNull Long id) {
        log.debug("Buscando psicólogo con ID: {}", id);
        return psychologistRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, PSYCHOLOGIST_NOT_FOUND + id)));
    }

    // --- CREACIÓN (Transaccional: User + Psychologist) ---
    @Transactional
    @CacheEvict(value = "psychologists", allEntries = true)
    public Mono<Psychologist> createPsychologist(String name, String specialty, String email, String phone,
            String dni, String username, String password) {
        log.info("Creando psicólogo: name={}, specialty={}, email={}, dni={}, username={}",
                name, specialty, email, dni, username);

        // 1. Crear Usuario con Rol de Psicólogo
        User newUser = new User(null, username, passwordEncoder.encode(password), Role.ROLE_PSYCHOLOGIST);

        return userRepository.save(newUser)
                .flatMap(savedUser -> {
                    // 2. Insertar Psicólogo forzando el ID del usuario (SQL Nativo)
                    String sql = "INSERT INTO \"psychologists\" (id, name, specialty, email, phone, dni) " +
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
                            .doOnSuccess(rows -> log.info("Psicólogo creado exitosamente con ID: {}", savedUser.id()))
                            .thenReturn(new Psychologist(savedUser.id(), name, specialty, email, phone, dni));
                });
    }

    // --- ACTUALIZACIÓN ---
    @Transactional
    @CacheEvict(value = "psychologists", allEntries = true)
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
                            .doOnSuccess(rows -> log.info("Psicólogo actualizado exitosamente con ID: {}", id))
                            .thenReturn(new Psychologist(id, name, specialty, email, phone, dni));
                });
    }

    // --- ELIMINACIÓN (Transaccional: Psychologist + User) ---
    @Transactional
    @CacheEvict(value = "psychologists", allEntries = true)
    public Mono<Void> deletePsychologist(@NonNull Long id) {
        log.info("Eliminando psicólogo con ID: {}", id);
        return psychologistRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, PSYCHOLOGIST_NOT_FOUND + id)))
                .flatMap(p ->
                // Borramos entidad de negocio primero, luego el usuario de acceso
                psychologistRepository.deleteById(Objects.requireNonNull(p.id()))
                        .then(userRepository.deleteById(id))
                        .doOnSuccess(v -> log.info("Psicólogo eliminado exitosamente con ID: {}", id)));
    }
}
