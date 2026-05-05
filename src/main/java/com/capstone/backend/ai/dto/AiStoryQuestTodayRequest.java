package com.capstone.backend.ai.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AiStoryQuestTodayRequest {

    @Parameter(name = "scenario_id", description = "스토리 퀘스트를 생성/조회할 시나리오 ID", required = true, example = "4")
    @NotNull(message = "시나리오 ID를 입력해 주세요.")
    @Positive(message = "시나리오 ID는 1 이상이어야 합니다.")
    private Long scenarioId;

    public Long getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
    }

    public void setScenario_id(Long scenarioId) {
        this.scenarioId = scenarioId;
    }
}
