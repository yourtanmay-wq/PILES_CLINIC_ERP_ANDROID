-- V1430 (১৩.০৯.২০২৬ দুপুর, TK-নির্দেশ) — COB-UTTAMA · KNE-LAXMI · JPE-CRP-র হাতে-দেওয়া Extra Income
-- খাতায় (hr.salary_payments, kind='EXTRA', status='PAID') বসানো — অ্যাপের "Save Extra Income"
-- বোতাম ঠিক যে ঘরগুলো লেখে, সেই একই ঘর (src_key ফাঁকা = হাতে-লেখা, V1093-এর নিয়মে)।
-- ধরে নেওয়া (TK পাশ করেছেন): মাসের এন্ট্রির তারিখ = ওই মাসের শেষ দিন (২০২৬) · Mode = Cash ·
-- UTTAMA-র Card/Unexpected/Other branch/Review — তারিখ আজ (2026-09-13)।
-- ⛔ দ্বিতীয়বার চালালেও একই সারি আবার বসবে না (একই স্টাফ+কারণ+টাকা থাকলে বাদ)।
begin;
with new_rows(person_code, paid_on, amount, extra_reason) as (
  values
    ('COB-UTTAMA', date '2026-01-31', 1500, 'Extra income · January 2026'),
    ('COB-UTTAMA', date '2026-02-28', 1500, 'Extra income · February 2026'),
    ('COB-UTTAMA', date '2026-03-31', 1300, 'Extra income · March 2026'),
    ('COB-UTTAMA', date '2026-04-30', 2500, 'Extra income · April 2026'),
    ('COB-UTTAMA', date '2026-05-31', 1800, 'Extra income · May 2026'),
    ('COB-UTTAMA', date '2026-06-30', 3700, 'Extra income · June 2026'),
    ('COB-UTTAMA', date '2026-07-31', 3000, 'Extra income · July 2026'),
    ('COB-UTTAMA', date '2026-08-31', 4300, 'Extra income · August 2026'),
    ('COB-UTTAMA', date '2026-09-13', 6000, 'Card'),
    ('COB-UTTAMA', date '2026-09-13', 10000, 'Unexpected'),
    ('COB-UTTAMA', date '2026-09-13', 6500, 'Other branch'),
    ('COB-UTTAMA', date '2026-09-13', 5000, 'Review'),
    ('KNE-LAXMI',  date '2026-01-31', 1396, 'Extra income · January 2026'),
    ('KNE-LAXMI',  date '2026-02-28', 2869, 'Extra income · February 2026'),
    ('KNE-LAXMI',  date '2026-03-31', 4117, 'Extra income · March 2026'),
    ('KNE-LAXMI',  date '2026-04-30', 3607, 'Extra income · April 2026'),
    ('KNE-LAXMI',  date '2026-05-31', 1784, 'Extra income · May 2026'),
    ('KNE-LAXMI',  date '2026-06-30', 3084, 'Extra income · June 2026'),
    ('KNE-LAXMI',  date '2026-07-31', 2603, 'Extra income · July 2026'),
    ('KNE-LAXMI',  date '2026-08-31', 4950, 'Extra income · August 2026'),
    -- JPE-CRP (TK, ১৩.০৯ দুপুর — মোট 26,300)
    ('JPE-CRP',    date '2026-01-31', 2500, 'Extra income · January 2026'),
    ('JPE-CRP',    date '2026-02-28', 1800, 'Extra income · February 2026'),
    ('JPE-CRP',    date '2026-03-31', 3000, 'Extra income · March 2026'),
    ('JPE-CRP',    date '2026-04-30', 1700, 'Extra income · April 2026'),
    ('JPE-CRP',    date '2026-05-31', 4600, 'Extra income · May 2026'),
    ('JPE-CRP',    date '2026-06-30', 2100, 'Extra income · June 2026'),
    ('JPE-CRP',    date '2026-07-31', 3200, 'Extra income · July 2026'),
    ('JPE-CRP',    date '2026-08-31', 7400, 'Extra income · August 2026')
)
insert into hr.salary_payments (person_code, paid_on, amount, mode, paid_by, remark, for_month, kind, extra_reason, status)
select n.person_code, n.paid_on, n.amount, 'Cash', 'MASTER', '', '', 'EXTRA', n.extra_reason, 'PAID'
  from new_rows n
 where not exists (
   select 1 from hr.salary_payments s
    where s.person_code = n.person_code and s.kind = 'EXTRA'
      and coalesce(s.src_key,'') = '' and s.extra_reason = n.extra_reason and s.amount = n.amount
 );
commit;

-- যাচাই: তিনজনের সব হাতে-লেখা Extra Income (মোট: UTTAMA 47,100 · LAXMI 24,410 · CRP 26,300)
select person_code, paid_on, amount, extra_reason, status
  from hr.salary_payments
 where kind = 'EXTRA' and coalesce(src_key,'') = '' and person_code in ('COB-UTTAMA','KNE-LAXMI','JPE-CRP')
 order by person_code, paid_on;
select person_code, sum(amount) as total_hand_extra
  from hr.salary_payments
 where kind = 'EXTRA' and coalesce(src_key,'') = '' and person_code in ('COB-UTTAMA','KNE-LAXMI','JPE-CRP')
 group by person_code;
