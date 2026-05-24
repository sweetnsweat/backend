package com.capstone.backend.achievement.dto;

import java.util.List;

public record AchievementBadgeListResponse(
        List<AchievementBadgeResponse> badges,
        Integer earnedCount,
        Integer totalCount
) {
    public static AchievementBadgeListResponse from(List<AchievementBadgeResponse> badges) {
        int earnedCount = (int) badges.stream()
                .filter(badge -> Boolean.TRUE.equals(badge.earned()))
                .count();
        return new AchievementBadgeListResponse(badges, earnedCount, badges.size());
    }
}
