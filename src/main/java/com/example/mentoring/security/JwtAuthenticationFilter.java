package com.example.mentoring.security;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // 임시로 간단한 필터 구현
    // 실제로는 JWT 토큰을 검증하지만, 현재는 모든 요청을 통과시킴
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 현재는 모든 요청을 그대로 통과
        filterChain.doFilter(request, response);
    }
}
