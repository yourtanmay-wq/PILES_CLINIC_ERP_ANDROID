-- =====================================================================
-- V256_WORK_NOTEBOOK_MISSING_COLUMNS.sql
-- আবিষ্কার: 03.08.2026, Work Notebook-এর ওয়েব-ভার্সন বানাতে গিয়ে ধরা পড়েছে।
--
-- ফোনের Work Notebook (Android) অনেক আগেই (B323 "Mark as Leave", B330
-- "Outside Calls Today", B342 "Notes একটাই বাক্স") নিচের চারটে কলামে
-- লেখার চেষ্টা করছে:
--   wn.notebook_days.is_leave
--   wn.notebook_days.leave_reason
--   wn.notebook_days.outside_calls_manual
--   wn.notebook_days.day_note
--
-- কিন্তু V246_ONE_RUN_SETUP-এ (যেটা এই টেবিল প্রথম বানিয়েছিল) এই চারটে
-- কলাম কখনোই যোগ হয়নি, আর তারপর আলাদা কোনো SQL patch-ও বানানো হয়নি।
-- মানে: ফোনে "Mark as Leave" চাপলে, "Outside Calls Today" লিখলে, বা
-- নতুন Notes বাক্সে লিখে Save করলে, PostgREST কলাম-না-থাকার এরর দেয় —
-- এই কাজগুলো **ফোনের পর্দায় ঠিকই দেখাচ্ছিল, কিন্তু ক্লাউডে আসলে সেভ
-- হচ্ছিল না** (upsert() ব্যর্থ হয়ে false ফেরত দিত, যেটা caller-side এ
-- toast ছাড়া কিছু করে না, তাই কেউ খেয়াল করেননি)।
--
-- এটা এই সেশনের কাজ না (Android কোড ছোঁয়া হয়নি) — কিন্তু ওয়েব-ভার্সন
-- একই কলাম ব্যবহার করবে বলে এই ফাঁকটা এখনই ধরা প্রয়োজন ছিল।
--
-- ⛔ এই SQL সম্পূর্ণ additive (শুধু কলাম যোগ, কিছু মোছে না/বদলায় না)।
-- ⛔ চালানোর পরে আগের থেকে জমে-থাকা কোনো "Mark as Leave"/outside-calls/
--    notes এন্ট্রি নিজে থেকে ফিরে আসবে না (সেগুলো কখনো ক্লাউডে পৌঁছায়ইনি)
--    — আজ থেকে নতুন যা করা হবে তা-ই সঠিকভাবে সেভ হবে।
-- =====================================================================

alter table wn.notebook_days
  add column if not exists is_leave boolean not null default false,
  add column if not exists leave_reason text,
  add column if not exists outside_calls_manual integer not null default 0,
  add column if not exists day_note text;

