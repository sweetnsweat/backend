package com.capstone.backend.routine.dto;

import com.capstone.backend.routine.entity.Routine;
import java.util.List;

public record RoutineSummaryResponse(
        Long id,
        String name,
        String description,
        String difficulty,
        Integer estimatedMinutes,
        String targetExperienceLevel,
        List<String> targetCurrentExerciseStatuses,
        List<String> goalTypes,
        List<String> placeTypes,
        Integer weeklyFrequency,
        List<String> recommendedExerciseTypes,
        Boolean isDefault
) {
    public static RoutineSummaryResponse from(Routine routine) {
        return new RoutineSummaryResponse(
                routine.getId(),
                routine.getName(),
                routine.getDescription(),
                routine.getDifficulty(),
                routine.getEstimatedMinutes(),
                routine.getTargetExperienceLevel(),
                routine.getTargetCurrentExerciseStatuses(),
                routine.getGoalTypes(),
                routine.getPlaceTypes(),
                routine.getWeeklyFrequency(),
                routine.getRecommendedExerciseTypes(),
                routine.getDefaultRoutine()
        );
    }
}
