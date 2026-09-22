-- আজকের ইমপোর্টে ৪ জন বিল-সহ ডুপ্লিকেট পাওয়া গেছে (KAYU MALI/SUMITA ROY/
-- MALEKA PARVIN/MINA SARKAR -- এরা লাইভে (KAYUM ALI/SUMITA ROY/MALAKA PARWEEN/
-- MINA SARKAR নামে) আগে থেকেই আছেন, '+91' ফরম্যাটের কারণে ধরা পড়েনি)।
-- সমাধান: পুরনো কিস্তিগুলো লাইভ প্রোফাইলের payments-এ নতুন সারি হিসেবে যোগ,
-- তারপর ডুপ্লিকেট patients সারি ও তার ভুল payments সারি মুছে ফেলা।

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'histmerge_pay_c81bae26cd8b871c', 'treatment', 'Advance', 'Advance', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-08-23', '3000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:30:00.000Z', '2025-08-23T10:30:00.000Z'
),
(
  'histmerge_pay_8f08e3817ff76ea5', 'treatment', '2nd Payment', '2nd Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-08-26', '3000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-26T10:31:00.000Z', '2025-08-26T10:31:00.000Z'
),
(
  'histmerge_pay_23fa72a7f05c010b', 'treatment', '3rd Payment', '3rd Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-08-30', '1500', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:32:00.000Z', '2025-08-30T10:32:00.000Z'
),
(
  'histmerge_pay_5982339749c80274', 'treatment', '4th Payment', '4th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-09-02', '2500', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-02T10:33:00.000Z', '2025-09-02T10:33:00.000Z'
),
(
  'histmerge_pay_5df4a0d17744d278', 'treatment', '5th Payment', '5th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-09-06', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:34:00.000Z', '2025-09-06T10:34:00.000Z'
),
(
  'histmerge_pay_d1ccaf6e449f473d', 'treatment', '6th Payment', '6th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-09-09', '1000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-09T10:35:00.000Z', '2025-09-09T10:35:00.000Z'
),
(
  'histmerge_pay_0e4cf5bef04a37a3', 'treatment', '7th Payment', '7th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-09-13', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:36:00.000Z', '2025-09-13T10:36:00.000Z'
),
(
  'histmerge_pay_8e24f54dd5f3baa3', 'treatment', '8th Payment', '8th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-09-20', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:37:00.000Z', '2025-09-20T10:37:00.000Z'
),
(
  'histmerge_pay_3193a2c6dc5c00f7', 'treatment', '9th Payment', '9th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-10-04', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:38:00.000Z', '2025-10-04T10:38:00.000Z'
),
(
  'histmerge_pay_e0d295602e605c22', 'treatment', '10th Payment', '10th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-10-11', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:39:00.000Z', '2025-10-11T10:39:00.000Z'
),
(
  'histmerge_pay_4e13d0b71029f489', 'treatment', '11th Payment', '11th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-11-08', '1500', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:40:00.000Z', '2025-11-08T10:40:00.000Z'
),
(
  'histmerge_pay_4bef4b641bf63b01', 'treatment', '12th Payment', '12th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-11-15', '1500', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:41:00.000Z', '2025-11-15T10:41:00.000Z'
),
(
  'histmerge_pay_3ab38effbadc77c0', 'treatment', '13th Payment', '13th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-11-22', '3000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-22T10:42:00.000Z', '2025-11-22T10:42:00.000Z'
),
(
  'histmerge_pay_1262944ec01606e1', 'treatment', '14th Payment', '14th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-11-29', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-29T10:43:00.000Z', '2025-11-29T10:43:00.000Z'
),
(
  'histmerge_pay_1edd2aeac25b740c', 'treatment', '15th Payment', '15th Payment', 'pat_7699983914', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-12-06', '3000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-06T10:44:00.000Z', '2025-12-06T10:44:00.000Z'
),
(
  'histmerge_pay_4e99e45fc89d63fb', 'treatment', 'Advance', 'Advance', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-08-26', '5000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-26T10:30:00.000Z', '2025-08-26T10:30:00.000Z'
),
(
  'histmerge_pay_e2880bb14e9be879', 'treatment', '2nd Payment', '2nd Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-09-02', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-02T10:31:00.000Z', '2025-09-02T10:31:00.000Z'
),
(
  'histmerge_pay_32a6f9737accfce7', 'treatment', '3rd Payment', '3rd Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-09-06', '5000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:32:00.000Z', '2025-09-06T10:32:00.000Z'
),
(
  'histmerge_pay_4a476671b6fd302a', 'treatment', '4th Payment', '4th Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-09-09', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-09T10:33:00.000Z', '2025-09-09T10:33:00.000Z'
),
(
  'histmerge_pay_fddae546a7945e8d', 'treatment', '5th Payment', '5th Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-09-13', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:34:00.000Z', '2025-09-13T10:34:00.000Z'
),
(
  'histmerge_pay_b69608b5c274fcb1', 'treatment', '6th Payment', '6th Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-09-27', '500', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:35:00.000Z', '2025-09-27T10:35:00.000Z'
),
(
  'histmerge_pay_3805882f23991bd0', 'treatment', '7th Payment', '7th Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-10-07', '1500', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-07T10:36:00.000Z', '2025-10-07T10:36:00.000Z'
),
(
  'histmerge_pay_25621d448ac61be3', 'treatment', '8th Payment', '8th Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-10-11', '500', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:37:00.000Z', '2025-10-11T10:37:00.000Z'
),
(
  'histmerge_pay_47788a612f154873', 'treatment', '9th Payment', '9th Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-10-14', '500', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-14T10:38:00.000Z', '2025-10-14T10:38:00.000Z'
),
(
  'histmerge_pay_aca60e1e15b6b19a', 'treatment', '10th Payment', '10th Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-10-21', '1000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-21T10:39:00.000Z', '2025-10-21T10:39:00.000Z'
),
(
  'histmerge_pay_f030dd4f4829355a', 'treatment', '11th Payment', '11th Payment', 'pat_8972515634', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-11-15', '1000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:40:00.000Z', '2025-11-15T10:40:00.000Z'
),
(
  'histmerge_pay_6658a4db2d0438cb', 'treatment', 'Advance', 'Advance', 'pat_9932427384', '9932427384', 'Jalpaiguri', 'MALEKA PARVIN',
  '2025-10-21', '3000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-21T10:30:00.000Z', '2025-10-21T10:30:00.000Z'
),
(
  'histmerge_pay_5cb0a9fa639f71a9', 'treatment', '2nd Payment', '2nd Payment', 'pat_9932427384', '9932427384', 'Jalpaiguri', 'MALEKA PARVIN',
  '2025-10-25', '5000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-25T10:31:00.000Z', '2025-10-25T10:31:00.000Z'
),
(
  'histmerge_pay_a3478901f3798d27', 'treatment', '3rd Payment', '3rd Payment', 'pat_9932427384', '9932427384', 'Jalpaiguri', 'MALEKA PARVIN',
  '2025-10-28', '500', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-28T10:32:00.000Z', '2025-10-28T10:32:00.000Z'
),
(
  'histmerge_pay_adec968aae900ce7', 'treatment', '4th Payment', '4th Payment', 'pat_9932427384', '9932427384', 'Jalpaiguri', 'MALEKA PARVIN',
  '2025-11-01', '500', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-01T10:33:00.000Z', '2025-11-01T10:33:00.000Z'
),
(
  'histmerge_pay_5c946f24f1684157', 'treatment', '5th Payment', '5th Payment', 'pat_9932427384', '9932427384', 'Jalpaiguri', 'MALEKA PARVIN',
  '2025-11-08', '1000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:34:00.000Z', '2025-11-08T10:34:00.000Z'
),
(
  'histmerge_pay_c2044471569299b7', 'treatment', '6th Payment', '6th Payment', 'pat_9932427384', '9932427384', 'Jalpaiguri', 'MALEKA PARVIN',
  '2025-11-11', '1000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-11T10:35:00.000Z', '2025-11-11T10:35:00.000Z'
),
(
  'histmerge_pay_d77f55fa0c4834b0', 'treatment', '7th Payment', '7th Payment', 'pat_9932427384', '9932427384', 'Jalpaiguri', 'MALEKA PARVIN',
  '2025-11-18', '1000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-18T10:36:00.000Z', '2025-11-18T10:36:00.000Z'
),
(
  'histmerge_pay_59202adeb7f9548f', 'treatment', '8th Payment', '8th Payment', 'pat_9932427384', '9932427384', 'Jalpaiguri', 'MALEKA PARVIN',
  '2025-12-02', '1000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-02T10:37:00.000Z', '2025-12-02T10:37:00.000Z'
),
(
  'histmerge_pay_221acee417f05f62', 'treatment', '9th Payment', '9th Payment', 'pat_9932427384', '9932427384', 'Jalpaiguri', 'MALEKA PARVIN',
  '2025-12-20', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-20T10:38:00.000Z', '2025-12-20T10:38:00.000Z'
),
(
  'histmerge_pay_336c09a957c98873', 'treatment', 'Advance', 'Advance', 'pat_9647231089', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-01-10', '5000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-10T10:30:00.000Z', '2026-01-10T10:30:00.000Z'
),
(
  'histmerge_pay_2e1c3be56cc8ac83', 'treatment', '2nd Payment', '2nd Payment', 'pat_9647231089', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-01-20', '5000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-20T10:31:00.000Z', '2026-01-20T10:31:00.000Z'
),
(
  'histmerge_pay_9482838ba847fea8', 'treatment', '3rd Payment', '3rd Payment', 'pat_9647231089', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-01-27', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-27T10:32:00.000Z', '2026-01-27T10:32:00.000Z'
),
(
  'histmerge_pay_9b53391244d3d773', 'treatment', '4th Payment', '4th Payment', 'pat_9647231089', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-02-03', '3000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-03T10:33:00.000Z', '2026-02-03T10:33:00.000Z'
),
(
  'histmerge_pay_d402b4259bdf6ac8', 'treatment', '5th Payment', '5th Payment', 'pat_9647231089', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-02-07', '2000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-07T10:34:00.000Z', '2026-02-07T10:34:00.000Z'
),
(
  'histmerge_pay_18cb3d6b4f40ec8c', 'treatment', '6th Payment', '6th Payment', 'pat_9647231089', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-02-14', '3000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-14T10:35:00.000Z', '2026-02-14T10:35:00.000Z'
),
(
  'histmerge_pay_e4fc8113d3ad211c', 'treatment', '7th Payment', '7th Payment', 'pat_9647231089', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-02-28', '1000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-28T10:36:00.000Z', '2026-02-28T10:36:00.000Z'
),
(
  'histmerge_pay_cb2e4364c898acd8', 'treatment', '8th Payment', '8th Payment', 'pat_9647231089', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-03-17', '5000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-03-17T10:37:00.000Z', '2026-03-17T10:37:00.000Z'
),
(
  'histmerge_pay_5b47d43bc4293c5d', 'treatment', '9th Payment', '9th Payment', 'pat_9647231089', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-04-04', '1000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-04-04T10:38:00.000Z', '2026-04-04T10:38:00.000Z'
),
(
  'histmerge_pay_e4209743d9b64fd0', 'treatment', '10th Payment', '10th Payment', 'pat_9647231089', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-04-18', '3000', 'CASH', 'Historical import merge (duplicate-cleanup, 16.09.2026 -- was wrongly a separate patient row)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-04-18T10:39:00.000Z', '2026-04-18T10:39:00.000Z'
);

-- ভুল (ডুপ্লিকেট) সারিগুলোর পুরনো payments মুছে ফেলা:
delete from public.payments where "patientId" = 'histjpe25_7869bed96fb2b219';
delete from public.payments where "patientId" = 'histjpe25_134f162f63c28f5b';
delete from public.payments where "patientId" = 'histjpe25_eb5e4f634f9123f3';
delete from public.payments where "patientId" = 'histjpe25_05590ea482349912';

-- ভুল (ডুপ্লিকেট) patients সারিগুলো মুছে ফেলা:
delete from public.patients where id = 'histjpe25_7869bed96fb2b219' and name = 'KAYU MALI';
delete from public.patients where id = 'histjpe25_134f162f63c28f5b' and name = 'SUMITA ROY';
delete from public.patients where id = 'histjpe25_eb5e4f634f9123f3' and name = 'MALEKA PARVIN';
delete from public.patients where id = 'histjpe25_05590ea482349912' and name = 'MINA SARKAR';
