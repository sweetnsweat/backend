drop index if exists user_quests_user_id_quest_date_key;

alter table user_quests
    drop constraint if exists user_quests_user_id_quest_date_key;

create index if not exists idx_user_quests_user_date
    on user_quests (user_id, quest_date);
