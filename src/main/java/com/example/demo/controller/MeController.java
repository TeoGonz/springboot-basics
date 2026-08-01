package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.repository.UserRepository;

/**
 * Datos del usuario autenticado. Es el endpoint mínimo para comprobar que la
 * autenticación funciona desde Postman/curl; el spec backend 02 lo mantiene tal
 * cual cuando la autenticación pase a JWT.
 */
@RestController
@RequestMapping("/api")
public class MeController {

    private final UserRepository userRepository;

    public MeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .map(user -> new MeResponse(
                        user.getUsername(),
                        user.isEnabled(),
                        user.getRoles().stream().map(Enum::name).sorted().toList()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuario no encontrado: " + authentication.getName()));
    }

    /** Nunca expone el hash de la contraseña. */
    public record MeResponse(String username, boolean enabled, List<String> roles) {
    }
}
