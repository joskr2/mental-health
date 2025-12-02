package com.clinica.mentalhealth.security;

import com.clinica.mentalhealth.domain.Role;
import com.clinica.mentalhealth.domain.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests para JwtService.
 *
 * Verifica la generación, validación y extracción de claims de tokens JWT.
 */
@DisplayName("JwtService Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Configurar propiedades usando reflection (simula @Value injection)
        ReflectionTestUtils.setField(jwtService, "accessSecretString",
                "test-access-secret-key-minimum-32-characters!");
        ReflectionTestUtils.setField(jwtService, "refreshSecretString",
                "test-refresh-secret-key-minimum-32-characters");
        ReflectionTestUtils.setField(jwtService, "accessTtl", Duration.ofMinutes(30));
        ReflectionTestUtils.setField(jwtService, "refreshTtl", Duration.ofDays(14));

        // Inicializar las claves
        jwtService.init();

        // Usuario de prueba
        testUser = new User(1L, "testuser", "encodedPassword", Role.ROLE_ADMIN);
    }

    @Nested
    @DisplayName("generateAccessToken()")
    class GenerateAccessTokenTests {

        @Test
        @DisplayName("Debe generar un token válido")
        void shouldGenerateValidToken() {
            // Act
            String token = jwtService.generateAccessToken(testUser);

            // Assert
            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertTrue(token.split("\\.").length == 3); // JWT tiene 3 partes
        }

        @Test
        @DisplayName("Debe incluir username como subject")
        void shouldIncludeUsernameAsSubject() {
            // Act
            String token = jwtService.generateAccessToken(testUser);
            Claims claims = jwtService.getAccessClaims(token);

            // Assert
            assertEquals("testuser", claims.getSubject());
        }

        @Test
        @DisplayName("Debe incluir userId en claims")
        void shouldIncludeUserId() {
            // Act
            String token = jwtService.generateAccessToken(testUser);
            Claims claims = jwtService.getAccessClaims(token);

            // Assert
            assertEquals(1L, claims.get("userId", Long.class));
        }

        @Test
        @DisplayName("Debe incluir rol en claims")
        void shouldIncludeRole() {
            // Act
            String token = jwtService.generateAccessToken(testUser);
            Claims claims = jwtService.getAccessClaims(token);

            // Assert
            assertEquals("ROLE_ADMIN", claims.get("role", String.class));
        }

        @Test
        @DisplayName("Debe incluir tipo 'access' en claims")
        void shouldIncludeTypeAccess() {
            // Act
            String token = jwtService.generateAccessToken(testUser);
            Claims claims = jwtService.getAccessClaims(token);

            // Assert
            assertEquals("access", claims.get("type", String.class));
        }

        @Test
        @DisplayName("Debe tener fecha de expiración futura")
        void shouldHaveFutureExpiration() {
            // Act
            String token = jwtService.generateAccessToken(testUser);
            Claims claims = jwtService.getAccessClaims(token);

            // Assert
            assertNotNull(claims.getExpiration());
            assertTrue(claims.getExpiration().getTime() > System.currentTimeMillis());
        }
    }

    @Nested
    @DisplayName("generateRefreshToken()")
    class GenerateRefreshTokenTests {

        @Test
        @DisplayName("Debe generar un refresh token válido")
        void shouldGenerateValidRefreshToken() {
            // Act
            String token = jwtService.generateRefreshToken(testUser);

            // Assert
            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("Debe incluir tipo 'refresh' en claims")
        void shouldIncludeTypeRefresh() {
            // Act
            String token = jwtService.generateRefreshToken(testUser);
            Claims claims = jwtService.getRefreshClaims(token);

            // Assert
            assertEquals("refresh", claims.get("type", String.class));
        }

        @Test
        @DisplayName("Debe incluir jti (JWT ID) único")
        void shouldIncludeJti() {
            // Act
            String token1 = jwtService.generateRefreshToken(testUser);
            String token2 = jwtService.generateRefreshToken(testUser);

            Claims claims1 = jwtService.getRefreshClaims(token1);
            Claims claims2 = jwtService.getRefreshClaims(token2);

            // Assert
            assertNotNull(claims1.get("jti"));
            assertNotNull(claims2.get("jti"));
            assertNotEquals(claims1.get("jti"), claims2.get("jti")); // Deben ser diferentes
        }

        @Test
        @DisplayName("Debe incluir userId")
        void shouldIncludeUserId() {
            // Act
            String token = jwtService.generateRefreshToken(testUser);
            Claims claims = jwtService.getRefreshClaims(token);

            // Assert
            assertEquals(1, ((Number) claims.get("userId")).longValue());
        }
    }

    @Nested
    @DisplayName("validateAccessToken()")
    class ValidateAccessTokenTests {

        @Test
        @DisplayName("Debe validar token correcto")
        void shouldValidateCorrectToken() {
            // Arrange
            String token = jwtService.generateAccessToken(testUser);

            // Act & Assert
            assertTrue(jwtService.validateAccessToken(token));
        }

        @Test
        @DisplayName("Debe rechazar token malformado")
        void shouldRejectMalformedToken() {
            // Act & Assert
            assertFalse(jwtService.validateAccessToken("invalid.token.here"));
        }

        @Test
        @DisplayName("Debe rechazar token vacío")
        void shouldRejectEmptyToken() {
            // Act & Assert
            assertFalse(jwtService.validateAccessToken(""));
        }

        @Test
        @DisplayName("Debe rechazar token null")
        void shouldRejectNullToken() {
            // Act & Assert
            assertFalse(jwtService.validateAccessToken(null));
        }

        @Test
        @DisplayName("Debe rechazar refresh token como access token")
        void shouldRejectRefreshTokenAsAccessToken() {
            // Arrange
            String refreshToken = jwtService.generateRefreshToken(testUser);

            // Act & Assert
            assertFalse(jwtService.validateAccessToken(refreshToken));
        }
    }

    @Nested
    @DisplayName("validateRefreshToken()")
    class ValidateRefreshTokenTests {

        @Test
        @DisplayName("Debe validar refresh token correcto")
        void shouldValidateCorrectRefreshToken() {
            // Arrange
            String token = jwtService.generateRefreshToken(testUser);

            // Act & Assert
            assertTrue(jwtService.validateRefreshToken(token));
        }

        @Test
        @DisplayName("Debe rechazar access token como refresh token")
        void shouldRejectAccessTokenAsRefreshToken() {
            // Arrange
            String accessToken = jwtService.generateAccessToken(testUser);

            // Act & Assert
            assertFalse(jwtService.validateRefreshToken(accessToken));
        }

        @Test
        @DisplayName("Debe rechazar token malformado")
        void shouldRejectMalformedToken() {
            // Act & Assert
            assertFalse(jwtService.validateRefreshToken("not.a.valid.jwt"));
        }
    }

    @Nested
    @DisplayName("rotateRefreshToken()")
    class RotateRefreshTokenTests {

        @Test
        @DisplayName("Debe generar nuevo token con mismo userId")
        void shouldGenerateNewTokenWithSameUserId() {
            // Arrange
            String originalToken = jwtService.generateRefreshToken(testUser);

            // Act
            String rotatedToken = jwtService.rotateRefreshToken(originalToken);

            // Assert
            Claims originalClaims = jwtService.getRefreshClaims(originalToken);
            Claims rotatedClaims = jwtService.getRefreshClaims(rotatedToken);

            assertEquals(originalClaims.get("userId"), rotatedClaims.get("userId"));
        }

        @Test
        @DisplayName("Debe generar nuevo jti diferente")
        void shouldGenerateNewJti() {
            // Arrange
            String originalToken = jwtService.generateRefreshToken(testUser);

            // Act
            String rotatedToken = jwtService.rotateRefreshToken(originalToken);

            // Assert
            Claims originalClaims = jwtService.getRefreshClaims(originalToken);
            Claims rotatedClaims = jwtService.getRefreshClaims(rotatedToken);

            assertNotEquals(originalClaims.get("jti"), rotatedClaims.get("jti"));
        }

        @Test
        @DisplayName("Debe mantener el mismo subject")
        void shouldKeepSameSubject() {
            // Arrange
            String originalToken = jwtService.generateRefreshToken(testUser);

            // Act
            String rotatedToken = jwtService.rotateRefreshToken(originalToken);

            // Assert
            Claims originalClaims = jwtService.getRefreshClaims(originalToken);
            Claims rotatedClaims = jwtService.getRefreshClaims(rotatedToken);

            assertEquals(originalClaims.getSubject(), rotatedClaims.getSubject());
        }
    }

    @Nested
    @DisplayName("Diferentes roles")
    class DifferentRolesTests {

        @Test
        @DisplayName("Debe funcionar con ROLE_PSYCHOLOGIST")
        void shouldWorkWithPsychologistRole() {
            // Arrange
            User psychologist = new User(2L, "doctor", "pass", Role.ROLE_PSYCHOLOGIST);

            // Act
            String token = jwtService.generateAccessToken(psychologist);
            Claims claims = jwtService.getAccessClaims(token);

            // Assert
            assertEquals("ROLE_PSYCHOLOGIST", claims.get("role"));
            assertEquals(2L, claims.get("userId", Long.class));
        }

        @Test
        @DisplayName("Debe funcionar con ROLE_PATIENT")
        void shouldWorkWithPatientRole() {
            // Arrange
            User patient = new User(3L, "patient@test.com", "pass", Role.ROLE_PATIENT);

            // Act
            String token = jwtService.generateAccessToken(patient);
            Claims claims = jwtService.getAccessClaims(token);

            // Assert
            assertEquals("ROLE_PATIENT", claims.get("role"));
            assertEquals(3L, claims.get("userId", Long.class));
        }
    }

    @Nested
    @DisplayName("getRefreshTokenTtl()")
    class GetRefreshTokenTtlTests {

        @Test
        @DisplayName("Debe retornar el TTL configurado")
        void shouldReturnConfiguredTtl() {
            // Act
            Duration ttl = jwtService.getRefreshTokenTtl();

            // Assert
            assertEquals(Duration.ofDays(14), ttl);
        }
    }

    @Nested
    @DisplayName("Legacy generateToken()")
    class LegacyGenerateTokenTests {

        @Test
        @DisplayName("Debe generar access token (compatibilidad)")
        @SuppressWarnings("deprecation")
        void shouldGenerateAccessTokenForCompatibility() {
            // Act
            String legacyToken = jwtService.generateToken(testUser);
            String accessToken = jwtService.generateAccessToken(testUser);

            // Assert - Ambos deberían ser válidos como access tokens
            assertTrue(jwtService.validateAccessToken(legacyToken));
            assertTrue(jwtService.validateAccessToken(accessToken));
        }
    }
}
