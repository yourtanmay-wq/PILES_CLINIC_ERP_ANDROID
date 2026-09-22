-- পড়া-মাত্র যাচাই — কিছুই বদলায় না।
-- TK-র প্রশ্ন (১৯.০৯.২০২৬): Falakata-র RMP Due List মাস্টারের ফোনে আর
-- ডাক্তারের ফোনে দুই রকম দেখাচ্ছিল। সরাসরি fin.rmp_branch_due('Falakata')
-- ডাকতে গেলে "Master, Staff or Doctor identity required" ভুল দেখাবে —
-- ওই ফাংশন fin.rmp_can_use() যাচাই করে, যেটা শুধু অ্যাপ থেকে লগইন-করা
-- অবস্থাতেই সত্যি হয়, Supabase-এর এই SQL এডিটরে নয় (আগেও একবার এই একই
-- কারণে TK "identity required" পেয়েছিলেন, খাতায় লেখা আছে — এটা কোনো নতুন
-- দোষ নয়)। তাই নিচে fin.rmp_branch_due()-এর ভিতরের হিসাবটাই হুবহু আবার
-- লেখা হলো, শুধু ওই যাচাই দুটো বাদ দিয়ে — ফল একই হওয়ার কথা।

with base as (
  select c.id, c.rmp_id, c.rmp_name, c.rmp_mobile, c.treatment_branch, c.patient_row_id,
         c.commission_mode, c.commission_value,
         greatest(0, fin.rmp_safe_number(p."bill") - fin.rmp_safe_number(p."discount")) as bill
  from fin.rmp_patient_commissions c
  left join public.patients p on p.id = c.patient_row_id
  where c.treatment_branch = 'Falakata'
),
paid_t as (
  select b.id,
    coalesce((select sum(fin.rmp_safe_number(x."amount")) from public.payments x
       where x."patientId" = b.patient_row_id
         and fin.rmp_is_treatment(x."payType", x."remarks")), 0)
  - coalesce((select sum(fin.rmp_safe_number(r."amount")) from public.payments r
       left join public.payments o on o.id = r."refundOfPaymentId"
      where r."patientId" = b.patient_row_id
        and lower(coalesce(r."payType",'')) = 'refund'
        and lower(coalesce(r."refundApprovalStatus",'')) = 'approved'
        and (trim(coalesce(r."refundOfPaymentId",'')) = ''
             or fin.rmp_is_treatment(o."payType", o."remarks"))), 0) as net_paid
  from base b
),
earn as (
  select b.*, case when b.bill > 0 then
      case when b.commission_mode = 'PERCENT'
        then least(greatest(0, pt.net_paid), b.bill) * b.commission_value / 100
        else b.commission_value * least(greatest(0, pt.net_paid), b.bill) / b.bill end
    else 0 end as earned
  from base b join paid_t pt on pt.id = b.id
),
given as (select x.patient_commission_id, sum(x.amount) g
            from fin.rmp_commission_payments x group by 1),
per_row as (
  select e.*, coalesce(g.g,0) as given, greatest(e.earned - coalesce(g.g,0), 0) as due
  from earn e left join given g on g.patient_commission_id = e.id
),
per_rmp as (
  select r.rmp_id as rid, max(r.rmp_name) nm, max(r.rmp_mobile) mb, max(r.treatment_branch) br,
         count(*) pts, round(sum(r.earned),2) ea, round(sum(r.given),2) gi, round(sum(r.due),2) du
  from per_row r group by r.rmp_id
),
adv as (select a.rmp_id as aid, coalesce(sum(a.amount - a.allocated_amount),0) un
          from fin.rmp_advance_payments a group by 1)
select
  q.nm as "RMP-এর নাম",
  q.mb as "মোবাইল",
  q.pts as "কতজন রোগী",
  q.ea  as "মোট কমিশন",
  round(q.gi + coalesce(v.un,0), 2) as "পাওয়া হয়েছে",
  round(greatest(0, q.du - coalesce(v.un,0)), 2) as "এখনো বাকি"
from per_rmp q left join adv v on v.aid = q.rid
order by 6 desc;
