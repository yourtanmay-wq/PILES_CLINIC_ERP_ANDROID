-- ⛔ শুধু দেখার SQL — একটাও সারি লেখে/বদলায়/মোছে না।
-- MANIK ROY আর DIPANKAR ROY-র টাকা এখনো ₹200 দেখাচ্ছে কেন — নতুন নিয়মে
-- (V1482) এখন কত হওয়া উচিত (wanted_now), আর ফোনের ঘরে (salary_payments)
-- এখনো আসলে কত বসে আছে (salary_now) — দুটো পাশাপাশি দেখাবে।

with target as (
  select id, coalesce(nullif("patientId", ''), id) as shown_code, name,
    right(regexp_replace(coalesce(mobile, ''), '\D', '', 'g'), 10) as mob10
  from public.patients
)
select t.name, t.shown_code,
  w.person_code, w.src_key,
  w.amount as wanted_now,
  s.amount as salary_now,
  s.status
from target t
join hr.incentive_wanted() w on w.src_key like 'INC:%:' || t.id || ':%'
left join hr.salary_payments s on s.src_key = w.src_key
where t.mob10 in ('8927947851', '6294514785')
order by t.name, w.src_key;
