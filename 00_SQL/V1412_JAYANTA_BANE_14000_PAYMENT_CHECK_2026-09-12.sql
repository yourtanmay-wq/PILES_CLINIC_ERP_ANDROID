-- V1412 (১৩.০৯.২০২৬ রাত, TK: "জয়ন্ত-র রোগীকে ₹14,000 দেওয়া হয়েছে, সেটা কোথায়?")
-- শুধু দেখার জন্য — Jalpaiguri RMP "JAYANTA BANE"-এর নামে যত টাকার সারি
-- আছে (হাতে-লেখা Referral Income + কমিশন-পেমেন্ট + সরাসরি RMP-পেমেন্ট) সবগুলো
-- এক তালিকায়, যাতে ₹14,000 কোথায় (যদি) লেখা আছে দেখা যায়। কিছু বদলায় না।
select d.name as rmp, d.mobile, d.branch, d.id as rmp_id, 'REFERRAL_INCOME_JSON' as source,
       e->>'date' as paid_on, e->>'patient' as patient, e->>'amount' as amount, e->>'status' as status
  from public.doctor_visits d
  cross join lateral jsonb_array_elements(case when jsonb_typeof(d."referralPayments") = 'array' then d."referralPayments" else '[]'::jsonb end) e
 where trim(coalesce(d.branch,'')) = 'Jalpaiguri' and upper(trim(coalesce(d.name,''))) = 'JAYANTA BANE'
union all
select d.name, d.mobile, d.branch, d.id, 'COMMISSION_PAYMENT',
       x.paid_on::text, c.patient_name, x.amount::text, x.mode
  from fin.rmp_commission_payments x
  join fin.rmp_patient_commissions c on c.id = x.patient_commission_id
  join public.doctor_visits d on d.id = x.rmp_id
 where trim(coalesce(d.branch,'')) = 'Jalpaiguri' and upper(trim(coalesce(d.name,''))) = 'JAYANTA BANE'
union all
select d.name, d.mobile, d.branch, d.id, 'DIRECT_ADVANCE',
       a.paid_on::text, '(no single patient — direct to RMP)', a.amount::text, a.mode
  from fin.rmp_advance_payments a
  join public.doctor_visits d on d.id = a.rmp_id
 where trim(coalesce(d.branch,'')) = 'Jalpaiguri' and upper(trim(coalesce(d.name,''))) = 'JAYANTA BANE'
order by 5, 6;
