-- ═══════════════════════════════════════════════════════════════════════════
-- V1081 (০৪.০৯.২০২৬) — PK ও PKB-র সব রোগী ০১.০৯.২০২৬ থেকে ৪০%,
--                       আর BULAN ROY-এর ভুল জোড়া সংশোধন
--
-- TK: *"PK ও PKB-র সব রোগীর হার ৪০% করুন, সেপ্টেম্বর মাসের ১ তারিখ থেকে…
--       ভবিষ্যতে যেন এটা কার্যকরী হয়… এটা একটা লাইভ ক্লিনিক"*
--
-- ⛔ ০১.০৯-এর আগে জমা হওয়া টাকায় **পুরনো হারই** থাকবে — এক পয়সাও নড়বে না।
--    ০১.০৯ ও তার পরের জমায় নতুন হার। ব্যবস্থাটা V941-এর **হুবহু প্রমাণিত** পথ
--    (`prev_mode`/`prev_value`/`rate_changed_on`) — নতুন নিয়ম বানানো হয়নি।
--
-- 🔵 TK-এর স্পষ্ট নিয়ম (০৪.০৯, সংশোধিত): **৪০% হলো ছাদ, সবার জন্য এক হার নয়।**
--    · হার ৪০%-এর **বেশি** ছিল (যেমন ৫০%)  ⇒ ০১.০৯ থেকে **৪০%**
--    · হার ৪০% বা তার **কম** ছিল           ⇒ **অপরিবর্তিত**, কিছুই বদলায় না
--    ⛔ কারো হার এতে **বাড়ে না** — শুধু বেশি হারগুলো নামে।
--    ⛔ AMOUNT (সরাসরি টাকা) ধরনের কোনো সারি ছোঁয়া হয় না — ওটা শতাংশ নয়।
--
-- 🔴 গভীরে যাচাই করতে গিয়ে পাওয়া আসল গোলমাল (ধাপ ৩–৪-এ সারানো):
--    Chamber Review-র "আজকের RMP কমিশন" (`rmp_day_commission`, V426) নিজের
--    আলাদা অঙ্ক কষত — **তারিখ-ভিত্তিক হার-বদল মানত না**। ফলে হার বদলালে
--    RMP Due List এক টাকা বলত, Chamber Review আরেক টাকা। এখন দুটোই একই
--    হিসাব ডাকে, তাই দুই পর্দায় দুরকম উত্তর আর হতে পারে না।
--
-- ⛔ কোনো রোগীর বিল/জমা/টাকা ছোঁয়া হয় না — শুধু কমিশনের হার ও জোড়া।
-- ═══════════════════════════════════════════════════════════════════════════

-- ── ধাপ ১: "ওই তারিখ পর্যন্ত অর্জিত কমিশন" ────────────────────────────────
--    `fin.rmp_earned_for` (V941)-এর হুবহু একই নিয়ম, শুধু একটা শেষ-তারিখ যোগ।
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
  paid_today numeric, commission_today numeric)
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
         round(c.ptoday, 2), round(greatest(0, c.ctoday), 2)
    from calc c
   where c.ptoday <> 0 or c.ctoday <> 0
   order by c.rnm, c.pnm;
end $$;

revoke all on function fin.rmp_day_commission(text,text) from public, anon;
grant execute on function fin.rmp_day_commission(text,text) to authenticated;

-- ── ধাপ ৩: BULAN ROY — ROBIN BARMAN থেকে PKB-তে সংশোধন ────────────────────
--    ⛔ শুধু ওই একটাই সারি (মোবাইল ৭৮৬৫৮৫৮০৫৬)। ROBIN BARMAN-কে এ বাবদ
--       কোনো টাকা দেওয়া হয়নি (Ref. Paid ₹০), তাই ফেরতের প্রশ্ন নেই।
--    ⛔ হার বসছে ৫০% — PKB-র বাকি রোগীদের সঙ্গে এক (১০% ছিল ROBIN BARMAN-এর,
--       ভুল করে বসা)। নিচের ধাপ ৪-এ এটাও ০১.০৯ থেকে ৪০% হয়ে যাবে।
update fin.rmp_patient_commissions c
   set rmp_id           = d.id,
       rmp_name         = coalesce(nullif(d.name,''),'PKB'),
       rmp_mobile       = coalesce(d.mobile,''),
       commission_mode  = 'PERCENT',
       commission_value = 50,   -- PKB-র পুরনো হার; ধাপ ৪-এ ছাদ ধরে ৪০% হবে
       prev_mode = null, prev_value = null, rate_changed_on = null,
       set_by = 'V1081_TK_CORRECTION',
       updated_at = now()
  from public.patients p, public.doctor_visits d
 where c.patient_row_id = p.id
   and right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) = '7865858056'
   and right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) = '9242009205';

insert into fin.rmp_commission_audit(action, entity_id, old_value, new_value, reason, changed_by)
select 'RMP_CORRECTED_WRONG_LINK', c.id::text, null, to_jsonb(c),
       'TK: Robin Barman never referred this patient - Ref By says PKB', 'V1081'
  from fin.rmp_patient_commissions c
  join public.patients p on p.id = c.patient_row_id
 where right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) = '7865858056';

-- ── ধাপ ৪: PK ও PKB-র **সব** রোগীর হার ০১.০৯.২০২৬ থেকে ৪০% ───────────────
--    ⛔ `coalesce` ব্যবহার করায় আগে একবার হার বদলানো থাকলে সেটা নষ্ট হয় না।
--    ⛔ **৪০% ছাদ** — ৪০% বা তার কম হারের কোনো সারি ছোঁয়াই হয় না।
--    ⛔ PK ও PKB-র id খাতার সারি B636-এ যাচাই করা; সঙ্গে PKB-র নম্বরও ধরা হলো।
update fin.rmp_patient_commissions c
   set prev_mode        = coalesce(c.prev_mode, c.commission_mode),
       prev_value       = coalesce(c.prev_value, c.commission_value),
       rate_changed_on  = coalesce(c.rate_changed_on, date '2026-09-01'),
       commission_mode  = 'PERCENT',
       commission_value = 40,
       updated_at = now()
 -- 🔵 ৪০% ছাদ: শুধু যাঁদের শতাংশ ৪০-এর বেশি, তাঁরাই নামবেন
 where c.commission_mode = 'PERCENT'
   and c.commission_value > 40
   and c.rmp_id in (
   select d.id from public.doctor_visits d
    where d.id in ('dv_41df54bdeee64a4b9c8c07f3124c5484',
                   'dv_91f6cdf6cff84218b82a0bbc74021b9e')
       or right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) = '9242009205');

-- ── ধাপ ৫: নতুন রোগীর ডিফল্টও ৪০% — তবে **শুধু নামানো, কখনো বাড়ানো নয়** ──
--    ⛔ যে RMP-র ডিফল্ট ৪০%-এর বেশি (যেমন ৫০%), শুধু তাঁরটাই ৪০% হবে।
--    ⛔ ডিফল্ট **নেই** এমন RMP-তে নতুন সারি বসানো হয় না — নইলে V488-এর
--       স্বয়ংক্রিয় ১০% থেকে ৪০%-এ **বেড়ে** যেত, যেটা TK-এর নিয়মের উল্টো।
update fin.rmp_commission_defaults f
   set commission_mode = 'PERCENT',
       commission_value = 40,
       effective_from = date '2026-09-01',
       updated_by = 'V1081',
       updated_at = now()
 where f.commission_mode = 'PERCENT'
   and f.commission_value > 40
   and f.rmp_id in (
     select d.id from public.doctor_visits d
      where d.id in ('dv_41df54bdeee64a4b9c8c07f3124c5484',
                     'dv_91f6cdf6cff84218b82a0bbc74021b9e')
         or right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) = '9242009205');

-- ── ধাপ ৫ক: ব্রাঞ্চ-নির্দিষ্ট হারেও একই ছাদ (থাকলে) ───────────────────────
update fin.rmp_commission_branch_defaults f
   set commission_mode = 'PERCENT',
       commission_value = 40,
       updated_at = now()
 where f.commission_mode = 'PERCENT'
   and f.commission_value > 40
   and f.rmp_id in (
     select d.id from public.doctor_visits d
      where d.id in ('dv_41df54bdeee64a4b9c8c07f3124c5484',
                     'dv_91f6cdf6cff84218b82a0bbc74021b9e')
         or right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) = '9242009205');

notify pgrst, 'reload schema';

-- ── ধাপ ৬: মিলিয়ে দেখা (শুধু পড়া) ────────────────────────────────────────
select coalesce(d.name,'')                                            as rmp,
       coalesce(p.name,'')                                            as rogi,
       c.commission_mode || ' ' || c.commission_value::text            as ekhon_har,
       coalesce(c.prev_mode,'-') || ' ' || coalesce(c.prev_value::text,'-') as ager_har,
       c.rate_changed_on                                              as bodol_theke
  from fin.rmp_patient_commissions c
  join public.doctor_visits d on d.id = c.rmp_id
  left join public.patients p on p.id = c.patient_row_id
 where c.rmp_id in ('dv_41df54bdeee64a4b9c8c07f3124c5484',
                    'dv_91f6cdf6cff84218b82a0bbc74021b9e')
    or right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) = '9242009205'
 order by d.name, p.name;
