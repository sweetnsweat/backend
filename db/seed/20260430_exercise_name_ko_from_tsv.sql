-- Bulk-apply Korean exercise names from a TSV copied into the PostgreSQL
-- container at /tmp/exercise_names_ko.tsv.
--
-- Expected TSV columns:
-- id, external_id, ko_name, original_name

begin;

create temp table exercise_name_ko_seed (
    id bigint,
    external_id text,
    ko_name text,
    original_name text
);

copy exercise_name_ko_seed
from '/tmp/exercise_names_ko.tsv'
with (format text, delimiter E'\t');

update exercises e
set name = left(s.ko_name, 100),
    raw_data = coalesce(e.raw_data, '{}'::jsonb) || jsonb_build_object(
        'nameKoAssignment',
        jsonb_build_object(
            'source', 'db_backups/exercise_names_ko_backup_20260426-233338_local.tsv',
            'assignedAt', '2026-04-30'
        )
    ),
    updated_at = now()
from exercise_name_ko_seed s
where e.external_id = s.external_id
  and e.name is distinct from left(s.ko_name, 100);

commit;
