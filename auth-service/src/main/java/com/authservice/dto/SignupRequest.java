package com.authservice.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
public class SignupRequest {
	@Email
	@NotBlank
	private String email;
	@NotBlank
	@Size(min = 6)
	private String password;
	private String role;
}