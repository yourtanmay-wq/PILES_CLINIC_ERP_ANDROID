-- V1154 · ০৬.০৯.২০২৬ · KABITA BANU (7477559892) — ডেমো দুটো সারি সরানো
-- TK: "শুধু ₹৮,০০২ আর ₹৪,০০১ ডেমো"
-- ⛔ বাকি কোনো সারিতে হাত পড়বে না (Visit Fee 400 · Advance 5500 · 5000 · 3000)

-- ধাপ ১ — আগে চোখে দেখুন (২টা সারি আসার কথা)
select id, "payLabel", amount, mode, "createdAt", "refund_status"
from public.payments
where regexp_replace(coalesce("mobile",''),'\D','','g') like '%7477559892'
  and "createdAt" in ('2026-08-27T21:16:28.513Z','2026-08-27T21:25:46.007Z');

-- ধাপ ২ — নকল রাখুন (ফেরত আনার দরকার হলে এখান থেকেই আসবে)
create table if not exists public.payments_demo_20260906 as
  select * from public.payments where false;
insert into public.payments_demo_20260906
select * from public.payments
where regexp_replace(coalesce("mobile",''),'\D','','g') like '%7477559892'
  and "createdAt" in ('2026-08-27T21:16:28.513Z','2026-08-27T21:25:46.007Z');

-- ধাপ ৩ — মুছুন
delete from public.payments
where id in (select id from public.payments_demo_20260906);

-- ধাপ ৪ — মিলিয়ে দেখুন (sorano=2 · baki=0 · treatment_taka=13500)
select
  (select count(*) from public.payments_demo_20260906) as sorano,
  (select count(*) from public.payments
     where regexp_replace(coalesce("mobile",''),'\D','','g') like '%7477559892'
       and "createdAt" in ('2026-08-27T21:16:28.513Z','2026-08-27T21:25:46.007Z')) as baki,
  (select coalesce(sum(regexp_replace(coalesce(amount::text,'0'),'\D','','g')::int),0)
     from public.payments
     where regexp_replace(coalesce("mobile",''),'\D','','g') like '%7477559892'
       and "payLabel" not in ('Visit Fee','Marked Arrived','Refund')) as treatment_taka;
