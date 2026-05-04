package com.capstone.backend.home.controller;

import com.capstone.backend.global.api.ApiResponse;
import com.capstone.backend.home.dto.HomeWorldBannerListResponse;
import com.capstone.backend.home.service.HomeService;
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
@Tag(name = "홈", description = "메인 홈 화면 API")
@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @Operation(summary = "홈 상단 세계관 슬라이드 조회", description = "메인 홈 상단 캐러셀에 표시할 활성 세계관과 대표 캐릭터 정보를 조회합니다. 기본 limit은 3개입니다.")
    @GetMapping("/world-banners")
    public ApiResponse<HomeWorldBannerListResponse> worldBanners(
            @Parameter(description = "조회할 슬라이드 개수. 기본 3개, 최대 20개입니다.", example = "3")
            @RequestParam(defaultValue = "3") @Min(1) @Max(20) int limit
    ) {
        return ApiResponse.ok(homeService.getWorldBanners(limit));
    }
}
