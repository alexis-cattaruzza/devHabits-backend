package com.devhabits.service;

import com.devhabits.model.dto.response.DashboardResponse;
import com.devhabits.model.dto.response.DashboardStats;
import com.devhabits.model.dto.response.HabitResponse;
import com.devhabits.model.dto.response.UserResponse;
import com.devhabits.model.entity.Habit;
import com.devhabits.model.entity.HabitLog;
import com.devhabits.model.entity.User;
import com.devhabits.repository.HabitLogRepository;
import com.devhabits.repository.HabitRepository;
import com.devhabits.repository.UserRepository;
import com.devhabits.util.StreakCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID userId) {
        log.info("Fetching dashboard for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Habit> allHabits = habitRepository.findByUserIdAndIsActiveTrue(userId);

        // Calculate today's completion status
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        AtomicInteger completedTodayCounter = new AtomicInteger(0);
        List<HabitResponse> todayHabits = allHabits.stream()
                .map(habit -> {
                    boolean isCompleted = habitLogRepository.existsForToday(
                            habit.getId(), startOfDay, endOfDay
                    );
                    if (isCompleted) {
                    	completedTodayCounter.incrementAndGet();
                    }

                    LocalDateTime lastCompleted = habitLogRepository
                            .findFirstByHabitIdOrderByCompletedAtDesc(habit.getId())
                            .map(HabitLog::getCompletedAt)
                            .orElse(null);

                    return mapToHabitResponse(habit, isCompleted, lastCompleted);
                })
                .collect(Collectors.toList());

        // Find habits at risk
        List<HabitResponse> atRiskHabits = todayHabits.stream()
                .filter(h -> !h.getCompletedToday() && h.getCurrentStreak() > 0)
                .collect(Collectors.toList());

        // Calculate weekly and monthly completions
        LocalDateTime startOfWeek = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().minusDays(30).atStartOfDay();

        int weeklyCompletions = (int) allHabits.stream()
                .flatMap(habit -> habitLogRepository
                        .findByHabitIdAndCompletedAtBetweenOrderByCompletedAtDesc(
                                habit.getId(), startOfWeek, LocalDateTime.now()
                        ).stream())
                .count();

        int monthlyCompletions = (int) allHabits.stream()
                .flatMap(habit -> habitLogRepository
                        .findByHabitIdAndCompletedAtBetweenOrderByCompletedAtDesc(
                                habit.getId(), startOfMonth, LocalDateTime.now()
                        ).stream())
                .count();
        
        int completedToday = completedTodayCounter.get();

        // Calculate completion rate for today
        double completionRate = allHabits.isEmpty() ? 0.0 
                : (completedToday * 100.0) / allHabits.size();

        // Find max current streak
        int maxStreak = allHabits.stream()
                .mapToInt(Habit::getCurrentStreak)
                .max()
                .orElse(0);

        // Build stats
        DashboardStats stats = DashboardStats.builder()
                .totalHabits(allHabits.size())
                .activeHabits(allHabits.size())
                .completedToday(completedToday)
                .totalCompletionsThisWeek(weeklyCompletions)
                .totalCompletionsThisMonth(monthlyCompletions)
                .completionRateToday(Math.round(completionRate * 10.0) / 10.0)
                .currentMaxStreak(maxStreak)
                .build();

        return DashboardResponse.builder()
                .user(mapToUserResponse(user))
                .stats(stats)
                .todayHabits(todayHabits)
                .streakAtRiskHabits(atRiskHabits)
                .build();
    }

    private HabitResponse mapToHabitResponse(Habit habit, boolean completedToday, LocalDateTime lastCompleted) {
        return HabitResponse.builder()
                .id(habit.getId())
                .name(habit.getName())
                .description(habit.getDescription())
                .category(habit.getCategory())
                .frequency(habit.getFrequency())
                .targetCount(habit.getTargetCount())
                .icon(habit.getIcon())
                .color(habit.getColor())
                .reminderEnabled(habit.getReminderEnabled())
                .reminderTime(habit.getReminderTime())
                .currentStreak(habit.getCurrentStreak())
                .longestStreak(habit.getLongestStreak())
                .totalCompletions(habit.getTotalCompletions())
                .completedToday(completedToday)
                .lastCompletedAt(lastCompleted)
                .createdAt(habit.getCreatedAt())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .timezone(user.getTimezone())
                .totalXp(user.getTotalXp())
                .level(user.getLevel())
                .currentStreak(user.getCurrentStreak())
                .longestStreak(user.getLongestStreak())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}