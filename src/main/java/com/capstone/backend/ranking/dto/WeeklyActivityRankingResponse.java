package com.capstone.backend.ranking.dto;

import java.time.LocalDate;
import java.util.List;

public record WeeklyActivityRankingResponse(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        String metric,
        List<WeeklyActivityRankingItemResponse> rankings
) {
}
