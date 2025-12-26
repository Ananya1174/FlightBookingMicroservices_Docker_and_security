package com.authservice.controller;

import com.authservice.model.User;
import com.authservice.payload.request.ChangePasswordRequest;
import com.authservice.payload.request.ForgotPasswordRequest;
import com.authservice.payload.request.LoginRequest;
import com.authservice.payload.request.ResetPasswordRequest;
import com.authservice.payload.request.SignupRequest;
import com.authservice.payload.response.JwtResponse;
import com.authservice.repository.RoleRepository;
import com.authservice.repository.UserRepository;
import com.authservice.security.JwtUtils;
import com.authservice.security.UserDetailsImpl;
import com.authservice.service.PasswordResetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.authservice.security.PasswordPolicyValidator;
import java.time.Duration;
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final PasswordResetService passwordResetService;

    // ---------------- SIGNUP ----------------
    @PostMapping("/signup")
    public ResponseEntity<String> register(@RequestBody SignupRequest req) {
        if (userRepo.existsByUsername(req.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        }

        if (userRepo.existsByEmail(req.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setPasswordLastChangedAt(Instant.now());
        user.setPasswordChangeRequired(false);
        
        Set<com.authservice.model.Role> roles = new HashSet<>();

        if (req.getRole() == null) {
            roles.add(roleRepo.findByName(com.authservice.model.ERole.ROLE_USER).orElseThrow());
        } else {
            req.getRole().forEach(r -> {
                if (r.equalsIgnoreCase("admin")) {
                    roles.add(roleRepo.findByName(com.authservice.model.ERole.ROLE_ADMIN).orElseThrow());
                } else {
                    roles.add(roleRepo.findByName(com.authservice.model.ERole.ROLE_USER).orElseThrow());
                }
            });
        }

        user.setRoles(roles);
        userRepo.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully!");
    }

    // ---------------- SIGNIN ----------------
    @PostMapping("/signin")
    public JwtResponse login(@RequestBody LoginRequest req) {

        var auth = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        User user = userRepo.findByEmail(userDetails.getEmail()).orElseThrow();

        long days =
            Duration.between(user.getPasswordLastChangedAt(), Instant.now()).toDays();

        boolean passwordExpired = days >= 90;

        if (passwordExpired) {
            user.setPasswordChangeRequired(true);
            userRepo.save(user);
        }

        String token = jwtUtils.generateJwtToken(userDetails, passwordExpired);

        return new JwtResponse(
            token,
            "Bearer",
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            userDetails.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .toList(),
            passwordExpired
        );
    }

    // ---------------- CHANGE PASSWORD ----------------
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestBody ChangePasswordRequest request) {

        User user = userRepo.findByEmail(principal.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!encoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("Current password is incorrect");
        }

        if (encoder.matches(request.getNewPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("New password must not be same as old password");
        }

        PasswordPolicyValidator.validate(request.getNewPassword());

        user.setPassword(encoder.encode(request.getNewPassword()));
        user.setPasswordLastChangedAt(Instant.now());
        user.setPasswordChangeRequired(false);

        userRepo.save(user);

        return ResponseEntity.ok("Password changed successfully. Please login again.");
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        passwordResetService.createResetToken(request.getEmail());
        return ResponseEntity.ok(
            "If the email exists, a password reset link has been sent."
        );
    }
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetPassword(
            request.getToken(),
            request.getNewPassword()
        );

        return ResponseEntity.ok("Password reset successful. Please login.");
    }
}