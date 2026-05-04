package com.capstone.backend.routine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "사용자 루틴 수정 요청")
public record UpdateCustomRoutineRequest(
        @Schema(description = "루틴 이름", example = "내 전신 루틴")
        @NotBlank(message = "루틴 이름을 입력해 주세요.")
        @Size(max = 255, message = "루틴 이름은 최대 255자까지 입력할 수 있습니다.")
        String name,

        @Schema(description = "루틴 설명", example = "수정한 전신 루틴")
        @Size(max = 1000, message = "루틴 설명은 최대 1000자까지 입력할 수 있습니다.")
        String description,

        @Schema(description = "요일별 세션 목록")
        @Valid
        @NotEmpty(message = "루틴 세션을 하나 이상 입력해 주세요.")
        @Size(max = 14, message = "루틴 세션은 최대 14개까지 입력할 수 있습니다.")
        List<CreateCustomRoutineRequest.SessionRequest> sessions
) {
}
