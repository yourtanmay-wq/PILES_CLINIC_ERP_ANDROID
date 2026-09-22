-- শুধু দেখা (৪১৬): MD RIYAZ 8337885258 — রোগীর সারিতে Queue-র ঘরগুলো কী, আর আজ কী কী পেমেন্ট/হাজিরা-চিহ্ন
select 'patient' as ki, id, coalesce("patientId",'') as code, coalesce(stage,'') as stage, coalesce(queue::text,'') as queue, coalesce("doctorComplete"::text,'') as "doctorComplete",
       coalesce("queuedAt",'') as "queuedAt", coalesce("nextVisitPlan"::text,'') as "nextVisitPlan", coalesce("updatedAt",'') as "updatedAt"
from patients
where right(regexp_replace(coalesce(mobile,''),'\D','','g'),10) = '8337885258'
union all
select 'payment_today', id, coalesce("patientId",''), coalesce("payType",''), coalesce(amount,''), coalesce(progress,''), coalesce(date,''), coalesce("receivedBy",''), coalesce("createdAt",'')
from payments
where right(regexp_replace(coalesce(mobile,''),'\D','','g'),10) = '8337885258' and date >= '2026-09-09'
order by 1, 9;
