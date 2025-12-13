package com.authservice.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;   // <-- add this


public class LoginRequest {

	@NotBlank(message = "email must not be blank")
	@Email(message = "must be a valid email")
	private String email;

	@NotBlank(message = "password must not be blank")
	private String password;

	public LoginRequest() {}

	public LoginRequest(String email, String password) {
	    this.email = email;
	    this.password = password;
	}

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }
}