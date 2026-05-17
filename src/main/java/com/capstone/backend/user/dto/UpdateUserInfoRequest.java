package com.capstone.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "사용자 정보 수정 요청")
public record UpdateUserInfoRequest(
        @Schema(description = "닉네임. 2자 이상 50자 이하", example = "새닉네임")
        @Size(min = 2, max = 50, message = "닉네임은 2~50자여야 합니다.")
        String nickname,

        @Schema(description = "이메일. 비워 보내면 수정하지 않습니다.", example = "new@example.com")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        @Pattern(
                regexp = "^$|^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$",
                message = "이메일 형식이 올바르지 않습니다."
        )
        String email,

        @Schema(description = "성별. 남성 male, 여성 female, 응답 안 함 prefer_not_to_say", example = "female")
        @Pattern(regexp = "^$|male|female|prefer_not_to_say", message = "성별 값이 올바르지 않습니다.")
        String gender,

        @Schema(description = "키(cm). 50.0 이상 250.0 이하", example = "164.5")
        @DecimalMin(value = "50.0", message = "키는 50.0cm 이상이어야 합니다.")
        @DecimalMax(value = "250.0", message = "키는 250.0cm 이하여야 합니다.")
        BigDecimal heightCm,

        @Schema(description = "몸무게(kg). 20.0 이상 300.0 이하", example = "58.2")
        @DecimalMin(value = "20.0", message = "몸무게는 20.0kg 이상이어야 합니다.")
        @DecimalMax(value = "300.0", message = "몸무게는 300.0kg 이하여야 합니다.")
        BigDecimal weightKg
) {
}
