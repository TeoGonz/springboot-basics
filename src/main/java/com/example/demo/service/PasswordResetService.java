package com.example.demo.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.PasswordResetToken;
import com.example.demo.entity.User;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.web.ApiException;

/**
 * Recuperación de contraseña: emisión del enlace y canje del token.
 *
 * <p>El token viaja en el correo; en la base solo queda su hash SHA-256. SHA-256
 * a secas basta aquí — al contrario que una contraseña, es un valor aleatorio de
 * 256 bits y ya, así que no hay nada que adivinar por fuerza bruta y no hace
 * falta el coste deliberado de BCrypt.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final Duration lifetime;
    private final String frontendBaseUrl;

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
            PasswordEncoder passwordEncoder, MailService mailService,
            @Value("${app.password-reset.expiration-ms}") long expirationMs,
            @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.lifetime = Duration.ofMillis(expirationMs);
        this.frontendBaseUrl = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
    }

    /**
     * Emite un enlace si el correo corresponde a una cuenta activa. No informa
     * de nada al llamante: quien pregunta no debe poder averiguar si una
     * dirección está registrada.
     */
    @Transactional
    public void requestReset(String email, String localeTag) {
        Optional<User> found = users.findByEmail(email.trim().toLowerCase());
        if (found.isEmpty() || !found.get().isEnabled()) {
            log.info("Solicitud de recuperación para una dirección sin cuenta activa; se ignora");
            return;
        }
        User user = found.get();

        Instant now = Instant.now();
        tokens.invalidateAllForUser(user.getId(), now);

        String token = newToken();
        tokens.save(new PasswordResetToken(sha256Hex(token), user, now.plus(lifetime)));

        String locale = (localeTag == null || localeTag.isBlank()) ? "es" : localeTag;
        String link = "%s/%s/reset-password?token=%s".formatted(
                frontendBaseUrl, locale, URLEncoder.encode(token, StandardCharsets.UTF_8));

        mailService.sendPasswordResetLink(user.getEmail(), user.getUsername(), link,
                (int) lifetime.toMinutes(), Locale.forLanguageTag(locale));
    }

    /** Canjea el token por una contraseña nueva. El token queda gastado. */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken stored = tokens.findByTokenHash(sha256Hex(token))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN",
                        "unknown reset token"));

        if (stored.isUsed()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN",
                    "reset token already used");
        }
        Instant now = Instant.now();
        if (stored.isExpired(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EXPIRED_TOKEN",
                    "reset token expired");
        }

        User user = stored.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        users.save(user);

        stored.setUsedAt(now);
        tokens.save(stored);
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 es obligatorio en toda JVM; si falta, algo va muy mal.
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
