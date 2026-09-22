-- পড়া-মাত্র যাচাই — কিছুই বদলায় না।
-- TK-র প্রশ্ন (১৯.০৯.২০২৬): "RMP TK BISWAS তো Falakata থেকে আরো বেশি কমিশন
-- হওয়ার কথা" — সার্ভার-হিসাব বলছে ১৯ জন রোগী, ₹১,৯৯,২০০ মোট কমিশন। যাচাই:
-- Falakata-য় "TK BISWAS"-এর নামে রেফার-করা রোগী কতজন সত্যিই আছে, আর তাদের
-- মধ্যে কতজনের কমিশনের সারিই (fin.rmp_patient_commissions) তৈরি হয়নি —
-- অর্থাৎ কমিশন হিসাবেই ঢোকেনি।

with referred as (
  select "id", "patientId", "name", "mobile", "refDoctor", "refDoctorMobile", "bill", "discount"
  from public.patients
  where "branch" = 'Falakata'
    and (
      "refDoctorMobile" in ('8001080080', '918001080080', '+918001080080')
      or "refDoctor" ilike '%TK BISWAS%'
      or "refDoctor" ilike '%T K BISWAS%'
    )
)
select
  r."patientId"    as "রোগীর কোড",
  r."name"         as "রোগীর নাম",
  r."refDoctor"    as "রেফার-করা ডাক্তার (রোগীর সারিতে যা লেখা)",
  r."refDoctorMobile" as "রেফার-ডাক্তারের মোবাইল",
  r."bill"         as "বিল",
  case when c."id" is null then '🔴 কমিশনের সারিই নেই — এই রোগী হিসাবে ঢোকেনি'
       else '✅ কমিশনের সারি আছে (rmp_id: ' || c."rmp_id" || ')'
  end as "কমিশন-লিংক অবস্থা"
from referred r
left join fin.rmp_patient_commissions c on c.patient_row_id = r."id"
order by (c."id" is null) desc, r."patientId";
