-- ═══════════════════════════════════════════════════════════════════════════
-- V941 (০১.০৯.২০২৬) — কোচবিহারের দুই RMP-র কমিশন আজ থেকে ৪০%
--
-- TK-এর নির্দেশ:
--   · +91 7479173399 (J.H MANDAL) ও +91 8001080080 (TK BISWAS) —
--     **শুধুমাত্র কোচবিহারের জন্য** আজ ০১.০৯.২০২৬ থেকে ৪০%।
--   · আগে যত রোগী পাঠিয়েছেন, তাঁদের **ইতিমধ্যে জমা হওয়া টাকায় পুরনো হারই**
--     থাকবে — এক পয়সাও নড়বে না।
--   · ওই রোগীরাই এরপর যত টাকা দেবেন, সেই টাকায় ৪০%।
--   · অন্য ব্রাঞ্চের রোগী/হিসাব কিচ্ছু বদলাবে না।
--
-- 🛡️ নিরাপত্তা: নতুন হিসাবটা **শুধু তখনই** চলে যখন ওই রোগীর সারিতে
--    `rate_changed_on` বসানো থাকে। বাকি সব রোগীর ক্ষেত্রে অঙ্ক **হুবহু আগের**।
-- ═══════════════════════════════════════════════════════════════════════════

-- ── ধাপ ১: হার-বদলের স্মৃতি রাখার তিনটে ঘর (থাকলে আর বসে না) ──────────────
alter table fin.rmp_patient_commissions
  add column if not exists prev_mode       text,
  add column if not exists prev_value      numeric(12,2),
  add column if not exists rate_changed_on date;

-- ── ধাপ ২: একটা তারিখ-সীমার ভিতরে ওই রোগীর নিট চিকিৎসা-জমা ────────────────
--    (রিফান্ড বাদ দেওয়ার নিয়ম হুবহু আগের `rmp_summary`-র মতোই)
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

-- ── ধাপ ৩: কমিশন গোনার **একটাই** নিয়ম (দুটো পর্দাই এটাই ডাকবে) ───────────
--    rate_changed_on ফাঁকা  ⇒ হুবহু আগের অঙ্ক।
--    বসানো থাকলে ⇒ ওই তারিখের আগের জমা পুরনো হারে, ওই তারিখ ও পরের জমা নতুন হারে।
--    বিলের বেশি টাকায় কখনো কমিশন হয় না (আগের জমা আগে গোনা হয়)।
create or replace function fin.rmp_earned_for(
  p_patient_row_id text, p_bill numeric,
  p_mode text, p_value numeric,
  p_prev_mode text, p_prev_value numeric, p_changed_on date)
returns numeric language plpgsql stable security definer
set search_path = fin, public, hr as $$
declare v_before numeric; v_after numeric; v_eb numeric; v_ea numeric;
        v_pm text; v_pv numeric;
begin
  if p_bill is null or p_bill <= 0 then return 0; end if;

  if p_changed_on is null then
    v_after := fin.rmp_net_paid_between(p_patient_row_id, null, null);
    v_ea := least(v_after, p_bill);
    if upper(coalesce(p_mode,'PERCENT')) = 'PERCENT'
      then return v_ea * coalesce(p_value,0) / 100;
      else return coalesce(p_value,0) * v_ea / p_bill; end if;
  end if;

  v_pm := upper(coalesce(nullif(trim(coalesce(p_prev_mode,'')),''), p_mode, 'PERCENT'));
  v_pv := coalesce(p_prev_value, p_value, 0);

  v_before := fin.rmp_net_paid_between(p_patient_row_id, null, p_changed_on - 1);
  v_after  := fin.rmp_net_paid_between(p_patient_row_id, p_changed_on, null);
  v_eb := least(v_before, p_bill);
  v_ea := least(v_after, greatest(0, p_bill - v_eb));

  return (case when v_pm = 'PERCENT' then v_eb * v_pv / 100 else v_pv * v_eb / p_bill end)
       + (case when upper(coalesce(p_mode,'PERCENT')) = 'PERCENT'
               then v_ea * coalesce(p_value,0) / 100
               else coalesce(p_value,0) * v_ea / p_bill end);
end $$;

-- ── ধাপ ৪: রোগীর কার্ডের হিসাব — শুধু "earned" লাইনটা নতুন নিয়মে ──────────
create or replace function fin.rmp_summary(p_patient_row_id text)
returns table(final_bill numeric, net_treatment_paid numeric, earned numeric,
              paid numeric, due numeric, overpaid numeric)
language plpgsql security definer set search_path = fin, public, hr as $$
declare
  c fin.rmp_patient_commissions%rowtype;
  v_bill numeric := 0; v_paid numeric := 0; v_earned numeric := 0; v_given numeric := 0;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  select * into c from fin.rmp_patient_commissions where patient_row_id=p_patient_row_id;
  if not found then return query select 0::numeric,0::numeric,0::numeric,0::numeric,0::numeric,0::numeric; return; end if;
  if not fin.rmp_can_write_branch(c.treatment_branch) then raise exception 'Not allowed for this patient branch'; end if;

  select greatest(0,fin.rmp_safe_number(p."bill")-fin.rmp_safe_number(p."discount"))
    into v_bill from public.patients p where p.id=p_patient_row_id;

  v_paid := fin.rmp_net_paid_between(p_patient_row_id, null, null);
  v_earned := fin.rmp_earned_for(p_patient_row_id, v_bill, c.commission_mode, c.commission_value,
                                 c.prev_mode, c.prev_value, c.rate_changed_on);

  select coalesce(sum(x.amount),0) into v_given from fin.rmp_commission_payments x
   where x.patient_commission_id=c.id;
  return query select round(v_bill,2),round(v_paid,2),round(v_earned,2),round(v_given,2),
    round(greatest(v_earned-v_given,0),2),round(greatest(v_given-v_earned,0),2);
end $$;

-- ── ধাপ ৫: RMP Due List-ও ঠিক সেই একই নিয়মে (নইলে দুই পর্দা আলাদা বলত) ────
create or replace function fin.rmp_branch_due(p_branch text)
returns table(rmp_id text, rmp_name text, rmp_mobile text, branch text,
              patient_count bigint, earned numeric, paid numeric, due numeric)
language plpgsql stable security definer set search_path = fin, public, hr as $$
declare v_branch text := nullif(trim(coalesce(p_branch,'')),'');
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  if v_branch is null or v_branch not in ('Kishanganj','Jalpaiguri','Cooch Behar','Falakata','Birpara') then
    raise exception 'Invalid branch';
  end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this branch'; end if;

  return query
  with base as (
    select c.id, c.rmp_id, c.rmp_name, c.rmp_mobile, c.treatment_branch, c.patient_row_id,
           c.commission_mode, c.commission_value, c.prev_mode, c.prev_value, c.rate_changed_on,
           greatest(0, fin.rmp_safe_number(p."bill") - fin.rmp_safe_number(p."discount")) as bill
    from fin.rmp_patient_commissions c
    left join public.patients p on p.id = c.patient_row_id
    where c.treatment_branch = v_branch
  ),
  earn as (
    select b.*, fin.rmp_earned_for(b.patient_row_id, b.bill, b.commission_mode, b.commission_value,
                                   b.prev_mode, b.prev_value, b.rate_changed_on) as earned
    from base b
  ),
  given as (select x.patient_commission_id, sum(x.amount) g
              from fin.rmp_commission_payments x group by 1),
  per_row as (
    select e.*, coalesce(g.g,0) as given, greatest(e.earned - coalesce(g.g,0), 0) as due
    from earn e left join given g on g.patient_commission_id = e.id
  ),
  per_rmp as (
    select r.rmp_id as rid, max(r.rmp_name) nm, max(r.rmp_mobile) mb, max(r.treatment_branch) br,
           count(*) pts, round(sum(r.earned),2) ea, round(sum(r.given),2) gi, round(sum(r.due),2) du
    from per_row r group by r.rmp_id
  ),
  adv as (select a.rmp_id as aid, coalesce(sum(a.amount - a.allocated_amount),0) un
            from fin.rmp_advance_payments a group by 1)
  select q.rid, q.nm, q.mb, q.br, q.pts, q.ea,
         round(q.gi + coalesce(v.un,0), 2),
         round(greatest(0, q.du - coalesce(v.un,0)), 2)
  from per_rmp q left join adv v on v.aid = q.rid
  where round(greatest(0, q.du - coalesce(v.un,0)), 2) > 0
  order by 8 desc;
end $$;

-- ── ধাপ ৬: শুধু কোচবিহারের ওই দুই RMP-র পুরনো রোগীদের হার বদল ─────────────
--    (পুরনো হার আগে জমা রাখা হয়, তারপর ৪০% বসে — আজ থেকে কার্যকর)
with target as (
  select d.id as rmp_id
  from public.doctor_visits d
  where trim(coalesce(d.branch,'')) = 'Cooch Behar'
    and (replace(coalesce(d.mobile,''),' ','') like '%7479173399%'
      or replace(coalesce(d.mobile,''),' ','') like '%8001080080%')
)
update fin.rmp_patient_commissions c
   set prev_mode       = coalesce(c.prev_mode, c.commission_mode),
       prev_value      = coalesce(c.prev_value, c.commission_value),
       rate_changed_on = coalesce(c.rate_changed_on, date '2026-09-01'),
       commission_mode = 'PERCENT',
       commission_value = 40,
       updated_at = now()
  from target t
 where c.rmp_id = t.rmp_id
   and c.treatment_branch = 'Cooch Behar';

-- ── ধাপ ৭: নতুন রোগীর জন্য ডিফল্ট ৪০% (ওই দুই RMP, কোচবিহার) ──────────────
insert into fin.rmp_commission_defaults(rmp_id, rmp_name, rmp_mobile, commission_mode, commission_value, updated_by)
select d.id, coalesce(d.name,''), coalesce(d.mobile,''), 'PERCENT', 40, 'V941'
from public.doctor_visits d
where trim(coalesce(d.branch,'')) = 'Cooch Behar'
  and (replace(coalesce(d.mobile,''),' ','') like '%7479173399%'
    or replace(coalesce(d.mobile,''),' ','') like '%8001080080%')
on conflict (rmp_id) do update
  set commission_mode = 'PERCENT', commission_value = 40,
      effective_from = date '2026-09-01', updated_by = 'V941', updated_at = now();

notify pgrst, 'reload schema';
