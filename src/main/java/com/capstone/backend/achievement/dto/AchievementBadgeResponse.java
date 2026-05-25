package com.capstone.backend.achievement.dto;

import com.capstone.backend.shop.entity.Item;
import com.capstone.backend.shop.entity.UserItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

@Schema(description = "배지 상세 응답")
public record AchievementBadgeResponse(
        @Schema(description = "배지 아이템 ID", example = "22")
        Long itemId,
        @Schema(description = "배지 코드", example = "FIRST_QUEST_COMPLETE")
        String badgeCode,
        @Schema(description = "배지 이름", example = "첫 퀘스트 완료")
        String name,
        @Schema(description = "배지 설명", example = "퀘스트를 처음 완료하면 획득합니다.")
        String description,
        @Schema(description = "배지 이미지 URL", example = "/media/assets/badge_first_quest.png")
        String imageUrl,
        @Schema(description = "획득 조건", example = "퀘스트 1회 완료")
        String criteria,
        @Schema(description = "현재 사용자의 획득 여부", example = "true")
        Boolean earned,
        @Schema(description = "획득 시각. 미획득이면 null")
        Instant earnedAt,
        @Schema(description = "배지 원본 메타데이터")
        Map<String, Object> metadata
) {
    public static AchievementBadgeResponse from(Item badge, UserItem userItem) {
        Map<String, Object> metadata = badge.getMetadata();
        return new AchievementBadgeResponse(
                badge.getId(),
                stringValue(metadata.get("badgeCode")),
                badge.getName(),
                badge.getDescription(),
                badge.getImageUrl(),
                stringValue(metadata.get("criteria")),
                userItem != null && userItem.getQuantity() > 0,
                userItem == null || userItem.getQuantity() <= 0 ? null : userItem.getUpdatedAt(),
                metadata
        );
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
