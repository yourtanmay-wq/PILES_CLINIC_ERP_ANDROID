-- ================================================================
-- V235 · কাজ-৩ (Enquiry Address) — READ-ONLY AUDIT
-- এই ফাইল কোনো data পরিবর্তন করে না (শুধু SELECT)। উদ্দেশ্য: পুরোনো
-- record-এ Address সত্যিই database-এ নেই, নাকি আছে কিন্তু View পাচ্ছে না —
-- তা প্রমাণ করা। Address সব টেবিলে একটাই column: "address"।
-- ================================================================

-- (ক) একটি নির্দিষ্ট রোগীর তিন টেবিলে address মিলিয়ে দেখুন।
--     <DIGITS> = রোগীর ১০-সংখ্যার নম্বর বসান (যেমন 9832011111)।
select 'enquiries' as source, id, name, mobile, address from public.enquiries where mobile like '%<DIGITS>%'
union all
select 'patients'  as source, id, name, mobile, address from public.patients  where mobile like '%<DIGITS>%'
union all
select 'followups' as source, id, name, mobile, address from public.followups where mobile like '%<DIGITS>%';
-- ব্যাখ্যা: patients ও enquiries-এর address ফাঁকা কিন্তু followups-এর address
--          পূর্ণ হলে → এটি code-gap (View followups-এ fallback করে না)।
--          তিনটেই ফাঁকা হলে → data সত্যিই নেই (কোনো bug নয়)।

-- (খ) নাম "UNKNOWN" এমন enquiry record-এ address ফাঁকা না পূর্ণ (read-only)
select
  count(*)                                          as unknown_enquiries,
  count(*) filter (where coalesce(address,'') <> '') as with_address,
  count(*) filter (where coalesce(address,'') =  '') as without_address
from public.enquiries
where upper(coalesce(name,'')) = 'UNKNOWN' or coalesce(name,'') = '';

-- (গ) সামগ্রিক: প্রতি টেবিলে কতগুলো record-এ address আছে বনাম নেই
select 'enquiries' as t, count(*) total, count(*) filter (where coalesce(address,'')<>'') with_addr from public.enquiries
union all
select 'patients',  count(*), count(*) filter (where coalesce(address,'')<>'') from public.patients
union all
select 'followups', count(*), count(*) filter (where coalesce(address,'')<>'') from public.followups;
