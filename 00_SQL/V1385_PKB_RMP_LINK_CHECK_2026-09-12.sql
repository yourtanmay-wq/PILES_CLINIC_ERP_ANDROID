-- V1385: শুধু দেখার জন্য (read-only) — MILAN POLLY / "Dr. PKB (birpara)" কমিশন অটো-লিংক না হওয়ার আসল কারণ
-- কিছুই বদলায় না, শুধু ২টা তথ্য বের করে

-- ১) "PKB"/"birpara" নামে কোনো RMP (doctor_visits) রেকর্ড আদৌ আছে কিনা
select id, name, mobile, branch, status
from public.doctor_visits
where name ilike '%pkb%' or name ilike '%birpara%';

-- ২) MILAN POLLY-র রেজিস্ট্রেশনে refDoctor আসলে কী লেখা হয়েছিল (হুবহু, বানান-সহ)
select "patientId", "name", "refDoctor", "refDoctorMobile", "refBy"
from public.patients
where "name" ilike '%milan%polly%' or "name" ilike '%milan%pally%';
