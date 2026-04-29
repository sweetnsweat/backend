package com.capstone.backend.condition.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConditionTodayRequest(
        @NotNull(message = "conditionLevel is required")
        @Min(value = 1, message = "conditionLevel must be at least 1")
        @Max(value = 5, message = "conditionLevel must be at most 5")
        Integer conditionLevel,

        @NotNull(message = "sleepScore is required")
        @Min(value = 1, message = "sleepScore must be at least 1")
        @Max(value = 4, message = "sleepScore must be at most 4")
        Integer sleepScore,

        @NotNull(message = "stressScore is required")
        @Min(value = 1, message = "stressScore must be at least 1")
        @Max(value = 5, message = "stressScore must be at most 5")
        Integer stressScore,

        @NotNull(message = "energyLevel is required")
        @Min(value = 1, message = "energyLevel must be at least 1")
        @Max(value = 5, message = "energyLevel must be at most 5")
        Integer energyLevel
) {
}
