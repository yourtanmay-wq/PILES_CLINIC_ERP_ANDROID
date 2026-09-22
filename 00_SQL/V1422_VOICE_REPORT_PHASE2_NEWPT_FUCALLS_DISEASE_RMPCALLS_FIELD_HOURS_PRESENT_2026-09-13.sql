-- ═══════════════════════════════════════════════════════════════════════════
-- 🎤🔒 V1422 (১৩.০৯.২০২৬, TK-নির্দেশ "চালিয়ে যান", বড় ব্যাচে) — ভয়েস-প্রশ্নের
-- জবাব, ধাপ ২ — এক ব্যাচে ৮টা প্যাটার্ন:
--  ২৯) কতজন নতুন রোগী, ট্রিটমেন্ট শুরু হয়নি (আইটেম ৬)
--  ৩০) কতগুলো ফলো-আপ কল করা হয়েছে (আইটেম ৭)
--  ৩১) রোগ-ভিত্তিক রোগীর সংখ্যা — Piles/Fissure/Fistula/Hydrocele/Gupt Rog (আইটেম ১৩)
--  ৩২) কতজন RMP ডাক্তারকে কল করা হয়েছে (আইটেম ৩৫-এর কল অংশ)
--  ৩৩) কতজন RMP-কে কল করার কথা (আইটেম ৩৭)
--  ৩৪) ফিল্ড স্টাফ কতগুলো ভিজিট মার্ক করেছেন / কত কিমি (আইটেম ৩৬)
--  ৩৫) স্টাফদের মোট কত ঘণ্টা কাজ (আইটেম ৩০, ব্রাঞ্চ-স্তরে)
--  ৩৬) কোন কোন স্টাফ হাজির ছিল (আইটেম ৯/১৬)
--  ⛔ বাদ (আইটেম ৩৩, অসময়ের এনকোয়ারির ইনসেন্টিভ): হিসাবটা hr.incentive_wanted()-এ
--     আগে থেকেই আছে ও hr.salary_payments-এ জমা হয়, কিন্তু "এই মাসে" মানে কোন মাস
--     (paid_on = sync-এর দিন, অর্জনের দিন নয়) ও ব্রাঞ্চ (স্টাফের না রোগীর) দুটোই
--     দ্বিধার — স্টাফের টাকা নিয়ে আন্দাজ নয়, TK-র সিদ্ধান্তের পরে।
--
-- ⚠️ যাচাই করে পাওয়া (আন্দাজ নয়, Explore সাবএজেন্ট দিয়ে কোড মিলিয়ে):
-- (ক) "ট্রিটমেন্ট শুরু" — patients.stage='Treatment Running' ভরসাযোগ্য নয় (V460-এর পর
--     প্রতিটা চেকআপ সেভেই বসে যায়); doctorComplete-ও নয়। অ্যাপের নিজের টাকার নিয়ম
--     (V420 staff_performance-এর treatment_count): treatment-ধরনের পেমেন্ট >0 থাকলেই
--     শুরু। এখানে তাই — রোগীর সারির id **বা** কোড দুটোই মিলিয়ে (fee-র নিয়মের মতো)।
-- (খ) ফলো-আপ কল "করা" — অ্যাপে রিমার্ক লেখা = কল (V1149); callCount ০-৫ সীমার সংকেত,
--     যোগ করা যায় না। তাই followups.history থেকে অ্যাপের callsFromHistory-র নিয়মে:
--     src<>'treat', রিমার্ক ফাঁকা নয়, তিনটে auto-stub লেখা বাদ, **দিনে একবার** (B53)।
--     ⚠️ এটা "রিমার্ক লেখা হয়েছে" — ডায়াল করা কল নয় (ডায়ালার = call_count, V1419)।
-- (গ) রোগ — enquiries/patients.disease-এ একাধিক রোগ ", " দিয়ে জোড়া থাকতে পারে (V1000),
--     তাই ilike '%X%'; disease ফাঁকা হলে diagnosis (অ্যাপের fallback)। একই রোগীর দুটো রোগ
--     থাকলে রোগ-ধরে যোগফল মোট রোগীর চেয়ে বেশি হতে পারে — উত্তরে মোটও দেখানো হয়।
-- (ঘ) RMP কল — public.doctor_visits.lastCallDate (ডাক্তার-প্রতি একটাই সারি, দিনে দুবার কল
--     = একবার); "কল করার কথা" = nextCallDate (Dr. Visit পর্দার "Today Call" কার্ডের নিয়ম)।
-- (ঙ) ফিল্ড ভিজিট — wn.doctor_visits (MARK VISIT) ও wn.field_visit_days (distance_m মিটারে);
--     এ দুটোর DDL রিপোতে নেই (হাতে বানানো) — না থাকলে সৎভাবে 'table not found'। ফিচারটা
--     শুধু দুই ফিল্ড-স্টাফের (RUPAM, ARMAN) জন্য — অন্য ব্রাঞ্চে ০ আসাই স্বাভাবিক।
-- (চ) ঘণ্টা — Attendance Sheet/HourSalary-র হুবহু ক্রম: ছুটি→৭ঘ, WFH→৭ঘ, অন্য-ব্রাঞ্চ→৭ঘ,
--     IN আছে OUT নেই→৭ঘ, IN নেই বা OUT≤IN→০, নইলে OUT−IN (সীমা নেই)। ব্রাঞ্চ = স্টাফের
--     হোম-ব্রাঞ্চ (hr.staff_profiles), অ্যাপের শিটের মতোই।
-- (ছ) হাজির — ওই দিনে notebook_days-এ বৈধ check_in থাকলেই হাজির (ছুটি/WFH আলাদা ঘরে)।
-- ═══════════════════════════════════════════════════════════════════════════
begin;

-- ── ২৯) নতুন রোগী — ট্রিটমেন্ট শুরু হয়নি ─────────────────────────────────────
create or replace function reports.new_patients_list(p_branch text, p_from date, p_to date)
returns table(patient_row_id text, patient_code text, name text, mobile text, registration_date text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p.id, coalesce(p."patientId",''), coalesce(p."name",''), coalesce(p."mobile",''),
           left(coalesce(nullif(p."registrationDate",''), p."date", ''),10)
      from public.patients p
     where lower(trim(coalesce(p."branch",''))) = lower(trim(p_branch))
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''),10) <= to_char(p_to,'YYYY-MM-DD')
       and not exists (select 1 from public.payments y
                        where (y."patientId" = p.id or y."patientId" = nullif(p."patientId",''))
                          and lower(coalesce(y."payType",'')) = 'treatment'
                          and coalesce(nullif(regexp_replace(coalesce(y."amount",'0'),'[^0-9.\-]','','g'),'')::numeric,0) > 0)
     order by left(coalesce(nullif(p."registrationDate",''), p."date", ''),10) desc, p."name"
     limit 500;
end $$;
revoke all on function reports.new_patients_list(text, date, date) from public, anon;
grant execute on function reports.new_patients_list(text, date, date) to authenticated;

create or replace function reports.new_patients_count(p_branch text, p_from date, p_to date)
returns table(total int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  return query select count(*)::int from reports.new_patients_list(p_branch, p_from, p_to);
end $$;
revoke all on function reports.new_patients_count(text, date, date) from public, anon;
grant execute on function reports.new_patients_count(text, date, date) to authenticated;

-- ── ৩০) ফলো-আপ কল করা হয়েছে (history থেকে, দিনে একবার) ──────────────────────
create or replace function reports.followup_calls_done_list(p_branch text, p_from date, p_to date)
returns table(followup_id text, name text, mobile text, call_day text, remarks int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select f.id, coalesce(f."name",''), coalesce(f."mobile",''), left(coalesce(h->>'date',''),10), count(*)::int
      from public.followups f
      cross join lateral jsonb_array_elements(case when jsonb_typeof(f."history") = 'array' then f."history" else '[]'::jsonb end) h
     where lower(trim(coalesce(f."branch",''))) = lower(trim(p_branch))
       and lower(coalesce(h->>'src','')) <> 'treat'
       and btrim(coalesce(h->>'remark','')) <> ''
       and lower(btrim(coalesce(h->>'remark',''))) not in
           ('registered patient / visit created','treatment payment / advance received','enquiry (syncing…)')
       and left(coalesce(h->>'date',''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(h->>'date',''),10) <= to_char(p_to,'YYYY-MM-DD')
     group by f.id, f."name", f."mobile", left(coalesce(h->>'date',''),10)
     order by left(coalesce(h->>'date',''),10) desc, f."name"
     limit 500;
end $$;
revoke all on function reports.followup_calls_done_list(text, date, date) from public, anon;
grant execute on function reports.followup_calls_done_list(text, date, date) to authenticated;

create or replace function reports.followup_calls_done_summary(p_branch text, p_from date, p_to date)
returns table(total int, patient_count int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  return query select count(*)::int, count(distinct l.followup_id)::int from reports.followup_calls_done_list(p_branch, p_from, p_to) l;
end $$;
revoke all on function reports.followup_calls_done_summary(text, date, date) from public, anon;
grant execute on function reports.followup_calls_done_summary(text, date, date) to authenticated;

-- ── ৩১) রোগ-ভিত্তিক রোগীর সংখ্যা ──────────────────────────────────────────────
create or replace function reports.disease_list(p_branch text, p_from date, p_to date, p_disease text)
returns table(patient_row_id text, patient_code text, name text, mobile text, disease text, registration_date text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if p_disease not in ('Piles','Fissure','Fistula','Hydrocele','Gupt Rog','Other') then raise exception 'Unknown disease'; end if;
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p.id, coalesce(p."patientId",''), coalesce(p."name",''), coalesce(p."mobile",''),
           coalesce(nullif(p."disease",''), p."diagnosis", ''),
           left(coalesce(nullif(p."registrationDate",''), p."date", ''),10)
      from public.patients p
     where lower(trim(coalesce(p."branch",''))) = lower(trim(p_branch))
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''),10) <= to_char(p_to,'YYYY-MM-DD')
       and coalesce(nullif(p."disease",''), p."diagnosis", '') ilike '%' || p_disease || '%'
     order by left(coalesce(nullif(p."registrationDate",''), p."date", ''),10) desc, p."name"
     limit 500;
end $$;
revoke all on function reports.disease_list(text, date, date, text) from public, anon;
grant execute on function reports.disease_list(text, date, date, text) to authenticated;

create or replace function reports.disease_count(p_branch text, p_from date, p_to date, p_disease text)
returns table(total int, all_patients int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  return query
    select (select count(*)::int from reports.disease_list(p_branch, p_from, p_to, p_disease)),
           (select count(*)::int from public.patients p
             where lower(trim(coalesce(p."branch",''))) = lower(trim(p_branch))
               and left(coalesce(nullif(p."registrationDate",''), p."date", ''),10) >= to_char(p_from,'YYYY-MM-DD')
               and left(coalesce(nullif(p."registrationDate",''), p."date", ''),10) <= to_char(p_to,'YYYY-MM-DD'));
end $$;
revoke all on function reports.disease_count(text, date, date, text) from public, anon;
grant execute on function reports.disease_count(text, date, date, text) to authenticated;

-- ── ৩২/৩৩) RMP-কে কল করা হয়েছে · কল করার কথা (public.doctor_visits) ────────────
create or replace function reports.rmp_called_list(p_branch text, p_from date, p_to date)
returns table(rmp_id text, name text, mobile text, last_call_date text, next_call_date text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select d.id, coalesce(d."name",''), coalesce(d."mobile",''), left(coalesce(d."lastCallDate",''),10), left(coalesce(d."nextCallDate",''),10)
      from public.doctor_visits d
     where lower(trim(coalesce(d."branch",''))) = lower(trim(p_branch))
       and left(coalesce(d."lastCallDate",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(d."lastCallDate",''),10) <= to_char(p_to,'YYYY-MM-DD')
     order by d."lastCallDate" desc, d."name"
     limit 500;
end $$;
revoke all on function reports.rmp_called_list(text, date, date) from public, anon;
grant execute on function reports.rmp_called_list(text, date, date) to authenticated;

create or replace function reports.rmp_called_count(p_branch text, p_from date, p_to date)
returns table(total int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  return query select count(*)::int from reports.rmp_called_list(p_branch, p_from, p_to);
end $$;
revoke all on function reports.rmp_called_count(text, date, date) from public, anon;
grant execute on function reports.rmp_called_count(text, date, date) to authenticated;

create or replace function reports.rmp_call_due_list(p_branch text, p_from date, p_to date)
returns table(rmp_id text, name text, mobile text, next_call_date text, last_call_date text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select d.id, coalesce(d."name",''), coalesce(d."mobile",''), left(coalesce(d."nextCallDate",''),10), left(coalesce(d."lastCallDate",''),10)
      from public.doctor_visits d
     where lower(trim(coalesce(d."branch",''))) = lower(trim(p_branch))
       and left(coalesce(d."nextCallDate",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(d."nextCallDate",''),10) <= to_char(p_to,'YYYY-MM-DD')
     order by d."nextCallDate", d."name"
     limit 500;
end $$;
revoke all on function reports.rmp_call_due_list(text, date, date) from public, anon;
grant execute on function reports.rmp_call_due_list(text, date, date) to authenticated;

create or replace function reports.rmp_call_due_count(p_branch text, p_from date, p_to date)
returns table(total int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  return query select count(*)::int from reports.rmp_call_due_list(p_branch, p_from, p_to);
end $$;
revoke all on function reports.rmp_call_due_count(text, date, date) from public, anon;
grant execute on function reports.rmp_call_due_count(text, date, date) to authenticated;

-- ── ৩৪) ফিল্ড ভিজিট — MARK VISIT সংখ্যা + কিমি (wn.doctor_visits, wn.field_visit_days) ──
create or replace function reports.field_visit_summary(p_branch text, p_from date, p_to date)
returns table(visits int, km numeric, staff_count int)
language plpgsql stable security definer set search_path = hr, wn, public as $$
declare v int; k numeric; s int;
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  if to_regclass('wn.doctor_visits') is null or to_regclass('wn.field_visit_days') is null then
    raise exception 'field visit tables not found';
  end if;
  execute 'select count(*)::int from wn.doctor_visits x
            where lower(trim(coalesce(x.branch,''''))) = lower(trim($1)) and x.work_date >= $2 and x.work_date <= $3'
     into v using p_branch, p_from, p_to;
  execute 'select coalesce(round(sum(coalesce(x.distance_m,0))/1000.0, 2),0), count(distinct x.staff_code)::int from wn.field_visit_days x
            where lower(trim(coalesce(x.branch,''''))) = lower(trim($1)) and x.work_date >= $2 and x.work_date <= $3'
     into k, s using p_branch, p_from, p_to;
  return query select v, k, s;
end $$;
revoke all on function reports.field_visit_summary(text, date, date) from public, anon;
grant execute on function reports.field_visit_summary(text, date, date) to authenticated;

create or replace function reports.field_visit_list(p_branch text, p_from date, p_to date)
returns table(staff_code text, work_date text, visits int, km numeric)
language plpgsql stable security definer set search_path = hr, wn, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  if to_regclass('wn.doctor_visits') is null or to_regclass('wn.field_visit_days') is null then
    raise exception 'field visit tables not found';
  end if;
  return query execute
    'with d as (select x.staff_code, x.work_date, count(*)::int as visits from wn.doctor_visits x
                 where lower(trim(coalesce(x.branch,''''))) = lower(trim($1)) and x.work_date >= $2 and x.work_date <= $3
                 group by x.staff_code, x.work_date),
          f as (select x.staff_code, x.work_date, round(coalesce(x.distance_m,0)/1000.0, 2) as km from wn.field_visit_days x
                 where lower(trim(coalesce(x.branch,''''))) = lower(trim($1)) and x.work_date >= $2 and x.work_date <= $3)
     select coalesce(d.staff_code, f.staff_code), to_char(coalesce(d.work_date, f.work_date),''YYYY-MM-DD''),
            coalesce(d.visits,0), coalesce(f.km,0)
       from d full outer join f on f.staff_code = d.staff_code and f.work_date = d.work_date
      order by 2 desc, 1
      limit 500'
    using p_branch, p_from, p_to;
end $$;
revoke all on function reports.field_visit_list(text, date, date) from public, anon;
grant execute on function reports.field_visit_list(text, date, date) to authenticated;

-- ── ৩৫) স্টাফদের মোট ঘণ্টা (Attendance Sheet-এর হুবহু নিয়ম, হোম-ব্রাঞ্চ ধরে) ───────
create or replace function reports.staff_hours_list(p_branch text, p_from date, p_to date)
returns table(staff_code text, hours numeric, days int, leave_days int, out_missing_days int)
language plpgsql stable security definer set search_path = hr, wn, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    with rows0 as (
      select n.staff_code as sc, coalesce(n.is_leave,false) as lv, coalesce(n.is_wfh,false) as wfh,
             coalesce(n.is_other_branch,false) as ob,
             case when nullif(lower(btrim(coalesce(n.check_in,''))),'null') ~ '^\d{1,2}:\d{2}'
                  then split_part(n.check_in,':',1)::int*60 + split_part(n.check_in,':',2)::int end as a,
             case when nullif(lower(btrim(coalesce(n.check_out,''))),'null') ~ '^\d{1,2}:\d{2}'
                  then split_part(n.check_out,':',1)::int*60 + split_part(n.check_out,':',2)::int end as b
        from wn.notebook_days n
        join hr.staff_profiles s on s.person_code = n.staff_code
       where lower(trim(coalesce(s.branch,''))) = lower(trim(p_branch))
         and n.work_date >= p_from and n.work_date <= p_to
    ),
    m as (
      select r.sc, r.lv,
             case when r.lv then 420 when r.wfh then 420 when r.ob then 420
                  when r.a is not null and r.b is null then 420
                  when r.a is null or r.b is null or r.b <= r.a then 0
                  else r.b - r.a end as mins,
             (r.a is not null and r.b is null and not r.lv and not r.wfh and not r.ob) as om
        from rows0 r
       where coalesce(r.a,0) between 0 and 1439 and coalesce(r.b,0) between 0 and 1439
    )
    select m.sc, round(sum(m.mins)/60.0, 1), count(*)::int,
           count(*) filter (where m.lv)::int, count(*) filter (where m.om)::int
      from m
     group by m.sc
     order by sum(m.mins) desc, m.sc
     limit 500;
end $$;
revoke all on function reports.staff_hours_list(text, date, date) from public, anon;
grant execute on function reports.staff_hours_list(text, date, date) to authenticated;

create or replace function reports.staff_hours_summary(p_branch text, p_from date, p_to date)
returns table(total_hours numeric, staff_count int)
language plpgsql stable security definer set search_path = hr, wn, public as $$
begin
  return query select coalesce(sum(l.hours),0), count(*)::int from reports.staff_hours_list(p_branch, p_from, p_to) l;
end $$;
revoke all on function reports.staff_hours_summary(text, date, date) from public, anon;
grant execute on function reports.staff_hours_summary(text, date, date) to authenticated;

-- ── ৩৬) কোন কোন স্টাফ হাজির ছিল (বৈধ check_in) ─────────────────────────────────
create or replace function reports.staff_present_list(p_branch text, p_from date, p_to date)
returns table(staff_code text, work_date text, check_in text, check_out text)
language plpgsql stable security definer set search_path = hr, wn, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select n.staff_code, to_char(n.work_date,'YYYY-MM-DD'), coalesce(n.check_in,''),
           coalesce(nullif(lower(btrim(coalesce(n.check_out,''))),'null'),'')
      from wn.notebook_days n
      join hr.staff_profiles s on s.person_code = n.staff_code
     where lower(trim(coalesce(s.branch,''))) = lower(trim(p_branch))
       and n.work_date >= p_from and n.work_date <= p_to
       and coalesce(nullif(lower(btrim(coalesce(n.check_in,''))),'null'),'') <> ''
     order by n.work_date desc, n.check_in
     limit 500;
end $$;
revoke all on function reports.staff_present_list(text, date, date) from public, anon;
grant execute on function reports.staff_present_list(text, date, date) to authenticated;

create or replace function reports.staff_present_summary(p_branch text, p_from date, p_to date)
returns table(total int, staff_count int)
language plpgsql stable security definer set search_path = hr, wn, public as $$
begin
  return query select count(*)::int, count(distinct l.staff_code)::int from reports.staff_present_list(p_branch, p_from, p_to) l;
end $$;
revoke all on function reports.staff_present_summary(text, date, date) from public, anon;
grant execute on function reports.staff_present_summary(text, date, date) to authenticated;

notify pgrst, 'reload schema';
commit;
