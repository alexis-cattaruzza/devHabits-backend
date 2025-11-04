package com.devhabits.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStats {
    private int totalHabits;
    private int activeHabits;
    private int completedToday;
    private int totalCompletionsThisWeek;
    private int totalCompletionsThisMonth;
    private double completionRateToday;
    private int currentMaxStreak;
}