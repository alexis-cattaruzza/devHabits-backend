package com.devhabits.model.dto.github;

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
public class GitHubConnectionResponse {

    private UUID id;
    private UUID userId;
    private Long githubUserId;
    private String githubUsername;
    private String githubEmail;
    private String githubAvatarUrl;
    private String scope;
    private LocalDateTime connectedAt;
    private LocalDateTime lastSyncedAt;
    private Boolean isActive;
}
