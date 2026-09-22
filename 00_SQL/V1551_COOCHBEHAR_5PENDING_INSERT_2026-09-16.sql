-- কোচবিহার -- দীর্ঘদিন বাকি থাকা ৫ জন, TK-এর সংশোধিত সিট-ডেটা (১৬.০৯.২০২৬) থেকে
-- AZAD ALI MANDAL ও SAHANA PARVIN: আগে মোবাইল ভুল ছিল, এখন ঠিক ১০ ডিজিট পাওয়া গেছে
-- HAIDUL ISLAM TALUKDAR, Bikash Barman, RANJIT BARMAN: আগে শিটের বিল/কিস্তি ঘরে তারিখ ঢুকে গিয়েছিল,
-- TK সংশোধন করে পাঠিয়েছেন। Bikash Barman-এর প্রথম কিস্তি সিটে '29.8.26' লেখা ছিল --
-- রেজিস্ট্রেশনের দিনই (29.8.2025) প্রথম কিস্তি, তাই '29.8.25' ধরে নেওয়া হলো (বছর-টাইপো, এই সেশনে বারবার দেখা প্যাটার্ন)।
-- মোবাইল-ডুপ্লিকেট-চেক (V1550) ও সিরিয়াল-চেক (V1549) দুটোই TK চালিয়ে দেখেছেন -- কোনো সংঘর্ষ নেই।

insert into public.patients (
  id, "patientId", date, "registrationDate", "visitDate",
  name, mobile, "altMobile", branch, age, sex,
  address, disease, bill,
  stage, queue, "doctorComplete",
  "createdBy", "registeredBy", "createdAt", "updatedAt"
) values
(
  'histcob_126c3236e7c69cd8', 'COB-28022025-005', '2025-02-28', '2025-02-28', '2025-02-28',
  'AZAD ALI MANDAL', '9957468191', '', 'Cooch Behar', '40', 'Male',
  'BARIBARI, BARIBARI, DHUBRI ASSAM', 'Fistula', '23000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-28T10:00:00.000Z', '2025-02-28T10:00:00.000Z'
),
(
  'histcob_d5ef5030facad900', 'COB-23082025-002', '2025-08-23', '2025-08-23', '2025-08-23',
  'SAHANA PARVIN', '7584937152', '', 'Cooch Behar', '15', 'Female',
  'CHILAKHANA CHULKANIRBAJAR, DAWCHARAI, TUFANGANJ, COOCHBEHAR', 'Piles', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:00:00.000Z', '2025-08-23T10:00:00.000Z'
),
(
  'histcob_e3ccf3fdfa59ea92', 'COB-29092025-001', '2025-09-29', '2025-09-29', '2025-09-29',
  'RANJIT BARMAN', '7478155578', '', 'Cooch Behar', '38', 'Male',
  'COOCHBEHAR, COOCHBEHAR, COOCHBEHAR', 'Piles', '47000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-29T10:00:00.000Z', '2025-09-29T10:00:00.000Z'
),
(
  'histcob_654df89f92d7add2', 'COB-29082025-003', '2025-08-29', '2025-08-29', '2025-08-29',
  'Bikash Barman', '7319394685', '', 'Cooch Behar', '44', 'Male',
  'Seoraguri, Kamat seoraguri, Tufanganj, Coochbehar', 'Piles', '26000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-29T10:00:00.000Z', '2025-08-29T10:00:00.000Z'
),
(
  'histcob_85f53df393dd0c9f', 'COB-02082025-002', '2025-08-02', '2025-08-02', '2025-08-02',
  'HAIDUL ISLAM TALUKDAR', '9365310365', '', 'Cooch Behar', '40', 'Male',
  '', 'Hydrocele', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:00:00.000Z', '2025-08-02T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'histcob_pay_38ec470edc1af2c0', 'treatment', 'Advance', 'Advance', 'histcob_126c3236e7c69cd8', '9957468191', 'Cooch Behar', 'AZAD ALI MANDAL',
  '2025-02-28', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-28T10:00:00.000Z', '2025-02-28T10:00:00.000Z'
),
(
  'histcob_pay_eb5a33be2c356d4c', 'treatment', '2nd Payment', '2nd Payment', 'histcob_126c3236e7c69cd8', '9957468191', 'Cooch Behar', 'AZAD ALI MANDAL',
  '2025-03-03', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-03T10:01:00.000Z', '2025-03-03T10:01:00.000Z'
),
(
  'histcob_pay_29a67856e131a0e1', 'treatment', '3rd Payment', '3rd Payment', 'histcob_126c3236e7c69cd8', '9957468191', 'Cooch Behar', 'AZAD ALI MANDAL',
  '2025-03-07', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-07T10:02:00.000Z', '2025-03-07T10:02:00.000Z'
),
(
  'histcob_pay_a6655456c3da12a6', 'treatment', '4th Payment', '4th Payment', 'histcob_126c3236e7c69cd8', '9957468191', 'Cooch Behar', 'AZAD ALI MANDAL',
  '2025-03-10', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-10T10:03:00.000Z', '2025-03-10T10:03:00.000Z'
),
(
  'histcob_pay_7d8d5a1836dc883f', 'treatment', '5th Payment', '5th Payment', 'histcob_126c3236e7c69cd8', '9957468191', 'Cooch Behar', 'AZAD ALI MANDAL',
  '2025-03-17', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-17T10:04:00.000Z', '2025-03-17T10:04:00.000Z'
),
(
  'histcob_pay_30db42401273e9bd', 'treatment', '6th Payment', '6th Payment', 'histcob_126c3236e7c69cd8', '9957468191', 'Cooch Behar', 'AZAD ALI MANDAL',
  '2025-03-28', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-28T10:05:00.000Z', '2025-03-28T10:05:00.000Z'
),
(
  'histcob_pay_f3bfb335fd3d4d24', 'treatment', '7th Payment', '7th Payment', 'histcob_126c3236e7c69cd8', '9957468191', 'Cooch Behar', 'AZAD ALI MANDAL',
  '2025-04-04', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-04T10:06:00.000Z', '2025-04-04T10:06:00.000Z'
),
(
  'histcob_pay_278217c2168bbb2c', 'treatment', '8th Payment', '8th Payment', 'histcob_126c3236e7c69cd8', '9957468191', 'Cooch Behar', 'AZAD ALI MANDAL',
  '2025-04-18', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-18T10:07:00.000Z', '2025-04-18T10:07:00.000Z'
),
(
  'histcob_pay_71f84f80cdb4d7bc', 'treatment', '9th Payment', '9th Payment', 'histcob_126c3236e7c69cd8', '9957468191', 'Cooch Behar', 'AZAD ALI MANDAL',
  '2025-04-25', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-25T10:08:00.000Z', '2025-04-25T10:08:00.000Z'
),
(
  'histcob_pay_9fb39a56fcc22623', 'treatment', 'Advance', 'Advance', 'histcob_d5ef5030facad900', '7584937152', 'Cooch Behar', 'SAHANA PARVIN',
  '2025-08-23', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:00:00.000Z', '2025-08-23T10:00:00.000Z'
),
(
  'histcob_pay_206aeea40fa7a8d6', 'treatment', '2nd Payment', '2nd Payment', 'histcob_d5ef5030facad900', '7584937152', 'Cooch Behar', 'SAHANA PARVIN',
  '2025-08-25', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-25T10:01:00.000Z', '2025-08-25T10:01:00.000Z'
),
(
  'histcob_pay_77539e1106cba630', 'treatment', '3rd Payment', '3rd Payment', 'histcob_d5ef5030facad900', '7584937152', 'Cooch Behar', 'SAHANA PARVIN',
  '2025-08-30', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:02:00.000Z', '2025-08-30T10:02:00.000Z'
),
(
  'histcob_pay_772388fbae3a2be6', 'treatment', '4th Payment', '4th Payment', 'histcob_d5ef5030facad900', '7584937152', 'Cooch Behar', 'SAHANA PARVIN',
  '2025-09-01', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-01T10:03:00.000Z', '2025-09-01T10:03:00.000Z'
),
(
  'histcob_pay_02ac2c7a85854193', 'treatment', '5th Payment', '5th Payment', 'histcob_d5ef5030facad900', '7584937152', 'Cooch Behar', 'SAHANA PARVIN',
  '2025-09-08', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-08T10:04:00.000Z', '2025-09-08T10:04:00.000Z'
),
(
  'histcob_pay_4b62f7ecaf3e4a53', 'treatment', '6th Payment', '6th Payment', 'histcob_d5ef5030facad900', '7584937152', 'Cooch Behar', 'SAHANA PARVIN',
  '2025-09-12', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-12T10:05:00.000Z', '2025-09-12T10:05:00.000Z'
),
(
  'histcob_pay_4f0b9316dbe7d20b', 'treatment', '7th Payment', '7th Payment', 'histcob_d5ef5030facad900', '7584937152', 'Cooch Behar', 'SAHANA PARVIN',
  '2025-09-22', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-22T10:06:00.000Z', '2025-09-22T10:06:00.000Z'
),
(
  'histcob_pay_cf7f9c73e42f701e', 'treatment', '8th Payment', '8th Payment', 'histcob_d5ef5030facad900', '7584937152', 'Cooch Behar', 'SAHANA PARVIN',
  '2025-10-03', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-03T10:07:00.000Z', '2025-10-03T10:07:00.000Z'
),
(
  'histcob_pay_986b206c48c38a42', 'treatment', '9th Payment', '9th Payment', 'histcob_d5ef5030facad900', '7584937152', 'Cooch Behar', 'SAHANA PARVIN',
  '2025-10-14', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-14T10:08:00.000Z', '2025-10-14T10:08:00.000Z'
),
(
  'histcob_pay_4bb3d3cc170ef195', 'treatment', 'Advance', 'Advance', 'histcob_e3ccf3fdfa59ea92', '7478155578', 'Cooch Behar', 'RANJIT BARMAN',
  '2025-12-22', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-22T10:00:00.000Z', '2025-12-22T10:00:00.000Z'
),
(
  'histcob_pay_538a16b81ccbded0', 'treatment', '2nd Payment', '2nd Payment', 'histcob_e3ccf3fdfa59ea92', '7478155578', 'Cooch Behar', 'RANJIT BARMAN',
  '2025-12-24', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-24T10:01:00.000Z', '2025-12-24T10:01:00.000Z'
),
(
  'histcob_pay_1eb1dd86a6761eb1', 'treatment', '3rd Payment', '3rd Payment', 'histcob_e3ccf3fdfa59ea92', '7478155578', 'Cooch Behar', 'RANJIT BARMAN',
  '2025-12-26', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-26T10:02:00.000Z', '2025-12-26T10:02:00.000Z'
),
(
  'histcob_pay_8dd168b932d3a965', 'treatment', '4th Payment', '4th Payment', 'histcob_e3ccf3fdfa59ea92', '7478155578', 'Cooch Behar', 'RANJIT BARMAN',
  '2025-12-29', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-29T10:03:00.000Z', '2025-12-29T10:03:00.000Z'
),
(
  'histcob_pay_e7ca8d4d7a110ae6', 'treatment', '5th Payment', '5th Payment', 'histcob_e3ccf3fdfa59ea92', '7478155578', 'Cooch Behar', 'RANJIT BARMAN',
  '2025-12-31', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-31T10:04:00.000Z', '2025-12-31T10:04:00.000Z'
),
(
  'histcob_pay_4a096da182241e5a', 'treatment', '6th Payment', '6th Payment', 'histcob_e3ccf3fdfa59ea92', '7478155578', 'Cooch Behar', 'RANJIT BARMAN',
  '2026-01-03', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-03T10:05:00.000Z', '2026-01-03T10:05:00.000Z'
),
(
  'histcob_pay_ac44d9c158e22e80', 'treatment', '7th Payment', '7th Payment', 'histcob_e3ccf3fdfa59ea92', '7478155578', 'Cooch Behar', 'RANJIT BARMAN',
  '2026-01-06', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-06T10:06:00.000Z', '2026-01-06T10:06:00.000Z'
),
(
  'histcob_pay_f3ace259b5d4d844', 'treatment', '8th Payment', '8th Payment', 'histcob_e3ccf3fdfa59ea92', '7478155578', 'Cooch Behar', 'RANJIT BARMAN',
  '2026-01-09', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-09T10:07:00.000Z', '2026-01-09T10:07:00.000Z'
),
(
  'histcob_pay_5cea3033b7e111c6', 'treatment', 'Advance', 'Advance', 'histcob_654df89f92d7add2', '7319394685', 'Cooch Behar', 'Bikash Barman',
  '2025-08-29', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-29T10:00:00.000Z', '2025-08-29T10:00:00.000Z'
),
(
  'histcob_pay_887d3fe7dcdb082a', 'treatment', '2nd Payment', '2nd Payment', 'histcob_654df89f92d7add2', '7319394685', 'Cooch Behar', 'Bikash Barman',
  '2025-09-01', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-01T10:01:00.000Z', '2025-09-01T10:01:00.000Z'
),
(
  'histcob_pay_47a38ff3e9bfa472', 'treatment', '3rd Payment', '3rd Payment', 'histcob_654df89f92d7add2', '7319394685', 'Cooch Behar', 'Bikash Barman',
  '2025-09-05', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-05T10:02:00.000Z', '2025-09-05T10:02:00.000Z'
),
(
  'histcob_pay_111b1973fd17b08b', 'treatment', '4th Payment', '4th Payment', 'histcob_654df89f92d7add2', '7319394685', 'Cooch Behar', 'Bikash Barman',
  '2025-09-08', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-08T10:03:00.000Z', '2025-09-08T10:03:00.000Z'
),
(
  'histcob_pay_9d669937b099c6f4', 'treatment', '5th Payment', '5th Payment', 'histcob_654df89f92d7add2', '7319394685', 'Cooch Behar', 'Bikash Barman',
  '2025-09-12', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-12T10:04:00.000Z', '2025-09-12T10:04:00.000Z'
),
(
  'histcob_pay_731f929848aee019', 'treatment', '6th Payment', '6th Payment', 'histcob_654df89f92d7add2', '7319394685', 'Cooch Behar', 'Bikash Barman',
  '2025-09-22', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-22T10:05:00.000Z', '2025-09-22T10:05:00.000Z'
),
(
  'histcob_pay_8c33fea228c35f0a', 'treatment', '7th Payment', '7th Payment', 'histcob_654df89f92d7add2', '7319394685', 'Cooch Behar', 'Bikash Barman',
  '2025-10-06', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-06T10:06:00.000Z', '2025-10-06T10:06:00.000Z'
),
(
  'histcob_pay_47216d3ca162eb73', 'treatment', '8th Payment', '8th Payment', 'histcob_654df89f92d7add2', '7319394685', 'Cooch Behar', 'Bikash Barman',
  '2025-10-10', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-10T10:07:00.000Z', '2025-10-10T10:07:00.000Z'
),
(
  'histcob_pay_14c4fd781a48e1cf', 'treatment', '9th Payment', '9th Payment', 'histcob_654df89f92d7add2', '7319394685', 'Cooch Behar', 'Bikash Barman',
  '2025-10-13', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-13T10:08:00.000Z', '2025-10-13T10:08:00.000Z'
),
(
  'histcob_pay_a298873b15117f72', 'treatment', '10th Payment', '10th Payment', 'histcob_654df89f92d7add2', '7319394685', 'Cooch Behar', 'Bikash Barman',
  '2025-10-20', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-20T10:09:00.000Z', '2025-10-20T10:09:00.000Z'
),
(
  'histcob_pay_88dd3dda59e71b0d', 'treatment', '11th Payment', '11th Payment', 'histcob_654df89f92d7add2', '7319394685', 'Cooch Behar', 'Bikash Barman',
  '2025-10-27', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-27T10:10:00.000Z', '2025-10-27T10:10:00.000Z'
),
(
  'histcob_pay_935bf6a3c38c45b5', 'treatment', 'Advance', 'Advance', 'histcob_85f53df393dd0c9f', '9365310365', 'Cooch Behar', 'HAIDUL ISLAM TALUKDAR',
  '2025-08-02', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:00:00.000Z', '2025-08-02T10:00:00.000Z'
),
(
  'histcob_pay_2f270ac77d87524d', 'treatment', '2nd Payment', '2nd Payment', 'histcob_85f53df393dd0c9f', '9365310365', 'Cooch Behar', 'HAIDUL ISLAM TALUKDAR',
  '2025-08-11', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-11T10:01:00.000Z', '2025-08-11T10:01:00.000Z'
),
(
  'histcob_pay_f80d693d375cc7f6', 'treatment', '3rd Payment', '3rd Payment', 'histcob_85f53df393dd0c9f', '9365310365', 'Cooch Behar', 'HAIDUL ISLAM TALUKDAR',
  '2025-08-15', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-15T10:02:00.000Z', '2025-08-15T10:02:00.000Z'
),
(
  'histcob_pay_9e24eef6670cfa0e', 'treatment', '4th Payment', '4th Payment', 'histcob_85f53df393dd0c9f', '9365310365', 'Cooch Behar', 'HAIDUL ISLAM TALUKDAR',
  '2025-08-23', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:03:00.000Z', '2025-08-23T10:03:00.000Z'
),
(
  'histcob_pay_8344c43fc5846e74', 'treatment', '5th Payment', '5th Payment', 'histcob_85f53df393dd0c9f', '9365310365', 'Cooch Behar', 'HAIDUL ISLAM TALUKDAR',
  '2025-09-08', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-08T10:04:00.000Z', '2025-09-08T10:04:00.000Z'
),
(
  'histcob_pay_312effc8d7f061f1', 'treatment', '6th Payment', '6th Payment', 'histcob_85f53df393dd0c9f', '9365310365', 'Cooch Behar', 'HAIDUL ISLAM TALUKDAR',
  '2025-09-19', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-19T10:05:00.000Z', '2025-09-19T10:05:00.000Z'
),
(
  'histcob_pay_e8c5f52479e2652f', 'treatment', '7th Payment', '7th Payment', 'histcob_85f53df393dd0c9f', '9365310365', 'Cooch Behar', 'HAIDUL ISLAM TALUKDAR',
  '2025-10-20', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-20T10:06:00.000Z', '2025-10-20T10:06:00.000Z'
),
(
  'histcob_pay_a6ebd92177bd9f73', 'treatment', '8th Payment', '8th Payment', 'histcob_85f53df393dd0c9f', '9365310365', 'Cooch Behar', 'HAIDUL ISLAM TALUKDAR',
  '2025-11-07', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet, TK-corrected 16.09.2026 -- previously held back for bad mobile/date-in-money-cell corruption)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-07T10:07:00.000Z', '2025-11-07T10:07:00.000Z'
);
