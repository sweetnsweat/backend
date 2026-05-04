create table if not exists scenario_genres (
    id bigserial primary key,
    scenario_id integer not null references scenarios(id) on delete cascade,
    genre_name varchar(100) not null,
    seq integer not null default 1,
    created_at timestamptz not null default now(),
    constraint scenario_genres_scenario_name_key unique (scenario_id, genre_name)
);

create index if not exists idx_scenario_genres_name
    on scenario_genres (genre_name);

create index if not exists idx_scenario_genres_scenario_seq
    on scenario_genres (scenario_id, seq);

insert into scenario_genres (scenario_id, genre_name, seq)
select
    s.id,
    token.genre_name,
    token.seq::integer
from scenarios s
cross join lateral regexp_split_to_table(coalesce(s.genre, ''), '\s+') with ordinality as token(genre_name, seq)
where btrim(token.genre_name) <> ''
on conflict (scenario_id, genre_name) do nothing;
