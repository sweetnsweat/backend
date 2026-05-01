package com.capstone.backend.routine.dto;

import java.util.List;

public record RoutineRecommendationResponse(
        RoutineSummaryResponse routine,
        Integer score,
        List<String> reasons
) {
}
