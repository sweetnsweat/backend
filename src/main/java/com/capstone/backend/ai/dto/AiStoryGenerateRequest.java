package com.capstone.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "AI 스토리 생성 요청. AI 서버의 StoryTemplateInput과 동일한 의미의 필드를 백엔드 Swagger에 명확히 노출합니다.")
public record AiStoryGenerateRequest(
        @Schema(description = "작품 가제", example = "붉은 달의 계약")
        @Size(max = 255, message = "작품 가제는 최대 255자까지 입력할 수 있습니다.")
        String title,

        @Schema(description = "세계관 선택 화면에 보여줄 썸네일 이미지 URL", example = "https://example.com/thumbnails/red-moon.jpg")
        @JsonProperty("thumbnail_url")
        @Size(max = 1000, message = "썸네일 URL은 최대 1000자까지 입력할 수 있습니다.")
        String thumbnailUrl,

        @Schema(description = "true면 세계관/플레이어/주요 캐릭터 일러스트를 생성해 URL을 저장합니다.", example = "false")
        @JsonProperty("generate_images")
        Boolean generateImages,

        @Schema(description = "장르", example = "로맨스 판타지")
        @NotBlank(message = "장르를 입력해 주세요.")
        @Size(max = 100, message = "장르는 최대 100자까지 입력할 수 있습니다.")
        String genre,

        @Schema(description = "선호 클리셰/후킹 장치. 예: 회귀, 빙의, 환생, 계약결혼, 복수, 정체숨김", example = "회귀/빙의/환생")
        @JsonProperty("trope_style")
        @Size(max = 500, message = "선호 클리셰는 최대 500자까지 입력할 수 있습니다.")
        String tropeStyle,

        @Schema(description = "갈등 자극도. 예: low, medium, high. high면 배신/누명/폭로/권력 역전 같은 막장 전개를 강화합니다.", example = "high")
        @JsonProperty("drama_intensity")
        @Size(max = 50, message = "갈등 자극도는 최대 50자까지 입력할 수 있습니다.")
        String dramaIntensity,

        @Schema(description = "전개 리듬. 예: balanced, shortform_melodrama. shortform_melodrama면 숏폼 웹드라마처럼 빠른 사건 전환을 우선합니다.", example = "shortform_melodrama")
        @JsonProperty("pacing_style")
        @Size(max = 100, message = "전개 리듬은 최대 100자까지 입력할 수 있습니다.")
        String pacingStyle,

        @Schema(description = "세계관", example = "마법과 귀족 정치가 공존하는 제국. 붉은 달이 뜨는 밤마다 금지된 계약이 깨어난다.")
        @JsonProperty("world_setting")
        @NotBlank(message = "세계관을 입력해 주세요.")
        @Size(max = 5000, message = "세계관은 최대 5000자까지 입력할 수 있습니다.")
        String worldSetting,

        @Schema(description = "분위기", example = "긴장감 있고 서정적이며, 인물 간 감정선이 중요하다.")
        @JsonProperty("tone_and_mood")
        @NotBlank(message = "분위기를 입력해 주세요.")
        @Size(max = 3000, message = "분위기는 최대 3000자까지 입력할 수 있습니다.")
        String toneAndMood,

        @Schema(description = "플레이어 역할", example = "기억을 잃고 제국 신전에 나타난 계약자")
        @JsonProperty("player_role")
        @NotBlank(message = "플레이어 역할을 입력해 주세요.")
        @Size(max = 3000, message = "플레이어 역할은 최대 3000자까지 입력할 수 있습니다.")
        String playerRole,

        @Schema(description = "핵심 갈등", example = "플레이어는 자신의 정체와 제국을 뒤흔들 예언 사이에서 선택해야 한다.")
        @JsonProperty("core_conflict")
        @NotBlank(message = "핵심 갈등을 입력해 주세요.")
        @Size(max = 5000, message = "핵심 갈등은 최대 5000자까지 입력할 수 있습니다.")
        String coreConflict,

        @Schema(description = "꼭 넣고 싶은 사건", example = "황태자와의 첫 만남, 금지된 마법의 발현, 배신자의 등장")
        @JsonProperty("required_events")
        @Size(max = 5000, message = "필수 사건은 최대 5000자까지 입력할 수 있습니다.")
        String requiredEvents,

        @Schema(description = "넣고 싶지 않은 요소", example = "지나치게 코믹한 전개")
        @JsonProperty("forbidden_elements")
        @Size(max = 3000, message = "금지 요소는 최대 3000자까지 입력할 수 있습니다.")
        String forbiddenElements,

        @Schema(description = "원하는 결말 방향", example = "플레이어가 스스로 운명을 선택하는 결말")
        @JsonProperty("ending_direction")
        @Size(max = 3000, message = "결말 방향은 최대 3000자까지 입력할 수 있습니다.")
        String endingDirection,

        @Schema(description = "예상 챕터 수. 기본값은 AI 서버 기준 5이며, 허용 범위는 1~30입니다.", example = "5")
        @JsonProperty("chapter_count")
        @Min(value = 1, message = "챕터 수는 1 이상이어야 합니다.")
        @Max(value = 30, message = "챕터 수는 30 이하여야 합니다.")
        Integer chapterCount,

        @ArraySchema(schema = @Schema(description = "초기 주요 캐릭터 설정"))
        @NotEmpty(message = "캐릭터를 하나 이상 입력해 주세요.")
        List<@Valid CharacterTemplateInput> characters
) {

    @Schema(description = "AI 스토리 생성용 캐릭터 설정")
    public record CharacterTemplateInput(
            @Schema(description = "캐릭터 이름", example = "리안")
            @NotBlank(message = "캐릭터 이름을 입력해 주세요.")
            @Size(max = 100, message = "캐릭터 이름은 최대 100자까지 입력할 수 있습니다.")
            String name,

            @Schema(description = "캐릭터 역할", example = "황태자")
            @NotBlank(message = "캐릭터 역할을 입력해 주세요.")
            @Size(max = 500, message = "캐릭터 역할은 최대 500자까지 입력할 수 있습니다.")
            String role,

            @Schema(description = "성격", example = "차갑고 신중하지만 플레이어에게만 약한 면을 보인다.")
            @NotBlank(message = "캐릭터 성격을 입력해 주세요.")
            @Size(max = 2000, message = "캐릭터 성격은 최대 2000자까지 입력할 수 있습니다.")
            String personality,

            @Schema(description = "플레이어와의 관계", example = "처음에는 경계하지만 점점 신뢰하게 되는 인물")
            @JsonProperty("relationship_to_player")
            @Size(max = 2000, message = "플레이어와의 관계는 최대 2000자까지 입력할 수 있습니다.")
            String relationshipToPlayer,

            @Schema(description = "캐릭터 배경", example = "제국의 정치적 음모 속에서 살아남은 후계자")
            @Size(max = 3000, message = "캐릭터 배경은 최대 3000자까지 입력할 수 있습니다.")
            String background,

            @Schema(description = "특별 설정", example = "플레이어가 위험해질 때 감정이 크게 흔들린다.")
            @JsonProperty("special_notes")
            @Size(max = 3000, message = "특별 설정은 최대 3000자까지 입력할 수 있습니다.")
            String specialNotes
    ) {
    }
}
