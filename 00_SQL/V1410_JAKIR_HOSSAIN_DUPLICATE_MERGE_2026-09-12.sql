-- V1410 (১২.০৯.২০২৬ রাত, TK-নির্দেশ: "7407407675 এটা আসল" · "Remarks রাখতে হবে", তালিকা ৫১৫)
-- Cooch Behar-এ JAKIR HOSSAIN-এর নকল রেকর্ড (+919332664749, CHILKIRHAT) → তার কল-নোট/রিমার্ক
-- আসল রেকর্ডে (+917407407675, KHOCHABARI) তুলে দিয়ে নকলটা মোছা। ফোন/কম্পিউটার থেকেও নকলটা
-- সরে যাওয়ার জন্য deleted_records-এ চিহ্ন। ⛔ আসল রেকর্ডের কিছু মোছে না — শুধু যোগ হয়।
begin;

do $$
declare v_real text; v_dup text; n_real int; n_dup int;
begin
  select count(*), max(d.id) into n_real, v_real from public.doctor_visits d
   where trim(coalesce(d.branch,'')) = 'Cooch Behar' and upper(trim(coalesce(d.name,''))) = 'JAKIR HOSSAIN'
     and right(regexp_replace(coalesce(d.mobile,''),'\D','','g'),10) = '7407407675';
  select count(*), max(d.id) into n_dup, v_dup from public.doctor_visits d
   where trim(coalesce(d.branch,'')) = 'Cooch Behar' and upper(trim(coalesce(d.name,''))) = 'JAKIR HOSSAIN'
     and right(regexp_replace(coalesce(d.mobile,''),'\D','','g'),10) = '9332664749';
  if n_real <> 1 or n_dup <> 1 then
    raise exception 'Expected exactly 1 real and 1 duplicate record, found real=% dup=%', n_real, n_dup;
  end if;
  if exists (select 1 from fin.rmp_patient_commissions c where c.rmp_id = v_dup)
     or exists (select 1 from fin.rmp_advance_payments a where a.rmp_id = v_dup) then
    raise exception 'Duplicate record has commission/advance rows — stop';
  end if;

  -- কল-নোট ও রেফারেল-এন্ট্রি আসলটায় যোগ (তারিখ অনুযায়ী নতুন আগে), রিমার্ক ফাঁকা থাকলে নকলেরটা
  update public.doctor_visits r
     set "callHistory" = (
           select coalesce(jsonb_agg(e order by coalesce(e->>'date','') desc, coalesce(e->>'createdAt','') desc), '[]'::jsonb)
             from (
               select x as e from jsonb_array_elements(case when jsonb_typeof(r."callHistory") = 'array' then r."callHistory" else '[]'::jsonb end) x
               union all
               select x from public.doctor_visits d2,
                      jsonb_array_elements(case when jsonb_typeof(d2."callHistory") = 'array' then d2."callHistory" else '[]'::jsonb end) x
                where d2.id = v_dup
             ) u),
         "referralPayments" = (
           select coalesce(jsonb_agg(e), '[]'::jsonb) from (
               select x as e from jsonb_array_elements(case when jsonb_typeof(r."referralPayments") = 'array' then r."referralPayments" else '[]'::jsonb end) x
               union all
               select x from public.doctor_visits d2,
                      jsonb_array_elements(case when jsonb_typeof(d2."referralPayments") = 'array' then d2."referralPayments" else '[]'::jsonb end) x
                where d2.id = v_dup
             ) u),
         "remarks" = case when trim(coalesce(r."remarks",'')) = '' then (select d2."remarks" from public.doctor_visits d2 where d2.id = v_dup) else r."remarks" end,
         "lastCallDate" = greatest(coalesce(r."lastCallDate",''), coalesce((select d2."lastCallDate" from public.doctor_visits d2 where d2.id = v_dup),'')),
         "updatedAt" = to_char(now() at time zone 'utc', 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
   where r.id = v_real;

  insert into public.deleted_records (id, "tableName", "rowId", "deletedBy", "deletedAt")
  values ('doctor_visits|' || v_dup, 'doctor_visits', v_dup, 'TK-SQL-V1410',
          to_char(now() at time zone 'utc', 'YYYY-MM-DD"T"HH24:MI:SS"Z"'))
  on conflict (id) do nothing;

  delete from public.doctor_visits d where d.id = v_dup;
end $$;

select d.name, d.mobile, d.area,
       jsonb_array_length(coalesce(d."callHistory",'[]'::jsonb)) as call_notes,
       jsonb_array_length(coalesce(d."referralPayments",'[]'::jsonb)) as referral_entries,
       (select count(*) from public.doctor_visits x where upper(trim(coalesce(x.name,''))) = 'JAKIR HOSSAIN' and trim(coalesce(x.branch,'')) = 'Cooch Behar') as jakir_records_now
  from public.doctor_visits d
 where trim(coalesce(d.branch,'')) = 'Cooch Behar' and upper(trim(coalesce(d.name,''))) = 'JAKIR HOSSAIN';

commit;
