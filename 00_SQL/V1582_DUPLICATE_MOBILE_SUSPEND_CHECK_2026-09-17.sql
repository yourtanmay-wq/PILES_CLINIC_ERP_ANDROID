-- শুধু দেখার জন্য (SELECT) -- কিচ্ছু বদলায় না, শুধু পড়া হয়।
-- নতুন সন্দেহ (কোডে যাচাই করে): AMIT GOLDAR আসলে সাসপেন্ড নন (আগেই দেখা
-- গেছে), কিন্তু অ্যাপ পেমেন্টের আগে যে ফাংশন দিয়ে "সাসপেন্ড কিনা" জিজ্ঞাসা
-- করে (`suspended_until_for`), সেটা মোবাইল-নম্বর মিলিয়ে `limit 1` করে,
-- কোনো ক্রম (ORDER BY) ছাড়াই। hr.staff_profiles-এ যদি একই মোবাইল নম্বরে
-- (অঙ্ক মিলিয়ে) একাধিক সারি থাকে (যেমন কোনো পুরনো/ডুপ্লিকেট স্টাফ-রেকর্ড),
-- তাহলে এই ফাংশন ভুল করে সেই অন্য সারির suspended_until ফিরিয়ে দিতে পারে --
-- ঠিক আজ সকালের মাস্টার-পরিচয়ের বাগের মতোই একই ধরনের কারণ। AMIT GOLDAR-এর
-- মোবাইল নম্বরে (বা অন্য যে কারো) এরকম ডুপ্লিকেট আছে কিনা এখানে দেখা হচ্ছে।

-- ১) AMIT GOLDAR-এর নিজের সারি -- তাঁর আসল মোবাইল নম্বর দেখার জন্য
select person_code, full_name, "link_mobile" as mobile, branch, role_kind, active, suspended_until
from hr.staff_profiles
where full_name ilike '%AMIT%GOLDAR%' or full_name ilike '%GOLDAR%AMIT%';

-- ২) গোটা hr.staff_profiles-এ যেসব মোবাইল নম্বর (শেষ ১০ অঙ্ক মিলিয়ে) একাধিক
--    সারিতে আছে -- এটাই আসল সন্দেহের জায়গা
select
  right(regexp_replace(coalesce("link_mobile",''),'\D','','g'),10) as mobile_digits,
  count(*) as row_count,
  array_agg(person_code order by person_code) as person_codes,
  array_agg(full_name order by person_code) as names,
  array_agg(suspended_until order by person_code) as suspended_untils
from hr.staff_profiles
group by 1
having count(*) > 1
order by row_count desc;
