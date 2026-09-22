-- ═══════════════════════════════════════════════════════════════════════════
-- 🗑️🔒 V861 (৩০.০৮.২০২৬) — TK-নিশ্চিত **৭টা ডেমো নম্বর** চিরতরে মোছা
--
-- TK নিজে দেখে নিশ্চিত করেছেন (V860-এর ফল দেখে):
--   ৭টা নম্বরের নাম হয় স্টাফের নিজের, নয় "TEST" / "Ravi" ⇒ ডেমো।
--   ⛔ Raja Roy (7583973566) ও SERINA KHATTON (8210342405) — **আসল রোগী**,
--      TK-এর স্পষ্ট কথা ⇒ ওই দুটো নম্বর এই তালিকায় **নেই**, ছোঁয়াও হবে না।
--   ⏸️ Boni Mukherjee (8101397763) — TK এখনো বলেননি ⇒ এই তালিকায় **নেই**।
--
-- ⛔⛔ মোছা **ফেরানো যায় না**। ধাপ ১-এর ব্যাকআপ আগে বসে, তারপর মোছা।
-- ⛔ যা **ছোঁয়া হবে না**: `doctor_visits` (RMP ডাক্তারের রেকর্ড) ও
--    `hr.staff_profiles` (স্টাফের নিজের প্রোফাইল/লগইন) — ওগুলো আসল।
-- 💰 এতে প্রায় **₹১,৪০,৪০০** ডেমো টাকা রিপোর্ট থেকে চলে যাবে — এটাই চাওয়া।
-- ═══════════════════════════════════════════════════════════════════════════

create temporary table if not exists _demo(m text);
delete from _demo;
insert into _demo values
  ('9002003540'),  -- FIELD-OFFICER   → নাম "TEST"
  ('8676002200'),  -- Clinic Kishanganj → নাম "Ravi" / "MOHSINA"
  ('8001080080'),  -- TK-এর নিজের ডেমো → "TK BISWAS" / "TK"
  ('7679751521'),  -- COB-UTTAMA      → স্টাফের নিজের নাম
  ('8167096595'),  -- JPE-RUPAM       → স্টাফের নিজের নাম
  ('7321960416'),  -- KNE-KISHAN8     → স্টাফের নিজের নাম
  ('6207841890');  -- KNE-KISHAN5     → স্টাফের নিজের নাম

-- ─── ধাপ ১ — ব্যাকআপ (মোছার আগেই বসে) ────────────────────────────────────
create table if not exists public.v861_demo_backup (
  "id" bigserial primary key, "tableName" text, "row" jsonb,
  "savedAt" timestamptz default now()
);
insert into public.v861_demo_backup("tableName","row")
select 'enquiries', to_jsonb(t) from public.enquiries t, _demo g where t."mobile" like '%'||g.m;
insert into public.v861_demo_backup("tableName","row")
select 'followups', to_jsonb(t) from public.followups t, _demo g where t."mobile" like '%'||g.m;
insert into public.v861_demo_backup("tableName","row")
select 'patients',  to_jsonb(t) from public.patients  t, _demo g where t."mobile" like '%'||g.m or t."altMobile" like '%'||g.m;
insert into public.v861_demo_backup("tableName","row")
select 'payments',  to_jsonb(t) from public.payments  t, _demo g where t."mobile" like '%'||g.m;
insert into public.v861_demo_backup("tableName","row")
select 'medical',   to_jsonb(t) from public.medical   t, _demo g where t."mobile" like '%'||g.m;
insert into public.v861_demo_backup("tableName","row")
select 'products',  to_jsonb(t) from public.products  t, _demo g where t."mobile" like '%'||g.m;

-- ─── ধাপ ২ — চিরতরে মোছা ─────────────────────────────────────────────────
delete from public.payments  t using _demo g where t."mobile" like '%'||g.m;
delete from public.medical   t using _demo g where t."mobile" like '%'||g.m;
delete from public.products  t using _demo g where t."mobile" like '%'||g.m;
delete from public.followups t using _demo g where t."mobile" like '%'||g.m;
delete from public.enquiries t using _demo g where t."mobile" like '%'||g.m;
delete from public.patients  t using _demo g where t."mobile" like '%'||g.m or t."altMobile" like '%'||g.m;

do $$
begin
  if to_regclass('public.call_remarks') is not null then
    execute 'delete from public.call_remarks t using _demo g where t."mobile" like ''%''||g.m';
  end if;
  if to_regclass('public.dialer_calls') is not null then
    execute 'delete from public.dialer_calls t using _demo g where t."dialedNumber" like ''%''||g.m';
  end if;
  if to_regclass('public.trash') is not null then
    execute 'delete from public.trash t using _demo g where t."record"::text like ''%''||g.m||''%''';
  end if;
end $$;

notify pgrst, 'reload schema';

-- ─── ধাপ ৩ — মিলিয়ে দেখা (সব ঘরে ০ আসা চাই) ─────────────────────────────
select g.m::text as "নম্বর",
       (select count(*) from public.enquiries e where e."mobile" like '%'||g.m) as "এনকোয়ারি",
       (select count(*) from public.followups f where f."mobile" like '%'||g.m) as "ফলোআপ",
       (select count(*) from public.patients  p where p."mobile" like '%'||g.m) as "রোগী",
       (select count(*) from public.payments  y where y."mobile" like '%'||g.m) as "পেমেন্ট",
       (select count(*) from public.v861_demo_backup b
          where b."row"::text like '%'||g.m||'%')                              as "ব্যাকআপে জমা"
  from _demo g order by 1;

-- ─── ধাপ ৪ — ফোনে ─────────────────────────────────────────────────────────
-- প্রতিটা ফোনে অ্যাপ একবার বন্ধ করে আবার খুলুন।
