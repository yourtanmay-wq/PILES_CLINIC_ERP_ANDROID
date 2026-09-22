-- ═══════════════════════════════════════════════════════════════════════════
-- V1359 (১১.০৯.২০২৬) — ০৪.০৯.২০২৬-এ JH MANDAL-কে দেওয়া ₹2,800 কোথায় বসেছে?
--   V1358-এর ফলে ০৪.০৯-এ RMP-কে দেওয়ার কোনো সারি নেই; স্টাফ বলছেন দিয়েছেন।
--   এই SQL চার জায়গায় খোঁজে (০৩.০৯ – ০৬.০৯):
--     A) সব RMP-কে দেওয়া টাকা (আগাম + রোগীর নামে) — ভুল RMP-র নামে বসেছে কিনা
--     B) 7479173399 নম্বরের **সব** RMP-সারি (যেকোনো ব্রাঞ্চ) — জোড়া সারি আছে কিনা
--     C) পুরনো "Referral Income" পদ্ধতির (doctor_visits.referralPayments) সারি
--     D) দৈনিক খরচের খাতা (fin.expenses) — RMP-র টাকা খরচ হিসেবে তোলা হয়েছে কিনা
--   + E) JH MANDAL-কে দেওয়া প্রতিটা টাকার "কবে তোলা হয়েছিল" (recorded_at) — ১১.০৯-এর
--        ₹2,800 আসলে ০৪.০৯-এই তোলা কিনা
-- ⛔ শুধু **পড়া** — কিছু লেখা/বদলানো হয় না।
-- চালানোর নিয়ম: Supabase → SQL Editor → New query → পুরো ফাইল পেস্ট → Run।
-- ═══════════════════════════════════════════════════════════════════════════

with win as (
  select date '2026-09-03' as d1, date '2026-09-06' as d2
),
-- A) ওই দিনগুলোতে যে-কোনো RMP-কে দেওয়া টাকা
a_adv as (
  select 'A · RMP PAYMENT (advance)'::text as source, to_char(a.paid_on,'YYYY-MM-DD') as "date",
         coalesce(a.rmp_name,'') as who, coalesce(a.branch,'') as branch, a.amount, coalesce(a.mode,'') as mode,
         ''::text as detail, to_char(a.recorded_at,'YYYY-MM-DD HH24:MI') as recorded_at, coalesce(a.recorded_by,'') as recorded_by
  from fin.rmp_advance_payments a, win
  where a.paid_on between win.d1 and win.d2
),
a_com as (
  select 'A · RMP PAYMENT (patient)'::text, to_char(p.paid_on,'YYYY-MM-DD'),
         coalesce(p.rmp_name,''), coalesce(p.treatment_branch,''), p.amount, coalesce(p.mode,''),
         coalesce(pc.patient_name,''), to_char(p.recorded_at,'YYYY-MM-DD HH24:MI'), coalesce(p.recorded_by,'')
  from fin.rmp_commission_payments p
  left join fin.rmp_patient_commissions pc on pc.id = p.patient_commission_id, win
  where p.paid_on between win.d1 and win.d2
),
-- B) 7479173399 নম্বরের সব RMP-সারি
b_rows as (
  select 'B · RMP ROW (same number)'::text, coalesce(d."updatedAt",''), coalesce(d.name,''), coalesce(d.branch,''),
         null::numeric, coalesce(d.status,''), 'id=' || d.id, ''::text, coalesce(d."createdBy",'')
  from public.doctor_visits d
  where right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) = '7479173399'
),
-- C) পুরনো Referral Income সারি (ওই নম্বরের সব RMP-সারিতে), ০৩–০৬.০৯
c_legacy as (
  select 'C · OLD Referral Income'::text, left(coalesce(e->>'date',''),10), coalesce(d.name,''), coalesce(d.branch,''),
         fin.rmp_safe_number(e->>'amount'), coalesce(e->>'status',''),
         coalesce(e->>'patient','') || ' · ' || coalesce(e->>'mode',''), ''::text, ''::text
  from public.doctor_visits d
  cross join lateral jsonb_array_elements(
    case when jsonb_typeof(d."referralPayments") = 'array' then d."referralPayments" else '[]'::jsonb end) e
  where right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) = '7479173399'
    and left(coalesce(e->>'date',''),10) between '2026-09-03' and '2026-09-06'
),
-- D) খরচের খাতা, কোচবিহার, ০৩–০৬.০৯
d_exp as (
  select 'D · EXPENSE BOOK'::text, to_char(x.entry_date,'YYYY-MM-DD'), coalesce(x.paid_to,''), coalesce(x.branch,''),
         x.amount, coalesce(x.mode,''), coalesce(x.category,'') || ' · ' || coalesce(x.note,'') ||
         case when x.ignored then ' · IGNORED' else '' end,
         to_char(x.created_at,'YYYY-MM-DD HH24:MI'), coalesce(x.created_by,'')
  from fin.expenses x, win
  where x.entry_date between win.d1 and win.d2
    and lower(trim(coalesce(x.branch,''))) = 'cooch behar'
),
-- E) JH MANDAL (কোচবিহার)-কে দেওয়া সব টাকা — কবে তোলা হয়েছিল
e_all as (
  select 'E · JH MANDAL all payments'::text, to_char(a.paid_on,'YYYY-MM-DD'), coalesce(a.rmp_name,''), coalesce(a.branch,''),
         a.amount, coalesce(a.mode,''), 'advance', to_char(a.recorded_at,'YYYY-MM-DD HH24:MI'), coalesce(a.recorded_by,'')
  from fin.rmp_advance_payments a
  join public.doctor_visits d on d.id = a.rmp_id
  where right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) = '7479173399'
  union all
  select 'E · JH MANDAL all payments'::text, to_char(p.paid_on,'YYYY-MM-DD'), coalesce(p.rmp_name,''), coalesce(p.treatment_branch,''),
         p.amount, coalesce(p.mode,''), 'patient: ' || coalesce(pc.patient_name,''), to_char(p.recorded_at,'YYYY-MM-DD HH24:MI'), coalesce(p.recorded_by,'')
  from fin.rmp_commission_payments p
  join public.doctor_visits d on d.id = p.rmp_id
  left join fin.rmp_patient_commissions pc on pc.id = p.patient_commission_id
  where right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) = '7479173399'
    and not exists (select 1 from fin.rmp_advance_allocations al where al.commission_payment_id = p.id)
)
select * from a_adv
union all select * from a_com
union all select * from b_rows
union all select * from c_legacy
union all select * from d_exp
union all select * from e_all
order by 1, 2, 3;
