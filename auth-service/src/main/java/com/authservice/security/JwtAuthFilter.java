package com.authservice.security;

import com.authservice.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    // ---------- PUBLIC ENDPOINTS ----------
    private boolean isPublicEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI().toLowerCase();

        return uri.equals("/auth/signin")
                || uri.equals("/auth/signup")
                || uri.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // ✅ Skip JWT processing for public endpoints
        if (isPublicEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String header = request.getHeader("Authorization");

            if (header != null && header.startsWith("Bearer ")) {

                String token = header.substring(7);
                String email = jwtUtils.extractUsername(token);

                if (email != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                    // 🔐 Load full user entity
                    User user = userDetailsService.getUserByEmail(email);

                    // 🔐 OPTION A: FORCE PASSWORD CHANGE AFTER EXPIRY
                    if (user.isPasswordChangeRequired()
                            && !request.getRequestURI().equals("/auth/change-password")) {

                        response.sendError(
                                HttpServletResponse.SC_FORBIDDEN,
                                "Password expired. Please change your password."
                        );
                        return;
                    }

                    var userDetails = userDetailsService.loadUserByUsername(email);

                    if (jwtUtils.validateToken(token, userDetails)) {

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception ex) {
            logger.debug("JWT processing failed: " + ex.getMessage(), ex);
        }

        filterChain.doFilter(request, response);
    }
}