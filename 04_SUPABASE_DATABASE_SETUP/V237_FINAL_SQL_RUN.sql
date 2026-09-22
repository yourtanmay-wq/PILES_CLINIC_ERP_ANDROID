-- ==================================================================
-- V237 · FINAL SQL (একবারেই চালানোর জন্য · copy-paste)
-- Maa Ayurved Piles Clinic · owner: TK Biswas
-- ------------------------------------------------------------------
-- এই একটি ফাইলই যথেষ্ট। Supabase Dashboard -> SQL Editor -> New query ->
-- পুরোটা paste করে RUN করুন।
--
-- নিরাপত্তা (গ্যারান্টি):
--   * সম্পূর্ণ idempotent — `add column if not exists`. দুই/তিনবার চালালেও কিছু ভাঙবে না।
--   * কোনো `DROP` নেই · কোনো পুরোনো column/row মোছা বা overwrite নেই।
--   * শুধু একটি নতুন column যোগ (patients.altMobile) + শেষে READ-ONLY যাচাই (কিছু বদলায় না)।
--   * বর্তমান `mobile` = Primary Mobile (অপরিবর্তিত)। নতুন `altMobile` = Alternate/Enquiry Mobile।
--   * App এই column ছাড়াও নিরাপদ (safe fallback) — তবু এটি চালালে Alternate Mobile
--     পুরোপুরি cloud-এ সংরক্ষিত হবে।
-- ==================================================================


-- ==================================================================
-- PART A — কাজ-২: patients টেবিলে Alternate Mobile column (RUN করে)
-- ==================================================================
alter table public.patients add column if not exists "altMobile" text;

-- (ঐচ্ছিক — দরকার নেই; enquiry-র নিজের নম্বর তো `mobile`-এই আছে।
--  চাইলে uncomment করে চালাতে পারেন, ক্ষতি নেই):
-- alter table public.enquiries add column if not exists "altMobile" text;


-- ==================================================================
-- PART B — READ-ONLY যাচাই (কোনো data বদলায় না · শুধু SELECT)
-- উপরের ALTER-এর পরে চালিয়ে নিশ্চিত হন সব ঠিক আছে।
-- ==================================================================

-- B1) altMobile column সত্যিই যোগ হয়েছে কি না
select table_name, column_name, data_type
from information_schema.columns
where table_schema = 'public'
  and table_name in ('patients','enquiries')
  and column_name in ('mobile','altMobile')
order by table_name, column_name;

-- B2) patients-এ mobile ফাঁকা/ছোট কতগুলো (health-check)
select
  count(*)                                             as total_patients,
  count(*) filter (where coalesce(mobile,'') = '')     as blank_mobile,
  count(*) filter (where length(regexp_replace(coalesce(mobile,''),'\D','','g')) < 10) as short_mobile
from public.patients;

-- B3) কয়টা patient-এ ইতিমধ্যে altMobile বসেছে (ALTER চালানোর পরে; শুরুতে 0 স্বাভাবিক)
select
  count(*)                                                as total_patients,
  count(*) filter (where coalesce("altMobile",'') <> '') as with_alt_mobile
from public.patients;


-- ==================================================================
-- PART C — কাজ-৫: পুরোনো Address যাচাই (READ-ONLY · কোনো data বদলায় না)
-- উদ্দেশ্য: পুরোনো record-এ Address সত্যিই নেই, নাকি আছে কিন্তু View পেত না।
-- Address সব টেবিলে একটাই column: "address"।
-- ==================================================================

-- C1) একটি নির্দিষ্ট রোগীর তিন টেবিলে address মিলিয়ে দেখুন।
--     <DIGITS> = রোগীর 10-সংখ্যার নম্বর বসান (যেমন 9832011111)।
select 'enquiries' as source, id, name, mobile, address from public.enquiries where mobile like '%<DIGITS>%'
union all
select 'patients'  as source, id, name, mobile, address from public.patients  where mobile like '%<DIGITS>%'
union all
select 'followups' as source, id, name, mobile, address from public.followups where mobile like '%<DIGITS>%';
-- ব্যাখ্যা: patients ও enquiries-এর address ফাঁকা কিন্তু followups-এর address পূর্ণ হলে
--          -> App এখন (V235) followups.address-এও fallback করে, তাই View-তে দেখাবে।
--          তিনটেই ফাঁকা হলে -> data সত্যিই নেই (কোনো bug নয়; App ভুয়া address দেখাবে না)।

-- C2) প্রতি টেবিলে কতগুলো record-এ address আছে বনাম নেই
select 'enquiries' as t, count(*) total, count(*) filter (where coalesce(address,'')<>'') with_addr from public.enquiries
union all
select 'patients',  count(*), count(*) filter (where coalesce(address,'')<>'') from public.patients
union all
select 'followups', count(*), count(*) filter (where coalesce(address,'')<>'') from public.followups;

-- ==================================================================
-- শেষ। PART A একবার চললেই App সম্পূর্ণ Alternate Mobile সমর্থন করবে।
-- PART B ও C শুধু চোখে দেখার যাচাই — যতবার খুশি চালান, কিছুই বদলাবে না।
-- ==================================================================
