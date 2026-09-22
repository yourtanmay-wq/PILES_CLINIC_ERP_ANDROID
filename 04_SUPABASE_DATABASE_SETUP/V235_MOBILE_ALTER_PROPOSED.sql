-- ================================================================
-- V235 · কাজ-২ — PROPOSED ALTER (এখনো চালাবেন না)
-- ⚠️ শুধুমাত্র আপনার অনুমোদনের পরে চালাবেন। আগে উপরের
--    V235_MOBILE_READONLY_VERIFY.sql চালিয়ে নিশ্চিত হন column নেই।
--
-- এটি শুধু একটি নতুন column **যোগ** করে — কোনো column/ডেটা মোছে বা বদলায় না।
-- `if not exists` থাকায় দুবার চললেও কিছু ভাঙবে না (idempotent)।
-- বর্তমান `mobile` = Primary Mobile (অপরিবর্তিত)। নতুন `altMobile` = Alternate/Enquiry।
-- ================================================================

alter table public.patients add column if not exists "altMobile" text;

-- (ঐচ্ছিক — শুধু যদি enquiry record-এও Alternate রাখতে চান; কাজের জন্য
--  আবশ্যক নয়, কারণ enquiry-র নিজের নম্বর তো `mobile`-এই আছে):
-- alter table public.enquiries add column if not exists "altMobile" text;

-- যাচাই (read-only) — column যোগ হয়েছে কি না:
-- select column_name from information_schema.columns
-- where table_schema='public' and table_name='patients' and column_name='altMobile';
