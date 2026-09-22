-- ২০২৫ শিট ধাপ ৪ -- ব্যাচ 2/7 (রোগী 15-29, মোট 15)
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
  'hist2025_27733fd60ec9d556', 'KNE-05032025-002', '2025-03-05', '2025-03-05', '2025-03-05',
  'SANTOSH CHOWHAN', '7488369122', '', 'Kishanganj', '47', 'Male',
  'FORUNGOLA, KNE, KNE, KNE', 'Fistula', '52000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-05T10:00:00.000Z', '2025-03-05T10:00:00.000Z'
),
(
  'hist2025_388d44e61ca04696', 'KNE-25032025-001', '2025-03-25', '2025-03-25', '2025-03-25',
  'ABU BAKAR', '9064346441', '', 'Kishanganj', '40', 'Male',
  'SEKHPURA, TAIYABUR, POTHIYA, KNE', 'Fistula', '55000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-25T10:00:00.000Z', '2025-03-25T10:00:00.000Z'
),
(
  'hist2025_b039066420864f7a', 'KNE-02042025-001', '2025-04-02', '2025-04-02', '2025-04-02',
  'SAKIL PARVEZ', '9665704592', '', 'Kishanganj', '', 'Male',
  'DHANTOLA', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-02T10:00:00.000Z', '2025-04-02T10:00:00.000Z'
),
(
  'hist2025_306f96d3150e04a2', 'KNE-09042025-001', '2025-04-09', '2025-04-09', '2025-04-09',
  'BABLU HEMRAO', '9229963597', '', 'Kishanganj', '', 'Male',
  '', 'Other', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-09T10:00:00.000Z', '2025-04-09T10:00:00.000Z'
),
(
  'hist2025_c87cdfd13034981f', 'KNE-15042025-001', '2025-04-15', '2025-04-15', '2025-04-15',
  'MANOJ KAMATI', '9798367794', '', 'Kishanganj', '', 'Male',
  'Deramari', 'Piles', '38000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-15T10:00:00.000Z', '2025-04-15T10:00:00.000Z'
),
(
  'hist2025_fabce8c86066732b', 'KNE-16042025-001', '2025-04-16', '2025-04-16', '2025-04-16',
  'HASINA KHATUN', '9933905097', '', 'Kishanganj', '70', 'Female',
  'Chakla', 'Piles', '50000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-16T10:00:00.000Z', '2025-04-16T10:00:00.000Z'
),
(
  'hist2025_648d95fed60e8bae', 'KNE-16042025-002', '2025-04-16', '2025-04-16', '2025-04-16',
  'MD IBRAHIM', '9430655565', '', 'Kishanganj', '33', 'Male',
  'Samda', 'Piles', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-16T10:00:00.000Z', '2025-04-16T10:00:00.000Z'
),
(
  'hist2025_91fb0a2f5913cddf', 'KNE-21042025-001', '2025-04-21', '2025-04-21', '2025-04-21',
  'TANVIR ALAM', '9679788944', '', 'Kishanganj', '28', 'Male',
  'Lohagachi', 'Piles, Fistula', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-21T10:00:00.000Z', '2025-04-21T10:00:00.000Z'
),
(
  'hist2025_ce06c26454644bc9', 'KNE-23042025-001', '2025-04-23', '2025-04-23', '2025-04-23',
  'ANTIMA KUMARI', '9348825003', '', 'Kishanganj', '', 'Female',
  'pipla', 'Piles', '12000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-23T10:00:00.000Z', '2025-04-23T10:00:00.000Z'
),
(
  'hist2025_71245e5a159e2bf3', 'KNE-23042025-002', '2025-04-23', '2025-04-23', '2025-04-23',
  'MANOJ KR GHOSH', '8617379288', '', 'Kishanganj', '', 'Male',
  'Bijuliya, THAKUR BARI, CHAKULIYA, U.D', 'Piles, Fistula', '50000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-23T10:00:00.000Z', '2025-04-23T10:00:00.000Z'
),
(
  'hist2025_7e5997a5066627e2', 'KNE-23042025-003', '2025-04-23', '2025-04-23', '2025-04-23',
  'MONOHAR CHOUHAN', '9006664849', '', 'Kishanganj', '', 'Male',
  'Ranipatra', 'Piles', '51200',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-23T10:00:00.000Z', '2025-04-23T10:00:00.000Z'
),
(
  'hist2025_f3e5a03776207506', 'KNE-02052025-001', '2025-05-02', '2025-05-02', '2025-05-02',
  'SOMIJA KHATOON', '8580418375', '', 'Kishanganj', '40', 'Female',
  '', 'Other', '65000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-02T10:00:00.000Z', '2025-05-02T10:00:00.000Z'
),
(
  'hist2025_32c03339aaf38a5b', 'KNE-03052025-001', '2025-05-03', '2025-05-03', '2025-05-03',
  'RAHMAT ULLAH', '9973867443', '', 'Kishanganj', '36', 'Male',
  'PATHKOI, PATKHOI HAT, POCHADHAMON, KISHANGANJ', 'Hydrocele', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-03T10:00:00.000Z', '2025-05-03T10:00:00.000Z'
),
(
  'hist2025_0b4d6ea829565e33', 'KNE-05052025-001', '2025-05-05', '2025-05-05', '2025-05-05',
  'BIBI FARIDA KHATOON', '8348973365', '', 'Kishanganj', '49', 'Female',
  'SAPATBARI, AMALIA, CHAKULIYA, U.D', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-05T10:00:00.000Z', '2025-05-05T10:00:00.000Z'
),
(
  'hist2025_16df3b3f2c2ecfaa', 'KNE-06052025-001', '2025-05-06', '2025-05-06', '2025-05-06',
  'ALARJAN KHATOON', '8116291683', '7001155804', 'Kishanganj', '25', 'Female',
  'SOLPARA, SOLPARA, GOYALPOKHAR, U.D', 'Piles', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-06T10:00:00.000Z', '2025-05-06T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'hist2025_pay_e7c06825c7e4e1a2', 'treatment', 'Advance', 'Advance', 'hist2025_27733fd60ec9d556', '7488369122', 'Kishanganj', 'SANTOSH CHOWHAN',
  '2025-03-06', '8000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-06T10:15:00.000Z', '2025-03-06T10:15:00.000Z'
),
(
  'hist2025_pay_4c03f55ec7d3d1cf', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_27733fd60ec9d556', '7488369122', 'Kishanganj', 'SANTOSH CHOWHAN',
  '2025-03-08', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-08T10:16:00.000Z', '2025-03-08T10:16:00.000Z'
),
(
  'hist2025_pay_284e0cd2356b7ba9', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_27733fd60ec9d556', '7488369122', 'Kishanganj', 'SANTOSH CHOWHAN',
  '2025-03-12', '7000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-12T10:17:00.000Z', '2025-03-12T10:17:00.000Z'
),
(
  'hist2025_pay_058567ce89f0224e', 'treatment', '4th Payment', '4th Payment', 'hist2025_27733fd60ec9d556', '7488369122', 'Kishanganj', 'SANTOSH CHOWHAN',
  '2025-03-15', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-15T10:18:00.000Z', '2025-03-15T10:18:00.000Z'
),
(
  'hist2025_pay_c10281f9b82242c3', 'treatment', '5th Payment', '5th Payment', 'hist2025_27733fd60ec9d556', '7488369122', 'Kishanganj', 'SANTOSH CHOWHAN',
  '2025-03-17', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-17T10:19:00.000Z', '2025-03-17T10:19:00.000Z'
),
(
  'hist2025_pay_c5e6fcf98978de34', 'treatment', '6th Payment', '6th Payment', 'hist2025_27733fd60ec9d556', '7488369122', 'Kishanganj', 'SANTOSH CHOWHAN',
  '2025-03-19', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-19T10:20:00.000Z', '2025-03-19T10:20:00.000Z'
),
(
  'hist2025_pay_f008764585b32eb7', 'treatment', '7th Payment', '7th Payment', 'hist2025_27733fd60ec9d556', '7488369122', 'Kishanganj', 'SANTOSH CHOWHAN',
  '2025-03-21', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-21T10:21:00.000Z', '2025-03-21T10:21:00.000Z'
),
(
  'hist2025_pay_a2dfe7b1dd1ac138', 'treatment', '8th Payment', '8th Payment', 'hist2025_27733fd60ec9d556', '7488369122', 'Kishanganj', 'SANTOSH CHOWHAN',
  '2025-03-23', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-23T10:22:00.000Z', '2025-03-23T10:22:00.000Z'
),
(
  'hist2025_pay_fe45d0d81a9b4874', 'treatment', '9th Payment', '9th Payment', 'hist2025_27733fd60ec9d556', '7488369122', 'Kishanganj', 'SANTOSH CHOWHAN',
  '2025-03-25', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-25T10:23:00.000Z', '2025-03-25T10:23:00.000Z'
),
(
  'hist2025_pay_fb81e1def1cda2c0', 'treatment', '10th Payment', '10th Payment', 'hist2025_27733fd60ec9d556', '7488369122', 'Kishanganj', 'SANTOSH CHOWHAN',
  '2025-03-27', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-27T10:24:00.000Z', '2025-03-27T10:24:00.000Z'
),
(
  'hist2025_pay_d936d5d551023f11', 'treatment', '11th Payment', '11th Payment', 'hist2025_27733fd60ec9d556', '7488369122', 'Kishanganj', 'SANTOSH CHOWHAN',
  '2025-03-29', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-29T10:25:00.000Z', '2025-03-29T10:25:00.000Z'
),
(
  'hist2025_pay_beb3670e48d3a90c', 'treatment', '12th Payment', '12th Payment', 'hist2025_27733fd60ec9d556', '7488369122', 'Kishanganj', 'SANTOSH CHOWHAN',
  '2025-04-01', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-01T10:26:00.000Z', '2025-04-01T10:26:00.000Z'
),
(
  'hist2025_pay_4945c8a3dea256a7', 'treatment', '13th Payment', '13th Payment', 'hist2025_27733fd60ec9d556', '7488369122', 'Kishanganj', 'SANTOSH CHOWHAN',
  '2025-04-08', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-08T10:27:00.000Z', '2025-04-08T10:27:00.000Z'
),
(
  'hist2025_pay_a017fd1574d2d960', 'treatment', 'Advance', 'Advance', 'hist2025_388d44e61ca04696', '9064346441', 'Kishanganj', 'ABU BAKAR',
  '2025-03-25', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-25T10:15:00.000Z', '2025-03-25T10:15:00.000Z'
),
(
  'hist2025_pay_0a2e00c9b183e75d', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_388d44e61ca04696', '9064346441', 'Kishanganj', 'ABU BAKAR',
  '2025-03-29', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-29T10:16:00.000Z', '2025-03-29T10:16:00.000Z'
),
(
  'hist2025_pay_7b3d0ce85a4c605e', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_388d44e61ca04696', '9064346441', 'Kishanganj', 'ABU BAKAR',
  '2025-04-02', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-02T10:17:00.000Z', '2025-04-02T10:17:00.000Z'
),
(
  'hist2025_pay_895c2d9b878fbbbe', 'treatment', '4th Payment', '4th Payment', 'hist2025_388d44e61ca04696', '9064346441', 'Kishanganj', 'ABU BAKAR',
  '2025-04-16', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-16T10:18:00.000Z', '2025-04-16T10:18:00.000Z'
),
(
  'hist2025_pay_9115a66e013eac4b', 'treatment', '5th Payment', '5th Payment', 'hist2025_388d44e61ca04696', '9064346441', 'Kishanganj', 'ABU BAKAR',
  '2025-04-23', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-23T10:19:00.000Z', '2025-04-23T10:19:00.000Z'
),
(
  'hist2025_pay_4a2fbf89767c27b8', 'treatment', '6th Payment', '6th Payment', 'hist2025_388d44e61ca04696', '9064346441', 'Kishanganj', 'ABU BAKAR',
  '2025-04-26', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-26T10:20:00.000Z', '2025-04-26T10:20:00.000Z'
),
(
  'hist2025_pay_0709060131e036e6', 'treatment', '7th Payment', '7th Payment', 'hist2025_388d44e61ca04696', '9064346441', 'Kishanganj', 'ABU BAKAR',
  '2025-04-07', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-07T10:21:00.000Z', '2025-04-07T10:21:00.000Z'
),
(
  'hist2025_pay_13fb4529313fdba4', 'treatment', '8th Payment', '8th Payment', 'hist2025_388d44e61ca04696', '9064346441', 'Kishanganj', 'ABU BAKAR',
  '2025-04-14', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-14T10:22:00.000Z', '2025-04-14T10:22:00.000Z'
),
(
  'hist2025_pay_d8ac3d24727e8b22', 'treatment', '9th Payment', '9th Payment', 'hist2025_388d44e61ca04696', '9064346441', 'Kishanganj', 'ABU BAKAR',
  '2025-06-04', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-04T10:23:00.000Z', '2025-06-04T10:23:00.000Z'
),
(
  'hist2025_pay_45222fee62ae1e0c', 'treatment', 'Advance', 'Advance', 'hist2025_b039066420864f7a', '9665704592', 'Kishanganj', 'SAKIL PARVEZ',
  '2025-04-02', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-02T10:15:00.000Z', '2025-04-02T10:15:00.000Z'
),
(
  'hist2025_pay_da8f3c97c04480f5', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_b039066420864f7a', '9665704592', 'Kishanganj', 'SAKIL PARVEZ',
  '2025-04-05', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-05T10:16:00.000Z', '2025-04-05T10:16:00.000Z'
),
(
  'hist2025_pay_8b224bbe08b7963f', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_b039066420864f7a', '9665704592', 'Kishanganj', 'SAKIL PARVEZ',
  '2025-04-09', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-09T10:17:00.000Z', '2025-04-09T10:17:00.000Z'
),
(
  'hist2025_pay_cd48de02df77175c', 'treatment', '4th Payment', '4th Payment', 'hist2025_b039066420864f7a', '9665704592', 'Kishanganj', 'SAKIL PARVEZ',
  '2025-04-14', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-14T10:18:00.000Z', '2025-04-14T10:18:00.000Z'
),
(
  'hist2025_pay_fddae21d477ab6a9', 'treatment', '5th Payment', '5th Payment', 'hist2025_b039066420864f7a', '9665704592', 'Kishanganj', 'SAKIL PARVEZ',
  '2025-04-21', '9000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-21T10:19:00.000Z', '2025-04-21T10:19:00.000Z'
),
(
  'hist2025_pay_acc7487943369460', 'treatment', '6th Payment', '6th Payment', 'hist2025_b039066420864f7a', '9665704592', 'Kishanganj', 'SAKIL PARVEZ',
  '2025-05-28', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-28T10:20:00.000Z', '2025-05-28T10:20:00.000Z'
),
(
  'hist2025_pay_d031bca3672d62fc', 'treatment', '7th Payment', '7th Payment', 'hist2025_b039066420864f7a', '9665704592', 'Kishanganj', 'SAKIL PARVEZ',
  '2025-06-15', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-15T10:21:00.000Z', '2025-06-15T10:21:00.000Z'
),
(
  'hist2025_pay_b46432a0d31843a6', 'treatment', 'Advance', 'Advance', 'hist2025_306f96d3150e04a2', '9229963597', 'Kishanganj', 'BABLU HEMRAO',
  '2025-04-09', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-09T10:15:00.000Z', '2025-04-09T10:15:00.000Z'
),
(
  'hist2025_pay_fe4f4072dff5280d', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_306f96d3150e04a2', '9229963597', 'Kishanganj', 'BABLU HEMRAO',
  '2025-04-18', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-18T10:16:00.000Z', '2025-04-18T10:16:00.000Z'
),
(
  'hist2025_pay_9307c880ae1012f4', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_306f96d3150e04a2', '9229963597', 'Kishanganj', 'BABLU HEMRAO',
  '2025-04-22', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-22T10:17:00.000Z', '2025-04-22T10:17:00.000Z'
),
(
  'hist2025_pay_73500d278b95c7b5', 'treatment', '4th Payment', '4th Payment', 'hist2025_306f96d3150e04a2', '9229963597', 'Kishanganj', 'BABLU HEMRAO',
  '2025-04-28', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-28T10:18:00.000Z', '2025-04-28T10:18:00.000Z'
),
(
  'hist2025_pay_87d2a5b612b68e48', 'treatment', '5th Payment', '5th Payment', 'hist2025_306f96d3150e04a2', '9229963597', 'Kishanganj', 'BABLU HEMRAO',
  '2025-05-03', '1400', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-03T10:19:00.000Z', '2025-05-03T10:19:00.000Z'
),
(
  'hist2025_pay_52997d7b2ded2019', 'treatment', 'Advance', 'Advance', 'hist2025_c87cdfd13034981f', '9798367794', 'Kishanganj', 'MANOJ KAMATI',
  '2025-04-15', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-15T10:15:00.000Z', '2025-04-15T10:15:00.000Z'
),
(
  'hist2025_pay_a95eb7b42c145588', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_c87cdfd13034981f', '9798367794', 'Kishanganj', 'MANOJ KAMATI',
  '2025-04-19', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-19T10:16:00.000Z', '2025-04-19T10:16:00.000Z'
),
(
  'hist2025_pay_8d7d26cb256db93c', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_c87cdfd13034981f', '9798367794', 'Kishanganj', 'MANOJ KAMATI',
  '2025-04-23', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-23T10:17:00.000Z', '2025-04-23T10:17:00.000Z'
),
(
  'hist2025_pay_648d89e939304209', 'treatment', '4th Payment', '4th Payment', 'hist2025_c87cdfd13034981f', '9798367794', 'Kishanganj', 'MANOJ KAMATI',
  '2025-04-24', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-24T10:18:00.000Z', '2025-04-24T10:18:00.000Z'
),
(
  'hist2025_pay_3a66b41a10c9b77e', 'treatment', '5th Payment', '5th Payment', 'hist2025_c87cdfd13034981f', '9798367794', 'Kishanganj', 'MANOJ KAMATI',
  '2025-04-29', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-29T10:19:00.000Z', '2025-04-29T10:19:00.000Z'
),
(
  'hist2025_pay_9c1eaf0db02e58d7', 'treatment', '6th Payment', '6th Payment', 'hist2025_c87cdfd13034981f', '9798367794', 'Kishanganj', 'MANOJ KAMATI',
  '2025-05-05', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-05T10:20:00.000Z', '2025-05-05T10:20:00.000Z'
),
(
  'hist2025_pay_21b485e34331de51', 'treatment', '7th Payment', '7th Payment', 'hist2025_c87cdfd13034981f', '9798367794', 'Kishanganj', 'MANOJ KAMATI',
  '2025-05-14', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-14T10:21:00.000Z', '2025-05-14T10:21:00.000Z'
),
(
  'hist2025_pay_7cc427edb3d6d844', 'treatment', 'Advance', 'Advance', 'hist2025_fabce8c86066732b', '9933905097', 'Kishanganj', 'HASINA KHATUN',
  '2025-04-16', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-16T10:15:00.000Z', '2025-04-16T10:15:00.000Z'
),
(
  'hist2025_pay_dd281d619e28cf3a', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_fabce8c86066732b', '9933905097', 'Kishanganj', 'HASINA KHATUN',
  '2025-04-19', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-19T10:16:00.000Z', '2025-04-19T10:16:00.000Z'
),
(
  'hist2025_pay_012a1c82ddde1a4f', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_fabce8c86066732b', '9933905097', 'Kishanganj', 'HASINA KHATUN',
  '2025-04-23', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-23T10:17:00.000Z', '2025-04-23T10:17:00.000Z'
),
(
  'hist2025_pay_d2ec36e50e66a4ac', 'treatment', '4th Payment', '4th Payment', 'hist2025_fabce8c86066732b', '9933905097', 'Kishanganj', 'HASINA KHATUN',
  '2025-04-26', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-26T10:18:00.000Z', '2025-04-26T10:18:00.000Z'
),
(
  'hist2025_pay_032bb78c92910108', 'treatment', '5th Payment', '5th Payment', 'hist2025_fabce8c86066732b', '9933905097', 'Kishanganj', 'HASINA KHATUN',
  '2025-04-29', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-29T10:19:00.000Z', '2025-04-29T10:19:00.000Z'
),
(
  'hist2025_pay_c396b8c790da87d1', 'treatment', '6th Payment', '6th Payment', 'hist2025_fabce8c86066732b', '9933905097', 'Kishanganj', 'HASINA KHATUN',
  '2025-05-07', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-07T10:20:00.000Z', '2025-05-07T10:20:00.000Z'
),
(
  'hist2025_pay_bed5b002439789ea', 'treatment', '7th Payment', '7th Payment', 'hist2025_fabce8c86066732b', '9933905097', 'Kishanganj', 'HASINA KHATUN',
  '2025-05-14', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-14T10:21:00.000Z', '2025-05-14T10:21:00.000Z'
),
(
  'hist2025_pay_7bff0d5f40695440', 'treatment', '8th Payment', '8th Payment', 'hist2025_fabce8c86066732b', '9933905097', 'Kishanganj', 'HASINA KHATUN',
  '2025-05-21', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-21T10:22:00.000Z', '2025-05-21T10:22:00.000Z'
),
(
  'hist2025_pay_8d8023d58affcf14', 'treatment', 'Advance', 'Advance', 'hist2025_648d95fed60e8bae', '9430655565', 'Kishanganj', 'MD IBRAHIM',
  '2025-04-16', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-16T10:15:00.000Z', '2025-04-16T10:15:00.000Z'
),
(
  'hist2025_pay_d59c8d7784150376', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_648d95fed60e8bae', '9430655565', 'Kishanganj', 'MD IBRAHIM',
  '2025-04-19', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-19T10:16:00.000Z', '2025-04-19T10:16:00.000Z'
),
(
  'hist2025_pay_d0c13095c11cf614', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_648d95fed60e8bae', '9430655565', 'Kishanganj', 'MD IBRAHIM',
  '2025-05-07', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-07T10:17:00.000Z', '2025-05-07T10:17:00.000Z'
),
(
  'hist2025_pay_3d6b3129b43f409c', 'treatment', '4th Payment', '4th Payment', 'hist2025_648d95fed60e8bae', '9430655565', 'Kishanganj', 'MD IBRAHIM',
  '2025-05-21', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-21T10:18:00.000Z', '2025-05-21T10:18:00.000Z'
),
(
  'hist2025_pay_b6f253e000c02e67', 'treatment', 'Advance', 'Advance', 'hist2025_91fb0a2f5913cddf', '9679788944', 'Kishanganj', 'TANVIR ALAM',
  '2025-04-21', '2500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-21T10:15:00.000Z', '2025-04-21T10:15:00.000Z'
),
(
  'hist2025_pay_5345819adbf5a49c', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_91fb0a2f5913cddf', '9679788944', 'Kishanganj', 'TANVIR ALAM',
  '2025-04-26', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-26T10:16:00.000Z', '2025-04-26T10:16:00.000Z'
),
(
  'hist2025_pay_38f03593edc5aed7', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_91fb0a2f5913cddf', '9679788944', 'Kishanganj', 'TANVIR ALAM',
  '2025-04-29', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-29T10:17:00.000Z', '2025-04-29T10:17:00.000Z'
),
(
  'hist2025_pay_6c9af18ce4d73a00', 'treatment', '4th Payment', '4th Payment', 'hist2025_91fb0a2f5913cddf', '9679788944', 'Kishanganj', 'TANVIR ALAM',
  '2025-05-07', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-07T10:18:00.000Z', '2025-05-07T10:18:00.000Z'
),
(
  'hist2025_pay_ec9edcb08436b07c', 'treatment', '5th Payment', '5th Payment', 'hist2025_91fb0a2f5913cddf', '9679788944', 'Kishanganj', 'TANVIR ALAM',
  '2025-05-14', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-14T10:19:00.000Z', '2025-05-14T10:19:00.000Z'
),
(
  'hist2025_pay_76a765d4b340279c', 'treatment', '6th Payment', '6th Payment', 'hist2025_91fb0a2f5913cddf', '9679788944', 'Kishanganj', 'TANVIR ALAM',
  '2025-05-21', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-21T10:20:00.000Z', '2025-05-21T10:20:00.000Z'
),
(
  'hist2025_pay_e557036a3d312e75', 'treatment', '7th Payment', '7th Payment', 'hist2025_91fb0a2f5913cddf', '9679788944', 'Kishanganj', 'TANVIR ALAM',
  '2025-05-28', '6000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-28T10:21:00.000Z', '2025-05-28T10:21:00.000Z'
),
(
  'hist2025_pay_545489bd8875e159', 'treatment', '8th Payment', '8th Payment', 'hist2025_91fb0a2f5913cddf', '9679788944', 'Kishanganj', 'TANVIR ALAM',
  '2025-06-04', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-04T10:22:00.000Z', '2025-06-04T10:22:00.000Z'
),
(
  'hist2025_pay_80c1ce10b521e71d', 'treatment', '9th Payment', '9th Payment', 'hist2025_91fb0a2f5913cddf', '9679788944', 'Kishanganj', 'TANVIR ALAM',
  '2025-06-11', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-11T10:23:00.000Z', '2025-06-11T10:23:00.000Z'
),
(
  'hist2025_pay_948a2974161bb533', 'treatment', '10th Payment', '10th Payment', 'hist2025_91fb0a2f5913cddf', '9679788944', 'Kishanganj', 'TANVIR ALAM',
  '2025-06-18', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-18T10:24:00.000Z', '2025-06-18T10:24:00.000Z'
),
(
  'hist2025_pay_bd50611bd83b0a0b', 'treatment', 'Advance', 'Advance', 'hist2025_ce06c26454644bc9', '9348825003', 'Kishanganj', 'ANTIMA KUMARI',
  '2025-04-23', '600', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-23T10:15:00.000Z', '2025-04-23T10:15:00.000Z'
),
(
  'hist2025_pay_98c45e7e4b196187', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_ce06c26454644bc9', '9348825003', 'Kishanganj', 'ANTIMA KUMARI',
  '2025-04-26', '700', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-26T10:16:00.000Z', '2025-04-26T10:16:00.000Z'
),
(
  'hist2025_pay_76c5c2c10a7c5a8e', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_ce06c26454644bc9', '9348825003', 'Kishanganj', 'ANTIMA KUMARI',
  '2025-05-01', '5200', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-01T10:17:00.000Z', '2025-05-01T10:17:00.000Z'
),
(
  'hist2025_pay_29ae0a26de133da4', 'treatment', '4th Payment', '4th Payment', 'hist2025_ce06c26454644bc9', '9348825003', 'Kishanganj', 'ANTIMA KUMARI',
  '2025-05-03', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-03T10:18:00.000Z', '2025-05-03T10:18:00.000Z'
),
(
  'hist2025_pay_b8f76c6fda6976da', 'treatment', '5th Payment', '5th Payment', 'hist2025_ce06c26454644bc9', '9348825003', 'Kishanganj', 'ANTIMA KUMARI',
  '2025-05-07', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-07T10:19:00.000Z', '2025-05-07T10:19:00.000Z'
),
(
  'hist2025_pay_7c18ab86ee5c906c', 'treatment', 'Advance', 'Advance', 'hist2025_71245e5a159e2bf3', '8617379288', 'Kishanganj', 'MANOJ KR GHOSH',
  '2025-04-23', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-23T10:15:00.000Z', '2025-04-23T10:15:00.000Z'
),
(
  'hist2025_pay_3de7473ccb03ae72', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_71245e5a159e2bf3', '8617379288', 'Kishanganj', 'MANOJ KR GHOSH',
  '2025-04-25', '8000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-25T10:16:00.000Z', '2025-04-25T10:16:00.000Z'
),
(
  'hist2025_pay_70f36c73fb3b2259', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_71245e5a159e2bf3', '8617379288', 'Kishanganj', 'MANOJ KR GHOSH',
  '2025-04-30', '6000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-30T10:17:00.000Z', '2025-04-30T10:17:00.000Z'
),
(
  'hist2025_pay_1c185ee0bdf2c106', 'treatment', '4th Payment', '4th Payment', 'hist2025_71245e5a159e2bf3', '8617379288', 'Kishanganj', 'MANOJ KR GHOSH',
  '2025-05-07', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-07T10:18:00.000Z', '2025-05-07T10:18:00.000Z'
),
(
  'hist2025_pay_1e7ca73ec162467b', 'treatment', '5th Payment', '5th Payment', 'hist2025_71245e5a159e2bf3', '8617379288', 'Kishanganj', 'MANOJ KR GHOSH',
  '2025-05-14', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-14T10:19:00.000Z', '2025-05-14T10:19:00.000Z'
),
(
  'hist2025_pay_330676eee36c5b3b', 'treatment', '6th Payment', '6th Payment', 'hist2025_71245e5a159e2bf3', '8617379288', 'Kishanganj', 'MANOJ KR GHOSH',
  '2025-05-27', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-27T10:20:00.000Z', '2025-05-27T10:20:00.000Z'
),
(
  'hist2025_pay_90d5ae265118b200', 'treatment', '7th Payment', '7th Payment', 'hist2025_71245e5a159e2bf3', '8617379288', 'Kishanganj', 'MANOJ KR GHOSH',
  '2025-06-04', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-04T10:21:00.000Z', '2025-06-04T10:21:00.000Z'
),
(
  'hist2025_pay_c4bf521b67e82543', 'treatment', '8th Payment', '8th Payment', 'hist2025_71245e5a159e2bf3', '8617379288', 'Kishanganj', 'MANOJ KR GHOSH',
  '2025-06-11', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-11T10:22:00.000Z', '2025-06-11T10:22:00.000Z'
),
(
  'hist2025_pay_a45a52fe0d26080d', 'treatment', '9th Payment', '9th Payment', 'hist2025_71245e5a159e2bf3', '8617379288', 'Kishanganj', 'MANOJ KR GHOSH',
  '2025-06-17', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-17T10:23:00.000Z', '2025-06-17T10:23:00.000Z'
),
(
  'hist2025_pay_d05f4daa2f5b47df', 'treatment', '10th Payment', '10th Payment', 'hist2025_71245e5a159e2bf3', '8617379288', 'Kishanganj', 'MANOJ KR GHOSH',
  '2025-09-17', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-17T10:24:00.000Z', '2025-09-17T10:24:00.000Z'
),
(
  'hist2025_pay_b3feea69987d22ed', 'treatment', 'Advance', 'Advance', 'hist2025_7e5997a5066627e2', '9006664849', 'Kishanganj', 'MONOHAR CHOUHAN',
  '2025-05-29', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-29T10:15:00.000Z', '2025-05-29T10:15:00.000Z'
),
(
  'hist2025_pay_a91f635381f5f810', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_7e5997a5066627e2', '9006664849', 'Kishanganj', 'MONOHAR CHOUHAN',
  '2025-06-04', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-04T10:16:00.000Z', '2025-06-04T10:16:00.000Z'
),
(
  'hist2025_pay_be981cd9fa55a60b', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_7e5997a5066627e2', '9006664849', 'Kishanganj', 'MONOHAR CHOUHAN',
  '2025-06-11', '2500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-11T10:17:00.000Z', '2025-06-11T10:17:00.000Z'
),
(
  'hist2025_pay_451b4c10cb9be42f', 'treatment', '4th Payment', '4th Payment', 'hist2025_7e5997a5066627e2', '9006664849', 'Kishanganj', 'MONOHAR CHOUHAN',
  '2025-06-23', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-23T10:18:00.000Z', '2025-06-23T10:18:00.000Z'
),
(
  'hist2025_pay_e0476e3cc3fc61ed', 'treatment', 'Advance', 'Advance', 'hist2025_f3e5a03776207506', '8580418375', 'Kishanganj', 'SOMIJA KHATOON',
  '2025-05-02', '6000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-02T10:15:00.000Z', '2025-05-02T10:15:00.000Z'
),
(
  'hist2025_pay_3957e2e4aa50f662', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_f3e5a03776207506', '8580418375', 'Kishanganj', 'SOMIJA KHATOON',
  '2025-05-03', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-03T10:16:00.000Z', '2025-05-03T10:16:00.000Z'
),
(
  'hist2025_pay_d54cd64f4c50c669', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_f3e5a03776207506', '8580418375', 'Kishanganj', 'SOMIJA KHATOON',
  '2025-05-07', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-07T10:17:00.000Z', '2025-05-07T10:17:00.000Z'
),
(
  'hist2025_pay_b10216eab73dd603', 'treatment', '4th Payment', '4th Payment', 'hist2025_f3e5a03776207506', '8580418375', 'Kishanganj', 'SOMIJA KHATOON',
  '2025-05-10', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:18:00.000Z', '2025-05-10T10:18:00.000Z'
),
(
  'hist2025_pay_78d6db7c79b2837b', 'treatment', '5th Payment', '5th Payment', 'hist2025_f3e5a03776207506', '8580418375', 'Kishanganj', 'SOMIJA KHATOON',
  '2025-05-14', '3500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-14T10:19:00.000Z', '2025-05-14T10:19:00.000Z'
),
(
  'hist2025_pay_8b8c67725e5d72d4', 'treatment', '6th Payment', '6th Payment', 'hist2025_f3e5a03776207506', '8580418375', 'Kishanganj', 'SOMIJA KHATOON',
  '2025-05-15', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-15T10:20:00.000Z', '2025-05-15T10:20:00.000Z'
),
(
  'hist2025_pay_14dc169ef0791625', 'treatment', '7th Payment', '7th Payment', 'hist2025_f3e5a03776207506', '8580418375', 'Kishanganj', 'SOMIJA KHATOON',
  '2025-05-17', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:21:00.000Z', '2025-05-17T10:21:00.000Z'
),
(
  'hist2025_pay_fc213e2adef7ce31', 'treatment', '8th Payment', '8th Payment', 'hist2025_f3e5a03776207506', '8580418375', 'Kishanganj', 'SOMIJA KHATOON',
  '2025-05-21', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-21T10:22:00.000Z', '2025-05-21T10:22:00.000Z'
),
(
  'hist2025_pay_9b15479b35946a5d', 'treatment', '9th Payment', '9th Payment', 'hist2025_f3e5a03776207506', '8580418375', 'Kishanganj', 'SOMIJA KHATOON',
  '2025-05-28', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-28T10:23:00.000Z', '2025-05-28T10:23:00.000Z'
),
(
  'hist2025_pay_7648ebe6acdb4441', 'treatment', '10th Payment', '10th Payment', 'hist2025_f3e5a03776207506', '8580418375', 'Kishanganj', 'SOMIJA KHATOON',
  '2025-05-31', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-31T10:24:00.000Z', '2025-05-31T10:24:00.000Z'
),
(
  'hist2025_pay_4dd726dd0443191b', 'treatment', '11th Payment', '11th Payment', 'hist2025_f3e5a03776207506', '8580418375', 'Kishanganj', 'SOMIJA KHATOON',
  '2025-06-11', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-11T10:25:00.000Z', '2025-06-11T10:25:00.000Z'
),
(
  'hist2025_pay_02966bcd6c77c7d7', 'treatment', '12th Payment', '12th Payment', 'hist2025_f3e5a03776207506', '8580418375', 'Kishanganj', 'SOMIJA KHATOON',
  '2025-06-19', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-19T10:26:00.000Z', '2025-06-19T10:26:00.000Z'
),
(
  'hist2025_pay_19faa0fae9dfd920', 'treatment', '13th Payment', '13th Payment', 'hist2025_f3e5a03776207506', '8580418375', 'Kishanganj', 'SOMIJA KHATOON',
  '2025-06-25', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-25T10:27:00.000Z', '2025-06-25T10:27:00.000Z'
),
(
  'hist2025_pay_c1943f7bf3b2edf5', 'treatment', 'Advance', 'Advance', 'hist2025_32c03339aaf38a5b', '9973867443', 'Kishanganj', 'RAHMAT ULLAH',
  '2025-05-05', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-05T10:15:00.000Z', '2025-05-05T10:15:00.000Z'
),
(
  'hist2025_pay_ff350cdb01157550', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_32c03339aaf38a5b', '9973867443', 'Kishanganj', 'RAHMAT ULLAH',
  '2025-05-15', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-15T10:16:00.000Z', '2025-05-15T10:16:00.000Z'
),
(
  'hist2025_pay_474d1fe196e5b091', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_32c03339aaf38a5b', '9973867443', 'Kishanganj', 'RAHMAT ULLAH',
  '2025-06-10', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-10T10:17:00.000Z', '2025-06-10T10:17:00.000Z'
),
(
  'hist2025_pay_3a1401eda7d6f06c', 'treatment', '4th Payment', '4th Payment', 'hist2025_32c03339aaf38a5b', '9973867443', 'Kishanganj', 'RAHMAT ULLAH',
  '2025-06-11', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-11T10:18:00.000Z', '2025-06-11T10:18:00.000Z'
),
(
  'hist2025_pay_91bf65664caf5137', 'treatment', '5th Payment', '5th Payment', 'hist2025_32c03339aaf38a5b', '9973867443', 'Kishanganj', 'RAHMAT ULLAH',
  '2025-06-18', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-18T10:19:00.000Z', '2025-06-18T10:19:00.000Z'
),
(
  'hist2025_pay_c373b2042d534d0f', 'treatment', 'Advance', 'Advance', 'hist2025_0b4d6ea829565e33', '8348973365', 'Kishanganj', 'BIBI FARIDA KHATOON',
  '2025-05-05', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-05T10:15:00.000Z', '2025-05-05T10:15:00.000Z'
),
(
  'hist2025_pay_2d765b510afb9b3c', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_0b4d6ea829565e33', '8348973365', 'Kishanganj', 'BIBI FARIDA KHATOON',
  '2025-05-07', '6500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-07T10:16:00.000Z', '2025-05-07T10:16:00.000Z'
),
(
  'hist2025_pay_0d384f3f9c1c025c', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_0b4d6ea829565e33', '8348973365', 'Kishanganj', 'BIBI FARIDA KHATOON',
  '2025-05-10', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:17:00.000Z', '2025-05-10T10:17:00.000Z'
),
(
  'hist2025_pay_afeb9620c0206c1e', 'treatment', '4th Payment', '4th Payment', 'hist2025_0b4d6ea829565e33', '8348973365', 'Kishanganj', 'BIBI FARIDA KHATOON',
  '2025-05-15', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-15T10:18:00.000Z', '2025-05-15T10:18:00.000Z'
),
(
  'hist2025_pay_f4c0c615386ca205', 'treatment', '5th Payment', '5th Payment', 'hist2025_0b4d6ea829565e33', '8348973365', 'Kishanganj', 'BIBI FARIDA KHATOON',
  '2025-05-21', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-21T10:19:00.000Z', '2025-05-21T10:19:00.000Z'
),
(
  'hist2025_pay_4f3ed94055dcfb8f', 'treatment', '6th Payment', '6th Payment', 'hist2025_0b4d6ea829565e33', '8348973365', 'Kishanganj', 'BIBI FARIDA KHATOON',
  '2025-05-28', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-28T10:20:00.000Z', '2025-05-28T10:20:00.000Z'
),
(
  'hist2025_pay_0ed583fef4f61a9f', 'treatment', '7th Payment', '7th Payment', 'hist2025_0b4d6ea829565e33', '8348973365', 'Kishanganj', 'BIBI FARIDA KHATOON',
  '2025-06-04', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-04T10:21:00.000Z', '2025-06-04T10:21:00.000Z'
),
(
  'hist2025_pay_68131c7251c4c8aa', 'treatment', '8th Payment', '8th Payment', 'hist2025_0b4d6ea829565e33', '8348973365', 'Kishanganj', 'BIBI FARIDA KHATOON',
  '2025-06-11', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-11T10:22:00.000Z', '2025-06-11T10:22:00.000Z'
),
(
  'hist2025_pay_6017c58ffe30121e', 'treatment', '9th Payment', '9th Payment', 'hist2025_0b4d6ea829565e33', '8348973365', 'Kishanganj', 'BIBI FARIDA KHATOON',
  '2025-08-26', '3500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-26T10:23:00.000Z', '2025-08-26T10:23:00.000Z'
),
(
  'hist2025_pay_4a3d2e754d085361', 'treatment', 'Advance', 'Advance', 'hist2025_16df3b3f2c2ecfaa', '8116291683', 'Kishanganj', 'ALARJAN KHATOON',
  '2025-05-06', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-06T10:15:00.000Z', '2025-05-06T10:15:00.000Z'
),
(
  'hist2025_pay_aaeb8e7e8ad36a39', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_16df3b3f2c2ecfaa', '8116291683', 'Kishanganj', 'ALARJAN KHATOON',
  '2025-05-14', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-14T10:16:00.000Z', '2025-05-14T10:16:00.000Z'
),
(
  'hist2025_pay_d23e95e74b0cb3e0', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_16df3b3f2c2ecfaa', '8116291683', 'Kishanganj', 'ALARJAN KHATOON',
  '2025-05-15', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-15T10:17:00.000Z', '2025-05-15T10:17:00.000Z'
),
(
  'hist2025_pay_d05989b4e828907b', 'treatment', '4th Payment', '4th Payment', 'hist2025_16df3b3f2c2ecfaa', '8116291683', 'Kishanganj', 'ALARJAN KHATOON',
  '2025-06-11', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-11T10:18:00.000Z', '2025-06-11T10:18:00.000Z'
);
