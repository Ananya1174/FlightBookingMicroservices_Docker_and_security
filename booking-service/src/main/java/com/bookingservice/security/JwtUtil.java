package com.bookingservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private Key signingKey;

    @PostConstruct
    public void init() {
        signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    private Key getSignKey() {
        return signingKey;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract roles claim robustly. Returns empty list if missing or not a List.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            return extractClaim(token, claims -> {
                Object raw = claims.get("roles");
                if (raw instanceof List<?>) {
                    // convert elements to strings
                    return ((List<?>) raw).stream().map(Object::toString).toList();
                }
                // fallback: maybe single string under 'role' or 'roles'
                Object alt = claims.get("role");
                if (alt != null) return List.of(alt.toString());
                return Collections.emptyList();
            });
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token).getBody();
        return resolver.apply(claims);
    }
}