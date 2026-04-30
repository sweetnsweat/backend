package com.capstone.backend.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateActiveRoutineRequest(
        @NotNull(message = "설정할 운동 루틴 ID를 입력해 주세요.")
        Long routineId
) {
}
