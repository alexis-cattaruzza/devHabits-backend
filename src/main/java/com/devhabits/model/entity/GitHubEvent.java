package com.devhabits.model.entity;

import com.devhabits.model.enums.GitHubEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "github_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "habit_id")
    private UUID habitId;

    @Column(name = "habit_log_id")
    private UUID habitLogId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private GitHubEventType eventType;

    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    @Column(name = "repository_name", nullable = false, length = 255)
    private String repositoryName;

    @Column(name = "repository_full_name", nullable = false, length = 500)
    private String repositoryFullName;

    @Column(name = "commit_sha", length = 40)
    private String commitSha;

    @Column(name = "commit_message", columnDefinition = "TEXT")
    private String commitMessage;

    @Column(name = "pull_request_number")
    private Integer pullRequestNumber;

    @Column(name = "pull_request_title", columnDefinition = "TEXT")
    private String pullRequestTitle;

    @Column(name = "issue_number")
    private Integer issueNumber;

    @Column(name = "issue_title", columnDefinition = "TEXT")
    private String issueTitle;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", columnDefinition = "jsonb")
    private Map<String, Object> eventData;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Helper method to mark as processed
    public void markAsProcessed() {
        this.processedAt = LocalDateTime.now();
    }
}
