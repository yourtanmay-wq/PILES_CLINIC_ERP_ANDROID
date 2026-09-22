-- V380: Master may edit/delete any commission payment at any time.
-- Staff/Doctor may edit/delete only a payment dated today (Asia/Kolkata).
-- Every edit/delete is audited; linked Expense stays in sync.

create or replace function fin.rmp_edit_payment(
  p_payment_id uuid, p_amount numeric, p_paid_on date, p_mode text,
  p_reference_no text default null, p_master_private boolean default false,
  p_reason text default null)
returns void language plpgsql security definer set search_path = fin, public, hr as $$
declare x fin.rmp_commission_payments%rowtype; v_old jsonb; s record;
        v_patient_row_id text; v_lock_id uuid; v_today date := (now() at time zone 'Asia/Kolkata')::date;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  if p_amount is null or p_amount<=0 then raise exception 'Amount must be greater than zero'; end if;
  if upper(coalesce(p_mode,'')) not in ('CASH','ONLINE') then raise exception 'Payment mode must be CASH or ONLINE'; end if;
  select * into x from fin.rmp_commission_payments where id=p_payment_id for update;
  if not found then raise exception 'Commission payment not found'; end if;
  if not fin.rmp_can_write_branch(x.treatment_branch) then raise exception 'Not allowed for this patient branch'; end if;
  if p_master_private and not hr.is_master() then raise exception 'Master only'; end if;
  if p_paid_on is null then raise exception 'Payment date is required'; end if;
  if p_paid_on>v_today then raise exception 'Future payment date is not allowed'; end if;
  if not hr.is_master() and (x.paid_on<>v_today or p_paid_on<>v_today) then
    raise exception 'Only Master can edit an earlier payment';
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

create or replace function fin.rmp_delete_payment(p_payment_id uuid, p_reason text default null)
returns void language plpgsql security definer set search_path = fin, public, hr as $$
declare x fin.rmp_commission_payments%rowtype; v_old jsonb;
        v_today date := (now() at time zone 'Asia/Kolkata')::date;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  select * into x from fin.rmp_commission_payments where id=p_payment_id for update;
  if not found then raise exception 'Commission payment not found'; end if;
  if not fin.rmp_can_write_branch(x.treatment_branch) then raise exception 'Not allowed for this patient branch'; end if;
  if not hr.is_master() and x.paid_on<>v_today then
    raise exception 'Only Master can delete an earlier payment';
  end if;
  v_old:=to_jsonb(x);
  delete from fin.rmp_commission_payments where id=x.id;
  delete from fin.expenses where id=x.expense_id;
  insert into fin.rmp_commission_audit(action,entity_id,old_value,new_value,reason,changed_by)
  values('COMMISSION_PAYMENT_DELETE',x.id::text,v_old,null,
    nullif(trim(coalesce(p_reason,'')),''),hr.my_code());
end $$;

revoke all on function fin.rmp_delete_payment(uuid,text) from public,anon;
grant execute on function fin.rmp_delete_payment(uuid,text) to authenticated;

-- Old app versions must not bypass the new rule by sending a Master approval request.
create or replace function fin.rmp_block_old_payment_edit_request() returns trigger
language plpgsql security definer set search_path=fin,public,hr as $$
begin
  if new.request_type='PAYMENT_EDIT' then
    raise exception 'Earlier commission payments are Master-only and cannot be requested by Staff/Doctor';
  end if;
  return new;
end $$;
drop trigger if exists rmp_block_old_payment_edit_request on fin.rmp_commission_requests;
create trigger rmp_block_old_payment_edit_request before insert on fin.rmp_commission_requests
for each row execute function fin.rmp_block_old_payment_edit_request();
update fin.rmp_commission_requests set status='REJECTED',decided_by='V380-RULE',decided_at=now()
 where request_type='PAYMENT_EDIT' and status='PENDING';

-- Effective-dated RMP default. Existing patient snapshots remain untouched.
-- A patient linked to the default follows each new default only from that date;
-- an individually overridden patient remains on the individual setting.
create table if not exists fin.rmp_commission_default_history (
  id uuid primary key default gen_random_uuid(),
  rmp_id text not null,
  commission_mode text not null check (commission_mode in ('PERCENT','AMOUNT')),
  commission_value numeric(12,2) not null check (commission_value>=0),
  effective_from date not null,
  changed_by text not null,
  changed_at timestamptz not null default now(),
  unique(rmp_id,effective_from)
);
alter table fin.rmp_commission_default_history enable row level security;
alter table fin.rmp_commission_default_history force row level security;
drop policy if exists rmp_default_history_read on fin.rmp_commission_default_history;
create policy rmp_default_history_read on fin.rmp_commission_default_history for select to authenticated
  using (fin.rmp_can_use() and fin.rmp_can_access_rmp(rmp_id));
alter table fin.rmp_patient_commissions add column if not exists use_rmp_default boolean not null default false;
insert into fin.rmp_commission_default_history(rmp_id,commission_mode,commission_value,effective_from,changed_by)
select rmp_id,commission_mode,commission_value,effective_from,updated_by from fin.rmp_commission_defaults
on conflict(rmp_id,effective_from) do nothing;

create or replace function fin.rmp_set_default(
  p_rmp_id text, p_rmp_name text, p_rmp_mobile text, p_mode text, p_value numeric)
returns void language plpgsql security definer set search_path = fin, public, hr as $$
declare v_old jsonb; v_branch text; v_today date := (now() at time zone 'Asia/Kolkata')::date;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  if trim(coalesce(p_rmp_id,''))='' then raise exception 'RMP is required'; end if;
  select coalesce(x.branch,'') into v_branch from public.doctor_visits x where x.id=p_rmp_id;
  if not found then raise exception 'RMP not found'; end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this RMP branch'; end if;
  if upper(coalesce(p_mode,'')) not in ('PERCENT','AMOUNT') or p_value is null or p_value<0 then raise exception 'Valid commission mode and value are required'; end if;
  if upper(p_mode)='PERCENT' and p_value>100 then raise exception 'Percent cannot exceed 100'; end if;
  select to_jsonb(x) into v_old from fin.rmp_commission_defaults x where rmp_id=p_rmp_id;
  insert into fin.rmp_commission_defaults(rmp_id,rmp_name,rmp_mobile,commission_mode,commission_value,effective_from,updated_by)
  values(trim(p_rmp_id),coalesce(p_rmp_name,''),coalesce(p_rmp_mobile,''),upper(p_mode),p_value,v_today,hr.my_code())
  on conflict(rmp_id) do update set rmp_name=excluded.rmp_name,rmp_mobile=excluded.rmp_mobile,
    commission_mode=excluded.commission_mode,commission_value=excluded.commission_value,
    effective_from=v_today,updated_by=excluded.updated_by,updated_at=now();
  insert into fin.rmp_commission_default_history(rmp_id,commission_mode,commission_value,effective_from,changed_by)
  values(trim(p_rmp_id),upper(p_mode),p_value,v_today,hr.my_code())
  on conflict(rmp_id,effective_from) do update set commission_mode=excluded.commission_mode,
    commission_value=excluded.commission_value,changed_by=excluded.changed_by,changed_at=now();
  insert into fin.rmp_commission_audit(action,entity_id,old_value,new_value,changed_by)
  select 'SET_RMP_DEFAULT',p_rmp_id,v_old,to_jsonb(x),hr.my_code() from fin.rmp_commission_defaults x where x.rmp_id=p_rmp_id;
end $$;

-- Mark future "Use RMP Default" saves without changing the function signature.
create or replace function fin.rmp_set_patient_commission(
  p_patient_row_id text, p_rmp_id text, p_mode text default null, p_value numeric default null, p_set_on date default null)
returns uuid language plpgsql security definer set search_path = fin, public, hr as $$
declare p public.patients%rowtype; d fin.rmp_commission_defaults%rowtype;
  v_mode text; v_value numeric; v_date date; v_id uuid; v_old jsonb; v_use_default boolean;
  v_existing_set_on date; v_today date := (now() at time zone 'Asia/Kolkata')::date;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  select * into p from public.patients where id=p_patient_row_id;
  if not found then raise exception 'Patient not found'; end if;
  if not fin.rmp_can_write_branch(p.branch) then raise exception 'Not allowed for this patient branch'; end if;
  select * into d from fin.rmp_commission_defaults where rmp_id=p_rmp_id;
  v_use_default := nullif(trim(coalesce(p_mode,'')),'') is null;
  v_mode:=upper(nullif(trim(coalesce(p_mode,'')),'')); v_value:=p_value;
  if v_use_default then
    if not found then raise exception 'RMP default is not set'; end if;
    v_mode:=d.commission_mode; v_value:=d.commission_value;
  end if;
  if v_mode not in ('PERCENT','AMOUNT') or v_value is null or v_value<0 then raise exception 'Valid commission is required'; end if;
  if v_mode='PERCENT' and v_value>100 then raise exception 'Percent cannot exceed 100'; end if;
  v_date:=coalesce(p_set_on,v_today);
  if v_date>v_today then raise exception 'Future commission date is not allowed'; end if;
  if v_date<>v_today and not hr.is_master() then raise exception 'Master approval required for an old date'; end if;
  select x.set_on into v_existing_set_on from fin.rmp_patient_commissions x where x.patient_row_id=p_patient_row_id;
  if found and v_existing_set_on<v_today and not hr.is_master() then raise exception 'Master approval required to change an earlier commission'; end if;
  if exists(select 1 from fin.rmp_patient_commissions x where x.patient_row_id=p_patient_row_id and x.rmp_id<>p_rmp_id) then raise exception 'Use the protected RMP reassignment workflow'; end if;
  select to_jsonb(x) into v_old from fin.rmp_patient_commissions x where patient_row_id=p_patient_row_id;
  insert into fin.rmp_patient_commissions(patient_row_id,patient_code,patient_name,patient_mobile,treatment_branch,
    rmp_id,rmp_name,rmp_mobile,commission_mode,commission_value,set_on,set_by,use_rmp_default)
  values(p.id,coalesce(p."patientId",''),coalesce(p.name,''),coalesce(p.mobile,''),coalesce(p.branch,''),
    p_rmp_id,coalesce(nullif(d.rmp_name,''),p."refDoctor",''),coalesce(nullif(d.rmp_mobile,''),p."refDoctorMobile",''),
    v_mode,v_value,v_date,hr.my_code(),v_use_default)
  on conflict(patient_row_id) do update set rmp_id=excluded.rmp_id,rmp_name=excluded.rmp_name,
    rmp_mobile=excluded.rmp_mobile,commission_mode=excluded.commission_mode,commission_value=excluded.commission_value,
    set_on=excluded.set_on,set_by=excluded.set_by,use_rmp_default=excluded.use_rmp_default,updated_at=now()
  returning id into v_id;
  insert into fin.rmp_commission_audit(action,entity_id,old_value,new_value,changed_by)
  select 'SET_PATIENT_COMMISSION',v_id::text,v_old,to_jsonb(x),hr.my_code() from fin.rmp_patient_commissions x where x.id=v_id;
  return v_id;
end $$;

create or replace function fin.rmp_payment_day(p_text text) returns date language sql immutable as $$
  select case
    when trim(coalesce(p_text,'')) ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}' then left(trim(p_text),10)::date
    when trim(coalesce(p_text,'')) ~ '^[0-9]{2}[.][0-9]{2}[.][0-9]{4}$' then to_date(trim(p_text),'DD.MM.YYYY')
    else null end
$$;

create or replace function fin.rmp_summary(p_patient_row_id text)
returns table(final_bill numeric, net_treatment_paid numeric, earned numeric, paid numeric, due numeric, overpaid numeric)
language plpgsql security definer set search_path = fin, public, hr as $$
declare c fin.rmp_patient_commissions%rowtype; z record;
  v_bill numeric:=0; v_paid numeric:=0; v_refund numeric:=0; v_earned numeric:=0; v_given numeric:=0;
  v_remaining numeric:=0; v_net numeric:=0; v_base numeric:=0; v_counted_base numeric:=0;
  v_rate_mode text; v_rate_value numeric;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  select * into c from fin.rmp_patient_commissions where patient_row_id=p_patient_row_id;
  if not found then return query select 0::numeric,0::numeric,0::numeric,0::numeric,0::numeric,0::numeric; return; end if;
  if not fin.rmp_can_write_branch(c.treatment_branch) then raise exception 'Not allowed for this patient branch'; end if;
  select greatest(0,fin.rmp_safe_number(p."bill")-fin.rmp_safe_number(p."discount")) into v_bill from public.patients p where p.id=p_patient_row_id;
  select coalesce(sum(fin.rmp_safe_number(x."amount")),0) into v_paid from public.payments x
   where x."patientId"=p_patient_row_id and fin.rmp_is_treatment(x."payType",x."remarks");
  select coalesce(sum(fin.rmp_safe_number(r."amount")),0) into v_refund from public.payments r
   left join public.payments original on original.id=r."refundOfPaymentId"
   where r."patientId"=p_patient_row_id and lower(coalesce(r."payType",''))='refund'
     and lower(coalesce(r."refundApprovalStatus",''))='approved'
     and (trim(coalesce(r."refundOfPaymentId",''))='' or fin.rmp_is_treatment(original."payType",original."remarks"));
  v_paid:=greatest(0,v_paid-v_refund);
  if v_bill>0 and c.use_rmp_default then
    v_remaining:=v_bill;
    for z in
      select fin.rmp_payment_day(x."date") d,
        greatest(0,fin.rmp_safe_number(x."amount")-coalesce((select sum(fin.rmp_safe_number(r."amount")) from public.payments r
          where r."refundOfPaymentId"=x.id and lower(coalesce(r."payType",''))='refund'
            and lower(coalesce(r."refundApprovalStatus",''))='approved'),0)) net
      from public.payments x where x."patientId"=p_patient_row_id
        and fin.rmp_is_treatment(x."payType",x."remarks") and fin.rmp_payment_day(x."date") is not null
      order by fin.rmp_payment_day(x."date"),x.id
    loop
      exit when v_remaining<=0; v_base:=least(z.net,v_remaining); v_remaining:=v_remaining-v_base; v_counted_base:=v_counted_base+v_base;
      select h.commission_mode,h.commission_value into v_rate_mode,v_rate_value from fin.rmp_commission_default_history h
       where h.rmp_id=c.rmp_id and h.effective_from<=z.d order by h.effective_from desc limit 1;
      if not found then v_rate_mode:=c.commission_mode; v_rate_value:=c.commission_value; end if;
      if v_rate_mode='PERCENT' then v_earned:=v_earned+v_base*v_rate_value/100;
      else v_earned:=v_earned+v_base*v_rate_value/v_bill; end if;
    end loop;
    -- Preserve the old safe total if any historic row had an unreadable date.
    if v_remaining>0 and v_paid>(v_bill-v_remaining) then
      v_base:=least(v_remaining,v_paid-(v_bill-v_remaining));
      v_counted_base:=v_counted_base+v_base;
      if c.commission_mode='PERCENT' then v_earned:=v_earned+v_base*c.commission_value/100;
      else v_earned:=v_earned+v_base*c.commission_value/v_bill; end if;
    end if;
    if v_counted_base>least(v_paid,v_bill) and v_counted_base>0 then
      v_earned:=v_earned*least(v_paid,v_bill)/v_counted_base;
    end if;
  elsif v_bill>0 then
    if c.commission_mode='PERCENT' then v_earned:=least(v_paid,v_bill)*c.commission_value/100;
    else v_earned:=c.commission_value*least(v_paid,v_bill)/v_bill; end if;
  end if;
  select coalesce(sum(x.amount),0) into v_given from fin.rmp_commission_payments x where x.patient_commission_id=c.id;
  return query select round(v_bill,2),round(v_paid,2),round(v_earned,2),round(v_given,2),
    round(greatest(v_earned-v_given,0),2),round(greatest(v_given-v_earned,0),2);
end $$;

revoke all on fin.rmp_commission_default_history from public,anon;
grant select on fin.rmp_commission_default_history to authenticated;
