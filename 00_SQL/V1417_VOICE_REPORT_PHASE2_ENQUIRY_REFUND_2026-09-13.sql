-- ═══════════════════════════════════════════════════════════════════════════
-- 🎤🔒 V1417 (১৩.০৯.২০২৬, TK-নির্দেশ "চালিয়ে যান") — ভয়েস-প্রশ্নের জবাব,
-- ধাপ ২ চলমান: এনকোয়ারি-সংখ্যা (VOICE_QUERY_PLAN আইটেম ১১) + রিফান্ড-টাকা
-- (আইটেম ১৪-র শুধু রিফান্ড অংশ — নিচে ব্যাখ্যা)। একই ছাঁচ, শুধু পড়া, ছোট্ট
-- যোগফল, Master-only, ব্রাঞ্চ-পাহারা।
--
-- ⚠️ যাচাই করে পাওয়া (আন্দাজ নয়, Explore সাবএজেন্ট দিয়ে কোড+DDL মিলিয়ে):
-- (ক) `public.enquiries`-এ প্রতিটা সারিই একটা নতুন এনকোয়ারি — ফলো-আপ কলের কোনো
--     সারি এখানে বসে না (ফলো-আপ কল `public.followups.history`-তে জমা হয়, একই
--     সারিতে, নতুন সারি নয়) — তাই আলাদা করে বাদ দেওয়ার দরকার নেই।
-- (খ) VOICE_QUERY_PLAN-এর আইটেম ১৪-এ "রিফান্ড/ডিসকাউন্ট" এক সাথে লেখা থাকলেও
--     ডিসকাউন্ট আসলে payments-এর সারি নয় — patients.discount (বিলের ছাড়),
--     সম্পূর্ণ আলাদা জায়গা। তাই এখানে শুধু **রিফান্ড** বসানো হলো, সততার সাথে;
--     ডিসকাউন্টের প্রশ্ন লাগলে ভবিষ্যতে আলাদা ফাংশন লাগবে (patients টেবিল থেকে)।
-- (গ) রিফান্ডের amount সবসময় সারিতে **পজিটিভ** বসে (V1415-এর collection_summary-র
--     মাইনাস চিহ্নটা শুধু নিট-কালেকশনের হিসাবের জন্য, এখানে দরকার নেই)।
-- ═══════════════════════════════════════════════════════════════════════════
begin;

-- ── ৪) কতজন এনকোয়ারি এসেছে ──────────────────────────────────────────────
create or replace function reports.enquiry_count(p_branch text, p_from date, p_to date)
returns int language sql stable security definer set search_path = hr, public as $$
  select case when not reports.can_access_branch(p_branch) then null::int else (
    select count(*)::int from public.enquiries e
     where e."branch" = p_branch
       and left(coalesce(e."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(e."date",''),10) <= to_char(p_to,'YYYY-MM-DD')
  ) end
$$;
revoke all on function reports.enquiry_count(text, date, date) from public, anon;
grant execute on function reports.enquiry_count(text, date, date) to authenticated;

create or replace function reports.enquiry_list(p_branch text, p_from date, p_to date)
returns table(enquiry_row_id text, name text, mobile text, disease text, enquiry_date text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select e.id, coalesce(e."name",''), coalesce(e."mobile",''), coalesce(e."disease",''),
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

-- ── ৫) কত টাকা রিফান্ড হয়েছে (Approved রিফান্ড, ডিসকাউন্ড নয়) ─────────────
create or replace function reports.refund_summary(p_branch text, p_from date, p_to date)
returns table(total numeric, refund_count int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    with rows0 as (
      select coalesce(nullif(regexp_replace(coalesce(y."amount",'0'),'[^0-9.\-]','','g'),'')::numeric,0) as amt
        from public.payments y
       where y."branch" = p_branch
         and lower(coalesce(y."payType",'')) = 'refund'
         and lower(coalesce(y."refundApprovalStatus",'')) = 'approved'
         and left(coalesce(y."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
         and left(coalesce(y."date",''),10) <= to_char(p_to,'YYYY-MM-DD')
    )
    select coalesce(sum(amt),0), count(*)::int from rows0;
end $$;
revoke all on function reports.refund_summary(text, date, date) from public, anon;
grant execute on function reports.refund_summary(text, date, date) to authenticated;

create or replace function reports.refund_list(p_branch text, p_from date, p_to date)
returns table(payment_id text, patient_row_id text, name text, mobile text, amount numeric, mode text, refunded_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select y.id, coalesce(y."patientId",''), coalesce(y."name",''), coalesce(y."mobile",''),
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

notify pgrst, 'reload schema';
commit;
