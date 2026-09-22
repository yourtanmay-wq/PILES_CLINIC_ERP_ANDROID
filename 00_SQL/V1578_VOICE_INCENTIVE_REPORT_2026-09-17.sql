-- ═══════════════════════════════════════════════════════════════════════════
-- 🎤🔒 V1578 (১৭.০৯.২০২৬, TK-নির্দেশ "ইনসেন্টিভ শুরু করেন সাবধানে") —
-- ভয়েস-প্রশ্নের বাকি থাকা আইটেম ৩৩: "অসময়ের এনকোয়ারির ইনসেন্টিভ"।
--
-- TK-র দুটো সিদ্ধান্ত (প্রশ্ন করে নেওয়া, আন্দাজ নয়):
--  ১) "এই মাসে" = পেশেন্ট যেদিন রেজিস্ট্রেশন করেছেন সেই দিন ধরে (এনকোয়ারি
--     যখনই হোক থাকুক না কেন) — hr.incentive_wanted()-এর regdate-ই আগে থেকে
--     এই নিয়মেই চলে, তাই এখানে নতুন কিছু বানাতে হয়নি।
--  ২) ব্রাঞ্চ = পেশেন্ট যে ব্রাঞ্চে এসেছেন, সেই ব্রাঞ্চ থেকেই টাকা দেখানো হবে
--     (স্টাফের নিজের হোম-ব্রাঞ্চ নয়)।
--
-- ⛔ এই ফাইল hr.incentive_wanted()-কে **ছোঁয়নি** — ওটাই মাইনে/বেতনের আসল
--    হিসাবে ব্যবহার হয় (hr.incentive_sync), তাতে হাত দেওয়া ঝুঁকিপূর্ণ। এখানে
--    হুবহু সেই একই যুক্তি (pat/pat2/enq/money/who/split — অক্ষরে অক্ষরে কপি
--    করা, নিয়ম বদলানো হয়নি) দিয়ে শুধু **পড়ার জন্য** আলাদা নতুন ফাংশন —
--    সঙ্গে পেশেন্টের ব্রাঞ্চ ও তারিখ-সীমা যোগ করা হয়েছে (ভয়েস-রিপোর্টের
--    বাকি সব প্রশ্নের মতোই একই ছাঁচ — reports.can_access_branch গার্ড,
--    Master-only, শুধু সংখ্যা/অল্প সারি ফেরত, ফ্রি-প্ল্যান-নিরাপদ)।
-- ═══════════════════════════════════════════════════════════════════════════
begin;

create or replace function reports.incentive_summary(p_branch text, p_from date, p_to date)
returns table(total numeric, entry_count int, staff_count int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    with pat as (
      select
        p.id,
        coalesce(nullif(p."patientId", ''), p.id)                                as shown_code,
        p."branch"                                                               as branch,
        right(regexp_replace(coalesce(p.mobile, ''), '\D', '', 'g'), 10)         as pmob,
        right(regexp_replace(coalesce(p."registeredBy", ''), '\D', '', 'g'), 10) as regmob,
        nullif(left(coalesce(nullif(p."registrationDate", ''), p."date", ''), 10), '') as regdate
      from public.patients p
      where coalesce(p."timeType", '') = 'Unexpected Time'
    ), pat2 as (
      select * from pat
      where regdate ~ '^\d{4}-\d{2}-\d{2}$'
        and regdate::date >= date '2026-08-18'
        and regdate::date >= p_from
        and regdate::date <= p_to
        and branch = p_branch
    ), enq as (
      select distinct on (m.id)
        m.id,
        right(regexp_replace(coalesce(e."receivedBy", ''), '\D', '', 'g'), 10) as enqmob
      from pat2 m
      join public.enquiries e
        on right(regexp_replace(coalesce(e.mobile, ''), '\D', '', 'g'), 10) = m.pmob
      order by m.id, coalesce(e."createdAt", e."date", '') desc
    ), money as (
      select
        m.id,
        bool_or(lower(coalesce(y."payType", '')) in ('visit_fee', 'visitfee', 'registration')
                and coalesce(nullif(regexp_replace(coalesce(y."amount", ''), '[^0-9.]', '', 'g'), ''), '0')::numeric > 0) as has_fee,
        bool_or(lower(coalesce(y."payType", '')) = 'treatment'
                and coalesce(nullif(regexp_replace(coalesce(y."amount", ''), '[^0-9.]', '', 'g'), ''), '0')::numeric > 0) as has_trt
      from pat2 m
      join public.payments y on y."patientId" = m.id
      group by m.id
    ), who as (
      select
        m.id, m.shown_code, m.branch,
        sr.person_code as reg_code,
        se.person_code as enq_code,
        coalesce(mn.has_fee, false) as has_fee,
        coalesce(mn.has_trt, false) as has_trt
      from pat2 m
      left join enq q on q.id = m.id
      left join hr.staff_profiles sr
        on length(m.regmob) = 10
       and right(regexp_replace(coalesce(sr.link_mobile, ''), '\D', '', 'g'), 10) = m.regmob
       and sr.active is not false
      left join hr.staff_profiles se
        on length(coalesce(q.enqmob, '')) = 10
       and right(regexp_replace(coalesce(se.link_mobile, ''), '\D', '', 'g'), 10) = q.enqmob
       and se.active is not false
      left join money mn on mn.id = m.id
    ), split as (
      select
        w.id, w.shown_code, w.branch, w.has_fee, c.pc,
        count(*) over (partition by w.id) as n
      from who w
      cross join lateral (
        select distinct x as pc
        from unnest(array[w.reg_code, w.enq_code]) x
        where x is not null and x <> ''
      ) c
    ), rows0 as (
      select s.pc as person_code, round(100::numeric / s.n, 2) as amount
        from split s where s.has_fee
      union all
      select w.enq_code as person_code, 400::numeric as amount
        from who w where w.has_trt and w.enq_code is not null and w.enq_code <> ''
    )
    select coalesce(sum(r.amount), 0), count(*)::int, count(distinct r.person_code)::int
      from rows0 r;
end $$;
revoke all on function reports.incentive_summary(text, date, date) from public, anon;
grant execute on function reports.incentive_summary(text, date, date) to authenticated;

create or replace function reports.incentive_list(p_branch text, p_from date, p_to date)
returns table(person_code text, patient_row_id text, patient_code text, amount numeric, reason text, branch text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    with pat as (
      select
        p.id,
        coalesce(nullif(p."patientId", ''), p.id)                                as shown_code,
        p."branch"                                                               as branch,
        right(regexp_replace(coalesce(p.mobile, ''), '\D', '', 'g'), 10)         as pmob,
        right(regexp_replace(coalesce(p."registeredBy", ''), '\D', '', 'g'), 10) as regmob,
        nullif(left(coalesce(nullif(p."registrationDate", ''), p."date", ''), 10), '') as regdate
      from public.patients p
      where coalesce(p."timeType", '') = 'Unexpected Time'
    ), pat2 as (
      select * from pat
      where regdate ~ '^\d{4}-\d{2}-\d{2}$'
        and regdate::date >= date '2026-08-18'
        and regdate::date >= p_from
        and regdate::date <= p_to
        and branch = p_branch
    ), enq as (
      select distinct on (m.id)
        m.id,
        right(regexp_replace(coalesce(e."receivedBy", ''), '\D', '', 'g'), 10) as enqmob
      from pat2 m
      join public.enquiries e
        on right(regexp_replace(coalesce(e.mobile, ''), '\D', '', 'g'), 10) = m.pmob
      order by m.id, coalesce(e."createdAt", e."date", '') desc
    ), money as (
      select
        m.id,
        bool_or(lower(coalesce(y."payType", '')) in ('visit_fee', 'visitfee', 'registration')
                and coalesce(nullif(regexp_replace(coalesce(y."amount", ''), '[^0-9.]', '', 'g'), ''), '0')::numeric > 0) as has_fee,
        bool_or(lower(coalesce(y."payType", '')) = 'treatment'
                and coalesce(nullif(regexp_replace(coalesce(y."amount", ''), '[^0-9.]', '', 'g'), ''), '0')::numeric > 0) as has_trt
      from pat2 m
      join public.payments y on y."patientId" = m.id
      group by m.id
    ), who as (
      select
        m.id, m.shown_code, m.branch,
        sr.person_code as reg_code,
        se.person_code as enq_code,
        coalesce(mn.has_fee, false) as has_fee,
        coalesce(mn.has_trt, false) as has_trt
      from pat2 m
      left join enq q on q.id = m.id
      left join hr.staff_profiles sr
        on length(m.regmob) = 10
       and right(regexp_replace(coalesce(sr.link_mobile, ''), '\D', '', 'g'), 10) = m.regmob
       and sr.active is not false
      left join hr.staff_profiles se
        on length(coalesce(q.enqmob, '')) = 10
       and right(regexp_replace(coalesce(se.link_mobile, ''), '\D', '', 'g'), 10) = q.enqmob
       and se.active is not false
      left join money mn on mn.id = m.id
    ), split as (
      select
        w.id, w.shown_code, w.branch, w.has_fee, c.pc,
        count(*) over (partition by w.id) as n
      from who w
      cross join lateral (
        select distinct x as pc
        from unnest(array[w.reg_code, w.enq_code]) x
        where x is not null and x <> ''
      ) c
    )
    select s.pc, s.id, s.shown_code, round(100::numeric / s.n, 2), ('Registration · ' || s.shown_code), s.branch
      from split s where s.has_fee
    union all
    select w.enq_code, w.id, w.shown_code, 400::numeric, ('Treatment · ' || w.shown_code), w.branch
      from who w where w.has_trt and w.enq_code is not null and w.enq_code <> ''
    limit 500;
end $$;
revoke all on function reports.incentive_list(text, date, date) from public, anon;
grant execute on function reports.incentive_list(text, date, date) to authenticated;

notify pgrst, 'reload schema';
commit;
