-- V1528 — applied to live Supabase on 18.09.2026.
-- 1) guard_patient_bill_typo trigger rejects obvious extra-zero Total Bill values.
-- 2) stale pending leave rows are marked expired by Android/Web when approvals load;
--    existing V1520 trigger closes the matching Leave Request briefing.

create or replace function public.guard_patient_bill_typo()
returns trigger language plpgsql security invoker set search_path=public as $$
declare new_bill numeric; max_bill numeric; ceiling_bill numeric;
begin
  if new."bill" is null or btrim(new."bill")='' then return new; end if;
  if new."bill" !~ '^\s*[0-9]+(\.[0-9]+)?\s*$' then return new; end if;
  new_bill:=btrim(new."bill")::numeric;
  select coalesce(max(btrim(p."bill")::numeric),0) into max_bill from public.patients p
   where p.id is distinct from new.id and p."bill" ~ '^\s*[0-9]+(\.[0-9]+)?\s*$'
     and btrim(p."bill")::numeric < 10000000;
  ceiling_bill:=greatest(1000000::numeric,max_bill*10);
  if new_bill>ceiling_bill then raise exception 'Total Bill looks unusually high. Please re-check the amount before saving.' using errcode='22003'; end if;
  return new;
end $$;
drop trigger if exists trg_guard_patient_bill_typo on public.patients;
create trigger trg_guard_patient_bill_typo before insert or update of "bill" on public.patients
for each row execute function public.guard_patient_bill_typo();
