package com.ahmed.hospital.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(String email) {

        return generateToken(
                email,
                accessTokenExpiration,
                "access"
        );
    }

    public String generateRefreshToken(String email) {

        return generateToken(
                email,
                refreshTokenExpiration,
                "refresh"
        );
    }

    private String generateToken(
            String email,
            long expiration,
            String type
    ) {

        Date now = new Date();

        Date expiryDate = new Date(
                now.getTime() + expiration
        );

        return Jwts.builder()
                .subject(email)
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public String extractEmail(String token) {

        return extractAllClaims(token)
                .getSubject();
    }

    public boolean isRefreshToken(String token) {

        try {

            Claims claims = extractAllClaims(token);

            return "refresh".equals(
                    claims.get("type", String.class)
            );

        } catch (Exception ex) {

            return false;
        }
    }

    public boolean isTokenValid(
            String token,
            String email
    ) {

        try {

            Claims claims = extractAllClaims(token);

            String extractedEmail = claims.getSubject();

            String type = claims.get(
                    "type",
                    String.class
            );

            return "access".equals(type)
                    && extractedEmail.equals(email)
                    && !isTokenExpired(token);

        } catch (Exception ex) {

            return false;
        }
    }

    private boolean isTokenExpired(String token) {

        return extractAllClaims(token)
                .getExpiration()
                .before(new Date());
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}