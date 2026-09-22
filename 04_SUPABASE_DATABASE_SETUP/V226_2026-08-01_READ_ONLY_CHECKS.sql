-- =====================================================================
-- V226 · READ-ONLY CHECKS · 2026-08-01
-- =====================================================================
-- ⛔ এই ফাইলের প্রতিটি statement শুধুমাত্র SELECT (read-only)।
--    কোনো CREATE / ALTER / UPDATE / DELETE / DROP নেই।
--    Cloud AI এই SQL কোথাও চালায়নি — owner নিজে Supabase SQL Editor-এ
--    চালিয়ে ফল দেখবেন, তারপর সিদ্ধান্ত নেবেন।
--
-- উদ্দেশ্য:
--   (A) Official Patient ID duplicate-guard index সত্যিই তৈরি হয়েছে কিনা যাচাই
--   (B) এখন কোনো duplicate Official Patient ID আছে কিনা তালিকা
--   (C) blank / orphan patientId সারি তালিকা (আন্দাজে বদলানো নিষেধ)
--   (D) "7777777777" placeholder mobile-এর সারি তালিকা
--   (E) Free Plan ঝুঁকি বোঝাতে বড় টেবিলের সারি-গণনা
--
-- ⚠️ কোনো ফল দেখে destructive/update SQL বানানোর আগে owner-এর অনুমোদন লাগবে।
--    Duplicate থাকলে আগে (B)-এর তালিকা মিলিয়ে সঠিক রোগী চিহ্নিত করতে হবে;
--    কোনো Patient ID আন্দাজে পরিবর্তন করা যাবে না (নিয়ম ৭৯/৮১)।
-- =====================================================================


-- ---------------------------------------------------------------------
-- (A) Official Patient ID unique index তৈরি হয়েছে কিনা?
--     V224_2026-08-01_official_patient_id_unique.sql-এ যে index দেওয়া ছিল
--     (patients_officialid_unique_idx) সেটি live DB-তে সত্যিই আছে কিনা।
--     সারি এলে = index তৈরি হয়েছে (guard সক্রিয়)। খালি এলে = এখনো চালানো হয়নি।
-- ---------------------------------------------------------------------
select
    schemaname,
    tablename,
    indexname,
    indexdef
from pg_indexes
where schemaname = 'public'
  and tablename  = 'patients'
  and indexname  = 'patients_officialid_unique_idx';


-- ---------------------------------------------------------------------
-- (B) এখন কোনো duplicate Official Patient ID আছে কিনা?
--     ("patientId" = Official Patient ID; blank/null বাদ)
--     সারি এলে = ঐ ID-গুলো একাধিক রেকর্ডে আছে — unique index তৈরির আগে
--     এগুলো হাতে মিলিয়ে ঠিক করতে হবে। খালি = কোনো duplicate নেই।
-- ---------------------------------------------------------------------
select
    "patientId"                as official_patient_id,
    count(*)                   as how_many_rows,
    string_agg("id", ', ')     as system_record_ids,
    string_agg(coalesce("mobile",''), ', ') as mobiles,
    string_agg(coalesce("branch",''), ', ') as branches
from public.patients
where "patientId" is not null
  and btrim("patientId") <> ''
group by "patientId"
having count(*) > 1
order by count(*) desc;


-- ---------------------------------------------------------------------
-- (C) blank / orphan Official Patient ID সারি (patientId null বা ফাঁকা)
--     এগুলো partial unique index-এ বাদ থাকে (নিরাপদ), কিন্তু owner হয়তো
--     সঠিক রোগীর সঙ্গে হাতে মেলাতে চাইবেন। ⛔ আন্দাজে ID বসানো নিষেধ।
--     (limit 500 — বড় ফল এড়াতে; দরকারে বাড়ানো যায়।)
-- ---------------------------------------------------------------------
select
    "id"        as system_record_id,
    "patientId" as official_patient_id,
    "name",
    "mobile",
    "branch",
    coalesce("registrationDate", "date") as reg_or_date
from public.patients
where "patientId" is null
   or btrim("patientId") = ''
order by coalesce("registrationDate", "date") desc nulls last
limit 500;


-- ---------------------------------------------------------------------
-- (D) "7777777777" placeholder mobile-এর সারি (demo/orphan সন্দেহ)
--     শেষ ১০ অঙ্ক 7777777777 হলে দেখায়। ⛔ আন্দাজে পরিবর্তন নিষেধ —
--     শুধু চিহ্নিত করে owner-কে দেখানো।
-- ---------------------------------------------------------------------
select
    "id"        as system_record_id,
    "patientId" as official_patient_id,
    "name",
    "mobile",
    "branch",
    coalesce("registrationDate", "date") as reg_or_date
from public.patients
where right(regexp_replace(coalesce("mobile",''), '[^0-9]', '', 'g'), 10) = '7777777777'
order by coalesce("registrationDate", "date") desc nulls last
limit 500;


-- ---------------------------------------------------------------------
-- (E) Free Plan ঝুঁকি বোঝাতে বড় টেবিলের সারি-গণনা (read-only)
--     Supabase Free Plan-এ storage/row বাড়লে খরচ/সীমা ঝুঁকি — শুধু গণনা,
--     কিছু বদলায় না।
-- ---------------------------------------------------------------------
select 'patients'      as table_name, count(*) as row_count from public.patients
union all
select 'enquiries'     as table_name, count(*) from public.enquiries
union all
select 'followups'     as table_name, count(*) from public.followups
union all
select 'payments'      as table_name, count(*) from public.payments
union all
select 'doctor_visits' as table_name, count(*) from public.doctor_visits
union all
select 'trash'         as table_name, count(*) from public.trash
union all
select 'deleted_records' as table_name, count(*) from public.deleted_records
order by row_count desc;

-- =====================================================================
-- শেষ। কোনো ফল দেখে পরবর্তী পদক্ষেপ (index তৈরি / duplicate সংশোধন)
-- করার আগে owner-এর সুস্পষ্ট অনুমোদন নিতে হবে। এই ফাইল নিজে কিছুই বদলায় না।
-- =====================================================================
