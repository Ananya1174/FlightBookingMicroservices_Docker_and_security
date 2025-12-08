package com.apigateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;

@Component
public class TokenValidationFilter implements GlobalFilter, Ordered {

	private final WebClient webClient;
	private final String authServiceBaseUrl;

	public TokenValidationFilter(WebClient webClient,
			@Value("${auth.service.base-url:http://localhost:8085}") String authServiceBaseUrl) {
		this.webClient = webClient;
		this.authServiceBaseUrl = authServiceBaseUrl;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String path = exchange.getRequest().getPath().value();

		if (path.startsWith("/auth/") || (exchange.getRequest().getMethod() != null
				&& exchange.getRequest().getMethod().equals(HttpMethod.OPTIONS))) {
			System.out.println("[Gateway] Allowing open path: " + exchange.getRequest().getMethod() + " " + path);
			return chain.filter(exchange);
		}

		String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		System.out.println("[Gateway] Incoming request: " + exchange.getRequest().getMethod() + " " + path
				+ " , Authorization present: " + (authHeader != null));

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
			return exchange.getResponse().setComplete();
		}

		return webClient.get().uri(authServiceBaseUrl + "/auth/validate").accept(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, authHeader).retrieve().bodyToMono(ValidateResponse.class)
				.flatMap(validateResp -> {
					if (validateResp != null && validateResp.isValid()) {
						ServerHttpRequest mutatedReq = exchange.getRequest().mutate()
								.header(HttpHeaders.AUTHORIZATION, authHeader).build();

						ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedReq).build();

						System.out.println("[Gateway] Token valid for " + validateResp.getEmail() + " forwarding to: "
								+ mutatedReq.getURI());
						return chain.filter(mutatedExchange);
					} else {
						exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
						return exchange.getResponse().setComplete();
					}
				}).onErrorResume(ex -> {
					System.out.println("[Gateway] Validate call failed: " + ex.getMessage());
					exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
					return exchange.getResponse().setComplete();
				});
	}

	@Override
	public int getOrder() {
		return -100;
	}

	public static class ValidateResponse {
		private boolean valid;
		private String email;
		private String role;
		private String jti;

		public ValidateResponse() {
		}

		public boolean isValid() {
			return valid;
		}

		public void setValid(boolean valid) {
			this.valid = valid;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getRole() {
			return role;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public String getJti() {
			return jti;
		}

		public void setJti(String jti) {
			this.jti = jti;
		}
	}
}