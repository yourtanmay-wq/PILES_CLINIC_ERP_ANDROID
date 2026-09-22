-- ============================================================
-- প্যাচ — এই SQL-টা Supabase-এর SQL Editor-এ গিয়ে একবার Run করুন
-- ============================================================
-- কেন দরকার (V1186, ০৭.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ):
--   • "এটা প্রত্যেকের হোম স্ক্রিনে ই থাকবে"
--   • "যে কোন staff, যে কোন ডাক্তার এবং মাস্টার এটা ক্রিয়েট করতে পারবে"
--   • "যে ব্রাঞ্চের যে Doctor তাকেই করতে পারবে"
--   • "পাঠানোর পর KH MANDAL কে Accept করতে হবে"
--   • "পরে যেন History তে ও দেখতে পায়, যাতে কেউ অস্বীকার না করতে পারে"
--
-- এতদিন রিমাইন্ডার জমা হত `patients` টেবিলের তিনটে ঘরে — এক রোগীর একটাই,
-- আর "কে পাঠাল / কে মেনে নিল" কোথাও লেখা থাকত না। তাই নিজের একটা টেবিল।
--
-- ⚠️ এই প্যাচ শুধু ১টা নতুন টেবিল বানায় — কোনো পুরনো টেবিল/কলাম/ডেটা
-- কোনোভাবেই ছোঁয়া হয় না। পুরনো `patients.doctorReminder*` ঘরগুলো ও তাদের
-- নোটিফিকেশন **আগের মতোই** চলবে। একাধিকবার চালালেও সমস্যা নেই।
-- `wfh_requests`/`backdate_payment_grants`-এর হুবহু একই কনভেনশন।
--
-- কীভাবে Run করবেন:
-- ১) Supabase Dashboard → SQL Editor → New query
-- ২) নিচের পুরো লেখাটা কপি-পেস্ট করে Run (সবুজ "Success" দেখা উচিত)
-- ============================================================

create table if not exists public.doctor_reminders (
    "id" text primary key,
    "patientId" text,
    "patientName" text,
    "patientMobile" text,
    "branch" text,
    "note" text,
    "forMobile" text,
    "forName" text,
    "byMobile" text,
    "byName" text,
    "byBranch" text,
    "remindDate" text,
    "remindTime" text,
    "createdAt" text,
    "acceptedBy" text,
    "acceptedByName" text,
    "acceptedAt" text,
    "active" boolean default true
);

-- হোম পর্দা ও নোটিফিকেশন এই দুটো ঘর ধরেই খোঁজে।
create index if not exists idx_doctor_reminders_day
    on public.doctor_reminders ("remindDate", "active");

-- "আমাকে পাঠানো" ও "আমি পাঠিয়েছি" — দ্রুত বের করার জন্য।
create index if not exists idx_doctor_reminders_for
    on public.doctor_reminders ("forMobile");
create index if not exists idx_doctor_reminders_by
    on public.doctor_reminders ("byMobile");

-- Run করা শেষে "Success" বার্তা দেখলে বুঝবেন ঠিকভাবে হয়েছে।
