package com.capstone.backend.statistics.dto;

import java.time.LocalDate;
import java.util.List;

public record ConditionStatisticsResponse(
        String period,
        LocalDate startDate,
        LocalDate endDate,
        List<DailyConditionPointResponse> conditionTrend,
        ConditionStatisticsSummaryResponse summary,
        List<ExerciseEffectResponse> exerciseEffects,
        AiInsightResponse aiInsight,
        List<WeeklyExerciseRecordResponse> weeklyRecords
) {
}
