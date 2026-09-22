-- জলপাইগুড়ি ২০২৫-শিট -- আগে বাদ-রাখা ৯ জনের মধ্যে ৪ জন, TK-র পাঠানো
-- আসল সিট-তথ্য দিয়ে পুনর্যাচাই করে ঢোকানো হলো।
-- ABHIJIT BASAK / BABULAL BASAK / HARIPADA ROY -- কিস্তির যোগ সিটের নিজের
-- Paid ঘরের সাথে হুবহু মিলেছে (কোনো গরমিল নেই)।
-- PALASH TANTRA -- সিট বলছে Paid ২৮,০০০, কিন্তু কিস্তির তালিকায় একটা "১০০০"
-- কোন তারিখ ছাড়াই লেখা ছিল (আসল তারিখ সম্ভবত ১৬.৮.২৫-এর পাশেই ডুপ্লিকেট
-- হয়ে গিয়েছিল) -- TK-নির্দেশে (১৬.০৯.২০২৬: "১০০০ এডজাস্ট করে দিন") সেই
-- অতিরিক্ত/ডুপ্লিকেট এক হাজার বাদ দিয়ে বাকি ১৩টা কিস্তি (২৯,০০০) রাখা হলো।
-- ২টা স্পষ্ট তারিখ-টাইপো (ধারাবাহিকতা দেখে বোঝা যায়) ঠিক করা হয়েছে --
-- টাকার অঙ্ক কিছুই বদলায়নি: ABHIJIT-এর "06/09/26"→06/09/25,
-- BABULAL-এর "25/11/26"→25/11/25; PALASH-এর "12/07/25"→12/08/25 (আগস্টের
-- তারিখগুলোর মাঝে বসা, জুলাইয়ে হলে উল্টোক্রমে পড়ে)।
-- লাইভ-ডুপ্লিকেট-চেক (V1522, ০ সারি) ও patientId-সংঘর্ষ-চেক (V1521)
-- দুটোই TK চালিয়ে দেখেছেন।
-- বাকি বাদ-দেওয়া তালিকা: RABINDRA NATH DAS (মোবাইল নেই, বাদই থাকবে),
-- KARMA KHARIYA (নতুন নম্বর ১১-ডিজিট, সঠিক ১০-ডিজিট নম্বরের অপেক্ষায়),
-- MONTU KUMAR BISWAS ও JAGABANDHU ROY (তথ্য এখনো পাঠানো হয়নি)।

insert into public.patients (
  id, "patientId", date, "registrationDate", "visitDate",
  name, mobile, "altMobile", branch, age, sex,
  address, disease, bill,
  stage, queue, "doctorComplete",
  "createdBy", "registeredBy", "createdAt", "updatedAt"
) values
(
  'histjpe25b_cfd4b899fe2c8c78', 'JPE-24062025-002', '2025-06-24', '2025-06-24', '2025-06-24',
  'PALASH TANTRA', '6296917436', '', 'Jalpaiguri', '40', 'Male',
  'BALAPARA, PAHARPUR, KOTWALI, JALPAIGURI', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-24T10:00:00.000Z', '2025-06-24T10:00:00.000Z'
),
(
  'histjpe25b_1b372ba08baf7c19', 'JPE-15072025-001', '2025-07-15', '2025-07-15', '2025-07-15',
  'ABHIJIT BASAK', '7602772364', '', 'Jalpaiguri', '20', 'Male',
  'VOTPATTI, VOTPATTI, MAYNAGURI, JALPAIGURI', 'Fissure', '60000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-15T10:00:00.000Z', '2025-07-15T10:00:00.000Z'
),
(
  'histjpe25b_ec4e5197332ea859', 'JPE-23082025-006', '2025-08-23', '2025-08-23', '2025-08-23',
  'BABULAL BASAK', '9735924135', '', 'Jalpaiguri', '38', 'Male',
  'BALA PARA, PAHARPUR, KOTWALI, JALPAIGURI', 'Piles', '45000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:00:00.000Z', '2025-08-23T10:00:00.000Z'
),
(
  'histjpe25b_3b1ab7341e7e484e', 'JPE-26072025-001', '2025-07-26', '2025-07-26', '2025-07-26',
  'HARIPADA ROY', '9382721677', '', 'Jalpaiguri', '27', 'Male',
  'UTTAR DANGA PARA, MAYNAGURI, MAYNAGURI, JALPAIGURI', 'Fissure', '68000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:00:00.000Z', '2025-07-26T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'histjpe25b_pay_0e2dd1a4d431f178', 'treatment', 'Advance', 'Advance', 'histjpe25b_cfd4b899fe2c8c78', '6296917436', 'Jalpaiguri', 'PALASH TANTRA',
  '2025-06-24', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-24T10:15:00.000Z', '2025-06-24T10:15:00.000Z'
),
(
  'histjpe25b_pay_44c2883cc76b4584', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25b_cfd4b899fe2c8c78', '6296917436', 'Jalpaiguri', 'PALASH TANTRA',
  '2025-06-28', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:16:00.000Z', '2025-06-28T10:16:00.000Z'
),
(
  'histjpe25b_pay_884864d4ae4370a2', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25b_cfd4b899fe2c8c78', '6296917436', 'Jalpaiguri', 'PALASH TANTRA',
  '2025-07-05', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:17:00.000Z', '2025-07-05T10:17:00.000Z'
),
(
  'histjpe25b_pay_d0826e800e38b37a', 'treatment', '4th Payment', '4th Payment', 'histjpe25b_cfd4b899fe2c8c78', '6296917436', 'Jalpaiguri', 'PALASH TANTRA',
  '2025-07-08', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-08T10:18:00.000Z', '2025-07-08T10:18:00.000Z'
),
(
  'histjpe25b_pay_f156f4e24a390d4e', 'treatment', '5th Payment', '5th Payment', 'histjpe25b_cfd4b899fe2c8c78', '6296917436', 'Jalpaiguri', 'PALASH TANTRA',
  '2025-07-15', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-15T10:19:00.000Z', '2025-07-15T10:19:00.000Z'
),
(
  'histjpe25b_pay_9b0758cee16bd242', 'treatment', '6th Payment', '6th Payment', 'histjpe25b_cfd4b899fe2c8c78', '6296917436', 'Jalpaiguri', 'PALASH TANTRA',
  '2025-07-19', '3500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-19T10:20:00.000Z', '2025-07-19T10:20:00.000Z'
),
(
  'histjpe25b_pay_a39ca3385f69ef9d', 'treatment', '7th Payment', '7th Payment', 'histjpe25b_cfd4b899fe2c8c78', '6296917436', 'Jalpaiguri', 'PALASH TANTRA',
  '2025-07-22', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-22T10:21:00.000Z', '2025-07-22T10:21:00.000Z'
),
(
  'histjpe25b_pay_a358d4ce4a5c47b1', 'treatment', '8th Payment', '8th Payment', 'histjpe25b_cfd4b899fe2c8c78', '6296917436', 'Jalpaiguri', 'PALASH TANTRA',
  '2025-07-26', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:22:00.000Z', '2025-07-26T10:22:00.000Z'
),
(
  'histjpe25b_pay_7503eee9931548fe', 'treatment', '9th Payment', '9th Payment', 'histjpe25b_cfd4b899fe2c8c78', '6296917436', 'Jalpaiguri', 'PALASH TANTRA',
  '2025-07-29', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-29T10:23:00.000Z', '2025-07-29T10:23:00.000Z'
),
(
  'histjpe25b_pay_0beb068a6dc1a548', 'treatment', '10th Payment', '10th Payment', 'histjpe25b_cfd4b899fe2c8c78', '6296917436', 'Jalpaiguri', 'PALASH TANTRA',
  '2025-08-02', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:24:00.000Z', '2025-08-02T10:24:00.000Z'
),
(
  'histjpe25b_pay_06d8fd48326ff419', 'treatment', '11th Payment', '11th Payment', 'histjpe25b_cfd4b899fe2c8c78', '6296917436', 'Jalpaiguri', 'PALASH TANTRA',
  '2025-08-05', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-05T10:25:00.000Z', '2025-08-05T10:25:00.000Z'
),
(
  'histjpe25b_pay_42b80e97f99f376a', 'treatment', '12th Payment', '12th Payment', 'histjpe25b_cfd4b899fe2c8c78', '6296917436', 'Jalpaiguri', 'PALASH TANTRA',
  '2025-08-12', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-12T10:26:00.000Z', '2025-08-12T10:26:00.000Z'
),
(
  'histjpe25b_pay_ae91e85e68dfcc35', 'treatment', '13th Payment', '13th Payment', 'histjpe25b_cfd4b899fe2c8c78', '6296917436', 'Jalpaiguri', 'PALASH TANTRA',
  '2025-08-16', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:27:00.000Z', '2025-08-16T10:27:00.000Z'
),
(
  'histjpe25b_pay_b6ea3691f0cbfaf8', 'treatment', 'Advance', 'Advance', 'histjpe25b_1b372ba08baf7c19', '7602772364', 'Jalpaiguri', 'ABHIJIT BASAK',
  '2025-07-15', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-15T10:15:00.000Z', '2025-07-15T10:15:00.000Z'
),
(
  'histjpe25b_pay_9f9dba6b3873e902', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25b_1b372ba08baf7c19', '7602772364', 'Jalpaiguri', 'ABHIJIT BASAK',
  '2025-07-19', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-19T10:16:00.000Z', '2025-07-19T10:16:00.000Z'
),
(
  'histjpe25b_pay_4e1a173e32609d1b', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25b_1b372ba08baf7c19', '7602772364', 'Jalpaiguri', 'ABHIJIT BASAK',
  '2025-07-22', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-22T10:17:00.000Z', '2025-07-22T10:17:00.000Z'
),
(
  'histjpe25b_pay_ad8b4ba9f91411be', 'treatment', '4th Payment', '4th Payment', 'histjpe25b_1b372ba08baf7c19', '7602772364', 'Jalpaiguri', 'ABHIJIT BASAK',
  '2025-07-26', '10000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:18:00.000Z', '2025-07-26T10:18:00.000Z'
),
(
  'histjpe25b_pay_8d4d4590600d7ffa', 'treatment', '5th Payment', '5th Payment', 'histjpe25b_1b372ba08baf7c19', '7602772364', 'Jalpaiguri', 'ABHIJIT BASAK',
  '2025-07-29', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-29T10:19:00.000Z', '2025-07-29T10:19:00.000Z'
),
(
  'histjpe25b_pay_dd5a95b1fc689fce', 'treatment', '6th Payment', '6th Payment', 'histjpe25b_1b372ba08baf7c19', '7602772364', 'Jalpaiguri', 'ABHIJIT BASAK',
  '2025-08-02', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:20:00.000Z', '2025-08-02T10:20:00.000Z'
),
(
  'histjpe25b_pay_2ca11179da810db6', 'treatment', '7th Payment', '7th Payment', 'histjpe25b_1b372ba08baf7c19', '7602772364', 'Jalpaiguri', 'ABHIJIT BASAK',
  '2025-08-05', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-05T10:21:00.000Z', '2025-08-05T10:21:00.000Z'
),
(
  'histjpe25b_pay_b0cda525a576a1c2', 'treatment', '8th Payment', '8th Payment', 'histjpe25b_1b372ba08baf7c19', '7602772364', 'Jalpaiguri', 'ABHIJIT BASAK',
  '2025-08-09', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-09T10:22:00.000Z', '2025-08-09T10:22:00.000Z'
),
(
  'histjpe25b_pay_31af76e2d9d7e371', 'treatment', '9th Payment', '9th Payment', 'histjpe25b_1b372ba08baf7c19', '7602772364', 'Jalpaiguri', 'ABHIJIT BASAK',
  '2025-08-12', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-12T10:23:00.000Z', '2025-08-12T10:23:00.000Z'
),
(
  'histjpe25b_pay_7418804eb4ce3e5b', 'treatment', '10th Payment', '10th Payment', 'histjpe25b_1b372ba08baf7c19', '7602772364', 'Jalpaiguri', 'ABHIJIT BASAK',
  '2025-08-16', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:24:00.000Z', '2025-08-16T10:24:00.000Z'
),
(
  'histjpe25b_pay_810a5ad758aed835', 'treatment', '11th Payment', '11th Payment', 'histjpe25b_1b372ba08baf7c19', '7602772364', 'Jalpaiguri', 'ABHIJIT BASAK',
  '2025-08-19', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-19T10:25:00.000Z', '2025-08-19T10:25:00.000Z'
),
(
  'histjpe25b_pay_076ec57c49b9ee04', 'treatment', '12th Payment', '12th Payment', 'histjpe25b_1b372ba08baf7c19', '7602772364', 'Jalpaiguri', 'ABHIJIT BASAK',
  '2025-08-23', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:26:00.000Z', '2025-08-23T10:26:00.000Z'
),
(
  'histjpe25b_pay_fe3b6c4fb554373c', 'treatment', '13th Payment', '13th Payment', 'histjpe25b_1b372ba08baf7c19', '7602772364', 'Jalpaiguri', 'ABHIJIT BASAK',
  '2025-08-26', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-26T10:27:00.000Z', '2025-08-26T10:27:00.000Z'
),
(
  'histjpe25b_pay_60a20afbc3d8b8ca', 'treatment', '14th Payment', '14th Payment', 'histjpe25b_1b372ba08baf7c19', '7602772364', 'Jalpaiguri', 'ABHIJIT BASAK',
  '2025-09-02', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-02T10:28:00.000Z', '2025-09-02T10:28:00.000Z'
),
(
  'histjpe25b_pay_2a69825decbd00d3', 'treatment', '15th Payment', '15th Payment', 'histjpe25b_1b372ba08baf7c19', '7602772364', 'Jalpaiguri', 'ABHIJIT BASAK',
  '2025-09-06', '2500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:29:00.000Z', '2025-09-06T10:29:00.000Z'
),
(
  'histjpe25b_pay_7bf2828f1c19ab58', 'treatment', 'Advance', 'Advance', 'histjpe25b_ec4e5197332ea859', '9735924135', 'Jalpaiguri', 'BABULAL BASAK',
  '2025-08-23', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:15:00.000Z', '2025-08-23T10:15:00.000Z'
),
(
  'histjpe25b_pay_e642a2ca2724cc58', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25b_ec4e5197332ea859', '9735924135', 'Jalpaiguri', 'BABULAL BASAK',
  '2025-08-26', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-26T10:16:00.000Z', '2025-08-26T10:16:00.000Z'
),
(
  'histjpe25b_pay_3f266bedd4f9480e', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25b_ec4e5197332ea859', '9735924135', 'Jalpaiguri', 'BABULAL BASAK',
  '2025-09-02', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-02T10:17:00.000Z', '2025-09-02T10:17:00.000Z'
),
(
  'histjpe25b_pay_f4fc76059e3f1111', 'treatment', '4th Payment', '4th Payment', 'histjpe25b_ec4e5197332ea859', '9735924135', 'Jalpaiguri', 'BABULAL BASAK',
  '2025-09-06', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:18:00.000Z', '2025-09-06T10:18:00.000Z'
),
(
  'histjpe25b_pay_d0e8aa8fda489229', 'treatment', '5th Payment', '5th Payment', 'histjpe25b_ec4e5197332ea859', '9735924135', 'Jalpaiguri', 'BABULAL BASAK',
  '2025-09-07', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-07T10:19:00.000Z', '2025-09-07T10:19:00.000Z'
),
(
  'histjpe25b_pay_31dbd3f731990a47', 'treatment', '6th Payment', '6th Payment', 'histjpe25b_ec4e5197332ea859', '9735924135', 'Jalpaiguri', 'BABULAL BASAK',
  '2025-09-13', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:20:00.000Z', '2025-09-13T10:20:00.000Z'
),
(
  'histjpe25b_pay_dfdeb78fa9ad48a9', 'treatment', '7th Payment', '7th Payment', 'histjpe25b_ec4e5197332ea859', '9735924135', 'Jalpaiguri', 'BABULAL BASAK',
  '2025-09-20', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:21:00.000Z', '2025-09-20T10:21:00.000Z'
),
(
  'histjpe25b_pay_c729980628010050', 'treatment', '8th Payment', '8th Payment', 'histjpe25b_ec4e5197332ea859', '9735924135', 'Jalpaiguri', 'BABULAL BASAK',
  '2025-09-23', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-23T10:22:00.000Z', '2025-09-23T10:22:00.000Z'
),
(
  'histjpe25b_pay_4da9d6b00e8f2523', 'treatment', '9th Payment', '9th Payment', 'histjpe25b_ec4e5197332ea859', '9735924135', 'Jalpaiguri', 'BABULAL BASAK',
  '2025-09-27', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:23:00.000Z', '2025-09-27T10:23:00.000Z'
),
(
  'histjpe25b_pay_ef4f6f65f8cd59f5', 'treatment', '10th Payment', '10th Payment', 'histjpe25b_ec4e5197332ea859', '9735924135', 'Jalpaiguri', 'BABULAL BASAK',
  '2025-10-04', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:24:00.000Z', '2025-10-04T10:24:00.000Z'
),
(
  'histjpe25b_pay_eaaa850cc0afbf60', 'treatment', '11th Payment', '11th Payment', 'histjpe25b_ec4e5197332ea859', '9735924135', 'Jalpaiguri', 'BABULAL BASAK',
  '2025-11-11', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-11T10:25:00.000Z', '2025-11-11T10:25:00.000Z'
),
(
  'histjpe25b_pay_0a34c26ee56ce831', 'treatment', '12th Payment', '12th Payment', 'histjpe25b_ec4e5197332ea859', '9735924135', 'Jalpaiguri', 'BABULAL BASAK',
  '2025-11-15', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:26:00.000Z', '2025-11-15T10:26:00.000Z'
),
(
  'histjpe25b_pay_f59a70075a78dc55', 'treatment', '13th Payment', '13th Payment', 'histjpe25b_ec4e5197332ea859', '9735924135', 'Jalpaiguri', 'BABULAL BASAK',
  '2025-11-22', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-22T10:27:00.000Z', '2025-11-22T10:27:00.000Z'
),
(
  'histjpe25b_pay_e106921e7e854a9e', 'treatment', '14th Payment', '14th Payment', 'histjpe25b_ec4e5197332ea859', '9735924135', 'Jalpaiguri', 'BABULAL BASAK',
  '2025-11-25', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-25T10:28:00.000Z', '2025-11-25T10:28:00.000Z'
),
(
  'histjpe25b_pay_7ed34cb7a84d8b47', 'treatment', '15th Payment', '15th Payment', 'histjpe25b_ec4e5197332ea859', '9735924135', 'Jalpaiguri', 'BABULAL BASAK',
  '2025-12-02', '5500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-02T10:29:00.000Z', '2025-12-02T10:29:00.000Z'
),
(
  'histjpe25b_pay_e8ed5279803e964f', 'treatment', 'Advance', 'Advance', 'histjpe25b_3b1ab7341e7e484e', '9382721677', 'Jalpaiguri', 'HARIPADA ROY',
  '2025-07-26', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:15:00.000Z', '2025-07-26T10:15:00.000Z'
),
(
  'histjpe25b_pay_d78f94bf8b1a39bd', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25b_3b1ab7341e7e484e', '9382721677', 'Jalpaiguri', 'HARIPADA ROY',
  '2025-07-29', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-29T10:16:00.000Z', '2025-07-29T10:16:00.000Z'
),
(
  'histjpe25b_pay_94e0e8ddfceb492d', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25b_3b1ab7341e7e484e', '9382721677', 'Jalpaiguri', 'HARIPADA ROY',
  '2025-08-02', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:17:00.000Z', '2025-08-02T10:17:00.000Z'
),
(
  'histjpe25b_pay_70b7e66d45859eb1', 'treatment', '4th Payment', '4th Payment', 'histjpe25b_3b1ab7341e7e484e', '9382721677', 'Jalpaiguri', 'HARIPADA ROY',
  '2025-08-05', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-05T10:18:00.000Z', '2025-08-05T10:18:00.000Z'
),
(
  'histjpe25b_pay_85569f4bdc292e86', 'treatment', '5th Payment', '5th Payment', 'histjpe25b_3b1ab7341e7e484e', '9382721677', 'Jalpaiguri', 'HARIPADA ROY',
  '2025-08-09', '6000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-09T10:19:00.000Z', '2025-08-09T10:19:00.000Z'
),
(
  'histjpe25b_pay_dc74685c33f41355', 'treatment', '6th Payment', '6th Payment', 'histjpe25b_3b1ab7341e7e484e', '9382721677', 'Jalpaiguri', 'HARIPADA ROY',
  '2025-08-12', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-12T10:20:00.000Z', '2025-08-12T10:20:00.000Z'
),
(
  'histjpe25b_pay_efa8e5838efa8d41', 'treatment', '7th Payment', '7th Payment', 'histjpe25b_3b1ab7341e7e484e', '9382721677', 'Jalpaiguri', 'HARIPADA ROY',
  '2025-08-16', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:21:00.000Z', '2025-08-16T10:21:00.000Z'
),
(
  'histjpe25b_pay_29968378e87257bc', 'treatment', '8th Payment', '8th Payment', 'histjpe25b_3b1ab7341e7e484e', '9382721677', 'Jalpaiguri', 'HARIPADA ROY',
  '2025-08-19', '8000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-19T10:22:00.000Z', '2025-08-19T10:22:00.000Z'
),
(
  'histjpe25b_pay_bbd30bf2a4846df0', 'treatment', '9th Payment', '9th Payment', 'histjpe25b_3b1ab7341e7e484e', '9382721677', 'Jalpaiguri', 'HARIPADA ROY',
  '2025-08-23', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:23:00.000Z', '2025-08-23T10:23:00.000Z'
),
(
  'histjpe25b_pay_967efb9fbdfd613c', 'treatment', '10th Payment', '10th Payment', 'histjpe25b_3b1ab7341e7e484e', '9382721677', 'Jalpaiguri', 'HARIPADA ROY',
  '2025-08-26', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-26T10:24:00.000Z', '2025-08-26T10:24:00.000Z'
),
(
  'histjpe25b_pay_3cac09f028d4d0ba', 'treatment', '11th Payment', '11th Payment', 'histjpe25b_3b1ab7341e7e484e', '9382721677', 'Jalpaiguri', 'HARIPADA ROY',
  '2025-08-30', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:25:00.000Z', '2025-08-30T10:25:00.000Z'
),
(
  'histjpe25b_pay_33e2cd0beab49a6b', 'treatment', '12th Payment', '12th Payment', 'histjpe25b_3b1ab7341e7e484e', '9382721677', 'Jalpaiguri', 'HARIPADA ROY',
  '2025-09-02', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-02T10:26:00.000Z', '2025-09-02T10:26:00.000Z'
),
(
  'histjpe25b_pay_0bac697a6d5f6d8f', 'treatment', '13th Payment', '13th Payment', 'histjpe25b_3b1ab7341e7e484e', '9382721677', 'Jalpaiguri', 'HARIPADA ROY',
  '2025-09-06', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:27:00.000Z', '2025-09-06T10:27:00.000Z'
),
(
  'histjpe25b_pay_f1916dd9e741fdd1', 'treatment', '14th Payment', '14th Payment', 'histjpe25b_3b1ab7341e7e484e', '9382721677', 'Jalpaiguri', 'HARIPADA ROY',
  '2025-09-09', '6000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet, previously excluded -- re-verified with TK)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-09T10:28:00.000Z', '2025-09-09T10:28:00.000Z'
);
