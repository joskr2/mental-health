package com.clinica.mentalhealth;

import com.clinica.mentalhealth.domain.Role;
import com.clinica.mentalhealth.domain.User;
import com.clinica.mentalhealth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class MentalHealthApplication {

    public static void main(String[] args) {
        SpringApplication.run(MentalHealthApplication.class, args);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Mantenemos esto para crear el Admin al inicio siempre
    @Bean
    public CommandLineRunner seedUsers(UserRepository userRepo, PasswordEncoder encoder) {
        return args -> {
            var admin = new User(null, "admin", encoder.encode("password123"), Role.ROLE_ADMIN);
            userRepo.findByUsername("admin")
                    .switchIfEmpty(userRepo.save(admin))
                    .subscribe();
        };
    }
}