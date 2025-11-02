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
@Table(name = "habit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "habit_id", nullable = false)
    private UUID habitId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "completed_at", nullable = false)
    @Builder.Default
    private LocalDateTime completedAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "github_commit_sha", length = 40)
    private String githubCommitSha;

    @Column(name = "github_repo_name", length = 255)
    private String githubRepoName;

    @Column(name = "xp_earned")
    @Builder.Default
    private Integer xpEarned = 10;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}