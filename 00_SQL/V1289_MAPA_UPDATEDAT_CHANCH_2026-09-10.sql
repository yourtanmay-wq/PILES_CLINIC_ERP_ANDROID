-- মাপার SQL (কিছু বদলায় না): updatedAt-এর ছাঁচ — অ্যাপের 'YYYY-MM-DDTHH:MM:SS…Z' ছাড়া অন্য ছাঁচ কত সারিতে
with s as (
  select 'patients' as tebil, count(*) as mot, count(*) filter (where "updatedAt" !~ '^\d{4}-\d{2}-\d{2}T') as onno_chanch, min("updatedAt") filter (where "updatedAt" !~ '^\d{4}-\d{2}-\d{2}T') as udaharon from patients
  union all select 'payments',  count(*), count(*) filter (where "updatedAt" !~ '^\d{4}-\d{2}-\d{2}T'), min("updatedAt") filter (where "updatedAt" !~ '^\d{4}-\d{2}-\d{2}T') from payments
  union all select 'followups', count(*), count(*) filter (where "updatedAt" !~ '^\d{4}-\d{2}-\d{2}T'), min("updatedAt") filter (where "updatedAt" !~ '^\d{4}-\d{2}-\d{2}T') from followups
  union all select 'enquiries', count(*), count(*) filter (where "updatedAt" !~ '^\d{4}-\d{2}-\d{2}T'), min("updatedAt") filter (where "updatedAt" !~ '^\d{4}-\d{2}-\d{2}T') from enquiries
  union all select 'medical',   count(*), count(*) filter (where "updatedAt" !~ '^\d{4}-\d{2}-\d{2}T'), min("updatedAt") filter (where "updatedAt" !~ '^\d{4}-\d{2}-\d{2}T') from medical
  union all select 'doctor_visits', count(*), count(*) filter (where "updatedAt" !~ '^\d{4}-\d{2}-\d{2}T'), min("updatedAt") filter (where "updatedAt" !~ '^\d{4}-\d{2}-\d{2}T') from doctor_visits
)
select tebil, mot, onno_chanch, udaharon from s
union all
select 'DB collation', null, null, datcollate from pg_database where datname = current_database()
order by 3 desc nulls last;
