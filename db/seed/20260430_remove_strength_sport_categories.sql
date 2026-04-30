-- Remove exercise categories that are out of scope for the current product:
-- 플라이오메트릭, 파워리프팅, 올림픽 역도, 스트롱맨.
--
-- Some default routine items may reference these exercises, so delete those
-- routine item rows first and resequence remaining items per routine.

begin;

with target_exercises as (
    select id
    from exercises
    where category in ('플라이오메트릭', '파워리프팅', '올림픽 역도', '스트롱맨')
)
delete from routine_items ri
using target_exercises te
where ri.exercise_id = te.id;

delete from exercises
where category in ('플라이오메트릭', '파워리프팅', '올림픽 역도', '스트롱맨');

with resequenced as (
    select
        id,
        row_number() over (partition by routine_id order by seq, id) as next_seq
    from routine_items
)
update routine_items ri
set seq = resequenced.next_seq
from resequenced
where ri.id = resequenced.id
  and ri.seq is distinct from resequenced.next_seq;

commit;
