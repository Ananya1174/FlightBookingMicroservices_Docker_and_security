package com.authservice.service;

import com.authservice.message.EmailPublisher;

import com.authservice.model.PasswordResetToken;
import com.authservice.model.User;
import com.authservice.repository.PasswordResetTokenRepository;
import com.authservice.repository.UserRepository;
import com.authservice.security.PasswordPolicyValidator;
import com.flightapp.message.EmailMessage;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration TOKEN_VALIDITY = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailPublisher emailPublisher;

    /**
     * STEP 2.1 – Create reset token
     */
    @Transactional
    public PasswordResetToken createResetToken(String email) {

        User user = userRepository.findByEmail(email).orElse(null);

        // 🔐 Do not reveal whether user exists
        if (user == null) {
            return null;
        }

        // Invalidate existing tokens
        tokenRepository.deleteByUser(user);

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(Instant.now().plus(TOKEN_VALIDITY));
        token.setUsed(false);

        PasswordResetToken savedToken = tokenRepository.save(token);

        emailPublisher.publishPasswordReset(
            new EmailMessage(
                user.getEmail(),
                "Reset Your Password",
                """
                Click the link below to reset your password:

                http://localhost:4200/reset-password?token=%s

                This link expires in 15 minutes.
                """.formatted(savedToken.getToken())
            )
        );

        return savedToken;
    }

    /**
     * STEP 2.2 – Validate reset token
     */
    public PasswordResetToken validateToken(String tokenValue) {

        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid password reset token"));

        if (token.isUsed()) {
            throw new IllegalStateException("Token already used");
        }

        if (token.isExpired()) {
            throw new IllegalStateException("Token expired");
        }

        return token;
    }

    /**
     * STEP 2.3 – Reset password
     */
    @Transactional
    public void resetPassword(String tokenValue, String newPassword) {

        PasswordResetToken token = validateToken(tokenValue);
        User user = token.getUser();

        // Password strength validation
        PasswordPolicyValidator.validate(newPassword);

        // Prevent same password reuse
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password must be different");
        }

        // Update user password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordLastChangedAt(Instant.now());
        user.setPasswordChangeRequired(false);

        userRepository.save(user);

        // Mark token as used
        token.setUsed(true);
        tokenRepository.save(token);
    }
}