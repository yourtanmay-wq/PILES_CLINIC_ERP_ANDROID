-- ============================================================================
-- V328 — MASTER-ONLY RMP PERFORMANCE FOUNDATION (ADDITIVE / NOT LIVE YET)
-- No patient/RMP/payment/commission/expense row is changed.
-- Reproduces current branch filtering BEFORE the newest-5,000 patient limit.
-- ============================================================================

create or replace function fin.rmp_legacy_performance(p_branch text default null)
returns table(
  rmp_id text, this_month_count bigint, all_time_count bigint,
  referral_paid numeric, most_recent_date text
)
language plpgsql
stable
security definer
set search_path = fin, hr, public
as $$
declare
  v_branch text := nullif(trim(coalesce(p_branch,'')),'');
begin
  if not hr.is_master() then raise exception 'Master only'; end if;
  if v_branch='All' then v_branch:=null; end if;
  if v_branch is not null and v_branch not in
    ('Kishanganj','Jalpaiguri','Cooch Behar','Falakata','Birpara') then
    raise exception 'Invalid branch';
  end if;

  return query
  with doctors as (
    select d.id,fin.rmp_safe_number(d."referralPaid") as paid_value,
      lower(trim(coalesce(d.name,''))) as match_name,
      right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) as match_mobile
    from public.doctor_visits d
    where (d.status='Active' or d.status is null)
      and (v_branch is null or d.branch=v_branch)
  ), capped_patients as (
    select p.* from public.patients p
    where v_branch is null or p.branch=v_branch
    order by p."updatedAt" desc nulls last limit 5000
  ), matched as (
    select distinct d.id as rmp_id,d.paid_value,p.id as patient_row_id,
      coalesce(nullif(trim(coalesce(p."registrationDate",'')),''),coalesce(p.date,'')) as referral_date
    from doctors d join capped_patients p on
         (lower(trim(coalesce(p."refBy",'')))<>'' and lower(trim(p."refBy"))=d.match_name)
      or (length(right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10))=10
          and right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10)=d.match_mobile)
  )
  select m.rmp_id,
    count(*) filter (where left(m.referral_date,7)=
      to_char(now() at time zone 'Asia/Kolkata','YYYY-MM'))::bigint,
    count(*)::bigint,round(max(m.paid_value),2),coalesce(max(nullif(m.referral_date,'')),'')
  from matched m
  group by m.rmp_id
  order by max(nullif(m.referral_date,'')) desc nulls last,m.rmp_id;
end
$$;

revoke all on function fin.rmp_legacy_performance(text) from public, anon, authenticated;
grant execute on function fin.rmp_legacy_performance(text) to authenticated;
notify pgrst, 'reload schema';

select
  has_function_privilege('authenticated','fin.rmp_legacy_performance(text)','EXECUTE') as authenticated_has_doorway,
  has_function_privilege('anon','fin.rmp_legacy_performance(text)','EXECUTE') as anon_can_call;

