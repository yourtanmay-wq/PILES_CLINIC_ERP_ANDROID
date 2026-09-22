-- 🔍 READ-ONLY — শুধু দেখার জন্য, কোনো টাকা/ডেটা বদলাবে না
-- TK-প্রশ্ন (১৭.০৯.২০২৬): AMIT GOLDAR-এর Referred 56 অথচ Total Entries 82 কেন?
-- ⛔ V1588-এর প্রথম চেষ্টা fin.rmp_patient_breakdown() সরাসরি ডেকেছিল, যেটা
-- Master/Staff/Doctor লগ-ইন ছাড়া SQL Editor থেকে চললে আটকে যায় (এটাই সঠিক
-- আচরণ, দোষ নয়)। এবার V1367-এর প্রমাণিত পথে (নিচু-স্তরের ফাংশন সরাসরি,
-- গেট-করা wrapper না ডেকে) একই হিসাব করা হলো, তাই SQL Editor থেকেই চলবে।
with rmp as (
  select id, "name", "mobile", "callHistory"
  from public.doctor_visits
  where right(regexp_replace(coalesce("mobile",''),'\D','','g'),10) = '9046366596'
  limit 1
), calls as (
  select jsonb_array_length(coalesce(r."callHistory", '[]'::jsonb)) as call_entries
  from rmp r
), referred as (
  select count(*) as referred_patient_rows
  from public.patients p, rmp r
  where lower(trim(coalesce(p."refBy",'')))     = lower(trim(r."name"))
     or lower(trim(coalesce(p."refDoctor",'')))  = lower(trim(r."name"))
     or right(regexp_replace(coalesce(p."refDoctorMobile",''),'\D','','g'),10)
        = right(regexp_replace(coalesce(r."mobile",''),'\D','','g'),10)
), auto as (
  -- এই RMP-র সাথে বাঁধা (commission-bound) প্রতিটা রোগী
  select case when length(right(regexp_replace(coalesce(c.patient_mobile,''),'\D','','g'),10)) = 10
              then 'm:' || right(regexp_replace(coalesce(c.patient_mobile,''),'\D','','g'),10)
              else 'r:' || c.patient_row_id end as k,
         c.patient_row_id, c.patient_name, c.patient_mobile,
         c.commission_mode, c.commission_value, c.prev_mode, c.prev_value, c.rate_changed_on,
         round(fin.rmp_earned_for(c.patient_row_id,
                 greatest(0, fin.rmp_safe_number(p."bill") - fin.rmp_safe_number(p."discount")),
                 c.commission_mode, c.commission_value, c.prev_mode, c.prev_value, c.rate_changed_on), 0) as computed,
         c.capped_amount
    from fin.rmp_patient_commissions c
    left join public.patients p on p.id = c.patient_row_id
    cross join rmp r
   where c.rmp_id = r.id
), leg_raw as (
  -- হাতে-লেখা (Entered by hand) রেফারেল ইনকাম এন্ট্রি
  select right(regexp_replace(coalesce(j->>'patientMobile',''),'\D','','g'),10) as mob10,
         lower(trim(coalesce(j->>'patient',''))) as nm,
         fin.rmp_safe_number(j->>'amount') as amt,
         (lower(trim(coalesce(j->>'status',''))) = 'paid') as is_paid
    from public.doctor_visits dv
    cross join rmp r
    cross join lateral jsonb_array_elements(case when jsonb_typeof(dv."referralPayments") = 'array' then dv."referralPayments" else '[]'::jsonb end) j
   where dv.id = r.id
), leg as (
  select case when length(mob10) = 10 then 'm:' || mob10 else 'n:' || nm end as k,
         sum(case when is_paid then amt else 0 end) as lp,
         sum(case when is_paid then 0 else amt end) as ld
    from leg_raw group by 1
), leg_m as (
  select l.*, coalesce(a1.k, null) as ak
    from leg l left join auto a1 on a1.k = l.k
), leg_by_auto as (
  select ak, sum(lp) as lp, sum(ld) as ld from leg_m where ak is not null group by ak
), rows0 as (
  select a.k, coalesce(round(coalesce(a.capped_amount, greatest(a.computed, coalesce(lb.lp,0)+coalesce(lb.ld,0))),0),0) as earned
    from auto a left join leg_by_auto lb on lb.ak = a.k
  union all
  select lm.k, round(lm.lp + lm.ld, 0) as earned
    from leg_m lm where lm.ak is null
), income as (
  select count(*) as referral_income_rows from rows0 where earned > 0.5
)
select
  (select "name" from rmp)  as rmp_name,
  (select call_entries from calls)              as "1_call_entries",
  (select referred_patient_rows from referred)  as "2_referred_patient_rows",
  (select referral_income_rows from income)     as "3_referral_income_rows",
  (select call_entries from calls) + (select referred_patient_rows from referred) + (select referral_income_rows from income) as "4_total_1_plus_2_plus_3";
