-- V1389: শুধু দেখার জন্য (read-only) — MILAN POLLY-র কমিশন PKB-র নতুন
-- জলপাইগুড়ি RMP-এর সাথে সত্যিই লিংক হয়েছে কিনা যাচাই। কিছুই বদলায় না।

select c.*
from fin.rmp_patient_commissions c
join public.patients p on p.id = c.patient_row_id
where p."patientId" = 'JPE-12092026-002';
