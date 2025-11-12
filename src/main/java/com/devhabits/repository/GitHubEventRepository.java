package com.devhabits.repository;

import com.devhabits.model.entity.GitHubEvent;
import com.devhabits.model.enums.GitHubEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GitHubEventRepository extends JpaRepository<GitHubEvent, UUID> {

    List<GitHubEvent> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<GitHubEvent> findByHabitIdOrderByCreatedAtDesc(UUID habitId);

    Optional<GitHubEvent> findByEventIdAndEventType(String eventId, GitHubEventType eventType);

    boolean existsByEventIdAndEventType(String eventId, GitHubEventType eventType);

    @Query("SELECT ge FROM GitHubEvent ge WHERE ge.userId = :userId AND ge.createdAt >= :since ORDER BY ge.createdAt DESC")
    List<GitHubEvent> findRecentEventsByUser(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    @Query("SELECT ge FROM GitHubEvent ge WHERE ge.userId = :userId AND ge.eventType = :eventType ORDER BY ge.createdAt DESC")
    List<GitHubEvent> findByUserIdAndEventType(@Param("userId") UUID userId, @Param("eventType") GitHubEventType eventType);

    long countByUserIdAndCreatedAtAfter(UUID userId, LocalDateTime after);
}
