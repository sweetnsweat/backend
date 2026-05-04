package com.capstone.backend.user.dto;

import java.time.LocalDate;

public record WeeklyStatsResponse(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        int completedWorkoutCount,
        int maxStreakDays,
        int estimatedCaloriesKcal,
        int earnedExp
) {
}
