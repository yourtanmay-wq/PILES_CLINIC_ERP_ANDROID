-- ০১.০৯.২০২৬ — TK-রিপোর্ট (ছবিসহ): "ডেমো নাম্বার গুলো এখনো কেন রয়ে গেছে"।
--   GHJBHJ · +919969999999 · KNE-09082026-002 (Kishanganj)
--
-- খোঁজার নিয়ম: একই অঙ্ক পরপর ৬ বারের বেশি (`mobile ~ '(\d)\1{5,}'`) —
-- পুরো ডেটাবেসে এই একটাই সারি পাওয়া গেছে (patients + followups)।
--
-- ⚠️ TK-কে আগে জানানো হয়েছিল: ১টা টাকার সারি ছিল (₹৪০০), তাই ০৯.০৮.২০২৬-এর
--    আয় ৪০০ কমবে। TK ডেমো নিশ্চিত করে মুছতে বলেছেন।
-- ⛔ মোছার আগে সবকিছু `public.bak_demo_9969999999`-এ জমা।

create table if not exists public.bak_demo_9969999999 as
select 'patients' as t, to_jsonb(x) j from public.patients x where x.mobile like '%9969999999%'
union all select 'followups', to_jsonb(x) from public.followups x where x.mobile like '%9969999999%'
union all select 'enquiries', to_jsonb(x) from public.enquiries x where x.mobile like '%9969999999%'
union all select 'payments', to_jsonb(x) from public.payments x where x.mobile like '%9969999999%';

delete from public.payments  where mobile like '%9969999999%';
delete from public.followups where mobile like '%9969999999%';
delete from public.enquiries where mobile like '%9969999999%';
delete from public.patients  where mobile like '%9969999999%';
