package com.example.demo.dto;

import java.util.List;

/** Usuario recién creado. Sin hash de contraseña, como cualquier DTO de salida. */
public record UserResponse(Long id, String username, String email, List<String> roles) {
}
