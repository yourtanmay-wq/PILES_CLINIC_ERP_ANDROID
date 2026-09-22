-- ═══════════════════════════════════════════════════════════════════════
-- V580 (২৩.০৮.২০২৬) — **শুধু মেপে দেখার SQL** (কিছু বদলায় না)
--
-- TK-এর নির্দেশ: *"সম্পূর্ণ প্রজেক্ট খুঁটিয়ে খুঁটিয়ে মিলিয়ে দেখুন, এখনো
-- কোথায় কোথায় ফ্রী প্লানে ঝুঁকি আছে ... সততার সাথে সঠিকভাবে যাচাই করে।"*
--
-- ফ্রি প্ল্যানের ডেটাবেসের সীমা **৫০০ MB**। এখন ৬৮ MB (১৪%)। কিন্তু
-- **কোন টেবিল কত জায়গা নিচ্ছে আর ছবিগুলো কত** — সেটা কোড দেখে বলা যায় না,
-- মেপে দেখতে হয়। এই SQL সেটাই দেখায়।
--
-- ⛔ এটা **একটাও সারি বদলায় না / মোছে না** — শুধু পড়ে ও যোগফল দেখায়।
-- ⛔ যতবার খুশি চালানো যায়।
--
-- কীভাবে: Supabase → SQL Editor → New query → পুরোটা পেস্ট → Run।
-- তিনটে ফল আসবে; তিনটেরই ছবি তুলে পাঠাবেন।
-- ═══════════════════════════════════════════════════════════════════════

-- ── ১) কোন টেবিল কত বড় ─────────────────────────────────────────────────
select
  c.relname                                   as "টেবিল",
  pg_size_pretty(pg_total_relation_size(c.oid)) as "মোট জায়গা",
  c.reltuples::bigint                          as "আনুমানিক সারি"
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
where n.nspname = 'public' and c.relkind = 'r'
order by pg_total_relation_size(c.oid) desc;

-- ── ২) ছবিগুলো কত জায়গা নিচ্ছে ──────────────────────────────────────────
-- (ছবি ডেটাবেসের ভিতরে base64 লেখা হিসেবে জমা থাকে)
select 'patients.photo' as "ঘর",
       count(*) filter (where coalesce(photo,'') <> '') as "যতগুলোয় ছবি আছে",
       pg_size_pretty(coalesce(sum(length(photo)),0))   as "মোট মাপ"
from public.patients
union all
select 'followups.photo',
       count(*) filter (where coalesce(photo,'') <> ''),
       pg_size_pretty(coalesce(sum(length(photo)),0))
from public.followups
union all
select 'medical.photos',
       count(*) filter (where coalesce(photos,'') <> ''),
       pg_size_pretty(coalesce(sum(length(photos)),0))
from public.medical
union all
select 'trash.record (মুছে ফেলা পুরো রেকর্ড)',
       count(*),
       pg_size_pretty(coalesce(sum(length(record::text)),0))
from public.trash;

-- ── ৩) যে টেবিলগুলো শুধু বাড়ে, কখনো পরিষ্কার হয় না ──────────────────────
select 'trash'            as "টেবিল", count(*) as "সারি", min(left(coalesce("deletedAt",''),10)) as "সবচেয়ে পুরনো" from public.trash
union all
select 'activity_logs',   count(*), min(left(coalesce("createdAt",''),10)) from public.activity_logs
union all
select 'deleted_records', count(*), min(left(coalesce("deletedAt",''),10)) from public.deleted_records;

-- 🔴 যদি কোনো টেবিলের নাম না মেলে (যেমন ঘরটা নেই), তাহলে ওই অংশটুকু বাদ
--    দিয়ে বাকিটা চালাবেন — ভুল বার্তা এলে আমাকে ছবি পাঠাবেন, ঠিক করে দেব।
