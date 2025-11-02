package com.devhabits.service;

import com.devhabits.exception.BadRequestException;
import com.devhabits.model.dto.request.RegisterRequest;
import com.devhabits.model.dto.response.AuthResponse;
import com.devhabits.model.entity.User;
import com.devhabits.repository.UserRepository;
import com.devhabits.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .email("test@example.com")
                .username("testuser")
                .password("Password123")
                .firstName("Test")
                .lastName("User")
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .username("testuser")
                .passwordHash("hashed_password")
                .firstName("Test")
                .lastName("User")
                .build();
    }

    @Test
    void register_WhenValidRequest_ShouldReturnAuthResponse() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(any(UUID.class), anyString()))
                .thenReturn("access_token");
        when(jwtTokenProvider.generateRefreshToken(any(UUID.class)))
                .thenReturn("refresh_token");
        when(jwtTokenProvider.getExpirationInSeconds()).thenReturn(86400L);

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo(user.getEmail());

        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository).existsByUsername(registerRequest.getUsername());
        verify(userRepository, times(2)).save(any(User.class));
        verify(passwordEncoder).encode(registerRequest.getPassword());
    }

    @Test
    void register_WhenEmailExists_ShouldThrowBadRequestException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email is already registered");

        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_WhenUsernameExists_ShouldThrowBadRequestException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Username is already taken");

        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository).existsByUsername(registerRequest.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }
}