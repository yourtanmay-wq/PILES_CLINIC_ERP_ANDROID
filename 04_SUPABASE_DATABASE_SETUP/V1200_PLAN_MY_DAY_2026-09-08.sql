-- ============================================================
-- প্যাচ — এই SQL-টা Supabase-এর SQL Editor-এ গিয়ে একবার Run করুন
-- ============================================================
-- কেন দরকার (V1200, ০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ):
--   • "work from home / work on another branch — স্টাফ এরকম যেন আগে থেকে
--      বিবেচনা করে নিতে পারে, আর সেই ক্ষেত্রে ৭ ঘন্টা ধরা হবে"
--   • অন্য ব্রাঞ্চের ডিউটি — "শুধু জানিয়ে রাখলেই হবে" (অনুমতি লাগবে না)
--
-- দুটো জায়গায় ঘর যোগ হয় —
--   public.wfh_requests : kind ('wfh' / 'branch') ও toBranch (কোন ব্রাঞ্চে)
--   wn.notebook_days    : is_other_branch (ওই দিন অন্য ব্রাঞ্চের ডিউটি)
--
-- ⚠️ শুধু নতুন কলাম যোগ হয় — পুরনো কোনো ঘর/সারি/ডেটা ছোঁয়া হয় না।
-- একাধিকবার চালালেও কোনো সমস্যা নেই।
--
-- কীভাবে Run করবেন:
-- ১) Supabase Dashboard → SQL Editor → New query
-- ২) নিচের পুরো লেখাটা কপি-পেস্ট করে Run (সবুজ "Success" দেখা উচিত)
-- ============================================================

alter table public.wfh_requests
    add column if not exists "kind" text default 'wfh';

alter table public.wfh_requests
    add column if not exists "toBranch" text default '';

alter table wn.notebook_days
    add column if not exists "is_other_branch" boolean default false;

-- Run করা শেষে "Success" বার্তা দেখলে বুঝবেন ঠিকভাবে হয়েছে।
