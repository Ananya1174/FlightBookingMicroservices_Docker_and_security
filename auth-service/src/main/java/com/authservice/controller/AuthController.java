package com.authservice.controller;

import com.authservice.model.ERole;
import com.authservice.model.Role;
import com.authservice.model.User;
import com.authservice.payload.request.LoginRequest;
import com.authservice.payload.request.SignupRequest;
import com.authservice.payload.response.JwtResponse;
import com.authservice.repository.RoleRepository;
import com.authservice.repository.UserRepository;
import com.authservice.security.JwtUtils;
import com.authservice.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/signup")
    public String register(@RequestBody SignupRequest req) {

        if (userRepo.existsByUsername(req.getUsername()))
            return "Username already exists";

        if (userRepo.existsByEmail(req.getEmail()))
            return "Email already exists";

        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(encoder.encode(req.getPassword()));

        Set<Role> roles = new HashSet<>();

        if (req.getRole() == null) {
            Role userRole = roleRepo.findByName(ERole.ROLE_USER)
                    .orElseThrow();
            roles.add(userRole);
        } else {
            req.getRole().forEach(r -> {
                if (r.equalsIgnoreCase("admin")) {
                    roles.add(roleRepo.findByName(ERole.ROLE_ADMIN).orElseThrow());
                } else {
                    roles.add(roleRepo.findByName(ERole.ROLE_USER).orElseThrow());
                }
            });
        }

        user.setRoles(roles);
        userRepo.save(user);

        return "User registered successfully!";
    }

    @PostMapping("/signin")
    public JwtResponse login(@RequestBody LoginRequest req) {

    	var auth = authManager.authenticate(
    	        new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
    	);

        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();

        String token = jwtUtils.generateJwtToken(user);

        List<String> roles = user.getAuthorities()
                .stream().map(a -> a.getAuthority()).toList();

        return new JwtResponse(
                token,
                "Bearer",
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roles
        );
    }
}