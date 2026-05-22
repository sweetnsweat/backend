package com.capstone.backend.quest.dto;

import com.capstone.backend.health.dto.HealthMetricSampleRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "퀘스트 완료 요청. 프론트는 완료 버튼 클릭 시 가능한 Health Connect 데이터를 healthSamples에 담아 보내고, 백엔드는 VERIFIED/MANUAL 완료를 자동 판정합니다.")
public record CompleteQuestRequest(
        @Schema(description = "수동 완료 시 프론트가 알고 있는 진행값. healthSamples 검증 성공 시에는 건강 데이터 기반 progressValue가 우선됩니다.", example = "1")
        Integer progressValue,
        @Schema(description = "수동 완료 참고용 증빙 메타데이터. 예: {\"source\":\"manual\"}. 백엔드는 이 값만으로 배틀 반영 여부를 인정하지 않습니다.")
        Map<String, Object> proof,
        @Schema(description = "Health Connect/HealthKit에서 읽은 건강 데이터 샘플. 충분하면 VERIFIED 완료, 없거나 부족하면 MANUAL 완료로 처리됩니다.")
        List<HealthMetricSampleRequest> healthSamples
) {
}
