-- পড়া-মাত্র যাচাই — কিছুই বদলায় না।
-- TK-র নির্দেশ (১৯.০৯.২০২৬): গত ১ বছরে RMP TK BISWAS (8001080080)-এর নামে
-- রেফার হওয়া সবগুলো রোগীর তালিকা (যেকোনো ব্রাঞ্চে — যদি কেউ ভুল করে অন্য
-- ব্রাঞ্চে বসে গিয়ে থাকে, সেটাও যেন এখানেই ধরা পড়ে)।

select
  "patientId"         as "রোগীর কোড",
  "name"              as "রোগীর নাম",
  "branch"            as "ব্রাঞ্চ",
  "registrationDate"  as "রেজিস্ট্রেশনের তারিখ",
  "refDoctor"          as "রেফার-করা ডাক্তার (যা লেখা আছে)",
  "bill"              as "বিল"
from public.patients
where (
    "refDoctorMobile" in ('8001080080', '918001080080', '+918001080080')
    or "refDoctor" ilike '%TK BIS%'
  )
  and "registrationDate" >= to_char(current_date - interval '1 year', 'YYYY-MM-DD')
order by "registrationDate" asc;
