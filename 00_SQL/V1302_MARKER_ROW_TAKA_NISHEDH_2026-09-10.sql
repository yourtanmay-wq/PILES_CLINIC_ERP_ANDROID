-- V1302 (তালিকা ৪১৩, TK: "ভবিষ্যতে এই ধরনের সমস্যা যেন না হয়") — ডেটাবেস-স্তরের পাহারা:
-- চিহ্ন-সারি (bill_edit · chamber_expected · attendance_mark) কখনো টাকা বহন করতে পারবে না।
-- কোনো অ্যাপ (পুরনো APK · ওয়েব · হাতে SQL) ওতে ০ ছাড়া টাকা বসাতে গেলে ডেটাবেস নিজেই আটকাবে।
-- ⛔ কোনো পুরনো সারি বদলায় না; শুধু ভবিষ্যতের লেখা আটকায়।
create or replace function public.tk_marker_rows_no_money() returns trigger
language plpgsql as $$
declare amt numeric;
begin
  if lower(coalesce(new."payType",'')) in ('bill_edit','chamber_expected','attendance_mark') then
    begin
      amt := coalesce(nullif(trim(coalesce(new.amount::text,'')),''),'0')::numeric;
    exception when others then
      amt := 0;
    end;
    if amt <> 0 then
      raise exception 'TK guard: marker row (%) cannot carry money (amount %). Add a real payment instead.', new."payType", new.amount;
    end if;
  end if;
  return new;
end $$;

drop trigger if exists tk_marker_rows_no_money on public.payments;
create trigger tk_marker_rows_no_money
before insert or update on public.payments
for each row execute function public.tk_marker_rows_no_money();

-- যাচাই: (১) ট্রিগার বসেছে কিনা, (২) এখন DB-তে টাকা-সহ চিহ্ন-সারি কটা (V1297A চালানোর পরে ০ হওয়ার কথা)
select 'trigger' as ki, count(*)::text as sonkha from pg_trigger where tgname = 'tk_marker_rows_no_money'
union all
select 'marker_rows_with_money', count(*)::text from public.payments
where lower(coalesce("payType",'')) in ('bill_edit','chamber_expected','attendance_mark')
  and coalesce(nullif(trim(coalesce(amount::text,'')),''),'0') ~ '^-?[0-9]+(\.[0-9]+)?$'
  and coalesce(nullif(trim(coalesce(amount::text,'')),''),'0')::numeric <> 0;
