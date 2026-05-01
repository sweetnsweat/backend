with session_seed (routine_name, seq, day_of_week, session_name, session_type, estimated_minutes) as (
    values
        ('초급 전신 루틴', 1, 'MONDAY', '전신 A', 'full_body', 25),
        ('초급 전신 루틴', 2, 'WEDNESDAY', '전신 B', 'full_body', 25),
        ('초급 전신 루틴', 3, 'FRIDAY', '코어 회복', 'core_recovery', 20),

        ('회복 스트레칭 루틴', 1, 'TUESDAY', '상체 이완', 'recovery', 18),
        ('회복 스트레칭 루틴', 2, 'THURSDAY', '하체 이완', 'recovery', 18),
        ('회복 스트레칭 루틴', 3, 'SATURDAY', '전신 가동성', 'mobility', 18),

        ('유산소 스타터 루틴', 1, 'MONDAY', '걷기 적응', 'cardio', 20),
        ('유산소 스타터 루틴', 2, 'WEDNESDAY', '걷기/조깅 반복', 'cardio', 20),
        ('유산소 스타터 루틴', 3, 'FRIDAY', '가벼운 지속 유산소', 'cardio', 20),

        ('초급 헬스장 근력 입문 루틴', 1, 'MONDAY', '상체 머신', 'upper_body', 30),
        ('초급 헬스장 근력 입문 루틴', 2, 'WEDNESDAY', '하체/후면', 'lower_body', 30),
        ('초급 헬스장 근력 입문 루틴', 3, 'FRIDAY', '유산소/코어', 'cardio_core', 25),

        ('초급 야외 걷기 루틴', 1, 'TUESDAY', '걷기 준비', 'mobility', 20),
        ('초급 야외 걷기 루틴', 2, 'THURSDAY', '야외 걷기', 'cardio', 25),
        ('초급 야외 걷기 루틴', 3, 'SATURDAY', '긴 걷기/회복', 'cardio_recovery', 25)
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

insert into routine_items (routine_id, routine_session_id, exercise_id, seq, reps, sets, duration_sec, rest_sec)
select r.id, rs.id, e.id, 3, null::integer, 1, 600, 60
from routines r
join routine_sessions rs
    on rs.routine_id = r.id
   and rs.seq = 3
join exercises e
    on e.name = '걷기, 런닝머신'
where r.is_default = true
  and r.name = '유산소 스타터 루틴'
  and not exists (
      select 1
      from routine_items ri
      where ri.routine_id = r.id
        and ri.seq = 3
  );

with item_session_seed (routine_name, item_seq, session_seq) as (
    values
        ('초급 전신 루틴', 1, 1),
        ('초급 전신 루틴', 2, 1),
        ('초급 전신 루틴', 3, 2),
        ('초급 전신 루틴', 4, 3),
        ('초급 전신 루틴', 5, 3),

        ('회복 스트레칭 루틴', 1, 1),
        ('회복 스트레칭 루틴', 2, 1),
        ('회복 스트레칭 루틴', 3, 2),
        ('회복 스트레칭 루틴', 4, 2),
        ('회복 스트레칭 루틴', 5, 3),

        ('유산소 스타터 루틴', 1, 1),
        ('유산소 스타터 루틴', 2, 2),

        ('초급 헬스장 근력 입문 루틴', 1, 3),
        ('초급 헬스장 근력 입문 루틴', 2, 1),
        ('초급 헬스장 근력 입문 루틴', 3, 1),
        ('초급 헬스장 근력 입문 루틴', 4, 2),
        ('초급 헬스장 근력 입문 루틴', 5, 3),

        ('초급 야외 걷기 루틴', 1, 1),
        ('초급 야외 걷기 루틴', 2, 2),
        ('초급 야외 걷기 루틴', 3, 2),
        ('초급 야외 걷기 루틴', 4, 3)
)
update routine_items ri
set routine_session_id = rs.id
from item_session_seed s
join routines r
    on r.is_default = true
   and r.name = s.routine_name
join routine_sessions rs
    on rs.routine_id = r.id
   and rs.seq = s.session_seq
where ri.routine_id = r.id
  and ri.seq = s.item_seq
  and (
      ri.routine_session_id is null
      or ri.routine_session_id <> rs.id
  );
