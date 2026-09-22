-- V384 — Referred-patient compact card: additive read-only RPC with real disease.
-- No existing function/table/data is changed or deleted.

create or replace function fin.rmp_legacy_view_all_v2(p_rmp_id text)
returns table(
  patient_row_id text, patient_code text, patient_name text,
  patient_mobile text, referral_date text, bill numeric, paid numeric,
  disease text
)
language plpgsql
stable
security definer
set search_path = fin, hr, public
as $$
declare
  v_master boolean := hr.is_master();
  v_branch text := '';
  v_name text := '';
  v_mobile text := '';
  v_rmp_branch text := '';
begin
  if not fin.rmp_can_use() then
    raise exception 'Master, Staff or Doctor identity required';
  end if;
  if trim(coalesce(p_rmp_id,''))='' then raise exception 'RMP is required'; end if;

  select lower(trim(coalesce(d.name,''))),
         right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10),
         coalesce(d.branch,'')
    into v_name,v_mobile,v_rmp_branch
  from public.doctor_visits d where d.id=p_rmp_id;
  if not found then raise exception 'RMP not found'; end if;

  if not v_master then
    select coalesce(s.branch,'') into v_branch
    from hr.staff_profiles s
    where s.person_code=hr.my_code() and s.active=true
    limit 1;
    if not found or trim(v_branch)='' then raise exception 'Active staff branch is required'; end if;
    if lower(trim(v_rmp_branch))<>lower(trim(v_branch)) then
      raise exception 'Not allowed for this RMP branch';
    end if;
  end if;

  return query
  with capped_patients as (
    select p.* from public.patients p
    order by p."updatedAt" desc nulls last limit 5000
  ), capped_payments as (
    select p.* from public.payments p
    order by p."updatedAt" desc nulls last limit 5000
  ), paid_by_mobile as (
    select right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) as mobile_key,
      sum(case
        when coalesce(p."payType",'') in ('visit_fee','attendance_mark') then 0
        when lower(coalesce(p."payType",''))='refund'
         and lower(coalesce(p."refundApprovalStatus",''))='approved'
          then -fin.rmp_safe_number(p.amount)
        when lower(coalesce(p."payType",''))='refund' then 0
        else fin.rmp_safe_number(p.amount)
      end) as paid_value
    from capped_payments p
    group by right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10)
  )
  select
    p.id,coalesce(p."patientId",''),coalesce(p.name,''),
    right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10),
    coalesce(nullif(trim(coalesce(p."registrationDate",'')),''),coalesce(p.date,'')),
    round(fin.rmp_safe_number(p.bill),2),round(coalesce(pm.paid_value,0),2),
    coalesce(nullif(trim(coalesce(p.disease,'')),''),nullif(trim(coalesce(p.diagnosis,'')),''),'')
  from capped_patients p
  left join paid_by_mobile pm on pm.mobile_key=
    right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10)
  where (v_master or trim(coalesce(p.branch,''))=''
         or lower(trim(p.branch))=lower(trim(v_branch)))
    and (
      (lower(trim(coalesce(p."refBy",'')))<>'' and lower(trim(p."refBy"))=v_name)
      or
      (length(right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10))=10
       and right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10)=v_mobile)
    )
  order by p."updatedAt" desc nulls last;
end
$$;

revoke all on function fin.rmp_legacy_view_all_v2(text) from public, anon;
grant execute on function fin.rmp_legacy_view_all_v2(text) to authenticated;
notify pgrst, 'reload schema';

-- Verification only: expected authenticated=true and anon=false.
select
  has_function_privilege('authenticated','fin.rmp_legacy_view_all_v2(text)','EXECUTE') as authenticated_can_call,
  has_function_privilege('anon','fin.rmp_legacy_view_all_v2(text)','EXECUTE') as anon_can_call;
