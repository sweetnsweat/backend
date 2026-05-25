update items
set is_active = false,
    is_sellable = false
where name in (
    '블루 트레이닝 스킨',
    '골드 프로필 프레임',
    '스토리 재도전 티켓',
    '회복 응원 배지',
    '기록 방어권',
    '승률하락 방어권',
    '배틀 부활권',
    '이수연',
    '칼라일',
    '제로스',
    '하나엘',
    '드라켄',
    '마린',
    '루나르',
    '카일린',
    '세라피나',
    '아스트라',
    '리온',
    '네오'
);

update items item
set is_active = false,
    is_sellable = false
where item.item_type in ('skin', 'profile')
  and not exists (
      select 1
      from character_profiles character_profile
      where btrim(character_profile.name) = item.name
        and nullif(btrim(character_profile.image_url), '') is not null
  );

with story_characters as (
    select distinct on (btrim(character_profile.name))
        character_profile.id as character_profile_id,
        character_profile.scenario_id,
        btrim(character_profile.name) as name,
        nullif(btrim(character_profile.character_title), '') as character_title,
        nullif(btrim(character_profile.character_type), '') as character_type,
        nullif(btrim(character_profile.image_url), '') as image_url,
        coalesce(character_profile.is_representative, false) as representative
    from character_profiles character_profile
    where nullif(btrim(character_profile.name), '') is not null
      and nullif(btrim(character_profile.image_url), '') is not null
    order by btrim(character_profile.name),
             coalesce(character_profile.is_representative, false) desc,
             character_profile.scenario_id,
             character_profile.id
),
updated_story_items as (
    update items item
    set item_type = 'skin',
        description = coalesce(nullif(concat_ws(' · ', story_characters.character_title, story_characters.character_type), ''), '스토리 캐릭터입니다.'),
        price_currency = 300,
        is_sellable = true,
        image_url = story_characters.image_url,
        metadata = coalesce(item.metadata, '{}'::jsonb)
            || jsonb_build_object(
                'storyCharacter', true,
                'scenarioId', story_characters.scenario_id,
                'characterProfileId', story_characters.character_profile_id,
                'characterTitle', story_characters.character_title,
                'characterType', story_characters.character_type,
                'effect', '스토리 캐릭터',
                'special', story_characters.representative
            ),
        is_active = true
    from story_characters
    where item.item_type in ('skin', 'profile')
      and item.name = story_characters.name
    returning item.name
)
insert into items (item_type, name, description, price_currency, is_sellable, image_url, metadata, is_active)
select 'skin',
       story_characters.name,
       coalesce(nullif(concat_ws(' · ', story_characters.character_title, story_characters.character_type), ''), '스토리 캐릭터입니다.'),
       300,
       true,
       story_characters.image_url,
       jsonb_build_object(
           'storyCharacter', true,
           'scenarioId', story_characters.scenario_id,
           'characterProfileId', story_characters.character_profile_id,
           'characterTitle', story_characters.character_title,
           'characterType', story_characters.character_type,
           'effect', '스토리 캐릭터',
           'special', story_characters.representative
       ),
       true
from story_characters
where not exists (
    select 1
    from updated_story_items updated_item
    where updated_item.name = story_characters.name
)
  and not exists (
      select 1
      from items item
      where item.item_type = 'skin'
        and item.name = story_characters.name
  );

insert into items (item_type, name, description, price_currency, is_sellable, image_url, metadata, is_active)
select item_type, name, description, price_currency, is_sellable, image_url, metadata::jsonb, true
from (values
    ('ticket', '경험치 2배권', '24시간 동안 경험치가 2배로 쌓여요', 200, true, '/media/assets/item_exp_boost.png', '{"special":false,"effect":"경험치 2배 · 24시간","bg":["#fefce8","#fef9c3"]}'),
    ('ticket', '퀘스트 스킵권', '오늘의 퀘스트를 건너뛸 수 있어요', 150, true, '/media/assets/item_quest_skip.png', '{"special":false,"effect":"퀘스트 스킵 · 1회","bg":["#ede9fe","#f3e8ff"]}')
) as seed(item_type, name, description, price_currency, is_sellable, image_url, metadata)
where not exists (select 1 from items where items.name = seed.name);

update items item
set item_type = seed.item_type,
    description = seed.description,
    price_currency = seed.price_currency,
    is_sellable = seed.is_sellable,
    image_url = seed.image_url,
    metadata = seed.metadata::jsonb,
    is_active = true
from (values
    ('ticket', '경험치 2배권', '24시간 동안 경험치가 2배로 쌓여요', 200, true, '/media/assets/item_exp_boost.png', '{"special":false,"effect":"경험치 2배 · 24시간","bg":["#fefce8","#fef9c3"]}'),
    ('ticket', '퀘스트 스킵권', '오늘의 퀘스트를 건너뛸 수 있어요', 150, true, '/media/assets/item_quest_skip.png', '{"special":false,"effect":"퀘스트 스킵 · 1회","bg":["#ede9fe","#f3e8ff"]}')
) as seed(item_type, name, description, price_currency, is_sellable, image_url, metadata)
where item.name = seed.name;
