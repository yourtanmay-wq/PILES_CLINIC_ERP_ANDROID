-- PILES CLINIC V1654 · 21.09.2026 · TK-approved Cooch Behar Partner Share correction
-- Scope ONLY: fin.partner_pct_history, Cooch Behar, 4 partners.
-- Does NOT touch collections / expenses / drawings / patients / payments.
-- Approved rule:
--   01 Jan–31 Jul 2026 = TK 50% · K.H 30% · J.H 10% · Gokul 10%
--   From 01 Aug 2026  = TK 40% · K.H 40% · J.H 10% · Gokul 10%
-- Jan–Jul J.H + Gokul shares come from K.H Mandal's original 50%.

begin;

-- Fail closed unless the known V309 timeline is present.
do $$
declare c int;
begin
  select count(*) into c
  from fin.partner_pct_history
  where branch='Cooch Behar'
    and (
      (mobile='8001080080' and pct=50 and effective_from=date '2026-01-01') or
      (mobile='7980993652' and pct=50 and effective_from=date '2026-01-01') or
      (mobile='8001080080' and pct=40 and effective_from=date '2026-08-01') or
      (mobile='7980993652' and pct=40 and effective_from=date '2026-08-01') or
      (mobile='7479173399' and pct=10 and effective_from=date '2026-08-01') or
      (mobile='9002610352' and pct=10 and effective_from=date '2026-08-01')
    );
  if c <> 6 then
    raise exception 'Cooch Behar partner timeline precondition failed: expected 6 known rows, got %', c;
  end if;
end $$;

update fin.partner_pct_history
set pct=30, created_by='V1654-approved-correction'
where branch='Cooch Behar' and mobile='7980993652'
  and effective_from=date '2026-01-01' and pct=50;

insert into fin.partner_pct_history(partner_id,branch,mobile,pct,effective_from,created_by)
select id,branch,mobile,10,date '2026-01-01','V1654-approved-correction'
from fin.partners
where branch='Cooch Behar' and mobile in ('7479173399','9002610352');

commit;

-- Verification:
-- select p.name,h.pct,h.effective_from
-- from fin.partner_pct_history h join fin.partners p on p.id=h.partner_id
-- where h.branch='Cooch Behar'
-- order by h.effective_from,p.name;
