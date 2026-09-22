-- =====================================================================
-- V219 SECURITY — COPY-PASTE SQL (Supabase SQL Editor-এ)   2026-07-31 IST
-- Base: PILES_CLINIC_APP_V218_FINAL → V219 · Owner: TK BISWAS
--
-- ⚠️ সততা / নিরাপত্তা:
--   • PART A নিরাপদ ও additive — এখনই চালানো যায়, Login বা Master Password
--     Center কিচ্ছু ভাঙে না, কোনো Patient/Payment data মোছে না।
--   • PART B (RLS enable) ও PART C (plaintext column drop) ইচ্ছে করে **COMMENT**।
--     ⛔ TK-এর নির্দেশ: এখন RLS চালু করবেন না — anon key দিয়ে চলা Login/সব read-write
--        সঙ্গে সঙ্গে বন্ধ হবে। আগে Supabase Auth (নিচে ধাপ)।
--   • Default password (admin123 ইত্যাদি) **SQL দিয়ে বদলাবেন না** — অ্যাপের
--     Master Password Center থেকে বদলালে সেটা নিরাপদে hash হয়ে বসে (কোড অপরিবর্তিত)।
-- =====================================================================


-- ┌───────────────────────────────────────────────────────────────────┐
-- │ PART A — নিরাপদ, এখনই চালানো যায় (COPY-PASTE)                      │
-- └───────────────────────────────────────────────────────────────────┘

-- A1. দরকারি কলাম নিশ্চিত (থাকলে কিছু হয় না — idempotent)।
alter table if exists usercredentials add column if not exists password_hash text;
alter table if exists usercredentials add column if not exists password_algo  text;
alter table if exists usercredentials add column if not exists auth_user_id   uuid;

-- A2. কোন কোন custom-password account এখনো শুধু plaintext (hash বসেনি) —
--     এদের একবার লগইন করালে (বা Master Password Center-এ Save করালে) hash বসে যাবে।
--     ⛔ শুধু SELECT — কিছুই বদলায় না।
-- select mobile, name, role
-- from usercredentials
-- where coalesce(password,'') <> '' and coalesce(password_hash,'') = '';

-- A3. (ঐচ্ছিক, নিরাপদ) Refund pending দ্রুত খোঁজার index — §1/§13 ফিচারে সহায়ক।
create index if not exists payments_refund_pending_idx
  on payments (refundApprovalStatus) where payType = 'refund';


-- ┌───────────────────────────────────────────────────────────────────┐
-- │ PART B — RLS (Row Level Security) — ⛔ এখন চালাবেন না                │
-- │   আগে Supabase Auth-এ যেতে হবে (নিচের ধাপ), তবেই এই policy enable।  │
-- └───────────────────────────────────────────────────────────────────┘
-- Supabase Auth-এ যাওয়ার ধাপ (একবারের manual, অ্যাপ কোড অপরিবর্তিত রেখেই শুরু):
--   1) প্রতিটা staff/doctor/master-এর জন্য একটা Auth user (email/phone + password)।
--      অ্যাপে প্রস্তুত helper আছে: SupabaseAuth.signUp(... metadata: role/branch ...)।
--   2) তৈরি Auth uid → usercredentials.auth_user_id-এ বসান (A1-এর কলাম)।
--   3) অ্যাপের সব request Auth JWT (Authorization: Bearer <jwt>) দিয়ে পাঠান
--      (SupabaseAuth.signInWithPassword)। এই ধাপ live-এ সাবধানে, টেস্ট করে।
--   4) তবেই নিচের policy enable — JWT claim (role/branch) ধরে DB নিজেই যাচাই করবে।
--   ⛔ ৩ নম্বর ধাপ শেষ না করে RLS enable করলে app বন্ধ হবে।
--
-- নমুনা policy (রেফারেন্স — চালানোর জন্য নয়):
-- alter table patients enable row level security;
-- create policy patients_rw on patients
--   using ( (auth.jwt() ->> 'role') = 'master' or branch = (auth.jwt() ->> 'branch') )
--   with check ( (auth.jwt() ->> 'role') = 'master' or branch = (auth.jwt() ->> 'branch') );
-- -- payments / followups / medical / enquiries / doctor_visits — একই ধাঁচে।
-- -- usercredentials: শুধু master; password/password_hash কখনো staff/doctor-এর কাছে নয়:
-- alter table usercredentials enable row level security;
-- create policy usercreds_master_only on usercredentials
--   using ( (auth.jwt() ->> 'role') = 'master' )
--   with check ( (auth.jwt() ->> 'role') = 'master' );


-- ┌───────────────────────────────────────────────────────────────────┐
-- │ PART C — plaintext password কলাম — ⛔ এখন মুছবেন না                  │
-- └───────────────────────────────────────────────────────────────────┘
-- কারণ: এখনো পুরোনো APK ও (আগের) web plaintext মেলায়। সব ফোন hash-ready হলে,
-- এবং Login সম্পূর্ণ hash-only হলে, তবেই নিচেরটা — তার আগে নয়:
-- -- update usercredentials set password = '' where coalesce(password_hash,'') <> '';
-- (কলামটা drop না করে ফাঁকা করা নিরাপদতর — পুরোনো কোনো path NULL-এ crash করবে না।)

-- END V219 SECURITY SQL
