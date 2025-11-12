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
@Table(name = "github_repositories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "github_repo_id", nullable = false)
    private Long githubRepoId;

    @Column(name = "repository_name", nullable = false, length = 255)
    private String repositoryName;

    @Column(name = "repository_full_name", nullable = false, length = 500)
    private String repositoryFullName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_private")
    @Builder.Default
    private Boolean isPrivate = false;

    @Column(name = "is_tracked")
    @Builder.Default
    private Boolean isTracked = true;

    @Column(length = 100)
    private String language;

    @Column(name = "stargazers_count")
    @Builder.Default
    private Integer stargazersCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public void toggleTracking() {
        this.isTracked = !this.isTracked;
    }

    public void enableTracking() {
        this.isTracked = true;
    }

    public void disableTracking() {
        this.isTracked = false;
    }
}
