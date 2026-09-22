-- 🔍 READ-ONLY — শুধু দেখার জন্য, কোনো টাকা/ডেটা বদলাবে না
-- V1591-এ ৪ জনের (DURJAM PASWAN, MD SAMSUL, TARIK ANWAR, DHARAM KUMAR) চিকিৎসার
-- টাকা জমা আছে কিন্তু automatic commission বাঁধা নেই দেখিয়েছিল। কিন্তু TK-র
-- আগের স্ক্রিনশটে DURJAM PASWAN-এর "Entered by hand · ₹12,550 · Due" এন্ট্রি
-- আগে থেকেই ছিল — মানে হাতে-লেখা এন্ট্রি (legacy) দিয়ে তাঁর টাকা হয়তো ইতিমধ্যেই
-- ধরা আছে, শুধু automatic % হিসাবে বাঁধা নেই। এই কোয়েরি ওই ৪ জনের নাম/নম্বর
-- হাতে-লেখা এন্ট্রিতে (referralPayments) আছে কিনা মিলিয়ে দেখাবে —
-- আসলেই টাকা মিসিং নাকি শুধু অন্য পথে (হাতে) ধরা আছে সেটাই আসল প্রশ্নের উত্তর।
with rmp as (
  select id from public.doctor_visits
  where right(regexp_replace(coalesce("mobile",''),'\D','','g'),10) = '9046366596'
  limit 1
), legacy as (
  select right(regexp_replace(coalesce(j->>'patientMobile',''),'\D','','g'),10) as mob10,
         lower(trim(coalesce(j->>'patient',''))) as nm,
         j->>'amount' as amt, j->>'status' as status
  from public.doctor_visits dv, rmp r
  cross join lateral jsonb_array_elements(
    case when jsonb_typeof(dv."referralPayments") = 'array' then dv."referralPayments" else '[]'::jsonb end) j
  where dv.id = r.id
), watch as (
  select unnest(array['9771066263','8539077984','9199822385','6294096912']) as mob10,
         unnest(array['DURJAM PASWAN','MD SAMSUL','TARIK ANWAR','DHARAM KUMAR']) as nm
)
select w.nm as patient_name,
       exists(select 1 from legacy l where l.mob10 = w.mob10 or l.nm = lower(w.nm)) as has_legacy_hand_entry,
       (select string_agg('₹'||l.amt||' · '||l.status, ' + ') from legacy l where l.mob10 = w.mob10 or l.nm = lower(w.nm)) as legacy_entries_detail
from watch w;
