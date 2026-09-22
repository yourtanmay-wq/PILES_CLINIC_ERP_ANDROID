-- V1395: TAPOSHI BARMAN (COB-04082026-002) ভুলবশত PKB-র জলপাইগুড়ি-রেকর্ডে
-- (dv_5b5ab2ff...) বাঁধা হয়ে গিয়েছিল (আসল কারণ: কোডের একটা বাগ, ফিক্স
-- একসাথে দেওয়া হচ্ছে) — আসল হওয়া উচিত PKB-র কোচবিহার-রেকর্ড
-- (dv_91f6cdf6cff84218b82a0bbc74021b9e), যেহেতু তিনি কোচবিহারের রোগী।
-- হার আন্দাজ না করে branch-default → global-default → auto ১০% এই একই
-- ক্রম মেনে বসানো হলো (rmp_set_patient_commission()-এর হুবহু নিয়ম)।
-- কোনো টাকা/বিল ছোঁয়া হয়নি — এখনো তাঁর কোনো কমিশন কারো দেওয়া হয়নি (paid=0)।

begin;

update fin.rmp_patient_commissions
set rmp_id = 'dv_91f6cdf6cff84218b82a0bbc74021b9e',
    rmp_name = 'PKB',
    rmp_mobile = '+919242009205',
    commission_mode = coalesce(
      (select commission_mode from fin.rmp_commission_branch_defaults
        where rmp_id = 'dv_91f6cdf6cff84218b82a0bbc74021b9e' and branch = 'Cooch Behar'),
      (select commission_mode from fin.rmp_commission_defaults
        where rmp_id = 'dv_91f6cdf6cff84218b82a0bbc74021b9e'),
      'PERCENT'),
    commission_value = coalesce(
      (select commission_value from fin.rmp_commission_branch_defaults
        where rmp_id = 'dv_91f6cdf6cff84218b82a0bbc74021b9e' and branch = 'Cooch Behar'),
      (select commission_value from fin.rmp_commission_defaults
        where rmp_id = 'dv_91f6cdf6cff84218b82a0bbc74021b9e'),
      10),
    updated_at = now()
where patient_row_id = 'pat_7363037864';

insert into fin.rmp_commission_audit(action, entity_id, old_value, new_value, reason, changed_by)
select 'CORRECTION_WRONG_BRANCH_RMP', c.id::text,
  jsonb_build_object('rmp_id','dv_5b5ab2ff39454839a0a4c3f2fe776c15','commission_value',10,'commission_mode','PERCENT'),
  to_jsonb(c),
  'TK-verified he never did this manually; app auto-picked the wrong same-mobile branch record (code fixed in V1395); reassigned to the correct Cooch Behar PKB record',
  'CLAUDE_DIRECT'
from fin.rmp_patient_commissions c where c.patient_row_id = 'pat_7363037864';

-- যাচাই — সংশোধনের ফল দেখুন
select patient_row_id, patient_name, treatment_branch, rmp_id, rmp_name, commission_mode, commission_value
from fin.rmp_patient_commissions where patient_row_id = 'pat_7363037864';

commit;
