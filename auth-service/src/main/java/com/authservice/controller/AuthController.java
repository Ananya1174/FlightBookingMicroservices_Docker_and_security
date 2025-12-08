package com.authservice.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.authservice.dto.*;
import com.authservice.repo.UserRepository;
import com.authservice.model.User;
import com.authservice.service.AuthService;
import com.authservice.service.TokenBlacklistService;
import com.authservice.jwt.JwtUtil;
import jakarta.validation.Valid;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;

    @Autowired
    public AuthController(UserRepository userRepo, AuthService authService, JwtUtil jwtUtil, TokenBlacklistService blacklistService) {
        this.userRepo = userRepo;
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.blacklistService = blacklistService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) return ResponseEntity.badRequest().body(Map.of("error","email exists"));
        String role = (req.getRole() == null || req.getRole().isBlank()) ? "ROLE_USER" : req.getRole();
        User u = authService.createUser(req.getEmail(), req.getPassword(), role);
        return ResponseEntity.status(201).body(Map.of("message", "created", "email", u.getEmail()));
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@Valid @RequestBody com.authservice.dto.AuthRequest req) {
        try {
            String token = authService.loginAndGetToken(req.getEmail(), req.getPassword());
            return ResponseEntity.ok(new AuthResponse(token, authService.getExpirySeconds(), userRepo.findByEmail(req.getEmail()).get().getRole()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).body(Map.of("error","invalid credentials"));
        }
    }

    @PostMapping("/signout")
    public ResponseEntity<?> signout(@RequestHeader(name="Authorization", required=false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return ResponseEntity.badRequest().body(Map.of("error","no token"));
        String token = authHeader.substring(7);
        try {
            Jws<Claims> parsed = jwtUtil.parseToken(token);
            String jti = parsed.getBody().getId();
            Instant exp = parsed.getBody().getExpiration().toInstant();
            blacklistService.blacklist(jti, exp);
            return ResponseEntity.ok(Map.of("message","signed out"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error","invalid token"));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(@RequestHeader(name="Authorization", required=false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(new ValidateResponse(false, null, null, null));
        }
        String token = authHeader.substring(7);
        try {
            Jws<Claims> parsed = jwtUtil.parseToken(token);
            String jti = parsed.getBody().getId();
            if (blacklistService.isBlacklisted(jti)) {
                return ResponseEntity.ok(new ValidateResponse(false, null, null, jti));
            }
            String email = parsed.getBody().getSubject();
            String role = parsed.getBody().get("roles", String.class);
            return ResponseEntity.ok(new ValidateResponse(true, email, role, jti));
        } catch (Exception e) {
            return ResponseEntity.ok(new ValidateResponse(false, null, null, null));
        }
    }
}