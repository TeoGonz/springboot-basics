package com.example.demo.dto;

import java.util.List;

/**
 * Respuesta del login. {@code expiresInMs} evita que el cliente tenga que
 * decodificar el token para saber cuánto dura la cookie de sesión.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        String username,
        List<String> roles) {
}
