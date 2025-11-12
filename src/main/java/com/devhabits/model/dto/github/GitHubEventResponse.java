package com.devhabits.model.dto.github;

import com.devhabits.model.enums.GitHubEventType;
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
public class GitHubEventResponse {

    private UUID id;
    private UUID userId;
    private UUID habitId;
    private GitHubEventType eventType;
    private String eventId;
    private String repositoryName;
    private String repositoryFullName;
    private String commitSha;
    private String commitMessage;
    private Integer pullRequestNumber;
    private String pullRequestTitle;
    private Integer issueNumber;
    private String issueTitle;
    private LocalDateTime createdAt;
}
