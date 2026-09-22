-- ═══════════════════════════════════════════════════════════════════════════
-- V1356 (১১.০৯.২০২৬) — TK-নির্দেশ, ছবিসহ (তালিকা ৪৫৫ · ৪৫৬)
--
--   ১) TK: *"Bill বসানোর সাথে কমিশনের ব্যাপার তো থাকার কথা না। এটা ঠিক করুন আগে"*
--      আগে (V325→V941): Final Bill ফাঁকা/০ হলে কমিশন **সবসময় ০**, আর জমা
--      বিলের বেশি হলে বিল পর্যন্তই গোনা হত। এখন **শতাংশ (PERCENT) হারে** কমিশন
--      = চিকিৎসা-জমা × % — বিল থাকুক বা না থাকুক। হার-বদলের (V941) হিসাব
--      অটুট: বদলের আগের জমা পুরনো %, পরের জমা নতুন %।
--      ⛔ টাকার-অঙ্ক (AMOUNT) হারে বিল ছাড়া ভাগ করা সম্ভব নয় — সেই নিয়ম
--         হুবহু আগের মতোই (V941) রাখা হলো।
--
--   ২) TK: JH MANDAL-এর কার্ডে *Referred 0 · Total Entries 0* অথচ Ref. Due ₹2,798।
--      কারণ: তালিকা (`rmp_legacy_view_all_v2`) ও কার্ডের সংখ্যা
--      (`rmp_legacy_card_counts`) শুধু `refBy`-র নাম বা `refDoctorMobile` মেলাত;
--      আজকের অ্যাপ নামটা রাখে `refDoctor`-এ (`refBy`-তে থাকে শুধু "Dr. Visit")।
--      অথচ কমিশন-বাঁধার কাজ (V489/V1078) `refDoctor`-ও মেলায় — তাই টাকা গোনা
--      হত, রোগী তালিকায় দেখাত না। এখন `refDoctor`-ও মেলে, আর যে রোগীর সঙ্গে
--      এই RMP-র কমিশন **বাঁধা আছে** সে সবসময়ই তালিকায় থাকে।
--
-- ⛔ কোনো টেবিল · সারি · টাকার রেকর্ড লেখা/বদলানো হয় না — শুধু ৪টা **পড়ার**
--    ফাংশন বদলায়। RMP Due List · Ref. Due · দিনের কমিশন · কমিশন-শীট — সবই
--    এই একই দুটো হিসাব-ফাংশন ডাকে, তাই সব পর্দায় একসাথে একই নিয়ম।
--
-- চালানোর নিয়ম: Supabase → SQL Editor → New query → পুরো ফাইল পেস্ট → Run।
-- ═══════════════════════════════════════════════════════════════════════════

-- ── ১ · কমিশন গোনার একটাই নিয়ম — PERCENT-এ বিল লাগে না ───────────────────
create or replace function fin.rmp_earned_for(
  p_patient_row_id text, p_bill numeric,
  p_mode text, p_value numeric,
  p_prev_mode text, p_prev_value numeric, p_changed_on date)
returns numeric language plpgsql stable security definer
set search_path = fin, public, hr as $$
declare v_before numeric; v_after numeric; v_eb numeric; v_ea numeric;
        v_pm text; v_pv numeric; v_mode text := upper(coalesce(p_mode,'PERCENT'));
begin
  v_pm := upper(coalesce(nullif(trim(coalesce(p_prev_mode,'')),''), p_mode, 'PERCENT'));
  v_pv := coalesce(p_prev_value, p_value, 0);

  -- 🔴 V1356: শতাংশ হারে (এখনকার ও আগের — দুটোই PERCENT) বিলের সঙ্গে সম্পর্ক নেই
  if v_mode = 'PERCENT' and (p_changed_on is null or v_pm = 'PERCENT') then
    if p_changed_on is null then
      return fin.rmp_net_paid_between(p_patient_row_id, null, null) * coalesce(p_value,0) / 100;
    end if;
    v_before := fin.rmp_net_paid_between(p_patient_row_id, null, p_changed_on - 1);
    v_after  := fin.rmp_net_paid_between(p_patient_row_id, p_changed_on, null);
    return v_before * v_pv / 100 + v_after * coalesce(p_value,0) / 100;
  end if;

  -- AMOUNT হার (বা AMOUNT থেকে বদল) — V941-এর নিয়ম হুবহু, বিল লাগে
  if p_bill is null or p_bill <= 0 then return 0; end if;

  if p_changed_on is null then
    v_after := fin.rmp_net_paid_between(p_patient_row_id, null, null);
    v_ea := least(v_after, p_bill);
    if v_mode = 'PERCENT'
      then return v_ea * coalesce(p_value,0) / 100;
      else return coalesce(p_value,0) * v_ea / p_bill; end if;
  end if;

  v_before := fin.rmp_net_paid_between(p_patient_row_id, null, p_changed_on - 1);
  v_after  := fin.rmp_net_paid_between(p_patient_row_id, p_changed_on, null);
  v_eb := least(v_before, p_bill);
  v_ea := least(v_after, greatest(0, p_bill - v_eb));

  return (case when v_pm = 'PERCENT' then v_eb * v_pv / 100 else v_pv * v_eb / p_bill end)
       + (case when v_mode = 'PERCENT'
               then v_ea * coalesce(p_value,0) / 100
               else coalesce(p_value,0) * v_ea / p_bill end);
end $$;

revoke all on function fin.rmp_earned_for(text,numeric,text,numeric,text,numeric,date) from public, anon;
grant execute on function fin.rmp_earned_for(text,numeric,text,numeric,text,numeric,date) to authenticated;


-- ── ২ · একটা তারিখ পর্যন্ত অর্জিত (দিনের কমিশনের জন্য) — একই নিয়ম ─────────
create or replace function fin.rmp_earned_upto(
  p_patient_row_id text, p_bill numeric,
  p_mode text, p_value numeric,
  p_prev_mode text, p_prev_value numeric, p_changed_on date, p_upto date)
returns numeric language plpgsql stable security definer
set search_path = fin, public, hr as $$
declare v_before numeric; v_after numeric; v_eb numeric; v_ea numeric;
        v_pm text; v_pv numeric; v_mode text := upper(coalesce(p_mode,'PERCENT'));
begin
  if p_upto is null then return 0; end if;
  v_pm := upper(coalesce(nullif(trim(coalesce(p_prev_mode,'')),''), p_mode, 'PERCENT'));
  v_pv := coalesce(p_prev_value, p_value, 0);

  -- 🔴 V1356: শতাংশ হারে বিলের সঙ্গে সম্পর্ক নেই
  if v_mode = 'PERCENT' and (p_changed_on is null or v_pm = 'PERCENT') then
    if p_changed_on is null then
      return fin.rmp_net_paid_between(p_patient_row_id, null, p_upto) * coalesce(p_value,0) / 100;
    end if;
    if p_upto < p_changed_on then
      return fin.rmp_net_paid_between(p_patient_row_id, null, p_upto) * v_pv / 100;
    end if;
    v_before := fin.rmp_net_paid_between(p_patient_row_id, null, p_changed_on - 1);
    v_after  := fin.rmp_net_paid_between(p_patient_row_id, p_changed_on, p_upto);
    return v_before * v_pv / 100 + v_after * coalesce(p_value,0) / 100;
  end if;

  -- AMOUNT হার — V1083-এর নিয়ম হুবহু, বিল লাগে
  if p_bill is null or p_bill <= 0 then return 0; end if;

  if p_changed_on is null then
    v_ea := least(fin.rmp_net_paid_between(p_patient_row_id, null, p_upto), p_bill);
    if v_mode = 'PERCENT'
      then return v_ea * coalesce(p_value,0) / 100;
      else return coalesce(p_value,0) * v_ea / p_bill; end if;
  end if;

  if p_upto < p_changed_on then
    v_eb := least(fin.rmp_net_paid_between(p_patient_row_id, null, p_upto), p_bill);
    if v_pm = 'PERCENT' then return v_eb * v_pv / 100;
                        else return v_pv * v_eb / p_bill; end if;
  end if;

  v_before := fin.rmp_net_paid_between(p_patient_row_id, null, p_changed_on - 1);
  v_after  := fin.rmp_net_paid_between(p_patient_row_id, p_changed_on, p_upto);
  v_eb := least(v_before, p_bill);
  v_ea := least(v_after, greatest(0, p_bill - v_eb));

  return (case when v_pm = 'PERCENT' then v_eb * v_pv / 100 else v_pv * v_eb / p_bill end)
       + (case when v_mode = 'PERCENT'
               then v_ea * coalesce(p_value,0) / 100
               else coalesce(p_value,0) * v_ea / p_bill end);
end $$;

revoke all on function fin.rmp_earned_upto(text,numeric,text,numeric,text,numeric,date,date) from public, anon;
grant execute on function fin.rmp_earned_upto(text,numeric,text,numeric,text,numeric,date,date) to authenticated;


-- ── ৩ · Referred Patient তালিকা — refDoctor + কমিশন-বাঁধা রোগীও ────────────
create or replace function fin.rmp_legacy_view_all_v2(p_rmp_id text)
returns table(
  patient_row_id text, patient_code text, patient_name text,
  patient_mobile text, referral_date text, bill numeric, paid numeric,
  disease text
)
language plpgsql
stable
security definer
set search_path = fin, hr, public
as $$
declare
  v_master boolean := hr.is_master();
  v_branch text := '';
  v_name text := '';
  v_mobile text := '';
  v_rmp_branch text := '';
begin
  if not fin.rmp_can_use() then
    raise exception 'Master, Staff or Doctor identity required';
  end if;
  if trim(coalesce(p_rmp_id,''))='' then raise exception 'RMP is required'; end if;

  select lower(trim(coalesce(d.name,''))),
         right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10),
         coalesce(d.branch,'')
    into v_name,v_mobile,v_rmp_branch
  from public.doctor_visits d where d.id=p_rmp_id;
  if not found then raise exception 'RMP not found'; end if;

  if not v_master then
    select coalesce(s.branch,'') into v_branch
    from hr.staff_profiles s
    where s.person_code=hr.my_code() and s.active=true
    limit 1;
    if not found or trim(v_branch)='' then raise exception 'Active staff branch is required'; end if;
    if lower(trim(v_rmp_branch))<>lower(trim(v_branch)) then
      raise exception 'Not allowed for this RMP branch';
    end if;
  end if;

  return query
  with capped_patients as (
    select p.* from public.patients p
    order by p."updatedAt" desc nulls last limit 5000
  ), capped_payments as (
    select p.* from public.payments p
    order by p."updatedAt" desc nulls last limit 5000
  ), paid_by_mobile as (
    select right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) as mobile_key,
      sum(case
        when coalesce(p."payType",'') in ('visit_fee','attendance_mark') then 0
        when lower(coalesce(p."payType",''))='refund'
         and lower(coalesce(p."refundApprovalStatus",''))='approved'
          then -fin.rmp_safe_number(p.amount)
        when lower(coalesce(p."payType",''))='refund' then 0
        else fin.rmp_safe_number(p.amount)
      end) as paid_value
    from capped_payments p
    group by right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10)
  )
  select
    p.id,coalesce(p."patientId",''),coalesce(p.name,''),
    right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10),
    coalesce(nullif(trim(coalesce(p."registrationDate",'')),''),coalesce(p.date,'')),
    round(fin.rmp_safe_number(p.bill),2),round(coalesce(pm.paid_value,0),2),
    coalesce(nullif(trim(coalesce(p.disease,'')),''),nullif(trim(coalesce(p.diagnosis,'')),''),'')
  from capped_patients p
  left join paid_by_mobile pm on pm.mobile_key=
    right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10)
  where (v_master or trim(coalesce(p.branch,''))=''
         or lower(trim(p.branch))=lower(trim(v_branch)))
    -- V1132: রোগী দেখাবেন শুধু এই RMP সারিটার নিজের ব্রাঞ্চের কার্ডে।
    and (trim(coalesce(p.branch,''))=''
         or trim(coalesce(v_rmp_branch,''))=''
         or lower(trim(p.branch))=lower(trim(v_rmp_branch)))
    and (
      (lower(trim(coalesce(p."refBy",'')))<>'' and lower(trim(p."refBy"))=v_name)
      or
      -- 🔴 V1356: আজকের অ্যাপ ডাক্তারের নাম রাখে refDoctor-এ
      (lower(trim(coalesce(p."refDoctor",'')))<>'' and lower(trim(p."refDoctor"))=v_name)
      or
      (length(right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10))=10
       and right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10)=v_mobile)
      or
      -- 🔴 V1356: যার সঙ্গে এই RMP-র কমিশন বাঁধা, সে টাকার হিসাবে আছে ⇒ তালিকাতেও থাকবে
      exists(select 1 from fin.rmp_patient_commissions c
              where c.patient_row_id=p.id and c.rmp_id=p_rmp_id)
    )
  order by p."updatedAt" desc nulls last;
end
$$;

revoke all on function fin.rmp_legacy_view_all_v2(text) from public, anon;
grant execute on function fin.rmp_legacy_view_all_v2(text) to authenticated;


-- ── ৪ · RMP কার্ডের "Referred" সংখ্যা — একই মেলানোর নিয়ম ───────────────────
create or replace function fin.rmp_legacy_card_counts()
returns table(rmp_id text, referred_count bigint)
language plpgsql
stable
security definer
set search_path = fin, hr, public
as $$
declare
  v_master boolean := hr.is_master();
  v_branch text := '';
begin
  if not fin.rmp_can_use() then
    raise exception 'Master, Staff or Doctor identity required';
  end if;

  if not v_master then
    select coalesce(s.branch,'') into v_branch
      from hr.staff_profiles s
     where s.person_code=hr.my_code() and s.active=true
     limit 1;
    if not found or trim(v_branch)='' then
      raise exception 'Active staff branch is required';
    end if;
  end if;

  return query
  with doctor_rows as (
    select
      d.id,
      coalesce(d.branch,'') as doc_branch,
      lower(trim(coalesce(d.name,''))) as match_name,
      right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) as match_mobile
    from public.doctor_visits d
    where (d.status='Active' or d.status is null)
      and (v_master or lower(trim(coalesce(d.branch,'')))=lower(trim(v_branch)))
  ), patient_rows as (
    select
      p.id,
      coalesce(p.branch,'') as pat_branch,
      lower(trim(coalesce(p."refBy",''))) as match_name,
      lower(trim(coalesce(p."refDoctor",''))) as match_doc,      -- 🔴 V1356
      right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10) as match_mobile
    from public.patients p
    where v_master
       or trim(coalesce(p.branch,''))=''
       or lower(trim(p.branch))=lower(trim(v_branch))
  ), matched as (
    select distinct d.id as rmp_id, p.id as patient_row_id
    from doctor_rows d
    join patient_rows p on
      (   (p.match_name<>'' and p.match_name=d.match_name)
       or (p.match_doc<>'' and p.match_doc=d.match_name)          -- 🔴 V1356
       or (length(p.match_mobile)=10 and p.match_mobile=d.match_mobile)
       or exists(select 1 from fin.rmp_patient_commissions c        -- 🔴 V1356
                  where c.patient_row_id=p.id and c.rmp_id=d.id))
      -- V1132: রোগী গোনা হবে শুধু তাঁর নিজের ব্রাঞ্চের RMP সারিতে।
      and (trim(p.pat_branch)='' or trim(d.doc_branch)=''
           or lower(trim(p.pat_branch))=lower(trim(d.doc_branch)))
  )
  select m.rmp_id,count(*)::bigint
  from matched m
  group by m.rmp_id;
end
$$;

revoke all on function fin.rmp_legacy_card_counts() from public, anon;
grant execute on function fin.rmp_legacy_card_counts() to authenticated;

notify pgrst, 'reload schema';
