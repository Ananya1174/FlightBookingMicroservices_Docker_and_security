package com.apigateway.config;

import com.apigateway.security.CustomAuthEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@RequiredArgsConstructor
public class GatewaySecurityConfig {

    private final CustomAuthEntryPoint authEntryPoint;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

        http
            .csrf(csrf -> csrf.disable())
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())

            .authorizeExchange(ex -> ex
                .pathMatchers("/auth/**", "/AUTH-SERVICE/**", "/auth-service/**").permitAll()
                .pathMatchers("/booking-service/**", "/flight-service/**").permitAll()
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                .pathMatchers("/actuator/**", "/actuator").permitAll()
                .anyExchange().authenticated()
            )

            // ⭐ THIS IS THE KEY LINE
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint)
            );

        return http.build();
    }
}