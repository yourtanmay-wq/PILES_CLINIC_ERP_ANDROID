-- RMP direct payment: Paid increases and aggregate Due decreases immediately.
-- Allocation to a patient later only moves the same money; it never counts twice.

create or replace function fin.rmp_rmp_summary(p_rmp_id text)
returns table(patient_count bigint, earned numeric, paid_to_this_rmp numeric,
              previous_rmp_paid numeric, due numeric, overpaid numeric)
language plpgsql security definer set search_path = fin, public, hr as $$
declare c record; s record; v_count bigint:=0; v_earned numeric:=0; v_due numeric:=0;
        v_over numeric:=0; v_paid numeric:=0; v_previous numeric:=0;
        v_unallocated numeric:=0; v_branch text;
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
  select coalesce(sum(x.amount),0) into v_paid
    from fin.rmp_commission_payments x where x.rmp_id=p_rmp_id;
  select coalesce(sum(x.amount-x.allocated_amount),0) into v_unallocated
    from fin.rmp_advance_payments x where x.rmp_id=p_rmp_id;
  select coalesce(sum(x.amount),0) into v_previous
    from fin.rmp_commission_payments x join fin.rmp_patient_commissions pc on pc.id=x.patient_commission_id
   where pc.rmp_id=p_rmp_id and x.rmp_id<>p_rmp_id;
  return query select v_count,round(v_earned,2),round(v_paid+v_unallocated,2),round(v_previous,2),
    round(greatest(0,v_due-v_unallocated),2),round(v_over+greatest(0,v_unallocated-v_due),2);
end $$;

create or replace function fin.rmp_record_advance(
  p_rmp_id text, p_amount numeric, p_paid_on date, p_mode text, p_reference_no text default null)
returns uuid language plpgsql security definer set search_path=fin,public,hr as $$
declare d record; v_expense uuid; v_id uuid;
begin
  if not hr.is_master() then raise exception 'Only Master can record an unallocated RMP payment'; end if;
  if p_amount is null or p_amount<=0 then raise exception 'Amount must be greater than zero'; end if;
  if p_paid_on is null or p_paid_on>(now() at time zone 'Asia/Kolkata')::date then
    raise exception 'Future payment date is not allowed';
  end if;
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

revoke all on function fin.rmp_rmp_summary(text),fin.rmp_record_advance(text,numeric,date,text,text) from public,anon;
grant execute on function fin.rmp_rmp_summary(text),fin.rmp_record_advance(text,numeric,date,text,text) to authenticated;
notify pgrst,'reload schema';

-- READ-ONLY proof: no payment row is changed here.
select has_function_privilege('authenticated','fin.rmp_rmp_summary(text)','EXECUTE') as summary_ready,
       has_function_privilege('authenticated','fin.rmp_record_advance(text,numeric,date,text,text)','EXECUTE') as payment_ready;
