-- ═══════════════════════════════════════════════════════════════════════
-- V586 (২৩.০৮.২০২৬) — RMP-এর পুরনো রোগীদের কমিশন বসেছে কি না, শুধু দেখা।
--
-- TK-এর সিদ্ধান্ত: *"আগে তালিকা দেখান"*।
--
-- ⛔⛔ এই SQL কিচ্ছু বদলায় না — একটাও insert / update / delete নেই।
--     শুধু `select`, অর্থাৎ পড়া। নিরাপদে যতবার খুশি চালানো যায়।
--
-- কে কোন RMP-এর রোগী, সেই নিয়মটা নিজে বানানো হয়নি — প্রকল্পে আগে থেকেই
-- চলা `fin.rmp_legacy_view_all_v2()`-এর হুবহু একই নিয়ম ব্যবহার করা হয়েছে
-- (V384, লাইন ৭৫–৮১): রোগীর `refBy` RMP-এর নামের সাথে মেলে, **অথবা**
-- রোগীর `refDoctorMobile`-এর শেষ ১০ অঙ্ক RMP-এর নম্বরের সাথে মেলে।
--
-- Supabase → SQL Editor-এ পুরোটা কপি করে Run করুন। দুটো ফল আসবে।
-- ═══════════════════════════════════════════════════════════════════════

-- ── ফল ১ · প্রতিটা RMP-এর সারাংশ ──────────────────────────────────────
with rmp as (
  select d.rmp_id,
         coalesce(nullif(trim(d.rmp_name),''), v.name, '')            as rmp_naam,
         d.commission_mode                                            as dhoron,
         d.commission_value                                           as maan,
         lower(trim(coalesce(v.name,'')))                             as name_key,
         right(regexp_replace(coalesce(v.mobile,''),'[^0-9]','','g'),10) as mob_key,
         coalesce(v.branch,'')                                        as branch
  from fin.rmp_commission_defaults d
  join public.doctor_visits v on v.id = d.rmp_id
),
matched as (
  select r.rmp_id, r.rmp_naam, r.dhoron, r.maan, r.branch,
         p.id                                   as patient_row_id,
         coalesce(p."patientId",'')             as rogi_id,
         coalesce(p.name,'')                    as rogir_naam,
         right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) as mobile,
         coalesce(nullif(trim(coalesce(p."registrationDate",'')),''),
                  coalesce(p.date,''))          as tarikh,
         fin.rmp_safe_number(p.bill::text)      as bill
  from rmp r
  join public.patients p
    on ( lower(trim(coalesce(p."refBy",''))) <> ''
         and lower(trim(p."refBy")) = r.name_key )
    or ( length(r.mob_key) = 10
         and right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10) = r.mob_key )
),
flagged as (
  select m.*,
         (c.patient_row_id is not null) as commission_bosano
  from matched m
  left join fin.rmp_patient_commissions c on c.patient_row_id = m.patient_row_id
)
select
  rmp_naam                                                as "RMP",
  branch                                                  as "ব্রাঞ্চ",
  dhoron || ' ' || maan::text                             as "ডিফল্ট কমিশন",
  count(*)                                                as "মোট রোগী",
  count(*) filter (where commission_bosano)               as "কমিশন বসেছে",
  count(*) filter (where not commission_bosano)           as "বসেনি",
  round(sum(bill) filter (where not commission_bosano), 2) as "বসেনি — মোট বিল",
  round(sum(bill) filter (where not commission_bosano)
        * case when dhoron = 'PERCENT' then maan / 100.0 else 0 end, 2)
                                                          as "৫০% হলে কত টাকা"
from flagged
group by rmp_naam, branch, dhoron, maan
order by "বসেনি" desc;


-- ── ফল ২ · যে রোগীদের কমিশন বসেনি, তাদের তালিকা ──────────────────────
with rmp as (
  select d.rmp_id,
         coalesce(nullif(trim(d.rmp_name),''), v.name, '')            as rmp_naam,
         d.commission_mode                                            as dhoron,
         d.commission_value                                           as maan,
         lower(trim(coalesce(v.name,'')))                             as name_key,
         right(regexp_replace(coalesce(v.mobile,''),'[^0-9]','','g'),10) as mob_key
  from fin.rmp_commission_defaults d
  join public.doctor_visits v on v.id = d.rmp_id
),
matched as (
  select r.rmp_naam, r.dhoron, r.maan,
         p.id                                   as patient_row_id,
         coalesce(p."patientId",'')             as rogi_id,
         coalesce(p.name,'')                    as rogir_naam,
         right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) as mobile,
         coalesce(p.branch,'')                  as branch,
         coalesce(nullif(trim(coalesce(p."registrationDate",'')),''),
                  coalesce(p.date,''))          as tarikh,
         fin.rmp_safe_number(p.bill::text)      as bill
  from rmp r
  join public.patients p
    on ( lower(trim(coalesce(p."refBy",''))) <> ''
         and lower(trim(p."refBy")) = r.name_key )
    or ( length(r.mob_key) = 10
         and right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10) = r.mob_key )
)
select
  m.rmp_naam    as "RMP",
  m.rogi_id     as "রোগী আইডি",
  m.rogir_naam  as "রোগীর নাম",
  m.mobile      as "মোবাইল",
  m.branch      as "ব্রাঞ্চ",
  m.tarikh      as "তারিখ",
  round(m.bill, 2) as "বিল",
  round(m.bill * case when m.dhoron = 'PERCENT' then m.maan / 100.0 else 0 end, 2)
                as "কমিশন হলে"
from matched m
left join fin.rmp_patient_commissions c on c.patient_row_id = m.patient_row_id
where c.patient_row_id is null
order by m.rmp_naam, m.tarikh desc;
