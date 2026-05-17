package com.capstone.backend.statistics.dto;

public record AiInsightResponse(
        String title,
        String summary,
        String recommendation
) {
}
