package com.clinica.mentalhealth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("rooms")
public record Room(
        @Id Long id,
        String name
) {}