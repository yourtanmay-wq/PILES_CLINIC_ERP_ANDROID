-- ============================================================
-- প্যাচ — Supabase-এর SQL Editor-এ (আসল/পুরনো account-এ) একবার Run করুন
-- ============================================================
-- কেন দরকার (V605, ২৪.০৮.২০২৬, TK-নির্দেশ):
-- Incoming/Outgoing দুই ধরনের কলেই রিমার্কস লেখার জায়গা — কল চলাকালীন ও
-- কল-শেষে দুটোতেই নোটিফিকেশন থেকে সরাসরি লেখা যাবে।
--
-- ⚠️ এই প্যাচ শুধু ১টা নতুন টেবিল বানায় — কোনো পুরনো টেবিল/কলাম/ডেটা
-- কোনোভাবেই ছোঁয়া হয় না (dialer_calls-সহ)। একাধিকবার চালালেও সমস্যা
-- নেই (IF NOT EXISTS)। বাকি সব টেবিলের মতো একই কনভেনশনে (public schema,
-- text id, RLS নেই — app-এর anon key দিয়েই অ্যাক্সেস হয়)।
--
-- কীভাবে Run করবেন:
-- ১) Supabase Dashboard-এ লগইন করুন (আসল/পুরনো account)
-- ২) বাঁ পাশের মেনু থেকে "SQL Editor" এ ক্লিক করুন
-- ৩) "New query" চাপুন
-- ৪) নিচের পুরো লেখাটা কপি করে পেস্ট করুন
-- ৫) "Run" চাপুন (সবুজ "Success" বার্তা দেখা উচিত)
-- ============================================================

create table if not exists public.call_remarks (
    "id" text primary key,
    "mobile" text,               -- যে নম্বর, শেষ ১০ অঙ্ক
    "direction" text,            -- "incoming" / "outgoing"
    "remark" text,
    "patientId" text,            -- মিললে রোগীর/followups-এর id (না মিললে খালি)
    "staffMobile" text,
    "staffName" text,
    "branch" text,
    "calledAt" text,             -- কলের সময়
    "createdAt" text,
    "updatedAt" text
);

-- একই নম্বরের সাম্প্রতিক রিমার্কস দ্রুত বার করার জন্য।
create index if not exists idx_call_remarks_mobile_date
    on public.call_remarks ("mobile", "calledAt");

-- Run করা শেষে "Success" বার্তা দেখলে বুঝবেন ঠিকভাবে হয়েছে।
