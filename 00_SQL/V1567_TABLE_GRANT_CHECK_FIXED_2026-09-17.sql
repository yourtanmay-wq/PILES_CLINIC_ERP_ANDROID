-- শুধু দেখার জন্য (SELECT) -- V1566-এ নিজের ভুল: ORDER BY-তে "status" নামের
-- alias-টা জটিল এক্সপ্রেশনের ভেতরে রাখা হয়েছিল ((status <> 'MISMATCH')),
-- Postgres সেটাকে টেবিলের আসল কলাম ধরে নিয়ে "column status does not exist"
-- বলে আটকে দিয়েছে। এখন সরাসরি তুলনাটাই ORDER BY-তে বসানো হলো, একই ফল।
-- কিচ্ছু বদলায় না, শুধু পড়া হয়।
with base as (
  select distinct table_name
  from information_schema.role_table_grants
  where table_schema = 'public'
    and table_name in (
      'activity_logs','backdate_payment_grants','briefings','call_remarks',
      'chamber_close','device_logins','device_tokens','dialer_calls',
      'doctor_visits','enquiries','followups','medical','message_log',
      'patients','payment_backdate_requests','payment_edit_requests',
      'payments','products','referral_edit_requests','trash','usercredentials'
    )
),
agg as (
  select
    b.table_name,
    (select array_agg(privilege_type order by privilege_type)
       from information_schema.role_table_grants g
       where g.table_schema = 'public' and g.table_name = b.table_name and g.grantee = 'anon'
    ) as anon_privs,
    (select array_agg(privilege_type order by privilege_type)
       from information_schema.role_table_grants g
       where g.table_schema = 'public' and g.table_name = b.table_name and g.grantee = 'authenticated'
    ) as authenticated_privs
  from base b
)
select
  table_name, anon_privs, authenticated_privs,
  case when anon_privs = authenticated_privs then 'OK' else 'MISMATCH' end as status
from agg
order by (anon_privs = authenticated_privs), table_name;
