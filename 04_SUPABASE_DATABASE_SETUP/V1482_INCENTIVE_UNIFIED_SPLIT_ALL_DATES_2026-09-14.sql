-- ============================================================================
-- V1482 — Extra Income-এর ভাগ এখন থেকে পুরনো-নতুন সব পেশেন্টেই একই নিয়মে
--   TK-নির্দেশ (১৪.০৯.২০২৬ রাত):
--   "আমি যেটা বললাম নতুন পুরনো সব ক্ষেত্রে একই রকম করে দিন — যে স্টাফ Enquiry
--    করেছিল সেভাবে ১০০ টাকা যদি কোন পেশেন্ট শুধু ভিজিট ফি দেয়, একই পেশেন্ট
--    যদি ট্রিটমেন্ট শুরু করে সেই ক্ষেত্রে যে এনকোয়ারি করেছিল সেভাবে ৪৫০, যে
--    রেজিস্ট্রেশন করলো সেভাবে ৫০ টাকা।"
--   AskUserQuestion-এ TK নিশ্চিত করেছেন: পুরনো রোগীদের এখনো-বাকি (না-দেওয়া)
--   কমিশনও এই নতুন নিয়মে বদলাবে — "হ্যাঁ, বদলে দিন"।
--
-- ⇒ V1184 (০৮.০৯.২০২৬)-এ যে তারিখ-ভেদ (০৮.০৯-এর আগে পুরনো ৫০/৫০ ভাগ, পরে
--   নতুন ভাগ) বসানো হয়েছিল, সেটা এখন তুলে দেওয়া হলো — এখন সব Unexpected
--   Time পেশেন্টেই (এখনো প্রোগ্রামের শুরুর তারিখ ১৮.০৮.২০২৬-এর মধ্যেই):
--     • ভিজিট ফি জমা পড়লে       → ₹১০০, পুরোটা Enquiry-ফর্ম যিনি ভরেছেন
--     • প্রথম Treatment-এর টাকা জমা পড়লে → ₹৩৫০ Enquiry-ফর্মকারী + ₹৫০ Registration-কারী
--     • Enquiry ফর্মই না থাকলে → কোনো টাকা নয়
--
-- ⚠️ TK-কে আগেই সরাসরি বলা হয়েছে ও তিনি রাজি হয়েছেন: এই বদলে পুরনো রোগীদের
--    "এখনো বাকি" (status='DUE', এখনও দেওয়া হয়নি) কমিশনের অঙ্কও বদলে যাবে।
--    ⛔ ইতিমধ্যে যা PAID হয়ে গেছে, hr.incentive_sync() কখনোই সেটা ছোঁয় না —
--    দেওয়া টাকা কক্ষনো বদলাবে না, শুধু এখনো-না-দেওয়া অঙ্কটাই নতুন নিয়মে বসবে।
-- ============================================================================

create or replace function hr.incentive_wanted()
returns table(person_code text, src_key text, amount numeric, reason text)
language sql stable security definer set search_path = hr, public
as $fn$
  with pat as (
    select p.id, coalesce(nullif(p."patientId", ''), p.id) as shown_code,
      right(regexp_replace(coalesce(p.mobile, ''), '\D', '', 'g'), 10) as pmob,
      right(regexp_replace(coalesce(p."registeredBy", ''), '\D', '', 'g'), 10) as regmob,
      nullif(left(coalesce(nullif(p."registrationDate", ''), p."date", ''), 10), '') as regdate
    from public.patients p
    where coalesce(p."timeType", '') = 'Unexpected Time'
  ), pat2 as (
    -- প্রোগ্রামের শুরুর তারিখ (১৮.০৮.২০২৬) অটুট — এটা ভাগের নিয়ম নয়, তাই ছোঁয়া হয়নি
    select * from pat where regdate ~ '^\d{4}-\d{2}-\d{2}$' and regdate::date >= date '2026-08-18'
  ), enq as (
    select distinct on (m.id) m.id,
      right(regexp_replace(coalesce(e."receivedBy", ''), '\D', '', 'g'), 10) as enqmob
    from pat2 m join public.enquiries e
      on right(regexp_replace(coalesce(e.mobile, ''), '\D', '', 'g'), 10) = m.pmob
    order by m.id, coalesce(e."createdAt", e."date", '') desc
  ), money as (
    select m.id,
      bool_or(lower(coalesce(y."payType", '')) in ('visit_fee', 'visitfee', 'registration')
              and coalesce(nullif(regexp_replace(coalesce(y."amount", ''), '[^0-9.]', '', 'g'), ''), '0')::numeric > 0) as has_fee,
      bool_or(lower(coalesce(y."payType", '')) = 'treatment'
              and coalesce(nullif(regexp_replace(coalesce(y."amount", ''), '[^0-9.]', '', 'g'), ''), '0')::numeric > 0) as has_trt
    from pat2 m join public.payments y on y."patientId" = m.id group by m.id
  ), who as (
    select m.id, m.shown_code, sr.person_code as reg_code, se.person_code as enq_code,
      coalesce(mn.has_fee, false) as has_fee, coalesce(mn.has_trt, false) as has_trt
    from pat2 m left join enq q on q.id = m.id
    left join hr.staff_profiles sr on length(m.regmob) = 10
      and right(regexp_replace(coalesce(sr.link_mobile, ''), '\D', '', 'g'), 10) = m.regmob and sr.active is not false
    left join hr.staff_profiles se on length(coalesce(q.enqmob, '')) = 10
      and right(regexp_replace(coalesce(se.link_mobile, ''), '\D', '', 'g'), 10) = q.enqmob and se.active is not false
    left join money mn on mn.id = m.id
  ),
  eligible as (
    -- Enquiry ফর্ম না থাকলে এই পেশেন্ট কোনো কমিশনেই ঢুকবে না (TK-র নিয়ম, সব তারিখেই)
    select * from who where enq_code is not null and enq_code <> ''
  ), parts as (
    select w.enq_code as pc, 'REG'::text as stage, w.id, w.shown_code, 100::numeric as amt, 'Registration'::text as label
    from eligible w where w.has_fee
    union all
    select w.enq_code, 'TRT'::text, w.id, w.shown_code, 350::numeric, 'Treatment'::text
    from eligible w where w.has_trt
    union all
    select w.reg_code, 'TRT'::text, w.id, w.shown_code, 50::numeric, 'Treatment'::text
    from eligible w where w.has_trt and w.reg_code is not null and w.reg_code <> ''
  ), rows as (
    select pc, stage, id, shown_code, sum(amt) as amt, min(label) as label
    from parts where pc is not null and pc <> '' group by pc, stage, id, shown_code
  )
  select r.pc::text, ('INC:' || r.stage || ':' || r.id || ':' || r.pc)::text, r.amt,
    (r.label || ' · ' || r.shown_code)::text
  from rows r where r.amt > 0;
$fn$;

notify pgrst, 'reload schema';

-- মিলিয়ে দেখা (শুধু পড়া, TK-র হাতে চালানোর দরকার নেই — Master অ্যাপে ঢুকলে
-- hr.incentive_sync() নিজে থেকেই এই নতুন হিসাব মতো বাকি (DUE) সারি আপডেট করবে):
-- select * from hr.incentive_wanted() order by person_code, src_key;
