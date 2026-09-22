-- =====================================================================
-- V230 · READ-ONLY SQL তালিকা · 2026-08-01
-- =====================================================================
-- ⛔ প্রতিটি statement শুধুমাত্র SELECT (read-only)। কিছুই বদলায় না।
--    Cloud AI কিছুই চালায়নি — owner একবারে একটি করে চালাবেন।
--
-- ইতিমধ্যে সম্পন্ন (আগের ধাপে):
--   • Duplicate Official Patient ID check → কোনো duplicate নেই ✅
--   • patients_officialid_unique_idx তৈরি ✅ (ভবিষ্যতে duplicate আটকাবে)
--   • মাস-অনুযায়ী রেকর্ড বণ্টন → Report সঠিক প্রমাণিত ✅
--
-- নিচের দুটি শুধু "শনাক্ত করার" query — কোনো রেকর্ড আন্দাজে বদলানো হবে না;
-- ফল দেখে owner সিদ্ধান্ত নেবেন (নিয়ম ৭৯/৮১: আন্দাজে ID/নাম বসানো নিষেধ)।
-- =====================================================================


-- ---------------------------------------------------------------------
-- QUERY 1 · নাম বা Official Patient ID ফাঁকা এমন রোগীর তালিকা
--   (কোন রোগীদের নাম/ID পরে হাতে ঠিক করা দরকার — শুধু দেখা)
-- ---------------------------------------------------------------------
select
    "id"                                   as system_record_id,
    "patientId"                            as official_patient_id,
    coalesce(nullif(btrim("name"),''),'(no name)') as name_shown,
    "mobile",
    "branch",
    coalesce("registrationDate","date")    as reg_or_date
from public.patients
where coalesce(btrim("name"),'')      = ''
   or "patientId" is null
   or btrim("patientId")              = ''
order by coalesce("registrationDate","date") desc nulls last
limit 500;


-- ---------------------------------------------------------------------
-- QUERY 2 · "7777777777" placeholder mobile-এর রেকর্ডের তালিকা
--   (demo/orphan সন্দেহ — শুধু দেখা; সঠিক রোগী owner মিলিয়ে ঠিক করবেন)
-- ---------------------------------------------------------------------
select
    "id"                                   as system_record_id,
    "patientId"                            as official_patient_id,
    coalesce(nullif(btrim("name"),''),'(no name)') as name_shown,
    "mobile",
    "branch",
    coalesce("registrationDate","date")    as reg_or_date
from public.patients
where right(regexp_replace(coalesce("mobile",''), '[^0-9]', '', 'g'), 10) = '7777777777'
order by coalesce("registrationDate","date") desc nulls last
limit 500;

-- =====================================================================
-- শেষ। কোনো ফল দেখে পরিবর্তনকারী SQL দরকার হলে, owner-এর সিদ্ধান্তের পরেই
-- আলাদাভাবে (কী বদলাবে + Free Plan ঝুঁকি জানিয়ে) দেওয়া হবে।
-- =====================================================================
