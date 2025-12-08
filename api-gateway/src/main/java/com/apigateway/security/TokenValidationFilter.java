package com.apigateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class TokenValidationFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;

    @Value("${auth.service.url:http://localhost:8080}")
    private String authServiceUrl;

    public TokenValidationFilter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://auth-service").build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Allow unauthenticated paths:
        if (path.startsWith("/auth") || path.startsWith("/actuator") || path.startsWith("/favicon.ico")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        // Call auth service /auth/validate (pass Authorization header)
        return webClient.get()
                .uri(authServiceUrl + "/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(map -> {
                    // Auth service returns object like { valid: true, email: "...", role: "...", jti: "..." }
                    Boolean valid = (Boolean) map.get("valid");
                    if (Boolean.TRUE.equals(valid)) {
                        String email = (String) map.get("email");
                        String role = (String) map.get("role");
                        // attach headers for downstream services
                        if (email != null) exchange.getRequest().mutate().header("X-User-Email", email);
                        if (role != null) exchange.getRequest().mutate().header("X-User-Role", role);
                        return chain.filter(exchange);
                    } else {
                        return unauthorized(exchange);
                    }
                })
                .onErrorResume(err -> {
                    // Treat any error contacting auth service as unauthorized (or SERVICE_UNAVAILABLE if you prefer)
                    return unauthorized(exchange);
                });
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse resp = exchange.getResponse();
        resp.setStatusCode(HttpStatus.UNAUTHORIZED);
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = ("{\"error\":\"Unauthorized\"}").getBytes();
        return resp.writeWith(Mono.just(resp.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        // run before many built-in filters
        return -100;
    }
}