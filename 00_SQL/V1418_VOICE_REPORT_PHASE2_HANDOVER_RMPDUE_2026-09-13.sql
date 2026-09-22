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
