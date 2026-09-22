-- V328 — Four suspicious RMP patients: payment/refund proof (READ ONLY)
-- DO NOT SAVE. SELECT only; changes nothing.

with target_patients as (
  select
    p.id as patient_row_id,coalesce(p."patientId",'') as patient_code,
    coalesce(p.name,'') as patient_name,coalesce(p.branch,'') as patient_branch,
    right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) as patient_mobile,
    fin.rmp_safe_number(p.bill) as bill
  from public.patients p
  where p."patientId" in (
    'COB-10082026-001','FLK-04082026-001','JPE-19052026-001','FLK-25122025-001'
  )
), capped_payments as (
  select p.* from public.payments p
  order by p."updatedAt" desc nulls last limit 5000
)
select
  t.patient_code,t.patient_name,t.patient_branch,t.patient_mobile,t.bill,
  p.id as payment_id,coalesce(p.date,'') as payment_date,
  coalesce(p."payType",'') as payment_type,
  coalesce(p."payLabel",p."paymentLabel",'') as payment_label,
  fin.rmp_safe_number(p.amount) as payment_amount,
  coalesce(p.mode,'') as payment_mode,coalesce(p.remarks,'') as remarks,
  coalesce(p."refundApprovalStatus",'') as refund_status,
  coalesce(p."refundOfPaymentId",'') as refund_of_payment_id,
  coalesce(p."patientId",'') as payment_patient_id,
  case
    when coalesce(p."patientId",'')=t.patient_row_id then 'INTERNAL ID MATCH'
    when coalesce(p."patientId",'')=t.patient_code then 'PATIENT CODE MATCH'
    when trim(coalesce(p."patientId",''))='' then 'PAYMENT ID BLANK'
    else 'OTHER ID / MOBILE-ONLY MATCH'
  end as payment_link,
  case
    when coalesce(p."payType",'') in ('visit_fee','attendance_mark') then 0
    when lower(coalesce(p."payType",''))='refund'
     and lower(coalesce(p."refundApprovalStatus",''))='approved'
      then -fin.rmp_safe_number(p.amount)
    when lower(coalesce(p."payType",''))='refund' then 0
    else fin.rmp_safe_number(p.amount)
  end as current_view_all_effect
from target_patients t
left join capped_payments p on
  right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10)=t.patient_mobile
order by t.patient_code,p.date,p."createdAt",p.id;

