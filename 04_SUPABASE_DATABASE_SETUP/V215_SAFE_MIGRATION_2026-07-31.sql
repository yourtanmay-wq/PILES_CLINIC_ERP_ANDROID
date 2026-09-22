-- =====================================================================
-- V215 SAFE MIGRATION — 2026-07-31 (IST)
-- Base: PILES_CLINIC_APP_V214_FINAL
-- Owner: TK BISWAS
--
-- ⚠️ পড়ুন আগে:
--  এই ফাইলের PART A **নিরাপদ ও additive** — চালালে কোনো Patient/Payment
--  data মোছে না, কোনো টেবিল drop হয় না, live app বন্ধ হয় না।
--  PART B ও PART C ইচ্ছে করে **COMMENT করা** — কারণ ওগুলো (UNIQUE/FK enforce
--  এবং RLS enable) live data-তে সরাসরি চালালে বিদ্যমান duplicate থাকলে
--  fail করবে বা anon-key app বন্ধ করে দেবে। আগে PART A-র duplicate-finder
--  চালিয়ে ফল দেখে, TK-এর অনুমতি নিয়ে, তবেই ধাপে ধাপে খোলা হবে।
--
--  ⛔ TK-এর নির্দেশ: "এখনই RLS Enable করবেন না।" — তাই PART C বন্ধ রাখা হলো।
-- =====================================================================


-- =====================================================================
-- PART A — নিরাপদ, এখনই চালানো যায় (additive only)
-- =====================================================================

-- A1. usercredentials-এ password_hash কলাম যোগ (nullable, additive)।
--     পুরোনো plaintext `password` কলাম এখনই মোছা হয় না — migration শেষ হলে
--     (সব hash বসে গেলে, login hash দিয়ে যাচাই শুরু হলে) তখন আলাদা ধাপে মোছা হবে।
--     ⛔ কোনো row বদলায় না, কিছু হারায় না।
alter table if exists usercredentials
  add column if not exists password_hash text;

alter table if exists usercredentials
  add column if not exists password_algo text;   -- যেমন 'pbkdf2_sha256$120000' — কোন নিয়মে hash

-- A2. Refund feature (§13)-এর জন্য payments টেবিলে কিছু ঐচ্ছিক কলাম।
--     Refund একটা আলাদা row হিসেবেই থাকবে (payType='refund'), পুরোনো payment
--     row কখনো edit/delete হবে না। নিচের কলামগুলো শুধু refund row-এর বাড়তি তথ্য
--     ধরে (reason/approval/who) — সাধারণ payment row-এ null থাকবে, হিসাব বদলায় না।
--     ⛔ additive; পুরোনো কোনো হিসাব/row বদলায় না।
alter table if exists payments add column if not exists refundReason text;
alter table if exists payments add column if not exists refundApprovalStatus text;   -- 'pending' | 'approved' | 'rejected' | '' (master সরাসরি করলে approved)
alter table if exists payments add column if not exists refundRequestedBy text;
alter table if exists payments add column if not exists refundApprovedBy text;
alter table if exists payments add column if not exists refundOfPaymentId text;       -- কোন advance/collection-এর বিপরীতে ফেরত (optional trace)

-- A3. DUPLICATE-FINDER (শুধু SELECT — কিছুই বদলায় না)।
--     PART B (UNIQUE/FK) চালানোর আগে এগুলো চালিয়ে দেখুন ফল ফাঁকা কিনা।
--     ফল ফাঁকা না হলে আগে duplicate ঠিক করতে হবে, নইলে UNIQUE/FK fail করবে।

-- A3.1 একই স্বাভাবিক-মোবাইলে একাধিক patients row (duplicate রোগী):
--   select right(regexp_replace(mobile,'\D','','g'),10) as mob10, count(*) c,
--          array_agg(id) ids, array_agg(patientId) codes
--   from patients
--   group by 1 having count(*) > 1
--   order by c desc;

-- A3.2 একই patientId (patientCode) একাধিক row-তে (duplicate code):
--   select "patientId", count(*) c, array_agg(id) ids
--   from patients where coalesce("patientId",'') <> ''
--   group by 1 having count(*) > 1;

-- A3.3 orphan payment (যে patientId কোনো patients row-এ নেই):
--   select p.id, p."patientId", p.mobile, p.amount, p.date
--   from payments p
--   left join patients pt on pt.id = p."patientId"
--   where coalesce(p."patientId",'') <> '' and pt.id is null;

-- A4. Error/Activity log cleanup নীতি (§6.14 — সফল হওয়ার ৭ দিন পরে শুধু error log)।
--     ⛔ এটি Patient/Payment/Treatment/Medical/Follow-up কিছুই মোছে না — শুধু
--        activity_logs-এর 'error'/'sync_error' ধরনের সারি, ৭ দিনের পুরোনো।
--     নিরাপদ রাখতে এটাও এখন COMMENT — চালানোর আগে TK দেখে নেবেন কোন level নাম
--     আসল টেবিলে আছে।
--   -- delete from activity_logs
--   -- where lower(coalesce(level,'')) in ('error','sync_error')
--   --   and (createdAt)::timestamptz < now() - interval '7 days';


-- =====================================================================
-- PART B — INTEGRITY (UNIQUE / FOREIGN KEY) — ⛔ এখন চালাবেন না
--   আগে PART A3-এর duplicate-finder ফাঁকা হতে হবে। তারপর একটা একটা করে।
--   এগুলো live-এ চালানোর আগে অবশ্যই একটা backup/branch-এ পরীক্ষা করুন।
-- =====================================================================

-- B1. ⛔⛔⛔ কখনো চালাবেন না — TK-নির্দেশে স্থায়ীভাবে বাতিল (২৯.০৮.২০২৬)।
--     কারণ: এটা মোবাইল নম্বরে তালা দেয়। কিন্তু অ্যাপে TK-এর নিজের পাশ করা
--     "Different Patient — Same Mobile" (V516/V520) আছে — এক পরিবারে স্বামী ও
--     স্ত্রী দুজনেই রোগী, যোগাযোগের মোবাইল একটাই। এই index বসালে দ্বিতীয়
--     জনকে আর কোনোদিন রেজিস্টার করা যাবে না, লাইভ ক্লিনিকে সেভ আটকে যাবে।
--     ⛔ Patient ID-র আসল পাহারা আগে থেকেই আছে ও চলছে:
--        V224_2026-08-01_official_patient_id_unique.sql (patients_officialid_unique_idx)।
-- B1. একই স্বাভাবিক-মোবাইলে দ্বিতীয় patients row আটকানো (partial unique index)।
--     -- create unique index concurrently if not exists patients_mob10_uidx
--     --   on patients ( (right(regexp_replace(mobile,'\D','','g'),10)) )
--     --   where mobile is not null and length(regexp_replace(mobile,'\D','','g')) >= 10;

-- B2. patientId (patientCode) unique।
--     -- create unique index concurrently if not exists patients_patientid_uidx
--     --   on patients ("patientId") where coalesce("patientId",'') <> '';

-- B3. payment -> patient foreign key (orphan payment আটকায়)।
--     ⛔ ON DELETE কিছুই দেওয়া হয়নি ইচ্ছে করে — app soft-delete (Trash) ব্যবহার
--        করে, DB থেকে patient hard-delete হয় না, তাই cascade দরকার নেই।
--     -- alter table payments
--     --   add constraint payments_patient_fk
--     --   foreign key ("patientId") references patients(id) not valid;
--     -- alter table payments validate constraint payments_patient_fk;


-- =====================================================================
-- PART C — RLS (Row Level Security) — ⛔⛔ TK-এর সরাসরি নির্দেশ: এখন চালাবেন না
--   কারণ: এখন সব read/write হয় anon (publishable) key দিয়ে। RLS enable করলেই
--   anon key আটকে যাবে → **live app সঙ্গে সঙ্গে বন্ধ**। আগে Supabase Auth-এ
--   সরাতে হবে (প্রতিটা staff/doctor/master-এর আসল Auth user), তারপর নিচের
--   policy-গুলো JWT claim (role/branch) ধরে লিখে, তবেই enable।
--   পুরো ধাপ V215_MANUAL_SETUP_IF_REQUIRED.md-তে লেখা।
--
--   নিচে target policy-র নমুনা রইল (রেফারেন্স, চালানোর জন্য নয়):
--   -- alter table patients enable row level security;
--   -- create policy patients_branch_read on patients for select
--   --   using ( auth.jwt() ->> 'role' = 'master'
--   --           or branch = (auth.jwt() ->> 'branch') );
--   -- create policy patients_branch_write on patients for insert with check (
--   --           auth.jwt() ->> 'role' = 'master'
--   --           or branch = (auth.jwt() ->> 'branch') );
--   -- (payments/followups/medical/usercredentials ইত্যাদির জন্য একই ধাঁচে,
--   --  usercredentials শুধু master read করতে পারবে — password/hash কখনো staff/doctor নয়।)
-- =====================================================================

-- END V215 SAFE MIGRATION
