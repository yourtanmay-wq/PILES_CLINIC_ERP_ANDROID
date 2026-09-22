-- ================================================================
-- V235 · কাজ-২ (Primary/Alternate Mobile) — READ-ONLY VERIFICATION
-- এই ফাইল কোনো data পরিবর্তন করে না (শুধু SELECT)। আগে এটি চালিয়ে
-- জানুন altMobile column আগে যোগ হয়েছে কি না ও বর্তমান mobile data কেমন।
-- Supabase → SQL Editor-এ একটি একটি করে চালান।
-- ================================================================

-- 1) altMobile column আগে যোগ হয়েছে কি না (patients ও enquiries)
select table_name, column_name, data_type
from information_schema.columns
where table_schema = 'public'
  and table_name in ('patients','enquiries')
  and column_name in ('mobile','altMobile')
order by table_name, column_name;

-- 2) patients-এ mobile ফাঁকা/অস্বাভাবিক কতগুলো (read-only health-check)
select
  count(*)                                             as total_patients,
  count(*) filter (where coalesce(mobile,'') = '')     as blank_mobile,
  count(*) filter (where length(regexp_replace(coalesce(mobile,''),'\D','','g')) < 10) as short_mobile
from public.patients;

-- 3) সাম্প্রতিক ২০টি patient-এর mobile (চোখে দেখে যাচাই)
select id, "patientId", name, mobile, branch
from public.patients
order by "createdAt" desc nulls last
limit 20;
