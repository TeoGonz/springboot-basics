package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales de acceso. Se entra con el nombre de usuario, no con el correo:
 * el correo existe para poder recuperar la contraseña.
 */
public record LoginRequest(

        @NotBlank String username,

        @NotBlank String password) {
}
