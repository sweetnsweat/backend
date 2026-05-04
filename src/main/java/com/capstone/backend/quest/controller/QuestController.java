package com.capstone.backend.quest.controller;

import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.global.api.ApiResponse;
import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.quest.dto.CompleteQuestRequest;
import com.capstone.backend.quest.dto.QuestResponse;
import com.capstone.backend.quest.service.QuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "퀘스트", description = "활성 루틴 기반 일일 퀘스트 API")
@RestController
@RequestMapping("/api/quests")
public class QuestController {

    private final QuestService questService;

    public QuestController(QuestService questService) {
        this.questService = questService;
    }

    @Operation(summary = "오늘 퀘스트 조회 및 자동 생성", description = "오늘 퀘스트가 있으면 그대로 조회하고, 없으면 활성 루틴과 오늘 컨디션을 기준으로 자동 생성합니다.")
    @GetMapping("/today")
    public ApiResponse<QuestResponse> todayQuest(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok("오늘 퀘스트를 조회했습니다.", questService.getTodayQuest(authUser.userId()));
    }

    @Operation(summary = "AI 서버용 오늘 퀘스트 조회", description = "캡스톤 데모용 AI 서버 연동 API입니다. 토큰 없이 userId로 오늘 퀘스트를 조회하거나 자동 생성합니다.")
    @GetMapping("/today/by-user")
    public ApiResponse<QuestResponse> todayQuestByUserId(
            @Parameter(description = "조회할 사용자 ID", required = true, example = "14")
            @RequestParam Long userId) {
        if (userId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_USER_ID", "userId는 1 이상이어야 합니다.");
        }
        return ApiResponse.ok("오늘 퀘스트를 조회했습니다.", questService.getTodayQuest(userId));
    }

    @Operation(summary = "퀘스트 완료 처리", description = "오늘 받은 퀘스트를 완료 처리합니다. 이미 완료된 퀘스트는 같은 완료 상태를 다시 반환합니다.")
    @PatchMapping("/{questId}/complete")
    public ApiResponse<QuestResponse> completeQuest(@AuthenticationPrincipal AuthUser authUser,
                                                    @PathVariable Long questId,
                                                    @RequestBody(required = false) CompleteQuestRequest request) {
        return ApiResponse.ok("퀘스트가 완료되었습니다.", questService.completeQuest(authUser.userId(), questId, request));
    }
}
