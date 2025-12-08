package com.authservice.dto;
import lombok.*;
import jakarta.validation.constraints.*;

@Data
public class AuthRequest {
    @Email @NotBlank private String email;
    @NotBlank private String password;
}