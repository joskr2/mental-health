package com.clinica.mentalhealth.security;

import com.clinica.mentalhealth.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    // Constants to avoid duplicated literals
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_TYPE_REFRESH = "refresh";

    // Separate keys for access and refresh; in real use, load from configuration
    private final SecretKey accessKey = Jwts.SIG.HS256.key().build();
    private final SecretKey refreshKey = Jwts.SIG.HS256.key().build();

    // Expirations
    private final Duration accessTtl = Duration.ofMinutes(30);
    private final Duration refreshTtl = Duration.ofDays(14);

    // Legacy single-token method (kept for compatibility if referenced elsewhere)
    public String generateToken(User user) {
        return generateAccessToken(user);
    }

    public String generateAccessToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.role())
                .claim(CLAIM_USER_ID, user.id())
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTtl.toMillis()))
                .signWith(accessKey)
                .compact();
    }

    public String generateRefreshToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("type", CLAIM_TYPE_REFRESH)
                .claim(CLAIM_USER_ID, user.id())
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshTtl.toMillis()))
                .signWith(refreshKey)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        try {
            parseClaims(token, accessKey);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token, refreshKey);
            return CLAIM_TYPE_REFRESH.equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getAccessClaims(String token) {
        return parseClaims(token, accessKey);
    }

    public Claims getRefreshClaims(String token) {
        return parseClaims(token, refreshKey);
    }

    public String rotateRefreshToken(String refreshToken) {
        Claims claims = getRefreshClaims(refreshToken);
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(claims.getSubject())
                .claim("type", CLAIM_TYPE_REFRESH)
                .claim(CLAIM_USER_ID, claims.get(CLAIM_USER_ID))
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshTtl.toMillis()))
                .signWith(refreshKey)
                .compact();
    }

    private Claims parseClaims(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}