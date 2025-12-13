package com.authservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expirationSeconds:3600}")
    private long jwtExpirationSeconds;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateJwtToken(UserDetailsImpl user) {
        long now = System.currentTimeMillis();
        List<String> roles = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        // Use email as subject
        return Jwts.builder()
                .setSubject(user.getEmail())    // <-- changed to email
                .claim("roles", roles)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + jwtExpirationSeconds * 1000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
        } catch (JwtException ex) {
            // invalid token
            return null;
        }
    }

    /**
     * Validate token against a UserDetails (interface) rather than concrete impl.
     * This keeps the API generic and avoids casting in callers.
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            var claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            String subject = claims.getSubject();
            Date expiration = claims.getExpiration();

            // If userDetails is our implementation, compare with its email. Otherwise fall back to username.
            String compareWith = userDetails instanceof UserDetailsImpl
                    ? ((UserDetailsImpl) userDetails).getEmail()
                    : userDetails.getUsername();

            return (subject != null && subject.equals(compareWith) && expiration.after(new Date()));
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}