-- ============================================================================
-- V328 — RMP VIEW ALL MOBILE/PAYMENT RISK CHECK (READ ONLY)
-- SELECT only. No data/schema/permission is changed.
-- It mirrors the current 5,000-row caps before checking mobile-based totals.
-- ============================================================================

with doctors as (
  select
    d.id,
    lower(trim(coalesce(d.name,''))) as match_name,
    right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) as match_mobile
  from public.doctor_visits d
  where d.status='Active' or d.status is null
), capped_patients as (
  select p.*
  from public.patients p
  order by p."updatedAt" desc nulls last
  limit 5000
), referred as (
  select distinct
    p.id,
    coalesce(p."patientId",'') as patient_code,
    coalesce(p.name,'') as patient_name,
    right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) as patient_mobile
  from capped_patients p
  join doctors d on
       (lower(trim(coalesce(p."refBy",'')))<>''
        and lower(trim(p."refBy"))=d.match_name)
    or (length(right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10))=10
        and right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10)=d.match_mobile)
), duplicate_referred_mobiles as (
  select patient_mobile,count(*) as patient_count
  from referred
  where length(patient_mobile)=10
  group by patient_mobile
  having count(*)>1
), capped_payments as (
  select p.*
  from public.payments p
  order by p."updatedAt" desc nulls last
  limit 5000
)
select
  (select count(*) from referred) as referred_patients,
  (select count(*) from referred where length(patient_mobile)<>10) as referred_without_valid_mobile,
  (select count(*) from duplicate_referred_mobiles) as duplicated_referred_mobile_numbers,
  (select coalesce(sum(patient_count),0) from duplicate_referred_mobiles) as patients_sharing_those_mobiles,
  (select count(*) from capped_payments
    where length(right(regexp_replace(coalesce(mobile,''),'[^0-9]','','g'),10))<>10) as payment_rows_without_valid_mobile,
  (select count(*) from capped_payments
    where trim(coalesce(amount,''))<>''
      and trim(amount) !~ '^[+-]?[0-9]+([.][0-9]+)?$') as payment_rows_with_invalid_amount,
  (select count(*) from capped_payments
    where lower(coalesce("payType",''))='refund'
      and lower(coalesce("refundApprovalStatus",''))='approved') as approved_refund_rows,
  (select count(*) from capped_payments
    where lower(coalesce("payType",''))='refund'
      and lower(coalesce("refundApprovalStatus",''))<>'approved') as pending_or_rejected_refund_rows;

