package com.capstone.backend.auth.dto;

import com.capstone.backend.global.media.MediaUrlResolver;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.reward.policy.LevelPolicy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UserProfileResponse(
        Long id,
        String email,
        String loginId,
        String nickname,
        String profileImageUrl,
        Integer level,
        Integer totalExp,
        Integer currentLevelExp,
        Integer nextLevelRequiredExp,
        Integer nextLevelRemainingExp,
        Integer balanceCurrency,
        String gender,
        LocalDate birthDate,
        BigDecimal heightCm,
        BigDecimal weightKg,
        String experienceLevel,
        String currentExerciseStatus,
        String fitnessGoal,
        String preferredWorkoutPlace,
        Integer weeklyWorkoutFrequency,
        Integer availableWorkoutMinutes,
        List<String> preferredExerciseTypes,
        Boolean onboardingCompleted,
        Boolean requiresOnboarding,
        Boolean todayConditionCompleted,
        Long activeRoutineId,
        Boolean routineSetupRequired,
        Boolean pushEnabled,
        Boolean pushQuestEnabled,
        Boolean pushRoutineEnabled,
        Boolean pushCompetitionEnabled,
        String status
) {
    public static UserProfileResponse from(User user) {
        return from(user, false, 0);
    }

    public static UserProfileResponse from(User user, boolean todayConditionCompleted) {
        return from(user, todayConditionCompleted, 0);
    }

    public static UserProfileResponse from(User user, boolean todayConditionCompleted, int balanceCurrency) {
        return from(user, todayConditionCompleted, balanceCurrency, null);
    }

    public static UserProfileResponse from(User user,
                                           boolean todayConditionCompleted,
                                           int balanceCurrency,
                                           MediaUrlResolver mediaUrlResolver) {
        int totalExp = user.getTotalExp();
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getLoginId(),
                user.getNickname(),
                resolveProfileImageUrl(user.getProfileImageUrl(), mediaUrlResolver),
                user.getLevel(),
                totalExp,
                LevelPolicy.currentLevelExp(totalExp),
                LevelPolicy.nextLevelRequiredExp(totalExp),
                LevelPolicy.nextLevelRemainingExp(totalExp),
                balanceCurrency,
                user.getGender(),
                user.getBirthDate(),
                user.getHeightCm(),
                user.getWeightKg(),
                user.getExperienceLevel(),
                user.getCurrentExerciseStatus(),
                user.getFitnessGoal(),
                user.getPreferredWorkoutPlace(),
                user.getWeeklyWorkoutFrequency(),
                user.getAvailableWorkoutMinutes(),
                user.getPreferredExerciseTypes(),
                user.isOnboardingCompleted(),
                !user.isOnboardingCompleted(),
                todayConditionCompleted,
                user.getActiveRoutine() == null ? null : user.getActiveRoutine().getId(),
                user.isOnboardingCompleted() && user.getActiveRoutine() == null,
                user.getPushEnabled(),
                user.getPushQuestEnabled(),
                user.getPushRoutineEnabled(),
                user.getPushCompetitionEnabled(),
                user.getStatus()
        );
    }

    private static String resolveProfileImageUrl(String profileImageUrl, MediaUrlResolver mediaUrlResolver) {
        if (mediaUrlResolver == null) {
            return profileImageUrl;
        }
        return mediaUrlResolver.resolve(profileImageUrl);
    }
}
