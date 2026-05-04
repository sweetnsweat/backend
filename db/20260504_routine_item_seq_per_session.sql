alter table routine_items
    drop constraint if exists routine_items_routine_id_seq_key;

drop index if exists ux_routine_items_routine_id_seq;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'routine_items_routine_session_id_seq_key'
    ) then
        alter table routine_items
            add constraint routine_items_routine_session_id_seq_key
            unique (routine_session_id, seq);
    end if;
end $$;
