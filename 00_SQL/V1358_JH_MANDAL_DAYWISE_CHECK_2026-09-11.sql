-- ═══════════════════════════════════════════════════════════════════════════
-- V1358 (১১.০৯.২০২৬) — JH MANDAL (কোচবিহার, 7479173399): দিন-ধরে কমিশন মেলানো
--   TK: *"তাদের তো দিনের দিন পেমেন্ট করে দেয়া হয়েছে প্রতিদিন"* — তবু Due ₹2,797.50
--
-- ⛔ শুধু **পড়া** — একটাও সারি লেখা/বদলানো হয় না। যতবার খুশি চালানো যায়।
-- চালানোর নিয়ম: Supabase → SQL Editor → New query → পুরো ফাইল পেস্ট → Run।
--
-- ফল (তারিখ অনুযায়ী এক তালিকা):
--   kind = PATIENT PAID  ⇒ রোগী সেদিন চিকিৎসার টাকা দিয়েছেন (Visit/Registration/
--                         Medicine বাদ; অনুমোদিত Refund ঋণাত্মক) · সেদিনের হার ·
--                         সেই টাকায় RMP-র কমিশন
--   kind = PAID TO RMP   ⇒ সেদিন RMP-কে কত দেওয়া হয়েছে
--   running_due          ⇒ ওই সারি পর্যন্ত জমা-বাকি (কমিশন − দেওয়া), তারিখ ধরে
--   যে দিনে PATIENT PAID আছে কিন্তু পাশে PAID TO RMP নেই (বা কম) — সেদিনটাই বাদ গেছে।
-- ═══════════════════════════════════════════════════════════════════════════

-- (V941-এর হার-বদলের তিনটে ঘর আগে থেকেই আছে — এই লাইন কিছু বদলায় না, শুধু নিশ্চিত করে)
alter table fin.rmp_patient_commissions
  add column if not exists prev_mode       text,
  add column if not exists prev_value      numeric(12,2),
  add column if not exists rate_changed_on date;

with rmp as (
  select d.id, coalesce(d.name,'') as name
  from public.doctor_visits d
  where right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) = '7479173399'
    and lower(trim(coalesce(d.branch,''))) = 'cooch behar'
  limit 1
), pc as (
  select c.* from fin.rmp_patient_commissions c join rmp on c.rmp_id = rmp.id
), earn as (
  -- চিকিৎসার জমা (ধনাত্মক)
  select left(coalesce(x."date",''),10) as d,
         coalesce(pc.patient_name,'') as patient,
         fin.rmp_safe_number(x."amount") as amt,
         case when pc.rate_changed_on is not null
                   and left(coalesce(x."date",''),10) < to_char(pc.rate_changed_on,'YYYY-MM-DD')
              then coalesce(pc.prev_value, pc.commission_value)
              else pc.commission_value end as rate
  from public.payments x
  join pc on x."patientId" = pc.patient_row_id
  where fin.rmp_is_treatment(x."payType", x."remarks")
    and fin.rmp_safe_number(x."amount") > 0
  union all
  -- অনুমোদিত Refund (ঋণাত্মক) — কমিশনের হিসাবের একই নিয়মে
  select left(coalesce(r."date",''),10),
         coalesce(pc.patient_name,''),
         -fin.rmp_safe_number(r."amount"),
         case when pc.rate_changed_on is not null
                   and left(coalesce(r."date",''),10) < to_char(pc.rate_changed_on,'YYYY-MM-DD')
              then coalesce(pc.prev_value, pc.commission_value)
              else pc.commission_value end
  from public.payments r
  left join public.payments o on o.id = r."refundOfPaymentId"
  join pc on r."patientId" = pc.patient_row_id
  where lower(coalesce(r."payType",'')) = 'refund'
    and lower(coalesce(r."refundApprovalStatus",'')) = 'approved'
    and (trim(coalesce(r."refundOfPaymentId",'')) = '' or fin.rmp_is_treatment(o."payType", o."remarks"))
), given as (
  -- RMP-কে দেওয়া টাকা: আগাম (RMP Payment) + রোগীর নামে সরাসরি (adjust-এর নকল সারি বাদ)
  select to_char(a.paid_on,'YYYY-MM-DD') as d, a.amount as amount, coalesce(a.mode,'') as mode
  from fin.rmp_advance_payments a join rmp on a.rmp_id = rmp.id
  union all
  select to_char(p.paid_on,'YYYY-MM-DD'), p.amount, coalesce(p.mode,'')
  from fin.rmp_commission_payments p join rmp on p.rmp_id = rmp.id
  where not exists (select 1 from fin.rmp_advance_allocations al where al.commission_payment_id = p.id)
), rows_ as (
  select d, 'PATIENT PAID'::text as kind, patient, amt as amount, rate,
         round(amt * coalesce(rate,0) / 100, 2) as commission, 0::numeric as paid_to_rmp, ''::text as mode
  from earn
  union all
  select d, 'PAID TO RMP'::text, ''::text, null::numeric, null::numeric, 0::numeric, amount, mode
  from given
)
select d as "date", kind, patient, amount, rate as "rate_%", commission, paid_to_rmp, mode,
       round(sum(commission - paid_to_rmp) over (order by d, kind, patient rows unbounded preceding), 2) as running_due
from rows_
order by d, kind, patient;
