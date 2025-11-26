package com.clinica.mentalhealth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("patients") // Mapea al nombre de la tabla SQL
public record Patient(
                @Id Long id,
                String name,
                String email,
                String phone,
                String dni) {
}
