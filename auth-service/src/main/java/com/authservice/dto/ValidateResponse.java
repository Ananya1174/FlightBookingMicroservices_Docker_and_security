package com.authservice.dto;
import lombok.*;
@Data @AllArgsConstructor @NoArgsConstructor
public class ValidateResponse {
    private boolean valid;
    private String email;
    private String role;
    private String jti;
}