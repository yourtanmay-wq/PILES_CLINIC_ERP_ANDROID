-- =====================================================================
-- V217 — Refund ফিচারের জন্য দরকারি SQL (একবারই চালাতে হবে)
-- =====================================================================
--
-- 🟢 এটা সম্পূর্ণ নিরাপদ — কোনো Patient/Payment ডেটা মোছে না, কোনো টেবিল
--    বদলায় না, শুধু নতুন কিছু "ঘর" (column) যোগ করে যেগুলো এমনিতে ফাঁকা থাকে।
--
-- 👉 কোথায় Paste করবেন (এক ধাপে):
--    ১. Supabase.com-এ লগইন করুন, আপনার প্রজেক্টে ঢুকুন
--       (bcyeogjqtupbdyciqfmz.supabase.co)
--    ২. বাঁদিকের মেনু থেকে "SQL Editor" চাপুন
--    ৩. "New query" চাপুন
--    ৪. নিচের পুরো লেখাটা কপি করে ওখানে Paste করুন
--    ৫. "Run" (বা Ctrl+Enter) চাপুন
--    ৬. "Success" লেখা দেখলেই কাজ শেষ — আর কিছু করতে হবে না
--
-- ⛔ যদি আগের কোনো সেশনে V215_SAFE_MIGRATION_2026-07-31.sql ইতিমধ্যে চালিয়ে
--    থাকেন, তাহলে এটা আবার চালালেও কোনো ক্ষতি নেই (if not exists — দ্বিতীয়বার
--    চালালে চুপচাপ কিছুই করবে না)।
-- =====================================================================

alter table if exists payments add column if not exists refundReason text;
alter table if exists payments add column if not exists refundApprovalStatus text;
alter table if exists payments add column if not exists refundRequestedBy text;
alter table if exists payments add column if not exists refundApprovedBy text;
alter table if exists payments add column if not exists refundOfPaymentId text;

create index if not exists payments_refund_pending_idx
  on payments (refundApprovalStatus)
  where payType = 'refund';

-- (Password hashing-এর জন্য এই দুটো কলামও একই ফাইলে ছিল, আগেই লাগতে পারে —
--  থাকলে ক্ষতি নেই, দ্বিতীয়বার চালালেও চুপচাপ থাকবে)
alter table if exists usercredentials add column if not exists password_hash text;
alter table if exists usercredentials add column if not exists password_algo text;

-- END — এইটুকুই যথেষ্ট। এর বেশি কিছু চালানোর দরকার নেই।
