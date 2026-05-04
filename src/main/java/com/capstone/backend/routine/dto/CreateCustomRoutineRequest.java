package com.capstone.backend.routine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "사용자 직접 루틴 생성 요청")
public record CreateCustomRoutineRequest(
        @Schema(description = "루틴 이름", example = "내 전신 루틴")
        @NotBlank(message = "루틴 이름을 입력해 주세요.")
        @Size(max = 255, message = "루틴 이름은 최대 255자까지 입력할 수 있습니다.")
        String name,

        @Schema(description = "루틴 설명", example = "직접 만든 전신 루틴")
        @Size(max = 1000, message = "루틴 설명은 최대 1000자까지 입력할 수 있습니다.")
        String description,

        @Schema(description = "저장 후 활성 루틴으로 설정할지 여부. 생략하면 true로 처리합니다.", example = "true")
        Boolean activate,

        @Schema(description = "요일별 세션 목록")
        @Valid
        @NotEmpty(message = "루틴 세션을 하나 이상 입력해 주세요.")
        @Size(max = 14, message = "루틴 세션은 최대 14개까지 입력할 수 있습니다.")
        List<SessionRequest> sessions
) {

    @Schema(description = "사용자 직접 루틴 세션")
    public record SessionRequest(
            @Schema(description = "요일. MONDAY~SUNDAY", example = "MONDAY")
            @NotBlank(message = "요일을 선택해 주세요.")
            @Pattern(
                    regexp = "MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY",
                    message = "요일 값이 올바르지 않습니다."
            )
            String dayOfWeek,

            @Schema(description = "세션 이름", example = "월요일 전신")
            @NotBlank(message = "세션 이름을 입력해 주세요.")
            @Size(max = 100, message = "세션 이름은 최대 100자까지 입력할 수 있습니다.")
            String sessionName,

            @Schema(description = "세션 유형. 예: full_body, upper_body, lower_body, cardio. 응답에는 화면 표시용 한글 라벨인 sessionTypeDisplayName이 함께 내려갑니다.", example = "full_body")
            @Size(max = 30, message = "세션 유형은 최대 30자까지 입력할 수 있습니다.")
            String sessionType,

            @Schema(description = "세션 예상 소요 시간(분)", example = "40")
            @Min(value = 1, message = "세션 예상 시간은 1분 이상이어야 합니다.")
            @Max(value = 300, message = "세션 예상 시간은 300분 이하여야 합니다.")
            Integer estimatedMinutes,

            @Schema(description = "세션 안의 운동 목록")
            @Valid
            @NotEmpty(message = "세션에는 운동을 하나 이상 입력해 주세요.")
            @Size(max = 30, message = "세션 운동은 최대 30개까지 입력할 수 있습니다.")
            List<ItemRequest> items
    ) {
    }

    @Schema(description = "사용자 직접 루틴 운동 항목")
    public record ItemRequest(
            @Schema(description = "운동 ID", example = "1")
            @NotNull(message = "운동 ID를 입력해 주세요.")
            Long exerciseId,

            @Schema(description = "운동 순서. 생략하면 요청 순서대로 저장합니다.", example = "1")
            @Min(value = 1, message = "운동 순서는 1 이상이어야 합니다.")
            Integer seq,

            @Schema(description = "세트 수", example = "3")
            @Min(value = 1, message = "세트 수는 1 이상이어야 합니다.")
            @Max(value = 100, message = "세트 수는 100 이하여야 합니다.")
            Integer sets,

            @Schema(description = "반복 횟수", example = "12")
            @Min(value = 1, message = "반복 횟수는 1 이상이어야 합니다.")
            @Max(value = 1000, message = "반복 횟수는 1000 이하여야 합니다.")
            Integer reps,

            @Schema(description = "운동 시간(초)", example = "60")
            @Min(value = 1, message = "운동 시간은 1초 이상이어야 합니다.")
            @Max(value = 86400, message = "운동 시간은 86400초 이하여야 합니다.")
            Integer durationSec,

            @Schema(description = "휴식 시간(초)", example = "60")
            @Min(value = 0, message = "휴식 시간은 0초 이상이어야 합니다.")
            @Max(value = 86400, message = "휴식 시간은 86400초 이하여야 합니다.")
            Integer restSec
    ) {
    }
}
