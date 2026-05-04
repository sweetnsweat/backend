package com.capstone.backend.world.dto;

import java.util.List;

public record WorldRankingListResponse(
        String metric,
        List<WorldRankingResponse> rankings
) {
}
