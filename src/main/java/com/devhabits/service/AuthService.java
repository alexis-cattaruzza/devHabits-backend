package com.devhabits.service;

import com.devhabits.exception.BadRequestException;
import com.devhabits.exception.ResourceNotFoundException;
import com.devhabits.model.dto.request.LoginRequest;
import com.devhabits.model.dto.request.RefreshTokenRequest;
import com.devhabits.model.dto.request.RegisterRequest;
import com.devhabits.model.dto.response.AuthResponse;
import com.devhabits.model.dto.response.UserResponse;
import com.devhabits.model.entity.User;
import com.devhabits.repository.UserRepository;
import com.devhabits.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .isActive(true)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        user.updateLastLogin();
        userRepository.save(user);

        log.info("User registered successfully with ID: {}", user.getId());

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                .user(mapToUserResponse(user))
                .build();
    }

    /**
     * Login user
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getEmailOrUsername());

        try {
            // Find user
            User user = userRepository.findByEmailOrUsername(
                    request.getEmailOrUsername(),
                    request.getEmailOrUsername()
            ).orElseThrow(() -> new BadRequestException("Invalid credentials"));

            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getId().toString(),
                            request.getPassword()
                    )
            );

            // Update last login
            user.updateLastLogin();
            userRepository.save(user);

            log.info("User logged in successfully: {}", user.getId());

            // Generate tokens
            String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                    .user(mapToUserResponse(user))
                    .build();

        } catch (AuthenticationException e) {
            log.error("Authentication failed for: {}", request.getEmailOrUsername(), e);
            throw new BadRequestException("Invalid credentials");
        }
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        // Check if it's actually a refresh token
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BadRequestException("Token is not a refresh token");
        }

        // Get user ID from token
        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));

        // Generate new access token
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());

        log.info("Access token refreshed for user: {}", userId);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Return same refresh token
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                .user(mapToUserResponse(user))
                .build();
    }

    /**
     * Map User entity to UserResponse DTO
     */
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .timezone(user.getTimezone())
                .totalXp(user.getTotalXp())
                .level(user.getLevel())
                .currentStreak(user.getCurrentStreak())
                .longestStreak(user.getLongestStreak())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}