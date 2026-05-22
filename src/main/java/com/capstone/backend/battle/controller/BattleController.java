package com.capstone.backend.battle.controller;

import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.battle.dto.BattleDetailResponse;
import com.capstone.backend.battle.dto.BattleHistoryPageResponse;
import com.capstone.backend.battle.dto.BattleMatchRequest;
import com.capstone.backend.battle.dto.BattleResultResponse;
import com.capstone.backend.battle.dto.BattleSummaryResponse;
import com.capstone.backend.battle.service.BattleService;
import com.capstone.backend.global.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "배틀", description = "사용자 간 하루/주간 활동량 점수 배틀 API")
@RestController
@RequestMapping("/api/battles")
public class BattleController {

    private final BattleService battleService;

    public BattleController(BattleService battleService) {
        this.battleService = battleService;
    }

    @Operation(summary = "내 배틀 요약 조회", description = "배틀 로비에서 사용할 전적, 승률, 랭크, 현재 진행 중인 하루/주간 배틀을 조회합니다.")
    @GetMapping("/me/summary")
    public ApiResponse<BattleSummaryResponse> summary(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(battleService.getSummary(authUser.userId()));
    }

    @Operation(summary = "배틀 매칭 시작", description = "DAILY 또는 WEEKLY 배틀을 시작합니다. 같은 기간에 이미 진행 중인 배틀이 있으면 기존 배틀을 반환합니다.")
    @PostMapping("/match")
    public ApiResponse<BattleDetailResponse> match(@AuthenticationPrincipal AuthUser authUser,
                                                   @Valid @RequestBody BattleMatchRequest request) {
        return ApiResponse.ok("Battle matched", battleService.match(authUser.userId(), request.mode()));
    }

    @Operation(summary = "배틀 상세 조회", description = "배틀 상세 화면에 표시할 참여자, 현재 활동량 기반 배틀 점수, 비교 지표를 조회합니다.")
    @GetMapping("/{battleId}")
    public ApiResponse<BattleDetailResponse> detail(@AuthenticationPrincipal AuthUser authUser,
                                                    @PathVariable Long battleId) {
        return ApiResponse.ok(battleService.getDetail(authUser.userId(), battleId));
    }

    @Operation(summary = "배틀 결과 조회", description = "현재 기준 결과를 조회합니다. 기간이 끝난 배틀은 최초 조회 시 결과를 확정합니다.")
    @GetMapping("/{battleId}/result")
    public ApiResponse<BattleResultResponse> result(@AuthenticationPrincipal AuthUser authUser,
                                                    @PathVariable Long battleId) {
        return ApiResponse.ok(battleService.getResult(authUser.userId(), battleId));
    }

    @Operation(summary = "내 배틀 기록 조회", description = "종료된 내 배틀 기록을 최신순으로 조회합니다.")
    @GetMapping("/history")
    public ApiResponse<BattleHistoryPageResponse> history(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "페이지 번호. 0부터 시작합니다.", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기. 최대 100입니다.", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.ok(battleService.getHistory(authUser.userId(), page, size));
    }
}
