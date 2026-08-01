package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Petición de enlace de recuperación. {@code locale} solo elige el idioma del
 * correo; si no llega, se envía en español.
 */
public record ForgotPasswordRequest(

        @NotBlank @Email String email,

        @Pattern(regexp = "es|en|pt", message = "idioma no soportado") String locale) {
}
