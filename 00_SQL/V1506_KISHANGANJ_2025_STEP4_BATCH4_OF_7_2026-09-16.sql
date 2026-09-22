-- ২০২৫ শিট ধাপ ৪ -- ব্যাচ 4/7 (রোগী 47-64, মোট 18)
-- ১১৬ জন বিল-করা রোগীর মধ্যে: ৩ জন লাইভ প্রোফাইলে মার্জ (আলাদা SQL-এ
-- হয়ে গেছে), ৫ জন বাদ (শিটেই DATE/PAYMENT ঘর উল্টে/দ্বিতীয়বার বসা --
-- BHAVESH KR DAS, WAJIFA KHATUN, BIKASH NATH YOGI, TAPAN KUMAR DAS,
-- SAMSURUDDIN), ১ জন বাদ (MD AZAM -- DATE ঘরে শুধু একটা টিক-চিহ্ন)।
-- বাকি ১০৭ জন এই ব্যাচগুলোয়। sex নাম দেখে আন্দাজ। REF BY এই শিটে
-- রেফারেল-উৎসের ক্যাটেগরি (LOCAL/ONLINE ইত্যাদি), কোনো RMP-র নাম নয়।

insert into public.patients (
  id, "patientId", date, "registrationDate", "visitDate",
  name, mobile, "altMobile", branch, age, sex,
  address, disease, bill,
  stage, queue, "doctorComplete",
  "createdBy", "registeredBy", "createdAt", "updatedAt"
) values
(
  'hist2025_d5a3c51145e1759c', 'KNE-26062025-001', '2025-06-26', '2025-06-26', '2025-06-26',
  'MAHAMAD MAHFUJ ALAM', '8146078470', '', 'Kishanganj', '50', 'Male',
  'CHROLOYA, DUSMAL, ANGAR, PURNIYA', 'Piles', '45000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-26T10:00:00.000Z', '2025-06-26T10:00:00.000Z'
),
(
  'hist2025_4ddd27499458b346', 'KNE-26062025-002', '2025-06-26', '2025-06-26', '2025-06-26',
  'MOHAMAD CHAND', '8809705409', '', 'Kishanganj', '22', 'Male',
  'RUIDASHA, KISHANGANJ, Kishanganj, Kishanganj', 'Piles', '58000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-26T10:00:00.000Z', '2025-06-26T10:00:00.000Z'
),
(
  'hist2025_10d9644e504705ff', 'KNE-02072025-001', '2025-07-02', '2025-07-02', '2025-07-02',
  'ARSHAD NAIK', '9103252865', '', 'Kishanganj', '27', 'Male',
  'LILIYACHOWK, LILIYACHOWK, LILIYACHOWK, Kishanganj', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-02T10:00:00.000Z', '2025-07-02T10:00:00.000Z'
),
(
  'hist2025_f367f41b21d311a4', 'KNE-05072025-001', '2025-07-05', '2025-07-05', '2025-07-05',
  'MD SAHID', '7061321194', '', 'Kishanganj', '26', 'Male',
  'Balrampur, Barsoi, Balrampur, Katihar', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:00:00.000Z', '2025-07-05T10:00:00.000Z'
),
(
  'hist2025_35ae1d6fe56dc53f', 'KNE-07072025-001', '2025-07-07', '2025-07-07', '2025-07-07',
  'SUNIL JAIN', '8822702712', '', 'Kishanganj', '72', 'Male',
  'PURB PALLI, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-07T10:00:00.000Z', '2025-07-07T10:00:00.000Z'
),
(
  'hist2025_9e912a547990c596', 'KNE-07072025-002', '2025-07-07', '2025-07-07', '2025-07-07',
  'LOVELY RANI', '8884841062', '', 'Kishanganj', '33', 'Female',
  'MILLA PALLI, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-07T10:00:00.000Z', '2025-07-07T10:00:00.000Z'
),
(
  'hist2025_7c49eacd0e2ac5a7', 'KNE-09072025-001', '2025-07-09', '2025-07-09', '2025-07-09',
  'MARJINA', '7908231003', '', 'Kishanganj', '20', 'Male',
  'LODHAN, LODHON, GOYALPOKHAR, U.D', 'Fissure', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-09T10:00:00.000Z', '2025-07-09T10:00:00.000Z'
),
(
  'hist2025_293c0dff82e9c675', 'KNE-09072025-002', '2025-07-09', '2025-07-09', '2025-07-09',
  'CHADNI', '8016775777', '', 'Kishanganj', '20', 'Male',
  'PANJIPARA, PANJIPARA, GOYALPOKHAR, U.D', 'Piles', '100000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-09T10:00:00.000Z', '2025-07-09T10:00:00.000Z'
),
(
  'hist2025_c823748994221ff7', 'KNE-14072025-001', '2025-07-14', '2025-07-14', '2025-07-14',
  'HAKIM UDDIN', '8391991430', '', 'Kishanganj', '18', 'Male',
  'UTTAR DUMORIYA, THAKURBARI, CHAKULIYA, U.D', 'Other', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-14T10:00:00.000Z', '2025-07-14T10:00:00.000Z'
),
(
  'hist2025_3f830b538d44602f', 'KNE-17072025-001', '2025-07-17', '2025-07-17', '2025-07-17',
  'MD SAYED', '8089107966', '', 'Kishanganj', '28', 'Male',
  'CHOURAGACHH, GOTI, GOYALPOKHAR, U.D', 'Piles', '36000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-17T10:00:00.000Z', '2025-07-17T10:00:00.000Z'
),
(
  'hist2025_b3fd6639ece7bc72', 'KNE-17072025-002', '2025-07-17', '2025-07-17', '2025-07-17',
  'SAHANOR KHATOON', '8967399644', '', 'Kishanganj', '22', 'Female',
  'GOYALPOKHAR, LAL KHURI, GOYALPOKHAR, U.D', 'Fistula', '36000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-17T10:00:00.000Z', '2025-07-17T10:00:00.000Z'
),
(
  'hist2025_3e476c864c02cd0d', 'KNE-22072025-001', '2025-07-22', '2025-07-22', '2025-07-22',
  'HASAN ALI', '7872766362', '', 'Kishanganj', '32', 'Male',
  'CHAKULIYA, CHAKULIYA, CHAKULIYA, U.D', 'Other', '45000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-22T10:00:00.000Z', '2025-07-22T10:00:00.000Z'
),
(
  'hist2025_f22f28d45b7e5b6b', 'KNE-23072025-001', '2025-07-23', '2025-07-23', '2025-07-23',
  'FARHA PARVIN', '7865803067', '', 'Kishanganj', '18', 'Female',
  'BASTADANGI, SUMALIA, DALKHOLA, U. D', 'Piles', '24000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-23T10:00:00.000Z', '2025-07-23T10:00:00.000Z'
),
(
  'hist2025_4daaef4c04ad44ce', 'KNE-24072025-001', '2025-07-24', '2025-07-24', '2025-07-24',
  'MD SAKIL', '7888475776', '', 'Kishanganj', '27', 'Male',
  'GHORA, GHORA, Goyalpokhar, U. D', 'Fistula', '97500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-24T10:00:00.000Z', '2025-07-24T10:00:00.000Z'
),
(
  'hist2025_d755c537eebc155c', 'KNE-29072025-001', '2025-07-29', '2025-07-29', '2025-07-29',
  'MAIMUL HOWK', '7009135532', '', 'Kishanganj', '26', 'Male',
  'CHURRAKUTTI, GHORRA, GOYALPOKHAR, U.D', 'Piles', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-29T10:00:00.000Z', '2025-07-29T10:00:00.000Z'
),
(
  'hist2025_78bdba7af4d2b9cb', 'KNE-08082025-001', '2025-08-08', '2025-08-08', '2025-08-08',
  'CHOTU MARDIN', '9907652834', '', 'Kishanganj', '45', 'Male',
  'DEUGOU, MUNDES, CHAKULIYA, U.D', 'Piles', '115000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-08T10:00:00.000Z', '2025-08-08T10:00:00.000Z'
),
(
  'hist2025_a1ab6455b6f93caa', 'KNE-21082025-001', '2025-08-21', '2025-08-21', '2025-08-21',
  'TARAFUL KHATOON', '9296362956', '', 'Kishanganj', '25', 'Female',
  'LILIYA CHOWK, MEHERGANCH, BAHADURGANJ, KISHANGANJ', 'Piles', '36000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-21T10:00:00.000Z', '2025-08-21T10:00:00.000Z'
),
(
  'hist2025_51de98e074702388', 'KNE-21082025-002', '2025-08-21', '2025-08-21', '2025-08-21',
  'NUSRAT JAHA', '9973609230', '', 'Kishanganj', '35', 'Male',
  'AMABOSE, HAPHONIYA, AMOU, PIURNIYA', 'Fistula', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-21T10:00:00.000Z', '2025-08-21T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'hist2025_pay_fe61652f107ad0c0', 'treatment', 'Advance', 'Advance', 'hist2025_d5a3c51145e1759c', '8146078470', 'Kishanganj', 'MAHAMAD MAHFUJ ALAM',
  '2025-06-26', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-26T10:15:00.000Z', '2025-06-26T10:15:00.000Z'
),
(
  'hist2025_pay_77b4c14f1c1c4d83', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_d5a3c51145e1759c', '8146078470', 'Kishanganj', 'MAHAMAD MAHFUJ ALAM',
  '2025-07-02', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-02T10:16:00.000Z', '2025-07-02T10:16:00.000Z'
),
(
  'hist2025_pay_9b44a4707458f50a', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_d5a3c51145e1759c', '8146078470', 'Kishanganj', 'MAHAMAD MAHFUJ ALAM',
  '2025-07-05', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:17:00.000Z', '2025-07-05T10:17:00.000Z'
),
(
  'hist2025_pay_21cf44d6ab36ecf2', 'treatment', '4th Payment', '4th Payment', 'hist2025_d5a3c51145e1759c', '8146078470', 'Kishanganj', 'MAHAMAD MAHFUJ ALAM',
  '2025-07-09', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-09T10:18:00.000Z', '2025-07-09T10:18:00.000Z'
),
(
  'hist2025_pay_e46dfbdfd3911e53', 'treatment', '5th Payment', '5th Payment', 'hist2025_d5a3c51145e1759c', '8146078470', 'Kishanganj', 'MAHAMAD MAHFUJ ALAM',
  '2025-07-15', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-15T10:19:00.000Z', '2025-07-15T10:19:00.000Z'
),
(
  'hist2025_pay_871e51e21c85e7ee', 'treatment', '6th Payment', '6th Payment', 'hist2025_d5a3c51145e1759c', '8146078470', 'Kishanganj', 'MAHAMAD MAHFUJ ALAM',
  '2025-07-19', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-19T10:20:00.000Z', '2025-07-19T10:20:00.000Z'
),
(
  'hist2025_pay_fb9dd5a2212b9cab', 'treatment', '7th Payment', '7th Payment', 'hist2025_d5a3c51145e1759c', '8146078470', 'Kishanganj', 'MAHAMAD MAHFUJ ALAM',
  '2025-07-23', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-23T10:21:00.000Z', '2025-07-23T10:21:00.000Z'
),
(
  'hist2025_pay_dcf9219b9ba3cd62', 'treatment', '8th Payment', '8th Payment', 'hist2025_d5a3c51145e1759c', '8146078470', 'Kishanganj', 'MAHAMAD MAHFUJ ALAM',
  '2025-07-30', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-30T10:22:00.000Z', '2025-07-30T10:22:00.000Z'
),
(
  'hist2025_pay_4da8128f02bacd5c', 'treatment', 'Advance', 'Advance', 'hist2025_4ddd27499458b346', '8809705409', 'Kishanganj', 'MOHAMAD CHAND',
  '2025-06-27', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-27T10:15:00.000Z', '2025-06-27T10:15:00.000Z'
),
(
  'hist2025_pay_6d3ae265da19c346', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_4ddd27499458b346', '8809705409', 'Kishanganj', 'MOHAMAD CHAND',
  '2025-07-02', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-02T10:16:00.000Z', '2025-07-02T10:16:00.000Z'
),
(
  'hist2025_pay_d2f9dc3269789724', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_4ddd27499458b346', '8809705409', 'Kishanganj', 'MOHAMAD CHAND',
  '2025-07-05', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:17:00.000Z', '2025-07-05T10:17:00.000Z'
),
(
  'hist2025_pay_b0d0392491b4b980', 'treatment', '4th Payment', '4th Payment', 'hist2025_4ddd27499458b346', '8809705409', 'Kishanganj', 'MOHAMAD CHAND',
  '2025-07-12', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-12T10:18:00.000Z', '2025-07-12T10:18:00.000Z'
),
(
  'hist2025_pay_c901dae909ec6c92', 'treatment', '5th Payment', '5th Payment', 'hist2025_4ddd27499458b346', '8809705409', 'Kishanganj', 'MOHAMAD CHAND',
  '2025-07-16', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-16T10:19:00.000Z', '2025-07-16T10:19:00.000Z'
),
(
  'hist2025_pay_02053c598631942b', 'treatment', '6th Payment', '6th Payment', 'hist2025_4ddd27499458b346', '8809705409', 'Kishanganj', 'MOHAMAD CHAND',
  '2025-07-23', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-23T10:20:00.000Z', '2025-07-23T10:20:00.000Z'
),
(
  'hist2025_pay_9b150c22f9e4b6ed', 'treatment', '7th Payment', '7th Payment', 'hist2025_4ddd27499458b346', '8809705409', 'Kishanganj', 'MOHAMAD CHAND',
  '2025-08-12', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-12T10:21:00.000Z', '2025-08-12T10:21:00.000Z'
),
(
  'hist2025_pay_b6aaae12c3dc3c38', 'treatment', '8th Payment', '8th Payment', 'hist2025_4ddd27499458b346', '8809705409', 'Kishanganj', 'MOHAMAD CHAND',
  '2025-08-20', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-20T10:22:00.000Z', '2025-08-20T10:22:00.000Z'
),
(
  'hist2025_pay_31eb565a8e66eb40', 'treatment', '9th Payment', '9th Payment', 'hist2025_4ddd27499458b346', '8809705409', 'Kishanganj', 'MOHAMAD CHAND',
  '2025-08-23', '2500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:23:00.000Z', '2025-08-23T10:23:00.000Z'
),
(
  'hist2025_pay_c65c601ee4952669', 'treatment', '10th Payment', '10th Payment', 'hist2025_4ddd27499458b346', '8809705409', 'Kishanganj', 'MOHAMAD CHAND',
  '2025-09-03', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-03T10:24:00.000Z', '2025-09-03T10:24:00.000Z'
),
(
  'hist2025_pay_c6e6cd85586cf1f5', 'treatment', '11th Payment', '11th Payment', 'hist2025_4ddd27499458b346', '8809705409', 'Kishanganj', 'MOHAMAD CHAND',
  '2025-09-20', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:25:00.000Z', '2025-09-20T10:25:00.000Z'
),
(
  'hist2025_pay_2997e41191a0f532', 'treatment', '12th Payment', '12th Payment', 'hist2025_4ddd27499458b346', '8809705409', 'Kishanganj', 'MOHAMAD CHAND',
  '2025-09-24', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-24T10:26:00.000Z', '2025-09-24T10:26:00.000Z'
),
(
  'hist2025_pay_86d0dffd314b4bd1', 'treatment', 'Advance', 'Advance', 'hist2025_10d9644e504705ff', '9103252865', 'Kishanganj', 'ARSHAD NAIK',
  '2025-07-02', '10000', 'CASH', 'Historical import (2025 Google Sheet) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-02T10:15:00.000Z', '2025-07-02T10:15:00.000Z'
),
(
  'hist2025_pay_2dee319c6cf3d800', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_10d9644e504705ff', '9103252865', 'Kishanganj', 'ARSHAD NAIK',
  '2025-07-02', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-02T10:16:00.000Z', '2025-07-02T10:16:00.000Z'
),
(
  'hist2025_pay_642e4fbd7f6daf17', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_10d9644e504705ff', '9103252865', 'Kishanganj', 'ARSHAD NAIK',
  '2025-08-01', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-01T10:17:00.000Z', '2025-08-01T10:17:00.000Z'
),
(
  'hist2025_pay_1b1e03ee06a1add8', 'treatment', 'Advance', 'Advance', 'hist2025_f367f41b21d311a4', '7061321194', 'Kishanganj', 'MD SAHID',
  '2025-07-05', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:15:00.000Z', '2025-07-05T10:15:00.000Z'
),
(
  'hist2025_pay_ee6690c1050b5c16', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_f367f41b21d311a4', '7061321194', 'Kishanganj', 'MD SAHID',
  '2025-07-09', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-09T10:16:00.000Z', '2025-07-09T10:16:00.000Z'
),
(
  'hist2025_pay_28ba7ff2c1074368', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_f367f41b21d311a4', '7061321194', 'Kishanganj', 'MD SAHID',
  '2025-07-14', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-14T10:17:00.000Z', '2025-07-14T10:17:00.000Z'
),
(
  'hist2025_pay_b42eace1d468f126', 'treatment', '4th Payment', '4th Payment', 'hist2025_f367f41b21d311a4', '7061321194', 'Kishanganj', 'MD SAHID',
  '2025-07-19', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-19T10:18:00.000Z', '2025-07-19T10:18:00.000Z'
),
(
  'hist2025_pay_a043f41a05980d77', 'treatment', '5th Payment', '5th Payment', 'hist2025_f367f41b21d311a4', '7061321194', 'Kishanganj', 'MD SAHID',
  '2025-07-23', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-23T10:19:00.000Z', '2025-07-23T10:19:00.000Z'
),
(
  'hist2025_pay_84e570820173384c', 'treatment', '6th Payment', '6th Payment', 'hist2025_f367f41b21d311a4', '7061321194', 'Kishanganj', 'MD SAHID',
  '2025-07-28', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-28T10:20:00.000Z', '2025-07-28T10:20:00.000Z'
),
(
  'hist2025_pay_fb8ef5eddd0e50bb', 'treatment', '7th Payment', '7th Payment', 'hist2025_f367f41b21d311a4', '7061321194', 'Kishanganj', 'MD SAHID',
  '2025-08-06', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-06T10:21:00.000Z', '2025-08-06T10:21:00.000Z'
),
(
  'hist2025_pay_ab082a0e683747bd', 'treatment', 'Advance', 'Advance', 'hist2025_35ae1d6fe56dc53f', '8822702712', 'Kishanganj', 'SUNIL JAIN',
  '2025-07-07', '8000', 'CASH', 'Historical import (2025 Google Sheet) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-07T10:15:00.000Z', '2025-07-07T10:15:00.000Z'
),
(
  'hist2025_pay_0f20b7ac9eada31c', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_35ae1d6fe56dc53f', '8822702712', 'Kishanganj', 'SUNIL JAIN',
  '2025-07-08', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-08T10:16:00.000Z', '2025-07-08T10:16:00.000Z'
),
(
  'hist2025_pay_0c3ba2333807c1d6', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_35ae1d6fe56dc53f', '8822702712', 'Kishanganj', 'SUNIL JAIN',
  '2025-07-10', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-10T10:17:00.000Z', '2025-07-10T10:17:00.000Z'
),
(
  'hist2025_pay_dfa3360d0635fc8a', 'treatment', 'Advance', 'Advance', 'hist2025_9e912a547990c596', '8884841062', 'Kishanganj', 'LOVELY RANI',
  '2025-07-07', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-07T10:15:00.000Z', '2025-07-07T10:15:00.000Z'
),
(
  'hist2025_pay_e14de32dbce9f33e', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_9e912a547990c596', '8884841062', 'Kishanganj', 'LOVELY RANI',
  '2025-07-09', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-09T10:16:00.000Z', '2025-07-09T10:16:00.000Z'
),
(
  'hist2025_pay_51fe5dd76e72149d', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_9e912a547990c596', '8884841062', 'Kishanganj', 'LOVELY RANI',
  '2025-07-12', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-12T10:17:00.000Z', '2025-07-12T10:17:00.000Z'
),
(
  'hist2025_pay_b3c7c2a3279795be', 'treatment', '4th Payment', '4th Payment', 'hist2025_9e912a547990c596', '8884841062', 'Kishanganj', 'LOVELY RANI',
  '2025-07-16', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-16T10:18:00.000Z', '2025-07-16T10:18:00.000Z'
),
(
  'hist2025_pay_bff4e7cb4a1de378', 'treatment', 'Advance', 'Advance', 'hist2025_7c49eacd0e2ac5a7', '7908231003', 'Kishanganj', 'MARJINA',
  '2025-07-09', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-09T10:15:00.000Z', '2025-07-09T10:15:00.000Z'
),
(
  'hist2025_pay_75d0188461c537ac', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_7c49eacd0e2ac5a7', '7908231003', 'Kishanganj', 'MARJINA',
  '2025-07-12', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-12T10:16:00.000Z', '2025-07-12T10:16:00.000Z'
),
(
  'hist2025_pay_1acb7a7cea47ac6d', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_7c49eacd0e2ac5a7', '7908231003', 'Kishanganj', 'MARJINA',
  '2025-07-18', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-18T10:17:00.000Z', '2025-07-18T10:17:00.000Z'
),
(
  'hist2025_pay_6a6e55a3b752ace4', 'treatment', '4th Payment', '4th Payment', 'hist2025_7c49eacd0e2ac5a7', '7908231003', 'Kishanganj', 'MARJINA',
  '2025-07-23', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-23T10:18:00.000Z', '2025-07-23T10:18:00.000Z'
),
(
  'hist2025_pay_074a0339fa93fdec', 'treatment', '5th Payment', '5th Payment', 'hist2025_7c49eacd0e2ac5a7', '7908231003', 'Kishanganj', 'MARJINA',
  '2025-07-26', '11000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:19:00.000Z', '2025-07-26T10:19:00.000Z'
),
(
  'hist2025_pay_51bbf1488b08ee98', 'treatment', '6th Payment', '6th Payment', 'hist2025_7c49eacd0e2ac5a7', '7908231003', 'Kishanganj', 'MARJINA',
  '2025-07-30', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-30T10:20:00.000Z', '2025-07-30T10:20:00.000Z'
),
(
  'hist2025_pay_423889846855be35', 'treatment', '7th Payment', '7th Payment', 'hist2025_7c49eacd0e2ac5a7', '7908231003', 'Kishanganj', 'MARJINA',
  '2025-08-06', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-06T10:21:00.000Z', '2025-08-06T10:21:00.000Z'
),
(
  'hist2025_pay_bff710e373775772', 'treatment', '8th Payment', '8th Payment', 'hist2025_7c49eacd0e2ac5a7', '7908231003', 'Kishanganj', 'MARJINA',
  '2025-08-13', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-13T10:22:00.000Z', '2025-08-13T10:22:00.000Z'
),
(
  'hist2025_pay_dc083bbba3bdb114', 'treatment', '9th Payment', '9th Payment', 'hist2025_7c49eacd0e2ac5a7', '7908231003', 'Kishanganj', 'MARJINA',
  '2025-08-30', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:23:00.000Z', '2025-08-30T10:23:00.000Z'
),
(
  'hist2025_pay_580b7a9015843463', 'treatment', 'Advance', 'Advance', 'hist2025_293c0dff82e9c675', '8016775777', 'Kishanganj', 'CHADNI',
  '2025-07-09', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-09T10:15:00.000Z', '2025-07-09T10:15:00.000Z'
),
(
  'hist2025_pay_09f894ec3019a6ed', 'treatment', 'Advance', 'Advance', 'hist2025_c823748994221ff7', '8391991430', 'Kishanganj', 'HAKIM UDDIN',
  '2025-07-22', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-22T10:15:00.000Z', '2025-07-22T10:15:00.000Z'
),
(
  'hist2025_pay_56ffb642d92f4af0', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_c823748994221ff7', '8391991430', 'Kishanganj', 'HAKIM UDDIN',
  '2025-07-23', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-23T10:16:00.000Z', '2025-07-23T10:16:00.000Z'
),
(
  'hist2025_pay_a2cfedd942332f1c', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_c823748994221ff7', '8391991430', 'Kishanganj', 'HAKIM UDDIN',
  '2025-07-26', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:17:00.000Z', '2025-07-26T10:17:00.000Z'
),
(
  'hist2025_pay_395ed519865cdf5e', 'treatment', '4th Payment', '4th Payment', 'hist2025_c823748994221ff7', '8391991430', 'Kishanganj', 'HAKIM UDDIN',
  '2025-08-02', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:18:00.000Z', '2025-08-02T10:18:00.000Z'
),
(
  'hist2025_pay_b2a7d2eabbb9c39a', 'treatment', '5th Payment', '5th Payment', 'hist2025_c823748994221ff7', '8391991430', 'Kishanganj', 'HAKIM UDDIN',
  '2025-08-06', '5500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-06T10:19:00.000Z', '2025-08-06T10:19:00.000Z'
),
(
  'hist2025_pay_9a69d1e3e085c86f', 'treatment', '6th Payment', '6th Payment', 'hist2025_c823748994221ff7', '8391991430', 'Kishanganj', 'HAKIM UDDIN',
  '2025-08-09', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-09T10:20:00.000Z', '2025-08-09T10:20:00.000Z'
),
(
  'hist2025_pay_e2296103a1c18556', 'treatment', '7th Payment', '7th Payment', 'hist2025_c823748994221ff7', '8391991430', 'Kishanganj', 'HAKIM UDDIN',
  '2025-08-13', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-13T10:21:00.000Z', '2025-08-13T10:21:00.000Z'
),
(
  'hist2025_pay_c396143ef53aa8bc', 'treatment', '8th Payment', '8th Payment', 'hist2025_c823748994221ff7', '8391991430', 'Kishanganj', 'HAKIM UDDIN',
  '2025-08-18', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-18T10:22:00.000Z', '2025-08-18T10:22:00.000Z'
),
(
  'hist2025_pay_b11953c223886752', 'treatment', '9th Payment', '9th Payment', 'hist2025_c823748994221ff7', '8391991430', 'Kishanganj', 'HAKIM UDDIN',
  '2025-09-03', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-03T10:23:00.000Z', '2025-09-03T10:23:00.000Z'
),
(
  'hist2025_pay_0682c6df065ddd91', 'treatment', 'Advance', 'Advance', 'hist2025_3f830b538d44602f', '8089107966', 'Kishanganj', 'MD SAYED',
  '2025-07-17', '6000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-17T10:15:00.000Z', '2025-07-17T10:15:00.000Z'
),
(
  'hist2025_pay_0eed1f8bdf647516', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_3f830b538d44602f', '8089107966', 'Kishanganj', 'MD SAYED',
  '2025-07-23', '9000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-23T10:16:00.000Z', '2025-07-23T10:16:00.000Z'
),
(
  'hist2025_pay_6757da8531b2a7be', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_3f830b538d44602f', '8089107966', 'Kishanganj', 'MD SAYED',
  '2025-08-02', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:17:00.000Z', '2025-08-02T10:17:00.000Z'
),
(
  'hist2025_pay_c66e928f2a55c2bd', 'treatment', '4th Payment', '4th Payment', 'hist2025_3f830b538d44602f', '8089107966', 'Kishanganj', 'MD SAYED',
  '2025-08-06', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-06T10:18:00.000Z', '2025-08-06T10:18:00.000Z'
),
(
  'hist2025_pay_205bb7a0b937db62', 'treatment', '5th Payment', '5th Payment', 'hist2025_3f830b538d44602f', '8089107966', 'Kishanganj', 'MD SAYED',
  '2025-08-10', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-10T10:19:00.000Z', '2025-08-10T10:19:00.000Z'
),
(
  'hist2025_pay_de35496d3d653df4', 'treatment', '6th Payment', '6th Payment', 'hist2025_3f830b538d44602f', '8089107966', 'Kishanganj', 'MD SAYED',
  '2025-08-13', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-13T10:20:00.000Z', '2025-08-13T10:20:00.000Z'
),
(
  'hist2025_pay_5dd8ae5212275961', 'treatment', '7th Payment', '7th Payment', 'hist2025_3f830b538d44602f', '8089107966', 'Kishanganj', 'MD SAYED',
  '2025-08-16', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:21:00.000Z', '2025-08-16T10:21:00.000Z'
),
(
  'hist2025_pay_0e762d93cf872e55', 'treatment', '8th Payment', '8th Payment', 'hist2025_3f830b538d44602f', '8089107966', 'Kishanganj', 'MD SAYED',
  '2025-08-20', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-20T10:22:00.000Z', '2025-08-20T10:22:00.000Z'
),
(
  'hist2025_pay_f52c7ce6557e0696', 'treatment', 'Advance', 'Advance', 'hist2025_b3fd6639ece7bc72', '8967399644', 'Kishanganj', 'SAHANOR KHATOON',
  '2025-07-17', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-17T10:15:00.000Z', '2025-07-17T10:15:00.000Z'
),
(
  'hist2025_pay_ef32363bbd962fbd', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_b3fd6639ece7bc72', '8967399644', 'Kishanganj', 'SAHANOR KHATOON',
  '2025-07-23', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-23T10:16:00.000Z', '2025-07-23T10:16:00.000Z'
),
(
  'hist2025_pay_928776b70f5c1c22', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_b3fd6639ece7bc72', '8967399644', 'Kishanganj', 'SAHANOR KHATOON',
  '2025-07-26', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:17:00.000Z', '2025-07-26T10:17:00.000Z'
),
(
  'hist2025_pay_44bc7c2c6be3f78d', 'treatment', '4th Payment', '4th Payment', 'hist2025_b3fd6639ece7bc72', '8967399644', 'Kishanganj', 'SAHANOR KHATOON',
  '2025-07-30', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-30T10:18:00.000Z', '2025-07-30T10:18:00.000Z'
),
(
  'hist2025_pay_7a46deeb8320a9eb', 'treatment', 'Advance', 'Advance', 'hist2025_3e476c864c02cd0d', '7872766362', 'Kishanganj', 'HASAN ALI',
  '2025-07-22', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-22T10:15:00.000Z', '2025-07-22T10:15:00.000Z'
),
(
  'hist2025_pay_2be8d5d966244ed1', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_3e476c864c02cd0d', '7872766362', 'Kishanganj', 'HASAN ALI',
  '2025-07-23', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-23T10:16:00.000Z', '2025-07-23T10:16:00.000Z'
),
(
  'hist2025_pay_c2492d12bf160b6a', 'treatment', 'Advance', 'Advance', 'hist2025_f22f28d45b7e5b6b', '7865803067', 'Kishanganj', 'FARHA PARVIN',
  '2025-07-23', '7000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-23T10:15:00.000Z', '2025-07-23T10:15:00.000Z'
),
(
  'hist2025_pay_73505351b9b114f4', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_f22f28d45b7e5b6b', '7865803067', 'Kishanganj', 'FARHA PARVIN',
  '2025-07-30', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-30T10:16:00.000Z', '2025-07-30T10:16:00.000Z'
),
(
  'hist2025_pay_7437956b3a378d37', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_f22f28d45b7e5b6b', '7865803067', 'Kishanganj', 'FARHA PARVIN',
  '2025-08-11', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-11T10:17:00.000Z', '2025-08-11T10:17:00.000Z'
),
(
  'hist2025_pay_0444adfd90154c47', 'treatment', 'Advance', 'Advance', 'hist2025_4daaef4c04ad44ce', '7888475776', 'Kishanganj', 'MD SAKIL',
  '2025-07-26', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:15:00.000Z', '2025-07-26T10:15:00.000Z'
),
(
  'hist2025_pay_48e6d2e17e6837a4', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_4daaef4c04ad44ce', '7888475776', 'Kishanganj', 'MD SAKIL',
  '2025-08-02', '20000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:16:00.000Z', '2025-08-02T10:16:00.000Z'
),
(
  'hist2025_pay_990fcaaa7a359847', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_4daaef4c04ad44ce', '7888475776', 'Kishanganj', 'MD SAKIL',
  '2025-08-09', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-09T10:17:00.000Z', '2025-08-09T10:17:00.000Z'
),
(
  'hist2025_pay_5c5eaacea0d83ae4', 'treatment', '4th Payment', '4th Payment', 'hist2025_4daaef4c04ad44ce', '7888475776', 'Kishanganj', 'MD SAKIL',
  '2025-08-13', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-13T10:18:00.000Z', '2025-08-13T10:18:00.000Z'
),
(
  'hist2025_pay_7c846f292e2f7b5c', 'treatment', '5th Payment', '5th Payment', 'hist2025_4daaef4c04ad44ce', '7888475776', 'Kishanganj', 'MD SAKIL',
  '2025-08-16', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:19:00.000Z', '2025-08-16T10:19:00.000Z'
),
(
  'hist2025_pay_4b386f28e8a1c260', 'treatment', 'Advance', 'Advance', 'hist2025_d755c537eebc155c', '7009135532', 'Kishanganj', 'MAIMUL HOWK',
  '2025-07-30', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-30T10:15:00.000Z', '2025-07-30T10:15:00.000Z'
),
(
  'hist2025_pay_7a687cb2c3388646', 'treatment', 'Advance', 'Advance', 'hist2025_78bdba7af4d2b9cb', '9907652834', 'Kishanganj', 'CHOTU MARDIN',
  '2025-08-08', '6000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-08T10:15:00.000Z', '2025-08-08T10:15:00.000Z'
),
(
  'hist2025_pay_0913568a05a5885d', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_78bdba7af4d2b9cb', '9907652834', 'Kishanganj', 'CHOTU MARDIN',
  '2025-08-13', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-13T10:16:00.000Z', '2025-08-13T10:16:00.000Z'
),
(
  'hist2025_pay_4f845ce5b3028fe4', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_78bdba7af4d2b9cb', '9907652834', 'Kishanganj', 'CHOTU MARDIN',
  '2025-08-16', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:17:00.000Z', '2025-08-16T10:17:00.000Z'
),
(
  'hist2025_pay_cc6b94ad5aa4e8e3', 'treatment', '4th Payment', '4th Payment', 'hist2025_78bdba7af4d2b9cb', '9907652834', 'Kishanganj', 'CHOTU MARDIN',
  '2025-08-18', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-18T10:18:00.000Z', '2025-08-18T10:18:00.000Z'
),
(
  'hist2025_pay_6932bfc3c79f8ead', 'treatment', '5th Payment', '5th Payment', 'hist2025_78bdba7af4d2b9cb', '9907652834', 'Kishanganj', 'CHOTU MARDIN',
  '2025-08-20', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-20T10:19:00.000Z', '2025-08-20T10:19:00.000Z'
),
(
  'hist2025_pay_e2cf3bd3a5460f9a', 'treatment', '6th Payment', '6th Payment', 'hist2025_78bdba7af4d2b9cb', '9907652834', 'Kishanganj', 'CHOTU MARDIN',
  '2025-08-23', '14000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:20:00.000Z', '2025-08-23T10:20:00.000Z'
),
(
  'hist2025_pay_a34079e3c3b7f8e6', 'treatment', '7th Payment', '7th Payment', 'hist2025_78bdba7af4d2b9cb', '9907652834', 'Kishanganj', 'CHOTU MARDIN',
  '2025-08-27', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-27T10:21:00.000Z', '2025-08-27T10:21:00.000Z'
),
(
  'hist2025_pay_36a41ac40a563db3', 'treatment', '8th Payment', '8th Payment', 'hist2025_78bdba7af4d2b9cb', '9907652834', 'Kishanganj', 'CHOTU MARDIN',
  '2025-08-30', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:22:00.000Z', '2025-08-30T10:22:00.000Z'
),
(
  'hist2025_pay_e8e902a0ac9ca010', 'treatment', '9th Payment', '9th Payment', 'hist2025_78bdba7af4d2b9cb', '9907652834', 'Kishanganj', 'CHOTU MARDIN',
  '2025-09-06', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:23:00.000Z', '2025-09-06T10:23:00.000Z'
),
(
  'hist2025_pay_950ce1e6549da1c5', 'treatment', '10th Payment', '10th Payment', 'hist2025_78bdba7af4d2b9cb', '9907652834', 'Kishanganj', 'CHOTU MARDIN',
  '2025-09-11', '7500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-11T10:24:00.000Z', '2025-09-11T10:24:00.000Z'
),
(
  'hist2025_pay_20b6496c140ecedf', 'treatment', '11th Payment', '11th Payment', 'hist2025_78bdba7af4d2b9cb', '9907652834', 'Kishanganj', 'CHOTU MARDIN',
  '2025-09-15', '12500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-15T10:25:00.000Z', '2025-09-15T10:25:00.000Z'
),
(
  'hist2025_pay_b5c81333b0de43e8', 'treatment', 'Advance', 'Advance', 'hist2025_a1ab6455b6f93caa', '9296362956', 'Kishanganj', 'TARAFUL KHATOON',
  '2025-08-21', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-21T10:15:00.000Z', '2025-08-21T10:15:00.000Z'
),
(
  'hist2025_pay_961206c93ac5d5d1', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_a1ab6455b6f93caa', '9296362956', 'Kishanganj', 'TARAFUL KHATOON',
  '2025-08-23', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:16:00.000Z', '2025-08-23T10:16:00.000Z'
),
(
  'hist2025_pay_6636623d0f9577f0', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_a1ab6455b6f93caa', '9296362956', 'Kishanganj', 'TARAFUL KHATOON',
  '2025-08-25', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-25T10:17:00.000Z', '2025-08-25T10:17:00.000Z'
),
(
  'hist2025_pay_3a047102db8218eb', 'treatment', '4th Payment', '4th Payment', 'hist2025_a1ab6455b6f93caa', '9296362956', 'Kishanganj', 'TARAFUL KHATOON',
  '2025-08-27', '8000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-27T10:18:00.000Z', '2025-08-27T10:18:00.000Z'
),
(
  'hist2025_pay_e1b064303e201e3c', 'treatment', '5th Payment', '5th Payment', 'hist2025_a1ab6455b6f93caa', '9296362956', 'Kishanganj', 'TARAFUL KHATOON',
  '2025-08-30', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:19:00.000Z', '2025-08-30T10:19:00.000Z'
),
(
  'hist2025_pay_b02fb123551d7048', 'treatment', '6th Payment', '6th Payment', 'hist2025_a1ab6455b6f93caa', '9296362956', 'Kishanganj', 'TARAFUL KHATOON',
  '2025-09-03', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-03T10:20:00.000Z', '2025-09-03T10:20:00.000Z'
),
(
  'hist2025_pay_d96dd2fa7b963a38', 'treatment', '7th Payment', '7th Payment', 'hist2025_a1ab6455b6f93caa', '9296362956', 'Kishanganj', 'TARAFUL KHATOON',
  '2025-09-13', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:21:00.000Z', '2025-09-13T10:21:00.000Z'
),
(
  'hist2025_pay_cb41bdfcf43cf93f', 'treatment', '8th Payment', '8th Payment', 'hist2025_a1ab6455b6f93caa', '9296362956', 'Kishanganj', 'TARAFUL KHATOON',
  '2025-09-20', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:22:00.000Z', '2025-09-20T10:22:00.000Z'
),
(
  'hist2025_pay_75287cbd4d5f36e3', 'treatment', '9th Payment', '9th Payment', 'hist2025_a1ab6455b6f93caa', '9296362956', 'Kishanganj', 'TARAFUL KHATOON',
  '2025-10-04', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:23:00.000Z', '2025-10-04T10:23:00.000Z'
),
(
  'hist2025_pay_a5f005b44cb1c405', 'treatment', '10th Payment', '10th Payment', 'hist2025_a1ab6455b6f93caa', '9296362956', 'Kishanganj', 'TARAFUL KHATOON',
  '2025-10-11', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:24:00.000Z', '2025-10-11T10:24:00.000Z'
),
(
  'hist2025_pay_d03a0e82cc74a52e', 'treatment', '11th Payment', '11th Payment', 'hist2025_a1ab6455b6f93caa', '9296362956', 'Kishanganj', 'TARAFUL KHATOON',
  '2025-11-07', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-07T10:25:00.000Z', '2025-11-07T10:25:00.000Z'
),
(
  'hist2025_pay_0fe56e1ea80f8365', 'treatment', 'Advance', 'Advance', 'hist2025_51de98e074702388', '9973609230', 'Kishanganj', 'NUSRAT JAHA',
  '2025-08-21', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-21T10:15:00.000Z', '2025-08-21T10:15:00.000Z'
),
(
  'hist2025_pay_f55cf72ffb295567', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_51de98e074702388', '9973609230', 'Kishanganj', 'NUSRAT JAHA',
  '2025-08-23', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:16:00.000Z', '2025-08-23T10:16:00.000Z'
),
(
  'hist2025_pay_c245a717c272b209', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_51de98e074702388', '9973609230', 'Kishanganj', 'NUSRAT JAHA',
  '2025-08-25', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-25T10:17:00.000Z', '2025-08-25T10:17:00.000Z'
),
(
  'hist2025_pay_9e7c0abd2bd8f6b8', 'treatment', '4th Payment', '4th Payment', 'hist2025_51de98e074702388', '9973609230', 'Kishanganj', 'NUSRAT JAHA',
  '2025-08-27', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-27T10:18:00.000Z', '2025-08-27T10:18:00.000Z'
),
(
  'hist2025_pay_4a3d6c7f3e3b8bef', 'treatment', '5th Payment', '5th Payment', 'hist2025_51de98e074702388', '9973609230', 'Kishanganj', 'NUSRAT JAHA',
  '2025-08-29', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-29T10:19:00.000Z', '2025-08-29T10:19:00.000Z'
),
(
  'hist2025_pay_a1d2e96ced4fd348', 'treatment', '6th Payment', '6th Payment', 'hist2025_51de98e074702388', '9973609230', 'Kishanganj', 'NUSRAT JAHA',
  '2025-09-03', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-03T10:20:00.000Z', '2025-09-03T10:20:00.000Z'
),
(
  'hist2025_pay_42b5091e2df1e997', 'treatment', '7th Payment', '7th Payment', 'hist2025_51de98e074702388', '9973609230', 'Kishanganj', 'NUSRAT JAHA',
  '2025-09-06', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:21:00.000Z', '2025-09-06T10:21:00.000Z'
),
(
  'hist2025_pay_db23142fdfa1cdfc', 'treatment', '8th Payment', '8th Payment', 'hist2025_51de98e074702388', '9973609230', 'Kishanganj', 'NUSRAT JAHA',
  '2025-09-10', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-10T10:22:00.000Z', '2025-09-10T10:22:00.000Z'
),
(
  'hist2025_pay_6c8f7d9df64d55e6', 'treatment', '9th Payment', '9th Payment', 'hist2025_51de98e074702388', '9973609230', 'Kishanganj', 'NUSRAT JAHA',
  '2025-09-15', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-15T10:23:00.000Z', '2025-09-15T10:23:00.000Z'
),
(
  'hist2025_pay_51720c46e296ef2d', 'treatment', '10th Payment', '10th Payment', 'hist2025_51de98e074702388', '9973609230', 'Kishanganj', 'NUSRAT JAHA',
  '2025-09-20', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:24:00.000Z', '2025-09-20T10:24:00.000Z'
),
(
  'hist2025_pay_831058c44bbf4141', 'treatment', '11th Payment', '11th Payment', 'hist2025_51de98e074702388', '9973609230', 'Kishanganj', 'NUSRAT JAHA',
  '2025-09-27', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:25:00.000Z', '2025-09-27T10:25:00.000Z'
),
(
  'hist2025_pay_8e638b6e8d5effb1', 'treatment', '12th Payment', '12th Payment', 'hist2025_51de98e074702388', '9973609230', 'Kishanganj', 'NUSRAT JAHA',
  '2025-10-11', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:26:00.000Z', '2025-10-11T10:26:00.000Z'
);
