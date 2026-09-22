-- V329 — FIVE CONFIRMED DEMO NUMBERS: RECOVERABLE CLEANUP
-- Owner confirmation: TK BISWAS, 12.08.2026.
-- Exact last-10-digit targets only:
-- 1234567890, 8888888888, 7777777777, 3030303030, 1231231231
--
-- Safety:
-- 1) one private backuprecords snapshot is written first;
-- 2) global deleted_records tombstones are written before live deletion;
-- 3) all work is one transaction — any error rolls everything back;
-- 4) no doctor/RMP/staff/account row is touched;
-- 5) only the five exact mobile numbers and their exact patient/payment links.

do $demo_cleanup$
declare
  v_mobiles text[] := array[
    '1234567890','8888888888','7777777777','3030303030','1231231231'
  ];
  v_patient_ids text[];
  v_patient_rows text[];
  v_payment_ids text[];
  v_now text := to_char(clock_timestamp() at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"');
  v_backup_id text := 'backup_demo_cleanup_20260812_five_confirmed_numbers';
begin
  -- Capture every known patient code/row before anything is removed. Trash is
  -- included because a live payment may point to a patient already in Trash.
  select coalesce(array_agg(distinct x.patient_id) filter (where x.patient_id <> ''), array[]::text[])
  into v_patient_ids
  from (
    select coalesce(p."patientId", '') as patient_id
    from public.patients p
    where right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)
    union all
    select coalesce(f."patientId", '')
    from public.followups f
    where right(regexp_replace(coalesce(f.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)
    union all
    select coalesce(t.record->>'patientId', '')
    from public.trash t
    where right(regexp_replace(coalesce(t.record->>'mobile',''),'[^0-9]','','g'),10) = any(v_mobiles)
  ) x;

  select coalesce(array_agg(distinct x.row_id) filter (where x.row_id <> ''), array[]::text[])
  into v_patient_rows
  from (
    select coalesce(p.id, '') as row_id
    from public.patients p
    where right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)
    union all
    select coalesce(t.record->>'id', '')
    from public.trash t
    where coalesce(t."table", '') = 'patients'
      and right(regexp_replace(coalesce(t.record->>'mobile',''),'[^0-9]','','g'),10) = any(v_mobiles)
  ) x;

  select coalesce(array_agg(distinct p.id), array[]::text[])
  into v_payment_ids
  from public.payments p
  where right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)
     or coalesce(p."patientId", '') = any(v_patient_ids);

  -- One recoverable, non-UI backup. Existing Trash history is included, then
  -- removed below so these confirmed demos do not remain visible in Trash.
  insert into public.backuprecords
    (id, date, reason, status, size, payload, "by", "createdAt", "updatedAt")
  values (
    v_backup_id,
    '2026-08-12',
    'Owner-confirmed Demo cleanup: five exact mobile numbers',
    'completed',
    'five-demo-numbers',
    jsonb_build_object(
      'targetMobiles', to_jsonb(v_mobiles),
      'patients', (select coalesce(jsonb_agg(to_jsonb(p)), '[]'::jsonb) from public.patients p
        where right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)),
      'enquiries', (select coalesce(jsonb_agg(to_jsonb(e)), '[]'::jsonb) from public.enquiries e
        where right(regexp_replace(coalesce(e.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)),
      'followups', (select coalesce(jsonb_agg(to_jsonb(f)), '[]'::jsonb) from public.followups f
        where right(regexp_replace(coalesce(f.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)),
      'payments', (select coalesce(jsonb_agg(to_jsonb(p)), '[]'::jsonb) from public.payments p
        where p.id = any(v_payment_ids)),
      'medical', (select coalesce(jsonb_agg(to_jsonb(m)), '[]'::jsonb) from public.medical m
        where right(regexp_replace(coalesce(m.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)
           or coalesce(m."patientId", '') = any(v_patient_ids)),
      'products', (select coalesce(jsonb_agg(to_jsonb(p)), '[]'::jsonb) from public.products p
        where right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)),
      'trashBeforeCleanup', (select coalesce(jsonb_agg(to_jsonb(t)), '[]'::jsonb) from public.trash t
        where right(regexp_replace(coalesce(t.record->>'mobile',''),'[^0-9]','','g'),10) = any(v_mobiles)),
      'paymentEditRequests', (select coalesce(jsonb_agg(to_jsonb(r)), '[]'::jsonb) from public.payment_edit_requests r
        where right(regexp_replace(coalesce(r.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)
           or coalesce(r."patientRowId", '') = any(v_patient_rows)
           or coalesce(r."patientCode", '') = any(v_patient_ids)
           or coalesce(r."paymentId", '') = any(v_payment_ids)),
      'paymentBackdateRequests', (select coalesce(jsonb_agg(to_jsonb(r)), '[]'::jsonb) from public.payment_backdate_requests r
        where right(regexp_replace(coalesce(r.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)
           or coalesce(r."patientRowId", '') = any(v_patient_rows)
           or coalesce(r."patientCode", '') = any(v_patient_ids)),
      'referralEditRequests', (select coalesce(jsonb_agg(to_jsonb(r)), '[]'::jsonb) from public.referral_edit_requests r
        where right(regexp_replace(coalesce(r."patientMobile",''),'[^0-9]','','g'),10) = any(v_mobiles))
    ),
    'TK BISWAS — MASTER',
    v_now,
    v_now
  )
  on conflict (id) do update set
    payload = excluded.payload,
    "updatedAt" = excluded."updatedAt";

  -- Global tombstones: every phone/device can see that these cloud rows were
  -- intentionally deleted, so an old pending/local write cannot restore them.
  insert into public.deleted_records (id, "tableName", "rowId", "deletedBy", "deletedAt")
  select x.table_name || '|' || x.row_id, x.table_name, x.row_id,
         'TK BISWAS — MASTER DEMO CLEANUP', v_now
  from (
    select 'patients'::text table_name, p.id::text row_id from public.patients p
      where right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)
    union all
    select 'enquiries', e.id from public.enquiries e
      where right(regexp_replace(coalesce(e.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)
    union all
    select 'followups', f.id from public.followups f
      where right(regexp_replace(coalesce(f.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)
    union all
    select 'payments', p.id from public.payments p where p.id = any(v_payment_ids)
    union all
    select 'medical', m.id from public.medical m
      where right(regexp_replace(coalesce(m.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)
         or coalesce(m."patientId", '') = any(v_patient_ids)
    union all
    select 'products', p.id from public.products p
      where right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)
  ) x
  on conflict (id) do update set
    "deletedBy" = excluded."deletedBy",
    "deletedAt" = excluded."deletedAt";

  -- Dependent requests first, then medical/financial/workflow sources.
  delete from public.payment_edit_requests r
   where right(regexp_replace(coalesce(r.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)
      or coalesce(r."patientRowId", '') = any(v_patient_rows)
      or coalesce(r."patientCode", '') = any(v_patient_ids)
      or coalesce(r."paymentId", '') = any(v_payment_ids);

  delete from public.payment_backdate_requests r
   where right(regexp_replace(coalesce(r.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)
      or coalesce(r."patientRowId", '') = any(v_patient_rows)
      or coalesce(r."patientCode", '') = any(v_patient_ids);

  delete from public.referral_edit_requests r
   where right(regexp_replace(coalesce(r."patientMobile",''),'[^0-9]','','g'),10) = any(v_mobiles);

  delete from public.medical m
   where right(regexp_replace(coalesce(m.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles)
      or coalesce(m."patientId", '') = any(v_patient_ids);

  delete from public.products p
   where right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles);

  delete from public.payments p where p.id = any(v_payment_ids);

  delete from public.followups f
   where right(regexp_replace(coalesce(f.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles);

  delete from public.enquiries e
   where right(regexp_replace(coalesce(e.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles);

  delete from public.patients p
   where right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) = any(v_mobiles);

  delete from public.trash t
   where right(regexp_replace(coalesce(t.record->>'mobile',''),'[^0-9]','','g'),10) = any(v_mobiles);
end
$demo_cleanup$;

-- Final proof: every value below must be zero.
with targets(mobile10) as (
  values ('1234567890'),('8888888888'),('7777777777'),('3030303030'),('1231231231')
)
select
  (select count(*) from public.patients p where right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) in (select mobile10 from targets)) as patients_left,
  (select count(*) from public.enquiries e where right(regexp_replace(coalesce(e.mobile,''),'[^0-9]','','g'),10) in (select mobile10 from targets)) as enquiries_left,
  (select count(*) from public.followups f where right(regexp_replace(coalesce(f.mobile,''),'[^0-9]','','g'),10) in (select mobile10 from targets)) as followups_left,
  (select count(*) from public.payments p where right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) in (select mobile10 from targets)) as payments_by_mobile_left,
  (select count(*) from public.medical m where right(regexp_replace(coalesce(m.mobile,''),'[^0-9]','','g'),10) in (select mobile10 from targets)) as medical_by_mobile_left,
  (select count(*) from public.products p where right(regexp_replace(coalesce(p.mobile,''),'[^0-9]','','g'),10) in (select mobile10 from targets)) as products_left,
  (select count(*) from public.trash t where right(regexp_replace(coalesce(t.record->>'mobile',''),'[^0-9]','','g'),10) in (select mobile10 from targets)) as trash_left;
