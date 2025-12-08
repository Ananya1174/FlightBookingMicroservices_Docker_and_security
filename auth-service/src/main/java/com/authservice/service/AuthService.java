package com.authservice.service;
import org.springframework.stereotype.Service;
import com.authservice.repo.UserRepository;
import com.authservice.model.User;
import com.authservice.jwt.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder;

    @Value("${jwt.expirationSeconds}")
    private long expirySeconds;

    public AuthService(UserRepository userRepo, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
        this.encoder = new BCryptPasswordEncoder();
    }

    public User createUser(String email, String rawPassword, String role) {
        User u = User.builder()
                .email(email)
                .password(encoder.encode(rawPassword))
                .role(role)
                .build();
        return userRepo.save(u);
    }

    public String loginAndGetToken(String email, String rawPassword) {
        User u = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!encoder.matches(rawPassword, u.getPassword())) throw new RuntimeException("Invalid credentials");
        return jwtUtil.generateToken(email, u.getRole());
    }

    public long getExpirySeconds() { return expirySeconds; }
}