package com.capstone.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank(message = "로그인 아이디를 입력해 주세요.")
        @Size(min = 4, max = 50, message = "로그인 아이디는 4~50자여야 합니다.")
        String loginId,

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        @Pattern(
                regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$",
                message = "이메일 형식이 올바르지 않습니다."
        )
        String email
) {
}
