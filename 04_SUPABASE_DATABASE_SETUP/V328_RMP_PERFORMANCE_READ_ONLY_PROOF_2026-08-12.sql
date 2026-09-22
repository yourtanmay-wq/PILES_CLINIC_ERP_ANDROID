-- V328 — Master RMP Performance exact current-result proof (READ ONLY)
-- DO NOT SAVE. No data/schema/permission change.

with doctors as (
  select d.id as rmp_id,coalesce(d.name,'') as rmp_name,
    coalesce(d.mobile,'') as rmp_mobile,coalesce(d.branch,'') as rmp_branch,
    fin.rmp_safe_number(d."referralPaid") as referral_paid,
    lower(trim(coalesce(d.name,''))) as match_name,
    right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) as match_mobile
  from public.doctor_visits d
  where d.status='Active' or d.status is null
), capped_patients as (
  select p.* from public.patients p
  order by p."updatedAt" desc nulls last limit 5000
), matches as (
  select distinct d.rmp_id,d.rmp_name,d.rmp_mobile,d.rmp_branch,d.referral_paid,
    p.id as patient_row_id,coalesce(p.branch,'') as patient_branch,
    coalesce(nullif(trim(coalesce(p."registrationDate",'')),''),coalesce(p.date,'')) as referral_date
  from doctors d join capped_patients p on
       (lower(trim(coalesce(p."refBy",'')))<>'' and lower(trim(p."refBy"))=d.match_name)
    or (length(right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10))=10
        and right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10)=d.match_mobile)
), report as (
  select rmp_id,rmp_name,rmp_mobile,rmp_branch,referral_paid,
    count(*) as all_time_count,
    count(*) filter (where left(referral_date,7)=to_char(now() at time zone 'Asia/Kolkata','YYYY-MM')) as this_month_count,
    max(nullif(referral_date,'')) as most_recent_date,
    count(*) filter (where trim(patient_branch)='') as blank_branch_patients
  from matches
  group by rmp_id,rmp_name,rmp_mobile,rmp_branch,referral_paid
)
select rmp_id,rmp_name,rmp_mobile,rmp_branch,this_month_count,all_time_count,
       referral_paid,coalesce(most_recent_date,'') as most_recent_date,
       blank_branch_patients
from report
order by most_recent_date desc nulls last,rmp_name,rmp_mobile,rmp_id;

