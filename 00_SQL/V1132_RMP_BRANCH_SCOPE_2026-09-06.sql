-- ═══════════════════════════════════════════════════════════════════════
-- V1132 (০৬.০৯.২০২৬) — TK-নির্দেশ ও অনুমতি
--   *"যে ব্রাঞ্চ থেকে দেখা হবে, সেই ব্রাঞ্চে ওই RMP কতজন পেশেন্ট পাঠিয়েছে
--     সেটাই যেন দেখায়।"*
--   *"তিনি যখন বীরপাড়ায় পেশেন্ট পাঠাবেন, বীরপাড়া ব্রাঞ্চে তাঁর নাম্বার
--     সেভ থাকতে হবে।"*
--
-- ধরা পড়েছিল: কোচবিহারের "TK BISWAS" কার্ডে ১৯ জন — তার ১৭ জনই ফালাকাটার।
-- কারণ নিচের ফাংশনগুলো Master-এর জন্য ব্রাঞ্চ **একেবারেই** ছাঁকত না, আর
-- RMP সারিটার নিজের ব্রাঞ্চ কখনো মেলানোই হত না।
--
-- এই ফাইল শুধু **পড়ার** তিনটে ফাংশন বদলায়:
--   fin.rmp_legacy_view_all_v2 · fin.rmp_legacy_card_counts · fin.rmp_legacy_performance
-- ⛔ কোনো টেবিল · ট্রিগার · policy · সারি · টাকার হিসাব ছোঁয়া হয়নি।
-- ⛔ রোগীর অথবা RMP-র ব্রাঞ্চ ফাঁকা হলে আগের মতোই ধরা হয় — কেউ হারায় না।
-- ═══════════════════════════════════════════════════════════════════════

-- ── ১ · Referred Patient তালিকা ────────────────────────────────────────
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
    -- 🔴 V1132: রোগী দেখাবেন শুধু **এই RMP সারিটার নিজের ব্রাঞ্চের** কার্ডে।
    and (trim(coalesce(p.branch,''))=''
         or trim(coalesce(v_rmp_branch,''))=''
         or lower(trim(p.branch))=lower(trim(v_rmp_branch)))
    and (
      (lower(trim(coalesce(p."refBy",'')))<>'' and lower(trim(p."refBy"))=v_name)
      or
      (length(right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10))=10
       and right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10)=v_mobile)
    )
  order by p."updatedAt" desc nulls last;
end
$$;

revoke all on function fin.rmp_legacy_view_all_v2(text) from public, anon;
grant execute on function fin.rmp_legacy_view_all_v2(text) to authenticated;


-- ── ২ · RMP তালিকার কার্ডে "Referred" সংখ্যা ───────────────────────────
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
       or (length(p.match_mobile)=10 and p.match_mobile=d.match_mobile))
      -- 🔴 V1132: রোগী গোনা হবে শুধু তাঁর নিজের ব্রাঞ্চের RMP সারিতে।
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


-- ── ৩ · RMP Performance ────────────────────────────────────────────────
create or replace function fin.rmp_legacy_performance(p_branch text default null)
returns table(
  rmp_id text, this_month_count bigint, all_time_count bigint,
  referral_paid numeric, most_recent_date text
)
language plpgsql
stable
security definer
set search_path = fin, hr, public
as $$
declare
  v_branch text := nullif(trim(coalesce(p_branch,'')),'');
begin
  if not hr.is_master() then raise exception 'Master only'; end if;
  if v_branch='All' then v_branch:=null; end if;
  if v_branch is not null and v_branch not in
    ('Kishanganj','Jalpaiguri','Cooch Behar','Falakata','Birpara') then
    raise exception 'Invalid branch';
  end if;

  return query
  with doctors as (
    select d.id,fin.rmp_safe_number(d."referralPaid") as paid_value,
      coalesce(d.branch,'') as doc_branch,
      lower(trim(coalesce(d.name,''))) as match_name,
      right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10) as match_mobile
    from public.doctor_visits d
    where (d.status='Active' or d.status is null)
      and (v_branch is null or d.branch=v_branch)
  ), capped_patients as (
    select p.* from public.patients p
    where v_branch is null or p.branch=v_branch
    order by p."updatedAt" desc nulls last limit 5000
  ), matched as (
    select distinct d.id as rmp_id,d.paid_value,p.id as patient_row_id,
      coalesce(nullif(trim(coalesce(p."registrationDate",'')),''),coalesce(p.date,'')) as referral_date
    from doctors d join capped_patients p on
      (    (lower(trim(coalesce(p."refBy",'')))<>'' and lower(trim(p."refBy"))=d.match_name)
        or (length(right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10))=10
            and right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10)=d.match_mobile))
      -- 🔴 V1132: এখানেও রোগী নিজের ব্রাঞ্চের RMP সারিতেই।
      and (trim(coalesce(p.branch,''))='' or trim(d.doc_branch)=''
           or lower(trim(p.branch))=lower(trim(d.doc_branch)))
  )
  select m.rmp_id,
    count(*) filter (where left(m.referral_date,7)=
      to_char(now() at time zone 'Asia/Kolkata','YYYY-MM'))::bigint,
    count(*)::bigint,round(max(m.paid_value),2),coalesce(max(nullif(m.referral_date,'')),'')
  from matched m
  group by m.rmp_id
  order by max(nullif(m.referral_date,'')) desc nulls last,m.rmp_id;
end
$$;

revoke all on function fin.rmp_legacy_performance(text) from public, anon, authenticated;
grant execute on function fin.rmp_legacy_performance(text) to authenticated;

notify pgrst, 'reload schema';

-- ── যাচাই · TK BISWAS-এর তিনটে কার্ডে এখন কতজন ─────────────────────────
select d.branch, d.name, coalesce(c.referred_count,0) as referred
from public.doctor_visits d
left join fin.rmp_legacy_card_counts() c on c.rmp_id = d.id
where right(regexp_replace(coalesce(d.mobile,''),'[^0-9]','','g'),10)='8001080080'
order by d.branch;
