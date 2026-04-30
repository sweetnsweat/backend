-- Fill missing MET values for the existing free-exercise-db import and add
-- multi-sport exercises for swimming, yoga, running, cycling, and Pilates.
-- Source basis: 2024 Adult Compendium of Physical Activities
-- https://pacompendium.com/adult-compendium/

begin;

update exercises
set met = case
        when category = '유산소' and external_id ilike '%Walking%' then 3.80
        when category = '유산소' and external_id ilike '%Running%' then 9.30
        when category = '유산소' and external_id ilike '%Skipping%' then 11.00
        when category = '유산소' then 6.00
        when category = '스트레칭' then 2.30
        when category = '근력' and level = '고급' then 6.00
        when category = '근력' and level = '중급' then 5.00
        when category = '근력' then 3.50
        when category = '플라이오메트릭' and level = '고급' then 8.00
        when category = '플라이오메트릭' and level = '중급' then 8.00
        when category = '플라이오메트릭' then 6.00
        when category = '파워리프팅' then 6.00
        when category = '올림픽 역도' then 6.00
        when category = '스트롱맨' then 6.00
        else 3.50
    end,
    raw_data = coalesce(raw_data, '{}'::jsonb) || jsonb_build_object(
        'metAssignment',
        jsonb_build_object(
            'source', '2024 Adult Compendium of Physical Activities',
            'sourceUrl', 'https://pacompendium.com/adult-compendium/',
            'method', 'category-level mapping for existing free-exercise-db rows',
            'assignedAt', '2026-04-30'
        )
    ),
    updated_at = now()
where met is null;

with name_fix (external_id, name) as (
    values
    ('Alternate_Hammer_Curl', '얼터네이트 해머 컬'),
    ('Alternate_Heel_Touchers', '얼터네이트 힐 터치'),
    ('Alternate_Incline_Dumbbell_Curl', '얼터네이트 인클라인 덤벨 컬'),
    ('Alternate_Leg_Diagonal_Bound', '얼터네이트 레그 다이애고널 바운드'),
    ('Alternating_Hang_Clean', '얼터네이팅 행 클린'),
    ('Alternating_Renegade_Row', '얼터네이팅 레니게이드 로우'),
    ('Bodyweight_Mid_Row', '바디웨이트 미드 로우'),
    ('Bottoms-Up_Clean_From_The_Hang_Position', '바텀업 행 클린'),
    ('Cable_Preacher_Curl', '케이블 프리처 컬'),
    ('Clean', '클린'),
    ('Clean_and_Press', '클린 앤 프레스'),
    ('Clean_from_Blocks', '블록 클린'),
    ('Clean_Shrug', '클린 슈러그'),
    ('Decline_Close-Grip_Bench_To_Skull_Crusher', '디클라인 클로즈그립 스컬 크러셔'),
    ('Decline_Dumbbell_Bench_Press', '디클라인 덤벨 벤치 프레스'),
    ('Decline_Dumbbell_Triceps_Extension', '디클라인 덤벨 트라이셉스 익스텐션'),
    ('Decline_EZ_Bar_Triceps_Extension', '디클라인 이지바 트라이셉스 익스텐션'),
    ('Decline_Push-Up', '디클라인 푸시업'),
    ('Decline_Reverse_Crunch', '디클라인 리버스 크런치'),
    ('Decline_Smith_Press', '디클라인 스미스 프레스'),
    ('Double_Kettlebell_Alternating_Hang_Clean', '더블 케틀벨 얼터네이팅 행 클린'),
    ('Dumbbell_Alternate_Bicep_Curl', '덤벨 얼터네이트 바이셉 컬'),
    ('Elevated_Cable_Rows', '엘리베이티드 케이블 로우'),
    ('Hang_Clean', '행 클린'),
    ('Hang_Clean_-_Below_the_Knees', '행 클린 - 무릎 아래'),
    ('Hang_Snatch_-_Below_Knees', '행 스내치 - 무릎 아래'),
    ('Inverted_Row', '인버티드 로우'),
    ('Inverted_Row_with_Straps', '스트랩 인버티드 로우'),
    ('Kettlebell_Hang_Clean', '케틀벨 행 클린'),
    ('Lateral_Bound', '레터럴 바운드'),
    ('Machine_Preacher_Curls', '머신 프리처 컬'),
    ('One_Arm_Dumbbell_Preacher_Curl', '원암 덤벨 프리처 컬'),
    ('Parallel_Bar_Dip', '평행봉 딥'),
    ('Preacher_Curl', '프리처 컬'),
    ('Preacher_Hammer_Dumbbell_Curl', '프리처 해머 덤벨 컬'),
    ('Reverse_Barbell_Preacher_Curls', '리버스 바벨 프리처 컬'),
    ('Sled_Row', '썰매 로우'),
    ('Smith_Machine_Hang_Power_Clean', '스미스 머신 행 파워 클린'),
    ('Snatch_Pull', '스내치 풀'),
    ('Split_Clean', '스플릿 클린'),
    ('Suspended_Row', '서스펜디드 로우'),
    ('Two-Arm_Dumbbell_Preacher_Curl', '투암 덤벨 프리처 컬'),
    ('Upright_Cable_Row', '업라이트 케이블 로우'),
    ('Worlds_Greatest_Stretch', '월드 그레이티스트 스트레치'),
    ('Yoke_Walk', '요크 워크'),
    ('Zercher_Squats', '저처 스쿼트'),
    ('Zottman_Preacher_Curl', '조트만 프리처 컬')
)
update exercises e
set name = name_fix.name,
    raw_data = coalesce(e.raw_data, '{}'::jsonb) || jsonb_build_object(
        'nameKoManualFix',
        jsonb_build_object(
            'reason', 'exercise terminology cleanup after bulk Korean name assignment',
            'assignedAt', '2026-04-30'
        )
    ),
    updated_at = now()
from name_fix
where e.external_id = name_fix.external_id
  and e.name is distinct from name_fix.name;

with seed (
    external_id,
    name,
    category,
    intensity,
    met,
    level,
    equipment,
    primary_muscles,
    secondary_muscles,
    instructions,
    source_code,
    english_name
) as (
    values
    ('compendium-2024-swim-freestyle-fast-laps', '자유형 빠른 랩 수영', '수영', '고급', 9.80, '고급', '수영장', '["전신"]'::jsonb, '["어깨","등","코어","둔근","대퇴사두근"]'::jsonb, '["충분히 워밍업한 뒤 자유형으로 빠르게 왕복한다.","호흡 리듬과 스트로크 자세가 무너지지 않는 속도를 유지한다.","세트 사이에는 30~60초 휴식한다."]'::jsonb, '18375', 'swimming laps, freestyle, fast'),
    ('compendium-2024-swim-freestyle-slow-laps', '자유형 보통 랩 수영', '수영', '중급', 5.80, '중급', '수영장', '["전신"]'::jsonb, '["어깨","등","코어","둔근"]'::jsonb, '["편안한 자유형 페이스로 왕복한다.","몸이 가라앉지 않도록 코어에 힘을 유지한다.","호흡이 거칠어지면 속도를 낮춘다."]'::jsonb, '18366', 'swimming laps, freestyle, slow'),
    ('compendium-2024-swim-breaststroke-laps', '평영 랩 수영', '수영', '고급', 10.30, '고급', '수영장', '["전신"]'::jsonb, '["가슴","등","둔근","내전근"]'::jsonb, '["팔 당기기와 킥 타이밍을 맞춰 평영으로 왕복한다.","무릎이 과하게 벌어지지 않게 조절한다.","허리 통증이 있으면 강도를 낮춘다."]'::jsonb, '18355', 'swimming laps, breaststroke'),
    ('compendium-2024-swim-backstroke-training', '배영 훈련 수영', '수영', '고급', 9.50, '고급', '수영장', '["전신"]'::jsonb, '["어깨","등","코어","햄스트링"]'::jsonb, '["천장을 향해 누운 자세로 배영 랩을 진행한다.","골반이 가라앉지 않도록 몸통을 길게 유지한다.","팔 회전 중 어깨에 통증이 있으면 중단한다."]'::jsonb, '18345', 'swimming laps, backstroke'),
    ('compendium-2024-swim-butterfly', '접영 수영', '수영', '고급', 13.80, '고급', '수영장', '["전신"]'::jsonb, '["어깨","등","코어","둔근"]'::jsonb, '["짧은 거리부터 접영 스트로크를 반복한다.","돌핀 킥과 상체 리듬을 맞춘다.","기술 난도가 높으므로 피로가 심하면 즉시 휴식한다."]'::jsonb, '18350', 'swimming, butterfly'),
    ('compendium-2024-swim-open-water', '오픈워터 수영', '수영', '고급', 10.00, '고급', '수영 장비', '["전신"]'::jsonb, '["어깨","등","코어","둔근"]'::jsonb, '["안전 요원 또는 동행자가 있는 환경에서 진행한다.","시야 확보를 위해 주기적으로 전방을 확인한다.","수온과 파도에 따라 운동 시간을 조절한다."]'::jsonb, '18390', 'swimming, open water'),
    ('compendium-2024-swim-treading-water-moderate', '입영 보통 강도', '수영', '중급', 3.50, '중급', '수영장', '["전신"]'::jsonb, '["어깨","코어","둔근","종아리"]'::jsonb, '["깊은 물에서 발이 바닥에 닿지 않게 유지한다.","팔과 다리를 작게 움직여 안정적인 호흡을 만든다.","초보자는 짧은 세트로 시작한다."]'::jsonb, '18310', 'treading water, moderate effort'),
    ('compendium-2024-swim-water-aerobics', '수중 에어로빅', '수영', '중급', 5.50, '중급', '수영장', '["전신"]'::jsonb, '["어깨","코어","둔근","대퇴사두근"]'::jsonb, '["가슴 높이 물에서 팔 동작과 하체 스텝을 반복한다.","관절 부담이 낮은 범위에서 리듬을 유지한다.","미끄럼에 주의한다."]'::jsonb, '18100', 'water aerobics'),

    ('compendium-2024-yoga-hatha', '하타 요가', '요가', '초급', 2.30, '초급', '매트', '["전신"]'::jsonb, '["코어","햄스트링","어깨"]'::jsonb, '["호흡에 맞춰 기본 자세를 천천히 연결한다.","통증이 느껴지는 범위까지 억지로 늘리지 않는다.","자세 사이에 충분히 호흡한다."]'::jsonb, '02100', 'yoga, Hatha'),
    ('compendium-2024-yoga-general', '요가 일반', '요가', '초급', 2.50, '초급', '매트', '["전신"]'::jsonb, '["코어","햄스트링","둔근"]'::jsonb, '["기본 서기, 균형, 이완 자세를 조합한다.","호흡을 멈추지 않고 자연스럽게 이어간다.","목과 허리에 부담이 없도록 정렬한다."]'::jsonb, '02101', 'yoga, general'),
    ('compendium-2024-yoga-vinyasa', '빈야사 요가', '요가', '중급', 3.30, '중급', '매트', '["전신"]'::jsonb, '["어깨","코어","둔근","대퇴사두근"]'::jsonb, '["호흡과 함께 자세를 연속적으로 연결한다.","손목과 어깨에 체중이 과하게 실리지 않도록 조절한다.","속도보다 자세 안정성을 우선한다."]'::jsonb, '02105', 'yoga, Vinyasa'),
    ('compendium-2024-yoga-power', '파워 요가', '요가', '중급', 4.00, '중급', '매트', '["전신"]'::jsonb, '["어깨","코어","둔근","대퇴사두근"]'::jsonb, '["근력 자세와 균형 자세를 연속적으로 수행한다.","호흡이 흐트러지면 전환 속도를 낮춘다.","손목 통증이 있으면 대체 자세를 사용한다."]'::jsonb, '02106', 'yoga, power'),
    ('compendium-2024-yoga-hot', '핫 요가', '요가', '중급', 3.00, '중급', '매트', '["전신"]'::jsonb, '["코어","햄스트링","어깨"]'::jsonb, '["고온 환경에서 기본 요가 자세를 수행한다.","수분을 충분히 보충하고 어지러움이 있으면 중단한다.","실내 온도에 따라 시간을 조절한다."]'::jsonb, '02107', 'yoga, hot'),
    ('compendium-2024-yoga-sun-salutation', '태양경배 요가', '요가', '중급', 3.50, '중급', '매트', '["전신"]'::jsonb, '["어깨","코어","햄스트링","둔근"]'::jsonb, '["산 자세에서 전굴, 플랭크, 코브라, 다운독을 순서대로 연결한다.","각 전환마다 호흡을 맞춘다.","허리가 꺾이지 않게 복부에 힘을 유지한다."]'::jsonb, '02108', 'yoga, sun salutation'),
    ('compendium-2024-yoga-ashtanga', '아쉬탕가 요가', '요가', '고급', 4.00, '고급', '매트', '["전신"]'::jsonb, '["어깨","코어","둔근","햄스트링"]'::jsonb, '["정해진 시퀀스를 호흡 리듬에 맞춰 수행한다.","강도가 높으므로 짧은 구간부터 진행한다.","관절 통증이 있으면 즉시 자세를 수정한다."]'::jsonb, '02109', 'yoga, Ashtanga'),
    ('compendium-2024-yoga-restorative', '회복 요가', '요가', '초급', 2.00, '초급', '매트', '["전신"]'::jsonb, '["허리","햄스트링","어깨"]'::jsonb, '["보조 도구를 사용해 편안한 자세를 오래 유지한다.","호흡을 길게 가져가며 긴장을 낮춘다.","운동 후 회복 세션으로 활용한다."]'::jsonb, '02110', 'yoga, restorative'),

    ('compendium-2024-run-jogging-general', '조깅 일반', '러닝', '중급', 7.50, '중급', '러닝화', '["하체"]'::jsonb, '["둔근","햄스트링","대퇴사두근","종아리","코어"]'::jsonb, '["가벼운 워밍업 후 편안한 조깅 페이스를 유지한다.","발 착지는 몸 중심 아래에서 이루어지게 한다.","호흡이 너무 가빠지면 걷기 구간을 섞는다."]'::jsonb, '12020', 'jogging, general'),
    ('compendium-2024-run-4mph', '러닝 시속 6.4킬로미터', '러닝', '중급', 6.00, '중급', '러닝화', '["하체"]'::jsonb, '["둔근","햄스트링","대퇴사두근","종아리"]'::jsonb, '["시속 6.4km 수준의 느린 러닝으로 진행한다.","상체를 세우고 보폭을 무리하게 늘리지 않는다.","초보자는 걷기와 번갈아 수행한다."]'::jsonb, '12025', 'running, 4 mph'),
    ('compendium-2024-run-5mph', '러닝 시속 8.0킬로미터', '러닝', '중급', 8.30, '중급', '러닝화', '["하체"]'::jsonb, '["둔근","햄스트링","대퇴사두근","종아리","코어"]'::jsonb, '["시속 8.0km 수준으로 일정하게 달린다.","팔은 몸 옆에서 자연스럽게 앞뒤로 흔든다.","피로가 누적되면 보폭을 줄인다."]'::jsonb, '12030', 'running, 5 mph'),
    ('compendium-2024-run-6mph', '러닝 시속 9.7킬로미터', '러닝', '고급', 9.80, '고급', '러닝화', '["하체"]'::jsonb, '["둔근","햄스트링","대퇴사두근","종아리","코어"]'::jsonb, '["시속 9.7km 수준으로 달린다.","호흡과 케이던스를 일정하게 유지한다.","충분한 워밍업 후 본 세트를 시작한다."]'::jsonb, '12040', 'running, 6 mph'),
    ('compendium-2024-run-7mph', '러닝 시속 11.3킬로미터', '러닝', '고급', 11.00, '고급', '러닝화', '["하체"]'::jsonb, '["둔근","햄스트링","대퇴사두근","종아리","코어"]'::jsonb, '["시속 11.3km 수준의 빠른 러닝을 수행한다.","자세가 무너지지 않는 짧은 구간부터 시작한다.","무릎이나 발목 통증이 있으면 중단한다."]'::jsonb, '12050', 'running, 7 mph'),
    ('compendium-2024-run-8mph', '러닝 시속 12.9킬로미터', '러닝', '고급', 11.80, '고급', '러닝화', '["하체"]'::jsonb, '["둔근","햄스트링","대퇴사두근","종아리","코어"]'::jsonb, '["고강도 러닝 구간으로 짧게 수행한다.","워밍업과 쿨다운을 반드시 포함한다.","회복 시간이 부족하면 반복 수를 줄인다."]'::jsonb, '12060', 'running, 8 mph'),
    ('compendium-2024-run-cross-country', '크로스컨트리 러닝', '러닝', '고급', 9.00, '고급', '러닝화', '["하체"]'::jsonb, '["둔근","햄스트링","대퇴사두근","종아리","코어"]'::jsonb, '["비포장 길이나 완만한 지형 변화가 있는 코스를 달린다.","노면을 확인하며 보폭을 짧게 조절한다.","내리막에서는 속도를 과하게 높이지 않는다."]'::jsonb, '12120', 'running, cross country'),
    ('compendium-2024-run-stairs', '계단 러닝', '러닝', '고급', 15.00, '고급', '계단', '["하체"]'::jsonb, '["둔근","햄스트링","대퇴사두근","종아리","코어"]'::jsonb, '["짧은 계단 구간을 빠르게 오른다.","내려올 때는 천천히 걸어 회복한다.","무릎 부담이 크므로 반복 수를 보수적으로 잡는다."]'::jsonb, '12135', 'running stairs, up'),
    ('compendium-2024-run-marathon', '마라톤 페이스 러닝', '러닝', '고급', 13.30, '고급', '러닝화', '["하체"]'::jsonb, '["둔근","햄스트링","대퇴사두근","종아리","코어"]'::jsonb, '["장거리 러닝 페이스를 유지한다.","수분과 에너지 보충 계획을 함께 세운다.","훈련량은 주 단위로 점진적으로 늘린다."]'::jsonb, '12150', 'running, marathon'),

    ('compendium-2024-cycle-leisure', '여유 자전거 타기', '사이클', '초급', 4.00, '초급', '자전거', '["하체"]'::jsonb, '["대퇴사두근","햄스트링","둔근","종아리"]'::jsonb, '["평지에서 편안한 속도로 주행한다.","안장 높이를 맞추고 무릎이 과하게 접히지 않게 한다.","야외 주행 시 헬멧을 착용한다."]'::jsonb, '01010', 'bicycling, leisure, <10 mph'),
    ('compendium-2024-cycle-general', '자전거 타기 일반', '사이클', '중급', 7.50, '중급', '자전거', '["하체"]'::jsonb, '["대퇴사두근","햄스트링","둔근","종아리","코어"]'::jsonb, '["일정한 케이던스로 중간 강도 주행을 유지한다.","상체는 긴장시키지 않고 시선은 전방을 본다.","오르막에서는 기어를 낮춰 무릎 부담을 줄인다."]'::jsonb, '01015', 'bicycling, general'),
    ('compendium-2024-cycle-10-12mph', '사이클 시속 16~19킬로미터', '사이클', '중급', 6.80, '중급', '자전거', '["하체"]'::jsonb, '["대퇴사두근","햄스트링","둔근","종아리"]'::jsonb, '["시속 16~19km 수준으로 꾸준히 주행한다.","페달링이 끊기지 않게 원형 움직임을 유지한다.","교통 상황에 맞춰 속도를 조절한다."]'::jsonb, '01020', 'bicycling, 10-12 mph'),
    ('compendium-2024-cycle-12-14mph', '사이클 시속 19~22킬로미터', '사이클', '중급', 8.00, '중급', '자전거', '["하체"]'::jsonb, '["대퇴사두근","햄스트링","둔근","종아리","코어"]'::jsonb, '["시속 19~22km 수준으로 중상 강도 주행을 한다.","호흡이 급격히 무너지지 않는 범위를 유지한다.","장거리 전에는 충분히 워밍업한다."]'::jsonb, '01030', 'bicycling, 12-14 mph'),
    ('compendium-2024-cycle-14-16mph', '사이클 시속 22~26킬로미터', '사이클', '고급', 10.00, '고급', '자전거', '["하체"]'::jsonb, '["대퇴사두근","햄스트링","둔근","종아리","코어"]'::jsonb, '["시속 22~26km 수준의 빠른 주행을 수행한다.","전방 시야와 제동 거리를 충분히 확보한다.","피로 누적 시 속도를 낮춘다."]'::jsonb, '01040', 'bicycling, 14-16 mph'),
    ('compendium-2024-cycle-mountain', '산악자전거', '사이클', '고급', 8.50, '고급', '산악자전거', '["하체"]'::jsonb, '["대퇴사두근","햄스트링","둔근","종아리","코어"]'::jsonb, '["비포장 코스에서 지형에 맞춰 주행한다.","팔과 무릎을 살짝 굽혀 충격을 흡수한다.","보호 장비를 착용하고 무리한 장애물은 피한다."]'::jsonb, '01050', 'bicycling, mountain'),
    ('compendium-2024-cycle-stationary-light', '실내 자전거 가벼운 강도', '사이클', '초급', 4.00, '초급', '실내 자전거', '["하체"]'::jsonb, '["대퇴사두근","햄스트링","둔근","종아리"]'::jsonb, '["낮은 저항으로 편안하게 페달링한다.","허리를 과하게 숙이지 않고 손잡이를 가볍게 잡는다.","워밍업 또는 회복 운동으로 사용한다."]'::jsonb, '01110', 'bicycling, stationary, light'),
    ('compendium-2024-cycle-stationary-moderate', '실내 자전거 보통 강도', '사이클', '중급', 6.80, '중급', '실내 자전거', '["하체"]'::jsonb, '["대퇴사두근","햄스트링","둔근","종아리","코어"]'::jsonb, '["중간 저항으로 일정한 케이던스를 유지한다.","무릎이 안쪽으로 모이지 않게 정렬한다.","세트 중 호흡이 안정적인지 확인한다."]'::jsonb, '01120', 'bicycling, stationary, moderate'),
    ('compendium-2024-cycle-stationary-vigorous', '실내 자전거 고강도', '사이클', '고급', 8.80, '고급', '실내 자전거', '["하체"]'::jsonb, '["대퇴사두근","햄스트링","둔근","종아리","코어"]'::jsonb, '["높은 저항이나 빠른 케이던스로 고강도 구간을 수행한다.","짧은 인터벌과 충분한 회복을 번갈아 진행한다.","어지러움이 있으면 즉시 중단한다."]'::jsonb, '01130', 'bicycling, stationary, vigorous'),
    ('compendium-2024-cycle-spin-class', '스피닝 클래스', '사이클', '고급', 9.00, '고급', '실내 자전거', '["하체"]'::jsonb, '["대퇴사두근","햄스트링","둔근","종아리","코어"]'::jsonb, '["음악 또는 인터벌 큐에 맞춰 저항과 케이던스를 바꾼다.","상체가 흔들리지 않게 코어를 잡는다.","초보자는 서서 타는 구간을 줄인다."]'::jsonb, '01140', 'indoor cycling class'),

    ('compendium-2024-pilates-mat', '매트 필라테스', '필라테스', '초급', 1.80, '초급', '매트', '["코어"]'::jsonb, '["복근","허리","둔근","햄스트링"]'::jsonb, '["매트에서 호흡과 코어 조절을 중심으로 동작한다.","목과 허리에 부담이 가지 않도록 범위를 줄인다.","정확한 자세를 우선한다."]'::jsonb, '02020', 'Pilates, mat'),
    ('compendium-2024-pilates-general', '필라테스 일반', '필라테스', '초급', 2.80, '초급', '매트', '["코어"]'::jsonb, '["복근","허리","둔근","어깨"]'::jsonb, '["기본 코어 안정화 동작과 가벼운 전신 동작을 연결한다.","호흡에 맞춰 천천히 반복한다.","허리가 뜨지 않도록 복부에 힘을 유지한다."]'::jsonb, '02021', 'Pilates, general'),
    ('compendium-2024-pilates-reformer-light', '리포머 필라테스 가벼운 강도', '필라테스', '초급', 3.00, '초급', '리포머', '["코어"]'::jsonb, '["복근","둔근","햄스트링","어깨"]'::jsonb, '["리포머 저항을 낮게 설정하고 기본 동작을 수행한다.","발과 무릎 정렬을 유지한다.","처음에는 지도자 안내에 따라 진행한다."]'::jsonb, '02022', 'Pilates reformer, light'),
    ('compendium-2024-pilates-reformer-moderate', '리포머 필라테스 보통 강도', '필라테스', '중급', 4.00, '중급', '리포머', '["코어"]'::jsonb, '["복근","둔근","햄스트링","어깨","대퇴사두근"]'::jsonb, '["중간 저항으로 코어와 하체 동작을 연결한다.","반동 없이 천천히 밀고 돌아온다.","골반이 흔들리지 않도록 조절한다."]'::jsonb, '02023', 'Pilates reformer, moderate'),
    ('compendium-2024-pilates-fitball', '핏볼 필라테스', '필라테스', '중급', 2.80, '중급', '핏볼', '["코어"]'::jsonb, '["복근","허리","둔근","어깨"]'::jsonb, '["핏볼 위에서 균형을 잡으며 코어 동작을 수행한다.","공이 미끄러지지 않는 바닥에서 진행한다.","불안정성이 크면 동작 범위를 줄인다."]'::jsonb, '02024', 'Pilates or conditioning with exercise ball'),
    ('compendium-2024-pilates-power', '고강도 필라테스', '필라테스', '고급', 5.00, '고급', '매트', '["코어"]'::jsonb, '["복근","둔근","햄스트링","어깨","대퇴사두근"]'::jsonb, '["코어 동작과 전신 근지구력 동작을 빠르게 연결한다.","호흡과 자세가 무너지면 속도를 낮춘다.","고강도 세트 후 충분히 회복한다."]'::jsonb, '02025', 'Pilates, vigorous')
)
insert into exercises (
    external_id,
    name,
    category,
    intensity,
    met,
    level,
    equipment,
    primary_muscles,
    secondary_muscles,
    instructions,
    image_urls,
    source,
    source_license,
    source_url,
    raw_data,
    created_at,
    updated_at
)
select
    external_id,
    name,
    category,
    intensity,
    met,
    level,
    equipment,
    primary_muscles,
    secondary_muscles,
    instructions,
    '[]'::jsonb,
    '2024 Adult Compendium',
    'MET reference',
    'https://pacompendium.com/adult-compendium/',
    jsonb_build_object(
        'sourceCode', source_code,
        'englishName', english_name,
        'metSource', '2024 Adult Compendium of Physical Activities',
        'seededAt', '2026-04-30'
    ),
    now(),
    now()
from seed
on conflict (external_id) do update
set name = excluded.name,
    category = excluded.category,
    intensity = excluded.intensity,
    met = excluded.met,
    level = excluded.level,
    equipment = excluded.equipment,
    primary_muscles = excluded.primary_muscles,
    secondary_muscles = excluded.secondary_muscles,
    instructions = excluded.instructions,
    image_urls = excluded.image_urls,
    source = excluded.source,
    source_license = excluded.source_license,
    source_url = excluded.source_url,
    raw_data = excluded.raw_data,
    updated_at = now();

commit;
