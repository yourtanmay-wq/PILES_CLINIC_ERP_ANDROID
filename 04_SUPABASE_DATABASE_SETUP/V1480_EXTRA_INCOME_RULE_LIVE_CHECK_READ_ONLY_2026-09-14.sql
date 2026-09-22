-- ⛔ শুধু দেখার SQL — একটাও সারি লেখে/বদলায়/মোছে না।
-- TK-এর প্রশ্ন: Extra Income-এর নতুন নিয়ম (Enquiry-কারী একাই ১০০, ট্রিটমেন্ট
-- শুরু হলে মোট ৫০০-এর মধ্যে Enquiry-কারী ৪৫০ + Registration-কারী ৫০ —
-- ০৮.০৯.২০২৬ থেকে চালু) আসলেই ডেটাবেসে বসানো (V1184) আছে কিনা, আর থাকলে
-- ঠিকমতো হিসাব করছে কিনা।

-- ১) V1184 (নতুন নিয়ম) আসলেই লাইভ আছে কিনা
SELECT
  CASE WHEN pg_get_functiondef('hr.incentive_wanted()'::regprocedure) LIKE '%new_who%'
       THEN '✅ V1184 (নতুন নিয়ম) লাইভ আছে'
       ELSE '⛔ V1184 এখনো Run করা হয়নি — পুরনো নিয়মেই চলছে' END AS v1184_status;

-- ২) ০৮.০৯.২০২৬ বা তার পরের রেজিস্ট্রেশনের Unexpected Time পেশেন্টদের
--    বর্তমান Extra Income সারিগুলো (কে কত পেল/পাবে, কোন ধাপে)
WITH inc AS (
  SELECT s.*,
    split_part(s.src_key, ':', 2) AS stage,
    split_part(s.src_key, ':', 3) AS patient_id
  FROM hr.salary_payments s
  WHERE s.kind = 'EXTRA' AND s.src_key LIKE 'INC:%'
)
SELECT
  p."patientId" AS patient_code,
  p.name AS patient_name,
  left(coalesce(nullif(p."registrationDate", ''), p."date", ''), 10) AS reg_date,
  inc.stage,
  inc.person_code,
  sp.full_name AS staff_name,
  inc.amount,
  inc.status,
  inc.src_key
FROM inc
JOIN public.patients p ON p.id = inc.patient_id
LEFT JOIN hr.staff_profiles sp ON sp.person_code = inc.person_code
WHERE coalesce(p."timeType", '') = 'Unexpected Time'
  AND left(coalesce(nullif(p."registrationDate", ''), p."date", ''), 10) >= '2026-09-08'
ORDER BY reg_date, patient_code, inc.stage;
