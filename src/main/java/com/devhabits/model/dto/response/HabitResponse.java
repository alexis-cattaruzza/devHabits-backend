package com.devhabits.model.dto.response;

import com.devhabits.model.enums.HabitCategory;
import com.devhabits.model.enums.HabitFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitResponse {
    private UUID id;
    private String name;
    private String description;
    private HabitCategory category;
    private HabitFrequency frequency;
    private Integer targetCount;
    private String icon;
    private String color;
    private Boolean reminderEnabled;
    private LocalTime reminderTime;
    private Integer currentStreak;
    private Integer longestStreak;
    private Integer totalCompletions;
    private Boolean completedToday;
    private LocalDateTime lastCompletedAt;
    private LocalDateTime createdAt;
}