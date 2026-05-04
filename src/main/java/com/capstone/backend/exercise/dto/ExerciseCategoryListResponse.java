package com.capstone.backend.exercise.dto;

import java.util.List;

public record ExerciseCategoryListResponse(
        int page,
        int size,
        long totalCount,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        Integer nextPage,
        List<ExerciseCategoryResponse> categories
) {
}
