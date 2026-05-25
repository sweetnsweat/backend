alter table battle_participants
    add column if not exists baseline_score integer not null default 0,
    add column if not exists baseline_completed_quest_count integer not null default 0,
    add column if not exists baseline_routine_quest_count integer not null default 0,
    add column if not exists baseline_exercise_minutes integer not null default 0,
    add column if not exists baseline_steps integer not null default 0,
    add column if not exists baseline_distance_meters integer not null default 0,
    add column if not exists baseline_active_calories integer not null default 0,
    add column if not exists baseline_health_verified_quest_count integer not null default 0;
