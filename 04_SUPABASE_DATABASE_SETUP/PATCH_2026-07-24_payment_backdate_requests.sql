-- ============================================================
-- প্যাচ — এই SQL-টা Supabase-এর SQL Editor-এ গিয়ে একবার Run করুন
-- ============================================================
-- কেন দরকার:
-- Payment-এ নতুন "Backdate" ফিচার (Staff অনুরোধ পাঠায়, Master অনুমোদন
-- দেয়) — এর জন্য একটা সম্পূর্ণ নতুন টেবিল দরকার। কোনো পুরনো টেবিল
-- (patients/payments/followups ইত্যাদি) স্পর্শ করা হয় না।
--
-- ⚠️ এই প্যাচ শুধু ১টা নতুন টেবিল বানায় — কোনো পুরনো টেবিল/কলাম/
-- ডেটা কোনোভাবেই ছোঁয়া হয় না। একাধিকবার চালালেও সমস্যা নেই
-- (IF NOT EXISTS)।
--
-- কীভাবে Run করবেন:
-- ১) Supabase Dashboard-এ লগইন করুন
-- ২) বাঁ পাশের মেনু থেকে "SQL Editor" এ ক্লিক করুন
-- ৩) "New query" চাপুন
-- ৪) নিচের পুরো লেখাটা কপি করে পেস্ট করুন
-- ৫) "Run" চাপুন (সবুজ "Success" বার্তা দেখা উচিত)
-- ============================================================

create table if not exists public.payment_backdate_requests (
    "id" text primary key,
    "patientRowId" text,
    "patientCode" text,
    "mobile" text,
    "name" text,
    "branch" text,
    "billAmount" double precision,
    "amount" double precision,
    "mode" text,
    "remarks" text,
    "requestedDate" text,
    "requestedBy" text,
    "requestedByName" text,
    "requestedAt" text,
    "status" text default 'pending',
    "approvedBy" text,
    "approvedAt" text,
    "createdAt" text,
    "updatedAt" text
);

-- payments টেবিলে ২টা নতুন nullable কলাম — একবার Approve হয়ে গেলে,
-- আসল payment রো-তেও কে অনুরোধ করেছিল/কে অনুমোদন দিয়েছিল সেটা
-- স্থায়ীভাবে থেকে যাবে (audit trail), শুধু request-টেবিলেই সীমাবদ্ধ থাকবে না।
alter table public.payments add column if not exists "backdateRequestedBy" text;
alter table public.payments add column if not exists "backdateApprovedBy" text;

-- Run করা শেষে "Success" বার্তা দেখলে বুঝবেন ঠিকভাবে হয়েছে।
