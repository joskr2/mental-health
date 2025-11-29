package com.clinica.mentalhealth.ai.tools;

/**
 * DTO para que la IA pueda contratar un nuevo Psicólogo.
 * Solo disponible para ADMIN.
 * 
 * NOTA DE SEGURIDAD: No incluye password. El sistema genera una
 * contraseña temporal segura que se muestra al administrador una sola vez.
 */
public record CreatePsychologistRequest(
                String name,
                String specialty,
                String email,
                String phone,
                String dni,
                String username
// password removido por seguridad - se genera automáticamente
) {
}
