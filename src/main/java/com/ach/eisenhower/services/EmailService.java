package com.ach.eisenhower.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Sends transactional emails over SMTP (Scaleway Transactional Email).
 */
@Component
public class EmailService {
    private final JavaMailSender mailSender;
    private final String from;
    private final String baseUrl;
    private final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public EmailService(JavaMailSender mailSender,
                        @Value("${eisenhower.mail.from:Eisenhower Matrix <no-reply@eisenhowermatrix.net>}") String from,
                        @Value("${eisenhower.app.base-url:http://localhost:8080}") String baseUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.baseUrl = baseUrl;
    }

    /**
     * Sends the password-reset link to the given address. Runs asynchronously so SMTP
     * latency never blocks the HTTP response (and never reveals whether the account exists).
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        var resetUrl = baseUrl + "/?resetToken=" + rawToken;
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("Reset your Eisenhower Matrix password");
        message.setText(
                "You requested to reset your password.\n\n" +
                "Click the link below to choose a new password. This link expires in 1 hour:\n\n" +
                resetUrl + "\n\n" +
                "If you did not request this, you can safely ignore this email."
        );
        try {
            mailSender.send(message);
        } catch (MailException e) {
            logger.atError().log("Failed to send password reset email: " + e.getMessage());
        }
    }
}
