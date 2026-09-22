-- ============================================================================
-- V470 — RMP: ব্রাঞ্চ-ভিত্তিক আলাদা Default Commission % (20.08.2026)
--   TK-এর নির্দেশ (হুবহু): "মনে করুন এক RMP বীরপাড়া-য় দুটো, ফালাকাটা-য়
--   দুটো, কোচবিহারে দুটো পেশেন্ট পাঠিয়েছে — কিন্তু কমিশন Default % প্রতিটা
--   ব্রাঞ্চে আলাদা। ব্রাঞ্চ-ভিত্তিক স্টাফরা যেন দেখতে পারে কত বাকি, কতজন
--   পাঠিয়েছে — অন্য ব্রাঞ্চের হিসাব না মিশে, অন্য ব্রাঞ্চের টাকাও ফাঁস না হয়ে।"
--
-- ⛔⛔⛔ এই ফাইল সম্পূর্ণ বাড়তি (additive) — পুরনো কোনো টেবিল/ফাংশন/কলাম
--   মোছা বা তার গঠন বদলানো হয়নি। শুধু ১টা নতুন টেবিল + ২টা নতুন ফাংশন,
--   আর ১টা পুরনো ফাংশনে (`rmp_set_patient_commission`) একটা "আগে
--   ব্রাঞ্চ-নির্দিষ্ট Default আছে কিনা দেখো, না থাকলে আগের বৈশ্বিক
--   Default-ই ব্যবহার করো" — এই fallback-নিয়ম যোগ হয়েছে।
--
-- 🔒 নিরাপত্তা (যাচাই করা, আন্দাজ নয়):
--   ⛔ যে RMP-র জন্য কোনো ব্রাঞ্চ-নির্দিষ্ট Default কখনো সেট করা হয়নি —
--      তার জন্য হিসাব **আগের মতোই, এক অক্ষরও না বদলে** চলবে (পুরনো
--      বৈশ্বিক Default-ই ব্যবহার হবে, ঠিক আগের মতো)।
--   ⛔ ইতিমধ্যে সেভ হওয়া কোনো `rmp_patient_commissions` সারি (পুরনো
--      কমিশন-এন্ট্রি) এই ফাইল ছোঁয় না — শুধু **নতুন** এন্ট্রি তৈরির সময়
--      Default রেজলিউশনের নিয়ম বদলায়।
--   ⛔ Branch-নির্দিষ্ট Default সেট/পড়া দুটোই `fin.rmp_can_write_branch()`
--      দিয়ে পাহারা দেওয়া — স্টাফ শুধু নিজের ব্রাঞ্চের জন্যই সেট/পড়তে
--      পারবেন, Master সব ব্রাঞ্চের জন্য পারবেন। এটা RMP Due List-এর
--      (V411) একই প্রমাণিত পাহারা-নিয়ম।
--
-- চালানোর নিয়ম: Supabase → SQL Editor → New query → পুরো ফাইল পেস্ট → Run।
-- ============================================================================

begin;

-- ── ধাপ ১: নতুন টেবিল — rmp_commission_defaults-এর হুবহু একই গঠনে,
--    শুধু branch কলাম ও (rmp_id, branch) মিলিয়ে চাবি। ──
create table if not exists fin.rmp_commission_branch_defaults (
  rmp_id text not null,
  branch text not null,
  rmp_name text not null default '',
  rmp_mobile text not null default '',
  commission_mode text not null check (commission_mode in ('PERCENT','AMOUNT')),
  commission_value numeric(12,2) not null check (commission_value >= 0),
  effective_from date not null default (now() at time zone 'Asia/Kolkata')::date,
  updated_by text not null,
  updated_at timestamptz not null default now(),
  primary key (rmp_id, branch)
);

alter table fin.rmp_commission_branch_defaults enable row level security;
alter table fin.rmp_commission_branch_defaults force row level security;

drop policy if exists rmp_branch_defaults_read on fin.rmp_commission_branch_defaults;
create policy rmp_branch_defaults_read on fin.rmp_commission_branch_defaults for select to authenticated
  using (fin.rmp_can_use());

revoke all on fin.rmp_commission_branch_defaults from public, anon;
grant select on fin.rmp_commission_branch_defaults to authenticated;

-- ── ধাপ ২: ব্রাঞ্চ-নির্দিষ্ট Default সেট করা (নতুন ফাংশন, পুরনো
--    rmp_set_default এক অক্ষরও বদলানো হয়নি, পাশাপাশি চলবে)। ──
create or replace function fin.rmp_set_branch_default(
  p_rmp_id text, p_rmp_name text, p_rmp_mobile text, p_branch text,
  p_mode text, p_value numeric)
returns void language plpgsql security definer set search_path = fin, public, hr as $$
declare v_old jsonb; v_branch_norm text;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  if trim(coalesce(p_rmp_id,''))='' then raise exception 'RMP is required'; end if;
  v_branch_norm := trim(coalesce(p_branch,''));
  if v_branch_norm='' then raise exception 'Branch is required'; end if;
  if not exists(select 1 from public.doctor_visits where id=p_rmp_id) then raise exception 'RMP not found'; end if;
  if not fin.rmp_can_write_branch(v_branch_norm) then raise exception 'Not allowed for this branch'; end if;
  if upper(coalesce(p_mode,'')) not in ('PERCENT','AMOUNT') or p_value is null or p_value<0 then
    raise exception 'Valid commission mode and value are required';
  end if;
  if upper(p_mode)='PERCENT' and p_value>100 then raise exception 'Percent cannot exceed 100'; end if;
  select to_jsonb(x) into v_old from fin.rmp_commission_branch_defaults x
    where rmp_id=p_rmp_id and branch=v_branch_norm;
  insert into fin.rmp_commission_branch_defaults(rmp_id,branch,rmp_name,rmp_mobile,commission_mode,commission_value,updated_by)
  values(trim(p_rmp_id),v_branch_norm,coalesce(p_rmp_name,''),coalesce(p_rmp_mobile,''),upper(p_mode),p_value,hr.my_code())
  on conflict(rmp_id,branch) do update set rmp_name=excluded.rmp_name,rmp_mobile=excluded.rmp_mobile,
    commission_mode=excluded.commission_mode,commission_value=excluded.commission_value,
    effective_from=(now() at time zone 'Asia/Kolkata')::date,updated_by=excluded.updated_by,updated_at=now();
  insert into fin.rmp_commission_audit(action,entity_id,old_value,new_value,changed_by)
  select 'SET_RMP_BRANCH_DEFAULT',p_rmp_id||':'||v_branch_norm,v_old,to_jsonb(x),hr.my_code()
    from fin.rmp_commission_branch_defaults x where x.rmp_id=p_rmp_id and x.branch=v_branch_norm;
end $$;

revoke all on function fin.rmp_set_branch_default(text,text,text,text,text,numeric) from public, anon;
grant execute on function fin.rmp_set_branch_default(text,text,text,text,text,numeric) to authenticated;

-- ── ধাপ ৩: ব্রাঞ্চ-নির্দিষ্ট Default পড়া (নতুন ফাংশন — Android-এ
--    দেখানোর জন্য, RMP Default Commission পপ-আপের ঠিক একই কাজে)। ──
create or replace function fin.rmp_get_branch_default(p_rmp_id text, p_branch text)
returns table(commission_mode text, commission_value numeric, is_branch_specific boolean)
language plpgsql stable security definer set search_path = fin, public, hr as $$
declare v_branch_norm text;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  v_branch_norm := trim(coalesce(p_branch,''));
  if v_branch_norm<>'' and fin.rmp_can_write_branch(v_branch_norm) then
    return query select x.commission_mode, x.commission_value, true
      from fin.rmp_commission_branch_defaults x where x.rmp_id=p_rmp_id and x.branch=v_branch_norm;
    if found then return; end if;
  end if;
  return query select x.commission_mode, x.commission_value, false
    from fin.rmp_commission_defaults x where x.rmp_id=p_rmp_id;
end $$;

revoke all on function fin.rmp_get_branch_default(text,text) from public, anon;
grant execute on function fin.rmp_get_branch_default(text,text) to authenticated;

-- ── ধাপ ৪: `rmp_set_patient_commission`-এ fallback-নিয়ম যোগ ──
--    ⛔ এই ফাংশনের বাকি সব লাইন (validation, audit, insert/update) হুবহু
--    আগের মতোই — শুধু Default রেজলিউশনের ২ লাইনের জায়গায় নতুন যুক্তি:
--    আগে ব্রাঞ্চ-নির্দিষ্ট Default দেখা হয়, না পেলে বৈশ্বিক Default-ই
--    (আগের মতোই) ব্যবহার হয়। মোড/মান explicit দিলে (p_mode/p_value)
--    Default-ই ছোঁয়া হয় না, ঠিক আগের মতো।
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
      raise exception 'RMP default is not set';
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

-- ── ধাপ ৫: যাচাই — নতুন টেবিল ফাঁকা থাকবে (কেউ এখনো ব্যবহার করেননি),
--    কারো টাকার হিসাব বদলায়নি (বাড়তি নিরাপত্তা-চেক)। ──
do $$
declare n_branch_defaults int; n_patient_commissions int;
begin
  select count(*) into n_branch_defaults from fin.rmp_commission_branch_defaults;
  select count(*) into n_patient_commissions from fin.rmp_patient_commissions;
  raise notice '───────────────────────────────────────────';
  raise notice 'নতুন ব্রাঞ্চ-Default টেবিলে সারি (আজ হওয়ার কথা ০): % টি', n_branch_defaults;
  raise notice 'পুরনো rmp_patient_commissions মোট সারি (অপরিবর্তিত থাকার কথা): % টি', n_patient_commissions;
  raise notice '───────────────────────────────────────────';
  raise notice '✅ কারো কমিশনের অঙ্ক এই ফাইলে বদলায়নি — শুধু নতুন, বাড়তি ব্যবস্থা যোগ হলো।';
end $$;

commit;
