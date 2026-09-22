-- V1520 — RMP financial branch integrity hard guard
-- Purpose: a patient can only be linked to an RMP card from the SAME branch.
-- This is enforced in matching/backfill/RPC plus a table trigger so old clients
-- or accidental future code cannot cross-link commission money between branches.

create or replace function fin.rmp_match_rmp_for_patient(p_patient_row_id text)
returns table(rmp_id text, rmp_name text, rmp_mobile text, hits integer)
language sql stable security definer
set search_path = fin, public, hr
as $function$
  with p as (
    select lower(trim(coalesce(x."refDoctor",''))) as doc_name,
           lower(trim(coalesce(x."refBy",''))) as ref_by,
           right(regexp_replace(coalesce(x."refDoctorMobile",''),'[^0-9]','','g'),10) as doc_mob,
           lower(trim(coalesce(x.branch,''))) as pbranch
      from public.patients x where x.id = p_patient_row_id
  ), d as (
    select v.id, coalesce(v.name,'') as name, coalesce(v.mobile,'') as mobile,
           lower(trim(coalesce(v.name,''))) as name_key,
           right(regexp_replace(coalesce(v.mobile,''),'[^0-9]','','g'),10) as mob_key,
           lower(trim(coalesce(v.branch,''))) as dbranch
      from public.doctor_visits v
     where (v.status = 'Active' or v.status is null)
  ), m as (
    select distinct d.id, d.name, d.mobile
      from d, p
     where d.dbranch = p.pbranch
       and (
            (p.doc_name <> '' and p.doc_name = d.name_key)
         or (p.ref_by  <> '' and p.ref_by  = d.name_key)
         or (length(p.doc_mob) = 10 and p.doc_mob = d.mob_key)
       )
  )
  select m.id, m.name, m.mobile, (select count(*)::int from m)
    from m limit 1;
$function$;

create or replace function fin.rmp_refby_points_to(p_patient_row_id text, p_rmp_id text)
returns text
language sql stable security definer
set search_path = fin, public, hr
as $function$
  with p as (
    select lower(trim(coalesce(x."refDoctor",''))) as dn,
           lower(trim(coalesce(x."refBy",''))) as rb,
           right(regexp_replace(coalesce(x."refDoctorMobile",''),'[^0-9]','','g'),10) as dm,
           lower(trim(coalesce(x.branch,''))) as pbranch
      from public.patients x where x.id = p_patient_row_id
  ), hit as (
    select v.id
      from public.doctor_visits v, p
     where lower(trim(coalesce(v.branch,''))) = p.pbranch
       and (
            (p.dn <> '' and p.dn = lower(trim(coalesce(v.name,''))))
         or (p.rb <> '' and p.rb = lower(trim(coalesce(v.name,''))))
         or (length(p.dm) = 10 and p.dm = right(regexp_replace(coalesce(v.mobile,''),'[^0-9]','','g'),10))
       )
  )
  select case
    when exists (select 1 from hit where hit.id = p_rmp_id) then 'OK'
    when exists (select 1 from hit) then 'MISMATCH'
    else 'NO_REFBY' end;
$function$;

create or replace function fin.rmp_set_patient_commission(
  p_patient_row_id text, p_rmp_id text, p_mode text default null,
  p_value numeric default null, p_set_on date default null)
returns uuid
language plpgsql security definer
set search_path = fin, public, hr
as $function$
declare
  p public.patients%rowtype;
  d fin.rmp_commission_defaults%rowtype;
  bd fin.rmp_commission_branch_defaults%rowtype;
  v_mode text; v_value numeric; v_date date; v_id uuid; v_old jsonb;
  v_default_found boolean:=false; v_branch_default_found boolean:=false;
  v_existing_set_on date; v_rmp_branch text;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;

  select * into p from public.patients where id=p_patient_row_id;
  if not found then raise exception 'Patient not found'; end if;
  if not fin.rmp_can_write_branch(p.branch) then raise exception 'Not allowed for this patient branch'; end if;

  select coalesce(v.branch,'') into v_rmp_branch
    from public.doctor_visits v where v.id=p_rmp_id;
  if not found then raise exception 'RMP not found'; end if;
  if lower(trim(coalesce(v_rmp_branch,''))) <> lower(trim(coalesce(p.branch,''))) then
    raise exception 'RMP branch mismatch: patient is %, RMP is %', coalesce(p.branch,''), coalesce(v_rmp_branch,'');
  end if;

  perform fin.rmp_guard_refby(p_patient_row_id, p_rmp_id);

  select * into bd from fin.rmp_commission_branch_defaults where rmp_id=p_rmp_id and branch=p.branch;
  v_branch_default_found:=found;
  select * into d from fin.rmp_commission_defaults where rmp_id=p_rmp_id;
  v_default_found:=found;
  v_mode:=upper(nullif(trim(coalesce(p_mode,'')),'')); v_value:=p_value;
  if v_mode is null then
    if v_branch_default_found then
      v_mode:=bd.commission_mode; v_value:=bd.commission_value;
    elsif v_default_found then
      v_mode:=d.commission_mode; v_value:=d.commission_value;
    else
      v_mode:='PERCENT'; v_value:=fin.rmp_auto_default_percent();
    end if;
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
    treatment_branch=excluded.treatment_branch,
    set_on=excluded.set_on,set_by=excluded.set_by,updated_at=now()
  returning id into v_id;
  insert into fin.rmp_commission_audit(action,entity_id,old_value,new_value,changed_by)
  select 'SET_PATIENT_COMMISSION',v_id::text,v_old,to_jsonb(x),hr.my_code()
    from fin.rmp_patient_commissions x where x.id=v_id;
  return v_id;
end $function$;

create or replace function fin.rmp_backfill_commissions(p_rmp_id text)
returns integer
language plpgsql security definer
set search_path = fin, public, hr
as $function$
declare v_name text; v_mobile text; v_branch text;
        v_match_name text; v_match_mobile text;
        r record; v_made int := 0;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;

  if exists(
       select 1 from fin.rmp_commission_backfill_done x
        where x.rmp_id = p_rmp_id
          and (x.done_at at time zone 'Asia/Kolkata')::date
              = (now() at time zone 'Asia/Kolkata')::date
     ) then
    return 0;
  end if;

  select coalesce(d.name,''), coalesce(d.mobile,''), coalesce(d.branch,'')
    into v_name, v_mobile, v_branch
    from public.doctor_visits d where d.id=p_rmp_id;
  if not found then raise exception 'RMP not found'; end if;
  if trim(v_branch)='' then raise exception 'RMP branch is required'; end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this RMP branch'; end if;

  v_match_name   := lower(trim(v_name));
  v_match_mobile := right(regexp_replace(v_mobile,'[^0-9]','','g'),10);

  for r in
    select p.id as pid, coalesce(p.branch,'') as pbranch
      from public.patients p
     where lower(trim(coalesce(p.branch,''))) = lower(trim(v_branch))
       and (
         (v_match_name <> '' and (
              lower(trim(coalesce(p."refBy",'')))     = v_match_name
           or lower(trim(coalesce(p."refDoctor",''))) = v_match_name))
         or
         (length(v_match_mobile) = 10 and
          right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10) = v_match_mobile)
       )
       and not exists(select 1 from fin.rmp_patient_commissions c where c.patient_row_id=p.id)
       and exists(
             select 1 from public.payments x
              where x."patientId"=p.id
                and fin.rmp_is_treatment(x."payType",x."remarks")
                and fin.rmp_safe_number(x."amount") > 0)
  loop
    begin
      if fin.rmp_can_write_branch(r.pbranch) then
        perform fin.rmp_set_patient_commission(r.pid, p_rmp_id);
        v_made := v_made + 1;
      end if;
    exception when others then
      null;
    end;
  end loop;

  insert into fin.rmp_commission_backfill_done(rmp_id, created_rows, done_by)
  values (p_rmp_id, v_made, hr.my_code())
  on conflict (rmp_id) do update
    set done_at      = now(),
        created_rows = fin.rmp_commission_backfill_done.created_rows + excluded.created_rows,
        done_by      = excluded.done_by;

  return v_made;
end $function$;

create or replace function fin.rmp_enforce_patient_rmp_branch()
returns trigger
language plpgsql security definer
set search_path = fin, public, hr
as $function$
declare v_patient_branch text; v_rmp_branch text;
begin
  select coalesce(p.branch,'') into v_patient_branch from public.patients p where p.id=new.patient_row_id;
  if not found then raise exception 'RMP commission blocked: patient not found'; end if;
  select coalesce(d.branch,'') into v_rmp_branch from public.doctor_visits d where d.id=new.rmp_id;
  if not found then raise exception 'RMP commission blocked: RMP not found'; end if;
  if trim(v_patient_branch)='' or trim(v_rmp_branch)='' then
    raise exception 'RMP commission blocked: patient/RMP branch is missing';
  end if;
  if lower(trim(v_patient_branch)) <> lower(trim(v_rmp_branch)) then
    raise exception 'RMP commission blocked: patient branch % does not match RMP branch %', v_patient_branch, v_rmp_branch;
  end if;
  new.treatment_branch := v_patient_branch;
  return new;
end $function$;

revoke all on function fin.rmp_enforce_patient_rmp_branch() from public, anon;
drop trigger if exists trg_rmp_patient_branch_guard on fin.rmp_patient_commissions;
create trigger trg_rmp_patient_branch_guard
before insert or update of patient_row_id, rmp_id, treatment_branch
on fin.rmp_patient_commissions
for each row execute function fin.rmp_enforce_patient_rmp_branch();

create or replace function fin.rmp_guard_patient_branch_change()
returns trigger
language plpgsql security definer
set search_path = fin, public, hr
as $function$
begin
  if lower(trim(coalesce(new.branch,''))) is distinct from lower(trim(coalesce(old.branch,''))) then
    if exists(
      select 1 from fin.rmp_patient_commissions c
      join public.doctor_visits d on d.id=c.rmp_id
      where c.patient_row_id=new.id
        and lower(trim(coalesce(d.branch,''))) <> lower(trim(coalesce(new.branch,'')))
    ) then
      raise exception 'Patient branch change blocked: linked RMP belongs to another branch. Reassign RMP first.';
    end if;
  end if;
  return new;
end $function$;

revoke all on function fin.rmp_guard_patient_branch_change() from public, anon;
drop trigger if exists trg_patient_rmp_branch_change_guard on public.patients;
create trigger trg_patient_rmp_branch_change_guard
before update of branch on public.patients
for each row execute function fin.rmp_guard_patient_branch_change();

create or replace function fin.rmp_guard_rmp_branch_change()
returns trigger
language plpgsql security definer
set search_path = fin, public, hr
as $function$
begin
  if lower(trim(coalesce(new.branch,''))) is distinct from lower(trim(coalesce(old.branch,''))) then
    if exists(
      select 1 from fin.rmp_patient_commissions c
      join public.patients p on p.id=c.patient_row_id
      where c.rmp_id=new.id
        and lower(trim(coalesce(p.branch,''))) <> lower(trim(coalesce(new.branch,'')))
    ) then
      raise exception 'RMP branch change blocked: linked patients belong to another branch. Reassign them first.';
    end if;
  end if;
  return new;
end $function$;

revoke all on function fin.rmp_guard_rmp_branch_change() from public, anon;
drop trigger if exists trg_doctor_visit_rmp_branch_change_guard on public.doctor_visits;
create trigger trg_doctor_visit_rmp_branch_change_guard
before update of branch on public.doctor_visits
for each row execute function fin.rmp_guard_rmp_branch_change();

notify pgrst, 'reload schema';
