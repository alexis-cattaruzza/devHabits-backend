package com.devhabits.util;

import com.devhabits.model.entity.HabitLog;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class StreakCalculator {

    /**
     * Calculate current streak
     * A streak is broken if there's more than 1 day gap between check-ins
     */
    public static int calculateCurrentStreak(List<HabitLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return 0;
        }

        // Sort logs by date descending (most recent first)
        List<HabitLog> sortedLogs = logs.stream()
                .sorted(Comparator.comparing(HabitLog::getCompletedAt).reversed())
                .toList();

        LocalDate today = LocalDate.now();
        LocalDate lastCompletion = sortedLogs.get(0).getCompletedAt().toLocalDate();

        // Check if streak is still alive (last completion must be today or yesterday)
        long daysSinceLastCompletion = ChronoUnit.DAYS.between(lastCompletion, today);
        if (daysSinceLastCompletion > 1) {
            log.debug("Streak broken: {} days since last completion", daysSinceLastCompletion);
            return 0; // Streak broken
        }

        int streak = 1;
        LocalDate currentDate = lastCompletion;

        // Count consecutive days backwards
        for (int i = 1; i < sortedLogs.size(); i++) {
            LocalDate previousDate = sortedLogs.get(i).getCompletedAt().toLocalDate();
            long daysBetween = ChronoUnit.DAYS.between(previousDate, currentDate);

            if (daysBetween == 1) {
                // Consecutive day
                streak++;
                currentDate = previousDate;
            } else if (daysBetween == 0) {
                // Same day (multiple check-ins) - skip
                continue;
            } else {
                // Gap found, streak ends here
                break;
            }
        }

        log.debug("Current streak calculated: {}", streak);
        return streak;
    }

    /**
     * Calculate longest streak in history
     */
    public static int calculateLongestStreak(List<HabitLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return 0;
        }

        // Sort logs by date ascending
        List<HabitLog> sortedLogs = logs.stream()
                .sorted(Comparator.comparing(HabitLog::getCompletedAt))
                .toList();

        int maxStreak = 1;
        int currentStreak = 1;
        LocalDate previousDate = sortedLogs.get(0).getCompletedAt().toLocalDate();

        for (int i = 1; i < sortedLogs.size(); i++) {
            LocalDate currentDate = sortedLogs.get(i).getCompletedAt().toLocalDate();
            long daysBetween = ChronoUnit.DAYS.between(previousDate, currentDate);

            if (daysBetween == 1) {
                // Consecutive day
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else if (daysBetween == 0) {
                // Same day - skip
                continue;
            } else {
                // Gap found, reset current streak
                currentStreak = 1;
            }

            previousDate = currentDate;
        }

        log.debug("Longest streak calculated: {}", maxStreak);
        return maxStreak;
    }

    /**
     * Check if a habit is at risk (last completion was yesterday)
     */
    public static boolean isStreakAtRisk(List<HabitLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return false;
        }

        LocalDate today = LocalDate.now();
        LocalDate lastCompletion = logs.stream()
                .max(Comparator.comparing(HabitLog::getCompletedAt))
                .map(log -> log.getCompletedAt().toLocalDate())
                .orElse(null);

        if (lastCompletion == null) {
            return false;
        }

        long daysSince = ChronoUnit.DAYS.between(lastCompletion, today);
        return daysSince == 1; // At risk if last completion was yesterday
    }
}