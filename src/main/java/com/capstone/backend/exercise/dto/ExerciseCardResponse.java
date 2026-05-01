package com.capstone.backend.exercise.dto;

import com.capstone.backend.routine.entity.Exercise;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record ExerciseCardResponse(
        Long id,
        String name,
        String category,
        String categoryDisplayName,
        String level,
        String levelDisplayName,
        String equipment,
        BigDecimal met,
        Integer estimatedKcalPerHour,
        List<String> primaryMuscles,
        String emoji,
        String imageUrl,
        Boolean liked
) {
    private static final BigDecimal DEFAULT_WEIGHT_KG = BigDecimal.valueOf(70);

    public static ExerciseCardResponse from(Exercise exercise, boolean liked) {
        return from(exercise, liked, DEFAULT_WEIGHT_KG);
    }

    public static ExerciseCardResponse from(Exercise exercise, boolean liked, BigDecimal weightKg) {
        return new ExerciseCardResponse(
                exercise.getId(),
                exercise.getName(),
                exercise.getCategory(),
                displayCategory(exercise.getCategory()),
                exercise.getLevel(),
                displayLevel(exercise.getLevel()),
                exercise.getEquipment(),
                exercise.getMet(),
                estimatedKcalPerHour(exercise.getMet(), weightKg),
                exercise.getPrimaryMuscles(),
                emoji(exercise.getCategory()),
                firstImageUrl(exercise.getImageUrls()),
                liked
        );
    }

    public static String displayCategory(String category) {
        if ("근력".equals(category)) {
            return "헬스";
        }
        return category;
    }

    public static String displayLevel(String level) {
        if ("beginner".equals(level)) {
            return "입문";
        }
        if ("초급".equals(level)) {
            return "초급";
        }
        if ("intermediate".equals(level)) {
            return "중급";
        }
        if ("advanced".equals(level)) {
            return "고급";
        }
        return level;
    }

    private static Integer estimatedKcalPerHour(BigDecimal met, BigDecimal weightKg) {
        if (met == null) {
            return null;
        }
        BigDecimal effectiveWeightKg = weightKg == null ? DEFAULT_WEIGHT_KG : weightKg;
        return met.multiply(effectiveWeightKg).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private static String emoji(String category) {
        return switch (category == null ? "" : category) {
            case "수영" -> "🏊";
            case "요가" -> "🧘";
            case "러닝" -> "🏃";
            case "사이클" -> "🚴";
            case "필라테스" -> "🤸";
            case "스트레칭" -> "🧘";
            case "유산소" -> "🔥";
            case "근력" -> "🏋️";
            default -> "💪";
        };
    }

    private static String firstImageUrl(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return null;
        }
        return imageUrls.getFirst();
    }
}
