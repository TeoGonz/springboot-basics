package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.demo.security.JwtAuthFilter;
import com.example.demo.security.RestAccessDeniedHandler;
import com.example.demo.security.RestAuthEntryPoint;
import com.example.demo.service.JpaUserDetailsService;

/**
 * Seguridad de la API. La aplicación no sirve vistas: no hay form login, ni
 * logout, ni página 403 — el front es un proyecto Next.js aparte y esta app solo
 * responde JSON.
 *
 * <p>La autenticación es por <b>token JWT</b> en la cabecera
 * {@code Authorization: Bearer …}, que emite {@code /api/auth/login}. El HTTP
 * Basic provisional que quedó al retirar Thymeleaf ya no existe.
 *
 * <p>Sin CORS a propósito: quien llama a la API es el proceso Node de Next
 * (Server Actions), no el navegador, así que no hay origen cruzado que
 * autorizar. El día que un navegador llame directo, ese será el momento de
 * añadir el bean — no antes.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Lo necesita {@code /api/auth/login} para comprobar las credenciales. En
     * Spring Security 7 el {@code UserDetailsService} va en el constructor del
     * proveedor.
     */
    @Bean
    public AuthenticationManager authenticationManager(JpaUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtAuthFilter jwtAuthFilter,
            RestAuthEntryPoint authEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                // Sin cookies ni formularios no hay sesión que falsificar.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Registro, login y recuperación: hay que poder llamarlos sin token.
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
