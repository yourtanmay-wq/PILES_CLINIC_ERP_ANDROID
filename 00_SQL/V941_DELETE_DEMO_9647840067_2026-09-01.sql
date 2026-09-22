-- ০১.০৯.২০২৬ — TK-রিপোর্ট: জলপাইগুড়ির এক পুরনো ডেমো নম্বর (9647840067) থেকে কল
-- এলে ব্যানারে এখনো পুরনো তথ্য (Jalpaiguri · Fissure · PANDA PARA) দেখাচ্ছিল।
--
-- কারণ (কোড ধরে যাচাই): DialerRepository.matchNumbersBatch() সরাসরি
-- `followups` টেবিল পড়ে (CloudReadCache TTL মাত্র ২০ সেকেন্ড, তাই ক্যাশ নয়)।
-- ওই নম্বরের `followups` সারিটা কখনো মোছাই হয়নি — সেটাই দেখাচ্ছিল।
--
-- ⛔ গোটা শ্রেণি একসাথে মোছা হয়নি — যাচাই করে দেখা গেছে "অনাথ" মনে হওয়া
--    ৩১টা সারির বেশিরভাগই আসল এনকোয়ারি ("কাল আসবে", "উনি শুক্রবার আসবেন")।
--    তাই শুধু TK-র বলা নম্বরটাই মোছা হলো, ব্যাকআপ রেখে।

create table if not exists public.bak_9647840067_01092026 as
select 'followups' as t, to_jsonb(x) j from public.followups x where x.mobile like '%9647840067%'
union all select 'enquiries', to_jsonb(x) from public.enquiries x where x.mobile like '%9647840067%'
union all select 'dialer_calls', to_jsonb(x) from public.dialer_calls x where x::text like '%9647840067%'
union all select 'call_remarks', to_jsonb(x) from public.call_remarks x where x::text like '%9647840067%';

delete from public.followups    where mobile like '%9647840067%';
delete from public.enquiries    where mobile like '%9647840067%';
delete from public.dialer_calls where dialer_calls::text like '%9647840067%';
delete from public.call_remarks where call_remarks::text like '%9647840067%';
