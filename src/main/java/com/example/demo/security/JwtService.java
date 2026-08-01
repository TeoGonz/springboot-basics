package com.example.demo.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Emite y valida los tokens de acceso. HS256 con un secreto compartido: no hay
 * pares de claves porque quien firma y quien verifica son el mismo servicio.
 *
 * <p>Sin token de refresco: un solo token de acceso con caducidad corta. Mientras
 * no caduque no hay forma de revocarlo — es el precio de una sesión sin estado.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            // Mejor no arrancar que firmar con una clave débil.
            throw new IllegalStateException(
                    "app.jwt.secret debe tener al menos 32 bytes; tiene " + bytes.length);
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public String generateToken(UserDetails user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("roles", user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * Nombre de usuario del token, o {@code null} si la firma no cuadra, el
     * formato es basura o ya caducó. Un token inválido no es una excepción que
     * deba propagarse: es simplemente una petición sin autenticar.
     */
    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            Object roles = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload().get("roles");
            return roles instanceof List<?> list ? (List<String>) list : List.of();
        } catch (JwtException | IllegalArgumentException ex) {
            return List.of();
        }
    }
}
