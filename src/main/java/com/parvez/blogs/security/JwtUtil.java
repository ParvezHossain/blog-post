package com.parvez.blogs.security;

import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key key;
    private long ACCESS_TOKEN_EXPIRATION_MS;
    private long REFRESH_TOKEN_EXPIRATION_MS;

    @Autowired
    public JwtUtil(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiration}") long ACCESS_TOKEN_EXPIRATION_MS,
            @Value("${jwt.refresh-expiration}") long REFRESH_TOKEN_EXPIRATION_MS
    ) {
        byte[] decodedKey = Base64.getDecoder().decode(secretKey);
        this.key = Keys.hmacShaKeyFor(decodedKey);
        this.ACCESS_TOKEN_EXPIRATION_MS = ACCESS_TOKEN_EXPIRATION_MS;
        this.REFRESH_TOKEN_EXPIRATION_MS = REFRESH_TOKEN_EXPIRATION_MS;
    }

    // ------------------- Access Token -------------------
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_MS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ------------------- Refresh Token -------------------
    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_MS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ------------------- Validated Refresh Token -------------------
    public boolean validateRefreshToken(@NotNull  String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseToken(token)
                .getBody()
                .getSubject();
    }

    public String extractRole(String token) {
        return parseToken(token)
                .getBody()
                .get("role", String.class);
    }

    public boolean validateToken(String token) {
        return extractUsername(token).equals(extractRole(token)) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
//        return System.currentTimeMillis() + EXPIRATION_MS < System.currentTimeMillis();
        return parseToken(token).getBody().getExpiration().before(new Date());
    }

    private Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

}