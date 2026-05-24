create table if not exists user_item_effects (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    item_id bigint not null references items(id),
    effect_type varchar(50) not null,
    status varchar(20) not null default 'ACTIVE',
    starts_at timestamptz not null default now(),
    expires_at timestamptz,
    used_at timestamptz,
    ref_type varchar(50),
    ref_id bigint,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint user_item_effects_effect_type_check
        check (effect_type in ('EXP_BOOST', 'RECORD_SHIELD', 'WIN_RATE_SHIELD', 'BATTLE_RETRY')),
    constraint user_item_effects_status_check
        check (status in ('ACTIVE', 'USED', 'EXPIRED'))
);

create index if not exists idx_user_item_effects_active
    on user_item_effects (user_id, effect_type, status, starts_at, expires_at);

create index if not exists idx_user_item_effects_ref
    on user_item_effects (ref_type, ref_id);
