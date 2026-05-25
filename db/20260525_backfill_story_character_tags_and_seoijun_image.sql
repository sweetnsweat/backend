update character_profiles
set tags = case id
    when 1 then '["황태자", "차가운동맹", "붉은달"]'
    when 2 then '["신전권력", "상냥한통제", "예언"]'
    when 3 then '["마법사", "예언", "조언자"]'
    when 4 then '["그림자", "정보조력", "황실"]'
    when 5 then '["정보상", "중립조력", "비밀거래"]'
    when 6 then '["떠돌이검객", "강호동행", "월하검"]'
    when 7 then '["검수", "숨은단서", "검파"]'
    when 8 then '["침묵", "협객", "강호변수"]'
    when 9 then '["여검객", "서늘함", "강호조력"]'
    when 10 then '["계약동맹", "재벌후계자", "결혼식"]'
    when 11 then '["전약혼자", "배신", "가면"]'
    when 12 then '["라이벌", "상냥함", "재벌가"]'
    when 13 then '["야심가", "재벌가", "우아함"]'
    when 14 then '["정치가", "청렴한척", "적수"]'
    when 15 then '["스캔들", "엔터조력", "설계자"]'
    when 16 then '["회귀", "검귀", "복수자"]'
    when 17 then '["동문", "성장형동료", "흔들림"]'
    when 18 then '["사파책사", "위험한조력", "계략"]'
    when 19 then '["사문", "그림자", "내부변수"]'
    when 20 then '["여검객", "의리", "강호동맹"]'
    when 21 then '["문파", "사문인물", "흔들림"]'
    when 22 then '["회귀", "검귀", "복수자"]'
    when 23 then '["동문", "성장형동료", "흔들림"]'
    when 24 then '["사파책사", "위험한조력", "계략"]'
    when 25 then '["암살자", "정보변수", "그림자"]'
    when 26 then '["여검객", "의리", "강호동맹"]'
    when 27 then '["황실첩보", "검객", "변수"]'
    else tags
end
where id between 1 and 27
  and (tags is null or btrim(tags) in ('', '[]'));

update character_profiles
set image_url = '/media/assets/scenario_9_player.png',
    image_status = 'completed'
where name = '서이준'
  and scenario_id = 9
  and (image_url is null or btrim(image_url) = '');

delete from scenario_genres
where scenario_id = 9
  and genre_name = '현대 헌터 회귀 계약 로맨스 복수극';

insert into scenario_genres (scenario_id, genre_name, seq)
values
    (1, '로맨스', 1),
    (1, '판타지', 2),
    (2, '무협', 1),
    (2, '판타지', 2),
    (3, '현대', 1),
    (3, '막장', 2),
    (3, '회귀', 3),
    (3, '복수', 4),
    (3, '드라마', 5),
    (4, '무협', 1),
    (4, '회귀', 2),
    (4, '복수', 3),
    (4, '로맨스', 4),
    (5, '무협', 1),
    (5, '회귀', 2),
    (5, '복수', 3),
    (5, '로맨스', 4),
    (7, '로맨스', 1),
    (7, '판타지', 2),
    (7, '회귀', 3),
    (7, '복수', 4),
    (8, '다크', 1),
    (8, '판타지', 2),
    (8, '로맨스', 3),
    (9, '현대', 1),
    (9, '헌터', 2),
    (9, '회귀', 3),
    (9, '계약', 4),
    (9, '로맨스', 5),
    (9, '복수극', 6),
    (100, '코미디', 1)
on conflict (scenario_id, genre_name) do nothing;
