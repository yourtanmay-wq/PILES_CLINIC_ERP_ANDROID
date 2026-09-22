-- ============================================================
-- প্যাচ — এই SQL-টা Supabase-এর SQL Editor-এ গিয়ে একবার Run করুন
-- ============================================================
-- কেন দরকার:
-- Payment Amount Edit-এর নতুন নিয়ম (TK-REQUESTED, 2026-07-25):
-- Payment-এর দিন + তার পরের দিন পর্যন্ত যে কেউ (স্টাফ/Master) সরাসরি
-- Amount ঠিক করতে পারবে। তার পরে ঠিক করতে হলে Master-এর অনুমতি লাগবে
-- (Staff অনুরোধ পাঠাবে, Master Dashboard-এর ঘণ্টা বোতামে দেখে
-- Approve/Reject করবেন) — ঠিক "Backdate Payment"-এর মতোই একই প্যাটার্নে,
-- কিন্তু নতুন Payment না বানিয়ে, আগের Payment-এর Amount বদলানোর জন্য।
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

create table if not exists public.payment_edit_requests (
    "id" text primary key,
    "paymentId" text,
    "patientRowId" text,
    "patientCode" text,
    "mobile" text,
    "name" text,
    "branch" text,
    "oldAmount" double precision,
    "newAmount" double precision,
    "mode" text,
    "paymentDate" text,
    "reason" text,
    "requestedBy" text,
    "requestedByName" text,
    "requestedAt" text,
    "status" text default 'pending',
    "approvedBy" text,
    "approvedAt" text,
    "createdAt" text,
    "updatedAt" text
);

-- payments টেবিলে ২টা নতুন nullable কলাম — Approve হয়ে গেলে আসল payment
-- রো-তেও কে অনুরোধ করেছিল/কে অনুমোদন দিয়েছিল স্থায়ীভাবে থেকে যাবে
-- (audit trail), একই প্যাটার্ন backdateRequestedBy/backdateApprovedBy-এর মতো।
alter table public.payments add column if not exists "editRequestedBy" text;
alter table public.payments add column if not exists "editApprovedBy" text;

-- Run করা শেষে "Success" বার্তা দেখলে বুঝবেন ঠিকভাবে হয়েছে।
