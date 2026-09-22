-- শুধু দেখা (⑬): backuprecords টেবিলে কটা ব্যাকআপ, মোট কত MB, কে/কবে থেকে, শেষ কবে, শেষটার মাপ
select coalesce("by",'') as ke, coalesce(reason,'') as karon, count(*) as koto,
       round(sum(length(coalesce(payload::text,'')))/1048576.0, 1) as mot_mb,
       min("createdAt") as prothom, max("createdAt") as shesh,
       round(max(length(coalesce(payload::text,'')))/1048576.0, 2) as boro_mb
from backuprecords
group by 1,2
union all
select 'MOT', '', count(*), round(sum(length(coalesce(payload::text,'')))/1048576.0, 1), min("createdAt"), max("createdAt"), round(max(length(coalesce(payload::text,'')))/1048576.0, 2)
from backuprecords
union all
select 'টেবিলের মাপ', pg_size_pretty(pg_total_relation_size('public.backuprecords')), null, null, null, null, null
order by 3 desc nulls last;
