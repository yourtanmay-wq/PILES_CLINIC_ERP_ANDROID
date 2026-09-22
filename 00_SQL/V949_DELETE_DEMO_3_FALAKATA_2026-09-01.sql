-- ০১.০৯.২০২৬ — TK-নির্দেশ (ছবিসহ): Falakata-র তিনটে ডেমো রোগী মুছে ফেলা।
--   SUMONA BARMAN  8967819962  FLK-25072026-001
--   Swapna Adhikary 9485088395 FLK-21072026-001
--   PRASENJI GOPE  8382530343  FLK-19032026-002
--
-- ⚠️ TK-কে আগে জানানো হয়েছিল: এদের নামে **৪টা টাকার সারি** ছিল, তাই মুছলে ওই
--    দিনগুলোর আয়ের হিসাব কমবে। TK ডেমো নিশ্চিত করে মুছতে বলেছেন।
-- ⛔ মোছার আগে সবকিছু `bak_demo_3_01092026` টেবিলে জমা রাখা হয়েছে।

create table if not exists public.bak_demo_3_01092026 as
select 'patients' as t, to_jsonb(x) j from public.patients x
 where x.mobile like '%8967819962%' or x.mobile like '%9485088395%' or x.mobile like '%8382530343%'
union all select 'followups', to_jsonb(x) from public.followups x
 where x.mobile like '%8967819962%' or x.mobile like '%9485088395%' or x.mobile like '%8382530343%'
union all select 'payments', to_jsonb(x) from public.payments x
 where x.mobile like '%8967819962%' or x.mobile like '%9485088395%' or x.mobile like '%8382530343%';

delete from public.payments  where mobile like '%8967819962%' or mobile like '%9485088395%' or mobile like '%8382530343%';
delete from public.followups where mobile like '%8967819962%' or mobile like '%9485088395%' or mobile like '%8382530343%';
delete from public.patients  where mobile like '%8967819962%' or mobile like '%9485088395%' or mobile like '%8382530343%';
