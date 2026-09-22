-- ============================================================================
-- V328 — RMP SAFE CARD COUNTS FOUNDATION (ADDITIVE / NOT LIVE IN APP YET)
--
-- PURPOSE
--   Return only RMP id + current referred-patient count. Android can later
--   avoid downloading up to 5,000 patient rows merely to calculate 12 counts.
--
-- SAFETY
--   * Does not insert, update or delete any patient/RMP/payment/commission row.
--   * Does not alter any existing table, policy, trigger or function.
--   * Not used by Android until a later, separately verified code change.
--   * anon/public cannot call it; authenticated Master/Staff/Doctor only.
--   * Master keeps current all-patient counting behavior.
--   * Staff/Doctor are limited to their own branch; blank legacy patient
--     branch remains included exactly like current Android behavior.
-- ============================================================================

create or replace function fin.rmp_legacy_card_counts()
returns table(rmp_id text, referred_count bigint)
language plpgsql
stable
security definer
set search_path = fin, hr, public
as $$
declare
  v_master boolean := hr.is_master();
  v_branch text := '';
begin
  if not fin.rmp_can_use() then
    raise exception 'Master, Staff or Doctor identity required';
  end if;

  if not v_master then
    select coalesce(s.branch,'') into v_branch
      from hr.staff_profiles s
     where s.person_code=hr.my_code() and s.active=true
     limit 1;
    if not found or trim(v_branch)='' then
      raise exception 'Active staff branch is required';
    end if;
  end if;

  return query
  with doctor_rows as (
    select
      d.id,
      lower(trim(coalesce(d.name,''))) as match_name,
      right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) as match_mobile
    from public.doctor_visits d
    where (d.status='Active' or d.status is null)
      and (v_master or lower(trim(coalesce(d.branch,'')))=lower(trim(v_branch)))
  ), patient_rows as (
    select
      p.id,
      lower(trim(coalesce(p."refBy",''))) as match_name,
      right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10) as match_mobile
    from public.patients p
    where v_master
       or trim(coalesce(p.branch,''))=''
       or lower(trim(p.branch))=lower(trim(v_branch))
  ), matched as (
    select distinct d.id as rmp_id, p.id as patient_row_id
    from doctor_rows d
    join patient_rows p on
         (p.match_name<>'' and p.match_name=d.match_name)
      or (length(p.match_mobile)=10 and p.match_mobile=d.match_mobile)
  )
  select m.rmp_id,count(*)::bigint
  from matched m
  group by m.rmp_id;
end
$$;

revoke all on function fin.rmp_legacy_card_counts() from public, anon;
grant execute on function fin.rmp_legacy_card_counts() to authenticated;

notify pgrst, 'reload schema';

-- Safe confirmation only; expected: authenticated_can_call = true,
-- anon_can_call = false. This SELECT changes nothing.
select
  has_function_privilege('authenticated','fin.rmp_legacy_card_counts()','EXECUTE') as authenticated_can_call,
  has_function_privilege('anon','fin.rmp_legacy_card_counts()','EXECUTE') as anon_can_call;

