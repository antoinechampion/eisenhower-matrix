package com.ach.eisenhower.services;

import com.ach.eisenhower.entities.PasswordResetTokenEntity;
import com.ach.eisenhower.repositories.EisenhowerUserRepository;
import com.ach.eisenhower.repositories.PasswordResetTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

/**
 * Issues and consumes single-use, time-limited password reset tokens.
 * Only the SHA-256 hash of a token is persisted; the raw token lives solely in the emailed link.
 */
@Component
public class PasswordResetService {
    private static final long TOKEN_VALIDITY_MS = 60 * 60 * 1000; // 1 hour

    private final PasswordResetTokenRepository tokens;
    private final EisenhowerUserRepository users;
    private final PasswordEncoder encoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(PasswordResetTokenRepository tokens, EisenhowerUserRepository users,
                                PasswordEncoder encoder, EmailService emailService) {
        this.tokens = tokens;
        this.users = users;
        this.encoder = encoder;
        this.emailService = emailService;
    }

    /**
     * Issues a reset token and emails the link, but only if the account exists.
     * Never reveals whether the email is registered (anti-enumeration).
     */
    @Transactional
    public void requestReset(String email) {
        var user = users.findByEmail(email);
        if (user.isEmpty()) {
            return;
        }

        // Supersede any prior token so at most one is live per user. Flush the delete
        // before the insert, otherwise Hibernate orders the INSERT first and the unique
        // user_id constraint is violated.
        tokens.deleteByUser(user.get());
        tokens.flush();

        var rawToken = generateRawToken();
        var token = PasswordResetTokenEntity.builder()
                .tokenHash(hash(rawToken))
                .user(user.get())
                .expiryDate(new Date(System.currentTimeMillis() + TOKEN_VALIDITY_MS))
                .build();
        tokens.save(token);

        emailService.sendPasswordResetEmail(email, rawToken);
    }

    /**
     * Consumes a reset token and sets the new password. The token is single-use: it is
     * deleted on success. Throws 400 on an invalid or expired token.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        var token = tokens.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset link"));

        if (token.getExpiryDate().before(new Date())) {
            tokens.delete(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset link");
        }

        var user = token.getUser();
        user.setPassword(encoder.encode(newPassword));
        users.save(user);
        tokens.delete(token);
    }

    private String generateRawToken() {
        var bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
