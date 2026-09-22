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
