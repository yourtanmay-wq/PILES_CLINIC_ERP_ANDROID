-- V1614 (১৯.০৯.২০২৬, TK-রিপোর্ট, ছবিসহ — COB-26072026-002-এ Paid ₹৬১,০০০ দেখাচ্ছিল,
-- আসল টাকা ₹৩৩,০০০ হওয়া উচিত ছিল)।
--
-- আসল কারণ: V1520 (১৬.০৯.২০২৬)-এ কোচবিহারের দুই "held-back different-person"
-- ঐতিহাসিক রোগী (BABU RAHAMAN, GULBAHADUR ALI) নতুন আলাদা রোগী হিসেবে ঢোকানো
-- হয়েছিল ঠিকই, কিন্তু তাঁদের patients.id সাধারণ (histcob_...) রেখে দেওয়া
-- হয়েছিল — অ্যাপের "এক নম্বরে সত্যিই দু'জন আলাদা মানুষ" চেনার একমাত্র প্রমাণ
-- হলো id-টা `pat_<মোবাইল>_...` দিয়ে শুরু হওয়া (isDeclaredSeparateRowId /
-- wlv1IsDeclaredSeparateRowId)। সেই প্রমাণ না থাকায়, ওই একই মোবাইলে পরে অন্য
-- কেউ নতুন করে ভর্তি হলে তাঁর Full Journey/Payment পর্দায় এই পুরনো টাকাও
-- যোগ হয়ে যাচ্ছিল।
--
-- TK নিজে নিশ্চিত করেছেন (১৯.০৯.২০২৬): BABU RAHAMAN ও GULBAHADUR ALI সত্যিই
-- আলাদা মানুষ, শুধু কাকতালীয়ভাবে একই মোবাইল নম্বর অন্য কারো রেজিস্ট্রেশনে
-- আবার ব্যবহার হয়েছে। তাই এখন তাঁদের patients.id-কে সঠিক `pat_<মোবাইল>_...`
-- ফরম্যাটে বদলানো হলো, আর সেই সাথে তাঁদের payments-এর patientId-ও মেলানো
-- হলো — টাকার অঙ্ক/তারিখ/মোড কিছুই বদলায়নি, শুধু কার নামে বসানো ছিল সেই
-- চেনার-চিহ্নটা ঠিক হলো।
--
-- ⛔ কোনো FK constraint payments.patientId → patients.id-এ বসানো নেই
--    (04_SUPABASE_DATABASE_SETUP/V215_SAFE_MIGRATION_2026-07-31.sql-এ ওটা
--    মন্তব্য-করা উদাহরণ মাত্র, বাস্তবে চালু নয়) — তাই দুটো UPDATE যেকোনো
--    ক্রমেই নিরাপদ।
-- ⛔ চালানোর আগে/পরে যাচাই: প্রতিটা UPDATE-এর নিচের SELECT দিয়ে সারি-সংখ্যা
--    ও যোগফল মিলিয়ে দেখা হবে, যাতে একটা টাকার সারিও হারিয়ে না যায়।

begin;

-- ১) BABU RAHAMAN (মোবাইল 9990895281) — বিল ২৪,০০০, ১২টা কিস্তি
update public.patients
   set id = 'pat_9990895281_histcob_9a50c28ce3a21adc'
 where id = 'histcob_9a50c28ce3a21adc';

update public.payments
   set "patientId" = 'pat_9990895281_histcob_9a50c28ce3a21adc'
 where "patientId" = 'histcob_9a50c28ce3a21adc';

-- ২) GULBAHADUR ALI (মোবাইল 6900389133) — বিল ৩৩,০০০, ৭টা কিস্তি
update public.patients
   set id = 'pat_6900389133_histcob_5e392768b6fc62bd'
 where id = 'histcob_5e392768b6fc62bd';

update public.payments
   set "patientId" = 'pat_6900389133_histcob_5e392768b6fc62bd'
 where "patientId" = 'histcob_5e392768b6fc62bd';

commit;

-- ৩) যাচাই (আগে/পরে TK/স্টাফ নিজে মিলিয়ে দেখতে পারেন) —
--    দুটো সারি, যোগফল ২৪০০০ ও ৩৩০০০ ঠিক থাকা উচিত:
select "patientId", count(*) as kisti, sum(amount::numeric) as total
  from public.payments
 where "patientId" in (
   'pat_9990895281_histcob_9a50c28ce3a21adc',
   'pat_6900389133_histcob_5e392768b6fc62bd'
 )
 group by "patientId";
