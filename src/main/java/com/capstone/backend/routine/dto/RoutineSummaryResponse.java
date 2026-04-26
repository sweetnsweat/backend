package com.capstone.backend.routine.dto;

import com.capstone.backend.routine.entity.Routine;

public record RoutineSummaryResponse(
        Long id,
        String name,
        String description,
        String difficulty,
        Integer estimatedMinutes,
        Boolean isDefault
) {
    public static RoutineSummaryResponse from(Routine routine) {
        return new RoutineSummaryResponse(
                routine.getId(),
                routine.getName(),
                routine.getDescription(),
                routine.getDifficulty(),
                routine.getEstimatedMinutes(),
                routine.getDefaultRoutine()
        );
    }
}
