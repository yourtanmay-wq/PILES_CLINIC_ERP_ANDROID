-- শুধু দেখা: মোছা-তালিকায় (deleted_records) চিহ্ন আছে, অথচ সারিটা টেবিলে এখনো আছে — কটা, কোন টেবিলে
with d as (select "tableName" as t, "rowId" as rid, "deletedAt" from deleted_records)
select 'enquiries' as tebil, count(*) as ache, string_agg(e.name || ' · ' || coalesce(e.mobile,'') || ' · ' || coalesce(e.status,''), ' | ') as ke
  from enquiries e join d on d.t = 'enquiries' and d.rid = e.id
union all
select 'patients', count(*), string_agg(p.name || ' · ' || coalesce(p.mobile,'') || ' · ' || coalesce(p."patientId",''), ' | ')
  from patients p join d on d.t = 'patients' and d.rid = p.id
union all
select 'followups', count(*), string_agg(f.name || ' · ' || coalesce(f.mobile,'') || ' · ' || coalesce(f.stage,'') || '/' || coalesce(f.status,''), ' | ')
  from followups f join d on d.t = 'followups' and d.rid = f.id
union all
select 'payments', count(*), string_agg(coalesce(p."patientId",'') || ' · ' || coalesce(p.date,'') || ' · ' || coalesce(p.amount,'') || ' · ' || coalesce(p."payType",''), ' | ')
  from payments p join d on d.t = 'payments' and d.rid = p.id
union all
select 'doctor_visits', count(*), null from doctor_visits v join d on d.t = 'doctor_visits' and d.rid = v.id
union all
select 'medical', count(*), null from medical m join d on d.t = 'medical' and d.rid = m.id;
