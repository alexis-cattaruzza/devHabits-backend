package com.devhabits.repository;

import com.devhabits.model.entity.GitHubConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GitHubConnectionRepository extends JpaRepository<GitHubConnection, UUID> {

    Optional<GitHubConnection> findByUserId(UUID userId);

    Optional<GitHubConnection> findByUserIdAndIsActiveTrue(UUID userId);

    Optional<GitHubConnection> findByGithubUserId(Long githubUserId);

    boolean existsByUserId(UUID userId);

    boolean existsByUserIdAndIsActiveTrue(UUID userId);
}
