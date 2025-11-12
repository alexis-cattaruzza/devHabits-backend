package com.devhabits.model.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubWebhookPayload {

    private String action;
    private Repository repository;
    private Sender sender;
    private Commit commit;
    private PullRequest pullRequest;
    private Issue issue;

    @JsonProperty("head_commit")
    private Commit headCommit;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Repository {
        private Long id;
        private String name;

        @JsonProperty("full_name")
        private String fullName;

        @JsonProperty("private")
        private Boolean isPrivate;

        private String description;
        private String language;

        @JsonProperty("stargazers_count")
        private Integer stargazersCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Sender {
        private Long id;
        private String login;

        @JsonProperty("avatar_url")
        private String avatarUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Commit {
        private String id;
        private String sha;
        private String message;
        private Author author;
        private String timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Author {
        private String name;
        private String email;
        private String username;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PullRequest {
        private Integer number;
        private String title;
        private String state;

        @JsonProperty("html_url")
        private String htmlUrl;

        private User user;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Issue {
        private Integer number;
        private String title;
        private String state;

        @JsonProperty("html_url")
        private String htmlUrl;

        private User user;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class User {
        private Long id;
        private String login;
    }
}
