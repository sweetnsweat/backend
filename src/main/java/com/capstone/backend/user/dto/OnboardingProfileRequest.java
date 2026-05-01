package com.capstone.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "최초 로그인 온보딩 프로필 저장 요청")
public record OnboardingProfileRequest(
        @Schema(description = "성별. 남성 male, 여성 female, 응답 안 함 prefer_not_to_say", example = "female")
        @NotBlank(message = "성별을 선택해 주세요.")
        @Pattern(regexp = "male|female|prefer_not_to_say", message = "성별 값이 올바르지 않습니다.")
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

        @Schema(description = "현재 운동 상태. 없음 none, 가끔 occasional, 정기적으로 regular", example = "none")
        @NotBlank(message = "현재 운동 상태를 선택해 주세요.")
        @Pattern(regexp = "none|occasional|regular", message = "현재 운동 상태 값이 올바르지 않습니다.")
        String currentExerciseStatus,

        @Schema(description = "운동 목표. 체력 stamina, 다이어트 weight_loss, 근력 strength, 습관 habit, 스트레스 해소 stress_relief", example = "habit")
        @NotBlank(message = "운동 목표를 선택해 주세요.")
        @Pattern(regexp = "stamina|weight_loss|strength|habit|stress_relief", message = "운동 목표 값이 올바르지 않습니다.")
        String fitnessGoal,

        @Schema(description = "주 운동 장소. 집 home, 헬스장 gym, 야외 outdoor, 수영장/시설 facility, 기타 other", example = "home")
        @NotBlank(message = "주 운동 장소를 선택해 주세요.")
        @Pattern(regexp = "home|gym|outdoor|facility|other", message = "주 운동 장소 값이 올바르지 않습니다.")
        String preferredWorkoutPlace,

        @Schema(description = "주당 운동 가능 횟수. 1 이상 7 이하", example = "3")
        @NotNull(message = "주당 운동 가능 횟수를 입력해 주세요.")
        @Min(value = 1, message = "주당 운동 가능 횟수는 1회 이상이어야 합니다.")
        @Max(value = 7, message = "주당 운동 가능 횟수는 7회 이하여야 합니다.")
        Integer weeklyWorkoutFrequency,

        @Schema(description = "1회 운동 가능 시간(분). 10 이상 180 이하", example = "30")
        @NotNull(message = "1회 운동 가능 시간을 입력해 주세요.")
        @Min(value = 10, message = "1회 운동 가능 시간은 10분 이상이어야 합니다.")
        @Max(value = 180, message = "1회 운동 가능 시간은 180분 이하여야 합니다.")
        Integer availableWorkoutMinutes,

        @Schema(description = "선호 운동 유형 목록. 선택값이며 최대 10개까지 선택할 수 있습니다. 허용값: strength, cardio, stretching, bodyweight, walking, running, swimming, yoga_pilates", example = "[\"bodyweight\", \"walking\"]")
        @Size(max = 10, message = "선호 운동 유형은 최대 10개까지 선택할 수 있습니다.")
        List<@Pattern(
                regexp = "strength|cardio|stretching|bodyweight|walking|running|swimming|yoga_pilates",
                message = "선호 운동 유형 값이 올바르지 않습니다."
        ) String> preferredExerciseTypes
) {
}
