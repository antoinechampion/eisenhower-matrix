package com.ach.eisenhower;

import com.ach.eisenhower.entities.PasswordResetTokenEntity;
import com.ach.eisenhower.repositories.EisenhowerUserRepository;
import com.ach.eisenhower.repositories.PasswordResetTokenRepository;
import com.ach.eisenhower.services.EmailService;
import com.ach.eisenhower.services.PasswordResetService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class PasswordResetTest extends EisenhowerBaseTest {
    @Autowired
    private PasswordResetService passwordResetService;
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    @Autowired
    private EisenhowerUserRepository userRepository;
    @Autowired
    private PasswordEncoder encoder;

    // Prevent real SMTP sends and let us capture the emitted raw token.
    @MockBean
    private EmailService emailService;

    @Test
    public void testRequestResetForUnknownEmailSendsNothing() {
        passwordResetService.requestReset(RandomStringUtils.randomAlphabetic(8) + "@nobody.com");

        verifyNoInteractions(emailService);
        assertEquals(0, tokenRepository.count());
    }

    @Test
    public void testResetPasswordHappyPath() {
        var user = userTestingUtils.createTestUser(email(), "oldpassword");

        passwordResetService.requestReset(user.getEmail());
        var rawToken = captureEmailedToken(user.getEmail());

        passwordResetService.resetPassword(rawToken, "newpassword");

        var updated = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertTrue(encoder.matches("newpassword", updated.getPassword()));
        // Single-use: the token row is deleted after a successful reset.
        assertEquals(0, tokenRepository.count());
    }

    @Test
    public void testResetPasswordWithReusedTokenFails() {
        var user = userTestingUtils.createTestUser(email(), "oldpassword");
        passwordResetService.requestReset(user.getEmail());
        var rawToken = captureEmailedToken(user.getEmail());

        passwordResetService.resetPassword(rawToken, "newpassword");

        assertThrows(ResponseStatusException.class,
                () -> passwordResetService.resetPassword(rawToken, "another"));
    }

    @Test
    public void testResetPasswordWithExpiredTokenFails() {
        var user = userTestingUtils.createTestUser(email(), "oldpassword");
        passwordResetService.requestReset(user.getEmail());
        var rawToken = captureEmailedToken(user.getEmail());

        // Force the persisted token to be already expired.
        var token = tokenRepository.findAll().get(0);
        token.setExpiryDate(new Date(System.currentTimeMillis() - 1000));
        tokenRepository.save(token);

        assertThrows(ResponseStatusException.class,
                () -> passwordResetService.resetPassword(rawToken, "newpassword"));
    }

    @Test
    public void testRequestResetTwiceSupersedesPriorToken() {
        var user = userTestingUtils.createTestUser(email(), "oldpassword");

        passwordResetService.requestReset(user.getEmail());
        var firstToken = captureEmailedToken(user.getEmail());
        passwordResetService.requestReset(user.getEmail());

        // At most one live token per user.
        assertEquals(1, tokenRepository.count());
        // The first (superseded) token no longer works.
        assertThrows(ResponseStatusException.class,
                () -> passwordResetService.resetPassword(firstToken, "newpassword"));
    }

    @Test
    public void testDeletingUserDeletesItsToken() {
        var user = userTestingUtils.createTestUser(email(), "oldpassword");
        passwordResetService.requestReset(user.getEmail());
        assertEquals(1, tokenRepository.count());

        userRepository.delete(user);

        assertEquals(0, tokenRepository.count());
    }

    /**
     * Captures the raw token passed to the (mocked) EmailService for the given address.
     */
    private String captureEmailedToken(String email) {
        var tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendPasswordResetEmail(eq(email), tokenCaptor.capture());
        return tokenCaptor.getValue();
    }

    private String email() {
        return RandomStringUtils.randomAlphabetic(8) + "@test.com";
    }
}
