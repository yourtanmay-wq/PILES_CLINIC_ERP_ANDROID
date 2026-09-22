-- ═══════════════════════════════════════════════════════════════════════════
-- 🔍🔒 V1504-অডিট-V2 (১৫.০৯.২০২৬, TK-নির্দেশ) — আগের অডিটে (V1) শুধু **হুবহু
-- একই অঙ্ক** মিললেই ধরত, তাই KHAGEN BHAGAT-এর ঘটনায় মাঝে একটা ₹5,000-এর
-- সারি (বাকি ১৪টা ₹2,000) আলাদা পড়ে গিয়ে ধরা পড়েনি — TK-র স্ক্রিনশটে
-- আসল মোট ₹33,000 দেখে ভুলটা ধরা পড়ল।
--
-- এই সংস্করণ অঙ্ক না মিললেও ধরে — শুধু **একই রোগী + একই দিন + কাছাকাছি
-- সময়ে (আগের সারির ৩ মিনিটের মধ্যে) তৈরি** হলেই একই "ঘটনা" (cluster)
-- হিসেবে জোড়া হয়, তারপর প্রতিটা ঘটনায় কয়টা সারি ও মোট কত টাকা বসেছে
-- সেটা দেখায়।
--
-- ⛔ শুধু পড়া (SELECT) — কোনো টেবিল বদলায় না।
-- ⛔ payType='treatment'।
-- ═══════════════════════════════════════════════════════════════════════════
with clean as (
  select p.*
  from public.payments p
  where p."payType" = 'treatment'
    and p."amount" ~ '^[0-9]+(\.[0-9]+)?$'
    and p."amount"::numeric > 0
    and p."createdAt" ~ '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}'
),
withgap as (
  select
    c.*,
    extract(epoch from (
      c."createdAt"::timestamptz
      -- 🔴 payLabel-ও ভাগ করে দেখা হয় — নইলে কাছাকাছি সময়ে নেওয়া
      -- সম্পূর্ণ আলাদা টাকা (যেমন "Visit Fee" আর "Advance") একই
      -- ঘটনা বলে ভুল করে জুড়ে যেত।
      - lag(c."createdAt"::timestamptz) over (partition by c."patientCode", c."date", c."payLabel" order by c."createdAt")
    )) / 60.0 as gap_minute
  from clean c
),
grouped as (
  select
    w.*,
    sum(case when w.gap_minute is null or w.gap_minute > 3 then 1 else 0 end)
      over (partition by w."patientCode", w."date", w."payLabel" order by w."createdAt") as cluster_no
  from withgap w
)
select
  g."branch",
  g."name",
  g."patientCode",
  g."mobile",
  g."date",
  g."payLabel",
  count(*) as koybar_esheche,
  sum(g."amount"::numeric) as mot_taka,
  min(g."createdAt") as prothom_somoy,
  max(g."createdAt") as shesh_somoy,
  round(extract(epoch from (max(g."createdAt"::timestamptz) - min(g."createdAt"::timestamptz))) / 60.0, 1) as minute_forak,
  array_agg(g."amount" order by g."createdAt") as amounts,
  array_agg(g."id" order by g."createdAt") as row_ids
from grouped g
group by g."branch", g."name", g."patientCode", g."mobile", g."date", g."payLabel", g.cluster_no
having count(*) >= 2
order by mot_taka desc, g."date" desc;
