package com.example.demo.service;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.repository.UserRepository;

/**
 * Carga usuarios desde Postgres para Spring Security. Lo usan ambas cadenas de
 * seguridad (form login Thymeleaf y, más adelante, el filtro JWT de la API).
 */
@Service
public class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public JpaUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername())
                        .password(u.getPassword())
                        .disabled(!u.isEnabled())
                        .authorities(u.getRoles().stream()
                                .map(r -> new SimpleGrantedAuthority(r.name()))
                                .toList())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + username));
    }
}
