-- ═══════════════════════════════════════════════════════════════════
-- RLS বড় কাজের ধাপ ১ (তরতাজা দিন, ১৭.০৯.২০২৬) — শুধু প্রস্তুতি, আচরণ বদলায় না
-- ═══════════════════════════════════════════════════════════════════
-- এই SQL শুধু hr.app_identity-তে একটা নতুন "branch" ঘর যোগ করছে আর
-- StaffDirectory.kt-এর আসল (২৩ জনের) তালিকা মিলিয়ে ভরে দিচ্ছে।
-- ⛔ এখনো কোনো নীতি (policy)/লগইন/অ্যাপের কোড এই ঘরটা পড়ছে না -- তাই
--    এই মুহূর্তে অ্যাপের কোনো আচরণ এক চুলও বদলাবে না, সম্পূর্ণ নিরাপদ ও
--    উল্টে ফেরানো যায় (`alter table hr.app_identity drop column branch;`)।
-- এটাই পরের ধাপে (patients/payments-এর RLS) "কে কোন ব্রাঞ্চ দেখতে পারবে"
-- সেই নিয়ম বসানোর ভিত্তি হবে।

alter table hr.app_identity add column if not exists branch text;

-- StaffDirectory.kt (২৩ জন) থেকে হুবহু মিলিয়ে -- mobile শেষ ১০ ডিজিট ধরে (নিরাপদ মিল)
update hr.app_identity set branch = 'All' where right(link_mobile, 10) = '8001080080';        -- TK BISWAS (master)
update hr.app_identity set branch = 'Kishanganj' where right(link_mobile, 10) = '9883605917';  -- KNE-LAXMI
update hr.app_identity set branch = 'Kishanganj' where right(link_mobile, 10) = '8676002200';  -- KNE-BRANCH
update hr.app_identity set branch = 'Kishanganj' where right(link_mobile, 10) = '7482966958';  -- KNE-KISHAN10
update hr.app_identity set branch = 'Jalpaiguri' where right(link_mobile, 10) = '9647840067';  -- JPE-CRP
update hr.app_identity set branch = 'Jalpaiguri' where right(link_mobile, 10) = '8101397763';  -- JPE-JALPAI-13
update hr.app_identity set branch = 'Jalpaiguri' where right(link_mobile, 10) = '8167096595';  -- JPE-RUPAM
update hr.app_identity set branch = 'Jalpaiguri' where right(link_mobile, 10) = '8436002200';  -- JPE-BRANCH
update hr.app_identity set branch = 'Cooch Behar' where right(link_mobile, 10) = '7679751521'; -- COB-UTTAMA
update hr.app_identity set branch = 'Cooch Behar' where right(link_mobile, 10) = '7501256248'; -- COB-4
update hr.app_identity set branch = 'Cooch Behar' where right(link_mobile, 10) = '8514002200'; -- COB-BRANCH
update hr.app_identity set branch = 'Cooch Behar' where right(link_mobile, 10) = '9883884394'; -- COB-ARMAN
update hr.app_identity set branch = 'Falakata' where right(link_mobile, 10) = '9883623823';    -- FLK-1
update hr.app_identity set branch = 'Falakata' where right(link_mobile, 10) = '8514001100';    -- FLK-BRANCH
update hr.app_identity set branch = 'Birpara' where right(link_mobile, 10) = '8538002200';     -- BIR-BRANCH
update hr.app_identity set branch = 'Cooch Behar' where right(link_mobile, 10) = '7980993652'; -- Dr. K.H MANDAL
update hr.app_identity set branch = 'Jalpaiguri' where right(link_mobile, 10) = '8001800148';  -- Dr. JAY BANIK
update hr.app_identity set branch = 'Kishanganj' where right(link_mobile, 10) = '9046366596';  -- AMIT GOLDAR
update hr.app_identity set branch = 'Cooch Behar' where right(link_mobile, 10) = '7479173399'; -- J.H MANDAL
update hr.app_identity set branch = 'Cooch Behar' where right(link_mobile, 10) = '9002610352'; -- GOKUL
update hr.app_identity set branch = 'Falakata' where right(link_mobile, 10) = '7810907954';    -- Dr. SAIKAT ROY
update hr.app_identity set branch = 'Birpara' where right(link_mobile, 10) = '9242009205';     -- Dr. PRANAB BISWAS
update hr.app_identity set branch = 'All' where right(link_mobile, 10) = '9002003540';         -- Field Officer

-- যাচাই (শুধু দেখার জন্য) -- কতজনের branch এখনো ফাঁকা রয়ে গেল (নতুন কেউ যোগ
-- হয়ে থাকলে, বা উপরের ২৩ জনের বাইরে কেউ থাকলে) -- এটা শুধু তথ্য, কিছু বদলায় না।
select uid, person_code, link_mobile, role_kind, branch
from hr.app_identity
where branch is null;
