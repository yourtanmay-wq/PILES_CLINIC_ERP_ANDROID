-- V1392: শুধু দেখার জন্য (read-only) — TAPOSHI BARMAN (COB-04082026-002)
-- কেন জলপাইগুড়ির PKB-র সাথে লিংক হলো, তার আসল কারণ যাচাই। কিছুই বদলায় না।

select id, "patientId", "name", "branch", "refBy", "refDoctor", "refDoctorMobile", "date"
from public.patients
where id = 'pat_7363037864';
