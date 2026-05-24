create table if not exists battle_match_queue (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    mode varchar(20) not null,
    period_start_date date not null,
    period_end_date date not null,
    status varchar(20) not null,
    matched_battle_id bigint references battles(id) on delete set null,
    queued_at timestamptz not null default now(),
    matched_at timestamptz,
    expires_at timestamptz not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_battle_match_queue_waiting
    on battle_match_queue (mode, period_start_date, period_end_date, status, queued_at);

create index if not exists idx_battle_match_queue_user_period_status
    on battle_match_queue (user_id, mode, period_start_date, period_end_date, status);
