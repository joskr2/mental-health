package com.clinica.mentalhealth.ai.tools;

/**
 * DTO para que la IA pueda contratar un nuevo Psicólogo.
 * Solo disponible para ADMIN.
 */
public record CreatePsychologistRequest(
        String name,
        String specialty,
        String email,
        String phone,
        String dni,
        String username,
        String password
) {
}
