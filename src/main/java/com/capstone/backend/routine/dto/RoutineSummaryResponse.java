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
        Boolean isDefault,
        Long sourceRoutineId,
        Boolean active
) {
    public static RoutineSummaryResponse from(Routine routine) {
        return from(routine, false);
    }

    public static RoutineSummaryResponse from(Routine routine, boolean active) {
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
                routine.getDefaultRoutine(),
                routine.getSourceRoutine() == null ? null : routine.getSourceRoutine().getId(),
                active
        );
    }
}
