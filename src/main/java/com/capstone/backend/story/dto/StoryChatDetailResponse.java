package com.capstone.backend.story.dto;

import java.util.List;

public record StoryChatDetailResponse(
        StoryChatSummaryResponse chat,
        List<StoryChatCharacterResponse> characters,
        int messageLimit,
        long messageTotalCount,
        boolean hasMoreMessages,
        List<StoryChatMessageResponse> recentMessages
) {
}
