package com.devhabits.repository;

import com.devhabits.model.entity.GitHubRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GitHubRepositoryRepo extends JpaRepository<GitHubRepository, UUID> {

    List<GitHubRepository> findByUserId(UUID userId);

    List<GitHubRepository> findByUserIdAndIsTrackedTrue(UUID userId);

    Optional<GitHubRepository> findByUserIdAndGithubRepoId(UUID userId, Long githubRepoId);

    Optional<GitHubRepository> findByUserIdAndRepositoryFullName(UUID userId, String repositoryFullName);

    boolean existsByUserIdAndGithubRepoId(UUID userId, Long githubRepoId);
}
