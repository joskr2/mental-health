package com.clinica.mentalhealth.web;

import com.clinica.mentalhealth.repository.UserRepository;
import com.clinica.mentalhealth.security.JwtService;
import com.clinica.mentalhealth.service.RefreshTokenSessionService;
import com.clinica.mentalhealth.web.dto.LoginRequest;
import com.clinica.mentalhealth.web.dto.LoginResponse;
import com.clinica.mentalhealth.web.dto.LogoutRequest;
import com.clinica.mentalhealth.web.dto.RefreshTokenRequest;
import com.clinica.mentalhealth.web.dto.SessionInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Autenticación con JWT (access y refresh tokens) con seguridad máxima")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenSessionService refreshTokenSessionService;

    private static final String UNKNOWN_DEVICE = "Unknown";
    private static final String UNKNOWN_IP = "0.0.0.0";

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica usuario y devuelve access token (30 min) y refresh token (14 días). Crea una sesión con estado para máxima seguridad.")
    @ApiResponse(responseCode = "200", description = "Login exitoso, tokens generados")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    public Mono<ResponseEntity<LoginResponse>> login(
            @RequestBody LoginRequest request,
            ServerHttpRequest httpRequest) {

        String deviceInfo = extractDeviceInfo(httpRequest);
        String ipAddress = extractIpAddress(httpRequest);

        return userRepository.findByUsername(request.username())
                .filter(user -> passwordEncoder.matches(request.password(), user.getPassword()))
                .flatMap(user -> refreshTokenSessionService.createSession(user, deviceInfo, ipAddress))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar tokens", description = "Usa el refresh token para obtener un nuevo par de tokens. "
            +
            "El token usado se invalida inmediatamente (one-time use). " +
            "Si se detecta reutilización de un token ya usado, TODAS las sesiones del usuario son revocadas por seguridad.")
    @ApiResponse(responseCode = "200", description = "Tokens refrescados exitosamente")
    @ApiResponse(responseCode = "401", description = "Refresh token inválido, expirado o ya utilizado")
    public Mono<ResponseEntity<LoginResponse>> refresh(
            @RequestBody RefreshTokenRequest request,
            ServerHttpRequest httpRequest) {

        String deviceInfo = extractDeviceInfo(httpRequest);
        String ipAddress = extractIpAddress(httpRequest);

        return refreshTokenSessionService.rotateToken(request.refreshToken(), deviceInfo, ipAddress)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Revoca el refresh token actual. Si logoutAll=true, cierra sesión en TODOS los dispositivos.")
    @ApiResponse(responseCode = "200", description = "Sesión cerrada exitosamente")
    @ApiResponse(responseCode = "401", description = "Token inválido")
    public Mono<ResponseEntity<Void>> logout(@RequestBody LogoutRequest request) {
        if (request.logoutAll()) {
            // Obtener userId del token y revocar todas las sesiones
            if (!jwtService.validateRefreshToken(request.refreshToken())) {
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            }
            var claims = jwtService.getRefreshClaims(request.refreshToken());
            Long userId = Long.valueOf(claims.get("userId").toString());
            return refreshTokenSessionService.revokeAllUserSessions(userId)
                    .map(count -> ResponseEntity.ok().<Void>build());
        } else {
            return refreshTokenSessionService.revokeToken(request.refreshToken())
                    .map(success -> Boolean.TRUE.equals(success)
                            ? ResponseEntity.ok().<Void>build()
                            : ResponseEntity.status(HttpStatus.UNAUTHORIZED).<Void>build());
        }
    }

    @GetMapping("/sessions")
    @Operation(summary = "Ver sesiones activas", description = "Lista todas las sesiones activas del usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Lista de sesiones activas")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    public Flux<SessionInfo> getActiveSessions(@AuthenticationPrincipal com.clinica.mentalhealth.domain.User user) {
        return refreshTokenSessionService.getActiveSessions(user.id());
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Revocar sesión específica", description = "Cierra sesión en un dispositivo específico por su ID de sesión")
    @ApiResponse(responseCode = "200", description = "Sesión revocada")
    @ApiResponse(responseCode = "404", description = "Sesión no encontrada")
    public Mono<ResponseEntity<Void>> revokeSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal com.clinica.mentalhealth.domain.User user) {
        // Aquí deberíamos verificar que la sesión pertenece al usuario
        return refreshTokenSessionService.revokeToken(sessionId)
                .map(success -> Boolean.TRUE.equals(success)
                        ? ResponseEntity.ok().<Void>build()
                        : ResponseEntity.notFound().<Void>build());
    }

    /**
     * Extrae información del dispositivo desde el User-Agent header
     */
    private String extractDeviceInfo(ServerHttpRequest request) {
        var headers = request.getHeaders();
        String userAgent = headers.getFirst("User-Agent");
        return userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 512)) : UNKNOWN_DEVICE;
    }

    /**
     * Extrae la IP del cliente, considerando proxies (X-Forwarded-For)
     */
    private String extractIpAddress(ServerHttpRequest request) {
        var headers = request.getHeaders();
        String xForwardedFor = headers.getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // El primer valor es la IP original del cliente
            return xForwardedFor.split(",")[0].trim();
        }
        var remoteAddress = request.getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : UNKNOWN_IP;
    }
}
