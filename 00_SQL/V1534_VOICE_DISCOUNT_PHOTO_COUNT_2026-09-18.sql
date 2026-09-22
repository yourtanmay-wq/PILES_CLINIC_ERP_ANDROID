-- 🎤🔒 V1534 (১৮.০৯.২০২৬, TK-নির্দেশ) — Search-এ দুটো নতুন প্রশ্ন:
--   ১) "শুধু ডিসকাউন্ট কত হয়েছে" (রিফান্ড থেকে আলাদা) — patients."discount"
--   ২) "এই মাসে কতজন রোগীর ছবি তোলা হয়েছে" — patients."photo"
-- ⛔ শুধু SELECT পড়া — কোনো টেবিল/ট্রিগার/কলাম বদলায় না।
-- ⛔ সংখ্যায় বদলানোর নিয়ম collection_summary-র হুবহু একই (regexp_replace)।

create or replace function reports.discount_summary(p_branch text, p_from date, p_to date)
returns table(total numeric, patient_count int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select coalesce(sum(coalesce((substring(coalesce(p."discount",'') from '([0-9]+\.?[0-9]*)'))::numeric,0)),0),
           count(*) filter (where coalesce((substring(coalesce(p."discount",'') from '([0-9]+\.?[0-9]*)'))::numeric,0) > 0)::int
      from public.patients p
     where p."branch" = p_branch
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) <= to_char(p_to,'YYYY-MM-DD');
end $$;
revoke all on function reports.discount_summary(text, date, date) from public, anon;
grant execute on function reports.discount_summary(text, date, date) to authenticated;

create or replace function reports.discount_list(p_branch text, p_from date, p_to date)
returns table(patient_row_id text, patient_code text, name text, mobile text, amount numeric, reg_date text, branch text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p.id, coalesce(p."patientId",''), coalesce(p.name,''), coalesce(p.mobile,''),
           coalesce((substring(coalesce(p."discount",'') from '([0-9]+\.?[0-9]*)'))::numeric,0),
           left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) as reg_date,
           coalesce(p."branch",'')
      from public.patients p
     where p."branch" = p_branch
       and coalesce((substring(coalesce(p."discount",'') from '([0-9]+\.?[0-9]*)'))::numeric,0) > 0
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) <= to_char(p_to,'YYYY-MM-DD')
     order by reg_date desc, p.name
     limit 500;
end $$;
revoke all on function reports.discount_list(text, date, date) from public, anon;
grant execute on function reports.discount_list(text, date, date) to authenticated;

create or replace function reports.patient_photo_count(p_branch text, p_from date, p_to date)
returns table(total int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select count(*)::int from public.patients p
     where p."branch" = p_branch
       and coalesce(p."photo",'') <> ''
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) <= to_char(p_to,'YYYY-MM-DD');
end $$;
revoke all on function reports.patient_photo_count(text, date, date) from public, anon;
grant execute on function reports.patient_photo_count(text, date, date) to authenticated;

create or replace function reports.patient_photo_list(p_branch text, p_from date, p_to date)
returns table(patient_row_id text, patient_code text, name text, mobile text, reg_date text, branch text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p.id, coalesce(p."patientId",''), coalesce(p.name,''), coalesce(p.mobile,''),
           left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) as reg_date,
           coalesce(p."branch",'')
      from public.patients p
     where p."branch" = p_branch
       and coalesce(p."photo",'') <> ''
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) <= to_char(p_to,'YYYY-MM-DD')
     order by reg_date desc, p.name
     limit 500;
end $$;
revoke all on function reports.patient_photo_list(text, date, date) from public, anon;
grant execute on function reports.patient_photo_list(text, date, date) to authenticated;
