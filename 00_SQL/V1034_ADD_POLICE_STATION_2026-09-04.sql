-- 🚓 V1034 (০৪.০৯.২০২৬, TK-নির্দেশ) — RMP ডাক্তার কোন থানার অধীনে।
-- TK: "আরএমপি ঠিকানা লেখা আছে, আমি চাইছি সেই আরএমপি কোন থানায়"।
-- ⛔ শুধু একটা নতুন ঘর যোগ — পুরনো একটাও সারি/ঘর ছোঁয়া হয় না।
alter table public.doctor_visits
  add column if not exists "policeStation" text;

select 'ঘর যোগ হলো' as step,
       count(*) as n
from information_schema.columns
where table_schema='public'
  and table_name='doctor_visits'
  and column_name='policeStation';
