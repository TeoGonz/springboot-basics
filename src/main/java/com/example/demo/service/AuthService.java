package com.example.demo.service;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import com.example.demo.web.ApiException;

/** Alta de usuarios y emisión de tokens. */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Crea el usuario con {@code ROLE_USER}. No devuelve token: el registro y el
     * login son dos pasos, y el front encadena el segundo.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (users.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_TAKEN",
                    "username already registered");
        }
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN",
                    "email already registered");
        }

        User saved = users.save(new User(username, email,
                passwordEncoder.encode(request.password()),
                Set.of(Role.ROLE_USER)));

        return new UserResponse(saved.getId(), saved.getUsername(), saved.getEmail(),
                saved.getRoles().stream().map(Enum::name).sorted().toList());
    }

    public AuthResponse login(LoginRequest request) {
        UserDetails principal;
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            principal = (UserDetails) authentication.getPrincipal();
        } catch (AuthenticationException ex) {
            // Mismo código para usuario inexistente, contraseña errónea y cuenta
            // deshabilitada: distinguirlos diría de más a quien prueba a ciegas.
            throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS",
                    "invalid username or password");
        }

        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();

        return new AuthResponse(jwtService.generateToken(principal), "Bearer",
                jwtService.getExpirationMs(), principal.getUsername(), roles);
    }
}
