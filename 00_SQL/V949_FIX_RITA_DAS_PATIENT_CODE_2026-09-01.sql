-- ০১.০৯.২০২৬ — TK-নির্দেশ: RITA DAS (8371876340) Falakata-র রোগী, কিন্তু তাঁর
-- পেশেন্ট আইডি ছিল JPE-01092026-003 (জলপাইগুড়ির কোড)।
--
-- কারণ (কোড ধরে যাচাই): কোডের অক্ষর তিনটে `PatientIdGenerator.branchCode()`
-- তৈরি করে **যে ব্রাঞ্চ দিয়ে ডাকা হয়** সেটা থেকে। JPE-CRP-র ক্রস-ব্রাঞ্চ ছাড়
-- (V453) থাকায় রেজিস্ট্রেশনের সময় স্টাফের নিজের ব্রাঞ্চ (Jalpaiguri) বসে গিয়েছিল,
-- অথচ সারির `branch` ঘরে ঠিকই Falakata বসেছিল। ছাড়টা V949-এ বন্ধ করা হয়েছে,
-- তাই নতুন করে আর এমন হবে না।
--
-- ⛔ ওই তারিখে FLK-এর ব্যবহৃত সিরিয়াল দেখে নেওয়া হয়েছে (শুধু -001 ছিল),
--    তাই -002 বসানো হলো — দুজনের এক আইডি হওয়ার ঝুঁকি নেই।
-- ⛔ মোছার/বদলানোর আগে সব `bak_rita_code_01092026`-এ জমা।

create table if not exists public.bak_rita_code_01092026 as
select 'patients' as t, to_jsonb(x) j from public.patients x where x."patientId"='JPE-01092026-003'
union all select 'payments', to_jsonb(x) from public.payments x where x."patientCode"='JPE-01092026-003';

update public.patients set "patientId"='FLK-01092026-002', "updatedAt"=now()
 where "patientId"='JPE-01092026-003';

update public.payments set "patientCode"='FLK-01092026-002', "updatedAt"=now()
 where "patientCode"='JPE-01092026-003';

update fin.rmp_patient_commissions set patient_code='FLK-01092026-002', updated_at=now()
 where patient_code='JPE-01092026-003';
