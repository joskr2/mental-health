package com.clinica.mentalhealth.repository;

import com.clinica.mentalhealth.domain.Psychologist;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface PsychologistRepository extends ReactiveCrudRepository<Psychologist, Long> {
}