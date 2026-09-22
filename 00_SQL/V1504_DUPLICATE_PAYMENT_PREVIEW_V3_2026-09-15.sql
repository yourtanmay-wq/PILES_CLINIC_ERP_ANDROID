-- ═══════════════════════════════════════════════════════════════════════════
-- 🔍🔒 V1504-প্রিভিউ-V3 (১৫.০৯.২০২৬) — V2-তে ধরা পড়া দুটো ভুল সারানো হলো:
--
-- ১) V2 "patientCode" দিয়ে রোগী আলাদা করত — কিন্তু কিছু সারিতে এই ঘরটা
--    ফাঁকা (null), আর ফাঁকা মানে PostgreSQL-এ সবাই "একই দল" ধরে নেয় —
--    ফলে সম্পূর্ণ আলাদা রোগী (SUROJIT BASHAK / DULAL HOSSAIN / HEMENDR ROY
--    SARKAR) একে অপরের "ডুপ্লিকেট" বলে ভুল দেখিয়েছিল। এখন সবসময়-ভরা
--    "patientId" (আসল ভেতরের সংযোগ-ঘর, কখনো ফাঁকা থাকে না) দিয়ে ভাগ হয়।
--
-- ২) V2 কাছাকাছি সময়ে হওয়া **যেকোনো অঙ্কের** সারিকেই সন্দেহভাজন ধরত —
--    কিন্তু LAL BABU/KALAM KHAN-এর মতো কিছু ঘটনায় প্রতিবার **আলাদা আলাদা
--    অঙ্ক** — RIMPA ROY/KHAGEN BHAGAT-এর প্রমাণিত বাগ-প্যাটার্নের (হুবহু
--    একই অঙ্ক + একই ধরন বারবার) সাথে মেলে না, সত্যিকারের আলাদা কিস্তি
--    হতে পারে। **এখন শুধু হুবহু একই অঙ্ক + একই ধরন কাছাকাছি সময়ে (১৫
--    মিনিটের মধ্যে) বসলে তবেই "নিশ্চিত ডুপ্লিকেট"** — বাকি সবাই (একই
--    কাছাকাছি-সময়ের ঘটনার ভিতরে থাকা, কিন্তু অঙ্ক না-মেলা সারিও, যেমন
--    KHAGEN BHAGAT-এর একলা ₹5,000-টা) আলাদা "নিজে দেখুন" তালিকায় —
--    মোছা হয় না, শুধু নজরে আনা হয়।
--
-- ⛔ শুধু পড়া (SELECT)।  ⛔ payType='treatment'।
-- ═══════════════════════════════════════════════════════════════════════════
with clean as (
  select p.*
  from public.payments p
  where p."payType" = 'treatment'
    and p."amount" ~ '^[0-9]+(\.[0-9]+)?$'
    and p."amount"::numeric > 0
    and p."createdAt" ~ '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}'
    and coalesce(p."patientId",'') <> ''
),
exact_grp as (
  select
    c.*,
    count(*) over (partition by c."patientId", c."date", c."payLabel", c."amount", c."mode") as grp_size,
    row_number() over (partition by c."patientId", c."date", c."payLabel", c."amount", c."mode" order by c."createdAt") as rn,
    min(c."createdAt") over (partition by c."patientId", c."date", c."payLabel", c."amount", c."mode") as grp_min,
    max(c."createdAt") over (partition by c."patientId", c."date", c."payLabel", c."amount", c."mode") as grp_max
  from clean c
),
withgap as (
  select
    e.*,
    extract(epoch from (
      e."createdAt"::timestamptz
      - lag(e."createdAt"::timestamptz) over (partition by e."patientId", e."date", e."payLabel" order by e."createdAt")
    )) / 60.0 as gap_minute
  from exact_grp e
),
grouped as (
  select
    w.*,
    sum(case when w.gap_minute is null or w.gap_minute > 3 then 1 else 0 end)
      over (partition by w."patientId", w."date", w."payLabel" order by w."createdAt") as cluster_no
  from withgap w
),
final as (
  select
    g.*,
    count(*) over (partition by g."patientId", g."date", g."payLabel", g.cluster_no) as time_cluster_size,
    (g.grp_size >= 2 and extract(epoch from (g.grp_max::timestamptz - g.grp_min::timestamptz)) / 60.0 <= 15) as is_nishchit
  from grouped g
)
select
  case when f.is_nishchit then 'NISHCHIT_DUPLICATE' else 'SONDEHOVAJON_KINTU_AMOUNT_ALADA' end as dhoron,
  f."branch", f."name", f."patientCode", f."mobile", f."date", f."payLabel",
  f."amount", f."mode", f."createdAt", f."id",
  case
    when f.is_nishchit and f.rn = 1 then 'RAKHBO (আসল, প্রথম)'
    when f.is_nishchit then 'MUCHBO (ডুপ্লিকেট)'
    else 'NIJE_DEKHUN (মোছা হবে না)'
  end as sidhanto
from final f
where f.is_nishchit or f.time_cluster_size >= 2
order by dhoron, f."patientCode", f."date", f."payLabel", f."createdAt";
