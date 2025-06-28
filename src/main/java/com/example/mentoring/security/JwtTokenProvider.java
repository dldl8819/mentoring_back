package com.example.mentoring.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    
    private final SecretKey secretKey;
    private final String issuer = "mentoring-app";
    private final String audience = "mentoring-users";
    
    public JwtTokenProvider(@Value("${jwt.secret:mySecretKeyForJWTTokenGeneration123456789}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }
    
    public String generateToken(String email, String name, String role) {
        Instant now = Instant.now();
        Instant expiration = now.plus(1, ChronoUnit.HOURS); // 1시간 유효기간
        
        return Jwts.builder()
                // RFC 7519 표준 클레임들
                .setIssuer(issuer)                           // iss
                .setSubject(email)                          // sub (사용자 이메일)
                .setAudience(audience)                      // aud
                .setExpiration(Date.from(expiration))       // exp (1시간 후)
                .setNotBefore(Date.from(now))              // nbf (현재 시각부터 유효)
                .setIssuedAt(Date.from(now))               // iat (발급 시각)
                .setId(UUID.randomUUID().toString())        // jti (고유 ID)
                
                // 커스텀 클레임들
                .claim("name", name)
                .claim("email", email)
                .claim("role", role)
                
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    public String getEmailFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? claims.get("email", String.class) : null;
    }
    
    public String getNameFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? claims.get("name", String.class) : null;
    }
    
    public String getRoleFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? claims.get("role", String.class) : null;
    }
    
    private Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
