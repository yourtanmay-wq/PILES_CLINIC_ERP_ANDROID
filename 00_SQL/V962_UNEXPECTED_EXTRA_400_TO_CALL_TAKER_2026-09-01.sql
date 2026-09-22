-- ═══════════════════════════════════════════════════════════════════════════
-- 🕘🔒 V962 (০১.০৯.২০২৬) — TK-এর নতুন নিয়ম, Extra Income
--
-- TK: *"একটা প্রথমবার আনএক্সপেক্টেড টাইমে কল রিসিভ করেছিল — সে একাই পাবে"*
--     (চিকিৎসার ধাপের ₹৪০০ প্রসঙ্গে), আর
--     *"যদি চেনা না যায় তাহলে আমি দেবো না"*।
--
-- ─── আগে কী ছিল ───────────────────────────────────────────────────────────
-- V418-এ **দুটো ধাপই** (₹১০০ ও ₹৪০০) এনকোয়ারি-স্টাফ ও রেজিস্ট্রেশন-স্টাফের
-- মধ্যে সমান ভাগ হত (`round(st.amt / s.n, 2)`)।
--
-- ─── এখন কী হবে ───────────────────────────────────────────────────────────
--   • ₹১০০ (Registration ধাপ) — **আগের মতোই ভাগ** (TK: "ঠিক আছে, এটাই থাকবে")
--   • ₹৪০০ (Treatment ধাপ)   — **পুরোটাই কল-ধরা (এনকোয়ারি) স্টাফের**
--   • কল-ধরা স্টাফকে চেনা না গেলে ₹৪০০ **কেউ পাবে না** (সারিই তৈরি হয় না)
--
-- ─── সুরক্ষা ──────────────────────────────────────────────────────────────
-- ⛔ ইতিমধ্যে **দেওয়া (PAID)** এক্সট্রা এই নিয়ম কখনো ছোঁয় না — `incentive_sync`
--    কেবল "DUE" সারিই বদলায় বা তোলে (V418-এ প্রমাণিত)।
-- ⛔ কোনো টেবিল/কলাম বদলায় না — শুধু একটা ফাংশনের ভিতরের নিয়ম।
-- ⛔ ১৮.০৮.২০২৬-এর আগের রেজিস্ট্রেশন আগের মতোই নিয়মের বাইরে।
-- ⚠️ চালানোর পরে কারো "বাকি" ₹২০০ বেড়ে ₹৪০০ হবে, আর অন্যজনের ₹২০০ বাকিটা
--    উঠে যাবে — এটাই TK-এর চাওয়া নতুন হিসাব।
-- ═══════════════════════════════════════════════════════════════════════════

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
    select * from pat
    where regdate ~ '^\d{4}-\d{2}-\d{2}$'
      and regdate::date >= date '2026-08-18'
  ), enq as (
    select distinct on (m.id)
      m.id,
      right(regexp_replace(coalesce(e."receivedBy", ''), '\D', '', 'g'), 10) as enqmob
    from pat2 m
    join public.enquiries e
      on right(regexp_replace(coalesce(e.mobile, ''), '\D', '', 'g'), 10) = m.pmob
    order by m.id, coalesce(e."createdAt", e."date", '') desc
  ), money as (
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
    -- ₹১০০-র ভাগ গোনা: দুজন আলাদা হলে ২ ভাগ, একজন হলে ১ ভাগ (আগের মতোই)
    select
      w.id, w.shown_code, w.has_fee, c.pc,
      count(*) over (partition by w.id) as n
    from who w
    cross join lateral (
      select distinct x as pc
      from unnest(array[w.reg_code, w.enq_code]) x
      where x is not null and x <> ''
    ) c
  )
  -- ধাপ ১ · Registration ₹১০০ — ভাগ হয় (অপরিবর্তিত)
  select
    s.pc::text,
    ('INC:REG:' || s.id || ':' || s.pc)::text,
    round(100::numeric / s.n, 2),
    ('Registration · ' || s.shown_code)::text
  from split s
  where s.has_fee

  union all

  -- 🆕 ধাপ ২ · Treatment ₹৪০০ — পুরোটাই কল-ধরা (এনকোয়ারি) স্টাফের।
  --    কল-ধরা স্টাফকে চেনা না গেলে (`enq_code` ফাঁকা) কোনো সারিই হয় না।
  select
    w.enq_code::text,
    ('INC:TRT:' || w.id || ':' || w.enq_code)::text,
    400::numeric,
    ('Treatment · ' || w.shown_code)::text
  from who w
  where w.has_trt
    and w.enq_code is not null
    and w.enq_code <> '';
$fn$;

revoke all on function hr.incentive_wanted() from public, anon, authenticated;
notify pgrst, 'reload schema';

-- ⛔ এখানে `select hr.incentive_sync();` **রাখা হয়নি** — ইচ্ছে করে।
--    ০১.০৯.২০২৬: TK চালাতে গিয়ে `P0001: Master identity required` পেয়েছিলেন।
--    কারণ ওই ফাংশনটা `hr.is_master()` যাচাই করে, আর Supabase-এর SQL এডিটর
--    কোনো লগইন-করা মাস্টার নয় — তাই যাচাইটা ফেল করত, আর গোটা স্ক্রিপ্টটাই
--    (একই লেনদেনে থাকায়) ফিরে যেত।
--    ⇒ হিসাব মেলানোর দরকার নেই: **অ্যাপ নিজেই চালায়** — TK মাস্টার হিসেবে
--      কোনো স্টাফের Salary পর্দা খুললেই (`incentiveSyncThrottled`, ৫ মিনিটে
--      একবার)। তাই উপরের অংশটুকু চললেই যথেষ্ট।
