package com.devhabits.model.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubUserResponse {

    private Long id;
    private String login;
    private String email;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    private String name;

    @JsonProperty("html_url")
    private String htmlUrl;

    private String bio;
    private String location;
    private String company;

    @JsonProperty("public_repos")
    private Integer publicRepos;

    private Integer followers;
    private Integer following;
}
