package com.authservice.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public class SignupRequest {

    @NotBlank(message = "username must not be blank")
    @Size(min = 3, max = 50)
    private String username;

    @Email(message = "must be a valid email")
    @NotBlank(message = "email must not be blank")
    private String email;

    @NotBlank(message = "password must not be blank")
    private String password;

    // optional roles passed from client (e.g. ["admin"] or ["user"])
    private Set<String> role;

    public SignupRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Set<String> getRole() { return role; }
    public void setRole(Set<String> role) { this.role = role; }
}