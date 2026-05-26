-- Evidence-based default routine library.
-- References used for the routine frequency/intensity split:
-- - CDC Adult Activity guidelines: 150 minutes/week moderate aerobic activity + 2 days/week muscle-strengthening activity.
--   https://www.cdc.gov/physical-activity-basics/guidelines/adults.html
-- - ACSM Position Stands list: exercise prescription and resistance-training progression statements.
--   https://acsm.org/education-resources/pronouncements-scientific-communications/position-stands/
-- - ACSM resistance progression summary: novice/intermediate 2-3 d/week, advanced 4-5 d/week.
--   https://pubmed.ncbi.nlm.nih.gov/11828249/

with routine_seed (
    name,
    description,
    difficulty,
    estimated_minutes,
    target_experience_level,
    target_current_exercise_statuses,
    goal_types,
    place_types,
    weekly_frequency,
    recommended_exercise_types
) as (
    values
        ('초급 맨몸 근력 습관 루틴', '집에서 맨몸 하체, 상체, 코어를 번갈아 수행해 근력 운동 습관을 만드는 초급 루틴입니다.', 'easy', 20, 'beginner', '["none", "occasional"]'::jsonb, '["habit", "strength"]'::jsonb, '["home", "other"]'::jsonb, 3, '["bodyweight", "strength"]'::jsonb),
        ('초급 걷기-조깅 전환 루틴', '걷기에서 가벼운 조깅으로 넘어가며 유산소 지속 시간을 늘리는 초급 야외 루틴입니다.', 'easy', 25, 'beginner', '["none", "occasional"]'::jsonb, '["habit", "stamina", "weight_loss"]'::jsonb, '["outdoor", "gym", "other"]'::jsonb, 3, '["walking", "running", "cardio"]'::jsonb),
        ('초급 수영 적응 루틴', '수영장 이용자를 위해 수중 유산소와 자유형 보통 강도를 섞은 초급 루틴입니다.', 'easy', 25, 'beginner', '["none", "occasional"]'::jsonb, '["stamina", "stress_relief", "habit"]'::jsonb, '["facility", "other"]'::jsonb, 2, '["swimming", "cardio"]'::jsonb),
        ('초급 실내 사이클 루틴', '실내 자전거 가벼운 강도와 스트레칭을 묶어 무릎 부담을 줄인 초급 유산소 루틴입니다.', 'easy', 25, 'beginner', '["none", "occasional"]'::jsonb, '["stamina", "weight_loss", "habit"]'::jsonb, '["gym", "home"]'::jsonb, 3, '["cardio"]'::jsonb),
        ('초급 요가 필라테스 코어 루틴', '하타 요가, 매트 필라테스, 기본 스트레칭으로 코어와 유연성을 만드는 초급 루틴입니다.', 'easy', 25, 'beginner', '["none", "occasional"]'::jsonb, '["stress_relief", "habit", "strength"]'::jsonb, '["home", "facility", "other"]'::jsonb, 3, '["yoga_pilates", "stretching", "bodyweight"]'::jsonb),
        ('초급 유산소 머신 입문 루틴', '러닝머신 걷기, 일립티컬, 고정식 자전거로 헬스장 유산소에 적응하는 초급 루틴입니다.', 'easy', 25, 'beginner', '["none", "occasional"]'::jsonb, '["stamina", "weight_loss", "habit"]'::jsonb, '["gym"]'::jsonb, 3, '["cardio", "walking"]'::jsonb),

        ('중급 상하체 근력 분할 루틴', '상체와 하체를 나누어 주 4회 반복하는 중급 헬스장 근력 루틴입니다.', 'medium', 40, 'intermediate', '["occasional", "regular"]'::jsonb, '["strength", "habit"]'::jsonb, '["gym"]'::jsonb, 4, '["strength"]'::jsonb),
        ('중급 러닝 지구력 루틴', '조깅, 템포 러닝, 회복 조깅을 섞어 러닝 지속력을 키우는 중급 루틴입니다.', 'medium', 35, 'intermediate', '["occasional", "regular"]'::jsonb, '["stamina", "weight_loss", "habit"]'::jsonb, '["outdoor", "gym"]'::jsonb, 4, '["running", "cardio"]'::jsonb),
        ('중급 수영 컨디셔닝 루틴', '자유형, 평영, 수중 에어로빅을 번갈아 수행하는 중급 수영 컨디셔닝 루틴입니다.', 'medium', 35, 'intermediate', '["occasional", "regular"]'::jsonb, '["stamina", "stress_relief"]'::jsonb, '["facility"]'::jsonb, 3, '["swimming", "cardio"]'::jsonb),
        ('중급 사이클 인터벌 루틴', '보통 강도 사이클과 스피닝을 섞어 심폐 지구력을 높이는 중급 루틴입니다.', 'medium', 35, 'intermediate', '["occasional", "regular"]'::jsonb, '["stamina", "weight_loss"]'::jsonb, '["gym", "outdoor"]'::jsonb, 3, '["cardio"]'::jsonb),
        ('중급 요가 필라테스 밸런스 루틴', '빈야사 요가와 리포머 필라테스로 코어, 균형, 유연성을 함께 올리는 중급 루틴입니다.', 'medium', 35, 'intermediate', '["occasional", "regular"]'::jsonb, '["stress_relief", "strength", "habit"]'::jsonb, '["home", "facility"]'::jsonb, 3, '["yoga_pilates", "stretching"]'::jsonb),
        ('중급 하이브리드 체지방 감량 루틴', '근력 운동과 유산소 머신을 같은 주에 배치한 중급 체지방 감량 루틴입니다.', 'medium', 35, 'intermediate', '["occasional", "regular"]'::jsonb, '["weight_loss", "strength", "stamina"]'::jsonb, '["gym"]'::jsonb, 4, '["strength", "cardio"]'::jsonb),

        ('고급 근력 파워 분할 루틴', '스쿼트, 데드리프트, 벤치, 로우 중심으로 주 4회 수행하는 고급 근력 루틴입니다.', 'hard', 55, 'advanced', '["regular"]'::jsonb, '["strength"]'::jsonb, '["gym"]'::jsonb, 4, '["strength"]'::jsonb),
        ('고급 러닝 인터벌 루틴', '고강도 러닝, 계단 러닝, 회복 조깅을 나누어 수행하는 고급 러닝 루틴입니다.', 'hard', 45, 'advanced', '["regular"]'::jsonb, '["stamina", "weight_loss"]'::jsonb, '["outdoor", "gym"]'::jsonb, 4, '["running", "cardio"]'::jsonb),
        ('고급 수영 고강도 루틴', '빠른 자유형, 접영, 평영을 활용해 강도 높은 수영 세션을 구성한 고급 루틴입니다.', 'hard', 45, 'advanced', '["regular"]'::jsonb, '["stamina", "strength"]'::jsonb, '["facility"]'::jsonb, 4, '["swimming", "cardio"]'::jsonb),
        ('고급 사이클 고강도 루틴', '스피닝, 산악자전거, 고속 사이클을 조합한 고급 심폐 루틴입니다.', 'hard', 45, 'advanced', '["regular"]'::jsonb, '["stamina", "weight_loss"]'::jsonb, '["gym", "outdoor"]'::jsonb, 4, '["cardio"]'::jsonb),
        ('고급 하이브리드 컨디셔닝 루틴', '근력, 러닝, 사이클, 회복을 주 5회로 나눈 고급 종합 컨디셔닝 루틴입니다.', 'hard', 45, 'advanced', '["regular"]'::jsonb, '["stamina", "strength", "weight_loss"]'::jsonb, '["gym", "outdoor"]'::jsonb, 5, '["strength", "running", "cardio", "stretching"]'::jsonb),
        ('고급 파워 요가 필라테스 루틴', '파워 요가, 아쉬탕가 요가, 고강도 필라테스로 코어와 유연성을 끌어올리는 고급 루틴입니다.', 'hard', 40, 'advanced', '["regular"]'::jsonb, '["strength", "stress_relief"]'::jsonb, '["home", "facility"]'::jsonb, 4, '["yoga_pilates", "stretching"]'::jsonb)
)
update routines r
set description = s.description,
    difficulty = s.difficulty,
    estimated_minutes = s.estimated_minutes,
    target_experience_level = s.target_experience_level,
    target_current_exercise_statuses = s.target_current_exercise_statuses,
    goal_types = s.goal_types,
    place_types = s.place_types,
    weekly_frequency = s.weekly_frequency,
    recommended_exercise_types = s.recommended_exercise_types,
    is_active = true,
    updated_at = now()
from routine_seed s
where r.is_default = true
  and r.name = s.name;

with routine_seed (
    name,
    description,
    difficulty,
    estimated_minutes,
    target_experience_level,
    target_current_exercise_statuses,
    goal_types,
    place_types,
    weekly_frequency,
    recommended_exercise_types
) as (
    values
        ('초급 맨몸 근력 습관 루틴', '집에서 맨몸 하체, 상체, 코어를 번갈아 수행해 근력 운동 습관을 만드는 초급 루틴입니다.', 'easy', 20, 'beginner', '["none", "occasional"]'::jsonb, '["habit", "strength"]'::jsonb, '["home", "other"]'::jsonb, 3, '["bodyweight", "strength"]'::jsonb),
        ('초급 걷기-조깅 전환 루틴', '걷기에서 가벼운 조깅으로 넘어가며 유산소 지속 시간을 늘리는 초급 야외 루틴입니다.', 'easy', 25, 'beginner', '["none", "occasional"]'::jsonb, '["habit", "stamina", "weight_loss"]'::jsonb, '["outdoor", "gym", "other"]'::jsonb, 3, '["walking", "running", "cardio"]'::jsonb),
        ('초급 수영 적응 루틴', '수영장 이용자를 위해 수중 유산소와 자유형 보통 강도를 섞은 초급 루틴입니다.', 'easy', 25, 'beginner', '["none", "occasional"]'::jsonb, '["stamina", "stress_relief", "habit"]'::jsonb, '["facility", "other"]'::jsonb, 2, '["swimming", "cardio"]'::jsonb),
        ('초급 실내 사이클 루틴', '실내 자전거 가벼운 강도와 스트레칭을 묶어 무릎 부담을 줄인 초급 유산소 루틴입니다.', 'easy', 25, 'beginner', '["none", "occasional"]'::jsonb, '["stamina", "weight_loss", "habit"]'::jsonb, '["gym", "home"]'::jsonb, 3, '["cardio"]'::jsonb),
        ('초급 요가 필라테스 코어 루틴', '하타 요가, 매트 필라테스, 기본 스트레칭으로 코어와 유연성을 만드는 초급 루틴입니다.', 'easy', 25, 'beginner', '["none", "occasional"]'::jsonb, '["stress_relief", "habit", "strength"]'::jsonb, '["home", "facility", "other"]'::jsonb, 3, '["yoga_pilates", "stretching", "bodyweight"]'::jsonb),
        ('초급 유산소 머신 입문 루틴', '러닝머신 걷기, 일립티컬, 고정식 자전거로 헬스장 유산소에 적응하는 초급 루틴입니다.', 'easy', 25, 'beginner', '["none", "occasional"]'::jsonb, '["stamina", "weight_loss", "habit"]'::jsonb, '["gym"]'::jsonb, 3, '["cardio", "walking"]'::jsonb),
        ('중급 상하체 근력 분할 루틴', '상체와 하체를 나누어 주 4회 반복하는 중급 헬스장 근력 루틴입니다.', 'medium', 40, 'intermediate', '["occasional", "regular"]'::jsonb, '["strength", "habit"]'::jsonb, '["gym"]'::jsonb, 4, '["strength"]'::jsonb),
        ('중급 러닝 지구력 루틴', '조깅, 템포 러닝, 회복 조깅을 섞어 러닝 지속력을 키우는 중급 루틴입니다.', 'medium', 35, 'intermediate', '["occasional", "regular"]'::jsonb, '["stamina", "weight_loss", "habit"]'::jsonb, '["outdoor", "gym"]'::jsonb, 4, '["running", "cardio"]'::jsonb),
        ('중급 수영 컨디셔닝 루틴', '자유형, 평영, 수중 에어로빅을 번갈아 수행하는 중급 수영 컨디셔닝 루틴입니다.', 'medium', 35, 'intermediate', '["occasional", "regular"]'::jsonb, '["stamina", "stress_relief"]'::jsonb, '["facility"]'::jsonb, 3, '["swimming", "cardio"]'::jsonb),
        ('중급 사이클 인터벌 루틴', '보통 강도 사이클과 스피닝을 섞어 심폐 지구력을 높이는 중급 루틴입니다.', 'medium', 35, 'intermediate', '["occasional", "regular"]'::jsonb, '["stamina", "weight_loss"]'::jsonb, '["gym", "outdoor"]'::jsonb, 3, '["cardio"]'::jsonb),
        ('중급 요가 필라테스 밸런스 루틴', '빈야사 요가와 리포머 필라테스로 코어, 균형, 유연성을 함께 올리는 중급 루틴입니다.', 'medium', 35, 'intermediate', '["occasional", "regular"]'::jsonb, '["stress_relief", "strength", "habit"]'::jsonb, '["home", "facility"]'::jsonb, 3, '["yoga_pilates", "stretching"]'::jsonb),
        ('중급 하이브리드 체지방 감량 루틴', '근력 운동과 유산소 머신을 같은 주에 배치한 중급 체지방 감량 루틴입니다.', 'medium', 35, 'intermediate', '["occasional", "regular"]'::jsonb, '["weight_loss", "strength", "stamina"]'::jsonb, '["gym"]'::jsonb, 4, '["strength", "cardio"]'::jsonb),
        ('고급 근력 파워 분할 루틴', '스쿼트, 데드리프트, 벤치, 로우 중심으로 주 4회 수행하는 고급 근력 루틴입니다.', 'hard', 55, 'advanced', '["regular"]'::jsonb, '["strength"]'::jsonb, '["gym"]'::jsonb, 4, '["strength"]'::jsonb),
        ('고급 러닝 인터벌 루틴', '고강도 러닝, 계단 러닝, 회복 조깅을 나누어 수행하는 고급 러닝 루틴입니다.', 'hard', 45, 'advanced', '["regular"]'::jsonb, '["stamina", "weight_loss"]'::jsonb, '["outdoor", "gym"]'::jsonb, 4, '["running", "cardio"]'::jsonb),
        ('고급 수영 고강도 루틴', '빠른 자유형, 접영, 평영을 활용해 강도 높은 수영 세션을 구성한 고급 루틴입니다.', 'hard', 45, 'advanced', '["regular"]'::jsonb, '["stamina", "strength"]'::jsonb, '["facility"]'::jsonb, 4, '["swimming", "cardio"]'::jsonb),
        ('고급 사이클 고강도 루틴', '스피닝, 산악자전거, 고속 사이클을 조합한 고급 심폐 루틴입니다.', 'hard', 45, 'advanced', '["regular"]'::jsonb, '["stamina", "weight_loss"]'::jsonb, '["gym", "outdoor"]'::jsonb, 4, '["cardio"]'::jsonb),
        ('고급 하이브리드 컨디셔닝 루틴', '근력, 러닝, 사이클, 회복을 주 5회로 나눈 고급 종합 컨디셔닝 루틴입니다.', 'hard', 45, 'advanced', '["regular"]'::jsonb, '["stamina", "strength", "weight_loss"]'::jsonb, '["gym", "outdoor"]'::jsonb, 5, '["strength", "running", "cardio", "stretching"]'::jsonb),
        ('고급 파워 요가 필라테스 루틴', '파워 요가, 아쉬탕가 요가, 고강도 필라테스로 코어와 유연성을 끌어올리는 고급 루틴입니다.', 'hard', 40, 'advanced', '["regular"]'::jsonb, '["strength", "stress_relief"]'::jsonb, '["home", "facility"]'::jsonb, 4, '["yoga_pilates", "stretching"]'::jsonb)
)
insert into routines (
    user_id,
    name,
    description,
    is_default,
    difficulty,
    estimated_minutes,
    target_experience_level,
    target_current_exercise_statuses,
    goal_types,
    place_types,
    weekly_frequency,
    recommended_exercise_types,
    is_active,
    created_at,
    updated_at
)
select
    null,
    s.name,
    s.description,
    true,
    s.difficulty,
    s.estimated_minutes,
    s.target_experience_level,
    s.target_current_exercise_statuses,
    s.goal_types,
    s.place_types,
    s.weekly_frequency,
    s.recommended_exercise_types,
    true,
    now(),
    now()
from routine_seed s
where not exists (
    select 1
    from routines r
    where r.is_default = true
      and r.name = s.name
);

with session_seed (routine_name, seq, day_of_week, session_name, session_type, estimated_minutes) as (
    values
        ('초급 맨몸 근력 습관 루틴', 1, 'MONDAY', '맨몸 하체', 'bodyweight_strength', 20),
        ('초급 맨몸 근력 습관 루틴', 2, 'WEDNESDAY', '맨몸 상체', 'bodyweight_strength', 20),
        ('초급 맨몸 근력 습관 루틴', 3, 'FRIDAY', '코어와 회복', 'core_recovery', 20),
        ('초급 걷기-조깅 전환 루틴', 1, 'MONDAY', '걷기 적응', 'cardio', 25),
        ('초급 걷기-조깅 전환 루틴', 2, 'WEDNESDAY', '걷기/조깅 반복', 'cardio', 25),
        ('초급 걷기-조깅 전환 루틴', 3, 'SATURDAY', '긴 걷기', 'cardio_recovery', 30),
        ('초급 수영 적응 루틴', 1, 'TUESDAY', '수중 적응', 'swimming', 25),
        ('초급 수영 적응 루틴', 2, 'THURSDAY', '자유형 보통', 'swimming', 25),
        ('초급 실내 사이클 루틴', 1, 'MONDAY', '가벼운 실내 자전거', 'cycling', 25),
        ('초급 실내 사이클 루틴', 2, 'WEDNESDAY', '보통 강도 자전거', 'cycling', 25),
        ('초급 실내 사이클 루틴', 3, 'FRIDAY', '사이클 회복', 'cycling_recovery', 20),
        ('초급 요가 필라테스 코어 루틴', 1, 'MONDAY', '하타 요가', 'yoga', 25),
        ('초급 요가 필라테스 코어 루틴', 2, 'WEDNESDAY', '매트 필라테스', 'pilates', 25),
        ('초급 요가 필라테스 코어 루틴', 3, 'FRIDAY', '전신 스트레칭', 'mobility', 20),
        ('초급 유산소 머신 입문 루틴', 1, 'MONDAY', '러닝머신 걷기', 'cardio', 25),
        ('초급 유산소 머신 입문 루틴', 2, 'WEDNESDAY', '일립티컬', 'cardio', 25),
        ('초급 유산소 머신 입문 루틴', 3, 'FRIDAY', '고정식 자전거', 'cardio', 25),

        ('중급 상하체 근력 분할 루틴', 1, 'MONDAY', '상체 밀기', 'upper_push', 40),
        ('중급 상하체 근력 분할 루틴', 2, 'TUESDAY', '하체', 'lower_body', 40),
        ('중급 상하체 근력 분할 루틴', 3, 'THURSDAY', '상체 당기기', 'upper_pull', 40),
        ('중급 상하체 근력 분할 루틴', 4, 'SATURDAY', '하체/코어', 'lower_core', 40),
        ('중급 러닝 지구력 루틴', 1, 'MONDAY', '가벼운 조깅', 'running', 35),
        ('중급 러닝 지구력 루틴', 2, 'WEDNESDAY', '템포 러닝', 'running', 35),
        ('중급 러닝 지구력 루틴', 3, 'FRIDAY', '회복 조깅', 'running_recovery', 30),
        ('중급 러닝 지구력 루틴', 4, 'SUNDAY', '긴 지속주', 'running', 45),
        ('중급 수영 컨디셔닝 루틴', 1, 'MONDAY', '자유형 랩', 'swimming', 35),
        ('중급 수영 컨디셔닝 루틴', 2, 'WEDNESDAY', '평영 랩', 'swimming', 35),
        ('중급 수영 컨디셔닝 루틴', 3, 'FRIDAY', '수중 회복', 'swimming_recovery', 30),
        ('중급 사이클 인터벌 루틴', 1, 'TUESDAY', '보통 강도 사이클', 'cycling', 35),
        ('중급 사이클 인터벌 루틴', 2, 'THURSDAY', '스피닝 인터벌', 'cycling_interval', 35),
        ('중급 사이클 인터벌 루틴', 3, 'SATURDAY', '야외 사이클', 'cycling', 40),
        ('중급 요가 필라테스 밸런스 루틴', 1, 'MONDAY', '빈야사 요가', 'yoga', 35),
        ('중급 요가 필라테스 밸런스 루틴', 2, 'WEDNESDAY', '리포머 필라테스', 'pilates', 35),
        ('중급 요가 필라테스 밸런스 루틴', 3, 'FRIDAY', '밸런스 회복', 'mobility', 30),
        ('중급 하이브리드 체지방 감량 루틴', 1, 'MONDAY', '근력 A', 'strength', 35),
        ('중급 하이브리드 체지방 감량 루틴', 2, 'TUESDAY', '유산소 A', 'cardio', 35),
        ('중급 하이브리드 체지방 감량 루틴', 3, 'THURSDAY', '근력 B', 'strength', 35),
        ('중급 하이브리드 체지방 감량 루틴', 4, 'SATURDAY', '유산소 B', 'cardio', 35),

        ('고급 근력 파워 분할 루틴', 1, 'MONDAY', '스쿼트 중심', 'power_lower', 55),
        ('고급 근력 파워 분할 루틴', 2, 'TUESDAY', '벤치 중심', 'power_upper', 55),
        ('고급 근력 파워 분할 루틴', 3, 'THURSDAY', '데드리프트 중심', 'power_lower', 55),
        ('고급 근력 파워 분할 루틴', 4, 'SATURDAY', '전신 보조', 'power_assist', 50),
        ('고급 러닝 인터벌 루틴', 1, 'MONDAY', '고속 인터벌', 'running_interval', 45),
        ('고급 러닝 인터벌 루틴', 2, 'WEDNESDAY', '계단 러닝', 'running_interval', 40),
        ('고급 러닝 인터벌 루틴', 3, 'FRIDAY', '회복 조깅', 'running_recovery', 35),
        ('고급 러닝 인터벌 루틴', 4, 'SUNDAY', '장거리 페이스', 'running', 50),
        ('고급 수영 고강도 루틴', 1, 'MONDAY', '빠른 자유형', 'swimming', 45),
        ('고급 수영 고강도 루틴', 2, 'WEDNESDAY', '접영 세션', 'swimming', 40),
        ('고급 수영 고강도 루틴', 3, 'FRIDAY', '평영 랩', 'swimming', 45),
        ('고급 수영 고강도 루틴', 4, 'SATURDAY', '오픈워터 대비', 'swimming', 45),
        ('고급 사이클 고강도 루틴', 1, 'MONDAY', '고강도 실내 자전거', 'cycling_interval', 45),
        ('고급 사이클 고강도 루틴', 2, 'WEDNESDAY', '스피닝 클래스', 'cycling_interval', 45),
        ('고급 사이클 고강도 루틴', 3, 'FRIDAY', '고속 사이클', 'cycling', 45),
        ('고급 사이클 고강도 루틴', 4, 'SUNDAY', '산악자전거', 'cycling', 50),
        ('고급 하이브리드 컨디셔닝 루틴', 1, 'MONDAY', '파워 근력', 'strength', 45),
        ('고급 하이브리드 컨디셔닝 루틴', 2, 'TUESDAY', '러닝 인터벌', 'running_interval', 45),
        ('고급 하이브리드 컨디셔닝 루틴', 3, 'THURSDAY', '사이클 인터벌', 'cycling_interval', 45),
        ('고급 하이브리드 컨디셔닝 루틴', 4, 'FRIDAY', '전신 근력', 'strength', 45),
        ('고급 하이브리드 컨디셔닝 루틴', 5, 'SUNDAY', '회복 가동성', 'mobility', 30),
        ('고급 파워 요가 필라테스 루틴', 1, 'MONDAY', '파워 요가', 'yoga', 40),
        ('고급 파워 요가 필라테스 루틴', 2, 'WEDNESDAY', '고강도 필라테스', 'pilates', 40),
        ('고급 파워 요가 필라테스 루틴', 3, 'FRIDAY', '아쉬탕가 요가', 'yoga', 40),
        ('고급 파워 요가 필라테스 루틴', 4, 'SATURDAY', '회복 스트레칭', 'mobility', 30)
)
insert into routine_sessions (
    routine_id,
    day_of_week,
    session_name,
    session_type,
    seq,
    estimated_minutes,
    is_active,
    created_at,
    updated_at
)
select
    r.id,
    s.day_of_week,
    s.session_name,
    s.session_type,
    s.seq,
    s.estimated_minutes,
    true,
    now(),
    now()
from session_seed s
join routines r
    on r.is_default = true
   and r.name = s.routine_name
where not exists (
    select 1
    from routine_sessions rs
    where rs.routine_id = r.id
      and rs.seq = s.seq
);

with session_seed (routine_name, seq, day_of_week, session_name, session_type, estimated_minutes) as (
    values
        ('초급 맨몸 근력 습관 루틴', 1, 'MONDAY', '맨몸 하체', 'bodyweight_strength', 20),
        ('초급 맨몸 근력 습관 루틴', 2, 'WEDNESDAY', '맨몸 상체', 'bodyweight_strength', 20),
        ('초급 맨몸 근력 습관 루틴', 3, 'FRIDAY', '코어와 회복', 'core_recovery', 20),
        ('초급 걷기-조깅 전환 루틴', 1, 'MONDAY', '걷기 적응', 'cardio', 25),
        ('초급 걷기-조깅 전환 루틴', 2, 'WEDNESDAY', '걷기/조깅 반복', 'cardio', 25),
        ('초급 걷기-조깅 전환 루틴', 3, 'SATURDAY', '긴 걷기', 'cardio_recovery', 30),
        ('초급 수영 적응 루틴', 1, 'TUESDAY', '수중 적응', 'swimming', 25),
        ('초급 수영 적응 루틴', 2, 'THURSDAY', '자유형 보통', 'swimming', 25),
        ('초급 실내 사이클 루틴', 1, 'MONDAY', '가벼운 실내 자전거', 'cycling', 25),
        ('초급 실내 사이클 루틴', 2, 'WEDNESDAY', '보통 강도 자전거', 'cycling', 25),
        ('초급 실내 사이클 루틴', 3, 'FRIDAY', '사이클 회복', 'cycling_recovery', 20),
        ('초급 요가 필라테스 코어 루틴', 1, 'MONDAY', '하타 요가', 'yoga', 25),
        ('초급 요가 필라테스 코어 루틴', 2, 'WEDNESDAY', '매트 필라테스', 'pilates', 25),
        ('초급 요가 필라테스 코어 루틴', 3, 'FRIDAY', '전신 스트레칭', 'mobility', 20),
        ('초급 유산소 머신 입문 루틴', 1, 'MONDAY', '러닝머신 걷기', 'cardio', 25),
        ('초급 유산소 머신 입문 루틴', 2, 'WEDNESDAY', '일립티컬', 'cardio', 25),
        ('초급 유산소 머신 입문 루틴', 3, 'FRIDAY', '고정식 자전거', 'cardio', 25),
        ('중급 상하체 근력 분할 루틴', 1, 'MONDAY', '상체 밀기', 'upper_push', 40),
        ('중급 상하체 근력 분할 루틴', 2, 'TUESDAY', '하체', 'lower_body', 40),
        ('중급 상하체 근력 분할 루틴', 3, 'THURSDAY', '상체 당기기', 'upper_pull', 40),
        ('중급 상하체 근력 분할 루틴', 4, 'SATURDAY', '하체/코어', 'lower_core', 40),
        ('중급 러닝 지구력 루틴', 1, 'MONDAY', '가벼운 조깅', 'running', 35),
        ('중급 러닝 지구력 루틴', 2, 'WEDNESDAY', '템포 러닝', 'running', 35),
        ('중급 러닝 지구력 루틴', 3, 'FRIDAY', '회복 조깅', 'running_recovery', 30),
        ('중급 러닝 지구력 루틴', 4, 'SUNDAY', '긴 지속주', 'running', 45),
        ('중급 수영 컨디셔닝 루틴', 1, 'MONDAY', '자유형 랩', 'swimming', 35),
        ('중급 수영 컨디셔닝 루틴', 2, 'WEDNESDAY', '평영 랩', 'swimming', 35),
        ('중급 수영 컨디셔닝 루틴', 3, 'FRIDAY', '수중 회복', 'swimming_recovery', 30),
        ('중급 사이클 인터벌 루틴', 1, 'TUESDAY', '보통 강도 사이클', 'cycling', 35),
        ('중급 사이클 인터벌 루틴', 2, 'THURSDAY', '스피닝 인터벌', 'cycling_interval', 35),
        ('중급 사이클 인터벌 루틴', 3, 'SATURDAY', '야외 사이클', 'cycling', 40),
        ('중급 요가 필라테스 밸런스 루틴', 1, 'MONDAY', '빈야사 요가', 'yoga', 35),
        ('중급 요가 필라테스 밸런스 루틴', 2, 'WEDNESDAY', '리포머 필라테스', 'pilates', 35),
        ('중급 요가 필라테스 밸런스 루틴', 3, 'FRIDAY', '밸런스 회복', 'mobility', 30),
        ('중급 하이브리드 체지방 감량 루틴', 1, 'MONDAY', '근력 A', 'strength', 35),
        ('중급 하이브리드 체지방 감량 루틴', 2, 'TUESDAY', '유산소 A', 'cardio', 35),
        ('중급 하이브리드 체지방 감량 루틴', 3, 'THURSDAY', '근력 B', 'strength', 35),
        ('중급 하이브리드 체지방 감량 루틴', 4, 'SATURDAY', '유산소 B', 'cardio', 35),
        ('고급 근력 파워 분할 루틴', 1, 'MONDAY', '스쿼트 중심', 'power_lower', 55),
        ('고급 근력 파워 분할 루틴', 2, 'TUESDAY', '벤치 중심', 'power_upper', 55),
        ('고급 근력 파워 분할 루틴', 3, 'THURSDAY', '데드리프트 중심', 'power_lower', 55),
        ('고급 근력 파워 분할 루틴', 4, 'SATURDAY', '전신 보조', 'power_assist', 50),
        ('고급 러닝 인터벌 루틴', 1, 'MONDAY', '고속 인터벌', 'running_interval', 45),
        ('고급 러닝 인터벌 루틴', 2, 'WEDNESDAY', '계단 러닝', 'running_interval', 40),
        ('고급 러닝 인터벌 루틴', 3, 'FRIDAY', '회복 조깅', 'running_recovery', 35),
        ('고급 러닝 인터벌 루틴', 4, 'SUNDAY', '장거리 페이스', 'running', 50),
        ('고급 수영 고강도 루틴', 1, 'MONDAY', '빠른 자유형', 'swimming', 45),
        ('고급 수영 고강도 루틴', 2, 'WEDNESDAY', '접영 세션', 'swimming', 40),
        ('고급 수영 고강도 루틴', 3, 'FRIDAY', '평영 랩', 'swimming', 45),
        ('고급 수영 고강도 루틴', 4, 'SATURDAY', '오픈워터 대비', 'swimming', 45),
        ('고급 사이클 고강도 루틴', 1, 'MONDAY', '고강도 실내 자전거', 'cycling_interval', 45),
        ('고급 사이클 고강도 루틴', 2, 'WEDNESDAY', '스피닝 클래스', 'cycling_interval', 45),
        ('고급 사이클 고강도 루틴', 3, 'FRIDAY', '고속 사이클', 'cycling', 45),
        ('고급 사이클 고강도 루틴', 4, 'SUNDAY', '산악자전거', 'cycling', 50),
        ('고급 하이브리드 컨디셔닝 루틴', 1, 'MONDAY', '파워 근력', 'strength', 45),
        ('고급 하이브리드 컨디셔닝 루틴', 2, 'TUESDAY', '러닝 인터벌', 'running_interval', 45),
        ('고급 하이브리드 컨디셔닝 루틴', 3, 'THURSDAY', '사이클 인터벌', 'cycling_interval', 45),
        ('고급 하이브리드 컨디셔닝 루틴', 4, 'FRIDAY', '전신 근력', 'strength', 45),
        ('고급 하이브리드 컨디셔닝 루틴', 5, 'SUNDAY', '회복 가동성', 'mobility', 30),
        ('고급 파워 요가 필라테스 루틴', 1, 'MONDAY', '파워 요가', 'yoga', 40),
        ('고급 파워 요가 필라테스 루틴', 2, 'WEDNESDAY', '고강도 필라테스', 'pilates', 40),
        ('고급 파워 요가 필라테스 루틴', 3, 'FRIDAY', '아쉬탕가 요가', 'yoga', 40),
        ('고급 파워 요가 필라테스 루틴', 4, 'SATURDAY', '회복 스트레칭', 'mobility', 30)
)
update routine_sessions rs
set day_of_week = s.day_of_week,
    session_name = s.session_name,
    session_type = s.session_type,
    estimated_minutes = s.estimated_minutes,
    is_active = true,
    updated_at = now()
from session_seed s
join routines r
    on r.is_default = true
   and r.name = s.routine_name
where rs.routine_id = r.id
  and rs.seq = s.seq;

with item_seed (routine_name, session_seq, item_seq, exercise_name, reps, sets, duration_sec, rest_sec) as (
    values
        ('초급 맨몸 근력 습관 루틴', 1, 1, '바디웨이트 스쿼트', 12, 2, null::integer, 45),
        ('초급 맨몸 근력 습관 루틴', 1, 2, '덤벨 런지', 10, 2, null::integer, 45),
        ('초급 맨몸 근력 습관 루틴', 2, 1, '인클라인 푸시업 클로즈 그립', 10, 2, null::integer, 45),
        ('초급 맨몸 근력 습관 루틴', 2, 2, '바디웨이트 미드 로우', 10, 2, null::integer, 45),
        ('초급 맨몸 근력 습관 루틴', 3, 1, '데드버그', 10, 2, null::integer, 45),
        ('초급 맨몸 근력 습관 루틴', 3, 2, '90/90 햄스트링', null::integer, 1, 180, 30),

        ('초급 걷기-조깅 전환 루틴', 1, 1, '걷기, 런닝머신', null::integer, 1, 1200, 60),
        ('초급 걷기-조깅 전환 루틴', 1, 2, '러너의 스트레칭', null::integer, 1, 180, 30),
        ('초급 걷기-조깅 전환 루틴', 2, 1, '조깅 일반', null::integer, 1, 900, 60),
        ('초급 걷기-조깅 전환 루틴', 2, 2, '걷기, 런닝머신', null::integer, 1, 300, 30),
        ('초급 걷기-조깅 전환 루틴', 3, 1, '트레일 러닝/워킹', null::integer, 1, 1500, 60),

        ('초급 수영 적응 루틴', 1, 1, '수중 에어로빅', null::integer, 1, 1200, 60),
        ('초급 수영 적응 루틴', 1, 2, '입영 보통 강도', null::integer, 1, 300, 60),
        ('초급 수영 적응 루틴', 2, 1, '자유형 보통 랩 수영', null::integer, 1, 1200, 60),
        ('초급 수영 적응 루틴', 2, 2, '어깨 스트레칭', null::integer, 1, 180, 30),

        ('초급 실내 사이클 루틴', 1, 1, '실내 자전거 가벼운 강도', null::integer, 1, 1200, 60),
        ('초급 실내 사이클 루틴', 1, 2, '기립 고관절 굴근', null::integer, 1, 180, 30),
        ('초급 실내 사이클 루틴', 2, 1, '실내 자전거 보통 강도', null::integer, 1, 1200, 60),
        ('초급 실내 사이클 루틴', 3, 1, '여유 자전거 타기', null::integer, 1, 900, 60),
        ('초급 실내 사이클 루틴', 3, 2, '시티드 햄스트링 및 종아리 스트레칭', null::integer, 1, 180, 30),

        ('초급 요가 필라테스 코어 루틴', 1, 1, '하타 요가', null::integer, 1, 1200, 60),
        ('초급 요가 필라테스 코어 루틴', 2, 1, '매트 필라테스', null::integer, 1, 1200, 60),
        ('초급 요가 필라테스 코어 루틴', 2, 2, '크런치', 12, 2, null::integer, 45),
        ('초급 요가 필라테스 코어 루틴', 3, 1, '아이의 포즈', null::integer, 1, 180, 30),
        ('초급 요가 필라테스 코어 루틴', 3, 2, '고양이 스트레칭', null::integer, 1, 180, 30),
        ('초급 요가 필라테스 코어 루틴', 3, 3, '햄스트링 스트레칭', null::integer, 1, 180, 30),

        ('초급 유산소 머신 입문 루틴', 1, 1, '걷기, 런닝머신', null::integer, 1, 1200, 60),
        ('초급 유산소 머신 입문 루틴', 2, 1, '일립티컬 트레이너', null::integer, 1, 1200, 60),
        ('초급 유산소 머신 입문 루틴', 3, 1, '자전거, 고정식', null::integer, 1, 1200, 60),

        ('중급 상하체 근력 분할 루틴', 1, 1, '와이드 그립 바벨 벤치 프레스', 8, 3, null::integer, 90),
        ('중급 상하체 근력 분할 루틴', 1, 2, '삼두근 푸시다운 - 로프 부착', 10, 3, null::integer, 60),
        ('중급 상하체 근력 분할 루틴', 2, 1, '고블렛 스쿼트', 10, 3, null::integer, 90),
        ('중급 상하체 근력 분할 루틴', 2, 2, '루마니아 데드리프트', 8, 3, null::integer, 90),
        ('중급 상하체 근력 분할 루틴', 3, 1, '시티드 케이블 로우', 10, 3, null::integer, 75),
        ('중급 상하체 근력 분할 루틴', 3, 2, '업라이트 바벨 로우', 10, 3, null::integer, 75),
        ('중급 상하체 근력 분할 루틴', 4, 1, '덤벨 런지', 10, 3, null::integer, 75),
        ('중급 상하체 근력 분할 루틴', 4, 2, '케이블 크런치', 12, 3, null::integer, 60),

        ('중급 러닝 지구력 루틴', 1, 1, '조깅 일반', null::integer, 1, 1800, 60),
        ('중급 러닝 지구력 루틴', 2, 1, '러닝 시속 8.0킬로미터', null::integer, 1, 1800, 60),
        ('중급 러닝 지구력 루틴', 3, 1, '러닝 시속 6.4킬로미터', null::integer, 1, 1500, 60),
        ('중급 러닝 지구력 루틴', 4, 1, '크로스컨트리 러닝', null::integer, 1, 2400, 60),

        ('중급 수영 컨디셔닝 루틴', 1, 1, '자유형 보통 랩 수영', null::integer, 1, 1800, 60),
        ('중급 수영 컨디셔닝 루틴', 2, 1, '평영 랩 수영', null::integer, 1, 1800, 60),
        ('중급 수영 컨디셔닝 루틴', 3, 1, '수중 에어로빅', null::integer, 1, 1500, 60),

        ('중급 사이클 인터벌 루틴', 1, 1, '실내 자전거 보통 강도', null::integer, 1, 1800, 60),
        ('중급 사이클 인터벌 루틴', 2, 1, '스피닝 클래스', null::integer, 1, 1800, 60),
        ('중급 사이클 인터벌 루틴', 3, 1, '사이클 시속 19~22킬로미터', null::integer, 1, 2100, 60),

        ('중급 요가 필라테스 밸런스 루틴', 1, 1, '빈야사 요가', null::integer, 1, 1800, 60),
        ('중급 요가 필라테스 밸런스 루틴', 2, 1, '리포머 필라테스 보통 강도', null::integer, 1, 1800, 60),
        ('중급 요가 필라테스 밸런스 루틴', 3, 1, '태양경배 요가', null::integer, 1, 1200, 60),
        ('중급 요가 필라테스 밸런스 루틴', 3, 2, '월드 그레이티스트 스트레치', null::integer, 1, 300, 30),

        ('중급 하이브리드 체지방 감량 루틴', 1, 1, '고블렛 스쿼트', 10, 3, null::integer, 75),
        ('중급 하이브리드 체지방 감량 루틴', 1, 2, '시티드 케이블 로우', 10, 3, null::integer, 75),
        ('중급 하이브리드 체지방 감량 루틴', 2, 1, '조깅, 런닝머신', null::integer, 1, 1800, 60),
        ('중급 하이브리드 체지방 감량 루틴', 3, 1, '루마니아 데드리프트', 8, 3, null::integer, 90),
        ('중급 하이브리드 체지방 감량 루틴', 3, 2, '와이드 그립 바벨 벤치 프레스', 8, 3, null::integer, 90),
        ('중급 하이브리드 체지방 감량 루틴', 4, 1, '일립티컬 트레이너', null::integer, 1, 1800, 60),

        ('고급 근력 파워 분할 루틴', 1, 1, '바벨 스쿼트', 5, 4, null::integer, 120),
        ('고급 근력 파워 분할 루틴', 1, 2, '프론트 스쿼트(클린 그립)', 6, 3, null::integer, 120),
        ('고급 근력 파워 분할 루틴', 2, 1, '와이드 그립 바벨 벤치 프레스', 5, 4, null::integer, 120),
        ('고급 근력 파워 분할 루틴', 2, 2, '스미스 머신 인클라인 벤치 프레스', 8, 3, null::integer, 90),
        ('고급 근력 파워 분할 루틴', 3, 1, '바벨 데드리프트', 5, 4, null::integer, 150),
        ('고급 근력 파워 분할 루틴', 3, 2, '루마니아 데드리프트', 8, 3, null::integer, 120),
        ('고급 근력 파워 분할 루틴', 4, 1, '시티드 케이블 로우', 8, 4, null::integer, 90),
        ('고급 근력 파워 분할 루틴', 4, 2, '더블 케틀벨 푸시 프레스', 8, 3, null::integer, 90),

        ('고급 러닝 인터벌 루틴', 1, 1, '러닝 시속 11.3킬로미터', null::integer, 1, 1800, 90),
        ('고급 러닝 인터벌 루틴', 2, 1, '계단 러닝', null::integer, 1, 1500, 90),
        ('고급 러닝 인터벌 루틴', 3, 1, '조깅 일반', null::integer, 1, 1800, 60),
        ('고급 러닝 인터벌 루틴', 4, 1, '마라톤 페이스 러닝', null::integer, 1, 2700, 90),

        ('고급 수영 고강도 루틴', 1, 1, '자유형 빠른 랩 수영', null::integer, 1, 2100, 90),
        ('고급 수영 고강도 루틴', 2, 1, '접영 수영', null::integer, 1, 1800, 90),
        ('고급 수영 고강도 루틴', 3, 1, '평영 랩 수영', null::integer, 1, 2100, 90),
        ('고급 수영 고강도 루틴', 4, 1, '오픈워터 수영', null::integer, 1, 2100, 90),

        ('고급 사이클 고강도 루틴', 1, 1, '실내 자전거 고강도', null::integer, 1, 2100, 90),
        ('고급 사이클 고강도 루틴', 2, 1, '스피닝 클래스', null::integer, 1, 2100, 90),
        ('고급 사이클 고강도 루틴', 3, 1, '사이클 시속 22~26킬로미터', null::integer, 1, 2100, 90),
        ('고급 사이클 고강도 루틴', 4, 1, '산악자전거', null::integer, 1, 2400, 90),

        ('고급 하이브리드 컨디셔닝 루틴', 1, 1, '바벨 스쿼트', 5, 3, null::integer, 120),
        ('고급 하이브리드 컨디셔닝 루틴', 1, 2, '와이드 그립 바벨 벤치 프레스', 5, 3, null::integer, 120),
        ('고급 하이브리드 컨디셔닝 루틴', 2, 1, '러닝 시속 9.7킬로미터', null::integer, 1, 2100, 90),
        ('고급 하이브리드 컨디셔닝 루틴', 3, 1, '실내 자전거 고강도', null::integer, 1, 2100, 90),
        ('고급 하이브리드 컨디셔닝 루틴', 4, 1, '바벨 데드리프트', 5, 3, null::integer, 150),
        ('고급 하이브리드 컨디셔닝 루틴', 4, 2, '시티드 케이블 로우', 8, 3, null::integer, 90),
        ('고급 하이브리드 컨디셔닝 루틴', 5, 1, '회복 요가', null::integer, 1, 1200, 60),
        ('고급 하이브리드 컨디셔닝 루틴', 5, 2, '시티드 햄스트링 및 종아리 스트레칭', null::integer, 1, 300, 30),

        ('고급 파워 요가 필라테스 루틴', 1, 1, '파워 요가', null::integer, 1, 2100, 60),
        ('고급 파워 요가 필라테스 루틴', 2, 1, '고강도 필라테스', null::integer, 1, 2100, 60),
        ('고급 파워 요가 필라테스 루틴', 3, 1, '아쉬탕가 요가', null::integer, 1, 2100, 60),
        ('고급 파워 요가 필라테스 루틴', 4, 1, '월드 그레이티스트 스트레치', null::integer, 1, 300, 30),
        ('고급 파워 요가 필라테스 루틴', 4, 2, '상향 스트레치', null::integer, 1, 300, 30)
),
resolved_item_seed as (
    select
        r.id as routine_id,
        rs.id as routine_session_id,
        e.id as exercise_id,
        i.item_seq,
        i.reps,
        i.sets,
        i.duration_sec,
        i.rest_sec
    from item_seed i
    join routines r
        on r.is_default = true
       and r.name = i.routine_name
    join routine_sessions rs
        on rs.routine_id = r.id
       and rs.seq = i.session_seq
    join exercises e
        on e.name = i.exercise_name
)
insert into routine_items (
    routine_id,
    routine_session_id,
    exercise_id,
    seq,
    reps,
    sets,
    duration_sec,
    rest_sec
)
select
    routine_id,
    routine_session_id,
    exercise_id,
    item_seq,
    reps,
    sets,
    duration_sec,
    rest_sec
from resolved_item_seed
on conflict on constraint routine_items_routine_session_id_seq_key
do update
set exercise_id = excluded.exercise_id,
    reps = excluded.reps,
    sets = excluded.sets,
    duration_sec = excluded.duration_sec,
    rest_sec = excluded.rest_sec;

drop table if exists pg_temp.beginner_routine_ids;
drop table if exists pg_temp.beginner_session_ids;
drop table if exists pg_temp.beginner_item_rank;
drop table if exists pg_temp.beginner_session_item_counts;

create temp table beginner_routine_ids as
select id
from routines
where is_default = true
  and is_active = true
  and (
      target_experience_level = 'beginner'
      or difficulty = 'easy'
      or name like '초급%'
  );

create temp table beginner_session_ids as
select id
from routine_sessions
where routine_id in (select id from beginner_routine_ids);

create temp table beginner_item_rank as
select
    id,
    row_number() over (partition by routine_session_id order by seq, id) as rn
from routine_items
where routine_session_id in (select id from beginner_session_ids);

delete from routine_items ri
using beginner_item_rank bir
where ri.id = bir.id
  and bir.rn > 2;

update routine_items ri
set seq = bir.rn + 1000
from beginner_item_rank bir
where ri.id = bir.id
  and bir.rn <= 2;

update routine_items ri
set seq = ranked.rn
from (
    select
        id,
        row_number() over (partition by routine_session_id order by seq, id) as rn
    from routine_items
    where routine_session_id in (select id from beginner_session_ids)
) ranked
where ri.id = ranked.id;

create temp table beginner_session_item_counts as
select
    routine_session_id,
    count(*) as item_count
from routine_items
where routine_session_id in (select id from beginner_session_ids)
group by routine_session_id;

update routine_items ri
set duration_sec = case
    when counts.item_count <= 1 then 120
    else 60
end
from beginner_session_item_counts counts
where ri.routine_session_id = counts.routine_session_id;

update routine_sessions rs
set estimated_minutes = 2,
    updated_at = now()
where rs.id in (select id from beginner_session_ids);

update routines r
set estimated_minutes = 2,
    updated_at = now()
where r.id in (select id from beginner_routine_ids);

drop table if exists pg_temp.beginner_session_item_counts;
drop table if exists pg_temp.beginner_item_rank;
drop table if exists pg_temp.beginner_session_ids;
drop table if exists pg_temp.beginner_routine_ids;
