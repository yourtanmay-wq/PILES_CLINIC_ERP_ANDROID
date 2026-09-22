-- ============================================================================
-- V418 — Extra Income আপনা থেকে (TK-অনুমোদিত নিয়ম, ১৭.০৮.২০২৬)
--
-- নিয়ম (TK-এর হুবহু কথা থেকে):
--   • Registration-এর Timing = "Unexpected Time" আর Registration Fee জমা পড়েছে
--       ⇒ ₹১০০
--   • ওই রোগীর প্রথম Advance/Treatment টাকা জমা পড়লে আরও ₹৪০০ ⇒ মোট ₹৫০০
--   • টাকাটা ভাগ হয় ৫০-৫০ — যিনি Enquiry ফর্ম ভরেছিলেন আর যিনি Registration
--     ফর্ম ভরলেন। একই লোক হলে পুরোটা তাঁরই।
--   • Enquiry ফর্ম না থাকলে পুরোটা Registration-করা staff-এর।
--   • নিয়মটা খাটে ১৮.০৮.২০২৬ থেকে করা রেজিস্ট্রেশনে (TK: "আজ থেকে নতুন কাজে")।
--   • রেজিস্ট্রেশন মুছে গেলে বা Timing শুধরানো হলে — টাকাটা এখনো "বাকি" থাকলে
--     আপনা থেকেই বাতিল হয়ে যায় (TK: "বাকি থাকলে আপনাই বাতিল")।
--     ⛔ কিন্তু একবার "দেওয়া হয়েছে" (PAID) হয়ে গেলে সেটা কখনো ছোঁয়া হয় না।
--
-- ⛔ এই SQL কোনো সারি মোছে না, কোনো পুরনো অঙ্ক বদলায় না।
-- ⛔ রোগী · এনকোয়ারি · পেমেন্ট — একটাও টেবিলে লেখা হয় না, শুধু পড়া হয়।
-- ============================================================================

-- ── ধাপ ১: একই টাকা যেন কখনো দুবার না বসে ────────────────────────────────
-- প্রতিটা আপনা-থেকে-বসা সারির একটা নিজস্ব চাবি থাকে:
--     INC:<REG|TRT>:<রোগীর id>:<staff-এর কোড>
-- চাবিটা unique, তাই একই রোগীর একই ধাপের টাকা দ্বিতীয়বার বসতেই পারে না।
-- হাতে-বসানো পুরনো সারিতে চাবি ফাঁকা (NULL) থাকে — Postgres-এ একাধিক NULL
-- unique index-এ চলে, তাই পুরনো কিছুই আটকায় না।
alter table hr.salary_payments
  add column if not exists src_key text;

create unique index if not exists salary_payments_src_key_uidx
  on hr.salary_payments(src_key);

-- ── ধাপ ২: "কার কত পাওনা" — শুধু হিসাব, কোথাও লেখা নয় ────────────────────
create or replace function hr.incentive_wanted()
returns table(person_code text, src_key text, amount numeric, reason text)
language sql
stable
security definer
set search_path = hr, public
as $fn$
  with pat as (
    select
      p.id,
      coalesce(nullif(p."patientId", ''), p.id)                                as shown_code,
      right(regexp_replace(coalesce(p.mobile, ''), '\D', '', 'g'), 10)         as pmob,
      right(regexp_replace(coalesce(p."registeredBy", ''), '\D', '', 'g'), 10) as regmob,
      nullif(left(coalesce(nullif(p."registrationDate", ''), p."date", ''), 10), '') as regdate
    from public.patients p
    where coalesce(p."timeType", '') = 'Unexpected Time'
  ), pat2 as (
    -- ⛔ ১৮.০৮.২০২৬-এর আগের রেজিস্ট্রেশন এই নিয়মের বাইরে (TK-নির্দেশ)
    select * from pat
    where regdate ~ '^\d{4}-\d{2}-\d{2}$'
      and regdate::date >= date '2026-08-18'
  ), enq as (
    -- যিনি Enquiry ফর্ম ভরেছিলেন (একই নম্বরের সবচেয়ে নতুন এনকোয়ারি)
    select distinct on (m.id)
      m.id,
      right(regexp_replace(coalesce(e."receivedBy", ''), '\D', '', 'g'), 10) as enqmob
    from pat2 m
    join public.enquiries e
      on right(regexp_replace(coalesce(e.mobile, ''), '\D', '', 'g'), 10) = m.pmob
    order by m.id, coalesce(e."createdAt", e."date", '') desc
  ), money as (
    -- টাকাটা সত্যিই জমা পড়েছে কিনা (অঙ্কটা text, তাই সাবধানে সংখ্যা করা হলো)
    select
      m.id,
      bool_or(lower(coalesce(y."payType", '')) in ('visit_fee', 'visitfee', 'registration')
              and coalesce(nullif(regexp_replace(coalesce(y."amount", ''), '[^0-9.]', '', 'g'), ''), '0')::numeric > 0) as has_fee,
      bool_or(lower(coalesce(y."payType", '')) = 'treatment'
              and coalesce(nullif(regexp_replace(coalesce(y."amount", ''), '[^0-9.]', '', 'g'), ''), '0')::numeric > 0) as has_trt
    from pat2 m
    join public.payments y on y."patientId" = m.id
    group by m.id
  ), who as (
    select
      m.id, m.shown_code,
      sr.person_code as reg_code,
      se.person_code as enq_code,
      coalesce(mn.has_fee, false) as has_fee,
      coalesce(mn.has_trt, false) as has_trt
    from pat2 m
    left join enq q on q.id = m.id
    left join hr.staff_profiles sr
      on length(m.regmob) = 10
     and right(regexp_replace(coalesce(sr.link_mobile, ''), '\D', '', 'g'), 10) = m.regmob
     and sr.active is not false
    left join hr.staff_profiles se
      on length(coalesce(q.enqmob, '')) = 10
     and right(regexp_replace(coalesce(se.link_mobile, ''), '\D', '', 'g'), 10) = q.enqmob
     and se.active is not false
    left join money mn on mn.id = m.id
  ), split as (
    -- দুজন আলাদা হলে ২ ভাগ, একই লোক (বা একজনই পাওয়া গেল) হলে ১ ভাগ
    select
      w.id, w.shown_code, w.has_fee, w.has_trt, c.pc,
      count(*) over (partition by w.id) as n
    from who w
    cross join lateral (
      select distinct x as pc
      from unnest(array[w.reg_code, w.enq_code]) x
      where x is not null and x <> ''
    ) c
  )
  select
    s.pc::text,
    ('INC:' || st.stage || ':' || s.id || ':' || s.pc)::text,
    round(st.amt / s.n, 2),
    (st.label || ' · ' || s.shown_code)::text
  from split s
  cross join lateral (values
      ('REG', 100::numeric, 'Registration', s.has_fee),
      ('TRT', 400::numeric, 'Treatment',    s.has_trt)
    ) as st(stage, amt, label, ok)
  where st.ok;
$fn$;

-- ── ধাপ ৩: হিসাবটা মিলিয়ে দেওয়া — বসানো ও বাতিল করা ─────────────────────
create or replace function hr.incentive_sync()
returns jsonb
language plpgsql
security definer
set search_path = hr, public
as $fn$
declare
  v_made int := 0;
  v_gone int := 0;
begin
  -- ⛔ টাকার হিসাব — শুধু Master-ই চালাতে পারবেন
  if not hr.is_master() then
    raise exception 'Master identity required';
  end if;

  -- ১) পাওনা টাকা "বাকি" হয়ে বসে। চাবি এক থাকলে নতুন সারি হয় না —
  --    শুধু অঙ্কটা মিলিয়ে নেওয়া হয়, আর সেটাও কেবল যদি এখনো "বাকি" থাকে।
  --    ⛔ একবার PAID হয়ে গেলে সেই সারি এই SQL কখনো ছোঁয় না।
  insert into hr.salary_payments
    (person_code, paid_on, amount, mode, paid_by, remark, for_month, kind, extra_reason, status, src_key)
  select w.person_code, current_date, w.amount, '', 'auto', '', '', 'EXTRA', w.reason, 'DUE', w.src_key
  from hr.incentive_wanted() w
  on conflict (src_key) do update
     set amount = excluded.amount,
         extra_reason = excluded.extra_reason
     where salary_payments.status = 'DUE';
  get diagnostics v_made = row_count;

  -- ২) আর পাওনা নয় (রেজিস্ট্রেশন মুছে গেছে / Timing শুধরানো হয়েছে / টাকা
  --    ফেরত গেছে) — এমন সারি বাতিল, কিন্তু কেবল যদি এখনো "বাকি" থাকে।
  --    ⛔ হিসাবটা একবারই কষা হয় (materialized) — প্রতিটা সারির জন্য বারবার নয়।
  with want as materialized (select w.src_key from hr.incentive_wanted() w)
  delete from hr.salary_payments s
  where s.src_key like 'INC:%'
    and coalesce(s.status, 'PAID') = 'DUE'
    and not exists (select 1 from want q where q.src_key = s.src_key);
  get diagnostics v_gone = row_count;

  return jsonb_build_object('ok', true, 'written', v_made, 'removed', v_gone);
end;
$fn$;

-- ⛔ `incentive_wanted()` সবার পাওনা একসাথে দেখায় — তাই কোনো কর্মী সরাসরি
--    ডাকতে পারবেন না। শুধু `incentive_sync()` ভিতর থেকে ব্যবহার করে (security
--    definer, মালিকের অধিকারে চলে), আর সেটাও Master ছাড়া চলে না।
revoke all on function hr.incentive_wanted() from public, anon, authenticated;
revoke all on function hr.incentive_sync()   from public, anon;
grant execute on function hr.incentive_sync() to authenticated;

notify pgrst, 'reload schema';

-- ── মিলিয়ে দেখা (শুধু পড়া, কিছুই বদলায় না) ───────────────────────────────
-- select * from hr.incentive_wanted() order by person_code, src_key;
