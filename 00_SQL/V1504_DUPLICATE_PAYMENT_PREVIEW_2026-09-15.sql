-- ═══════════════════════════════════════════════════════════════════════════
-- 🔍🔒 V1504-প্রিভিউ (১৫.০৯.২০২৬, TK-নির্দেশ: "মুছে দিন, কিন্তু আন্দাজে না,
-- কোনো অরিজিনাল পেমেন্ট যেন ডিলিট না হয়") — মোছার আগে **কী মোছা হবে সেটা
-- আগে চোখে দেখা**।
--
-- নিয়ম: একই রোগী + একই দিন + একই পেমেন্ট-লেবেল (Advance/2nd Payment/…)
-- -এর মধ্যে যেসব সারি একে অপরের ৩ মিনিটের মধ্যে তৈরি হয়েছে, সেগুলোকে একটা
-- "ঘটনা" (cluster) ধরা হয়। প্রতিটা ঘটনায় **প্রথম সারিটাই আসল** (স্টাফ যা
-- সত্যিই প্রথমবার সেভ করেছিলেন), বাকিগুলো ধীর নেটে বারবার Save চাপার ফল —
-- V1504-এর আগে যে ফাঁক ছিল তার প্রমাণ। এই ধারণাটাই RIMPA ROY আর KHAGEN
-- BHAGAT দুটো নিশ্চিত ঘটনাতেই ঠিক মিলেছে।
--
-- ⛔ শুধু পড়া (SELECT) — কিছু মোছে না। TK নিজে "MUCHBO" তালিকাটা একবার
--    চোখ বুলিয়ে নেবেন, তারপরই DELETE (V1504_DUPLICATE_PAYMENT_DELETE_...sql) চলবে।
-- ⛔ payType='treatment'-এই সীমাবদ্ধ (এটাই যে পথে ফাঁকটা ছিল)।
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
),
sized as (
  select
    g.*,
    count(*) over (partition by g."patientCode", g."date", g."payLabel", g.cluster_no) as cluster_size,
    row_number() over (partition by g."patientCode", g."date", g."payLabel", g.cluster_no order by g."createdAt") as rn
  from grouped g
)
select
  s."branch",
  s."name",
  s."patientCode",
  s."mobile",
  s."date",
  s."payLabel",
  s."amount",
  s."mode",
  s."createdAt",
  s."id",
  case when s.rn = 1 then 'RAKHBO (আসল, প্রথম)' else 'MUCHBO (ডুপ্লিকেট)' end as sidhanto
from sized s
where s.cluster_size >= 2
order by s."patientCode", s."date", s."payLabel", s."createdAt";
