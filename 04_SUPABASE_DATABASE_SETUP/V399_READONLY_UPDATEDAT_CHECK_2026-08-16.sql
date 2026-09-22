-- =====================================================================
-- V399b (16.08.2026) — **শুধু পড়া · কিচ্ছু বদলায় না · সম্পূর্ণ নিরাপদ**
--
-- ⚠️ প্রথম চেষ্টায় (V399) ভুল হয়েছিল: `trash` টেবিলে `updatedAt` ঘরটাই নেই,
--    তাই Postgres থামিয়ে দিয়েছিল —
--    ERROR 42703: column "updatedAt" does not exist (LINE 20: 'trash')।
--    ⇒ এটাই একটা কাজের তথ্য: অন্তত একটা টেবিলে ঘরটা নেই।
--
-- এই সংস্করণে `to_jsonb(x)->>'updatedAt'` ব্যবহার করা হয়েছে — ঘরটা না থাকলেও
-- কোনো ভুল হবে না, শুধু ফাঁকা হিসেবে গোনা হবে।
--
-- ফল কীভাবে পড়বেন:
--   blank_updated = 0                      → ওই টেবিল delta sync-এর জন্য নিরাপদ
--   blank_updated = total_rows             → ওই টেবিলে ঘরটাই নেই → delta নয়,
--                                            পুরোটাই নামাতে হবে (ছোট টেবিল হলে ঠিক আছে)
--   0 < blank_updated < total_rows         → কিছু সারিতে ফাঁকা → আগে ঠিক করতে হবে
--
-- চালানোর নিয়ম: Supabase → SQL Editor → পুরোটা পেস্ট করে Run।
-- ⛔ একটাও insert/update/delete নেই — শুধু select।
-- =====================================================================

select 'enquiries' as table_name,
       count(*) as total_rows,
       count(*) filter (where nullif(trim(coalesce(to_jsonb(x)->>'updatedAt','')),'') is null) as blank_updated,
       max(to_jsonb(x)->>'updatedAt') as newest_updated
  from public.enquiries x
union all
select 'patients', count(*),
       count(*) filter (where nullif(trim(coalesce(to_jsonb(x)->>'updatedAt','')),'') is null),
       max(to_jsonb(x)->>'updatedAt')
  from public.patients x
union all
select 'payments', count(*),
       count(*) filter (where nullif(trim(coalesce(to_jsonb(x)->>'updatedAt','')),'') is null),
       max(to_jsonb(x)->>'updatedAt')
  from public.payments x
union all
select 'followups', count(*),
       count(*) filter (where nullif(trim(coalesce(to_jsonb(x)->>'updatedAt','')),'') is null),
       max(to_jsonb(x)->>'updatedAt')
  from public.followups x
union all
select 'medical', count(*),
       count(*) filter (where nullif(trim(coalesce(to_jsonb(x)->>'updatedAt','')),'') is null),
       max(to_jsonb(x)->>'updatedAt')
  from public.medical x
union all
select 'products', count(*),
       count(*) filter (where nullif(trim(coalesce(to_jsonb(x)->>'updatedAt','')),'') is null),
       max(to_jsonb(x)->>'updatedAt')
  from public.products x
union all
select 'doctor_visits', count(*),
       count(*) filter (where nullif(trim(coalesce(to_jsonb(x)->>'updatedAt','')),'') is null),
       max(to_jsonb(x)->>'updatedAt')
  from public.doctor_visits x
union all
select 'briefings', count(*),
       count(*) filter (where nullif(trim(coalesce(to_jsonb(x)->>'updatedAt','')),'') is null),
       max(to_jsonb(x)->>'updatedAt')
  from public.briefings x
union all
select 'trash', count(*),
       count(*) filter (where nullif(trim(coalesce(to_jsonb(x)->>'updatedAt','')),'') is null),
       max(to_jsonb(x)->>'updatedAt')
  from public.trash x
union all
select 'address_tags', count(*),
       count(*) filter (where nullif(trim(coalesce(to_jsonb(x)->>'updatedAt','')),'') is null),
       max(to_jsonb(x)->>'updatedAt')
  from public.address_tags x
order by 1;
