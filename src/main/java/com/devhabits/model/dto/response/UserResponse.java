package com.devhabits.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String timezone;
    private Integer totalXp;
    private Integer level;
    private Integer currentStreak;
    private Integer longestStreak;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}