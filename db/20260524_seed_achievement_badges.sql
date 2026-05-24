insert into items (item_type, name, description, price_currency, is_sellable, image_url, metadata, is_active)
select 'pvp_badge', '첫 퀘스트 완료', '퀘스트를 처음 완료하면 자동 지급되는 배지입니다.', 0, false,
       '/media/assets/badges/first_quest_complete.png',
       '{"kind":"achievement_badge","badgeCode":"FIRST_QUEST_COMPLETE","criteria":"퀘스트 1회 완료","sortOrder":10}'::jsonb,
       true
where not exists (select 1 from items where metadata ->> 'badgeCode' = 'FIRST_QUEST_COMPLETE');

insert into items (item_type, name, description, price_currency, is_sellable, image_url, metadata, is_active)
select 'pvp_badge', '검증 완료', '건강 데이터로 검증된 퀘스트를 처음 완료하면 자동 지급되는 배지입니다.', 0, false,
       '/media/assets/badges/verified_quest_complete.png',
       '{"kind":"achievement_badge","badgeCode":"VERIFIED_QUEST_COMPLETE","criteria":"건강 데이터 검증 퀘스트 1회 완료","sortOrder":20}'::jsonb,
       true
where not exists (select 1 from items where metadata ->> 'badgeCode' = 'VERIFIED_QUEST_COMPLETE');

insert into items (item_type, name, description, price_currency, is_sellable, image_url, metadata, is_active)
select 'pvp_badge', '3일 연속 달성', '퀘스트를 3일 연속 완료하면 자동 지급되는 배지입니다.', 0, false,
       '/media/assets/badges/quest_streak_3.png',
       '{"kind":"achievement_badge","badgeCode":"QUEST_STREAK_3","criteria":"퀘스트 3일 연속 완료","sortOrder":30}'::jsonb,
       true
where not exists (select 1 from items where metadata ->> 'badgeCode' = 'QUEST_STREAK_3');

insert into items (item_type, name, description, price_currency, is_sellable, image_url, metadata, is_active)
select 'pvp_badge', '퀘스트 루키', '퀘스트를 누적 10회 완료하면 자동 지급되는 배지입니다.', 0, false,
       '/media/assets/badges/quest_10_complete.png',
       '{"kind":"achievement_badge","badgeCode":"QUEST_10_COMPLETE","criteria":"퀘스트 누적 10회 완료","sortOrder":40}'::jsonb,
       true
where not exists (select 1 from items where metadata ->> 'badgeCode' = 'QUEST_10_COMPLETE');

insert into items (item_type, name, description, price_currency, is_sellable, image_url, metadata, is_active)
select 'pvp_badge', '첫 배틀 참가', '배틀 결과가 처음 확정되면 자동 지급되는 배지입니다.', 0, false,
       '/media/assets/badges/first_battle_join.png',
       '{"kind":"achievement_badge","badgeCode":"FIRST_BATTLE_JOIN","criteria":"배틀 1회 참가 및 결과 확정","sortOrder":50}'::jsonb,
       true
where not exists (select 1 from items where metadata ->> 'badgeCode' = 'FIRST_BATTLE_JOIN');

insert into items (item_type, name, description, price_currency, is_sellable, image_url, metadata, is_active)
select 'pvp_badge', '첫 승리', '배틀에서 처음 승리하면 자동 지급되는 배지입니다.', 0, false,
       '/media/assets/badges/first_battle_win.png',
       '{"kind":"achievement_badge","badgeCode":"FIRST_BATTLE_WIN","criteria":"배틀 1회 승리","sortOrder":60}'::jsonb,
       true
where not exists (select 1 from items where metadata ->> 'badgeCode' = 'FIRST_BATTLE_WIN');

insert into items (item_type, name, description, price_currency, is_sellable, image_url, metadata, is_active)
select 'pvp_badge', '1000점 돌파', '배틀 정산 점수 1000점 이상을 기록하면 자동 지급되는 배지입니다.', 0, false,
       '/media/assets/badges/battle_score_1000.png',
       '{"kind":"achievement_badge","badgeCode":"BATTLE_SCORE_1000","criteria":"배틀 정산 점수 1000점 이상","sortOrder":70}'::jsonb,
       true
where not exists (select 1 from items where metadata ->> 'badgeCode' = 'BATTLE_SCORE_1000');
