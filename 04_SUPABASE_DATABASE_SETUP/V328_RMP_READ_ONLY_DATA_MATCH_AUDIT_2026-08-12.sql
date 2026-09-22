-- ============================================================================
-- V328 (12.08.2026) — RMP DATA-MATCH AUDIT (READ ONLY)
--
-- SAFETY PROMISE
--   * SELECT statements only.
--   * No INSERT / UPDATE / DELETE / ALTER / CREATE / DROP / GRANT / RPC call.
--   * No patient, RMP, payment, commission or design is changed.
--   * This checks whether old and new RMP records can be matched safely before
--     any faster filtered/summary path is allowed to replace the old path.
-- ============================================================================

-- 1) How many patients carry an RMP/Doctor reference, and how complete is it?
with referred as (
  select
    p.id,
    coalesce(p."patientId", '') as patient_code,
    coalesce(p.name, '') as patient_name,
    coalesce(p.branch, '') as patient_branch,
    trim(coalesce(p."refBy", '')) as ref_by,
    trim(coalesce(p."refDoctor", '')) as ref_doctor,
    right(regexp_replace(coalesce(p."refDoctorMobile", ''), '[^0-9]', '', 'g'), 10) as ref_mobile
  from public.patients p
  where lower(trim(coalesce(p."refBy", ''))) in ('dr. visit','dr visit','doctor visit','rmp')
     or trim(coalesce(p."refDoctor", '')) <> ''
     or trim(coalesce(p."refDoctorMobile", '')) <> ''
)
select
  count(*) as rmp_referred_patients,
  count(*) filter (where ref_doctor <> '') as with_rmp_name,
  count(*) filter (where length(ref_mobile) = 10) as with_valid_rmp_mobile,
  count(*) filter (where ref_doctor <> '' and length(ref_mobile) = 10) as with_both,
  count(*) filter (where ref_doctor = '' and length(ref_mobile) <> 10) as missing_both
from referred;

-- 2) Safe-match result. The desired result is:
--    exactly_one_match = all referred patients, unmatched = 0, ambiguous = 0.
with referred as (
  select
    p.id,
    coalesce(p.branch, '') as patient_branch,
    lower(trim(coalesce(p."refDoctor", ''))) as ref_name,
    right(regexp_replace(coalesce(p."refDoctorMobile", ''), '[^0-9]', '', 'g'), 10) as ref_mobile
  from public.patients p
  where lower(trim(coalesce(p."refBy", ''))) in ('dr. visit','dr visit','doctor visit','rmp')
     or trim(coalesce(p."refDoctor", '')) <> ''
     or trim(coalesce(p."refDoctorMobile", '')) <> ''
), doctors as (
  select
    d.id,
    coalesce(d.branch, '') as doctor_branch,
    lower(trim(coalesce(d.name, ''))) as doctor_name,
    right(regexp_replace(coalesce(d.mobile, ''), '[^0-9]', '', 'g'), 10) as doctor_mobile
  from public.doctor_visits d
), matched as (
  select
    r.id,
    r.ref_name,
    r.ref_mobile,
    count(distinct d.id) filter (
      where (length(r.ref_mobile) = 10 and r.ref_mobile = d.doctor_mobile)
         or (r.ref_name <> '' and r.ref_name = d.doctor_name)
    ) as match_count,
    count(distinct d.id) filter (
      where ((length(r.ref_mobile) = 10 and r.ref_mobile = d.doctor_mobile)
          or (r.ref_name <> '' and r.ref_name = d.doctor_name))
        and r.patient_branch <> '' and d.doctor_branch <> ''
        and lower(trim(r.patient_branch)) <> lower(trim(d.doctor_branch))
    ) as other_branch_match_count
  from referred r cross join doctors d
  group by r.id,r.ref_name,r.ref_mobile
)
select
  count(*) as checked_referred_patients,
  count(*) filter (where match_count = 1) as exactly_one_match,
  count(*) filter (where match_count = 0) as unmatched,
  count(*) filter (where match_count > 1) as ambiguous_multiple_matches,
  count(*) filter (where other_branch_match_count > 0) as branch_mismatch
from matched;

-- 3) Only problem rows, maximum 100. This is for proof; it changes nothing.
with referred as (
  select
    p.id,
    coalesce(p."patientId", '') as patient_code,
    coalesce(p.name, '') as patient_name,
    coalesce(p.branch, '') as patient_branch,
    trim(coalesce(p."refBy", '')) as ref_by,
    trim(coalesce(p."refDoctor", '')) as ref_doctor,
    right(regexp_replace(coalesce(p."refDoctorMobile", ''), '[^0-9]', '', 'g'), 10) as ref_mobile
  from public.patients p
  where lower(trim(coalesce(p."refBy", ''))) in ('dr. visit','dr visit','doctor visit','rmp')
     or trim(coalesce(p."refDoctor", '')) <> ''
     or trim(coalesce(p."refDoctorMobile", '')) <> ''
), doctors as (
  select
    d.id,
    coalesce(d.branch, '') as doctor_branch,
    lower(trim(coalesce(d.name, ''))) as doctor_name,
    right(regexp_replace(coalesce(d.mobile, ''), '[^0-9]', '', 'g'), 10) as doctor_mobile
  from public.doctor_visits d
), matched as (
  select
    r.*,
    count(distinct d.id) filter (
      where (length(r.ref_mobile) = 10 and r.ref_mobile = d.doctor_mobile)
         or (lower(r.ref_doctor) <> '' and lower(r.ref_doctor) = d.doctor_name)
    ) as match_count
  from referred r cross join doctors d
  group by r.id,r.patient_code,r.patient_name,r.patient_branch,r.ref_by,r.ref_doctor,r.ref_mobile
)
select patient_code,patient_name,patient_branch,ref_by,ref_doctor,ref_mobile,
       case when match_count=0 then 'UNMATCHED' else 'MULTIPLE MATCHES' end as problem
from matched
where match_count <> 1
order by problem,patient_code
limit 100;

-- 4) Duplicate RMP identities. Empty result is safest.
with doctors as (
  select
    d.id,d.name,d.mobile,d.branch,
    lower(trim(coalesce(d.name, ''))) as doctor_name,
    right(regexp_replace(coalesce(d.mobile, ''), '[^0-9]', '', 'g'), 10) as doctor_mobile
  from public.doctor_visits d
)
select 'MOBILE' as duplicate_type,doctor_mobile as duplicate_value,
       count(*) as rows,array_agg(id order by id) as rmp_ids
from doctors
where length(doctor_mobile)=10
group by doctor_mobile having count(*)>1
union all
select 'NAME' as duplicate_type,doctor_name as duplicate_value,
       count(*) as rows,array_agg(id order by id) as rmp_ids
from doctors
where doctor_name<>''
group by doctor_name having count(*)>1
order by duplicate_type,duplicate_value;

-- 5) Read-only size/coverage facts for choosing the least expensive route.
select
  (select count(*) from public.doctor_visits) as total_rmp_rows,
  (select count(*) from public.patients) as total_patient_rows,
  (select count(*) from public.payments) as total_payment_rows,
  (select count(*) from fin.rmp_patient_commissions) as commission_patient_rows,
  (select count(*) from fin.rmp_commission_payments) as commission_payment_rows;
