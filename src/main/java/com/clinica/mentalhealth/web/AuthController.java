package com.clinica.mentalhealth.web;

import com.clinica.mentalhealth.repository.UserRepository;
import com.clinica.mentalhealth.security.JwtService;
import com.clinica.mentalhealth.web.dto.LoginRequest;
import com.clinica.mentalhealth.web.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest request) {
        return userRepository.findByUsername(request.username())
                .filter(user -> passwordEncoder.matches(request.password(), user.getPassword())) // Verificar password
                .map(user -> {
                    // Si pasa el filtro, generamos token
                    String token = jwtService.generateToken(user);
                    return ResponseEntity.ok(new LoginResponse(token));
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()); // Si no existe o pass incorrecto
    }
}