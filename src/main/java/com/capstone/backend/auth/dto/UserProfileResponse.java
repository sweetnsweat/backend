package com.capstone.backend.auth.dto;

import com.capstone.backend.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UserProfileResponse(
        Long id,
        String email,
        String loginId,
        String nickname,
        String phone,
        String profileImageUrl,
        String gender,
        LocalDate birthDate,
        BigDecimal heightCm,
        BigDecimal weightKg,
        String experienceLevel,
        List<String> preferredExerciseTypes,
        Boolean onboardingCompleted,
        Boolean requiresOnboarding,
        Boolean todayConditionCompleted,
        Boolean pushEnabled,
        Boolean pushQuestEnabled,
        Boolean pushRoutineEnabled,
        Boolean pushCompetitionEnabled,
        String status
) {
    public static UserProfileResponse from(User user) {
        return from(user, false);
    }

    public static UserProfileResponse from(User user, boolean todayConditionCompleted) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getLoginId(),
                user.getNickname(),
                user.getPhone(),
                user.getProfileImageUrl(),
                user.getGender(),
                user.getBirthDate(),
                user.getHeightCm(),
                user.getWeightKg(),
                user.getExperienceLevel(),
                user.getPreferredExerciseTypes(),
                user.isOnboardingCompleted(),
                !user.isOnboardingCompleted(),
                todayConditionCompleted,
                user.getPushEnabled(),
                user.getPushQuestEnabled(),
                user.getPushRoutineEnabled(),
                user.getPushCompetitionEnabled(),
                user.getStatus()
        );
    }
}
