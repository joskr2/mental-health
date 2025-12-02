package com.clinica.mentalhealth.config;

import com.clinica.mentalhealth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configuración de seguridad para la API.
 *
 * Define:
 * - Rutas públicas (login, swagger, actuator)
 * - Rutas protegidas (requieren JWT)
 * - Integración del filtro JWT
 *
 * Soporta tanto rutas legacy (/api/...) como versionadas (/api/v1/...).
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(
    ServerHttpSecurity http,
    JwtAuthenticationFilter jwtFilter
  ) {
    return http
      .csrf(ServerHttpSecurity.CsrfSpec::disable)
      .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
      .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
      .authorizeExchange(auth ->
        auth
          // === Rutas de autenticación (públicas) ===
          // Soporta tanto legacy como versionadas
          .pathMatchers(
            HttpMethod.POST,
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh"
          )
          .permitAll()
          // === Swagger UI y OpenAPI (públicas para desarrollo) ===
          .pathMatchers(
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/webjars/**"
          )
          .permitAll()
          // === Actuator endpoints (públicas para monitoreo) ===
          .pathMatchers("/actuator/**")
          .permitAll()
          // === Todas las demás rutas requieren autenticación ===
          .anyExchange()
          .authenticated()
      )
      .addFilterBefore(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
      .build();
  }
}
