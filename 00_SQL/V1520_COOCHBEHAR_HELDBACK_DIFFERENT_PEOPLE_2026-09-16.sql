-- কোচবিহার ব্রাঞ্চ -- আগে সাময়িক বাদ রাখা ২ জন (GULBAHADUR ALI, BABU RAHAMAN)।
-- V1515-এ লাইভে একই মোবাইলে অন্য একটা রোগীর রেকর্ড পাওয়া গিয়েছিল -- TK নিশ্চিত
-- করেছেন এরা আলাদা মানুষ (২৬.০৯.১৬), তাই নতুন আলাদা রোগী হিসেবে ঢোকানো হলো।
-- দুজনেরই সিটের "মোট" ঘরের সাথে কিস্তির যোগফল মিলছিল না -- TK নির্দেশ দিয়েছেন
-- কিস্তির যোগফলটাই সঠিক ধরে নিতে (BABU RAHAMAN: ২৪০০০, GULBAHADUR ALI: ৩৩০০০)।
-- patientId সংঘর্ষ-চেক (V1519) করা হয়েছে -- দুটো তারিখেই (06102025, 14112025)
-- আগে থেকে -001 সিরিয়াল থাকায় এদের -002 দেওয়া হলো।

insert into public.patients (
  id, "patientId", date, "registrationDate", "visitDate",
  name, mobile, "altMobile", branch, age, sex,
  address, disease, bill,
  stage, queue, "doctorComplete",
  "createdBy", "registeredBy", "createdAt", "updatedAt"
) values
(
  'histcob_9a50c28ce3a21adc', 'COB-06102025-002', '2025-10-06', '2025-10-06', '2025-10-06',
  'BABU RAHAMAN', '9990895281', '', 'Cooch Behar', '36', 'Male',
  'GORVODANGO, KHOCHABARI, SAHEBGANJ, COOCHBEHAR', 'Piles', '24000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-06T10:00:00.000Z', '2025-10-06T10:00:00.000Z'
),
(
  'histcob_5e392768b6fc62bd', 'COB-14112025-002', '2025-11-14', '2025-11-14', '2025-11-14',
  'GULBAHADUR ALI', '6900389133', '', 'Cooch Behar', '34', 'Male',
  'ALAMGANJ BAZAR, ALAMGANJ, GOURIPUR, DHUBRI', 'Fistula', '33000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-14T10:00:00.000Z', '2025-11-14T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'histcob_pay_f9455499f25bc16b', 'treatment', 'Advance', 'Advance', 'histcob_9a50c28ce3a21adc', '9990895281', 'Cooch Behar', 'BABU RAHAMAN',
  '2025-10-06', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-06T10:15:00.000Z', '2025-10-06T10:15:00.000Z'
),
(
  'histcob_pay_343fc78bdfc35f07', 'treatment', '2nd Payment', '2nd Payment', 'histcob_9a50c28ce3a21adc', '9990895281', 'Cooch Behar', 'BABU RAHAMAN',
  '2025-10-10', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-10T10:16:00.000Z', '2025-10-10T10:16:00.000Z'
),
(
  'histcob_pay_baaf48111cbbaa63', 'treatment', '3rd Payment', '3rd Payment', 'histcob_9a50c28ce3a21adc', '9990895281', 'Cooch Behar', 'BABU RAHAMAN',
  '2025-10-13', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-13T10:17:00.000Z', '2025-10-13T10:17:00.000Z'
),
(
  'histcob_pay_ee7197f3e942ea14', 'treatment', '4th Payment', '4th Payment', 'histcob_9a50c28ce3a21adc', '9990895281', 'Cooch Behar', 'BABU RAHAMAN',
  '2025-10-17', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-17T10:18:00.000Z', '2025-10-17T10:18:00.000Z'
),
(
  'histcob_pay_bc798e62c97b1ae8', 'treatment', '5th Payment', '5th Payment', 'histcob_9a50c28ce3a21adc', '9990895281', 'Cooch Behar', 'BABU RAHAMAN',
  '2025-10-20', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-20T10:19:00.000Z', '2025-10-20T10:19:00.000Z'
),
(
  'histcob_pay_62e94878cee3f15f', 'treatment', '6th Payment', '6th Payment', 'histcob_9a50c28ce3a21adc', '9990895281', 'Cooch Behar', 'BABU RAHAMAN',
  '2025-10-27', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-27T10:20:00.000Z', '2025-10-27T10:20:00.000Z'
),
(
  'histcob_pay_2ae62e310a738e36', 'treatment', '7th Payment', '7th Payment', 'histcob_9a50c28ce3a21adc', '9990895281', 'Cooch Behar', 'BABU RAHAMAN',
  '2025-11-03', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-03T10:21:00.000Z', '2025-11-03T10:21:00.000Z'
),
(
  'histcob_pay_07c25ffb6e3bc8ed', 'treatment', '8th Payment', '8th Payment', 'histcob_9a50c28ce3a21adc', '9990895281', 'Cooch Behar', 'BABU RAHAMAN',
  '2025-11-07', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-07T10:22:00.000Z', '2025-11-07T10:22:00.000Z'
),
(
  'histcob_pay_5636f4e4d6c75ec7', 'treatment', '9th Payment', '9th Payment', 'histcob_9a50c28ce3a21adc', '9990895281', 'Cooch Behar', 'BABU RAHAMAN',
  '2025-11-10', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-10T10:23:00.000Z', '2025-11-10T10:23:00.000Z'
),
(
  'histcob_pay_7a49eeb0e3acef2d', 'treatment', '10th Payment', '10th Payment', 'histcob_9a50c28ce3a21adc', '9990895281', 'Cooch Behar', 'BABU RAHAMAN',
  '2025-11-17', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-17T10:24:00.000Z', '2025-11-17T10:24:00.000Z'
),
(
  'histcob_pay_cd3f28f3b6be231a', 'treatment', '11th Payment', '11th Payment', 'histcob_9a50c28ce3a21adc', '9990895281', 'Cooch Behar', 'BABU RAHAMAN',
  '2025-11-24', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-24T10:25:00.000Z', '2025-11-24T10:25:00.000Z'
),
(
  'histcob_pay_1a961303be475bb9', 'treatment', '12th Payment', '12th Payment', 'histcob_9a50c28ce3a21adc', '9990895281', 'Cooch Behar', 'BABU RAHAMAN',
  '2025-12-01', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-01T10:26:00.000Z', '2025-12-01T10:26:00.000Z'
),
(
  'histcob_pay_278a42b66f74e114', 'treatment', 'Advance', 'Advance', 'histcob_5e392768b6fc62bd', '6900389133', 'Cooch Behar', 'GULBAHADUR ALI',
  '2025-11-14', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-14T10:15:00.000Z', '2025-11-14T10:15:00.000Z'
),
(
  'histcob_pay_d28f691e428adce4', 'treatment', '2nd Payment', '2nd Payment', 'histcob_5e392768b6fc62bd', '6900389133', 'Cooch Behar', 'GULBAHADUR ALI',
  '2025-11-17', '8000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-17T10:16:00.000Z', '2025-11-17T10:16:00.000Z'
),
(
  'histcob_pay_82a3420e46ab5835', 'treatment', '3rd Payment', '3rd Payment', 'histcob_5e392768b6fc62bd', '6900389133', 'Cooch Behar', 'GULBAHADUR ALI',
  '2025-11-24', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-24T10:17:00.000Z', '2025-11-24T10:17:00.000Z'
),
(
  'histcob_pay_91e19e3620a379d0', 'treatment', '4th Payment', '4th Payment', 'histcob_5e392768b6fc62bd', '6900389133', 'Cooch Behar', 'GULBAHADUR ALI',
  '2025-12-05', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-05T10:18:00.000Z', '2025-12-05T10:18:00.000Z'
),
(
  'histcob_pay_ccc2e0809fb1d8da', 'treatment', '5th Payment', '5th Payment', 'histcob_5e392768b6fc62bd', '6900389133', 'Cooch Behar', 'GULBAHADUR ALI',
  '2025-12-08', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-08T10:19:00.000Z', '2025-12-08T10:19:00.000Z'
),
(
  'histcob_pay_e662a5462de4032b', 'treatment', '6th Payment', '6th Payment', 'histcob_5e392768b6fc62bd', '6900389133', 'Cooch Behar', 'GULBAHADUR ALI',
  '2025-12-19', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-19T10:20:00.000Z', '2025-12-19T10:20:00.000Z'
),
(
  'histcob_pay_5e513d2921915968', 'treatment', '7th Payment', '7th Payment', 'histcob_5e392768b6fc62bd', '6900389133', 'Cooch Behar', 'GULBAHADUR ALI',
  '2026-01-12', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- held-back different-person case, TK confirmed separate people, installment-sum used as bill per TK 2026-09-16',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-12T10:21:00.000Z', '2026-01-12T10:21:00.000Z'
);
