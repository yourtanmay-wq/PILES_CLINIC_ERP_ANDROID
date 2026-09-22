-- 🔍 READ-ONLY — শুধু দেখার জন্য, কোনো টাকা/ডেটা বদলাবে না
-- TARIK ANWAR ও DHARAM KUMAR-এর কমিশন কেন স্বয়ংক্রিয়ভাবে বাঁধা হয়নি (অথচ
-- এই স্ক্রিন খোলার সময় প্রতিবারই fin.rmp_autolink_refdoctor স্বয়ংক্রিয়ভাবে
-- চলে) — তার আসল কারণ যাচাই। সবচেয়ে বড় সন্দেহ: AMIT GOLDAR নামে/নম্বরে
-- একাধিক RMP-সারি (doctor_visits) থাকলে, ওই ফাংশন নিজে থেকেই "AMBIGUOUS"
-- ধরে বাদ দিয়ে দেয় (কোনো ভুল বার্তাও দেখায় না)।
select id, "name", "mobile", "branch", "status"
from public.doctor_visits
where lower(trim(coalesce("name",''))) = 'amit goldar'
   or right(regexp_replace(coalesce("mobile",''),'\D','','g'),10) = '9046366596';
