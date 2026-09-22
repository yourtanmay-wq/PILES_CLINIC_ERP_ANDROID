-- ⚠️ V1406 দেখুন: এই ফাইলের rmp_patient_breakdown/rmp_rmp_summary/rmp_branch_due অস্থায়ী টেবিল ব্যবহার করত —
--    অ্যাপ-পথে (authenticated + security definer) সেটা আটকায় (১২.০৯.২০২৬ রাত, তালিকা ৫১২)। V1406 সেগুলো বদলে দিয়েছে;
--    ধাপ ১ (capped_* ঘর) ও ধাপ ৬ (rmp_cap_patient) এখনো এই ফাইলেরটাই চালু।
-- ═══════════════════════════════════════════════════════════════════════════
-- 💰🔒 V1404 (১২.০৯.২০২৬ রাত, TK-নির্দেশ ও ডেমো-প্রুফ পাশ — তালিকা সারি ৫০৯-৫১১)
-- RMP-র টাকার **একটাই খাতা**।
--
-- আগে দুটো খাতা চলত: (১) হাতে-লেখা "Referral Income" এন্ট্রি (doctor_visits.referralPayments)
-- (২) অটো-কমিশন (fin.rmp_patient_commissions — বিল/জমা × %)। প্রোফাইল দুটো যোগ করত,
-- Due List/পপ-আপ শুধু (২) দেখত ⇒ এক পর্দায় ₹46,501, অন্যটায় ₹37,700; TAPOSHI-র ₹8,801
-- এক খাতায় দেওয়া, অন্য খাতায় বাকি; PK-র ₹4 পয়সার টুকরো।
--
-- এখন থেকে একটাই নিয়ম (ফোন · কম্পিউটার · Due List · পপ-আপ — সবাই এই ফাংশনই ডাকে):
--   · প্রতিটা রোগীর Earned = অটো-কমিশন (পুরো টাকায় গোল) আর হাতে-লেখা মোটের (Paid+Due)
--     মধ্যে যেটা বেশি; Master "এখানেই বন্ধ" করলে সেই স্থির অঙ্কটাই (capped_amount)।
--   · Paid = রোগী-ধরে দেওয়া টাকা (fin.rmp_commission_payments) + হাতে-লেখা Paid এন্ট্রি
--     + RMP-কে সরাসরি দেওয়া টাকা (advance)-র যে অংশ কোনো রোগীর সাথে জোড়া নেই
--     (legacy_covered_amount বাদ — একই টাকা দুবার নয়)।
--   · সরাসরি দেওয়া টাকা নিজে থেকে **সবচেয়ে পুরনো বাকি** থেকে কাটে (set_on ক্রমে);
--     হাতে "Select Old Patient" আর লাগে না।
--   · Due = Earned − Paid (০-এর নিচে নয়); বেশি দেওয়া থাকলে Overpaid।
--
-- ⛔ কোনো টেবিলের একটাও সারি মোছা/বদলানো হয় না — শুধু ৩টা নতুন ঘর (capped_*) আর
--    হিসাবের ফাংশন। পুরনো ফাংশনের নাম/ফেরত একই, তাই পুরনো অ্যাপও চলবে (শুধু নতুন
--    নিয়মের অঙ্ক দেখবে)। ফাংশনগুলো আগের মতোই পাহারা-দেওয়া (fin.rmp_can_use + ব্রাঞ্চ)।
-- ═══════════════════════════════════════════════════════════════════════════
begin;

-- ── ধাপ ১: "এখানেই বন্ধ" রাখার ঘর ─────────────────────────────────────────
alter table fin.rmp_patient_commissions
  add column if not exists capped_amount numeric(12,2),
  add column if not exists capped_by text,
  add column if not exists capped_at timestamptz;

-- ── ধাপ ২: একটাই হিসাব — রোগী ধরে (ভাঙা হিসাবের পর্দা এটাই দেখায়) ────────────
create or replace function fin.rmp_patient_breakdown(p_rmp_id text)
returns table(patient_row_id text, patient_code text, patient_name text, patient_mobile text,
              treatment_branch text, commission_mode text, commission_value numeric, set_on date,
              net_paid numeric, computed numeric, legacy_paid numeric, legacy_due numeric,
              capped_amount numeric, earned numeric, paid numeric, due numeric, source text)
language plpgsql security definer set search_path = fin, public, hr as $$
#variable_conflict use_column
declare v_branch text; v_pool numeric := 0; v_excess numeric := 0; r record; v_alloc numeric;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  select coalesce(x.branch,'') into v_branch from public.doctor_visits x where x.id = p_rmp_id;
  if not found then raise exception 'RMP not found'; end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this RMP branch'; end if;
  -- V489/V815: পুরনো রেফার করা রোগীর কমিশন-সারি না থাকলে একবার তৈরি (ব্যর্থ হলেও হিসাব থামে না)
  begin perform fin.rmp_backfill_commissions(p_rmp_id); exception when others then null; end;

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
revoke all on function fin.rmp_patient_breakdown(text) from public, anon;
grant execute on function fin.rmp_patient_breakdown(text) to authenticated;

-- ── ধাপ ৩: RMP-র মোট — একই নাম, একই ফেরত, শুধু নিয়ম এক ──────────────────────
create or replace function fin.rmp_rmp_summary(p_rmp_id text)
returns table(patient_count bigint, earned numeric, paid_to_this_rmp numeric,
              previous_rmp_paid numeric, due numeric, overpaid numeric)
language plpgsql security definer set search_path = fin, public, hr as $$
declare v_count bigint := 0; v_earned numeric := 0; v_paid numeric := 0; v_previous numeric := 0;
        v_pool numeric := 0; v_legacy_paid numeric := 0; v_given numeric := 0;
begin
  select count(*), coalesce(sum(b.earned),0), coalesce(sum(b.legacy_paid),0)
    into v_count, v_earned, v_legacy_paid
    from fin.rmp_patient_breakdown(p_rmp_id) b;
  -- Paid (মোট) = রোগী-ধরে দেওয়া + হাতে-লেখা Paid + থোকের যে অংশ কোনো রোগীর সাথে জোড়া নয়
  --   (ভাঙা হিসাবের রোগী-ধরে paid-ও ঠিক এই একই টাকা থেকেই বসে — দুটো কখনো আলাদা হয় না)
  select coalesce(sum(x.amount),0) into v_given from fin.rmp_commission_payments x where x.rmp_id = p_rmp_id;
  select coalesce(sum(greatest(0, a.amount - a.allocated_amount - a.legacy_covered_amount)), 0)
    into v_pool from fin.rmp_advance_payments a where a.rmp_id = p_rmp_id;
  v_paid := v_given + v_legacy_paid + v_pool;
  select coalesce(sum(x.amount),0) into v_previous
    from fin.rmp_commission_payments x join fin.rmp_patient_commissions pc on pc.id = x.patient_commission_id
   where pc.rmp_id = p_rmp_id and x.rmp_id <> p_rmp_id;
  return query select v_count, round(v_earned,2), round(v_paid,2), round(v_previous,2),
                      round(greatest(0, v_earned - v_paid),2), round(greatest(0, v_paid - v_earned),2);
end $$;
revoke all on function fin.rmp_rmp_summary(text) from public, anon;
grant execute on function fin.rmp_rmp_summary(text) to authenticated;

-- ── ধাপ ৪: একজন রোগীর হিসাব — একই খাতা থেকে ─────────────────────────────────
create or replace function fin.rmp_summary(p_patient_row_id text)
returns table(final_bill numeric, net_treatment_paid numeric, earned numeric,
              paid numeric, due numeric, overpaid numeric)
language plpgsql security definer set search_path = fin, public, hr as $$
declare c fin.rmp_patient_commissions%rowtype; v_bill numeric := 0; b record;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  select * into c from fin.rmp_patient_commissions where patient_row_id = p_patient_row_id;
  if not found then return query select 0::numeric,0::numeric,0::numeric,0::numeric,0::numeric,0::numeric; return; end if;
  if not fin.rmp_can_write_branch(c.treatment_branch) then raise exception 'Not allowed for this patient branch'; end if;
  select greatest(0, fin.rmp_safe_number(p."bill") - fin.rmp_safe_number(p."discount"))
    into v_bill from public.patients p where p.id = p_patient_row_id;
  select * into b from fin.rmp_patient_breakdown(c.rmp_id) x where x.patient_row_id = p_patient_row_id limit 1;
  if not found then return query select round(coalesce(v_bill,0),2),0::numeric,0::numeric,0::numeric,0::numeric,0::numeric; return; end if;
  return query select round(coalesce(v_bill,0),2), round(b.net_paid,2), round(b.earned,2), round(b.paid,2),
                      round(b.due,2), round(greatest(0, b.paid - b.earned),2);
end $$;
revoke all on function fin.rmp_summary(text) from public, anon;
grant execute on function fin.rmp_summary(text) to authenticated;

-- ── ধাপ ৫: RMP Due List — একই মোট ─────────────────────────────────────────────
create or replace function fin.rmp_branch_due(p_branch text)
returns table(rmp_id text, rmp_name text, rmp_mobile text, branch text,
              patient_count bigint, earned numeric, paid numeric, due numeric)
language plpgsql security definer set search_path = fin, public, hr as $$
#variable_conflict use_column
declare v_branch text := nullif(trim(coalesce(p_branch,'')),''); d record; s record;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  if v_branch is null or v_branch not in ('Kishanganj','Jalpaiguri','Cooch Behar','Falakata','Birpara') then
    raise exception 'Invalid branch';
  end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this branch'; end if;

  drop table if exists _rmp_bd;
  create temp table _rmp_bd (rmp_id text, rmp_name text, rmp_mobile text, branch text,
                             patient_count bigint, earned numeric, paid numeric, due numeric) on commit drop;
  for d in
    select x.id, coalesce(x.name,'') as name, coalesce(x.mobile,'') as mobile, coalesce(x.branch,'') as branch
      from public.doctor_visits x
     where trim(coalesce(x.branch,'')) = v_branch
       and (exists (select 1 from fin.rmp_patient_commissions c where c.rmp_id = x.id)
         or exists (select 1 from fin.rmp_advance_payments a where a.rmp_id = x.id)
         or (jsonb_typeof(x."referralPayments") = 'array' and jsonb_array_length(x."referralPayments") > 0))
  loop
    begin
      select * into s from fin.rmp_rmp_summary(d.id);
      if coalesce(s.due,0) > 0 then
        insert into _rmp_bd values (d.id, d.name, d.mobile, d.branch, s.patient_count, s.earned, s.paid_to_this_rmp, s.due);
      end if;
    exception when others then
      null;   -- একজনের হিসাব ভাঙলে বাকিদের তালিকা থেমে যায় না
    end;
  end loop;
  return query select b.rmp_id, b.rmp_name, b.rmp_mobile, b.branch, b.patient_count,
                      round(b.earned,2), round(b.paid,2), round(b.due,2)
                 from _rmp_bd b order by b.due desc;
end $$;
revoke all on function fin.rmp_branch_due(text) from public, anon;
grant execute on function fin.rmp_branch_due(text) to authenticated;

-- ── ধাপ ৬: Master-এর "আর কমিশন নয় — এখানেই বন্ধ" (p_cap null = আবার চালু) ───────
create or replace function fin.rmp_cap_patient(p_patient_row_id text, p_rmp_id text, p_cap numeric)
returns void language plpgsql security definer set search_path = fin, public, hr as $$
declare c fin.rmp_patient_commissions%rowtype;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  if not hr.is_master() then raise exception 'Master only'; end if;
  if p_cap is not null and p_cap < 0 then raise exception 'Invalid amount'; end if;
  select * into c from fin.rmp_patient_commissions where patient_row_id = p_patient_row_id and rmp_id = p_rmp_id;
  if not found then raise exception 'Commission row not found'; end if;
  update fin.rmp_patient_commissions
     set capped_amount = case when p_cap is null then null else round(p_cap, 0) end,
         capped_by = case when p_cap is null then null else hr.my_code() end,
         capped_at = case when p_cap is null then null else now() end,
         updated_at = now()
   where id = c.id;
  insert into fin.rmp_commission_audit(action, entity_id, old_value, new_value, reason, changed_by)
  values ('CAP_PATIENT_COMMISSION', p_patient_row_id,
          jsonb_build_object('capped_amount', c.capped_amount),
          jsonb_build_object('capped_amount', case when p_cap is null then null else round(p_cap,0) end, 'rmp_id', p_rmp_id),
          case when p_cap is null then 'Cap removed by Master' else 'No further commission — fixed by Master' end,
          hr.my_code());
end $$;
revoke all on function fin.rmp_cap_patient(text, text, numeric) from public, anon;
grant execute on function fin.rmp_cap_patient(text, text, numeric) to authenticated;

commit;
