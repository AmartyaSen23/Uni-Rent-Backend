package com.campusshare.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct; // ✅ ADD THIS
import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    // 1. Inject the secret string from application.properties
    @Value("${jwt.secret}")
    private String secretString;

    private Key key;
    private final long EXPIRATION_TIME = 1000 * 60 * 60 * 10; // 10 Hours

    // 2. Initialize the Key object ONCE when the app starts
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }

    // 3. GENERATE TOKEN
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 4. EXTRACT EMAIL
    public String extractEmail(String token) {
        if (token.startsWith("Bearer ")) token = token.substring(7);
        
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            return null; // Invalid token
        }
    }

    public boolean validateToken(String token) {
        return extractEmail(token) != null;
    }
}