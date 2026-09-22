-- আসল কারণ পাওয়া গেছে (AMIT GOLDAR নয়, অন্য একজন) --
-- TK-র চালানো SQL ফলে দেখা গেছে: FIELD-OFFICER (মোবাইল 9002003540) এখনো
-- সাসপেন্ড অবস্থায় আছেন (suspended_until = 2026-09-19, অর্থাৎ আজ ১৭.০৯-এর
-- পরেও ২ দিন বাকি) -- এটাই ১৬.০৯.২০২৬-এর সেই ৫ জনের কাজে বাকি থেকে যাওয়া
-- একটা (V1558-এ যাচাই না করা কাজ)। এই একজনের যেকোনো টাকা/হাজিরা-জাতীয়
-- লেখা এখন নিঃশব্দে আটকে যাওয়ার কথা।
-- বাকি ৪ জন (মোবাইল 8167096595, 8436002200, 8514002200, 9002610352) ঠিকই
-- আছেন -- তাঁদের ছোঁয়া হয়নি।
-- ⛔ Kishanganj-এর ৪ জন স্টাফ + SWAPNA ADHIKARI + DR-PK-ROY -- এঁরা সবাই
--    ইচ্ছাকৃতভাবে বাদ দেওয়া/আগে চলে যাওয়া, এই ফিক্সে ছোঁয়া হয়নি।

update hr.staff_profiles
set suspended_until = null
where "link_mobile" = '9002003540'
  and active is true;

-- যাচাই -- ফাঁকা suspended_until, active অপরিবর্তিত (true)
select person_code, full_name, "link_mobile" as mobile, branch, active, suspended_until
from hr.staff_profiles
where "link_mobile" = '9002003540';
