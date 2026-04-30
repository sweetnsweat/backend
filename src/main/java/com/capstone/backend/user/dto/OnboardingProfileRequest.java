package com.capstone.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "최초 로그인 온보딩 프로필 저장 요청")
public record OnboardingProfileRequest(
        @Schema(description = "성별. 남성 male, 여성 female, 기타 other, 응답 안 함 prefer_not_to_say", example = "female")
        @NotBlank(message = "성별을 선택해 주세요.")
        @Pattern(regexp = "male|female|other|prefer_not_to_say", message = "성별 값이 올바르지 않습니다.")
        String gender,

        @Schema(description = "생년월일. YYYY-MM-DD 형식이며 오늘보다 과거 날짜여야 합니다.", example = "2002-05-20")
        @NotNull(message = "생년월일을 입력해 주세요.")
        @Past(message = "생년월일은 오늘보다 과거 날짜여야 합니다.")
        LocalDate birthDate,

        @Schema(description = "키(cm). 50.0 이상 250.0 이하", example = "164.5")
        @NotNull(message = "키를 입력해 주세요.")
        @DecimalMin(value = "50.0", message = "키는 50.0cm 이상이어야 합니다.")
        @DecimalMax(value = "250.0", message = "키는 250.0cm 이하여야 합니다.")
        BigDecimal heightCm,

        @Schema(description = "몸무게(kg). 20.0 이상 300.0 이하", example = "58.2")
        @NotNull(message = "몸무게를 입력해 주세요.")
        @DecimalMin(value = "20.0", message = "몸무게는 20.0kg 이상이어야 합니다.")
        @DecimalMax(value = "300.0", message = "몸무게는 300.0kg 이하여야 합니다.")
        BigDecimal weightKg,

        @Schema(description = "운동 경험 수준. 초급 beginner, 중급 intermediate, 고급 advanced", example = "beginner")
        @NotBlank(message = "운동 경험 수준을 선택해 주세요.")
        @Pattern(regexp = "beginner|intermediate|advanced", message = "운동 경험 수준 값이 올바르지 않습니다.")
        String experienceLevel,

        @Schema(description = "선호 운동 유형 목록. 최소 1개, 최대 10개까지 선택할 수 있습니다. 허용값: strength, cardio, stretching, bodyweight, walking", example = "[\"strength\", \"walking\"]")
        @NotEmpty(message = "선호 운동 유형을 하나 이상 선택해 주세요.")
        @Size(max = 10, message = "선호 운동 유형은 최대 10개까지 선택할 수 있습니다.")
        List<@Pattern(
                regexp = "strength|cardio|stretching|bodyweight|walking",
                message = "선호 운동 유형 값이 올바르지 않습니다."
        ) String> preferredExerciseTypes
) {
}
