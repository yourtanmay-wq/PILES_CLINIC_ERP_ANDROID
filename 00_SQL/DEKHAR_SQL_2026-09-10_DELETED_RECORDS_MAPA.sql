-- শুধু দেখা (⑪): মোছা-চিহ্নের তালিকা (deleted_records) কত বড়, কোন টেবিলের কটা, কত পুরনো
select coalesce("tableName",'?') as tebil, count(*) as chinho,
       min("deletedAt")::text as prothom, max("deletedAt")::text as shesh,
       count(*) filter (where nullif("deletedAt",'')::timestamptz < now() - interval '30 days') as tirish_diner_purono
from deleted_records
group by 1
union all
select 'MOT', count(*), min("deletedAt")::text, max("deletedAt")::text,
       count(*) filter (where nullif("deletedAt",'')::timestamptz < now() - interval '30 days')
from deleted_records
union all
select 'টেবিলের মাপ', null, pg_size_pretty(pg_total_relation_size('public.deleted_records')), null, null
order by 2 desc nulls last;
