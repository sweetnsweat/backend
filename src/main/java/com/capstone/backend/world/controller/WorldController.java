package com.capstone.backend.world.controller;

import com.capstone.backend.global.api.ApiResponse;
import com.capstone.backend.world.dto.WorldRankingListResponse;
import com.capstone.backend.world.service.WorldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "세계관", description = "세계관 목록과 랭킹 API")
@RestController
@RequestMapping("/api/worlds")
public class WorldController {

    private final WorldService worldService;

    public WorldController(WorldService worldService) {
        this.worldService = worldService;
    }

    @Operation(summary = "세계관 랭킹 조회", description = "메인 홈 세계관 랭킹에 표시할 활성 세계관 순위를 조회합니다. IN_PROGRESS 상태인 채팅 수를 score로 집계합니다.")
    @GetMapping("/rankings")
    public ApiResponse<WorldRankingListResponse> rankings(
            @Parameter(description = "조회할 랭킹 개수. 기본 5개, 최대 20개입니다.", example = "5")
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit
    ) {
        return ApiResponse.ok(worldService.getRankings(limit));
    }
}
