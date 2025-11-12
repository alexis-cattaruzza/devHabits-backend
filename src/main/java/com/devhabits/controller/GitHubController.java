package com.devhabits.controller;

import com.devhabits.model.dto.github.*;
import com.devhabits.model.dto.response.ApiResponse;
import com.devhabits.service.GitHubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
@Tag(name = "GitHub Integration", description = "GitHub OAuth and webhook endpoints")
public class GitHubController {

    private final GitHubService githubService;

    @PostMapping("/connect")
    @Operation(
            summary = "Connect GitHub account",
            description = "Exchange OAuth code for access token and link GitHub account to user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<GitHubConnectionResponse>> connectGitHub(
            @RequestParam @NotBlank String code,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());
        GitHubConnectionResponse connection = githubService.connectGitHub(userId, code);

        return ResponseEntity.ok(ApiResponse.<GitHubConnectionResponse>builder()
                .success(true)
                .message("GitHub account connected successfully")
                .data(connection)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/disconnect")
    @Operation(
            summary = "Disconnect GitHub account",
            description = "Disconnect GitHub account from user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> disconnectGitHub(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        githubService.disconnectGitHub(userId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("GitHub account disconnected successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/connection")
    @Operation(
            summary = "Get GitHub connection status",
            description = "Get current GitHub connection details",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<GitHubConnectionResponse>> getConnection(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Optional<GitHubConnectionResponse> connection = githubService.getConnection(userId);

        if (connection.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.<GitHubConnectionResponse>builder()
                    .success(false)
                    .message("No GitHub connection found")
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        return ResponseEntity.ok(ApiResponse.<GitHubConnectionResponse>builder()
                .success(true)
                .message("GitHub connection retrieved successfully")
                .data(connection.get())
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/repositories")
    @Operation(
            summary = "Get user repositories",
            description = "Get all GitHub repositories for the connected account",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<List<GitHubRepoResponse>>> getRepositories(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<GitHubRepoResponse> repositories = githubService.getUserRepositories(userId);

        return ResponseEntity.ok(ApiResponse.<List<GitHubRepoResponse>>builder()
                .success(true)
                .message("Repositories retrieved successfully")
                .data(repositories)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PatchMapping("/repositories/{repoId}/toggle-tracking")
    @Operation(
            summary = "Toggle repository tracking",
            description = "Enable or disable tracking for a specific repository",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<GitHubRepoResponse>> toggleRepositoryTracking(
            @PathVariable UUID repoId,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());
        GitHubRepoResponse repository = githubService.toggleRepositoryTracking(userId, repoId);

        return ResponseEntity.ok(ApiResponse.<GitHubRepoResponse>builder()
                .success(true)
                .message("Repository tracking toggled successfully")
                .data(repository)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/events")
    @Operation(
            summary = "Get recent GitHub events",
            description = "Get recent GitHub events that triggered habit completions",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<List<GitHubEventResponse>>> getRecentEvents(
            @RequestParam(defaultValue = "7") int days,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());
        List<GitHubEventResponse> events = githubService.getRecentEvents(userId, days);

        return ResponseEntity.ok(ApiResponse.<List<GitHubEventResponse>>builder()
                .success(true)
                .message("GitHub events retrieved successfully")
                .data(events)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/webhook")
    @Operation(
            summary = "GitHub webhook endpoint",
            description = "Receive GitHub webhook events (push, pull_request, issues, etc.)"
    )
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody GitHubWebhookPayload payload) {

        log.info("Received GitHub webhook: {}", eventType);

        try {
            // TODO: Validate webhook signature
            // validateWebhookSignature(signature, payload);

            // Process the webhook event
            githubService.processWebhookEvent(eventType, payload);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing GitHub webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/sync-repositories")
    @Operation(
            summary = "Sync repositories",
            description = "Manually trigger repository synchronization",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> syncRepositories(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());

        // Service will fetch access token internally from database
        githubService.syncUserRepositories(userId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Repository sync completed successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }
}
