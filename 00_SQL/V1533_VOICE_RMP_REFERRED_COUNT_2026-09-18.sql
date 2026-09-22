-- 🎤🔒 V1533 (১৮.০৯.২০২৬, TK-নির্দেশ) — Search-এর নতুন প্রশ্ন:
-- "গত সপ্তাহে কতজন পেশেন্ট RMP পাঠিয়েছে" / "গত মাসে কতজন পেশেন্ট RMP পাঠানো"
-- ⛔ শুধু SELECT পড়া — কোনো টেবিল/ট্রিগার/কলাম বদলায় না।
-- ⛔ patients."refBy" = 'Dr. Visit' মানেই RMP/ডাক্তার রেফার করা রোগী
--    (RegistrationActivity.kt-এর refByOptions লিস্টে এই একই মান, TK-অনুমোদিত)।
-- ⛔ গোনা হয় registrationDate (না থাকলে date) দিয়ে — অন্য সব voice-প্রশ্নের
--    (V1503 patients_visited_list) হুবহু একই তারিখ-ধরার নিয়ম।

create or replace function reports.rmp_referred_list(p_branch text, p_from date, p_to date)
returns table(patient_row_id text, patient_code text, name text, mobile text, ref_doctor text, reg_date text, branch text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p.id, coalesce(p."patientId",''), coalesce(p.name,''), coalesce(p.mobile,''),
           coalesce(p."refDoctor",''),
           left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) as reg_date,
           coalesce(p."branch",'')
      from public.patients p
     where p."branch" = p_branch
       and lower(trim(coalesce(p."refBy",''))) = 'dr. visit'
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) <= to_char(p_to,'YYYY-MM-DD')
     order by reg_date desc, p.name
     limit 500;
end $$;
revoke all on function reports.rmp_referred_list(text, date, date) from public, anon;
grant execute on function reports.rmp_referred_list(text, date, date) to authenticated;

create or replace function reports.rmp_referred_count(p_branch text, p_from date, p_to date)
returns table(total int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  return query select count(*)::int from reports.rmp_referred_list(p_branch, p_from, p_to);
end $$;
revoke all on function reports.rmp_referred_count(text, date, date) from public, anon;
grant execute on function reports.rmp_referred_count(text, date, date) to authenticated;
