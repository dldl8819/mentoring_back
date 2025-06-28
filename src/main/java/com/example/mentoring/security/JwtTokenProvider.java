package com.example.mentoring.security;

import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
    // 임시로 간단한 토큰 제공자 구현
    // 실제 JWT 대신 단순한 문자열 토큰 사용
    
    public String generateToken(String email) {
        return "simple-token-" + email + "-" + System.currentTimeMillis();
    }
    
    public boolean validateToken(String token) {
        return token != null && token.startsWith("simple-token-");
    }
    
    public String getEmailFromToken(String token) {
        if (token != null && token.startsWith("simple-token-")) {
            String[] parts = token.split("-");
            if (parts.length >= 3) {
                return parts[2];
            }
        }
        return null;
    }
}
