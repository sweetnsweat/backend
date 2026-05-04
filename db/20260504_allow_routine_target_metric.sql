alter table user_quests
    drop constraint if exists user_quests_target_metric_check;

alter table user_quests
    add constraint user_quests_target_metric_check
        check (target_metric in ('routine', 'exercises', 'minutes', 'steps', 'calories', 'reps'));
