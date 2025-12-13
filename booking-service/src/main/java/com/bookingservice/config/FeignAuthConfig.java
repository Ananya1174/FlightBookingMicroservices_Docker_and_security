package com.bookingservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignAuthConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attrs != null) {
                String authHeader = attrs.getRequest().getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    template.header("Authorization", authHeader);
                }
            }
        };
    }
}