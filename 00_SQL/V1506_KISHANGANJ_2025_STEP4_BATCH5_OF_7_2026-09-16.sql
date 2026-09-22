-- ২০২৫ শিট ধাপ ৪ -- ব্যাচ 5/7 (রোগী 65-81, মোট 17)
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
  'hist2025_12561fcb3692e9f1', 'KNE-27082025-001', '2025-08-27', '2025-08-27', '2025-08-27',
  'KALACHAN SARKAR', '7872821364', '', 'Kishanganj', '53', 'Male',
  'HASAN, HASAN, DALKHOLA, U.D', 'Piles', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-27T10:00:00.000Z', '2025-08-27T10:00:00.000Z'
),
(
  'hist2025_d6581689e9f2d7eb', 'KNE-08092025-001', '2025-09-08', '2025-09-08', '2025-09-08',
  'GANESH MONDAL', '8918318739', '', 'Kishanganj', '40', 'Male',
  'KANKI, MAJLESHPUR, CHAKULIYA, U.D', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-08T10:00:00.000Z', '2025-09-08T10:00:00.000Z'
),
(
  'hist2025_d81645626033f40d', 'KNE-08092025-002', '2025-09-08', '2025-09-08', '2025-09-08',
  'MD BABUL', '7991127657', '', 'Kishanganj', '40', 'Male',
  'BARIYA MALA BASTI, BARIYA, PAHARKHATTA, KISHANGANJ', 'Fistula', '115000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-08T10:00:00.000Z', '2025-09-08T10:00:00.000Z'
),
(
  'hist2025_deaff4151b929790', 'KNE-10092025-001', '2025-09-10', '2025-09-10', '2025-09-10',
  'MD SARWAR', '8882020975', '', 'Kishanganj', '36', 'Male',
  'MAUJABARI, BAGALBARI, KOCHADHAMAN, KISHANGANJ', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-10T10:00:00.000Z', '2025-09-10T10:00:00.000Z'
),
(
  'hist2025_4db832765a855869', 'KNE-13092025-001', '2025-09-13', '2025-09-13', '2025-09-13',
  'MD SHAKIL', '8292098117', '', 'Kishanganj', '34', 'Male',
  'KHAGRA, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '36000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:00:00.000Z', '2025-09-13T10:00:00.000Z'
),
(
  'hist2025_8a64ff6dc6622e44', 'KNE-13092025-002', '2025-09-13', '2025-09-13', '2025-09-13',
  'NABAB ALAM', '7033231463', '', 'Kishanganj', '25', 'Male',
  'KHAGRA, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '50000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:00:00.000Z', '2025-09-13T10:00:00.000Z'
),
(
  'hist2025_1fa2487956047d1b', 'KNE-15092025-001', '2025-09-15', '2025-09-15', '2025-09-15',
  'NAFIZ ALAM', '7004460126', '', 'Kishanganj', '35', 'Male',
  'KISHANGANJ, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Gupt Rog', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-15T10:00:00.000Z', '2025-09-15T10:00:00.000Z'
),
(
  'hist2025_da0723f768c4d04b', 'KNE-18092025-001', '2025-09-18', '2025-09-18', '2025-09-18',
  'ARIF ALAM', '7888985454', '', 'Kishanganj', '25', 'Male',
  'KALASINGHIA, RAIPUR, PAHARKATTA, KISHANGANJ', 'Piles', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-18T10:00:00.000Z', '2025-09-18T10:00:00.000Z'
),
(
  'hist2025_958010866582efc7', 'KNE-18092025-002', '2025-09-18', '2025-09-18', '2025-09-18',
  'ROJINA KHATOON', '8016390092', '', 'Kishanganj', '18', 'Female',
  'SAMASTPUR, VIDYANANDPUR, CHAKULIA, UTTAR DINAJPUR', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-18T10:00:00.000Z', '2025-09-18T10:00:00.000Z'
),
(
  'hist2025_9edca24954aa2c17', 'KNE-20092025-001', '2025-09-20', '2025-09-20', '2025-09-20',
  'ATAUR RAHMAN', '8084528864', '', 'Kishanganj', '50', 'Male',
  'MASTANCHOWK, BAGALBARI, KOCHADHAMAN, KISHANGANJ', 'Piles', '45000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:00:00.000Z', '2025-09-20T10:00:00.000Z'
),
(
  'hist2025_ea463d34166bf6dd', 'KNE-24092025-001', '2025-09-24', '2025-09-24', '2025-09-24',
  'SHABNAM', '6297654766', '', 'Kishanganj', '26', 'Male',
  'BANBARI, SINGIA, KISHANGANJ, KISHANGANJ', 'Other', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-24T10:00:00.000Z', '2025-09-24T10:00:00.000Z'
),
(
  'hist2025_0a74354ebe510e10', 'KNE-24092025-002', '2025-09-24', '2025-09-24', '2025-09-24',
  'BABUL ALAM', '9264215216', '', 'Kishanganj', '35', 'Male',
  'BAGALBARI, BAGALBARI, KOCHADHAMAN, KISHANGANJ', 'Piles', '350000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-24T10:00:00.000Z', '2025-09-24T10:00:00.000Z'
),
(
  'hist2025_4dd959f33fd953fe', 'KNE-24092025-003', '2025-09-24', '2025-09-24', '2025-09-24',
  'WASIM AKHTER', '7063087735', '', 'Kishanganj', '30', 'Female',
  'GOALPOKHAR, BARBILLA, GOALPOKHAR, UTTAR DINAJPUR', 'Gupt Rog', '28000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-24T10:00:00.000Z', '2025-09-24T10:00:00.000Z'
),
(
  'hist2025_4afc2ef352904522', 'KNE-26092025-001', '2025-09-26', '2025-09-26', '2025-09-26',
  'MANJUR ALAM', '8670764872', '', 'Kishanganj', '39', 'Male',
  'NICHIT PUR, DALKHOLA, DALKHOLA, UD', 'Piles', '21000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-26T10:00:00.000Z', '2025-09-26T10:00:00.000Z'
),
(
  'hist2025_40b3c22e7ed2dcd3', 'KNE-26092025-002', '2025-09-26', '2025-09-26', '2025-09-26',
  'MD ASLAM', '9528607637', '', 'Kishanganj', '30', 'Male',
  'KALASINGIA, RAIPUR, PAHARKATTA, KISHANGANJ', 'Fissure', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-26T10:00:00.000Z', '2025-09-26T10:00:00.000Z'
),
(
  'hist2025_affb93bed7ec295b', 'KNE-27092025-001', '2025-09-27', '2025-09-27', '2025-09-27',
  'NASHIBA KHATOON', '8509744421', '', 'Kishanganj', '40', 'Female',
  'DAULA CHOWK, DAULA CHOWK, KISHANGANJ, KISHANGANJ', 'Piles', '37000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:00:00.000Z', '2025-09-27T10:00:00.000Z'
),
(
  'hist2025_95df6521767ba110', 'KNE-04102025-001', '2025-10-04', '2025-10-04', '2025-10-04',
  'SAMIM AKHTER', '6207143684', '', 'Kishanganj', '32', 'Female',
  'RAIPUR, RAIPUR, ARRABARI, KISHANGANJ', 'Piles', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:00:00.000Z', '2025-10-04T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'hist2025_pay_d1fad217c1902e5b', 'treatment', 'Advance', 'Advance', 'hist2025_12561fcb3692e9f1', '7872821364', 'Kishanganj', 'KALACHAN SARKAR',
  '2025-08-27', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-27T10:15:00.000Z', '2025-08-27T10:15:00.000Z'
),
(
  'hist2025_pay_870f85816847454b', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_12561fcb3692e9f1', '7872821364', 'Kishanganj', 'KALACHAN SARKAR',
  '2025-08-30', '12000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:16:00.000Z', '2025-08-30T10:16:00.000Z'
),
(
  'hist2025_pay_b81f0de34dd0dcbc', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_12561fcb3692e9f1', '7872821364', 'Kishanganj', 'KALACHAN SARKAR',
  '2025-09-06', '12000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:17:00.000Z', '2025-09-06T10:17:00.000Z'
),
(
  'hist2025_pay_03dce4a1bba79b73', 'treatment', '4th Payment', '4th Payment', 'hist2025_12561fcb3692e9f1', '7872821364', 'Kishanganj', 'KALACHAN SARKAR',
  '2025-09-13', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:18:00.000Z', '2025-09-13T10:18:00.000Z'
),
(
  'hist2025_pay_f4c0aefcd821a0b1', 'treatment', '5th Payment', '5th Payment', 'hist2025_12561fcb3692e9f1', '7872821364', 'Kishanganj', 'KALACHAN SARKAR',
  '2025-09-17', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-17T10:19:00.000Z', '2025-09-17T10:19:00.000Z'
),
(
  'hist2025_pay_671a2f4cc5a06aee', 'treatment', '6th Payment', '6th Payment', 'hist2025_12561fcb3692e9f1', '7872821364', 'Kishanganj', 'KALACHAN SARKAR',
  '2025-09-24', '8000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-24T10:20:00.000Z', '2025-09-24T10:20:00.000Z'
),
(
  'hist2025_pay_2e2ff89bd996272b', 'treatment', 'Advance', 'Advance', 'hist2025_d6581689e9f2d7eb', '8918318739', 'Kishanganj', 'GANESH MONDAL',
  '2025-09-08', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-08T10:15:00.000Z', '2025-09-08T10:15:00.000Z'
),
(
  'hist2025_pay_02c78b60d960f3bc', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_d6581689e9f2d7eb', '8918318739', 'Kishanganj', 'GANESH MONDAL',
  '2025-09-10', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-10T10:16:00.000Z', '2025-09-10T10:16:00.000Z'
),
(
  'hist2025_pay_5f25d04ef953d984', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_d6581689e9f2d7eb', '8918318739', 'Kishanganj', 'GANESH MONDAL',
  '2025-09-13', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:17:00.000Z', '2025-09-13T10:17:00.000Z'
),
(
  'hist2025_pay_027b603b92de7cab', 'treatment', '4th Payment', '4th Payment', 'hist2025_d6581689e9f2d7eb', '8918318739', 'Kishanganj', 'GANESH MONDAL',
  '2025-09-17', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-17T10:18:00.000Z', '2025-09-17T10:18:00.000Z'
),
(
  'hist2025_pay_e01b5211c419e7d4', 'treatment', '5th Payment', '5th Payment', 'hist2025_d6581689e9f2d7eb', '8918318739', 'Kishanganj', 'GANESH MONDAL',
  '2025-09-20', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:19:00.000Z', '2025-09-20T10:19:00.000Z'
),
(
  'hist2025_pay_bac4484b263910a9', 'treatment', '6th Payment', '6th Payment', 'hist2025_d6581689e9f2d7eb', '8918318739', 'Kishanganj', 'GANESH MONDAL',
  '2025-09-24', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-24T10:20:00.000Z', '2025-09-24T10:20:00.000Z'
),
(
  'hist2025_pay_c248731ab14b06df', 'treatment', '7th Payment', '7th Payment', 'hist2025_d6581689e9f2d7eb', '8918318739', 'Kishanganj', 'GANESH MONDAL',
  '2025-09-27', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:21:00.000Z', '2025-09-27T10:21:00.000Z'
),
(
  'hist2025_pay_080eb028bdc8a38b', 'treatment', '8th Payment', '8th Payment', 'hist2025_d6581689e9f2d7eb', '8918318739', 'Kishanganj', 'GANESH MONDAL',
  '2025-09-04', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-04T10:22:00.000Z', '2025-09-04T10:22:00.000Z'
),
(
  'hist2025_pay_2eb4aac5f6187f0f', 'treatment', '9th Payment', '9th Payment', 'hist2025_d6581689e9f2d7eb', '8918318739', 'Kishanganj', 'GANESH MONDAL',
  '2025-10-08', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-08T10:23:00.000Z', '2025-10-08T10:23:00.000Z'
),
(
  'hist2025_pay_e8f8faa239f1811f', 'treatment', '10th Payment', '10th Payment', 'hist2025_d6581689e9f2d7eb', '8918318739', 'Kishanganj', 'GANESH MONDAL',
  '2025-10-18', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:24:00.000Z', '2025-10-18T10:24:00.000Z'
),
(
  'hist2025_pay_94df0525aa1e352f', 'treatment', '11th Payment', '11th Payment', 'hist2025_d6581689e9f2d7eb', '8918318739', 'Kishanganj', 'GANESH MONDAL',
  '2025-10-22', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-22T10:25:00.000Z', '2025-10-22T10:25:00.000Z'
),
(
  'hist2025_pay_c51cc1b085d53726', 'treatment', '12th Payment', '12th Payment', 'hist2025_d6581689e9f2d7eb', '8918318739', 'Kishanganj', 'GANESH MONDAL',
  '2025-10-29', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-29T10:26:00.000Z', '2025-10-29T10:26:00.000Z'
),
(
  'hist2025_pay_62c52da360a1eed6', 'treatment', '13th Payment', '13th Payment', 'hist2025_d6581689e9f2d7eb', '8918318739', 'Kishanganj', 'GANESH MONDAL',
  '2025-11-05', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-05T10:27:00.000Z', '2025-11-05T10:27:00.000Z'
),
(
  'hist2025_pay_36ba7a6e54eed929', 'treatment', '14th Payment', '14th Payment', 'hist2025_d6581689e9f2d7eb', '8918318739', 'Kishanganj', 'GANESH MONDAL',
  '2025-11-22', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-22T10:28:00.000Z', '2025-11-22T10:28:00.000Z'
),
(
  'hist2025_pay_ae6948af18cf5f41', 'treatment', '15th Payment', '15th Payment', 'hist2025_d6581689e9f2d7eb', '8918318739', 'Kishanganj', 'GANESH MONDAL',
  '2025-11-29', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-29T10:29:00.000Z', '2025-11-29T10:29:00.000Z'
),
(
  'hist2025_pay_f166603a58622551', 'treatment', 'Advance', 'Advance', 'hist2025_d81645626033f40d', '7991127657', 'Kishanganj', 'MD BABUL',
  '2025-09-17', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-17T10:15:00.000Z', '2025-09-17T10:15:00.000Z'
),
(
  'hist2025_pay_8a2729bf1bbd1c99', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_d81645626033f40d', '7991127657', 'Kishanganj', 'MD BABUL',
  '2025-09-20', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:16:00.000Z', '2025-09-20T10:16:00.000Z'
),
(
  'hist2025_pay_abac8bc50beb421c', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_d81645626033f40d', '7991127657', 'Kishanganj', 'MD BABUL',
  '2025-09-24', '50000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-24T10:17:00.000Z', '2025-09-24T10:17:00.000Z'
),
(
  'hist2025_pay_12f00dade3261b66', 'treatment', '4th Payment', '4th Payment', 'hist2025_d81645626033f40d', '7991127657', 'Kishanganj', 'MD BABUL',
  '2025-09-29', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-29T10:18:00.000Z', '2025-09-29T10:18:00.000Z'
),
(
  'hist2025_pay_a4e601318bdd7743', 'treatment', '5th Payment', '5th Payment', 'hist2025_d81645626033f40d', '7991127657', 'Kishanganj', 'MD BABUL',
  '2025-10-04', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:19:00.000Z', '2025-10-04T10:19:00.000Z'
),
(
  'hist2025_pay_42e5cf2e3df05089', 'treatment', '6th Payment', '6th Payment', 'hist2025_d81645626033f40d', '7991127657', 'Kishanganj', 'MD BABUL',
  '2025-10-11', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:20:00.000Z', '2025-10-11T10:20:00.000Z'
),
(
  'hist2025_pay_69708267c0db50ff', 'treatment', '7th Payment', '7th Payment', 'hist2025_d81645626033f40d', '7991127657', 'Kishanganj', 'MD BABUL',
  '2025-10-18', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:21:00.000Z', '2025-10-18T10:21:00.000Z'
),
(
  'hist2025_pay_f29122468adef772', 'treatment', '8th Payment', '8th Payment', 'hist2025_d81645626033f40d', '7991127657', 'Kishanganj', 'MD BABUL',
  '2025-10-25', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-25T10:22:00.000Z', '2025-10-25T10:22:00.000Z'
),
(
  'hist2025_pay_fb25f35f349e28b9', 'treatment', '9th Payment', '9th Payment', 'hist2025_d81645626033f40d', '7991127657', 'Kishanganj', 'MD BABUL',
  '2025-10-05', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-05T10:23:00.000Z', '2025-10-05T10:23:00.000Z'
),
(
  'hist2025_pay_aa2d40a954e98a96', 'treatment', '10th Payment', '10th Payment', 'hist2025_d81645626033f40d', '7991127657', 'Kishanganj', 'MD BABUL',
  '2025-11-29', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-29T10:24:00.000Z', '2025-11-29T10:24:00.000Z'
),
(
  'hist2025_pay_869c06cd18b08f7a', 'treatment', '11th Payment', '11th Payment', 'hist2025_d81645626033f40d', '7991127657', 'Kishanganj', 'MD BABUL',
  '2025-12-13', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:25:00.000Z', '2025-12-13T10:25:00.000Z'
),
(
  'hist2025_pay_7ae2857b05fd37bd', 'treatment', 'Advance', 'Advance', 'hist2025_deaff4151b929790', '8882020975', 'Kishanganj', 'MD SARWAR',
  '2025-09-11', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-11T10:15:00.000Z', '2025-09-11T10:15:00.000Z'
),
(
  'hist2025_pay_0e54d9e33de4adf0', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_deaff4151b929790', '8882020975', 'Kishanganj', 'MD SARWAR',
  '2025-09-13', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:16:00.000Z', '2025-09-13T10:16:00.000Z'
),
(
  'hist2025_pay_255b3c1bf391d5ac', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_deaff4151b929790', '8882020975', 'Kishanganj', 'MD SARWAR',
  '2025-09-17', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-17T10:17:00.000Z', '2025-09-17T10:17:00.000Z'
),
(
  'hist2025_pay_11f025e31e73d21d', 'treatment', '4th Payment', '4th Payment', 'hist2025_deaff4151b929790', '8882020975', 'Kishanganj', 'MD SARWAR',
  '2025-09-24', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-24T10:18:00.000Z', '2025-09-24T10:18:00.000Z'
),
(
  'hist2025_pay_a28dbb3afe729994', 'treatment', '5th Payment', '5th Payment', 'hist2025_deaff4151b929790', '8882020975', 'Kishanganj', 'MD SARWAR',
  '2025-10-15', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-15T10:19:00.000Z', '2025-10-15T10:19:00.000Z'
),
(
  'hist2025_pay_5de626d227f70c49', 'treatment', '6th Payment', '6th Payment', 'hist2025_deaff4151b929790', '8882020975', 'Kishanganj', 'MD SARWAR',
  '2025-10-22', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-22T10:20:00.000Z', '2025-10-22T10:20:00.000Z'
),
(
  'hist2025_pay_55bc5aa8b67fdffd', 'treatment', '7th Payment', '7th Payment', 'hist2025_deaff4151b929790', '8882020975', 'Kishanganj', 'MD SARWAR',
  '2025-12-04', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-04T10:21:00.000Z', '2025-12-04T10:21:00.000Z'
),
(
  'hist2025_pay_976c14c36a9f2cf9', 'treatment', '8th Payment', '8th Payment', 'hist2025_deaff4151b929790', '8882020975', 'Kishanganj', 'MD SARWAR',
  '2025-12-13', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:22:00.000Z', '2025-12-13T10:22:00.000Z'
),
(
  'hist2025_pay_d16e1c6318747889', 'treatment', 'Advance', 'Advance', 'hist2025_4db832765a855869', '8292098117', 'Kishanganj', 'MD SHAKIL',
  '2025-09-13', '2500', 'CASH', 'Historical import (2025 Google Sheet) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:15:00.000Z', '2025-09-13T10:15:00.000Z'
),
(
  'hist2025_pay_77384b01f22e5b24', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_4db832765a855869', '8292098117', 'Kishanganj', 'MD SHAKIL',
  '2025-09-13', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:16:00.000Z', '2025-09-13T10:16:00.000Z'
),
(
  'hist2025_pay_35f9cdca99c055f8', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_4db832765a855869', '8292098117', 'Kishanganj', 'MD SHAKIL',
  '2025-09-17', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-17T10:17:00.000Z', '2025-09-17T10:17:00.000Z'
),
(
  'hist2025_pay_26d7dcbc9fb50464', 'treatment', '4th Payment', '4th Payment', 'hist2025_4db832765a855869', '8292098117', 'Kishanganj', 'MD SHAKIL',
  '2025-09-20', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:18:00.000Z', '2025-09-20T10:18:00.000Z'
),
(
  'hist2025_pay_032c51ae7b843cbb', 'treatment', '5th Payment', '5th Payment', 'hist2025_4db832765a855869', '8292098117', 'Kishanganj', 'MD SHAKIL',
  '2025-09-27', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:19:00.000Z', '2025-09-27T10:19:00.000Z'
),
(
  'hist2025_pay_7e00bb64bda73aed', 'treatment', '6th Payment', '6th Payment', 'hist2025_4db832765a855869', '8292098117', 'Kishanganj', 'MD SHAKIL',
  '2025-10-08', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-08T10:20:00.000Z', '2025-10-08T10:20:00.000Z'
),
(
  'hist2025_pay_74f03c10ab52e85a', 'treatment', '7th Payment', '7th Payment', 'hist2025_4db832765a855869', '8292098117', 'Kishanganj', 'MD SHAKIL',
  '2025-10-15', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-15T10:21:00.000Z', '2025-10-15T10:21:00.000Z'
),
(
  'hist2025_pay_aa69b20d54f6fa5d', 'treatment', '8th Payment', '8th Payment', 'hist2025_4db832765a855869', '8292098117', 'Kishanganj', 'MD SHAKIL',
  '2025-12-13', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:22:00.000Z', '2025-12-13T10:22:00.000Z'
),
(
  'hist2025_pay_151d90ced60410bf', 'treatment', 'Advance', 'Advance', 'hist2025_8a64ff6dc6622e44', '7033231463', 'Kishanganj', 'NABAB ALAM',
  '2025-09-13', '800', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:15:00.000Z', '2025-09-13T10:15:00.000Z'
),
(
  'hist2025_pay_978498ac4eadec88', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_8a64ff6dc6622e44', '7033231463', 'Kishanganj', 'NABAB ALAM',
  '2025-09-17', '20000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-17T10:16:00.000Z', '2025-09-17T10:16:00.000Z'
),
(
  'hist2025_pay_ff026e697d2cbfc1', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_8a64ff6dc6622e44', '7033231463', 'Kishanganj', 'NABAB ALAM',
  '2025-11-15', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:17:00.000Z', '2025-11-15T10:17:00.000Z'
),
(
  'hist2025_pay_b16d0d258270ea06', 'treatment', '4th Payment', '4th Payment', 'hist2025_8a64ff6dc6622e44', '7033231463', 'Kishanganj', 'NABAB ALAM',
  '2025-12-13', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:18:00.000Z', '2025-12-13T10:18:00.000Z'
),
(
  'hist2025_pay_41d3e4b6d7d4d6c3', 'treatment', 'Advance', 'Advance', 'hist2025_1fa2487956047d1b', '7004460126', 'Kishanganj', 'NAFIZ ALAM',
  '2025-09-18', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-18T10:15:00.000Z', '2025-09-18T10:15:00.000Z'
),
(
  'hist2025_pay_be53728a7fc70c52', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_1fa2487956047d1b', '7004460126', 'Kishanganj', 'NAFIZ ALAM',
  '2025-09-19', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-19T10:16:00.000Z', '2025-09-19T10:16:00.000Z'
),
(
  'hist2025_pay_69c3f174750782b1', 'treatment', 'Advance', 'Advance', 'hist2025_da0723f768c4d04b', '7888985454', 'Kishanganj', 'ARIF ALAM',
  '2025-09-18', '3000', 'CASH', 'Historical import (2025 Google Sheet) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-18T10:15:00.000Z', '2025-09-18T10:15:00.000Z'
),
(
  'hist2025_pay_0742611cbfade739', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_da0723f768c4d04b', '7888985454', 'Kishanganj', 'ARIF ALAM',
  '2025-09-18', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-18T10:16:00.000Z', '2025-09-18T10:16:00.000Z'
),
(
  'hist2025_pay_d650c66b00956c96', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_da0723f768c4d04b', '7888985454', 'Kishanganj', 'ARIF ALAM',
  '2025-09-20', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:17:00.000Z', '2025-09-20T10:17:00.000Z'
),
(
  'hist2025_pay_223ea3ff258a4961', 'treatment', '4th Payment', '4th Payment', 'hist2025_da0723f768c4d04b', '7888985454', 'Kishanganj', 'ARIF ALAM',
  '2025-09-25', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-25T10:18:00.000Z', '2025-09-25T10:18:00.000Z'
),
(
  'hist2025_pay_7285afcf46fa7cb2', 'treatment', '5th Payment', '5th Payment', 'hist2025_da0723f768c4d04b', '7888985454', 'Kishanganj', 'ARIF ALAM',
  '2025-09-29', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-29T10:19:00.000Z', '2025-09-29T10:19:00.000Z'
),
(
  'hist2025_pay_a9c9a5adcfed267e', 'treatment', '6th Payment', '6th Payment', 'hist2025_da0723f768c4d04b', '7888985454', 'Kishanganj', 'ARIF ALAM',
  '2025-10-04', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:20:00.000Z', '2025-10-04T10:20:00.000Z'
),
(
  'hist2025_pay_3aa68afbf30bc639', 'treatment', '7th Payment', '7th Payment', 'hist2025_da0723f768c4d04b', '7888985454', 'Kishanganj', 'ARIF ALAM',
  '2025-10-11', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:21:00.000Z', '2025-10-11T10:21:00.000Z'
),
(
  'hist2025_pay_ff48cdc2a249a8d6', 'treatment', '8th Payment', '8th Payment', 'hist2025_da0723f768c4d04b', '7888985454', 'Kishanganj', 'ARIF ALAM',
  '2025-10-22', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-22T10:22:00.000Z', '2025-10-22T10:22:00.000Z'
),
(
  'hist2025_pay_f53b42bb7f04b81f', 'treatment', 'Advance', 'Advance', 'hist2025_958010866582efc7', '8016390092', 'Kishanganj', 'ROJINA KHATOON',
  '2025-09-18', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-18T10:15:00.000Z', '2025-09-18T10:15:00.000Z'
),
(
  'hist2025_pay_fb637bb8b3835595', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_958010866582efc7', '8016390092', 'Kishanganj', 'ROJINA KHATOON',
  '2025-09-24', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-24T10:16:00.000Z', '2025-09-24T10:16:00.000Z'
),
(
  'hist2025_pay_60afcb67dcb5971f', 'treatment', 'Advance', 'Advance', 'hist2025_9edca24954aa2c17', '8084528864', 'Kishanganj', 'ATAUR RAHMAN',
  '2025-09-20', '1800', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:15:00.000Z', '2025-09-20T10:15:00.000Z'
),
(
  'hist2025_pay_b02321bfe129a5dc', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_9edca24954aa2c17', '8084528864', 'Kishanganj', 'ATAUR RAHMAN',
  '2025-09-27', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:16:00.000Z', '2025-09-27T10:16:00.000Z'
),
(
  'hist2025_pay_932de19e27e221c0', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_9edca24954aa2c17', '8084528864', 'Kishanganj', 'ATAUR RAHMAN',
  '2025-10-04', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:17:00.000Z', '2025-10-04T10:17:00.000Z'
),
(
  'hist2025_pay_491af333c955ffcf', 'treatment', '4th Payment', '4th Payment', 'hist2025_9edca24954aa2c17', '8084528864', 'Kishanganj', 'ATAUR RAHMAN',
  '2025-10-14', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-14T10:18:00.000Z', '2025-10-14T10:18:00.000Z'
),
(
  'hist2025_pay_0552e4c23f2b2920', 'treatment', 'Advance', 'Advance', 'hist2025_ea463d34166bf6dd', '6297654766', 'Kishanganj', 'SHABNAM',
  '2025-09-24', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-24T10:15:00.000Z', '2025-09-24T10:15:00.000Z'
),
(
  'hist2025_pay_a71cd0fd1d2c7f2f', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_ea463d34166bf6dd', '6297654766', 'Kishanganj', 'SHABNAM',
  '2025-09-27', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:16:00.000Z', '2025-09-27T10:16:00.000Z'
),
(
  'hist2025_pay_b5041521a807a6df', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_ea463d34166bf6dd', '6297654766', 'Kishanganj', 'SHABNAM',
  '2025-10-04', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:17:00.000Z', '2025-10-04T10:17:00.000Z'
),
(
  'hist2025_pay_63b2ca848f367ee0', 'treatment', '4th Payment', '4th Payment', 'hist2025_ea463d34166bf6dd', '6297654766', 'Kishanganj', 'SHABNAM',
  '2025-10-11', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:18:00.000Z', '2025-10-11T10:18:00.000Z'
),
(
  'hist2025_pay_0e3dea0c99aeaff9', 'treatment', '5th Payment', '5th Payment', 'hist2025_ea463d34166bf6dd', '6297654766', 'Kishanganj', 'SHABNAM',
  '2025-12-20', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-20T10:19:00.000Z', '2025-12-20T10:19:00.000Z'
),
(
  'hist2025_pay_446787f18bc86064', 'treatment', '6th Payment', '6th Payment', 'hist2025_ea463d34166bf6dd', '6297654766', 'Kishanganj', 'SHABNAM',
  '2025-12-24', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-24T10:20:00.000Z', '2025-12-24T10:20:00.000Z'
),
(
  'hist2025_pay_f1ba5caadfa7caf2', 'treatment', 'Advance', 'Advance', 'hist2025_0a74354ebe510e10', '9264215216', 'Kishanganj', 'BABUL ALAM',
  '2025-09-24', '25000', 'CASH', 'Historical import (2025 Google Sheet) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-24T10:15:00.000Z', '2025-09-24T10:15:00.000Z'
),
(
  'hist2025_pay_a088f34b70d8233e', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_0a74354ebe510e10', '9264215216', 'Kishanganj', 'BABUL ALAM',
  '2025-09-24', '25000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-24T10:16:00.000Z', '2025-09-24T10:16:00.000Z'
),
(
  'hist2025_pay_a6fd4a3ea2e8e194', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_0a74354ebe510e10', '9264215216', 'Kishanganj', 'BABUL ALAM',
  '2025-10-04', '50000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:17:00.000Z', '2025-10-04T10:17:00.000Z'
),
(
  'hist2025_pay_1847324f76ee1dd0', 'treatment', '4th Payment', '4th Payment', 'hist2025_0a74354ebe510e10', '9264215216', 'Kishanganj', 'BABUL ALAM',
  '2025-10-08', '50000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-08T10:18:00.000Z', '2025-10-08T10:18:00.000Z'
),
(
  'hist2025_pay_24eefd666140974a', 'treatment', '5th Payment', '5th Payment', 'hist2025_0a74354ebe510e10', '9264215216', 'Kishanganj', 'BABUL ALAM',
  '2025-10-18', '20000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:19:00.000Z', '2025-10-18T10:19:00.000Z'
),
(
  'hist2025_pay_6df79eb284142bbb', 'treatment', '6th Payment', '6th Payment', 'hist2025_0a74354ebe510e10', '9264215216', 'Kishanganj', 'BABUL ALAM',
  '2025-10-29', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-29T10:20:00.000Z', '2025-10-29T10:20:00.000Z'
),
(
  'hist2025_pay_8a75e02190b24294', 'treatment', '7th Payment', '7th Payment', 'hist2025_0a74354ebe510e10', '9264215216', 'Kishanganj', 'BABUL ALAM',
  '2025-11-08', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:21:00.000Z', '2025-11-08T10:21:00.000Z'
),
(
  'hist2025_pay_68618885c1cb7503', 'treatment', '8th Payment', '8th Payment', 'hist2025_0a74354ebe510e10', '9264215216', 'Kishanganj', 'BABUL ALAM',
  '2025-11-26', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-26T10:22:00.000Z', '2025-11-26T10:22:00.000Z'
),
(
  'hist2025_pay_cd6e1ecfa15de9b9', 'treatment', '9th Payment', '9th Payment', 'hist2025_0a74354ebe510e10', '9264215216', 'Kishanganj', 'BABUL ALAM',
  '2025-12-01', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-01T10:23:00.000Z', '2025-12-01T10:23:00.000Z'
),
(
  'hist2025_pay_7911fff8d3f86c52', 'treatment', '10th Payment', '10th Payment', 'hist2025_0a74354ebe510e10', '9264215216', 'Kishanganj', 'BABUL ALAM',
  '2025-12-03', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-03T10:24:00.000Z', '2025-12-03T10:24:00.000Z'
),
(
  'hist2025_pay_ba547f6a4bcf7ecd', 'treatment', 'Advance', 'Advance', 'hist2025_4dd959f33fd953fe', '7063087735', 'Kishanganj', 'WASIM AKHTER',
  '2025-09-24', '20000', 'CASH', 'Historical import (2025 Google Sheet) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-24T10:15:00.000Z', '2025-09-24T10:15:00.000Z'
),
(
  'hist2025_pay_9b83bbd6bcf17712', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_4dd959f33fd953fe', '7063087735', 'Kishanganj', 'WASIM AKHTER',
  '2025-09-24', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-24T10:16:00.000Z', '2025-09-24T10:16:00.000Z'
),
(
  'hist2025_pay_9c37ed88038673bf', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_4dd959f33fd953fe', '7063087735', 'Kishanganj', 'WASIM AKHTER',
  '2025-09-27', '20000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:17:00.000Z', '2025-09-27T10:17:00.000Z'
),
(
  'hist2025_pay_77e5a393c8562202', 'treatment', 'Advance', 'Advance', 'hist2025_4afc2ef352904522', '8670764872', 'Kishanganj', 'MANJUR ALAM',
  '2025-09-26', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-26T10:15:00.000Z', '2025-09-26T10:15:00.000Z'
),
(
  'hist2025_pay_41b67c52eb16f239', 'treatment', 'Advance', 'Advance', 'hist2025_40b3c22e7ed2dcd3', '9528607637', 'Kishanganj', 'MD ASLAM',
  '2025-10-06', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-06T10:15:00.000Z', '2025-10-06T10:15:00.000Z'
),
(
  'hist2025_pay_f43eff1d2bc8131c', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_40b3c22e7ed2dcd3', '9528607637', 'Kishanganj', 'MD ASLAM',
  '2025-10-13', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-13T10:16:00.000Z', '2025-10-13T10:16:00.000Z'
),
(
  'hist2025_pay_a4610b729e44c1b2', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_40b3c22e7ed2dcd3', '9528607637', 'Kishanganj', 'MD ASLAM',
  '2025-10-15', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-15T10:17:00.000Z', '2025-10-15T10:17:00.000Z'
),
(
  'hist2025_pay_2359dcae3a460d4c', 'treatment', '4th Payment', '4th Payment', 'hist2025_40b3c22e7ed2dcd3', '9528607637', 'Kishanganj', 'MD ASLAM',
  '2025-10-18', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:18:00.000Z', '2025-10-18T10:18:00.000Z'
),
(
  'hist2025_pay_39423e5748f2ec3b', 'treatment', '5th Payment', '5th Payment', 'hist2025_40b3c22e7ed2dcd3', '9528607637', 'Kishanganj', 'MD ASLAM',
  '2025-10-29', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-29T10:19:00.000Z', '2025-10-29T10:19:00.000Z'
),
(
  'hist2025_pay_e2f34c84dae6ebcd', 'treatment', '6th Payment', '6th Payment', 'hist2025_40b3c22e7ed2dcd3', '9528607637', 'Kishanganj', 'MD ASLAM',
  '2025-11-05', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-05T10:20:00.000Z', '2025-11-05T10:20:00.000Z'
),
(
  'hist2025_pay_f56e20a4c61d8c28', 'treatment', '7th Payment', '7th Payment', 'hist2025_40b3c22e7ed2dcd3', '9528607637', 'Kishanganj', 'MD ASLAM',
  '2025-11-19', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-19T10:21:00.000Z', '2025-11-19T10:21:00.000Z'
),
(
  'hist2025_pay_29d11dbb86baf893', 'treatment', 'Advance', 'Advance', 'hist2025_affb93bed7ec295b', '8509744421', 'Kishanganj', 'NASHIBA KHATOON',
  '2025-09-26', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-26T10:15:00.000Z', '2025-09-26T10:15:00.000Z'
),
(
  'hist2025_pay_149b626b37104e49', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_affb93bed7ec295b', '8509744421', 'Kishanganj', 'NASHIBA KHATOON',
  '2025-09-29', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-29T10:16:00.000Z', '2025-09-29T10:16:00.000Z'
),
(
  'hist2025_pay_c94b8896525416c9', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_affb93bed7ec295b', '8509744421', 'Kishanganj', 'NASHIBA KHATOON',
  '2025-10-08', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-08T10:17:00.000Z', '2025-10-08T10:17:00.000Z'
),
(
  'hist2025_pay_fb1652aacc11c759', 'treatment', '4th Payment', '4th Payment', 'hist2025_affb93bed7ec295b', '8509744421', 'Kishanganj', 'NASHIBA KHATOON',
  '2025-10-14', '4500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-14T10:18:00.000Z', '2025-10-14T10:18:00.000Z'
),
(
  'hist2025_pay_b941d5a9957d48a1', 'treatment', '5th Payment', '5th Payment', 'hist2025_affb93bed7ec295b', '8509744421', 'Kishanganj', 'NASHIBA KHATOON',
  '2025-10-27', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-27T10:19:00.000Z', '2025-10-27T10:19:00.000Z'
),
(
  'hist2025_pay_1ca1cd020d82d821', 'treatment', '6th Payment', '6th Payment', 'hist2025_affb93bed7ec295b', '8509744421', 'Kishanganj', 'NASHIBA KHATOON',
  '2025-10-29', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-29T10:20:00.000Z', '2025-10-29T10:20:00.000Z'
),
(
  'hist2025_pay_c6bd76e1135c438b', 'treatment', '7th Payment', '7th Payment', 'hist2025_affb93bed7ec295b', '8509744421', 'Kishanganj', 'NASHIBA KHATOON',
  '2025-11-01', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-01T10:21:00.000Z', '2025-11-01T10:21:00.000Z'
),
(
  'hist2025_pay_b5950c7258fc2cd5', 'treatment', '8th Payment', '8th Payment', 'hist2025_affb93bed7ec295b', '8509744421', 'Kishanganj', 'NASHIBA KHATOON',
  '2025-11-07', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-07T10:22:00.000Z', '2025-11-07T10:22:00.000Z'
),
(
  'hist2025_pay_ae0219795bc1a8c7', 'treatment', '9th Payment', '9th Payment', 'hist2025_affb93bed7ec295b', '8509744421', 'Kishanganj', 'NASHIBA KHATOON',
  '2025-11-12', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-12T10:23:00.000Z', '2025-11-12T10:23:00.000Z'
),
(
  'hist2025_pay_8537f090d7f085aa', 'treatment', '10th Payment', '10th Payment', 'hist2025_affb93bed7ec295b', '8509744421', 'Kishanganj', 'NASHIBA KHATOON',
  '2025-11-17', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-17T10:24:00.000Z', '2025-11-17T10:24:00.000Z'
),
(
  'hist2025_pay_25a47f618336672a', 'treatment', 'Advance', 'Advance', 'hist2025_95df6521767ba110', '6207143684', 'Kishanganj', 'SAMIM AKHTER',
  '2025-10-06', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-06T10:15:00.000Z', '2025-10-06T10:15:00.000Z'
),
(
  'hist2025_pay_76d4b34b676952f9', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_95df6521767ba110', '6207143684', 'Kishanganj', 'SAMIM AKHTER',
  '2025-10-15', '7000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-15T10:16:00.000Z', '2025-10-15T10:16:00.000Z'
),
(
  'hist2025_pay_a72cba209bee2479', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_95df6521767ba110', '6207143684', 'Kishanganj', 'SAMIM AKHTER',
  '2025-10-18', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:17:00.000Z', '2025-10-18T10:17:00.000Z'
),
(
  'hist2025_pay_4dbdc401ae632f05', 'treatment', '4th Payment', '4th Payment', 'hist2025_95df6521767ba110', '6207143684', 'Kishanganj', 'SAMIM AKHTER',
  '2025-10-25', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-25T10:18:00.000Z', '2025-10-25T10:18:00.000Z'
),
(
  'hist2025_pay_6ac52b651219b8df', 'treatment', '5th Payment', '5th Payment', 'hist2025_95df6521767ba110', '6207143684', 'Kishanganj', 'SAMIM AKHTER',
  '2025-11-01', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-01T10:19:00.000Z', '2025-11-01T10:19:00.000Z'
),
(
  'hist2025_pay_3b38a6733a5ba275', 'treatment', '6th Payment', '6th Payment', 'hist2025_95df6521767ba110', '6207143684', 'Kishanganj', 'SAMIM AKHTER',
  '2025-11-05', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-05T10:20:00.000Z', '2025-11-05T10:20:00.000Z'
),
(
  'hist2025_pay_a314e56a7a2b675a', 'treatment', '7th Payment', '7th Payment', 'hist2025_95df6521767ba110', '6207143684', 'Kishanganj', 'SAMIM AKHTER',
  '2025-11-15', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:21:00.000Z', '2025-11-15T10:21:00.000Z'
);
