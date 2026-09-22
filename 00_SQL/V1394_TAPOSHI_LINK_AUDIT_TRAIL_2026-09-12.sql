-- V1394: শুধু দেখার জন্য (read-only) — TAPOSHI BARMAN-এর কমিশন-লিংকটা ঠিক
-- কবে/কীভাবে/কার দ্বারা তৈরি হলো, তার প্রমাণিত ইতিহাস (audit trail) দেখা —
-- কোড পড়ে অনুমান না করে সরাসরি রেকর্ড থেকে। কিছুই বদলায় না।

select a.*
from fin.rmp_commission_audit a
join fin.rmp_patient_commissions c on c.id::text = a.entity_id
where c.patient_row_id = 'pat_7363037864'
order by a.id;
