package com.example.demo.config;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

/**
 * Siembra usuarios iniciales si la tabla está vacía. Conserva las mismas
 * credenciales del antiguo almacén in-memory: {@code admin}/{@code admin123}
 * y {@code user}/{@code user123}. Idempotente.
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedUsers(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (users.count() > 0) {
                return;
            }
            users.save(new User("admin", "admin@bitacora.local",
                    encoder.encode("admin123"),
                    Set.of(Role.ROLE_ADMIN, Role.ROLE_USER)));
            users.save(new User("user", "user@bitacora.local",
                    encoder.encode("user123"),
                    Set.of(Role.ROLE_USER)));
        };
    }
}
