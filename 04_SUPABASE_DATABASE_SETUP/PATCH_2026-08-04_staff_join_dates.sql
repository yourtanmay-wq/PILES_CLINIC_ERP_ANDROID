-- =====================================================================
-- PATCH 2026-08-04 — শুধু ৯ জন স্টাফের Joining Date আপডেট (TK-নির্দেশ)
-- =====================================================================
-- ⛔ শুধু hr.staff_profiles-এর join_date কলাম বদলাচ্ছে। Staff Code, নাম,
--    মোবাইল, ব্রাঞ্চ, salary, password, permission, design, workflow —
--    কিছুই ছোঁয়া হচ্ছে না।
-- ⛔ এটা UPDATE, INSERT না — কোনো নতুন Staff Profile তৈরি হবে না। কোনো
--    person_code না মিললে সেই সারিতে কিছুই বদলাবে না (affected rows কমে
--    যাবে, নিচের SELECT দিয়ে ধরা পড়বে)।
-- 🔒 ফরম্যাট ISO (YYYY-MM-DD), DOT (DD.MM.YYYY) না — কারণ:
--    - কম্পিউটারের Staff Profile Edit পাতায় (profile.js) এটা আসল
--      ব্রাউজার ক্যালেন্ডার (type="date"), যেটা শুধু ISO ফরম্যাট বোঝে।
--      DOT ফরম্যাট দিলে ক্যালেন্ডার ফাঁকা দেখাত, আর Master ভুল করে
--      Save চাপলে নতুন বসানো তারিখটাই মুছে যেতে পারত (আসল ঝুঁকি ছিল,
--      TK-কে আগেই জানানো হয়েছে, TK-ই ISO বেছেছেন)।
--    - ফোনে এই ফিল্ড সাধারণ টেক্সট বক্স, তাই ISO ফরম্যাট এখানে দেখতে
--      DOT-এর মতো হবে না (2026-05-01 দেখাবে, 01.05.2026 না) — শুধু
--      দেখতে ভিন্ন, তথ্য সঠিকই থাকবে।
-- =====================================================================

update hr.staff_profiles set join_date = '2026-05-01', updated_at = now() where person_code = 'KNE-KISHAN5';   -- Mohsina Anjum
update hr.staff_profiles set join_date = '2025-04-03', updated_at = now() where person_code = 'KNE-LAXMI';     -- Laxmi Gupta
update hr.staff_profiles set join_date = '2025-04-07', updated_at = now() where person_code = 'COB-UTTAMA';    -- Uttama Barman
update hr.staff_profiles set join_date = '2026-05-01', updated_at = now() where person_code = 'JPE-JALPAI-13'; -- Barnali Roy
update hr.staff_profiles set join_date = '2025-12-15', updated_at = now() where person_code = 'FLK-1';         -- Rina Barman
update hr.staff_profiles set join_date = '2026-08-01', updated_at = now() where person_code = 'COB-4';         -- Bulti Singha
update hr.staff_profiles set join_date = '2026-04-04', updated_at = now() where person_code = 'JPE-RUPAM';
update hr.staff_profiles set join_date = '2026-05-01', updated_at = now() where person_code = 'FALA-15';
update hr.staff_profiles set join_date = '2024-03-18', updated_at = now() where person_code = 'JPE-CRP';       -- Chandana Roy Pradhan

-- চালানোর পরে এই SELECT দিয়ে ৯টা সারিই ঠিক বসেছে কিনা নিজে চোখে দেখে
-- নিন (ছবি তুলে পাঠালে আমিও নিশ্চিত করব — লাইভ ডাটাবেসে আমি নিজে
-- ঢুকতে পারি না, তাই আপনার চালানো ফলাফলই একমাত্র প্রমাণ):
select person_code, full_name, join_date, updated_at
from hr.staff_profiles
where person_code in
  ('KNE-KISHAN5','KNE-LAXMI','COB-UTTAMA','JPE-JALPAI-13','FLK-1','COB-4','JPE-RUPAM','FALA-15','JPE-CRP')
order by person_code;
