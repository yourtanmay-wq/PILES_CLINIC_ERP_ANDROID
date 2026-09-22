-- শুধু দেখার জন্য (SELECT) -- কিচ্ছু বদলায় না, শুধু পড়া হয়।
-- আজকের সমস্যা: AMIT GOLDAR (Doctor, Kishanganj)-এর ফোনে একটা টাকার লেখা
-- ব্যর্থ হয়েছে ("Failed -- check your connection"), অথচ ইন্টারনেট ভালো,
-- আর মাস্টারের কাছে কোনো অনুরোধও আসেনি -- মানে লেখাটা কোথাও জমাই হয়নি।
-- কোডে যাচাই করে পাওয়া গেছে: গুরুত্বপূর্ণ কিছু লেখার ঠিক আগে অ্যাপ নিজে
-- থেকেই সার্ভারে জিজ্ঞাসা করে "এই স্টাফ কি সাসপেন্ড/বাদ দেওয়া আছেন" --
-- থাকলে লেখাটা নিঃশব্দে আটকে যায় (কোনো সারিও জমা থাকে না, রিট্রাইও হয় না)।
-- সন্দেহ: কিছুদিন আগে ৫ জন স্টাফকে সাসপেন্ড করে আবার চালু করা হয়েছিল
-- (পাসওয়ার্ড-কাজের অংশ), সেটা সবার ক্ষেত্রে ঠিকভাবে "আবার চালু" হয়েছিল
-- কিনা তখন যাচাই করা হয়নি। AMIT GOLDAR এখনো "সাসপেন্ড" অবস্থায় রয়ে
-- গেছেন কিনা এখানে দেখা হচ্ছে -- আন্দাজ নয়।

-- ১) AMIT GOLDAR নির্দিষ্ট করে
select person_code, full_name, "link_mobile" as mobile, branch, active, suspended_until
from hr.staff_profiles
where full_name ilike '%AMIT%GOLDAR%' or full_name ilike '%GOLDAR%AMIT%';

-- ২) আগের ১৬.০৯.২০২৬-এর সেই ৫ জন (V1558) -- তখনকার সাসপেন্ড-উঠানো কাজ
--    সত্যিই সবার ক্ষেত্রে ঠিকভাবে হয়েছিল কিনা, তখন ফলাফল যাচাই করাই হয়নি
select person_code, full_name, "link_mobile" as mobile, branch, active, suspended_until
from hr.staff_profiles
where "link_mobile" in ('8167096595','8436002200','8514002200','9002003540','9002610352');

-- ৩) গোটা staff_profiles-এ আজও যাঁরা "বন্ধ" (active=false বা suspended_until আজ/ভবিষ্যতে) --
--    যাতে শুধু AMIT GOLDAR বা ওই ৫ জন নয়, একই সমস্যায় আটকে থাকা বাকি যে কেউও ধরা পড়ে
select person_code, full_name, "link_mobile" as mobile, branch, active, suspended_until
from hr.staff_profiles
where active is false
   or (suspended_until is not null and suspended_until >= current_date)
order by full_name;
