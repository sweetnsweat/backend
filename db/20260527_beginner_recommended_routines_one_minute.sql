begin;

create temp table beginner_recommended_routine_ids as
select id
from routines
where is_default = true
  and is_active = true
  and (
      target_experience_level = 'beginner'
      or difficulty = 'easy'
      or name like '초급%'
  );

update routines r
set estimated_minutes = 1,
    updated_at = now()
where r.id in (select id from beginner_recommended_routine_ids);

update routine_sessions rs
set estimated_minutes = 1,
    updated_at = now()
where rs.routine_id in (select id from beginner_recommended_routine_ids);

update routine_items ri
set duration_sec = 60
where ri.routine_id in (select id from beginner_recommended_routine_ids)
  and ri.duration_sec is not null;

commit;
