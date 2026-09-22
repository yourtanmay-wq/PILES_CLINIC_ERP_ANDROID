-- ═══════════════════════════════════════════════════════════════════════════
-- 🎤 ভয়েস-প্রশ্নের সব SQL এক ফাইলে (V1415 → V1421, ক্রমে) — ১৩.০৯.২০২৬
-- TK: Supabase → SQL Editor → পুরোটা পেস্ট → Run। V1415 আগে চালানো থাকলেও আবার
-- চালানো নিরাপদ (create or replace)। শেষে 'Success. No rows returned' আসবে।
-- ═══════════════════════════════════════════════════════════════════════════

-- ──────────────── V1415_VOICE_REPORT_PHASE1_2026-09-13.sql ────────────────
-- ═══════════════════════════════════════════════════════════════════════════
-- 🎤🔒 V1415 (১৩.০৯.২০২৬, TK-নির্দেশ "কাজ শুরু করে দিন", তালিকা ৫২০) —
-- ভয়েসে/টাইপ করে প্রশ্নের জবাব — প্রথম ধাপ (৪৮টার মধ্যে প্রথম ২টা):
--   ১) "[তারিখ] [ব্রাঞ্চ]-এ কতজন পেশেন্ট এসেছিল" — রেজিস্ট্রেশন-সংখ্যা
--   ২) "[তারিখ] [ব্রাঞ্চ]-এ কত কালেকশন হয়েছে" — জমা-টাকার মোট
-- প্রতিটার জন্য দুটো ফাংশন — একটা সংখ্যা/যোগফল (উত্তরে দেখানোর জন্য), আরেকটা
-- আসল তালিকা (উত্তরে চাপ দিলে যে পাতা খুলবে তার জন্য) — Master-এর RMP-খাতার
-- মতোই ব্রাঞ্চ-পাহারা (নিজের ব্রাঞ্চ ছাড়া দেখা যাবে না, Master সব দেখেন)।
-- ⛔ শুধু পড়া, কোনো টেবিল বদলায় না। শুধু ছোট যোগফল/গণনা ফেরত আসে —
--    হাজার হাজার সারি টেনে আনা হয় না (ফ্রি প্ল্যান-নিরাপদ)।
-- ═══════════════════════════════════════════════════════════════════════════
begin;

create schema if not exists reports;

-- 🔒 TK-নির্দেশ (১৩.০৯.২০২৬): "ভয়েস কমান্ড আপাতত শুধু মাস্টারের জন্য থাকবে —
-- ভবিষ্যতে অন্যদের জন্য চালু করতে চাইলে তখন চালু হবে।" তাই এখন শুধু Master;
-- নিজের ব্রাঞ্চের স্টাফকেও দেখানো হলে ভবিষ্যতে নিচের `or exists(...)` অংশটা
-- আনকমেন্ট করলেই যথেষ্ট — বাকি কোনো ফাংশন/অ্যাপ-কোড বদলাতে হবে না।
create or replace function reports.can_access_branch(p_branch text) returns boolean
language sql stable security definer set search_path = hr, public as $$
  select hr.is_master()
  -- or exists(
  --   select 1 from hr.staff_profiles s
  --    where s.person_code = hr.my_code() and s.active is not false
  --      and lower(trim(coalesce(s.branch,''))) = lower(trim(coalesce(p_branch,'')))
  -- )
$$;
revoke all on function reports.can_access_branch(text) from public, anon;
grant execute on function reports.can_access_branch(text) to authenticated;

-- ── ১) কতজন পেশেন্ট রেজিস্ট্রেশন হয়েছে ───────────────────────────────────
create or replace function reports.patients_registered_count(p_branch text, p_from date, p_to date)
returns int language sql stable security definer set search_path = hr, public as $$
  select case when not reports.can_access_branch(p_branch) then null::int else (
    select count(*)::int from public.patients p
     where p."branch" = p_branch
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(nullif(p."registrationDate",''), p."date", ''), 10) <= to_char(p_to,'YYYY-MM-DD')
  ) end
$$;
revoke all on function reports.patients_registered_count(text, date, date) from public, anon;
grant execute on function reports.patients_registered_count(text, date, date) to authenticated;

create or replace function reports.patients_registered_list(p_branch text, p_from date, p_to date)
returns table(patient_row_id text, patient_code text, name text, mobile text, registration_date text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select p.id, coalesce(p."patientId",''), coalesce(p.name,''), coalesce(p.mobile,''),
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

-- ── ২) কত টাকা কালেকশন হয়েছে (রিফান্ড বাদ, Approved রিফান্ড বিয়োগ) ────────
create or replace function reports.collection_summary(p_branch text, p_from date, p_to date)
returns table(total numeric, patient_count int, payment_count int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    with rows0 as (
      select y.id, y."patientId",
             case when lower(coalesce(y."payType",'')) = 'refund'
                       and lower(coalesce(y."refundApprovalStatus",'')) = 'approved'
                  then -coalesce(nullif(regexp_replace(coalesce(y."amount",'0'),'[^0-9.\-]','','g'),'')::numeric,0)
                  when lower(coalesce(y."payType",'')) = 'refund' then 0
                  else coalesce(nullif(regexp_replace(coalesce(y."amount",'0'),'[^0-9.\-]','','g'),'')::numeric,0)
             end as eff
        from public.payments y
       where y."branch" = p_branch
         and left(coalesce(y."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
         and left(coalesce(y."date",''),10) <= to_char(p_to,'YYYY-MM-DD')
    )
    select coalesce(sum(eff),0), count(distinct "patientId")::int, count(*)::int from rows0;
end $$;
revoke all on function reports.collection_summary(text, date, date) from public, anon;
grant execute on function reports.collection_summary(text, date, date) to authenticated;

create or replace function reports.collection_list(p_branch text, p_from date, p_to date)
returns table(payment_id text, patient_row_id text, name text, mobile text, amount numeric, mode text, pay_type text, paid_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select y.id, coalesce(y."patientId",''), coalesce(p.name,''), coalesce(y.mobile,''),
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

notify pgrst, 'reload schema';
commit;


-- ──────────────── V1416_VOICE_REPORT_PHASE2_PRODUCT_SALE_2026-09-13.sql ────────────────
-- ═══════════════════════════════════════════════════════════════════════════
-- 🎤🔒 V1416 (১৩.০৯.২০২৬, TK-নির্দেশ "শুরু করে দিন") — ভয়েস-প্রশ্নের জবাব,
-- দ্বিতীয় ধাপ: TK-র নিজের ৫ নম্বর উদাহরণ ("লাস্ট সাত দিনে কত টাকার মেডিসিন
-- বিক্রি হয়েছে কোচবিহারে") + স্যালাইনের সমতুল্য (VOICE_QUERY_PLAN আইটেম ৪ ও ২১)।
-- একই ছাঁচ (V1415-এর মতোই) — শুধু পড়া, ছোট্ট যোগফল, Master-only, ব্রাঞ্চ-পাহারা।
--
-- ⚠️ যাচাই করে পাওয়া (আন্দাজ নয়, `products` টেবিলের DDL ও MedicinePaymentActivity.kt
-- মিলিয়ে): এই টেবিলের প্রতিটা ঘর text — bill/deposit/due সংখ্যা মনে হলেও আসলে text,
-- তাই cast লাগবে। "kind" ঘরে শুধু দুটো মান আছে: medicinePayment / salinePayment।
-- "বিক্রির টাকা" = bill (প্রতিটা সারির নিজস্ব বিল, due-সেটলমেন্ট সারিতে bill=0 বলে
-- এমনিতেই যোগ হয় না, আলাদা করে বাদ দেওয়ার দরকার নেই)। এই টেবিলে patientId নেই —
-- শুধু নাম/মোবাইল (free text), তাই তালিকায় Patient Timeline-এ যাওয়ার বোতাম শুধু
-- ১০ ডিজিট আসল মোবাইল থাকলেই দেখানো হবে (VoiceReportDetailActivity-র মতোই)।
-- ═══════════════════════════════════════════════════════════════════════════
begin;

-- ── ৩) কত টাকার মেডিসিন/স্যালাইন বিক্রি হয়েছে ───────────────────────────
create or replace function reports.product_sale_summary(p_branch text, p_from date, p_to date, p_kind text)
returns table(total numeric, sale_count int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if p_kind not in ('medicinePayment','salinePayment') then
    raise exception 'Invalid kind';
  end if;
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    with rows0 as (
      select coalesce(nullif(regexp_replace(coalesce(y."bill",'0'),'[^0-9.\-]','','g'),'')::numeric,0) as b
        from public.products y
       where y."branch" = p_branch
         and y."kind" = p_kind
         and left(coalesce(y."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
         and left(coalesce(y."date",''),10) <= to_char(p_to,'YYYY-MM-DD')
    )
    select coalesce(sum(b),0), count(*) filter (where b > 0)::int from rows0;
end $$;
revoke all on function reports.product_sale_summary(text, date, date, text) from public, anon;
grant execute on function reports.product_sale_summary(text, date, date, text) to authenticated;

create or replace function reports.product_sale_list(p_branch text, p_from date, p_to date, p_kind text)
returns table(product_row_id text, customer text, mobile text, product text, bill numeric, deposit numeric, due numeric, mode text, sold_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if p_kind not in ('medicinePayment','salinePayment') then
    raise exception 'Invalid kind';
  end if;
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select y.id, coalesce(y."customer",''), coalesce(y."mobile",''), coalesce(y."product",''),
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

notify pgrst, 'reload schema';
commit;


-- ──────────────── V1417_VOICE_REPORT_PHASE2_ENQUIRY_REFUND_2026-09-13.sql ────────────────
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


-- ──────────────── V1418_VOICE_REPORT_PHASE2_HANDOVER_RMPDUE_2026-09-13.sql ────────────────
-- ═══════════════════════════════════════════════════════════════════════════
-- 🎤🔒 V1418 (১৩.০৯.২০২৬, TK-নির্দেশ "চালিয়ে যান") — ভয়েস-প্রশ্নের জবাব,
-- ধাপ ২ চলমান: ক্যাশ হ্যান্ডওভার (VOICE_QUERY_PLAN আইটেম ১৫) + RMP-দের
-- ব্রাঞ্চ-বাকি (আইটেম ২৫, TK-র নিজের ৮ নম্বর সাবধানতার জায়গা)।
--
-- ⚠️ যাচাই করে পাওয়া (আন্দাজ নয়, Explore সাবএজেন্ট দিয়ে কোড মিলিয়ে):
-- (ক) হ্যান্ডওভারের টেবিল আলাদা কিছু নয় — `chamber_close`-এরই কয়েকটা ঘর
--     (MoneyHandover.kt-এর নিজস্ব মন্তব্য অনুযায়ী)। এই টেবিলের "branch" ঘর
--     সবসময় **বড় হাতের অক্ষরে** বসে (অন্য সব টেবিলের মতো normal-case নয়) —
--     তাই এখানে upper(trim(...)) মিলিয়ে দেখা বাধ্যতামূলক, নইলে শূন্য সারি আসবে।
-- (খ) "handoverStatus" না মিললে টাকা স্টাফের কাছেই রয়ে গেছে ধরে নিতে হয় —
--     শুধু 'received' (রিসিভার নিশ্চিত করেছেন) গোনা হবে, 'waiting'/'pending'/''
--     নয়, নইলে এখনো হাতে-না-আসা টাকাও "হয়ে গেছে" বলে দেখানো হবে।
-- (গ) TK-র নিজের টাকা শুধু ক্যাশ বোঝানো হয় ("অনলাইন সরাসরি অফিসে চলে আসে") —
--     তাই cashTotal ঘরটাই দেখানো হচ্ছে, grandTotal/feesTotal নয়।
-- (ঘ) RMP-দের বাকি — TK-র রাতের "একটাই সার্ভার-নিয়ম" (CLAUDE.md ৭গ) মেনে
--     নতুন কোনো হিসাব বসানো হয়নি, শুধু আজই বানানো `fin.rmp_branch_due(p_branch)`
--     ডাকা হচ্ছে (V1406) — নিজে কোনো যোগ-বিয়োগ করছে না এই ফাংশন।
-- ═══════════════════════════════════════════════════════════════════════════
begin;

-- ── ৬) কত টাকা ক্যাশ হ্যান্ডওভার হয়েছে ───────────────────────────────────
create or replace function reports.cash_handover_summary(p_branch text, p_from date, p_to date)
returns table(total numeric, day_count int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select coalesce(sum(coalesce(nullif(regexp_replace(coalesce(c."cashTotal"::text,'0'),'[^0-9.\-]','','g'),''),'0')::numeric),0),
           count(*)::int
      from public.chamber_close c
     where upper(trim(coalesce(c."branch",''))) = upper(trim(p_branch))
       and lower(coalesce(c."handoverStatus",'')) = 'received'
       and left(coalesce(c."date",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(c."date",''),10) <= to_char(p_to,'YYYY-MM-DD');
end $$;
revoke all on function reports.cash_handover_summary(text, date, date) from public, anon;
grant execute on function reports.cash_handover_summary(text, date, date) to authenticated;

create or replace function reports.cash_handover_list(p_branch text, p_from date, p_to date)
returns table(handover_date text, cash numeric, receiver_name text, received_at text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select left(coalesce(c."date",''),10),
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

-- ── ৭) এই ব্রাঞ্চে RMP-দের মোট কত কমিশন বাকি (একই fin.rmp_branch_due, নতুন হিসাব নয়) ──
create or replace function reports.rmp_due_summary(p_branch text)
returns table(total_due numeric, rmp_count int)
language plpgsql stable security definer set search_path = hr, fin, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select coalesce(sum(b.due),0), count(*)::int
      from fin.rmp_branch_due(p_branch) b;
end $$;
revoke all on function reports.rmp_due_summary(text) from public, anon;
grant execute on function reports.rmp_due_summary(text) to authenticated;

create or replace function reports.rmp_due_list(p_branch text)
returns table(rmp_id text, rmp_name text, rmp_mobile text, due numeric)
language plpgsql stable security definer set search_path = hr, fin, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select b.rmp_id, b.rmp_name, b.rmp_mobile, b.due
      from fin.rmp_branch_due(p_branch) b
     order by b.due desc
     limit 500;
end $$;
revoke all on function reports.rmp_due_list(text) from public, anon;
grant execute on function reports.rmp_due_list(text) to authenticated;

notify pgrst, 'reload schema';
commit;


-- ──────────────── V1419_VOICE_REPORT_PHASE2_DUE_CALLS_TRASH_ADVANCE_2026-09-13.sql ────────────────
-- ═══════════════════════════════════════════════════════════════════════════
-- 🎤🔒 V1419 (১৩.০৯.২০২৬, TK-নির্দেশ "একসাথে যতগুলো সম্ভব করুন, সাবধানে ও
-- সততার সাথে") — ভয়েস-প্রশ্নের জবাব, ধাপ ২ — এক ব্যাচে ৪টা প্যাটার্ন:
--   ৮) মেডিসিন/স্যালাইনের বর্তমান মোট বাকি (আইটেম ২২)
--   ৯) অ্যাপ থেকে কতগুলো কল করা হয়েছে (আইটেম ৩৯)
--  ১০) এই মাসে কতগুলো রেকর্ড ট্র্যাশে গেছে (আইটেম ৪৩)
--  ১১) RMP-দের এই সময়ে কত টাকা অগ্রিম দেওয়া হয়েছে (আইটেম ২৬)
--
-- ⚠️ যাচাই করে পাওয়া (আন্দাজ নয়, Explore সাবএজেন্ট দিয়ে কোড মিলিয়ে):
-- (ক) products.due ঘরটা সরাসরি যোগ করলে **ভুল** হবে — due মেটানো হলে পুরনো
--     সারির due বদলায় না, বরং "due_<originalId>_<millis>" নামে নতুন আলাদা সারি
--     বসে (MedicinePaymentActivity.kt/app.js মিলিয়ে দেখা)। তাই এখানে
--     MedicineDue.kt-এর নিজস্ব নিয়মেই "আসল বাকি" গোনা হচ্ছে: বিল - জমা -
--     পরে-মেটানো সারিগুলোর যোগফল। এই প্রশ্নে কোনো সময়-সীমা লাগে না (RMP-বাকির
--     মতোই এখন-পর্যন্ত-মোট-বাকি প্রশ্ন)।
-- (খ) কল-সংখ্যা `wn.call_taps`-এ জমা হয় (V246), কিন্তু এই টেবিলে নিজের কোনো
--     branch ঘর নেই — staff_code দিয়ে hr.staff_profiles.branch থেকে বের করতে
--     হয় (V1415-এর নিজস্ব কমেন্ট করে-রাখা কোডেও একই জোড়া-নিয়ম লেখা আছে)।
-- (গ) ট্র্যাশ — `public.trash`-এ নিজের কোনো branch ঘর নেই, পুরো মোছা-সারিটাই
--     `record` (jsonb) ঘরে জমা থাকে, তাই branch বের হয় `record->>'branch'` দিয়ে।
--     `"table"` PostgreSQL-এর সংরক্ষিত শব্দ, তাই সবসময় ডাবল-কোটে।
-- (ঘ) RMP-অগ্রিম (`fin.rmp_advance_payments`) — এই একটাই টাকার ঘর (amount)
--     ইতিমধ্যে numeric (text নয়), তাই আলাদা cast লাগেনি; branch সরাসরি ঘরে আছে।
-- ═══════════════════════════════════════════════════════════════════════════
begin;

-- ── ৮) মেডিসিন/স্যালাইনের বর্তমান মোট বাকি (settlement-সারি বাদ দিয়ে আসল হিসাব) ──
create or replace function reports.product_due_summary(p_branch text)
returns table(total numeric, row_count int)
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
    select coalesce(sum(due_live),0), count(*) filter (where due_live > 0)::int from live;
end $$;
revoke all on function reports.product_due_summary(text) from public, anon;
grant execute on function reports.product_due_summary(text) to authenticated;

create or replace function reports.product_due_list(p_branch text)
returns table(product_row_id text, customer text, mobile text, product text, due numeric, sold_on text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    with orig as (
      select p.id, p."customer" as customer, p."mobile" as mobile, p."product" as product,
             left(coalesce(p."date",''),10) as sold_on,
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
      select o.id, o.customer, o.mobile, o.product, o.sold_on,
             greatest(o.bill - o.deposit - coalesce(sum(st.settled),0), 0) as due_live
        from orig o left join settle st on st.orig_id = o.id
       group by o.id, o.customer, o.mobile, o.product, o.sold_on, o.bill, o.deposit
    )
    select l.id, l.customer, l.mobile, l.product, l.due_live, l.sold_on
      from live l
     where l.due_live > 0
     order by l.sold_on desc
     limit 500;
end $$;
revoke all on function reports.product_due_list(text) from public, anon;
grant execute on function reports.product_due_list(text) to authenticated;

-- ── ৯) অ্যাপ থেকে কতগুলো কল করা হয়েছে (wn.call_taps + hr.staff_profiles জোড়) ──
create or replace function reports.call_count(p_branch text, p_from date, p_to date)
returns int language sql stable security definer set search_path = hr, wn, public as $$
  select case when not reports.can_access_branch(p_branch) then null::int else (
    select count(*)::int
      from wn.call_taps c
      join hr.staff_profiles s on s.person_code = c.staff_code
     where lower(trim(coalesce(s.branch,''))) = lower(trim(p_branch))
       and c.call_date >= p_from and c.call_date <= p_to
  ) end
$$;
revoke all on function reports.call_count(text, date, date) from public, anon;
grant execute on function reports.call_count(text, date, date) to authenticated;

create or replace function reports.call_list(p_branch text, p_from date, p_to date)
returns table(call_row_id text, staff_code text, target_mobile_mask text, call_date text)
language plpgsql stable security definer set search_path = hr, wn, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select c.id::text, c.staff_code, coalesce(c.target_mobile_mask,''), to_char(c.call_date,'YYYY-MM-DD')
      from wn.call_taps c
      join hr.staff_profiles s on s.person_code = c.staff_code
     where lower(trim(coalesce(s.branch,''))) = lower(trim(p_branch))
       and c.call_date >= p_from and c.call_date <= p_to
     order by c.call_date desc, c.tapped_at desc
     limit 500;
end $$;
revoke all on function reports.call_list(text, date, date) from public, anon;
grant execute on function reports.call_list(text, date, date) to authenticated;

-- ── ১০) এই মাসে কতগুলো রেকর্ড ট্র্যাশে গেছে (public.trash, branch = record জসন থেকে) ──
create or replace function reports.trash_summary(p_branch text, p_from date, p_to date)
returns table(total int)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select count(*)::int
      from public.trash t
     where lower(trim(coalesce(t."record"->>'branch',''))) = lower(trim(p_branch))
       and left(coalesce(t."deletedAt",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(t."deletedAt",''),10) <= to_char(p_to,'YYYY-MM-DD');
end $$;
revoke all on function reports.trash_summary(text, date, date) from public, anon;
grant execute on function reports.trash_summary(text, date, date) to authenticated;

create or replace function reports.trash_list(p_branch text, p_from date, p_to date)
returns table(trash_row_id text, table_name text, deleted_at text, deleted_by text)
language plpgsql stable security definer set search_path = hr, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select t.id, coalesce(t."table",''), coalesce(t."deletedAt",''), coalesce(t."deletedBy",'')
      from public.trash t
     where lower(trim(coalesce(t."record"->>'branch',''))) = lower(trim(p_branch))
       and left(coalesce(t."deletedAt",''),10) >= to_char(p_from,'YYYY-MM-DD')
       and left(coalesce(t."deletedAt",''),10) <= to_char(p_to,'YYYY-MM-DD')
     order by t."deletedAt" desc
     limit 500;
end $$;
revoke all on function reports.trash_list(text, date, date) from public, anon;
grant execute on function reports.trash_list(text, date, date) to authenticated;

-- ── ১১) RMP-দের এই সময়ে মোট কত টাকা অগ্রিম দেওয়া হয়েছে (fin.rmp_advance_payments) ──
create or replace function reports.rmp_advance_summary(p_branch text, p_from date, p_to date)
returns table(total numeric, advance_count int)
language plpgsql stable security definer set search_path = hr, fin, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select coalesce(sum(a.amount),0), count(*)::int
      from fin.rmp_advance_payments a
     where a.branch = p_branch
       and a.paid_on >= p_from and a.paid_on <= p_to;
end $$;
revoke all on function reports.rmp_advance_summary(text, date, date) from public, anon;
grant execute on function reports.rmp_advance_summary(text, date, date) to authenticated;

create or replace function reports.rmp_advance_list(p_branch text, p_from date, p_to date)
returns table(advance_id text, rmp_name text, amount numeric, mode text, paid_on text)
language plpgsql stable security definer set search_path = hr, fin, public as $$
begin
  if not reports.can_access_branch(p_branch) then raise exception 'Not allowed for this branch'; end if;
  return query
    select a.id::text, a.rmp_name, a.amount, a.mode, to_char(a.paid_on,'YYYY-MM-DD')
      from fin.rmp_advance_payments a
     where a.branch = p_branch
       and a.paid_on >= p_from and a.paid_on <= p_to
     order by a.paid_on desc
     limit 500;
end $$;
revoke all on function reports.rmp_advance_list(text, date, date) from public, anon;
grant execute on function reports.rmp_advance_list(text, date, date) to authenticated;

notify pgrst, 'reload schema';
commit;


-- ──────────────── V1420_VOICE_REPORT_PHASE2_APPT_EXPECTED_REQUESTS_LEAVE_REMINDERS_2026-09-13.sql ────────────────
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


-- ──────────────── V1421_VOICE_REPORT_PHASE2_UNCLOSED_NOSHOW_OUT_WFH_DUP_FEE_CALLS_MSG_2026-09-13.sql ────────────────
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

