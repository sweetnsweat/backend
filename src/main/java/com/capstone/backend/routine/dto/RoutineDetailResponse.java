package com.capstone.backend.routine.dto;

import com.capstone.backend.routine.entity.Exercise;
import com.capstone.backend.routine.entity.Routine;
import com.capstone.backend.routine.entity.RoutineItem;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record RoutineDetailResponse(
        Long id,
        String name,
        String description,
        String difficulty,
        Integer estimatedMinutes,
        Boolean isDefault,
        List<RoutineItemResponse> items
) {
    public static RoutineDetailResponse from(Routine routine) {
        return new RoutineDetailResponse(
                routine.getId(),
                routine.getName(),
                routine.getDescription(),
                routine.getDifficulty(),
                routine.getEstimatedMinutes(),
                routine.getDefaultRoutine(),
                routine.getItems().stream()
                        .sorted(Comparator.comparing(RoutineItem::getSeq))
                        .map(RoutineItemResponse::from)
                        .toList()
        );
    }

    public record RoutineItemResponse(
            Long id,
            Integer seq,
            Integer reps,
            Integer sets,
            Integer durationSec,
            Integer restSec,
            ExerciseResponse exercise
    ) {
        public static RoutineItemResponse from(RoutineItem item) {
            return new RoutineItemResponse(
                    item.getId(),
                    item.getSeq(),
                    item.getReps(),
                    item.getSets(),
                    item.getDurationSec(),
                    item.getRestSec(),
                    ExerciseResponse.from(item.getExercise())
            );
        }
    }

    public record ExerciseResponse(
            Long id,
            String externalId,
            String name,
            String category,
            String intensity,
            String level,
            String force,
            String mechanic,
            String equipment,
            BigDecimal met,
            List<String> primaryMuscles,
            List<String> secondaryMuscles,
            List<String> instructions,
            List<String> imageUrls,
            String source,
            String sourceLicense,
            String sourceUrl
    ) {
        public static ExerciseResponse from(Exercise exercise) {
            return new ExerciseResponse(
                    exercise.getId(),
                    exercise.getExternalId(),
                    exercise.getName(),
                    exercise.getCategory(),
                    exercise.getIntensity(),
                    exercise.getLevel(),
                    exercise.getForce(),
                    exercise.getMechanic(),
                    exercise.getEquipment(),
                    exercise.getMet(),
                    exercise.getPrimaryMuscles(),
                    exercise.getSecondaryMuscles(),
                    exercise.getInstructions(),
                    exercise.getImageUrls(),
                    exercise.getSource(),
                    exercise.getSourceLicense(),
                    exercise.getSourceUrl()
            );
        }
    }
}
