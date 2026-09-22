-- ═══════════════════════════════════════════════════════════════════════
-- V1094 (০৫.০৯.২০২৬) — যেসব টাকার সারির রোগীই নেই, সেগুলো Collection থেকে বাদ
--
-- 🔴 পাওয়া গেছে: ৮২টা সারির `patientId` কোনো রোগীর সঙ্গে মেলে না, অথচ
--    Collection সব সারি যোগ করে (কোডে দেখা: app.js-এর টাকার তালিকা রোগী
--    আছে কিনা দেখেই না)। ⇒ মোট ₹৩,৬১,৬১১ ভুল করে হিসাবে ছিল।
--
-- ① **১৫টা** (₹১৮,৬০০) — রোগী আছেই, শুধু জোড়া পুরনো ⇒ জোড়া ঠিক করা হয়,
--    টাকা হিসাবেই থাকে। ⛔ একই নম্বরে একাধিক রোগী থাকলে ছোঁয়াই হয় না।
-- ② বাকিগুলো `payments` থেকে **সরিয়ে** `payments_orphan_20260905`-এ রাখা হয় —
--    ⛔ কিছুই মোছা হয় না, দরকার হলে ফেরানো যায়।
-- ⛔ সরানোর সময় V1088-এর ট্রিগার প্রতিটার "মুছে ফেলা" চিহ্ন বসিয়ে দেবে, তাই
--    কোনো ফোন/কম্পিউটার পুরনো কপি আবার তুলে দিতে পারবে না। ফেরাতে হলে আগে
--    `deleted_records` থেকে ওই চিহ্নগুলো তুলতে হবে।
-- ⚠️ ফল: জুলাই–আগস্টের পুরনো Collection ওই টাকাটা কমবে — TK-এর অনুমতি নেওয়া।
-- ═══════════════════════════════════════════════════════════════════════

-- ① জোড়া ঠিক করা (শুধু যেখানে ওই নম্বরে একজনই রোগী)
update public.payments y
set "patientId" = m.pid
from (
  select right(regexp_replace(coalesce(p.mobile,''),'\D','','g'),10) as m10,
         min(p.id) as pid, count(*) as n
  from public.patients p
  group by 1
) m
where coalesce(y."patientId",'') <> ''
  and not exists (select 1 from public.patients q where q.id = y."patientId")
  and m.n = 1
  and m.m10 = right(regexp_replace(coalesce(y.mobile,''),'\D','','g'),10);

-- ② বাকিগুলো ব্যাকআপ টেবিলে সরানো
create table if not exists public.payments_orphan_20260905 as
  select * from public.payments where false;

insert into public.payments_orphan_20260905
select * from public.payments y
where coalesce(y."patientId",'') <> ''
  and not exists (select 1 from public.patients p where p.id = y."patientId");

delete from public.payments
where id in (select id from public.payments_orphan_20260905);

-- ③ যাচাই — baki_onath অবশ্যই 0 হবে
select
  (select count(*) from public.payments_orphan_20260905) as sorano_koyta,
  (select sum(coalesce(nullif(regexp_replace(coalesce(amount,''),'[^0-9.]','','g'),'')::numeric,0))
     from public.payments_orphan_20260905) as sorano_taka,
  (select count(*) from public.payments y
    where coalesce(y."patientId",'') <> ''
      and not exists (select 1 from public.patients p where p.id = y."patientId")) as baki_onath;
