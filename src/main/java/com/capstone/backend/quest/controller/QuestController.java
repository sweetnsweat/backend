package com.capstone.backend.quest.controller;

import com.capstone.backend.auth.security.AuthUser;
import com.capstone.backend.global.api.ApiResponse;
import com.capstone.backend.quest.dto.CompleteQuestRequest;
import com.capstone.backend.quest.dto.QuestResponse;
import com.capstone.backend.quest.service.QuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @Operation(summary = "퀘스트 완료 처리", description = "오늘 받은 퀘스트를 완료 처리합니다. 이미 완료된 퀘스트는 같은 완료 상태를 다시 반환합니다.")
    @PatchMapping("/{questId}/complete")
    public ApiResponse<QuestResponse> completeQuest(@AuthenticationPrincipal AuthUser authUser,
                                                    @PathVariable Long questId,
                                                    @RequestBody(required = false) CompleteQuestRequest request) {
        return ApiResponse.ok("퀘스트가 완료되었습니다.", questService.completeQuest(authUser.userId(), questId, request));
    }
}
