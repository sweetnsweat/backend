package com.capstone.backend.quest.dto;

public record QuestExerciseResponse(
        Long exerciseId,
        String exerciseName,
        String category,
        Integer seq,
        Integer targetSets,
        Integer targetReps,
        Integer targetDurationSec
) {
}
