package com.devhabits.model.entity;

import com.devhabits.model.enums.HabitCategory;
import com.devhabits.model.enums.HabitFrequency;
import com.devhabits.model.enums.GitHubEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "habits")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private HabitCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private HabitFrequency frequency = HabitFrequency.DAILY;

    @Column(name = "target_count")
    @Builder.Default
    private Integer targetCount = 1;

    @Column(length = 50)
    private String icon;

    @Column(length = 20)
    private String color;

    @Column(name = "github_auto_track")
    @Builder.Default
    private Boolean githubAutoTrack = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "github_event_type", length = 50)
    private GitHubEventType githubEventType;

    @Column(name = "reminder_enabled")
    @Builder.Default
    private Boolean reminderEnabled = false;

    @Column(name = "reminder_time")
    private LocalTime reminderTime;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "current_streak")
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "longest_streak")
    @Builder.Default
    private Integer longestStreak = 0;

    @Column(name = "total_completions")
    @Builder.Default
    private Integer totalCompletions = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper method to archive habit
    public void archive() {
        this.isActive = false;
        this.archivedAt = LocalDateTime.now();
    }

    // Helper method to restore habit
    public void restore() {
        this.isActive = true;
        this.archivedAt = null;
    }

    // Helper method to increment completions
    public void incrementCompletions() {
        this.totalCompletions++;
    }

    // Helper method to update streak
    public void updateStreak(int newStreak) {
        this.currentStreak = newStreak;
        if (newStreak > this.longestStreak) {
            this.longestStreak = newStreak;
        }
    }
}