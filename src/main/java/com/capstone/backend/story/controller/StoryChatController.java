package com.capstone.backend.story.controller;

import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.global.api.ApiResponse;
import com.capstone.backend.story.dto.StoryChatDetailResponse;
import com.capstone.backend.story.dto.StoryChatListResponse;
import com.capstone.backend.story.service.StoryChatService;
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
@Tag(name = "스토리 채팅", description = "이미 입장한 AI 스토리 채팅방 목록과 입장 API")
@RestController
@RequestMapping("/api/stories/chats")
public class StoryChatController {

    private final StoryChatService storyChatService;

    public StoryChatController(StoryChatService storyChatService) {
        this.storyChatService = storyChatService;
    }

    @Operation(summary = "내 스토리 채팅 목록 조회", description = "세계관에서 한 번 이상 입장해 진행 상태가 생긴 채팅방 목록을 최근 업데이트 순으로 조회합니다.")
    @GetMapping
    public ApiResponse<StoryChatListResponse> chats(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "조회할 채팅방 수. 기본 50개, 최대 100개입니다.", example = "50")
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.ok(storyChatService.getChats(authUser.userId(), limit));
    }

    @Operation(summary = "스토리 채팅방 입장 정보 조회", description = "채팅 목록에서 선택한 세계관 채팅방의 입장 메타데이터, 전체 캐릭터 목록, 최근 대화 턴을 조회합니다.")
    @GetMapping("/{scenarioId}")
    public ApiResponse<StoryChatDetailResponse> chat(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "입장할 세계관 ID", example = "4")
            @PathVariable @Min(1) Integer scenarioId,
            @Parameter(description = "함께 내려받을 최근 대화 턴 수. 기본 30개, 최대 100개입니다.", example = "30")
            @RequestParam(defaultValue = "30") @Min(1) @Max(100) int messageLimit
    ) {
        return ApiResponse.ok(storyChatService.getChat(authUser.userId(), scenarioId, messageLimit));
    }
}
