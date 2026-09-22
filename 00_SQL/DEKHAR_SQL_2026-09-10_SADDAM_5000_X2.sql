-- শুধু দেখা (তালিকা ৪১৩ · SADDAM 9547006061): ১৫.০৭-এর সব টাকার সারি — কে · কখন · কীভাবে · dailyEvents
select id, "payType", amount, "payLabel", mode, "cashAmount", "onlineAmount", date, "createdBy", "receivedBy", "createdAt", "updatedAt",
       left(coalesce(remarks,''),120) as remarks, coalesce("dailyEvents"::text,'') as "dailyEvents"
from public.payments
where right(regexp_replace(coalesce(mobile,''),'\D','','g'),10) = '9547006061'
order by date, "createdAt";
