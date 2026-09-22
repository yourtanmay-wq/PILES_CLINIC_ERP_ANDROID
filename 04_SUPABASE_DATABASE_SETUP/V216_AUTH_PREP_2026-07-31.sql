-- =====================================================================
-- V216 AUTH-PREP + REFUND INDEX — 2026-07-31 (IST)
-- Base: PILES_CLINIC_APP_V215_FINAL → V216
-- Owner: TK BISWAS
--
-- ⚠️ PART A নিরাপদ ও additive (এখনই চালানো যায়)। PART B (RLS) ইচ্ছে করে
--    COMMENT — ⛔ TK-এর নির্দেশ: এখন RLS চালু নয় (live app বন্ধ হবে)।
-- =====================================================================


-- =====================================================================
-- PART A — নিরাপদ additive (এখনই চালানো যায়)
-- =====================================================================

-- A1. Auth-এ সরার প্রস্তুতি: প্রতিটা usercredentials row-কে ভবিষ্যতের Supabase
--     Auth user-এর সঙ্গে মেলানোর জন্য একটা ঐচ্ছিক কলাম। এখন NULL থাকবে;
--     Auth migration-এর সময় (SupabaseAuth.signUp) প্রতিটা user তৈরি হলে তার
--     auth uid এখানে বসবে। ⛔ additive, কিছু ভাঙে না।
alter table if exists usercredentials
  add column if not exists auth_user_id uuid;

-- A2. V215-এ password_hash/password_algo যোগ হয়েছে; নিশ্চিত করতে আবার (idempotent)।
alter table if exists usercredentials add column if not exists password_hash text;
alter table if exists usercredentials add column if not exists password_algo text;

-- A3. Refund (§13) — pending refund দ্রুত খুঁজতে সহায়ক index (Briefing পর্দার
--     "Pending Refund Requests" ও ঘন্টার count এই query চালায়)।
--     ⛔ non-unique, additive; কোনো data বদলায় না।
create index if not exists payments_refund_pending_idx
  on payments (refundApprovalStatus)
  where payType = 'refund';

-- A4. (রেফারেন্স) hash যাচাই: কোন কোন custom-password user এখনো hash পায়নি?
--   -- select mobile, name from usercredentials
--   -- where coalesce(password,'') <> '' and coalesce(password_hash,'') = '';


-- =====================================================================
-- PART B — RLS (⛔ এখন চালাবেন না — Auth migration-এর পরে)
--   প্রতিটা staff/doctor/master Supabase Auth-এ গেলে, তাদের JWT-তে role/branch
--   claim বসিয়ে (signUp-এর metadata বা custom claim), তারপর নিচের policy।
--   নমুনা (চালানোর জন্য নয়):
--
--   -- alter table patients enable row level security;
--   -- create policy patients_rw on patients using (
--   --   (auth.jwt() ->> 'role') = 'master'
--   --   or branch = (auth.jwt() ->> 'branch')
--   -- ) with check (
--   --   (auth.jwt() ->> 'role') = 'master'
--   --   or branch = (auth.jwt() ->> 'branch')
--   -- );
--   -- payments / followups / medical / doctor_visits — একই ধাঁচে।
--   -- usercredentials: শুধু master read/write; password/password_hash কখনো
--   --   staff/doctor-এর কাছে যাবে না:
--   -- create policy usercreds_master_only on usercredentials using (
--   --   (auth.jwt() ->> 'role') = 'master'
--   -- );
-- =====================================================================

-- END V216 AUTH-PREP
