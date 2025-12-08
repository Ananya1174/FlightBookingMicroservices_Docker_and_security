package com.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    // Provide a builder if other components want to customize
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    // Optional: a default WebClient instance
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }
}