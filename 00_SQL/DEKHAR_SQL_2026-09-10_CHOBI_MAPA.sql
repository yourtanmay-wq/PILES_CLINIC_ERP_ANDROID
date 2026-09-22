-- শুধু দেখা (⑩): টেবিলের ভিতরে জমা ছবি (base64) — কটা সারিতে, মোট কত MB, গড়/সবচেয়ে বড় কত KB
select 'patients.photo' as ghor, count(*) filter (where length(coalesce(photo,'')) > 100) as chobi_sari, count(*) as mot_sari,
       round(sum(length(coalesce(photo,'')))/1048576.0, 2) as mb, round(avg(length(photo)) filter (where length(coalesce(photo,'')) > 100)/1024.0, 0) as gor_kb, round(max(length(photo))/1024.0, 0) as boro_kb
from patients
union all
select 'followups.photo', count(*) filter (where length(coalesce(photo,'')) > 100), count(*),
       round(sum(length(coalesce(photo,'')))/1048576.0, 2), round(avg(length(photo)) filter (where length(coalesce(photo,'')) > 100)/1024.0, 0), round(max(length(photo))/1024.0, 0)
from followups
union all
select 'medical.photos', count(*) filter (where length(coalesce(photos,'')) > 100), count(*),
       round(sum(length(coalesce(photos,'')))/1048576.0, 2), round(avg(length(photos)) filter (where length(coalesce(photos,'')) > 100)/1024.0, 0), round(max(length(photos))/1024.0, 0)
from medical
union all
select 'DB মোট (pg_database_size)', null, null, round(pg_database_size(current_database())/1048576.0, 0), null, null;
