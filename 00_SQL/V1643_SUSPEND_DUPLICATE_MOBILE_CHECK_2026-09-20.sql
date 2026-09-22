-- শুধু দেখার জন্য (SELECT) — কিছু বদলায় না, শুধু পড়া হয়।
--
-- TK-র প্রশ্ন (২০.০৯.২০২৬): "কাউকে সাসপেন্ড করা হলে বাস্তবে সে সাসপেন্ড হয়
-- না কেন?" — অ্যাপ যখন লগইনে/কাজের সময় "সে সাসপেন্ড কিনা" জিজ্ঞাসা করে,
-- সেটা করে public.suspended_until_for(মোবাইল) দিয়ে। কোডে দেখা গেছে এই
-- ফাংশন hr.staff_profiles-এ মোবাইল-নম্বর মিলিয়ে "limit 1" করে, কোনো
-- ORDER BY ছাড়াই। একই মোবাইল নম্বরে (শেষ ১০ অঙ্ক মিলিয়ে) যদি একাধিক সারি
-- থাকে (পুরনো/ডুপ্লিকেট স্টাফ-রেকর্ড), তাহলে ডেটাবেস যেকোনো একটা সারি
-- ফিরিয়ে দিতে পারে — এমনকি সাসপেন্ড-করা সারিটা না হয়ে অন্য সারিও আসতে
-- পারে, তখন সাসপেন্ড করলেও বাস্তবে কাজ করবে না।
--
-- এই SQL শুধু দেখাবে গোটা hr.staff_profiles-এ এরকম ডুপ্লিকেট মোবাইল আছে
-- কিনা, আর থাকলে ঠিক কার কার — তাহলে বলা যাবে এটাই আসল কারণ কিনা।

-- ১) গোটা hr.staff_profiles-এ যেসব মোবাইল নম্বর (শেষ ১০ অঙ্ক মিলিয়ে)
--    একাধিক সারিতে আছে
select
  right(regexp_replace(coalesce(link_mobile,''),'\D','','g'),10) as mobile_digits,
  count(*) as row_count,
  array_agg(person_code order by person_code) as person_codes,
  array_agg(full_name order by person_code) as names,
  array_agg(branch order by person_code) as branches,
  array_agg(active order by person_code) as actives,
  array_agg(suspended_until order by person_code) as suspended_untils
from hr.staff_profiles
group by 1
having count(*) > 1
order by row_count desc;

-- ২) J.H MANDAL-এর নিজের সারি(গুলো) ও তাঁর মোবাইলে ফাংশনটা সত্যিই কী
--    ফেরায় — অ্যাপ ঠিক যা করে হুবহু তাই
select person_code, full_name, link_mobile as mobile, branch, active, suspended_until
from hr.staff_profiles
where full_name ilike '%J%H%MANDAL%' or full_name ilike '%MANDAL%J%H%';

select public.suspended_until_for(
  (select right(regexp_replace(coalesce(link_mobile,''),'\D','','g'),10)
     from hr.staff_profiles where full_name ilike '%J.H MANDAL%' limit 1)
) as suspended_until_result;
