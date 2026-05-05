package com.capstone.backend.story.dto;

import java.util.List;

public record StoryChatListResponse(
        int limit,
        long totalCount,
        List<StoryChatSummaryResponse> chats
) {
}
