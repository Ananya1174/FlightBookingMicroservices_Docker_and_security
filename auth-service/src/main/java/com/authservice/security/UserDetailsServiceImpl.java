package com.authservice.security;

import com.authservice.model.User;
import com.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    	// parameter 'username' will actually be the email (since we authenticate by email)
    	User user = userRepo.findByEmail(username)
    	        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return UserDetailsImpl.build(user);
    }
}