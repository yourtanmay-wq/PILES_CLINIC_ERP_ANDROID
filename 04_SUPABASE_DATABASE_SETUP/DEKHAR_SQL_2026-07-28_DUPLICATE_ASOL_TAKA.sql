-- =====================================================================
-- 👀 ডুপ্লিকেট রোগী — আসল টাকার হিসাব (২৮.০৭.২০২৬, খাতার সারি B30)
-- =====================================================================
-- ✅ শুধু দেখার SQL — কিছুই মোছে না, বদলায় না।
--
-- ⚠️ আগের লেখাটায় ভুল ছিল: ডুপ্লিকেট সারিগুলোর Patient ID এক, তাই
--    একই টাকা প্রতিটা সারির নামে বারবার গোনা হচ্ছিল।
--
-- এখন টাকা দু'ভাগে আলাদা করে দেখানো হচ্ছে:
--   • "সারি-ধরে টাকা"  = যে টাকা ঠিক ওই সারিটার সঙ্গেই বাঁধা (আলাদা করা যায়)
--   • "কোড-ধরে টাকা"   = যে টাকা শুধু Patient ID-র সঙ্গে বাঁধা, তাই ওটা
--                        গোটা রোগীর — একবারই গোনা হয়েছে
-- =====================================================================

select
  min(name) as "নাম",
  count(*) as "সারি",
  count(*) filter (where bill = 0 and own_paid = 0) as "ফাঁকা সারি",
  sum(own_paid) as "সারি-ধরে টাকা",
  max(code_paid) as "কোড-ধরে টাকা",
  sum(own_paid) + max(code_paid) as "আসল মোট টাকা",
  case
    when count(*) filter (where own_paid > 0) > 1 then 'সাবধান — একাধিক সারিতে আলাদা টাকা'
    when count(*) filter (where bill = 0 and own_paid = 0) = 0 then 'সাবধান — কোনো সারিই ফাঁকা নয়'
    else 'ফাঁকা সারি সরানো নিরাপদ'
  end as "সিদ্ধান্ত"
from (
  select
    name,
    coalesce(nullif(bill,'')::numeric,0) as bill,
    right(regexp_replace(coalesce(mobile,''),'\D','','g'),10) as mob10,
    coalesce((select sum(coalesce(nullif(amount,'')::numeric,0))
              from public.payments
              where "patientId" = patients.id),0) as own_paid,
    coalesce((select sum(coalesce(nullif(amount,'')::numeric,0))
              from public.payments
              where "patientId" = patients."patientId"),0) as code_paid
  from public.patients
  where right(regexp_replace(coalesce(mobile,''),'\D','','g'),10) <> ''
) x
group by mob10
having count(*) > 1
order by 1;
