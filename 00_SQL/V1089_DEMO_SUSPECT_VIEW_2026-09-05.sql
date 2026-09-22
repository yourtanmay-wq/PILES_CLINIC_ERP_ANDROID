-- ═══════════════════════════════════════════════════════════════════════
-- V1089 (০৫.০৯.২০২৬) — TK: *"এই ধরনের আর কোনো ডেমো নাম্বার সম্পূর্ণ
-- প্রজেক্টে আর কোথাও আছে কিনা দেখার উপায় আছে কি?"*
--
-- স্থায়ী তালিকা — যখন খুশি:   select * from tk_demo_suspects;
-- চারটে ঘরে খোঁজে: patients · enquiries · followups · doctor_visits
--
-- 🔴 প্রথম চেষ্টায় ছাঁকনি খুব ঢিলে ছিল — ৮৯৭টা সারি উঠেছিল। কারণ গুনে দেখা:
--    ৫৩৫টায় নাম ফাঁকা, আর `medical` ঘরে মোবাইল লেখাই থাকে না ('null' লেখা
--    থাকে) — দুটোই ভুল করে "সন্দেহ" ধরা হচ্ছিল। ⇒ এখন:
--      · নাম বা নম্বর ফাঁকা হলে ধরা হয় না
--      · `medical` বাদ (ওখানে নম্বর থাকেই না; ডেমো থাকলে রোগীর ঘরেই ধরা পড়ে)
--      · একই অক্ষর ৩ বার — ফাঁকা জায়গা বাদ দিয়ে মেলানো হয়
--      · "নাম খুব ছোট" নিয়মটা RMP-র ঘরে খাটে না (PK · PKB আসল নাম)
--    ⇒ ৮৯৭ থেকে নেমে হাতেগোনা কয়েকটা।
--
-- ⚠️ এটা **সন্দেহের** তালিকা, প্রমাণ নয় — আসল রোগীও উঠতে পারে (যেমন
--    NONOTA MURMU, নম্বরের প্রথম অঙ্ক ভুল টাইপ)। `reason` দেখে তবেই সিদ্ধান্ত।
-- ⛔ শুধু পড়া — কোনো সারি বদলায় না, মোছে না। Egress-এ প্রভাব নেই।
-- ═══════════════════════════════════════════════════════════════════════

create or replace view public.tk_demo_suspects as
with src as (
  select 'patients'::text      as tbl, id, name, mobile, coalesce(address,'') as address, branch, "createdAt" from public.patients
  union all
  select 'enquiries'::text,     id, name, mobile, coalesce(address,''),        branch, "createdAt" from public.enquiries
  union all
  select 'followups'::text,     id, name, mobile, coalesce(address,''),        branch, "createdAt" from public.followups
  union all
  select 'doctor_visits'::text, id, name, mobile, coalesce(area,''),           branch, "createdAt" from public.doctor_visits
), f as (
  select s.*,
         regexp_replace(coalesce(s.mobile,''),'\D','','g')                as dig,
         btrim(coalesce(s.name,''))                                       as nm,
         regexp_replace(coalesce(s.name,''),'\s','','g')                  as nm_ns,
         regexp_replace(coalesce(s.address,''),'\s','','g')               as ad_ns
  from src s
), g as (
  select f.*, right(f.dig,10) as m from f
)
select tbl, id, name, mobile, branch, address, "createdAt",
  concat_ws(' · ',
    case when length(dig) >= 7 and m !~ '^[6-9][0-9]{9}$' then 'number shape wrong' end,
    case when length(dig) >= 7 and m ~ '^(.)\1{9}$'       then 'same digit repeated' end,
    case when nm <> '' and nm_ns ~ '(.)\1\1'              then 'name has 3 same letters' end,
    case when ad_ns ~ '(.)\1\1'                           then 'address has 3 same letters' end,
    case when (nm||' '||address) ~* '(test|demo|asdf|qwer|xxxx)' then 'test/demo word' end,
    case when tbl <> 'doctor_visits' and nm <> '' and length(nm_ns) <= 3 then 'name too short' end
  ) as reason
from g
where (length(dig) >= 7 and m !~ '^[6-9][0-9]{9}$')
   or (length(dig) >= 7 and m ~ '^(.)\1{9}$')
   or (nm <> '' and nm_ns ~ '(.)\1\1')
   or ad_ns ~ '(.)\1\1'
   or (nm||' '||address) ~* '(test|demo|asdf|qwer|xxxx)'
   or (tbl <> 'doctor_visits' and nm <> '' and length(nm_ns) <= 3);
