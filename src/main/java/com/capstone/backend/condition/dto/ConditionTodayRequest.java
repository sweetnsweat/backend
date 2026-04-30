package com.capstone.backend.condition.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConditionTodayRequest(
        @NotNull(message = "오늘 컨디션을 선택해 주세요.")
        @Min(value = 1, message = "오늘 컨디션은 1 이상이어야 합니다.")
        @Max(value = 5, message = "오늘 컨디션은 5 이하여야 합니다.")
        Integer conditionLevel,

        @NotNull(message = "수면 상태를 선택해 주세요.")
        @Min(value = 1, message = "수면 상태는 1 이상이어야 합니다.")
        @Max(value = 4, message = "수면 상태는 4 이하여야 합니다.")
        Integer sleepScore,

        @NotNull(message = "스트레스 정도를 선택해 주세요.")
        @Min(value = 1, message = "스트레스 정도는 1 이상이어야 합니다.")
        @Max(value = 5, message = "스트레스 정도는 5 이하여야 합니다.")
        Integer stressScore,

        @NotNull(message = "에너지 레벨을 선택해 주세요.")
        @Min(value = 1, message = "에너지 레벨은 1 이상이어야 합니다.")
        @Max(value = 5, message = "에너지 레벨은 5 이하여야 합니다.")
        Integer energyLevel
) {
}
