-- ============================================================================
-- V426 — এক দিনের RMP কমিশন এক ডাকে (Chamber Review পর্দার জন্য)
--   TK-নির্দেশ ১৭.০৮.২০২৬: *"Review পর্দাতে যদি কোন আরএমপির পেশেন্ট হয়ে থাকে
--   তাহলে তার কমিশন এখানে মেনশন করতে হবে · এবং কমিশন বাদ দিয়ে সর্বমোট টাকার
--   পরিমান থেকে কমে যাবে · … ডিফল্ট পারসেন্ট হিসাবে অটোমেটিক মেনশন হয়ে যাবে,
--   কোন আরএমপির কত পেশেন্টের কত টাকা"*
--
--   TK-এর অনুমোদিত সিদ্ধান্ত (কাজ শুরুর আগে জিজ্ঞাসা করা হয়েছিল):
--     • কমিশন গোনা হবে **অ্যাপের পুরনো নিয়মেই** (Registration/Visit Fee ও
--       Medicine বাদ · Final Bill-এর বেশি নয়) — তাই RMP পর্দার হিসাবের সঙ্গে
--       কখনো গরমিল হবে না।
--     • **শুধু আজকের কমিশন** দেখাবে ও TOTAL থেকে বাদ যাবে।
--     • **পাঁচ ব্রাঞ্চেই** এই নিয়ম।
--
--   ⛔ শুধু পড়ে (stable) — একটাও সারি লেখা/বদলানো হয় না।
--   ⛔ পাহারা হুবহু অন্য RMP-ফাংশনের মতো: fin.rmp_can_use() + ব্রাঞ্চ-পরীক্ষা।
--   ⛔ হিসাবের সূত্র fin.rmp_branch_due() (V411) থেকে **হুবহু নকল** — নতুন কোনো
--      নিয়ম বানানো হয়নি, শুধু "আজকের অংশটুকু" আলাদা করা হয়েছে:
--          আজকের কমিশন = (আজ পর্যন্ত অর্জিত) − (গতকাল পর্যন্ত অর্জিত)
--      এভাবে করায় Final Bill-এর সীমাটাও ঠিক ঠিক মানা হয়।
--   ⛔ Egress: রোগীপ্রতি আলাদা ডাক নয় — **এক ডাকেই** ওই দিনের পুরো তালিকা।
-- ============================================================================

create or replace function fin.rmp_day_commission(p_branch text, p_date text)
returns table(
  patient_row_id text,
  patient_mobile text,
  patient_code   text,
  patient_name   text,
  rmp_id         text,
  rmp_name       text,
  rmp_mobile     text,
  paid_today     numeric,   -- আজ ওই রোগীর চিকিৎসার নিট জমা
  commission_today numeric  -- আজকের অংশের কমিশন
)
language plpgsql stable security definer set search_path = fin, public, hr as $$
declare
  v_branch text := nullif(trim(coalesce(p_branch,'')),'');
  v_date   text := nullif(trim(coalesce(p_date,'')),'');
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  if v_branch is null or v_branch not in ('Kishanganj','Jalpaiguri','Cooch Behar','Falakata','Birpara') then
    raise exception 'Invalid branch';
  end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this branch'; end if;
  if v_date is null or v_date !~ '^\d{4}-\d{2}-\d{2}$' then raise exception 'Invalid date'; end if;

  return query
  with base as (
    -- ওই ব্রাঞ্চের RMP-বাঁধা রোগী, তার বিল (ছাড় বাদে) — V411-এর হুবহু একই
    select c.id, c.rmp_id, c.rmp_name, c.rmp_mobile, c.patient_row_id,
           c.commission_mode, c.commission_value,
           greatest(0, fin.rmp_safe_number(p."bill") - fin.rmp_safe_number(p."discount")) as bill,
           coalesce(p."mobile", '')    as pmobile,
           coalesce(p."patientId", '') as pcode,
           coalesce(p."name", '')      as pname
    from fin.rmp_patient_commissions c
    left join public.patients p on p.id = c.patient_row_id
    where c.treatment_branch = v_branch
  ),
  -- ছাঁকনি: শুধু সেই রোগীরা, যাঁদের **আজ** চিকিৎসার টাকা জমা পড়েছে
  today_rows as (
    select b.id,
      coalesce((select sum(fin.rmp_safe_number(x."amount")) from public.payments x
         where x."patientId" = b.patient_row_id
           and x."date" = v_date
           and fin.rmp_is_treatment(x."payType", x."remarks")), 0)
    - coalesce((select sum(fin.rmp_safe_number(r."amount")) from public.payments r
         left join public.payments o on o.id = r."refundOfPaymentId"
        where r."patientId" = b.patient_row_id
          and r."date" = v_date
          and lower(coalesce(r."payType",'')) = 'refund'
          and lower(coalesce(r."refundApprovalStatus",'')) = 'approved'
          and (trim(coalesce(r."refundOfPaymentId",'')) = ''
               or fin.rmp_is_treatment(o."payType", o."remarks"))), 0) as paid_today
    from base b
  ),
  -- আজ পর্যন্ত (আজকের দিনসহ) নিট চিকিৎসা-জমা
  upto_today as (
    select b.id,
      coalesce((select sum(fin.rmp_safe_number(x."amount")) from public.payments x
         where x."patientId" = b.patient_row_id
           and x."date" <= v_date
           and fin.rmp_is_treatment(x."payType", x."remarks")), 0)
    - coalesce((select sum(fin.rmp_safe_number(r."amount")) from public.payments r
         left join public.payments o on o.id = r."refundOfPaymentId"
        where r."patientId" = b.patient_row_id
          and r."date" <= v_date
          and lower(coalesce(r."payType",'')) = 'refund'
          and lower(coalesce(r."refundApprovalStatus",'')) = 'approved'
          and (trim(coalesce(r."refundOfPaymentId",'')) = ''
               or fin.rmp_is_treatment(o."payType", o."remarks"))), 0) as net_paid
    from base b
  ),
  -- গতকাল পর্যন্ত নিট চিকিৎসা-জমা
  upto_prev as (
    select b.id,
      coalesce((select sum(fin.rmp_safe_number(x."amount")) from public.payments x
         where x."patientId" = b.patient_row_id
           and x."date" < v_date
           and fin.rmp_is_treatment(x."payType", x."remarks")), 0)
    - coalesce((select sum(fin.rmp_safe_number(r."amount")) from public.payments r
         left join public.payments o on o.id = r."refundOfPaymentId"
        where r."patientId" = b.patient_row_id
          and r."date" < v_date
          and lower(coalesce(r."payType",'')) = 'refund'
          and lower(coalesce(r."refundApprovalStatus",'')) = 'approved'
          and (trim(coalesce(r."refundOfPaymentId",'')) = ''
               or fin.rmp_is_treatment(o."payType", o."remarks"))), 0) as net_paid
    from base b
  ),
  earn as (
    select b.*, t.paid_today,
      -- V411-এর হুবহু একই সূত্র, দুই সময়ের জন্য আলাদা করে
      case when b.bill > 0 then
        case when b.commission_mode = 'PERCENT'
          then least(greatest(0, u.net_paid), b.bill) * b.commission_value / 100
          else b.commission_value * least(greatest(0, u.net_paid), b.bill) / b.bill end
      else 0 end
      -
      case when b.bill > 0 then
        case when b.commission_mode = 'PERCENT'
          then least(greatest(0, v.net_paid), b.bill) * b.commission_value / 100
          else b.commission_value * least(greatest(0, v.net_paid), b.bill) / b.bill end
      else 0 end as comm_today
    from base b
    join today_rows t on t.id = b.id
    join upto_today  u on u.id = b.id
    join upto_prev   v on v.id = b.id
  )
  select e.patient_row_id, e.pmobile, e.pcode, e.pname,
         e.rmp_id, e.rmp_name, e.rmp_mobile,
         round(e.paid_today, 2),
         round(greatest(0, e.comm_today), 2)
  from earn e
  where e.paid_today <> 0 or e.comm_today <> 0
  order by e.rmp_name, e.pname;
end $$;

revoke all on function fin.rmp_day_commission(text, text) from public, anon;
grant execute on function fin.rmp_day_commission(text, text) to authenticated;
notify pgrst, 'reload schema';

-- ── মিলিয়ে দেখা (শুধু পড়া) — আজকের কোচবিহার ─────────────────────────────────
-- select * from fin.rmp_day_commission('Cooch Behar', to_char((now() at time zone 'Asia/Kolkata')::date,'YYYY-MM-DD'));
