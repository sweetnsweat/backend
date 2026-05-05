package com.capstone.backend.ai.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AiStoryQuestListRequest {

    @Parameter(name = "scenario_id", description = "스토리 퀘스트를 조회할 시나리오 ID", required = true, example = "4")
    @NotNull(message = "시나리오 ID를 입력해 주세요.")
    @Positive(message = "시나리오 ID는 1 이상이어야 합니다.")
    private Long scenarioId;

    @Parameter(description = "가져올 퀘스트 수. AI 서버 허용 범위는 1~300입니다.", example = "100")
    @Min(value = 1, message = "조회 개수는 1 이상이어야 합니다.")
    @Max(value = 300, message = "조회 개수는 300 이하여야 합니다.")
    private Integer limit;

    @Parameter(description = "건너뛸 퀘스트 수. 기본값은 0입니다.", example = "0")
    @Min(value = 0, message = "offset은 0 이상이어야 합니다.")
    private Integer offset;

    public Long getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
    }

    public void setScenario_id(Long scenarioId) {
        this.scenarioId = scenarioId;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public int resolvedLimit() {
        return limit == null ? 100 : limit;
    }

    public int resolvedOffset() {
        return offset == null ? 0 : offset;
    }
}
