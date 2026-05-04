package com.capstone.backend.quest.dto;

import com.capstone.backend.quest.entity.UserQuest;
import com.capstone.backend.routine.entity.Routine;
import com.capstone.backend.routine.entity.RoutineSession;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public record QuestResponse(
        Long id,
        LocalDate questDate,
        String questType,
        String targetMetric,
        String status,
        Boolean completed,
        String title,
        String description,
        Integer targetValue,
        Integer progressValue,
        Boolean conditionAdjusted,
        Long routineId,
        String routineName,
        Long sourceSessionId,
        String sessionName,
        String sessionType,
        String sessionTypeDisplayName,
        BigDecimal conditionScore,
        BigDecimal exerciseMultiplier,
        Integer rewardCurrency,
        Integer rewardExp,
        Instant completedAt,
        List<QuestExerciseResponse> exercises
) {

    public static QuestResponse from(UserQuest quest, List<QuestExerciseResponse> exercises) {
        Routine routine = quest.getRoutine();
        RoutineSession session = quest.getSourceSession();
        return new QuestResponse(
                quest.getId(),
                quest.getQuestDate(),
                upper(quest.getQuestType()),
                upper(quest.getTargetMetric()),
                upper(quest.getStatus()),
                UserQuest.STATUS_COMPLETED.equals(quest.getStatus()),
                quest.getTitle(),
                quest.getDescription(),
                quest.getTargetValue(),
                quest.getProgressValue(),
                quest.getConditionAdjusted(),
                routine == null ? null : routine.getId(),
                routine == null ? null : routine.getName(),
                session == null ? null : session.getId(),
                session == null ? null : session.getSessionName(),
                session == null ? null : session.getSessionType(),
                session == null ? null : displaySessionType(session.getSessionType()),
                quest.getConditionLog() == null ? null : quest.getConditionLog().getConditionScore(),
                quest.getConditionLog() == null ? null : quest.getConditionLog().getExerciseMultiplier(),
                quest.getRewardCurrency(),
                quest.getRewardExp(),
                quest.getCompletedAt(),
                exercises
        );
    }

    private static String upper(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
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
}
