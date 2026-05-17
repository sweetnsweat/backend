package com.capstone.backend.statistics.controller;

import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.global.api.ApiResponse;
import com.capstone.backend.statistics.dto.ConditionStatisticsResponse;
import com.capstone.backend.statistics.service.ConditionStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "통계", description = "컨디션 변화 및 운동별 효과 분석 API")
@RestController
@RequestMapping("/api/statistics")
public class ConditionStatisticsController {

    private final ConditionStatisticsService conditionStatisticsService;

    public ConditionStatisticsController(ConditionStatisticsService conditionStatisticsService) {
        this.conditionStatisticsService = conditionStatisticsService;
    }

    @Operation(
            summary = "컨디션 통계 조회",
            description = "기간별 컨디션 변화, 운동별 평균 컨디션, 요약 지표, 인사이트 문구, 주간 기록 표 데이터를 조회합니다."
    )
    @GetMapping("/condition")
    public ApiResponse<ConditionStatisticsResponse> getConditionStatistics(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "week") String period
    ) {
        return ApiResponse.ok(conditionStatisticsService.getConditionStatistics(authUser.userId(), period));
    }
}
