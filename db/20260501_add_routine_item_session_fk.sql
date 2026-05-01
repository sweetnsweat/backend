alter table routine_items
    add column if not exists routine_session_id bigint;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'routine_items_routine_session_id_fkey'
    ) then
        alter table routine_items
            add constraint routine_items_routine_session_id_fkey
            foreign key (routine_session_id)
            references routine_sessions(id)
            on delete set null;
    end if;
end $$;

create index if not exists idx_routine_items_routine_session_id
    on routine_items (routine_session_id);
