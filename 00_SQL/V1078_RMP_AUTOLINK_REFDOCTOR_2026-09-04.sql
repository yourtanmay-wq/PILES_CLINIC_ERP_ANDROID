-- ═══════════════════════════════════════════════════════════════════════════
-- V1078 (০৪.০৯.২০২৬) — "Ref By"-তে RMP-র নাম লেখা থাকলে কমিশন নিজে থেকেই বাঁধা
--
-- TK-এর কথা: *"BULAN ROY-এর ঘরে By- Dr. PKB লেখা আছে, কিন্তু কমিশন ওঠেনি"*
--            *"হ্যাঁ করুন, তবে সাবধানে"*
--
-- 🔴 যে দোষটা পাওয়া গেল (কোড মিলিয়ে, আন্দাজ নয়):
--    পুরনো মেলানোর নিয়ম (V328/V384/V586) রোগীর **`refBy`** ঘরটাকে RMP-র নামের
--    সঙ্গে মেলাত। কিন্তু আজকের অ্যাপে `refBy`-তে থাকে শুধু **ধরন** ("Dr. Visit"),
--    আর RMP-র **নাম** থাকে `refDoctor` ঘরে। তাই নাম লেখা থাকলেও কোনোদিন মিলত না —
--    কেবল নম্বর (`refDoctorMobile`) লেখা থাকলে মিলত। নাম লিখে নম্বর না লিখলে
--    রোগীটা কমিশনের হিসাব থেকে চুপচাপ বাদ পড়ে যেত।
--
-- ✅ এই ফাইল যা করে:
--    ① `fin.rmp_match_rmp_for_patient()` — মেলানোর **একটাই** নিয়ম, এখন
--       `refDoctor` ঘরটাও দেখে (পুরনো `refBy` ও নম্বরের নিয়ম অক্ষত)।
--    ② `fin.rmp_autolink_refdoctor(p_branch, p_dry_run)` — যাদের নাম মিলছে
--       অথচ কমিশন বাঁধা নেই, তাদের RMP-র **বাঁধা হারে** জুড়ে দেয়।
--
-- ⛔ সাবধানতা (TK-এর "সাবধানে" মেনে):
--    • **আগে থেকে বাঁধা কোনো কমিশন কখনো বদলায় না** — শুধু `insert`, কোনো
--      `update` নেই। হাতে বসানো হার সবসময় জিতবে।
--    • RMP-র **হার বসানো না থাকলে জোড়া হয় না** — 'NO_RATE' বলে ফেরত আসে।
--    • একই নামে **দুই বা তার বেশি RMP** মিললে জোড়া হয় না — 'AMBIGUOUS'।
--    • প্রতিটা জোড়া `fin.rmp_commission_audit`-এ লেখা থাকে, কে কখন করল সহ।
--    • `p_dry_run = true` (ডিফল্ট) হলে **কিচ্ছু লেখা হয় না**, শুধু তালিকা।
--    • পাহারা হুবহু অন্য RMP-ফাংশনের মতো: rmp_can_use() + ব্রাঞ্চ-পরীক্ষা।
--    • টাকার সূত্রে (rmp_day_commission / rmp_summary) **এক অক্ষরও** হাত পড়েনি।
-- ═══════════════════════════════════════════════════════════════════════════

-- ── ১) মেলানোর একটাই নিয়ম ─────────────────────────────────────────────────
create or replace function fin.rmp_match_rmp_for_patient(p_patient_row_id text)
returns table(rmp_id text, rmp_name text, rmp_mobile text, hits int)
language sql stable security definer set search_path = fin, public, hr as $$
  with p as (
    select lower(trim(coalesce(x."refDoctor",'')))                            as doc_name,
           lower(trim(coalesce(x."refBy",'')))                                as ref_by,
           right(regexp_replace(coalesce(x."refDoctorMobile",''),'[^0-9]','','g'),10) as doc_mob,
           coalesce(x.branch,'')                                              as pbranch
      from public.patients x where x.id = p_patient_row_id
  ), d as (
    select v.id, coalesce(v.name,'') as name, coalesce(v.mobile,'') as mobile,
           lower(trim(coalesce(v.name,'')))                                   as name_key,
           right(regexp_replace(coalesce(v.mobile,''),'[^0-9]','','g'),10)     as mob_key
      from public.doctor_visits v
     where (v.status = 'Active' or v.status is null)
  ), m as (
    select distinct d.id, d.name, d.mobile
      from d, p
     -- 🔴 নতুন: `refDoctor` (আজকের অ্যাপ নামটা এখানেই রাখে)
     where (p.doc_name <> '' and p.doc_name = d.name_key)
        -- পুরনো সারি: `refBy`-তেই নাম থাকত — নিয়মটা রাখা হলো
        or (p.ref_by  <> '' and p.ref_by  = d.name_key)
        -- নম্বর মিললেও চলবে (V384-এর পুরনো নিয়ম, অক্ষত)
        or (length(p.doc_mob) = 10 and p.doc_mob = d.mob_key)
  )
  select m.id, m.name, m.mobile, (select count(*)::int from m) from m limit 1;
$$;

revoke all on function fin.rmp_match_rmp_for_patient(text) from public, anon;
grant execute on function fin.rmp_match_rmp_for_patient(text) to authenticated;

-- ── ২) নিজে থেকে জুড়ে দেওয়া ───────────────────────────────────────────────
create or replace function fin.rmp_autolink_refdoctor(
  p_branch text, p_dry_run boolean default true)
returns table(
  patient_row_id text, patient_code text, patient_name text,
  rmp_name text, action text)
language plpgsql security definer set search_path = fin, public, hr as $$
declare
  v_branch text := nullif(trim(coalesce(p_branch,'')),'');
  r record; d fin.rmp_commission_defaults%rowtype; v_id uuid; v_hits int;
  v_rmp_id text; v_rmp_name text; v_rmp_mobile text;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  if v_branch is null or v_branch not in ('Kishanganj','Jalpaiguri','Cooch Behar','Falakata','Birpara') then
    raise exception 'Invalid branch';
  end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this branch'; end if;

  for r in
    select x.id, coalesce(x."patientId",'') as pcode, coalesce(x.name,'') as pname
      from public.patients x
     where lower(trim(coalesce(x.branch,''))) = lower(trim(v_branch))
       -- নাম বা নম্বর — কিছু একটা লেখা থাকতেই হবে
       and ( trim(coalesce(x."refDoctor",'')) <> ''
          or trim(coalesce(x."refDoctorMobile",'')) <> '' )
       -- ⛔ আগে থেকে বাঁধা থাকলে ছোঁয়া হয় না
       and not exists (select 1 from fin.rmp_patient_commissions c
                        where c.patient_row_id = x.id)
     order by x."updatedAt" desc nulls last
     limit 5000
  loop
    v_rmp_id := null; v_rmp_name := null; v_rmp_mobile := null; v_hits := 0;
    select t.rmp_id, t.rmp_name, t.rmp_mobile, t.hits
      into v_rmp_id, v_rmp_name, v_rmp_mobile, v_hits
      from fin.rmp_match_rmp_for_patient(r.id) t;

    if v_rmp_id is null then
      continue;                                   -- কোনো RMP মেলেনি — চুপচাপ বাদ
    elsif v_hits > 1 then
      patient_row_id := r.id; patient_code := r.pcode; patient_name := r.pname;
      rmp_name := coalesce(v_rmp_name,''); action := 'AMBIGUOUS';
      return next; continue;                      -- একাধিক মিলেছে — মানুষ ঠিক করবে
    end if;

    select * into d from fin.rmp_commission_defaults where rmp_id = v_rmp_id;
    if not found then
      patient_row_id := r.id; patient_code := r.pcode; patient_name := r.pname;
      rmp_name := coalesce(v_rmp_name,''); action := 'NO_RATE';
      return next; continue;                      -- হার বসানো নেই — TK বসাবেন
    end if;

    if p_dry_run then
      patient_row_id := r.id; patient_code := r.pcode; patient_name := r.pname;
      rmp_name := coalesce(nullif(d.rmp_name,''), v_rmp_name, ''); action := 'WOULD_LINK';
      return next; continue;
    end if;

    insert into fin.rmp_patient_commissions(
      patient_row_id, patient_code, patient_name, patient_mobile, treatment_branch,
      rmp_id, rmp_name, rmp_mobile, commission_mode, commission_value, set_on, set_by)
    select p.id, coalesce(p."patientId",''), coalesce(p.name,''), coalesce(p.mobile,''),
           coalesce(p.branch,''), v_rmp_id,
           coalesce(nullif(d.rmp_name,''), v_rmp_name, ''),
           coalesce(nullif(d.rmp_mobile,''), v_rmp_mobile, ''),
           d.commission_mode, d.commission_value,
           (now() at time zone 'Asia/Kolkata')::date, hr.my_code()
      from public.patients p where p.id = r.id
    on conflict (patient_row_id) do nothing         -- ⛔ কখনো বদলায় না
    returning id into v_id;

    if v_id is null then continue; end if;          -- মাঝপথে কেউ বেঁধে ফেলেছে

    insert into fin.rmp_commission_audit(action, entity_id, old_value, new_value, reason, changed_by)
    select 'AUTOLINK_REFDOCTOR', v_id::text, null, to_jsonb(x),
           'Ref By name matched the RMP directory', hr.my_code()
      from fin.rmp_patient_commissions x where x.id = v_id;

    patient_row_id := r.id; patient_code := r.pcode; patient_name := r.pname;
    rmp_name := coalesce(nullif(d.rmp_name,''), v_rmp_name, ''); action := 'LINKED';
    return next;
  end loop;
  return;
end $$;

revoke all on function fin.rmp_autolink_refdoctor(text, boolean) from public, anon;
grant execute on function fin.rmp_autolink_refdoctor(text, boolean) to authenticated;

notify pgrst, 'reload schema';
