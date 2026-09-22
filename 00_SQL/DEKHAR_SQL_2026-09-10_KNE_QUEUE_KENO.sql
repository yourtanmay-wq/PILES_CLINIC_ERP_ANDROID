-- শুধু দেখা (তালিকা ৪২২): কিষানগঞ্জের CHECK-UP Queue-তে আজ কারা এবং কী কারণে
with today as (select to_char(now() at time zone 'Asia/Kolkata','YYYY-MM-DD') as d),
q as (
  select p.id, p."patientId", p.name, p.mobile, p.stage, coalesce(p.queue::text,'') as queue, coalesce(p."doctorComplete"::text,'') as dc,
         coalesce(p."queuedAt",'') as "queuedAt", coalesce(p."visitDate",'') as "visitDate", coalesce(p."registrationDate",'') as "registrationDate",
         coalesce(p."updatedAt",'') as "updatedAt", coalesce(p."nextVisitPlan"::text,'') as "nextVisitPlan"
  from public.patients p, today t
  where p.branch = 'Kishanganj'
    and lower(coalesce(p."doctorComplete"::text,'')) not in ('true','1')
    and (lower(coalesce(p.queue::text,'')) in ('true','1') or p.stage in ('Doctor Queue','Visit'))
    and left(coalesce(nullif(p."queuedAt",''), nullif(p."visitDate",''), nullif(p."registrationDate",''), p."createdAt"),10) = t.d
)
select 'রোগী' as ki, q."patientId", q.name, q.stage, q.queue, q.dc, q."queuedAt", q."visitDate", q."registrationDate", q."updatedAt", '' as "payType", '' as "createdBy", '' as "createdAt"
from q
union all
select 'আজকের-টাকা/চিহ্ন', q."patientId", q.name, '', '', '', '', '', '', '', y."payType", y."createdBy", y."createdAt"
from q join public.payments y on right(regexp_replace(coalesce(y.mobile,''),'\D','','g'),10) = right(regexp_replace(coalesce(q.mobile,''),'\D','','g'),10)
where y."createdAt" >= (select d from today) or y.date = (select d from today)
order by 1, 2, 13;
