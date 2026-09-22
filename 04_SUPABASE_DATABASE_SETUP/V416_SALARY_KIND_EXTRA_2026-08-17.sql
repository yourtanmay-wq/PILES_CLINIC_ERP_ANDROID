-- ============================================================================
-- V416 — বেতন আর "বাড়তি টাকা" (Extra Income) আলাদা করার ঘর
--   TK নিজে চালিয়েছেন ১৭.০৮.২০২৬ · Success
--   ⛔ কোনো সারি মোছে না, কোনো অঙ্ক বদলায় না।
--   ⛔ পুরনো সব সারি নিজে থেকেই 'SALARY' হয়ে যায়, তাই পুরনো হিসাব অটুট।
-- ============================================================================
alter table hr.salary_payments
  add column if not exists kind text not null default 'SALARY';

alter table hr.salary_payments
  drop constraint if exists salary_payments_kind_chk;
alter table hr.salary_payments
  add constraint salary_payments_kind_chk
  check (kind in ('SALARY', 'EXTRA'));

alter table hr.salary_payments
  add column if not exists extra_reason text;

create index if not exists salary_payments_kind_idx
  on hr.salary_payments(person_code, kind);

notify pgrst, 'reload schema';

-- মিলিয়ে দেখা (শুধু পড়া):
-- select kind, count(*), coalesce(sum(amount),0) from hr.salary_payments group by kind;
