package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta de usuario. Las mismas reglas están repetidas en el front como ayuda al
 * escribir; la validación que manda es esta.
 */
public record RegisterRequest(

        @NotBlank
        @Size(min = 3, max = 30)
        @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "solo letras, dígitos y . _ -")
        String username,

        @NotBlank
        @Email
        @Size(max = 180)
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password) {
}
