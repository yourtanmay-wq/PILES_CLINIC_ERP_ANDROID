-- V1396: PKB-র নতুন জলপাইগুড়ি RMP-এর (dv_5b5ab2ff...) জন্য ৩০% ব্রাঞ্চ-ডিফল্ট
-- বসানো (TK-র আগে থেকে ঠিক করা রেট, সারি ২৭৫) — যাতে ভবিষ্যতের সব রোগীর
-- জন্য এই সঠিক হারই স্বয়ংক্রিয়ভাবে বসে, auto-১০% আর না হয়।
-- সাথে GOPAL BISWAS-এর ইতিমধ্যে ভুল হারে (১০%) বসে যাওয়া কমিশনও ৩০%-এ
-- সংশোধন। rmp_id তাঁর ক্ষেত্রে ঠিকই ছিল (তিনি সত্যিই জলপাইগুড়ির রোগী) —
-- শুধু হারটাই ভুল ছিল।

begin;

-- ধাপ ১: ভবিষ্যতের জন্য — জলপাইগুড়িতে PKB-র ৩০% ব্রাঞ্চ-ডিফল্ট বসানো
insert into fin.rmp_commission_branch_defaults
  (rmp_id, branch, rmp_name, rmp_mobile, commission_mode, commission_value, updated_by)
values
  ('dv_5b5ab2ff39454839a0a4c3f2fe776c15', 'Jalpaiguri', 'PKB', '+919242009205', 'PERCENT', 30, 'TK_DIRECT')
on conflict (rmp_id, branch) do update set
  commission_mode = excluded.commission_mode,
  commission_value = excluded.commission_value,
  updated_by = excluded.updated_by,
  updated_at = now();

-- ধাপ ২: GOPAL BISWAS-এর ইতিমধ্যে বসে যাওয়া ভুল হার (১০%) সংশোধন
update fin.rmp_patient_commissions
set commission_mode = 'PERCENT',
    commission_value = 30,
    updated_at = now()
where patient_row_id = 'pat_6296595872';

insert into fin.rmp_commission_audit(action, entity_id, old_value, new_value, reason, changed_by)
select 'CORRECTION_WRONG_DEFAULT_RATE', c.id::text,
  jsonb_build_object('commission_mode','PERCENT','commission_value',10),
  to_jsonb(c),
  'New Jalpaiguri PKB record had no branch-default yet, auto-fell to 10%; corrected to the 30% TK had already set for PKB/Jalpaiguri (row 275)',
  'CLAUDE_DIRECT'
from fin.rmp_patient_commissions c where c.patient_row_id = 'pat_6296595872';

-- যাচাই
select patient_row_id, patient_name, rmp_id, commission_mode, commission_value
from fin.rmp_patient_commissions where patient_row_id = 'pat_6296595872';

select rmp_id, branch, commission_mode, commission_value
from fin.rmp_commission_branch_defaults where rmp_id = 'dv_5b5ab2ff39454839a0a4c3f2fe776c15';

commit;
