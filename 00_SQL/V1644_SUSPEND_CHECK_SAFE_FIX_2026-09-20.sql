-- এটা বদলায় (শুধু একটা ফাংশন — কোনো টেবিল/ডেটা ছোঁয়া হয় না)।
--
-- আসল কারণ (V1643-এ দেখা কোড-দুর্বলতা, ডুপ্লিকেট মোবাইল থাকুক বা না থাকুক
-- এটা নিজেই একটা ঝুঁকি): public.suspended_until_for(মোবাইল) এতদিন
-- "limit 1" করত, কোনো ORDER BY ছাড়া — একই মোবাইলে একাধিক hr.staff_profiles
-- সারি থাকলে PostgreSQL যেকোনো একটা সারি ফেরাতে পারে, ইচ্ছাকৃত ক্রম কিছুই
-- নেই। ফলে সাসপেন্ড করা হলেও সেই সারিটাই না ফিরে অন্য (সাসপেন্ড-না-করা)
-- সারিটা ফিরে আসতে পারে — তখন বাস্তবে সাসপেন্ড কার্যকর হয় না।
--
-- সমাধান: এখন "limit 1"-এর বদলে ওই মোবাইলের **সবকটা** সারি একসাথে দেখে,
-- যেটা সবচেয়ে বেশি আটকানোর কথা (সবচেয়ে দূরের তারিখ) সেটাই ফেরায় —
-- অর্থাৎ কোনো একটা সারি সাসপেন্ড/বাদ-দেওয়া থাকলে সেই মোবাইল আটকে যাবে,
-- কোনোটাই না থাকলে আগের মতোই খোলা থাকবে। এটা নিরাপদ দিকেই ভুল করে
-- (কাউকে ভুলে খুলে রাখে না), লগইন/লেখার আচরণ আর কিছু বদলায়নি।
--
-- ⛔ sql_local_check.py-র নকল ডেটাবেসে `hr` schema বসানো হয় না (স্ক্রিপ্টের
-- নিজস্ব সীমা, PILES_CLINIC_DB_SETUP.sql-এ শুধু `public` schema আছে) —
-- তাই ওই পাহারা এই ফাইলে PASS/FAIL কিছুই বলতে পারবে না। এর বদলে হাতে
-- একটা নকল hr.staff_profiles বানিয়ে (দুই সারি, একই মোবাইল, একটা সাসপেন্ড)
-- আগের ফাংশন ও এই নতুন ফাংশন দুটোই চালিয়ে যাচাই করা হয়েছে — পুরনোটা
-- এই দৃশ্যে সত্যিই ভুল করে "সাসপেন্ড নেই" ফেরত দিয়েছে, নতুনটা ঠিকভাবে
-- সাসপেন্ড-তারিখ ফিরিয়েছে; সাধারণ (ডুপ্লিকেট-ছাড়া) ও বাদ-দেওয়া (removed)
-- কেস দুটোতেও ফল আগের মতোই ঠিক এসেছে।

begin;

create or replace function public.suspended_until_for(p_mobile text) returns date
  language sql stable security definer set search_path = public, hr as $$
  select max(case when coalesce(active, true) = false then date '2999-12-31'
              else suspended_until end)
    from hr.staff_profiles
   where right(regexp_replace(coalesce(link_mobile,''),'\D','','g'),10)
       = right(regexp_replace(coalesce(p_mobile,''),'\D','','g'),10);
$$;
revoke all on function public.suspended_until_for(text) from public;
grant execute on function public.suspended_until_for(text) to anon, authenticated;

commit;

-- ---------------------------------------------------------------------
-- যাচাই (শুধু-দেখা, চাইলে চালান):
-- ১) J.H MANDAL-এর মোবাইলে এখন কী ফেরে (V1643-এর প্রশ্ন ২-এর মতোই):
-- select public.suspended_until_for(
--   (select right(regexp_replace(coalesce(link_mobile,''),'\D','','g'),10)
--      from hr.staff_profiles where full_name ilike '%J.H MANDAL%' limit 1)
-- ) as suspended_until_result;
--
-- ২) সাসপেন্ড-না-করা কারো মোবাইলে আগের মতোই null আসছে কিনা (কিছু ভাঙেনি):
-- select person_code, full_name, suspended_until
--   from hr.staff_profiles where suspended_until is null limit 5;
-- ---------------------------------------------------------------------
