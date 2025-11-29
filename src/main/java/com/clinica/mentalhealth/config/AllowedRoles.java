package com.clinica.mentalhealth.config;

import com.clinica.mentalhealth.domain.Role;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para declarar qué roles pueden usar una herramienta de IA.
 * Reemplaza el filtrado manual if/else en ClinicalAgentService.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedRoles {
    Role[] value();
}
