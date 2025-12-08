package com.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;


@Configuration
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
          .csrf(csrf -> csrf.disable())                 // <- disable CSRF for gateway
          .httpBasic(httpBasic -> httpBasic.disable())
          .formLogin(form -> form.disable())
          .authorizeExchange(ex -> ex
              .pathMatchers("/auth/**", "/AUTH-SERVICE/**", "/auth-service/**", "/actuator/**").permitAll()
              .pathMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
              .anyExchange().authenticated()
          );
        return http.build();
    }
}