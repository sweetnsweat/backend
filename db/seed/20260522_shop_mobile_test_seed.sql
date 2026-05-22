insert into items (item_type, name, description, price_currency, is_sellable, image_url, metadata, is_active)
select item_type, name, description, price_currency, is_sellable, image_url, metadata::jsonb, true
from (values
    ('skin', '이수연', '체대 입시생 · 인내력', 0, true, 'https://i.imgur.com/v0njcuh.png', '{"special":false,"bg":["#fce7f3","#ffe4e6"],"effect":"기본 캐릭터"}'),
    ('skin', '칼라일', '불의 전사 · 투지', 300, true, 'https://i.imgur.com/83q0Fz8.jpeg', '{"special":false,"bg":["#ffedd5","#fef3c7"],"effect":"캐릭터 스킨"}'),
    ('skin', '제로스', '번개 추적자 · 민첩', 500, true, '', '{"special":false,"bg":["#fef9c3","#ecfccb"],"effect":"캐릭터 스킨"}'),
    ('skin', '하나엘', '꽃의 정령 · 평온', 400, true, '', '{"special":false,"bg":["#fce7f3","#fdf4ff"],"effect":"캐릭터 스킨"}'),
    ('skin', '드라켄', '용기사 · 지배력', 800, true, '', '{"special":true,"bg":["#d1fae5","#ccfbf1"],"effect":"스페셜 캐릭터"}'),
    ('skin', '마린', '파도 항해사 · 자유', 450, true, '', '{"special":false,"bg":["#e0f2fe","#dbeafe"],"effect":"캐릭터 스킨"}'),
    ('skin', '루나르', '달의 암살자 · 집중력', 600, true, '', '{"special":false,"bg":["#ede9fe","#f3e8ff"],"effect":"캐릭터 스킨"}'),
    ('skin', '카일린', '여우 도적 · 기민함', 350, true, '', '{"special":false,"bg":["#ffedd5","#fee2e2"],"effect":"캐릭터 스킨"}'),
    ('skin', '세라피나', '왕국의 여왕 · 권위', 1200, true, '', '{"special":true,"bg":["#fef9c3","#fef3c7"],"effect":"스페셜 캐릭터"}'),
    ('skin', '아스트라', '별의 여행자 · 희망', 550, true, '', '{"special":false,"bg":["#fef3c7","#fef9c3"],"effect":"캐릭터 스킨"}'),
    ('skin', '리온', '명중 사수 · 집중', 420, true, '', '{"special":false,"bg":["#d1fae5","#dcfce7"],"effect":"캐릭터 스킨"}'),
    ('skin', '네오', '우주 탐험가 · 도전', 900, true, '', '{"special":true,"bg":["#e0f2fe","#e0e7ff"],"effect":"스페셜 캐릭터"}'),
    ('pvp_badge', '기록 방어권', '이번 주 최고 기록을 유지해 줘요', 300, true, '/media/assets/item_record_shield.png', '{"special":false,"effect":"최고 기록 보호 · 1회","bg":["#fdf2f8","#fce7f3"]}'),
    ('pvp_badge', '승률하락 방어권', '패배해도 배틀 승률이 내려가지 않아요', 500, true, '/media/assets/item_winrate_shield.png', '{"special":false,"effect":"승률 보호 · 1회","bg":["#f0f9ff","#e0f2fe"]}'),
    ('ticket', 'EXP 2배권', '24시간 동안 경험치가 2배로 쌓여요', 200, true, '/media/assets/item_exp_boost.png', '{"special":false,"effect":"EXP x2 · 24시간","bg":["#fefce8","#fef9c3"]}'),
    ('ticket', '배틀 부활권', '배틀 패배 시 즉시 재도전할 수 있어요', 400, true, '/media/assets/item_battle_retry.png', '{"special":false,"effect":"재도전 1회","bg":["#f0fdf4","#dcfce7"]}'),
    ('ticket', '퀘스트 스킵권', '오늘의 퀘스트를 건너뛸 수 있어요', 150, true, '/media/assets/item_quest_skip.png', '{"special":false,"effect":"퀘스트 스킵 · 1회","bg":["#ede9fe","#f3e8ff"]}')
) as seed(item_type, name, description, price_currency, is_sellable, image_url, metadata)
where not exists (select 1 from items where items.name = seed.name);

insert into wallets (user_id, balance_currency, updated_at)
select id, 1200, now()
from users
where status = 'active'
on conflict (user_id) do update
set balance_currency = greatest(wallets.balance_currency, excluded.balance_currency),
    updated_at = now();

insert into user_items (user_id, item_id, quantity, updated_at)
select users.id, items.id, 1, now()
from users
join items on items.name in ('이수연', '칼라일', '제로스')
where users.status = 'active'
on conflict (user_id, item_id) do update
set quantity = greatest(user_items.quantity, excluded.quantity),
    updated_at = now();
