package com.devhabits.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private UserResponse user;
    private DashboardStats stats;
    private List<HabitResponse> todayHabits;
    private List<HabitResponse> streakAtRiskHabits;
}