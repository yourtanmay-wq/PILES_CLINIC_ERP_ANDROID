-- ============================================================================
-- V1184 — Unexpected Time-এর কমিশনের নতুন ভাগ (TK-নির্দেশ, ০৭.০৯.২০২৬)
--
-- TK-এর হুবহু কথা:
--   "Enquiry From যে Fillup করবে সে পেশেন্ট যে কোন ব্রাঞ্চ এর জন্য হতে পারে।
--    Unexpected time হলে 100/- সেই staff ই পাবে, যে রেজিষ্ট্রেশন করেছে সে
--    কিছুই পাবে না। তবে Unexpected time এর কেউ যদি ট্রিটমেন্ট শুরু করেন তাহলে
--    যে All Branch Enquiry From Fillup করেছিল সে পাবে 350, আর যে Registration
--    করেছে সে পাবে 50। 08/09/2026 থেকে এই নীয়ম চালু হবে।"
--   "Enquiry form এ Unexpected হলে তবেই কমিশন দেওয়া হয়। Enquiry form fillup
--    না হলে কোন পেশেন্ট ই Unexpected time হবে না।"
--
-- ⇒ ০৮.০৯.২০২৬ **থেকে করা রেজিস্ট্রেশনে**:
--      • Registration Fee জমা পড়লে       → ₹১০০, পুরোটা Enquiry-ফর্ম যিনি ভরেছেন
--      • প্রথম Treatment-এর টাকা জমা পড়লে → ₹৩৫০ Enquiry-ফর্মকারী + ₹৫০ Registration-কারী
--      • Enquiry ফর্মই না থাকলে → **কোনো টাকা নয়** (TK-র নিয়ম অনুযায়ী তখন
--        রোগীটা Unexpected Time-ই নয়)
--
-- ⛔ ০৮.০৯.২০২৬-এর **আগের** রেজিস্ট্রেশনগুলো হুবহু **পুরনো নিয়মেই** থাকে
--    (১৮.০৮.২০২৬ থেকে চালু V418-এর সমান-ভাগ: ₹১০০ ও ₹৪০০ দুজনের মধ্যে ভাগ,
--    Enquiry না থাকলে পুরোটা Registration-কারীর)। তাই আগে দেওয়া বা এখনো বাকি
--    থাকা কোনো পুরনো টাকা এই প্যাচে **এক পয়সাও নড়ে না**।
-- ⛔ চাবির (`src_key`) ছাঁচ হুবহু আগের: `INC:<REG|TRT>:<রোগীর id>:<staff-কোড>`,
--    তাই আগে বসে যাওয়া সারিগুলো চেনা যায় ও দ্বিতীয়বার বসে না।
-- ⛔ এই SQL কোনো সারি মোছে না, কোনো টেবিলে লেখে না — শুধু ফাংশনটা বদলায়।
--    `hr.incentive_sync()` · `src_key`-এর unique index · বাকি সব অপরিবর্তিত।
--
-- কীভাবে Run করবেন:
-- ১) Supabase Dashboard → SQL Editor → New query
-- ২) নিচের পুরো লেখাটা কপি-পেস্ট করে Run (সবুজ "Success" দেখা উচিত)
-- ============================================================================

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
      m.id, m.shown_code, m.regdate,
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
  ),
  -- ── পুরনো নিয়ম (রেজিস্ট্রেশন ১৮.০৮.২০২৬ … ০৭.০৯.২০২৬) — হুবহু V418 ──────
  old_who as (
    select * from who where regdate::date < date '2026-09-08'
  ), split as (
    -- দুজন আলাদা হলে ২ ভাগ, একই লোক (বা একজনই পাওয়া গেল) হলে ১ ভাগ
    select
      w.id, w.shown_code, w.has_fee, w.has_trt, c.pc,
      count(*) over (partition by w.id) as n
    from old_who w
    cross join lateral (
      select distinct x as pc
      from unnest(array[w.reg_code, w.enq_code]) x
      where x is not null and x <> ''
    ) c
  ), old_rows as (
    select
      s.pc                        as pc,
      st.stage                    as stage,
      s.id                        as id,
      s.shown_code                as shown_code,
      round(st.amt / s.n, 2)      as amt,
      st.label                    as label
    from split s
    cross join lateral (values
        ('REG', 100::numeric, 'Registration', s.has_fee),
        ('TRT', 400::numeric, 'Treatment',    s.has_trt)
      ) as st(stage, amt, label, ok)
    where st.ok
  ),
  -- ── নতুন নিয়ম (রেজিস্ট্রেশন ০৮.০৯.২০২৬ থেকে) — TK-নির্দেশ ০৭.০৯.২০২৬ ────
  new_who as (
    -- ⛔ Enquiry ফর্ম না থাকলে (enq_code ফাঁকা) এই রোগী তালিকাতেই আসে না
    select * from who
    where regdate::date >= date '2026-09-08'
      and enq_code is not null and enq_code <> ''
  ), new_parts as (
    -- Registration Fee → ₹১০০, পুরোটা Enquiry-ফর্মকারীর
    select w.enq_code as pc, 'REG'::text as stage, w.id, w.shown_code,
           100::numeric as amt, 'Registration'::text as label
    from new_who w where w.has_fee
    union all
    -- Treatment → ₹৩৫০ Enquiry-ফর্মকারীর
    select w.enq_code, 'TRT'::text, w.id, w.shown_code,
           350::numeric, 'Treatment'::text
    from new_who w where w.has_trt
    union all
    -- Treatment → ₹৫০ Registration-কারীর
    select w.reg_code, 'TRT'::text, w.id, w.shown_code,
           50::numeric, 'Treatment'::text
    from new_who w
    where w.has_trt and w.reg_code is not null and w.reg_code <> ''
  ), new_rows as (
    -- ⛔ একই লোক দুটো কাজই করলে তাঁর দুই ভাগ **যোগ** হয়ে একটাই সারি হয়
    --    (চাবি একই হত বলে নইলে দ্বিতীয়টা বসতেই পারত না)
    select pc, stage, id, shown_code, sum(amt) as amt, min(label) as label
    from new_parts
    where pc is not null and pc <> ''
    group by pc, stage, id, shown_code
  ), all_rows as (
    select pc, stage, id, shown_code, amt, label from old_rows
    union all
    select pc, stage, id, shown_code, amt, label from new_rows
  )
  select
    r.pc::text,
    ('INC:' || r.stage || ':' || r.id || ':' || r.pc)::text,
    r.amt,
    (r.label || ' · ' || r.shown_code)::text
  from all_rows r
  where r.amt > 0;
$fn$;

-- মিলিয়ে দেখার জন্য (কিছু বদলায় না, শুধু দেখায়):
-- select * from hr.incentive_wanted() order by person_code, src_key;
