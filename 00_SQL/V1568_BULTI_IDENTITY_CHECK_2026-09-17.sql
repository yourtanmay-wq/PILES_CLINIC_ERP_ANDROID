-- শুধু দেখার জন্য (SELECT) -- BULTI SINGHA (COB-4, মোবাইল 7501256248) Staff
-- Profiles খুললে বারবার "signing in" ধাপ দেখাচ্ছে (TK-র ভিডিও, ১৭.০৯.২০২৬)।
-- সন্দেহ: hr.app_identity-তে তাঁর person_code আসল কোড ("COB-4") হিসেবে বসানো
-- আছে, অথচ অ্যাপ তাঁকে নামে ("BULTI SINGHA") চেনে -- এই দুটো না মিললেই
-- প্রতিবার নতুন করে সাইন-ইন হয়। যাচাই না করে অনুমান করে বদল করা হচ্ছে না।
-- কিচ্ছু বদলায় না, শুধু পড়া হয়।
select uid, person_code, link_mobile, role_kind, is_master, created_at
from hr.app_identity
where link_mobile = '7501256248'
   or person_code ilike '%bulti%'
   or person_code ilike 'cob-4';
