-- শুধু দেখা (⑧ ঘড়ি): ১) কোন টেবিলে সার্ভার-সময়ের ঘর আছে (default now())
--                    ২) পেমেন্ট/রোগী/ফলো-আপে ফোনের ঘড়ি সন্দেহজনক — তৈরির সময় ভারতীয় সময়ে রাত ১১টা–ভোর ৬টা, বা date > createdAt-এর দিন, বা createdAt ভবিষ্যতে
select 'server_time_column' as ki, table_name || '.' || column_name as kothay, column_default as man, null::bigint as koto
from information_schema.columns
where table_schema = 'public' and column_default ilike '%now()%'
union all
select 'payments_odd_hour', min(date) || ' → ' || max(date), string_agg(distinct coalesce("receivedBy",''), ', '), count(*)
from payments
where "createdAt" ~ '^\d{4}-\d{2}-\d{2}T'
  and extract(hour from (("createdAt")::timestamptz at time zone 'Asia/Kolkata')) not between 6 and 22
  and coalesce("payType",'') not in ('chamber_expected','bill_edit','attendance_mark')
union all
select 'payments_date_after_created', min(date) || ' → ' || max(date), string_agg(distinct coalesce("receivedBy",''), ', '), count(*)
from payments
where "createdAt" ~ '^\d{4}-\d{2}-\d{2}T' and date > to_char((("createdAt")::timestamptz at time zone 'Asia/Kolkata'), 'YYYY-MM-DD')
  and coalesce("payType",'') not in ('chamber_expected','bill_edit','attendance_mark')
union all
select 'payments_created_in_future', min(date) || ' → ' || max(date), string_agg(distinct coalesce("receivedBy",''), ', '), count(*)
from payments
where "createdAt" ~ '^\d{4}-\d{2}-\d{2}T' and ("createdAt")::timestamptz > now() + interval '10 minutes'
union all
select 'patients_odd_hour', min("registrationDate") || ' → ' || max("registrationDate"), string_agg(distinct coalesce("registeredBy",''), ', '), count(*)
from patients
where "createdAt" ~ '^\d{4}-\d{2}-\d{2}T'
  and extract(hour from (("createdAt")::timestamptz at time zone 'Asia/Kolkata')) not between 6 and 22
union all
select 'followups_odd_hour', min("createdAt") || ' → ' || max("createdAt"), string_agg(distinct coalesce("createdBy",''), ', '), count(*)
from followups
where "createdAt" ~ '^\d{4}-\d{2}-\d{2}T'
  and extract(hour from (("createdAt")::timestamptz at time zone 'Asia/Kolkata')) not between 6 and 22;
