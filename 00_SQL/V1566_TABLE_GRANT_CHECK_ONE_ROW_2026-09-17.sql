-- শুধু দেখার জন্য (SELECT) -- V1565-ও আবার ঠিক ১০০ সারিতে কাটা পড়েছিল
-- (Supabase-এর নিজের পর্দার সীমা)। এবার প্রতিটা টেবিলের জন্য মাত্র **একটা**
-- সারি -- ২১টা টেবিলে সর্বোচ্চ ২১টা সারি, তাই আর কখনো কাটবে না।
-- status='OK' মানে সেই টেবিলে anon আর authenticated-এর অনুমতি হুবহু এক।
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
order by (status <> 'MISMATCH'), table_name;
