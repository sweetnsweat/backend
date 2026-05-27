begin;

drop table if exists pg_temp.beginner_routine_ids;
drop table if exists pg_temp.beginner_primary_sessions;
drop table if exists pg_temp.beginner_primary_items;

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

create temp table beginner_primary_sessions as
select distinct on (rs.routine_id)
    rs.routine_id,
    rs.id as session_id
from routine_sessions rs
join beginner_routine_ids br on br.id = rs.routine_id
order by rs.routine_id, rs.seq, rs.id;

create temp table beginner_primary_items as
select distinct on (ri.routine_id)
    ri.routine_id,
    ri.id as item_id
from routine_items ri
join beginner_routine_ids br on br.id = ri.routine_id
left join beginner_primary_sessions ps on ps.routine_id = ri.routine_id
order by
    ri.routine_id,
    case when ri.routine_session_id = ps.session_id then 0 else 1 end,
    ri.seq,
    ri.id;

delete from routine_items ri
using beginner_primary_items keep
where ri.routine_id = keep.routine_id
  and ri.id <> keep.item_id;

update routine_items ri
set routine_session_id = coalesce(ps.session_id, ri.routine_session_id),
    seq = 1,
    reps = null,
    sets = 1,
    duration_sec = 60,
    rest_sec = 0
from beginner_primary_items keep
left join beginner_primary_sessions ps on ps.routine_id = keep.routine_id
where ri.id = keep.item_id;

update routine_sessions rs
set seq = seq + 1000,
    estimated_minutes = 1,
    is_active = false,
    updated_at = now()
where rs.routine_id in (select id from beginner_routine_ids);

update routine_sessions rs
set seq = 1,
    estimated_minutes = 1,
    is_active = true,
    updated_at = now()
from beginner_primary_sessions ps
where rs.id = ps.session_id;

update routines r
set estimated_minutes = 1,
    updated_at = now()
where r.id in (select id from beginner_routine_ids);

drop table if exists pg_temp.beginner_primary_items;
drop table if exists pg_temp.beginner_primary_sessions;
drop table if exists pg_temp.beginner_routine_ids;

commit;
