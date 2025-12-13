package com.authservice.security;

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
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    private boolean isAuthPath(HttpServletRequest req) {
        String uri = safe(req::getRequestURI);
        String servlet = safe(req::getServletPath);
        String pathInfo = safe(req::getPathInfo);
        String combined = (uri + "|" + servlet + "|" + pathInfo).toLowerCase(Locale.ROOT);
        return combined.contains("/auth/") || combined.contains("/actuator/");
    }

    private static String safe(java.util.function.Supplier<String> s) {
        try { String v = s.get(); return v == null ? "" : v; } catch (Exception e) { return ""; }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // skip auth endpoints
        if (isAuthPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                String username = jwtUtils.extractUsername(token);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtUtils.validateToken(token, userDetails)) {
                        var authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception ex) {
            logger.debug("JWT processing issue: " + ex.getMessage(), ex);
            // do not abort - let security handle auth for protected endpoints
        }

        filterChain.doFilter(request, response);
    }
}