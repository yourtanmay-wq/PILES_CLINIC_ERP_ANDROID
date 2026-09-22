-- =====================================================================
-- PILES CLINIC — V311 · LEAVE APPROVAL (advance + monthly-4 + same-day)
--                 + STAFF SUSPEND
-- Owner: TK BISWAS · 11.08.2026 (IST)
--
-- উদ্দেশ্য (TK-নির্দেশ, ধাপে-ধাপে আলোচনা করে ফাইনাল, প্রুফ-অনুমোদিত):
--  • স্টাফ আজ বা অগ্রিম যেকোনো তারিখে ছুটির আবেদন করবে।
--  • সরাসরি ছুটি যদি: (ক) ওই মাসে ৪টার কম  ও  (খ) ওই দিন ব্রাঞ্চে অন্য স্টাফ
--    ছুটিতে নেই। এর যেকোনো একটা ভাঙলে (৫ম+  বা  একই-দিনে দ্বিতীয় জন) →
--    "pending", ব্রাঞ্চের ডাক্তার বা মাস্টার Approve করলে তবেই confirmed।
--  • confirmed ছুটি ব্রাঞ্চের সবাই দেখবে (Briefing) + স্টাফের WhatsApp খুলবে।
--  • মাস্টার কোনো স্টাফকে X দিন Suspend করলে সে ওই সময় লগইন করতে পারবে না।
--
-- ⛔ নিরাপত্তা: কোনো পুরনো টেবিল/হিসাব/RLS ভাঙা হয় না — শুধু নতুন যোগ +
--    hr.staff_profiles-এ একটা নতুন কলাম। বারবার চালানো নিরাপদ (idempotent)।
--
-- চালানোর নিয়ম: Supabase → SQL Editor → পুরো ফাইল পেস্ট → Run।
-- =====================================================================
begin;

-- ---------------------------------------------------------------------
-- 0) সহায়ক: caller-এর role (staff/doctor/master/field) — app_identity থেকে।
--    (hr.my_code()/hr.is_master() V246-এ আছে; role আলাদা লাগবে অনুমোদনে।)
-- ---------------------------------------------------------------------
create or replace function hr.my_role() returns text
  language sql stable security definer set search_path = hr, public as $$
  select role_kind from hr.app_identity where uid = auth.uid();
$$;
revoke all on function hr.my_role() from public, anon;
grant execute on function hr.my_role() to authenticated;

-- ---------------------------------------------------------------------
-- 1) ছুটির আবেদন/অনুমোদন টেবিল — এক সারি = একজন স্টাফের এক দিনের ছুটি।
--    status: pending | confirmed | rejected | cancelled
--    need_reason: '' | '5th' | 'conflict' | '5th+conflict'  (কেন অনুমোদন লাগল)
-- ---------------------------------------------------------------------
create table if not exists wn.leave_requests (
  "id"           uuid primary key default gen_random_uuid(),
  "staff_code"   text not null,
  "staff_mobile" text not null,                 -- 10-digit
  "staff_name"   text,
  "branch"       text not null,
  "leave_date"   date not null,
  "reason"       text,
  "status"       text not null default 'pending',
  "need_reason"  text default '',
  "decided_by"   text,                          -- approver mobile/code
  "decided_at"   timestamptz,
  "created_by"   text,
  "created_at"   timestamptz not null default now(),
  "updated_at"   timestamptz not null default now()
);
-- একজন স্টাফের এক দিনে একটাই সারি (আবার আবেদন করলে upsert)।
create unique index if not exists lr_staff_date_uidx on wn.leave_requests(staff_code, leave_date);
create index if not exists lr_branch_date_idx on wn.leave_requests(branch, leave_date);

alter table wn.leave_requests enable row level security;
alter table wn.leave_requests force row level security;

-- READ: সব authenticated (ছুটি এমনিতেই ব্রাঞ্চে Briefing-এ ঘোষণা হয়, গোপন নয়;
--   অ্যাপ ব্রাঞ্চ ধরে ছাঁকে — তাই পড়া খোলা রাখা নিরাপদ ও দরকারি, কারণ স্টাফ/ডাক্তার
--   দুজনকেই একই-দিন-দ্বন্দ্ব যাচাই করতে ব্রাঞ্চের সারি পড়তে হয়)।
drop policy if exists lr_read on wn.leave_requests;
create policy lr_read on wn.leave_requests for select using ( true );

-- INSERT: স্টাফ নিজের নামেই (staff_code = নিজের code), অথবা master।
drop policy if exists lr_insert on wn.leave_requests;
create policy lr_insert on wn.leave_requests for insert
  with check ( staff_code = hr.my_code() or hr.is_master() );

-- UPDATE: master · ডাক্তার (Approve/Reject) · নিজের সারি (স্টাফ নিজে cancel)।
--   (ব্রাঞ্চ-স্কোপ অ্যাপেই — bell শুধু নিজ ব্রাঞ্চের অনুরোধ দেখায়; ছুটি আর্থিক নয়।)
drop policy if exists lr_update on wn.leave_requests;
create policy lr_update on wn.leave_requests for update
  using ( hr.is_master() or hr.my_role() = 'doctor' or staff_code = hr.my_code() )
  with check ( hr.is_master() or hr.my_role() = 'doctor' or staff_code = hr.my_code() );

grant select, insert, update on wn.leave_requests to authenticated;

-- ---------------------------------------------------------------------
-- 2) SUSPEND — hr.staff_profiles-এ নতুন কলাম (কবে পর্যন্ত সাসপেন্ড)।
--    NULL/অতীত তারিখ = সাসপেন্ড নয়। master-ই লেখে (sp_write master-only, V246)।
-- ---------------------------------------------------------------------
alter table hr.staff_profiles add column if not exists suspended_until date;

-- লগইন-চেক: লগইন হওয়ার আগেই (pre-auth/anon) দেখা দরকার স্টাফ সাসপেন্ড কিনা,
--   কিন্তু sp_read master/self-only — তাই একটা security-definer ফাংশন যা শুধু
--   ওই মোবাইলের সাসপেন্ড-তারিখ ফেরায় (অন্য কিছু নয়)। **public schema-এ**, যাতে
--   অ্যাপের লগইন (anon) সরাসরি /rest/v1/rpc/suspended_until_for দিয়ে ডাকতে পারে।
create or replace function public.suspended_until_for(p_mobile text) returns date
  language sql stable security definer set search_path = public, hr as $$
  select suspended_until from hr.staff_profiles
   where right(regexp_replace(coalesce(link_mobile,''),'\D','','g'),10)
       = right(regexp_replace(coalesce(p_mobile,''),'\D','','g'),10)
   limit 1;
$$;
revoke all on function public.suspended_until_for(text) from public;
grant execute on function public.suspended_until_for(text) to anon, authenticated;

commit;

-- ---------------------------------------------------------------------
-- 3) যাচাই (শুধু-দেখা, চাইলে চালান):
-- select * from wn.leave_requests order by leave_date desc limit 20;
-- select person_code, branch, suspended_until from hr.staff_profiles
--   where suspended_until is not null;
-- =====================================================================
