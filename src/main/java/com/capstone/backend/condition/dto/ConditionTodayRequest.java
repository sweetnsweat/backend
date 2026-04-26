package com.capstone.backend.condition.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConditionTodayRequest(
        @NotNull(message = "sleepScore is required")
        @Min(value = 1, message = "sleepScore must be at least 1")
        @Max(value = 5, message = "sleepScore must be at most 5")
        Integer sleepScore,

        @NotNull(message = "stressScore is required")
        @Min(value = 1, message = "stressScore must be at least 1")
        @Max(value = 5, message = "stressScore must be at most 5")
        Integer stressScore,

        @NotNull(message = "fatigueScore is required")
        @Min(value = 1, message = "fatigueScore must be at least 1")
        @Max(value = 5, message = "fatigueScore must be at most 5")
        Integer fatigueScore,

        @Size(max = 1000, message = "memo must have at most 1000 characters")
        String memo
) {
}
