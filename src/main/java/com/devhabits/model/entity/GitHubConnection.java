package com.devhabits.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "github_connections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "github_user_id", nullable = false)
    private Long githubUserId;

    @Column(name = "github_username", nullable = false, length = 255)
    private String githubUsername;

    @Column(name = "github_email", length = 255)
    private String githubEmail;

    @Column(name = "github_avatar_url", length = 500)
    private String githubAvatarUrl;

    @Column(name = "access_token", nullable = false, length = 500)
    private String accessToken;

    @Column(name = "token_type", length = 50)
    @Builder.Default
    private String tokenType = "Bearer";

    @Column(columnDefinition = "TEXT")
    private String scope;

    @CreationTimestamp
    @Column(name = "connected_at", updatable = false)
    private LocalDateTime connectedAt;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Helper method to update sync time
    public void updateLastSync() {
        this.lastSyncedAt = LocalDateTime.now();
    }

    // Helper method to disconnect
    public void disconnect() {
        this.isActive = false;
    }

    // Helper method to reconnect
    public void reconnect() {
        this.isActive = true;
    }
}
