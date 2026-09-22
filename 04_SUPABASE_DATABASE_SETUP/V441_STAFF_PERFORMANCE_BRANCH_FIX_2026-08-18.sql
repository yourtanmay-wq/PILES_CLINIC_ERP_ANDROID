-- ============================================================================
-- V441 — V440-এর তালিকাগুলো "BRANCH (BRANCH)" সারিতেও কাজ করবে (TK-রিপোর্ট
-- ১৮.০৮.২০২৬-এর আগে নিজেই ধরা পড়েছে, TK-কে জানিয়ে ঠিক করা হলো)।
--
-- আসল কারণ: V440-এর ৫টা তালিকা-ফাংশন `hr.staff_profiles`-এ p_code খুঁজে
-- স্টাফের মোবাইল বের করত। কিন্তু "BRANCH-Cooch Behar"-এর মতো ব্রাঞ্চ-সারি
-- (V428, চেম্বারের সাধারণ নম্বরে করা কাজ) কোনো স্টাফ নয় — staff_profiles-এ
-- এই কোড নেই। তাই ওই সারির Cash/Online/Enquiry/Registration/Treatment/RMP
-- সংখ্যায় চাপ দিলে তালিকা **ফাঁকা** দেখাত, যদিও সংখ্যাটা ০ নয়।
--
-- সমাধান: p_code "BRANCH-" দিয়ে শুরু হলে hr.branch_performance()-এর ঠিক
-- একই নিয়মে (branch=নাম, আর কোনো active staff-এর মোবাইলের সাথে না মেলা)
-- সারি বাছা হয় — নইলে আগের মতোই স্টাফ-ভিত্তিক বাছাই।
--
-- ⛔ App calls/Outside calls/Days present — ব্রাঞ্চ-সারিতে এমনিতেই সবসময় ০
--    (branch_performance-এ হার্ডকোড করা), তাই ওই তিনটের তালিকা-ফাংশন
--    ছোঁয়ার দরকার নেই — শূন্য দেখালেই ঠিক আছে।
-- ⛔ স্টাফ-ভিত্তিক পুরনো ফলাফল **এক অক্ষরও বদলায়নি**।
-- ============================================================================

create or replace function hr.perf_enquiry_list(p_month text, p_code text)
returns table(enq_date text, name text, mobile text, branch text, disease text)
language sql stable security definer set search_path = hr, public, wn
as $fn$
  with guard as (select 1 as ok where hr.is_master()),
  mon as (
    select k as key, length(k) as klen from (
      select case
        when coalesce(p_month,'') ~ '^\d{4}-\d{2}-\d{2}$' then p_month
        when coalesce(p_month,'') ~ '^\d{4}-\d{2}$'        then p_month
        else to_char((now() at time zone 'Asia/Kolkata')::date, 'YYYY-MM') end as k
    ) q
  ), st as (
    select hr.perf_m10(s.link_mobile) as m10
    from hr.staff_profiles s where s.person_code = p_code
  ), known as (
    select hr.perf_m10(s.link_mobile) as m10 from hr.staff_profiles s
    where s.active is not false and lower(coalesce(s.role_kind,'')) <> 'doctor'
      and upper(coalesce(s.person_code,'')) not like 'DR-%'
      and length(hr.perf_m10(s.link_mobile)) = 10
  )
  select e."date", e."name", e."mobile", e."branch", e."disease"
  from public.enquiries e, mon, guard
  where left(coalesce(e."date", ''), mon.klen) = mon.key
    and (
      (p_code like 'BRANCH-%' and e."branch" = substring(p_code from 8)
        and length(hr.perf_m10(coalesce(nullif(e."receivedBy",''), e."createdBy"))) = 10
        and hr.perf_m10(coalesce(nullif(e."receivedBy",''), e."createdBy")) not in (select m10 from known))
      or
      (p_code not like 'BRANCH-%' and exists (select 1 from st where length(st.m10) = 10)
        and hr.perf_m10(coalesce(nullif(e."receivedBy",''), e."createdBy")) = (select m10 from st))
    )
  order by e."date" desc;
$fn$;
revoke all on function hr.perf_enquiry_list(text, text) from public, anon;
grant execute on function hr.perf_enquiry_list(text, text) to authenticated;

create or replace function hr.perf_registration_list(p_month text, p_code text)
returns table(reg_date text, name text, mobile text, branch text, patient_id text)
language sql stable security definer set search_path = hr, public, wn
as $fn$
  with guard as (select 1 as ok where hr.is_master()),
  mon as (
    select k as key, length(k) as klen from (
      select case
        when coalesce(p_month,'') ~ '^\d{4}-\d{2}-\d{2}$' then p_month
        when coalesce(p_month,'') ~ '^\d{4}-\d{2}$'        then p_month
        else to_char((now() at time zone 'Asia/Kolkata')::date, 'YYYY-MM') end as k
    ) q
  ), st as (
    select hr.perf_m10(s.link_mobile) as m10
    from hr.staff_profiles s where s.person_code = p_code
  ), known as (
    select hr.perf_m10(s.link_mobile) as m10 from hr.staff_profiles s
    where s.active is not false and lower(coalesce(s.role_kind,'')) <> 'doctor'
      and upper(coalesce(s.person_code,'')) not like 'DR-%'
      and length(hr.perf_m10(s.link_mobile)) = 10
  )
  select coalesce(nullif(p."registrationDate",''), p."date"), p."name", p."mobile", p."branch", p."patientId"
  from public.patients p, mon, guard
  where left(coalesce(nullif(p."registrationDate",''), p."date", ''), mon.klen) = mon.key
    and (
      (p_code like 'BRANCH-%' and p."branch" = substring(p_code from 8)
        and length(hr.perf_m10(coalesce(nullif(p."registeredBy",''), p."createdBy"))) = 10
        and hr.perf_m10(coalesce(nullif(p."registeredBy",''), p."createdBy")) not in (select m10 from known))
      or
      (p_code not like 'BRANCH-%' and exists (select 1 from st where length(st.m10) = 10)
        and hr.perf_m10(coalesce(nullif(p."registeredBy",''), p."createdBy")) = (select m10 from st))
    )
  order by coalesce(nullif(p."registrationDate",''), p."date") desc;
$fn$;
revoke all on function hr.perf_registration_list(text, text) from public, anon;
grant execute on function hr.perf_registration_list(text, text) to authenticated;

create or replace function hr.perf_treatment_list(p_month text, p_code text)
returns table(reg_date text, name text, mobile text, branch text, patient_id text)
language sql stable security definer set search_path = hr, public, wn
as $fn$
  with guard as (select 1 as ok where hr.is_master()),
  mon as (
    select k as key, length(k) as klen from (
      select case
        when coalesce(p_month,'') ~ '^\d{4}-\d{2}-\d{2}$' then p_month
        when coalesce(p_month,'') ~ '^\d{4}-\d{2}$'        then p_month
        else to_char((now() at time zone 'Asia/Kolkata')::date, 'YYYY-MM') end as k
    ) q
  ), st as (
    select hr.perf_m10(s.link_mobile) as m10
    from hr.staff_profiles s where s.person_code = p_code
  ), known as (
    select hr.perf_m10(s.link_mobile) as m10 from hr.staff_profiles s
    where s.active is not false and lower(coalesce(s.role_kind,'')) <> 'doctor'
      and upper(coalesce(s.person_code,'')) not like 'DR-%'
      and length(hr.perf_m10(s.link_mobile)) = 10
  )
  select coalesce(nullif(p."registrationDate",''), p."date"), p."name", p."mobile", p."branch", p."patientId"
  from public.patients p, mon, guard
  where left(coalesce(nullif(p."registrationDate",''), p."date", ''), mon.klen) = mon.key
    and (
      (p_code like 'BRANCH-%' and p."branch" = substring(p_code from 8)
        and length(hr.perf_m10(coalesce(nullif(p."registeredBy",''), p."createdBy"))) = 10
        and hr.perf_m10(coalesce(nullif(p."registeredBy",''), p."createdBy")) not in (select m10 from known))
      or
      (p_code not like 'BRANCH-%' and exists (select 1 from st where length(st.m10) = 10)
        and hr.perf_m10(coalesce(nullif(p."registeredBy",''), p."createdBy")) = (select m10 from st))
    )
    and exists (select 1 from public.payments y
                 where y."patientId" = p.id
                   and lower(coalesce(y."payType",'')) = 'treatment'
                   and hr.perf_num(y."amount") > 0)
  order by coalesce(nullif(p."registrationDate",''), p."date") desc;
$fn$;
revoke all on function hr.perf_treatment_list(text, text) from public, anon;
grant execute on function hr.perf_treatment_list(text, text) to authenticated;

create or replace function hr.perf_rmp_list(p_month text, p_code text)
returns table(added_date text, name text, mobile text, area text)
language sql stable security definer set search_path = hr, public, wn
as $fn$
  with guard as (select 1 as ok where hr.is_master()),
  mon as (
    select k as key, length(k) as klen from (
      select case
        when coalesce(p_month,'') ~ '^\d{4}-\d{2}-\d{2}$' then p_month
        when coalesce(p_month,'') ~ '^\d{4}-\d{2}$'        then p_month
        else to_char((now() at time zone 'Asia/Kolkata')::date, 'YYYY-MM') end as k
    ) q
  ), st as (
    select hr.perf_m10(s.link_mobile) as m10
    from hr.staff_profiles s where s.person_code = p_code
  ), known as (
    select hr.perf_m10(s.link_mobile) as m10 from hr.staff_profiles s
    where s.active is not false and lower(coalesce(s.role_kind,'')) <> 'doctor'
      and upper(coalesce(s.person_code,'')) not like 'DR-%'
      and length(hr.perf_m10(s.link_mobile)) = 10
  )
  select coalesce(d."createdAt", d."date"), d."name", d."mobile", d."area"
  from public.doctor_visits d, mon, guard
  where left(coalesce(d."createdAt", d."date", ''), mon.klen) = mon.key
    and (
      (p_code like 'BRANCH-%' and coalesce(d."branch",'') = substring(p_code from 8)
        and length(hr.perf_m10(d."createdBy")) = 10
        and hr.perf_m10(d."createdBy") not in (select m10 from known))
      or
      (p_code not like 'BRANCH-%' and exists (select 1 from st where length(st.m10) = 10)
        and hr.perf_m10(d."createdBy") = (select m10 from st))
    )
  order by coalesce(d."createdAt", d."date") desc;
$fn$;
revoke all on function hr.perf_rmp_list(text, text) from public, anon;
grant execute on function hr.perf_rmp_list(text, text) to authenticated;

create or replace function hr.perf_payment_list(p_month text, p_code text, p_mode text)
returns table(pay_date text, name text, mobile text, branch text, amount numeric, remarks text)
language sql stable security definer set search_path = hr, public, wn
as $fn$
  with guard as (select 1 as ok where hr.is_master()),
  mon as (
    select k as key, length(k) as klen from (
      select case
        when coalesce(p_month,'') ~ '^\d{4}-\d{2}-\d{2}$' then p_month
        when coalesce(p_month,'') ~ '^\d{4}-\d{2}$'        then p_month
        else to_char((now() at time zone 'Asia/Kolkata')::date, 'YYYY-MM') end as k
    ) q
  ), st as (
    select hr.perf_m10(s.link_mobile) as m10
    from hr.staff_profiles s where s.person_code = p_code
  ), known as (
    select hr.perf_m10(s.link_mobile) as m10 from hr.staff_profiles s
    where s.active is not false and lower(coalesce(s.role_kind,'')) <> 'doctor'
      and upper(coalesce(s.person_code,'')) not like 'DR-%'
      and length(hr.perf_m10(s.link_mobile)) = 10
  )
  select y."date", y."name", y."mobile", y."branch", hr.perf_num(y."amount"), y."remarks"
  from public.payments y, mon, guard
  where left(coalesce(y."date", ''), mon.klen) = mon.key
    and (case when lower(coalesce(p_mode,'cash'))='online' then upper(coalesce(y."mode",'CASH'))='ONLINE'
              else upper(coalesce(y."mode",'CASH'))<>'ONLINE' end)
    and lower(coalesce(y."payType",'')) <> 'refund'
    and (
      (p_code like 'BRANCH-%' and y."branch" = substring(p_code from 8)
        and length(hr.perf_m10(coalesce(nullif(y."receivedBy",''), y."createdBy"))) = 10
        and hr.perf_m10(coalesce(nullif(y."receivedBy",''), y."createdBy")) not in (select m10 from known))
      or
      (p_code not like 'BRANCH-%' and exists (select 1 from st where length(st.m10) = 10)
        and hr.perf_m10(coalesce(nullif(y."receivedBy",''), y."createdBy")) = (select m10 from st))
    )
  order by y."date" desc;
$fn$;
revoke all on function hr.perf_payment_list(text, text, text) from public, anon;
grant execute on function hr.perf_payment_list(text, text, text) to authenticated;

notify pgrst, 'reload schema';

-- ── মিলিয়ে দেখা (শুধু পড়া) ────────────────────────────────────────────────
-- select * from hr.perf_payment_list('2026-08','BRANCH-Cooch Behar','cash');
