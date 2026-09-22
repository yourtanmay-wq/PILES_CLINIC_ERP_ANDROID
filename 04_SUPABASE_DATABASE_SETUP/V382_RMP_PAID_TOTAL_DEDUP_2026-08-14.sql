-- V381 OWNER CONFIRMED: ₹12,000 + ₹8,000 paid Online on 14/08/2026 to
-- RMP TK BISWAS.  It belongs to older patients not matched yet, therefore it
-- must never be attached to SUSANTO SARKAR (or guessed onto any patient).

create table if not exists fin.rmp_advance_payments (
  id uuid primary key default gen_random_uuid(),
  rmp_id text not null,
  rmp_name text not null,
  branch text not null,
  paid_on date not null,
  amount numeric(12,2) not null check (amount > 0),
  allocated_amount numeric(12,2) not null default 0 check (allocated_amount >= 0 and allocated_amount <= amount),
  legacy_covered_amount numeric(12,2) not null default 0 check (legacy_covered_amount >= 0 and legacy_covered_amount <= amount),
  mode text not null check (mode in ('CASH','ONLINE')),
  reference_no text,
  expense_id uuid unique references fin.expenses(id),
  recorded_by text not null,
  recorded_at timestamptz not null default now()
);
alter table fin.rmp_advance_payments add column if not exists legacy_covered_amount numeric(12,2) not null default 0;

create table if not exists fin.rmp_advance_allocations (
  id uuid primary key default gen_random_uuid(),
  advance_id uuid not null references fin.rmp_advance_payments(id),
  patient_commission_id uuid not null references fin.rmp_patient_commissions(id),
  commission_payment_id uuid not null unique references fin.rmp_commission_payments(id),
  amount numeric(12,2) not null check (amount > 0),
  allocated_by text not null,
  allocated_at timestamptz not null default now()
);
create unique index if not exists rmp_advance_exact_duplicate_guard
  on fin.rmp_advance_payments(rmp_id,paid_on,amount,mode,coalesce(reference_no,''));

alter table fin.rmp_advance_payments enable row level security;
alter table fin.rmp_advance_payments force row level security;
alter table fin.rmp_advance_allocations enable row level security;
alter table fin.rmp_advance_allocations force row level security;
drop policy if exists rmp_advance_read on fin.rmp_advance_payments;
create policy rmp_advance_read on fin.rmp_advance_payments for select to authenticated using (fin.rmp_can_use());
drop policy if exists rmp_advance_alloc_read on fin.rmp_advance_allocations;
create policy rmp_advance_alloc_read on fin.rmp_advance_allocations for select to authenticated using (fin.rmp_can_use());

create or replace function fin.rmp_advance_summary(p_rmp_id text)
returns table(total_paid numeric, allocated numeric, available numeric)
language sql security definer set search_path=fin,public,hr as $$
  select round(coalesce(sum(a.amount),0),2), round(coalesce(sum(a.allocated_amount),0),2),
         round(coalesce(sum(a.amount-a.allocated_amount),0),2)
  from fin.rmp_advance_payments a
  where a.rmp_id=p_rmp_id and fin.rmp_can_use()
$$;

create or replace function fin.rmp_record_advance(
  p_rmp_id text, p_amount numeric, p_paid_on date, p_mode text, p_reference_no text default null)
returns uuid language plpgsql security definer set search_path=fin,public,hr as $$
declare d record; v_expense uuid; v_id uuid;
begin
  if not hr.is_master() then raise exception 'Only Master can record an unallocated RMP payment'; end if;
  if p_amount is null or p_amount<=0 then raise exception 'Amount must be greater than zero'; end if;
  if upper(coalesce(p_mode,'')) not in ('CASH','ONLINE') then raise exception 'Payment mode must be CASH or ONLINE'; end if;
  select id,name,coalesce(branch,'') branch into d from public.doctor_visits where id=p_rmp_id;
  if not found then raise exception 'RMP not found'; end if;
  insert into fin.expenses(entry_date,branch,category,paid_to,amount,mode,note,ignored,created_by)
  values(p_paid_on,d.branch,'RMP Commission Advance',d.name,p_amount,upper(p_mode),
    nullif(trim(coalesce(p_reference_no,'')),''),false,hr.my_code()) returning id into v_expense;
  insert into fin.rmp_advance_payments(rmp_id,rmp_name,branch,paid_on,amount,mode,reference_no,expense_id,recorded_by)
  values(d.id,d.name,d.branch,p_paid_on,p_amount,upper(p_mode),nullif(trim(coalesce(p_reference_no,'')),''),v_expense,hr.my_code())
  returning id into v_id;
  insert into fin.rmp_commission_audit(action,entity_id,new_value,changed_by)
  values('RMP_ADVANCE_PAYMENT',v_id::text,jsonb_build_object('rmp_id',d.id,'amount',p_amount,'paid_on',p_paid_on),hr.my_code());
  return v_id;
end $$;

-- The two visible old rows (₹12,000 + ₹8,000) are this same ₹20,000 payment.
-- Keep those rows visible, but mark the opening Advance as already represented
-- by legacy history so Ref. Paid never counts the same money twice.
update fin.rmp_advance_payments
set legacy_covered_amount=20000
where lower(trim(rmp_name))='tk bisaws' and paid_on=date '2026-08-14' and amount=20000 and mode='ONLINE';

create or replace function fin.rmp_allocate_advance(
  p_advance_id uuid, p_patient_row_id text, p_amount numeric, p_allow_over_due boolean default false)
returns uuid language plpgsql security definer set search_path=fin,public,hr as $$
declare a fin.rmp_advance_payments%rowtype; c fin.rmp_patient_commissions%rowtype;
        v_expense uuid; v_payment uuid; v_remaining numeric; s record;
begin
  if not hr.is_master() then raise exception 'Only Master can adjust an RMP advance'; end if;
  if p_amount is null or p_amount<=0 then raise exception 'Amount must be greater than zero'; end if;
  select * into a from fin.rmp_advance_payments where id=p_advance_id for update;
  if not found then raise exception 'Advance payment not found'; end if;
  v_remaining:=a.amount-a.allocated_amount;
  if p_amount>v_remaining then raise exception 'Amount is higher than available advance'; end if;
  select * into c from fin.rmp_patient_commissions where patient_row_id=p_patient_row_id for update;
  if not found then raise exception 'Patient commission is not set'; end if;
  if c.rmp_id<>a.rmp_id then raise exception 'Patient belongs to another RMP'; end if;
  select * into s from fin.rmp_summary(p_patient_row_id);
  if p_amount>s.due and not coalesce(p_allow_over_due,false) then
    raise exception 'Payment is higher than this patient commission due; Master warning approval required';
  end if;

  insert into fin.expenses(entry_date,branch,category,paid_to,amount,mode,note,ignored,created_by)
  values(a.paid_on,c.treatment_branch,'RMP Commission Payment',a.rmp_name,p_amount,a.mode,
    'Adjusted from RMP advance '||a.id::text,false,hr.my_code()) returning id into v_expense;
  insert into fin.rmp_commission_payments(patient_commission_id,rmp_id,rmp_name,treatment_branch,paid_on,amount,mode,
    reference_no,expense_id,recorded_by)
  values(c.id,a.rmp_id,a.rmp_name,c.treatment_branch,a.paid_on,p_amount,a.mode,
    'Adjusted from advance',v_expense,hr.my_code()) returning id into v_payment;
  insert into fin.rmp_advance_allocations(advance_id,patient_commission_id,commission_payment_id,amount,allocated_by)
  values(a.id,c.id,v_payment,p_amount,hr.my_code());
  update fin.rmp_advance_payments set allocated_amount=allocated_amount+p_amount where id=a.id;
  if v_remaining=p_amount then
    update fin.rmp_advance_payments set expense_id=null where id=a.id;
    delete from fin.expenses where id=a.expense_id;
  else update fin.expenses set amount=v_remaining-p_amount where id=a.expense_id;
  end if;
  insert into fin.rmp_commission_audit(action,entity_id,new_value,changed_by)
  values('RMP_ADVANCE_ALLOCATED',v_payment::text,jsonb_build_object('advance_id',a.id,'patient_row_id',p_patient_row_id,'amount',p_amount),hr.my_code());
  return v_payment;
end $$;

revoke all on fin.rmp_advance_payments,fin.rmp_advance_allocations from public,anon;
grant select on fin.rmp_advance_payments,fin.rmp_advance_allocations to authenticated;
revoke all on function fin.rmp_advance_summary(text),fin.rmp_record_advance(text,numeric,date,text,text),fin.rmp_allocate_advance(uuid,text,numeric,boolean) from public,anon;
grant execute on function fin.rmp_advance_summary(text),fin.rmp_record_advance(text,numeric,date,text,text),fin.rmp_allocate_advance(uuid,text,numeric,boolean) to authenticated;

-- Idempotent owner-confirmed opening entry. Stop instead of guessing if the
-- exact RMP name is absent or duplicated.
do $$
declare d record; v_count integer; v_expense uuid;
begin
  select count(*) into v_count from public.doctor_visits where lower(trim(name))='tk bisaws';
  if v_count<>1 then raise exception 'Expected exactly one RMP named TK BISWAS; found %',v_count; end if;
  select id,name,coalesce(branch,'') branch into d from public.doctor_visits where lower(trim(name))='tk bisaws';
  if not exists(select 1 from fin.rmp_advance_payments where rmp_id=d.id and paid_on=date '2026-08-14' and amount=20000 and mode='ONLINE') then
    insert into fin.expenses(entry_date,branch,category,paid_to,amount,mode,note,ignored,created_by)
    values(date '2026-08-14',d.branch,'RMP Commission Advance',d.name,20000,'ONLINE',
      'Owner confirmed: ₹12,000 + ₹8,000; older patients not matched yet',false,'MASTER-TK-CONFIRMED') returning id into v_expense;
    insert into fin.rmp_advance_payments(rmp_id,rmp_name,branch,paid_on,amount,mode,reference_no,expense_id,recorded_by)
    values(d.id,d.name,d.branch,date '2026-08-14',20000,'ONLINE',
      '₹12,000 + ₹8,000; older patients pending matching',v_expense,'MASTER-TK-CONFIRMED');
  end if;
end $$;

-- Keep the existing Performance design, but make its Ref. Paid agree with
-- Details/Summary: legacy paid + allocated modern payments + unallocated advance.
create or replace function fin.rmp_legacy_performance(p_branch text default null)
returns table(rmp_id text,this_month_count bigint,all_time_count bigint,referral_paid numeric,most_recent_date text)
language plpgsql stable security definer set search_path=fin,hr,public as $$
declare v_branch text:=nullif(trim(coalesce(p_branch,'')),'');
begin
  if not hr.is_master() then raise exception 'Master only'; end if;
  if v_branch='All' then v_branch:=null; end if;
  if v_branch is not null and v_branch not in ('Kishanganj','Jalpaiguri','Cooch Behar','Falakata','Birpara') then raise exception 'Invalid branch'; end if;
  return query
  with doctors as (
    select d.id,fin.rmp_safe_number(d."referralPaid") legacy_paid,lower(trim(coalesce(d.name,''))) match_name,
      right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) match_mobile
    from public.doctor_visits d where (d.status='Active' or d.status is null) and (v_branch is null or d.branch=v_branch)
  ), capped_patients as (
    select p.* from public.patients p where v_branch is null or p.branch=v_branch order by p."updatedAt" desc nulls last limit 5000
  ), matched as (
    select distinct d.id rmp_id,d.legacy_paid,p.id patient_row_id,
      coalesce(nullif(trim(coalesce(p."registrationDate",'')),''),coalesce(p.date,'')) referral_date
    from doctors d join capped_patients p on
      (lower(trim(coalesce(p."refBy",'')))<>'' and lower(trim(p."refBy"))=d.match_name)
      or (length(right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10))=10
        and right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10)=d.match_mobile)
  ), modern as (
    select x.rmp_id,coalesce(sum(x.amount),0) paid from fin.rmp_commission_payments x group by x.rmp_id
  ), advance as (
    select x.rmp_id,coalesce(sum(x.amount-x.allocated_amount),0) available,
      coalesce(sum(x.legacy_covered_amount),0) legacy_covered
    from fin.rmp_advance_payments x group by x.rmp_id
  )
  select m.rmp_id,
    count(*) filter(where left(m.referral_date,7)=to_char(now() at time zone 'Asia/Kolkata','YYYY-MM'))::bigint,
    count(*)::bigint,round(greatest(0,max(m.legacy_paid)+coalesce(max(n.paid),0)+coalesce(max(a.available),0)-coalesce(max(a.legacy_covered),0)),2),
    coalesce(max(nullif(m.referral_date,'')),'')
  from matched m left join modern n on n.rmp_id=m.rmp_id left join advance a on a.rmp_id=m.rmp_id
  group by m.rmp_id order by max(nullif(m.referral_date,'')) desc nulls last,m.rmp_id;
end $$;

revoke all on function fin.rmp_legacy_performance(text) from public,anon,authenticated;
grant execute on function fin.rmp_legacy_performance(text) to authenticated;
notify pgrst,'reload schema';

-- Expected final proof: exactly one TK BISWAS row with 20000 / 0 / 20000.
select rmp_name,paid_on,amount as paid,allocated_amount as adjusted,
       amount-allocated_amount as available,legacy_covered_amount,mode
from fin.rmp_advance_payments
where lower(trim(rmp_name))='tk bisaws' and paid_on=date '2026-08-14' and amount=20000 and mode='ONLINE';
