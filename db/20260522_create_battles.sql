create table if not exists battles (
    id bigserial primary key,
    mode varchar(20) not null,
    status varchar(20) not null,
    period_start_date date not null,
    period_end_date date not null,
    starts_at timestamptz not null,
    ends_at timestamptz not null,
    finalized_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists battle_participants (
    id bigserial primary key,
    battle_id bigint not null references battles(id) on delete cascade,
    user_id bigint not null references users(id) on delete cascade,
    final_score integer,
    baseline_score integer not null default 0,
    baseline_completed_quest_count integer not null default 0,
    baseline_routine_quest_count integer not null default 0,
    baseline_exercise_minutes integer not null default 0,
    baseline_steps integer not null default 0,
    baseline_distance_meters integer not null default 0,
    baseline_active_calories integer not null default 0,
    baseline_health_verified_quest_count integer not null default 0,
    result varchar(20) not null default 'PENDING',
    joined_at timestamptz not null default now(),
    constraint battle_participants_battle_user_key unique (battle_id, user_id)
);

create index if not exists idx_battles_mode_period_status
    on battles (mode, period_start_date, period_end_date, status);

create index if not exists idx_battle_participants_user_id
    on battle_participants (user_id);
