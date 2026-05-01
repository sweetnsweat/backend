alter table user_quests
    add column if not exists quest_type varchar(30) not null default 'routine',
    add column if not exists target_metric varchar(30) not null default 'exercises',
    add column if not exists source_session_id bigint,
    add column if not exists condition_adjusted boolean not null default false,
    add column if not exists quest_context_json jsonb not null default '{}'::jsonb;

alter table user_quests
    drop constraint if exists user_quests_status_check;

alter table user_quests
    add constraint user_quests_status_check
        check (status in ('assigned', 'in_progress', 'issued', 'completed', 'failed', 'expired'));

alter table user_quests
    drop constraint if exists user_quests_quest_type_check;

alter table user_quests
    add constraint user_quests_quest_type_check
        check (quest_type in ('routine', 'off_day', 'recovery'));

alter table user_quests
    drop constraint if exists user_quests_target_metric_check;

alter table user_quests
    add constraint user_quests_target_metric_check
        check (target_metric in ('exercises', 'minutes', 'steps', 'calories', 'reps'));

alter table user_quests
    drop constraint if exists user_quests_source_session_id_fkey;

alter table user_quests
    add constraint user_quests_source_session_id_fkey
        foreign key (source_session_id) references routine_sessions(id) on delete set null;

create unique index if not exists user_quests_user_id_quest_date_key
    on user_quests (user_id, quest_date);

create index if not exists idx_user_quests_source_session_id
    on user_quests (source_session_id);
