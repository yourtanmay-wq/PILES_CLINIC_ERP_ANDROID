-- সংশোধিত (V1542-এর জায়গায়): লাইভ payments বিস্তারিতভাবে যাচাই করে দেখা গেল
-- MINA SARKAR ও MALAKA PARWEEN-এর সিটের পুরো টাকার হিসাব ইতিমধ্যেই লাইভে
-- হুবহু (তারিখ-টাকা মিলিয়ে) বসানো আছে -- তাদের ক্ষেত্রে নতুন কিছু যোগ
-- করা হচ্ছে না, শুধু ডুপ্লিকেট সারি মুছে ফেলা হচ্ছে।
-- KAYU MALI ও SUMITA ROY-এর লাইভ প্রোফাইলে শুধু ₹৪০০ Visit Fee ছিল, bill=0
-- (আসল চিকিৎসার বিল কখনো অ্যাপে বসানো হয়নি) -- তাই এদের bill ঠিক করে
-- সিটের কিস্তিগুলো নতুন সারি হিসেবে যোগ করা হচ্ছে (₹৪০০ আলাদা রকমের ফি,
-- কিস্তির তালিকায় নেই, তাই দ্বিগুণ হওয়ার ঝুঁকি নেই)।

-- ১. bill ঠিক করা (শুধু KAYU MALI/SUMITA ROY, যাদের bill=0 ছিল):
update public.patients set bill = '34000' where id = 'pat_7699983914';
update public.patients set bill = '25000' where id = 'pat_8972515634';

-- ২. সিটের কিস্তি নতুন payments সারি হিসেবে যোগ (শুধু KAYU MALI/SUMITA ROY):
insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'histmerge_pay_c81bae26cd8b871c', 'treatment', 'Advance', 'Advance', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-08-23', '3000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:30:00.000Z', '2025-08-23T10:30:00.000Z'
),
(
  'histmerge_pay_8f08e3817ff76ea5', 'treatment', '2nd Payment', '2nd Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-08-26', '3000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-26T10:31:00.000Z', '2025-08-26T10:31:00.000Z'
),
(
  'histmerge_pay_23fa72a7f05c010b', 'treatment', '3rd Payment', '3rd Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-08-30', '1500', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:32:00.000Z', '2025-08-30T10:32:00.000Z'
),
(
  'histmerge_pay_5982339749c80274', 'treatment', '4th Payment', '4th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-09-02', '2500', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-02T10:33:00.000Z', '2025-09-02T10:33:00.000Z'
),
(
  'histmerge_pay_5df4a0d17744d278', 'treatment', '5th Payment', '5th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-09-06', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:34:00.000Z', '2025-09-06T10:34:00.000Z'
),
(
  'histmerge_pay_d1ccaf6e449f473d', 'treatment', '6th Payment', '6th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-09-09', '1000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-09T10:35:00.000Z', '2025-09-09T10:35:00.000Z'
),
(
  'histmerge_pay_0e4cf5bef04a37a3', 'treatment', '7th Payment', '7th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-09-13', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:36:00.000Z', '2025-09-13T10:36:00.000Z'
),
(
  'histmerge_pay_8e24f54dd5f3baa3', 'treatment', '8th Payment', '8th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-09-20', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:37:00.000Z', '2025-09-20T10:37:00.000Z'
),
(
  'histmerge_pay_3193a2c6dc5c00f7', 'treatment', '9th Payment', '9th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-10-04', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:38:00.000Z', '2025-10-04T10:38:00.000Z'
),
(
  'histmerge_pay_e0d295602e605c22', 'treatment', '10th Payment', '10th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-10-11', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:39:00.000Z', '2025-10-11T10:39:00.000Z'
),
(
  'histmerge_pay_4e13d0b71029f489', 'treatment', '11th Payment', '11th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-11-08', '1500', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:40:00.000Z', '2025-11-08T10:40:00.000Z'
),
(
  'histmerge_pay_4bef4b641bf63b01', 'treatment', '12th Payment', '12th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-11-15', '1500', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:41:00.000Z', '2025-11-15T10:41:00.000Z'
),
(
  'histmerge_pay_3ab38effbadc77c0', 'treatment', '13th Payment', '13th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-11-22', '3000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-22T10:42:00.000Z', '2025-11-22T10:42:00.000Z'
),
(
  'histmerge_pay_1262944ec01606e1', 'treatment', '14th Payment', '14th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-11-29', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-29T10:43:00.000Z', '2025-11-29T10:43:00.000Z'
),
(
  'histmerge_pay_1edd2aeac25b740c', 'treatment', '15th Payment', '15th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-12-06', '3000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-06T10:44:00.000Z', '2025-12-06T10:44:00.000Z'
),
(
  'histmerge_pay_4e99e45fc89d63fb', 'treatment', 'Advance', 'Advance', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-08-26', '5000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-26T10:30:00.000Z', '2025-08-26T10:30:00.000Z'
),
(
  'histmerge_pay_e2880bb14e9be879', 'treatment', '2nd Payment', '2nd Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-09-02', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-02T10:31:00.000Z', '2025-09-02T10:31:00.000Z'
),
(
  'histmerge_pay_32a6f9737accfce7', 'treatment', '3rd Payment', '3rd Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-09-06', '5000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:32:00.000Z', '2025-09-06T10:32:00.000Z'
),
(
  'histmerge_pay_4a476671b6fd302a', 'treatment', '4th Payment', '4th Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-09-09', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-09T10:33:00.000Z', '2025-09-09T10:33:00.000Z'
),
(
  'histmerge_pay_fddae546a7945e8d', 'treatment', '5th Payment', '5th Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-09-13', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:34:00.000Z', '2025-09-13T10:34:00.000Z'
),
(
  'histmerge_pay_b69608b5c274fcb1', 'treatment', '6th Payment', '6th Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-09-27', '500', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:35:00.000Z', '2025-09-27T10:35:00.000Z'
),
(
  'histmerge_pay_3805882f23991bd0', 'treatment', '7th Payment', '7th Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-10-07', '1500', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-07T10:36:00.000Z', '2025-10-07T10:36:00.000Z'
),
(
  'histmerge_pay_25621d448ac61be3', 'treatment', '8th Payment', '8th Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-10-11', '500', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:37:00.000Z', '2025-10-11T10:37:00.000Z'
),
(
  'histmerge_pay_47788a612f154873', 'treatment', '9th Payment', '9th Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-10-14', '500', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-14T10:38:00.000Z', '2025-10-14T10:38:00.000Z'
),
(
  'histmerge_pay_aca60e1e15b6b19a', 'treatment', '10th Payment', '10th Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-10-21', '1000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-21T10:39:00.000Z', '2025-10-21T10:39:00.000Z'
),
(
  'histmerge_pay_f030dd4f4829355a', 'treatment', '11th Payment', '11th Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-11-15', '1000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- treatment billing never entered live, only Visit Fee existed)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:40:00.000Z', '2025-11-15T10:40:00.000Z'
);

-- ৩. ডুপ্লিকেট (ভুল) সারিগুলোর পুরনো payments মুছে ফেলা (সব ৪ জনের):
delete from public.payments where "patientId" = 'histjpe25_7869bed96fb2b219';
delete from public.payments where "patientId" = 'histjpe25_134f162f63c28f5b';
delete from public.payments where "patientId" = 'histjpe25_eb5e4f634f9123f3';
delete from public.payments where "patientId" = 'histjpe25_05590ea482349912';

-- ৪. ডুপ্লিকেট (ভুল) patients সারি মুছে ফেলা (সব ৪ জনের):
delete from public.patients where id = 'histjpe25_7869bed96fb2b219' and name = 'KAYU MALI';
delete from public.patients where id = 'histjpe25_134f162f63c28f5b' and name = 'SUMITA ROY';
delete from public.patients where id = 'histjpe25_eb5e4f634f9123f3' and name = 'MALEKA PARVIN';
delete from public.patients where id = 'histjpe25_05590ea482349912' and name = 'MINA SARKAR';
