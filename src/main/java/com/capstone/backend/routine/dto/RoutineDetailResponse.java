package com.capstone.backend.routine.dto;

import com.capstone.backend.routine.entity.Exercise;
import com.capstone.backend.routine.entity.Routine;
import com.capstone.backend.routine.entity.RoutineItem;
import com.capstone.backend.routine.entity.RoutineSession;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record RoutineDetailResponse(
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
        List<RoutineSessionResponse> sessions,
        List<RoutineItemResponse> items
) {
    public static RoutineDetailResponse from(Routine routine) {
        return from(routine, routine.getSessions(), routine.getItems());
    }

    public static RoutineDetailResponse from(Routine routine, List<RoutineSession> sessions) {
        return from(routine, sessions, routine.getItems());
    }

    public static RoutineDetailResponse from(Routine routine, List<RoutineSession> sessions, List<RoutineItem> items) {
        return new RoutineDetailResponse(
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
                sessions.stream()
                        .sorted(Comparator.comparing(RoutineSession::getSeq))
                        .map(RoutineSessionResponse::from)
                        .toList(),
                items.stream()
                        .sorted(Comparator.comparing(RoutineItem::getSeq))
                        .map(RoutineItemResponse::from)
                        .toList()
        );
    }

    public record RoutineSessionResponse(
            Long id,
            Integer seq,
            String dayOfWeek,
            String dayOfWeekDisplayName,
            String sessionName,
            String sessionType,
            String sessionTypeDisplayName,
            Integer estimatedMinutes,
            Boolean active,
            List<RoutineItemResponse> items
    ) {
        public static RoutineSessionResponse from(RoutineSession session) {
            return new RoutineSessionResponse(
                    session.getId(),
                    session.getSeq(),
                    session.getDayOfWeek(),
                    displayDayOfWeek(session.getDayOfWeek()),
                    session.getSessionName(),
                    session.getSessionType(),
                    displaySessionType(session.getSessionType()),
                    session.getEstimatedMinutes(),
                    session.getActive(),
                    session.getItems().stream()
                            .sorted(Comparator.comparing(RoutineItem::getSeq))
                            .map(RoutineItemResponse::from)
                            .toList()
            );
        }
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

    private static String displayDayOfWeek(String dayOfWeek) {
        return switch (dayOfWeek == null ? "" : dayOfWeek) {
            case "MONDAY" -> "월요일";
            case "TUESDAY" -> "화요일";
            case "WEDNESDAY" -> "수요일";
            case "THURSDAY" -> "목요일";
            case "FRIDAY" -> "금요일";
            case "SATURDAY" -> "토요일";
            case "SUNDAY" -> "일요일";
            default -> dayOfWeek;
        };
    }

    private static String displaySessionType(String sessionType) {
        return switch (sessionType == null ? "" : sessionType) {
            case "upper_body" -> "상체";
            case "lower_body" -> "하체";
            case "full_body" -> "전신";
            case "core_recovery" -> "코어/회복";
            case "recovery" -> "회복";
            case "mobility" -> "가동성";
            case "cardio" -> "유산소";
            case "cardio_core" -> "유산소/코어";
            case "cardio_recovery" -> "유산소/회복";
            default -> sessionType;
        };
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
