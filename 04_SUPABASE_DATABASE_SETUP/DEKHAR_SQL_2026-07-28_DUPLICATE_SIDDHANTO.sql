-- =====================================================================
-- 👀 ডুপ্লিকেট রোগী — সংক্ষেপে সিদ্ধান্ত (২৮.০৭.২০২৬, খাতার সারি B30)
-- =====================================================================
-- ✅ শুধু দেখার SQL — কিছুই মোছে না, বদলায় না।
-- ⚠️ আগের লেখাটা Supabase এডিটরের নিজের suggestion বাক্সে কেটে যাচ্ছিল
--    (`r.mob10` হয়ে যাচ্ছিল `r.mo`)। তাই এটা ছোট করে লেখা হয়েছে —
--    এখানে ওই জোড়া লাগানোর (join) লাইনটাই নেই।
-- =====================================================================

select
  min(name) as "নাম",
  count(*) as "সারি",
  count(*) filter (where paid = 0 and bill = 0) as "ফাঁকা",
  sum(paid) as "মোট জমা",
  case
    when count(*) filter (where paid > 0) > 1 then 'একাধিক সারিতে টাকা'
    when count(*) filter (where paid = 0 and bill = 0) = 0 then 'কোনো সারিই ফাঁকা নয়'
    else 'ফাঁকাগুলো সরানো নিরাপদ'
  end as "সিদ্ধান্ত"
from (
  select
    name,
    coalesce(nullif(bill,'')::numeric,0) as bill,
    right(regexp_replace(coalesce(mobile,''),'\D','','g'),10) as mob10,
    coalesce((select sum(coalesce(nullif(amount,'')::numeric,0))
              from public.payments
              where "patientId" = patients.id
                 or "patientId" = patients."patientId"),0) as paid
  from public.patients
  where right(regexp_replace(coalesce(mobile,''),'\D','','g'),10) <> ''
) x
group by mob10
having count(*) > 1
order by 1;
