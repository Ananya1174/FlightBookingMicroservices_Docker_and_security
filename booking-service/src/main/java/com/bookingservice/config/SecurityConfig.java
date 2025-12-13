package com.bookingservice.config;

import com.bookingservice.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity // enables @PreAuthorize
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
          .csrf(csrf -> csrf.disable())
          .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(auth -> auth
              // allow actuator and health checks
              .requestMatchers("/actuator/**").permitAll()

              // Our controllers live under /api/flight
              .requestMatchers("/api/flight/booking/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
              .requestMatchers("/api/flight/ticket/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
              .requestMatchers("/api/flight/booking/history/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
              .requestMatchers("/api/flight/airline/inventory/add").hasAuthority("ROLE_ADMIN")

              // any other requests must be authenticated
              .anyRequest().authenticated()
          );

        // register JWT filter before UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}