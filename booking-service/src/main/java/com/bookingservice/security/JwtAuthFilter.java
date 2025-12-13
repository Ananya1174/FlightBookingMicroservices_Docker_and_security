package com.bookingservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    // keep this property name same as used elsewhere (auth-service/gateway)
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);

            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String username = claims.getSubject(); // we assume username == email
                @SuppressWarnings("unchecked")
                List<String> roles = claims.get("roles", List.class);

                // Ensure roles are prefixed with ROLE_ if your tokens carry "USER","ADMIN"
                // but if tokens already contain "ROLE_USER" leave them as-is.
                List<String> normalizedRoles = roles.stream()
                        .map(r -> r.startsWith("ROLE_") ? r : ("ROLE_" + r))
                        .toList();

                // Create CustomUserDetails and Authentication token
                CustomUserDetails userDetails = new CustomUserDetails(username, normalizedRoles);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);

            } catch (Exception ex) {
                // token invalid/expired — let it fall through (Security will reject if required)
                logger.warn("JWT parse/validate failed: " + ex.getMessage());
                // optionally: response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token"); return;
            }
        }

        filterChain.doFilter(request, response);
    }
}