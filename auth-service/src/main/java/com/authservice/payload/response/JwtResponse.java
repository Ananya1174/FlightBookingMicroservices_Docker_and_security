package com.authservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class JwtResponse {

    private String token;
    private String type;
    private Long id;
    private String username;
    private String email;
    private List<String> roles;

    // 🔐 IMPORTANT
    private boolean passwordExpired;
}