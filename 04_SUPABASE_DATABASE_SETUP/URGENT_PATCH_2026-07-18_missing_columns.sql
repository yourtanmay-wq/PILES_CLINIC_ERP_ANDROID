-- ============================================================
-- জরুরি প্যাচ — এই SQL-টা Supabase-এর SQL Editor-এ গিয়ে একবার Run করুন
-- ============================================================
-- কেন দরকার:
-- অ্যাপের কোড দুটো টেবিলে কিছু তথ্য লিখতে চেষ্টা করে, কিন্তু আসল
-- Database-এ সেই কলাম (column) গুলো তৈরিই করা হয়নি। এই কারণে:
--   ১) Patient Card-এ "Referring Doctor" (কে রেফার করেছে) বসানো/দেখানো
--   ২) Doctor Checkup-এ ছবি (photo) যুক্ত করা
-- এই দুটো ফিচার Supabase-এ সেভ হওয়ার সময় ব্যর্থ হতে পারে (silently
-- fail করতে পারে, বা error দিতে পারে)।
--
-- এই প্যাচ শুধু নতুন কলাম যোগ করে — আপনার আগের কোনো ডেটা,
-- কোনো টেবিল, কোনো রেকর্ড ছোঁয় না। একাধিকবার চালালেও কোনো সমস্যা
-- নেই (IF NOT EXISTS ব্যবহার করা হয়েছে)।
--
-- কীভাবে Run করবেন:
-- ১) Supabase Dashboard-এ লগইন করুন
-- ২) বাঁ পাশের মেনু থেকে "SQL Editor" এ ক্লিক করুন
-- ৩) "New query" চাপুন
-- ৪) নিচের পুরো লেখাটা কপি করে পেস্ট করুন
-- ৫) "Run" চাপুন (সবুজ "Success" বার্তা দেখা উচিত)
-- ============================================================

alter table public.patients add column if not exists "refDoctor" text;
alter table public.patients add column if not exists "refDoctorMobile" text;
alter table public.medical add column if not exists "photos" text;

-- Run করা শেষে "Success. No rows returned" — এই বার্তা দেখলে বুঝবেন ঠিকভাবে হয়েছে।
