-- ═══════════════════════════════════════════════════════════════════════════
-- 🔍🔒 V1504-জলপাইগুড়ি-ডায়াগনস্টিক (১৫.০৯.২০২৬) — V1 তালিকায় ছিল, V3-তে
-- নেই কেন সেটা দেখার জন্য। patientId ফাঁকা কিনা, নাকি সত্যিই আলাদা রোগী,
-- সেটাই এই SQL দেখাবে। ⛔ শুধু পড়া।
-- ═══════════════════════════════════════════════════════════════════════════
select
  p."branch", p."name", p."patientId", p."patientCode", p."mobile",
  p."date", p."payLabel", p."amount", p."mode", p."createdAt", p."id"
from public.payments p
where p."payType" = 'treatment'
  and p."date" = '2026-07-27'
  and p."branch" = 'Jalpaiguri'
  and p."name" in ('DHANANJOY ROY','SNIGDHYA DUTTA','KAMAL ROY','BABLI MISRA','BIBHASH BARMON')
order by p."name", p."createdAt";
