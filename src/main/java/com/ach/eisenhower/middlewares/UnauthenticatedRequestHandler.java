package com.ach.eisenhower.middlewares;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Handles unauthenticated access to various parts of the application
 */
public class UnauthenticatedRequestHandler implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        if (request.getRequestURI().startsWith("/api/")) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
        }
        else if (request.getRequestURI().startsWith("/auth/")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
        else {
            response.sendRedirect("/");
        }
    }
}