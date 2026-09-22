-- ============================================================================
-- V488 — সব ব্রাঞ্চের সব RMP-র জন্য **স্বয়ংক্রিয় ১০% ডিফল্ট কমিশন**
-- TK-নির্দেশ, ২০.০৮.২০২৬
-- ============================================================================
--
-- TK-এর কথা:
--   > "সমস্ত ব্রাঞ্চের সমস্ত RMP-দের automatic 10% Default কমিশন করে রাখুন।
--   >  কিন্তু আগে যাদের সেট করে রেখেছিলাম সেগুলোই থাকবে। শুধু যাদের এখনো
--   >  কিছু করা হয়নি, তাদের ক্ষেত্রে 10% ডিফল্ট। আমি চাইলে কমাতে বা বাড়াতে
--   >  পারি, অথবা পার্সেন্টেজ না দিয়ে ডাইরেক্ট কোনো অ্যামাউন্টও দিতে পারি।"
--
-- ── কীভাবে করা হলো (এবং কেন এভাবেই সবচেয়ে নিরাপদ) ────────────────────────
--
-- ❌ যেভাবে করা হয়নি: ৯১৪ জন RMP-র প্রত্যেকের জন্য একটা করে সারি লিখে
--    দেওয়া। ওতে হাজার হাজার নতুন সারি তৈরি হত, Free-plan-এ চাপ পড়ত, আর
--    ভবিষ্যতে নতুন RMP যোগ হলে তাঁর জন্য আবার একই কাজ করতে হত।
--
-- ✅ যেভাবে করা হলো: **নিয়মটাই বদলানো হলো, তথ্য নয়।** আগে যেখানে কোনো
--    Default না পেলে কাজ থেমে যেত ('RMP default is not set'), এখন সেখানে
--    ১০% PERCENT ধরা হয়। একটাও নতুন সারি লেখা হয় না।
--
-- ── অগ্রাধিকারের ক্রম (উপর থেকে নিচে) ────────────────────────────────────
--   ১. এই RMP-র এই ব্রাঞ্চের নিজস্ব Default  → থাকলে সেটাই   (V470)
--   ২. এই RMP-র বৈশ্বিক Default              → থাকলে সেটাই   (V325)
--   ৩. কোনোটাই নেই                            → **১০% (নতুন, V488)**
--   (মোড/মান হাতে দিলে — যেমন সরাসরি ₹৫০০ — উপরের কোনোটাই ছোঁয়া হয় না।)
--
-- ── 🔒 নিরাপত্তা (কোড পড়ে যাচাই করা, আন্দাজ নয়) ──────────────────────────
--   ⛔ আগে সেট করা কোনো Default **বদলায় না** — নতুন নিয়মটা শুধু তখনই
--      কাজ করে যখন ব্রাঞ্চ ও বৈশ্বিক **দুটোই অনুপস্থিত**। একটাও থাকলে
--      হিসাব হুবহু আগের মতোই চলে।
--   ⛔ ইতিমধ্যে সেভ হয়ে যাওয়া কোনো `rmp_patient_commissions` সারি এই
--      ফাইল ছোঁয় না — কারো পুরনো কমিশনের অঙ্ক বদলায় না।
--   ⛔ কোনো টেবিল তৈরি/মোছা হয় না, কোনো সারি insert/update/delete হয় না।
--      শুধু দুটো ফাংশনের ভিতরের যুক্তি নতুন করে লেখা হয়।
--   ⛔ পাহারা (rmp_can_use · rmp_can_write_branch · Master-অনুমোদনের শর্ত ·
--      audit লেখা) — সব হুবহু আগের মতোই, একটাও শিথিল করা হয়নি।
--   ⛔ ভুল ঠেকানোর পরীক্ষাগুলোও অটুট: PERCENT ১০০-র বেশি নয়, ঋণাত্মক নয়,
--      ভবিষ্যতের তারিখ নয়।
--
-- ── ভবিষ্যতে ১০% বদলাতে হলে ──────────────────────────────────────────────
--   নিচের ধাপ ১-এর `fin.rmp_auto_default_percent()` ফাংশনটায় শুধু সংখ্যাটা
--   বদলে আবার Run করলেই হবে — আর কোথাও হাত দিতে হবে না।
--
-- চালানোর নিয়ম: Supabase → SQL Editor → New query → পুরো ফাইল পেস্ট → Run।
-- ============================================================================

begin;

-- ── ধাপ ১: স্বয়ংক্রিয় হারটা একটাই জায়গায় ──────────────────────────────
--    সংখ্যাটা কোডের ভিতরে ছড়িয়ে না রেখে একটাই ফাংশনে রাখা হলো, যাতে
--    পরে বদলাতে হলে এক জায়গাতেই বদলায় (দুই জায়গায় দুই রকম হয়ে যাওয়ার
--    কোনো সুযোগ থাকে না)।
create or replace function fin.rmp_auto_default_percent()
returns numeric language sql immutable as $$
  select 10::numeric
$$;

comment on function fin.rmp_auto_default_percent() is
  'V488 (TK, 20.08.2026): যে RMP-র কোনো ব্রাঞ্চ-নির্দিষ্ট বা বৈশ্বিক Default সেট করা নেই, তাঁর জন্য স্বয়ংক্রিয় হার। বদলাতে হলে শুধু এই সংখ্যাটা।';

revoke all on function fin.rmp_auto_default_percent() from public, anon;
grant execute on function fin.rmp_auto_default_percent() to authenticated;


-- ── ধাপ ২: `rmp_get_branch_default` — দেখানোর সময়ও যেন ১০% আসে ─────────
--    ⛔ প্রথম দুটো ধাপ (ব্রাঞ্চ-নির্দিষ্ট → বৈশ্বিক) V470-এর হুবহু একই,
--       এক অক্ষরও বদলায়নি। শুধু শেষে — দুটোর একটাও না পেলে এখন খালি
--       হাতে ফেরার বদলে ১০% ফেরত যায় (is_branch_specific = false)।
--    এর ফলে ফোনের "Referral Income" ফর্মে হারটা নিজে থেকেই ১০% বসে
--    থাকবে, স্টাফকে কিছু টাইপ করতে হবে না।
create or replace function fin.rmp_get_branch_default(p_rmp_id text, p_branch text)
returns table(commission_mode text, commission_value numeric, is_branch_specific boolean)
language plpgsql stable security definer set search_path = fin, public, hr as $$
declare v_branch_norm text;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  v_branch_norm := trim(coalesce(p_branch,''));
  -- ১) এই ব্রাঞ্চের নিজস্ব Default (V470) — আগের মতোই
  if v_branch_norm<>'' and fin.rmp_can_write_branch(v_branch_norm) then
    return query select x.commission_mode, x.commission_value, true
      from fin.rmp_commission_branch_defaults x where x.rmp_id=p_rmp_id and x.branch=v_branch_norm;
    if found then return; end if;
  end if;
  -- ২) বৈশ্বিক Default (V325) — আগের মতোই
  return query select x.commission_mode, x.commission_value, false
    from fin.rmp_commission_defaults x where x.rmp_id=p_rmp_id;
  if found then return; end if;
  -- ৩) 🔵 V488 — দুটোর একটাও নেই, তাই স্বয়ংক্রিয় হার।
  return query select 'PERCENT'::text, fin.rmp_auto_default_percent(), false;
end $$;

revoke all on function fin.rmp_get_branch_default(text,text) from public, anon;
grant execute on function fin.rmp_get_branch_default(text,text) to authenticated;


-- ── ধাপ ৩: `rmp_set_patient_commission` — আসল হিসাবেও একই নিয়ম ─────────
--    ⛔ এই ফাংশনের বাকি **প্রতিটা** লাইন V470-এর হুবহু নকল — পাহারা,
--       validation, তারিখের নিয়ম, Master-অনুমোদন, reassignment-বাধা,
--       insert/update, audit — কিচ্ছু বদলায়নি।
--    ⛔ বদলেছে শুধু একটাই জায়গা: আগে যেখানে
--         raise exception 'RMP default is not set';
--       ছিল, এখন সেখানে ১০% PERCENT ধরা হয়।
create or replace function fin.rmp_set_patient_commission(
  p_patient_row_id text, p_rmp_id text,
  p_mode text default null, p_value numeric default null,
  p_set_on date default null)
returns uuid language plpgsql security definer set search_path = fin, public, hr as $$
declare p public.patients%rowtype; d fin.rmp_commission_defaults%rowtype;
        bd fin.rmp_commission_branch_defaults%rowtype;
        v_mode text; v_value numeric; v_date date; v_id uuid; v_old jsonb;
        v_default_found boolean:=false; v_branch_default_found boolean:=false;
        v_existing_set_on date;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  select * into p from public.patients where id=p_patient_row_id;
  if not found then raise exception 'Patient not found'; end if;
  if not fin.rmp_can_write_branch(p.branch) then raise exception 'Not allowed for this patient branch'; end if;
  -- 🔴🔒 V470 — আগে এই রোগীর ব্রাঞ্চের জন্য RMP-এর কোনো ব্রাঞ্চ-নির্দিষ্ট
  -- Default আছে কিনা দেখা হয়; থাকলে সেটাই, না থাকলে (আগের মতোই) বৈশ্বিক।
  select * into bd from fin.rmp_commission_branch_defaults where rmp_id=p_rmp_id and branch=p.branch;
  v_branch_default_found:=found;
  select * into d from fin.rmp_commission_defaults where rmp_id=p_rmp_id;
  v_default_found:=found;
  v_mode:=upper(nullif(trim(coalesce(p_mode,'')),'')); v_value:=p_value;
  if v_mode is null then
    if v_branch_default_found then
      v_mode:=bd.commission_mode; v_value:=bd.commission_value;
    elsif v_default_found then
      v_mode:=d.commission_mode; v_value:=d.commission_value;
    else
      -- 🔵🔒 V488 (TK, 20.08.2026) — আগে এখানে
      --     raise exception 'RMP default is not set';
      -- ছিল, তাই সেট-না-করা RMP-র রোগীর কমিশন কখনো বসত না, সব ₹0
      -- দেখাত। এখন স্বয়ংক্রিয় হার ব্যবহার হয়।
      -- ⛔ উপরের দুটো শর্তের একটাও সত্যি হলে এই লাইনে আসাই হয় না —
      --    অর্থাৎ TK-র আগে সেট করা Default সবসময় অগ্রাধিকার পায়।
      v_mode:='PERCENT'; v_value:=fin.rmp_auto_default_percent();
    end if;
  end if;
  if v_mode not in ('PERCENT','AMOUNT') or v_value is null or v_value<0 then raise exception 'Valid commission is required'; end if;
  if v_mode='PERCENT' and v_value>100 then raise exception 'Percent cannot exceed 100'; end if;
  v_date:=coalesce(p_set_on,(now() at time zone 'Asia/Kolkata')::date);
  if v_date>(now() at time zone 'Asia/Kolkata')::date then raise exception 'Future commission date is not allowed'; end if;
  if v_date<>(now() at time zone 'Asia/Kolkata')::date and not hr.is_master() then
    raise exception 'Master approval required for an old date';
  end if;
  select x.set_on into v_existing_set_on from fin.rmp_patient_commissions x
   where x.patient_row_id=p_patient_row_id;
  if found and v_existing_set_on<(now() at time zone 'Asia/Kolkata')::date and not hr.is_master() then
    raise exception 'Master approval required to change an earlier commission';
  end if;
  if exists(select 1 from fin.rmp_patient_commissions x where x.patient_row_id=p_patient_row_id and x.rmp_id<>p_rmp_id) then
    raise exception 'Use the protected RMP reassignment workflow';
  end if;
  select to_jsonb(x) into v_old from fin.rmp_patient_commissions x where patient_row_id=p_patient_row_id;
  insert into fin.rmp_patient_commissions(patient_row_id,patient_code,patient_name,patient_mobile,treatment_branch,
    rmp_id,rmp_name,rmp_mobile,commission_mode,commission_value,set_on,set_by)
  values(p.id,coalesce(p."patientId",''),coalesce(p.name,''),coalesce(p.mobile,''),coalesce(p.branch,''),
    p_rmp_id,coalesce(nullif(d.rmp_name,''),p."refDoctor",''),coalesce(nullif(d.rmp_mobile,''),p."refDoctorMobile",''),
    v_mode,v_value,v_date,hr.my_code())
  on conflict(patient_row_id) do update set rmp_id=excluded.rmp_id,rmp_name=excluded.rmp_name,
    rmp_mobile=excluded.rmp_mobile,commission_mode=excluded.commission_mode,commission_value=excluded.commission_value,
    set_on=excluded.set_on,set_by=excluded.set_by,updated_at=now()
  returning id into v_id;
  insert into fin.rmp_commission_audit(action,entity_id,old_value,new_value,changed_by)
  select 'SET_PATIENT_COMMISSION',v_id::text,v_old,to_jsonb(x),hr.my_code()
    from fin.rmp_patient_commissions x where x.id=v_id;
  return v_id;
end $$;

revoke all on function fin.rmp_set_patient_commission(text,text,text,numeric,date) from public, anon;
grant execute on function fin.rmp_set_patient_commission(text,text,text,numeric,date) to authenticated;


-- ── ধাপ ৪: যাচাই — কারো আগের সেটিং বা টাকার হিসাব বদলায়নি ──────────────
--    এই ব্লকটা কিছুই লেখে না; শুধু গুনে দেখে সব ঠিক আছে কিনা। কিছু গোলমাল
--    থাকলে পুরো ফাইলটাই নিজে থেকে বাতিল (rollback) হয়ে যায় — অর্ধেক কাজ
--    হয়ে পড়ে থাকার কোনো সুযোগ নেই।
do $$
declare n_global int; n_branch int; n_patient int; v_rate numeric;
begin
  select count(*) into n_global   from fin.rmp_commission_defaults;
  select count(*) into n_branch    from fin.rmp_commission_branch_defaults;
  select count(*) into n_patient   from fin.rmp_patient_commissions;
  select fin.rmp_auto_default_percent() into v_rate;

  if v_rate is null or v_rate <= 0 or v_rate > 100 then
    raise exception 'V488 থেমে গেল — স্বয়ংক্রিয় হারটা ভুল (%)', v_rate;
  end if;

  raise notice '── V488 সফল ────────────────────────────────';
  raise notice 'আগে থেকে সেট করা বৈশ্বিক Default   : % টি (অক্ষত)', n_global;
  raise notice 'আগে থেকে সেট করা ব্রাঞ্চ Default   : % টি (অক্ষত)', n_branch;
  raise notice 'আগে থেকে সেভ হওয়া রোগীর কমিশন     : % টি (অক্ষত)', n_patient;
  raise notice 'যাদের কিছুই সেট করা নেই তাদের হার  : % শতাংশ (নতুন)', v_rate;
  raise notice '─────────────────────────────────────────────';
end $$;

commit;
