package com.capstone.backend.user.dto;

public record MyPageResponse(
        Long id,
        String loginId,
        String nickname,
        String profileImageUrl,
        Integer level,
        Integer totalExp,
        Integer currentLevelExp,
        Integer nextLevelRequiredExp,
        Integer nextLevelRemainingExp,
        Integer balanceCurrency,
        Integer currentStreakDays,
        Long activeRoutineId,
        String activeRoutineName,
        Boolean onboardingCompleted,
        Boolean todayConditionCompleted,
        Boolean routineSetupRequired,
        WeeklyStatsResponse weeklyStats
) {
}
