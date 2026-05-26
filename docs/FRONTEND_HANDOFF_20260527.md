# Frontend Handoff 2026-05-27

## 스토리 채팅방 상세 퀘스트 복원

### 배경

채팅방에서 운동 퀘스트를 받은 뒤 다른 화면으로 이동했다가 다시 채팅방에 들어오면, 퀘스트 카드가 사라지는 문제가 있었다.

원인은 퀘스트 데이터 저장 자체가 누락된 것이 아니라, 채팅방 상세 API가 일반 대화 로그만 내려주고 AI가 래핑한 스토리 퀘스트 로그를 `recentMessages`에 포함하지 않았기 때문이다.

현재 저장 구조는 다음과 같다.

- 실제 운동 퀘스트: `user_quests`
- AI가 스토리 문맥으로 래핑한 퀘스트: `story_quests`
- 일반 대화 로그: `story_play_logs`

### 변경 API

`GET /api/stories/chats/{scenarioId}?messageLimit=30`

이제 `recentMessages`에 `story_play_logs`와 `story_quests`를 시간순으로 합쳐 내려준다.

일반 대화 메시지는 기존 필드를 그대로 유지하면서 `role: "story_log"`가 추가된다.

```json
{
  "id": 603,
  "role": "story_log",
  "chapterNum": 4,
  "choiceId": null,
  "detailId": null,
  "unitIndex": 2,
  "userMessage": "...",
  "narrationText": "...",
  "dialogueText": "...",
  "outputText": "...",
  "content": null,
  "quest_id": null,
  "workout_quest": null,
  "createdAt": "2026-05-26T17:39:31.462125Z"
}
```

퀘스트 메시지는 아래처럼 내려간다.

```json
{
  "id": 65,
  "role": "workout_quest",
  "chapterNum": 3,
  "choiceId": null,
  "detailId": null,
  "unitIndex": 3,
  "userMessage": null,
  "narrationText": null,
  "dialogueText": null,
  "outputText": null,
  "content": "요가와 필라테스의 코어 운동을 ...",
  "quest_id": 65,
  "workout_quest": {
    "title": "오늘의 콩쥐 집안일 단련",
    "description": "요가와 필라테스의 코어 운동을 ...",
    "source": "external_quest_today",
    "quests": [
      {
        "quest_name": "깨진 독 틈새를 막으며 중심을 잡는 집안일 단련",
        "routine_items": [],
        "original_routine": {
          "external_quest_id": 1321
        }
      }
    ]
  },
  "createdAt": "2026-05-27T02:24:52.514632Z"
}
```

`quest_id`는 AI 스토리 퀘스트 래핑 ID다. 실제 백엔드 운동 퀘스트 ID는 기존처럼 `workout_quest.quests[].original_routine.external_quest_id` 또는 `routine_items[].external_quest_id`에서 읽으면 된다.

### 프론트 수정 포인트

채팅방 재입장 시 `recentMessages`를 복원할 때 `role === "workout_quest"`를 퀘스트 카드로 렌더링해야 한다.

권장 매핑:

```ts
if (item.role === 'workout_quest') {
  return {
    id: `${item.role}-${item.id}`,
    role: 'quest',
    text: '',
    questId: extractExternalQuestId(item.workout_quest) ?? item.quest_id,
    questData: item.workout_quest,
    time: formatServerTime(item.createdAt),
  };
}
```

완료 버튼 활성화를 위해 `activeQuest`도 복원해야 한다.

```ts
const latestQuestMessage = restoredMessages
  .filter(m => m.role === 'quest' && m.questId)
  .at(-1);

if (latestQuestMessage && !storyQuestCompleted(latestQuestMessage.questData)) {
  setActiveQuest({
    id: latestQuestMessage.questId,
    data: latestQuestMessage.questData,
  });
}
```

기존 `/api/stories/play/history`도 퀘스트를 내려주지만, 채팅방 상세 화면에서 `/api/stories/chats/{scenarioId}`를 사용한다면 이제 이 API 응답만으로도 퀘스트 카드 복원이 가능하다.

## 완료된 스토리 퀘스트 카드 제외

### 변경 API

`GET /api/stories/chats/{scenarioId}?messageLimit=30`

채팅방 입장 정보 조회 시 `story_quests.quest_json` 안의 실제 운동 퀘스트 ID를 확인해서, 연결된 `user_quests.status`가 `completed`인 경우 해당 `workout_quest` 메시지는 `recentMessages`에서 제외된다.

즉 프론트는 완료된 퀘스트 카드를 별도로 숨기기 위해 같은 퀘스트 ID를 다시 조회하거나 필터링하지 않아도 된다.

### 프론트 수정 포인트

- `recentMessages`에 `role === "workout_quest"`가 없으면 현재 채팅방에서 다시 띄울 미완료 퀘스트 카드가 없는 상태로 처리한다.
- 완료 후 채팅방에 재입장하면 기존 퀘스트 카드는 내려오지 않는 것이 정상 동작이다.
- 기존에 `activeQuest`를 복원할 때 마지막 퀘스트 메시지를 찾는 로직이 있다면, 이제 응답에 남아있는 `workout_quest`만 대상으로 잡으면 된다.

응답 예시는 일반 대화만 남는 형태다.

```json
{
  "messageTotalCount": 3,
  "hasMoreMessages": false,
  "recentMessages": [
    { "role": "story_log", "userMessage": "첫 번째 입력" },
    { "role": "story_log", "userMessage": "두 번째 입력" },
    { "role": "story_log", "userMessage": "세 번째 입력" }
  ]
}
```
