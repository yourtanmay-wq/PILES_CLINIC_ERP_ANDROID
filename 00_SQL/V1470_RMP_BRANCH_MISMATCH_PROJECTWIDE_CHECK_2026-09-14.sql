-- ═══════════════════════════════════════════════════════════════════════════
-- V1470 (১৪.০৯.২০২৬) — শুধুমাত্র পড়া (READ-ONLY), কিছুই বদলায়/মোছে না।
--
-- TK-র প্রশ্ন: DURDARS PAL-এর মতো ভুল-ব্রাঞ্চে-কমিশন-বসা সমস্যা আর কোন কোন
-- ব্রাঞ্চ ও রোগীর সাথে হয়েছে? এই SQL পুরো প্রজেক্টের fin.rmp_patient_commissions
-- টেবিলের প্রতিটা সারি চেক করে — রোগী আসলে কোন ব্রাঞ্চে চিকিৎসা করিয়েছেন
-- (treatment_branch) তার সাথে কমিশন যে RMP-কার্ডে বসেছে সেই কার্ডের নিজের
-- ব্রাঞ্চ (doctor_visits.branch) মিলছে কিনা — শুধু TK BISWAS নয়, একই নামের
-- একাধিক-ব্রাঞ্চ যেকোনো RMP (PKB ইত্যাদি)-এর জন্যও।
-- ═══════════════════════════════════════════════════════════════════════════

select
  c.patient_name,
  c.patient_mobile,
  c.treatment_branch as patient_actual_branch,
  d.branch as rmp_card_branch,
  c.rmp_name,
  c.commission_mode,
  c.commission_value,
  c.set_by,
  c.updated_at
from fin.rmp_patient_commissions c
join public.doctor_visits d on d.id = c.rmp_id
where c.treatment_branch is not null
  and d.branch is not null
  and lower(trim(c.treatment_branch)) <> lower(trim(d.branch))
order by c.updated_at desc;
