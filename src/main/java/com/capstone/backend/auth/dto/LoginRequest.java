package com.capstone.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "로그인 아이디를 입력해 주세요.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        String password
) {
}
