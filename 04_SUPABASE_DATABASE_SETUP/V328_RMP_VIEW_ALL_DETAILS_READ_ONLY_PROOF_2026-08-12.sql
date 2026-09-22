-- V328 — RMP View All exact server-result proof (READ ONLY / DO NOT SAVE)
-- Mirrors current Android: newest 5,000 patients + newest 5,000 payments,
-- Ref By OR RMP mobile match, mobile-based Paid, approved Refund subtracts.

with doctors as (
  select d.id,coalesce(d.name,'') as rmp_name,coalesce(d.branch,'') as rmp_branch,
    lower(trim(coalesce(d.name,''))) as match_name,
    right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) as match_mobile
  from public.doctor_visits d
  where d.status='Active' or d.status is null
), capped_patients as (
  select p.* from public.patients p
  order by p."updatedAt" desc nulls last limit 5000
), capped_payments as (
  select p.* from public.payments p
  order by p."updatedAt" desc nulls last limit 5000
), paid_by_mobile as (
  select right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) as patient_mobile,
    round(sum(case
      when coalesce(p."payType",'') in ('visit_fee','attendance_mark') then 0
      when lower(coalesce(p."payType",''))='refund'
       and lower(coalesce(p."refundApprovalStatus",''))='approved'
        then -fin.rmp_safe_number(p.amount)
      when lower(coalesce(p."payType",''))='refund' then 0
      else fin.rmp_safe_number(p.amount)
    end),2) as paid
  from capped_payments p
  group by right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10)
), details as (
  select distinct
    d.id as rmp_id,d.rmp_name,d.rmp_branch,
    p.id as patient_row_id,coalesce(p."patientId",'') as patient_code,
    coalesce(p.name,'') as patient_name,
    right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) as patient_mobile,
    coalesce(nullif(trim(coalesce(p."registrationDate",'')),''),coalesce(p.date,'')) as referral_date,
    round(fin.rmp_safe_number(p.bill),2) as bill,
    coalesce(pm.paid,0)::numeric(12,2) as paid
  from doctors d join capped_patients p on
       (lower(trim(coalesce(p."refBy",'')))<>'' and lower(trim(p."refBy"))=d.match_name)
    or (length(right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10))=10
        and right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10)=d.match_mobile)
  left join paid_by_mobile pm on pm.patient_mobile=
    right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10)
)
select rmp_id,rmp_name,rmp_branch,patient_row_id,patient_code,patient_name,
       patient_mobile,referral_date,bill,paid
from details
order by rmp_branch,rmp_name,referral_date desc,patient_code;

