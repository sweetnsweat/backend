package com.capstone.backend.achievement.controller;

import com.capstone.backend.achievement.dto.AchievementBadgeListResponse;
import com.capstone.backend.achievement.service.AchievementBadgeService;
import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.global.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "획득 배지", description = "자동 지급 배지 조회 API")
@RestController
@RequestMapping("/api/users/me/badges")
public class AchievementBadgeController {

    private final AchievementBadgeService achievementBadgeService;

    public AchievementBadgeController(AchievementBadgeService achievementBadgeService) {
        this.achievementBadgeService = achievementBadgeService;
    }

    @Operation(summary = "내 배지 목록 조회", description = "자동 지급 배지 목록과 현재 사용자의 획득 여부를 조회합니다.")
    @GetMapping
    public ApiResponse<AchievementBadgeListResponse> badges(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(achievementBadgeService.getBadges(authUser.userId()));
    }

    @Operation(summary = "내 배지 지급 상태 동기화", description = "기존 퀘스트/배틀 기록 기준으로 누락된 배지를 다시 지급합니다.")
    @PostMapping("/sync")
    public ApiResponse<AchievementBadgeListResponse> sync(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok("배지 지급 상태를 동기화했습니다.", achievementBadgeService.syncBadges(authUser.userId()));
    }
}
