package com.capstone.backend.exercise.dto;

import com.capstone.backend.routine.entity.Exercise;
import java.math.BigDecimal;
import java.util.List;

public record ExerciseDetailResponse(
        Long id,
        String externalId,
        String name,
        String category,
        String categoryDisplayName,
        String intensity,
        String level,
        String levelDisplayName,
        String force,
        String mechanic,
        String equipment,
        BigDecimal met,
        Integer estimatedKcalPerHour,
        List<String> primaryMuscles,
        List<String> secondaryMuscles,
        List<String> instructions,
        List<String> imageUrls,
        String source,
        String sourceLicense,
        String sourceUrl,
        String emoji,
        Boolean liked
) {
    public static ExerciseDetailResponse from(Exercise exercise, boolean liked) {
        return from(exercise, liked, null);
    }

    public static ExerciseDetailResponse from(Exercise exercise, boolean liked, BigDecimal weightKg) {
        ExerciseCardResponse card = ExerciseCardResponse.from(exercise, liked, weightKg);
        return new ExerciseDetailResponse(
                exercise.getId(),
                exercise.getExternalId(),
                exercise.getName(),
                exercise.getCategory(),
                card.categoryDisplayName(),
                exercise.getIntensity(),
                exercise.getLevel(),
                card.levelDisplayName(),
                exercise.getForce(),
                exercise.getMechanic(),
                exercise.getEquipment(),
                exercise.getMet(),
                card.estimatedKcalPerHour(),
                exercise.getPrimaryMuscles(),
                exercise.getSecondaryMuscles(),
                exercise.getInstructions(),
                exercise.getImageUrls(),
                exercise.getSource(),
                exercise.getSourceLicense(),
                exercise.getSourceUrl(),
                card.emoji(),
                liked
        );
    }
}
