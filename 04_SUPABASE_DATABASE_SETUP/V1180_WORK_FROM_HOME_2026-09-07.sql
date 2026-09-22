-- ============================================================
-- প্যাচ — এই SQL-টা Supabase-এর SQL Editor-এ গিয়ে একবার Run করুন
-- ============================================================
-- কেন দরকার (V1179, ০৭.০৯.২০২৬, TK-নির্দেশ, হুবহু):
-- "তাছাড়া সে যদি Work from Home করতে চায় তার ব্যাবস্থা যেন থাকে এবং
--  মাস্টারের অনুমতি নেওয়া জরুরী Work From Home এর ক্ষেত্রে"
--
-- স্টাফ ক্লিনিকের বাইরে থেকে IN TIME চাপলে "Work From Home" চাইতে পারেন।
-- অনুরোধটা এই টেবিলে বসে, মাস্টার Briefing পর্দা থেকে Approve/Reject করেন।
-- Approve হলে **শুধু ওই দিনের জন্যই** GPS-পাহারা বাদ যায়।
--
-- ⚠️ এই প্যাচ শুধু ১টা নতুন টেবিল বানায় — কোনো পুরনো টেবিল/কলাম/ডেটা
-- কোনোভাবেই ছোঁয়া হয় না। একাধিকবার চালালেও সমস্যা নেই (IF NOT EXISTS)।
-- `backdate_payment_grants` টেবিলের হুবহু একই কনভেনশনে বানানো
-- (public schema, text id, app-এর চেনা পথেই অ্যাক্সেস)।
--
-- ⚠️ এই SQL না চালানো পর্যন্ত: অন্য ব্রাঞ্চে গিয়ে IN TIME দেওয়া (V1179-এর
-- প্রথম অংশ) ঠিকই কাজ করবে — শুধু Work From Home-এর অনুরোধটাই যাবে না।
--
-- কীভাবে Run করবেন:
-- ১) Supabase Dashboard-এ লগইন করুন
-- ২) বাঁ পাশের মেনু থেকে "SQL Editor" এ ক্লিক করুন
-- ৩) "New query" চাপুন
-- ৪) নিচের পুরো লেখাটা কপি করে পেস্ট করুন
-- ৫) "Run" চাপুন (সবুজ "Success" বার্তা দেখা উচিত)
-- ============================================================

create table if not exists public.wfh_requests (
    "id" text primary key,
    "staffMobile" text,
    "staffCode" text,
    "staffName" text,
    "branch" text,
    "workDate" text,
    "reason" text,
    "status" text default 'pending',
    "requestedAt" text,
    "decidedBy" text,
    "decidedByName" text,
    "decidedAt" text
);

-- দ্রুত খোঁজার জন্য: IN TIME চাপার সময় "আজ এই স্টাফের অনুমতি আছে কি না"
-- এই দুটো ঘর ধরেই খোঁজা হয়।
create index if not exists idx_wfh_requests_staff_day
    on public.wfh_requests ("staffMobile", "workDate");

-- মাস্টারের পর্দায় আজকের অপেক্ষমাণ অনুরোধ খোঁজার জন্য।
create index if not exists idx_wfh_requests_day_status
    on public.wfh_requests ("workDate", "status");

-- ============================================================
-- V1180 (০৭.০৯.২০২৬, TK-নির্দেশ, হুবহু):
-- "Work from Home… মাস্টার অনুমতি দিলে সেটা 7 ঘন্টাই হিসাব করা হবে,
--  কম বেশি হিসাবে সেদিনের জন্য হবে না" (সকাল ১০টা–বিকেল ৫টা)।
--
-- তাই দিনের সারিতে একটা চিহ্ন দরকার — নিচের ঘরটা যোগ করে।
-- ⚠️ শুধু **নতুন একটা ঘর** যোগ হয়, পুরনো একটা সারি/ঘরও ছোঁয়া হয় না।
-- ⚠️ এটা না চালালেও হাজিরা ঠিকই চলবে (অ্যাপ ঘরটা বাদ দিয়ে সারি বসায়),
--    শুধু Work From Home দিনটা ৭ ঘণ্টা ধরা হবে না।
-- ============================================================

alter table wn.notebook_days
    add column if not exists "is_wfh" boolean default false;

-- Run করা শেষে "Success" বার্তা দেখলে বুঝবেন ঠিকভাবে হয়েছে।
