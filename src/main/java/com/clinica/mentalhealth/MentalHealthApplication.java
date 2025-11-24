package com.clinica.mentalhealth;

import com.clinica.mentalhealth.domain.Role;
import com.clinica.mentalhealth.domain.User;
import com.clinica.mentalhealth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@SpringBootApplication
public class MentalHealthApplication {

    public static void main(String[] args) {
        SpringApplication.run(MentalHealthApplication.class, args);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // HE BORRADO EL BEAN 'r2dbcDialect'. Spring Boot lo configura solo.

    /**
     * SEMILLA DE DATOS MAESTROS
     * Se ejecuta cada vez que arranca la aplicación.
     */
    @Bean
    @org.springframework.context.annotation.Profile("!test")
    public CommandLineRunner seedData(
            UserRepository userRepo,
            DatabaseClient databaseClient,
            PasswordEncoder encoder) {

        return args -> {
            log.info("🌱 INICIANDO CARGA DE DATOS...");

            // 1. Limpiar tablas (Orden inverso a las Foreign Keys)
            databaseClient.sql("DELETE FROM \"appointments\"").fetch().rowsUpdated().block();
            databaseClient.sql("DELETE FROM \"patients\"").fetch().rowsUpdated().block();
            databaseClient.sql("DELETE FROM \"psychologists\"").fetch().rowsUpdated().block();
            databaseClient.sql("DELETE FROM \"rooms\"").fetch().rowsUpdated().block();
            databaseClient.sql("DELETE FROM \"users\"").fetch().rowsUpdated().block();

            // 2. Crear Usuarios (Identity Layer)
            var admin = userRepo.save(new User(null, "admin", encoder.encode("123"), Role.ROLE_ADMIN)).block();
            var doc = userRepo.save(new User(null, "doc", encoder.encode("123"), Role.ROLE_PSYCHOLOGIST)).block();
            var pepe = userRepo.save(new User(null, "pepe", encoder.encode("123"), Role.ROLE_PATIENT)).block();

            log.info("👤 Usuarios creados: Admin(ID={}), Doc(ID={}), Pepe(ID={})", admin.id(), doc.id(), pepe.id());

            // 3. Insertar Datos de Negocio (Business Layer) sincronizados con los IDs de
            // Usuario

            // Insertar Psicólogo
            databaseClient.sql("INSERT INTO \"psychologists\" (id, name, specialty) VALUES (:id, :name, :spec)")
                    .bind("id", java.util.Objects.requireNonNull(doc.id()))
                    .bind("name", "Dr. Strange")
                    .bind("spec", "Misticismo")
                    .fetch().rowsUpdated().block();

            // Insertar Paciente
            databaseClient.sql("INSERT INTO \"patients\" (id, name, email) VALUES (:id, :name, :email)")
                    .bind("id", java.util.Objects.requireNonNull(pepe.id()))
                    .bind("name", "Pepe Grillo")
                    .bind("email", "pepe@test.com")
                    .fetch().rowsUpdated().block();

            // Insertar Sala
            databaseClient.sql("INSERT INTO \"rooms\" (id, name) VALUES (1, 'Sala Suprema')")
                    .fetch().rowsUpdated().block();

            log.info("✅ DATOS MAESTROS CARGADOS: Listo para probar IA.");
            log.info("👉 Login Admin: username='admin', pass='123'");
            log.info("👉 Login Doc:   username='doc',   pass='123'");
            log.info("👉 Login Pepe:  username='pepe',  pass='123'");
        };
    }
}
