-- পড়া-মাত্র যাচাই — কিছুই বদলায় না।
-- TK-র প্রশ্ন (১৯.০৯.২০২৬): GANESH CHANDRA ROY (JPE-02062026-001, 16th Visit)-এর
-- Check-up পর্দায় "History & Previous" ধাপ খুললেই ফাঁকা/সবুজ থাকা উচিত ছিল
-- (আগেই সম্পূর্ণ হয়ে গেছে বলে), কিন্তু খোলা/সাধারণ অবস্থায় দেখা যাচ্ছিল।
-- যাচাই: এই রোগীর doctorFullNote ঘরে সত্যিই আগে কোনো সম্পূর্ণ Doctor
-- Checkup জমা আছে কিনা।

select
  "patientId"      as "রোগীর কোড",
  "name"           as "নাম",
  "mobile"         as "মোবাইল",
  case
    when "doctorFullNote" is null or trim("doctorFullNote"::text) in ('', '{}', 'null') then 'ফাঁকা — আগে কোনো সম্পূর্ণ Doctor Checkup সেভ হয়নি'
    else 'ভরা — আগের একটা সম্পূর্ণ Doctor Checkup আছে'
  end as "doctorFullNote অবস্থা",
  "doctorComplete" as "doctorComplete",
  length(coalesce("doctorFullNote"::text, '')) as "doctorFullNote-এর দৈর্ঘ্য",
  "updatedAt"      as "সবশেষ বদল"
from public.patients
where "patientId" = 'JPE-02062026-001'
   or ("mobile" = '9832620422' and "name" ilike 'GANESH%');
