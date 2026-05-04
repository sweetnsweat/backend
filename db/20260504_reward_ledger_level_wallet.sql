alter table users
    add column if not exists total_exp integer not null default 0;

alter table users
    add column if not exists level integer not null default 1;

create table if not exists user_exp_logs (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    amount integer not null,
    before_total_exp integer not null,
    after_total_exp integer not null,
    before_level integer not null,
    after_level integer not null,
    ref_type varchar(50) not null,
    ref_id bigint not null,
    memo text,
    created_at timestamptz not null default now(),
    constraint user_exp_logs_user_ref_key unique (user_id, ref_type, ref_id)
);

create index if not exists idx_user_exp_logs_user_created_at
    on user_exp_logs (user_id, created_at desc);

create table if not exists wallets (
    user_id bigint primary key references users(id) on delete cascade,
    balance_currency integer not null default 0,
    updated_at timestamptz not null default now()
);

create table if not exists wallet_transactions (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    tx_type varchar(50) not null,
    amount integer not null,
    item_id bigint references items(id),
    ref_type varchar(50),
    ref_id bigint,
    memo text,
    created_at timestamptz not null default now()
);

create unique index if not exists wallet_transactions_user_tx_ref_key
    on wallet_transactions (user_id, tx_type, ref_type, ref_id)
    where ref_type is not null and ref_id is not null;

create index if not exists idx_wallet_transactions_user_created_at
    on wallet_transactions (user_id, created_at desc);

insert into wallets (user_id, balance_currency, updated_at)
select id, 0, now()
from users
on conflict (user_id) do nothing;

with completed_quests as (
    select
        id,
        user_id,
        greatest(coalesce(reward_exp, 0), 0) as reward_exp,
        coalesce(completed_at, created_at, now()) as occurred_at
    from user_quests
    where status = 'completed'
      and coalesce(reward_exp, 0) > 0
),
running_exp as (
    select
        id,
        user_id,
        reward_exp,
        occurred_at,
        sum(reward_exp) over (
            partition by user_id
            order by occurred_at asc, id asc
            rows between unbounded preceding and current row
        ) as after_total_exp
    from completed_quests
),
exp_rows as (
    select
        id,
        user_id,
        reward_exp,
        occurred_at,
        (after_total_exp - reward_exp)::integer as before_total_exp,
        after_total_exp::integer as after_total_exp
    from running_exp
)
insert into user_exp_logs (
    user_id,
    amount,
    before_total_exp,
    after_total_exp,
    before_level,
    after_level,
    ref_type,
    ref_id,
    memo,
    created_at
)
select
    user_id,
    reward_exp,
    before_total_exp,
    after_total_exp,
    greatest(1, floor((1 + sqrt(1 + (before_total_exp::numeric / 12.5))) / 2)::integer),
    greatest(1, floor((1 + sqrt(1 + (after_total_exp::numeric / 12.5))) / 2)::integer),
    'user_quest',
    id,
    '기존 완료 퀘스트 EXP 보상 이관',
    occurred_at
from exp_rows
on conflict (user_id, ref_type, ref_id) do nothing;

with user_totals as (
    select
        user_id,
        coalesce(sum(amount), 0)::integer as total_exp
    from user_exp_logs
    group by user_id
)
update users u
set
    total_exp = user_totals.total_exp,
    level = greatest(1, floor((1 + sqrt(1 + (user_totals.total_exp::numeric / 12.5))) / 2)::integer)
from user_totals
where u.id = user_totals.user_id;

insert into wallet_transactions (
    user_id,
    tx_type,
    amount,
    ref_type,
    ref_id,
    memo,
    created_at
)
select
    user_id,
    'quest_reward',
    greatest(coalesce(reward_currency, 0), 0),
    'user_quest',
    id,
    '기존 완료 퀘스트 골드 보상 이관',
    coalesce(completed_at, created_at, now())
from user_quests
where status = 'completed'
  and coalesce(reward_currency, 0) > 0
on conflict do nothing;

with wallet_totals as (
    select
        user_id,
        coalesce(sum(amount), 0)::integer as balance_currency
    from wallet_transactions
    group by user_id
)
update wallets w
set
    balance_currency = wallet_totals.balance_currency,
    updated_at = now()
from wallet_totals
where w.user_id = wallet_totals.user_id;
