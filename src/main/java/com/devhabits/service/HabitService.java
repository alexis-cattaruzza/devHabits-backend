package com.devhabits.service;

import com.devhabits.exception.BadRequestException;
import com.devhabits.exception.ResourceNotFoundException;
import com.devhabits.model.dto.request.CheckInRequest;
import com.devhabits.model.dto.request.CreateHabitRequest;
import com.devhabits.model.dto.request.UpdateHabitRequest;
import com.devhabits.model.dto.response.HabitResponse;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HabitService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final UserRepository userRepository;

    private boolean isCompletedToday(UUID habitId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return habitLogRepository.existsForToday(habitId, startOfDay, endOfDay);
    }

    /**
     * Create a new habit
     */
    @Transactional
    public HabitResponse createHabit(UUID userId, CreateHabitRequest request) {
        log.info("Creating habit for user: {}", userId);

        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));

        // Create habit
        Habit habit = Habit.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .frequency(request.getFrequency())
                .targetCount(request.getTargetCount() != null ? request.getTargetCount() : 1)
                .icon(request.getIcon())
                .color(request.getColor())
                .reminderEnabled(request.getReminderEnabled())
                .reminderTime(request.getReminderTime())
                .isActive(true)
                .currentStreak(0)
                .longestStreak(0)
                .totalCompletions(0)
                .build();

        habit = habitRepository.save(habit);

        log.info("Habit created successfully: {}", habit.getId());

        return mapToHabitResponse(habit, false, null);
    }

    /**
     * Get all habits for a user
     */
    @Transactional(readOnly = true)
    public List<HabitResponse> getUserHabits(UUID userId, Boolean includeArchived) {
        log.info("Fetching habits for user: {}, includeArchived: {}", userId, includeArchived);

        List<Habit> habits = includeArchived != null && includeArchived
                ? habitRepository.findByUserId(userId)
                : habitRepository.findByUserIdAndIsActiveTrue(userId);

        return habits.stream()
                .map(habit -> {
                	LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
                	LocalDateTime endOfDay = startOfDay.plusDays(1);
                    boolean completedToday = isCompletedToday(habit.getId());
                    LocalDateTime lastCompleted = habitLogRepository
                            .findFirstByHabitIdOrderByCompletedAtDesc(habit.getId())
                            .map(HabitLog::getCompletedAt)
                            .orElse(null);
                    return mapToHabitResponse(habit, completedToday, lastCompleted);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get a single habit by ID
     */
    @Transactional(readOnly = true)
    public HabitResponse getHabitById(UUID userId, UUID habitId) {
        log.info("Fetching habit: {} for user: {}", habitId, userId);

        Habit habit = habitRepository.findByIdAndUserId(habitId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit", "id", habitId.toString()));

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        boolean completedToday = isCompletedToday(habit.getId());
        
        LocalDateTime lastCompleted = habitLogRepository
                .findFirstByHabitIdOrderByCompletedAtDesc(habit.getId())
                .map(HabitLog::getCompletedAt)
                .orElse(null);

        return mapToHabitResponse(habit, completedToday, lastCompleted);
    }

    /**
     * Update a habit
     */
    @Transactional
    public HabitResponse updateHabit(UUID userId, UUID habitId, UpdateHabitRequest request) {
        log.info("Updating habit: {} for user: {}", habitId, userId);

        Habit habit = habitRepository.findByIdAndUserId(habitId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit", "id", habitId.toString()));

        // Update fields if provided
        if (request.getName() != null) habit.setName(request.getName());
        if (request.getDescription() != null) habit.setDescription(request.getDescription());
        if (request.getCategory() != null) habit.setCategory(request.getCategory());
        if (request.getFrequency() != null) habit.setFrequency(request.getFrequency());
        if (request.getTargetCount() != null) habit.setTargetCount(request.getTargetCount());
        if (request.getIcon() != null) habit.setIcon(request.getIcon());
        if (request.getColor() != null) habit.setColor(request.getColor());
        if (request.getReminderEnabled() != null) habit.setReminderEnabled(request.getReminderEnabled());
        if (request.getReminderTime() != null) habit.setReminderTime(request.getReminderTime());
        if (request.getGithubAutoTrack() != null) habit.setGithubAutoTrack(request.getGithubAutoTrack());
        if (request.getGithubEventType() != null) habit.setGithubEventType(request.getGithubEventType());

        habit = habitRepository.save(habit);

        boolean completedToday = isCompletedToday(habit.getId());
        
        LocalDateTime lastCompleted = habitLogRepository
                .findFirstByHabitIdOrderByCompletedAtDesc(habit.getId())
                .map(HabitLog::getCompletedAt)
                .orElse(null);

        log.info("Habit updated successfully: {}", habitId);

        return mapToHabitResponse(habit, completedToday, lastCompleted);
    }

    /**
     * Delete (archive) a habit
     */
    @Transactional
    public void deleteHabit(UUID userId, UUID habitId) {
        log.info("Deleting habit: {} for user: {}", habitId, userId);

        Habit habit = habitRepository.findByIdAndUserId(habitId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit", "id", habitId.toString()));

        habit.archive();
        habitRepository.save(habit);

        log.info("Habit archived successfully: {}", habitId);
    }

    /**
     * Check-in a habit (mark as done today)
     */
    @Transactional
    public HabitResponse checkInHabit(UUID userId, UUID habitId, CheckInRequest request) {
        log.info("Check-in habit: {} for user: {}", habitId, userId);

        Habit habit = habitRepository.findByIdAndUserId(habitId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit", "id", habitId.toString()));

        // Check if already completed today
        if (isCompletedToday(habit.getId())) {
            throw new BadRequestException("Habit already completed today");
        }

        // Create habit log
        HabitLog habitLog = HabitLog.builder()
                .habitId(habitId)
                .userId(userId)
                .completedAt(LocalDateTime.now())
                .note(request.getNote())
                .xpEarned(10)
                .build();

        habitLogRepository.save(habitLog);

        // Update habit stats
        habit.incrementCompletions();
        
        // Calculate and update streaks
        List<HabitLog> allLogs = habitLogRepository.findByHabitIdOrderByCompletedAtDesc(habitId);
	        int currentStreak = StreakCalculator.calculateCurrentStreak(allLogs);
	        int longestStreak = StreakCalculator.calculateLongestStreak(allLogs);
	
	        habit.setCurrentStreak(currentStreak);
	        if (longestStreak > habit.getLongestStreak()) {
	            habit.setLongestStreak(longestStreak);
        }

        // Update user XP
        User user = userRepository.findById(userId).orElseThrow();
        user.addXp(10);
        
     // Update user's overall streak (max of all habits)
        updateUserOverallStreak(userId);
        
        userRepository.save(user);

        habit = habitRepository.save(habit);

        log.info("Habit checked-in successfully: {}", habitId);

        return mapToHabitResponse(habit, true, LocalDateTime.now());
    }
    
    /**
     * Auto-complete a habit from GitHub event
     * Similar to checkInHabit but allows completion even if already done today
     * Returns the HabitLog ID
     */
    @Transactional
    public UUID autoCompleteHabitFromGitHub(UUID userId, UUID habitId, String note) {
        log.info("Auto-completing habit from GitHub: {} for user: {}", habitId, userId);

        Habit habit = habitRepository.findByIdAndUserId(habitId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit", "id", habitId.toString()));

        // Check if already completed today
        if (isCompletedToday(habit.getId())) {
            log.debug("Habit already completed today, skipping auto-completion");
            // Return existing log ID
            return habitLogRepository.findFirstByHabitIdOrderByCompletedAtDesc(habitId)
                    .map(HabitLog::getId)
                    .orElseThrow();
        }

        // Create habit log
        HabitLog habitLog = HabitLog.builder()
                .habitId(habitId)
                .userId(userId)
                .completedAt(LocalDateTime.now())
                .note(note)
                .xpEarned(10)
                .build();

        habitLog = habitLogRepository.save(habitLog);

        // Update habit stats
        habit.incrementCompletions();

        // Calculate and update streaks
        List<HabitLog> allLogs = habitLogRepository.findByHabitIdOrderByCompletedAtDesc(habitId);
        int currentStreak = StreakCalculator.calculateCurrentStreak(allLogs);
        int longestStreak = StreakCalculator.calculateLongestStreak(allLogs);

        habit.setCurrentStreak(currentStreak);
        if (longestStreak > habit.getLongestStreak()) {
            habit.setLongestStreak(longestStreak);
        }

        // Update user XP
        User user = userRepository.findById(userId).orElseThrow();
        user.addXp(10);

        // Update user's overall streak (max of all habits)
        updateUserOverallStreak(userId);

        userRepository.save(user);
        habitRepository.save(habit);

        log.info("Habit auto-completed from GitHub successfully: {}", habitId);

        return habitLog.getId();
    }

    /**
     * Update user's overall current streak (max across all habits)
     */
    private void updateUserOverallStreak(UUID userId) {
        List<Habit> habits = habitRepository.findByUserIdAndIsActiveTrue(userId);

        int maxStreak = habits.stream()
                .mapToInt(Habit::getCurrentStreak)
                .max()
                .orElse(0);

        User user = userRepository.findById(userId).orElseThrow();
        user.setCurrentStreak(maxStreak);

        // Update longest streak if needed
        int maxLongestStreak = habits.stream()
                .mapToInt(Habit::getLongestStreak)
                .max()
                .orElse(0);

        if (maxLongestStreak > user.getLongestStreak()) {
            user.setLongestStreak(maxLongestStreak);
        }
    }

    /**
     * Map Habit entity to HabitResponse DTO
     */
    private HabitResponse mapToHabitResponse(Habit habit, Boolean completedToday, LocalDateTime lastCompleted) {
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
}