-- ═══════════════════════════════════════════════════════════════════════════
-- 🔎 V1424 (১৩.০৯.২০২৬ সকাল, তালিকা ৫৩৪) — JAKIR HOSSAIN (KHOCHABARI, Cooch Behar,
-- +917407407675): Ref. Paid ₹13,000 অথচ Advance পপ-আপে ₹3,000, বাস্তবে দেওয়া ₹8,000।
-- ⛔ শুধু পড়া — কিছু বদলায় না। Ref. Paid (V1406) = হাতে-লেখা Referral Income-এর "Paid"
-- এন্ট্রি (doctor_visits.referralPayments) + রোগী-ধরে দেওয়া + থোক অগ্রিম (rmp_advance_payments)।
-- Advance পপ-আপ শুধু অগ্রিম দেখায় (₹3,000) — তাই দুটো সংখ্যা এক হওয়ার কথা নয়।
-- 🔎 সন্দেহ (দোষ নয়, যাচাই বাকি): গতকাল V1410-এ নকল JAKIR রেকর্ডের (CHILKIRHAT) হাতে-লেখা
-- এন্ট্রিগুলো আসলটায় "union all" করে জোড়া হয়েছিল — দুটোতেই TOTA MIYA ₹5,000 "Paid" থাকলে
-- এখন একই টাকা দুবার (₹5,000 + ₹5,000 + অগ্রিম ₹3,000 = ₹13,000)। গতকাল Ref. Paid ছিল ₹8,000।
-- এই SQL-এর ফলে HAND-WRITTEN সারিতে TOTA MIYA দুবার এলে সেটাই কারণ — তখন একটা সরানোর SQL দেবো।
-- ═══════════════════════════════════════════════════════════════════════════
with d as (
  select dv.id, dv.name, dv.mobile, dv.branch, dv."referralPayments"
    from public.doctor_visits dv
   where right(regexp_replace(coalesce(dv.mobile,''),'\D','','g'),10) = '7407407675'
     and trim(coalesce(dv.branch,'')) = 'Cooch Behar'
)
select 'HAND-WRITTEN (referralPayments)' as kind, j.ord::int as n,
       coalesce(j.e->>'patient','') as patient, coalesce(j.e->>'patientMobile','') as patient_mobile,
       fin.rmp_safe_number(j.e->>'amount') as amount, coalesce(j.e->>'status','') as status,
       left(coalesce(j.e->>'date',''),10) as on_date, coalesce(j.e->>'note', j.e->>'remarks', '') as note
  from d
  cross join lateral jsonb_array_elements(case when jsonb_typeof(d."referralPayments") = 'array' then d."referralPayments" else '[]'::jsonb end) with ordinality j(e, ord)
union all
select 'ADVANCE (rmp_advance_payments)', 0, a.rmp_name, '', a.amount, a.mode || ' · allocated ' || a.allocated_amount || ' · legacy_covered ' || a.legacy_covered_amount,
       to_char(a.paid_on,'YYYY-MM-DD'), coalesce(a.reference_no,'')
  from fin.rmp_advance_payments a join d on a.rmp_id = d.id
union all
select 'PER-PATIENT PAYMENT (rmp_commission_payments)', 0, coalesce(c.patient_name,''), coalesce(c.patient_mobile,''), x.amount, '', '', ''
  from fin.rmp_commission_payments x
  join fin.rmp_patient_commissions c on c.id = x.patient_commission_id
  join d on c.rmp_id = d.id
order by 1, 2;
