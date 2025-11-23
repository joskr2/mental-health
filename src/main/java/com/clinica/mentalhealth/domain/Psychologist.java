package com.clinica.mentalhealth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("psychologists")
public record Psychologist(
        @Id Long id,
        String name,
        String specialty
) {}