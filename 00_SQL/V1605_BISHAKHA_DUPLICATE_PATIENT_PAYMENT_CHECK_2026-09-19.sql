-- পড়া-মাত্র যাচাই — কিছুই বদলায় না।
-- TK-র প্রশ্ন (১৯.০৯.২০২৬): BISHAKHA MANDAL-এর দুটো আলাদা রেজিস্ট্রেশন
-- (COB-11092026-002 ও COB-11092026-006, দুটো আলাদা মোবাইল নম্বর দিয়ে) —
-- ডাক্তার ₹20,000 নিয়েছেন, কিন্তু কোন আইডির বিলে বসেছে সেটা যাচাই।

select
  p."patientId"      as "রোগীর কোড",
  p."id"             as "ভিতরের আইডি",
  p."name"           as "নাম",
  p."mobile"         as "মোবাইল",
  p."bill"           as "মোট বিল",
  pay."payLabel"     as "পেমেন্টের ধরন",
  pay."amount"       as "টাকা",
  pay."mode"         as "মোড",
  pay."date"         as "তারিখ",
  pay."createdAt"    as "কখন বসেছে",
  pay."receivedBy"   as "কে নিয়েছেন"
from public.patients p
left join public.payments pay
  on pay."patientId" = p."id"
 and pay."payType" = 'treatment'
 and pay."date" = '2026-09-19'
where p."patientId" in ('COB-11092026-002', 'COB-11092026-006')
   or p."mobile" in ('7865073750', '8670104488')
order by p."patientId", pay."createdAt";
