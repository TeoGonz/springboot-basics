package com.example.demo.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.example.demo.dto.ApiError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
// Spring Boot 4 trae Jackson 3: el ObjectMapper vive en tools.jackson.
import tools.jackson.databind.ObjectMapper;

/**
 * Respuesta a una petición sin autenticar. Devuelve JSON en lugar de la
 * redirección a {@code /login} que Spring haría por defecto: aquí no hay
 * páginas a las que redirigir.
 */
@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ApiError(
                HttpStatus.UNAUTHORIZED.value(), "UNAUTHENTICATED", "missing or invalid token"));
    }
}
