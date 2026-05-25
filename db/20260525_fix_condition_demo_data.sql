-- Rebuild recent development condition logs so stored values match ConditionService.
-- Formula:
-- conditionScore = avg(
--   conditionLevel normalized from 1-5,
--   sleepScore normalized from 1-4,
--   stressScore inverse-normalized from 1-5,
--   energyLevel normalized from 1-5
-- )

begin;

alter table condition_logs
    drop constraint if exists condition_logs_sleep_score_check;

alter table condition_logs
    drop constraint if exists condition_logs_exercise_multiplier_check;

with target_days as (
    select
        gs::date as log_date,
        (current_date - gs::date)::integer as days_ago
    from generate_series(current_date - interval '44 days', current_date, interval '1 day') as gs
),
target_users as (
    select id as user_id
    from users
    where status = 'active'
),
base as (
    select
        u.user_id,
        d.log_date,
        least(
            96,
            greatest(
                28,
                40 + ((u.user_id * 17 + d.days_ago * 11) % 60) - (d.days_ago / 5)
            )
        ) as readiness
    from target_users u
    cross join target_days d
),
inputs as (
    select
        user_id,
        log_date,
        case
            when readiness < 38 then 2
            when readiness < 55 then 3
            when readiness < 78 then 4
            else 5
        end as condition_level,
        case
            when readiness < 38 then 2
            when readiness < 78 then 3
            else 4
        end as sleep_score,
        case
            when readiness < 38 then 5
            when readiness < 55 then 4
            when readiness < 78 then 3
            when readiness < 90 then 2
            else 1
        end as stress_score,
        case
            when readiness < 38 then 2
            when readiness < 55 then 3
            when readiness < 78 then 4
            else 5
        end as energy_level
    from base
),
calculated as (
    select
        user_id,
        log_date,
        condition_level,
        sleep_score,
        stress_score,
        energy_level,
        6 - energy_level as fatigue_score,
        round((
            ((condition_level - 1) * 100.0 / 4)
            + ((sleep_score - 1) * 100.0 / 3)
            + ((5 - stress_score) * 100.0 / 4)
            + ((energy_level - 1) * 100.0 / 4)
        )::numeric / 4, 2) as condition_score
    from inputs
),
seed as (
    select
        user_id,
        log_date,
        condition_level,
        sleep_score,
        stress_score,
        fatigue_score,
        energy_level,
        condition_score,
        case
            when condition_score < 40 then 0.70::numeric(4,2)
            when condition_score < 60 then 0.85::numeric(4,2)
            when condition_score < 80 then 1.00::numeric(4,2)
            else 1.10::numeric(4,2)
        end as exercise_multiplier
    from calculated
)
insert into condition_logs (
    user_id,
    log_date,
    condition_level,
    sleep_score,
    stress_score,
    fatigue_score,
    energy_level,
    condition_score,
    exercise_multiplier,
    fatigue,
    created_at,
    updated_at
)
select
    user_id,
    log_date,
    condition_level,
    sleep_score,
    stress_score,
    fatigue_score,
    energy_level,
    condition_score,
    exercise_multiplier,
    fatigue_score,
    now(),
    now()
from seed
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

alter table condition_logs
    add constraint condition_logs_sleep_score_check
    check (sleep_score >= 1 and sleep_score <= 4);

alter table condition_logs
    add constraint condition_logs_exercise_multiplier_check
    check (exercise_multiplier = any (array[0.70, 0.85, 1.00, 1.10]));

commit;
