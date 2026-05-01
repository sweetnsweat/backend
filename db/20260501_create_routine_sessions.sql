create table if not exists routine_sessions (
    id bigserial primary key,
    routine_id bigint not null references routines(id) on delete cascade,
    day_of_week varchar(20) not null,
    session_name varchar(100) not null,
    session_type varchar(30),
    seq integer not null,
    estimated_minutes integer,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint routine_sessions_day_of_week_check
        check (day_of_week in ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    constraint routine_sessions_seq_check
        check (seq > 0),
    constraint routine_sessions_estimated_minutes_check
        check (estimated_minutes is null or estimated_minutes > 0)
);

create index if not exists idx_routine_sessions_routine_id
    on routine_sessions (routine_id);

create index if not exists idx_routine_sessions_routine_day
    on routine_sessions (routine_id, day_of_week);

create unique index if not exists ux_routine_sessions_routine_seq
    on routine_sessions (routine_id, seq);
