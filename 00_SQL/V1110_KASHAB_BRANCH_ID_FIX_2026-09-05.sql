-- ═══════════════════════════════════════════════════════════════════════
-- V1110 (০৫.০৯.২০২৬) — KASHAB MANDAL-এর আইডি COB → JPE
--
-- TK: *"জলপাইগুড়ির পেশেন্ট … তাহলে কেন কোচবিহার ব্রাঞ্চে শো করছে?"*
--
-- 🔴 কারণ (প্রমাণিত): সারিটার `branch` = Jalpaiguri, তৈরি করেছেন
--    JPE-JALPAI-13 (8101397763), আজ ১১:২৭। আইডিটা আজই তৈরি হয়েছিল যখন
--    ব্রাঞ্চের ঘরে Cooch Behar ছিল; পরে ব্রাঞ্চ ঠিক করে আবার সেভ হয়েছে,
--    কিন্তু কোড ব্রাঞ্চ বদলালে পুরনো আইডিটা আর যাচাই করত না।
--
-- ⛔ শুধু **মানুষের-পড়ার আইডির ঘরটা** বদলায় — নাম · নম্বর · ব্রাঞ্চ · টাকা ·
--    তারিখ · হিস্ট্রি একটাও ছোঁয়া হয় না।
-- ⛔ টাকার সারির `patientCode`-ও একই সঙ্গে ঠিক হয়, তাই দুই জায়গায় দুরকম
--    আইডি থেকে যেতে পারে না।
-- ⛔ নতুন সিরিয়ালটা ওই দিনের **খালি** নম্বর থেকেই নেওয়া হয় — কারো সঙ্গে
--    সংঘর্ষ হবে না।
-- ⚠️ রোগীকে আগে ছাপা কাগজ দেওয়া হয়ে থাকলে তাতে পুরনো আইডিটাই থাকবে —
--    আজকের রেজিস্ট্রেশন বলে ঝুঁকি সামান্য, তবু TK-কে জানানো হয়েছে।
-- ═══════════════════════════════════════════════════════════════════════

-- ① রোগীর সারিতে সঠিক ব্রাঞ্চের নতুন আইডি
with nxt as (
  select 'JPE-05092026-' ||
         lpad((coalesce(max(substring("patientId" from 14)::int), 0) + 1)::text, 3, '0') as newid
  from public.patients
  where "patientId" ~ '^JPE-05092026-[0-9]{3}$'
)
update public.patients p
set "patientId" = (select newid from nxt),
    "updatedAt" = to_char(now() at time zone 'utc','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
where p."patientId" = 'COB-05092026-001';

-- ② ওই রোগীর টাকার সারিগুলোর মানুষের-পড়ার আইডিও একই সঙ্গে
update public.payments y
set "patientCode" = p."patientId",
    "updatedAt"   = to_char(now() at time zone 'utc','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
from public.patients p
where p.id = y."patientId"
  and y."patientCode" = 'COB-05092026-001';

-- ③ যাচাই — baki_cob অবশ্যই 0 হবে, আর নতুন আইডিটা দেখা যাবে
select
  (select "patientId" from public.patients where branch = 'Jalpaiguri'
     and "registrationDate" = '2026-09-05' and "createdBy" = '8101397763'
     order by "createdAt" desc limit 1) as notun_id,
  (select count(*) from public.patients where "patientId" = 'COB-05092026-001')
  + (select count(*) from public.payments  where "patientCode" = 'COB-05092026-001') as baki_cob;
