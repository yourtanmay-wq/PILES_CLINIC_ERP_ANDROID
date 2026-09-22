-- =====================================================================
-- V1289 (১০.০৯.২০২৬) — তালিকা ৪১১-⑦ (ক): "updatedAt" সব সারিতে অ্যাপের এক ছাঁচে
--   অ্যাপ লেখে: 2026-09-10T09:44:12.123Z   · আগের কিছু SQL লিখেছে: 2026-09-06 13:52:13.337702+00
--   ফোন/কম্পিউটার লেখা-তুলনায় "এর পরের সারি" খোঁজে ⇒ দ্বিতীয় ছাঁচের সারি বাদ পড়ত।
-- ১) একবার: সব টেবিলের অন্য-ছাঁচের "updatedAt" → অ্যাপের ছাঁচে (একই মুহূর্ত, শুধু লেখার ধরন)
-- ২) পাহারা (trigger): এরপর কোনো SQL/টুল অন্য ছাঁচে লিখলে ঢোকার সময়েই ঠিক হয়ে যাবে
-- ⛔ অ্যাপের লেখা সারি (T…Z) একটুও ছোঁয়া হয় না। ⛔ মান বদলায় না, শুধু ছাঁচ। ⛔ যা পড়া যায় না, তা যেমন ছিল তেমনই থাকে।
-- =====================================================================
begin;

create or replace function public.tk_app_stamp(v text) returns text
language plpgsql immutable as $$
begin
  if v is null or v = '' or v ~ '^\d{4}-\d{2}-\d{2}T' then return v; end if;   -- ফাঁকা বা অ্যাপের ছাঁচ ⇒ যেমন আছে
  if v !~ '^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}' then return v; end if;            -- সময় নয় ⇒ যেমন আছে
  begin
    return to_char((v::timestamptz) at time zone 'utc', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"');
  exception when others then
    return v;                                                                   -- পড়া গেল না ⇒ যেমন আছে
  end;
end $$;

create or replace function public.tk_fix_updated_at_format() returns trigger
language plpgsql as $$
begin
  new."updatedAt" := public.tk_app_stamp(new."updatedAt");
  return new;
end $$;

do $$
declare t record; n bigint; total bigint := 0;
begin
  for t in
    select table_name from information_schema.columns
    where table_schema = 'public' and column_name = 'updatedAt' and data_type = 'text'
    order by table_name
  loop
    execute format('update public.%I set "updatedAt" = public.tk_app_stamp("updatedAt") where "updatedAt" !~ %L and "updatedAt" ~ %L',
                   t.table_name, '^\d{4}-\d{2}-\d{2}T', '^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}');
    get diagnostics n = row_count;
    total := total + n;
    raise notice 'V1289 % : % সারি এক ছাঁচে', t.table_name, n;
    execute format('drop trigger if exists tk_fix_updated_at_format on public.%I', t.table_name);
    execute format('create trigger tk_fix_updated_at_format before insert or update of "updatedAt" on public.%I for each row execute function public.tk_fix_updated_at_format()', t.table_name);
  end loop;
  raise notice 'V1289 মোট % সারি', total;
end $$;

-- যাচাই: এরপর অন্য ছাঁচ কটা বাকি (সব ০ হওয়ার কথা)
select 'followups' as tebil, count(*) filter (where "updatedAt" !~ '^\d{4}-\d{2}-\d{2}T' and "updatedAt" <> '') as baki from followups
union all select 'patients',  count(*) filter (where "updatedAt" !~ '^\d{4}-\d{2}-\d{2}T' and "updatedAt" <> '') from patients
union all select 'enquiries', count(*) filter (where "updatedAt" !~ '^\d{4}-\d{2}-\d{2}T' and "updatedAt" <> '') from enquiries
union all select 'payments',  count(*) filter (where "updatedAt" !~ '^\d{4}-\d{2}-\d{2}T' and "updatedAt" <> '') from payments;

commit;
