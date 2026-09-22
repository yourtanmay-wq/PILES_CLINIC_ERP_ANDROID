-- ============================================================================
-- V328 — RMP LEGACY CARD COUNT, SERVER-SIDE PROOF (READ ONLY)
-- SELECT only. It cannot add, edit or delete any data.
--
-- This deliberately reproduces DoctorVisitActivity's CURRENT card rule:
--   patient.refBy == RMP name OR patient.refDoctorMobile == RMP mobile.
-- It does NOT use the newer commission matching rule and does NOT change it.
-- ============================================================================

with doctor_rows as (
  select
    d.id as rmp_id,
    coalesce(d.name,'') as rmp_name,
    coalesce(d.mobile,'') as rmp_mobile,
    coalesce(d.branch,'') as rmp_branch,
    lower(trim(coalesce(d.name,''))) as match_name,
    right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) as match_mobile
  from public.doctor_visits d
  where lower(coalesce(d.status,'Active'))='active'
), patient_rows as (
  select
    p.id as patient_row_id,
    coalesce(p.branch,'') as patient_branch,
    lower(trim(coalesce(p."refBy",''))) as match_name,
    right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10) as match_mobile
  from public.patients p
), server_counts as (
  select
    d.rmp_id,d.rmp_name,d.rmp_mobile,d.rmp_branch,
    count(p.patient_row_id) as referred_count
  from doctor_rows d
  left join patient_rows p on
       (p.match_name<>'' and p.match_name=d.match_name)
    or (length(p.match_mobile)=10 and p.match_mobile=d.match_mobile)
  group by d.rmp_id,d.rmp_name,d.rmp_mobile,d.rmp_branch
)
select rmp_id,rmp_name,rmp_mobile,rmp_branch,referred_count
from server_counts
where referred_count>0
order by rmp_branch,rmp_name,rmp_mobile,rmp_id;

