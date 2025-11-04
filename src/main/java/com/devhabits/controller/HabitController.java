package com.devhabits.controller;

import com.devhabits.model.dto.request.CheckInRequest;
import com.devhabits.model.dto.request.CreateHabitRequest;
import com.devhabits.model.dto.request.UpdateHabitRequest;
import com.devhabits.model.dto.response.ApiResponse;
import com.devhabits.model.dto.response.HabitResponse;
import com.devhabits.service.HabitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/habits")
@RequiredArgsConstructor
@Tag(name = "Habits", description = "Habit management APIs")
@SecurityRequirement(name = "bearerAuth")
public class HabitController {

    private final HabitService habitService;

    @Operation(summary = "Create a new habit")
    @PostMapping
    public ResponseEntity<ApiResponse<HabitResponse>> createHabit(
            @Valid @RequestBody CreateHabitRequest request,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        HabitResponse habit = habitService.createHabit(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Habit created successfully", habit));
    }

    @Operation(summary = "Get all habits for current user")
    @GetMapping
    public ResponseEntity<ApiResponse<List<HabitResponse>>> getUserHabits(
            @RequestParam(required = false) Boolean includeArchived,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        List<HabitResponse> habits = habitService.getUserHabits(userId, includeArchived);

        return ResponseEntity.ok(ApiResponse.success(habits));
    }

    @Operation(summary = "Get a single habit by ID")
    @GetMapping("/{habitId}")
    public ResponseEntity<ApiResponse<HabitResponse>> getHabitById(
            @PathVariable UUID habitId,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        HabitResponse habit = habitService.getHabitById(userId, habitId);

        return ResponseEntity.ok(ApiResponse.success(habit));
    }

    @Operation(summary = "Update a habit")
    @PutMapping("/{habitId}")
    public ResponseEntity<ApiResponse<HabitResponse>> updateHabit(
            @PathVariable UUID habitId,
            @Valid @RequestBody UpdateHabitRequest request,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        HabitResponse habit = habitService.updateHabit(userId, habitId, request);

        return ResponseEntity.ok(ApiResponse.success("Habit updated successfully", habit));
    }

    @Operation(summary = "Delete (archive) a habit")
    @DeleteMapping("/{habitId}")
    public ResponseEntity<ApiResponse<Void>> deleteHabit(
            @PathVariable UUID habitId,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        habitService.deleteHabit(userId, habitId);

        return ResponseEntity.ok(ApiResponse.success("Habit deleted successfully", null));
    }

    @Operation(summary = "Check-in a habit (mark as done today)")
    @PostMapping("/{habitId}/check-in")
    public ResponseEntity<ApiResponse<HabitResponse>> checkInHabit(
            @PathVariable UUID habitId,
            @Valid @RequestBody CheckInRequest request,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        HabitResponse habit = habitService.checkInHabit(userId, habitId, request);

        return ResponseEntity.ok(ApiResponse.success("Habit checked-in successfully", habit));
    }
}