-- ═══════════════════════════════════════════════════════════════════════════
-- V1079 (০৪.০৯.২০২৬) — V1078-এর জোড়া লাগানো কাজ করেনি, কারণ ও সমাধান
--
-- 🔴 আমার নিজের ভুল: V1078-এর ছাঁকনি শুধু `refDoctor` বা `refDoctorMobile`
--    লেখা রোগীদেরই দেখত। কিন্তু পুরনো রোগীদের অনেকের নামটা আছে **`refBy`**
--    ঘরে (মেলানোর নিয়ম ওটাও মানে, কিন্তু ছাঁকনি ওটা বাদ দিয়ে দিত)।
--    ⇒ BULAN ROY-এর মতো রোগীরা লুপে ঢুকতই না, তাই কিছুই জোড়া লাগেনি।
--
-- ✅ এখানে যা ঠিক হলো:
--    • ছাঁকনিতে `refBy`-ও ধরা হয়।
--    • রোগীপ্রতি আলাদা ডাক নয় — **একটাই query**-তে সব মেলানো (অনেক দ্রুত,
--      ৫০০০ রোগীতেও আটকাবে না)।
--
-- ⛔ ফাংশনের নাম ও প্যারামিটার হুবহু আগের — **নতুন APK লাগবে না**,
--    এই SQL চালালেই ফোন ও কম্পিউটার দুটোতেই কাজ করবে।
-- ⛔ সাবধানতা আগের মতোই অক্ষত: আগে থেকে বাঁধা কমিশন কখনো বদলায় না
--    (শুধু insert, on conflict do nothing) · হার বসানো না থাকলে জোড়া হয় না
--    ('NO_RATE') · একই নামে একাধিক RMP মিললে জোড়া হয় না ('AMBIGUOUS') ·
--    প্রতিটা জোড়া `fin.rmp_commission_audit`-এ লেখা থাকে ·
--    টাকার সূত্রে (rmp_day_commission) এক অক্ষরও হাত পড়েনি।
-- ═══════════════════════════════════════════════════════════════════════════

create or replace function fin.rmp_autolink_refdoctor(
  p_branch text, p_dry_run boolean default true)
returns table(
  patient_row_id text, patient_code text, patient_name text,
  rmp_name text, action text)
language plpgsql security definer set search_path = fin, public, hr as $$
declare
  v_branch text := nullif(trim(coalesce(p_branch,'')),'');
  r record; d fin.rmp_commission_defaults%rowtype; v_id uuid;
begin
  if not fin.rmp_can_use() then raise exception 'Master, Staff or Doctor identity required'; end if;
  if v_branch is null or v_branch not in ('Kishanganj','Jalpaiguri','Cooch Behar','Falakata','Birpara') then
    raise exception 'Invalid branch';
  end if;
  if not fin.rmp_can_write_branch(v_branch) then raise exception 'Not allowed for this branch'; end if;

  for r in
    with cand as (
      select p.id                                as pid,
             coalesce(p."patientId",'')          as pcode,
             coalesce(p.name,'')                 as pname,
             coalesce(p.mobile,'')               as pmobile,
             coalesce(p.branch,'')               as pbranch,
             lower(trim(coalesce(p."refDoctor",''))) as dn,
             lower(trim(coalesce(p."refBy",'')))     as rb,
             right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10) as dm
        from public.patients p
       where lower(trim(coalesce(p.branch,''))) = lower(trim(v_branch))
         -- ⛔ আগে থেকে বাঁধা থাকলে ছোঁয়া হয় না
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
        from cand c
        join docs d
          -- 🔴 তিনটে নিয়মই: নাম (`refDoctor`) · পুরনো নাম (`refBy`) · নম্বর
          on (c.dn <> '' and c.dn = d.nk)
          or (c.rb <> '' and c.rb = d.nk)
          or (length(c.dm) = 10 and c.dm = d.mk)
    )
    select x.*, count(*) over (partition by x.pid) as hits
      from pairs x
     order by x.pid
  loop
    if r.hits > 1 then
      patient_row_id := r.pid; patient_code := r.pcode; patient_name := r.pname;
      rmp_name := r.dname; action := 'AMBIGUOUS';
      return next; continue;
    end if;

    select * into d from fin.rmp_commission_defaults where rmp_id = r.did;
    if not found then
      patient_row_id := r.pid; patient_code := r.pcode; patient_name := r.pname;
      rmp_name := r.dname; action := 'NO_RATE';
      return next; continue;
    end if;

    if p_dry_run then
      patient_row_id := r.pid; patient_code := r.pcode; patient_name := r.pname;
      rmp_name := coalesce(nullif(d.rmp_name,''), r.dname); action := 'WOULD_LINK';
      return next; continue;
    end if;

    insert into fin.rmp_patient_commissions(
      patient_row_id, patient_code, patient_name, patient_mobile, treatment_branch,
      rmp_id, rmp_name, rmp_mobile, commission_mode, commission_value, set_on, set_by)
    values (r.pid, r.pcode, r.pname, r.pmobile, r.pbranch,
            r.did,
            coalesce(nullif(d.rmp_name,''), r.dname),
            coalesce(nullif(d.rmp_mobile,''), r.dmobile),
            d.commission_mode, d.commission_value,
            (now() at time zone 'Asia/Kolkata')::date, hr.my_code())
    on conflict (patient_row_id) do nothing        -- ⛔ কখনো বদলায় না
    returning id into v_id;

    if v_id is null then continue; end if;

    insert into fin.rmp_commission_audit(action, entity_id, old_value, new_value, reason, changed_by)
    select 'AUTOLINK_REFDOCTOR', v_id::text, null, to_jsonb(x),
           'Ref By name matched the RMP directory', hr.my_code()
      from fin.rmp_patient_commissions x where x.id = v_id;

    patient_row_id := r.pid; patient_code := r.pcode; patient_name := r.pname;
    rmp_name := coalesce(nullif(d.rmp_name,''), r.dname); action := 'LINKED';
    return next;
  end loop;
  return;
end $$;

revoke all on function fin.rmp_autolink_refdoctor(text, boolean) from public, anon;
grant execute on function fin.rmp_autolink_refdoctor(text, boolean) to authenticated;

notify pgrst, 'reload schema';
