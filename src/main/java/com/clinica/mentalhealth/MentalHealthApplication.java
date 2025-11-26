package com.clinica.mentalhealth;

import com.clinica.mentalhealth.domain.Role;
import com.clinica.mentalhealth.domain.User;
import com.clinica.mentalhealth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Objects;

@Slf4j
@SpringBootApplication
@EnableCaching
public class MentalHealthApplication {

    private static final String BIND_EMAIL = "email";
    private static final String BIND_PHONE = "phone";

    public static void main(String[] args) {
        SpringApplication.run(MentalHealthApplication.class, args);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

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
            var grillo = userRepo.save(new User(null, "grillo", encoder.encode("123"), Role.ROLE_PATIENT)).block();

            log.info("👤 Usuarios creados: Admin(ID={}), Doc(ID={}), Pepe(ID={})", admin.id(), doc.id(), pepe.id());

            // 3. Insertar Datos de Negocio (Business Layer) sincronizados con los IDs de
            // Usuario

            // Insertar Psicólogo
            databaseClient
                    .sql("INSERT INTO \"psychologists\" (id, name, specialty, email, phone, dni) VALUES (:id, :name, :spec, :email, :phone, :dni)")
                    .bind("id", Objects.requireNonNull(doc.id()))
                    .bind("name", "Dr. Strange")
                    .bind("spec", "Misticismo")
                    .bind(BIND_EMAIL, "strange@clinic.com")
                    .bind(BIND_PHONE, "+51999111222")
                    .bind("dni", "99887766")
                    .fetch().rowsUpdated().block();

            // Insertar Paciente
            databaseClient.sql("INSERT INTO \"patients\" (id, name, email, phone) VALUES (:id, :name, :email, :phone)")
                    .bind("id", Objects.requireNonNull(pepe.id()))
                    .bind("name", "Pepe Grillo")
                    .bind(BIND_EMAIL, "pepe@test.com")
                    .bind(BIND_PHONE, "+51999333444")
                    .fetch().rowsUpdated().block();

            databaseClient.sql(
                    "INSERT INTO \"patients\" (id, name, email, phone, dni) VALUES (:id, :name, :email, :phone, :dni)")
                    .bind("id", Objects.requireNonNull(grillo.id()))
                    .bind("name", "Grillo Pepito")
                    .bind(BIND_EMAIL, "grillo@test.com")
                    .bind(BIND_PHONE, "+51999555666")
                    .bind("dni", "12345678")
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
