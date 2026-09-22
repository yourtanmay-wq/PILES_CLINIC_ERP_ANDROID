-- ═══════════════════════════════════════════════════════════════════════════
-- 💰🔒 V1433 (১৩.০৯.২০২৬ বিকেল, তালিকা ৫৫২) — RMP-র হিসাবে **পয়সা আর কখনো
-- বাকি/বেশি তৈরি করবে না**। V1406-এ অর্জিত (earned) পুরো টাকায় গোল হতো, কিন্তু
-- দেওয়া টাকা (given/legacy) ও থোক টাকা (pool) পয়সাসহ থাকত — তাই আমার নিজের V1411
-- (GULGAR HOSSAIN cap ₹2,205.50) → earned 2,206 − given 2,205.50 = ₹0.50 →
-- patient-wise পর্দায় ₹1, Due List-এ ₹0 (দুই পর্দায় দুরকম)। আমার ভুল।
-- এখন: earned · given · pool — তিনটেই পুরো টাকায়; তাই Earned/Paid/Due সবসময়
-- পূর্ণ সংখ্যা, সব পর্দায় এক। অ্যাপের নিজের "Fixed by Master" বোতাম আগেই পুরো
-- টাকায় গোল করে (rmp_cap_patient: round(p_cap,0)) — এই নিয়মই সবখানে বসল।
-- ⛔ কোনো সারি বদলায়/মোছে না। ফাংশনের নাম/ফেরত V1406-এর মতোই। rmp_branch_due অপরিবর্তিত।
-- 🔁 পুরনো অ্যাপ (V1400–V1405): সার্ভারের সংখ্যাই দেখায়, নিজে গোল করে না — তফাত < ₹1।
-- ═══════════════════════════════════════════════════════════════════════════
begin;

create or replace function fin.rmp_patient_breakdown(p_rmp_id text)
returns table(patient_row_id text, patient_code text, patient_name text, patient_mobile text,
              treatment_branch text, commission_mode text, commission_value numeric, set_on date,
              net_paid numeric, computed numeric, legacy_paid numeric, legacy_due numeric,
              capped_amount numeric, earned numeric, paid numeric, due numeric, source text)
language plpgsql security definer set search_path = fin, public, hr as $$
declare v_branch text; v_pool numeric := 0;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  select coalesce(x.branch,'') into v_branch from public.doctor_visits x where x.id = p_rmp_id;
  if not found then raise exception 'RMP not found'; end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this RMP branch'; end if;
  begin perform fin.rmp_backfill_commissions(p_rmp_id); exception when others then null; end;

  -- থোক টাকা (RMP-কে সরাসরি দেওয়া, কোনো রোগীর সাথে জোড়া নয়; legacy_covered বাদ)
  select round(coalesce(sum(greatest(0, a.amount - a.allocated_amount - a.legacy_covered_amount)), 0), 0)
    into v_pool from fin.rmp_advance_payments a where a.rmp_id = p_rmp_id;   -- V1433: থোক টাকাও পুরো টাকায়

  return query
  with auto as (
    select case when length(right(regexp_replace(coalesce(c.patient_mobile,''),'\D','','g'),10)) = 10
                then 'm:' || right(regexp_replace(coalesce(c.patient_mobile,''),'\D','','g'),10)
                else 'r:' || c.patient_row_id end as k,
           c.patient_row_id, c.patient_code, c.patient_name, c.patient_mobile, c.treatment_branch,
           c.commission_mode, c.commission_value, c.set_on,
           fin.rmp_net_paid_between(c.patient_row_id, null, null) as net_paid,
           round(fin.rmp_earned_for(c.patient_row_id,
                   greatest(0, fin.rmp_safe_number(p."bill") - fin.rmp_safe_number(p."discount")),
                   c.commission_mode, c.commission_value, c.prev_mode, c.prev_value, c.rate_changed_on), 0) as computed,
           coalesce((select sum(x.amount) from fin.rmp_commission_payments x where x.patient_commission_id = c.id), 0) as given,
           c.capped_amount
      from fin.rmp_patient_commissions c
      left join public.patients p on p.id = c.patient_row_id
     where c.rmp_id = p_rmp_id
  ), leg_raw as (
    select right(regexp_replace(coalesce(j->>'patientMobile',''),'\D','','g'),10) as mob10,
           lower(trim(coalesce(j->>'patient',''))) as nm,
           trim(coalesce(j->>'patient','')) as nm_raw,
           fin.rmp_safe_number(j->>'amount') as amt,
           (lower(trim(coalesce(j->>'status',''))) = 'paid') as is_paid,
           left(coalesce(j->>'date',''),10) as d
      from public.doctor_visits dv
      cross join lateral jsonb_array_elements(case when jsonb_typeof(dv."referralPayments") = 'array' then dv."referralPayments" else '[]'::jsonb end) j
     where dv.id = p_rmp_id
  ), leg as (
    select case when length(t.mob10) = 10 then 'm:' || t.mob10 else 'n:' || t.nm end as k,
           max(t.nm_raw) as nm_raw, max(t.mob10) as mob10,
           sum(case when t.is_paid then t.amt else 0 end) as lp,
           sum(case when t.is_paid then 0 else t.amt end) as ld,
           min(nullif(t.d,'')) as first_d
      from leg_raw t group by 1
  ), leg_m as (
    -- হাতে-লেখা এন্ট্রি কোন অটো-সারির সাথে যাবে: আগে মোবাইল, নইলে নাম
    select l.*, coalesce(a1.k, a2.k) as ak
      from leg l
      left join auto a1 on a1.k = l.k
      left join lateral (select a.k from auto a
                          where a1.k is null and trim(coalesce(l.nm_raw,'')) <> ''
                            and lower(trim(coalesce(a.patient_name,''))) = lower(trim(l.nm_raw))
                          order by a.set_on asc nulls last limit 1) a2 on true
  ), leg_by_auto as (
    select lm.ak, sum(lm.lp) as lp, sum(lm.ld) as ld from leg_m lm where lm.ak is not null group by lm.ak
  ), rows0 as (
    select a.k, a.patient_row_id, a.patient_code, a.patient_name, a.patient_mobile, a.treatment_branch,
           a.commission_mode, a.commission_value, a.set_on, a.net_paid, a.computed, a.given,
           coalesce(lb.lp,0) as legacy_paid, coalesce(lb.ld,0) as legacy_due, a.capped_amount, 'AUTO'::text as source
      from auto a left join leg_by_auto lb on lb.ak = a.k
    union all
    select lm.k, ''::text, ''::text, lm.nm_raw, lm.mob10, v_branch, 'LEGACY'::text, null::numeric,
           case when lm.first_d ~ '^\d{4}-\d{2}-\d{2}$' then lm.first_d::date else null end,
           0::numeric, 0::numeric, 0::numeric, lm.lp, lm.ld, null::numeric, 'LEGACY'::text
      from leg_m lm where lm.ak is null
  ), rows1 as (
    select r.*, round(coalesce(r.capped_amount, greatest(r.computed, r.legacy_paid + r.legacy_due)), 0) as earned,
           round(r.given + r.legacy_paid, 0) as specific   -- V1433: দেওয়া টাকাও পুরো টাকায়
      from rows0 r
  ), rows2 as (
    -- পুরো টাকায় গোল — পয়সার ভগ্নাংশে বাকি তৈরি হবে না
    select r.*, greatest(0, round(r.earned - r.specific, 0)) as remaining,
           greatest(0, r.specific - r.earned) as excess
      from rows1 r
  ), pooled as (
    select r.*, v_pool + (select coalesce(sum(x.excess),0) from rows2 x) as pool,
           coalesce(sum(r.remaining) over (order by r.set_on asc nulls last, r.patient_name asc, r.k asc
                                           rows between unbounded preceding and 1 preceding), 0) as before_me
      from rows2 r
  ), alloc as (
    -- পুরনো বাকি থেকে আগে কাটা (set_on ক্রমে)
    select p.*, least(p.remaining, greatest(0, p.pool - p.before_me)) as v_alloc from pooled p
  )
  select a.patient_row_id, a.patient_code, a.patient_name, a.patient_mobile, a.treatment_branch,
         a.commission_mode, a.commission_value, a.set_on,
         round(a.net_paid,2), round(a.computed,2), round(a.legacy_paid,2), round(a.legacy_due,2),
         a.capped_amount, round(a.earned,2),
         round(least(a.specific, a.earned) + a.v_alloc, 2) as paid,
         round(a.remaining - a.v_alloc, 2) as due,
         a.source
    from alloc a
   order by (a.remaining - a.v_alloc) desc, a.set_on desc nulls last, a.patient_name;
end $$;
revoke all on function fin.rmp_patient_breakdown(text) from public, anon;
grant execute on function fin.rmp_patient_breakdown(text) to authenticated;

-- RMP-র মোট — Due = রোগী-ধরে বাকির যোগ (ভাঙা হিসাবের সাথে হুবহু এক)
create or replace function fin.rmp_rmp_summary(p_rmp_id text)
returns table(patient_count bigint, earned numeric, paid_to_this_rmp numeric,
              previous_rmp_paid numeric, due numeric, overpaid numeric)
language plpgsql security definer set search_path = fin, public, hr as $$
declare v_count bigint := 0; v_earned numeric := 0; v_paid numeric := 0; v_previous numeric := 0;
        v_pool numeric := 0; v_legacy_paid numeric := 0; v_given numeric := 0; v_due numeric := 0;
begin
  select count(*), coalesce(sum(b.earned),0), coalesce(sum(b.legacy_paid),0), coalesce(sum(b.due),0)
    into v_count, v_earned, v_legacy_paid, v_due
    from fin.rmp_patient_breakdown(p_rmp_id) b;
  select coalesce(sum(x.amount),0) into v_given from fin.rmp_commission_payments x where x.rmp_id = p_rmp_id;
  select round(coalesce(sum(greatest(0, a.amount - a.allocated_amount - a.legacy_covered_amount)), 0), 0)
    into v_pool from fin.rmp_advance_payments a where a.rmp_id = p_rmp_id;
  v_paid := round(v_given + v_legacy_paid, 0) + v_pool;   -- V1433: পুরো টাকায়
  select coalesce(sum(x.amount),0) into v_previous
    from fin.rmp_commission_payments x join fin.rmp_patient_commissions pc on pc.id = x.patient_commission_id
   where pc.rmp_id = p_rmp_id and x.rmp_id <> p_rmp_id;
  return query select v_count, round(v_earned,2), round(v_paid,2), round(v_previous,2),
                      round(v_due,2), round(greatest(0, round(v_paid - v_earned, 0)),2);
end $$;
revoke all on function fin.rmp_rmp_summary(text) from public, anon;
grant execute on function fin.rmp_rmp_summary(text) to authenticated;

notify pgrst, 'reload schema';
commit;
