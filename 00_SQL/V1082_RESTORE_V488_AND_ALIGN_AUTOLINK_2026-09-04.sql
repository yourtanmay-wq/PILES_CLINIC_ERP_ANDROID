-- ═══════════════════════════════════════════════════════════════════════════
-- V1082 (০৪.০৯.২০২৬) — 🔴 আমার নিজের ভুলের সংশোধন + জোড়া লাগানোয় একই নিয়ম
--
-- TK: *"আরো সঠিকভাবে গভীরে যাচাই করুন যাতে এই ধরনের সমস্যা ভবিষ্যতে আর
--       কোনদিনও না হয়"*
--
-- 🔴 আমার ভুল (গভীরে যাচাই করতে গিয়ে নিজেই ধরলাম, TK-কে ঢাকিনি):
--    গতকালের V1080-এ পাহারা বসাতে গিয়ে আমি `rmp_set_patient_commission`-এর
--    **পুরনো (V325) সংস্করণ** বসিয়ে দিয়েছিলাম। কিন্তু ওই ফাংশনটা পরে আরও
--    তিনবার লেখা হয়েছে — V380 → V470 → **V488**। ফলে দুটো ভালো কাজ মুছে
--    গিয়েছিল:
--      ① V470 — RMP-র **ব্রাঞ্চ-নির্দিষ্ট** হার
--      ② V488 — হার বসানো না থাকলে **স্বয়ংক্রিয় ১০%** (নইলে "RMP default is
--         not set" বলে কমিশন বসতই না)
--    ⇒ এই ফাইলে **V488-এর হুবহু সংস্করণ** ফেরত, সঙ্গে V1080-এর পাহারার
--      একটামাত্র লাইন।
--
-- ✅ সঙ্গে: জোড়া লাগানোর কাজেও (`rmp_autolink_refdoctor`) এখন **একই নিয়ম** —
--    ব্রাঞ্চ-নির্দিষ্ট → বৈশ্বিক → স্বয়ংক্রিয় ১০%। আগে হার বসানো না থাকলে
--    জোড়াই লাগত না ('NO_RATE'), অথচ অ্যাপের বাকি সব জায়গায় ১০% ধরা হত —
--    দুই জায়গায় দুরকম নিয়ম। সেটাও এখানে মিলিয়ে দেওয়া হলো।
--
-- ⛔ কোনো রোগীর বিল/জমা/টাকা ছোঁয়া হয় না · আগে বাঁধা কোনো কমিশন বদলায় না ·
--    Ref By-র পাহারা (V1080) অটুট · নতুন APK লাগে না।
-- ═══════════════════════════════════════════════════════════════════════════

-- ── ধাপ ১: V488-এর আসল সংস্করণ ফেরত (+ V1080-এর পাহারা) ───────────────────
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
  perform fin.rmp_guard_refby(p_patient_row_id, p_rmp_id);   -- 🔴 V1082
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

-- ── ধাপ ২: জোড়া লাগানোতেও একই হার-বাছাইয়ের নিয়ম ──────────────────────────
--    ⛔ V1079-এর হুবহু একই, শুধু হার বাছাই বদলেছে:
--       ব্রাঞ্চ-নির্দিষ্ট → বৈশ্বিক → স্বয়ংক্রিয় ১০%। তাই 'NO_RATE' আর হয় না।
create or replace function fin.rmp_autolink_refdoctor(
  p_branch text, p_dry_run boolean default true)
returns table(
  patient_row_id text, patient_code text, patient_name text,
  rmp_name text, action text)
language plpgsql security definer set search_path = fin, public, hr as $$
declare
  v_branch text := nullif(trim(coalesce(p_branch,'')),'');
  r record; v_id uuid;
  v_mode text; v_value numeric; v_nm text; v_mb text;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  if v_branch is null or v_branch not in ('Kishanganj','Jalpaiguri','Cooch Behar','Falakata','Birpara') then
    raise exception 'Invalid branch';
  end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this branch'; end if;

  for r in
    with cand as (
      select p.id as pid, coalesce(p."patientId",'') as pcode, coalesce(p.name,'') as pname,
             coalesce(p.mobile,'') as pmobile, coalesce(p.branch,'') as pbranch,
             lower(trim(coalesce(p."refDoctor",''))) as dn,
             lower(trim(coalesce(p."refBy",'')))     as rb,
             right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10) as dm
        from public.patients p
       where lower(trim(coalesce(p.branch,''))) = lower(trim(v_branch))
         and not exists (select 1 from fin.rmp_patient_commissions c
                          where c.patient_row_id = p.id)
    ), docs as (
      select v.id as did, coalesce(v.name,'') as dname, coalesce(v.mobile,'') as dmobile,
             lower(trim(coalesce(v.name,''))) as nk,
             right(regexp_replace(coalesce(v.mobile,''),'[^0-9]','','g'),10) as mk
        from public.doctor_visits v
       where (v.status = 'Active' or v.status is null)
    ), pairs as (
      select distinct c.pid, c.pcode, c.pname, c.pmobile, c.pbranch,
                      d.did, d.dname, d.dmobile
        from cand c join docs d
          on (c.dn <> '' and c.dn = d.nk)
          or (c.rb <> '' and c.rb = d.nk)
          or (length(c.dm) = 10 and c.dm = d.mk)
    )
    select x.*, count(*) over (partition by x.pid) as hits
      from pairs x order by x.pid
  loop
    if r.hits > 1 then
      patient_row_id := r.pid; patient_code := r.pcode; patient_name := r.pname;
      rmp_name := r.dname; action := 'AMBIGUOUS';
      return next; continue;
    end if;

    -- 🔵 V1082 — অ্যাপের বাকি সব জায়গার হুবহু একই অগ্রাধিকার
    select g.commission_mode, g.commission_value
      into v_mode, v_value
      from fin.rmp_get_branch_default(r.did, r.pbranch) g;
    if v_mode is null then v_mode := 'PERCENT'; v_value := fin.rmp_auto_default_percent(); end if;

    select coalesce(nullif(d.rmp_name,''), r.dname), coalesce(nullif(d.rmp_mobile,''), r.dmobile)
      into v_nm, v_mb
      from fin.rmp_commission_defaults d where d.rmp_id = r.did;
    v_nm := coalesce(v_nm, r.dname); v_mb := coalesce(v_mb, r.dmobile);

    if p_dry_run then
      patient_row_id := r.pid; patient_code := r.pcode; patient_name := r.pname;
      rmp_name := v_nm; action := 'WOULD_LINK';
      return next; continue;
    end if;

    insert into fin.rmp_patient_commissions(
      patient_row_id, patient_code, patient_name, patient_mobile, treatment_branch,
      rmp_id, rmp_name, rmp_mobile, commission_mode, commission_value, set_on, set_by)
    values (r.pid, r.pcode, r.pname, r.pmobile, r.pbranch, r.did, v_nm, v_mb,
            v_mode, v_value, (now() at time zone 'Asia/Kolkata')::date, hr.my_code())
    on conflict (patient_row_id) do nothing
    returning id into v_id;

    if v_id is null then continue; end if;

    insert into fin.rmp_commission_audit(action, entity_id, old_value, new_value, reason, changed_by)
    select 'AUTOLINK_REFDOCTOR', v_id::text, null, to_jsonb(x),
           'Ref By name matched the RMP directory', hr.my_code()
      from fin.rmp_patient_commissions x where x.id = v_id;

    patient_row_id := r.pid; patient_code := r.pcode; patient_name := r.pname;
    rmp_name := v_nm; action := 'LINKED';
    return next;
  end loop;
  return;
end $$;

revoke all on function fin.rmp_autolink_refdoctor(text, boolean) from public, anon;
grant execute on function fin.rmp_autolink_refdoctor(text, boolean) to authenticated;

notify pgrst, 'reload schema';
