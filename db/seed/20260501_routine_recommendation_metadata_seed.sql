alter table routines
    add column if not exists target_experience_level varchar(20),
    add column if not exists target_current_exercise_statuses jsonb not null default '[]'::jsonb,
    add column if not exists goal_types jsonb not null default '[]'::jsonb,
    add column if not exists place_types jsonb not null default '[]'::jsonb,
    add column if not exists weekly_frequency integer,
    add column if not exists recommended_exercise_types jsonb not null default '[]'::jsonb;

update routines
set target_experience_level = 'beginner',
    target_current_exercise_statuses = '["none", "occasional"]'::jsonb,
    goal_types = '["habit", "stamina", "strength"]'::jsonb,
    place_types = '["home", "other"]'::jsonb,
    weekly_frequency = 3,
    recommended_exercise_types = '["bodyweight", "strength"]'::jsonb,
    updated_at = now()
where is_default = true
  and name = '초급 전신 루틴';

update routines
set target_experience_level = 'beginner',
    target_current_exercise_statuses = '["none", "occasional", "regular"]'::jsonb,
    goal_types = '["stress_relief", "habit"]'::jsonb,
    place_types = '["home", "gym", "facility", "other"]'::jsonb,
    weekly_frequency = 2,
    recommended_exercise_types = '["stretching", "yoga_pilates"]'::jsonb,
    updated_at = now()
where is_default = true
  and name = '회복 스트레칭 루틴';

update routines
set target_experience_level = 'beginner',
    target_current_exercise_statuses = '["none", "occasional"]'::jsonb,
    goal_types = '["stamina", "weight_loss", "habit"]'::jsonb,
    place_types = '["gym"]'::jsonb,
    weekly_frequency = 3,
    recommended_exercise_types = '["cardio", "walking", "running"]'::jsonb,
    updated_at = now()
where is_default = true
  and name = '유산소 스타터 루틴';

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
    '초급 헬스장 근력 입문 루틴',
    '헬스장에서 머신과 가벼운 유산소로 시작하는 초급 근력 루틴입니다.',
    true,
    'easy',
    30,
    'beginner',
    '["none", "occasional"]'::jsonb,
    '["strength", "stamina", "habit"]'::jsonb,
    '["gym"]'::jsonb,
    3,
    '["strength", "cardio"]'::jsonb,
    true,
    now(),
    now()
where not exists (
    select 1 from routines
    where is_default = true
      and name = '초급 헬스장 근력 입문 루틴'
);

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
    '초급 야외 걷기 루틴',
    '야외에서 걷기와 가벼운 조깅으로 운동 습관을 만드는 초급 유산소 루틴입니다.',
    true,
    'easy',
    25,
    'beginner',
    '["none", "occasional"]'::jsonb,
    '["stamina", "weight_loss", "habit", "stress_relief"]'::jsonb,
    '["outdoor", "other"]'::jsonb,
    3,
    '["walking", "running", "cardio"]'::jsonb,
    true,
    now(),
    now()
where not exists (
    select 1 from routines
    where is_default = true
      and name = '초급 야외 걷기 루틴'
);

insert into routine_items (routine_id, exercise_id, seq, reps, sets, duration_sec, rest_sec)
select r.id, e.id, v.seq, v.reps, v.sets, v.duration_sec, v.rest_sec
from routines r
join (
    values
        (1, '걷기, 런닝머신', null::integer, 1, 300, 30),
        (2, '나비', 12, 2, null::integer, 45),
        (3, '인클라인 체스트 프레스 활용', 10, 2, null::integer, 45),
        (4, '데드리프트 활용', 10, 2, null::integer, 45),
        (5, '판자', null::integer, 2, 30, 45)
) as v(seq, exercise_name, reps, sets, duration_sec, rest_sec) on true
join exercises e on e.name = v.exercise_name
where r.is_default = true
  and r.name = '초급 헬스장 근력 입문 루틴'
  and not exists (
      select 1 from routine_items ri
      where ri.routine_id = r.id
        and ri.seq = v.seq
  );

insert into routine_items (routine_id, exercise_id, seq, reps, sets, duration_sec, rest_sec)
select r.id, e.id, v.seq, v.reps, v.sets, v.duration_sec, v.rest_sec
from routines r
join (
    values
        (1, '하타 요가', null::integer, 1, 180, 30),
        (2, '트레일 러닝/워킹', null::integer, 1, 900, 60),
        (3, '걷기, 런닝머신', null::integer, 1, 300, 60),
        (4, '회복 요가', null::integer, 1, 180, 30)
) as v(seq, exercise_name, reps, sets, duration_sec, rest_sec) on true
join exercises e on e.name = v.exercise_name
where r.is_default = true
  and r.name = '초급 야외 걷기 루틴'
  and not exists (
      select 1 from routine_items ri
      where ri.routine_id = r.id
        and ri.seq = v.seq
  );
