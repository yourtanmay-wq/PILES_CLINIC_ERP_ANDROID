-- ═══════════════════════════════════════════════════════════════════════════
-- V1367 (১২.০৯.২০২৬) — TK-প্রশ্ন যাচাই: AMIT GOLDAR (কিশানগঞ্জ) — স্টাফের
-- কথায় রোগীরা মিলিয়ে ~₹29,000 জমা দিয়েছেন। অ্যাপে কমিশনের হিসাব সেটা
-- ধরছে কিনা।
-- ⛔ শুধু পড়া, কিছু বদলায় না। Supabase → SQL Editor → New query → Run।
-- ফল এক টেবিলেই, "section" কলাম ধরে বোঝা যাবে কোনটা কী।
-- ═══════════════════════════════════════════════════════════════════════════

-- (V941/V1356-এর ঘর ও ফাংশন — লাইভে আগে থেকেই আছে, এই লাইনগুলো শুধু
--  নিরাপদ নিশ্চয়তার জন্য — create/add-if-not-exists, তাই কিছু বদলায় না)
alter table fin.rmp_patient_commissions
  add column if not exists prev_mode       text,
  add column if not exists prev_value      numeric(12,2),
  add column if not exists rate_changed_on date;
create or replace function fin.rmp_net_paid_between(
  p_patient_row_id text, p_from date, p_to date)
returns numeric language sql stable security definer
set search_path = fin, public, hr as $$
  select greatest(0,
    coalesce((select sum(fin.rmp_safe_number(x."amount")) from public.payments x
       where x."patientId" = p_patient_row_id
         and fin.rmp_is_treatment(x."payType", x."remarks")
         and (p_from is null or left(coalesce(x."date",''),10) >= to_char(p_from,'YYYY-MM-DD'))
         and (p_to   is null or left(coalesce(x."date",''),10) <= to_char(p_to  ,'YYYY-MM-DD'))), 0)
  - coalesce((select sum(fin.rmp_safe_number(r."amount")) from public.payments r
       left join public.payments o on o.id = r."refundOfPaymentId"
      where r."patientId" = p_patient_row_id
        and lower(coalesce(r."payType",'')) = 'refund'
        and lower(coalesce(r."refundApprovalStatus",'')) = 'approved'
        and (trim(coalesce(r."refundOfPaymentId",'')) = ''
             or fin.rmp_is_treatment(o."payType", o."remarks"))
        and (p_from is null or left(coalesce(r."date",''),10) >= to_char(p_from,'YYYY-MM-DD'))
        and (p_to   is null or left(coalesce(r."date",''),10) <= to_char(p_to  ,'YYYY-MM-DD'))), 0)
  );
$$;
create or replace function fin.rmp_earned_for(
  p_patient_row_id text, p_bill numeric,
  p_mode text, p_value numeric,
  p_prev_mode text, p_prev_value numeric, p_changed_on date)
returns numeric language plpgsql stable security definer
set search_path = fin, public, hr as $$
declare v_before numeric; v_after numeric; v_eb numeric; v_ea numeric;
        v_pm text; v_pv numeric; v_mode text := upper(coalesce(p_mode,'PERCENT'));
begin
  v_pm := upper(coalesce(nullif(trim(coalesce(p_prev_mode,'')),''), p_mode, 'PERCENT'));
  v_pv := coalesce(p_prev_value, p_value, 0);
  if v_mode = 'PERCENT' and (p_changed_on is null or v_pm = 'PERCENT') then
    if p_changed_on is null then
      return fin.rmp_net_paid_between(p_patient_row_id, null, null) * coalesce(p_value,0) / 100;
    end if;
    v_before := fin.rmp_net_paid_between(p_patient_row_id, null, p_changed_on - 1);
    v_after  := fin.rmp_net_paid_between(p_patient_row_id, p_changed_on, null);
    return v_before * v_pv / 100 + v_after * coalesce(p_value,0) / 100;
  end if;
  if p_bill is null or p_bill <= 0 then return 0; end if;
  if p_changed_on is null then
    v_after := fin.rmp_net_paid_between(p_patient_row_id, null, null);
    v_ea := least(v_after, p_bill);
    if v_mode = 'PERCENT'
      then return v_ea * coalesce(p_value,0) / 100;
      else return coalesce(p_value,0) * v_ea / p_bill; end if;
  end if;
  v_before := fin.rmp_net_paid_between(p_patient_row_id, null, p_changed_on - 1);
  v_after  := fin.rmp_net_paid_between(p_patient_row_id, p_changed_on, null);
  v_eb := least(v_before, p_bill);
  v_ea := least(v_after, greatest(0, p_bill - v_eb));
  return (case when v_pm = 'PERCENT' then v_eb * v_pv / 100 else v_pv * v_eb / p_bill end)
       + (case when v_mode = 'PERCENT'
               then v_ea * coalesce(p_value,0) / 100
               else coalesce(p_value,0) * v_ea / p_bill end);
end $$;

with amit as (
  select id, name, mobile, branch, status,
         lower(trim(coalesce(name,''))) as nm,
         right(regexp_replace(coalesce(mobile,''),'[^0-9]','','g'),10) as mb
  from public.doctor_visits
  where lower(trim(coalesce(name,''))) like '%amit%goldar%'
),
-- A) তাঁর RMP-সারি (একাধিক থাকলে সবগুলো — জোড়া সারি সন্দেহ)
a_row as (
  select 'A · RMP ROW'::text as section, id as k1, name as k2, mobile as k3,
         branch as k4, status as k5, null::numeric as v1, null::numeric as v2, null::numeric as v3
  from amit
),
-- B) কমিশন-বাঁধা প্রতিটা রোগী — বিল/জমা/হার/অর্জিত/দেওয়া
b_bound as (
  select 'B · BOUND PATIENT'::text as section,
         c.patient_name as k1, c.patient_mobile as k2,
         (c.commission_mode || ' ' || c.commission_value) as k3, ''::text as k4, ''::text as k5,
         round(greatest(0, fin.rmp_safe_number(p."bill") - fin.rmp_safe_number(p."discount")),2) as v1,
         round(fin.rmp_net_paid_between(c.patient_row_id, null, null),2) as v2,
         round(fin.rmp_earned_for(c.patient_row_id,
           greatest(0, fin.rmp_safe_number(p."bill") - fin.rmp_safe_number(p."discount")),
           c.commission_mode, c.commission_value, c.prev_mode, c.prev_value, c.rate_changed_on),2) as v3
  from fin.rmp_patient_commissions c
  left join public.patients p on p.id = c.patient_row_id
  where c.rmp_id in (select id from amit)
),
-- C) refBy/refDoctor/refDoctorMobile-এ তাঁর নাম/নম্বর আছে এমন প্রতিটা রোগী,
--    কমিশন বাঁধা থাকুক বা না থাকুক — "বাঁধা নেই" মানেই তাঁর টাকা এই মুহূর্তে
--    RMP-র Due-তে ধরাই পড়ছে না (মূল সন্দেহ এখানেই)
c_matched as (
  select 'C · MATCHED (by name/mobile)'::text as section, p.name as k1, p.mobile as k2,
         case when exists(select 1 from fin.rmp_patient_commissions c where c.patient_row_id = p.id)
              then 'commission BOUND' else 'commission NOT BOUND ⚠' end as k3,
         coalesce(p."refBy",'') as k4, coalesce(p."refDoctor",'') as k5,
         round(greatest(0, fin.rmp_safe_number(p."bill") - fin.rmp_safe_number(p."discount")),2) as v1,
         round(fin.rmp_net_paid_between(p.id, null, null),2) as v2,
         null::numeric as v3
  from public.patients p, amit a
  where lower(trim(coalesce(p."refBy",''))) = a.nm
     or lower(trim(coalesce(p."refDoctor",''))) = a.nm
     or right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10) = a.mb
),
-- D) যোগফল — B (বাঁধা রোগীর জমা) বনাম C (নাম/নম্বর মিলিয়ে সব রোগীর জমা)
d_totals as (
  select 'D · TOTAL (bound patients only)'::text as section, ''::text as k1, ''::text as k2,
         ''::text as k3, ''::text as k4, ''::text as k5, null::numeric as v1,
         (select coalesce(sum(fin.rmp_net_paid_between(c.patient_row_id,null,null)),0)
            from fin.rmp_patient_commissions c where c.rmp_id in (select id from amit)) as v2,
         null::numeric as v3
  union all
  select 'D · TOTAL (matched by name/mobile, all)'::text, ''::text, ''::text,
         ''::text, ''::text, ''::text, null::numeric,
         (select coalesce(sum(fin.rmp_net_paid_between(p.id,null,null)),0)
            from public.patients p, amit a
            where lower(trim(coalesce(p."refBy",''))) = a.nm
               or lower(trim(coalesce(p."refDoctor",''))) = a.nm
               or right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10) = a.mb),
         null::numeric
)
select * from a_row
union all select * from b_bound
union all select * from c_matched
union all select * from d_totals
order by 1;
