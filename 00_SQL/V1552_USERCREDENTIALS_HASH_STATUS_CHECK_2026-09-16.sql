-- শুধু দেখার জন্য (SELECT) -- কিছুই বদলাবে না
-- পাসওয়ার্ড-নিরাপত্তার কাজ শুরুর আগে যাচাই: কতজনের নিরাপদ hash এখনো বসেনি
select mobile, role, name, branch,
       case when password_hash is not null and password_hash <> '' then 'হ্যাঁ' else 'না' end as hash_ache_ki
from public.usercredentials
order by hash_ache_ki, mobile;

-- মোট হিসাব
select
  count(*) as total,
  count(*) filter (where password_hash is not null and password_hash <> '') as hash_hoyeche,
  count(*) filter (where password_hash is null or password_hash = '') as hash_baki
from public.usercredentials;
