-- ২০২৫ শিট ধাপ ৪ -- ব্যাচ 7/7 (রোগী 100-107, মোট 8)
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
  'hist2025_7e5d875290d39cb8', 'KNE-22112025-001', '2025-11-22', '2025-11-22', '2025-11-22',
  'SAHID', '8864007348', '', 'Kishanganj', '35', 'Male',
  'PADAM PUR, PADAMPUR, DIGHAL BANK, KISHANGANJ', 'Piles', '24000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-22T10:00:00.000Z', '2025-11-22T10:00:00.000Z'
),
(
  'hist2025_af8f3797b8caeef8', 'KNE-28112025-001', '2025-11-28', '2025-11-28', '2025-11-28',
  'HAMEDA BANU', '9693490945', '', 'Kishanganj', '25', 'Male',
  'PURANA KHAGRA, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-28T10:00:00.000Z', '2025-11-28T10:00:00.000Z'
),
(
  'hist2025_d0f1991c2fe0eeed', 'KNE-13122025-001', '2025-12-13', '2025-12-13', '2025-12-13',
  'CHANDRAMOHAN', '9801016307', '', 'Kishanganj', '40', 'Male',
  'ORLAHA, ORLAHA, BARLA KOTHI, PURNEA', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:00:00.000Z', '2025-12-13T10:00:00.000Z'
),
(
  'hist2025_b5a56ea4f8bd567c', 'KNE-22122025-001', '2025-12-22', '2025-12-22', '2025-12-22',
  'NAJRUL ALAM', '9741275034', '', 'Kishanganj', '30', 'Male',
  'TOPAMARI, KOCHADHAMAN, KOCHADHAMAN, KISHANGANJ', 'Fistula', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-22T10:00:00.000Z', '2025-12-22T10:00:00.000Z'
),
(
  'hist2025_abad7c8f48a3a82f', 'KNE-22122025-002', '2025-12-22', '2025-12-22', '2025-12-22',
  'REKHA SING', '7484054594', '', 'Kishanganj', '50', 'Female',
  'DUMORIYA BHATTA, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '50000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-22T10:00:00.000Z', '2025-12-22T10:00:00.000Z'
),
(
  'hist2025_4f8c212c5aff57d4', 'KNE-24122025-001', '2025-12-24', '2025-12-24', '2025-12-24',
  'MUSARAF ALAM', '8670181777', '', 'Kishanganj', '24', 'Male',
  'ISLAMPUR, AMALJHARI, ISLAMPUR, UTTAR DINAJPUR', 'Piles', '150000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-24T10:00:00.000Z', '2025-12-24T10:00:00.000Z'
),
(
  'hist2025_f2fc98ae3434c92c', 'KNE-24122025-002', '2025-12-24', '2025-12-24', '2025-12-24',
  'GAYTRI DEVI', '9934966794', '', 'Kishanganj', '50', 'Female',
  'KAJLAMANI, MODHARHAAT, KOCHADHAMAN, KISHANGANJ', 'Piles', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-24T10:00:00.000Z', '2025-12-24T10:00:00.000Z'
),
(
  'hist2025_cf9c35d0ffb07984', 'KNE-24122025-003', '2025-12-24', '2025-12-24', '2025-12-24',
  'SEHZAD SAMDANI', '9973811074', '', 'Kishanganj', '35', 'Male',
  'TALBARI, KASHIBARI, KOCHADHAMAN, KISHANGANJ', 'Piles', '42000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-24T10:00:00.000Z', '2025-12-24T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'hist2025_pay_54fcc7beb584482c', 'treatment', 'Advance', 'Advance', 'hist2025_7e5d875290d39cb8', '8864007348', 'Kishanganj', 'SAHID',
  '2025-11-22', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-22T10:15:00.000Z', '2025-11-22T10:15:00.000Z'
),
(
  'hist2025_pay_a4711a2a138cd2c2', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_7e5d875290d39cb8', '8864007348', 'Kishanganj', 'SAHID',
  '2025-11-26', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-26T10:16:00.000Z', '2025-11-26T10:16:00.000Z'
),
(
  'hist2025_pay_954b354b784f6fdc', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_7e5d875290d39cb8', '8864007348', 'Kishanganj', 'SAHID',
  '2025-11-29', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-29T10:17:00.000Z', '2025-11-29T10:17:00.000Z'
),
(
  'hist2025_pay_fae8c73d14c0a3b3', 'treatment', 'Advance', 'Advance', 'hist2025_af8f3797b8caeef8', '9693490945', 'Kishanganj', 'HAMEDA BANU',
  '2025-11-28', '500', 'CASH', 'Historical import (2025 Google Sheet) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-28T10:15:00.000Z', '2025-11-28T10:15:00.000Z'
),
(
  'hist2025_pay_073c1a26b62d29f2', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_af8f3797b8caeef8', '9693490945', 'Kishanganj', 'HAMEDA BANU',
  '2025-11-28', '6000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-28T10:16:00.000Z', '2025-11-28T10:16:00.000Z'
),
(
  'hist2025_pay_214ded43696a9fc0', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_af8f3797b8caeef8', '9693490945', 'Kishanganj', 'HAMEDA BANU',
  '2025-11-29', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-29T10:17:00.000Z', '2025-11-29T10:17:00.000Z'
),
(
  'hist2025_pay_f27363b8e95bd537', 'treatment', '4th Payment', '4th Payment', 'hist2025_af8f3797b8caeef8', '9693490945', 'Kishanganj', 'HAMEDA BANU',
  '2025-12-10', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-10T10:18:00.000Z', '2025-12-10T10:18:00.000Z'
),
(
  'hist2025_pay_fe0aeb3a2c1cd0ba', 'treatment', '5th Payment', '5th Payment', 'hist2025_af8f3797b8caeef8', '9693490945', 'Kishanganj', 'HAMEDA BANU',
  '2025-12-17', '2500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-17T10:19:00.000Z', '2025-12-17T10:19:00.000Z'
),
(
  'hist2025_pay_416379667158dcb7', 'treatment', '6th Payment', '6th Payment', 'hist2025_af8f3797b8caeef8', '9693490945', 'Kishanganj', 'HAMEDA BANU',
  '2025-12-20', '2500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-20T10:20:00.000Z', '2025-12-20T10:20:00.000Z'
),
(
  'hist2025_pay_e8fed52218776b7e', 'treatment', '7th Payment', '7th Payment', 'hist2025_af8f3797b8caeef8', '9693490945', 'Kishanganj', 'HAMEDA BANU',
  '2025-12-24', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-24T10:21:00.000Z', '2025-12-24T10:21:00.000Z'
),
(
  'hist2025_pay_366fc1cbc5c58ab3', 'treatment', '8th Payment', '8th Payment', 'hist2025_af8f3797b8caeef8', '9693490945', 'Kishanganj', 'HAMEDA BANU',
  '2025-12-27', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:22:00.000Z', '2025-12-27T10:22:00.000Z'
),
(
  'hist2025_pay_fa94314d7d229785', 'treatment', 'Advance', 'Advance', 'hist2025_d0f1991c2fe0eeed', '9801016307', 'Kishanganj', 'CHANDRAMOHAN',
  '2025-12-13', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:15:00.000Z', '2025-12-13T10:15:00.000Z'
),
(
  'hist2025_pay_811c3195c6fe345b', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_d0f1991c2fe0eeed', '9801016307', 'Kishanganj', 'CHANDRAMOHAN',
  '2025-12-17', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-17T10:16:00.000Z', '2025-12-17T10:16:00.000Z'
),
(
  'hist2025_pay_41642c1c0ab148ee', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_d0f1991c2fe0eeed', '9801016307', 'Kishanganj', 'CHANDRAMOHAN',
  '2025-12-24', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-24T10:17:00.000Z', '2025-12-24T10:17:00.000Z'
),
(
  'hist2025_pay_4c943867b041ab88', 'treatment', 'Advance', 'Advance', 'hist2025_b5a56ea4f8bd567c', '9741275034', 'Kishanganj', 'NAJRUL ALAM',
  '2025-12-22', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-22T10:15:00.000Z', '2025-12-22T10:15:00.000Z'
),
(
  'hist2025_pay_424a82be8d54901a', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_b5a56ea4f8bd567c', '9741275034', 'Kishanganj', 'NAJRUL ALAM',
  '2025-12-23', '11000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-23T10:16:00.000Z', '2025-12-23T10:16:00.000Z'
),
(
  'hist2025_pay_ba2fdd45c1fcd7b7', 'treatment', 'Advance', 'Advance', 'hist2025_abad7c8f48a3a82f', '7484054594', 'Kishanganj', 'REKHA SING',
  '2025-12-22', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-22T10:15:00.000Z', '2025-12-22T10:15:00.000Z'
),
(
  'hist2025_pay_e3d909fe4a8ce89e', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_abad7c8f48a3a82f', '7484054594', 'Kishanganj', 'REKHA SING',
  '2025-12-24', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-24T10:16:00.000Z', '2025-12-24T10:16:00.000Z'
),
(
  'hist2025_pay_5abf4547acc91484', 'treatment', 'Advance', 'Advance', 'hist2025_4f8c212c5aff57d4', '8670181777', 'Kishanganj', 'MUSARAF ALAM',
  '2025-12-24', '30000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-24T10:15:00.000Z', '2025-12-24T10:15:00.000Z'
),
(
  'hist2025_pay_69eb61932f74759d', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_4f8c212c5aff57d4', '8670181777', 'Kishanganj', 'MUSARAF ALAM',
  '2025-12-27', '30000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:16:00.000Z', '2025-12-27T10:16:00.000Z'
),
(
  'hist2025_pay_14992d376afd6f31', 'treatment', 'Advance', 'Advance', 'hist2025_f2fc98ae3434c92c', '9934966794', 'Kishanganj', 'GAYTRI DEVI',
  '2025-12-24', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-24T10:15:00.000Z', '2025-12-24T10:15:00.000Z'
),
(
  'hist2025_pay_df2f6ba73e373816', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_f2fc98ae3434c92c', '9934966794', 'Kishanganj', 'GAYTRI DEVI',
  '2025-12-27', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:16:00.000Z', '2025-12-27T10:16:00.000Z'
),
(
  'hist2025_pay_f1ebe437b4d0cf42', 'treatment', 'Advance', 'Advance', 'hist2025_cf9c35d0ffb07984', '9973811074', 'Kishanganj', 'SEHZAD SAMDANI',
  '2025-12-24', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-24T10:15:00.000Z', '2025-12-24T10:15:00.000Z'
),
(
  'hist2025_pay_00f34690d41e7df0', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_cf9c35d0ffb07984', '9973811074', 'Kishanganj', 'SEHZAD SAMDANI',
  '2025-12-27', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:16:00.000Z', '2025-12-27T10:16:00.000Z'
);
