create table if not exists user_favorite_exercises (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    exercise_id bigint not null references exercises(id) on delete cascade,
    created_at timestamptz not null default now(),
    constraint user_favorite_exercises_user_id_exercise_id_key unique (user_id, exercise_id)
);

create index if not exists idx_user_favorite_exercises_user_id
    on user_favorite_exercises (user_id);

create index if not exists idx_user_favorite_exercises_exercise_id
    on user_favorite_exercises (exercise_id);
