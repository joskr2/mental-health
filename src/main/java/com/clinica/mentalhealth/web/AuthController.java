package com.clinica.mentalhealth.web;

import com.clinica.mentalhealth.repository.UserRepository;
import com.clinica.mentalhealth.security.JwtService;
import com.clinica.mentalhealth.web.dto.LoginRequest;
import com.clinica.mentalhealth.web.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Autenticación con JWT (access y refresh tokens)")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica usuario y devuelve access token (30 min) y refresh token (14 días)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login exitoso, tokens generados"),
        @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest request) {
        return userRepository.findByUsername(request.username())
                .filter(user -> passwordEncoder.matches(request.password(), user.getPassword())) // Verificar password
                .map(user -> {
                    // Si pasa el filtro, generamos token
                    String access = jwtService.generateAccessToken(user);
                    String refresh = jwtService.generateRefreshToken(user);
                    return ResponseEntity.ok(new LoginResponse(access, refresh));
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()); // Si no existe o pass incorrecto
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar tokens", description = "Usa el refresh token para obtener un nuevo par de tokens (rotación)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tokens refrescados exitosamente"),
        @ApiResponse(responseCode = "401", description = "Refresh token inválido o expirado")
    })
    public Mono<ResponseEntity<LoginResponse>> refresh(@RequestBody String refreshToken) {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        var claims = jwtService.getRefreshClaims(refreshToken);
        return userRepository.findById(Long.valueOf(claims.get("userId").toString()))
                .map(user -> {
                    String newAccess = jwtService.generateAccessToken(user);
                    String newRefresh = jwtService.rotateRefreshToken(refreshToken);
                    return ResponseEntity.ok(new LoginResponse(newAccess, newRefresh));
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}