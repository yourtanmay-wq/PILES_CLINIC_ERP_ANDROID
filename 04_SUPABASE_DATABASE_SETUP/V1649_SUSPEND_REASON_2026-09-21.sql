-- 🔴🔒 V1649 (২১.০৯.২০২৬, TK-নির্দেশ)
--
-- TK: "FLK-1 কে সাসপেন্ড করে দেওয়া হয়েছে কিন্তু সে বুঝতেই পারছে না... তবে
--      সাসপেন্ড কি কারনে করা হয়েছে সেটার জন্য সে বুঝতে পারে।" পরে: "ফাঁকা
--      রাখলেও চলবে, তবে সেই ক্ষেত্রে স্টাফ বার্তা দেখবে কিছু লেখা হয়নি।"
--
-- এই SQL শুধু সাসপেন্ডের "কারণ" লেখা ও পড়ার জন্য — সাসপেন্ড হওয়া/না-হওয়ার
-- আসল নিয়ম (`suspended_until_for`) এক অক্ষরও ছোঁয়া হয়নি, ⛔ পুরনো অ্যাপ/ওয়েব
-- (এই আপডেটের আগের বিল্ড) আগের মতোই ঠিক চলবে — শুধু কারণটা দেখাবে না, যেটা
-- আগেও দেখাতো না। নতুন একটা আলাদা ফাংশনে কারণ পড়া হয়, তাই পুরনো কোনো কলে
-- ভাঙার ঝুঁকি নেই।

-- ১) নতুন ঘর — ফাঁকা থাকতে পারে
alter table hr.staff_profiles add column if not exists suspend_reason text;

-- ২) কারণ পড়ার ফাংশন (suspended_until_for-এর হুবহু একই প্যাটার্ন ও নিরাপত্তা)
create or replace function public.suspend_reason_for(p_mobile text) returns text
  language sql stable security definer set search_path = public, hr as $$
  select suspend_reason
    from hr.staff_profiles
   where right(regexp_replace(coalesce(link_mobile,''),'\D','','g'),10)
       = right(regexp_replace(coalesce(p_mobile,''),'\D','','g'),10)
   limit 1;
$$;
revoke all on function public.suspend_reason_for(text) from public;
grant execute on function public.suspend_reason_for(text) to anon, authenticated;

-- শুধু-পড়া যাচাই (চালানোর দরকার নেই, শুধু TK নিজে চোখে দেখতে চাইলে):
-- select public.suspend_reason_for('মোবাইল নম্বর');
