package com.capstone.backend.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserInfoRequest(
        @Size(min = 2, max = 50, message = "닉네임은 2~50자여야 합니다.")
        String nickname,

        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        @Pattern(
                regexp = "^$|^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$",
                message = "이메일 형식이 올바르지 않습니다."
        )
        String email,

        @Size(max = 30, message = "휴대전화 번호는 30자 이하여야 합니다.")
        String phone
) {
}
