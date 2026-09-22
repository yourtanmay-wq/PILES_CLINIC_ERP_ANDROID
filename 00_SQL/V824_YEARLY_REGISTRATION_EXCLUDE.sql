-- ============================================================================
-- V824 — 📊 বার্ষিক রেজিস্ট্রেশন-হিসাব: "যাদের বাদ দেব" তাদের তালিকা
--
-- TK-নির্দেশ (২৯.০৮.২০২৬): *"চেম্বারে কতজন এসেছে ০১/০১/২০২৬ থেকে ৩১/১২/২০২৬
-- পর্যন্ত তার একটা হিসাব রাখতে হবে… সত্যিকারের, কোন ডেমোগুলো ধরা হবে না…
-- আমি যদি কিছু বাদ দিয়ে দেই… প্রকৃত নাম থাকলেও থাকতে পারে, ডেমো হিসেবে করা
-- হয়েছিল হয়তোবা — সেটা আমি দেখে দেখে বাদ দিতে পারব, তার ব্যবস্থা রাখবেন।"*
--
-- 🔒 নিরাপত্তা ও ঝুঁকি
--   · এই টেবিল শুধু **কাকে গোনায় ধরব না** তার দাগ রাখে।
--   · ⛔ রোগীর রেকর্ড · টাকা · Follow-up কিচ্ছু ছোঁয়া হয় না, মোছাও হয় না।
--   · ⛔ শুধু **মাস্টার** পড়তে ও লিখতে পারবেন (hr.is_master()) — স্টাফের
--     ফোন থেকে ফাঁকি দেওয়ার পথ নেই, নিয়মটা সার্ভারেই।
--   · ভুল হলে সারিটা মুছে দিলেই রোগী আবার গোনায় ফিরে আসে ("ফেরান" বোতাম)।
--   · Egress: টেবিলটা কয়েকটা সারির, আকার নগণ্য।
--   · দুবার চালালেও ক্ষতি নেই।
--
-- চালানোর জায়গা: Supabase → SQL Editor → পুরোটা পেস্ট করে Run
-- ============================================================================

create table if not exists fin.registration_count_excluded (
  "patient_row_id" text primary key,        -- patients টেবিলের নিজের id
  "patient_code"   text,                    -- দেখানোর জন্য (COB-06072026-001)
  "patient_name"   text,                    -- দেখানোর জন্য
  "reason"         text,                    -- ফাঁকা রাখা যায়
  "excluded_by"    text not null default '',
  "excluded_at"    timestamptz not null default now()
);

comment on table fin.registration_count_excluded is
  'V824: বার্ষিক রেজিস্ট্রেশন-হিসাব থেকে মাস্টারের হাতে বাদ দেওয়া রোগী। শুধু গোনা থেকে বাদ — কোনো রেকর্ড মোছে না।';

alter table fin.registration_count_excluded enable row level security;
revoke all on table fin.registration_count_excluded from public, anon;
grant select, insert, delete on table fin.registration_count_excluded to authenticated;

drop policy if exists rce_read   on fin.registration_count_excluded;
drop policy if exists rce_insert on fin.registration_count_excluded;
drop policy if exists rce_delete on fin.registration_count_excluded;

create policy rce_read   on fin.registration_count_excluded for select using     ( hr.is_master() );
create policy rce_insert on fin.registration_count_excluded for insert with check ( hr.is_master() );
create policy rce_delete on fin.registration_count_excluded for delete using     ( hr.is_master() );

notify pgrst, 'reload schema';

-- READ-ONLY proof: টেবিলটা তৈরি হয়েছে ও শুধু মাস্টারের জন্য খোলা।
select to_regclass('fin.registration_count_excluded') as table_ready;
