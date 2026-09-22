-- ধাপ ২/২ এর প্রথমটা — নতুন যাদের কমিশন এখনো ঘরেই বসেনি, তাদের বসানো
-- (আগেরটার মতো একই — নতুন কোনো নিয়ম নয়)

insert into hr.salary_payments
  (person_code, paid_on, amount, mode, paid_by, remark, for_month, kind, extra_reason, status, src_key)
select w.person_code, current_date, w.amount, '', 'auto', '', '', 'EXTRA', w.reason, 'DUE', w.src_key
from hr.incentive_wanted() w
where not exists (select 1 from hr.salary_payments s where s.src_key = w.src_key);
