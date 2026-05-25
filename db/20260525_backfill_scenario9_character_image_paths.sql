update character_profiles
set image_url = case name
    when '도하윤' then '/media/assets/character_도하윤.png'
    when '백유신' then '/media/assets/character_백유신.png'
    when '강민혁' then '/media/assets/character_강민혁.png'
    when '오세라' then '/media/assets/character_오세라.png'
    when '한태윤' then '/media/assets/character_한태윤.png'
    else image_url
end,
    image_status = 'completed'
where scenario_id = 9
  and name in ('도하윤', '백유신', '강민혁', '오세라', '한태윤')
  and (image_url is null or btrim(image_url) = '');
