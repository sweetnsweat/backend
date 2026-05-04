package com.capstone.backend.exercise.dto;

import java.util.List;

public record ExerciseListResponse(
        String scope,
        String category,
        String level,
        String keyword,
        int page,
        int size,
        long totalCount,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        Integer nextPage,
        List<ExerciseGroupResponse> groups
) {
    public record ExerciseGroupResponse(
            String category,
            String categoryDisplayName,
            int count,
            List<ExerciseCardResponse> exercises
    ) {
    }
}
