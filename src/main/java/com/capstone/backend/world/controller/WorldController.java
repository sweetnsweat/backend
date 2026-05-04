package com.capstone.backend.world.controller;

import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.global.api.ApiResponse;
import com.capstone.backend.world.dto.WorldPreviewResponse;
import com.capstone.backend.world.dto.WorldRankingListResponse;
import com.capstone.backend.world.dto.WorldRankingPageResponse;
import com.capstone.backend.world.service.WorldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @Operation(summary = "세계관 랭킹 전체 조회", description = "세계관 랭킹 전체보기 화면용 API입니다. 50개 단위 무한스크롤, 장르 필터, 키워드 검색을 지원합니다.")
    @GetMapping("/rankings/full")
    public ApiResponse<WorldRankingPageResponse> fullRankings(
            @Parameter(description = "페이지 번호. 0부터 시작합니다.", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기. 기본 50개, 최대 100개입니다.", example = "50")
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
            @Parameter(description = "장르 필터. 전체면 생략하거나 전체/all을 보냅니다.", example = "로맨스")
            @RequestParam(required = false) String genre,
            @Parameter(description = "검색어. 시나리오 제목/요약/장르/대표 캐릭터 이름/칭호/tags를 검색합니다.", example = "수연")
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(worldService.getFullRankings(genre, keyword, page, size));
    }

    @Operation(summary = "세계관 입장 전 미리보기 조회", description = "세계관 카드 클릭 시 표시할 모달 정보입니다. 세계관, 장르, 대표 캐릭터, 캐릭터 목록, 랭킹 점수, 사용자 진행 상태를 조회합니다.")
    @GetMapping("/{scenarioId}/preview")
    public ApiResponse<WorldPreviewResponse> preview(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "미리보기할 세계관 ID", example = "4")
            @PathVariable @Min(1) Integer scenarioId
    ) {
        return ApiResponse.ok(worldService.getPreview(scenarioId, authUser.userId()));
    }
}
