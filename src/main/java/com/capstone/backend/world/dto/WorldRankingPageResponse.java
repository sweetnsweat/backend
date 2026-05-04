package com.capstone.backend.world.dto;

import java.util.List;

public record WorldRankingPageResponse(
        String metric,
        String genre,
        String keyword,
        int page,
        int size,
        long totalCount,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        Integer nextPage,
        List<WorldRankingDetailResponse> rankings
) {
}
