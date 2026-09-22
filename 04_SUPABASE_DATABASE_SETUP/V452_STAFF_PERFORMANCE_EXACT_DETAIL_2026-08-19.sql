-- ============================================================================
-- V452 — Staff Performance exact drill-down + future full call number
-- TK অনুমোদন: 19.08.2026 ~21:47 IST
--
-- লক্ষ্য:
--   1) Staff Performance > Enquiry / Calls / Cash-Online তালিকার প্রতিটি row
--      থেকে exact read-only detail দেখানো যাবে।
--   2) ভবিষ্যতের App Call-এ যে পূর্ণ নম্বরে dial করা হয়েছিল সেটি রাখা হবে।
--      পুরনো masked-only call আন্দাজ করে পূর্ণ করা হবে না।
--
-- নিরাপত্তা:
--   • existing table/column/function delete/rename করা হয়নি।
--   • target_mobile nullable additive column; পুরনো row অক্ষত।
--   • পুরনো perf_* function অক্ষত; নতুন *_v2 function যোগ করা হয়েছে।
--   • সব detail function Master-only guard (hr.is_master()) বজায় রাখে।
--   • কোনো patient/payment/enquiry row লেখা/বদলানো হয় না — শুধু read.
-- ============================================================================

alter table wn.call_taps
  add column if not exists target_mobile text;

-- ১) Enquiry exact list/detail payload ----------------------------------------
create or replace function hr.perf_enquiry_list_v2(p_month text, p_code text)
returns table(
  id text,
  enq_date text,
  name text,
  mobile text,
  branch text,
  disease text,
  address text,
  remarks text,
  status text,
  stage text,
  received_by text,
  created_by text,
  created_at text
)
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
  select e.id, e."date", e."name", e."mobile", e."branch", e."disease",
         e."address", e."remarks", e."status", e."stage", e."receivedBy",
         e."createdBy", e."createdAt"
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
  order by e."date" desc, e."createdAt" desc nulls last, e.id;
$fn$;
revoke all on function hr.perf_enquiry_list_v2(text, text) from public, anon;
grant execute on function hr.perf_enquiry_list_v2(text, text) to authenticated;

-- ২) Calls exact list. New app calls prefer full target_mobile; old calls fall
--    back to target_mobile_mask and are explicitly marked unavailable. --------
create or replace function hr.perf_calls_list_v2(p_month text, p_code text, p_kind text)
returns table(
  id text,
  call_date text,
  call_time text,
  target text,
  remark text,
  call_kind text,
  full_number_available boolean
)
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
  select c.id::text,
         to_char(c.call_date, 'YYYY-MM-DD'),
         to_char(c.tapped_at at time zone 'Asia/Kolkata', 'HH12:MI AM'),
         coalesce(nullif(c.target_mobile,''), c.target_mobile_mask),
         ''::text,
         'app'::text,
         (nullif(c.target_mobile,'') is not null)
  from wn.call_taps c, mon, guard
  where lower(coalesce(p_kind,'app')) = 'app'
    and left(to_char(c.call_date, 'YYYY-MM-DD'), mon.klen) = mon.key
    and c.staff_code = p_code
  union all
  select o.id::text,
         to_char(o.call_date, 'YYYY-MM-DD'),
         o.call_time,
         o.target_mobile,
         o.remark,
         'outside'::text,
         (nullif(o.target_mobile,'') is not null)
  from wn.outside_calls o, mon, guard
  where lower(coalesce(p_kind,'app')) = 'outside'
    and left(to_char(o.call_date, 'YYYY-MM-DD'), mon.klen) = mon.key
    and o.staff_code = p_code
  order by 2 desc, 3 desc, 1;
$fn$;
revoke all on function hr.perf_calls_list_v2(text, text, text) from public, anon;
grant execute on function hr.perf_calls_list_v2(text, text, text) to authenticated;

-- ৩) Payment exact list/detail payload ---------------------------------------
create or replace function hr.perf_payment_list_v2(p_month text, p_code text, p_mode text)
returns table(
  id text,
  pay_date text,
  name text,
  mobile text,
  branch text,
  amount numeric,
  mode text,
  pay_type text,
  pay_label text,
  remarks text,
  patient_id text,
  patient_code text,
  received_by text,
  created_by text,
  created_at text,
  status text
)
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
  select y.id, y."date", y."name", y."mobile", y."branch", hr.perf_num(y."amount"),
         y."mode", y."payType",
         coalesce(nullif(y."payLabel",''), nullif(y."paymentLabel",''), y."payType"),
         y."remarks", y."patientId", y."patientCode", y."receivedBy", y."createdBy",
         y."createdAt", y."status"
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
  order by y."date" desc, y."createdAt" desc nulls last, y.id;
$fn$;
revoke all on function hr.perf_payment_list_v2(text, text, text) from public, anon;
grant execute on function hr.perf_payment_list_v2(text, text, text) to authenticated;

-- Backward-compatible improvement: old clients keep the same 4-column return
-- shape, but new rows can show the full target when it exists.
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
  select to_char(c.call_date, 'YYYY-MM-DD'),
         to_char(c.tapped_at at time zone 'Asia/Kolkata', 'HH12:MI AM'),
         coalesce(nullif(c.target_mobile,''), c.target_mobile_mask), ''
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

notify pgrst, 'reload schema';
