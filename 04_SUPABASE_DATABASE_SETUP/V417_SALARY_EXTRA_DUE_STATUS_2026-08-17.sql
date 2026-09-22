-- ============================================================================
-- V417 — "বাড়তি টাকা" এখনো দেওয়া হয়নি (DUE) নাকি দেওয়া হয়ে গেছে (PAID)
--   TK নিজে চালিয়েছেন ১৭.০৮.২০২৬ · Success
--   ⛔ কোনো সারি মোছে না, কোনো অঙ্ক বদলায় না।
--   ⛔ পুরনো সব সারি নিজে থেকেই 'PAID' হয়ে যায়, তাই পুরনো হিসাব অটুট।
--   ⛔ বেতনের সারিতে (kind='SALARY') এই ঘরটা কোনো কাজেই লাগে না — শুধু EXTRA-তে।
-- ============================================================================
alter table hr.salary_payments
  add column if not exists status text not null default 'PAID';

alter table hr.salary_payments
  drop constraint if exists salary_payments_status_chk;
alter table hr.salary_payments
  add constraint salary_payments_status_chk
  check (status in ('PAID', 'DUE'));

create index if not exists salary_payments_status_idx
  on hr.salary_payments(person_code, status);

notify pgrst, 'reload schema';

-- মিলিয়ে দেখা (শুধু পড়া):
-- select kind, status, count(*), coalesce(sum(amount),0)
--   from hr.salary_payments group by kind, status order by kind, status;
