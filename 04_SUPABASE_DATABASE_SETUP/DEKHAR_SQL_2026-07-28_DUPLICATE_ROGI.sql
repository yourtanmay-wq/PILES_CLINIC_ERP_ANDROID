-- =====================================================================
-- 👀 ডুপ্লিকেট রোগী দেখার SQL — ২৮.০৭.২০২৬ (খাতার সারি B30)
-- =====================================================================
--
-- ✅ এটা সম্পূর্ণ নিরাপদ — শুধু **দেখার** SQL।
--    • কোনো তথ্য মোছে না
--    • কোনো তথ্য বদলায় না
--    • যতবার খুশি চালানো যায়
--    (কোথাও `update`, `delete` বা `insert` লেখা নেই — মিলিয়ে দেখতে পারেন।)
--
-- কেন দরকার:
--   অ্যাপে ডুপ্লিকেট তৈরি হওয়া **বন্ধ** করা হয়ে গেছে (V147)। কিন্তু আগে
--   যেগুলো তৈরি হয়ে গেছে (যেমন ABDUL KAYAM), সেগুলো ডেটাবেসে রয়ে গেছে।
--   কোনটা কোনটা ডুপ্লিকেট আর **কোন সারিতে টাকা আছে** — সেটা আগে চোখে
--   দেখতে হবে, তারপরই সিদ্ধান্ত। না দেখে কিছু ছোঁয়া হবে না।
--
-- কোথায় চালাবেন: Supabase → বাঁ দিকের মেনু → SQL Editor → পেস্ট → RUN
-- =====================================================================


-- ---------------------------------------------------------------------
-- ১) এক নজরে: কতজন রোগীর একাধিক সারি আছে
-- ---------------------------------------------------------------------
with p as (
  select
    id,
    "patientId"          as patient_code,
    name,
    branch,
    coalesce(nullif("registrationDate",''), "date") as reg_date,
    coalesce(nullif(bill,'')::numeric, 0)           as bill,
    right(regexp_replace(coalesce(mobile,''), '\D', '', 'g'), 10) as mob10
  from public.patients
  where right(regexp_replace(coalesce(mobile,''), '\D', '', 'g'), 10) <> ''
)
select
  count(*) as "মোট ডুপ্লিকেট নম্বর"
from (select mob10 from p group by mob10 having count(*) > 1) d;


-- ---------------------------------------------------------------------
-- ২) পুরো তালিকা — একই মোবাইলে একাধিক সারি, সঙ্গে টাকার হিসাব
--    (প্রতিটা সারির নিচে দেখাবে ওই সারিতে কত টাকা জমা আছে)
-- ---------------------------------------------------------------------
with p as (
  select
    id,
    "patientId"          as patient_code,
    name,
    branch,
    coalesce(nullif("registrationDate",''), "date") as reg_date,
    coalesce(nullif(bill,'')::numeric, 0)           as bill,
    right(regexp_replace(coalesce(mobile,''), '\D', '', 'g'), 10) as mob10
  from public.patients
  where right(regexp_replace(coalesce(mobile,''), '\D', '', 'g'), 10) <> ''
),
dup as (
  select mob10 from p group by mob10 having count(*) > 1
),
-- টাকা দুই ভাবেই সারির সঙ্গে জোড়া থাকতে পারে:
--   • payments."patientId"   = রোগীর সারির ভিতরের আইডি (pat_xxxx)
--   • payments."patientId"   = মানুষের পড়ার Patient ID (KNE-...-001)
pay as (
  select
    "patientId" as key,
    count(*)                                        as pay_count,
    sum(coalesce(nullif(amount,'')::numeric, 0))     as pay_total
  from public.payments
  group by "patientId"
)
select
  p.mob10                                   as "মোবাইল (শেষ ১০)",
  p.name                                    as "নাম",
  p.patient_code                            as "Patient ID",
  p.branch                                  as "ব্রাঞ্চ",
  p.reg_date                                as "রেজিস্ট্রেশন",
  p.bill                                    as "বিল",
  coalesce(pr.pay_count, 0) + coalesce(pc.pay_count, 0) as "কতগুলো পেমেন্ট",
  coalesce(pr.pay_total, 0) + coalesce(pc.pay_total, 0) as "মোট জমা",
  p.id                                      as "সারির ভিতরের আইডি"
from p
join dup on dup.mob10 = p.mob10
left join pay pr on pr.key = p.id
left join pay pc on pc.key = p.patient_code
order by p.mob10, p.reg_date nulls last, p.id;


-- =====================================================================
-- 📖 ফল কীভাবে পড়বেন (সরল বাংলায়):
--
--   • একই "মোবাইল (শেষ ১০)" যতগুলো সারিতে দেখাবে — ততগুলো রেকর্ড ওই
--     একজন রোগীর নামে তৈরি হয়ে গেছে।
--
--   • **"মোট জমা" যে সারিতে বেশি, সেটাই আসল/চালু রেকর্ড** — ওখানেই
--     রোগীর টাকা ও চিকিৎসার ইতিহাস আছে।
--
--   • যে সারিতে **"মোট জমা" = 0 এবং "বিল" = 0**, সেটা সাধারণত ফাঁকা
--     ডুপ্লিকেট — ওটাই সরানোর কথা ভাবা যায়।
--
--   • ⚠️ দুই সারিতেই টাকা থাকলে **কিছুই সরানো যাবে না** — তখন আগে
--     ঠিক করতে হবে টাকাগুলো কোন সারিতে নিয়ে যাওয়া হবে।
--
-- ⛔ এই ফাইলে কোনো সারি সরানোর SQL ইচ্ছে করেই রাখা হয়নি।
--    TK-এর সিদ্ধান্ত ছাড়া, আর **ব্যাকআপ ছাড়া**, কিছুই ছোঁয়া হবে না।
-- =====================================================================
