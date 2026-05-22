package com.capstone.backend.battle.dto;

import java.util.List;

public record BattleHistoryPageResponse(
        int page,
        int size,
        long totalCount,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        Integer nextPage,
        List<BattleHistoryItemResponse> battles
) {
}
