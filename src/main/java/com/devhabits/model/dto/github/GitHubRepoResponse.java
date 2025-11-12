package com.devhabits.model.dto.github;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubRepoResponse {

    private UUID id;
    private UUID userId;
    private Long githubRepoId;
    private String repositoryName;
    private String repositoryFullName;
    private String description;
    private Boolean isPrivate;
    private Boolean isTracked;
    private String language;
    private Integer stargazersCount;
}
