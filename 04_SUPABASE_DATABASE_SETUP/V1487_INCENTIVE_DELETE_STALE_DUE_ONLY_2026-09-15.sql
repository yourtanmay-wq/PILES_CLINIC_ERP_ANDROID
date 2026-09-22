-- ধাপ ২/২ এর দ্বিতীয়টা — যে বাকি (DUE) সারি নতুন নিয়মে আর পাওনাই না
-- (যেমন Registration-কারীর পুরনো ভাগ), সেটা মুছে দেওয়া। PAID কোনো সারি
-- কখনো মোছে না।

delete from hr.salary_payments s
where s.src_key like 'INC:%'
  and s.status = 'DUE'
  and not exists (select 1 from hr.incentive_wanted() w where w.src_key = s.src_key);
