-- ═══════════════════════════════════════════════════════════════════════════
-- 🎤🔒 V1420 (১৩.০৯.২০২৬, TK-নির্দেশ "একসাথে যতগুলো সম্ভব, সাবধানে ও সততার
-- সাথে") — ভয়েস-প্রশ্নের জবাব, ধাপ ২ — এক ব্যাচে ৯টা প্যাটার্ন:
--  ১২) আজ/কাল কতজনের অ্যাপয়েন্টমেন্ট (আইটেম ১২)
--  ১৩) আগামীকাল কতজন রোগী আসার কথা (আইটেম ১৮)
--  ১৪) কতগুলো দিনের ক্যাশ এখনো হ্যান্ডওভার হয়নি (আইটেম ২৮)
--  ১৫) কতগুলো পেমেন্ট-অনুরোধ (Backdate/Edit/Refund) Pending (আইটেম ৪০)
--  ১৬) রেফারেল-এডিটের কতগুলো অনুরোধ Pending (আইটেম ৪২)
--  ১৭) এ মাসে কতগুলো ছুটির আবেদন (আইটেম ৪৫)
--  ১৮) ডাক্তারদের কতগুলো রিমাইন্ডার পাঠানো হয়েছে (আইটেম ৪৭)
--  ১৯) এই ব্রাঞ্চে স্টাফদের কতগুলো Reminder এখনো Open (আইটেম ৪৮, ব্রাঞ্চ-স্তরে)
--  ২০) কতজন রোগীর ভিজিট ফি ফেরত দেওয়া হয়েছে (আইটেম ৩৪)
--
-- ⚠️ যাচাই করে পাওয়া (আন্দাজ নয়, Explore সাবএজেন্ট দিয়ে কোড মিলিয়ে) — প্রতিটার
-- নিয়ম অ্যাপের নিজের পর্দার নিয়মের সাথে **হুবহু** মেলানো (নিয়ম ৭ক-২):
-- (ক) অ্যাপয়েন্টমেন্ট — enquiries.appointmentDate (plain yyyy-MM-dd); অ্যাপের
--     Appointment পর্দা branch-ফাঁকা সারিও দেখায়, রেজিস্টার-হওয়া রোগীর সারিও বাদ দেয়
--     না — এখানেও তাই। ⛔ চেনা মোবাইলের বুকিং এই ঘরে বসে না (followups.nextFollow-এ
--     যায়) — সেটা এই সংখ্যায় আসবে না, খাতায় লেখা।
-- (খ) "আসার কথা" — TK-র নিজের নিয়ম (১৯.০৭.২০২৬): followups.nextFollow নয়, শুধু
--     payments-এর payType='chamber_expected' মার্কার-সারি (টাকা ০, তারিখ = যেদিন
--     আসার কথা)। অ্যাপের ExpectedTomorrow পর্দা ঠিক এটাই পড়ে।
-- (গ) হ্যান্ডওভার-বাকি দিন — অ্যাপের নিজের গোনা: status ''/'pending' এবং cashTotal>0
--     ('waiting' = দিয়ে দেওয়া হয়েছে, স্বীকার বাকি — গোনায় নয়; ₹0-দিন — গোনায় নয়, V1308)।
-- (ঘ) পেমেন্ট-অনুরোধ তিন জায়গায়: payment_backdate_requests · payment_edit_requests
--     (status='pending') · payments-এর refundApprovalStatus='pending' (আলাদা টেবিল নেই)।
-- (ঙ) ছুটি — wn.leave_requests (hr.staff_leave মৃত কোড, কেউ ব্যবহার করে না)। অনুমোদনের
--     আসল শব্দ 'confirmed' ('approved' নয়)। একাধিক দিনের ছুটি = প্রতিদিন আলাদা সারি, তাই
--     এখানে সংখ্যাটা "ছুটির দিন", আবেদন-সংখ্যা নয় — উত্তরের লেখাতেও তাই বলা হবে।
-- (চ) ডাক্তার-রিমাইন্ডার — doctor_reminders (status ঘর নেই: acceptedAt ফাঁকা = এখনো
--     accept হয়নি)। ⛔ V1186-এর আগের পুরনো রিমাইন্ডার patients.doctorReminder*-এ,
--     এখানে গোনা হয় না — খাতায় লেখা।
-- (ছ) স্টাফ-রিমাইন্ডার Open — অ্যাপের নিজের নিয়ম: status <> 'done' (ReminderRepository)।
-- (জ) ভিজিট ফি ফেরত — TK-র ১১.০৯.২০২৬-এর সিদ্ধান্ত: followups-এর 'Returned' ট্যাগ
--     অবিশ্বাস্য, আসল উৎস payments-এর রিফান্ড-সারি; দুই অ্যাপই remarks-এ হুবহু
--     'Fees Return (Visit Card)' লেখে — তাই সেটাই চেনা হচ্ছে। ⚠️ এটা refund_summary-র
--     **উপ-অংশ** (আলাদা টাকা নয়) — দুটো যোগ করা যাবে না।
-- ═══════════════════════════════════════════════════════════════════════════
begin;

-- ── ১২) অ্যাপয়েন্টমেন্ট ─────────────────────────────────────────────────────
create or replace function reports.appointment_count(p_branch text, p_from date, p_to date)
returns int language sql stable security definer set search_path = hr, public as $$
  select case when not reports.can_access_branch(p_branch) then null::int else (
    select count(*)::int from public.enquiries e
     where (lower(trim(coalesce(e."branch",''))) = lower(trim(p_branch)) or coalesce(trim(e."branch"),'') = '')
       and left(coalesce(e."appointmentDate",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(e."appointmentDate",''),10) <= to_char(p_to,'YYYY-MM-DD')
  ) end
$$;
revoke all on function reports.appointment_count(text, date, date) from public, anon;
grant execute on function reports.appointment_count(text, date, date) to authenticated;

create or replace function reports.appointment_list(p_branch text, p_from date, p_to date)
returns table(enquiry_row_id text, name text, mobile text, disease text, appointment_date text, registered boolean)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select e.id, coalesce(e."name",''), coalesce(e."mobile",''), coalesce(e."disease",''),
           left(coalesce(e."appointmentDate",''),10), coalesce(e."convertedPatientId",'') <> ''
      from public.enquiries e
     where (lower(trim(coalesce(e."branch",''))) = lower(trim(p_branch)) or coalesce(trim(e."branch"),'') = '')
       and left(coalesce(e."appointmentDate",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(e."appointmentDate",''),10) <= to_char(p_to,'YYYY-MM-DD')
     order by e."appointmentDate", e."name"
     limit 500;
end $$;
revoke all on function reports.appointment_list(text, date, date) from public, anon;
grant execute on function reports.appointment_list(text, date, date) to authenticated;

-- ── ১৩) আসার কথা (chamber_expected মার্কার) ───────────────────────────────
create or replace function reports.expected_count(p_branch text, p_from date, p_to date)
returns int language sql stable security definer set search_path = hr, public as $$
  select case when not reports.can_access_branch(p_branch) then null::int else (
    select count(*)::int from public.payments y
     where lower(coalesce(y."payType",'')) = 'chamber_expected'
       and lower(trim(coalesce(y."branch",''))) = lower(trim(p_branch))
       and left(coalesce(y."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(y."date",''),10) <= to_char(p_to,'YYYY-MM-DD')
  ) end
$$;
revoke all on function reports.expected_count(text, date, date) from public, anon;
grant execute on function reports.expected_count(text, date, date) to authenticated;

create or replace function reports.expected_list(p_branch text, p_from date, p_to date)
returns table(mark_id text, patient_row_id text, name text, mobile text, expected_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select y.id, coalesce(y."patientId",''), coalesce(y."name",''), coalesce(y."mobile",''), left(coalesce(y."date",''),10)
      from public.payments y
     where lower(coalesce(y."payType",'')) = 'chamber_expected'
       and lower(trim(coalesce(y."branch",''))) = lower(trim(p_branch))
       and left(coalesce(y."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(y."date",''),10) <= to_char(p_to,'YYYY-MM-DD')
     order by y."date", y."name"
     limit 500;
end $$;
revoke all on function reports.expected_list(text, date, date) from public, anon;
grant execute on function reports.expected_list(text, date, date) to authenticated;

-- ── ১৪) হ্যান্ডওভার-বাকি দিন (স্ন্যাপশট, অ্যাপের নিজের গোনার নিয়ম) ────────────
create or replace function reports.handover_pending_summary(p_branch text)
returns table(total numeric, day_count int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    with rows0 as (
      select coalesce(nullif(regexp_replace(coalesce(c."cashTotal"::text,'0'),'[^0-9.\-]','','g'),''),'0')::numeric as cash
        from public.chamber_close c
       where upper(trim(coalesce(c."branch",''))) = upper(trim(p_branch))
         and lower(coalesce(c."handoverStatus",'')) in ('', 'pending')
    )
    select coalesce(sum(cash),0), count(*)::int from rows0 where cash > 0;
end $$;
revoke all on function reports.handover_pending_summary(text) from public, anon;
grant execute on function reports.handover_pending_summary(text) to authenticated;

create or replace function reports.handover_pending_list(p_branch text)
returns table(handover_date text, cash numeric, status text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    with rows0 as (
      select left(coalesce(c."date",''),10) as d,
             coalesce(nullif(regexp_replace(coalesce(c."cashTotal"::text,'0'),'[^0-9.\-]','','g'),''),'0')::numeric as cash,
             lower(coalesce(c."handoverStatus",'')) as st
        from public.chamber_close c
       where upper(trim(coalesce(c."branch",''))) = upper(trim(p_branch))
         and lower(coalesce(c."handoverStatus",'')) in ('', 'pending')
    )
    select r.d, r.cash, case when r.st = '' then 'not started' else r.st end
      from rows0 r where r.cash > 0
     order by r.d desc
     limit 500;
end $$;
revoke all on function reports.handover_pending_list(text) from public, anon;
grant execute on function reports.handover_pending_list(text) to authenticated;

-- ── ১৫) পেমেন্ট-অনুরোধ Pending (Backdate · Edit · Refund) — স্ন্যাপশট ─────────
create or replace function reports.payment_requests_summary(p_branch text)
returns table(backdate_count int, edit_count int, refund_count int, total int)
language plpgsql stable security definer set search_path = hr, public as $$
declare b int; e int; r int;
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  select count(*)::int into b from public.payment_backdate_requests x
   where lower(coalesce(x."status",'')) = 'pending' and lower(trim(coalesce(x."branch",''))) = lower(trim(p_branch));
  select count(*)::int into e from public.payment_edit_requests x
   where lower(coalesce(x."status",'')) = 'pending' and lower(trim(coalesce(x."branch",''))) = lower(trim(p_branch));
  select count(*)::int into r from public.payments y
   where lower(coalesce(y."payType",'')) = 'refund' and lower(coalesce(y."refundApprovalStatus",'')) = 'pending'
     and lower(trim(coalesce(y."branch",''))) = lower(trim(p_branch));
  return query select b, e, r, b + e + r;
end $$;
revoke all on function reports.payment_requests_summary(text) from public, anon;
grant execute on function reports.payment_requests_summary(text) to authenticated;

create or replace function reports.payment_requests_list(p_branch text)
returns table(request_id text, request_type text, name text, mobile text, amount numeric, requested_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select * from (
      select x.id::text, 'Backdate'::text, coalesce(x."name",''), coalesce(x."mobile",''), coalesce(x."amount",0)::numeric, left(coalesce(x."requestedAt",''),10)
        from public.payment_backdate_requests x
       where lower(coalesce(x."status",'')) = 'pending' and lower(trim(coalesce(x."branch",''))) = lower(trim(p_branch))
      union all
      select x.id::text, 'Edit'::text, coalesce(x."name",''), coalesce(x."mobile",''), coalesce(x."newAmount",0)::numeric, left(coalesce(x."requestedAt",''),10)
        from public.payment_edit_requests x
       where lower(coalesce(x."status",'')) = 'pending' and lower(trim(coalesce(x."branch",''))) = lower(trim(p_branch))
      union all
      select y.id, 'Refund'::text, coalesce(y."name",''), coalesce(y."mobile",''),
             coalesce(nullif(regexp_replace(coalesce(y."amount",'0'),'[^0-9.\-]','','g'),'')::numeric,0), left(coalesce(y."date",''),10)
        from public.payments y
       where lower(coalesce(y."payType",'')) = 'refund' and lower(coalesce(y."refundApprovalStatus",'')) = 'pending'
         and lower(trim(coalesce(y."branch",''))) = lower(trim(p_branch))
    ) u(request_id, request_type, name, mobile, amount, requested_on)
    order by u.requested_on desc
    limit 500;
end $$;
revoke all on function reports.payment_requests_list(text) from public, anon;
grant execute on function reports.payment_requests_list(text) to authenticated;

-- ── ১৬) রেফারেল-এডিট অনুরোধ Pending — স্ন্যাপশট ──────────────────────────────
create or replace function reports.referral_requests_summary(p_branch text)
returns table(total int, delete_count int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select count(*)::int, count(*) filter (where coalesce(x."isDelete",false))::int
      from public.referral_edit_requests x
     where lower(coalesce(x.status,'')) = 'pending' and lower(trim(coalesce(x.branch,''))) = lower(trim(p_branch));
end $$;
revoke all on function reports.referral_requests_summary(text) from public, anon;
grant execute on function reports.referral_requests_summary(text) to authenticated;

create or replace function reports.referral_requests_list(p_branch text)
returns table(request_id text, request_type text, new_amount numeric, requested_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select x.id::text, case when coalesce(x."isDelete",false) then 'Delete' else 'Edit' end,
           coalesce(x."newAmount",0)::numeric, left(coalesce(x."requestedAt",''),10)
      from public.referral_edit_requests x
     where lower(coalesce(x.status,'')) = 'pending' and lower(trim(coalesce(x.branch,''))) = lower(trim(p_branch))
     order by x."requestedAt" desc
     limit 500;
end $$;
revoke all on function reports.referral_requests_list(text) from public, anon;
grant execute on function reports.referral_requests_list(text) to authenticated;

-- ── ১৭) ছুটির আবেদন (wn.leave_requests, created_at ধরে; সংখ্যা = ছুটির দিন) ───
create or replace function reports.leave_summary(p_branch text, p_from date, p_to date)
returns table(total int, confirmed int, pending int, rejected int)
language plpgsql stable security definer set search_path = hr, wn, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select count(*)::int,
           count(*) filter (where lower(coalesce(l.status,'')) = 'confirmed')::int,
           count(*) filter (where lower(coalesce(l.status,'')) = 'pending')::int,
           count(*) filter (where lower(coalesce(l.status,'')) = 'rejected')::int
      from wn.leave_requests l
     where lower(trim(coalesce(l.branch,''))) = lower(trim(p_branch))
       and (l.created_at at time zone 'Asia/Kolkata')::date >= p_from
       and (l.created_at at time zone 'Asia/Kolkata')::date <= p_to;
end $$;
revoke all on function reports.leave_summary(text, date, date) from public, anon;
grant execute on function reports.leave_summary(text, date, date) to authenticated;

create or replace function reports.leave_list(p_branch text, p_from date, p_to date)
returns table(staff_code text, leave_date text, status text, applied_on text)
language plpgsql stable security definer set search_path = hr, wn, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select l.staff_code, to_char(l.leave_date,'YYYY-MM-DD'), coalesce(l.status,''),
           to_char(l.created_at at time zone 'Asia/Kolkata','YYYY-MM-DD')
      from wn.leave_requests l
     where lower(trim(coalesce(l.branch,''))) = lower(trim(p_branch))
       and (l.created_at at time zone 'Asia/Kolkata')::date >= p_from
       and (l.created_at at time zone 'Asia/Kolkata')::date <= p_to
     order by l.created_at desc, l.leave_date
     limit 500;
end $$;
revoke all on function reports.leave_list(text, date, date) from public, anon;
grant execute on function reports.leave_list(text, date, date) to authenticated;

-- ── ১৮) ডাক্তার-রিমাইন্ডার পাঠানো (doctor_reminders, createdAt ধরে) ────────────
create or replace function reports.doctor_reminder_summary(p_branch text, p_from date, p_to date)
returns table(total int, not_accepted int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select count(*)::int,
           count(*) filter (where coalesce(d."acceptedAt",'') = '' and coalesce(d."cancelledAt",'') = '' and coalesce(d."active",true))::int
      from public.doctor_reminders d
     where lower(trim(coalesce(d."branch",''))) = lower(trim(p_branch))
       and left(coalesce(d."createdAt",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(d."createdAt",''),10) <= to_char(p_to,'YYYY-MM-DD');
end $$;
revoke all on function reports.doctor_reminder_summary(text, date, date) from public, anon;
grant execute on function reports.doctor_reminder_summary(text, date, date) to authenticated;

create or replace function reports.doctor_reminder_list(p_branch text, p_from date, p_to date)
returns table(reminder_id text, remind_date text, created_on text, accepted boolean, cancelled boolean)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select d.id::text, left(coalesce(d."remindDate",''),10), left(coalesce(d."createdAt",''),10),
           coalesce(d."acceptedAt",'') <> '', coalesce(d."cancelledAt",'') <> ''
      from public.doctor_reminders d
     where lower(trim(coalesce(d."branch",''))) = lower(trim(p_branch))
       and left(coalesce(d."createdAt",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(d."createdAt",''),10) <= to_char(p_to,'YYYY-MM-DD')
     order by d."createdAt" desc
     limit 500;
end $$;
revoke all on function reports.doctor_reminder_list(text, date, date) from public, anon;
grant execute on function reports.doctor_reminder_list(text, date, date) to authenticated;

-- ── ১৯) স্টাফ-রিমাইন্ডার Open (status <> 'done') — স্ন্যাপশট, ব্রাঞ্চ-স্তরে ──────
create or replace function reports.staff_reminder_open_summary(p_branch text)
returns table(total int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select count(*)::int from public.reminders r
     where lower(trim(coalesce(r."branch",''))) = lower(trim(p_branch))
       and lower(coalesce(r."status",'')) <> 'done';
end $$;
revoke all on function reports.staff_reminder_open_summary(text) from public, anon;
grant execute on function reports.staff_reminder_open_summary(text) to authenticated;

create or replace function reports.staff_reminder_open_list(p_branch text)
returns table(reminder_id text, to_name text, to_code text, reminder_type text, remind_on text, status text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select r.id::text, coalesce(r."toName",''), coalesce(r."toCode",''), coalesce(r."type",''),
           left(coalesce(r."remindOn",''),10), coalesce(r."status",'')
      from public.reminders r
     where lower(trim(coalesce(r."branch",''))) = lower(trim(p_branch))
       and lower(coalesce(r."status",'')) <> 'done'
     order by r."remindOn" desc
     limit 500;
end $$;
revoke all on function reports.staff_reminder_open_list(text) from public, anon;
grant execute on function reports.staff_reminder_open_list(text) to authenticated;

-- ── ২০) ভিজিট ফি ফেরত (approved refund + remarks 'Fees Return') — refund_summary-র উপ-অংশ ──
create or replace function reports.fee_return_summary(p_branch text, p_from date, p_to date)
returns table(total numeric, patient_count int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select coalesce(sum(coalesce(nullif(regexp_replace(coalesce(y."amount",'0'),'[^0-9.\-]','','g'),'')::numeric,0)),0),
           count(distinct coalesce(nullif(y."patientId",''), y."mobile"))::int
      from public.payments y
     where lower(coalesce(y."payType",'')) = 'refund'
       and lower(coalesce(y."refundApprovalStatus",'')) = 'approved'
       and lower(coalesce(y."remarks",'')) like '%fees return%'
       and lower(trim(coalesce(y."branch",''))) = lower(trim(p_branch))
       and left(coalesce(y."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(y."date",''),10) <= to_char(p_to,'YYYY-MM-DD');
end $$;
revoke all on function reports.fee_return_summary(text, date, date) from public, anon;
grant execute on function reports.fee_return_summary(text, date, date) to authenticated;

create or replace function reports.fee_return_list(p_branch text, p_from date, p_to date)
returns table(payment_id text, patient_row_id text, name text, mobile text, amount numeric, returned_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select y.id, coalesce(y."patientId",''), coalesce(y."name",''), coalesce(y."mobile",''),
           coalesce(nullif(regexp_replace(coalesce(y."amount",'0'),'[^0-9.\-]','','g'),'')::numeric,0), left(coalesce(y."date",''),10)
      from public.payments y
     where lower(coalesce(y."payType",'')) = 'refund'
       and lower(coalesce(y."refundApprovalStatus",'')) = 'approved'
       and lower(coalesce(y."remarks",'')) like '%fees return%'
       and lower(trim(coalesce(y."branch",''))) = lower(trim(p_branch))
       and left(coalesce(y."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(y."date",''),10) <= to_char(p_to,'YYYY-MM-DD')
     order by y."date" desc
     limit 500;
end $$;
revoke all on function reports.fee_return_list(text, date, date) from public, anon;
grant execute on function reports.fee_return_list(text, date, date) to authenticated;

notify pgrst, 'reload schema';
commit;
