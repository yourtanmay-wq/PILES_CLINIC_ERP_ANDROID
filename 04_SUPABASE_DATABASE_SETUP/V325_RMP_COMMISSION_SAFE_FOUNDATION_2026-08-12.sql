-- ============================================================================
-- V325 (12.08.2026) — RMP Commission: additive, authenticated, fail-safe base
--
-- OWNER-APPROVED RULES IMPLEMENTED HERE
--   * Registration / Visit Fee / Medicine never earn commission.
--   * Percent: rate × net treatment collection, capped by Final Bill.
--   * Amount: fixed amount × (net treatment collection / Final Bill), capped.
--   * Approved treatment refund automatically reduces earned commission.
--   * Patient commission is a snapshot; changing an RMP default never changes
--     an already-set patient commission.
--   * Partial commission payments are allowed. Only Master may overpay.
--   * A commission payment and its linked fin.expenses row are one transaction.
--
-- SAFETY
--   * No existing table, policy, trigger or data is changed/deleted.
--   * anon/public receive no access. Only authenticated app identities may call.
--   * fin.expenses remains Master-only; staff cannot read/write other expenses.
--   * Every mutation is audited; audit rows are Master-only.
-- ============================================================================

create extension if not exists pgcrypto;

create table if not exists fin.rmp_commission_defaults (
  rmp_id text primary key,
  rmp_name text not null default '',
  rmp_mobile text not null default '',
  commission_mode text not null check (commission_mode in ('PERCENT','AMOUNT')),
  commission_value numeric(12,2) not null check (commission_value >= 0),
  effective_from date not null default (now() at time zone 'Asia/Kolkata')::date,
  updated_by text not null,
  updated_at timestamptz not null default now()
);

create table if not exists fin.rmp_patient_commissions (
  id uuid primary key default gen_random_uuid(),
  patient_row_id text not null unique,
  patient_code text not null default '',
  patient_name text not null default '',
  patient_mobile text not null default '',
  treatment_branch text not null,
  rmp_id text not null,
  rmp_name text not null default '',
  rmp_mobile text not null default '',
  commission_mode text not null check (commission_mode in ('PERCENT','AMOUNT')),
  commission_value numeric(12,2) not null check (commission_value >= 0),
  set_on date not null default (now() at time zone 'Asia/Kolkata')::date,
  set_by text not null,
  updated_at timestamptz not null default now()
);

create table if not exists fin.rmp_commission_payments (
  id uuid primary key default gen_random_uuid(),
  patient_commission_id uuid not null references fin.rmp_patient_commissions(id),
  rmp_id text not null,
  rmp_name text not null default '',
  treatment_branch text not null,
  paid_on date not null,
  amount numeric(12,2) not null check (amount > 0),
  mode text not null check (mode in ('CASH','ONLINE')),
  reference_no text,
  expense_id uuid not null unique references fin.expenses(id),
  hidden_from_non_master boolean not null default false,
  recorded_by text not null,
  recorded_at timestamptz not null default now()
);

create table if not exists fin.rmp_commission_audit (
  id uuid primary key default gen_random_uuid(),
  action text not null,
  entity_id text,
  old_value jsonb,
  new_value jsonb,
  reason text,
  changed_by text not null,
  changed_at timestamptz not null default now()
);

create table if not exists fin.rmp_commission_requests (
  id uuid primary key default gen_random_uuid(),
  request_type text not null check (request_type in ('BACKDATE_PAYMENT','PAYMENT_EDIT','PAST_COMMISSION_CHANGE','RMP_REASSIGNMENT')),
  patient_row_id text not null,
  payload jsonb not null default '{}'::jsonb,
  reason text,
  status text not null default 'PENDING' check (status in ('PENDING','APPROVED','REJECTED')),
  requested_by text not null,
  requested_at timestamptz not null default now(),
  decided_by text,
  decided_at timestamptz
);

create index if not exists rmp_pc_rmp_idx on fin.rmp_patient_commissions(rmp_id);
create index if not exists rmp_pay_pc_idx on fin.rmp_commission_payments(patient_commission_id, paid_on);
create index if not exists rmp_pay_rmp_idx on fin.rmp_commission_payments(rmp_id);
create index if not exists rmp_req_pending_idx on fin.rmp_commission_requests(status, requested_at);
-- Free-plan guard: every commission summary filters the existing public
-- Payment ledger by one patient. This small index prevents a full-ledger scan;
-- it changes no row, calculation, API response or visible workflow.
create index if not exists rmp_public_payments_patient_idx on public.payments("patientId");

alter table fin.rmp_commission_defaults enable row level security;
alter table fin.rmp_commission_defaults force row level security;
alter table fin.rmp_patient_commissions enable row level security;
alter table fin.rmp_patient_commissions force row level security;
alter table fin.rmp_commission_payments enable row level security;
alter table fin.rmp_commission_payments force row level security;
alter table fin.rmp_commission_audit enable row level security;
alter table fin.rmp_commission_audit force row level security;
alter table fin.rmp_commission_requests enable row level security;
alter table fin.rmp_commission_requests force row level security;

create or replace function fin.rmp_can_use() returns boolean
language sql stable security definer set search_path = fin,hr,public as $$
  select auth.uid() is not null and exists(
    select 1 from hr.app_identity i where i.uid=auth.uid()
      and (i.is_master or lower(coalesce(i.role_kind,'')) in ('master','staff','doctor'))
  )
$$;

create or replace function fin.rmp_can_write_branch(p_branch text) returns boolean
language sql stable security definer set search_path = fin,hr,public as $$
  select hr.is_master() or exists(
    select 1 from hr.staff_profiles s
     where s.person_code=hr.my_code() and s.active=true
       and lower(trim(coalesce(s.branch,'')))=lower(trim(coalesce(p_branch,'')))
  )
$$;

create or replace function fin.rmp_can_access_rmp(p_rmp_id text) returns boolean
language sql stable security definer set search_path = fin,hr,public as $$
  select hr.is_master() or exists(
    select 1 from public.doctor_visits d
     where d.id=p_rmp_id and fin.rmp_can_write_branch(d.branch)
  )
$$;

drop policy if exists rmp_defaults_read on fin.rmp_commission_defaults;
create policy rmp_defaults_read on fin.rmp_commission_defaults for select to authenticated
  using (fin.rmp_can_use() and fin.rmp_can_access_rmp(rmp_id));
drop policy if exists rmp_pc_read on fin.rmp_patient_commissions;
create policy rmp_pc_read on fin.rmp_patient_commissions for select to authenticated
  using (fin.rmp_can_use() and fin.rmp_can_write_branch(treatment_branch));
drop policy if exists rmp_pay_read on fin.rmp_commission_payments;
create policy rmp_pay_read on fin.rmp_commission_payments for select to authenticated
  using (fin.rmp_can_use() and fin.rmp_can_write_branch(treatment_branch)
    and (hr.is_master() or not hidden_from_non_master));
drop policy if exists rmp_audit_master_read on fin.rmp_commission_audit;
create policy rmp_audit_master_read on fin.rmp_commission_audit for select to authenticated using (hr.is_master());
drop policy if exists rmp_requests_read on fin.rmp_commission_requests;
create policy rmp_requests_read on fin.rmp_commission_requests for select to authenticated
  using (fin.rmp_can_use() and (hr.is_master() or requested_by=hr.my_code()));

-- No table has an INSERT/UPDATE/DELETE policy. All writes must pass the
-- validated functions below, which also write an audit record.

create or replace function fin.rmp_set_default(
  p_rmp_id text, p_rmp_name text, p_rmp_mobile text,
  p_mode text, p_value numeric)
returns void language plpgsql security definer set search_path = fin, public, hr as $$
declare v_old jsonb;
        v_branch text;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  if trim(coalesce(p_rmp_id,''))='' then raise exception 'RMP is required'; end if;
  select coalesce(x.branch,'') into v_branch from public.doctor_visits x where x.id=p_rmp_id;
  if not found then raise exception 'RMP not found'; end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this RMP branch'; end if;
  if upper(coalesce(p_mode,'')) not in ('PERCENT','AMOUNT') or p_value is null or p_value<0 then
    raise exception 'Valid commission mode and value are required';
  end if;
  if upper(p_mode)='PERCENT' and p_value>100 then raise exception 'Percent cannot exceed 100'; end if;
  select to_jsonb(x) into v_old from fin.rmp_commission_defaults x where rmp_id=p_rmp_id;
  insert into fin.rmp_commission_defaults(rmp_id,rmp_name,rmp_mobile,commission_mode,commission_value,updated_by)
  values(trim(p_rmp_id),coalesce(p_rmp_name,''),coalesce(p_rmp_mobile,''),upper(p_mode),p_value,hr.my_code())
  on conflict(rmp_id) do update set rmp_name=excluded.rmp_name,rmp_mobile=excluded.rmp_mobile,
    commission_mode=excluded.commission_mode,commission_value=excluded.commission_value,
    effective_from=(now() at time zone 'Asia/Kolkata')::date,updated_by=excluded.updated_by,updated_at=now();
  insert into fin.rmp_commission_audit(action,entity_id,old_value,new_value,changed_by)
  select 'SET_RMP_DEFAULT',p_rmp_id,v_old,to_jsonb(x),hr.my_code()
    from fin.rmp_commission_defaults x where x.rmp_id=p_rmp_id;
end $$;

create or replace function fin.rmp_set_patient_commission(
  p_patient_row_id text, p_rmp_id text,
  p_mode text default null, p_value numeric default null,
  p_set_on date default null)
returns uuid language plpgsql security definer set search_path = fin, public, hr as $$
declare p public.patients%rowtype; d fin.rmp_commission_defaults%rowtype;
        v_mode text; v_value numeric; v_date date; v_id uuid; v_old jsonb;
        v_default_found boolean:=false; v_existing_set_on date;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  select * into p from public.patients where id=p_patient_row_id;
  if not found then raise exception 'Patient not found'; end if;
  if not fin.rmp_can_write_branch(p.branch) then raise exception 'Not allowed for this patient branch'; end if;
  select * into d from fin.rmp_commission_defaults where rmp_id=p_rmp_id;
  v_default_found:=found;
  v_mode:=upper(nullif(trim(coalesce(p_mode,'')),'')); v_value:=p_value;
  if v_mode is null then
    if not v_default_found then raise exception 'RMP default is not set'; end if;
    v_mode:=d.commission_mode; v_value:=d.commission_value;
  end if;
  if v_mode not in ('PERCENT','AMOUNT') or v_value is null or v_value<0 then raise exception 'Valid commission is required'; end if;
  if v_mode='PERCENT' and v_value>100 then raise exception 'Percent cannot exceed 100'; end if;
  v_date:=coalesce(p_set_on,(now() at time zone 'Asia/Kolkata')::date);
  if v_date>(now() at time zone 'Asia/Kolkata')::date then raise exception 'Future commission date is not allowed'; end if;
  if v_date<>(now() at time zone 'Asia/Kolkata')::date and not hr.is_master() then
    raise exception 'Master approval required for an old date';
  end if;
  select x.set_on into v_existing_set_on from fin.rmp_patient_commissions x
   where x.patient_row_id=p_patient_row_id;
  if found and v_existing_set_on<(now() at time zone 'Asia/Kolkata')::date and not hr.is_master() then
    raise exception 'Master approval required to change an earlier commission';
  end if;
  if exists(select 1 from fin.rmp_patient_commissions x where x.patient_row_id=p_patient_row_id and x.rmp_id<>p_rmp_id) then
    raise exception 'Use the protected RMP reassignment workflow';
  end if;
  select to_jsonb(x) into v_old from fin.rmp_patient_commissions x where patient_row_id=p_patient_row_id;
  insert into fin.rmp_patient_commissions(patient_row_id,patient_code,patient_name,patient_mobile,treatment_branch,
    rmp_id,rmp_name,rmp_mobile,commission_mode,commission_value,set_on,set_by)
  values(p.id,coalesce(p."patientId",''),coalesce(p.name,''),coalesce(p.mobile,''),coalesce(p.branch,''),
    p_rmp_id,coalesce(nullif(d.rmp_name,''),p."refDoctor",''),coalesce(nullif(d.rmp_mobile,''),p."refDoctorMobile",''),
    v_mode,v_value,v_date,hr.my_code())
  on conflict(patient_row_id) do update set rmp_id=excluded.rmp_id,rmp_name=excluded.rmp_name,
    rmp_mobile=excluded.rmp_mobile,commission_mode=excluded.commission_mode,commission_value=excluded.commission_value,
    set_on=excluded.set_on,set_by=excluded.set_by,updated_at=now()
  returning id into v_id;
  insert into fin.rmp_commission_audit(action,entity_id,old_value,new_value,changed_by)
  select 'SET_PATIENT_COMMISSION',v_id::text,v_old,to_jsonb(x),hr.my_code()
    from fin.rmp_patient_commissions x where x.id=v_id;
  return v_id;
end $$;

create or replace function fin.rmp_safe_number(p_text text) returns numeric
language sql immutable as $$
  select case when trim(coalesce(p_text,'')) ~ '^[0-9]+([.][0-9]+)?$'
              then trim(p_text)::numeric else 0 end
$$;

create or replace function fin.rmp_is_treatment(p_type text, p_remarks text) returns boolean
language sql immutable as $$
  select lower(trim(coalesce(p_type,''))) in ('','treatment')
     and lower(coalesce(p_remarks,'')) not like '%visit fee%'
     and lower(coalesce(p_remarks,'')) not like '%registration fee%'
$$;

create or replace function fin.rmp_summary(p_patient_row_id text)
returns table(final_bill numeric, net_treatment_paid numeric, earned numeric,
              paid numeric, due numeric, overpaid numeric)
language plpgsql security definer set search_path = fin, public, hr as $$
declare
  c fin.rmp_patient_commissions%rowtype;
  v_bill numeric := 0; v_paid numeric := 0; v_refund numeric := 0;
  v_earned numeric := 0; v_given numeric := 0;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  select * into c from fin.rmp_patient_commissions where patient_row_id=p_patient_row_id;
  if not found then return query select 0::numeric,0::numeric,0::numeric,0::numeric,0::numeric,0::numeric; return; end if;
  if not fin.rmp_can_write_branch(c.treatment_branch) then raise exception 'Not allowed for this patient branch'; end if;

  -- Owner rule: denominator/cap is Final Bill after Discount. Current app
  -- normally stores the final Total Bill with discount=0; older rows that
  -- carry a separate discount are handled safely here.
  select greatest(0,fin.rmp_safe_number(p."bill")-fin.rmp_safe_number(p."discount"))
    into v_bill from public.patients p where p.id=p_patient_row_id;
  select coalesce(sum(fin.rmp_safe_number(x."amount")),0) into v_paid
    from public.payments x
   where x."patientId"=p_patient_row_id and fin.rmp_is_treatment(x."payType",x."remarks");
  -- Refund rows created by the treatment-refund workflow point to the original
  -- payment. Only approved refunds of a treatment payment reduce this base.
  select coalesce(sum(fin.rmp_safe_number(r."amount")),0) into v_refund
    from public.payments r left join public.payments original on original.id=r."refundOfPaymentId"
   where r."patientId"=p_patient_row_id and lower(coalesce(r."payType",''))='refund'
     and lower(coalesce(r."refundApprovalStatus",''))='approved'
     and (trim(coalesce(r."refundOfPaymentId",''))='' or
          fin.rmp_is_treatment(original."payType",original."remarks"));
  v_paid := greatest(0, v_paid-v_refund);

  if v_bill > 0 then
    if c.commission_mode='PERCENT' then
      v_earned := least(v_paid,v_bill) * c.commission_value / 100;
    else
      v_earned := c.commission_value * least(v_paid,v_bill) / v_bill;
    end if;
  end if;
  select coalesce(sum(x.amount),0) into v_given from fin.rmp_commission_payments x
   where x.patient_commission_id=c.id;
  return query select round(v_bill,2),round(v_paid,2),round(v_earned,2),round(v_given,2),
    round(greatest(v_earned-v_given,0),2),round(greatest(v_given-v_earned,0),2);
end $$;

create or replace function fin.rmp_record_payment(
  p_patient_row_id text, p_amount numeric, p_paid_on date,
  p_mode text, p_reference_no text default null)
returns uuid language plpgsql security definer set search_path = fin, public, hr as $$
declare c fin.rmp_patient_commissions%rowtype; s record; v_expense uuid; v_payment uuid;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  if p_amount is null or p_amount<=0 then raise exception 'Amount must be greater than zero'; end if;
  if upper(coalesce(p_mode,'')) not in ('CASH','ONLINE') then raise exception 'Payment mode must be CASH or ONLINE'; end if;
  if p_paid_on is null then raise exception 'Payment date is required'; end if;
  if p_paid_on>(now() at time zone 'Asia/Kolkata')::date then raise exception 'Future payment date is not allowed'; end if;
  if p_paid_on<>(now() at time zone 'Asia/Kolkata')::date and not hr.is_master() then
    raise exception 'Master approval required for an old date';
  end if;
  select * into c from fin.rmp_patient_commissions where patient_row_id=p_patient_row_id for update;
  if not found then raise exception 'Patient commission is not set'; end if;
  if not fin.rmp_can_write_branch(c.treatment_branch) then raise exception 'Not allowed for this patient branch'; end if;
  select * into s from fin.rmp_summary(p_patient_row_id);
  if p_amount>s.due and not hr.is_master() then raise exception 'Only Master can pay more than commission due'; end if;

  insert into fin.expenses(entry_date,branch,category,paid_to,amount,mode,note,ignored,created_by)
  values(p_paid_on,c.treatment_branch,'RMP Commission Payment',c.rmp_name,p_amount,upper(p_mode),
         nullif(trim(coalesce(p_reference_no,'')),''),false,hr.my_code()) returning id into v_expense;
  insert into fin.rmp_commission_payments(patient_commission_id,rmp_id,rmp_name,treatment_branch,paid_on,amount,mode,
    reference_no,expense_id,recorded_by)
  values(c.id,c.rmp_id,c.rmp_name,c.treatment_branch,p_paid_on,p_amount,upper(p_mode),nullif(trim(coalesce(p_reference_no,'')),''),v_expense,hr.my_code())
  returning id into v_payment;
  insert into fin.rmp_commission_audit(action,entity_id,new_value,changed_by)
  values('COMMISSION_PAYMENT',v_payment::text,jsonb_build_object('amount',p_amount,'paid_on',p_paid_on,'expense_id',v_expense),hr.my_code());
  return v_payment;
end $$;

create or replace function fin.rmp_rmp_summary(p_rmp_id text)
returns table(patient_count bigint, earned numeric, paid_to_this_rmp numeric,
              previous_rmp_paid numeric, due numeric, overpaid numeric)
language plpgsql security definer set search_path = fin, public, hr as $$
declare c record; s record; v_count bigint:=0; v_earned numeric:=0; v_due numeric:=0;
        v_over numeric:=0; v_paid numeric:=0; v_previous numeric:=0; v_branch text;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  select coalesce(x.branch,'') into v_branch from public.doctor_visits x where x.id=p_rmp_id;
  if not found then raise exception 'RMP not found'; end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this RMP branch'; end if;
  for c in select patient_row_id from fin.rmp_patient_commissions where rmp_id=p_rmp_id loop
    select * into s from fin.rmp_summary(c.patient_row_id);
    v_count:=v_count+1; v_earned:=v_earned+coalesce(s.earned,0);
    v_due:=v_due+coalesce(s.due,0); v_over:=v_over+coalesce(s.overpaid,0);
  end loop;
  -- Payment stays credited to the RMP who actually received it, even after
  -- patient reassignment (owner decision option 1).
  select coalesce(sum(x.amount),0) into v_paid from fin.rmp_commission_payments x where x.rmp_id=p_rmp_id;
  select coalesce(sum(x.amount),0) into v_previous
    from fin.rmp_commission_payments x join fin.rmp_patient_commissions pc on pc.id=x.patient_commission_id
   where pc.rmp_id=p_rmp_id and x.rmp_id<>p_rmp_id;
  return query select v_count,round(v_earned,2),round(v_paid,2),round(v_previous,2),round(v_due,2),round(v_over,2);
end $$;

create or replace function fin.rmp_request_approval(
  p_request_type text, p_patient_row_id text, p_payload jsonb, p_reason text default null)
returns uuid language plpgsql security definer set search_path = fin, public, hr as $$
declare v_id uuid; v_type text:=upper(coalesce(p_request_type,''));
        v_branch text; s record; v_payment fin.rmp_commission_payments%rowtype;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  if hr.is_master() then raise exception 'Master can perform this action directly'; end if;
  if v_type not in ('BACKDATE_PAYMENT','PAYMENT_EDIT','PAST_COMMISSION_CHANGE','RMP_REASSIGNMENT') then raise exception 'Invalid approval request'; end if;
  select coalesce(pat.branch,'') into v_branch from public.patients pat where pat.id=p_patient_row_id;
  if not found then raise exception 'Patient not found'; end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this patient branch'; end if;
  if v_type='BACKDATE_PAYMENT' then
    if fin.rmp_safe_number(p_payload->>'amount')<=0 then raise exception 'Valid amount is required'; end if;
    if upper(coalesce(p_payload->>'mode','')) not in ('CASH','ONLINE') then raise exception 'Valid payment mode is required'; end if;
    if coalesce(p_payload->>'paid_on','')='' then raise exception 'Payment date is required'; end if;
    if (p_payload->>'paid_on')::date>(now() at time zone 'Asia/Kolkata')::date then
      raise exception 'Future payment date is not allowed';
    end if;
    select * into s from fin.rmp_summary(p_patient_row_id);
    if fin.rmp_safe_number(p_payload->>'amount')>s.due then
      raise exception 'Only Master can save more than commission due';
    end if;
  end if;
  if v_type='PAYMENT_EDIT' then
    if coalesce(p_payload->>'payment_id','')='' or fin.rmp_safe_number(p_payload->>'amount')<=0 then
      raise exception 'Valid payment edit is required';
    end if;
    if coalesce(p_payload->>'paid_on','')='' or upper(coalesce(p_payload->>'mode','')) not in ('CASH','ONLINE') then
      raise exception 'Valid payment date and mode are required';
    end if;
    if (p_payload->>'paid_on')::date>(now() at time zone 'Asia/Kolkata')::date then
      raise exception 'Future payment date is not allowed';
    end if;
    select pay.* into v_payment from fin.rmp_commission_payments pay
      join fin.rmp_patient_commissions pc on pc.id=pay.patient_commission_id
     where pay.id=(p_payload->>'payment_id')::uuid and pc.patient_row_id=p_patient_row_id;
    if not found then raise exception 'Commission payment does not belong to this patient'; end if;
    select * into s from fin.rmp_summary(p_patient_row_id);
    if fin.rmp_safe_number(p_payload->>'amount') > greatest(0, s.earned-(s.paid-v_payment.amount)) then
      raise exception 'Only Master can save more than commission due';
    end if;
  end if;
  if v_type='PAST_COMMISSION_CHANGE' then
    if coalesce(p_payload->>'rmp_id','')='' or upper(coalesce(p_payload->>'mode','')) not in ('PERCENT','AMOUNT')
       or fin.rmp_safe_number(p_payload->>'value')<0 or coalesce(p_payload->>'set_on','')='' then
      raise exception 'Valid commission change is required';
    end if;
    if upper(p_payload->>'mode')='PERCENT' and fin.rmp_safe_number(p_payload->>'value')>100 then
      raise exception 'Percent cannot exceed 100';
    end if;
    if not exists(select 1 from fin.rmp_patient_commissions pc where pc.patient_row_id=p_patient_row_id
      and pc.rmp_id=p_payload->>'rmp_id' and pc.commission_mode=upper(p_payload->>'old_mode')
      and pc.commission_value=fin.rmp_safe_number(p_payload->>'old_value')
      and pc.set_on=(p_payload->>'set_on')::date) then
      raise exception 'Commission has changed; reopen and request again';
    end if;
  end if;
  if v_type='RMP_REASSIGNMENT' then
    if coalesce(p_payload->>'old_rmp_id','')='' or coalesce(p_payload->>'new_rmp_id','')='' then
      raise exception 'Old and new RMP are required';
    end if;
    if not exists(select 1 from fin.rmp_patient_commissions pc where pc.patient_row_id=p_patient_row_id
      and pc.rmp_id=p_payload->>'old_rmp_id') then
      raise exception 'Patient RMP has changed; reopen and request again';
    end if;
  end if;
  insert into fin.rmp_commission_requests(request_type,patient_row_id,payload,reason,requested_by)
  values(v_type,p_patient_row_id,coalesce(p_payload,'{}'::jsonb),nullif(trim(coalesce(p_reason,'')),''),hr.my_code())
  returning id into v_id;
  insert into fin.rmp_commission_audit(action,entity_id,new_value,reason,changed_by)
  values('REQUEST_'||v_type,v_id::text,p_payload,p_reason,hr.my_code());
  return v_id;
end $$;

create or replace function fin.rmp_edit_payment(
  p_payment_id uuid, p_amount numeric, p_paid_on date, p_mode text,
  p_reference_no text default null, p_master_private boolean default false,
  p_reason text default null)
returns void language plpgsql security definer set search_path = fin, public, hr as $$
declare x fin.rmp_commission_payments%rowtype; v_old jsonb; s record;
        v_patient_row_id text; v_lock_id uuid;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  if p_amount is null or p_amount<=0 then raise exception 'Amount must be greater than zero'; end if;
  if upper(coalesce(p_mode,'')) not in ('CASH','ONLINE') then raise exception 'Payment mode must be CASH or ONLINE'; end if;
  select * into x from fin.rmp_commission_payments where id=p_payment_id for update;
  if not found then raise exception 'Commission payment not found'; end if;
  if not fin.rmp_can_write_branch(x.treatment_branch) then raise exception 'Not allowed for this patient branch'; end if;
  if p_master_private and not hr.is_master() then raise exception 'Master only'; end if;
  if p_paid_on is null then raise exception 'Payment date is required'; end if;
  if p_paid_on>(now() at time zone 'Asia/Kolkata')::date then raise exception 'Future payment date is not allowed'; end if;
  if (x.paid_on<>(now() at time zone 'Asia/Kolkata')::date or
      p_paid_on<>(now() at time zone 'Asia/Kolkata')::date) and not hr.is_master() then
    raise exception 'Master approval required for an old payment';
  end if;
  if not hr.is_master() then
    select c.patient_row_id,c.id into v_patient_row_id,v_lock_id from fin.rmp_patient_commissions c
     where c.id=x.patient_commission_id for update;
    select * into s from fin.rmp_summary(v_patient_row_id);
    if p_amount > greatest(0, s.earned-(s.paid-x.amount)) then
      raise exception 'Only Master can save more than commission due';
    end if;
  end if;
  v_old:=to_jsonb(x);
  update fin.rmp_commission_payments set amount=p_amount,paid_on=p_paid_on,mode=upper(p_mode),
    reference_no=nullif(trim(coalesce(p_reference_no,'')),''),
    hidden_from_non_master=case when p_master_private then true else hidden_from_non_master end
   where id=x.id;
  update fin.expenses set entry_date=p_paid_on,amount=p_amount,mode=upper(p_mode),
    note=nullif(trim(coalesce(p_reference_no,'')),'') where id=x.expense_id;
  insert into fin.rmp_commission_audit(action,entity_id,old_value,new_value,reason,changed_by)
  select case when p_master_private then 'MASTER_PRIVATE_PAYMENT_EDIT' else 'COMMISSION_PAYMENT_EDIT' end,
    x.id::text,v_old,to_jsonb(n),nullif(trim(coalesce(p_reason,'')),''),hr.my_code()
    from fin.rmp_commission_payments n where n.id=x.id;
end $$;

create or replace function fin.rmp_reassign_patient(p_patient_row_id text, p_new_rmp_id text)
returns void language plpgsql security definer set search_path = fin, public, hr as $$
declare c fin.rmp_patient_commissions%rowtype; d fin.rmp_commission_defaults%rowtype;
        v_name text:=''; v_mobile text:=''; v_old jsonb;
begin
  if not hr.is_master() then raise exception 'Master only'; end if;
  select * into c from fin.rmp_patient_commissions where patient_row_id=p_patient_row_id for update;
  if not found then raise exception 'Patient commission is not set'; end if;
  if trim(coalesce(p_new_rmp_id,''))='' then raise exception 'New RMP is required'; end if;
  if c.rmp_id=p_new_rmp_id then return; end if;
  select * into d from fin.rmp_commission_defaults where rmp_id=p_new_rmp_id;
  if found then v_name:=d.rmp_name; v_mobile:=d.rmp_mobile; end if;
  if v_name='' then
    select coalesce(x.name,''),coalesce(x.mobile,'') into v_name,v_mobile
      from public.doctor_visits x where x.id=p_new_rmp_id;
  end if;
  if coalesce(v_name,'')='' then raise exception 'New RMP not found'; end if;
  v_old:=to_jsonb(c);
  -- Owner decision: existing payment rows keep their old rmp_id. Only the
  -- patient entitlement (remaining + future commission) moves to new RMP.
  update fin.rmp_patient_commissions set rmp_id=p_new_rmp_id,rmp_name=v_name,rmp_mobile=coalesce(v_mobile,''),
    set_by=hr.my_code(),updated_at=now() where id=c.id;
  insert into fin.rmp_commission_audit(action,entity_id,old_value,new_value,changed_by)
  select 'RMP_REASSIGN_KEEP_OLD_PAID',c.id::text,v_old,to_jsonb(n),hr.my_code()
    from fin.rmp_patient_commissions n where n.id=c.id;
end $$;

create or replace function fin.rmp_decide_request(p_request_id uuid, p_approve boolean)
returns void language plpgsql security definer set search_path = fin, public, hr as $$
declare r fin.rmp_commission_requests%rowtype; c fin.rmp_patient_commissions%rowtype;
        x fin.rmp_commission_payments%rowtype; s record; v_allowed numeric;
begin
  if not hr.is_master() then raise exception 'Master only'; end if;
  select * into r from fin.rmp_commission_requests where id=p_request_id for update;
  if not found or r.status<>'PENDING' then raise exception 'Pending request not found'; end if;
  if p_approve then
    if r.request_type='BACKDATE_PAYMENT' then
      select * into c from fin.rmp_patient_commissions where patient_row_id=r.patient_row_id for update;
      if not found then raise exception 'Patient commission is not set'; end if;
      select * into s from fin.rmp_summary(r.patient_row_id);
      if fin.rmp_safe_number(r.payload->>'amount')>s.due then
        raise exception 'Request is now more than commission due; review again';
      end if;
      perform fin.rmp_record_payment(r.patient_row_id,fin.rmp_safe_number(r.payload->>'amount'),
        (r.payload->>'paid_on')::date,upper(r.payload->>'mode'),r.payload->>'reference_no');
    elsif r.request_type='PAYMENT_EDIT' then
      select pay.* into x from fin.rmp_commission_payments pay
        join fin.rmp_patient_commissions pc on pc.id=pay.patient_commission_id
       where pay.id=(r.payload->>'payment_id')::uuid and pc.patient_row_id=r.patient_row_id for update of pay;
      if not found then raise exception 'Commission payment does not belong to this patient'; end if;
      select * into c from fin.rmp_patient_commissions where id=x.patient_commission_id for update;
      select * into s from fin.rmp_summary(r.patient_row_id);
      v_allowed:=greatest(0,s.earned-(s.paid-x.amount));
      if fin.rmp_safe_number(r.payload->>'amount')>v_allowed then
        raise exception 'Requested edit is now more than commission due; review again';
      end if;
      perform fin.rmp_edit_payment((r.payload->>'payment_id')::uuid,fin.rmp_safe_number(r.payload->>'amount'),
        (r.payload->>'paid_on')::date,upper(r.payload->>'mode'),r.payload->>'reference_no',false,r.reason);
    elsif r.request_type='PAST_COMMISSION_CHANGE' then
      select * into c from fin.rmp_patient_commissions where patient_row_id=r.patient_row_id for update;
      if not found or c.rmp_id<>r.payload->>'rmp_id' or c.commission_mode<>upper(r.payload->>'old_mode')
         or c.commission_value<>fin.rmp_safe_number(r.payload->>'old_value')
         or c.set_on<>(r.payload->>'set_on')::date then
        raise exception 'Commission changed after request; review again';
      end if;
      perform fin.rmp_set_patient_commission(r.patient_row_id,r.payload->>'rmp_id',
        r.payload->>'mode',fin.rmp_safe_number(r.payload->>'value'),(r.payload->>'set_on')::date);
    else
      select * into c from fin.rmp_patient_commissions where patient_row_id=r.patient_row_id for update;
      if not found or c.rmp_id<>r.payload->>'old_rmp_id' then
        raise exception 'Patient RMP changed after request; review again';
      end if;
      perform fin.rmp_reassign_patient(r.patient_row_id,r.payload->>'new_rmp_id');
    end if;
  end if;
  update fin.rmp_commission_requests set status=case when p_approve then 'APPROVED' else 'REJECTED' end,
    decided_by=hr.my_code(),decided_at=now() where id=r.id;
  insert into fin.rmp_commission_audit(action,entity_id,old_value,new_value,changed_by)
  values(case when p_approve then 'APPROVE_REQUEST' else 'REJECT_REQUEST' end,r.id::text,to_jsonb(r),
    jsonb_build_object('status',case when p_approve then 'APPROVED' else 'REJECTED' end),hr.my_code());
end $$;

revoke all on function fin.rmp_summary(text) from public, anon;
revoke all on function fin.rmp_record_payment(text,numeric,date,text,text) from public, anon;
revoke all on function fin.rmp_rmp_summary(text) from public, anon;
revoke all on function fin.rmp_set_default(text,text,text,text,numeric) from public, anon;
revoke all on function fin.rmp_set_patient_commission(text,text,text,numeric,date) from public, anon;
revoke all on function fin.rmp_request_approval(text,text,jsonb,text) from public, anon;
revoke all on function fin.rmp_decide_request(uuid,boolean) from public, anon;
revoke all on function fin.rmp_edit_payment(uuid,numeric,date,text,text,boolean,text) from public, anon;
revoke all on function fin.rmp_reassign_patient(text,text) from public, anon;
grant execute on function fin.rmp_summary(text) to authenticated;
grant execute on function fin.rmp_record_payment(text,numeric,date,text,text) to authenticated;
grant execute on function fin.rmp_rmp_summary(text) to authenticated;
grant execute on function fin.rmp_set_default(text,text,text,text,numeric) to authenticated;
grant execute on function fin.rmp_set_patient_commission(text,text,text,numeric,date) to authenticated;
grant execute on function fin.rmp_request_approval(text,text,jsonb,text) to authenticated;
grant execute on function fin.rmp_decide_request(uuid,boolean) to authenticated;
grant execute on function fin.rmp_edit_payment(uuid,numeric,date,text,text,boolean,text) to authenticated;
grant execute on function fin.rmp_reassign_patient(text,text) to authenticated;
revoke all on function fin.rmp_can_use() from public,anon;
grant execute on function fin.rmp_can_use() to authenticated;
revoke all on function fin.rmp_can_write_branch(text) from public,anon;
grant execute on function fin.rmp_can_write_branch(text) to authenticated;
revoke all on function fin.rmp_can_access_rmp(text) from public,anon;
grant execute on function fin.rmp_can_access_rmp(text) to authenticated;

revoke all on fin.rmp_commission_defaults,fin.rmp_patient_commissions,
  fin.rmp_commission_payments,fin.rmp_commission_audit from public,anon;
revoke all on fin.rmp_commission_requests from public,anon;
grant usage on schema fin to authenticated;
grant select on fin.rmp_commission_defaults,fin.rmp_patient_commissions,fin.rmp_commission_payments to authenticated;
grant select on fin.rmp_commission_audit to authenticated;
grant select on fin.rmp_commission_requests to authenticated;

notify pgrst, 'reload schema';
