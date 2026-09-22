-- ═══════════════════════════════════════════════════════════════════════════
-- 🎤🔒 V1448 (১৪.০৯.২০২৬, TK নিজে ধরেছেন) — ভয়েস/টাইপ-করা প্রশ্নের তালিকা-পর্দায়
-- যখন কোনো নির্দিষ্ট ব্রাঞ্চ বলা হয়নি (সব ব্রাঞ্চ একসাথে দেখানো হয়), প্রতিটা সারি
-- কোন ব্রাঞ্চের সেটা বোঝার কোনো উপায় ছিল না — TK: "কোন ব্রাঞ্চ থেকে এসেছে সেটা
-- বোঝা যাচ্ছে না"। সমাধান: reports.* schema-র প্রতিটা "_list" ফাংশনের আউটপুটে
-- একটা `branch` কলাম যোগ করা হলো, যেটা সবসময় সেই কলের `p_branch` প্যারামিটারটাই
-- ফেরত দেয় (এই ফাংশনগুলো সবসময় একটামাত্র আসল ব্রাঞ্চের জন্য ডাকা হয় — "সব ব্রাঞ্চ"
-- অংশটা অ্যাপ/ওয়েবের কোড ৫ বার লুপ করে সামলায়, তাই p_branch-ই আসল উত্তর, আলাদা করে
-- সারির নিজের টেবিল থেকে branch বের করার দরকার নেই)।
--
-- ⛔ যা বদলায়নি: প্রতিটা ফাংশনের WHERE-শর্ত, টাকা/কমিশনের হিসাব, তারিখ-ফিল্টার,
-- অর্ডার, লিমিট — সব হুবহু আগের মতোই। শুধু (ক) returns table(...)-এ `branch text`
-- যোগ, আর (খ) সবচেয়ে বাইরের select-এ `p_branch as branch` একটা নতুন কলাম হিসেবে
-- বসানো হয়েছে। `_summary`/`_count` বা অন্য কোনো হেল্পার ফাংশন ছোঁয়া হয়নি।
--
-- Postgres-এ `create or replace function` আউটপুট-কলাম বদলাতে দেয় না — তাই প্রতিটা
-- ফাংশন আগে `drop function` করে, তারপর নতুন করে `create function` করা হলো, এবং
-- আগের হুবহু `revoke`/`grant` লাইন আবার বসানো হয়েছে (Master-only পাহারা অটুট)।
--
-- ⚠️ ব্যতিক্রম — `reports.field_visit_list`: এর নিজের ভিতরে `order by 2 desc, 1`
-- (কলাম-পজিশন ধরে অর্ডার) আছে বলে branch-কে প্রথমে না বসিয়ে **শেষে** বসানো হয়েছে,
-- যাতে পুরনো পজিশন-নাম্বার নষ্ট না হয়। বাকি সব ফাংশনে branch সবচেয়ে প্রথম কলাম।
--
-- ⚠️ `reports.messages_list` ও `reports.field_visit_list` ডাইনামিক SQL
-- (`execute ... using p_branch, ...`) ব্যবহার করে — সেখানে `$1 as branch` বসানো
-- হয়েছে (`$1` মানেই ওই কলে পাঠানো `p_branch`)।
--
-- ⚠️ `reports.payment_requests_list`-এর ভেতরের `select *` বদলে স্পষ্ট কলাম-তালিকা
-- করা হয়েছে (নইলে নতুন branch কলাম প্রথমে বসানো যেত না) — কিন্তু ভেতরের তিনটে
-- union-অংশ ও তাদের WHERE-শর্ত অক্ষত।
-- ═══════════════════════════════════════════════════════════════════════════
begin;

-- ── ১) patients_registered_list ──────────────────────────────────────────
drop function reports.patients_registered_list(text, date, date);
create function reports.patients_registered_list(p_branch text, p_from date, p_to date)
returns table(branch text, patient_row_id text, patient_code text, name text, mobile text, registration_date text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, p.id, coalesce(p."patientId",''), coalesce(p.name,''), coalesce(p.mobile,''),
           left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10)
      from public.patients p
     where p."branch" = p_branch
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) <= to_char(p_to,'YYYY-MM-DD')
     order by left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) desc, p.name
     limit 500;
end $$;
revoke all on function reports.patients_registered_list(text, date, date) from public, anon;
grant execute on function reports.patients_registered_list(text, date, date) to authenticated;

-- ── ২) collection_list ────────────────────────────────────────────────────
drop function reports.collection_list(text, date, date);
create function reports.collection_list(p_branch text, p_from date, p_to date)
returns table(branch text, payment_id text, patient_row_id text, name text, mobile text, amount numeric, mode text, pay_type text, paid_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, y.id, coalesce(y."patientId",''), coalesce(p.name,''), coalesce(y.mobile,''),
           coalesce(nullif(regexp_replace(coalesce(y."amount",'0'),'[^0-9.\-]','','g'),'')::numeric,0),
           coalesce(y."mode",''), coalesce(y."payType",''), left(coalesce(y."date",''),10)
      from public.payments y
      left join public.patients p on p.id = y."patientId"
     where y."branch" = p_branch
       and left(coalesce(y."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(y."date",''),10) <= to_char(p_to,'YYYY-MM-DD')
       and not (lower(coalesce(y."payType",'')) = 'refund' and lower(coalesce(y."refundApprovalStatus",'')) <> 'approved')
     order by y."date" desc
     limit 500;
end $$;
revoke all on function reports.collection_list(text, date, date) from public, anon;
grant execute on function reports.collection_list(text, date, date) to authenticated;

-- ── ৩) product_sale_list ──────────────────────────────────────────────────
drop function reports.product_sale_list(text, date, date, text);
create function reports.product_sale_list(p_branch text, p_from date, p_to date, p_kind text)
returns table(branch text, product_row_id text, customer text, mobile text, product text, bill numeric, deposit numeric, due numeric, mode text, sold_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if p_kind not in ('medicinePayment','salinePayment') then
    raise exception 'Invalid kind';
  end if;
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, y.id, coalesce(y."customer",''), coalesce(y."mobile",''), coalesce(y."product",''),
           coalesce(nullif(regexp_replace(coalesce(y."bill",'0'),'[^0-9.\-]','','g'),'')::numeric,0),
           coalesce(nullif(regexp_replace(coalesce(y."deposit",'0'),'[^0-9.\-]','','g'),'')::numeric,0),
           coalesce(nullif(regexp_replace(coalesce(y."due",'0'),'[^0-9.\-]','','g'),'')::numeric,0),
           coalesce(y."mode",''), left(coalesce(y."date",''),10)
      from public.products y
     where y."branch" = p_branch
       and y."kind" = p_kind
       and left(coalesce(y."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(y."date",''),10) <= to_char(p_to,'YYYY-MM-DD')
       and coalesce(nullif(regexp_replace(coalesce(y."bill",'0'),'[^0-9.\-]','','g'),'')::numeric,0) > 0
     order by y."date" desc
     limit 500;
end $$;
revoke all on function reports.product_sale_list(text, date, date, text) from public, anon;
grant execute on function reports.product_sale_list(text, date, date, text) to authenticated;

-- ── ৪) enquiry_list ───────────────────────────────────────────────────────
drop function reports.enquiry_list(text, date, date);
create function reports.enquiry_list(p_branch text, p_from date, p_to date)
returns table(branch text, enquiry_row_id text, name text, mobile text, disease text, enquiry_date text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, e.id, coalesce(e."name",''), coalesce(e."mobile",''), coalesce(e."disease",''),
           left(coalesce(e."date",''),10)
      from public.enquiries e
     where e."branch" = p_branch
       and left(coalesce(e."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(e."date",''),10) <= to_char(p_to,'YYYY-MM-DD')
     order by e."date" desc
     limit 500;
end $$;
revoke all on function reports.enquiry_list(text, date, date) from public, anon;
grant execute on function reports.enquiry_list(text, date, date) to authenticated;

-- ── ৫) refund_list ────────────────────────────────────────────────────────
drop function reports.refund_list(text, date, date);
create function reports.refund_list(p_branch text, p_from date, p_to date)
returns table(branch text, payment_id text, patient_row_id text, name text, mobile text, amount numeric, mode text, refunded_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, y.id, coalesce(y."patientId",''), coalesce(y."name",''), coalesce(y."mobile",''),
           coalesce(nullif(regexp_replace(coalesce(y."amount",'0'),'[^0-9.\-]','','g'),'')::numeric,0),
           coalesce(y."mode",''), left(coalesce(y."date",''),10)
      from public.payments y
     where y."branch" = p_branch
       and lower(coalesce(y."payType",'')) = 'refund'
       and lower(coalesce(y."refundApprovalStatus",'')) = 'approved'
       and left(coalesce(y."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(y."date",''),10) <= to_char(p_to,'YYYY-MM-DD')
     order by y."date" desc
     limit 500;
end $$;
revoke all on function reports.refund_list(text, date, date) from public, anon;
grant execute on function reports.refund_list(text, date, date) to authenticated;

-- ── ৬) cash_handover_list ─────────────────────────────────────────────────
drop function reports.cash_handover_list(text, date, date);
create function reports.cash_handover_list(p_branch text, p_from date, p_to date)
returns table(branch text, handover_date text, cash numeric, receiver_name text, received_at text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, left(coalesce(c."date",''),10),
           coalesce(nullif(regexp_replace(coalesce(c."cashTotal"::text,'0'),'[^0-9.\-]','','g'),''),'0')::numeric,
           coalesce(c."receivedByName",''), coalesce(c."receivedAt",'')
      from public.chamber_close c
     where upper(trim(coalesce(c."branch",''))) = upper(trim(p_branch))
       and lower(coalesce(c."handoverStatus",'')) = 'received'
       and left(coalesce(c."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(c."date",''),10) <= to_char(p_to,'YYYY-MM-DD')
     order by c."date" desc
     limit 500;
end $$;
revoke all on function reports.cash_handover_list(text, date, date) from public, anon;
grant execute on function reports.cash_handover_list(text, date, date) to authenticated;

-- ── ৭) rmp_due_list ───────────────────────────────────────────────────────
drop function reports.rmp_due_list(text);
create function reports.rmp_due_list(p_branch text)
returns table(branch text, rmp_id text, rmp_name text, rmp_mobile text, due numeric)
language plpgsql stable security definer set search_path = hr, fin, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, b.rmp_id, b.rmp_name, b.rmp_mobile, b.due
      from fin.rmp_branch_due(p_branch) b
     order by b.due desc
     limit 500;
end $$;
revoke all on function reports.rmp_due_list(text) from public, anon;
grant execute on function reports.rmp_due_list(text) to authenticated;

-- ── ৮) product_due_list ───────────────────────────────────────────────────
drop function reports.product_due_list(text);
create function reports.product_due_list(p_branch text)
returns table(branch text, product_row_id text, customer text, mobile text, product text, due numeric, sold_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    with orig as (
      select p.id,
             coalesce(nullif(regexp_replace(coalesce(p."bill",'0'),'[^0-9.\-]','','g'),''),'0')::numeric as bill,
             coalesce(nullif(regexp_replace(coalesce(p."deposit",'0'),'[^0-9.\-]','','g'),''),'0')::numeric as deposit
        from public.products p
       where p."branch" = p_branch
         and p."kind" in ('medicinePayment','salinePayment')
         and p.id not like 'due\_%' escape '\'
    ),
    settle as (
      select regexp_replace(s.id, '^due_(.*)_[0-9]+$', '\1') as orig_id,
             coalesce(nullif(regexp_replace(coalesce(s."deposit",'0'),'[^0-9.\-]','','g'),''),'0')::numeric as settled
        from public.products s
       where s.id like 'due\_%' escape '\'
    ),
    live as (
      select o.id, greatest(o.bill - o.deposit - coalesce(sum(st.settled),0), 0) as due_live
        from orig o left join settle st on st.orig_id = o.id
       group by o.id, o.bill, o.deposit
    )
    select p_branch as branch, l.id, l.customer, l.mobile, l.product, l.due_live, l.sold_on
      from live l
     where l.due_live > 0
     order by l.sold_on desc
     limit 500;
end $$;
revoke all on function reports.product_due_list(text) from public, anon;
grant execute on function reports.product_due_list(text) to authenticated;

-- ── ৯) call_list ──────────────────────────────────────────────────────────
drop function reports.call_list(text, date, date);
create function reports.call_list(p_branch text, p_from date, p_to date)
returns table(branch text, call_row_id text, staff_code text, target_mobile_mask text, call_date text)
language plpgsql stable security definer set search_path = hr, wn, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, c.id::text, c.staff_code, coalesce(c.target_mobile_mask,''), to_char(c.call_date,'YYYY-MM-DD')
      from wn.call_taps c
      join hr.staff_profiles s on s.person_code = c.staff_code
     where lower(trim(coalesce(s.branch,''))) = lower(trim(p_branch))
       and c.call_date >= p_from and c.call_date <= p_to
     order by c.call_date desc, c.tapped_at desc
     limit 500;
end $$;
revoke all on function reports.call_list(text, date, date) from public, anon;
grant execute on function reports.call_list(text, date, date) to authenticated;

-- ── ১০) trash_list ────────────────────────────────────────────────────────
drop function reports.trash_list(text, date, date);
create function reports.trash_list(p_branch text, p_from date, p_to date)
returns table(branch text, trash_row_id text, table_name text, deleted_at text, deleted_by text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, t.id, coalesce(t."table",''), coalesce(t."deletedAt",''), coalesce(t."deletedBy",'')
      from public.trash t
     where lower(trim(coalesce(t."record"->>'branch',''))) = lower(trim(p_branch))
       and left(coalesce(t."deletedAt",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(t."deletedAt",''),10) <= to_char(p_to,'YYYY-MM-DD')
     order by t."deletedAt" desc
     limit 500;
end $$;
revoke all on function reports.trash_list(text, date, date) from public, anon;
grant execute on function reports.trash_list(text, date, date) to authenticated;

-- ── ১১) rmp_advance_list ──────────────────────────────────────────────────
drop function reports.rmp_advance_list(text, date, date);
create function reports.rmp_advance_list(p_branch text, p_from date, p_to date)
returns table(branch text, advance_id text, rmp_name text, amount numeric, mode text, paid_on text)
language plpgsql stable security definer set search_path = hr, fin, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, a.id::text, a.rmp_name, a.amount, a.mode, to_char(a.paid_on,'YYYY-MM-DD')
      from fin.rmp_advance_payments a
     where a.branch = p_branch
       and a.paid_on >= p_from and a.paid_on <= p_to
     order by a.paid_on desc
     limit 500;
end $$;
revoke all on function reports.rmp_advance_list(text, date, date) from public, anon;
grant execute on function reports.rmp_advance_list(text, date, date) to authenticated;

-- ── ১২) appointment_list ──────────────────────────────────────────────────
drop function reports.appointment_list(text, date, date);
create function reports.appointment_list(p_branch text, p_from date, p_to date)
returns table(branch text, enquiry_row_id text, name text, mobile text, disease text, appointment_date text, registered boolean)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, e.id, coalesce(e."name",''), coalesce(e."mobile",''), coalesce(e."disease",''),
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

-- ── ১৩) expected_list ─────────────────────────────────────────────────────
drop function reports.expected_list(text, date, date);
create function reports.expected_list(p_branch text, p_from date, p_to date)
returns table(branch text, mark_id text, patient_row_id text, name text, mobile text, expected_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, y.id, coalesce(y."patientId",''), coalesce(y."name",''), coalesce(y."mobile",''), left(coalesce(y."date",''),10)
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

-- ── ১৪) handover_pending_list ─────────────────────────────────────────────
drop function reports.handover_pending_list(text);
create function reports.handover_pending_list(p_branch text)
returns table(branch text, handover_date text, cash numeric, status text)
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
    select p_branch as branch, r.d, r.cash, case when r.st = '' then 'not started' else r.st end
      from rows0 r where r.cash > 0
     order by r.d desc
     limit 500;
end $$;
revoke all on function reports.handover_pending_list(text) from public, anon;
grant execute on function reports.handover_pending_list(text) to authenticated;

-- ── ১৫) payment_requests_list (ভেতরের select * → স্পষ্ট কলাম, union-অংশ অক্ষত) ──
drop function reports.payment_requests_list(text);
create function reports.payment_requests_list(p_branch text)
returns table(branch text, request_id text, request_type text, name text, mobile text, amount numeric, requested_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, u.request_id, u.request_type, u.name, u.mobile, u.amount, u.requested_on from (
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

-- ── ১৬) referral_requests_list ────────────────────────────────────────────
drop function reports.referral_requests_list(text);
create function reports.referral_requests_list(p_branch text)
returns table(branch text, request_id text, request_type text, new_amount numeric, requested_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, x.id::text, case when coalesce(x."isDelete",false) then 'Delete' else 'Edit' end,
           coalesce(x."newAmount",0)::numeric, left(coalesce(x."requestedAt",''),10)
      from public.referral_edit_requests x
     where lower(coalesce(x.status,'')) = 'pending' and lower(trim(coalesce(x.branch,''))) = lower(trim(p_branch))
     order by x."requestedAt" desc
     limit 500;
end $$;
revoke all on function reports.referral_requests_list(text) from public, anon;
grant execute on function reports.referral_requests_list(text) to authenticated;

-- ── ১৭) leave_list ────────────────────────────────────────────────────────
drop function reports.leave_list(text, date, date);
create function reports.leave_list(p_branch text, p_from date, p_to date)
returns table(branch text, staff_code text, leave_date text, status text, applied_on text)
language plpgsql stable security definer set search_path = hr, wn, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, l.staff_code, to_char(l.leave_date,'YYYY-MM-DD'), coalesce(l.status,''),
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

-- ── ১৮) doctor_reminder_list ──────────────────────────────────────────────
drop function reports.doctor_reminder_list(text, date, date);
create function reports.doctor_reminder_list(p_branch text, p_from date, p_to date)
returns table(branch text, reminder_id text, remind_date text, created_on text, accepted boolean, cancelled boolean)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, d.id::text, left(coalesce(d."remindDate",''),10), left(coalesce(d."createdAt",''),10),
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

-- ── ১৯) staff_reminder_open_list ──────────────────────────────────────────
drop function reports.staff_reminder_open_list(text);
create function reports.staff_reminder_open_list(p_branch text)
returns table(branch text, reminder_id text, to_name text, to_code text, reminder_type text, remind_on text, status text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, r.id::text, coalesce(r."toName",''), coalesce(r."toCode",''), coalesce(r."type",''),
           left(coalesce(r."remindOn",''),10), coalesce(r."status",'')
      from public.reminders r
     where lower(trim(coalesce(r."branch",''))) = lower(trim(p_branch))
       and lower(coalesce(r."status",'')) <> 'done'
     order by r."remindOn" desc
     limit 500;
end $$;
revoke all on function reports.staff_reminder_open_list(text) from public, anon;
grant execute on function reports.staff_reminder_open_list(text) to authenticated;

-- ── ২০) fee_return_list ───────────────────────────────────────────────────
drop function reports.fee_return_list(text, date, date);
create function reports.fee_return_list(p_branch text, p_from date, p_to date)
returns table(branch text, payment_id text, patient_row_id text, name text, mobile text, amount numeric, returned_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, y.id, coalesce(y."patientId",''), coalesce(y."name",''), coalesce(y."mobile",''),
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

-- ── ২১) chamber_unclosed_list ─────────────────────────────────────────────
drop function reports.chamber_unclosed_list(text, date, date);
create function reports.chamber_unclosed_list(p_branch text, p_from date, p_to date)
returns table(branch text, chamber_date text, arrived int, money numeric)
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
    select p_branch as branch, a.d, a.arrived, coalesce(a.money,0)
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

-- ── ২২) no_show_list ──────────────────────────────────────────────────────
drop function reports.no_show_list(text, date, date);
create function reports.no_show_list(p_branch text, p_from date, p_to date)
returns table(branch text, name text, mobile text, expected_on text, arrived boolean)
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
    select p_branch as branch, e.nm, e.mob, e.d,
           exists (select 1 from arr a where a.m = e.m and a.m <> '' and a.d = e.d)
      from exp e
     order by e.d desc, e.nm
     limit 500;
end $$;
revoke all on function reports.no_show_list(text, date, date) from public, anon;
grant execute on function reports.no_show_list(text, date, date) to authenticated;

-- ── ২৩) out_missing_list ──────────────────────────────────────────────────
drop function reports.out_missing_list(text, date, date);
create function reports.out_missing_list(p_branch text, p_from date, p_to date)
returns table(branch text, staff_code text, work_date text, check_in text)
language plpgsql stable security definer set search_path = hr, wn, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, n.staff_code, to_char(n.work_date,'YYYY-MM-DD'), coalesce(n.check_in,'')
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

-- ── ২৪) wfh_list ──────────────────────────────────────────────────────────
drop function reports.wfh_list(text, date, date);
create function reports.wfh_list(p_branch text, p_from date, p_to date)
returns table(branch text, staff_name text, staff_code text, work_date text, status text, requested_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, coalesce(w."staffName",''), coalesce(w."staffCode",''), left(coalesce(w."workDate",''),10),
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

-- ── ২৫) duplicate_list ────────────────────────────────────────────────────
drop function reports.duplicate_list(text);
create function reports.duplicate_list(p_branch text)
returns table(branch text, mobile text, row_count int, names text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, x.m, count(*)::int, string_agg(distinct coalesce(p2."name",''), ' / ')
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

-- ── ২৬) fee_unpaid_list ───────────────────────────────────────────────────
drop function reports.fee_unpaid_list(text);
create function reports.fee_unpaid_list(p_branch text)
returns table(branch text, patient_row_id text, patient_code text, name text, mobile text, registration_date text)
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
    select p_branch as branch, mi.id, mi.code, mi.nm, mi.mob, mi.rd from miss mi
     order by mi.rd desc, mi.nm
     limit 500;
end $$;
revoke all on function reports.fee_unpaid_list(text) from public, anon;
grant execute on function reports.fee_unpaid_list(text) to authenticated;

-- ── ২৭) calls_pending_list ────────────────────────────────────────────────
drop function reports.calls_pending_list(text);
create function reports.calls_pending_list(p_branch text)
returns table(branch text, followup_id text, name text, mobile text, stage text, next_follow text)
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
    select p_branch as branch, d.id, d.nm, d.mob, d.st, d.nf from dedup d
     order by d.nf, d.nm
     limit 500;
end $$;
revoke all on function reports.calls_pending_list(text) from public, anon;
grant execute on function reports.calls_pending_list(text) to authenticated;

-- ── ২৮) messages_list (ডাইনামিক SQL — $1 = p_branch, তাই '$1 as branch') ──────
drop function reports.messages_list(text, date, date);
create function reports.messages_list(p_branch text, p_from date, p_to date)
returns table(branch text, name text, mobile text, kind text, channel text, sent_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  if to_regclass('public.message_log') is null then raise exception 'message_log table not found'; end if;
  return query execute
    'select $1 as branch, coalesce(m.name,''''), coalesce(m.mobile,''''), coalesce(m.kind,''''), coalesce(m.channel,''''),
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

-- ── ২৯) new_patients_list ─────────────────────────────────────────────────
drop function reports.new_patients_list(text, date, date);
create function reports.new_patients_list(p_branch text, p_from date, p_to date)
returns table(branch text, patient_row_id text, patient_code text, name text, mobile text, registration_date text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, p.id, coalesce(p."patientId",''), coalesce(p."name",''), coalesce(p."mobile",''),
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

-- ── ৩০) followup_calls_done_list ──────────────────────────────────────────
drop function reports.followup_calls_done_list(text, date, date);
create function reports.followup_calls_done_list(p_branch text, p_from date, p_to date)
returns table(branch text, followup_id text, name text, mobile text, call_day text, remarks int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, f.id, coalesce(f."name",''), coalesce(f."mobile",''), left(coalesce(h->>'date',''),10), count(*)::int
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

-- ── ৩১) disease_list ──────────────────────────────────────────────────────
drop function reports.disease_list(text, date, date, text);
create function reports.disease_list(p_branch text, p_from date, p_to date, p_disease text)
returns table(branch text, patient_row_id text, patient_code text, name text, mobile text, disease text, registration_date text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if p_disease not in ('Piles','Fissure','Fistula','Hydrocele','Gupt Rog','Other') then raise exception 'Unknown disease'; end if;
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, p.id, coalesce(p."patientId",''), coalesce(p."name",''), coalesce(p."mobile",''),
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

-- ── ৩২) rmp_called_list ───────────────────────────────────────────────────
drop function reports.rmp_called_list(text, date, date);
create function reports.rmp_called_list(p_branch text, p_from date, p_to date)
returns table(branch text, rmp_id text, name text, mobile text, last_call_date text, next_call_date text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, d.id, coalesce(d."name",''), coalesce(d."mobile",''), left(coalesce(d."lastCallDate",''),10), left(coalesce(d."nextCallDate",''),10)
      from public.doctor_visits d
     where lower(trim(coalesce(d."branch",''))) = lower(trim(p_branch))
       and left(coalesce(d."lastCallDate",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(d."lastCallDate",''),10) <= to_char(p_to,'YYYY-MM-DD')
     order by d."lastCallDate" desc, d."name"
     limit 500;
end $$;
revoke all on function reports.rmp_called_list(text, date, date) from public, anon;
grant execute on function reports.rmp_called_list(text, date, date) to authenticated;

-- ── ৩৩) rmp_call_due_list ─────────────────────────────────────────────────
drop function reports.rmp_call_due_list(text, date, date);
create function reports.rmp_call_due_list(p_branch text, p_from date, p_to date)
returns table(branch text, rmp_id text, name text, mobile text, next_call_date text, last_call_date text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, d.id, coalesce(d."name",''), coalesce(d."mobile",''), left(coalesce(d."nextCallDate",''),10), left(coalesce(d."lastCallDate",''),10)
      from public.doctor_visits d
     where lower(trim(coalesce(d."branch",''))) = lower(trim(p_branch))
       and left(coalesce(d."nextCallDate",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(d."nextCallDate",''),10) <= to_char(p_to,'YYYY-MM-DD')
     order by d."nextCallDate", d."name"
     limit 500;
end $$;
revoke all on function reports.rmp_call_due_list(text, date, date) from public, anon;
grant execute on function reports.rmp_call_due_list(text, date, date) to authenticated;

-- ── ৩৪) field_visit_list — ব্যতিক্রম: branch শেষে বসেছে, কারণ ভেতরে
--        `order by 2 desc, 1` কলাম-পজিশন ধরে (branch প্রথমে বসালে সেটা ভেঙে যেত) ──
drop function reports.field_visit_list(text, date, date);
create function reports.field_visit_list(p_branch text, p_from date, p_to date)
returns table(staff_code text, work_date text, visits int, km numeric, branch text)
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
            coalesce(d.visits,0), coalesce(f.km,0), $1 as branch
       from d full outer join f on f.staff_code = d.staff_code and f.work_date = d.work_date
      order by 2 desc, 1
      limit 500'
    using p_branch, p_from, p_to;
end $$;
revoke all on function reports.field_visit_list(text, date, date) from public, anon;
grant execute on function reports.field_visit_list(text, date, date) to authenticated;

-- ── ৩৫) staff_hours_list ──────────────────────────────────────────────────
drop function reports.staff_hours_list(text, date, date);
create function reports.staff_hours_list(p_branch text, p_from date, p_to date)
returns table(branch text, staff_code text, hours numeric, days int, leave_days int, out_missing_days int)
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
    select p_branch as branch, m.sc, round(sum(m.mins)/60.0, 1), count(*)::int,
           count(*) filter (where m.lv)::int, count(*) filter (where m.om)::int
      from m
     group by m.sc
     order by sum(m.mins) desc, m.sc
     limit 500;
end $$;
revoke all on function reports.staff_hours_list(text, date, date) from public, anon;
grant execute on function reports.staff_hours_list(text, date, date) to authenticated;

-- ── ৩৬) staff_present_list ────────────────────────────────────────────────
drop function reports.staff_present_list(text, date, date);
create function reports.staff_present_list(p_branch text, p_from date, p_to date)
returns table(branch text, staff_code text, work_date text, check_in text, check_out text)
language plpgsql stable security definer set search_path = hr, wn, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, n.staff_code, to_char(n.work_date,'YYYY-MM-DD'), coalesce(n.check_in,''),
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

-- ── ৩৭) rmp_paid_list ─────────────────────────────────────────────────────
drop function reports.rmp_paid_list(text, date, date);
create function reports.rmp_paid_list(p_branch text, p_from date, p_to date)
returns table(branch text, payment_id text, rmp_id text, rmp_name text, paid_on text, amount numeric, kind text, patient_name text, mode text)
language plpgsql stable security definer set search_path = hr, fin, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, x.pid, x.rid, x.rname, x.pon, x.amt, x.knd, x.pname, x.md
      from (
        select cp.id::text as pid, cp.rmp_id as rid, coalesce(cp.rmp_name,'') as rname,
               to_char(cp.paid_on,'YYYY-MM-DD') as pon, cp.amount as amt, 'patient'::text as knd,
               coalesce(pc.patient_name,'') as pname, coalesce(cp.mode,'') as md,
               cp.paid_on as d, cp.recorded_at as r
          from fin.rmp_commission_payments cp
          left join fin.rmp_patient_commissions pc on pc.id = cp.patient_commission_id
         where lower(trim(coalesce(cp.treatment_branch,''))) = lower(trim(p_branch))
           and cp.paid_on >= p_from and cp.paid_on <= p_to
        union all
        select ap.id::text, ap.rmp_id, coalesce(ap.rmp_name,''),
               to_char(ap.paid_on,'YYYY-MM-DD'), ap.amount, 'advance'::text,
               ''::text, coalesce(ap.mode,''), ap.paid_on, ap.recorded_at
          from fin.rmp_advance_payments ap
         where lower(trim(coalesce(ap.branch,''))) = lower(trim(p_branch))
           and ap.paid_on >= p_from and ap.paid_on <= p_to
      ) x
     order by x.d desc, x.r desc
     limit 500;
end $$;
revoke all on function reports.rmp_paid_list(text, date, date) from public, anon;
grant execute on function reports.rmp_paid_list(text, date, date) to authenticated;

-- ── ৩৮) in_missing_list ───────────────────────────────────────────────────
drop function reports.in_missing_list(text, date, date);
create function reports.in_missing_list(p_branch text, p_from date, p_to date)
returns table(branch text, staff_code text, staff_name text, work_date text, check_out text)
language plpgsql stable security definer set search_path = hr, wn, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p_branch as branch, n.staff_code, coalesce(s.full_name,''), to_char(n.work_date,'YYYY-MM-DD'),
           coalesce(nullif(lower(btrim(coalesce(n.check_out,''))),'null'),'')
      from wn.notebook_days n
      join hr.staff_profiles s on s.person_code = n.staff_code
     where lower(trim(coalesce(s.branch,''))) = lower(trim(p_branch))
       and n.work_date >= p_from and n.work_date <= p_to
       and coalesce(n.is_leave,false) = false
       and coalesce(n.is_wfh,false) = false
       and coalesce(n.is_other_branch,false) = false
       and coalesce(nullif(lower(btrim(coalesce(n.check_in,''))),'null'),'') = ''
     order by n.work_date desc, n.staff_code
     limit 500;
end $$;
revoke all on function reports.in_missing_list(text, date, date) from public, anon;
grant execute on function reports.in_missing_list(text, date, date) to authenticated;

notify pgrst, 'reload schema';
commit;
