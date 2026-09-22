-- BISHAKHA MANDAL (ভুলে দুটো রেজিস্ট্রেশন) — ডাক্তারের নেওয়া ₹20,000
-- ভুল আইডিতে (COB-002, বিল ফাঁকা) বসে গিয়েছিল। এখন সঠিক আইডিতে
-- (COB-006, বিল ₹27,000) সরানো হলো। প্রমাণ রাখার জন্য আগে ব্যাকআপ।

create table if not exists public._backup_20260919_bishakha_payment as
select * from public.payments
where "patientId" = 'pat_7865073750' and "payType" = 'treatment'
  and "date" = '2026-09-19' and "amount" = '20000';

update public.payments
set "patientId" = 'pat_8670104488'
where "patientId" = 'pat_7865073750'
  and "payType" = 'treatment'
  and "date" = '2026-09-19'
  and "amount" = '20000'
returning "id", "patientId", "name", "amount", "payLabel", "date";

-- যাচাই: এখন দুই আইডির আজকের চিকিৎসার টাকা কেমন দেখাচ্ছে
select p."patientId" as "রোগীর কোড", p."bill" as "মোট বিল",
       coalesce(sum(pay."amount"::numeric), 0) as "আজকের চিকিৎসার জমা"
from public.patients p
left join public.payments pay on pay."patientId" = p."id" and pay."payType" = 'treatment' and pay."date" = '2026-09-19'
where p."id" in ('pat_7865073750', 'pat_8670104488')
group by p."patientId", p."bill";
