-- V1390: শুধু দেখার জন্য (read-only) — MILAN POLLY-র রেজিস্ট্রেশনে refDoctor/
-- refDoctorMobile-এ ঠিক কী লেখা আছে (হুবহু, বানান-সহ) — অটো-লিংক কেন এখনো
-- হচ্ছে না তার আসল কারণ এখান থেকেই বোঝা যাবে। কিছুই বদলায় না।

select "patientId", "name", "branch", "refBy", "refDoctor", "refDoctorMobile"
from public.patients
where "patientId" = 'JPE-12092026-002';
