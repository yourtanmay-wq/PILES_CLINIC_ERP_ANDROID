-- ═══════════════════════════════════════════════════════════════════════════
-- 🔎 V1425 (১৩.০৯.২০২৬ দুপুর, তালিকা ৫৩৫) — সব ব্রাঞ্চের প্রতিটা RMP: ডেটাবেসে আসল Paid কত,
-- আর পুরনো V1400 অ্যাপ কত দেখাচ্ছে (বেশি)। ⛔ শুধু পড়া — কিছু বদলায় না।
--
-- কারণ (আমার ভুল, সোজা স্বীকার): গতকাল V1406-এ সার্ভারের Paid-এর ভিতরে হাতে-লেখা "Paid" এন্ট্রি
-- ঢুকিয়ে দিয়েছি (এক-খাতা), কিন্তু ফোনের পুরনো V1400 অ্যাপ হাতে-লেখা Paid নিজে আবার যোগ করে —
-- ফলে নতুন অ্যাপ না বসা পর্যন্ত পুরনো অ্যাপে **যে RMP-র হাতে-লেখা Paid এন্ট্রি আছে** তার Ref. Paid
-- বেশি দেখাচ্ছে (JAKIR: ₹8,000 → ₹13,000)। ডেটাবেসের টাকা ঠিকই আছে।
--
-- কলাম: hand_written_paid = হাতে-লেখা Paid এন্ট্রির যোগ · advance_pool = অগ্রিমের বাকি অংশ ·
--       per_patient_given = রোগী-ধরে দেওয়া · db_paid (নতুন অ্যাপ/ওয়েব দেখায়) = তিনটের যোগ (V1406) ·
--       old_app_shows = db_paid + hand_written_paid − legacy_covered (পুরনো অ্যাপের নিজের যোগ)
-- যে সারিতে old_app_shows ≠ db_paid — সেই RMP পুরনো অ্যাপে ভুল দেখাচ্ছে (নতুন বিল্ডে ঠিক)।
-- ═══════════════════════════════════════════════════════════════════════════
with hw as (
  select dv.id, sum(case when lower(trim(coalesce(j->>'status',''))) = 'paid' then fin.rmp_safe_number(j->>'amount') else 0 end) as hand_paid,
         count(*) filter (where lower(trim(coalesce(j->>'status',''))) = 'paid') as hand_rows
    from public.doctor_visits dv
    cross join lateral jsonb_array_elements(case when jsonb_typeof(dv."referralPayments") = 'array' then dv."referralPayments" else '[]'::jsonb end) j
   group by dv.id
), adv as (
  select a.rmp_id, sum(greatest(0, a.amount - a.allocated_amount - a.legacy_covered_amount)) as pool,
         sum(a.legacy_covered_amount) as covered, sum(a.amount) as advance_total
    from fin.rmp_advance_payments a group by a.rmp_id
), giv as (
  select c.rmp_id, sum(x.amount) as given
    from fin.rmp_commission_payments x join fin.rmp_patient_commissions c on c.id = x.patient_commission_id
   group by c.rmp_id
)
select dv.branch, dv.name, right(regexp_replace(coalesce(dv.mobile,''),'\D','','g'),10) as mobile,
       coalesce(hw.hand_rows,0) as hand_written_rows,
       coalesce(hw.hand_paid,0) as hand_written_paid,
       coalesce(adv.advance_total,0) as advance_total,
       coalesce(adv.pool,0) as advance_pool,
       coalesce(giv.given,0) as per_patient_given,
       coalesce(giv.given,0) + coalesce(hw.hand_paid,0) + coalesce(adv.pool,0) as db_paid_new_app,
       coalesce(giv.given,0) + coalesce(hw.hand_paid,0) + coalesce(adv.pool,0) + coalesce(hw.hand_paid,0) - coalesce(adv.covered,0) as old_app_shows,
       case when coalesce(hw.hand_paid,0) - coalesce(adv.covered,0) <> 0 then 'OLD APP WRONG' else 'same' end as note
  from public.doctor_visits dv
  left join hw on hw.id = dv.id
  left join adv on adv.rmp_id = dv.id
  left join giv on giv.rmp_id = dv.id
 where coalesce(hw.hand_paid,0) > 0 or coalesce(adv.advance_total,0) > 0 or coalesce(giv.given,0) > 0
 order by (coalesce(hw.hand_paid,0) - coalesce(adv.covered,0)) desc, dv.branch, dv.name;
