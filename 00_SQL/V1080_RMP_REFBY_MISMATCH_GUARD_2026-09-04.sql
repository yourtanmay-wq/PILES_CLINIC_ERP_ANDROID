-- ═══════════════════════════════════════════════════════════════════════════
-- V1080 (০৪.০৯.২০২৬) — রোগীর "Ref By"-তে যে RMP-র নাম, কমিশনও তাঁরই নামে
--
-- TK: *"এই রবিন বর্মন তো কোন পেশেন্ট পাঠায় নাই… এটা কোন ডেমো ক্লিনিক নয়,
--       এটা বাস্তব ক্লিনিক… ভবিষ্যতে যেন এ ধরনের সমস্যা না হয়"*
--
-- 🔴 যা ঘটেছিল (ডেটাবেসের নিজের খাতা থেকে প্রমাণ):
--    ২২.০৮.২০২৬ ১০.৩৬ AM — স্টাফ COB-UTTAMA হাতে BULAN ROY-কে ROBIN BARMAN-এর
--    নামে ১০%-এ বেঁধেছিলেন, অথচ রোগীর ঘরে PKB-র নাম ও PKB-র নম্বর।
--    অ্যাপ ডাক্তারের কার্ড থেকে **যেকোনো** রোগীকে বাঁধতে দিত — একটাও সতর্কতা নয়।
--
-- ✅ এখানে যা বসছে — **সার্ভারেই**, তাই ফোন · কম্পিউটার · সব পথে একই পাহারা:
--    রোগীর Ref By-তে নাম/নম্বর লেখা থাকলে, সেটা যে RMP-কে দেখাচ্ছে তাঁর
--    নামেই কমিশন বসবে। না মিললে **স্টাফের কাছে আটকে যায়**, পরিষ্কার বার্তা সহ।
--
-- ⛔ সাবধানতা (বাস্তব ক্লিনিক — কোনো ভালো কাজ যেন নষ্ট না হয়):
--    • রোগীর Ref By **ফাঁকা** থাকলে আগের মতোই চলে — কিছু আটকায় না।
--    • **Master (TK) আটকান না** — ভুল সংশোধনের পথ খোলা থাকল; তবে তাঁর
--      কাজটা খাতায় "REF_BY_MISMATCH_OVERRIDE" নামে লেখা থাকে।
--    • টাকার সূত্রে (rmp_day_commission / rmp_summary) এক অক্ষরও হাত পড়েনি।
--    • আগে থেকে বাঁধা কোনো সারি এই SQL বদলায় না — শুধু নতুন বাঁধা পাহারা দেয়।
-- ═══════════════════════════════════════════════════════════════════════════

-- ── ১) রোগীর Ref By ওই RMP-কেই দেখাচ্ছে কি না ─────────────────────────────
create or replace function fin.rmp_refby_points_to(p_patient_row_id text, p_rmp_id text)
returns text  -- 'OK' · 'NO_REFBY' · 'MISMATCH'
language sql stable security definer set search_path = fin, public, hr as $$
  with p as (
    select lower(trim(coalesce(x."refDoctor",'')))                                   as dn,
           lower(trim(coalesce(x."refBy",'')))                                       as rb,
           right(regexp_replace(coalesce(x."refDoctorMobile",''),'[^0-9]','','g'),10) as dm
      from public.patients x where x.id = p_patient_row_id
  ), hit as (   -- রোগীর Ref By যে যে RMP-কে দেখায় (তালিকায় সত্যিই আছেন এমন)
    select v.id
      from public.doctor_visits v, p
     where (p.dn <> '' and p.dn = lower(trim(coalesce(v.name,''))))
        or (p.rb <> '' and p.rb = lower(trim(coalesce(v.name,''))))
        or (length(p.dm) = 10
            and p.dm = right(regexp_replace(coalesce(v.mobile,''),'[^0-9]','','g'),10))
  )
  select case
    -- ① যাঁকে বাঁধা হচ্ছে তিনিই Ref By-তে আছেন ⇒ ঠিক আছে
    when exists (select 1 from hit where hit.id = p_rmp_id) then 'OK'
    -- ② Ref By অন্য একজন **তালিকায় থাকা** RMP-কে দেখাচ্ছে ⇒ গোলমাল
    when exists (select 1 from hit) then 'MISMATCH'
    -- ③ Ref By ফাঁকা, বা লেখা নামটা তালিকার কারো সাথে মেলে না ⇒ মেলানোর কিছু
    --    নেই, তাই আগের মতোই চলবে (মিথ্যা আটকানো হবে না)
    else 'NO_REFBY' end;
$$;

revoke all on function fin.rmp_refby_points_to(text, text) from public, anon;
grant execute on function fin.rmp_refby_points_to(text, text) to authenticated;

-- ── ২) কাদের ভুল নামে বাঁধা আছে — শুধু পড়া, কিচ্ছু বদলায় না ────────────────
create or replace function fin.rmp_refby_mismatch_list(p_branch text default null)
returns table(
  patient_code text, patient_name text, patient_mobile text, branch text,
  ref_by_name text, ref_by_mobile text,
  commission_rmp text, commission_rate text, set_on date, set_by text)
language sql stable security definer set search_path = fin, public, hr as $$
  select coalesce(p."patientId",''), coalesce(p.name,''), coalesce(p.mobile,''),
         coalesce(p.branch,''),
         coalesce(nullif(trim(p."refDoctor"),''), trim(coalesce(p."refBy",''))),
         coalesce(p."refDoctorMobile",''),
         c.rmp_name, c.commission_mode || ' ' || c.commission_value::text,
         c.set_on, c.set_by
    from fin.rmp_patient_commissions c
    join public.patients p on p.id = c.patient_row_id
   where fin.rmp_can_use()
     and (p_branch is null or lower(trim(coalesce(p.branch,''))) = lower(trim(p_branch)))
     and fin.rmp_refby_points_to(c.patient_row_id, c.rmp_id) = 'MISMATCH'
   order by p.branch, c.set_on desc;
$$;

revoke all on function fin.rmp_refby_mismatch_list(text) from public, anon;
grant execute on function fin.rmp_refby_mismatch_list(text) to authenticated;

-- ── ৩) বাঁধার সময় পাহারা — স্টাফ আটকাবে, Master পারবেন ────────────────────
create or replace function fin.rmp_guard_refby(p_patient_row_id text, p_rmp_id text)
returns void language plpgsql stable security definer set search_path = fin, public, hr as $$
declare v text; v_says text;
begin
  v := fin.rmp_refby_points_to(p_patient_row_id, p_rmp_id);
  if v <> 'MISMATCH' then return; end if;
  if hr.is_master() then return; end if;   -- ⛔ TK-কে আটকানো হয় না
  select coalesce(nullif(trim(x."refDoctor"),''), trim(coalesce(x."refBy",'')), '')
    into v_says from public.patients x where x.id = p_patient_row_id;
  raise exception
    'This patient was referred by "%" - commission cannot be set for a different RMP. Ask Master.',
    coalesce(nullif(v_says,''),'someone else');
end $$;

revoke all on function fin.rmp_guard_refby(text, text) from public, anon;
grant execute on function fin.rmp_guard_refby(text, text) to authenticated;

notify pgrst, 'reload schema';

-- ── ৪) দুটো বাঁধার পথে পাহারা বসানো ───────────────────────────────────────
--    ⛔ নিচের দুটো ফাংশন V325-এর **হুবহু নকল**, শুধু একটা লাইন যোগ:
--       `perform fin.rmp_guard_refby(...)`। আর কিছুই বদলানো হয়নি।
create or replace function fin.rmp_set_patient_commission(
  p_patient_row_id text, p_rmp_id text,
  p_mode text default null, p_value numeric default null,
  p_set_on date default null)
returns uuid language plpgsql security definer set search_path = fin, public, hr as $$
declare p public.patients%rowtype; d fin.rmp_commission_defaults%rowtype;
        v_mode text; v_value numeric; v_date date; v_id uuid; v_old jsonb;
        v_default_found boolean:=false; v_existing_set_on date;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  perform fin.rmp_guard_refby(p_patient_row_id, p_rmp_id);   -- 🔴 V1080
  select * into p from public.patients where id=p_patient_row_id;
  if not found then raise exception 'Patient not found'; end if;
  if not fin.rmp_can_write_branch(p.branch) then raise exception 'Not allowed for this patient branch'; end if;
  select * into d from fin.rmp_commission_defaults where rmp_id=p_rmp_id;
  v_default_found:=found;
  v_mode:=upper(nullif(trim(coalesce(p_mode,'')),'')); v_value:=p_value;
  if v_mode is null then
    if not v_default_found then raise exception 'RMP default is not set'; end if;
    v_mode:=d.commission_mode; v_value:=d.commission_value;
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

create or replace function fin.rmp_reassign_patient(p_patient_row_id text, p_new_rmp_id text)
returns void language plpgsql security definer set search_path = fin, public, hr as $$
declare c fin.rmp_patient_commissions%rowtype; d fin.rmp_commission_defaults%rowtype;
        v_name text:=''; v_mobile text:=''; v_old jsonb;
begin
  if not hr.is_master() then raise exception 'Master only'; end if;
  perform fin.rmp_guard_refby(p_patient_row_id, p_new_rmp_id);   -- 🔴 V1080
  select * into c from fin.rmp_patient_commissions where patient_row_id=p_patient_row_id for update;
  if not found then raise exception 'Patient commission is not set'; end if;
  if trim(coalesce(p_new_rmp_id,''))='' then raise exception 'New RMP is required'; end if;
  if c.rmp_id=p_new_rmp_id then return; end if;
  select * into d from fin.rmp_commission_defaults where rmp_id=p_new_rmp_id;
  if found then v_name:=d.rmp_name; v_mobile:=d.rmp_mobile; end if;
  if v_name='' then
    select coalesce(x.name,''),coalesce(x.mobile,'') into v_name,v_mobile
      from public.doctor_visits x where x.id=p_new_rmp_id;
  end if;
  if coalesce(v_name,'')='' then raise exception 'New RMP not found'; end if;
  v_old:=to_jsonb(c);
  -- Owner decision: existing payment rows keep their old rmp_id. Only the
  -- patient entitlement (remaining + future commission) moves to new RMP.
  update fin.rmp_patient_commissions set rmp_id=p_new_rmp_id,rmp_name=v_name,rmp_mobile=coalesce(v_mobile,''),
    set_by=hr.my_code(),updated_at=now() where id=c.id;
  insert into fin.rmp_commission_audit(action,entity_id,old_value,new_value,changed_by)
  select 'RMP_REASSIGN_KEEP_OLD_PAID',c.id::text,v_old,to_jsonb(n),hr.my_code()
    from fin.rmp_patient_commissions n where n.id=c.id;
end $$;

revoke all on function fin.rmp_set_patient_commission(text,text,text,numeric,date) from public, anon;
grant execute on function fin.rmp_set_patient_commission(text,text,text,numeric,date) to authenticated;
revoke all on function fin.rmp_reassign_patient(text,text) from public, anon;
grant execute on function fin.rmp_reassign_patient(text,text) to authenticated;

notify pgrst, 'reload schema';
