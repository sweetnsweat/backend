create table if not exists password_reset_tokens (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    token_hash varchar(128) not null unique,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null default now()
);

create index if not exists idx_password_reset_tokens_user_created_at
    on password_reset_tokens (user_id, created_at desc);

create index if not exists idx_password_reset_tokens_expires_at
    on password_reset_tokens (expires_at);
