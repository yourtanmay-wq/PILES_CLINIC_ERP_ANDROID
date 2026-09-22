-- 🔵 শুধু-পড়ার (READ-ONLY) — আগের SQL (V1631) "0 rows" দিয়েছে, মানে
-- patients পর্দায় দেখানো ID আর payments-এ জমা থাকা patientId হুবহু এক
-- বানানে নাও থাকতে পারে। তাই এবার তিনভাবে খোঁজা হচ্ছে যাতে আসল সারিটা
-- মিস না হয়।

-- ১) patients টেবিলে এই ID ঠিক কীভাবে জমা আছে (আর মোবাইল নম্বরটা কী)
select id, "patientId", mobile, branch, name
from public.patients
where "patientId" ilike '%24022026%001%' or "patientId" ilike '%JPE%24022026%';

-- ২) payments-এ patientId-তে কাছাকাছি মিল (বানান/ড্যাশ আলাদা হলেও ধরবে)
select id, "patientId", mobile, branch, name, date, amount, mode,
       "receivedBy", "createdBy", "createdAt", "editedBy", "editedAt", "editHistory"
from public.payments
where "patientId" ilike '%24022026%'
order by date;

-- ৩) তারিখ ও টাকার অঙ্ক ধরে সরাসরি খোঁজা (৯ আগস্ট ২০২৬, ₹1,000) — patientId
--    ঘরে যা-ই থাকুক, আসল সারিটা এখানে ধরা পড়বে (তারিখের বানান যেমনই হোক)
select id, "patientId", mobile, branch, name, date, amount, mode,
       "receivedBy", "createdBy", "createdAt", "editedBy", "editedAt", "editHistory"
from public.payments
where (date ilike '%2026-08-09%' or date ilike '%09.08.2026%' or date ilike '%09/08/2026%' or date ilike '%09-08-2026%')
  and amount ilike '1000%';
