package com.capstone.backend.ai.controller;

final class AiStorySwaggerExamples {

    private AiStorySwaggerExamples() {
    }

    static final String HEALTH_RESPONSE = """
            {
              "success": true,
              "code": "OK",
              "message": "AI 서버 상태 확인에 성공했습니다.",
              "timestamp": "2026-05-05T18:40:00+09:00",
              "data": {
                "message": "story generation server is running"
              }
            }
            """;

    static final String GENERATE_RESPONSE = """
            {
              "success": true,
              "code": "OK",
              "message": "AI 스토리 생성이 완료되었습니다.",
              "timestamp": "2026-05-05T18:40:00+09:00",
              "data": {
                "scenario_id": 2,
                "title": "붉은 달의 계약",
                "summary": "기억을 잃은 계약자가 제국의 예언과 황태자의 비밀 사이에서 선택해야 하는 로맨스 판타지입니다.",
                "thumbnail_url": "http://100.89.171.113:8000/media/assets/scenario_red_moon.png",
                "world_image_url": "http://100.89.171.113:8000/media/assets/world_red_moon.png",
                "image_status": "ready",
                "representative_character": {
                  "id": 11,
                  "name": "리안",
                  "image_url": "http://100.89.171.113:8000/media/assets/character_lian.png",
                  "mid_story_line": "당신, 여기가 어디인지 정말 모르는 건가?",
                  "character_title": "차가운 황태자",
                  "character_type": "황태자",
                  "tags": ["로맨스", "황태자"],
                  "is_representative": true
                },
                "player_description": "기억을 잃고 제국 신전에 나타난 계약자",
                "genre": "로맨스 판타지",
                "is_active": true,
                "chapters": [
                  {
                    "id": 21,
                    "chapter_num": 1,
                    "content": "제국 변방 신전에서 눈을 뜬 당신은 자신이 왜 이곳에 남겨졌는지 알지 못한다.",
                    "question_text": "이 낯선 환경에서 나는 어떻게 행동할까?"
                  }
                ],
                "branches": [
                  {
                    "chapter_id": 21,
                    "chapter_num": 1,
                    "choices": [
                      {
                        "choice_id": 900111,
                        "question_text": "이 낯선 환경에서 나는 어떻게 행동할까?",
                        "choice_order": 1,
                        "choice_text": "지금 여기가 어디인지 조심스럽게 묻는다.",
                        "detail_id": 31,
                        "detail_text": "리안은 당신을 경계하면서도 현재 위치를 설명한다."
                      }
                    ]
                  }
                ]
              }
            }
            """;

    static final String PLAY_RESPONSE = """
            {
              "success": true,
              "code": "OK",
              "message": "AI 스토리 진행 응답을 조회했습니다.",
              "timestamp": "2026-05-05T18:40:00+09:00",
              "data": {
                "server_time": "2026-05-06T10:40:00+09:00",
                "chapter_num": 1,
                "phase": "DETAIL",
                "unit_index": 1,
                "total_units": 4,
                "narration": "낯선 신전의 공기가 폐부 깊숙이 스며든다...",
                "dialogue": [
                  {
                    "character_name": "리안",
                    "name": "리안",
                    "dialogue": "당신, 여기가 어디인지 정말 모르는 건가?",
                    "image_url": "/media/assets/character_lian.png",
                    "character_image_url": "/media/assets/character_lian.png",
                    "character_title": "차가운 황태자",
                    "representativeCharacterTitle": "차가운 황태자"
                  }
                ],
                "question_text": "지금은 어떻게 반응할까?",
                "choices": [
                  {
                    "choice_id": 900111,
                    "choice_order": 1,
                    "choice_text": "지금 여기가 어디인지 조심스럽게 묻는다.",
                    "choice_kind": "minor"
                  }
                ],
                "workout_quest_id": null,
                "quest_api_url": null,
                "workout_quest": null,
                "is_chapter_completed": false,
                "is_story_completed": false,
                "ending_type": null,
                "ending_text": null
              }
            }
            """;

    static final String HISTORY_RESPONSE = """
            {
              "success": true,
              "code": "OK",
              "message": "AI 스토리 대화 히스토리를 조회했습니다.",
              "timestamp": "2026-05-05T18:40:00+09:00",
              "data": {
                "items": [
                  {
                    "role": "narration",
                    "character_name": null,
                    "content": "낯선 신전의 공기가 폐부 깊숙이 스며든다...",
                    "quest_id": null,
                    "workout_quest": null
                  },
                  {
                    "role": "assistant",
                    "character_name": "리안",
                    "name": "리안",
                    "image_url": "/media/assets/character_lian.png",
                    "character_image_url": "/media/assets/character_lian.png",
                    "character_title": "차가운 황태자",
                    "representativeCharacterTitle": "차가운 황태자",
                    "character_type": "황태자",
                    "content": "당신, 여기가 어디인지 정말 모르는 건가?",
                    "quest_id": null,
                    "workout_quest": null
                  },
                  {
                    "role": "choice",
                    "character_name": null,
                    "content": "지금 여기가 어디인지 조심스럽게 묻는다.",
                    "quest_id": null,
                    "workout_quest": null
                  }
                ]
              }
            }
            """;

    static final String SCENARIOS_RESPONSE = """
            {
              "success": true,
              "code": "OK",
              "message": "AI 세계관 목록을 조회했습니다.",
              "timestamp": "2026-05-05T18:40:00+09:00",
              "data": [
                {
                  "scenario_id": 2,
                  "title": "붉은 달의 계약",
                  "summary": "기억을 잃은 계약자가 제국의 예언과 황태자의 비밀 사이에서 선택해야 하는 로맨스 판타지입니다.",
                  "thumbnail_url": "http://100.89.171.113:8000/media/assets/scenario_red_moon.png",
                  "world_image_url": "http://100.89.171.113:8000/media/assets/world_red_moon.png",
                  "image_status": "ready",
                  "representative_character": {
                    "id": 11,
                    "name": "리안",
                    "image_url": "http://100.89.171.113:8000/media/assets/character_lian.png",
                    "mid_story_line": "당신, 여기가 어디인지 정말 모르는 건가?",
                    "character_title": "차가운 황태자",
                    "character_type": "황태자",
                    "tags": ["로맨스", "황태자"],
                    "is_representative": true
                  },
                  "player_description": "기억을 잃고 제국 신전에 나타난 계약자",
                  "genre": "로맨스 판타지",
                  "is_active": true
                }
              ]
            }
            """;

    static final String SCENARIO_DETAIL_RESPONSE = """
            {
              "success": true,
              "code": "OK",
              "message": "AI 세계관 상세를 조회했습니다.",
              "timestamp": "2026-05-05T18:40:00+09:00",
              "data": {
                "scenario_id": 2,
                "title": "붉은 달의 계약",
                "summary": "기억을 잃은 계약자가 제국의 예언과 황태자의 비밀 사이에서 선택해야 하는 로맨스 판타지입니다.",
                "thumbnail_url": "http://100.89.171.113:8000/media/assets/scenario_red_moon.png",
                "world_image_url": "http://100.89.171.113:8000/media/assets/world_red_moon.png",
                "image_status": "ready",
                "genre": "로맨스 판타지",
                "is_active": true,
                "representative_character": {
                  "id": 11,
                  "name": "리안",
                  "image_url": "http://100.89.171.113:8000/media/assets/character_lian.png",
                  "mid_story_line": "당신, 여기가 어디인지 정말 모르는 건가?",
                  "character_title": "차가운 황태자",
                  "character_type": "황태자",
                  "tags": ["로맨스", "황태자"],
                  "is_representative": true
                },
                "player_description": "기억을 잃고 제국 신전에 나타난 계약자"
              }
            }
            """;
}
