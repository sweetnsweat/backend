create table if not exists items (
    id bigserial primary key,
    item_type varchar(50) not null,
    name varchar(255) not null,
    description text,
    price_currency integer not null default 0,
    is_sellable boolean not null default true,
    image_url text,
    metadata jsonb not null default '{}'::jsonb,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    constraint items_item_type_check
        check (item_type in ('consumable', 'skin', 'profile', 'pvp_badge', 'ticket', 'gift')),
    constraint items_price_currency_check
        check (price_currency >= 0)
);

create table if not exists user_items (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    item_id bigint not null references items(id),
    quantity integer not null default 0,
    updated_at timestamptz not null default now(),
    constraint user_items_quantity_check
        check (quantity >= 0),
    constraint user_items_user_id_item_id_key
        unique (user_id, item_id)
);

insert into items (item_type, name, description, price_currency, is_sellable, image_url, metadata, is_active)
select 'skin', '블루 트레이닝 스킨', '캐릭터 외형을 파란 트레이닝 테마로 변경합니다.', 100, true, '/media/assets/item_blue_training_skin.png', '{"theme":"blue_training"}'::jsonb, true
where not exists (select 1 from items where name = '블루 트레이닝 스킨');

insert into items (item_type, name, description, price_currency, is_sellable, image_url, metadata, is_active)
select 'profile', '골드 프로필 프레임', '마이페이지 프로필에 골드 프레임을 적용합니다.', 80, true, '/media/assets/item_gold_profile_frame.png', '{"frame":"gold"}'::jsonb, true
where not exists (select 1 from items where name = '골드 프로필 프레임');

insert into items (item_type, name, description, price_currency, is_sellable, image_url, metadata, is_active)
select 'ticket', '스토리 재도전 티켓', '완료한 스토리 챕터를 다시 진행할 수 있는 티켓입니다.', 50, true, '/media/assets/item_story_ticket.png', '{"use":"story_replay"}'::jsonb, true
where not exists (select 1 from items where name = '스토리 재도전 티켓');

insert into items (item_type, name, description, price_currency, is_sellable, image_url, metadata, is_active)
select 'consumable', '회복 응원 배지', '컨디션이 낮은 날 회복 퀘스트 화면에 표시되는 응원 배지입니다.', 30, true, '/media/assets/item_recovery_badge.png', '{"effect":"display_badge"}'::jsonb, true
where not exists (select 1 from items where name = '회복 응원 배지');
