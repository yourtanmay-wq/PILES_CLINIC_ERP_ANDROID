-- শুধু SELECT — কিছু মোছে না।
-- খোঁজা হচ্ছে: একই মোবাইলে একাধিক রোগী (Different Patient — Same Mobile
-- দিয়ে তৈরি), যাদের সারা জীবনে ১টার বেশি পেমেন্ট নেই, সেই একটাও শুধু
-- Registration/Visit fee, আর একবারও Checkup হয়নি (PROMOTH PARVIN-এর
-- মতোই প্যাটার্ন — সম্ভাব্য ভুল-করে-তৈরি রোগী)।
with dup_mobile as (
  select right(regexp_replace(coalesce(mobile,''),'\D','','g'),10) as m
  from public.patients
  where length(right(regexp_replace(coalesce(mobile,''),'\D','','g'),10)) = 10
  group by m having count(*) > 1
),
pay_stats as (
  select "patientId" as pid,
         count(*) as pay_count,
         sum(coalesce(nullif(regexp_replace(amount,'[^0-9.-]','','g'),'')::numeric,0)) as total_paid,
         bool_and(lower(coalesce("payType",'')) in ('visit_fee','visitfee','registration')) as only_registration
    from public.payments
   where coalesce("patientId",'') <> ''
   group by "patientId"
),
med_stats as (
  select "patientId" as pid, count(*) as med_count
    from public.medical
   where coalesce("patientId",'') <> ''
   group by "patientId"
)
select p.branch, p.name, p."patientId" as code, p.mobile, p.age, p.sex,
       p."registrationDate", coalesce(ps.pay_count,0) as payments,
       coalesce(ps.total_paid,0) as total_paid, coalesce(ms.med_count,0) as checkups
  from public.patients p
  join dup_mobile dm
    on right(regexp_replace(coalesce(p.mobile,''),'\D','','g'),10) = dm.m
  left join pay_stats ps on ps.pid = p.id
  left join med_stats ms on ms.pid = p.id
 where coalesce(ps.pay_count,0) <= 1
   and (ps.only_registration is null or ps.only_registration = true)
   and coalesce(ms.med_count,0) = 0
 order by p.branch, p.mobile, p."registrationDate";
