package com.authservice.security;

import com.authservice.model.User;
import com.authservice.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expirationSeconds:3600}")
    private long jwtExpirationSeconds;

    private final UserRepository userRepository;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // ================= TOKEN GENERATION =================

    public String generateJwtToken(UserDetailsImpl user, boolean passwordExpired) {

        long now = System.currentTimeMillis();

        List<String> roles = user.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("roles", roles)
                .claim("pwd_expired", passwordExpired) // 🔐 KEY
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + jwtExpirationSeconds * 1000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ================= TOKEN EXTRACTION =================

    public String extractUsername(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (JwtException ex) {
            return null;
        }
    }

    // ================= TOKEN VALIDATION =================

    public boolean validateToken(String token, UserDetails userDetails) {

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String subject = claims.getSubject();
            Date expiration = claims.getExpiration();
            Date issuedAt = claims.getIssuedAt();

            if (subject == null || expiration.before(new Date())) {
                return false;
            }

            // match email
            String emailFromUserDetails =
                    userDetails instanceof UserDetailsImpl
                            ? ((UserDetailsImpl) userDetails).getEmail()
                            : userDetails.getUsername();

            if (!subject.equals(emailFromUserDetails)) {
                return false;
            }

            // 🔐 PASSWORD CHANGE INVALIDATION (OPTION 1)
            User user = userRepository.findByEmail(subject).orElse(null);
            if (user == null) {
                return false;
            }

            Instant passwordChangedAt = user.getPasswordLastChangedAt();
            if (passwordChangedAt != null && issuedAt != null) {
                if (issuedAt.toInstant().isBefore(passwordChangedAt)) {
                    return false; // token issued before password change
                }
            }

            return true;

        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}