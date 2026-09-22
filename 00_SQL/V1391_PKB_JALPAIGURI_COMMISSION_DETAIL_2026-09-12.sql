-- V1391 (সংশোধিত): শুধু দেখার জন্য (read-only) — PKB (জলপাইগুড়ি)-র সাথে লিংক
-- হওয়া প্রতিটা রোগীর কমিশনের হিসাব বিস্তারিত দেখা। কিছুই বদলায় না।
-- (প্রথম চেষ্টায় fin.rmp_summary() ডাকায় "Master, Staff or Doctor identity
--  required" আটকেছিল — SQL Editor-এ লগইন-পরিচয় থাকে না বলে। এবার নিচু-স্তরের
--  হিসাব-ফাংশনগুলো (যেগুলোয় এই বাধা নেই) সরাসরি ব্যবহার করা হলো।)

select
  c.patient_row_id, c.patient_name, c.patient_code,
  c.commission_mode, c.commission_value, c.prev_mode, c.prev_value, c.rate_changed_on,
  p."bill", p."discount",
  greatest(0, fin.rmp_safe_number(p."bill") - fin.rmp_safe_number(p."discount")) as net_bill,
  fin.rmp_net_paid_between(c.patient_row_id, null::date, null::date) as net_treatment_paid,
  fin.rmp_earned_for(
    c.patient_row_id,
    greatest(0, fin.rmp_safe_number(p."bill") - fin.rmp_safe_number(p."discount")),
    c.commission_mode, c.commission_value, c.prev_mode, c.prev_value, c.rate_changed_on
  ) as earned,
  coalesce((select sum(x.amount) from fin.rmp_commission_payments x
            where x.patient_commission_id = c.id), 0) as commission_paid_to_rmp
from fin.rmp_patient_commissions c
join public.patients p on p.id = c.patient_row_id
where c.rmp_id = 'dv_5b5ab2ff39454839a0a4c3f2fe776c15';
