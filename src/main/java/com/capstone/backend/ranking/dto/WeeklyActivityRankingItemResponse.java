package com.capstone.backend.ranking.dto;

public record WeeklyActivityRankingItemResponse(
        int rank,
        Long userId,
        String nickname,
        int weeklyExp,
        boolean isMe
) {
}
