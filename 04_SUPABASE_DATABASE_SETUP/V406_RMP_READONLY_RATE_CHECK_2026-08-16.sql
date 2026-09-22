-- =====================================================================
-- V406 — RMP-র Default % সত্যিই সবার ক্ষেত্রে মিলছে কিনা, শুধু **দেখা**
--        🔧 সংশোধিত সংস্করণ (TK-এর লাইভ এরর দেখে)
-- =====================================================================
-- ⛔⛔ এই ফাইল একটাও ঘর বদলায় না, একটাও সারি মোছে না, এক পয়সাও নড়ায় না।
--
-- 🔧 কী ভুল ছিল ও কীভাবে সারানো হলো:
--    TK চালিয়ে পেয়েছিলেন — `ERROR: 42703: column pc.updated_by does not exist`।
--    কারণ: `fin.rmp_patient_commissions`-এ ঘরটার নাম **`updated_by` নয়,
--    `set_by`** (V325_RMP_COMMISSION_SAFE_FOUNDATION-এর টেবিল-গঠন দেখে
--    মিলিয়ে নেওয়া হয়েছে)।
--    সঙ্গে আরও ভালো হলো: রোগীর নাম · কোড · ব্রাঞ্চ **ওই টেবিলেই আছে**
--    (`patient_name` · `patient_code` · `treatment_branch`) — তাই
--    `public.patients`-এর সঙ্গে জোড়ার দরকারই নেই। এক ধাপ কম, আর
--    রোগীর সারি মুছে গেলেও নাম দেখাবে।
--
-- ✅ যাচাই: V325-এর **হুবহু টেবিল-গঠনে** (+ V380-এর `use_rmp_default`)
--    আসল PostgreSQL 16-এ চালানো হয়েছে — পাঁচটা ফলই ঠিকঠাক এসেছে।
--
-- 🔴 কেন সরাসরি % বদলে দিইনি — সৎভাবে:
--    TK বলেছেন "পুরনো সব হিসাবেও %-টা মিলুক"। কিন্তু কোড ও পুরনো SQL পড়ে
--    দেখা গেল, অন্ধভাবে করলে **সত্যিকারের টাকার ক্ষতি** হতে পারে:
--      ১) যে কমিশন **ইতিমধ্যে নগদে দেওয়া হয়ে গেছে** সেগুলো পুরনো হারে।
--         নতুন হারে কষলে ওই টাকা "বেশি দেওয়া হয়েছে" দেখাবে — অর্থাৎ
--         RMP-র কাছে ক্লিনিক টাকা পাবে বলে দেখাবে, যা সত্যি নয়।
--      ২) কিছু রোগীর % **ইচ্ছে করে আলাদা** বসানো। সবার উপরে এক % চাপালে
--         ওই ইচ্ছাকৃত সিদ্ধান্তগুলো মুছে যাবে — আর এখন "ইচ্ছাকৃত" ও
--         "পুরনো স্বয়ংক্রিয় কপি" আলাদা করে চেনার নিশ্চিত উপায় নেই।
--      ৩) পুরনো `referralPayments`-এ **কোনো %-ই লেখা নেই**, শুধু টাকার অঙ্ক।
--         ওগুলো % দিয়ে বানানো মানে **সংখ্যা আন্দাজে বানানো**।
--      ৪) TK BISWAS-এর ₹২০,০০০-এর হাতে-মেলানো এন্ট্রি দু'বার গোনা হয়ে
--         যেতে পারে (V382-এ `legacy_covered_amount` দিয়ে আটকানো আছে)।
--      ৫) ডেটাবেসে কোন নিয়মটা **আসলে বসানো আছে তা-ই অনিশ্চিত** — V398-এর
--         নোটেই সন্দেহ ছিল V383 আদৌ চালানো হয়েছিল কিনা।
--
--    আর সবচেয়ে বড় কথা: "RMP-র Default বদলালে পুরনো রোগীর হিসাব বদলাবে না"
--    — এই নিয়মটা **TK নিজেই অনুমোদন করেছিলেন** (V325, ১২.০৮.২০২৬), এবং
--    সেদিন প্রমাণও দেখানো হয়েছিল। তাই উল্টানোর আগে সংখ্যা দেখানো দরকার।
--
-- চালানোর নিয়ম: Supabase → SQL Editor → New query → পুরো ফাইল পেস্ট → Run।
-- =====================================================================

-- ---------------------------------------------------------------------
-- ০) 🔴 সবার আগে — ডেটাবেসে কোন নিয়মটা আসলে বসানো আছে?
-- ---------------------------------------------------------------------
select
  (select count(*) from pg_proc where proname='rmp_summary')                   as "rmp_summary আছে",
  (select count(*) from information_schema.tables
     where table_schema='fin' and table_name='rmp_commission_default_history') as "V380 হার-ইতিহাস",
  (select count(*) from information_schema.columns
     where table_schema='fin' and table_name='rmp_patient_commissions'
       and column_name='use_rmp_default')                                      as "V380 use_rmp_default",
  coalesce((select pg_get_functiondef(oid) like '%v_unallocated%'
     from pg_proc where proname='rmp_rmp_summary' limit 1), false)             as "V383/V398 বসানো";

-- ---------------------------------------------------------------------
-- ১) প্রতিটি RMP-র এখনকার Default কত
-- ---------------------------------------------------------------------
select rmp_name as "RMP", rmp_mobile as "মোবাইল", commission_mode as "ধরন",
       commission_value as "Default", effective_from as "কবে থেকে", updated_by as "কে বসিয়েছে"
from fin.rmp_commission_defaults order by rmp_name;

-- ---------------------------------------------------------------------
-- ২) 🔴 আসল প্রশ্ন — কার হিসাব Default মানছে, কার মানছে না
-- ---------------------------------------------------------------------
select
  d.rmp_name as "RMP", d.commission_value as "Default", count(*) as "মোট রোগী",
  count(*) filter (where pc.use_rmp_default)     as "Default মানছে",
  count(*) filter (where not pc.use_rmp_default) as "নিজের আলাদা %",
  count(*) filter (where not pc.use_rmp_default
        and pc.commission_value = d.commission_value
        and pc.commission_mode = d.commission_mode) as "আলাদা, কিন্তু মান একই",
  count(*) filter (where not pc.use_rmp_default
        and (pc.commission_value <> d.commission_value
          or pc.commission_mode <> d.commission_mode)) as "🔴 মান সত্যিই আলাদা"
from fin.rmp_patient_commissions pc
join fin.rmp_commission_defaults d on d.rmp_id = pc.rmp_id
group by d.rmp_name, d.commission_value, d.commission_mode
order by 7 desc, 1;

-- ---------------------------------------------------------------------
-- ৩) 🔴 যাদের মান সত্যিই আলাদা — একজন একজন করে (সর্বোচ্চ ৫০)
--    এদের হিসাব "সবার এক %" করলে **বদলে যাবে**। কোনগুলো TK ইচ্ছে করে
--    আলাদা করেছিলেন — সেটা তাঁকেই দেখে বলতে হবে।
-- ---------------------------------------------------------------------
select
  d.rmp_name as "RMP", pc.patient_name as "রোগী", pc.patient_code as "Patient ID",
  pc.treatment_branch as "ব্রাঞ্চ",
  d.commission_mode || ' ' || d.commission_value   as "RMP-র Default",
  pc.commission_mode || ' ' || pc.commission_value as "এই রোগীর হিসাবে",
  pc.set_on as "কবে বসানো", pc.set_by as "কে বসিয়েছে"
from fin.rmp_patient_commissions pc
join fin.rmp_commission_defaults d on d.rmp_id = pc.rmp_id
where not pc.use_rmp_default
  and (pc.commission_value <> d.commission_value or pc.commission_mode <> d.commission_mode)
order by d.rmp_name, pc.patient_name limit 50;

-- ---------------------------------------------------------------------
-- ৪) 🔴🔴 সবচেয়ে জরুরি — কত টাকা **ইতিমধ্যে হাতে দেওয়া হয়ে গেছে**
--    এগুলো পুরনো হারে দেওয়া। % বদলে হিসাব কষলে এর একটা অংশ
--    "বেশি দেওয়া হয়েছে" দেখাতে শুরু করবে।
-- ---------------------------------------------------------------------
select coalesce(d.rmp_name, cp.rmp_name, cp.rmp_id) as "RMP",
  count(*) as "কতবার দেওয়া হয়েছে", sum(cp.amount) as "মোট দেওয়া টাকা",
  min(cp.paid_on) as "প্রথম", max(cp.paid_on) as "শেষ"
from fin.rmp_commission_payments cp
left join fin.rmp_commission_defaults d on d.rmp_id = cp.rmp_id
group by 1 order by 3 desc nulls last;

-- ---------------------------------------------------------------------
-- ৫) পুরনো `referralPayments` — এগুলোয় কোনো % লেখাই নেই, শুধু টাকা।
--    ⛔ এগুলো % দিয়ে নতুন করে হিসাব করা **অসম্ভব** (তথ্যই নেই)।
-- ---------------------------------------------------------------------
select
  count(*) filter (where dv."referralPayments" is not null
        and dv."referralPayments"::text not in ('[]','null','')) as "যত RMP-র পুরনো এন্ট্রি",
  coalesce(sum((select count(*) from jsonb_array_elements(
      case when jsonb_typeof(dv."referralPayments")='array'
           then dv."referralPayments" else '[]'::jsonb end))),0) as "মোট পুরনো এন্ট্রি",
  coalesce(sum((select coalesce(sum((e->>'amount')::numeric),0)
    from jsonb_array_elements(
      case when jsonb_typeof(dv."referralPayments")='array'
           then dv."referralPayments" else '[]'::jsonb end) e
    where lower(coalesce(e->>'status',''))='paid')),0)          as "🔴 পুরনো পথে দেওয়া টাকা"
from public.doctor_visits dv;

-- =====================================================================
-- ⛔ এখানেই শেষ। কিচ্ছু বদলায়নি।
--
-- 📋 TK-কে দেখতে হবে:
--    · ২ নম্বরের শেষ ঘর — কতজন রোগীর হিসাব সত্যিই আলাদা
--    · ৩ নম্বরের তালিকা — এদের মধ্যে কারা ইচ্ছাকৃত, কারা ভুল
--    · ৪ ও ৫ নম্বর — কত টাকা ইতিমধ্যে দেওয়া হয়ে গেছে
--
--    এই সংখ্যাগুলো দেখে TK বলবেন কোন পথে যাব।
--    তার আগে এক পয়সাও নড়ানো হবে না।
-- =====================================================================
