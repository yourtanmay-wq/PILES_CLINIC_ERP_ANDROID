-- ═══════════════════════════════════════════════════════════════════════════
-- 🎤🔒 V1421 (১৩.০৯.২০২৬, TK-নির্দেশ "চালিয়ে যান", বড় ব্যাচে) — ভয়েস-প্রশ্নের
-- জবাব, ধাপ ২ — এক ব্যাচে ৮টা প্যাটার্ন:
--  ২১) কতদিন চেম্বার Close করা হয়নি (আইটেম ২৭)
--  ২২) কতজন আসার কথা ছিল কিন্তু আসেননি — no-show (আইটেম ২৯)
--  ২৩) IN দেওয়া কিন্তু OUT টাইম চাপা হয়নি — কতদিন (আইটেম ৩১-এর OUT অংশ)
--  ২৪) WFH আবেদন (আইটেম ৩২)
--  ২৫) ডুপ্লিকেট রোগীর রেকর্ড (আইটেম ৩৮)
--  ২৬) ভিজিট ফি এখনো জমা পড়েনি (আইটেম ৪১)
--  ২৭) আজ কতগুলো ফলো-আপ কল বাকি (আইটেম ৪৬)
--  ২৮) কতগুলো WhatsApp/SMS বার্তা পাঠানো হয়েছে (আইটেম ৪৪)
--  ⛔ বাদ (আইটেম ২৩, স্টাফ কতগুলো এডিট করেছে): activity_logs টেবিলে আসলে শুধু
--     দুটো জিনিস লেখা হয় (fee_missing/seen ও একটা auto-repair) — এডিটের কোনো
--     লগ নয়; এটা দিয়ে গোনা হলে ভুল (প্রায় শূন্য) সংখ্যা আসত। খাতায় লেখা।
--
-- ⚠️ প্রতিটার নিয়ম অ্যাপের **নিজের পর্দার নিয়মের সাথে হুবহু** মেলানো (নিয়ম ৭ক-২),
-- Explore সাবএজেন্ট দিয়ে কোড মিলিয়ে (আন্দাজ নয়):
-- (ক) চেম্বার-বন্ধ-হয়নি — অ্যাপের ChamberUnclosedRepository কোনো ক্যালেন্ডার দেখে না:
--     যেদিন আসল পেমেন্ট/রোগীর কাজ হয়েছে (chamber_expected বাদ; কেউ এসেছে বা টাকা
--     হয়েছে) অথচ chamber_close-এ সারি নেই — সেটাই "বন্ধ করা হয়নি"; **আজ বাদ**।
-- (খ) no-show — Chamber Attendance বোর্ডের নিয়ম: "আসার কথা" = chamber_expected
--     মার্কার; "এসেছে" = ওই দিনে ওই ব্রাঞ্চে আসল পেমেন্ট-সারি (chamber_expected/
--     bill_edit/refund বাদ) অথবা ওই দিনে রেজিস্ট্রেশন; মেলানো মোবাইলের শেষ ১০ ডিজিটে।
-- (গ) OUT-বাদ — Master-এর রাত ৯টার সতর্কতার (MasterOutTimeWorker) হুবহু নিয়ম:
--     ছুটির দিন বাদ, check_in আছে কিন্তু check_out ফাঁকা (আক্ষরিক "null" লেখাও ফাঁকা)।
--     ⚠️ "IN-ই দেয়নি" অংশটা এখানে নেই — সেটার জন্য স্টাফ-রোস্টার লাগে, পরে।
-- (ঘ) WFH — wfh_requests, kind='wfh' (অন্য-ব্রাঞ্চ-ডিউটির 'branch' সারি বাদ), অনুমোদনের
--     আসল শব্দ 'approved' (ছুটির 'confirmed'-এর সাথে গুলিয়ে ফেলা যাবে না)।
-- (ঙ) ডুপ্লিকেট — Master-এর "Duplicate Check" পর্দার তিনটে নিয়ম: একই মোবাইল ·
--     একই নাম+ব্রাঞ্চ (মোবাইল-গ্রুপে থাকলে বাদ) · একই রোগী+দিন+টাকা+ধরন পেমেন্ট।
--     ⚠️ অ্যাপের পর্দা সব ব্রাঞ্চ একসাথে ও ৫০০০ সারিতে সীমিত — এখানে ব্রাঞ্চ-ধরে,
--     সীমা ছাড়া; তাই সংখ্যা আলাদা হতে পারে (খাতায় লেখা)।
-- (চ) ভিজিট ফি জমা পড়েনি — "Visit Fee Missing" পর্দার হুবহু নিয়ম: ফি = payType
--     visit_fee/visitfee/registration (তিনটেই, V1060), রোগীর সারির id **অথবা** কোড দুটোই
--     মিলিয়ে, একই মোবাইলের যেকোনো সারিতে ফি থাকলে "দেওয়া", আর **০৫.০৯.২০২৬-এর আগের
--     রেজিস্ট্রেশন বাদ** (FEE_GUARD_FROM, অ্যাপের নিজের কঠিন সীমা)।
-- (ছ) ফলো-আপ কল বাকি — V1402-এর লাইভ SQL + V1403-এর "আজই কল হয়ে গেছে" শর্ত + ওয়েবের
--     stage-dedupe (একই মোবাইল একাধিক stage-এ থাকলে একবার, উঁচু stage-টা)। lastCallDate
--     ঘরটা সরাসরি পড়া হয় (ওয়েবের wlv1TodayCallRows-এর মতো)।
-- (জ) বার্তা — message_log টেবিল (অ্যাপ WhatsApp/SMS বোতাম চাপলে লেখে)। ⚠️ এটা
--     "পাঠানোর জন্য খোলা হয়েছে", ডেলিভারি নয়; ডাক্তার/RMP-র বার্তায় branch ফাঁকা
--     থাকে — তাই এখানে শুধু রোগীর বার্তা। এই টেবিলের DDL রিপোতে নেই (TK হাতে
--     বানিয়েছিলেন) — না থাকলে ফাংশন সৎভাবে 'message_log table not found' বলবে।
-- ═══════════════════════════════════════════════════════════════════════════
begin;

-- ── ২১) চেম্বার Close করা হয়নি ─────────────────────────────────────────────
create or replace function reports.chamber_unclosed_list(p_branch text, p_from date, p_to date)
returns table(chamber_date text, arrived int, money numeric)
language plpgsql stable security definer set search_path = hr, public as $$
declare v_to date;
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  v_to := least(p_to, ((now() at time zone 'Asia/Kolkata')::date - 1));
  return query
    with act as (
      select left(coalesce(y."date",''),10) as d,
             count(distinct right(regexp_replace(coalesce(y."mobile",''),'\D','','g'),10))
               filter (where right(regexp_replace(coalesce(y."mobile",''),'\D','','g'),10) <> '')::int as arrived,
             sum(case
                   when lower(coalesce(y."payType",'')) = 'refund' and lower(coalesce(y."refundApprovalStatus",'')) = 'approved'
                     then -coalesce(nullif(regexp_replace(coalesce(y."amount",'0'),'[^0-9.\-]','','g'),'')::numeric,0)
                   when lower(coalesce(y."payType",'')) = 'refund' then 0
                   else coalesce(nullif(regexp_replace(coalesce(y."amount",'0'),'[^0-9.\-]','','g'),'')::numeric,0)
                 end) as money
        from public.payments y
       where lower(trim(coalesce(y."branch",''))) = lower(trim(p_branch))
         and lower(coalesce(y."payType",'')) <> 'chamber_expected'
         and left(coalesce(y."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
         and left(coalesce(y."date",''),10) <= to_char(v_to,'YYYY-MM-DD')
       group by left(coalesce(y."date",''),10)
    )
    select a.d, a.arrived, coalesce(a.money,0)
      from act a
     where (a.arrived > 0 or coalesce(a.money,0) > 0)
       and not exists (select 1 from public.chamber_close c
                        where upper(trim(coalesce(c."branch",''))) = upper(trim(p_branch))
                          and left(coalesce(c."date",''),10) = a.d)
     order by a.d desc
     limit 500;
end $$;
revoke all on function reports.chamber_unclosed_list(text, date, date) from public, anon;
grant execute on function reports.chamber_unclosed_list(text, date, date) to authenticated;

create or replace function reports.chamber_unclosed_summary(p_branch text, p_from date, p_to date)
returns table(day_count int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  return query select count(*)::int from reports.chamber_unclosed_list(p_branch, p_from, p_to);
end $$;
revoke all on function reports.chamber_unclosed_summary(text, date, date) from public, anon;
grant execute on function reports.chamber_unclosed_summary(text, date, date) to authenticated;

-- ── ২২) no-show (আসার কথা ছিল, আসেননি) ──────────────────────────────────────
create or replace function reports.no_show_list(p_branch text, p_from date, p_to date)
returns table(name text, mobile text, expected_on text, arrived boolean)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    with exp as (
      select coalesce(y."name",'') as nm, coalesce(y."mobile",'') as mob,
             right(regexp_replace(coalesce(y."mobile",''),'\D','','g'),10) as m,
             left(coalesce(y."date",''),10) as d
        from public.payments y
       where lower(coalesce(y."payType",'')) = 'chamber_expected'
         and lower(trim(coalesce(y."branch",''))) = lower(trim(p_branch))
         and left(coalesce(y."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
         and left(coalesce(y."date",''),10) <= to_char(p_to,'YYYY-MM-DD')
    ),
    arr as (
      select right(regexp_replace(coalesce(y."mobile",''),'\D','','g'),10) as m, left(coalesce(y."date",''),10) as d
        from public.payments y
       where lower(trim(coalesce(y."branch",''))) = lower(trim(p_branch))
         and lower(coalesce(y."payType",'')) not in ('chamber_expected','bill_edit','refund')
         and left(coalesce(y."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
         and left(coalesce(y."date",''),10) <= to_char(p_to,'YYYY-MM-DD')
      union
      select right(regexp_replace(coalesce(p."mobile",''),'\D','','g'),10), left(coalesce(p."registrationDate",''),10)
        from public.patients p
       where lower(trim(coalesce(p."branch",''))) = lower(trim(p_branch))
         and left(coalesce(p."registrationDate",''),10) >= to_char(p_from,'YYYY-MM-DD')
         and left(coalesce(p."registrationDate",''),10) <= to_char(p_to,'YYYY-MM-DD')
    )
    select e.nm, e.mob, e.d,
           exists (select 1 from arr a where a.m = e.m and a.m <> '' and a.d = e.d)
      from exp e
     order by e.d desc, e.nm
     limit 500;
end $$;
revoke all on function reports.no_show_list(text, date, date) from public, anon;
grant execute on function reports.no_show_list(text, date, date) to authenticated;

create or replace function reports.no_show_summary(p_branch text, p_from date, p_to date)
returns table(no_show int, arrived int, expected_total int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  return query
    select count(*) filter (where not l.arrived)::int, count(*) filter (where l.arrived)::int, count(*)::int
      from reports.no_show_list(p_branch, p_from, p_to) l;
end $$;
revoke all on function reports.no_show_summary(text, date, date) from public, anon;
grant execute on function reports.no_show_summary(text, date, date) to authenticated;

-- ── ২৩) IN দেওয়া, OUT চাপা হয়নি (wn.notebook_days, MasterOutTimeWorker-এর নিয়ম) ──
create or replace function reports.out_missing_list(p_branch text, p_from date, p_to date)
returns table(staff_code text, work_date text, check_in text)
language plpgsql stable security definer set search_path = hr, wn, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select n.staff_code, to_char(n.work_date,'YYYY-MM-DD'), coalesce(n.check_in,'')
      from wn.notebook_days n
      join hr.staff_profiles s on s.person_code = n.staff_code
     where lower(trim(coalesce(s.branch,''))) = lower(trim(p_branch))
       and n.work_date >= p_from and n.work_date <= p_to
       and coalesce(n.is_leave,false) = false
       and coalesce(nullif(lower(trim(coalesce(n.check_in,''))),'null'),'') <> ''
       and coalesce(nullif(lower(trim(coalesce(n.check_out,''))),'null'),'') = ''
     order by n.work_date desc, n.staff_code
     limit 500;
end $$;
revoke all on function reports.out_missing_list(text, date, date) from public, anon;
grant execute on function reports.out_missing_list(text, date, date) to authenticated;

create or replace function reports.out_missing_summary(p_branch text, p_from date, p_to date)
returns table(total int, staff_count int)
language plpgsql stable security definer set search_path = hr, wn, public as $$
begin
  return query select count(*)::int, count(distinct l.staff_code)::int from reports.out_missing_list(p_branch, p_from, p_to) l;
end $$;
revoke all on function reports.out_missing_summary(text, date, date) from public, anon;
grant execute on function reports.out_missing_summary(text, date, date) to authenticated;

-- ── ২৪) WFH আবেদন (requestedAt ধরে; kind='wfh') ──────────────────────────────
create or replace function reports.wfh_summary(p_branch text, p_from date, p_to date)
returns table(total int, approved int, pending int, rejected int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select count(*)::int,
           count(*) filter (where lower(coalesce(w."status",'')) = 'approved')::int,
           count(*) filter (where lower(coalesce(w."status",'')) = 'pending')::int,
           count(*) filter (where lower(coalesce(w."status",'')) = 'rejected')::int
      from public.wfh_requests w
     where lower(trim(coalesce(w."branch",''))) = lower(trim(p_branch))
       and lower(coalesce(w."kind",'wfh')) = 'wfh'
       and left(coalesce(w."requestedAt",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(w."requestedAt",''),10) <= to_char(p_to,'YYYY-MM-DD');
end $$;
revoke all on function reports.wfh_summary(text, date, date) from public, anon;
grant execute on function reports.wfh_summary(text, date, date) to authenticated;

create or replace function reports.wfh_list(p_branch text, p_from date, p_to date)
returns table(staff_name text, staff_code text, work_date text, status text, requested_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select coalesce(w."staffName",''), coalesce(w."staffCode",''), left(coalesce(w."workDate",''),10),
           coalesce(w."status",''), left(coalesce(w."requestedAt",''),10)
      from public.wfh_requests w
     where lower(trim(coalesce(w."branch",''))) = lower(trim(p_branch))
       and lower(coalesce(w."kind",'wfh')) = 'wfh'
       and left(coalesce(w."requestedAt",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(w."requestedAt",''),10) <= to_char(p_to,'YYYY-MM-DD')
     order by w."requestedAt" desc
     limit 500;
end $$;
revoke all on function reports.wfh_list(text, date, date) from public, anon;
grant execute on function reports.wfh_list(text, date, date) to authenticated;

-- ── ২৫) ডুপ্লিকেট রোগীর রেকর্ড (Duplicate Check পর্দার ৩ নিয়ম, ব্রাঞ্চ-ধরে) — স্ন্যাপশট ──
create or replace function reports.duplicate_summary(p_branch text)
returns table(mobile_groups int, name_groups int, payment_groups int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    with pts as (
      select p.id, upper(trim(coalesce(p."name",''))) as nm,
             right(regexp_replace(coalesce(p."mobile",''),'\D','','g'),10) as m,
             upper(trim(coalesce(p."branch",''))) as br
        from public.patients p
       where lower(trim(coalesce(p."branch",''))) = lower(trim(p_branch))
    ),
    mg as (select x.m from pts x where length(x.m) = 10 group by x.m having count(*) > 1),
    ng as (select x.br, x.nm from pts x
            where x.nm <> '' and not exists (select 1 from mg where mg.m = x.m)
            group by x.br, x.nm having count(*) > 1),
    pg as (select 1 as one from public.payments y
            where lower(trim(coalesce(y."branch",''))) = lower(trim(p_branch))
              and lower(coalesce(y."payType",'')) not in ('chamber_expected','bill_edit','attendance_mark')
            group by coalesce(y."patientId",''), left(coalesce(y."date",''),10),
                     coalesce(nullif(regexp_replace(coalesce(y."amount",'0'),'[^0-9.\-]','','g'),'')::numeric,0),
                     lower(coalesce(y."payType",''))
            having count(*) > 1)
    select (select count(*) from mg)::int, (select count(*) from ng)::int, (select count(*) from pg)::int;
end $$;
revoke all on function reports.duplicate_summary(text) from public, anon;
grant execute on function reports.duplicate_summary(text) to authenticated;

create or replace function reports.duplicate_list(p_branch text)
returns table(mobile text, row_count int, names text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select x.m, count(*)::int, string_agg(distinct coalesce(p2."name",''), ' / ')
      from (select p.id, right(regexp_replace(coalesce(p."mobile",''),'\D','','g'),10) as m
              from public.patients p
             where lower(trim(coalesce(p."branch",''))) = lower(trim(p_branch))) x
      join public.patients p2 on p2.id = x.id
     where length(x.m) = 10
     group by x.m having count(*) > 1
     order by count(*) desc, x.m
     limit 500;
end $$;
revoke all on function reports.duplicate_list(text) from public, anon;
grant execute on function reports.duplicate_list(text) to authenticated;

-- ── ২৬) ভিজিট ফি এখনো জমা পড়েনি (Visit Fee Missing পর্দার নিয়ম) — স্ন্যাপশট ─────
create or replace function reports.fee_unpaid_list(p_branch text)
returns table(patient_row_id text, patient_code text, name text, mobile text, registration_date text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    with fee as (
      select distinct nullif(y."patientId",'') as pid from public.payments y
       where lower(coalesce(y."payType",'')) in ('visit_fee','visitfee','registration')
         and nullif(y."patientId",'') is not null
    ),
    pts as (
      select p.id, coalesce(p."patientId",'') as code, coalesce(p."name",'') as nm, coalesce(p."mobile",'') as mob,
             left(coalesce(nullif(p."registrationDate",''), p."date", ''),10) as rd,
             coalesce(nullif(right(regexp_replace(coalesce(p."mobile",''),'\D','','g'),10),''), p.id) as grp
        from public.patients p
       where lower(trim(coalesce(p."branch",''))) = lower(trim(p_branch))
    ),
    paid as (
      select distinct x.grp from pts x
       where exists (select 1 from fee where fee.pid = x.id or fee.pid = nullif(x.code,''))
    ),
    miss as (
      select distinct on (x.grp) x.id, x.code, x.nm, x.mob, x.rd
        from pts x
       where x.grp not in (select grp from paid)
         and not (x.rd ~ '^\d{4}-\d{2}-\d{2}$' and x.rd < '2026-09-05')
       order by x.grp, x.rd desc
    )
    select mi.id, mi.code, mi.nm, mi.mob, mi.rd from miss mi
     order by mi.rd desc, mi.nm
     limit 500;
end $$;
revoke all on function reports.fee_unpaid_list(text) from public, anon;
grant execute on function reports.fee_unpaid_list(text) to authenticated;

create or replace function reports.fee_unpaid_summary(p_branch text)
returns table(total int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  return query select count(*)::int from reports.fee_unpaid_list(p_branch);
end $$;
revoke all on function reports.fee_unpaid_summary(text) from public, anon;
grant execute on function reports.fee_unpaid_summary(text) to authenticated;

-- ── ২৭) আজ কতগুলো ফলো-আপ কল বাকি (V1402 SQL + V1403 শর্ত + stage-dedupe) — স্ন্যাপশট ──
create or replace function reports.calls_pending_list(p_branch text)
returns table(followup_id text, name text, mobile text, stage text, next_follow text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    with base as (
      select f.id, coalesce(f."name",'') as nm, coalesce(f."mobile",'') as mob, coalesce(f."stage",'') as st,
             left(coalesce(f."nextFollow",''),10) as nf,
             coalesce(nullif(right(regexp_replace(coalesce(f."mobile",''),'\D','','g'),10),''), f.id) as grp,
             case coalesce(f."stage",'') when 'Treatment' then 3 when 'Patient' then 2 when 'Inquiry' then 1 else 0 end as rnk
        from public.followups f
       where lower(trim(coalesce(f."branch",''))) = lower(trim(p_branch))
         and coalesce(f."stage",'') in ('Inquiry','Patient','Treatment')
         and coalesce(f."status",'') not in ('Cancelled','Incomplete','Rejected','Closed')
         and coalesce(f."noMoreCalls", false) = false
         and coalesce(f."nextFollow",'') <> ''
         and left(f."nextFollow",10) <= to_char(now() at time zone 'Asia/Kolkata','YYYY-MM-DD')
         and not (coalesce(f."lastCallDate",'') <> '' and left(f."lastCallDate",10) >= left(f."nextFollow",10))
    ),
    dedup as (select distinct on (b.grp) b.id, b.nm, b.mob, b.st, b.nf from base b order by b.grp, b.rnk desc)
    select d.id, d.nm, d.mob, d.st, d.nf from dedup d
     order by d.nf, d.nm
     limit 500;
end $$;
revoke all on function reports.calls_pending_list(text) from public, anon;
grant execute on function reports.calls_pending_list(text) to authenticated;

create or replace function reports.calls_pending_summary(p_branch text)
returns table(total int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  return query select count(*)::int from reports.calls_pending_list(p_branch);
end $$;
revoke all on function reports.calls_pending_summary(text) from public, anon;
grant execute on function reports.calls_pending_summary(text) to authenticated;

-- ── ২৮) WhatsApp/SMS বার্তা (message_log, শুধু রোগীর বার্তা) ────────────────────
create or replace function reports.messages_summary(p_branch text, p_from date, p_to date)
returns table(total int, whatsapp int, sms int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  if to_regclass('public.message_log') is null then raise exception 'message_log table not found'; end if;
  return query execute
    'select count(*)::int,
            count(*) filter (where lower(coalesce(channel,'''')) = ''whatsapp'')::int,
            count(*) filter (where lower(coalesce(channel,'''')) = ''sms'')::int
       from public.message_log m
      where lower(trim(coalesce(m.branch,''''))) = lower(trim($1))
        and (m.sent_at at time zone ''Asia/Kolkata'')::date >= $2
        and (m.sent_at at time zone ''Asia/Kolkata'')::date <= $3'
    using p_branch, p_from, p_to;
end $$;
revoke all on function reports.messages_summary(text, date, date) from public, anon;
grant execute on function reports.messages_summary(text, date, date) to authenticated;

create or replace function reports.messages_list(p_branch text, p_from date, p_to date)
returns table(name text, mobile text, kind text, channel text, sent_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  if to_regclass('public.message_log') is null then raise exception 'message_log table not found'; end if;
  return query execute
    'select coalesce(m.name,''''), coalesce(m.mobile,''''), coalesce(m.kind,''''), coalesce(m.channel,''''),
            to_char(m.sent_at at time zone ''Asia/Kolkata'',''YYYY-MM-DD'')
       from public.message_log m
      where lower(trim(coalesce(m.branch,''''))) = lower(trim($1))
        and (m.sent_at at time zone ''Asia/Kolkata'')::date >= $2
        and (m.sent_at at time zone ''Asia/Kolkata'')::date <= $3
      order by m.sent_at desc
      limit 500'
    using p_branch, p_from, p_to;
end $$;
revoke all on function reports.messages_list(text, date, date) from public, anon;
grant execute on function reports.messages_list(text, date, date) to authenticated;

notify pgrst, 'reload schema';
commit;
