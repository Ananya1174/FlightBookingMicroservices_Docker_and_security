package com.authservice.security;

import java.util.regex.Pattern;

public final class PasswordPolicyValidator {

    private PasswordPolicyValidator() {}

    private static final Pattern STRONG_PASSWORD =
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{12,}$");

    public static void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        if (!STRONG_PASSWORD.matcher(password).matches()) {
            throw new IllegalArgumentException(
                "Password must be at least 12 characters and include uppercase, lowercase, number, and special character"
            );
        }
    }
}