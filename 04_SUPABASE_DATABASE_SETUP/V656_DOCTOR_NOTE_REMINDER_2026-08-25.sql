-- =====================================================================
-- V656 — Doctor Note & Reminder (২৫.০৮.২০২৬, TK-নির্দেশ)
-- =====================================================================
-- TK: "কোন রোগীকে আগামী দিন কোন ঔষধ দেওয়া হবে অথবা কোন কাজ করা হবে
-- এরকম যদি প্রতিশ্রুতি দেওয়া হয়ে থাকে তবে সেই রোগীর আসার আগের দিন যেন
-- ডাক্তার বাবুকে মনে করিয়ে দেওয়া হয়।"
--
-- Doctor Checkup-এর History পাতায় নতুন "Doctor Note & Reminder" বাক্সে
-- ডাক্তার একটা নোট + একটা তারিখ বসান। এই দুটো নতুন ঘর সেই তথ্য জমা
-- রাখে — `doctorReminderDate`-এর আগের দিন সন্ধ্যা ৫টায় একবার শুধু
-- ডাক্তারকেই মনে করিয়ে দেওয়া হয় (DoctorReminderWorker.kt)।
--
-- ⛔ শুধু দুটো নতুন ঘর যোগ হয় — কোনো ডেটা/টেবিল মোছা হয় না।
-- চালানোর নিয়ম: Supabase → SQL Editor → New query → পুরো ফাইল পেস্ট → Run।
-- =====================================================================

alter table if exists public.patients
  add column if not exists "doctorReminderNote" text;
alter table if exists public.patients
  add column if not exists "doctorReminderDate" text;

-- "Success. No rows returned" দেখলেই হয়ে গেছে।
