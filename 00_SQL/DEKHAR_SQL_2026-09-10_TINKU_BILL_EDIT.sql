-- শুধু দেখা: Tinku Bauli (JPE-19072026-001)-র সব পেমেন্ট-সারি + পুরো ডেটাবেসে "Bill Edited" চিহ্ন-সারিতে টাকার অঙ্ক বসে থাকা কটা
select 'TINKU' as ki, id, date, "payType", amount, mode, coalesce("receivedBy",'') as ke, coalesce(remarks,'') as remarks, "createdAt"
from payments
where "patientId" in (select id from patients where "patientId" = 'JPE-19072026-001')
   or right(regexp_replace(coalesce(mobile,''),'\D','','g'),10) = '9239376246'
union all
select 'BILL_EDIT_with_amount', id, date, "payType", amount, mode, coalesce("receivedBy",''), coalesce(name,'') || ' · ' || coalesce(remarks,''), "createdAt"
from payments
where "payType" = 'bill_edit' and coalesce(nullif(amount,''),'0')::numeric <> 0
order by 1, 9;
