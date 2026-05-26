begin;

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

commit;
