package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad de la API. La aplicación ya no sirve vistas: no hay form login, ni
 * logout, ni página 403 — el front es un proyecto Next.js aparte y esta app solo
 * responde JSON.
 *
 * <p>Los usuarios se cargan desde Postgres vía
 * {@link com.example.demo.service.JpaUserDetailsService} (Spring lo detecta
 * automáticamente como el {@code UserDetailsService} de la app). El seed de
 * {@code admin}/{@code user} lo hace {@link DataSeeder}.
 *
 * <p>HTTP Basic es provisional: permite probar la API con Postman/curl mientras
 * llega el filtro JWT (spec backend 02), que lo sustituye.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Sin cookies ni formularios no hay sesión que falsificar.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
