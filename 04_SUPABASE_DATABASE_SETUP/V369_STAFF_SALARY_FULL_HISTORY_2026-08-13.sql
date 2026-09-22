-- V369 — Staff Salary config + full paid history (Master confirmed 13.08.2026)
-- Safe/idempotent: existing payment rows are never deleted or changed.
-- Missing monthly rows only are added. Salary day = joining day.

alter table hr.salary_payments add column if not exists for_month text;

with staff(person_code, salary_amount, salary_day) as (
  values
    ('KNE-KISHAN5', 7000::numeric, 1),
    ('KNE-LAXMI', 8000::numeric, 3),
    ('COB-UTTAMA', 7000::numeric, 7),
    ('JPE-JALPAI-13', 7000::numeric, 1),
    ('FLK-1', 7000::numeric, 15),
    ('COB-4', 7000::numeric, 1),
    ('JPE-RUPAM', 10000::numeric, 4),
    ('FALA-15', 7000::numeric, 1),
    ('JPE-CRP', 9000::numeric, 18)
)
insert into hr.salary_config
  (person_code, salary_enabled, salary_amount, salary_date, updated_by, updated_at)
select person_code, true, salary_amount, salary_day::text, 'TK BISWAS', now()
from staff
on conflict (person_code) do update set
  salary_enabled = excluded.salary_enabled,
  salary_amount = excluded.salary_amount,
  salary_date = excluded.salary_date,
  updated_by = excluded.updated_by,
  updated_at = excluded.updated_at;

with staff(person_code, join_date, salary_amount) as (
  values
    ('KNE-KISHAN5', date '2026-05-01', 7000::numeric),
    ('KNE-LAXMI', date '2025-04-03', 8000::numeric),
    ('COB-UTTAMA', date '2025-04-07', 7000::numeric),
    ('JPE-JALPAI-13', date '2026-05-01', 7000::numeric),
    ('FLK-1', date '2025-12-15', 7000::numeric),
    ('COB-4', date '2026-08-01', 7000::numeric),
    ('JPE-RUPAM', date '2026-04-04', 10000::numeric),
    ('FALA-15', date '2026-05-01', 7000::numeric),
    ('JPE-CRP', date '2024-03-18', 9000::numeric)
), due as (
  select s.person_code, s.salary_amount,
         gs::date as paid_on,
         to_char(gs, 'YYYY-MM') as for_month
  from staff s
  cross join lateral generate_series(
    s.join_date,
    date '2026-08-13',
    interval '1 month'
  ) gs
)
insert into hr.salary_payments
  (person_code, paid_on, amount, mode, paid_by, remark, for_month)
select d.person_code, d.paid_on, d.salary_amount, 'HISTORICAL', 'TK BISWAS',
       'Salary paid — confirmed by Master on 13.08.2026; original payment mode not recorded',
       d.for_month
from due d
where not exists (
  select 1 from hr.salary_payments p
  where p.person_code = d.person_code
    and coalesce(p.for_month, to_char(p.paid_on, 'YYYY-MM')) = d.for_month
);

-- Verification result: Master can compare every staff/month after Run.
select p.person_code, sp.full_name, sp.join_date, c.salary_amount, c.salary_date,
       p.for_month, p.paid_on, p.amount, p.mode
from hr.salary_payments p
join hr.staff_profiles sp on sp.person_code = p.person_code
join hr.salary_config c on c.person_code = p.person_code
where p.person_code in
 ('KNE-KISHAN5','KNE-LAXMI','COB-UTTAMA','JPE-JALPAI-13','FLK-1','COB-4','JPE-RUPAM','FALA-15','JPE-CRP')
order by p.person_code, coalesce(p.for_month, to_char(p.paid_on, 'YYYY-MM'));
