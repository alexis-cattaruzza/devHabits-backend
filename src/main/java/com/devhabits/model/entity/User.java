package com.devhabits.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 50)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "total_xp")
    @Builder.Default
    private Integer totalXp = 0;

    @Column
    @Builder.Default
    private Integer level = 1;

    @Column(name = "current_streak")
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "longest_streak")
    @Builder.Default
    private Integer longestStreak = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // Helper method to update last login
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    // Helper method to add XP and recalculate level
    public void addXp(int xp) {
        this.totalXp += xp;
        this.level = calculateLevel(this.totalXp);
    }

    // Simple level calculation: level = sqrt(totalXp / 100)
    private int calculateLevel(int xp) {
        return (int) Math.floor(Math.sqrt(xp / 100.0)) + 1;
    }
}