-- V1520 — legacy Referral Income duplicate + total guard
create or replace function fin.guard_legacy_referral_payments()
returns trigger
language plpgsql
security definer
set search_path = fin, public, pg_temp
as $function$
declare
  v_paid numeric := 0;
  v_due numeric := 0;
  v_dup boolean := false;
begin
  if new."referralPayments" is null then new."referralPayments" := '[]'::jsonb; end if;
  if jsonb_typeof(new."referralPayments") <> 'array' then
    raise exception 'Referral Income blocked: referralPayments must be an array';
  end if;

  select exists(
    select 1
      from jsonb_array_elements(new."referralPayments") e
     where right(regexp_replace(coalesce(e->>'patientMobile',''),'[^0-9]','','g'),10) <> ''
       and coalesce(e->>'date','') <> ''
       and fin.rmp_safe_number(e->>'amount') > 0
     group by right(regexp_replace(coalesce(e->>'patientMobile',''),'[^0-9]','','g'),10),
              coalesce(e->>'date',''), round(fin.rmp_safe_number(e->>'amount'),2)
    having count(*) > 1
  ) into v_dup;
  if v_dup then raise exception 'Duplicate Referral Income blocked: same patient, date and amount already exists'; end if;

  select
    coalesce(sum(case when lower(trim(coalesce(e->>'status','')))='paid' then fin.rmp_safe_number(e->>'amount') else 0 end),0),
    coalesce(sum(case when lower(trim(coalesce(e->>'status','')))='paid' then 0 else fin.rmp_safe_number(e->>'amount') end),0)
    into v_paid, v_due
    from jsonb_array_elements(new."referralPayments") e;
  new."referralPaid" := v_paid::text;
  new."referralDue" := v_due::text;
  return new;
end
$function$;

revoke all on function fin.guard_legacy_referral_payments() from public, anon;
drop trigger if exists trg_guard_legacy_referral_payments on public.doctor_visits;
create trigger trg_guard_legacy_referral_payments
before insert or update of "referralPayments" on public.doctor_visits
for each row execute function fin.guard_legacy_referral_payments();
