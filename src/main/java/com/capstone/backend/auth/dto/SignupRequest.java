package com.capstone.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "로그인 아이디를 입력해 주세요.")
        @Size(min = 4, max = 50, message = "로그인 아이디는 4~50자여야 합니다.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 72, message = "비밀번호는 8~72자여야 합니다.")
        String password,

        @NotBlank(message = "닉네임을 입력해 주세요.")
        @Size(min = 2, max = 50, message = "닉네임은 2~50자여야 합니다.")
        String nickname
) {
}
