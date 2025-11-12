package com.devhabits.service;

import com.devhabits.exception.BadRequestException;
import com.devhabits.exception.ResourceNotFoundException;
import com.devhabits.model.dto.github.*;
import com.devhabits.model.entity.*;
import com.devhabits.model.enums.GitHubEventType;
import com.devhabits.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubService {

    private final GitHubConnectionRepository connectionRepository;
    private final GitHubEventRepository eventRepository;
    private final GitHubRepositoryRepo repositoryRepo;
    private final HabitRepository habitRepository;
    private final HabitService habitService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.registration.github.client-id}")
    private String githubClientId;

    @Value("${spring.security.oauth2.client.registration.github.client-secret}")
    private String githubClientSecret;

    private static final String GITHUB_API_URL = "https://api.github.com";
    private static final String GITHUB_OAUTH_TOKEN_URL = "https://github.com/login/oauth/access_token";

    /**
     * Exchange GitHub OAuth code for access token and connect user account
     */
    @Transactional
    public GitHubConnectionResponse connectGitHub(UUID userId, String code) {
        log.info("Connecting GitHub account for user: {}", userId);

        // Exchange code for access token
        String accessToken = exchangeCodeForToken(code);

        // Fetch GitHub user info
        GitHubUserResponse githubUser = fetchGitHubUser(accessToken);

        // Check if GitHub account is already connected to another user
        Optional<GitHubConnection> existingConnection = connectionRepository.findByGithubUserId(githubUser.getId());
        if (existingConnection.isPresent() && !existingConnection.get().getUserId().equals(userId)) {
            throw new BadRequestException("This GitHub account is already connected to another DevHabits account");
        }

        // Create or update connection
        GitHubConnection connection = connectionRepository.findByUserId(userId)
                .map(existing -> {
                    existing.setGithubUserId(githubUser.getId());
                    existing.setGithubUsername(githubUser.getLogin());
                    existing.setGithubEmail(githubUser.getEmail());
                    existing.setGithubAvatarUrl(githubUser.getAvatarUrl());
                    existing.setAccessToken(accessToken);
                    existing.setScope("user:email,read:user,repo");
                    existing.reconnect();
                    return existing;
                })
                .orElse(GitHubConnection.builder()
                        .userId(userId)
                        .githubUserId(githubUser.getId())
                        .githubUsername(githubUser.getLogin())
                        .githubEmail(githubUser.getEmail())
                        .githubAvatarUrl(githubUser.getAvatarUrl())
                        .accessToken(accessToken)
                        .scope("user:email,read:user,repo")
                        .build());

        connection = connectionRepository.save(connection);

        // Sync repositories
        syncUserRepositories(userId, accessToken);

        log.info("GitHub account connected successfully for user: {}", userId);
        return mapToConnectionResponse(connection);
    }

    /**
     * Disconnect GitHub account
     */
    @Transactional
    public void disconnectGitHub(UUID userId) {
        GitHubConnection connection = connectionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("GitHubConnection", "userId", userId));

        connection.disconnect();
        connectionRepository.save(connection);

        log.info("GitHub account disconnected for user: {}", userId);
    }

    /**
     * Get GitHub connection status
     */
    @Transactional(readOnly = true)
    public Optional<GitHubConnectionResponse> getConnection(UUID userId) {
        return connectionRepository.findByUserIdAndIsActiveTrue(userId)
                .map(this::mapToConnectionResponse);
    }

    /**
     * Get user's GitHub repositories
     */
    @Transactional(readOnly = true)
    public List<GitHubRepoResponse> getUserRepositories(UUID userId) {
        return repositoryRepo.findByUserId(userId).stream()
                .map(this::mapToRepoResponse)
                .collect(Collectors.toList());
    }

    /**
     * Toggle repository tracking
     */
    @Transactional
    public GitHubRepoResponse toggleRepositoryTracking(UUID userId, UUID repoId) {
        GitHubRepository repo = repositoryRepo.findById(repoId)
                .orElseThrow(() -> new ResourceNotFoundException("GitHubRepository", "id", repoId));

        if (!repo.getUserId().equals(userId)) {
            throw new BadRequestException("Repository does not belong to user");
        }

        repo.toggleTracking();
        repo = repositoryRepo.save(repo);

        return mapToRepoResponse(repo);
    }

    /**
     * Get recent GitHub events for user
     */
    @Transactional(readOnly = true)
    public List<GitHubEventResponse> getRecentEvents(UUID userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return eventRepository.findRecentEventsByUser(userId, since).stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    /**
     * Process GitHub webhook event
     */
    @Transactional
    public void processWebhookEvent(String eventType, GitHubWebhookPayload payload) {
        log.info("Processing GitHub webhook event: {}", eventType);

        // Determine event type
        GitHubEventType githubEventType = mapWebhookEventType(eventType, payload.getAction());
        if (githubEventType == null) {
            log.warn("Unsupported webhook event type: {} with action: {}", eventType, payload.getAction());
            return;
        }

        // Get user from GitHub sender ID
        GitHubConnection connection = connectionRepository.findByGithubUserId(payload.getSender().getId())
                .orElse(null);

        if (connection == null || !connection.getIsActive()) {
            log.debug("No active connection found for GitHub user: {}", payload.getSender().getLogin());
            return;
        }

        // Create event ID to prevent duplicates
        String eventId = generateEventId(eventType, payload);

        // Check if event already processed
        if (eventRepository.existsByEventIdAndEventType(eventId, githubEventType)) {
            log.debug("Event already processed: {}", eventId);
            return;
        }

        // Find matching habits for auto-tracking
        List<Habit> matchingHabits = habitRepository.findByUserIdAndIsActiveTrue(connection.getUserId()).stream()
                .filter(habit -> habit.getGithubAutoTrack() && habit.getGithubEventType() == githubEventType)
                .collect(Collectors.toList());

        if (matchingHabits.isEmpty()) {
            log.debug("No matching habits found for event type: {}", githubEventType);
            return;
        }

        // Process each matching habit
        for (Habit habit : matchingHabits) {
            try {
                // Auto-complete the habit
                UUID habitLogId = habitService.autoCompleteHabitFromGitHub(
                        connection.getUserId(),
                        habit.getId(),
                        generateEventNote(githubEventType, payload)
                );

                // Create GitHub event record
                GitHubEvent event = createGitHubEvent(
                        connection.getUserId(),
                        habit.getId(),
                        habitLogId,
                        githubEventType,
                        eventId,
                        payload
                );

                eventRepository.save(event);

                log.info("Auto-completed habit {} from GitHub event {}", habit.getName(), eventType);
            } catch (Exception e) {
                log.error("Error processing habit {} for GitHub event: {}", habit.getId(), e.getMessage());
            }
        }

        // Update last sync time
        connection.updateLastSync();
        connectionRepository.save(connection);
    }

    /**
     * Sync user repositories from GitHub (retrieve access token internally)
     */
    @Transactional
    public void syncUserRepositories(UUID userId) {
        log.info("Syncing repositories for user: {}", userId);

        // Get access token from connection
        GitHubConnection connection = connectionRepository.findByUserIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("GitHubConnection", "userId", userId));

        syncUserRepositories(userId, connection.getAccessToken());
    }

    /**
     * Sync user repositories from GitHub with provided access token
     */
    @Transactional
    public void syncUserRepositories(UUID userId, String accessToken) {
        log.info("Syncing repositories for user: {} with provided token", userId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Accept", "application/vnd.github.v3+json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            @SuppressWarnings("rawtypes")
            ResponseEntity<List> response = restTemplate.exchange(
                    GITHUB_API_URL + "/user/repos?per_page=100&sort=updated",
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> repos = response.getBody();

                for (Map<String, Object> repoData : repos) {
                    Long githubRepoId = ((Number) repoData.get("id")).longValue();

                    GitHubRepository repo = repositoryRepo.findByUserIdAndGithubRepoId(userId, githubRepoId)
                            .orElse(GitHubRepository.builder()
                                    .userId(userId)
                                    .githubRepoId(githubRepoId)
                                    .build());

                    repo.setRepositoryName((String) repoData.get("name"));
                    repo.setRepositoryFullName((String) repoData.get("full_name"));
                    repo.setDescription((String) repoData.get("description"));
                    repo.setIsPrivate((Boolean) repoData.getOrDefault("private", false));
                    repo.setLanguage((String) repoData.get("language"));
                    repo.setStargazersCount(((Number) repoData.getOrDefault("stargazers_count", 0)).intValue());

                    repositoryRepo.save(repo);
                }

                log.info("Synced {} repositories for user: {}", repos.size(), userId);
            }
        } catch (Exception e) {
            log.error("Error syncing repositories: {}", e.getMessage());
        }
    }

    // ==================== Private Helper Methods ====================

    private String exchangeCodeForToken(String code) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");

            Map<String, String> body = new HashMap<>();
            body.put("client_id", githubClientId);
            body.put("client_secret", githubClientSecret);
            body.put("code", code);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    GITHUB_OAUTH_TOKEN_URL,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = response.getBody();
                String accessToken = (String) responseBody.get("access_token");

                if (accessToken == null) {
                    throw new BadRequestException("Failed to get access token from GitHub");
                }

                return accessToken;
            }

            throw new BadRequestException("Failed to exchange code for token");
        } catch (Exception e) {
            log.error("Error exchanging code for token: {}", e.getMessage());
            throw new BadRequestException("GitHub authentication failed: " + e.getMessage());
        }
    }

    private GitHubUserResponse fetchGitHubUser(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Accept", "application/vnd.github.v3+json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<GitHubUserResponse> response = restTemplate.exchange(
                    GITHUB_API_URL + "/user",
                    HttpMethod.GET,
                    entity,
                    GitHubUserResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }

            throw new BadRequestException("Failed to fetch GitHub user info");
        } catch (Exception e) {
            log.error("Error fetching GitHub user: {}", e.getMessage());
            throw new BadRequestException("Failed to fetch GitHub user: " + e.getMessage());
        }
    }

    private GitHubEventType mapWebhookEventType(String eventType, String action) {
        return switch (eventType.toLowerCase()) {
            case "push" -> GitHubEventType.COMMIT;
            case "pull_request" ->
                (action != null && action.equals("opened")) ? GitHubEventType.PULL_REQUEST : null;
            case "pull_request_review" -> GitHubEventType.CODE_REVIEW;
            case "issues" ->
                (action != null && (action.equals("opened") || action.equals("closed"))) ?
                    GitHubEventType.ISSUE : null;
            default -> null;
        };
    }

    private String generateEventId(String eventType, GitHubWebhookPayload payload) {
        if (payload.getHeadCommit() != null && payload.getHeadCommit().getSha() != null) {
            return payload.getHeadCommit().getSha();
        }
        if (payload.getPullRequest() != null) {
            return "pr-" + payload.getRepository().getFullName() + "-" + payload.getPullRequest().getNumber();
        }
        if (payload.getIssue() != null) {
            return "issue-" + payload.getRepository().getFullName() + "-" + payload.getIssue().getNumber();
        }
        return UUID.randomUUID().toString();
    }

    private String generateEventNote(GitHubEventType eventType, GitHubWebhookPayload payload) {
        return switch (eventType) {
            case COMMIT -> {
                String message = payload.getHeadCommit() != null ? payload.getHeadCommit().getMessage() : "Commit";
                yield "GitHub Commit: " + message;
            }
            case PULL_REQUEST -> {
                String title = payload.getPullRequest() != null ? payload.getPullRequest().getTitle() : "PR";
                yield "GitHub PR: " + title;
            }
            case CODE_REVIEW -> "GitHub Code Review completed";
            case ISSUE -> {
                String title = payload.getIssue() != null ? payload.getIssue().getTitle() : "Issue";
                yield "GitHub Issue: " + title;
            }
        };
    }

    private GitHubEvent createGitHubEvent(UUID userId, UUID habitId, UUID habitLogId,
                                          GitHubEventType eventType, String eventId,
                                          GitHubWebhookPayload payload) {
        GitHubEvent.GitHubEventBuilder eventBuilder = GitHubEvent.builder()
                .userId(userId)
                .habitId(habitId)
                .habitLogId(habitLogId)
                .eventType(eventType)
                .eventId(eventId)
                .repositoryName(payload.getRepository().getName())
                .repositoryFullName(payload.getRepository().getFullName());

        // Add type-specific data
        if (eventType == GitHubEventType.COMMIT && payload.getHeadCommit() != null) {
            eventBuilder.commitSha(payload.getHeadCommit().getSha())
                    .commitMessage(payload.getHeadCommit().getMessage());
        } else if (eventType == GitHubEventType.PULL_REQUEST && payload.getPullRequest() != null) {
            eventBuilder.pullRequestNumber(payload.getPullRequest().getNumber())
                    .pullRequestTitle(payload.getPullRequest().getTitle());
        } else if (eventType == GitHubEventType.ISSUE && payload.getIssue() != null) {
            eventBuilder.issueNumber(payload.getIssue().getNumber())
                    .issueTitle(payload.getIssue().getTitle());
        }

        return eventBuilder.build();
    }

    private GitHubConnectionResponse mapToConnectionResponse(GitHubConnection connection) {
        return GitHubConnectionResponse.builder()
                .id(connection.getId())
                .userId(connection.getUserId())
                .githubUserId(connection.getGithubUserId())
                .githubUsername(connection.getGithubUsername())
                .githubEmail(connection.getGithubEmail())
                .githubAvatarUrl(connection.getGithubAvatarUrl())
                .scope(connection.getScope())
                .connectedAt(connection.getConnectedAt())
                .lastSyncedAt(connection.getLastSyncedAt())
                .isActive(connection.getIsActive())
                .build();
    }

    private GitHubRepoResponse mapToRepoResponse(GitHubRepository repo) {
        return GitHubRepoResponse.builder()
                .id(repo.getId())
                .userId(repo.getUserId())
                .githubRepoId(repo.getGithubRepoId())
                .repositoryName(repo.getRepositoryName())
                .repositoryFullName(repo.getRepositoryFullName())
                .description(repo.getDescription())
                .isPrivate(repo.getIsPrivate())
                .isTracked(repo.getIsTracked())
                .language(repo.getLanguage())
                .stargazersCount(repo.getStargazersCount())
                .build();
    }

    private GitHubEventResponse mapToEventResponse(GitHubEvent event) {
        return GitHubEventResponse.builder()
                .id(event.getId())
                .userId(event.getUserId())
                .habitId(event.getHabitId())
                .eventType(event.getEventType())
                .eventId(event.getEventId())
                .repositoryName(event.getRepositoryName())
                .repositoryFullName(event.getRepositoryFullName())
                .commitSha(event.getCommitSha())
                .commitMessage(event.getCommitMessage())
                .pullRequestNumber(event.getPullRequestNumber())
                .pullRequestTitle(event.getPullRequestTitle())
                .issueNumber(event.getIssueNumber())
                .issueTitle(event.getIssueTitle())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
