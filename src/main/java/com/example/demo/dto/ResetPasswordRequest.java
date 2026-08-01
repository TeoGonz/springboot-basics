package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Canje del enlace de recuperación por una contraseña nueva. */
public record ResetPasswordRequest(

        @NotBlank String token,

        @NotBlank @Size(min = 8, max = 100) String password) {
}
