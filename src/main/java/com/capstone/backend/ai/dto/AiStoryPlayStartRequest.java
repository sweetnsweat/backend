package com.capstone.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "AI 스토리 처음부터 시작 요청")
public record AiStoryPlayStartRequest(
        @Schema(description = "처음부터 시작할 시나리오 ID", example = "1")
        @JsonProperty("scenario_id")
        @NotNull(message = "시나리오 ID를 입력해 주세요.")
        @Positive(message = "시나리오 ID는 1 이상이어야 합니다.")
        Long scenarioId
) {
}
