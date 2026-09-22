-- ============================================================================
-- V420 — Staff Performance: মাস **অথবা একটা দিন** (TK-নির্দেশ, ১৭.০৮.২০২৬)
--
-- TK যা দেখতে চেয়েছেন (চারটেই):
--   ১) রোগী আনার কাজ  — কতগুলো Enquiry ফর্ম · কতজন Registration · তার মধ্যে
--                        কতজন ট্রিটমেন্ট শুরু করেছেন
--   ২) ফলোআপ ও কল     — অ্যাপ থেকে কতগুলো কল · বাইরের কল · কতগুলো RMP যোগ
--   ৩) টাকা আদায়      — কার হাত দিয়ে কত নগদ ও কত অনলাইন জমা পড়েছে
--   ৪) হাজিরা ও রিপোর্ট — কতদিন এসেছেন · কতদিন দৈনিক রিপোর্ট পাঠিয়েছেন ·
--                        কতদিন ছুটি (মঞ্জুর হওয়া)
--
-- 🔵 V420: এখন **একটা দিনের** হিসাবও দেখা যায় — `'2026-08-17'` দিলে শুধু ওই দিন,
--    `'2026-08'` দিলে গোটা মাস। একই ফাংশন, একই সংখ্যা-নিয়ম।
--
-- ⛔ কোনো নতুন টেবিল লাগে না — সব তথ্য অ্যাপে আগে থেকেই জমা আছে, শুধু গুনে
--    দেখানো হচ্ছে। ⛔ একটাও সারি লেখা/বদলানো/মোছা হয় না — কেবল পড়া।
-- ⛔ পুরো হিসাব ডেটাবেসের ভিতরে ⇒ ফোন আর ওয়েবে সংখ্যা আলাদা হওয়ার সুযোগ নেই,
--    আর এক ডাকে ছোট্ট উত্তর আসে (Egress-এও সস্তা)।
-- ⛔ শুধু Master চালাতে পারবেন।
-- ============================================================================

-- কর্মীর মোবাইল ↔ পাবলিক টেবিলের মোবাইল মেলানোর একটাই নিয়ম (শেষ ১০ অঙ্ক)।
create or replace function hr.perf_m10(v text)
returns text
language sql
immutable
as $fn$
  select right(regexp_replace(coalesce(v, ''), '\D', '', 'g'), 10);
$fn$;

-- টাকার ঘরগুলো text-এ রাখা, তাই সাবধানে সংখ্যা করা হয় (অক্ষর থাকলেও ভাঙে না)।
create or replace function hr.perf_num(v text)
returns numeric
language sql
immutable
as $fn$
  select coalesce(nullif(regexp_replace(coalesce(v, ''), '[^0-9.]', '', 'g'), ''), '0')::numeric;
$fn$;

create or replace function hr.staff_performance(p_month text)
returns table(
  person_code        text,
  full_name          text,
  branch             text,
  enquiry_count      int,
  registration_count int,
  treatment_count    int,
  rmp_added          int,
  app_calls          int,
  outside_calls      int,
  cash_collected     numeric,
  online_collected   numeric,
  present_days       int,
  reports_sent       int,
  leave_days         int
)
language sql
stable
security definer
set search_path = hr, public, wn
as $fn$
  with guard as (
    -- ⛔ Master ছাড়া কেউ এই হিসাব দেখতে পাবেন না।
    --    Master না হলে এখানে **একটাও সারি থাকে না**, তাই নিচের জোড়া লাগানোয়
    --    পুরো ফলটাই খালি হয়ে যায় — অন্য কারও পর্দায় কারো হিসাব ওঠে না।
    select 1 as ok where hr.is_master()
  ), mon as (
    /* 🔵 V420 (TK-নির্দেশ: "daily performance দেখার ব্যবস্থা রাখতে হবে") —
       একই ঘরে **মাস** (2026-08) বা **একটা দিন** (2026-08-17) — দুটোই চলে।
       কৌশলটা সহজ: তারিখের প্রথম যতগুলো অক্ষর চাওয়া হয়েছে ততগুলোই মেলানো হয়।
       ⛔ ভুল/ফাঁকা লেখা এলে চলতি মাস ধরা হয় — কখনো ভাঙে না। */
    select k as key, length(k) as klen from (
      select case
        when coalesce(p_month,'') ~ '^\d{4}-\d{2}-\d{2}$' then p_month
        when coalesce(p_month,'') ~ '^\d{4}-\d{2}$'        then p_month
        else to_char((now() at time zone 'Asia/Kolkata')::date, 'YYYY-MM') end as k
    ) q
  ), staff as (
    -- 🔴 TK-নির্দেশ (১৭.০৮.২০২৬): *"ডাক্তারদের বাদ দিয়ে দিন"* — এই তালিকা
    --    শুধু কর্মীদের। ডাক্তার চেনা হয় দু'ভাবেই (একটা ফাঁকা থাকলেও অন্যটা ধরে):
    --    role_kind = doctor, অথবা কোড 'DR-' দিয়ে শুরু।
    select s.person_code, s.full_name, s.branch,
           hr.perf_m10(s.link_mobile) as m10
    from hr.staff_profiles s
    where s.active is not false
      and lower(coalesce(s.role_kind, '')) <> 'doctor'
      and upper(coalesce(s.person_code, '')) not like 'DR-%'
  )
  select
    st.person_code,
    coalesce(nullif(st.full_name, ''), st.person_code) as full_name,
    coalesce(st.branch, '') as branch,

    -- ১) রোগী আনার কাজ ----------------------------------------------------
    (select count(*)::int from public.enquiries e, mon
       where left(coalesce(e."date", ''), mon.klen) = mon.key
         and length(st.m10) = 10
         and hr.perf_m10(coalesce(nullif(e."receivedBy",''), e."createdBy")) = st.m10
    ) as enquiry_count,

    (select count(*)::int from public.patients p, mon
       where left(coalesce(nullif(p."registrationDate",''), p."date", ''), mon.klen) = mon.key
         and length(st.m10) = 10
         and hr.perf_m10(coalesce(nullif(p."registeredBy",''), p."createdBy")) = st.m10
    ) as registration_count,

    -- এই মাসে যাঁদের রেজিস্ট্রেশন করিয়েছেন, তাঁদের মধ্যে কতজন সত্যিই
    -- ট্রিটমেন্টের টাকা দিয়েছেন (অর্থাৎ চিকিৎসা শুরু করেছেন)।
    (select count(*)::int from public.patients p, mon
       where left(coalesce(nullif(p."registrationDate",''), p."date", ''), mon.klen) = mon.key
         and length(st.m10) = 10
         and hr.perf_m10(coalesce(nullif(p."registeredBy",''), p."createdBy")) = st.m10
         and exists (select 1 from public.payments y
                      where y."patientId" = p.id
                        and lower(coalesce(y."payType",'')) = 'treatment'
                        and hr.perf_num(y."amount") > 0)
    ) as treatment_count,

    -- ২) ফলোআপ ও কল -------------------------------------------------------
    (select count(*)::int from public.doctor_visits d, mon
       where left(coalesce(d."createdAt", d."date", ''), mon.klen) = mon.key
         and length(st.m10) = 10
         and hr.perf_m10(d."createdBy") = st.m10
    ) as rmp_added,

    (select count(*)::int from wn.call_taps c, mon
       where left(to_char(c.call_date, 'YYYY-MM-DD'), mon.klen) = mon.key
         and c.staff_code = st.person_code
    ) as app_calls,

    (select count(*)::int from wn.outside_calls o, mon
       where left(to_char(o.call_date, 'YYYY-MM-DD'), mon.klen) = mon.key
         and o.staff_code = st.person_code
    ) as outside_calls,

    -- ৩) টাকা আদায় --------------------------------------------------------
    (select coalesce(sum(hr.perf_num(y."amount")), 0) from public.payments y, mon
       where left(coalesce(y."date", ''), mon.klen) = mon.key
         and length(st.m10) = 10
         and hr.perf_m10(coalesce(nullif(y."receivedBy",''), y."createdBy")) = st.m10
         and upper(coalesce(y."mode", 'CASH')) <> 'ONLINE'
         and lower(coalesce(y."payType",'')) <> 'refund'
    ) as cash_collected,

    (select coalesce(sum(hr.perf_num(y."amount")), 0) from public.payments y, mon
       where left(coalesce(y."date", ''), mon.klen) = mon.key
         and length(st.m10) = 10
         and hr.perf_m10(coalesce(nullif(y."receivedBy",''), y."createdBy")) = st.m10
         and upper(coalesce(y."mode", 'CASH')) = 'ONLINE'
         and lower(coalesce(y."payType",'')) <> 'refund'
    ) as online_collected,

    -- ৪) হাজিরা ও রিপোর্ট --------------------------------------------------
    (select count(*)::int from wn.notebook_days n, mon
       where left(to_char(n.work_date, 'YYYY-MM-DD'), mon.klen) = mon.key
         and n.staff_code = st.person_code
         and coalesce(n.check_in, '') <> ''
    ) as present_days,

    (select count(distinct w.period_key)::int from wn.work_reports w, mon
       where w.period_type = 'daily'
         and left(coalesce(w.period_key, ''), mon.klen) = mon.key
         and w.staff_code = st.person_code
    ) as reports_sent,

    (select count(*)::int from wn.leave_requests l, mon
       where left(to_char(l.leave_date, 'YYYY-MM-DD'), mon.klen) = mon.key
         and l.staff_code = st.person_code
         and lower(coalesce(l.status,'')) in ('approved', 'accepted')
    ) as leave_days

  from staff st, guard
  order by 4 desc, 5 desc, 2;
$fn$;

revoke all on function hr.staff_performance(text) from public, anon;
grant execute on function hr.staff_performance(text) to authenticated;

notify pgrst, 'reload schema';

-- ── মিলিয়ে দেখা (শুধু পড়া) ────────────────────────────────────────────────
-- select * from hr.staff_performance('2026-08');      -- গোটা মাস
-- select * from hr.staff_performance('2026-08-17');   -- শুধু ওই একটা দিন
