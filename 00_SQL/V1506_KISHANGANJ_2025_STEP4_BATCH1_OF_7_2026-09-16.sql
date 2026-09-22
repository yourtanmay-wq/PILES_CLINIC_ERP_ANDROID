-- ২০২৫ শিট ধাপ ৪ -- ব্যাচ 1/7 (রোগী 1-14, মোট 14)
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
  'hist2025_47026da8fe5be4a7', 'KNE-06012025-001', '2025-01-06', '2025-01-06', '2025-01-06',
  'PARESH MANDAL', '9126495421', '', 'Kishanganj', '51', 'Male',
  'KANKI, MAJLISHPUR, CHAKULIYA, U.D', 'Fistula', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-06T10:00:00.000Z', '2025-01-06T10:00:00.000Z'
),
(
  'hist2025_0dcd595e07cc56eb', 'KNE-06012025-002', '2025-01-06', '2025-01-06', '2025-01-06',
  'NASIMA KHATUN', '9064332124', '', 'Kishanganj', '30', 'Female',
  'CHAPRA, CHAPRA, CHAKULIYA, U.D', 'Piles', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-06T10:00:00.000Z', '2025-01-06T10:00:00.000Z'
),
(
  'hist2025_c879971b33c9b390', 'KNE-17012025-001', '2025-01-17', '2025-01-17', '2025-01-17',
  'SABIR ALAM', '9006712520', '', 'Kishanganj', '30', 'Male',
  'DIGAL BANK, PADAMPUR, GANDHARB, KNE', 'Fistula', '55000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-17T10:00:00.000Z', '2025-01-17T10:00:00.000Z'
),
(
  'hist2025_e186a9d641acfbed', 'KNE-31012025-001', '2025-01-31', '2025-01-31', '2025-01-31',
  'BIPUL BARMAN', '8388008655', '', 'Kishanganj', '24', 'Male',
  'SAIDPUR, MAHESH PUR, KARAM, U.D', 'Fistula', '100000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-31T10:00:00.000Z', '2025-01-31T10:00:00.000Z'
),
(
  'hist2025_ff45a89e445c3975', 'KNE-03022025-001', '2025-02-03', '2025-02-03', '2025-02-03',
  'MD TEJIN UDDIN', '8942967494', '', 'Kishanganj', '65', 'Male',
  'BANGAO, GOYAGAO, GOYALPOKHAR, U.D', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-03T10:00:00.000Z', '2025-02-03T10:00:00.000Z'
),
(
  'hist2025_88d8f382014de132', 'KNE-05022025-001', '2025-02-05', '2025-02-05', '2025-02-05',
  'KAMRUL HOSSIN', '9347502740', '', 'Kishanganj', '22', 'Male',
  'MEMGAO, KNE, KNE, KNE', 'Gupt Rog', '19000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-05T10:00:00.000Z', '2025-02-05T10:00:00.000Z'
),
(
  'hist2025_7b51be03bcc5e104', 'KNE-05022025-002', '2025-02-05', '2025-02-05', '2025-02-05',
  'ZIDARUL HOWAK', '8825174821', '', 'Kishanganj', '26', 'Male',
  'MEMGAO, KNE, KNE, KNE', 'Gupt Rog', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-05T10:00:00.000Z', '2025-02-05T10:00:00.000Z'
),
(
  'hist2025_1e780218698b7151', 'KNE-12022025-001', '2025-02-12', '2025-02-12', '2025-02-12',
  'LALIF PASWAN', '8676073770', '', 'Kishanganj', '26', 'Male',
  'RAMPUR, CHAPOR, CHAKULIYA, U.D', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-12T10:00:00.000Z', '2025-02-12T10:00:00.000Z'
),
(
  'hist2025_0e0faa9efa689c24', 'KNE-19022025-001', '2025-02-19', '2025-02-19', '2025-02-19',
  'RESHMA KHATOON', '9593316409', '', 'Kishanganj', '45', 'Female',
  'DIGALGAO, BETNA, KNE, KNE', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-19T10:00:00.000Z', '2025-02-19T10:00:00.000Z'
),
(
  'hist2025_3effe16fc3428e38', 'KNE-19022025-002', '2025-02-19', '2025-02-19', '2025-02-19',
  'MD AHRAR ALAM', '9609023646', '', 'Kishanganj', '26', 'Male',
  'MAKHAN POKHAR, SAHAPUR, CHAKULIYA, U.D', 'Piles', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-19T10:00:00.000Z', '2025-02-19T10:00:00.000Z'
),
(
  'hist2025_cf1306f9b745efa4', 'KNE-20022025-001', '2025-02-20', '2025-02-20', '2025-02-20',
  'MD NABI', '8757937444', '', 'Kishanganj', '33', 'Male',
  'JELIVITA, DIGAL BANK, DIGAL BANK, KNE', 'Fistula', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-20T10:00:00.000Z', '2025-02-20T10:00:00.000Z'
),
(
  'hist2025_0197614e2aa43dfc', 'KNE-28022025-001', '2025-02-28', '2025-02-28', '2025-02-28',
  'DEVLAL SINGH', '9733237054', '', 'Kishanganj', '50', 'Male',
  'BANAGHECHH, KONIYAVITA, GOYALPOKHAR, U.D', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-28T10:00:00.000Z', '2025-02-28T10:00:00.000Z'
),
(
  'hist2025_9f9f6396797e0341', 'KNE-03032025-001', '2025-03-03', '2025-03-03', '2025-03-03',
  'NUR SELIM', '6296133324', '', 'Kishanganj', '25', 'Male',
  'DIGHLI, DIGHLI, GOYALPOKHAR, U.D', 'Piles', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-03T10:00:00.000Z', '2025-03-03T10:00:00.000Z'
),
(
  'hist2025_8acaa23ad2c62803', 'KNE-05032025-001', '2025-03-05', '2025-03-05', '2025-03-05',
  'ASIT HALDAR', '9593382924', '', 'Kishanganj', '31', 'Male',
  'ABDULPUR, HASMPUR, DALKHOLA, U.D', 'Fistula', '65000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-05T10:00:00.000Z', '2025-03-05T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'hist2025_pay_c96745fb12c47e04', 'treatment', 'Advance', 'Advance', 'hist2025_47026da8fe5be4a7', '9126495421', 'Kishanganj', 'PARESH MANDAL',
  '2025-01-06', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-06T10:15:00.000Z', '2025-01-06T10:15:00.000Z'
),
(
  'hist2025_pay_cc441977fa49979c', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_47026da8fe5be4a7', '9126495421', 'Kishanganj', 'PARESH MANDAL',
  '2025-01-08', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-08T10:16:00.000Z', '2025-01-08T10:16:00.000Z'
),
(
  'hist2025_pay_640ed509ad8defce', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_47026da8fe5be4a7', '9126495421', 'Kishanganj', 'PARESH MANDAL',
  '2025-01-13', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-13T10:17:00.000Z', '2025-01-13T10:17:00.000Z'
),
(
  'hist2025_pay_e957f7498e1a6368', 'treatment', '4th Payment', '4th Payment', 'hist2025_47026da8fe5be4a7', '9126495421', 'Kishanganj', 'PARESH MANDAL',
  '2025-01-15', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-15T10:18:00.000Z', '2025-01-15T10:18:00.000Z'
),
(
  'hist2025_pay_adf3d7d18a2f82f7', 'treatment', '5th Payment', '5th Payment', 'hist2025_47026da8fe5be4a7', '9126495421', 'Kishanganj', 'PARESH MANDAL',
  '2025-01-20', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-20T10:19:00.000Z', '2025-01-20T10:19:00.000Z'
),
(
  'hist2025_pay_44cd908de62c4577', 'treatment', '6th Payment', '6th Payment', 'hist2025_47026da8fe5be4a7', '9126495421', 'Kishanganj', 'PARESH MANDAL',
  '2025-01-22', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-22T10:20:00.000Z', '2025-01-22T10:20:00.000Z'
),
(
  'hist2025_pay_e40669eb6f97901a', 'treatment', '7th Payment', '7th Payment', 'hist2025_47026da8fe5be4a7', '9126495421', 'Kishanganj', 'PARESH MANDAL',
  '2025-01-24', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-24T10:21:00.000Z', '2025-01-24T10:21:00.000Z'
),
(
  'hist2025_pay_3403009f4a7e7adb', 'treatment', '8th Payment', '8th Payment', 'hist2025_47026da8fe5be4a7', '9126495421', 'Kishanganj', 'PARESH MANDAL',
  '2025-01-29', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-29T10:22:00.000Z', '2025-01-29T10:22:00.000Z'
),
(
  'hist2025_pay_d37399b83f044cb0', 'treatment', '9th Payment', '9th Payment', 'hist2025_47026da8fe5be4a7', '9126495421', 'Kishanganj', 'PARESH MANDAL',
  '2025-02-05', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-05T10:23:00.000Z', '2025-02-05T10:23:00.000Z'
),
(
  'hist2025_pay_9d1ee2cf2aafaf28', 'treatment', '10th Payment', '10th Payment', 'hist2025_47026da8fe5be4a7', '9126495421', 'Kishanganj', 'PARESH MANDAL',
  '2025-02-07', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-07T10:24:00.000Z', '2025-02-07T10:24:00.000Z'
),
(
  'hist2025_pay_b88d2d423b3a398b', 'treatment', '11th Payment', '11th Payment', 'hist2025_47026da8fe5be4a7', '9126495421', 'Kishanganj', 'PARESH MANDAL',
  '2025-02-14', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-14T10:25:00.000Z', '2025-02-14T10:25:00.000Z'
),
(
  'hist2025_pay_b58278548e552ea7', 'treatment', '12th Payment', '12th Payment', 'hist2025_47026da8fe5be4a7', '9126495421', 'Kishanganj', 'PARESH MANDAL',
  '2025-02-28', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-28T10:26:00.000Z', '2025-02-28T10:26:00.000Z'
),
(
  'hist2025_pay_ad02d5f43178153b', 'treatment', '13th Payment', '13th Payment', 'hist2025_47026da8fe5be4a7', '9126495421', 'Kishanganj', 'PARESH MANDAL',
  '2025-03-10', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-10T10:27:00.000Z', '2025-03-10T10:27:00.000Z'
),
(
  'hist2025_pay_d25c63f1f66e5aef', 'treatment', '14th Payment', '14th Payment', 'hist2025_47026da8fe5be4a7', '9126495421', 'Kishanganj', 'PARESH MANDAL',
  '2025-06-02', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-02T10:28:00.000Z', '2025-06-02T10:28:00.000Z'
),
(
  'hist2025_pay_ab5c7e3b52c270d1', 'treatment', 'Advance', 'Advance', 'hist2025_0dcd595e07cc56eb', '9064332124', 'Kishanganj', 'NASIMA KHATUN',
  '2025-01-06', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-06T10:15:00.000Z', '2025-01-06T10:15:00.000Z'
),
(
  'hist2025_pay_85a73d619c416a11', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_0dcd595e07cc56eb', '9064332124', 'Kishanganj', 'NASIMA KHATUN',
  '2025-01-08', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-08T10:16:00.000Z', '2025-01-08T10:16:00.000Z'
),
(
  'hist2025_pay_a08535ea6b7f5a39', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_0dcd595e07cc56eb', '9064332124', 'Kishanganj', 'NASIMA KHATUN',
  '2025-01-11', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-11T10:17:00.000Z', '2025-01-11T10:17:00.000Z'
),
(
  'hist2025_pay_1aebbffa215654ec', 'treatment', '4th Payment', '4th Payment', 'hist2025_0dcd595e07cc56eb', '9064332124', 'Kishanganj', 'NASIMA KHATUN',
  '2025-01-16', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-16T10:18:00.000Z', '2025-01-16T10:18:00.000Z'
),
(
  'hist2025_pay_28153d45dc6bdef9', 'treatment', '5th Payment', '5th Payment', 'hist2025_0dcd595e07cc56eb', '9064332124', 'Kishanganj', 'NASIMA KHATUN',
  '2025-02-03', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-03T10:19:00.000Z', '2025-02-03T10:19:00.000Z'
),
(
  'hist2025_pay_42e5fe068bc7a85d', 'treatment', '6th Payment', '6th Payment', 'hist2025_0dcd595e07cc56eb', '9064332124', 'Kishanganj', 'NASIMA KHATUN',
  '2025-02-05', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-05T10:20:00.000Z', '2025-02-05T10:20:00.000Z'
),
(
  'hist2025_pay_4d748137646ba723', 'treatment', '7th Payment', '7th Payment', 'hist2025_0dcd595e07cc56eb', '9064332124', 'Kishanganj', 'NASIMA KHATUN',
  '2025-02-08', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-08T10:21:00.000Z', '2025-02-08T10:21:00.000Z'
),
(
  'hist2025_pay_718c8d4365c1a595', 'treatment', '8th Payment', '8th Payment', 'hist2025_0dcd595e07cc56eb', '9064332124', 'Kishanganj', 'NASIMA KHATUN',
  '2025-02-12', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-12T10:22:00.000Z', '2025-02-12T10:22:00.000Z'
),
(
  'hist2025_pay_1a69254c1424b141', 'treatment', 'Advance', 'Advance', 'hist2025_c879971b33c9b390', '9006712520', 'Kishanganj', 'SABIR ALAM',
  '2025-01-17', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-17T10:15:00.000Z', '2025-01-17T10:15:00.000Z'
),
(
  'hist2025_pay_b926992b6c3d0889', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_c879971b33c9b390', '9006712520', 'Kishanganj', 'SABIR ALAM',
  '2025-01-29', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-29T10:16:00.000Z', '2025-01-29T10:16:00.000Z'
),
(
  'hist2025_pay_2b4ffc205e825ee7', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_c879971b33c9b390', '9006712520', 'Kishanganj', 'SABIR ALAM',
  '2025-02-05', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-05T10:17:00.000Z', '2025-02-05T10:17:00.000Z'
),
(
  'hist2025_pay_f3b4fe2682fb7f42', 'treatment', '4th Payment', '4th Payment', 'hist2025_c879971b33c9b390', '9006712520', 'Kishanganj', 'SABIR ALAM',
  '2025-02-12', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-12T10:18:00.000Z', '2025-02-12T10:18:00.000Z'
),
(
  'hist2025_pay_7ca64a437b101658', 'treatment', '5th Payment', '5th Payment', 'hist2025_c879971b33c9b390', '9006712520', 'Kishanganj', 'SABIR ALAM',
  '2025-02-19', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-19T10:19:00.000Z', '2025-02-19T10:19:00.000Z'
),
(
  'hist2025_pay_e09f64decc65725e', 'treatment', '6th Payment', '6th Payment', 'hist2025_c879971b33c9b390', '9006712520', 'Kishanganj', 'SABIR ALAM',
  '2025-02-26', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-26T10:20:00.000Z', '2025-02-26T10:20:00.000Z'
),
(
  'hist2025_pay_ee0bbddb32b99199', 'treatment', '7th Payment', '7th Payment', 'hist2025_c879971b33c9b390', '9006712520', 'Kishanganj', 'SABIR ALAM',
  '2025-03-05', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-05T10:21:00.000Z', '2025-03-05T10:21:00.000Z'
),
(
  'hist2025_pay_13922ac004855260', 'treatment', 'Advance', 'Advance', 'hist2025_e186a9d641acfbed', '8388008655', 'Kishanganj', 'BIPUL BARMAN',
  '2025-02-05', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-05T10:15:00.000Z', '2025-02-05T10:15:00.000Z'
),
(
  'hist2025_pay_11009e8105d503bc', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_e186a9d641acfbed', '8388008655', 'Kishanganj', 'BIPUL BARMAN',
  '2025-02-08', '20000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-08T10:16:00.000Z', '2025-02-08T10:16:00.000Z'
),
(
  'hist2025_pay_7d470d7c52ce038e', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_e186a9d641acfbed', '8388008655', 'Kishanganj', 'BIPUL BARMAN',
  '2025-02-15', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-15T10:17:00.000Z', '2025-02-15T10:17:00.000Z'
),
(
  'hist2025_pay_54ba3d90cd88ab46', 'treatment', '4th Payment', '4th Payment', 'hist2025_e186a9d641acfbed', '8388008655', 'Kishanganj', 'BIPUL BARMAN',
  '2025-02-19', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-19T10:18:00.000Z', '2025-02-19T10:18:00.000Z'
),
(
  'hist2025_pay_4e0b54594f233203', 'treatment', '5th Payment', '5th Payment', 'hist2025_e186a9d641acfbed', '8388008655', 'Kishanganj', 'BIPUL BARMAN',
  '2025-02-22', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-22T10:19:00.000Z', '2025-02-22T10:19:00.000Z'
),
(
  'hist2025_pay_b6ce0e6b0b44cecb', 'treatment', '6th Payment', '6th Payment', 'hist2025_e186a9d641acfbed', '8388008655', 'Kishanganj', 'BIPUL BARMAN',
  '2025-02-26', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-26T10:20:00.000Z', '2025-02-26T10:20:00.000Z'
),
(
  'hist2025_pay_e7f25cfcda0a24e5', 'treatment', '7th Payment', '7th Payment', 'hist2025_e186a9d641acfbed', '8388008655', 'Kishanganj', 'BIPUL BARMAN',
  '2025-02-28', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-28T10:21:00.000Z', '2025-02-28T10:21:00.000Z'
),
(
  'hist2025_pay_4b7129052ddd9684', 'treatment', '8th Payment', '8th Payment', 'hist2025_e186a9d641acfbed', '8388008655', 'Kishanganj', 'BIPUL BARMAN',
  '2025-03-08', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-08T10:22:00.000Z', '2025-03-08T10:22:00.000Z'
),
(
  'hist2025_pay_49b019a77bc42c0d', 'treatment', '9th Payment', '9th Payment', 'hist2025_e186a9d641acfbed', '8388008655', 'Kishanganj', 'BIPUL BARMAN',
  '2025-03-17', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-17T10:23:00.000Z', '2025-03-17T10:23:00.000Z'
),
(
  'hist2025_pay_6f12178e2656dba0', 'treatment', '10th Payment', '10th Payment', 'hist2025_e186a9d641acfbed', '8388008655', 'Kishanganj', 'BIPUL BARMAN',
  '2025-04-23', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-23T10:24:00.000Z', '2025-04-23T10:24:00.000Z'
),
(
  'hist2025_pay_d9469246f2568199', 'treatment', '11th Payment', '11th Payment', 'hist2025_e186a9d641acfbed', '8388008655', 'Kishanganj', 'BIPUL BARMAN',
  '2025-05-14', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-14T10:25:00.000Z', '2025-05-14T10:25:00.000Z'
),
(
  'hist2025_pay_47e861bc3c743993', 'treatment', '12th Payment', '12th Payment', 'hist2025_e186a9d641acfbed', '8388008655', 'Kishanganj', 'BIPUL BARMAN',
  '2025-06-18', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-18T10:26:00.000Z', '2025-06-18T10:26:00.000Z'
),
(
  'hist2025_pay_31452823630aa95b', 'treatment', 'Advance', 'Advance', 'hist2025_ff45a89e445c3975', '8942967494', 'Kishanganj', 'MD TEJIN UDDIN',
  '2025-02-03', '2500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-03T10:15:00.000Z', '2025-02-03T10:15:00.000Z'
),
(
  'hist2025_pay_5637bdec22a3c80c', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_ff45a89e445c3975', '8942967494', 'Kishanganj', 'MD TEJIN UDDIN',
  '2025-02-05', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-05T10:16:00.000Z', '2025-02-05T10:16:00.000Z'
),
(
  'hist2025_pay_618b9a78b23bc39e', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_ff45a89e445c3975', '8942967494', 'Kishanganj', 'MD TEJIN UDDIN',
  '2025-02-08', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-08T10:17:00.000Z', '2025-02-08T10:17:00.000Z'
),
(
  'hist2025_pay_b857b44f1004411a', 'treatment', '4th Payment', '4th Payment', 'hist2025_ff45a89e445c3975', '8942967494', 'Kishanganj', 'MD TEJIN UDDIN',
  '2025-02-12', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-12T10:18:00.000Z', '2025-02-12T10:18:00.000Z'
),
(
  'hist2025_pay_3c4eec34ae4f2ddb', 'treatment', '5th Payment', '5th Payment', 'hist2025_ff45a89e445c3975', '8942967494', 'Kishanganj', 'MD TEJIN UDDIN',
  '2025-02-16', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-16T10:19:00.000Z', '2025-02-16T10:19:00.000Z'
),
(
  'hist2025_pay_b30167b2953e4a31', 'treatment', '6th Payment', '6th Payment', 'hist2025_ff45a89e445c3975', '8942967494', 'Kishanganj', 'MD TEJIN UDDIN',
  '2025-02-19', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-19T10:20:00.000Z', '2025-02-19T10:20:00.000Z'
),
(
  'hist2025_pay_1ebac0b95442a0cc', 'treatment', '7th Payment', '7th Payment', 'hist2025_ff45a89e445c3975', '8942967494', 'Kishanganj', 'MD TEJIN UDDIN',
  '2025-02-22', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-22T10:21:00.000Z', '2025-02-22T10:21:00.000Z'
),
(
  'hist2025_pay_30ddd343ca33dc4d', 'treatment', '8th Payment', '8th Payment', 'hist2025_ff45a89e445c3975', '8942967494', 'Kishanganj', 'MD TEJIN UDDIN',
  '2025-02-25', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-25T10:22:00.000Z', '2025-02-25T10:22:00.000Z'
),
(
  'hist2025_pay_0a759ed072f3de84', 'treatment', '9th Payment', '9th Payment', 'hist2025_ff45a89e445c3975', '8942967494', 'Kishanganj', 'MD TEJIN UDDIN',
  '2025-02-29', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-29T10:23:00.000Z', '2025-02-29T10:23:00.000Z'
),
(
  'hist2025_pay_9b6e1c6f579d2cc0', 'treatment', '10th Payment', '10th Payment', 'hist2025_ff45a89e445c3975', '8942967494', 'Kishanganj', 'MD TEJIN UDDIN',
  '2025-03-01', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-01T10:24:00.000Z', '2025-03-01T10:24:00.000Z'
),
(
  'hist2025_pay_1286f8d6decdfb12', 'treatment', '11th Payment', '11th Payment', 'hist2025_ff45a89e445c3975', '8942967494', 'Kishanganj', 'MD TEJIN UDDIN',
  '2025-03-03', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-03T10:25:00.000Z', '2025-03-03T10:25:00.000Z'
),
(
  'hist2025_pay_a7c50bc182e7c0ac', 'treatment', '12th Payment', '12th Payment', 'hist2025_ff45a89e445c3975', '8942967494', 'Kishanganj', 'MD TEJIN UDDIN',
  '2025-03-05', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-05T10:26:00.000Z', '2025-03-05T10:26:00.000Z'
),
(
  'hist2025_pay_c0d3ecfaca6d9d03', 'treatment', '13th Payment', '13th Payment', 'hist2025_ff45a89e445c3975', '8942967494', 'Kishanganj', 'MD TEJIN UDDIN',
  '2025-03-10', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-10T10:27:00.000Z', '2025-03-10T10:27:00.000Z'
),
(
  'hist2025_pay_259f3f8d8d2debbd', 'treatment', 'Advance', 'Advance', 'hist2025_88d8f382014de132', '9347502740', 'Kishanganj', 'KAMRUL HOSSIN',
  '2025-02-05', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-05T10:15:00.000Z', '2025-02-05T10:15:00.000Z'
),
(
  'hist2025_pay_2c861320263f8f13', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_88d8f382014de132', '9347502740', 'Kishanganj', 'KAMRUL HOSSIN',
  '2025-02-15', '13000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-15T10:16:00.000Z', '2025-02-15T10:16:00.000Z'
),
(
  'hist2025_pay_0b672babac361fd1', 'treatment', 'Advance', 'Advance', 'hist2025_7b51be03bcc5e104', '8825174821', 'Kishanganj', 'ZIDARUL HOWAK',
  '2025-02-05', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-05T10:15:00.000Z', '2025-02-05T10:15:00.000Z'
),
(
  'hist2025_pay_de33b5c5d5bca459', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_7b51be03bcc5e104', '8825174821', 'Kishanganj', 'ZIDARUL HOWAK',
  '2025-02-13', '14000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-13T10:16:00.000Z', '2025-02-13T10:16:00.000Z'
),
(
  'hist2025_pay_7d9d46b8cf2f0863', 'treatment', 'Advance', 'Advance', 'hist2025_1e780218698b7151', '8676073770', 'Kishanganj', 'LALIF PASWAN',
  '2025-02-12', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-12T10:15:00.000Z', '2025-02-12T10:15:00.000Z'
),
(
  'hist2025_pay_88417bcbade1c782', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_1e780218698b7151', '8676073770', 'Kishanganj', 'LALIF PASWAN',
  '2025-02-15', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-15T10:16:00.000Z', '2025-02-15T10:16:00.000Z'
),
(
  'hist2025_pay_c3e9e19a04ad61a4', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_1e780218698b7151', '8676073770', 'Kishanganj', 'LALIF PASWAN',
  '2025-03-08', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-08T10:17:00.000Z', '2025-03-08T10:17:00.000Z'
),
(
  'hist2025_pay_c7fe41b8558f6318', 'treatment', '4th Payment', '4th Payment', 'hist2025_1e780218698b7151', '8676073770', 'Kishanganj', 'LALIF PASWAN',
  '2025-03-15', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-15T10:18:00.000Z', '2025-03-15T10:18:00.000Z'
),
(
  'hist2025_pay_956d641dc8683dec', 'treatment', '5th Payment', '5th Payment', 'hist2025_1e780218698b7151', '8676073770', 'Kishanganj', 'LALIF PASWAN',
  '2025-03-22', '2500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-22T10:19:00.000Z', '2025-03-22T10:19:00.000Z'
),
(
  'hist2025_pay_55b19c7b0a46e940', 'treatment', 'Advance', 'Advance', 'hist2025_0e0faa9efa689c24', '9593316409', 'Kishanganj', 'RESHMA KHATOON',
  '2025-02-19', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-19T10:15:00.000Z', '2025-02-19T10:15:00.000Z'
),
(
  'hist2025_pay_7163ccca6739b258', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_0e0faa9efa689c24', '9593316409', 'Kishanganj', 'RESHMA KHATOON',
  '2025-02-22', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-22T10:16:00.000Z', '2025-02-22T10:16:00.000Z'
),
(
  'hist2025_pay_367bd1135a4f6533', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_0e0faa9efa689c24', '9593316409', 'Kishanganj', 'RESHMA KHATOON',
  '2025-03-01', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-01T10:17:00.000Z', '2025-03-01T10:17:00.000Z'
),
(
  'hist2025_pay_0642465f28144d3b', 'treatment', '4th Payment', '4th Payment', 'hist2025_0e0faa9efa689c24', '9593316409', 'Kishanganj', 'RESHMA KHATOON',
  '2025-03-05', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-05T10:18:00.000Z', '2025-03-05T10:18:00.000Z'
),
(
  'hist2025_pay_3f91af2afc356161', 'treatment', 'Advance', 'Advance', 'hist2025_3effe16fc3428e38', '9609023646', 'Kishanganj', 'MD AHRAR ALAM',
  '2025-07-23', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-23T10:15:00.000Z', '2025-07-23T10:15:00.000Z'
),
(
  'hist2025_pay_5946ab02ae312c70', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_3effe16fc3428e38', '9609023646', 'Kishanganj', 'MD AHRAR ALAM',
  '2025-08-02', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:16:00.000Z', '2025-08-02T10:16:00.000Z'
),
(
  'hist2025_pay_afd870d363047ae5', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_3effe16fc3428e38', '9609023646', 'Kishanganj', 'MD AHRAR ALAM',
  '2025-08-06', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-06T10:17:00.000Z', '2025-08-06T10:17:00.000Z'
),
(
  'hist2025_pay_78b14a36a1864228', 'treatment', '4th Payment', '4th Payment', 'hist2025_3effe16fc3428e38', '9609023646', 'Kishanganj', 'MD AHRAR ALAM',
  '2025-08-13', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-13T10:18:00.000Z', '2025-08-13T10:18:00.000Z'
),
(
  'hist2025_pay_d376a8b804c90e8a', 'treatment', '5th Payment', '5th Payment', 'hist2025_3effe16fc3428e38', '9609023646', 'Kishanganj', 'MD AHRAR ALAM',
  '2025-08-16', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:19:00.000Z', '2025-08-16T10:19:00.000Z'
),
(
  'hist2025_pay_e61752f26963c62f', 'treatment', '6th Payment', '6th Payment', 'hist2025_3effe16fc3428e38', '9609023646', 'Kishanganj', 'MD AHRAR ALAM',
  '2025-08-20', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-20T10:20:00.000Z', '2025-08-20T10:20:00.000Z'
),
(
  'hist2025_pay_85aa203a22ac155b', 'treatment', '7th Payment', '7th Payment', 'hist2025_3effe16fc3428e38', '9609023646', 'Kishanganj', 'MD AHRAR ALAM',
  '2025-08-23', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:21:00.000Z', '2025-08-23T10:21:00.000Z'
),
(
  'hist2025_pay_dd2a05b79a4a2b39', 'treatment', '8th Payment', '8th Payment', 'hist2025_3effe16fc3428e38', '9609023646', 'Kishanganj', 'MD AHRAR ALAM',
  '2025-08-27', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-27T10:22:00.000Z', '2025-08-27T10:22:00.000Z'
),
(
  'hist2025_pay_c4b279a36580c697', 'treatment', '9th Payment', '9th Payment', 'hist2025_3effe16fc3428e38', '9609023646', 'Kishanganj', 'MD AHRAR ALAM',
  '2025-10-25', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-25T10:23:00.000Z', '2025-10-25T10:23:00.000Z'
),
(
  'hist2025_pay_dc67f10de783a546', 'treatment', 'Advance', 'Advance', 'hist2025_cf1306f9b745efa4', '8757937444', 'Kishanganj', 'MD NABI',
  '2025-07-23', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-23T10:15:00.000Z', '2025-07-23T10:15:00.000Z'
),
(
  'hist2025_pay_7db94526ec78bfab', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_cf1306f9b745efa4', '8757937444', 'Kishanganj', 'MD NABI',
  '2025-07-26', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:16:00.000Z', '2025-07-26T10:16:00.000Z'
),
(
  'hist2025_pay_06819917eb7ff1f9', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_cf1306f9b745efa4', '8757937444', 'Kishanganj', 'MD NABI',
  '2025-07-30', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-30T10:17:00.000Z', '2025-07-30T10:17:00.000Z'
),
(
  'hist2025_pay_80d129c028252fa9', 'treatment', '4th Payment', '4th Payment', 'hist2025_cf1306f9b745efa4', '8757937444', 'Kishanganj', 'MD NABI',
  '2025-08-06', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-06T10:18:00.000Z', '2025-08-06T10:18:00.000Z'
),
(
  'hist2025_pay_5b9e40b90197df3f', 'treatment', '5th Payment', '5th Payment', 'hist2025_cf1306f9b745efa4', '8757937444', 'Kishanganj', 'MD NABI',
  '2025-08-13', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-13T10:19:00.000Z', '2025-08-13T10:19:00.000Z'
),
(
  'hist2025_pay_ae875ade093a5d44', 'treatment', '6th Payment', '6th Payment', 'hist2025_cf1306f9b745efa4', '8757937444', 'Kishanganj', 'MD NABI',
  '2025-08-20', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-20T10:20:00.000Z', '2025-08-20T10:20:00.000Z'
),
(
  'hist2025_pay_628bdf069a071f66', 'treatment', '7th Payment', '7th Payment', 'hist2025_cf1306f9b745efa4', '8757937444', 'Kishanganj', 'MD NABI',
  '2025-08-27', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-27T10:21:00.000Z', '2025-08-27T10:21:00.000Z'
),
(
  'hist2025_pay_b86cd9e173fa200b', 'treatment', '8th Payment', '8th Payment', 'hist2025_cf1306f9b745efa4', '8757937444', 'Kishanganj', 'MD NABI',
  '2025-09-03', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-03T10:22:00.000Z', '2025-09-03T10:22:00.000Z'
),
(
  'hist2025_pay_6e702cc4115d5333', 'treatment', '9th Payment', '9th Payment', 'hist2025_cf1306f9b745efa4', '8757937444', 'Kishanganj', 'MD NABI',
  '2025-09-10', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-10T10:23:00.000Z', '2025-09-10T10:23:00.000Z'
),
(
  'hist2025_pay_39df6aab03d4e59e', 'treatment', '10th Payment', '10th Payment', 'hist2025_cf1306f9b745efa4', '8757937444', 'Kishanganj', 'MD NABI',
  '2025-09-17', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-17T10:24:00.000Z', '2025-09-17T10:24:00.000Z'
),
(
  'hist2025_pay_a262bfd87f86de4b', 'treatment', '11th Payment', '11th Payment', 'hist2025_cf1306f9b745efa4', '8757937444', 'Kishanganj', 'MD NABI',
  '2025-10-01', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-01T10:25:00.000Z', '2025-10-01T10:25:00.000Z'
),
(
  'hist2025_pay_82e2d259a33edc06', 'treatment', '12th Payment', '12th Payment', 'hist2025_cf1306f9b745efa4', '8757937444', 'Kishanganj', 'MD NABI',
  '2025-10-15', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-15T10:26:00.000Z', '2025-10-15T10:26:00.000Z'
),
(
  'hist2025_pay_d5786bbc188b45c6', 'treatment', 'Advance', 'Advance', 'hist2025_0197614e2aa43dfc', '9733237054', 'Kishanganj', 'DEVLAL SINGH',
  '2025-02-28', '1000', 'CASH', 'Historical import (2025 Google Sheet) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-28T10:15:00.000Z', '2025-02-28T10:15:00.000Z'
),
(
  'hist2025_pay_90e286fc2b975389', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_0197614e2aa43dfc', '9733237054', 'Kishanganj', 'DEVLAL SINGH',
  '2025-02-28', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-28T10:16:00.000Z', '2025-02-28T10:16:00.000Z'
),
(
  'hist2025_pay_e861758ab0e971a6', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_0197614e2aa43dfc', '9733237054', 'Kishanganj', 'DEVLAL SINGH',
  '2025-03-04', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-04T10:17:00.000Z', '2025-03-04T10:17:00.000Z'
),
(
  'hist2025_pay_83961efefe6128fd', 'treatment', '4th Payment', '4th Payment', 'hist2025_0197614e2aa43dfc', '9733237054', 'Kishanganj', 'DEVLAL SINGH',
  '2025-03-05', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-05T10:18:00.000Z', '2025-03-05T10:18:00.000Z'
),
(
  'hist2025_pay_7f8e9b3dd27e2fa8', 'treatment', '5th Payment', '5th Payment', 'hist2025_0197614e2aa43dfc', '9733237054', 'Kishanganj', 'DEVLAL SINGH',
  '2025-03-08', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-08T10:19:00.000Z', '2025-03-08T10:19:00.000Z'
),
(
  'hist2025_pay_03085d8d63bb85e1', 'treatment', '6th Payment', '6th Payment', 'hist2025_0197614e2aa43dfc', '9733237054', 'Kishanganj', 'DEVLAL SINGH',
  '2025-03-12', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-12T10:20:00.000Z', '2025-03-12T10:20:00.000Z'
),
(
  'hist2025_pay_448b3b3dff1c9e66', 'treatment', '7th Payment', '7th Payment', 'hist2025_0197614e2aa43dfc', '9733237054', 'Kishanganj', 'DEVLAL SINGH',
  '2025-03-19', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-19T10:21:00.000Z', '2025-03-19T10:21:00.000Z'
),
(
  'hist2025_pay_fd3a960b911d8598', 'treatment', '8th Payment', '8th Payment', 'hist2025_0197614e2aa43dfc', '9733237054', 'Kishanganj', 'DEVLAL SINGH',
  '2025-03-27', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-27T10:22:00.000Z', '2025-03-27T10:22:00.000Z'
),
(
  'hist2025_pay_37cfd9cb62f76298', 'treatment', '9th Payment', '9th Payment', 'hist2025_0197614e2aa43dfc', '9733237054', 'Kishanganj', 'DEVLAL SINGH',
  '2025-04-06', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-06T10:23:00.000Z', '2025-04-06T10:23:00.000Z'
),
(
  'hist2025_pay_a057e5f4865cba77', 'treatment', '10th Payment', '10th Payment', 'hist2025_0197614e2aa43dfc', '9733237054', 'Kishanganj', 'DEVLAL SINGH',
  '2025-05-28', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-28T10:24:00.000Z', '2025-05-28T10:24:00.000Z'
),
(
  'hist2025_pay_909ff3eab2949cbe', 'treatment', '11th Payment', '11th Payment', 'hist2025_0197614e2aa43dfc', '9733237054', 'Kishanganj', 'DEVLAL SINGH',
  '2025-06-06', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-06T10:25:00.000Z', '2025-06-06T10:25:00.000Z'
),
(
  'hist2025_pay_8581921afce66baf', 'treatment', 'Advance', 'Advance', 'hist2025_9f9f6396797e0341', '6296133324', 'Kishanganj', 'NUR SELIM',
  '2025-03-03', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-03T10:15:00.000Z', '2025-03-03T10:15:00.000Z'
),
(
  'hist2025_pay_5c7db3b573712791', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_9f9f6396797e0341', '6296133324', 'Kishanganj', 'NUR SELIM',
  '2025-03-05', '12500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-05T10:16:00.000Z', '2025-03-05T10:16:00.000Z'
),
(
  'hist2025_pay_c44927c13d1f9f2d', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_9f9f6396797e0341', '6296133324', 'Kishanganj', 'NUR SELIM',
  '2025-03-10', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-10T10:17:00.000Z', '2025-03-10T10:17:00.000Z'
),
(
  'hist2025_pay_8f17284424ebfccf', 'treatment', '4th Payment', '4th Payment', 'hist2025_9f9f6396797e0341', '6296133324', 'Kishanganj', 'NUR SELIM',
  '2025-03-20', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-20T10:18:00.000Z', '2025-03-20T10:18:00.000Z'
),
(
  'hist2025_pay_b55774a2b30cc3d6', 'treatment', 'Advance', 'Advance', 'hist2025_8acaa23ad2c62803', '9593382924', 'Kishanganj', 'ASIT HALDAR',
  '2025-03-05', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-05T10:15:00.000Z', '2025-03-05T10:15:00.000Z'
),
(
  'hist2025_pay_e1b44d5c12f09cce', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_8acaa23ad2c62803', '9593382924', 'Kishanganj', 'ASIT HALDAR',
  '2025-03-08', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-08T10:16:00.000Z', '2025-03-08T10:16:00.000Z'
),
(
  'hist2025_pay_6ca4e062205af22c', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_8acaa23ad2c62803', '9593382924', 'Kishanganj', 'ASIT HALDAR',
  '2025-03-12', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-12T10:17:00.000Z', '2025-03-12T10:17:00.000Z'
),
(
  'hist2025_pay_d35848ae5e42380d', 'treatment', '4th Payment', '4th Payment', 'hist2025_8acaa23ad2c62803', '9593382924', 'Kishanganj', 'ASIT HALDAR',
  '2025-03-18', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-18T10:18:00.000Z', '2025-03-18T10:18:00.000Z'
),
(
  'hist2025_pay_ae51dd2f454491ee', 'treatment', '5th Payment', '5th Payment', 'hist2025_8acaa23ad2c62803', '9593382924', 'Kishanganj', 'ASIT HALDAR',
  '2025-03-22', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-22T10:19:00.000Z', '2025-03-22T10:19:00.000Z'
),
(
  'hist2025_pay_c0b96859fc24b846', 'treatment', '6th Payment', '6th Payment', 'hist2025_8acaa23ad2c62803', '9593382924', 'Kishanganj', 'ASIT HALDAR',
  '2025-04-05', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-05T10:20:00.000Z', '2025-04-05T10:20:00.000Z'
),
(
  'hist2025_pay_d19d543d3d4c7762', 'treatment', '7th Payment', '7th Payment', 'hist2025_8acaa23ad2c62803', '9593382924', 'Kishanganj', 'ASIT HALDAR',
  '2025-04-09', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-09T10:21:00.000Z', '2025-04-09T10:21:00.000Z'
),
(
  'hist2025_pay_c3577293bdb8a8fb', 'treatment', '8th Payment', '8th Payment', 'hist2025_8acaa23ad2c62803', '9593382924', 'Kishanganj', 'ASIT HALDAR',
  '2025-03-15', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-15T10:22:00.000Z', '2025-03-15T10:22:00.000Z'
),
(
  'hist2025_pay_2633d0545a3f3172', 'treatment', '9th Payment', '9th Payment', 'hist2025_8acaa23ad2c62803', '9593382924', 'Kishanganj', 'ASIT HALDAR',
  '2025-03-26', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-26T10:23:00.000Z', '2025-03-26T10:23:00.000Z'
),
(
  'hist2025_pay_751e1e0d2f86b134', 'treatment', '10th Payment', '10th Payment', 'hist2025_8acaa23ad2c62803', '9593382924', 'Kishanganj', 'ASIT HALDAR',
  '2025-03-29', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-29T10:24:00.000Z', '2025-03-29T10:24:00.000Z'
),
(
  'hist2025_pay_a7fbab6ca6196727', 'treatment', '11th Payment', '11th Payment', 'hist2025_8acaa23ad2c62803', '9593382924', 'Kishanganj', 'ASIT HALDAR',
  '2025-04-02', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-02T10:25:00.000Z', '2025-04-02T10:25:00.000Z'
),
(
  'hist2025_pay_f1e557f275d8db09', 'treatment', '12th Payment', '12th Payment', 'hist2025_8acaa23ad2c62803', '9593382924', 'Kishanganj', 'ASIT HALDAR',
  '2025-04-17', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-17T10:26:00.000Z', '2025-04-17T10:26:00.000Z'
);
