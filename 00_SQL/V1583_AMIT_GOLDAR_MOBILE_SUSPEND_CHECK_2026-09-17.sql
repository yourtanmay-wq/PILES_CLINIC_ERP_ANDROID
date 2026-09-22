-- শুধু দেখার জন্য (SELECT) -- কিচ্ছু বদলায় না, শুধু পড়া হয়।
-- এতক্ষণ নাম দিয়ে খুঁজছিলাম, কিন্তু অ্যাপ যেই ফাংশন দিয়ে "সাসপেন্ড কিনা"
-- জিজ্ঞাসা করে (suspended_until_for) সেটা নাম দিয়ে নয়, **মোবাইল নম্বর**
-- দিয়ে মেলায়। AMIT GOLDAR-এর আসল লগইন-মোবাইল (+919046366596) দিয়ে এবার
-- সরাসরি সেই একই ফাংশনটাই ডেকে দেখা হচ্ছে -- অ্যাপ ঠিক যা করে হুবহু তাই।

-- ১) hr.staff_profiles-এ এই মোবাইলে (অঙ্ক মিলিয়ে) কোন সারি(গুলো) আছে
select person_code, full_name, "link_mobile" as mobile, branch, role_kind, active, suspended_until
from hr.staff_profiles
where right(regexp_replace(coalesce("link_mobile",''),'\D','','g'),10) = '9046366596';

-- ২) অ্যাপ যেই ফাংশনটা সরাসরি ডাকে, সেটাই এখানে ডাকা হচ্ছে -- আসল উত্তর
select public.suspended_until_for('9046366596') as suspended_until_result;
