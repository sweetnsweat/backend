package com.capstone.backend.exercise.dto;

public record ExerciseCategoryResponse(
        String category,
        String categoryDisplayName
) {
    public static ExerciseCategoryResponse from(String category) {
        return new ExerciseCategoryResponse(category, ExerciseCardResponse.displayCategory(category));
    }
}
