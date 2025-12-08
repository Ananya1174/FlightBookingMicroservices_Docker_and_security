package com.authservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Simple DTO returned by /signin
 */
public class AuthResponse {

    private String token;
    private long expiresInSeconds;
    private String role;

    // No-arg ctor for Jackson
    public AuthResponse() {}

    // The exact ctor your controller calls
    public AuthResponse(String token, long expiresInSeconds, String role) {
        this.token = token;
        this.expiresInSeconds = expiresInSeconds;
        this.role = role;
    }

    // getters + setters
    @JsonProperty("token")
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }

    @JsonProperty("expiresInSeconds")
    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }
    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    @JsonProperty("role")
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "AuthResponse{" +
                "token='" + token + '\'' +
                ", expiresInSeconds=" + expiresInSeconds +
                ", role='" + role + '\'' +
                '}';
    }
}