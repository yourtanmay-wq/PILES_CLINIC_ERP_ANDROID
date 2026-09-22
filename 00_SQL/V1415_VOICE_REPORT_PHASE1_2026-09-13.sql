-- ═══════════════════════════════════════════════════════════════════════════
-- 🎤🔒 V1415 (১৩.০৯.২০২৬, TK-নির্দেশ "কাজ শুরু করে দিন", তালিকা ৫২০) —
-- ভয়েসে/টাইপ করে প্রশ্নের জবাব — প্রথম ধাপ (৪৮টার মধ্যে প্রথম ২টা):
--   ১) "[তারিখ] [ব্রাঞ্চ]-এ কতজন পেশেন্ট এসেছিল" — রেজিস্ট্রেশন-সংখ্যা
--   ২) "[তারিখ] [ব্রাঞ্চ]-এ কত কালেকশন হয়েছে" — জমা-টাকার মোট
-- প্রতিটার জন্য দুটো ফাংশন — একটা সংখ্যা/যোগফল (উত্তরে দেখানোর জন্য), আরেকটা
-- আসল তালিকা (উত্তরে চাপ দিলে যে পাতা খুলবে তার জন্য) — Master-এর RMP-খাতার
-- মতোই ব্রাঞ্চ-পাহারা (নিজের ব্রাঞ্চ ছাড়া দেখা যাবে না, Master সব দেখেন)।
-- ⛔ শুধু পড়া, কোনো টেবিল বদলায় না। শুধু ছোট যোগফল/গণনা ফেরত আসে —
--    হাজার হাজার সারি টেনে আনা হয় না (ফ্রি প্ল্যান-নিরাপদ)।
-- ═══════════════════════════════════════════════════════════════════════════
begin;

create schema if not exists reports;

-- 🔒 TK-নির্দেশ (১৩.০৯.২০২৬): "ভয়েস কমান্ড আপাতত শুধু মাস্টারের জন্য থাকবে —
-- ভবিষ্যতে অন্যদের জন্য চালু করতে চাইলে তখন চালু হবে।" তাই এখন শুধু Master;
-- নিজের ব্রাঞ্চের স্টাফকেও দেখানো হলে ভবিষ্যতে নিচের `or exists(...)` অংশটা
-- আনকমেন্ট করলেই যথেষ্ট — বাকি কোনো ফাংশন/অ্যাপ-কোড বদলাতে হবে না।
create or replace function reports.can_access_branch(p_branch text) returns boolean
language sql stable security definer set search_path = hr, public as $$
  select hr.is_master()
  -- or exists(
  --   select 1 from hr.staff_profiles s
  --    where s.person_code = hr.my_code() and s.active is not false
  --      and lower(trim(coalesce(s.branch,''))) = lower(trim(coalesce(p_branch,'')))
  -- )
$$;
revoke all on function reports.can_access_branch(text) from public, anon;
grant execute on function reports.can_access_branch(text) to authenticated;

-- ── ১) কতজন পেশেন্ট রেজিস্ট্রেশন হয়েছে ───────────────────────────────────
create or replace function reports.patients_registered_count(p_branch text, p_from date, p_to date)
returns int language sql stable security definer set search_path = hr, public as $$
  select case when not reports.can_access_branch(p_branch) then null::int else (
    select count(*)::int from public.patients p
     where p."branch" = p_branch
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) <= to_char(p_to,'YYYY-MM-DD')
  ) end
$$;
revoke all on function reports.patients_registered_count(text, date, date) from public, anon;
grant execute on function reports.patients_registered_count(text, date, date) to authenticated;

create or replace function reports.patients_registered_list(p_branch text, p_from date, p_to date)
returns table(patient_row_id text, patient_code text, name text, mobile text, registration_date text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p.id, coalesce(p."patientId",''), coalesce(p.name,''), coalesce(p.mobile,''),
           left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10)
      from public.patients p
     where p."branch" = p_branch
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) <= to_char(p_to,'YYYY-MM-DD')
     order by left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) desc, p.name
     limit 500;
end $$;
revoke all on function reports.patients_registered_list(text, date, date) from public, anon;
grant execute on function reports.patients_registered_list(text, date, date) to authenticated;

-- ── ২) কত টাকা কালেকশন হয়েছে (রিফান্ড বাদ, Approved রিফান্ড বিয়োগ) ────────
create or replace function reports.collection_summary(p_branch text, p_from date, p_to date)
returns table(total numeric, patient_count int, payment_count int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    with rows0 as (
      select y.id, y."patientId",
             case when lower(coalesce(y."payType",'')) = 'refund'
                       and lower(coalesce(y."refundApprovalStatus",'')) = 'approved'
                  then -coalesce(nullif(regexp_replace(coalesce(y."amount",'0'),'[^0-9.\-]','','g'),'')::numeric,0)
                  when lower(coalesce(y."payType",'')) = 'refund' then 0
                  else coalesce(nullif(regexp_replace(coalesce(y."amount",'0'),'[^0-9.\-]','','g'),'')::numeric,0)
             end as eff
        from public.payments y
       where y."branch" = p_branch
         and left(coalesce(y."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
         and left(coalesce(y."date",''),10) <= to_char(p_to,'YYYY-MM-DD')
    )
    select coalesce(sum(eff),0), count(distinct "patientId")::int, count(*)::int from rows0;
end $$;
revoke all on function reports.collection_summary(text, date, date) from public, anon;
grant execute on function reports.collection_summary(text, date, date) to authenticated;

create or replace function reports.collection_list(p_branch text, p_from date, p_to date)
returns table(payment_id text, patient_row_id text, name text, mobile text, amount numeric, mode text, pay_type text, paid_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select y.id, coalesce(y."patientId",''), coalesce(p.name,''), coalesce(y.mobile,''),
           coalesce(nullif(regexp_replace(coalesce(y."amount",'0'),'[^0-9.\-]','','g'),'')::numeric,0),
           coalesce(y."mode",''), coalesce(y."payType",''), left(coalesce(y."date",''),10)
      from public.payments y
      left join public.patients p on p.id = y."patientId"
     where y."branch" = p_branch
       and left(coalesce(y."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(y."date",''),10) <= to_char(p_to,'YYYY-MM-DD')
       and not (lower(coalesce(y."payType",'')) = 'refund' and lower(coalesce(y."refundApprovalStatus",'')) <> 'approved')
     order by y."date" desc
     limit 500;
end $$;
revoke all on function reports.collection_list(text, date, date) from public, anon;
grant execute on function reports.collection_list(text, date, date) to authenticated;

notify pgrst, 'reload schema';
commit;
