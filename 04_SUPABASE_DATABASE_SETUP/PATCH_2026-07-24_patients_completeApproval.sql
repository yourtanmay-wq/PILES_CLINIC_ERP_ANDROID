-- ============================================================
-- প্যাচ — এই SQL-টা Supabase-এর SQL Editor-এ গিয়ে একবার Run করুন
-- ============================================================
-- কেন দরকার:
-- Patient/Treatment কার্ডে নতুন "Complete despite Due" ফিচার (Staff
-- অনুরোধ পাঠায়, Master অনুমোদন দেয়) — এর জন্য patients টেবিলে ২টা
-- নতুন কলাম দরকার। আসল Database-এ এই কলাম দুটো আগে থেকে নেই।
--
-- ⚠️ এই প্যাচ কোনো টাকার হিসাব (bill/paid/due) ছোঁয় না — শুধু ২টা
-- নতুন খালি (nullable) কলাম যোগ করে। কোনো টেবিল drop হয় না, কোনো
-- কলাম rename হয় না, কোনো রেকর্ড delete হয় না, কোনো পুরোনো ডেটা
-- বদলায় না। একাধিকবার চালালেও কোনো সমস্যা নেই (IF NOT EXISTS)।
--
-- কীভাবে Run করবেন:
-- ১) Supabase Dashboard-এ লগইন করুন
-- ২) বাঁ পাশের মেনু থেকে "SQL Editor" এ ক্লিক করুন
-- ৩) "New query" চাপুন
-- ৪) নিচের পুরো লেখাটা কপি করে পেস্ট করুন
-- ৫) "Run" চাপুন (সবুজ "Success" বার্তা দেখা উচিত)
-- ============================================================

-- Staff যখন "Complete করার অনুরোধ পাঠান" চাপে, তার মোবাইল নম্বর এখানে বসে।
-- Master Approve/Reject করলে আবার খালি হয়ে যায়।
alter table public.patients add column if not exists "completeRequestedBy" text;

-- Master যখন Approve করে, তার মোবাইল নম্বর এখানে বসে। এটা সেট থাকলেই
-- অ্যাপ এই পেশেন্টকে Draft-এর "Complete Patient" লিস্টে দেখায় এবং
-- Follow-up কল/রিমাইন্ডার থেকে বাদ দেয় — আসল bill/paid/due কলাম
-- অপরিবর্তিতই থাকে, Reports-এ ঠিক টাকাই দেখাবে।
alter table public.patients add column if not exists "completeApprovedBy" text;

-- Run করা শেষে "Success" বার্তা দেখলে বুঝবেন ঠিকভাবে হয়েছে।
