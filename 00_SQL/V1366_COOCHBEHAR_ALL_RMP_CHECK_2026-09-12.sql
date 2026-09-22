-- ═══════════════════════════════════════════════════════════════════════════
-- V1366 (১২.০৯.২০২৬) — TK-প্রশ্ন যাচাই: "কোচবিহারে TK BISWAS-এর ₹400 ছাড়া
-- আর কারো বাকি নেই" — অ্যাপ কী দেখাচ্ছে? (V1356-এর পরে বিল-না-থাকা রোগীর
-- কমিশনও এখন গোনা হয়, তাই কারো বাকি বাড়তেই পারে — এই ফলে বোঝা যাবে সেটাই
-- ঘটেছে, নাকি সত্যিই ভুল)
-- ⛔ শুধু পড়া, কিছু বদলায় না। Supabase → SQL Editor → New query → Run।
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

select r.id as rmp_id, r.name, r.mobile, r.branch,
       round(s.earned,2) as earned, round(s.paid,2) as paid, round(s.due,2) as due
from public.doctor_visits r
cross join lateral (
  select coalesce(sum(x.earned),0) as earned, coalesce(sum(x.given),0) as paid,
         coalesce(sum(greatest(x.earned - x.given,0)),0) as due
  from (
    select fin.rmp_earned_for(c.patient_row_id,
             greatest(0, fin.rmp_safe_number(p."bill") - fin.rmp_safe_number(p."discount")),
             c.commission_mode, c.commission_value, c.prev_mode, c.prev_value, c.rate_changed_on) as earned,
           coalesce((select sum(g.amount) from fin.rmp_commission_payments g where g.patient_commission_id = c.id),0) as given
    from fin.rmp_patient_commissions c
    left join public.patients p on p.id = c.patient_row_id
    where c.rmp_id = r.id
  ) x
) s
where trim(coalesce(r.branch,'')) = 'Cooch Behar'
order by s.due desc, r.name;
