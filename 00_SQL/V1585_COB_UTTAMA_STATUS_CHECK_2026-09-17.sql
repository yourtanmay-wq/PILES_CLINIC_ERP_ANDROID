-- শুধু দেখার জন্য (SELECT) -- কিচ্ছু বদলায় না, শুধু পড়া হয়।
-- COB-UTTAMA (কোচবিহার) বলছেন তাঁর এনকোয়ারিতে ওভারডিউ ছিল না, অথচ এখন
-- দেখাচ্ছে -- পুরনো "Last call"/"Next call" তারিখ রয়ে গেছে (০৪/০৯,
-- ১১/০৯, ১৩/০৯), যেন তাঁর করা নতুন কল/রিমার্ক সার্ভারে জমাই পড়েনি।
-- সন্দেহ: এটাও AMIT GOLDAR-এর মতোই লেখা-আটকে-যাওয়া সমস্যার আরেকটা
-- উদাহরণ হতে পারে -- তাঁর অ্যাকাউন্টও সাসপেন্ড কিনা যাচাই করা হচ্ছে।

select person_code, full_name, "link_mobile" as mobile, branch, role_kind, active, suspended_until
from hr.staff_profiles
where person_code ilike '%UTTAMA%' or full_name ilike '%UTTAMA%';
