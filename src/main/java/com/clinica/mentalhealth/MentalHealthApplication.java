package com.clinica.mentalhealth;

import com.clinica.mentalhealth.config.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Aplicación principal de Mental Health Clinic API.
 *
 * Esta es una API REST reactiva para gestión de clínica de salud mental
 * con asistente de IA integrado.
 *
 * Características:
 * - WebFlux (programación reactiva)
 * - R2DBC (acceso reactivo a PostgreSQL)
 * - Spring Security con JWT (access + refresh tokens)
 * - Spring AI con DeepSeek para asistente clínico
 * - Flyway para migraciones de base de datos
 * - Rate limiting para protección de la API
 * - Logging estructurado (JSON en producción)
 */
@Slf4j
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties(SecurityProperties.class)
public class MentalHealthApplication {

  public static void main(String[] args) {
    SpringApplication.run(MentalHealthApplication.class, args);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * CommandLineRunner que se ejecuta al iniciar la aplicación.
   *
   * NOTA: Los datos de prueba ahora se gestionan con Flyway (V2__seed_data.sql).
   * Este runner solo muestra información útil para desarrollo.
   *
   * En el perfil "test", este bean no se carga para evitar conflictos con los tests.
   */
  @Bean
  @org.springframework.context.annotation.Profile("!test")
  public CommandLineRunner startupInfo() {
    return args -> {
      log.info(
        "╔══════════════════════════════════════════════════════════════╗"
      );
      log.info(
        "║       🏥 Mental Health Clinic API - Started Successfully     ║"
      );
      log.info(
        "╠══════════════════════════════════════════════════════════════╣"
      );
      log.info(
        "║  📋 Database migrations managed by Flyway                    ║"
      );
      log.info(
        "║  🔐 JWT Authentication enabled                               ║"
      );
      log.info(
        "║  🤖 AI Assistant ready (DeepSeek)                            ║"
      );
      log.info(
        "║  🚦 Rate limiting active                                     ║"
      );
      log.info(
        "╠══════════════════════════════════════════════════════════════╣"
      );
      log.info(
        "║  Test Credentials (Development):                             ║"
      );
      log.info(
        "║    👤 Admin:    username=admin, password=password            ║"
      );
      log.info(
        "║    👨‍⚕️ Doctor:   username=doc, password=password              ║"
      );
      log.info(
        "║    🧑 Patient:  username=pepe@test.com, password=password    ║"
      );
      log.info(
        "╠══════════════════════════════════════════════════════════════╣"
      );
      log.info(
        "║  Endpoints:                                                  ║"
      );
      log.info(
        "║    📖 API Docs:    http://localhost:8080/docs                ║"
      );
      log.info(
        "║    ❤️  Health:      http://localhost:8080/actuator/health    ║"
      );
      log.info(
        "║    🔑 Login:       POST /api/auth/login                      ║"
      );
      log.info(
        "╚══════════════════════════════════════════════════════════════╝"
      );
    };
  }
}
