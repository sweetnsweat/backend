create table if not exists health_daily_summaries (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    summary_date date not null,
    steps integer not null default 0,
    distance_meters integer not null default 0,
    active_calories_kcal integer not null default 0,
    exercise_minutes integer not null default 0,
    sample_count integer not null default 0,
    synced_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint health_daily_summaries_user_date_key unique (user_id, summary_date)
);

create index if not exists idx_health_daily_summaries_user_date
    on health_daily_summaries (user_id, summary_date);
