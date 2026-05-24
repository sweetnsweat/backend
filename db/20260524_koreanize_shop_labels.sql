update items
set name = '경험치 2배권',
    metadata = jsonb_set(metadata, '{effect}', to_jsonb('경험치 2배 · 24시간'::text), true)
where name = 'EXP 2배권'
   or metadata ->> 'effect' = 'EXP x2 · 24시간';
