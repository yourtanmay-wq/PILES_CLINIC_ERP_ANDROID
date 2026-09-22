-- ═══════════════════════════════════════════════════════════════════════════
-- 💰🔒 V1594 (১৭.০৯.২০২৬) — আসল কারণ (TK-নির্দেশে গভীরে যাচাই করে পাওয়া):
--
-- V1132 (০৬.০৯.২০২৬, TK-নির্দেশ: *"তিনি যখন বীরপাড়ায় পেশেন্ট পাঠাবেন,
-- বীরপাড়া ব্রাঞ্চে তাঁর নাম্বার..."*) অনুযায়ী একই RMP (একই নাম/নম্বর)
-- একাধিক ব্রাঞ্চে আলাদা আলাদা সারি (doctor_visits) হিসেবে থাকতে পারেন —
-- প্রতিটা ব্রাঞ্চের কার্ডে শুধু সেই ব্রাঞ্চের রোগীই দেখানোর জন্য। এটাই
-- ইচ্ছাকৃত ব্যবস্থা, ভুল না (AMIT GOLDAR-এর কিশানগঞ্জ ও জলপাইগুড়ি — দুটো
-- সারিই ঠিক আছে, একটাও মোছার দরকার নেই)।
--
-- ⛔ কিন্তু V1132 শুধু **দেখার** তিনটে ফাংশন বদলেছিল (rmp_legacy_view_all_v2 ·
-- rmp_legacy_card_counts · rmp_legacy_performance) — টাকা/কমিশন **জোড়া
-- লাগানোর** ফাংশন (fin.rmp_autolink_refdoctor, V1078/V1082) তখন বদলানো হয়নি।
-- ফল: নাম/নম্বর মিলিয়ে একাধিক RMP-সারি পাওয়া গেলেই ওই ফাংশন "AMBIGUOUS"
-- ধরে চুপচাপ বাদ দিয়ে দেয় — TARIK ANWAR ও DHARAM KUMAR তাই জোড়া লাগেননি।
-- এটাই আমার (আগের কাজের) ভুল — V1132-র নিয়ম একটা জায়গায় বসিয়ে বাকি
-- জায়গায় (autolink) বসানো হয়নি।
--
-- ✅ এই ফাইলের কাজ: autolink-এও V1132-র হুবহু একই ব্রাঞ্চ-নিয়ম যোগ —
-- নাম/নম্বর একাধিক RMP-সারিতে মিললে, রোগীর নিজের ব্রাঞ্চের সাথে মেলা
-- সারিটাই আগে বাছাই হবে (নিশ্চিত হলে আর AMBIGUOUS হবে না, নিজে থেকেই
-- জোড়া লেগে যাবে)। ব্রাঞ্চেও কোনো মিল না পাওয়া গেলে আগের নিয়মই (AMBIGUOUS) —
-- একটাও নতুন আন্দাজ নেই, শুধু নিশ্চিত হলেই এগোয়।
--
-- এই ফাইল চালানোর পর — TARIK ANWAR ও DHARAM KUMAR-সহ ভবিষ্যতের সব এমন
-- রোগী পরের বার RMP পর্দা খোলার সময়ই (স্বয়ংক্রিয়) জোড়া লেগে যাবে —
-- আলাদা করে কিছু করতে হবে না।
--
-- ⛔ কোনো রোগীর বিল/জমা/টাকা ছোঁয়া হয় না · আগে বাঁধা কোনো কমিশন বদলায় না ·
--    হার বাছাইয়ের নিয়ম (ব্রাঞ্চ-নির্দিষ্ট → বৈশ্বিক → স্বয়ংক্রিয় ১০%) অটুট ·
--    নতুন APK লাগে না।
-- ═══════════════════════════════════════════════════════════════════════════
create or replace function fin.rmp_autolink_refdoctor(
  p_branch text, p_dry_run boolean default true)
returns table(
  patient_row_id text, patient_code text, patient_name text,
  rmp_name text, action text)
language plpgsql security definer set search_path = fin, public, hr as $$
#variable_conflict use_column
-- 🔴🔒 V1594 — গভীরে যাচাই করতে গিয়ে আরেকটা লুকানো দোষ পাওয়া গেছে (আলাদা,
-- আগে কখনো ধরা পড়েনি): এই ফাংশনের ফেরত-দেওয়া কলামের নাম (patient_row_id)
-- আর নিচের `on conflict (patient_row_id)`-এর কলামের নাম একই হওয়ায়
-- PostgreSQL/Supabase-এ "column reference is ambiguous" ত্রুটি দিত —
-- অর্থাৎ **নতুন কোনো রোগীই কখনো এই ফাংশন দিয়ে সত্যিই জোড়া লাগত না**,
-- Kotlin-এর try/catch নিঃশব্দে সেই ত্রুটি গিলে ফেলত। এই একটা লাইন
-- (`#variable_conflict use_column`) শুধু এইটুকু ঠিক করে — SQL-এর মধ্যে
-- নাম-দ্বন্দ্ব হলে টেবিলের কলামকেই আগে ধরবে। ফাংশনের বাকি কোনো আচরণ,
-- কোনো টাকার হিসাব বদলায় না।
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
             coalesce(v.branch,'') as dbranch,
             lower(trim(coalesce(v.name,''))) as nk,
             right(regexp_replace(coalesce(v.mobile,''),'[^0-9]','','g'),10) as mk
        from public.doctor_visits v
       where (v.status = 'Active' or v.status is null)
    ), pairs as (
      select distinct c.pid, c.pcode, c.pname, c.pmobile, c.pbranch,
                      d.did, d.dname, d.dmobile, d.dbranch
        from cand c join docs d
          on (c.dn <> '' and c.dn = d.nk)
          or (c.rb <> '' and c.rb = d.nk)
          or (length(c.dm) = 10 and c.dm = d.mk)
    ), branch_match as (
      -- 🔴🔒 V1594 — V1132-র হুবহু একই নিয়ম: নাম/নম্বর একাধিক RMP-সারিতে
      -- মিললে, যে সারিগুলোর নিজের ব্রাঞ্চ রোগীর ব্রাঞ্চের সাথে মেলে
      -- শুধু তাদেরই রাখা হবে (বাকি একই-নামের ভিন্ন-ব্রাঞ্চ সারি বাদ)।
      -- কোনো সারিরই ব্রাঞ্চ না মিললে (আগের মতোই) সবগুলোই থাকবে —
      -- একাধিক থাকলে AMBIGUOUS-ই হবে, নতুন কোনো আন্দাজ নেই।
      select p.*,
        exists(
          select 1 from pairs p2 where p2.pid = p.pid
            and trim(coalesce(p2.pbranch,'')) <> '' and trim(coalesce(p2.dbranch,'')) <> ''
            and lower(trim(p2.pbranch)) = lower(trim(p2.dbranch))
        ) as any_branch_match
      from pairs p
    ), filtered as (
      select pid, pcode, pname, pmobile, pbranch, did, dname, dmobile
        from branch_match
       where not any_branch_match
          or (trim(coalesce(pbranch,'')) <> '' and trim(coalesce(dbranch,'')) <> ''
              and lower(trim(pbranch)) = lower(trim(dbranch)))
    )
    select x.*, count(*) over (partition by x.pid) as hits
      from filtered x order by x.pid
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
           'Ref By name matched the RMP directory (branch-matched, V1594)', hr.my_code()
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
