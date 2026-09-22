-- 🔍 READ-ONLY — শুধু দেখার জন্য, কোনো টাকা/ডেটা বদলাবে না
-- TK-প্রশ্ন: Referred যত (৫৬), Income-ও কি ততগুলো হওয়া উচিত?
-- উত্তর যাচাই: প্রতিটা Referred রোগীর নিজের চিকিৎসার জমা টাকা (net_paid) ও
-- কমিশন বাঁধা আছে কিনা (bound) আলাদা দেখাবে। যাদের টাকা জমা পড়েছে (paid > 0)
-- অথচ কমিশন বাঁধা নেই — তাদেরই টাকা এখনো হিসেবে ধরা পড়েনি, এটাই আসল দেখার বিষয়।
with rmp as (
  select id, "name", "mobile", coalesce("branch",'') as rmp_branch
  from public.doctor_visits
  where right(regexp_replace(coalesce("mobile",''),'\D','','g'),10) = '9046366596'
  limit 1
), referred as (
  select p.id, p."name", p."mobile", coalesce(p."branch",'') as patient_branch
  from public.patients p, rmp r
  where (
      lower(trim(coalesce(p."refBy",'')))     = lower(trim(r."name"))
   or lower(trim(coalesce(p."refDoctor",'')))  = lower(trim(r."name"))
   or right(regexp_replace(coalesce(p."refDoctorMobile",''),'\D','','g'),10)
      = right(regexp_replace(coalesce(r."mobile",''),'\D','','g'),10)
  )
  and (trim(coalesce(p."branch",'')) = '' or trim(r.rmp_branch) = ''
       or lower(trim(p."branch")) = lower(trim(r.rmp_branch)))
)
select
  ref."name", ref."mobile", ref.patient_branch,
  round(fin.rmp_net_paid_between(ref.id, null, null), 2) as treatment_paid_by_patient,
  exists(select 1 from fin.rmp_patient_commissions c
          where c.patient_row_id = ref.id and c.rmp_id = (select id from rmp)) as commission_bound,
  case when round(fin.rmp_net_paid_between(ref.id, null, null), 2) > 0
        and not exists(select 1 from fin.rmp_patient_commissions c
                         where c.patient_row_id = ref.id and c.rmp_id = (select id from rmp))
       then '⚠ টাকা জমা আছে, কমিশন বাঁধা নেই'
       when round(fin.rmp_net_paid_between(ref.id, null, null), 2) <= 0
       then 'এখনো কোনো চিকিৎসার টাকা জমা পড়েনি'
       else 'ঠিক আছে — বাঁধা আছে'
  end as status
from referred ref
order by (round(fin.rmp_net_paid_between(ref.id, null, null), 2) > 0
          and not exists(select 1 from fin.rmp_patient_commissions c
                           where c.patient_row_id = ref.id and c.rmp_id = (select id from rmp))) desc,
         treatment_paid_by_patient desc;
