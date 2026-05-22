package com.capstone.backend.quest.dto;

import com.capstone.backend.quest.entity.UserQuest;
import com.capstone.backend.routine.entity.Routine;
import com.capstone.backend.routine.entity.RoutineSession;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Schema(description = "퀘스트 응답. 완료 후 completionType, verificationStatus, battleEligible로 검증 완료/수동 완료 여부와 배틀 반영 가능 여부를 확인합니다.")
public record QuestResponse(
        @Schema(description = "사용자 퀘스트 ID", example = "12")
        Long id,
        @Schema(description = "퀘스트 날짜", example = "2026-05-22")
        LocalDate questDate,
        @Schema(description = "퀘스트 유형", example = "ROUTINE")
        String questType,
        @Schema(description = "목표 지표", example = "ROUTINE")
        String targetMetric,
        @Schema(description = "퀘스트 상태", example = "COMPLETED")
        String status,
        @Schema(description = "완료 여부", example = "true")
        Boolean completed,
        @Schema(description = "완료 유형. VERIFIED는 건강 데이터 검증 성공, MANUAL은 데이터 없음/부족으로 수동 완료 처리된 상태입니다.", allowableValues = {"VERIFIED", "MANUAL"}, example = "VERIFIED")
        String completionType,
        @Schema(description = "검증 상태. VERIFIED는 검증 성공, NOT_PROVIDED는 건강 데이터 미전송, INSUFFICIENT_DATA는 건강 데이터가 있었지만 기준 부족입니다.", allowableValues = {"VERIFIED", "NOT_PROVIDED", "INSUFFICIENT_DATA"}, example = "VERIFIED")
        String verificationStatus,
        @Schema(description = "배틀/랭킹 반영 가능 여부. VERIFIED 완료만 true이며 MANUAL 완료는 false입니다.", example = "true")
        Boolean battleEligible,
        @Schema(description = "퀘스트 제목", example = "오늘 루틴 완료")
        String title,
        @Schema(description = "퀘스트 설명")
        String description,
        @Schema(description = "목표값", example = "1")
        Integer targetValue,
        @Schema(description = "진행값. VERIFIED 완료 시 건강 데이터 기반 값이 우선됩니다.", example = "1")
        Integer progressValue,
        @Schema(description = "컨디션에 의해 목표가 조정되었는지 여부", example = "false")
        Boolean conditionAdjusted,
        @Schema(description = "연결된 루틴 ID", example = "3")
        Long routineId,
        @Schema(description = "연결된 루틴 이름")
        String routineName,
        @Schema(description = "연결된 루틴 세션 ID")
        Long sourceSessionId,
        @Schema(description = "연결된 루틴 세션 이름")
        String sessionName,
        @Schema(description = "세션 유형", example = "strength")
        String sessionType,
        @Schema(description = "세션 유형 표시명", example = "근력")
        String sessionTypeDisplayName,
        @Schema(description = "퀘스트 생성 시점의 컨디션 점수", example = "72.92")
        BigDecimal conditionScore,
        @Schema(description = "컨디션 기반 운동 강도 배수", example = "1.00")
        BigDecimal exerciseMultiplier,
        @Schema(description = "실제 지급 또는 지급 예정 골드. 완료 전에는 예정 보상, 완료 후에는 VERIFIED/MANUAL 정책에 따른 실제 지급값입니다.", example = "15")
        Integer rewardCurrency,
        @Schema(description = "실제 지급 또는 지급 예정 EXP. 완료 전에는 예정 보상, 완료 후에는 VERIFIED/MANUAL 정책에 따른 실제 지급값입니다.", example = "30")
        Integer rewardExp,
        @Schema(description = "프론트 표시용 골드 보상. rewardCurrency와 같은 값입니다.", example = "15")
        Integer rewardGold,
        @Schema(description = "완료 시각")
        Instant completedAt,
        @Schema(description = "Health Connect 데이터를 읽어야 하는 검증 시간창")
        QuestVerificationWindowResponse verificationWindow,
        @Schema(description = "퀘스트에 포함된 운동 목록")
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
                completionType(quest),
                verificationStatus(quest),
                battleEligible(quest),
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
                rewardCurrency(quest),
                rewardExp(quest),
                rewardCurrency(quest),
                quest.getCompletedAt(),
                new QuestVerificationWindowResponse(
                        quest.getCreatedAt() == null ? null : quest.getCreatedAt().minus(Duration.ofMinutes(5)),
                        null
                ),
                exercises
        );
    }

    private static String completionType(UserQuest quest) {
        if (!UserQuest.STATUS_COMPLETED.equals(quest.getStatus())) {
            return null;
        }
        Object value = quest.getProofJson().get("completionType");
        if (value != null) {
            return upper(String.valueOf(value));
        }
        return Boolean.TRUE.equals(quest.getProofJson().get("verified")) ? "VERIFIED" : "MANUAL";
    }

    private static String verificationStatus(UserQuest quest) {
        if (!UserQuest.STATUS_COMPLETED.equals(quest.getStatus())) {
            return null;
        }
        Object value = quest.getProofJson().get("verificationStatus");
        if (value != null) {
            return upper(String.valueOf(value));
        }
        return Boolean.TRUE.equals(quest.getProofJson().get("verified")) ? "VERIFIED" : "NOT_PROVIDED";
    }

    private static Boolean battleEligible(UserQuest quest) {
        if (!UserQuest.STATUS_COMPLETED.equals(quest.getStatus())) {
            return null;
        }
        Object value = quest.getProofJson().get("battleEligible");
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.TRUE.equals(quest.getProofJson().get("verified"));
    }

    private static Integer rewardExp(UserQuest quest) {
        return completedReward(quest, "rewardExp", quest.getRewardExp());
    }

    private static Integer rewardCurrency(UserQuest quest) {
        return completedReward(quest, "rewardCurrency", quest.getRewardCurrency());
    }

    private static Integer completedReward(UserQuest quest, String key, Integer defaultValue) {
        if (!UserQuest.STATUS_COMPLETED.equals(quest.getStatus())) {
            return defaultValue;
        }
        Object value = quest.getProofJson().get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            return Integer.valueOf(string);
        }
        return defaultValue;
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
