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
