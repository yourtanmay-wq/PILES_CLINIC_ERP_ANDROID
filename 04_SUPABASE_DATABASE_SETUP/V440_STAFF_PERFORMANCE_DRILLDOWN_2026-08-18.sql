-- ============================================================================
-- V440 — Staff Performance-এর প্রতিটা সংখ্যায় চাপ দিলে আসল তালিকা (TK-নির্দেশ,
-- ১৮.০৮.২০২৬)। staff_performance()-এর ঠিক একই ফিল্টার — তাই গোনার সংখ্যা আর
-- এই তালিকার সারি-সংখ্যা কখনো অমিল হবে না (একই সোর্স-অফ-ট্রুথ)।
--
-- ⛔ কোনো নতুন টেবিল লাগে না। ⛔ একটাও সারি লেখা/বদলানো হয় না — কেবল পড়া।
-- ⛔ শুধু Master চালাতে পারবেন (staff_performance-এর মতোই hr.is_master() গার্ড)।
-- ============================================================================

-- ১) এনকোয়ারি ফর্মের তালিকা ------------------------------------------------
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
  )
  select e."date", e."name", e."mobile", e."branch", e."disease"
  from public.enquiries e, mon, st, guard
  where left(coalesce(e."date", ''), mon.klen) = mon.key
    and length(st.m10) = 10
    and hr.perf_m10(coalesce(nullif(e."receivedBy",''), e."createdBy")) = st.m10
  order by e."date" desc;
$fn$;
revoke all on function hr.perf_enquiry_list(text, text) from public, anon;
grant execute on function hr.perf_enquiry_list(text, text) to authenticated;

-- ২) রেজিস্ট্রেশনের তালিকা --------------------------------------------------
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
  )
  select coalesce(nullif(p."registrationDate",''), p."date"), p."name", p."mobile", p."branch", p."patientId"
  from public.patients p, mon, st, guard
  where left(coalesce(nullif(p."registrationDate",''), p."date", ''), mon.klen) = mon.key
    and length(st.m10) = 10
    and hr.perf_m10(coalesce(nullif(p."registeredBy",''), p."createdBy")) = st.m10
  order by coalesce(nullif(p."registrationDate",''), p."date") desc;
$fn$;
revoke all on function hr.perf_registration_list(text, text) from public, anon;
grant execute on function hr.perf_registration_list(text, text) to authenticated;

-- ৩) যাঁরা ট্রিটমেন্ট শুরু করেছেন --------------------------------------------
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
  )
  select coalesce(nullif(p."registrationDate",''), p."date"), p."name", p."mobile", p."branch", p."patientId"
  from public.patients p, mon, st, guard
  where left(coalesce(nullif(p."registrationDate",''), p."date", ''), mon.klen) = mon.key
    and length(st.m10) = 10
    and hr.perf_m10(coalesce(nullif(p."registeredBy",''), p."createdBy")) = st.m10
    and exists (select 1 from public.payments y
                 where y."patientId" = p.id
                   and lower(coalesce(y."payType",'')) = 'treatment'
                   and hr.perf_num(y."amount") > 0)
  order by coalesce(nullif(p."registrationDate",''), p."date") desc;
$fn$;
revoke all on function hr.perf_treatment_list(text, text) from public, anon;
grant execute on function hr.perf_treatment_list(text, text) to authenticated;

-- ৪) RMP/ডাক্তার যোগ করার তালিকা ---------------------------------------------
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
  )
  select coalesce(d."createdAt", d."date"), d."name", d."mobile", d."area"
  from public.doctor_visits d, mon, st, guard
  where left(coalesce(d."createdAt", d."date", ''), mon.klen) = mon.key
    and length(st.m10) = 10
    and hr.perf_m10(d."createdBy") = st.m10
  order by coalesce(d."createdAt", d."date") desc;
$fn$;
revoke all on function hr.perf_rmp_list(text, text) from public, anon;
grant execute on function hr.perf_rmp_list(text, text) to authenticated;

-- ৫) কলের তালিকা (অ্যাপ থেকে / বাইরের) ---------------------------------------
create or replace function hr.perf_calls_list(p_month text, p_code text, p_kind text)
returns table(call_date text, call_time text, target text, remark text)
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
  )
  select to_char(c.call_date, 'YYYY-MM-DD'), to_char(c.tapped_at, 'HH12:MI AM'),
         c.target_mobile_mask, ''
  from wn.call_taps c, mon, guard
  where lower(coalesce(p_kind,'app')) = 'app'
    and left(to_char(c.call_date, 'YYYY-MM-DD'), mon.klen) = mon.key
    and c.staff_code = p_code
  union all
  select to_char(o.call_date, 'YYYY-MM-DD'), o.call_time, o.target_mobile, o.remark
  from wn.outside_calls o, mon, guard
  where lower(coalesce(p_kind,'app')) = 'outside'
    and left(to_char(o.call_date, 'YYYY-MM-DD'), mon.klen) = mon.key
    and o.staff_code = p_code
  order by 1 desc;
$fn$;
revoke all on function hr.perf_calls_list(text, text, text) from public, anon;
grant execute on function hr.perf_calls_list(text, text, text) to authenticated;

-- ৬) টাকা আদায়ের তালিকা (নগদ / অনলাইন) --------------------------------------
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
  )
  select y."date", y."name", y."mobile", y."branch", hr.perf_num(y."amount"), y."remarks"
  from public.payments y, mon, st, guard
  where left(coalesce(y."date", ''), mon.klen) = mon.key
    and length(st.m10) = 10
    and hr.perf_m10(coalesce(nullif(y."receivedBy",''), y."createdBy")) = st.m10
    and (case when lower(coalesce(p_mode,'cash'))='online' then upper(coalesce(y."mode",'CASH'))='ONLINE'
              else upper(coalesce(y."mode",'CASH'))<>'ONLINE' end)
    and lower(coalesce(y."payType",'')) <> 'refund'
  order by y."date" desc;
$fn$;
revoke all on function hr.perf_payment_list(text, text, text) from public, anon;
grant execute on function hr.perf_payment_list(text, text, text) to authenticated;

-- ৭) দৈনিক রিপোর্ট পাঠানোর তারিখ-তালিকা --------------------------------------
create or replace function hr.perf_reports_list(p_month text, p_code text)
returns table(report_date text, status text, accepted boolean)
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
  )
  select distinct w.period_key, w.status, w.accepted
  from wn.work_reports w, mon, guard
  where w.period_type = 'daily'
    and left(coalesce(w.period_key, ''), mon.klen) = mon.key
    and w.staff_code = p_code
  order by 1 desc;
$fn$;
revoke all on function hr.perf_reports_list(text, text) from public, anon;
grant execute on function hr.perf_reports_list(text, text) to authenticated;

-- ৮) 🗓️ পুরো মাসের হাজিরা-খাতা — Date · IN · OUT · Leave, একটা টেবিলে (গুগল
--    সিটের মতো)। "Days present" ও "Leave days" — দুটো থেকেই এটাই খোলে।
create or replace function hr.perf_attendance_sheet(p_month text, p_code text)
returns table(work_date text, check_in text, check_out text, is_leave boolean)
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
  ),
  days as (
    select to_char(n.work_date, 'YYYY-MM-DD') as d, n.check_in, n.check_out
    from wn.notebook_days n, mon, guard
    where left(to_char(n.work_date, 'YYYY-MM-DD'), mon.klen) = mon.key
      and n.staff_code = p_code
  ),
  leaves as (
    select to_char(l.leave_date, 'YYYY-MM-DD') as d
    from wn.leave_requests l, mon, guard
    where left(to_char(l.leave_date, 'YYYY-MM-DD'), mon.klen) = mon.key
      and l.staff_code = p_code
      and lower(coalesce(l.status,'')) in ('approved','accepted')
  )
  select coalesce(days.d, leaves.d), days.check_in, days.check_out, (leaves.d is not null)
  from days full outer join leaves on days.d = leaves.d
  order by 1 desc;
$fn$;
revoke all on function hr.perf_attendance_sheet(text, text) from public, anon;
grant execute on function hr.perf_attendance_sheet(text, text) to authenticated;

notify pgrst, 'reload schema';

-- ── মিলিয়ে দেখা (শুধু পড়া) ────────────────────────────────────────────────
-- select * from hr.perf_enquiry_list('2026-08','JPE-JALPAI-13');
-- select * from hr.perf_attendance_sheet('2026-08','JPE-JALPAI-13');
