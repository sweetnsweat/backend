package com.capstone.backend.ranking.controller;

import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.global.api.ApiResponse;
import com.capstone.backend.ranking.dto.WeeklyActivityRankingResponse;
import com.capstone.backend.ranking.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "랭킹", description = "사용자 활동 랭킹 API")
@RestController
@RequestMapping("/api/rankings")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @Operation(summary = "이번 주 활동 랭킹 조회", description = "KST 기준 이번 주 월요일부터 일요일까지 완료한 퀘스트의 획득 EXP 합산으로 사용자 랭킹을 조회합니다. 기본 size는 3명입니다.")
    @GetMapping("/weekly-activity")
    public ApiResponse<WeeklyActivityRankingResponse> weeklyActivity(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "조회할 랭킹 인원 수. 기본 3명, 최대 100명입니다.", example = "3")
            @RequestParam(defaultValue = "3") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.ok(rankingService.getWeeklyActivityRanking(authUser.userId(), size));
    }
}
