-- V1405 (১২.০৯.২০২৬ রাত) — শুধু দেখার জন্য (read-only): V1404-এর হিসাব-ফাংশনটা
-- SQL Editor-এ হাতে চালিয়ে দেখা — কোথায় আটকাচ্ছে / কী সংখ্যা আসে।
-- ⛔ কিছুই বদলায় না। নিচের ফাংশনটা pg_temp-এ (এই এক Run-এর জন্যই) তৈরি হয়,
--    Run শেষ হলে নিজে থেকেই মুছে যায় — ডেটাবেসে কিছু থাকে না।
-- লাল এরর এলে সেটার ফটো পাঠান; টেবিল এলে Export → CSV পাঠান।
create function pg_temp.rmp_bd_dbg(p_rmp_id text)
returns table(patient_row_id text, patient_code text, patient_name text, patient_mobile text,
              treatment_branch text, commission_mode text, commission_value numeric, set_on date,
              net_paid numeric, computed numeric, legacy_paid numeric, legacy_due numeric,
              capped_amount numeric, earned numeric, paid numeric, due numeric, source text)
language plpgsql as $$
#variable_conflict use_column
declare v_branch text; v_pool numeric := 0; v_excess numeric := 0; r record; v_alloc numeric;
begin
  select coalesce(x.branch,'') into v_branch from public.doctor_visits x where x.id = p_rmp_id;
  if not found then raise exception 'RMP not found'; end if;
  -- V489/V815: পুরনো রেফার করা রোগীর কমিশন-সারি না থাকলে একবার তৈরি (ব্যর্থ হলেও হিসাব থামে না)

  drop table if exists _rmp_led;
  create temp table _rmp_led (
    k text, patient_row_id text, patient_code text, patient_name text, patient_mobile text,
    treatment_branch text, commission_mode text, commission_value numeric, set_on date,
    net_paid numeric default 0, computed numeric default 0, given numeric default 0,
    legacy_paid numeric default 0, legacy_due numeric default 0, capped_amount numeric,
    earned numeric default 0, specific numeric default 0, paid numeric default 0, due numeric default 0,
    source text
  ) on commit drop;

  -- (ক) অটো-কমিশনের সারি — Earned পুরো টাকায় গোল
  insert into _rmp_led(k, patient_row_id, patient_code, patient_name, patient_mobile, treatment_branch,
                       commission_mode, commission_value, set_on, net_paid, computed, given, capped_amount, source)
  select case when length(right(regexp_replace(coalesce(c.patient_mobile,''),'\D','','g'),10)) = 10
              then 'm:' || right(regexp_replace(coalesce(c.patient_mobile,''),'\D','','g'),10)
              else 'r:' || c.patient_row_id end,
         c.patient_row_id, c.patient_code, c.patient_name, c.patient_mobile, c.treatment_branch,
         c.commission_mode, c.commission_value, c.set_on,
         fin.rmp_net_paid_between(c.patient_row_id, null, null),
         round(fin.rmp_earned_for(c.patient_row_id,
                 greatest(0, fin.rmp_safe_number(p."bill") - fin.rmp_safe_number(p."discount")),
                 c.commission_mode, c.commission_value, c.prev_mode, c.prev_value, c.rate_changed_on), 0),
         coalesce((select sum(x.amount) from fin.rmp_commission_payments x where x.patient_commission_id = c.id), 0),
         c.capped_amount, 'AUTO'
  from fin.rmp_patient_commissions c
  left join public.patients p on p.id = c.patient_row_id
  where c.rmp_id = p_rmp_id;

  -- (খ) হাতে-লেখা Referral Income এন্ট্রি — মোবাইল (নইলে নাম) ধরে রোগীর সাথে মেলানো
  for r in
    with e as (
      select x as j from public.doctor_visits d,
             jsonb_array_elements(case when jsonb_typeof(d."referralPayments") = 'array' then d."referralPayments" else '[]'::jsonb end) x
      where d.id = p_rmp_id
    ), t as (
      select right(regexp_replace(coalesce(j->>'patientMobile',''),'\D','','g'),10) as mob10,
             lower(trim(coalesce(j->>'patient',''))) as nm,
             trim(coalesce(j->>'patient','')) as nm_raw,
             fin.rmp_safe_number(j->>'amount') as amt,
             (lower(trim(coalesce(j->>'status',''))) = 'paid') as is_paid,
             left(coalesce(j->>'date',''),10) as d
      from e
    )
    select case when length(mob10) = 10 then 'm:' || mob10 else 'n:' || nm end as k,
           max(nm_raw) as nm_raw, max(mob10) as mob10,
           sum(case when is_paid then amt else 0 end) as lp,
           sum(case when is_paid then 0 else amt end) as ld,
           min(nullif(d,'')) as first_d
    from t group by 1
  loop
    if exists (select 1 from _rmp_led l where l.k = r.k) then
      update _rmp_led l set legacy_paid = l.legacy_paid + r.lp, legacy_due = l.legacy_due + r.ld where l.k = r.k;
    elsif exists (select 1 from _rmp_led l where l.source = 'AUTO' and lower(trim(coalesce(l.patient_name,''))) = lower(trim(coalesce(r.nm_raw,''))) and trim(coalesce(r.nm_raw,'')) <> '') then
      update _rmp_led l set legacy_paid = l.legacy_paid + r.lp, legacy_due = l.legacy_due + r.ld
       where l.source = 'AUTO' and lower(trim(coalesce(l.patient_name,''))) = lower(trim(coalesce(r.nm_raw,'')));
    else
      insert into _rmp_led(k, patient_row_id, patient_code, patient_name, patient_mobile, treatment_branch,
                           commission_mode, commission_value, set_on, legacy_paid, legacy_due, source)
      values (r.k, '', '', r.nm_raw, r.mob10, v_branch, 'LEGACY', null,
              case when r.first_d ~ '^\d{4}-\d{2}-\d{2}$' then r.first_d::date else null end,
              r.lp, r.ld, 'LEGACY');
    end if;
  end loop;

  -- (গ) Earned / রোগী-নির্দিষ্ট Paid
  update _rmp_led l set
    earned = round(coalesce(l.capped_amount, greatest(l.computed, l.legacy_paid + l.legacy_due)), 0),
    specific = l.given + l.legacy_paid;

  -- (ঘ) থোক টাকা (RMP-কে সরাসরি দেওয়া, কোনো রোগীর সাথে জোড়া নয়) + কারো বেশি-দেওয়া অংশ
  select coalesce(sum(greatest(0, a.amount - a.allocated_amount - a.legacy_covered_amount)), 0)
    into v_pool from fin.rmp_advance_payments a where a.rmp_id = p_rmp_id;
  select coalesce(sum(greatest(0, l.specific - l.earned)), 0) into v_excess from _rmp_led l;
  v_pool := v_pool + v_excess;

  -- (ঙ) পুরনো বাকি থেকে কাটা — set_on ক্রমে (পুরনো আগে)
  for r in select l.k, greatest(0, l.earned - l.specific) as remaining
             from _rmp_led l order by l.set_on asc nulls last, l.patient_name asc, l.k asc
  loop
    v_alloc := least(v_pool, r.remaining);
    v_pool := v_pool - v_alloc;
    update _rmp_led l set paid = least(l.specific, l.earned) + v_alloc, due = r.remaining - v_alloc where l.k = r.k;
  end loop;

  return query
    select l.patient_row_id, l.patient_code, l.patient_name, l.patient_mobile, l.treatment_branch,
           l.commission_mode, l.commission_value, l.set_on,
           round(l.net_paid,2), round(l.computed,2), round(l.legacy_paid,2), round(l.legacy_due,2),
           l.capped_amount, round(l.earned,2), round(l.paid,2), round(l.due,2), l.source
    from _rmp_led l
    order by l.due desc, l.set_on desc nulls last, l.patient_name;
end $$;

select d.name as rmp, d.branch, b.patient_name, b.patient_mobile, b.commission_mode, b.commission_value, b.set_on,
       b.net_paid, b.computed, b.legacy_paid, b.legacy_due, b.capped_amount, b.earned, b.paid, b.due, b.source
  from public.doctor_visits d
  cross join lateral pg_temp.rmp_bd_dbg(d.id) b
 where trim(coalesce(d.branch,'')) = 'Cooch Behar'
   and upper(trim(coalesce(d.name,''))) in ('PKB','PK','JAKIR HOSSAIN')
 order by d.name, b.due desc, b.patient_name;
