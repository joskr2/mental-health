package com.clinica.mentalhealth.security;

// Un objeto simple para llevar los datos del usuario en la sesión reactiva
public record UserPrincipal(Long id, String username, String role) {}