-- 🔵 শুধু-পড়ার (READ-ONLY) ডায়াগনস্টিক — কিছু বদলায়/মোছে না।
-- আগের ফলাফলে ৪৫ জনের মধ্যে ৪৪ জনের app_paid ঠিক 0 এসেছে — এটা সত্যিই
-- "টাকা অ্যাপে নেই" নাকি আমার আগের SQL-এর branch/mobile মেলানোর নিয়মেই
-- ভুল ছিল, সেটা নিশ্চিত না হয়ে TK-কে রেড অ্যালার্ট দেওয়া ঠিক হবে না
-- (নিয়ম ৫ক)। এই তিনটে প্রশ্নের ফল পাঠালেই আসল কারণ বলা যাবে।

-- ১) payments টেবিলে branch ঘরে ঠিক কী কী বানান/মান আছে
select branch, count(*) as rows
from public.payments
group by branch
order by rows desc;

-- ২) SAJAL SARKAR-এর নম্বর (9635015302) — ব্রাঞ্চ না ধরেই payments-এ খোঁজা
select id, "patientId", mobile, branch, amount, date
from public.payments
where mobile like '%9635015302%';

-- ৩) একই নম্বর patients টেবিলে আছে কিনা (রোগীর তথ্যই ঢোকেনি কিনা)
select id, "patientId", mobile, branch, name, stage
from public.patients
where mobile like '%9635015302%';
