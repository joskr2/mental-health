package com.clinica.mentalhealth.service;

import com.clinica.mentalhealth.domain.Psychologist;
import com.clinica.mentalhealth.repository.PsychologistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PsychologistService {

    private final PsychologistRepository psychologistRepository;

    @Cacheable(value = "psychologists", key = "'all'")
    public Flux<Psychologist> getAllPsychologists() {
        return psychologistRepository.findAll();
    }

    @Cacheable(value = "psychologists", key = "#id")
    public Mono<Psychologist> getPsychologistById(Long id) {
        return psychologistRepository.findById(Objects.requireNonNull(id));
    }

    @CacheEvict(value = "psychologists", allEntries = true)
    public Mono<Psychologist> createPsychologist(Psychologist psychologist) {
        return psychologistRepository.save(Objects.requireNonNull(psychologist));
    }

    @CacheEvict(value = "psychologists", allEntries = true)
    public Mono<Psychologist> updatePsychologist(Psychologist psychologist) {
        return psychologistRepository.save(Objects.requireNonNull(psychologist));
    }

    @CacheEvict(value = "psychologists", allEntries = true)
    public Mono<Void> deletePsychologist(Long id) {
        return psychologistRepository.deleteById(Objects.requireNonNull(id));
    }
}
