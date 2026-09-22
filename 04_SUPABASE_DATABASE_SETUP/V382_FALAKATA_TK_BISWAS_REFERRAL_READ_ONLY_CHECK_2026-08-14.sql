-- READ-ONLY: Falakata patients linked to TK BISWAS / 8001080080.
-- This query does not INSERT, UPDATE or DELETE anything.
with falakata as (
  select
    p.id,
    coalesce(p.name, '') as patient_name,
    right(regexp_replace(coalesce(p.mobile, ''), '\D', '', 'g'), 10) as patient_mobile,
    coalesce(p."refBy", '') as ref_by,
    right(regexp_replace(coalesce(p."refDoctorMobile", ''), '\D', '', 'g'), 10) as ref_mobile,
    coalesce(p."registrationDate"::text, p.date::text, '') as registration_date
  from public.patients p
  where lower(trim(coalesce(p.branch, ''))) = 'falakata'
), checked as (
  select *,
    case
      when lower(trim(ref_by)) = 'tk biswas' or ref_mobile = '8001080080'
        then 'MATCHED_TK_BISWAS'
      when trim(ref_by) = '' and trim(ref_mobile) = ''
        then 'REFERRAL_BLANK'
      else 'OTHER_REFERRAL'
    end as referral_check
  from falakata
)
select
  patient_name,
  patient_mobile,
  registration_date,
  ref_by,
  ref_mobile,
  referral_check,
  count(*) over () as falakata_total,
  count(*) filter (where referral_check = 'MATCHED_TK_BISWAS') over () as matched_tk_biswas,
  count(*) filter (where referral_check = 'REFERRAL_BLANK') over () as referral_blank,
  count(*) filter (where referral_check = 'OTHER_REFERRAL') over () as other_referral
from checked
order by referral_check, registration_date, patient_name;
