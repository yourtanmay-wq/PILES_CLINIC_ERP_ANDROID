-- ============================================================================
-- V424 — প্রত্যেক স্টাফের জয়েনিং তারিখ থেকে আজ পর্যন্ত সব বেতন "পরিশোধ" করে দেওয়া
--   (TK-নির্দেশ ১৭.০৮.২০২৬ — "কোনো স্টাফের কোনো সেলারি বাকি নেই")
--
--   ⛔ কোনো সারি মোছে না। কোনো পুরনো অঙ্ক বদলায় না।
--   ⛔ যে মাসের বেতন আগে থেকেই বসানো আছে, সেটা আর দ্বিতীয়বার বসে না (ডবল হবে না)।
--   ⛔ বারবার Run করলেও একই ফল — নতুন কিছু যোগ হবে না।
--   ⛔ "বাড়তি টাকা" (Extra Income / kind='EXTRA') সারিতে হাত পড়ে না।
--
--   বেতনের অঙ্ক ও জয়েনিং তারিখ = TK নিজে ১৩.০৮.২০২৬-এ যেগুলো নিশ্চিত করেছিলেন
--   (V369 ফাইলের হুবহু সেই তালিকা)। নতুন করে কিছু অনুমান করা হয়নি।
--   FALA-15 বাদ — ওই স্টাফ ছাড়িয়ে দেওয়া হয়েছে (V404)।
-- ============================================================================

alter table hr.salary_payments add column if not exists for_month text;

-- ── ১) মাসিক বেতন ও বেতনের তারিখ চালু করা ──────────────────────────────────
with staff(person_code, salary_amount, salary_day) as (
  values
    ('KNE-KISHAN5',   7000::numeric,  1),
    ('KNE-LAXMI',     8000::numeric,  3),
    ('COB-UTTAMA',    7000::numeric,  7),
    ('JPE-JALPAI-13', 7000::numeric,  1),
    ('FLK-1',         7000::numeric, 15),
    ('COB-4',         7000::numeric,  1),
    ('JPE-RUPAM',    10000::numeric,  4),
    ('JPE-CRP',       9000::numeric, 18)
)
insert into hr.salary_config
  (person_code, salary_enabled, salary_amount, salary_date, updated_by, updated_at)
select s.person_code, true, s.salary_amount, s.salary_day::text, 'TK BISWAS', now()
from staff s
where exists (select 1 from hr.staff_profiles p
              where p.person_code = s.person_code and p.active is not false)
on conflict (person_code) do update set
  salary_enabled = true,
  salary_amount  = excluded.salary_amount,
  salary_date    = excluded.salary_date,
  updated_by     = excluded.updated_by,
  updated_at     = excluded.updated_at;

-- ── ২) জয়েনিং তারিখ থেকে আজ পর্যন্ত প্রতি মাসের বেতন "পরিশোধ" হিসেবে বসানো ──
with staff(person_code, join_date, salary_amount) as (
  values
    ('KNE-KISHAN5',   date '2026-05-01',  7000::numeric),
    ('KNE-LAXMI',     date '2025-04-03',  8000::numeric),
    ('COB-UTTAMA',    date '2025-04-07',  7000::numeric),
    ('JPE-JALPAI-13', date '2026-05-01',  7000::numeric),
    ('FLK-1',         date '2025-12-15',  7000::numeric),
    ('COB-4',         date '2026-08-01',  7000::numeric),
    ('JPE-RUPAM',     date '2026-04-04', 10000::numeric),
    ('JPE-CRP',       date '2024-03-18',  9000::numeric)
), due as (
  select s.person_code,
         s.salary_amount,
         gs::date            as paid_on,
         to_char(gs,'YYYY-MM') as for_month
  from staff s
  join hr.staff_profiles p
    on p.person_code = s.person_code and p.active is not false
  cross join lateral generate_series(
    s.join_date,
    (now() at time zone 'Asia/Kolkata')::date,
    interval '1 month'
  ) gs
)
insert into hr.salary_payments
  (person_code, paid_on, amount, mode, paid_by, remark, for_month, kind, status)
select d.person_code, d.paid_on, d.salary_amount, 'HISTORICAL', 'TK BISWAS',
       'Salary paid - confirmed by Master', d.for_month, 'SALARY', 'PAID'
from due d
where not exists (
  select 1 from hr.salary_payments p
  where p.person_code = d.person_code
    and coalesce(p.kind,'SALARY') = 'SALARY'
    and coalesce(p.for_month, to_char(p.paid_on,'YYYY-MM')) = d.for_month
);

notify pgrst, 'reload schema';

-- ── ৩) মিলিয়ে দেখা (শুধু পড়া) — প্রত্যেক স্টাফের বাকি ০ কিনা ────────────────
with paid as (
  select person_code,
         count(*)                as maser_songkha,
         coalesce(sum(amount),0) as mot_deoa,
         min(coalesce(for_month, to_char(paid_on,'YYYY-MM'))) as prothom_mash,
         max(coalesce(for_month, to_char(paid_on,'YYYY-MM'))) as shesh_mash
  from hr.salary_payments
  where coalesce(kind,'SALARY') = 'SALARY'
  group by person_code
)
select p.person_code,
       p.full_name,
       p.branch,
       c.salary_amount as mashik_beton,
       c.salary_date   as betoner_tarikh,
       coalesce(a.maser_songkha,0) as koto_mash_deoa,
       coalesce(a.mot_deoa,0)      as mot_deoa_taka,
       a.prothom_mash,
       a.shesh_mash,
       case when c.salary_enabled is true and coalesce(a.maser_songkha,0) > 0
            then 'OK - baki nei' else 'DEKHUN' end as obostha
from hr.staff_profiles p
left join hr.salary_config   c on c.person_code = p.person_code
left join paid               a on a.person_code = p.person_code
where p.active is not false
  and lower(coalesce(p.role_kind,'')) <> 'doctor'
  and upper(coalesce(p.person_code,'')) not like 'DR-%'
order by p.person_code;
