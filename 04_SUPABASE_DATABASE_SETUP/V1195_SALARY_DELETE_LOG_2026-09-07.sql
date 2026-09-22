-- ============================================================
-- প্যাচ — এই SQL-টা Supabase-এর SQL Editor-এ গিয়ে একবার Run করুন
-- ============================================================
-- কেন দরকার (V1195, ০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ):
--   • "মনে করুন আমি ভুল করে দিয়ে ফেলেছি, তাহলে সেটা ডিলিট করতে পারছি না কেন?"
--
-- স্যালারির ভুল সারি মাস্টার এখন মুছতে পারবেন। কিন্তু **কে · কখন · কোন সারি
-- মুছল** তার হিসাব থাকা দরকার (এটা আসল ক্লিনিকের টাকার খাতা)। তাই মোছার
-- ঠিক আগে সারিটার নকল এই খাতায় লেখা হয়, তারপরই আসল সারিটা মোছে।
--
-- ⚠️ এই প্যাচ শুধু **একটা নতুন টেবিল** বানায় — পুরনো কোনো টেবিল/কলাম/ডেটা
-- কোনোভাবেই ছোঁয়া হয় না। একাধিকবার চালালেও সমস্যা নেই।
--
-- কীভাবে Run করবেন:
-- ১) Supabase Dashboard → SQL Editor → New query
-- ২) নিচের পুরো লেখাটা কপি-পেস্ট করে Run (সবুজ "Success" দেখা উচিত)
-- ============================================================

create table if not exists hr.salary_deleted_log (
    "id" text primary key,
    "original_id" text,
    "person_code" text,
    "for_month" text,
    "amount" numeric,
    "mode" text,
    "kind" text,
    "paid_on" text,
    "paid_by" text,
    "remark" text,
    "deleted_by" text,
    "deleted_at" text
);

create index if not exists idx_salary_deleted_log_person
    on hr.salary_deleted_log ("person_code");

-- Run করা শেষে "Success" বার্তা দেখলে বুঝবেন ঠিকভাবে হয়েছে।
