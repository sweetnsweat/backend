alter table routines
    add column if not exists source_routine_id bigint;

alter table routines
    drop constraint if exists routines_source_routine_id_fkey;

alter table routines
    add constraint routines_source_routine_id_fkey
        foreign key (source_routine_id) references routines(id) on delete set null;

create index if not exists idx_routines_source_routine_id
    on routines (source_routine_id);

create unique index if not exists routines_user_id_source_routine_id_key
    on routines (user_id, source_routine_id)
    where source_routine_id is not null;
