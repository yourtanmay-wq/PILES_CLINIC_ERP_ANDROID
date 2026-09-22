-- =============================================================================
-- V224  |  2026-08-01  |  Official Patient ID — Database-level duplicate guard
-- -----------------------------------------------------------------------------
-- অনুমোদিত তালিকা item 82/83 অনুযায়ী:
--   "Free Plan অক্ষত রেখে Database-level unique rule দিয়ে duplicate
--    Official Patient ID বন্ধ করতে হবে" + "দুই ফোনে একসঙ্গে Registration
--    হলেও duplicate Patient ID হবে না।"
--
-- কলাম ম্যাপিং (কোড ও schema মিলিয়ে যাচাই করা — PILES_CLINIC_DB_SETUP.sql):
--   public.patients."id"        = System Record ID  (pat_...)   -- primary key
--   public.patients."patientId" = Official Patient ID (KNE-/COB-/JPE-/FLK-/BIR-…)
--
-- নিরাপত্তা নীতি:
--   * এটি একটি PARTIAL UNIQUE INDEX — শুধু non-null ও non-blank "patientId"-তে
--     প্রযোজ্য। ফলে Orphan/ID-হীন রোগীদের (item 78, blank/null patientId)
--     একাধিক row থাকলেও কোনো সমস্যা হবে না (item 84 রক্ষিত)।
--   * একটিমাত্র index — Supabase Free Plan-এ খরচ/quota অপ্রয়োজনে বাড়ে না
--     (item 16 রক্ষিত)।
--   * `if not exists` — বারবার চালালেও নিরাপদ (idempotent)।
--
-- ⚠️  চালানোর আগে বাধ্যতামূলক ধাপ:
--   STEP 1 (নিচের SELECT) আগে চালান। যদি কোনো row ফেরত আসে, তার মানে
--   এখনই duplicate Official Patient ID ডেটাবেসে আছে — তখন STEP 2 চালাবেন না।
--   আগে duplicate গুলো হাতে মিলিয়ে ঠিক করুন (DEKHAR_SQL_2026-07-28_DUPLICATE_ROGI.sql
--   দিয়ে দেখুন), তারপর আবার STEP 1 চালিয়ে 0 row নিশ্চিত করে STEP 2 চালান।
--   duplicate থাকা অবস্থায় STEP 2 নিজে থেকেই error দেবে ও index তৈরি হবে না —
--   অর্থাৎ ভুল করে চালালেও কোনো ডেটা নষ্ট হবে না, শুধু index তৈরি হবে না।
-- =============================================================================


-- STEP 1 — DUPLICATE PRE-CHECK  (আগে চালান; 0 row = নিরাপদ)
select
    "patientId"                       as official_patient_id,
    count(*)                          as how_many_rows,
    string_agg("id", ', ')            as system_record_ids
from public.patients
where "patientId" is not null
  and btrim("patientId") <> ''
group by "patientId"
having count(*) > 1
order by count(*) desc;


-- STEP 2 — CREATE THE GUARD  (STEP 1 শূন্য হলে তবেই চালান)
-- non-null ও non-blank Official Patient ID-তে duplicate স্থায়ীভাবে বন্ধ।
create unique index if not exists patients_officialid_unique_idx
    on public.patients ("patientId")
    where "patientId" is not null and btrim("patientId") <> '';


-- STEP 3 — VERIFY  (index তৈরি হয়েছে কিনা দেখুন — 1 row আসা উচিত)
select indexname, indexdef
from pg_indexes
where schemaname = 'public'
  and tablename  = 'patients'
  and indexname  = 'patients_officialid_unique_idx';

-- =============================================================================
-- ROLLBACK (প্রয়োজনে গার্ড তুলে দিতে):
--   drop index if exists public.patients_officialid_unique_idx;
-- =============================================================================
