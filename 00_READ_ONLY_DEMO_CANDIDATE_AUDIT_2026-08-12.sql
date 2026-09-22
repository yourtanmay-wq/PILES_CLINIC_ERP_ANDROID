-- ================================================================
-- READ ONLY: possible Demo/Test mobile audit (12.08.2026)
-- This query only SELECTs. It does not UPDATE, DELETE, INSERT or CREATE.
-- Result is a REVIEW LIST only. Nobody may be deleted from this result
-- until TK BISWAS confirms the individual name/mobile.
-- ================================================================

with live_rows as (
  select 'followups'::text as source_table, id::text, name::text,
         mobile::text, branch::text, stage::text, status::text,
         "createdAt"::text as created_at, "lastRemark"::text as note
  from public.followups

  union all

  select 'patients', id::text, name::text, mobile::text, branch::text,
         stage::text, 'Active'::text, "createdAt"::text,
         concat_ws(' · ', complaint::text, diagnosis::text)
  from public.patients

  union all

  select 'enquiries', id::text, name::text, mobile::text, branch::text,
         stage::text, status::text, "createdAt"::text, remarks::text
  from public.enquiries
),
live_normalized as (
  select *, right(regexp_replace(coalesce(mobile, ''), '[^0-9]', '', 'g'), 10) as mobile10
  from live_rows
),
trash_rows as (
  select
    right(regexp_replace(coalesce(record->>'mobile', ''), '[^0-9]', '', 'g'), 10) as mobile10,
    record->>'name' as deleted_name,
    "deletedAt"::text as deleted_at
  from public.trash
),
trash_per_mobile as (
  select mobile10, max(deleted_at) as latest_trash_date, count(*) as trash_rows
  from trash_rows
  where length(mobile10) = 10
  group by mobile10
),
per_mobile as (
  select
    l.mobile10,
    string_agg(distinct nullif(l.name, ''), ' | ') as names,
    string_agg(distinct l.source_table, ' | ') as live_tables,
    string_agg(distinct nullif(l.branch, ''), ' | ') as branches,
    count(*) as live_rows,
    count(*) filter (
      where lower(coalesce(l.status, 'active')) not in ('cancelled','incomplete','closed')
    ) as active_rows,
    count(distinct nullif(lower(trim(l.name)), '')) as different_names,
    count(*) filter (
      where l.source_table = 'followups'
        and lower(coalesce(l.status, 'active')) not in ('cancelled','incomplete','closed')
    ) as active_followups,
    max(l.created_at) as latest_live_created,
    max(t.latest_trash_date) as latest_trash_date,
    max(coalesce(t.trash_rows, 0)) as trash_rows,
    bool_or(
      lower(coalesce(l.name, '') || ' ' || coalesce(l.note, '')) ~
      '(^|[^a-z])(test|demo|dummy|sample|gst|pp)([^a-z]|$)'
    ) as has_demo_word
  from live_normalized l
  left join trash_per_mobile t on t.mobile10 = l.mobile10
  where length(l.mobile10) = 10
  group by l.mobile10
)
select
  mobile10 as mobile_number,
  names,
  branches,
  live_tables,
  live_rows,
  active_rows,
  active_followups,
  trash_rows,
  concat_ws(' | ',
    case when has_demo_word then 'Name/remark contains Test-Demo type word' end,
    case when mobile10 in (
      '0000000000','1111111111','2222222222','3333333333','4444444444',
      '5555555555','6666666666','7777777777','8888888888','9999999999',
      '0123456789','1234567890','9876543210'
    ) then 'Clearly dummy-looking mobile number' end,
    case when different_names >= 3 then 'Same mobile used with 3 or more names' end,
    case when active_followups >= 2 then 'Multiple active Follow-up rows' end,
    case when trash_rows > 0 and active_rows > 0 then 'Has Trash history but is active again' end
  ) as why_suspicious,
  latest_live_created,
  latest_trash_date
from per_mobile
where
  has_demo_word
  or mobile10 in (
    '0000000000','1111111111','2222222222','3333333333','4444444444',
    '5555555555','6666666666','7777777777','8888888888','9999999999',
    '0123456789','1234567890','9876543210'
  )
  or different_names >= 3
  or active_followups >= 2
  or (trash_rows > 0 and active_rows > 0)
order by active_rows desc, names, mobile10;
