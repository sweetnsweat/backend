package com.capstone.backend.exercise.dto;

public record ExerciseFavoriteResponse(
        Long exerciseId,
        Boolean liked
) {
}
