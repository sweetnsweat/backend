package com.capstone.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "AI 스토리 플레이 통합 진행 요청")
public record AiStoryPlayRequest(
        @Schema(description = "플레이할 시나리오 ID", example = "1")
        @JsonProperty("scenario_id")
        @NotNull(message = "시나리오 ID를 입력해 주세요.")
        @Positive(message = "시나리오 ID는 1 이상이어야 합니다.")
        Long scenarioId,

        @Schema(description = "사용자 입력/대사/행동. 선택지만 고를 때는 null 또는 생략 가능합니다.", example = "주변을 둘러본다.")
        @JsonProperty("user_message")
        @Size(max = 5000, message = "사용자 입력은 최대 5000자까지 입력할 수 있습니다.")
        String userMessage,

        @Schema(description = "선택지 ID. 응답의 choices[].choice_id 중 하나를 선택할 때만 보냅니다.", example = "37")
        @JsonProperty("choice_id")
        @Positive(message = "선택지 ID는 1 이상이어야 합니다.")
        Long choiceId,

        @Schema(description = "true면 기존 진행을 버리고 처음부터 다시 시작합니다.", example = "false")
        Boolean restart
) {
}
