-- ২০২৫ শিট ধাপ ৪ -- ব্যাচ 6/7 (রোগী 82-99, মোট 18)
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
  'hist2025_50261a41cf73942e', 'KNE-05102025-001', '2025-10-05', '2025-10-05', '2025-10-05',
  'MURSLIM ALAM', '6296133324', '', 'Kishanganj', '25', 'Male',
  'DIGLI, MAZLISPUR, GOALPOKHER, UD', 'Fissure', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-05T10:00:00.000Z', '2025-10-05T10:00:00.000Z'
),
(
  'hist2025_833e01e27316065e', 'KNE-06102025-001', '2025-10-06', '2025-10-06', '2025-10-06',
  'SURESH KR SINGH', '7858868682', '', 'Kishanganj', '31', 'Male',
  'GHAMBHIRGANJ, BHARATPUR, SUKHANI, KISHANGANJ', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-06T10:00:00.000Z', '2025-10-06T10:00:00.000Z'
),
(
  'hist2025_9428803fbfe88ea3', 'KNE-11102025-001', '2025-10-11', '2025-10-11', '2025-10-11',
  'ALIM UDDIN', '7091337563', '', 'Kishanganj', '12', 'Male',
  'PADAMPUR, PADAMPUR, GARBHANDANGA, KISHANGANJ', 'Piles', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:00:00.000Z', '2025-10-11T10:00:00.000Z'
),
(
  'hist2025_08355e7f4f990e0d', 'KNE-22102025-001', '2025-10-22', '2025-10-22', '2025-10-22',
  'MONJIRA KHATUN', '7369084326', '', 'Kishanganj', '20', 'Female',
  'BEHERATOLA, GACHHPARA, KISHANGANJ, KISHANGANJ', 'Piles', '41000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-22T10:00:00.000Z', '2025-10-22T10:00:00.000Z'
),
(
  'hist2025_79b886f0512788b0', 'KNE-22102025-002', '2025-10-22', '2025-10-22', '2025-10-22',
  'TUSHAR KANTI', '8340333653', '', 'Kishanganj', '42', 'Male',
  'RUIDHASA, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Fistula', '54000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-22T10:00:00.000Z', '2025-10-22T10:00:00.000Z'
),
(
  'hist2025_4acda601ba4cb971', 'KNE-22102025-003', '2025-10-22', '2025-10-22', '2025-10-22',
  'AMAL KUMAR', '8809972037', '', 'Kishanganj', '27', 'Male',
  'KHAGRA, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-22T10:00:00.000Z', '2025-10-22T10:00:00.000Z'
),
(
  'hist2025_256c2f7db876d45b', 'KNE-24102025-001', '2025-10-24', '2025-10-24', '2025-10-24',
  'MD ASRAFUL', '7904328499', '', 'Kishanganj', '24', 'Male',
  'CHOUKAI CHANDONTOLA, GALIYA, CHAKULIYA, U.D', 'Fistula', '29750',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-24T10:00:00.000Z', '2025-10-24T10:00:00.000Z'
),
(
  'hist2025_7f94acee42316ef1', 'KNE-29102025-001', '2025-10-29', '2025-10-29', '2025-10-29',
  'SARFARAZ', '8084633866', '', 'Kishanganj', '28', 'Male',
  'KATHALBARI, BANGAUA, BAHADURGANJ, KISHANGANJ', 'Piles', '19000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-29T10:00:00.000Z', '2025-10-29T10:00:00.000Z'
),
(
  'hist2025_42937563c7a4a8f4', 'KNE-06112025-001', '2025-11-06', '2025-11-06', '2025-11-06',
  'SAMIRUDDIN', '9733237451', '', 'Kishanganj', '42', 'Male',
  'JAKIRBASTI, HATKHOLA, CHAKULIYA, U.D', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-06T10:00:00.000Z', '2025-11-06T10:00:00.000Z'
),
(
  'hist2025_e7d86a0278aa9418', 'KNE-06112025-002', '2025-11-06', '2025-11-06', '2025-11-06',
  'RIHANA KHATOON', '9572967488', '', 'Kishanganj', '45', 'Female',
  'BELUWA, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '55000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-06T10:00:00.000Z', '2025-11-06T10:00:00.000Z'
),
(
  'hist2025_2beff50993684290', 'KNE-08112025-001', '2025-11-08', '2025-11-08', '2025-11-08',
  'NAYEEM AKHTER', '9647452857', '', 'Kishanganj', '25', 'Female',
  'SAMASTPUR, VIDYANANDPUR, CHAKULIA, UD', 'Gupt Rog', '24050',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:00:00.000Z', '2025-11-08T10:00:00.000Z'
),
(
  'hist2025_daf0bdf5f0c045a2', 'KNE-12112025-001', '2025-11-12', '2025-11-12', '2025-11-12',
  'MOHMAD IRFAN', '7250207063', '', 'Kishanganj', '23', 'Male',
  'TELIVITA, DIGHALBANK, DIGHALBANK, KISHANGANJ', 'Fistula', '37000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-12T10:00:00.000Z', '2025-11-12T10:00:00.000Z'
),
(
  'hist2025_cff877da7bc33de2', 'KNE-14112025-001', '2025-11-14', '2025-11-14', '2025-11-14',
  'RAHUL CHODHARY', '7903438083', '', 'Kishanganj', '33', 'Male',
  'DUMARIA, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Other', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-14T10:00:00.000Z', '2025-11-14T10:00:00.000Z'
),
(
  'hist2025_5a21fbb61e1852c2', 'KNE-15112025-001', '2025-11-15', '2025-11-15', '2025-11-15',
  'BAHADUR LAL', '6203686924', '', 'Kishanganj', '72', 'Male',
  'POWAKHALI, SARAYGURI, POWAKHALI, KISHANGANJ', 'Piles', '45000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:00:00.000Z', '2025-11-15T10:00:00.000Z'
),
(
  'hist2025_1ce78de2879d1882', 'KNE-17112025-001', '2025-11-17', '2025-11-17', '2025-11-17',
  'NASHIM', '9931647477', '', 'Kishanganj', '54', 'Male',
  'BELUYA, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '28000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-17T10:00:00.000Z', '2025-11-17T10:00:00.000Z'
),
(
  'hist2025_0a88f305f63eb9dd', 'KNE-17112025-002', '2025-11-17', '2025-11-17', '2025-11-17',
  'MD DABIR ALAM', '9311088607', '', 'Kishanganj', '22', 'Male',
  'BHOGDABAR, BHAVINIGANJ, THAKURGANJ, KISHANGANJ', 'Fissure', '26000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-17T10:00:00.000Z', '2025-11-17T10:00:00.000Z'
),
(
  'hist2025_02ccc85535cc2865', 'KNE-18112025-001', '2025-11-18', '2025-11-18', '2025-11-18',
  'SALIMUDDIN', '9679159055', '', 'Kishanganj', '64', 'Male',
  'HASKUNDA, PANJIPARA, GOYALPOKHAR, UTTAR DINAJPUR', 'Piles', '28000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-18T10:00:00.000Z', '2025-11-18T10:00:00.000Z'
),
(
  'hist2025_11eb702a625a0049', 'KNE-18112025-002', '2025-11-18', '2025-11-18', '2025-11-18',
  'JAGADIS MAJUMDAR', '6296824807', '', 'Kishanganj', '34', 'Male',
  'KANKI, MAZLISPUR, CHAKULIYA, UTTAR DINAJPUR', 'Piles', '38000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-18T10:00:00.000Z', '2025-11-18T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'hist2025_pay_e52c04a0f1d06018', 'treatment', 'Advance', 'Advance', 'hist2025_50261a41cf73942e', '6296133324', 'Kishanganj', 'MURSLIM ALAM',
  '2025-10-05', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-05T10:15:00.000Z', '2025-10-05T10:15:00.000Z'
),
(
  'hist2025_pay_5e02943b07a5776e', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_50261a41cf73942e', '6296133324', 'Kishanganj', 'MURSLIM ALAM',
  '2025-10-09', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-09T10:16:00.000Z', '2025-10-09T10:16:00.000Z'
),
(
  'hist2025_pay_3232e8988ffd85e9', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_50261a41cf73942e', '6296133324', 'Kishanganj', 'MURSLIM ALAM',
  '2025-10-15', '8000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-15T10:17:00.000Z', '2025-10-15T10:17:00.000Z'
),
(
  'hist2025_pay_2ca4b9286fefa609', 'treatment', '4th Payment', '4th Payment', 'hist2025_50261a41cf73942e', '6296133324', 'Kishanganj', 'MURSLIM ALAM',
  '2025-11-08', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:18:00.000Z', '2025-11-08T10:18:00.000Z'
),
(
  'hist2025_pay_3307d9fdfc119f78', 'treatment', 'Advance', 'Advance', 'hist2025_833e01e27316065e', '7858868682', 'Kishanganj', 'SURESH KR SINGH',
  '2025-10-06', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-06T10:15:00.000Z', '2025-10-06T10:15:00.000Z'
),
(
  'hist2025_pay_d4013c32379adc33', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_833e01e27316065e', '7858868682', 'Kishanganj', 'SURESH KR SINGH',
  '2025-10-08', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-08T10:16:00.000Z', '2025-10-08T10:16:00.000Z'
),
(
  'hist2025_pay_4b0b5a35acc8c9f4', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_833e01e27316065e', '7858868682', 'Kishanganj', 'SURESH KR SINGH',
  '2025-10-11', '5500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:17:00.000Z', '2025-10-11T10:17:00.000Z'
),
(
  'hist2025_pay_9e2e3bca107be9ef', 'treatment', '4th Payment', '4th Payment', 'hist2025_833e01e27316065e', '7858868682', 'Kishanganj', 'SURESH KR SINGH',
  '2025-10-14', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-14T10:18:00.000Z', '2025-10-14T10:18:00.000Z'
),
(
  'hist2025_pay_959600ddf21ca8fd', 'treatment', '5th Payment', '5th Payment', 'hist2025_833e01e27316065e', '7858868682', 'Kishanganj', 'SURESH KR SINGH',
  '2025-10-18', '3500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:19:00.000Z', '2025-10-18T10:19:00.000Z'
),
(
  'hist2025_pay_468f05a8781698c1', 'treatment', '6th Payment', '6th Payment', 'hist2025_833e01e27316065e', '7858868682', 'Kishanganj', 'SURESH KR SINGH',
  '2025-10-23', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-23T10:20:00.000Z', '2025-10-23T10:20:00.000Z'
),
(
  'hist2025_pay_6b569d13101ad3ef', 'treatment', '7th Payment', '7th Payment', 'hist2025_833e01e27316065e', '7858868682', 'Kishanganj', 'SURESH KR SINGH',
  '2025-10-25', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-25T10:21:00.000Z', '2025-10-25T10:21:00.000Z'
),
(
  'hist2025_pay_6c0ee734dd7765fc', 'treatment', '8th Payment', '8th Payment', 'hist2025_833e01e27316065e', '7858868682', 'Kishanganj', 'SURESH KR SINGH',
  '2025-11-12', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-12T10:22:00.000Z', '2025-11-12T10:22:00.000Z'
),
(
  'hist2025_pay_0e60b9cd4bc6ae90', 'treatment', 'Advance', 'Advance', 'hist2025_9428803fbfe88ea3', '7091337563', 'Kishanganj', 'ALIM UDDIN',
  '2025-10-11', '8000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:15:00.000Z', '2025-10-11T10:15:00.000Z'
),
(
  'hist2025_pay_3c7b85f8e9291353', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_9428803fbfe88ea3', '7091337563', 'Kishanganj', 'ALIM UDDIN',
  '2025-10-15', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-15T10:16:00.000Z', '2025-10-15T10:16:00.000Z'
),
(
  'hist2025_pay_ef3056188d5fbb31', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_9428803fbfe88ea3', '7091337563', 'Kishanganj', 'ALIM UDDIN',
  '2025-10-22', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-22T10:17:00.000Z', '2025-10-22T10:17:00.000Z'
),
(
  'hist2025_pay_5b8083af345e6171', 'treatment', '4th Payment', '4th Payment', 'hist2025_9428803fbfe88ea3', '7091337563', 'Kishanganj', 'ALIM UDDIN',
  '2025-11-05', '6000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-05T10:18:00.000Z', '2025-11-05T10:18:00.000Z'
),
(
  'hist2025_pay_350505ab0afd6748', 'treatment', 'Advance', 'Advance', 'hist2025_08355e7f4f990e0d', '7369084326', 'Kishanganj', 'MONJIRA KHATUN',
  '2025-10-22', '1000', 'CASH', 'Historical import (2025 Google Sheet) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-22T10:15:00.000Z', '2025-10-22T10:15:00.000Z'
),
(
  'hist2025_pay_ee5b3dead572b23a', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_08355e7f4f990e0d', '7369084326', 'Kishanganj', 'MONJIRA KHATUN',
  '2025-10-22', '6000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-22T10:16:00.000Z', '2025-10-22T10:16:00.000Z'
),
(
  'hist2025_pay_b8fc90d62f87c891', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_08355e7f4f990e0d', '7369084326', 'Kishanganj', 'MONJIRA KHATUN',
  '2025-10-25', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-25T10:17:00.000Z', '2025-10-25T10:17:00.000Z'
),
(
  'hist2025_pay_384c9058e6234bca', 'treatment', '4th Payment', '4th Payment', 'hist2025_08355e7f4f990e0d', '7369084326', 'Kishanganj', 'MONJIRA KHATUN',
  '2025-11-01', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-01T10:18:00.000Z', '2025-11-01T10:18:00.000Z'
),
(
  'hist2025_pay_459d21e89e1a296b', 'treatment', '5th Payment', '5th Payment', 'hist2025_08355e7f4f990e0d', '7369084326', 'Kishanganj', 'MONJIRA KHATUN',
  '2025-11-08', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:19:00.000Z', '2025-11-08T10:19:00.000Z'
),
(
  'hist2025_pay_a073b86bd9902da2', 'treatment', '6th Payment', '6th Payment', 'hist2025_08355e7f4f990e0d', '7369084326', 'Kishanganj', 'MONJIRA KHATUN',
  '2025-11-12', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-12T10:20:00.000Z', '2025-11-12T10:20:00.000Z'
),
(
  'hist2025_pay_25b436cfea58e977', 'treatment', '7th Payment', '7th Payment', 'hist2025_08355e7f4f990e0d', '7369084326', 'Kishanganj', 'MONJIRA KHATUN',
  '2025-11-26', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-26T10:21:00.000Z', '2025-11-26T10:21:00.000Z'
),
(
  'hist2025_pay_2f088c489bfb97a2', 'treatment', 'Advance', 'Advance', 'hist2025_79b886f0512788b0', '8340333653', 'Kishanganj', 'TUSHAR KANTI',
  '2025-10-22', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-22T10:15:00.000Z', '2025-10-22T10:15:00.000Z'
),
(
  'hist2025_pay_0fb5e0cf826c4d61', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_79b886f0512788b0', '8340333653', 'Kishanganj', 'TUSHAR KANTI',
  '2025-10-25', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-25T10:16:00.000Z', '2025-10-25T10:16:00.000Z'
),
(
  'hist2025_pay_2dec6c51d7bc0fe4', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_79b886f0512788b0', '8340333653', 'Kishanganj', 'TUSHAR KANTI',
  '2025-10-29', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-29T10:17:00.000Z', '2025-10-29T10:17:00.000Z'
),
(
  'hist2025_pay_b7d784e7f0827b37', 'treatment', '4th Payment', '4th Payment', 'hist2025_79b886f0512788b0', '8340333653', 'Kishanganj', 'TUSHAR KANTI',
  '2025-11-01', '6000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-01T10:18:00.000Z', '2025-11-01T10:18:00.000Z'
),
(
  'hist2025_pay_b8920571d21e5619', 'treatment', '5th Payment', '5th Payment', 'hist2025_79b886f0512788b0', '8340333653', 'Kishanganj', 'TUSHAR KANTI',
  '2025-11-05', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-05T10:19:00.000Z', '2025-11-05T10:19:00.000Z'
),
(
  'hist2025_pay_b1188e87812e88f4', 'treatment', '6th Payment', '6th Payment', 'hist2025_79b886f0512788b0', '8340333653', 'Kishanganj', 'TUSHAR KANTI',
  '2025-11-08', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:20:00.000Z', '2025-11-08T10:20:00.000Z'
),
(
  'hist2025_pay_373bc43fe06f55bf', 'treatment', '7th Payment', '7th Payment', 'hist2025_79b886f0512788b0', '8340333653', 'Kishanganj', 'TUSHAR KANTI',
  '2025-11-15', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:21:00.000Z', '2025-11-15T10:21:00.000Z'
),
(
  'hist2025_pay_9c2ae55cc4168c3c', 'treatment', 'Advance', 'Advance', 'hist2025_4acda601ba4cb971', '8809972037', 'Kishanganj', 'AMAL KUMAR',
  '2025-10-22', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-22T10:15:00.000Z', '2025-10-22T10:15:00.000Z'
),
(
  'hist2025_pay_30b185526a8cc4ef', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_4acda601ba4cb971', '8809972037', 'Kishanganj', 'AMAL KUMAR',
  '2025-10-25', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-25T10:16:00.000Z', '2025-10-25T10:16:00.000Z'
),
(
  'hist2025_pay_59a0e10b93cd92ac', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_4acda601ba4cb971', '8809972037', 'Kishanganj', 'AMAL KUMAR',
  '2025-11-08', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:17:00.000Z', '2025-11-08T10:17:00.000Z'
),
(
  'hist2025_pay_e633b2c8d76ca700', 'treatment', '4th Payment', '4th Payment', 'hist2025_4acda601ba4cb971', '8809972037', 'Kishanganj', 'AMAL KUMAR',
  '2025-11-12', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-12T10:18:00.000Z', '2025-11-12T10:18:00.000Z'
),
(
  'hist2025_pay_5f882894bdd7dac1', 'treatment', '5th Payment', '5th Payment', 'hist2025_4acda601ba4cb971', '8809972037', 'Kishanganj', 'AMAL KUMAR',
  '2025-11-19', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-19T10:19:00.000Z', '2025-11-19T10:19:00.000Z'
),
(
  'hist2025_pay_fb984176fc787486', 'treatment', '6th Payment', '6th Payment', 'hist2025_4acda601ba4cb971', '8809972037', 'Kishanganj', 'AMAL KUMAR',
  '2025-12-06', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-06T10:20:00.000Z', '2025-12-06T10:20:00.000Z'
),
(
  'hist2025_pay_15677d8cfe39b3a3', 'treatment', 'Advance', 'Advance', 'hist2025_256c2f7db876d45b', '7904328499', 'Kishanganj', 'MD ASRAFUL',
  '2025-10-24', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-24T10:15:00.000Z', '2025-10-24T10:15:00.000Z'
),
(
  'hist2025_pay_5166f0c98653176f', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_256c2f7db876d45b', '7904328499', 'Kishanganj', 'MD ASRAFUL',
  '2025-10-29', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-29T10:16:00.000Z', '2025-10-29T10:16:00.000Z'
),
(
  'hist2025_pay_3d7e805f837f46e9', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_256c2f7db876d45b', '7904328499', 'Kishanganj', 'MD ASRAFUL',
  '2025-11-01', '8000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-01T10:17:00.000Z', '2025-11-01T10:17:00.000Z'
),
(
  'hist2025_pay_924d219e9c16ad58', 'treatment', '4th Payment', '4th Payment', 'hist2025_256c2f7db876d45b', '7904328499', 'Kishanganj', 'MD ASRAFUL',
  '2025-11-05', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-05T10:18:00.000Z', '2025-11-05T10:18:00.000Z'
),
(
  'hist2025_pay_467648bf0c28064c', 'treatment', '5th Payment', '5th Payment', 'hist2025_256c2f7db876d45b', '7904328499', 'Kishanganj', 'MD ASRAFUL',
  '2025-11-08', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:19:00.000Z', '2025-11-08T10:19:00.000Z'
),
(
  'hist2025_pay_b6be08e088ca14f8', 'treatment', '6th Payment', '6th Payment', 'hist2025_256c2f7db876d45b', '7904328499', 'Kishanganj', 'MD ASRAFUL',
  '2025-11-12', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-12T10:20:00.000Z', '2025-11-12T10:20:00.000Z'
),
(
  'hist2025_pay_e4ed4dcaeec11c88', 'treatment', '7th Payment', '7th Payment', 'hist2025_256c2f7db876d45b', '7904328499', 'Kishanganj', 'MD ASRAFUL',
  '2025-11-14', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-14T10:21:00.000Z', '2025-11-14T10:21:00.000Z'
),
(
  'hist2025_pay_79e472e43dec568e', 'treatment', '8th Payment', '8th Payment', 'hist2025_256c2f7db876d45b', '7904328499', 'Kishanganj', 'MD ASRAFUL',
  '2025-11-15', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:22:00.000Z', '2025-11-15T10:22:00.000Z'
),
(
  'hist2025_pay_590ec3b0ae8b761b', 'treatment', '9th Payment', '9th Payment', 'hist2025_256c2f7db876d45b', '7904328499', 'Kishanganj', 'MD ASRAFUL',
  '2025-11-19', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-19T10:23:00.000Z', '2025-11-19T10:23:00.000Z'
),
(
  'hist2025_pay_47b2a9b3bfbb7fdb', 'treatment', '10th Payment', '10th Payment', 'hist2025_256c2f7db876d45b', '7904328499', 'Kishanganj', 'MD ASRAFUL',
  '2025-11-26', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-26T10:24:00.000Z', '2025-11-26T10:24:00.000Z'
),
(
  'hist2025_pay_7c3ccf8df2b64cbc', 'treatment', '11th Payment', '11th Payment', 'hist2025_256c2f7db876d45b', '7904328499', 'Kishanganj', 'MD ASRAFUL',
  '2025-12-03', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-03T10:25:00.000Z', '2025-12-03T10:25:00.000Z'
),
(
  'hist2025_pay_d56f1b0cb10b8f35', 'treatment', '12th Payment', '12th Payment', 'hist2025_256c2f7db876d45b', '7904328499', 'Kishanganj', 'MD ASRAFUL',
  '2025-12-12', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-12T10:26:00.000Z', '2025-12-12T10:26:00.000Z'
),
(
  'hist2025_pay_4ed85547c93a3226', 'treatment', 'Advance', 'Advance', 'hist2025_7f94acee42316ef1', '8084633866', 'Kishanganj', 'SARFARAZ',
  '2025-10-29', '2500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-29T10:15:00.000Z', '2025-10-29T10:15:00.000Z'
),
(
  'hist2025_pay_e522659b4e4835a7', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_7f94acee42316ef1', '8084633866', 'Kishanganj', 'SARFARAZ',
  '2025-11-01', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-01T10:16:00.000Z', '2025-11-01T10:16:00.000Z'
),
(
  'hist2025_pay_aaef64e26555e852', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_7f94acee42316ef1', '8084633866', 'Kishanganj', 'SARFARAZ',
  '2025-11-05', '2500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-05T10:17:00.000Z', '2025-11-05T10:17:00.000Z'
),
(
  'hist2025_pay_9706c3c1e53b479d', 'treatment', '4th Payment', '4th Payment', 'hist2025_7f94acee42316ef1', '8084633866', 'Kishanganj', 'SARFARAZ',
  '2025-11-12', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-12T10:18:00.000Z', '2025-11-12T10:18:00.000Z'
),
(
  'hist2025_pay_59fd0f48bba280c1', 'treatment', '5th Payment', '5th Payment', 'hist2025_7f94acee42316ef1', '8084633866', 'Kishanganj', 'SARFARAZ',
  '2025-11-14', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-14T10:19:00.000Z', '2025-11-14T10:19:00.000Z'
),
(
  'hist2025_pay_0498200727f2602b', 'treatment', '6th Payment', '6th Payment', 'hist2025_7f94acee42316ef1', '8084633866', 'Kishanganj', 'SARFARAZ',
  '2025-11-19', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-19T10:20:00.000Z', '2025-11-19T10:20:00.000Z'
),
(
  'hist2025_pay_3392585fca4475e9', 'treatment', '7th Payment', '7th Payment', 'hist2025_7f94acee42316ef1', '8084633866', 'Kishanganj', 'SARFARAZ',
  '2025-11-26', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-26T10:21:00.000Z', '2025-11-26T10:21:00.000Z'
),
(
  'hist2025_pay_f286c927f8bd6489', 'treatment', 'Advance', 'Advance', 'hist2025_42937563c7a4a8f4', '9733237451', 'Kishanganj', 'SAMIRUDDIN',
  '2025-11-06', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-06T10:15:00.000Z', '2025-11-06T10:15:00.000Z'
),
(
  'hist2025_pay_5a34e8f8726084b2', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_42937563c7a4a8f4', '9733237451', 'Kishanganj', 'SAMIRUDDIN',
  '2025-11-12', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-12T10:16:00.000Z', '2025-11-12T10:16:00.000Z'
),
(
  'hist2025_pay_9594c2f2acdf5b38', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_42937563c7a4a8f4', '9733237451', 'Kishanganj', 'SAMIRUDDIN',
  '2025-11-19', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-19T10:17:00.000Z', '2025-11-19T10:17:00.000Z'
),
(
  'hist2025_pay_48a40abb188ac3c6', 'treatment', 'Advance', 'Advance', 'hist2025_e7d86a0278aa9418', '9572967488', 'Kishanganj', 'RIHANA KHATOON',
  '2025-11-12', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-12T10:15:00.000Z', '2025-11-12T10:15:00.000Z'
),
(
  'hist2025_pay_985d18a6db556baf', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_e7d86a0278aa9418', '9572967488', 'Kishanganj', 'RIHANA KHATOON',
  '2025-11-15', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:16:00.000Z', '2025-11-15T10:16:00.000Z'
),
(
  'hist2025_pay_649dcd2f64c273e2', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_e7d86a0278aa9418', '9572967488', 'Kishanganj', 'RIHANA KHATOON',
  '2025-11-19', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-19T10:17:00.000Z', '2025-11-19T10:17:00.000Z'
),
(
  'hist2025_pay_7bb19563c6125c02', 'treatment', '4th Payment', '4th Payment', 'hist2025_e7d86a0278aa9418', '9572967488', 'Kishanganj', 'RIHANA KHATOON',
  '2025-11-22', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-22T10:18:00.000Z', '2025-11-22T10:18:00.000Z'
),
(
  'hist2025_pay_5ed71728c510b9c9', 'treatment', '5th Payment', '5th Payment', 'hist2025_e7d86a0278aa9418', '9572967488', 'Kishanganj', 'RIHANA KHATOON',
  '2025-11-26', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-26T10:19:00.000Z', '2025-11-26T10:19:00.000Z'
),
(
  'hist2025_pay_03fa781c3fb4fb19', 'treatment', '6th Payment', '6th Payment', 'hist2025_e7d86a0278aa9418', '9572967488', 'Kishanganj', 'RIHANA KHATOON',
  '2025-11-29', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-29T10:20:00.000Z', '2025-11-29T10:20:00.000Z'
),
(
  'hist2025_pay_70a8065e8cba9bde', 'treatment', '7th Payment', '7th Payment', 'hist2025_e7d86a0278aa9418', '9572967488', 'Kishanganj', 'RIHANA KHATOON',
  '2025-12-03', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-03T10:21:00.000Z', '2025-12-03T10:21:00.000Z'
),
(
  'hist2025_pay_ce63e1d8e2f9eaaf', 'treatment', '8th Payment', '8th Payment', 'hist2025_e7d86a0278aa9418', '9572967488', 'Kishanganj', 'RIHANA KHATOON',
  '2025-12-06', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-06T10:22:00.000Z', '2025-12-06T10:22:00.000Z'
),
(
  'hist2025_pay_bdaa6063cfd7ae99', 'treatment', '9th Payment', '9th Payment', 'hist2025_e7d86a0278aa9418', '9572967488', 'Kishanganj', 'RIHANA KHATOON',
  '2025-12-13', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:23:00.000Z', '2025-12-13T10:23:00.000Z'
),
(
  'hist2025_pay_f0dc9b119ff3f6f7', 'treatment', '10th Payment', '10th Payment', 'hist2025_e7d86a0278aa9418', '9572967488', 'Kishanganj', 'RIHANA KHATOON',
  '2025-12-17', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-17T10:24:00.000Z', '2025-12-17T10:24:00.000Z'
),
(
  'hist2025_pay_7849f5900a2570ab', 'treatment', '11th Payment', '11th Payment', 'hist2025_e7d86a0278aa9418', '9572967488', 'Kishanganj', 'RIHANA KHATOON',
  '2025-12-20', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-20T10:25:00.000Z', '2025-12-20T10:25:00.000Z'
),
(
  'hist2025_pay_fb4e365ca3e5bb22', 'treatment', '12th Payment', '12th Payment', 'hist2025_e7d86a0278aa9418', '9572967488', 'Kishanganj', 'RIHANA KHATOON',
  '2025-12-24', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-24T10:26:00.000Z', '2025-12-24T10:26:00.000Z'
),
(
  'hist2025_pay_f6b4d11040e366a7', 'treatment', '13th Payment', '13th Payment', 'hist2025_e7d86a0278aa9418', '9572967488', 'Kishanganj', 'RIHANA KHATOON',
  '2025-12-27', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:27:00.000Z', '2025-12-27T10:27:00.000Z'
),
(
  'hist2025_pay_595406f05b0c7a04', 'treatment', 'Advance', 'Advance', 'hist2025_2beff50993684290', '9647452857', 'Kishanganj', 'NAYEEM AKHTER',
  '2025-12-08', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-08T10:15:00.000Z', '2025-12-08T10:15:00.000Z'
),
(
  'hist2025_pay_82bf78540931a08f', 'treatment', 'Advance', 'Advance', 'hist2025_daf0bdf5f0c045a2', '7250207063', 'Kishanganj', 'MOHMAD IRFAN',
  '2025-11-17', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-17T10:15:00.000Z', '2025-11-17T10:15:00.000Z'
),
(
  'hist2025_pay_f620de66cb2c8f19', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_daf0bdf5f0c045a2', '7250207063', 'Kishanganj', 'MOHMAD IRFAN',
  '2025-11-19', '20000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-19T10:16:00.000Z', '2025-11-19T10:16:00.000Z'
),
(
  'hist2025_pay_4acb2360dc516509', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_daf0bdf5f0c045a2', '7250207063', 'Kishanganj', 'MOHMAD IRFAN',
  '2025-12-03', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-03T10:17:00.000Z', '2025-12-03T10:17:00.000Z'
),
(
  'hist2025_pay_a74bfe6d9622e9f2', 'treatment', '4th Payment', '4th Payment', 'hist2025_daf0bdf5f0c045a2', '7250207063', 'Kishanganj', 'MOHMAD IRFAN',
  '2025-12-25', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-25T10:18:00.000Z', '2025-12-25T10:18:00.000Z'
),
(
  'hist2025_pay_245648ba1e97c9c4', 'treatment', 'Advance', 'Advance', 'hist2025_cff877da7bc33de2', '7903438083', 'Kishanganj', 'RAHUL CHODHARY',
  '2025-11-14', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-14T10:15:00.000Z', '2025-11-14T10:15:00.000Z'
),
(
  'hist2025_pay_343fc61e363efdd1', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_cff877da7bc33de2', '7903438083', 'Kishanganj', 'RAHUL CHODHARY',
  '2025-11-15', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:16:00.000Z', '2025-11-15T10:16:00.000Z'
),
(
  'hist2025_pay_2cb2e9688df0f305', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_cff877da7bc33de2', '7903438083', 'Kishanganj', 'RAHUL CHODHARY',
  '2025-11-19', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-19T10:17:00.000Z', '2025-11-19T10:17:00.000Z'
),
(
  'hist2025_pay_432531aee511ce82', 'treatment', 'Advance', 'Advance', 'hist2025_5a21fbb61e1852c2', '6203686924', 'Kishanganj', 'BAHADUR LAL',
  '2025-11-15', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:15:00.000Z', '2025-11-15T10:15:00.000Z'
),
(
  'hist2025_pay_c2f4115225287e94', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_5a21fbb61e1852c2', '6203686924', 'Kishanganj', 'BAHADUR LAL',
  '2025-11-19', '35000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-19T10:16:00.000Z', '2025-11-19T10:16:00.000Z'
),
(
  'hist2025_pay_a01e00e67d9ea384', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_5a21fbb61e1852c2', '6203686924', 'Kishanganj', 'BAHADUR LAL',
  '2025-11-22', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-22T10:17:00.000Z', '2025-11-22T10:17:00.000Z'
),
(
  'hist2025_pay_0c5d0620c067eb61', 'treatment', '4th Payment', '4th Payment', 'hist2025_5a21fbb61e1852c2', '6203686924', 'Kishanganj', 'BAHADUR LAL',
  '2025-11-26', '25000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-26T10:18:00.000Z', '2025-11-26T10:18:00.000Z'
),
(
  'hist2025_pay_f6d7af1b74d6db56', 'treatment', '5th Payment', '5th Payment', 'hist2025_5a21fbb61e1852c2', '6203686924', 'Kishanganj', 'BAHADUR LAL',
  '2025-11-29', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-29T10:19:00.000Z', '2025-11-29T10:19:00.000Z'
),
(
  'hist2025_pay_fa4dc8ec05ad1cd8', 'treatment', 'Advance', 'Advance', 'hist2025_1ce78de2879d1882', '9931647477', 'Kishanganj', 'NASHIM',
  '2025-11-17', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-17T10:15:00.000Z', '2025-11-17T10:15:00.000Z'
),
(
  'hist2025_pay_c58b244322f649b0', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_1ce78de2879d1882', '9931647477', 'Kishanganj', 'NASHIM',
  '2025-11-19', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-19T10:16:00.000Z', '2025-11-19T10:16:00.000Z'
),
(
  'hist2025_pay_b690232923daf1cf', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_1ce78de2879d1882', '9931647477', 'Kishanganj', 'NASHIM',
  '2025-11-24', '2500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-24T10:17:00.000Z', '2025-11-24T10:17:00.000Z'
),
(
  'hist2025_pay_3aca233a662ad714', 'treatment', '4th Payment', '4th Payment', 'hist2025_1ce78de2879d1882', '9931647477', 'Kishanganj', 'NASHIM',
  '2025-11-26', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-26T10:18:00.000Z', '2025-11-26T10:18:00.000Z'
),
(
  'hist2025_pay_556aa5a9e6371be1', 'treatment', '5th Payment', '5th Payment', 'hist2025_1ce78de2879d1882', '9931647477', 'Kishanganj', 'NASHIM',
  '2025-12-01', '2500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-01T10:19:00.000Z', '2025-12-01T10:19:00.000Z'
),
(
  'hist2025_pay_30ff87fb9f418ffc', 'treatment', '6th Payment', '6th Payment', 'hist2025_1ce78de2879d1882', '9931647477', 'Kishanganj', 'NASHIM',
  '2025-12-08', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-08T10:20:00.000Z', '2025-12-08T10:20:00.000Z'
),
(
  'hist2025_pay_5e6505470ec7e184', 'treatment', 'Advance', 'Advance', 'hist2025_0a88f305f63eb9dd', '9311088607', 'Kishanganj', 'MD DABIR ALAM',
  '2025-11-19', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-19T10:15:00.000Z', '2025-11-19T10:15:00.000Z'
),
(
  'hist2025_pay_97fbbe370fb7a485', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_0a88f305f63eb9dd', '9311088607', 'Kishanganj', 'MD DABIR ALAM',
  '2025-11-26', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-26T10:16:00.000Z', '2025-11-26T10:16:00.000Z'
),
(
  'hist2025_pay_c138b6756dc49d92', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_0a88f305f63eb9dd', '9311088607', 'Kishanganj', 'MD DABIR ALAM',
  '2025-11-29', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-29T10:17:00.000Z', '2025-11-29T10:17:00.000Z'
),
(
  'hist2025_pay_d91cb50776f33ab1', 'treatment', '4th Payment', '4th Payment', 'hist2025_0a88f305f63eb9dd', '9311088607', 'Kishanganj', 'MD DABIR ALAM',
  '2025-12-03', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-03T10:18:00.000Z', '2025-12-03T10:18:00.000Z'
),
(
  'hist2025_pay_d0a85bf812cbecd8', 'treatment', '5th Payment', '5th Payment', 'hist2025_0a88f305f63eb9dd', '9311088607', 'Kishanganj', 'MD DABIR ALAM',
  '2025-12-08', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-08T10:19:00.000Z', '2025-12-08T10:19:00.000Z'
),
(
  'hist2025_pay_0d7ec81f9d78c81d', 'treatment', '6th Payment', '6th Payment', 'hist2025_0a88f305f63eb9dd', '9311088607', 'Kishanganj', 'MD DABIR ALAM',
  '2025-12-19', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-19T10:20:00.000Z', '2025-12-19T10:20:00.000Z'
),
(
  'hist2025_pay_ad4a85966af1465f', 'treatment', '7th Payment', '7th Payment', 'hist2025_0a88f305f63eb9dd', '9311088607', 'Kishanganj', 'MD DABIR ALAM',
  '2025-12-22', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-22T10:21:00.000Z', '2025-12-22T10:21:00.000Z'
),
(
  'hist2025_pay_b69f97b7111b3194', 'treatment', 'Advance', 'Advance', 'hist2025_02ccc85535cc2865', '9679159055', 'Kishanganj', 'SALIMUDDIN',
  '2025-11-18', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-18T10:15:00.000Z', '2025-11-18T10:15:00.000Z'
),
(
  'hist2025_pay_0eb2db4616b3704b', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_02ccc85535cc2865', '9679159055', 'Kishanganj', 'SALIMUDDIN',
  '2025-11-22', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-22T10:16:00.000Z', '2025-11-22T10:16:00.000Z'
),
(
  'hist2025_pay_12c59030515277a3', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_02ccc85535cc2865', '9679159055', 'Kishanganj', 'SALIMUDDIN',
  '2025-11-26', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-26T10:17:00.000Z', '2025-11-26T10:17:00.000Z'
),
(
  'hist2025_pay_ac98a31ed0f56469', 'treatment', '4th Payment', '4th Payment', 'hist2025_02ccc85535cc2865', '9679159055', 'Kishanganj', 'SALIMUDDIN',
  '2025-11-29', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-29T10:18:00.000Z', '2025-11-29T10:18:00.000Z'
),
(
  'hist2025_pay_cd4dc2a5791b5024', 'treatment', '5th Payment', '5th Payment', 'hist2025_02ccc85535cc2865', '9679159055', 'Kishanganj', 'SALIMUDDIN',
  '2025-12-06', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-06T10:19:00.000Z', '2025-12-06T10:19:00.000Z'
),
(
  'hist2025_pay_904cf00380b6aff9', 'treatment', '6th Payment', '6th Payment', 'hist2025_02ccc85535cc2865', '9679159055', 'Kishanganj', 'SALIMUDDIN',
  '2025-12-10', '2500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-10T10:20:00.000Z', '2025-12-10T10:20:00.000Z'
),
(
  'hist2025_pay_a80787e3d8f7d6e9', 'treatment', '7th Payment', '7th Payment', 'hist2025_02ccc85535cc2865', '9679159055', 'Kishanganj', 'SALIMUDDIN',
  '2025-12-13', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:21:00.000Z', '2025-12-13T10:21:00.000Z'
),
(
  'hist2025_pay_97557557496b2710', 'treatment', '8th Payment', '8th Payment', 'hist2025_02ccc85535cc2865', '9679159055', 'Kishanganj', 'SALIMUDDIN',
  '2025-12-17', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-17T10:22:00.000Z', '2025-12-17T10:22:00.000Z'
),
(
  'hist2025_pay_6dbca4c93b421ea4', 'treatment', '9th Payment', '9th Payment', 'hist2025_02ccc85535cc2865', '9679159055', 'Kishanganj', 'SALIMUDDIN',
  '2025-12-20', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-20T10:23:00.000Z', '2025-12-20T10:23:00.000Z'
),
(
  'hist2025_pay_ac88a23b08145eb4', 'treatment', '10th Payment', '10th Payment', 'hist2025_02ccc85535cc2865', '9679159055', 'Kishanganj', 'SALIMUDDIN',
  '2025-11-20', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-20T10:24:00.000Z', '2025-11-20T10:24:00.000Z'
),
(
  'hist2025_pay_9bc6f2609e07a82d', 'treatment', 'Advance', 'Advance', 'hist2025_11eb702a625a0049', '6296824807', 'Kishanganj', 'JAGADIS MAJUMDAR',
  '2025-11-18', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-18T10:15:00.000Z', '2025-11-18T10:15:00.000Z'
),
(
  'hist2025_pay_42f3435ece0ea326', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_11eb702a625a0049', '6296824807', 'Kishanganj', 'JAGADIS MAJUMDAR',
  '2025-11-26', '9000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-26T10:16:00.000Z', '2025-11-26T10:16:00.000Z'
),
(
  'hist2025_pay_abf6d439ade2f4f5', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_11eb702a625a0049', '6296824807', 'Kishanganj', 'JAGADIS MAJUMDAR',
  '2025-12-03', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-03T10:17:00.000Z', '2025-12-03T10:17:00.000Z'
),
(
  'hist2025_pay_697c9f3b97f391a3', 'treatment', '4th Payment', '4th Payment', 'hist2025_11eb702a625a0049', '6296824807', 'Kishanganj', 'JAGADIS MAJUMDAR',
  '2025-12-06', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-06T10:18:00.000Z', '2025-12-06T10:18:00.000Z'
),
(
  'hist2025_pay_20ccb6c4580998a9', 'treatment', '5th Payment', '5th Payment', 'hist2025_11eb702a625a0049', '6296824807', 'Kishanganj', 'JAGADIS MAJUMDAR',
  '2025-12-13', '6000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:19:00.000Z', '2025-12-13T10:19:00.000Z'
),
(
  'hist2025_pay_97ff0213b6080f33', 'treatment', '6th Payment', '6th Payment', 'hist2025_11eb702a625a0049', '6296824807', 'Kishanganj', 'JAGADIS MAJUMDAR',
  '2025-12-17', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-17T10:20:00.000Z', '2025-12-17T10:20:00.000Z'
),
(
  'hist2025_pay_f4c85a45795f2f34', 'treatment', '7th Payment', '7th Payment', 'hist2025_11eb702a625a0049', '6296824807', 'Kishanganj', 'JAGADIS MAJUMDAR',
  '2025-12-27', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:21:00.000Z', '2025-12-27T10:21:00.000Z'
);
