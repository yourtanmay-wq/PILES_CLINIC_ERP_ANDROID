-- ============================================================
-- প্যাচ — এই SQL-টা Supabase-এর SQL Editor-এ গিয়ে একবার Run করুন
-- ============================================================
-- কেন দরকার:
-- Enquiry Follow-up Card-এ ছোট করে "Official Time" / "Unexpected Time"
-- ব্যাজ দেখানোর জন্য অ্যাপ এখন followups টেবিলেও "timeType" লেখে।
-- আসল Database-এ followups টেবিলে এই কলামটা আগে থেকে নাও থাকতে পারে।
--
-- এই প্যাচ শুধু একটা নতুন nullable কলাম যোগ করে, তারপর পুরোনো
-- followups রেকর্ডগুলোতে (যাদের timeType ফাঁকা) তাদের linked enquiry
-- থেকে timeType নিরাপদে বসিয়ে দেয় (backfill)।
--
-- ⚠️ এই প্যাচ কোনো টেবিল drop করে না, কোনো কলাম rename করে না,
-- কোনো রেকর্ড delete করে না। শুধু নতুন কলাম যোগ + ফাঁকা মান পূরণ।
-- একাধিকবার চালালেও কোনো সমস্যা নেই (IF NOT EXISTS + শুধু ফাঁকা
-- রেকর্ডেই backfill)।
--
-- কীভাবে Run করবেন:
-- ১) Supabase Dashboard-এ লগইন করুন
-- ২) বাঁ পাশের মেনু থেকে "SQL Editor" এ ক্লিক করুন
-- ৩) "New query" চাপুন
-- ৪) নিচের পুরো লেখাটা কপি করে পেস্ট করুন
-- ৫) "Run" চাপুন (সবুজ "Success" বার্তা দেখা উচিত)
-- ============================================================

-- ধাপ ১: নতুন nullable কলাম যোগ (আগে থাকলে কিছু হবে না)
alter table public.followups add column if not exists "timeType" text;

-- ধাপ ২: পুরোনো followups রেকর্ডে timeType backfill করা হয় তাদের
--         linked enquiry (followups."refId" = enquiries."id") থেকে।
--         শুধু সেই রেকর্ডগুলোতেই বসে যেখানে timeType এখনো ফাঁকা/NULL,
--         আর enquiry-তে timeType আছে। বাকি কিছু ছোঁয়া হয় না।
update public.followups f
set "timeType" = e."timeType"
from public.enquiries e
where f."refId" = e."id"
  and (f."timeType" is null or f."timeType" = '')
  and e."timeType" is not null
  and e."timeType" <> '';

-- Run করা শেষে "Success" বার্তা দেখলে বুঝবেন ঠিকভাবে হয়েছে।
