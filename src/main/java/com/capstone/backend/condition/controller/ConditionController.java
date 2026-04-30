package com.capstone.backend.condition.controller;

import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.condition.dto.ConditionLogResponse;
import com.capstone.backend.condition.dto.ConditionTodayRequest;
import com.capstone.backend.condition.service.ConditionService;
import com.capstone.backend.global.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "컨디션", description = "일일 컨디션 입력 및 조회 API")
@RestController
@RequestMapping("/api/conditions")
public class ConditionController {

    private final ConditionService conditionService;

    public ConditionController(ConditionService conditionService) {
        this.conditionService = conditionService;
    }

    @Operation(summary = "오늘 컨디션 조회", description = "오늘 날짜에 저장된 컨디션 입력값과 계산된 운동 강도 배율을 조회합니다.")
    @GetMapping("/today")
    public ApiResponse<ConditionLogResponse> getTodayCondition(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(conditionService.getTodayCondition(authUser.userId()));
    }

    @Operation(summary = "오늘 컨디션 저장", description = "오늘 컨디션, 수면, 스트레스, 에너지 레벨을 저장하거나 수정하고 주관 웰니스 기반 컨디션 점수와 운동 강도 배율을 계산합니다.")
    @PutMapping("/today")
    public ApiResponse<ConditionLogResponse> updateTodayCondition(@AuthenticationPrincipal AuthUser authUser,
                                                                  @Valid @RequestBody ConditionTodayRequest request) {
        return ApiResponse.ok("오늘 컨디션이 저장되었습니다.", conditionService.updateTodayCondition(authUser.userId(), request));
    }
}
