package com.capstone.backend.auth.dto;

import com.capstone.backend.user.entity.User;

public record UserProfileResponse(
        Long id,
        String loginId,
        String nickname,
        String exerciseExperience,
        String preferredExercise
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getLoginId(),
                user.getNickname(),
                user.getExperienceLevel(),
                null
        );
    }
}
