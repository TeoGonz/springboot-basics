package com.example.demo.entity;

/**
 * Roles de la aplicación. El prefijo {@code ROLE_} es el que espera Spring
 * Security al construir las authorities; en {@code SecurityConfig} se usa
 * {@code hasRole("ADMIN")} (Spring añade el prefijo automáticamente).
 */
public enum Role {
    ROLE_USER,
    ROLE_ADMIN
}
