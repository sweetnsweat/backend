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
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @Operation(
            summary = "퀘스트 완료 처리",
            description = """
                    오늘 받은 퀘스트를 완료 처리합니다. 완료 버튼은 프론트에서 하나만 제공하고, 백엔드가 건강 데이터 검증 결과에 따라 완료 유형을 결정합니다.

                    - healthSamples가 충분하면 VERIFIED 완료로 처리합니다.
                    - healthSamples가 없거나 부족하면 MANUAL 완료로 처리합니다.
                    - VERIFIED 완료는 기존 퀘스트 보상을 지급하고 battleEligible=true로 내려가며 배틀 점수에 반영됩니다.
                    - MANUAL 완료는 EXP 10 / Gold 5 축소 보상을 지급하고 battleEligible=false로 내려가며 배틀 점수에는 반영되지 않습니다.
                    - 이미 완료된 퀘스트는 추가 지급 없이 기존 완료 결과를 그대로 반환합니다.
                    """
    )
    @PatchMapping("/{questId}/complete")
    public ApiResponse<QuestResponse> completeQuest(@AuthenticationPrincipal AuthUser authUser,
                                                    @PathVariable Long questId,
                                                    @RequestBody(required = false) CompleteQuestRequest request) {
        return ApiResponse.ok("퀘스트가 완료되었습니다.", questService.completeQuest(authUser.userId(), questId, request));
    }

    @Operation(
            summary = "퀘스트 완료 처리 - 폼 요청 호환",
            description = "모바일 클라이언트가 빈 요청을 application/x-www-form-urlencoded로 보낸 경우를 호환 처리합니다. 건강 데이터가 없으므로 MANUAL 완료로 처리됩니다."
    )
    @PatchMapping(value = "/{questId}/complete", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ApiResponse<QuestResponse> completeQuestForm(@AuthenticationPrincipal AuthUser authUser,
                                                        @PathVariable Long questId) {
        return ApiResponse.ok("퀘스트가 완료되었습니다.", questService.completeQuest(authUser.userId(), questId, null));
    }

    @Operation(
            summary = "퀘스트 완료 초기화",
            description = """
                    개발/테스트용 퀘스트 완료 초기화 API입니다. 현재 로그인한 사용자가 소유한 퀘스트만 초기화할 수 있습니다.

                    - COMPLETED 퀘스트를 ISSUED 상태로 되돌립니다.
                    - progressValue는 0, completedAt은 null, proofJson은 빈 객체로 초기화합니다.
                    - 해당 퀘스트 완료로 지급된 EXP 로그와 Gold 거래 내역을 제거하고 사용자 EXP/지갑 잔액을 되돌립니다.
                    - 이미 배틀 결과 확정에 사용한 퀘스트를 초기화하면 배틀 테스트 데이터와 맞지 않을 수 있으므로, 완료 API 재테스트 용도로만 사용해 주세요.
                    """
    )
    @PostMapping("/{questId}/reset")
    public ApiResponse<QuestResponse> resetQuestCompletion(@AuthenticationPrincipal AuthUser authUser,
                                                           @PathVariable Long questId) {
        return ApiResponse.ok("퀘스트 완료 상태가 초기화되었습니다.", questService.resetQuestCompletion(authUser.userId(), questId));
    }
}
