-- ═══════════════════════════════════════════════════════════════════════════
-- V1083 (০৪.০৯.২০২৬) — RMP-র নামে চাপ দিলে কোন রোগীর জন্য কত, সেটা দেখা
--
-- TK: *"RMP-র নামের উপরে চাপ দিলে যেন বোঝা যায় এটা কোন পেশেন্টের জন্য
--       তাকে কত টাকা দেওয়া হলো, আর কত বিল ছিল, কত জমা করেছে আজকে"*
--
-- এই ফাইলে শুধু **একটা ঘর যোগ** — রোগীর `final_bill`। বাকি সব হিসাব ও
-- সংখ্যা হুবহু আগের (V1081-এর হার-বদল-সচেতন অঙ্ক অটুট)।
--
-- ⛔ ঘর যোগ করতে হলে ফাংশনটা আগে মুছে আবার বসাতে হয় (Postgres-এর নিয়ম) —
--    নিচে সেটাই করা হয়েছে। পুরনো অ্যাপ নতুন ঘরটা কেবল **উপেক্ষা** করে,
--    তাই ফোনে পুরনো APK থাকলেও কিছু ভাঙে না।
-- ⛔ কোনো রোগীর বিল/জমা/টাকা/কমিশনের হার — কিচ্ছু ছোঁয়া হয়নি।
-- ═══════════════════════════════════════════════════════════════════════════

drop function if exists fin.rmp_day_commission(text, text);

create or replace function fin.rmp_earned_upto(
  p_patient_row_id text, p_bill numeric,
  p_mode text, p_value numeric,
  p_prev_mode text, p_prev_value numeric, p_changed_on date, p_upto date)
returns numeric language plpgsql stable security definer
set search_path = fin, public, hr as $$
declare v_before numeric; v_after numeric; v_eb numeric; v_ea numeric;
        v_pm text; v_pv numeric;
begin
  if p_bill is null or p_bill <= 0 or p_upto is null then return 0; end if;

  -- ক) হার কখনো বদলায়নি ⇒ পুরোটাই এখনকার হারে
  if p_changed_on is null then
    v_ea := least(fin.rmp_net_paid_between(p_patient_row_id, null, p_upto), p_bill);
    if upper(coalesce(p_mode,'PERCENT')) = 'PERCENT'
      then return v_ea * coalesce(p_value,0) / 100;
      else return coalesce(p_value,0) * v_ea / p_bill; end if;
  end if;

  v_pm := upper(coalesce(nullif(trim(coalesce(p_prev_mode,'')),''), p_mode, 'PERCENT'));
  v_pv := coalesce(p_prev_value, p_value, 0);

  -- খ) হিসাবের দিনটা হার-বদলের আগে ⇒ পুরোটাই পুরনো হারে
  if p_upto < p_changed_on then
    v_eb := least(fin.rmp_net_paid_between(p_patient_row_id, null, p_upto), p_bill);
    if v_pm = 'PERCENT' then return v_eb * v_pv / 100;
                        else return v_pv * v_eb / p_bill; end if;
  end if;

  -- গ) দুই ভাগে: বদলের আগের জমা পুরনো হারে, পরের জমা নতুন হারে
  v_before := fin.rmp_net_paid_between(p_patient_row_id, null, p_changed_on - 1);
  v_after  := fin.rmp_net_paid_between(p_patient_row_id, p_changed_on, p_upto);
  v_eb := least(v_before, p_bill);
  v_ea := least(v_after, greatest(0, p_bill - v_eb));

  return (case when v_pm = 'PERCENT' then v_eb * v_pv / 100 else v_pv * v_eb / p_bill end)
       + (case when upper(coalesce(p_mode,'PERCENT')) = 'PERCENT'
               then v_ea * coalesce(p_value,0) / 100
               else coalesce(p_value,0) * v_ea / p_bill end);
end $$;

revoke all on function fin.rmp_earned_upto(text,numeric,text,numeric,text,numeric,date,date) from public, anon;
grant execute on function fin.rmp_earned_upto(text,numeric,text,numeric,text,numeric,date,date) to authenticated;

-- ── ধাপ ২: দিনের কমিশনও ঠিক ওই একই হিসাবে ────────────────────────────────
--    আজ পর্যন্ত অর্জিত − গতকাল পর্যন্ত অর্জিত = আজকের অংশ (V426-এর একই ধারণা,
--    শুধু অঙ্কটা এখন হার-বদল-সচেতন)।
create or replace function fin.rmp_day_commission(p_branch text, p_date text)
returns table(
  patient_row_id text, patient_mobile text, patient_code text, patient_name text,
  rmp_id text, rmp_name text, rmp_mobile text,
  paid_today numeric, commission_today numeric,
  final_bill numeric)                      -- 🔵 V1083 — রোগীর মোট বিল
language plpgsql stable security definer set search_path = fin, public, hr as $$
declare
  v_branch text := nullif(trim(coalesce(p_branch,'')),'');
  v_date   date;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  if v_branch is null or v_branch not in ('Kishanganj','Jalpaiguri','Cooch Behar','Falakata','Birpara') then
    raise exception 'Invalid branch';
  end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this branch'; end if;
  if p_date is null or p_date !~ '^\d{4}-\d{2}-\d{2}$' then raise exception 'Invalid date'; end if;
  v_date := p_date::date;

  return query
  with base as (
    select c.patient_row_id as prid, c.rmp_id as rid, c.rmp_name as rnm, c.rmp_mobile as rmb,
           c.commission_mode as cm, c.commission_value as cv,
           c.prev_mode as pm, c.prev_value as pv, c.rate_changed_on as rc,
           greatest(0, fin.rmp_safe_number(p."bill") - fin.rmp_safe_number(p."discount")) as bill,
           coalesce(p."mobile",'')    as pmob,
           coalesce(p."patientId",'') as pcode,
           coalesce(p."name",'')      as pnm
      from fin.rmp_patient_commissions c
      left join public.patients p on p.id = c.patient_row_id
     where c.treatment_branch = v_branch
  ), calc as (
    select b.*,
           fin.rmp_net_paid_between(b.prid, v_date, v_date) as ptoday,
           fin.rmp_earned_upto(b.prid, b.bill, b.cm, b.cv, b.pm, b.pv, b.rc, v_date)
         - fin.rmp_earned_upto(b.prid, b.bill, b.cm, b.cv, b.pm, b.pv, b.rc, v_date - 1) as ctoday
      from base b
  )
  select c.prid, c.pmob, c.pcode, c.pnm, c.rid, c.rnm, c.rmb,
         round(c.ptoday, 2), round(greatest(0, c.ctoday), 2),
         round(c.bill, 2)
    from calc c
   where c.ptoday <> 0 or c.ctoday <> 0
   order by c.rnm, c.pnm;
end $$;

revoke all on function fin.rmp_day_commission(text,text) from public, anon;
grant execute on function fin.rmp_day_commission(text,text) to authenticated;


revoke all on function fin.rmp_day_commission(text,text) from public, anon;
grant execute on function fin.rmp_day_commission(text,text) to authenticated;

notify pgrst, 'reload schema';
