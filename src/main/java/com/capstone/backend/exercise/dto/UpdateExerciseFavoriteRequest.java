package com.capstone.backend.exercise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "운동 즐겨찾기 설정 요청")
public record UpdateExerciseFavoriteRequest(
        @Schema(description = "즐겨찾기 여부. true면 추가, false면 해제", example = "true")
        @NotNull(message = "즐겨찾기 여부를 입력해 주세요.")
        Boolean liked
) {
}
