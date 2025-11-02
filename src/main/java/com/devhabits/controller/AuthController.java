package com.devhabits.controller;

import com.devhabits.model.dto.request.LoginRequest;
import com.devhabits.model.dto.request.RefreshTokenRequest;
import com.devhabits.model.dto.request.RegisterRequest;
import com.devhabits.model.dto.response.ApiResponse;
import com.devhabits.model.dto.response.AuthResponse;
import com.devhabits.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Register new user",
            description = "Create a new user account with email, username and password"
    )
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        log.info("POST /api/auth/register - email: {}", request.getEmail());
        
        AuthResponse authResponse = authService.register(request);
        ApiResponse<AuthResponse> response = ApiResponse.success(
                "User registered successfully",
                authResponse
        );
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Login user",
            description = "Authenticate user with email/username and password"
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        log.info("POST /api/auth/login - emailOrUsername: {}", request.getEmailOrUsername());
        
        AuthResponse authResponse = authService.login(request);
        ApiResponse<AuthResponse> response = ApiResponse.success(
                "Login successful",
                authResponse
        );
        
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Refresh access token",
            description = "Get a new access token using a valid refresh token"
    )
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        log.info("POST /api/auth/refresh");
        
        AuthResponse authResponse = authService.refreshToken(request);
        ApiResponse<AuthResponse> response = ApiResponse.success(
                "Token refreshed successfully",
                authResponse
        );
        
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Health check",
            description = "Check if authentication service is up"
    )
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Authentication service is healthy"));
    }
}