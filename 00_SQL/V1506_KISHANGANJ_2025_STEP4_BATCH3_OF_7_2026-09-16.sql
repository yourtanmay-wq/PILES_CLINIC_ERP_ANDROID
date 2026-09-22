-- ২০২৫ শিট ধাপ ৪ -- ব্যাচ 3/7 (রোগী 30-46, মোট 17)
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
  'hist2025_bc2d80e597952916', 'KNE-07052025-001', '2025-05-07', '2025-05-07', '2025-05-07',
  'TAHERA BIBI', '8509791198', '', 'Kishanganj', '42', 'Female',
  'CHAKULIYA, THAKURBARI, CHAKULIYA, U.D', 'Piles', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-07T10:00:00.000Z', '2025-05-07T10:00:00.000Z'
),
(
  'hist2025_ddcbd67d9f7a0b90', 'KNE-08052025-001', '2025-05-08', '2025-05-08', '2025-05-08',
  'SAHALAM', '9971572893', '', 'Kishanganj', '25', 'Male',
  'Bahadurganj, padampur, Dighal bank, Dighal bank', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-08T10:00:00.000Z', '2025-05-08T10:00:00.000Z'
),
(
  'hist2025_39662572e0b76a0d', 'KNE-14052025-001', '2025-05-14', '2025-05-14', '2025-05-14',
  'INFIZ ALAM', '7908907082', '', 'Kishanganj', '', 'Male',
  'Madrasa SAHAPUR, Hat khola sahapur, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-14T10:00:00.000Z', '2025-05-14T10:00:00.000Z'
),
(
  'hist2025_4dae02a8960e7ab2', 'KNE-16052025-001', '2025-05-16', '2025-05-16', '2025-05-16',
  'ISKAT ALI', '6297977616', '', 'Kishanganj', '26', 'Male',
  'Goulin, Goulin, GOYALPOKHAR, U.d', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-16T10:00:00.000Z', '2025-05-16T10:00:00.000Z'
),
(
  'hist2025_db59ea44d16b434f', 'KNE-18052025-001', '2025-05-18', '2025-05-18', '2025-05-18',
  'SAFALI DAS', '9733190879', '', 'Kishanganj', '33', 'Male',
  'GOYALPOKHAR, GOHORA, GOALPOKHAR, U.D', 'Piles', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-18T10:00:00.000Z', '2025-05-18T10:00:00.000Z'
),
(
  'hist2025_abc89c9aac3990eb', 'KNE-20052025-001', '2025-05-20', '2025-05-20', '2025-05-20',
  'SALMAN KHAN', '6295680491', '', 'Kishanganj', '23', 'Male',
  'Dalkhola, Charaiya, Baisi, Purniya', 'Gupt Rog', '75000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-20T10:00:00.000Z', '2025-05-20T10:00:00.000Z'
),
(
  'hist2025_b296a5837e432655', 'KNE-22052025-001', '2025-05-22', '2025-05-22', '2025-05-22',
  'BINOD KUMAR GHOSE', '7602934215', '', 'Kishanganj', '40', 'Male',
  'Dalkhola, Potnor, Dalkhola, U.d', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-22T10:00:00.000Z', '2025-05-22T10:00:00.000Z'
),
(
  'hist2025_b7b141bc7a2247af', 'KNE-02062025-001', '2025-06-02', '2025-06-02', '2025-06-02',
  'SUFAL SAREN', '6294977796', '', 'Kishanganj', '25', 'Male',
  'Ghardhappa, Chakuliya, chakuliya, u.d', 'Gupt Rog', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-02T10:00:00.000Z', '2025-06-02T10:00:00.000Z'
),
(
  'hist2025_12be63dff54f7c6d', 'KNE-02062025-002', '2025-06-02', '2025-06-02', '2025-06-02',
  'AKHILESH MALAKAR', '8877244718', '', 'Kishanganj', '35', 'Male',
  'Dhobidin, Sunghram, Rajour, Bamka', 'Other', '28000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-02T10:00:00.000Z', '2025-06-02T10:00:00.000Z'
),
(
  'hist2025_32b47085efc7d37a', 'KNE-05062025-001', '2025-06-05', '2025-06-05', '2025-06-05',
  'MANIK SINGHA', '9382409362', '', 'Kishanganj', '40', 'Male',
  'Sathmeri, Hatkhola Sahpur, GOYALPOKHAR, U.d', 'Piles', '28000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-05T10:00:00.000Z', '2025-06-05T10:00:00.000Z'
),
(
  'hist2025_8f481f48e8b4d3b7', 'KNE-06062025-001', '2025-06-06', '2025-06-06', '2025-06-06',
  'MD DULAL', '9934637437', '', 'Kishanganj', '32', 'Male',
  'Dighalbank, Dighalbank, Dighalbank, Kishanganj', 'Piles', '42000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-06T10:00:00.000Z', '2025-06-06T10:00:00.000Z'
),
(
  'hist2025_3da8b1f7abd5303e', 'KNE-12062025-001', '2025-06-12', '2025-06-12', '2025-06-12',
  'NAJIMUL HODA', '8925261679', '', 'Kishanganj', '23', 'Male',
  'Barman, Andhoriya, DALKHOLA, U.d', 'Piles', '100000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-12T10:00:00.000Z', '2025-06-12T10:00:00.000Z'
),
(
  'hist2025_9db0d658b8405373', 'KNE-13062025-001', '2025-06-13', '2025-06-13', '2025-06-13',
  'KRISHNA KARMAKAR', '9631935125', '', 'Kishanganj', '29', 'Male',
  'Railway Colony, Churiptti, Kishanganj, Kishanganj', 'Piles', '64000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-13T10:00:00.000Z', '2025-06-13T10:00:00.000Z'
),
(
  'hist2025_791cbf75a06452fa', 'KNE-19062025-001', '2025-06-19', '2025-06-19', '2025-06-19',
  'SUMAN KUMARI', '8789193154', '', 'Kishanganj', '25', 'Female',
  'Loharpatti, Kishanganj, Kishanganj, KISHANGANJ', 'Piles', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-19T10:00:00.000Z', '2025-06-19T10:00:00.000Z'
),
(
  'hist2025_d157c5df6e0bcd89', 'KNE-20062025-001', '2025-06-20', '2025-06-20', '2025-06-20',
  'SAHAJADI', '8848852354', '', 'Kishanganj', '17', 'Male',
  'Lodhon, GOYALPOKHAR, GOYALPOKHAR, U.d', 'Piles', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-20T10:00:00.000Z', '2025-06-20T10:00:00.000Z'
),
(
  'hist2025_8288043e3779e060', 'KNE-22062025-001', '2025-06-22', '2025-06-22', '2025-06-22',
  'ANBAR HOUSSIN', '9733243745', '', 'Kishanganj', '44', 'Male',
  'Churrakutti, GORRA, GOYALPOKHAR, U.D', 'Piles', '45000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-22T10:00:00.000Z', '2025-06-22T10:00:00.000Z'
),
(
  'hist2025_0db30041f0978ac3', 'KNE-23062025-001', '2025-06-23', '2025-06-23', '2025-06-23',
  'SAHABAZ ALAM', '9391276598', '', 'Kishanganj', '28', 'Male',
  'TULSIYA, SOMESAR, BAHADURGANJ, KISHANGANJ', 'Gupt Rog', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-23T10:00:00.000Z', '2025-06-23T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'hist2025_pay_5cf67c521a4b696d', 'treatment', 'Advance', 'Advance', 'hist2025_bc2d80e597952916', '8509791198', 'Kishanganj', 'TAHERA BIBI',
  '2025-05-07', '3500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-07T10:15:00.000Z', '2025-05-07T10:15:00.000Z'
),
(
  'hist2025_pay_ec2490d9c912bd41', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_bc2d80e597952916', '8509791198', 'Kishanganj', 'TAHERA BIBI',
  '2025-05-10', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:16:00.000Z', '2025-05-10T10:16:00.000Z'
),
(
  'hist2025_pay_8394c69e091c6777', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_bc2d80e597952916', '8509791198', 'Kishanganj', 'TAHERA BIBI',
  '2025-05-15', '6500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-15T10:17:00.000Z', '2025-05-15T10:17:00.000Z'
),
(
  'hist2025_pay_d2bdf8950129f2d6', 'treatment', '4th Payment', '4th Payment', 'hist2025_bc2d80e597952916', '8509791198', 'Kishanganj', 'TAHERA BIBI',
  '2025-05-17', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:18:00.000Z', '2025-05-17T10:18:00.000Z'
),
(
  'hist2025_pay_cc95426f6f195bf1', 'treatment', '5th Payment', '5th Payment', 'hist2025_bc2d80e597952916', '8509791198', 'Kishanganj', 'TAHERA BIBI',
  '2025-05-20', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-20T10:19:00.000Z', '2025-05-20T10:19:00.000Z'
),
(
  'hist2025_pay_2735e77ad737f636', 'treatment', '6th Payment', '6th Payment', 'hist2025_bc2d80e597952916', '8509791198', 'Kishanganj', 'TAHERA BIBI',
  '2025-05-24', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-24T10:20:00.000Z', '2025-05-24T10:20:00.000Z'
),
(
  'hist2025_pay_d0456689a271032c', 'treatment', '7th Payment', '7th Payment', 'hist2025_bc2d80e597952916', '8509791198', 'Kishanganj', 'TAHERA BIBI',
  '2025-05-31', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-31T10:21:00.000Z', '2025-05-31T10:21:00.000Z'
),
(
  'hist2025_pay_41ddcfbc641e8c69', 'treatment', '8th Payment', '8th Payment', 'hist2025_bc2d80e597952916', '8509791198', 'Kishanganj', 'TAHERA BIBI',
  '2025-06-04', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-04T10:22:00.000Z', '2025-06-04T10:22:00.000Z'
),
(
  'hist2025_pay_6e1cf5b4f61b3e82', 'treatment', '9th Payment', '9th Payment', 'hist2025_bc2d80e597952916', '8509791198', 'Kishanganj', 'TAHERA BIBI',
  '2025-06-11', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-11T10:23:00.000Z', '2025-06-11T10:23:00.000Z'
),
(
  'hist2025_pay_cfd6e259d53cadef', 'treatment', '10th Payment', '10th Payment', 'hist2025_bc2d80e597952916', '8509791198', 'Kishanganj', 'TAHERA BIBI',
  '2025-06-14', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-14T10:24:00.000Z', '2025-06-14T10:24:00.000Z'
),
(
  'hist2025_pay_3a5c248fcb5297bc', 'treatment', 'Advance', 'Advance', 'hist2025_ddcbd67d9f7a0b90', '9971572893', 'Kishanganj', 'SAHALAM',
  '2025-05-08', '2500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-08T10:15:00.000Z', '2025-05-08T10:15:00.000Z'
),
(
  'hist2025_pay_eb40fef54e6fb020', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_ddcbd67d9f7a0b90', '9971572893', 'Kishanganj', 'SAHALAM',
  '2025-05-10', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:16:00.000Z', '2025-05-10T10:16:00.000Z'
),
(
  'hist2025_pay_7ea96dbeecfb475a', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_ddcbd67d9f7a0b90', '9971572893', 'Kishanganj', 'SAHALAM',
  '2025-05-14', '20000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-14T10:17:00.000Z', '2025-05-14T10:17:00.000Z'
),
(
  'hist2025_pay_8c3040a33c9d8bd0', 'treatment', '4th Payment', '4th Payment', 'hist2025_ddcbd67d9f7a0b90', '9971572893', 'Kishanganj', 'SAHALAM',
  '2025-05-21', '2500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-21T10:18:00.000Z', '2025-05-21T10:18:00.000Z'
),
(
  'hist2025_pay_940cab1a6ec18096', 'treatment', '5th Payment', '5th Payment', 'hist2025_ddcbd67d9f7a0b90', '9971572893', 'Kishanganj', 'SAHALAM',
  '2025-05-24', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-24T10:19:00.000Z', '2025-05-24T10:19:00.000Z'
),
(
  'hist2025_pay_4bcb17850b6cd360', 'treatment', '6th Payment', '6th Payment', 'hist2025_ddcbd67d9f7a0b90', '9971572893', 'Kishanganj', 'SAHALAM',
  '2025-06-05', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-05T10:20:00.000Z', '2025-06-05T10:20:00.000Z'
),
(
  'hist2025_pay_46d5232c593959d2', 'treatment', '7th Payment', '7th Payment', 'hist2025_ddcbd67d9f7a0b90', '9971572893', 'Kishanganj', 'SAHALAM',
  '2025-06-14', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-14T10:21:00.000Z', '2025-06-14T10:21:00.000Z'
),
(
  'hist2025_pay_177c1f04e7691401', 'treatment', 'Advance', 'Advance', 'hist2025_39662572e0b76a0d', '7908907082', 'Kishanganj', 'INFIZ ALAM',
  '2025-05-14', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-14T10:15:00.000Z', '2025-05-14T10:15:00.000Z'
),
(
  'hist2025_pay_00c40fd326151850', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_39662572e0b76a0d', '7908907082', 'Kishanganj', 'INFIZ ALAM',
  '2025-05-15', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-15T10:16:00.000Z', '2025-05-15T10:16:00.000Z'
),
(
  'hist2025_pay_74084d0869903bed', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_39662572e0b76a0d', '7908907082', 'Kishanganj', 'INFIZ ALAM',
  '2025-05-21', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-21T10:17:00.000Z', '2025-05-21T10:17:00.000Z'
),
(
  'hist2025_pay_1a59ace9a6e37856', 'treatment', '4th Payment', '4th Payment', 'hist2025_39662572e0b76a0d', '7908907082', 'Kishanganj', 'INFIZ ALAM',
  '2025-05-24', '3500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-24T10:18:00.000Z', '2025-05-24T10:18:00.000Z'
),
(
  'hist2025_pay_b3cace17fd3f4d1a', 'treatment', '5th Payment', '5th Payment', 'hist2025_39662572e0b76a0d', '7908907082', 'Kishanganj', 'INFIZ ALAM',
  '2025-05-29', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-29T10:19:00.000Z', '2025-05-29T10:19:00.000Z'
),
(
  'hist2025_pay_a3be15792f8d267d', 'treatment', '6th Payment', '6th Payment', 'hist2025_39662572e0b76a0d', '7908907082', 'Kishanganj', 'INFIZ ALAM',
  '2025-06-06', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-06T10:20:00.000Z', '2025-06-06T10:20:00.000Z'
),
(
  'hist2025_pay_a3ecb3da0fe110f6', 'treatment', '7th Payment', '7th Payment', 'hist2025_39662572e0b76a0d', '7908907082', 'Kishanganj', 'INFIZ ALAM',
  '2025-06-11', '6000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-11T10:21:00.000Z', '2025-06-11T10:21:00.000Z'
),
(
  'hist2025_pay_5e93168674881a98', 'treatment', '8th Payment', '8th Payment', 'hist2025_39662572e0b76a0d', '7908907082', 'Kishanganj', 'INFIZ ALAM',
  '2025-06-18', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-18T10:22:00.000Z', '2025-06-18T10:22:00.000Z'
),
(
  'hist2025_pay_c95eb04b8d305865', 'treatment', '9th Payment', '9th Payment', 'hist2025_39662572e0b76a0d', '7908907082', 'Kishanganj', 'INFIZ ALAM',
  '2025-11-15', '11000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:23:00.000Z', '2025-11-15T10:23:00.000Z'
),
(
  'hist2025_pay_06447cbd7191d38e', 'treatment', 'Advance', 'Advance', 'hist2025_4dae02a8960e7ab2', '6297977616', 'Kishanganj', 'ISKAT ALI',
  '2025-05-16', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-16T10:15:00.000Z', '2025-05-16T10:15:00.000Z'
),
(
  'hist2025_pay_003c0371f164f9a6', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_4dae02a8960e7ab2', '6297977616', 'Kishanganj', 'ISKAT ALI',
  '2025-05-23', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-23T10:16:00.000Z', '2025-05-23T10:16:00.000Z'
),
(
  'hist2025_pay_0c9491f19640b867', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_4dae02a8960e7ab2', '6297977616', 'Kishanganj', 'ISKAT ALI',
  '2025-05-30', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-30T10:17:00.000Z', '2025-05-30T10:17:00.000Z'
),
(
  'hist2025_pay_966370aaa96c6baa', 'treatment', 'Advance', 'Advance', 'hist2025_db59ea44d16b434f', '9733190879', 'Kishanganj', 'SAFALI DAS',
  '2025-05-18', '6000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-18T10:15:00.000Z', '2025-05-18T10:15:00.000Z'
),
(
  'hist2025_pay_7de54c3773158a96', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_db59ea44d16b434f', '9733190879', 'Kishanganj', 'SAFALI DAS',
  '2025-06-18', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-18T10:16:00.000Z', '2025-06-18T10:16:00.000Z'
),
(
  'hist2025_pay_3edc3ede2008d605', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_db59ea44d16b434f', '9733190879', 'Kishanganj', 'SAFALI DAS',
  '2025-07-03', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-03T10:17:00.000Z', '2025-07-03T10:17:00.000Z'
),
(
  'hist2025_pay_9233159df44886c7', 'treatment', '4th Payment', '4th Payment', 'hist2025_db59ea44d16b434f', '9733190879', 'Kishanganj', 'SAFALI DAS',
  '2025-07-05', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:18:00.000Z', '2025-07-05T10:18:00.000Z'
),
(
  'hist2025_pay_c257a3d5e618160b', 'treatment', '5th Payment', '5th Payment', 'hist2025_db59ea44d16b434f', '9733190879', 'Kishanganj', 'SAFALI DAS',
  '2025-07-16', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-16T10:19:00.000Z', '2025-07-16T10:19:00.000Z'
),
(
  'hist2025_pay_997814a02f38e315', 'treatment', '6th Payment', '6th Payment', 'hist2025_db59ea44d16b434f', '9733190879', 'Kishanganj', 'SAFALI DAS',
  '2025-07-23', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-23T10:20:00.000Z', '2025-07-23T10:20:00.000Z'
),
(
  'hist2025_pay_b0a99823f10dd887', 'treatment', '7th Payment', '7th Payment', 'hist2025_db59ea44d16b434f', '9733190879', 'Kishanganj', 'SAFALI DAS',
  '2025-08-13', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-13T10:21:00.000Z', '2025-08-13T10:21:00.000Z'
),
(
  'hist2025_pay_041a97f60b38873a', 'treatment', '8th Payment', '8th Payment', 'hist2025_db59ea44d16b434f', '9733190879', 'Kishanganj', 'SAFALI DAS',
  '2025-09-24', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-24T10:22:00.000Z', '2025-09-24T10:22:00.000Z'
),
(
  'hist2025_pay_a1a472b896f1cd72', 'treatment', '9th Payment', '9th Payment', 'hist2025_db59ea44d16b434f', '9733190879', 'Kishanganj', 'SAFALI DAS',
  '2025-11-05', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-05T10:23:00.000Z', '2025-11-05T10:23:00.000Z'
),
(
  'hist2025_pay_c9c7b95c7f4fcaeb', 'treatment', 'Advance', 'Advance', 'hist2025_abc89c9aac3990eb', '6295680491', 'Kishanganj', 'SALMAN KHAN',
  '2025-05-20', '25000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-20T10:15:00.000Z', '2025-05-20T10:15:00.000Z'
),
(
  'hist2025_pay_553af80398bfa304', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_abc89c9aac3990eb', '6295680491', 'Kishanganj', 'SALMAN KHAN',
  '2025-06-19', '15000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-19T10:16:00.000Z', '2025-06-19T10:16:00.000Z'
),
(
  'hist2025_pay_0effd508c211d8c7', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_abc89c9aac3990eb', '6295680491', 'Kishanganj', 'SALMAN KHAN',
  '2025-07-14', '35000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-14T10:17:00.000Z', '2025-07-14T10:17:00.000Z'
),
(
  'hist2025_pay_b28d2fd705b03034', 'treatment', 'Advance', 'Advance', 'hist2025_b296a5837e432655', '7602934215', 'Kishanganj', 'BINOD KUMAR GHOSE',
  '2025-05-22', '8000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-22T10:15:00.000Z', '2025-05-22T10:15:00.000Z'
),
(
  'hist2025_pay_4ebb70d673392bcd', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_b296a5837e432655', '7602934215', 'Kishanganj', 'BINOD KUMAR GHOSE',
  '2025-05-25', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-25T10:16:00.000Z', '2025-05-25T10:16:00.000Z'
),
(
  'hist2025_pay_7dc4f1a6f88e156b', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_b296a5837e432655', '7602934215', 'Kishanganj', 'BINOD KUMAR GHOSE',
  '2025-05-28', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-28T10:17:00.000Z', '2025-05-28T10:17:00.000Z'
),
(
  'hist2025_pay_92927a8cf893d9e2', 'treatment', '4th Payment', '4th Payment', 'hist2025_b296a5837e432655', '7602934215', 'Kishanganj', 'BINOD KUMAR GHOSE',
  '2025-06-11', '4000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-11T10:18:00.000Z', '2025-06-11T10:18:00.000Z'
),
(
  'hist2025_pay_14a77d1e9242681e', 'treatment', '5th Payment', '5th Payment', 'hist2025_b296a5837e432655', '7602934215', 'Kishanganj', 'BINOD KUMAR GHOSE',
  '2025-06-18', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-18T10:19:00.000Z', '2025-06-18T10:19:00.000Z'
),
(
  'hist2025_pay_feceef147e023d2e', 'treatment', '6th Payment', '6th Payment', 'hist2025_b296a5837e432655', '7602934215', 'Kishanganj', 'BINOD KUMAR GHOSE',
  '2025-07-02', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-02T10:20:00.000Z', '2025-07-02T10:20:00.000Z'
),
(
  'hist2025_pay_3aa7618cc7c3e053', 'treatment', 'Advance', 'Advance', 'hist2025_b7b141bc7a2247af', '6294977796', 'Kishanganj', 'SUFAL SAREN',
  '2025-06-02', '7000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-02T10:15:00.000Z', '2025-06-02T10:15:00.000Z'
),
(
  'hist2025_pay_a1b6060a9d82496b', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_b7b141bc7a2247af', '6294977796', 'Kishanganj', 'SUFAL SAREN',
  '2025-06-04', '7000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-04T10:16:00.000Z', '2025-06-04T10:16:00.000Z'
),
(
  'hist2025_pay_01a3b6aeac09b9e6', 'treatment', 'Advance', 'Advance', 'hist2025_12be63dff54f7c6d', '8877244718', 'Kishanganj', 'AKHILESH MALAKAR',
  '2025-06-02', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-02T10:15:00.000Z', '2025-06-02T10:15:00.000Z'
),
(
  'hist2025_pay_ee9e1b0a5ab803cc', 'treatment', 'Advance', 'Advance', 'hist2025_32b47085efc7d37a', '9382409362', 'Kishanganj', 'MANIK SINGHA',
  '2025-06-05', '15000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-05T10:15:00.000Z', '2025-06-05T10:15:00.000Z'
),
(
  'hist2025_pay_1a2d8b2c9aeadb62', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_32b47085efc7d37a', '9382409362', 'Kishanganj', 'MANIK SINGHA',
  '2025-06-11', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-11T10:16:00.000Z', '2025-06-11T10:16:00.000Z'
),
(
  'hist2025_pay_dc187374b4be1784', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_32b47085efc7d37a', '9382409362', 'Kishanganj', 'MANIK SINGHA',
  '2025-06-14', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-14T10:17:00.000Z', '2025-06-14T10:17:00.000Z'
),
(
  'hist2025_pay_b5e75ed13fc017a7', 'treatment', '4th Payment', '4th Payment', 'hist2025_32b47085efc7d37a', '9382409362', 'Kishanganj', 'MANIK SINGHA',
  '2025-06-18', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-18T10:18:00.000Z', '2025-06-18T10:18:00.000Z'
),
(
  'hist2025_pay_b233b234bf0c3593', 'treatment', '5th Payment', '5th Payment', 'hist2025_32b47085efc7d37a', '9382409362', 'Kishanganj', 'MANIK SINGHA',
  '2025-06-21', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:19:00.000Z', '2025-06-21T10:19:00.000Z'
),
(
  'hist2025_pay_38861f65c30d5ed1', 'treatment', '6th Payment', '6th Payment', 'hist2025_32b47085efc7d37a', '9382409362', 'Kishanganj', 'MANIK SINGHA',
  '2025-06-23', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-23T10:20:00.000Z', '2025-06-23T10:20:00.000Z'
),
(
  'hist2025_pay_b4e8abea20f591ae', 'treatment', 'Advance', 'Advance', 'hist2025_8f481f48e8b4d3b7', '9934637437', 'Kishanganj', 'MD DULAL',
  '2025-06-06', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-06T10:15:00.000Z', '2025-06-06T10:15:00.000Z'
),
(
  'hist2025_pay_2e5458a5659d933c', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_8f481f48e8b4d3b7', '9934637437', 'Kishanganj', 'MD DULAL',
  '2025-06-11', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-11T10:16:00.000Z', '2025-06-11T10:16:00.000Z'
),
(
  'hist2025_pay_fdcfb36d4562153c', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_8f481f48e8b4d3b7', '9934637437', 'Kishanganj', 'MD DULAL',
  '2025-06-14', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-14T10:17:00.000Z', '2025-06-14T10:17:00.000Z'
),
(
  'hist2025_pay_056d9323e507c52c', 'treatment', '4th Payment', '4th Payment', 'hist2025_8f481f48e8b4d3b7', '9934637437', 'Kishanganj', 'MD DULAL',
  '2025-06-18', '6500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-18T10:18:00.000Z', '2025-06-18T10:18:00.000Z'
),
(
  'hist2025_pay_1425fb3ddbac4943', 'treatment', '5th Payment', '5th Payment', 'hist2025_8f481f48e8b4d3b7', '9934637437', 'Kishanganj', 'MD DULAL',
  '2025-06-21', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:19:00.000Z', '2025-06-21T10:19:00.000Z'
),
(
  'hist2025_pay_ef532d981715cf09', 'treatment', '6th Payment', '6th Payment', 'hist2025_8f481f48e8b4d3b7', '9934637437', 'Kishanganj', 'MD DULAL',
  '2025-06-25', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-25T10:20:00.000Z', '2025-06-25T10:20:00.000Z'
),
(
  'hist2025_pay_e330c55ce3beb912', 'treatment', '7th Payment', '7th Payment', 'hist2025_8f481f48e8b4d3b7', '9934637437', 'Kishanganj', 'MD DULAL',
  '2025-06-28', '2500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:21:00.000Z', '2025-06-28T10:21:00.000Z'
),
(
  'hist2025_pay_ceba96ce2d6ad60e', 'treatment', '8th Payment', '8th Payment', 'hist2025_8f481f48e8b4d3b7', '9934637437', 'Kishanganj', 'MD DULAL',
  '2025-07-02', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-02T10:22:00.000Z', '2025-07-02T10:22:00.000Z'
),
(
  'hist2025_pay_99260382981f1ec7', 'treatment', '9th Payment', '9th Payment', 'hist2025_8f481f48e8b4d3b7', '9934637437', 'Kishanganj', 'MD DULAL',
  '2025-07-10', '2500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-10T10:23:00.000Z', '2025-07-10T10:23:00.000Z'
),
(
  'hist2025_pay_e35d4905f1675a3e', 'treatment', '10th Payment', '10th Payment', 'hist2025_8f481f48e8b4d3b7', '9934637437', 'Kishanganj', 'MD DULAL',
  '2025-07-16', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-16T10:24:00.000Z', '2025-07-16T10:24:00.000Z'
),
(
  'hist2025_pay_9542fe7e38b27df7', 'treatment', '11th Payment', '11th Payment', 'hist2025_8f481f48e8b4d3b7', '9934637437', 'Kishanganj', 'MD DULAL',
  '2025-07-23', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-23T10:25:00.000Z', '2025-07-23T10:25:00.000Z'
),
(
  'hist2025_pay_6aa0c736fa5b6076', 'treatment', '12th Payment', '12th Payment', 'hist2025_8f481f48e8b4d3b7', '9934637437', 'Kishanganj', 'MD DULAL',
  '2025-07-31', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-31T10:26:00.000Z', '2025-07-31T10:26:00.000Z'
),
(
  'hist2025_pay_10576935df966749', 'treatment', 'Advance', 'Advance', 'hist2025_3da8b1f7abd5303e', '8925261679', 'Kishanganj', 'NAJIMUL HODA',
  '2025-06-12', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-12T10:15:00.000Z', '2025-06-12T10:15:00.000Z'
),
(
  'hist2025_pay_89297204e8754c3c', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_3da8b1f7abd5303e', '8925261679', 'Kishanganj', 'NAJIMUL HODA',
  '2025-06-14', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-14T10:16:00.000Z', '2025-06-14T10:16:00.000Z'
),
(
  'hist2025_pay_95e2b33c9d46e00c', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_3da8b1f7abd5303e', '8925261679', 'Kishanganj', 'NAJIMUL HODA',
  '2025-06-18', '25000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-18T10:17:00.000Z', '2025-06-18T10:17:00.000Z'
),
(
  'hist2025_pay_cfd35663e33ccec1', 'treatment', '4th Payment', '4th Payment', 'hist2025_3da8b1f7abd5303e', '8925261679', 'Kishanganj', 'NAJIMUL HODA',
  '2025-06-21', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:18:00.000Z', '2025-06-21T10:18:00.000Z'
),
(
  'hist2025_pay_213aef2b2fe688b6', 'treatment', '5th Payment', '5th Payment', 'hist2025_3da8b1f7abd5303e', '8925261679', 'Kishanganj', 'NAJIMUL HODA',
  '2025-06-25', '13000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-25T10:19:00.000Z', '2025-06-25T10:19:00.000Z'
),
(
  'hist2025_pay_08a1c9c0d2df8db8', 'treatment', '6th Payment', '6th Payment', 'hist2025_3da8b1f7abd5303e', '8925261679', 'Kishanganj', 'NAJIMUL HODA',
  '2025-06-27', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-27T10:20:00.000Z', '2025-06-27T10:20:00.000Z'
),
(
  'hist2025_pay_32184ed4f959c2d6', 'treatment', '7th Payment', '7th Payment', 'hist2025_3da8b1f7abd5303e', '8925261679', 'Kishanganj', 'NAJIMUL HODA',
  '2025-07-05', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:21:00.000Z', '2025-07-05T10:21:00.000Z'
),
(
  'hist2025_pay_5ca7b37c336bc7d5', 'treatment', '8th Payment', '8th Payment', 'hist2025_3da8b1f7abd5303e', '8925261679', 'Kishanganj', 'NAJIMUL HODA',
  '2025-07-09', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-09T10:22:00.000Z', '2025-07-09T10:22:00.000Z'
),
(
  'hist2025_pay_38692a0675ff71cd', 'treatment', '9th Payment', '9th Payment', 'hist2025_3da8b1f7abd5303e', '8925261679', 'Kishanganj', 'NAJIMUL HODA',
  '2025-07-16', '13000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-16T10:23:00.000Z', '2025-07-16T10:23:00.000Z'
),
(
  'hist2025_pay_3124a0353b9ce4c3', 'treatment', '10th Payment', '10th Payment', 'hist2025_3da8b1f7abd5303e', '8925261679', 'Kishanganj', 'NAJIMUL HODA',
  '2025-07-22', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-22T10:24:00.000Z', '2025-07-22T10:24:00.000Z'
),
(
  'hist2025_pay_4a4c24226b455876', 'treatment', '11th Payment', '11th Payment', 'hist2025_3da8b1f7abd5303e', '8925261679', 'Kishanganj', 'NAJIMUL HODA',
  '2025-07-30', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-30T10:25:00.000Z', '2025-07-30T10:25:00.000Z'
),
(
  'hist2025_pay_db0e5baa7777945f', 'treatment', '12th Payment', '12th Payment', 'hist2025_3da8b1f7abd5303e', '8925261679', 'Kishanganj', 'NAJIMUL HODA',
  '2025-08-06', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-06T10:26:00.000Z', '2025-08-06T10:26:00.000Z'
),
(
  'hist2025_pay_3c0a9a0870eed1bd', 'treatment', '13th Payment', '13th Payment', 'hist2025_3da8b1f7abd5303e', '8925261679', 'Kishanganj', 'NAJIMUL HODA',
  '2025-08-11', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-11T10:27:00.000Z', '2025-08-11T10:27:00.000Z'
),
(
  'hist2025_pay_f20e2be58c6d70ba', 'treatment', '14th Payment', '14th Payment', 'hist2025_3da8b1f7abd5303e', '8925261679', 'Kishanganj', 'NAJIMUL HODA',
  '2025-09-03', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-03T10:28:00.000Z', '2025-09-03T10:28:00.000Z'
),
(
  'hist2025_pay_8bb6e3a8afe2bd5d', 'treatment', 'Advance', 'Advance', 'hist2025_9db0d658b8405373', '9631935125', 'Kishanganj', 'KRISHNA KARMAKAR',
  '2025-06-14', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-14T10:15:00.000Z', '2025-06-14T10:15:00.000Z'
),
(
  'hist2025_pay_7db3671eee87d327', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_9db0d658b8405373', '9631935125', 'Kishanganj', 'KRISHNA KARMAKAR',
  '2025-06-18', '15000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-18T10:16:00.000Z', '2025-06-18T10:16:00.000Z'
),
(
  'hist2025_pay_9107d6c4470493f0', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_9db0d658b8405373', '9631935125', 'Kishanganj', 'KRISHNA KARMAKAR',
  '2025-06-21', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:17:00.000Z', '2025-06-21T10:17:00.000Z'
),
(
  'hist2025_pay_ad857e2db44cec13', 'treatment', '4th Payment', '4th Payment', 'hist2025_9db0d658b8405373', '9631935125', 'Kishanganj', 'KRISHNA KARMAKAR',
  '2025-06-25', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-25T10:18:00.000Z', '2025-06-25T10:18:00.000Z'
),
(
  'hist2025_pay_2c2f4a2b2974cefa', 'treatment', '5th Payment', '5th Payment', 'hist2025_9db0d658b8405373', '9631935125', 'Kishanganj', 'KRISHNA KARMAKAR',
  '2025-07-05', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:19:00.000Z', '2025-07-05T10:19:00.000Z'
),
(
  'hist2025_pay_3a3c63622311a474', 'treatment', '6th Payment', '6th Payment', 'hist2025_9db0d658b8405373', '9631935125', 'Kishanganj', 'KRISHNA KARMAKAR',
  '2025-07-09', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-09T10:20:00.000Z', '2025-07-09T10:20:00.000Z'
),
(
  'hist2025_pay_d22cfcb6409feb65', 'treatment', '7th Payment', '7th Payment', 'hist2025_9db0d658b8405373', '9631935125', 'Kishanganj', 'KRISHNA KARMAKAR',
  '2025-07-10', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-10T10:21:00.000Z', '2025-07-10T10:21:00.000Z'
),
(
  'hist2025_pay_117212318d93a431', 'treatment', '8th Payment', '8th Payment', 'hist2025_9db0d658b8405373', '9631935125', 'Kishanganj', 'KRISHNA KARMAKAR',
  '2025-07-12', '4500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-12T10:22:00.000Z', '2025-07-12T10:22:00.000Z'
),
(
  'hist2025_pay_03f8529b0c431529', 'treatment', '9th Payment', '9th Payment', 'hist2025_9db0d658b8405373', '9631935125', 'Kishanganj', 'KRISHNA KARMAKAR',
  '2025-07-15', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-15T10:23:00.000Z', '2025-07-15T10:23:00.000Z'
),
(
  'hist2025_pay_3066b2eb855b7fca', 'treatment', '10th Payment', '10th Payment', 'hist2025_9db0d658b8405373', '9631935125', 'Kishanganj', 'KRISHNA KARMAKAR',
  '2025-07-18', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-18T10:24:00.000Z', '2025-07-18T10:24:00.000Z'
),
(
  'hist2025_pay_86dd15e22b663126', 'treatment', '11th Payment', '11th Payment', 'hist2025_9db0d658b8405373', '9631935125', 'Kishanganj', 'KRISHNA KARMAKAR',
  '2025-07-23', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-23T10:25:00.000Z', '2025-07-23T10:25:00.000Z'
),
(
  'hist2025_pay_e7b88e123409d047', 'treatment', '12th Payment', '12th Payment', 'hist2025_9db0d658b8405373', '9631935125', 'Kishanganj', 'KRISHNA KARMAKAR',
  '2025-07-30', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-30T10:26:00.000Z', '2025-07-30T10:26:00.000Z'
),
(
  'hist2025_pay_3a6c1b15ac1544f9', 'treatment', 'Advance', 'Advance', 'hist2025_791cbf75a06452fa', '8789193154', 'Kishanganj', 'SUMAN KUMARI',
  '2025-06-20', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-20T10:15:00.000Z', '2025-06-20T10:15:00.000Z'
),
(
  'hist2025_pay_01cbf95aeaa51da0', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_791cbf75a06452fa', '8789193154', 'Kishanganj', 'SUMAN KUMARI',
  '2025-06-25', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-25T10:16:00.000Z', '2025-06-25T10:16:00.000Z'
),
(
  'hist2025_pay_ca7813993326c441', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_791cbf75a06452fa', '8789193154', 'Kishanganj', 'SUMAN KUMARI',
  '2025-07-02', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-02T10:17:00.000Z', '2025-07-02T10:17:00.000Z'
),
(
  'hist2025_pay_6fd81e85e94c1515', 'treatment', '4th Payment', '4th Payment', 'hist2025_791cbf75a06452fa', '8789193154', 'Kishanganj', 'SUMAN KUMARI',
  '2025-07-16', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-16T10:18:00.000Z', '2025-07-16T10:18:00.000Z'
),
(
  'hist2025_pay_cb4bbe5ed4f41a02', 'treatment', '5th Payment', '5th Payment', 'hist2025_791cbf75a06452fa', '8789193154', 'Kishanganj', 'SUMAN KUMARI',
  '2025-07-20', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-20T10:19:00.000Z', '2025-07-20T10:19:00.000Z'
),
(
  'hist2025_pay_f02fa4e78bde530e', 'treatment', 'Advance', 'Advance', 'hist2025_d157c5df6e0bcd89', '8848852354', 'Kishanganj', 'SAHAJADI',
  '2025-07-23', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-23T10:15:00.000Z', '2025-07-23T10:15:00.000Z'
),
(
  'hist2025_pay_7c33ba82b8fffdd2', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_d157c5df6e0bcd89', '8848852354', 'Kishanganj', 'SAHAJADI',
  '2025-07-27', '7000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-27T10:16:00.000Z', '2025-07-27T10:16:00.000Z'
),
(
  'hist2025_pay_feebf71f2814c115', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_d157c5df6e0bcd89', '8848852354', 'Kishanganj', 'SAHAJADI',
  '2025-08-02', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:17:00.000Z', '2025-08-02T10:17:00.000Z'
),
(
  'hist2025_pay_f4f63a19effc8114', 'treatment', '4th Payment', '4th Payment', 'hist2025_d157c5df6e0bcd89', '8848852354', 'Kishanganj', 'SAHAJADI',
  '2025-08-05', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-05T10:18:00.000Z', '2025-08-05T10:18:00.000Z'
),
(
  'hist2025_pay_63f9f4d413eafb8e', 'treatment', '5th Payment', '5th Payment', 'hist2025_d157c5df6e0bcd89', '8848852354', 'Kishanganj', 'SAHAJADI',
  '2025-08-09', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-09T10:19:00.000Z', '2025-08-09T10:19:00.000Z'
),
(
  'hist2025_pay_16765c188ca1d42f', 'treatment', '6th Payment', '6th Payment', 'hist2025_d157c5df6e0bcd89', '8848852354', 'Kishanganj', 'SAHAJADI',
  '2025-08-12', '1000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-12T10:20:00.000Z', '2025-08-12T10:20:00.000Z'
),
(
  'hist2025_pay_451249c190fd7876', 'treatment', '7th Payment', '7th Payment', 'hist2025_d157c5df6e0bcd89', '8848852354', 'Kishanganj', 'SAHAJADI',
  '2025-08-16', '1500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:21:00.000Z', '2025-08-16T10:21:00.000Z'
),
(
  'hist2025_pay_d7c227d6f1803c41', 'treatment', '8th Payment', '8th Payment', 'hist2025_d157c5df6e0bcd89', '8848852354', 'Kishanganj', 'SAHAJADI',
  '2025-08-02', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:22:00.000Z', '2025-08-02T10:22:00.000Z'
),
(
  'hist2025_pay_efb0431edb14dead', 'treatment', '9th Payment', '9th Payment', 'hist2025_d157c5df6e0bcd89', '8848852354', 'Kishanganj', 'SAHAJADI',
  '2025-08-29', '500', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-29T10:23:00.000Z', '2025-08-29T10:23:00.000Z'
),
(
  'hist2025_pay_64ac82cff21d52fe', 'treatment', 'Advance', 'Advance', 'hist2025_8288043e3779e060', '9733243745', 'Kishanganj', 'ANBAR HOUSSIN',
  '2025-06-22', '2000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-22T10:15:00.000Z', '2025-06-22T10:15:00.000Z'
),
(
  'hist2025_pay_28c6831e912cce94', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_8288043e3779e060', '9733243745', 'Kishanganj', 'ANBAR HOUSSIN',
  '2025-06-24', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-24T10:16:00.000Z', '2025-06-24T10:16:00.000Z'
),
(
  'hist2025_pay_217fbaf573abf4f3', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_8288043e3779e060', '9733243745', 'Kishanganj', 'ANBAR HOUSSIN',
  '2025-07-04', '3900', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-04T10:17:00.000Z', '2025-07-04T10:17:00.000Z'
),
(
  'hist2025_pay_370acf278f9a4c78', 'treatment', 'Advance', 'Advance', 'hist2025_0db30041f0978ac3', '9391276598', 'Kishanganj', 'SAHABAZ ALAM',
  '2025-06-23', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-23T10:15:00.000Z', '2025-06-23T10:15:00.000Z'
),
(
  'hist2025_pay_80a22b01fd7c460f', 'treatment', '2nd Payment', '2nd Payment', 'hist2025_0db30041f0978ac3', '9391276598', 'Kishanganj', 'SAHABAZ ALAM',
  '2025-06-05', '5000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-05T10:16:00.000Z', '2025-06-05T10:16:00.000Z'
),
(
  'hist2025_pay_3a3d94684a2c3847', 'treatment', '3rd Payment', '3rd Payment', 'hist2025_0db30041f0978ac3', '9391276598', 'Kishanganj', 'SAHABAZ ALAM',
  '2025-07-09', '7000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-09T10:17:00.000Z', '2025-07-09T10:17:00.000Z'
),
(
  'hist2025_pay_55e2004f61fa79fb', 'treatment', '4th Payment', '4th Payment', 'hist2025_0db30041f0978ac3', '9391276598', 'Kishanganj', 'SAHABAZ ALAM',
  '2025-07-25', '10000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-25T10:18:00.000Z', '2025-07-25T10:18:00.000Z'
),
(
  'hist2025_pay_6c3f29f5d7e7374d', 'treatment', '5th Payment', '5th Payment', 'hist2025_0db30041f0978ac3', '9391276598', 'Kishanganj', 'SAHABAZ ALAM',
  '2025-08-13', '3000', 'CASH', 'Historical import (2025 Google Sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-13T10:19:00.000Z', '2025-08-13T10:19:00.000Z'
);
