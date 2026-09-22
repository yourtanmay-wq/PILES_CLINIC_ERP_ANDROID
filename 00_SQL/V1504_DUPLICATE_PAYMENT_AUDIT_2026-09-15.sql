-- ═══════════════════════════════════════════════════════════════════════════
-- 🔍🔒 V1504-অডিট (১৫.০৯.২০২৬, TK-নির্দেশ) — "অন্য ব্রাঞ্চেও এরকম ভুল পেমেন্ট
-- আছে কিনা চেক করুন"।
--
-- কী খোঁজে: RIMPA ROY-র মতো ঘটনা (একই রোগী · একই দিন · একই অঙ্ক · একই ধরন
-- (CASH/ONLINE) · খুব কাছাকাছি সময়ে তৈরি — যা ধীর নেটে Save দুবার চাপার
-- চিহ্ন) সব ব্রাঞ্চ মিলিয়ে খুঁজে বের করে।
--
-- ⛔ শুধু পড়া (SELECT) — কোনো টেবিল বদলায় না, কোনো সারি মোছে না।
-- ⛔ payType='treatment'-এই সীমাবদ্ধ — এটাই সেই পথ (Payment/Follow-up
--    Advance/Nth/Chamber) যেখানে V1504-এর ফাঁকটা ছিল।
-- ⛔ ১৫ মিনিটের ভিতরে তৈরি হওয়া একই অঙ্ক/ধরনের সারিকেই সন্দেহভাজন ধরা
--    হয়েছে — সত্যিকারের ইচ্ছাকৃত দ্বিতীয় পেমেন্ট (যেমন সকালে একবার, বিকেলে
--    আবার) এই তালিকায় আসবে না।
-- ═══════════════════════════════════════════════════════════════════════════
select
  p."branch",
  p."name",
  p."patientCode",
  p."mobile",
  p."date",
  p."amount",
  p."mode",
  count(*) as koybar_esheche,
  min(p."createdAt") as prothom_somoy,
  max(p."createdAt") as shesh_somoy,
  round(extract(epoch from (max(p."createdAt"::timestamptz) - min(p."createdAt"::timestamptz))) / 60.0, 1) as minute_forak,
  array_agg(p."id" order by p."createdAt") as row_ids
from public.payments p
where p."payType" = 'treatment'
  and p."amount" ~ '^[0-9]+(\.[0-9]+)?$'
  and p."amount"::numeric > 0
  and p."createdAt" ~ '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}'
group by p."branch", p."name", p."patientCode", p."mobile", p."date", p."amount", p."mode"
having count(*) >= 2
   and extract(epoch from (max(p."createdAt"::timestamptz) - min(p."createdAt"::timestamptz))) / 60.0 <= 15
order by minute_forak asc, p."date" desc;
