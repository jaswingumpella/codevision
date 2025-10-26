package com.codevision.codevisionbackend.security;

import com.codevision.codevisionbackend.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";
    private final SecurityProperties securityProperties;

    public ApiKeyFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String expectedKey = securityProperties.getApiKey();
        if (expectedKey == null || expectedKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String suppliedKey = request.getHeader(API_KEY_HEADER);
        if (expectedKey.equals(suppliedKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write("Unauthorized");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/error") || "GET".equalsIgnoreCase(request.getMethod());
    }
}
