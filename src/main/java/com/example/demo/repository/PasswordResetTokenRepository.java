package com.example.demo.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Invalida los enlaces anteriores del usuario. Se llama antes de emitir uno
     * nuevo: si alguien pide otro correo, el anterior deja de servir.
     */
    @Modifying
    @Query("""
            update PasswordResetToken t
               set t.usedAt = :now
             where t.user.id = :userId
               and t.usedAt is null
            """)
    int invalidateAllForUser(@Param("userId") Long userId, @Param("now") Instant now);
}
