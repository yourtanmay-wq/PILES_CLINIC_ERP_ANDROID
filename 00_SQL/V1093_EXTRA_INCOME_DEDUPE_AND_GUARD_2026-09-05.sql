-- ═══════════════════════════════════════════════════════════════════════
-- V1093 (০৫.০৯.২০২৬) — Extra Income-এর নকল সারি মোছা + স্থায়ী পাহারা
--
-- 🔴 কারণ (ডেটাবেস দেখে, আন্দাজে নয়): KNE-LAXMI-র ৮টা EXTRA সারির ৪টে
--    অ্যাপের নিজের (`src_key` আছে) — চারজন আলাদা রোগীর, ঠিক আছে। বাকি ৪টের
--    `src_key` ফাঁকা — সেগুলো আমার পুরনো SQL-এ হাতে বসানো, আর ওই দুজন
--    রোগীর অ্যাপ-সারি আগে থেকেই ছিল ⇒ ₹৪০০ নকল।
--
-- ① মোছা হয় **শুধু নকলটাই** — একই স্টাফ · একই রোগী · একই ধাপে একাধিক সারি
--    থাকলে একটাই থাকে (অ্যাপের নিজের সারিটাই আগে, নইলে সবচেয়ে পুরনোটা)।
-- ⛔ **PAID সারিতে হাত পড়ে না** — যে টাকা দেওয়া হয়ে গেছে তা কখনো মোছা হয় না,
--    শুধু `DUE` মোছে। (তাই কেউ কম টাকা পাবে না।)
-- ⛔ যে সারিতে রোগীর কোড বা ধাপ চেনা যায় না, সেটাও ছোঁয়া হয় না।
-- ⚠️ এটা **সব স্টাফের** নকল সারি ধরে — TK-কে আগেই জানানো হয়েছে; কী কী মোছা
--    হলো সেটা ফলাফলেই দেখা যায়।
--
-- ② পাহারা: একই স্টাফ · একই রোগী · একই ধাপে দ্বিতীয় সারি আর **বসতেই পারবে না**।
-- ═══════════════════════════════════════════════════════════════════════

with keyed as (
  select id, person_code, status, paid_on,
         substring(extra_reason from '[A-Z]{2,4}-[0-9]{6,8}-[0-9]{2,4}') as pcode,
         case when extra_reason ilike '%treatment%'    then 'T'
              when extra_reason ilike '%registration%' then 'R' end     as step,
         (coalesce(src_key,'') <> '')                                   as auto
  from hr.salary_payments
  where kind = 'EXTRA'
),
ranked as (
  select *, row_number() over (
      partition by person_code, pcode, step
      order by auto desc, paid_on asc, id::text asc) as rn
  from keyed
  where pcode is not null and step is not null
),
d as (
  delete from hr.salary_payments p
  using ranked r
  where p.id = r.id and r.rn > 1 and r.status = 'DUE'
  returning p.person_code, p.amount, p.status, p.paid_on, p.extra_reason
)
select * from d order by person_code, paid_on;

-- ② স্থায়ী পাহারা
create unique index if not exists tk_extra_once_per_patient_step
on hr.salary_payments (
  person_code,
  (substring(extra_reason from '[A-Z]{2,4}-[0-9]{6,8}-[0-9]{2,4}')),
  (case when extra_reason ilike '%treatment%'    then 'T'
        when extra_reason ilike '%registration%' then 'R' end)
)
where kind = 'EXTRA';
