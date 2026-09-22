-- ২০২৫ শিটের ৩ জন -- লাইভ অ্যাপের বর্তমান রোগীর প্রোফাইলেই পুরনো
-- পেমেন্ট যোগ (TK নিশ্চিত করেছেন এরা একই মানুষ, বানান একটু আলাদা ছিল
-- শুধু -- SAHJUL ALAM, KANAK LAL MANDAL/MONDAL, SAGAR KUMAR SINGH/SHING)।
-- patients টেবিলে হাত পড়েনি, শুধু payments-এ নতুন সারি -- আসল patientId
-- খুঁজে (select) বসানো হচ্ছে, তাই ভুল UUID বসার ঝুঁকি নেই।

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'hist_pay_107b5186e9184ca4', 'treatment', 'Advance', 'Advance',
  (select id from public.patients where "patientId" = 'KNE-19082026-003'),
  '6203558117', 'Kishanganj', 'SAHJUL ALAM',
  '2025-05-20', '5000', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-20T10:15:00.000Z', '2025-05-20T10:15:00.000Z'
),
(
  'hist_pay_1a0cf782f815f283', 'treatment', '2nd Payment', '2nd Payment',
  (select id from public.patients where "patientId" = 'KNE-19082026-003'),
  '6203558117', 'Kishanganj', 'SAHJUL ALAM',
  '2025-05-24', '5000', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-24T10:16:00.000Z', '2025-05-24T10:16:00.000Z'
),
(
  'hist_pay_787de475117b0504', 'treatment', '3rd Payment', '3rd Payment',
  (select id from public.patients where "patientId" = 'KNE-19082026-003'),
  '6203558117', 'Kishanganj', 'SAHJUL ALAM',
  '2025-05-31', '5000', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-31T10:17:00.000Z', '2025-05-31T10:17:00.000Z'
),
(
  'hist_pay_4c10cdea5a18624c', 'treatment', '4th Payment', '4th Payment',
  (select id from public.patients where "patientId" = 'KNE-19082026-003'),
  '6203558117', 'Kishanganj', 'SAHJUL ALAM',
  '2025-06-04', '8000', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-04T10:18:00.000Z', '2025-06-04T10:18:00.000Z'
),
(
  'hist_pay_9bef3217dce68836', 'treatment', '5th Payment', '5th Payment',
  (select id from public.patients where "patientId" = 'KNE-19082026-003'),
  '6203558117', 'Kishanganj', 'SAHJUL ALAM',
  '2025-06-09', '5000', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-09T10:19:00.000Z', '2025-06-09T10:19:00.000Z'
),
(
  'hist_pay_2d38a06c34ad69aa', 'treatment', '6th Payment', '6th Payment',
  (select id from public.patients where "patientId" = 'KNE-19082026-003'),
  '6203558117', 'Kishanganj', 'SAHJUL ALAM',
  '2025-06-14', '5000', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-14T10:20:00.000Z', '2025-06-14T10:20:00.000Z'
),
(
  'hist_pay_b3c7eb9068ace0ef', 'treatment', '7th Payment', '7th Payment',
  (select id from public.patients where "patientId" = 'KNE-19082026-003'),
  '6203558117', 'Kishanganj', 'SAHJUL ALAM',
  '2025-06-18', '2000', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-18T10:21:00.000Z', '2025-06-18T10:21:00.000Z'
),
(
  'hist_pay_81248b7d00eb998e', 'treatment', '8th Payment', '8th Payment',
  (select id from public.patients where "patientId" = 'KNE-19082026-003'),
  '6203558117', 'Kishanganj', 'SAHJUL ALAM',
  '2025-06-21', '2000', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:22:00.000Z', '2025-06-21T10:22:00.000Z'
),
(
  'hist_pay_d575c46d7784a23d', 'treatment', '9th Payment', '9th Payment',
  (select id from public.patients where "patientId" = 'KNE-19082026-003'),
  '6203558117', 'Kishanganj', 'SAHJUL ALAM',
  '2025-06-28', '5000', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:23:00.000Z', '2025-06-28T10:23:00.000Z'
),
(
  'hist_pay_e33570b04ac89f5f', 'treatment', '10th Payment', '10th Payment',
  (select id from public.patients where "patientId" = 'KNE-19082026-003'),
  '6203558117', 'Kishanganj', 'SAHJUL ALAM',
  '2025-07-10', '2000', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-10T10:24:00.000Z', '2025-07-10T10:24:00.000Z'
),
(
  'hist_pay_178fe32d9ee4e89b', 'treatment', 'Advance', 'Advance',
  (select id from public.patients where "patientId" = 'KNE-27062026-004'),
  '8292788091', 'Kishanganj', 'KANAK LAL MANDAL',
  '2025-01-06', '2500', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-06T10:15:00.000Z', '2025-01-06T10:15:00.000Z'
),
(
  'hist_pay_440cb3eddedcc08e', 'treatment', '2nd Payment', '2nd Payment',
  (select id from public.patients where "patientId" = 'KNE-27062026-004'),
  '8292788091', 'Kishanganj', 'KANAK LAL MANDAL',
  '2025-03-22', '4000', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-22T10:16:00.000Z', '2025-03-22T10:16:00.000Z'
),
(
  'hist_pay_1a3a994b1bca0325', 'treatment', '3rd Payment', '3rd Payment',
  (select id from public.patients where "patientId" = 'KNE-27062026-004'),
  '8292788091', 'Kishanganj', 'KANAK LAL MANDAL',
  '2025-03-29', '2000', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-29T10:17:00.000Z', '2025-03-29T10:17:00.000Z'
),
(
  'hist_pay_6d914154d085c4db', 'treatment', '4th Payment', '4th Payment',
  (select id from public.patients where "patientId" = 'KNE-27062026-004'),
  '8292788091', 'Kishanganj', 'KANAK LAL MANDAL',
  '2025-04-23', '2000', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-23T10:18:00.000Z', '2025-04-23T10:18:00.000Z'
),
(
  'hist_pay_a60e55b1879a8045', 'treatment', '5th Payment', '5th Payment',
  (select id from public.patients where "patientId" = 'KNE-27062026-004'),
  '8292788091', 'Kishanganj', 'KANAK LAL MANDAL',
  '2025-06-04', '1000', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-04T10:19:00.000Z', '2025-06-04T10:19:00.000Z'
),
(
  'hist_pay_2bb4295e3b8c3ab5', 'treatment', 'Advance', 'Advance',
  (select id from public.patients where "patientId" = 'KNE-02062026-001'),
  '7482046966', 'Kishanganj', 'SAGAR KUMAR SINGH',
  '2025-03-26', '5000', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-26T10:15:00.000Z', '2025-03-26T10:15:00.000Z'
),
(
  'hist_pay_89071173f1279d99', 'treatment', '2nd Payment', '2nd Payment',
  (select id from public.patients where "patientId" = 'KNE-02062026-001'),
  '7482046966', 'Kishanganj', 'SAGAR KUMAR SINGH',
  '2025-04-02', '5000', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-02T10:16:00.000Z', '2025-04-02T10:16:00.000Z'
),
(
  'hist_pay_bc5ce8487a3c1609', 'treatment', '3rd Payment', '3rd Payment',
  (select id from public.patients where "patientId" = 'KNE-02062026-001'),
  '7482046966', 'Kishanganj', 'SAGAR KUMAR SINGH',
  '2025-04-09', '500', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-09T10:17:00.000Z', '2025-04-09T10:17:00.000Z'
),
(
  'hist_pay_11d030d4efae7cd0', 'treatment', '4th Payment', '4th Payment',
  (select id from public.patients where "patientId" = 'KNE-02062026-001'),
  '7482046966', 'Kishanganj', 'SAGAR KUMAR SINGH',
  '2025-04-23', '2000', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-23T10:18:00.000Z', '2025-04-23T10:18:00.000Z'
),
(
  'hist_pay_cc30fe7aa3bb593c', 'treatment', '5th Payment', '5th Payment',
  (select id from public.patients where "patientId" = 'KNE-02062026-001'),
  '7482046966', 'Kishanganj', 'SAGAR KUMAR SINGH',
  '2025-04-26', '500', 'CASH', 'Historical import (2025 Google Sheet) -- merged into existing live patient, same person confirmed by TK',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-26T10:19:00.000Z', '2025-04-26T10:19:00.000Z'
);
