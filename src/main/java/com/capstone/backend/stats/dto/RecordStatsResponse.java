package com.capstone.backend.stats.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RecordStatsResponse(
        RecordStatsPeriod period,
        LocalDate startDate,
        LocalDate endDate,
        RecordStatsSummary summary,
        List<ConditionTrendPoint> conditionTrend,
        List<ExerciseEffect> exerciseEffects,
        List<DailyRecord> dailyRecords,
        Insight insight
) {
    public record RecordStatsSummary(
            BigDecimal averageConditionScore,
            BigDecimal averageConditionLevel,
            BigDecimal averageEnergyLevel,
            BigDecimal averageStressScore,
            Integer exerciseCount,
            Integer completedQuestCount,
            Integer healthSyncedDays,
            Integer improvementRatePercent,
            Integer totalExerciseMinutes,
            Integer totalSteps,
            Integer totalDistanceMeters,
            Integer totalActiveCaloriesKcal
    ) {
    }

    public record ConditionTrendPoint(
            LocalDate date,
            String label,
            Integer conditionLevel,
            BigDecimal conditionScore,
            Integer energyLevel,
            Integer stressScore,
            Integer exerciseMinutes,
            Integer steps,
            Integer distanceMeters,
            Integer activeCaloriesKcal,
            Integer completedQuestCount
    ) {
    }

    public record ExerciseEffect(
            String exerciseType,
            String label,
            Integer completedCount,
            Integer exerciseMinutes,
            BigDecimal averageConditionScore,
            BigDecimal conditionDelta,
            BigDecimal averageStressScore
    ) {
    }

    public record DailyRecord(
            LocalDate date,
            String dayOfWeek,
            String exerciseLabel,
            Integer conditionLevel,
            BigDecimal conditionScore,
            Integer energyLevel,
            Integer stressScore,
            Integer exerciseMinutes,
            Integer steps,
            Integer activeCaloriesKcal,
            Integer completedQuestCount
    ) {
    }

    public record Insight(
            String title,
            String summary,
            String recommendation
    ) {
    }
}
