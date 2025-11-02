package com.devhabits.security;

import com.devhabits.exception.ResourceNotFoundException;
import com.devhabits.model.entity.User;
import com.devhabits.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        try {
            UUID id = UUID.fromString(userId);
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

            return buildUserDetails(user);
        } catch (IllegalArgumentException e) {
            throw new UsernameNotFoundException("Invalid user ID format: " + userId);
        }
    }

    @Transactional
    public UserDetails loadUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        return buildUserDetails(user);
    }

    @Transactional
    public UserDetails loadUserByEmailOrUsername(String emailOrUsername) {
        User user = userRepository.findByEmailOrUsername(emailOrUsername, emailOrUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email or username", emailOrUsername));

        return buildUserDetails(user);
    }

    private UserDetails buildUserDetails(User user) {
        if (!user.getIsActive()) {
            throw new UsernameNotFoundException("User account is deactivated");
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getId().toString())
                .password(user.getPasswordHash())
                .authorities(new ArrayList<>()) // No roles for now, all users have same permissions
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.getIsActive())
                .build();
    }
}