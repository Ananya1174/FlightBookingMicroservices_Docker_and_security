package com.apigateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(-1)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {

        ServerHttpResponse response = exchange.getResponse();
        HttpStatusCode status = response.getStatusCode();

        if (status != null &&
            (status.value() == HttpStatus.UNAUTHORIZED.value()
          || status.value() == HttpStatus.FORBIDDEN.value())) {

            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            String error;
            String message;

            if (status.value() == 401) {
                error = "Unauthorized";
                message = "Authentication required or token is invalid/missing";
            } else {
                error = "Forbidden";
                message = "You do not have permission to access this resource";
            }

            Map<String, Object> body = new HashMap<>();
            body.put("timestamp", Instant.now().toString());
            body.put("status", status.value());
            body.put("error", error);
            body.put("message", message);
            body.put("path", exchange.getRequest().getPath().value());

            byte[] bytes;
            try {
                bytes = mapper.writeValueAsBytes(body);
            } catch (Exception e) {
                bytes = "{\"error\":\"Internal Server Error\"}"
                        .getBytes(StandardCharsets.UTF_8);
            }

            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }

        return Mono.error(ex);
    }
}