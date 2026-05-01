create or replace function copy_default_routine_for_demo_user(target_user_id bigint, source_default_routine_id bigint)
returns bigint
language plpgsql
as $$
declare
    copied_routine_id bigint;
    source_session record;
    copied_session_id bigint;
begin
    select id
    into copied_routine_id
    from routines
    where user_id = target_user_id
      and source_routine_id = source_default_routine_id
      and is_active = true
    order by id
    limit 1;

    if copied_routine_id is not null then
        return copied_routine_id;
    end if;

    insert into routines (
        user_id,
        source_routine_id,
        name,
        description,
        is_default,
        difficulty,
        estimated_minutes,
        target_experience_level,
        target_current_exercise_statuses,
        goal_types,
        place_types,
        weekly_frequency,
        recommended_exercise_types,
        is_active,
        created_at,
        updated_at
    )
    select
        target_user_id,
        source_default_routine_id,
        name,
        description,
        false,
        difficulty,
        estimated_minutes,
        target_experience_level,
        target_current_exercise_statuses,
        goal_types,
        place_types,
        weekly_frequency,
        recommended_exercise_types,
        true,
        now(),
        now()
    from routines
    where id = source_default_routine_id
    returning id into copied_routine_id;

    for source_session in
        select *
        from routine_sessions
        where routine_id = source_default_routine_id
        order by seq
    loop
        insert into routine_sessions (
            routine_id,
            day_of_week,
            session_name,
            session_type,
            seq,
            estimated_minutes,
            is_active,
            created_at,
            updated_at
        )
        values (
            copied_routine_id,
            source_session.day_of_week,
            source_session.session_name,
            source_session.session_type,
            source_session.seq,
            source_session.estimated_minutes,
            source_session.is_active,
            now(),
            now()
        )
        returning id into copied_session_id;

        insert into routine_items (
            routine_id,
            routine_session_id,
            exercise_id,
            seq,
            reps,
            sets,
            duration_sec,
            rest_sec
        )
        select
            copied_routine_id,
            copied_session_id,
            exercise_id,
            seq,
            reps,
            sets,
            duration_sec,
            rest_sec
        from routine_items
        where routine_session_id = source_session.id
        order by seq;
    end loop;

    insert into routine_items (
        routine_id,
        routine_session_id,
        exercise_id,
        seq,
        reps,
        sets,
        duration_sec,
        rest_sec
    )
    select
        copied_routine_id,
        null,
        exercise_id,
        seq,
        reps,
        sets,
        duration_sec,
        rest_sec
    from routine_items
    where routine_id = source_default_routine_id
      and routine_session_id is null
    order by seq;

    return copied_routine_id;
end $$;

do $$
declare
    demo_password_hash text := '$2y$10$kXyyhE/sxVX2GMMMHQRPg..o6X/NczvxcCXpeSvMHSc/DlvjVKYnK';
    recommend_user_id bigint;
    gym_user_id bigint;
    recovery_user_id bigint;
    gym_source_routine_id bigint;
    recovery_source_routine_id bigint;
    gym_active_routine_id bigint;
    recovery_active_routine_id bigint;
begin
    select coalesce(
        (select password_hash from users where login_id = 'demoUser' and password_hash is not null limit 1),
        '$2a$10$WrF.ykoxA4WF.KGkw5k.J.OaRbFoc8sBHwzqooBDvmdap2ibPsrKy'
    )
    into demo_password_hash;

    insert into users (
        email, login_id, password_hash, nickname, gender, birth_date, height_cm, weight_kg,
        experience_level, current_exercise_status, fitness_goal, preferred_workout_place,
        weekly_workout_frequency, available_workout_minutes, preferred_exercise_types,
        push_enabled, push_quest_enabled, push_routine_enabled, push_competition_enabled,
        status, created_at, updated_at
    )
    values (
        'demo-recommend@sweetnsweat.local', 'demo_recommend', demo_password_hash, '데모 추천 사용자',
        'female', date '2002-05-20', 164.5, 58.2,
        'beginner', 'none', 'habit', 'home',
        3, 30, '["bodyweight", "cardio"]'::jsonb,
        true, true, true, true, 'active', now(), now()
    )
    on conflict (login_id) do update set
        email = excluded.email,
        password_hash = excluded.password_hash,
        gender = excluded.gender,
        birth_date = excluded.birth_date,
        height_cm = excluded.height_cm,
        weight_kg = excluded.weight_kg,
        experience_level = excluded.experience_level,
        current_exercise_status = excluded.current_exercise_status,
        fitness_goal = excluded.fitness_goal,
        preferred_workout_place = excluded.preferred_workout_place,
        weekly_workout_frequency = excluded.weekly_workout_frequency,
        available_workout_minutes = excluded.available_workout_minutes,
        preferred_exercise_types = excluded.preferred_exercise_types,
        updated_at = now()
    returning id into recommend_user_id;

    insert into users (
        email, login_id, password_hash, nickname, gender, birth_date, height_cm, weight_kg,
        experience_level, current_exercise_status, fitness_goal, preferred_workout_place,
        weekly_workout_frequency, available_workout_minutes, preferred_exercise_types,
        push_enabled, push_quest_enabled, push_routine_enabled, push_competition_enabled,
        status, created_at, updated_at
    )
    values (
        'demo-gym@sweetnsweat.local', 'demo_gym_quest', demo_password_hash, '데모 헬스장 퀘스트',
        'male', date '2000-03-12', 176.0, 74.0,
        'beginner', 'occasional', 'strength', 'gym',
        3, 35, '["strength", "cardio"]'::jsonb,
        true, true, true, true, 'active', now(), now()
    )
    on conflict (login_id) do update set
        email = excluded.email,
        password_hash = excluded.password_hash,
        gender = excluded.gender,
        birth_date = excluded.birth_date,
        height_cm = excluded.height_cm,
        weight_kg = excluded.weight_kg,
        experience_level = excluded.experience_level,
        current_exercise_status = excluded.current_exercise_status,
        fitness_goal = excluded.fitness_goal,
        preferred_workout_place = excluded.preferred_workout_place,
        weekly_workout_frequency = excluded.weekly_workout_frequency,
        available_workout_minutes = excluded.available_workout_minutes,
        preferred_exercise_types = excluded.preferred_exercise_types,
        updated_at = now()
    returning id into gym_user_id;

    insert into users (
        email, login_id, password_hash, nickname, gender, birth_date, height_cm, weight_kg,
        experience_level, current_exercise_status, fitness_goal, preferred_workout_place,
        weekly_workout_frequency, available_workout_minutes, preferred_exercise_types,
        push_enabled, push_quest_enabled, push_routine_enabled, push_competition_enabled,
        status, created_at, updated_at
    )
    values (
        'demo-recovery@sweetnsweat.local', 'demo_recovery_quest', demo_password_hash, '데모 회복 퀘스트',
        'prefer_not_to_say', date '2001-09-03', 168.0, 65.0,
        'beginner', 'none', 'stamina', 'home',
        3, 25, '["bodyweight", "mobility"]'::jsonb,
        true, true, true, true, 'active', now(), now()
    )
    on conflict (login_id) do update set
        email = excluded.email,
        password_hash = excluded.password_hash,
        gender = excluded.gender,
        birth_date = excluded.birth_date,
        height_cm = excluded.height_cm,
        weight_kg = excluded.weight_kg,
        experience_level = excluded.experience_level,
        current_exercise_status = excluded.current_exercise_status,
        fitness_goal = excluded.fitness_goal,
        preferred_workout_place = excluded.preferred_workout_place,
        weekly_workout_frequency = excluded.weekly_workout_frequency,
        available_workout_minutes = excluded.available_workout_minutes,
        preferred_exercise_types = excluded.preferred_exercise_types,
        updated_at = now()
    returning id into recovery_user_id;

    select id into gym_source_routine_id
    from routines
    where is_default = true and name = '초급 헬스장 근력 입문 루틴'
    order by id
    limit 1;

    select id into recovery_source_routine_id
    from routines
    where is_default = true and name = '초급 전신 루틴'
    order by id
    limit 1;

    if gym_source_routine_id is null or recovery_source_routine_id is null then
        raise exception '기본 루틴 seed가 필요합니다.';
    end if;

    gym_active_routine_id := copy_default_routine_for_demo_user(gym_user_id, gym_source_routine_id);
    recovery_active_routine_id := copy_default_routine_for_demo_user(recovery_user_id, recovery_source_routine_id);

    update users
    set active_routine_id = gym_active_routine_id,
        updated_at = now()
    where id = gym_user_id;

    update users
    set active_routine_id = recovery_active_routine_id,
        updated_at = now()
    where id = recovery_user_id;

    insert into condition_logs (
        user_id, log_date, condition_level, sleep_score, stress_score, fatigue_score,
        energy_level, condition_score, exercise_multiplier, fatigue, created_at, updated_at
    )
    values
        (gym_user_id, current_date, 4, 3, 2, 2, 4, 72.92, 1.00, 2, now(), now()),
        (recovery_user_id, current_date, 1, 1, 5, 5, 1, 0.00, 0.70, 5, now(), now())
    on conflict (user_id, log_date) do update set
        condition_level = excluded.condition_level,
        sleep_score = excluded.sleep_score,
        stress_score = excluded.stress_score,
        fatigue_score = excluded.fatigue_score,
        energy_level = excluded.energy_level,
        condition_score = excluded.condition_score,
        exercise_multiplier = excluded.exercise_multiplier,
        fatigue = excluded.fatigue,
        updated_at = now();

    delete from user_quests
    where user_id in (gym_user_id, recovery_user_id)
      and quest_date = current_date;

    insert into user_favorite_exercises (user_id, exercise_id, created_at)
    select gym_user_id, ri.exercise_id, now()
    from routine_items ri
    where ri.routine_id = gym_active_routine_id
    order by ri.seq
    limit 2
    on conflict (user_id, exercise_id) do nothing;
end $$;
