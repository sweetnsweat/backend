alter table users
    add column if not exists current_exercise_status varchar(20),
    add column if not exists fitness_goal varchar(30),
    add column if not exists preferred_workout_place varchar(20),
    add column if not exists weekly_workout_frequency integer,
    add column if not exists available_workout_minutes integer;
