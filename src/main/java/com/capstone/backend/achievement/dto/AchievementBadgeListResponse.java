package com.capstone.backend.achievement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "사용자 배지 목록 응답")
public record AchievementBadgeListResponse(
        @Schema(description = "전체 배지 목록과 획득 상태")
        List<AchievementBadgeResponse> badges,
        @Schema(description = "현재 사용자가 획득한 배지 수", example = "3")
        Integer earnedCount,
        @Schema(description = "전체 배지 수", example = "7")
        Integer totalCount
) {
    public static AchievementBadgeListResponse from(List<AchievementBadgeResponse> badges) {
        int earnedCount = (int) badges.stream()
                .filter(badge -> Boolean.TRUE.equals(badge.earned()))
                .count();
        return new AchievementBadgeListResponse(badges, earnedCount, badges.size());
    }
}
