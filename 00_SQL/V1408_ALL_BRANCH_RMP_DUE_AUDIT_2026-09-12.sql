-- V1408 (১২.০৯.২০২৬ রাত, TK-নির্দেশ: "এই ধরনের ভুল সমস্ত ব্রাঞ্চের আর কোথায় কোথায় আছে
-- ভালো করে যাচাই করুন") — শুধু দেখার জন্য (read-only), কিছুই বদলায় না।
-- একটাই টেবিলে ৩টা অংশ (section ঘর দেখুন):
--   A_DUE      = সব ব্রাঞ্চে যে-যে রোগীর জন্য RMP-র টাকা বাকি দেখাচ্ছে — রোগীর রেজিস্ট্রেশনের
--                কতদিন পরে কমিশন-সারি বসেছে (gap_days) · হার · জমা · অর্জিত · দেওয়া · বাকি ·
--                origin (AUTOLINK = নিজে থেকে বাঁধা)। gap_days বড় (পুরনো রোগী) = FRANCIS SOREN-এর
--                মতো কেস — স্টাফকে জিজ্ঞাসা করে বন্ধ করার প্রার্থী।
--   B_OVERPAID = যে RMP-কে অর্জিত কমিশনের চেয়ে বেশি টাকা দেওয়া ধরা আছে।
--   C_DUP_RMP  = একই ব্রাঞ্চে একই নাম বা একই মোবাইলে একাধিক RMP রেকর্ড (JAKIR HOSSAIN-এর মতো)।
-- Run-এর পরে Export → CSV পাঠান।
create function pg_temp.rmp_bd(p_rmp_id text)
returns table(patient_row_id text, patient_code text, patient_name text, patient_mobile text,
              treatment_branch text, commission_mode text, commission_value numeric, set_on date,
              net_paid numeric, computed numeric, legacy_paid numeric, legacy_due numeric,
              capped_amount numeric, earned numeric, paid numeric, due numeric, source text)
language plpgsql as $$
declare v_branch text; v_pool numeric := 0;
begin
  select coalesce(x.branch,'') into v_branch from public.doctor_visits x where x.id = p_rmp_id;
  if not found then raise exception 'RMP not found'; end if;

  -- থোক টাকা (RMP-কে সরাসরি দেওয়া, কোনো রোগীর সাথে জোড়া নয়; legacy_covered বাদ)
  select coalesce(sum(greatest(0, a.amount - a.allocated_amount - a.legacy_covered_amount)), 0)
    into v_pool from fin.rmp_advance_payments a where a.rmp_id = p_rmp_id;

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
           (r.given + r.legacy_paid) as specific
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

with rmps as (
  select d.id, coalesce(d.name,'') as name, coalesce(d.mobile,'') as mobile, trim(coalesce(d.branch,'')) as branch
    from public.doctor_visits d
   where exists (select 1 from fin.rmp_patient_commissions c where c.rmp_id = d.id)
      or exists (select 1 from fin.rmp_advance_payments a where a.rmp_id = d.id)
      or (jsonb_typeof(d."referralPayments") = 'array' and jsonb_array_length(d."referralPayments") > 0)
), bd as (
  select r.id as rmp_id, r.name as rmp, r.mobile as rmp_mobile, r.branch, b.*
    from rmps r cross join lateral pg_temp.rmp_bd(r.id) b
), totals as (
  select x.rmp_id, sum(x.earned) as t_earned, sum(x.due) as t_due,
         (select coalesce(sum(y.amount),0) from fin.rmp_commission_payments y where y.rmp_id = x.rmp_id)
         + sum(x.legacy_paid)
         + (select coalesce(sum(greatest(0, a.amount - a.allocated_amount - a.legacy_covered_amount)),0)
              from fin.rmp_advance_payments a where a.rmp_id = x.rmp_id) as t_paid
    from bd x group by x.rmp_id
), a_due as (
  select 'A_DUE' as section, x.branch, x.rmp, x.rmp_mobile, x.patient_name as patient, x.patient_mobile,
         left(coalesce(nullif(p."registrationDate",''), p."date", ''),10) as registered,
         x.set_on::text as set_on,
         case when left(coalesce(nullif(p."registrationDate",''), p."date", ''),10) ~ '^\d{4}-\d{2}-\d{2}$' and x.set_on is not null
              then (x.set_on - left(coalesce(nullif(p."registrationDate",''), p."date", ''),10)::date)::text else '' end as gap_days,
         case when x.commission_mode = 'PERCENT' then x.commission_value::text || '%' else coalesce(x.commission_value::text, x.commission_mode) end as rate,
         x.net_paid, x.earned, x.paid, x.due,
         coalesce((select string_agg(distinct au.action, ',') from fin.rmp_commission_audit au
                    where au.entity_id in (c.id::text, x.patient_row_id) and au.action in ('AUTOLINK_REFDOCTOR','CAP_PATIENT_COMMISSION')), '') as origin,
         'RMP total due ' || round(t.t_due,0)::text as note
    from bd x
    left join public.patients p on p.id = x.patient_row_id
    left join fin.rmp_patient_commissions c on c.patient_row_id = x.patient_row_id and c.rmp_id = x.rmp_id
    join totals t on t.rmp_id = x.rmp_id
   where x.due > 0.5
), b_over as (
  select 'B_OVERPAID' as section, r.branch, r.name as rmp, r.mobile as rmp_mobile, '' as patient, '' as patient_mobile,
         '' as registered, '' as set_on, '' as gap_days, '' as rate,
         null::numeric as net_paid, round(t.t_earned,2) as earned, round(t.t_paid,2) as paid, 0::numeric as due, '' as origin,
         'paid more than earned by ' || round(t.t_paid - t.t_earned,0)::text as note
    from totals t join rmps r on r.id = t.rmp_id
   where t.t_paid - t.t_earned > 0.5
), dup as (
  select d.id, coalesce(d.name,'') as name, coalesce(d.mobile,'') as mobile, trim(coalesce(d.branch,'')) as branch,
         right(regexp_replace(coalesce(d.mobile,''),'\D','','g'),10) as m10, upper(trim(coalesce(d.name,''))) as nm
    from public.doctor_visits d
), c_dup as (
  select distinct 'C_DUP_RMP' as section, x.branch, x.name as rmp, x.mobile as rmp_mobile, '' as patient, '' as patient_mobile,
         '' as registered, '' as set_on, '' as gap_days, '' as rate,
         null::numeric as net_paid, null::numeric as earned, null::numeric as paid, null::numeric as due,
         (select count(*) from fin.rmp_patient_commissions c where c.rmp_id = x.id)::text || ' commission rows' as origin,
         case when exists (select 1 from dup y where y.id <> x.id and y.branch = x.branch and length(x.m10) = 10 and y.m10 = x.m10) then 'same mobile'
              else 'same name' end as note
    from dup x
   where exists (select 1 from dup y where y.id <> x.id and y.branch = x.branch
                  and ((length(x.m10) = 10 and y.m10 = x.m10) or (x.nm <> '' and y.nm = x.nm)))
)
select * from a_due
union all select * from b_over
union all select * from c_dup
order by 1, 2, 3, 15 desc nulls last, 5;
