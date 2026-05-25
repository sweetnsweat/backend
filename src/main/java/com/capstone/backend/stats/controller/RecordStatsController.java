package com.capstone.backend.stats.controller;

import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.global.api.ApiResponse;
import com.capstone.backend.stats.dto.RecordStatsPeriod;
import com.capstone.backend.stats.dto.RecordStatsResponse;
import com.capstone.backend.stats.service.RecordStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "기록/통계", description = "기록 페이지 차트와 요약 통계 API")
@RestController
@RequestMapping("/api/records")
public class RecordStatsController {

    private final RecordStatsService recordStatsService;

    public RecordStatsController(RecordStatsService recordStatsService) {
        this.recordStatsService = recordStatsService;
    }

    @Operation(
            summary = "기록 페이지 통계 조회",
            description = """
                    기록 페이지에서 사용하는 컨디션 변화, 운동별 효과 분석, 기간별 운동 기록을 한 번에 조회합니다.

                    - period=WEEKLY: KST 기준 이번 주 월요일부터 일요일까지 조회합니다.
                    - period=MONTHLY: KST 기준 이번 달 1일부터 말일까지 조회합니다.
                    - period=YEARLY: KST 기준 올해 1월 1일부터 12월 31일까지 조회합니다.
                    - conditionTrend는 라인 차트용 컨디션/에너지/스트레스/건강 데이터입니다. WEEKLY, MONTHLY는 일 단위이고 YEARLY는 1월-12월 월 단위입니다.
                    - summary는 상단 카드용 평균 컨디션, 운동 횟수, 개선율, 누적 건강 데이터입니다.
                    - exerciseEffects는 운동별 효과 분석 막대 차트용 데이터입니다.
                    - dailyRecords는 표 형태의 기록입니다. WEEKLY, MONTHLY는 일 단위이고 YEARLY는 월 단위 집계입니다.
                    - insight는 현재 데이터 기반의 룰베이스 요약 문구입니다.
                    """
    )
    @GetMapping("/stats")
    public ApiResponse<RecordStatsResponse> stats(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "조회 기간", example = "WEEKLY")
            @RequestParam(defaultValue = "WEEKLY") RecordStatsPeriod period) {
        return ApiResponse.ok("기록 통계를 조회했습니다.", recordStatsService.getStats(authUser.userId(), period));
    }
}
