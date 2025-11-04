package com.devhabits.model.dto.request;

import com.devhabits.model.enums.HabitCategory;
import com.devhabits.model.enums.HabitFrequency;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateHabitRequest {

    @NotBlank(message = "Habit name is required")
    @Size(min = 3, max = 200, message = "Habit name must be between 3 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private HabitCategory category;

    @NotNull(message = "Frequency is required")
    private HabitFrequency frequency;

    @Min(value = 1, message = "Target count must be at least 1")
    @Max(value = 100, message = "Target count cannot exceed 100")
    private Integer targetCount = 1;

    @Size(max = 50, message = "Icon cannot exceed 50 characters")
    private String icon;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Invalid color format")
    private String color;

    private Boolean reminderEnabled = false;

    private LocalTime reminderTime;
}