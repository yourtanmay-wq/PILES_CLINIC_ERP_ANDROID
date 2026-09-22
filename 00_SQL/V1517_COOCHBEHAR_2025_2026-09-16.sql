-- কোচবিহার ব্রাঞ্চ -- Patients2025_Details শিট -- 107 জন
-- ২৪৭ জনের মধ্যে ১১৪ জন বিল-করা। ৩ জন সত্যিকারের শিট-করাপশন বাদ
-- (HAIDUL ISLAM TALUKDAR, Bikash Barman, RANJIT BARMAN -- BILL/কিস্তি
-- ঘরে তারিখ ঢুকে গেছে), ২ জনের মোবাইল ভুল বাদ (AZAD ALI MANDAL,
-- SAHANA PARVIN), ২ জন (GULBAHADUR ALI, BABU RAHAMAN) সাময়িক বাদ --
-- লাইভে ২০২৬-এ একই মোবাইলে আগে থেকে আছেন, TK ব্র্যাঞ্চে খোঁজ নিচ্ছেন।
-- বাকি ১০৭ জন। শিটের "Total Paid" ঘর প্রায়ই ফাঁকা ছিল -- কিস্তির
-- যোগফলকেই আসল টাকা ধরা হয়েছে, প্রতিটা কিস্তি আলাদা যাচাই করে
-- (তারিখ-টাকা উল্টে যাওয়া নেই তা নিশ্চিত হয়ে)।
-- লাইভ-ডুপ্লিকেট-চেক (V1515) ও patientId-সংঘর্ষ-চেক (V1516) দুটোই
-- TK চালিয়ে দেখেছেন -- বাকি ১০৭ জনের কারো সমস্যা নেই।

insert into public.patients (
  id, "patientId", date, "registrationDate", "visitDate",
  name, mobile, "altMobile", branch, age, sex,
  address, disease, bill,
  stage, queue, "doctorComplete",
  "createdBy", "registeredBy", "createdAt", "updatedAt"
) values
(
  'histcob_33b2cdc447f617de', 'COB-05022025-001', '2025-02-05', '2025-02-05', '2025-02-05',
  'SAMINUR RAHMAN', '7077229318', '', 'Cooch Behar', '36', 'Male',
  'CHILAKHANA, CHILAKHANA, TUFANGANJ', 'Fistula', '21000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-05T10:00:00.000Z', '2025-02-05T10:00:00.000Z'
),
(
  'histcob_e12c8936a55b18aa', 'COB-07022025-001', '2025-02-07', '2025-02-07', '2025-02-07',
  'HAMIDUL HOWK', '8515822521', '', 'Cooch Behar', '35', 'Male',
  'BALABHOOT, TUFANGANJ, TUFANGANJ, COOCHBEHAR', 'Fistula', '65000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-07T10:00:00.000Z', '2025-02-07T10:00:00.000Z'
),
(
  'histcob_f4569bb2a0b22d10', 'COB-10022025-001', '2025-02-10', '2025-02-10', '2025-02-10',
  'SIKANDAR ALI SIKDAR', '7002065018', '', 'Cooch Behar', '33', 'Male',
  'MANKACHAR, MANKACHAR, ASSAM', 'Piles', '12000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-10T10:00:00.000Z', '2025-02-10T10:00:00.000Z'
),
(
  'histcob_c58aed205c0af232', 'COB-14022025-001', '2025-02-14', '2025-02-14', '2025-02-14',
  'SANJAY BARMAN', '8101686164', '', 'Cooch Behar', '32', 'Male',
  'LATAPATA, LATAPATA, GHOSKARDANGA, COOCHBEHAR', 'Piles', '24500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-14T10:00:00.000Z', '2025-02-14T10:00:00.000Z'
),
(
  'histcob_d9c4439d2a8b653f', 'COB-17022025-001', '2025-02-17', '2025-02-17', '2025-02-17',
  'PRASANTA SAHA', '8637532847', '', 'Cooch Behar', '53', 'Male',
  'TUFANGANJ, TUFANGANJ, COOCHBEHAR', 'Fistula', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-17T10:00:00.000Z', '2025-02-17T10:00:00.000Z'
),
(
  'histcob_e05061d5b1399048', 'COB-21022025-001', '2025-02-21', '2025-02-21', '2025-02-21',
  'TAPAS PAUL', '6297882139', '', 'Cooch Behar', '26', 'Male',
  'SOLDHKURI, BALARAMPUR, COOCHBEHAR', 'Piles', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-21T10:00:00.000Z', '2025-02-21T10:00:00.000Z'
),
(
  'histcob_e3f61c6f73b2f58d', 'COB-21022025-002', '2025-02-21', '2025-02-21', '2025-02-21',
  'SABINA KHATUN', '8967264485', '', 'Cooch Behar', '23', 'Female',
  'TAKA GACHE, COOCHBEHAR', 'Piles', '16500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-21T10:00:00.000Z', '2025-02-21T10:00:00.000Z'
),
(
  'histcob_a2e08625f9b81318', 'COB-22022025-001', '2025-02-22', '2025-02-22', '2025-02-22',
  'JAIRUL ISLAM', '6000027050', '', 'Cooch Behar', '39', 'Male',
  'HALAKURA, DHIGATAR, COOCHBEHAR', 'Fissure', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-22T10:00:00.000Z', '2025-02-22T10:00:00.000Z'
),
(
  'histcob_c749d0f312ac3e9b', 'COB-26022025-001', '2025-02-26', '2025-02-26', '2025-02-26',
  'SOUMEN SARKAR', '7319344840', '', 'Cooch Behar', '35', 'Male',
  'CHHATOA, TUFANGANJ, COOCHBEHAR', 'Fissure', '16000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-26T10:00:00.000Z', '2025-02-26T10:00:00.000Z'
),
(
  'histcob_1f616c2d57329ea7', 'COB-28022025-001', '2025-02-28', '2025-02-28', '2025-02-28',
  'BHARAT ROY', '9749171722', '', 'Cooch Behar', '33', 'Male',
  'CHOTOBULALMARI, PETLA, TUFANGANJ, COOCHBEHAR', 'Piles', '21500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-28T10:00:00.000Z', '2025-02-28T10:00:00.000Z'
),
(
  'histcob_e635b95314fb8661', 'COB-28022025-002', '2025-02-28', '2025-02-28', '2025-02-28',
  'ESMITA PARVEEN', '9832147783', '', 'Cooch Behar', '27', 'Female',
  'KALAKATA, MAHAMARI, COOCHBEHAR', 'Piles', '18500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-28T10:00:00.000Z', '2025-02-28T10:00:00.000Z'
),
(
  'histcob_aa10b76aad4142d1', 'COB-07032025-001', '2025-03-07', '2025-03-07', '2025-03-07',
  'NUR MAHAMMAD', '9854454132', '', 'Cooch Behar', '35', 'Male',
  'KAUCUARKASH, DHUBRI ASSAM', 'Piles, Fissure', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-07T10:00:00.000Z', '2025-03-07T10:00:00.000Z'
),
(
  'histcob_9cf8638006b3febb', 'COB-07032025-002', '2025-03-07', '2025-03-07', '2025-03-07',
  'SABINA KHATUN', '9387771476', '', 'Cooch Behar', '22', 'Female',
  'DAWAGURI, SRIRAMPUR, GOSSAIGAON, KOKRAJHAR', 'Piles', '19500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-07T10:00:00.000Z', '2025-03-07T10:00:00.000Z'
),
(
  'histcob_0fff6a18f0ea083e', 'COB-10032025-001', '2025-03-10', '2025-03-10', '2025-03-10',
  'RAKIBUL ISLAM', '8942024490', '', 'Cooch Behar', '31', 'Male',
  'PIKNIDHARA, KARALA, SAHEBGANJ, COOCHBEHAR', 'Piles, Fistula', '34000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-10T10:00:00.000Z', '2025-03-10T10:00:00.000Z'
),
(
  'histcob_911d8a53c915351e', 'COB-14032025-001', '2025-03-14', '2025-03-14', '2025-03-14',
  'HAREN DAS', '9382016981', '', 'Cooch Behar', '30', 'Male',
  'PUNDIBARI, COOCHBEHAR', 'Fistula', '23000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-14T10:00:00.000Z', '2025-03-14T10:00:00.000Z'
),
(
  'histcob_81c3477fac6a1da5', 'COB-25032025-001', '2025-03-25', '2025-03-25', '2025-03-25',
  'UJJAL PAUL', '8900136977', '', 'Cooch Behar', '30', 'Male',
  'SONDHU KURI, BALARAMPUR, TUFANGANJ, COOCHBEHAR', 'Piles', '21000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-25T10:00:00.000Z', '2025-03-25T10:00:00.000Z'
),
(
  'histcob_9e7dd96168a1f8a4', 'COB-28032025-001', '2025-03-28', '2025-03-28', '2025-03-28',
  'BIPLAB BARMAN', '8391907018', '', 'Cooch Behar', '27', 'Male',
  'RAKHALMARI, RAKHALMARI, DINHATA, COOCHBEHAR', 'Piles', '19500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-28T10:00:00.000Z', '2025-03-28T10:00:00.000Z'
),
(
  'histcob_c799f6566e807b83', 'COB-28032025-002', '2025-03-28', '2025-03-28', '2025-03-28',
  'BIMALA SHARMA', '9382191134', '', 'Cooch Behar', '58', 'Male',
  'TENGANMARI, RAJARHAT, PUNDIBARI, COOCHBEHAR', 'Piles', '17850',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-28T10:00:00.000Z', '2025-03-28T10:00:00.000Z'
),
(
  'histcob_e797b13bf8165f88', 'COB-31032025-001', '2025-03-31', '2025-03-31', '2025-03-31',
  'SANJAY BARMAN', '6296201505', '', 'Cooch Behar', '28', 'Male',
  'NAJIRAM, BARUIPARA, BOXIHAT, COOCHBEHAR', 'Piles', '21000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-31T10:00:00.000Z', '2025-03-31T10:00:00.000Z'
),
(
  'histcob_89c6fa39fde9d991', 'COB-04042025-001', '2025-04-04', '2025-04-04', '2025-04-04',
  'BISWAJIT BARMAN', '7892748445', '', 'Cooch Behar', '24', 'Male',
  'SITAI, SITAI, SITAI, COOCHBEHAR', 'Piles, Fissure', '17000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-04T10:00:00.000Z', '2025-04-04T10:00:00.000Z'
),
(
  'histcob_b4f7dc9ea7262ef1', 'COB-04042025-002', '2025-04-04', '2025-04-04', '2025-04-04',
  'UTPAL MODAK', '8101663797', '', 'Cooch Behar', '40', 'Male',
  'CHIKLIGURI, M, COOCHBEHAR', 'Fistula', '23000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-04T10:00:00.000Z', '2025-04-04T10:00:00.000Z'
),
(
  'histcob_5b04fd24f306ac08', 'COB-07042025-001', '2025-04-07', '2025-04-07', '2025-04-07',
  'RATAN SARKAR', '7586817093', '', 'Cooch Behar', '40', 'Male',
  'BALASHI, PAKHIHAG, MATHAVAGA, COOCHBEHAR', 'Fistula', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-07T10:00:00.000Z', '2025-04-07T10:00:00.000Z'
),
(
  'histcob_4c64e7be243f0111', 'COB-07042025-002', '2025-04-07', '2025-04-07', '2025-04-07',
  'RAJDEEP BARMAN', '9749663105', '', 'Cooch Behar', '4', 'Male',
  'RAMTHENGA, RAMTHENGA, GHOKSADHANGA, COOCHBEHAR', 'Piles', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-07T10:00:00.000Z', '2025-04-07T10:00:00.000Z'
),
(
  'histcob_64302ba618230674', 'COB-07042025-003', '2025-04-07', '2025-04-07', '2025-04-07',
  'AJIR MIYA', '9907668749', '', 'Cooch Behar', '62', 'Male',
  'TUFANGANJ, BOXIHAT, COOCHBEHAR', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-07T10:00:00.000Z', '2025-04-07T10:00:00.000Z'
),
(
  'histcob_ab3cb548c471e903', 'COB-11042025-001', '2025-04-11', '2025-04-11', '2025-04-11',
  'SUBRATA DAS', '9144244564', '', 'Cooch Behar', '22', 'Male',
  'CHANDAMARI, SALBARI, SHIVPUR, COOCHBEHAR', 'Piles, Fissure', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-11T10:00:00.000Z', '2025-04-11T10:00:00.000Z'
),
(
  'histcob_45f31e18bd977b43', 'COB-14042025-001', '2025-04-14', '2025-04-14', '2025-04-14',
  'NASHIP THAPA', '9775616759', '', 'Cooch Behar', '28', 'Male',
  'DARJEELING', 'Piles', '14000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-14T10:00:00.000Z', '2025-04-14T10:00:00.000Z'
),
(
  'histcob_62c53b71072deeda', 'COB-18042025-001', '2025-04-18', '2025-04-18', '2025-04-18',
  'UTTAM DEY', '6294125833', '', 'Cooch Behar', '35', 'Male',
  'SAJERPAR, GHORAMARA, COOCHBEHAR', 'Piles', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-18T10:00:00.000Z', '2025-04-18T10:00:00.000Z'
),
(
  'histcob_0dfa4608b639d066', 'COB-21042025-001', '2025-04-21', '2025-04-21', '2025-04-21',
  'MANGLU MODAK', '9799419924', '', 'Cooch Behar', '60', 'Male',
  'BARALAL DHANDRA, CHILKIRHAT, COOCHBEHAR', 'Piles', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-21T10:00:00.000Z', '2025-04-21T10:00:00.000Z'
),
(
  'histcob_134f9320b9f68562', 'COB-21042025-002', '2025-04-21', '2025-04-21', '2025-04-21',
  'BASANTI ROY', '8172023975', '', 'Cooch Behar', '55', 'Female',
  'FULBARI, FULBARI, GOSANIMARI, COOCHBEHAR', 'Piles', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-21T10:00:00.000Z', '2025-04-21T10:00:00.000Z'
),
(
  'histcob_bf4af50c6fba0028', 'COB-25042025-001', '2025-04-25', '2025-04-25', '2025-04-25',
  'ANOWAR HOSSAIN', '9954216046', '', 'Cooch Behar', '62', 'Male',
  'RIMIJHIMIGHAT, AMINKATA, GOSSAIGAON', 'Piles', '17000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-25T10:00:00.000Z', '2025-04-25T10:00:00.000Z'
),
(
  'histcob_43a2b00aba4054b4', 'COB-25042025-002', '2025-04-25', '2025-04-25', '2025-04-25',
  'SHANKAR DAS', '9851509380', '', 'Cooch Behar', '35', 'Male',
  'CHILAKHANA, COOCHBEHAR', 'Piles, Fistula', '68000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-25T10:00:00.000Z', '2025-04-25T10:00:00.000Z'
),
(
  'histcob_70df6f52b00d623f', 'COB-25042025-003', '2025-04-25', '2025-04-25', '2025-04-25',
  'PRASANJIT BARMAN', '8016093930', '', 'Cooch Behar', '28', 'Male',
  'NAJIRHAT, MANSAB SEORAGURI, SAHEBJANJ, COOCHBEHAR', 'Piles, Fissure', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-25T10:00:00.000Z', '2025-04-25T10:00:00.000Z'
),
(
  'histcob_9a585a9d836191eb', 'COB-28042025-001', '2025-04-28', '2025-04-28', '2025-04-28',
  'ANOYARA BIBI', '9851238886', '', 'Cooch Behar', '45', 'Female',
  '7MAIL, KOTWALI, COOCHBEHAR', 'Fissure', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-28T10:00:00.000Z', '2025-04-28T10:00:00.000Z'
),
(
  'histcob_d3c33478363844c2', 'COB-02052025-001', '2025-05-02', '2025-05-02', '2025-05-02',
  'ABDUL KARIM', '6003768668', '', 'Cooch Behar', '40', 'Male',
  'JOGIKOPA ISLAMPUR, BONGAIGAON', 'Piles', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-02T10:00:00.000Z', '2025-05-02T10:00:00.000Z'
),
(
  'histcob_683d2e48edd0125e', 'COB-02052025-002', '2025-05-02', '2025-05-02', '2025-05-02',
  'MANIK BARMAN', '6295725190', '', 'Cooch Behar', '26', 'Male',
  'UTTAR DAYGOUN, ALIPUR', 'Piles', '15573',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-02T10:00:00.000Z', '2025-05-02T10:00:00.000Z'
),
(
  'histcob_caef3134acb07c4b', 'COB-05052025-001', '2025-05-05', '2025-05-05', '2025-05-05',
  'MALIK MIA', '8670753016', '', 'Cooch Behar', '55', 'Male',
  'DHOLGHOBINDA, DHOLGHOBINDA, DINHATA, COOCHBEHAR', 'Piles', '28500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-05T10:00:00.000Z', '2025-05-05T10:00:00.000Z'
),
(
  'histcob_aea95478e4914235', 'COB-06052025-001', '2025-05-06', '2025-05-06', '2025-05-06',
  'SONEKA ROY', '9851443121', '', 'Cooch Behar', '53', 'Male',
  'SITAI, B.R.CHATRA, SITAI, COOCHBEHAR', 'Piles', '100000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-06T10:00:00.000Z', '2025-05-06T10:00:00.000Z'
),
(
  'histcob_8a35824d93529e1e', 'COB-09052025-001', '2025-05-09', '2025-05-09', '2025-05-09',
  'UJJAL DEY SARKAR', '7477832695', '', 'Cooch Behar', '30', 'Male',
  'RAMPUR, RAMPUR, BOXIHAT, COOCHBEHAR', 'Piles', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-09T10:00:00.000Z', '2025-05-09T10:00:00.000Z'
),
(
  'histcob_1a42f787d2df000f', 'COB-09052025-002', '2025-05-09', '2025-05-09', '2025-05-09',
  'MOBARAK HOSSAIN', '9635600815', '', 'Cooch Behar', '29', 'Male',
  'JIBRAMIRKUTHI, PUTIMARI, KOTWALI, COOCHBEHAR', 'Gupt Rog', '19500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-09T10:00:00.000Z', '2025-05-09T10:00:00.000Z'
),
(
  'histcob_139fe83fc83835c9', 'COB-16052025-001', '2025-05-16', '2025-05-16', '2025-05-16',
  'SATYAJIT SARKAR', '7602384437', '', 'Cooch Behar', '37', 'Male',
  'PUNDIBARI, PUNDIBARI, PUNDIBARI, COOCHBEHAR', 'Piles, Fistula', '37700',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-16T10:00:00.000Z', '2025-05-16T10:00:00.000Z'
),
(
  'histcob_d037ddab6e67d6c8', 'COB-16052025-002', '2025-05-16', '2025-05-16', '2025-05-16',
  'LAXAM MAHATA', '9434827298', '', 'Cooch Behar', '41', 'Male',
  'DEBIBARI, COOCHBEHAR, KOTWALI, COOCHBEHAR', 'Piles', '17500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-16T10:00:00.000Z', '2025-05-16T10:00:00.000Z'
),
(
  'histcob_ec865f519178d9dc', 'COB-19052025-001', '2025-05-19', '2025-05-19', '2025-05-19',
  'LOTIFA BIBI', '8537889405', '', 'Cooch Behar', '28', 'Female',
  'KHOLISHAGURI, GOSANIMARI, GOSANIMARI, COOCHBEHAR', 'Fistula', '28000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-19T10:00:00.000Z', '2025-05-19T10:00:00.000Z'
),
(
  'histcob_994894b5358c3f40', 'COB-23052025-001', '2025-05-23', '2025-05-23', '2025-05-23',
  'SUNIL DHAR', '7363808369', '', 'Cooch Behar', '64', 'Male',
  'CHAPRIPARA, PUNDIBARI, PUNDIBARI, COOCHBEHAR', 'Piles', '19000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-23T10:00:00.000Z', '2025-05-23T10:00:00.000Z'
),
(
  'histcob_286adb020749e431', 'COB-01062025-001', '2025-06-01', '2025-06-01', '2025-06-01',
  'BISHNU BARMAN', '8016142536', '', 'Cooch Behar', '30', 'Male',
  'SITALKUCHI, COOCHBEHAR', 'Hydrocele', '20009',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-01T10:00:00.000Z', '2025-06-01T10:00:00.000Z'
),
(
  'histcob_e51604d5a824a97f', 'COB-02062025-001', '2025-06-02', '2025-06-02', '2025-06-02',
  'KRITIKA DAS', '7797051503', '', 'Cooch Behar', '30', 'Male',
  'RAMPUR GORVANGA, RAMPUR, BOXIHAT, COOCHBEHAR', 'Piles', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-02T10:00:00.000Z', '2025-06-02T10:00:00.000Z'
),
(
  'histcob_7195cd872914de85', 'COB-02062025-002', '2025-06-02', '2025-06-02', '2025-06-02',
  'YAJADIN MIA', '7318792629', '', 'Cooch Behar', '30', 'Male',
  'sitai, COOCHBEHAR', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-02T10:00:00.000Z', '2025-06-02T10:00:00.000Z'
),
(
  'histcob_0c3ce75e4f1c0e52', 'COB-02062025-003', '2025-06-02', '2025-06-02', '2025-06-02',
  'SAHAJAYAN ALI KHAN', '9395019343', '', 'Cooch Behar', '45', 'Male',
  'JOSIHATIPARA, JOSIHATIPARA, HOWLI, BARPATA', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-02T10:00:00.000Z', '2025-06-02T10:00:00.000Z'
),
(
  'histcob_0484563877f37163', 'COB-06062025-001', '2025-06-06', '2025-06-06', '2025-06-06',
  'HOSSAIN ALI', '9085598519', '', 'Cooch Behar', '26', 'Male',
  'KUCHUYAR KHAS, KALAHAT, DHUBRI, DHUBRI', 'Fistula', '37450',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-06T10:00:00.000Z', '2025-06-06T10:00:00.000Z'
),
(
  'histcob_4e77a7609df270fa', 'COB-06062025-002', '2025-06-06', '2025-06-06', '2025-06-06',
  'NITYANAND SEN', '8900663765', '', 'Cooch Behar', '38', 'Male',
  'GOSANIMARI, DINHATA, DINHATA, COOCHBEHAR', 'Piles', '20700',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-06T10:00:00.000Z', '2025-06-06T10:00:00.000Z'
),
(
  'histcob_60100a5295653f7c', 'COB-09062025-001', '2025-06-09', '2025-06-09', '2025-06-09',
  'SAHID ALI', '8927079334', '', 'Cooch Behar', '20', 'Male',
  'BALASHIR, ONDORON PALHIGANJ, MATHAVANGA, COOCHBEHAR', 'Piles', '8000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-09T10:00:00.000Z', '2025-06-09T10:00:00.000Z'
),
(
  'histcob_a7d49b77deacd666', 'COB-16062025-001', '2025-06-16', '2025-06-16', '2025-06-16',
  'RAFIKUL ISLAM', '7002028352', '', 'Cooch Behar', '25', 'Male',
  'BANIYABARI PART -3, MAKRIJALA, GOURIPUR, DHUBRI', 'Piles', '62600',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-16T10:00:00.000Z', '2025-06-16T10:00:00.000Z'
),
(
  'histcob_b0e05fedd0b0c853', 'COB-16062025-002', '2025-06-16', '2025-06-16', '2025-06-16',
  'PRAKASH DAS', '9609918668', '', 'Cooch Behar', '52', 'Male',
  'FATAKURA, COOCHBEHAR, KOTWALI, COOCHBEHAR', 'Fistula', '39600',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-16T10:00:00.000Z', '2025-06-16T10:00:00.000Z'
),
(
  'histcob_dfa83baa67c08704', 'COB-20062025-001', '2025-06-20', '2025-06-20', '2025-06-20',
  'YOUSIMUDDIN ALI', '6002253819', '', 'Cooch Behar', '38', 'Male',
  'BARAPATA, MOYNAMATA, GOBORDANGA, BAKSHA', 'Piles', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-20T10:00:00.000Z', '2025-06-20T10:00:00.000Z'
),
(
  'histcob_5470e10d3702979c', 'COB-20062025-002', '2025-06-20', '2025-06-20', '2025-06-20',
  'ABDUL WAD SARKAR', '7002111915', '', 'Cooch Behar', '32', 'Male',
  'CHATIPUR, KAJALGOW, KAJALGOW, SIRANG', 'Piles', '26000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-20T10:00:00.000Z', '2025-06-20T10:00:00.000Z'
),
(
  'histcob_bfb3f79b0752023e', 'COB-20062025-003', '2025-06-20', '2025-06-20', '2025-06-20',
  'RAHAMAT ALI', '8116738720', '', 'Cooch Behar', '32', 'Male',
  'KOCHUBAN BARAMKHANA, CHOTOKHOURATI BARI, PUNDIBARI, COOCHBEHAR', 'Piles', '17500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-20T10:00:00.000Z', '2025-06-20T10:00:00.000Z'
),
(
  'histcob_901d9838d96b4335', 'COB-23062025-001', '2025-06-23', '2025-06-23', '2025-06-23',
  'AMINUR ISLAM', '6000633291', '', 'Cooch Behar', '35', 'Male',
  'MANKACHAR, MANKACHAR, SOUTH SALMARA, SOUTH SALMARA', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-23T10:00:00.000Z', '2025-06-23T10:00:00.000Z'
),
(
  'histcob_7b0278288cd7f866', 'COB-28062025-001', '2025-06-28', '2025-06-28', '2025-06-28',
  'KALPANA BALA BARMAN', '6901906971', '', 'Cooch Behar', '30', 'Female',
  'DHUBRI', 'Piles', '46000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:00:00.000Z', '2025-06-28T10:00:00.000Z'
),
(
  'histcob_bcffb4968d179b32', 'COB-30062025-001', '2025-06-30', '2025-06-30', '2025-06-30',
  'IBRAHIM MIAH', '9547395711', '', 'Cooch Behar', '48', 'Male',
  'BHAVANIPRASHAD, KOROLA, DINHATA, COOCHBEHAR', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-30T10:00:00.000Z', '2025-06-30T10:00:00.000Z'
),
(
  'histcob_071070e33d702b53', 'COB-30062025-002', '2025-06-30', '2025-06-30', '2025-06-30',
  'HAFIZUR ISLAM', '8453663281', '', 'Cooch Behar', '25', 'Male',
  'KHARUABANDHA, FAKAMARI, HATSHINGIMARI, SOUTH SHALMARI', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-30T10:00:00.000Z', '2025-06-30T10:00:00.000Z'
),
(
  'histcob_4cf4026e7aa635c7', 'COB-30062025-003', '2025-06-30', '2025-06-30', '2025-06-30',
  'SWAPAN ADHIKARI', '9821257313', '', 'Cooch Behar', '21', 'Male',
  'SAHURVATA, BAGARVATA, BOXIHAT, COOCHBEHAR', 'Piles', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-30T10:00:00.000Z', '2025-06-30T10:00:00.000Z'
),
(
  'histcob_58971ee936826126', 'COB-30062025-004', '2025-06-30', '2025-06-30', '2025-06-30',
  'ASRAFUL ALAM', '9101565033', '', 'Cooch Behar', '33', 'Male',
  'BICHANDOW, BORUNDANGA, GOLOKGANJ, DHUBRI', 'Piles', '130000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-30T10:00:00.000Z', '2025-06-30T10:00:00.000Z'
),
(
  'histcob_8975f81b7aedcaaa', 'COB-01072025-001', '2025-07-01', '2025-07-01', '2025-07-01',
  'RINA DEY SARKAR', '8016432396', '', 'Cooch Behar', '50', 'Female',
  'DOWAGURI, DOWAGURI, KOTWALI, COOCHBEHAR', 'Piles', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-01T10:00:00.000Z', '2025-07-01T10:00:00.000Z'
),
(
  'histcob_60a79ffb83f93a06', 'COB-04072025-001', '2025-07-04', '2025-07-04', '2025-07-04',
  'DEEP ACHARY', '9678800407', '', 'Cooch Behar', '7', 'Male',
  'MACHUYA, SHIMLAGURI, GOBORDANGA, BAKSHA', 'Piles', '45000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-04T10:00:00.000Z', '2025-07-04T10:00:00.000Z'
),
(
  'histcob_cab76b76a688ae4f', 'COB-05072025-001', '2025-07-05', '2025-07-05', '2025-07-05',
  'AMBIYA BIBI', '8348736026', '', 'Cooch Behar', '55', 'Female',
  'deocharai, deocharai, tufanganj, coochbehar', 'Piles', '45000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:00:00.000Z', '2025-07-05T10:00:00.000Z'
),
(
  'histcob_4b5c5e5f9f610f16', 'COB-07072025-001', '2025-07-07', '2025-07-07', '2025-07-07',
  'ASHOK ROY', '7865086004', '', 'Cooch Behar', '48', 'Male',
  'DUDUMARI, MORICHBARI, PUNDIBARI, COOCHBEHAR', 'Fistula', '54000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-07T10:00:00.000Z', '2025-07-07T10:00:00.000Z'
),
(
  'histcob_7ed4021862bbb3c7', 'COB-07072025-002', '2025-07-07', '2025-07-07', '2025-07-07',
  'NOSMINA KHATUN', '9707813942', '', 'Cooch Behar', '32', 'Female',
  'GOYALPARA, DOBTOTA, MONNOI, GOYALPARA', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-07T10:00:00.000Z', '2025-07-07T10:00:00.000Z'
),
(
  'histcob_c8d5ab9ecf12d08e', 'COB-11072025-001', '2025-07-11', '2025-07-11', '2025-07-11',
  'SOKINA BAGAM', '7099900257', '', 'Cooch Behar', '10', 'Female',
  'DURAHATI, DHUBRI, DHUBRI, DHUBRI', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-11T10:00:00.000Z', '2025-07-11T10:00:00.000Z'
),
(
  'histcob_3472df284bd95bf1', 'COB-11072025-002', '2025-07-11', '2025-07-11', '2025-07-11',
  'FAJULUR RAHAMAN', '8638464445', '', 'Cooch Behar', '33', 'Male',
  'DIGHALI BARI, TIPLAUL, RANGTOLI, GOYALPARA', 'Piles', '38000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-11T10:00:00.000Z', '2025-07-11T10:00:00.000Z'
),
(
  'histcob_2a0cc72600e6daf5', 'COB-11072025-003', '2025-07-11', '2025-07-11', '2025-07-11',
  'SIR MANGAL CHANDI DAS', '7636076903', '', 'Cooch Behar', '27', 'Male',
  'BOLODMARI CHOCK, BOLODMARI, GOYALPARA, GOYALPARA', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-11T10:00:00.000Z', '2025-07-11T10:00:00.000Z'
),
(
  'histcob_1b220961f27467d2', 'COB-14072025-001', '2025-07-14', '2025-07-14', '2025-07-14',
  'SOFIKUL RAHAMAN', '9025196675', '', 'Cooch Behar', '24', 'Male',
  'SIRAHANDI, DEULKUCHI, TAMURPUR, BAKSHA', 'Hydrocele', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-14T10:00:00.000Z', '2025-07-14T10:00:00.000Z'
),
(
  'histcob_93e263038005c15f', 'COB-18072025-001', '2025-07-18', '2025-07-18', '2025-07-18',
  'SADDAM HOSSAIN', '7099563030', '', 'Cooch Behar', '23', 'Male',
  'BASANTAPUR, DALKONA, PANCHARATAN, GOYALPARA', 'Fistula', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-18T10:00:00.000Z', '2025-07-18T10:00:00.000Z'
),
(
  'histcob_16666ebdf66c5c79', 'COB-18072025-002', '2025-07-18', '2025-07-18', '2025-07-18',
  'SAKIR AHAMMED', '7896219673', '', 'Cooch Behar', '23', 'Male',
  'SRIKUNA PAHARPUR, BOROKOLA, SILCHAR, SHILCHAR', 'Piles', '80000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-18T10:00:00.000Z', '2025-07-18T10:00:00.000Z'
),
(
  'histcob_48e5c27c8968e4ee', 'COB-25072025-001', '2025-07-25', '2025-07-25', '2025-07-25',
  'IJAJUK HOQUE', '6001155720', '', 'Cooch Behar', '24', 'Male',
  'ALAMGANG PART -5, ALAMGANG, GOURIPUR, DHUBRI', 'Piles', '24000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-25T10:00:00.000Z', '2025-07-25T10:00:00.000Z'
),
(
  'histcob_e59fdcee4ddd5316', 'COB-02082025-001', '2025-08-02', '2025-08-02', '2025-08-02',
  'NiRMAL MAJHI', '8972852747', '', 'Cooch Behar', '35', 'Male',
  'COOCHBEHAR', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:00:00.000Z', '2025-08-02T10:00:00.000Z'
),
(
  'histcob_a5045e8d6cb3af2d', 'COB-04082025-001', '2025-08-04', '2025-08-04', '2025-08-04',
  'NONDALAL NATH', '7006246957', '', 'Cooch Behar', '36', 'Male',
  'DAWONGOWN, NORTH BOITAMARI, BONGAIGAON, BONGAIGAON', 'Piles, Fistula', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-04T10:00:00.000Z', '2025-08-04T10:00:00.000Z'
),
(
  'histcob_fab2ced4478e6ed6', 'COB-08082025-001', '2025-08-08', '2025-08-08', '2025-08-08',
  'MOINUL HOQUE', '6002555376', '', 'Cooch Behar', '31', 'Male',
  'BIZNI, BIZNI, BIZNI, CHRANG', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-08T10:00:00.000Z', '2025-08-08T10:00:00.000Z'
),
(
  'histcob_cc45353f1f79ec1c', 'COB-08082025-002', '2025-08-08', '2025-08-08', '2025-08-08',
  'SANDIP DAS', '9309578296', '', 'Cooch Behar', '30', 'Male',
  'KUMARIJAN, KUMARIJAN, SAMUKTALA, ALIPUR', 'Piles', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-08T10:00:00.000Z', '2025-08-08T10:00:00.000Z'
),
(
  'histcob_b058ddba46aaf6fc', 'COB-08082025-003', '2025-08-08', '2025-08-08', '2025-08-08',
  'NITYAHARI DAS', '8116492048', '', 'Cooch Behar', '42', 'Male',
  'KISHNAPUR, KISHNAPUR, TUFANGANJ, COOCHBEHAR', 'Fistula', '72000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-08T10:00:00.000Z', '2025-08-08T10:00:00.000Z'
),
(
  'histcob_7c3a5085bdb5c5d1', 'COB-22082025-001', '2025-08-22', '2025-08-22', '2025-08-22',
  'SUJATA DAS', '7810953907', '', 'Cooch Behar', '22', 'Female',
  'HIRARKUTHI, GHUGUMARI, KOTWALI, COOCHBEHAR', 'Piles', '17000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-22T10:00:00.000Z', '2025-08-22T10:00:00.000Z'
),
(
  'histcob_c9bd49b3462a4bcd', 'COB-23082025-001', '2025-08-23', '2025-08-23', '2025-08-23',
  'JONEKA BIBI', '9474145911', '', 'Cooch Behar', '42', 'Female',
  'DAWANHAT, COOCHBEHAR', 'Piles', '45000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:00:00.000Z', '2025-08-23T10:00:00.000Z'
),
(
  'histcob_bcf4561169e031d3', 'COB-08092025-001', '2025-09-08', '2025-09-08', '2025-09-08',
  'Swapna Sarkar', '9564015320', '', 'Cooch Behar', '26', 'Female',
  'Bittibari, Kumargram, Kumargram, Alipur duar', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-08T10:00:00.000Z', '2025-09-08T10:00:00.000Z'
),
(
  'histcob_72a20fdbe4302d35', 'COB-12092025-001', '2025-09-12', '2025-09-12', '2025-09-12',
  'ABDUL KADER', '9864151048', '', 'Cooch Behar', '19', 'Male',
  'BALAJAN, BALAJAN, GOURIPUR, DHUBRI', 'Piles', '48000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-12T10:00:00.000Z', '2025-09-12T10:00:00.000Z'
),
(
  'histcob_e1711b749c6dd3ff', 'COB-15092025-001', '2025-09-15', '2025-09-15', '2025-09-15',
  'Samiyour Hoque', '9954239481', '', 'Cooch Behar', '26', 'Male',
  'Jomunotali, Kottimari, Gosaigawn, Kokrajhar', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-15T10:00:00.000Z', '2025-09-15T10:00:00.000Z'
),
(
  'histcob_96abd224c4382a71', 'COB-15092025-002', '2025-09-15', '2025-09-15', '2025-09-15',
  'Dilip Barman', '7076916258', '', 'Cooch Behar', '38', 'Male',
  'Sitalkuchi, Gosairhat, Mathavanga, Coochbehar', 'Fistula', '65000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-15T10:00:00.000Z', '2025-09-15T10:00:00.000Z'
),
(
  'histcob_5daf9ec32112b024', 'COB-15092025-003', '2025-09-15', '2025-09-15', '2025-09-15',
  'HAIDAR ALI', '8638342633', '', 'Cooch Behar', '35', 'Male',
  'BOWSMARI, BALAGAAN, GOLOKGANJ, DHUBRI', 'Piles', '39000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-15T10:00:00.000Z', '2025-09-15T10:00:00.000Z'
),
(
  'histcob_f6e3c118955fc251', 'COB-03102025-001', '2025-10-03', '2025-10-03', '2025-10-03',
  'Jamiran Bibi', '7864060845', '', 'Cooch Behar', '38', 'Female',
  'Deochorai, Deochorai, Tufanganj, Coochbehar', 'Piles', '21000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-03T10:00:00.000Z', '2025-10-03T10:00:00.000Z'
),
(
  'histcob_bd2af8792bd4e4c6', 'COB-06102025-001', '2025-10-06', '2025-10-06', '2025-10-06',
  'HIMANI BARMAN', '7029578077', '', 'Cooch Behar', '36', 'Female',
  'DHAKOGURI, KHATTIBARI, GHOSKADANGA, COOCHBEHAR', 'Piles, Fissure', '41000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-06T10:00:00.000Z', '2025-10-06T10:00:00.000Z'
),
(
  'histcob_83244ad1b9913bfe', 'COB-10102025-001', '2025-10-10', '2025-10-10', '2025-10-10',
  'KHOGENDRA BARMAN', '9895655656', '', 'Cooch Behar', '31', 'Male',
  'KANURI 2ND KHANDA, LAMIMARI, GOLOKGANJ, DHUBRI', 'Piles', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-10T10:00:00.000Z', '2025-10-10T10:00:00.000Z'
),
(
  'histcob_6cf03d81db0e78b1', 'COB-10102025-002', '2025-10-10', '2025-10-10', '2025-10-10',
  'KABIR AHAMMED', '6000092308', '', 'Cooch Behar', '31', 'Male',
  'SINGIMARI, TISTERPAR, DHUBRI, DHUBRI', 'Piles', '23000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-10T10:00:00.000Z', '2025-10-10T10:00:00.000Z'
),
(
  'histcob_4a5176c4cc59ad47', 'COB-16102025-001', '2025-10-16', '2025-10-16', '2025-10-16',
  'RATAN SARKAR', '9593682252', '', 'Cooch Behar', '49', 'Male',
  'SISHABE PALLI, DAWANHAT, KOTWALI, COOCHBEHAR', 'Piles', '31000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-16T10:00:00.000Z', '2025-10-16T10:00:00.000Z'
),
(
  'histcob_60e343857b64d688', 'COB-23102025-001', '2025-10-23', '2025-10-23', '2025-10-23',
  'JANNATUL HOSSAIN', '9679992827', '', 'Cooch Behar', '26', 'Male',
  'MOYNAGURI, DUMUKHA NOYARHAT, KOTWALI, COOCHBEHAR', 'Piles', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-23T10:00:00.000Z', '2025-10-23T10:00:00.000Z'
),
(
  'histcob_cc172fd73af0e776', 'COB-31102025-001', '2025-10-31', '2025-10-31', '2025-10-31',
  'SAHERA BIBI', '9635055340', '', 'Cooch Behar', '52', 'Female',
  'DAWAGURI, DAWAGURI, KOTWALI, COOCHBEHAR', 'Piles', '12000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-31T10:00:00.000Z', '2025-10-31T10:00:00.000Z'
),
(
  'histcob_2c62eb023bb4094b', 'COB-03112025-001', '2025-11-03', '2025-11-03', '2025-11-03',
  'Soma Das', '7908386978', '', 'Cooch Behar', '30', 'Female',
  'Jatrapur, Rajarhat, Pundibari, Coochbehar', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-03T10:00:00.000Z', '2025-11-03T10:00:00.000Z'
),
(
  'histcob_c21ffa9773038b6d', 'COB-05112025-001', '2025-11-05', '2025-11-05', '2025-11-05',
  'RAHIM BADSHA', '7086480986', '', 'Cooch Behar', '34', 'Male',
  '', 'Piles', '41000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-05T10:00:00.000Z', '2025-11-05T10:00:00.000Z'
),
(
  'histcob_856933f4588b3c65', 'COB-07112025-001', '2025-11-07', '2025-11-07', '2025-11-07',
  'Biswasnath Ghosh', '8653755936', '', 'Cooch Behar', '45', 'Male',
  'Shoul Dhubri mondol para, Mondol para, Tufanganj, Coochbehar', 'Fistula', '45000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-07T10:00:00.000Z', '2025-11-07T10:00:00.000Z'
),
(
  'histcob_8675fd0218e3725e', 'COB-14112025-001', '2025-11-14', '2025-11-14', '2025-11-14',
  'NUR KALAM', '9395202505', '', 'Cooch Behar', '43', 'Male',
  'KODALDHOWA, KALAPARI, MANKACHAR, SOUTH SALMARA', 'Piles', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-14T10:00:00.000Z', '2025-11-14T10:00:00.000Z'
),
(
  'histcob_84b9282c5fcf5ee1', 'COB-17112025-001', '2025-11-17', '2025-11-17', '2025-11-17',
  'ABDUL RAHIM MANDAL', '9957718621', '', 'Cooch Behar', '32', 'Male',
  'VAWRAGURI, VAWRAGURI, GOSAIGOUN, COOCHBEHAR', 'Piles', '46000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-17T10:00:00.000Z', '2025-11-17T10:00:00.000Z'
),
(
  'histcob_bbec894742bc5a3b', 'COB-17112025-002', '2025-11-17', '2025-11-17', '2025-11-17',
  'MIJANUR SAKH', '9957414247', '', 'Cooch Behar', '38', 'Male',
  'JUNGA CHAYALI, KALAPANI, MANKACHOR, SOUTH SALMARA', 'Piles', '45000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-17T10:00:00.000Z', '2025-11-17T10:00:00.000Z'
),
(
  'histcob_f56850588e91329c', 'COB-21112025-001', '2025-11-21', '2025-11-21', '2025-11-21',
  'RUMPA DAS', '8016432607', '', 'Cooch Behar', '24', 'Male',
  'DHALDABRI, VANUKUMARI, BOKSIRHAT, COOCHBEHAR', 'Piles', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-21T10:00:00.000Z', '2025-11-21T10:00:00.000Z'
),
(
  'histcob_88fd122e254c4d11', 'COB-26112025-001', '2025-11-26', '2025-11-26', '2025-11-26',
  'Bijli Bibi', '9647802520', '', 'Cooch Behar', '31', 'Female',
  'Deyanhat, Deyanhat, Koutuali, Coochbehar', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-26T10:00:00.000Z', '2025-11-26T10:00:00.000Z'
),
(
  'histcob_9b9e66964d52daee', 'COB-15122025-001', '2025-12-15', '2025-12-15', '2025-12-15',
  'MONIRUL HOSSAIN', '7478233316', '', 'Cooch Behar', '29', 'Male',
  'BALARAMPUR, BALARAMPUR, TUFANGANJ, COOCHBEHAR', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-15T10:00:00.000Z', '2025-12-15T10:00:00.000Z'
),
(
  'histcob_1bef335d59ff38f9', 'COB-15122025-002', '2025-12-15', '2025-12-15', '2025-12-15',
  'MOFIJUL HOQUE', '8372835069', '', 'Cooch Behar', '29', 'Male',
  'BANIDAS, GR ATIYABARI, DINHATA, COOCHBEHAR', 'Fistula', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-15T10:00:00.000Z', '2025-12-15T10:00:00.000Z'
),
(
  'histcob_523bba61ee37cc56', 'COB-15122025-003', '2025-12-15', '2025-12-15', '2025-12-15',
  'SUKBILASH ROY SHINGHO', '7683038396', '', 'Cooch Behar', '27', 'Male',
  'KRISHNAPUR, KRISHNAPUR, TUFANGANJ, COOCHBEHAR', 'Fistula', '44000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-15T10:00:00.000Z', '2025-12-15T10:00:00.000Z'
),
(
  'histcob_6f27c83300edc047', 'COB-19122025-001', '2025-12-19', '2025-12-19', '2025-12-19',
  'BADAL SAHA', '9382762795', '', 'Cooch Behar', '45', 'Male',
  'JATRAPUR, RAJARHAT, PUNDIBARI, COOCHBEHAR', 'Piles', '28000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-19T10:00:00.000Z', '2025-12-19T10:00:00.000Z'
),
(
  'histcob_80710fdfcdd97f8b', 'COB-19122025-002', '2025-12-19', '2025-12-19', '2025-12-19',
  'SANATAN DAS', '9091566634', '', 'Cooch Behar', '30', 'Male',
  'JATRAPUR, RAJARHAT, PUNDIBARI, COOCHBEHAR', 'Fistula', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-19T10:00:00.000Z', '2025-12-19T10:00:00.000Z'
),
(
  'histcob_b35a4216f585623d', 'COB-22122025-001', '2025-12-22', '2025-12-22', '2025-12-22',
  'AMIT MAJUMDAR', '7586986796', '', 'Cooch Behar', '45', 'Male',
  'AROBINDO KOLONI, ALIPUR DUAR GANSONG, ALIPUR DUAR, ALIPUR DUAR', 'Piles', '33000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-22T10:00:00.000Z', '2025-12-22T10:00:00.000Z'
),
(
  'histcob_e8ceaad56e8e9283', 'COB-26122025-001', '2025-12-26', '2025-12-26', '2025-12-26',
  'RAJIB AHAMMED', '9832844964', '', 'Cooch Behar', '22', 'Male',
  'SATGRAM, KUSSAMARI, MATHAVANGA, COOCHBEHAR', 'Fistula', '180000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-26T10:00:00.000Z', '2025-12-26T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'histcob_pay_ee1b78049d3a8498', 'treatment', 'Advance', 'Advance', 'histcob_33b2cdc447f617de', '7077229318', 'Cooch Behar', 'SAMINUR RAHMAN',
  '2025-02-05', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-05T10:15:00.000Z', '2025-02-05T10:15:00.000Z'
),
(
  'histcob_pay_3335883ceca3bc65', 'treatment', '2nd Payment', '2nd Payment', 'histcob_33b2cdc447f617de', '7077229318', 'Cooch Behar', 'SAMINUR RAHMAN',
  '2025-02-07', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-07T10:16:00.000Z', '2025-02-07T10:16:00.000Z'
),
(
  'histcob_pay_399c9beccd709091', 'treatment', '3rd Payment', '3rd Payment', 'histcob_33b2cdc447f617de', '7077229318', 'Cooch Behar', 'SAMINUR RAHMAN',
  '2025-02-14', '2500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-14T10:17:00.000Z', '2025-02-14T10:17:00.000Z'
),
(
  'histcob_pay_7609910bdf5681cc', 'treatment', '4th Payment', '4th Payment', 'histcob_33b2cdc447f617de', '7077229318', 'Cooch Behar', 'SAMINUR RAHMAN',
  '2025-02-17', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-17T10:18:00.000Z', '2025-02-17T10:18:00.000Z'
),
(
  'histcob_pay_4813570527939d0e', 'treatment', '5th Payment', '5th Payment', 'histcob_33b2cdc447f617de', '7077229318', 'Cooch Behar', 'SAMINUR RAHMAN',
  '2025-02-21', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-21T10:19:00.000Z', '2025-02-21T10:19:00.000Z'
),
(
  'histcob_pay_0731c83b9c33ac85', 'treatment', '6th Payment', '6th Payment', 'histcob_33b2cdc447f617de', '7077229318', 'Cooch Behar', 'SAMINUR RAHMAN',
  '2025-02-24', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-24T10:20:00.000Z', '2025-02-24T10:20:00.000Z'
),
(
  'histcob_pay_72d80b69a80b2c26', 'treatment', '7th Payment', '7th Payment', 'histcob_33b2cdc447f617de', '7077229318', 'Cooch Behar', 'SAMINUR RAHMAN',
  '2025-02-28', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-28T10:21:00.000Z', '2025-02-28T10:21:00.000Z'
),
(
  'histcob_pay_8b0b88b631a09893', 'treatment', '8th Payment', '8th Payment', 'histcob_33b2cdc447f617de', '7077229318', 'Cooch Behar', 'SAMINUR RAHMAN',
  '2025-03-07', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-07T10:22:00.000Z', '2025-03-07T10:22:00.000Z'
),
(
  'histcob_pay_c6bebd5e4544b3ff', 'treatment', '9th Payment', '9th Payment', 'histcob_33b2cdc447f617de', '7077229318', 'Cooch Behar', 'SAMINUR RAHMAN',
  '2025-03-10', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-10T10:23:00.000Z', '2025-03-10T10:23:00.000Z'
),
(
  'histcob_pay_f027d65cbbba479e', 'treatment', '10th Payment', '10th Payment', 'histcob_33b2cdc447f617de', '7077229318', 'Cooch Behar', 'SAMINUR RAHMAN',
  '2025-03-17', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-17T10:24:00.000Z', '2025-03-17T10:24:00.000Z'
),
(
  'histcob_pay_72a8ff0640c885e6', 'treatment', '11th Payment', '11th Payment', 'histcob_33b2cdc447f617de', '7077229318', 'Cooch Behar', 'SAMINUR RAHMAN',
  '2025-03-21', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-21T10:25:00.000Z', '2025-03-21T10:25:00.000Z'
),
(
  'histcob_pay_bd705197a26aae44', 'treatment', '12th Payment', '12th Payment', 'histcob_33b2cdc447f617de', '7077229318', 'Cooch Behar', 'SAMINUR RAHMAN',
  '2025-03-24', '750', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-24T10:26:00.000Z', '2025-03-24T10:26:00.000Z'
),
(
  'histcob_pay_a5b8f8700449c1bc', 'treatment', '13th Payment', '13th Payment', 'histcob_33b2cdc447f617de', '7077229318', 'Cooch Behar', 'SAMINUR RAHMAN',
  '2025-04-02', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-02T10:27:00.000Z', '2025-04-02T10:27:00.000Z'
),
(
  'histcob_pay_1c73caa1e66ed4d9', 'treatment', '14th Payment', '14th Payment', 'histcob_33b2cdc447f617de', '7077229318', 'Cooch Behar', 'SAMINUR RAHMAN',
  '2025-05-09', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-09T10:28:00.000Z', '2025-05-09T10:28:00.000Z'
),
(
  'histcob_pay_dfec9f1ee72f9c4e', 'treatment', 'Advance', 'Advance', 'histcob_e12c8936a55b18aa', '8515822521', 'Cooch Behar', 'HAMIDUL HOWK',
  '2025-02-07', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-07T10:15:00.000Z', '2025-02-07T10:15:00.000Z'
),
(
  'histcob_pay_a93682befe54304f', 'treatment', 'Advance', 'Advance', 'histcob_f4569bb2a0b22d10', '7002065018', 'Cooch Behar', 'SIKANDAR ALI SIKDAR',
  '2025-02-10', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- combined/date-unclear in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-10T10:15:00.000Z', '2025-02-10T10:15:00.000Z'
),
(
  'histcob_pay_b9d593a0beef55a3', 'treatment', 'Advance', 'Advance', 'histcob_c58aed205c0af232', '8101686164', 'Cooch Behar', 'SANJAY BARMAN',
  '2025-02-14', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-14T10:15:00.000Z', '2025-02-14T10:15:00.000Z'
),
(
  'histcob_pay_39a8c629a619a43c', 'treatment', '2nd Payment', '2nd Payment', 'histcob_c58aed205c0af232', '8101686164', 'Cooch Behar', 'SANJAY BARMAN',
  '2025-02-17', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-17T10:16:00.000Z', '2025-02-17T10:16:00.000Z'
),
(
  'histcob_pay_c4ff02544665359a', 'treatment', '3rd Payment', '3rd Payment', 'histcob_c58aed205c0af232', '8101686164', 'Cooch Behar', 'SANJAY BARMAN',
  '2025-02-21', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-21T10:17:00.000Z', '2025-02-21T10:17:00.000Z'
),
(
  'histcob_pay_a97aba93ab2269dc', 'treatment', '4th Payment', '4th Payment', 'histcob_c58aed205c0af232', '8101686164', 'Cooch Behar', 'SANJAY BARMAN',
  '2025-02-24', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-24T10:18:00.000Z', '2025-02-24T10:18:00.000Z'
),
(
  'histcob_pay_4dd4f9bbbabfed32', 'treatment', '5th Payment', '5th Payment', 'histcob_c58aed205c0af232', '8101686164', 'Cooch Behar', 'SANJAY BARMAN',
  '2025-02-28', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-28T10:19:00.000Z', '2025-02-28T10:19:00.000Z'
),
(
  'histcob_pay_d51312f585eb385c', 'treatment', '6th Payment', '6th Payment', 'histcob_c58aed205c0af232', '8101686164', 'Cooch Behar', 'SANJAY BARMAN',
  '2025-03-03', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-03T10:20:00.000Z', '2025-03-03T10:20:00.000Z'
),
(
  'histcob_pay_60a6f1b31c68a9b8', 'treatment', '7th Payment', '7th Payment', 'histcob_c58aed205c0af232', '8101686164', 'Cooch Behar', 'SANJAY BARMAN',
  '2025-03-07', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-07T10:21:00.000Z', '2025-03-07T10:21:00.000Z'
),
(
  'histcob_pay_190160413ff7486d', 'treatment', '8th Payment', '8th Payment', 'histcob_c58aed205c0af232', '8101686164', 'Cooch Behar', 'SANJAY BARMAN',
  '2025-03-10', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-10T10:22:00.000Z', '2025-03-10T10:22:00.000Z'
),
(
  'histcob_pay_25239f2d3b62521d', 'treatment', '9th Payment', '9th Payment', 'histcob_c58aed205c0af232', '8101686164', 'Cooch Behar', 'SANJAY BARMAN',
  '2025-03-17', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-17T10:23:00.000Z', '2025-03-17T10:23:00.000Z'
),
(
  'histcob_pay_0abb51f63d2a1304', 'treatment', '10th Payment', '10th Payment', 'histcob_c58aed205c0af232', '8101686164', 'Cooch Behar', 'SANJAY BARMAN',
  '2025-03-21', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-21T10:24:00.000Z', '2025-03-21T10:24:00.000Z'
),
(
  'histcob_pay_83167037515dfd77', 'treatment', '11th Payment', '11th Payment', 'histcob_c58aed205c0af232', '8101686164', 'Cooch Behar', 'SANJAY BARMAN',
  '2025-03-28', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-28T10:25:00.000Z', '2025-03-28T10:25:00.000Z'
),
(
  'histcob_pay_aefef2925b4f8584', 'treatment', '12th Payment', '12th Payment', 'histcob_c58aed205c0af232', '8101686164', 'Cooch Behar', 'SANJAY BARMAN',
  '2025-04-04', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-04T10:26:00.000Z', '2025-04-04T10:26:00.000Z'
),
(
  'histcob_pay_964669aa41419acd', 'treatment', '13th Payment', '13th Payment', 'histcob_c58aed205c0af232', '8101686164', 'Cooch Behar', 'SANJAY BARMAN',
  '2025-05-02', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-02T10:27:00.000Z', '2025-05-02T10:27:00.000Z'
),
(
  'histcob_pay_ebd3ab146409915a', 'treatment', 'Advance', 'Advance', 'histcob_d9c4439d2a8b653f', '8637532847', 'Cooch Behar', 'PRASANTA SAHA',
  '2025-02-17', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-17T10:15:00.000Z', '2025-02-17T10:15:00.000Z'
),
(
  'histcob_pay_fb7d47d98213affa', 'treatment', '2nd Payment', '2nd Payment', 'histcob_d9c4439d2a8b653f', '8637532847', 'Cooch Behar', 'PRASANTA SAHA',
  '2025-02-24', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-24T10:16:00.000Z', '2025-02-24T10:16:00.000Z'
),
(
  'histcob_pay_14be924e9c32f522', 'treatment', '3rd Payment', '3rd Payment', 'histcob_d9c4439d2a8b653f', '8637532847', 'Cooch Behar', 'PRASANTA SAHA',
  '2025-02-28', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-28T10:17:00.000Z', '2025-02-28T10:17:00.000Z'
),
(
  'histcob_pay_825bd84f9ae7b17d', 'treatment', '4th Payment', '4th Payment', 'histcob_d9c4439d2a8b653f', '8637532847', 'Cooch Behar', 'PRASANTA SAHA',
  '2025-03-03', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-03T10:18:00.000Z', '2025-03-03T10:18:00.000Z'
),
(
  'histcob_pay_3801f58e91bb5b41', 'treatment', '5th Payment', '5th Payment', 'histcob_d9c4439d2a8b653f', '8637532847', 'Cooch Behar', 'PRASANTA SAHA',
  '2025-03-07', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-07T10:19:00.000Z', '2025-03-07T10:19:00.000Z'
),
(
  'histcob_pay_d4d0a70aaa9d2b23', 'treatment', '6th Payment', '6th Payment', 'histcob_d9c4439d2a8b653f', '8637532847', 'Cooch Behar', 'PRASANTA SAHA',
  '2025-03-17', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-17T10:20:00.000Z', '2025-03-17T10:20:00.000Z'
),
(
  'histcob_pay_97993cdbe95db057', 'treatment', '7th Payment', '7th Payment', 'histcob_d9c4439d2a8b653f', '8637532847', 'Cooch Behar', 'PRASANTA SAHA',
  '2025-03-24', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-24T10:21:00.000Z', '2025-03-24T10:21:00.000Z'
),
(
  'histcob_pay_5b84fdee2120655c', 'treatment', '8th Payment', '8th Payment', 'histcob_d9c4439d2a8b653f', '8637532847', 'Cooch Behar', 'PRASANTA SAHA',
  '2025-06-15', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-15T10:22:00.000Z', '2025-06-15T10:22:00.000Z'
),
(
  'histcob_pay_fc44dd3138655f5f', 'treatment', 'Advance', 'Advance', 'histcob_e05061d5b1399048', '6297882139', 'Cooch Behar', 'TAPAS PAUL',
  '2025-02-21', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-21T10:15:00.000Z', '2025-02-21T10:15:00.000Z'
),
(
  'histcob_pay_351086106c225ed9', 'treatment', '2nd Payment', '2nd Payment', 'histcob_e05061d5b1399048', '6297882139', 'Cooch Behar', 'TAPAS PAUL',
  '2025-02-24', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-24T10:16:00.000Z', '2025-02-24T10:16:00.000Z'
),
(
  'histcob_pay_b6e64d14578e3535', 'treatment', '3rd Payment', '3rd Payment', 'histcob_e05061d5b1399048', '6297882139', 'Cooch Behar', 'TAPAS PAUL',
  '2025-02-28', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-28T10:17:00.000Z', '2025-02-28T10:17:00.000Z'
),
(
  'histcob_pay_43b7f69acd3784c7', 'treatment', '4th Payment', '4th Payment', 'histcob_e05061d5b1399048', '6297882139', 'Cooch Behar', 'TAPAS PAUL',
  '2025-03-03', '8500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-03T10:18:00.000Z', '2025-03-03T10:18:00.000Z'
),
(
  'histcob_pay_645dfda0befa2de7', 'treatment', '5th Payment', '5th Payment', 'histcob_e05061d5b1399048', '6297882139', 'Cooch Behar', 'TAPAS PAUL',
  '2025-03-03', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-03T10:19:00.000Z', '2025-03-03T10:19:00.000Z'
),
(
  'histcob_pay_523cafb8a068aa74', 'treatment', 'Advance', 'Advance', 'histcob_a2e08625f9b81318', '6000027050', 'Cooch Behar', 'JAIRUL ISLAM',
  '2025-02-22', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-22T10:15:00.000Z', '2025-02-22T10:15:00.000Z'
),
(
  'histcob_pay_66fa2df5d170cf72', 'treatment', 'Advance', 'Advance', 'histcob_c749d0f312ac3e9b', '7319344840', 'Cooch Behar', 'SOUMEN SARKAR',
  '2025-02-26', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-26T10:15:00.000Z', '2025-02-26T10:15:00.000Z'
),
(
  'histcob_pay_bf72ffc791a961b6', 'treatment', '2nd Payment', '2nd Payment', 'histcob_c749d0f312ac3e9b', '7319344840', 'Cooch Behar', 'SOUMEN SARKAR',
  '2028-02-28', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2028-02-28T10:16:00.000Z', '2028-02-28T10:16:00.000Z'
),
(
  'histcob_pay_600f5f81ee26e1b5', 'treatment', '3rd Payment', '3rd Payment', 'histcob_c749d0f312ac3e9b', '7319344840', 'Cooch Behar', 'SOUMEN SARKAR',
  '2025-03-03', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-03T10:17:00.000Z', '2025-03-03T10:17:00.000Z'
),
(
  'histcob_pay_19dead40ce9e08ea', 'treatment', '4th Payment', '4th Payment', 'histcob_c749d0f312ac3e9b', '7319344840', 'Cooch Behar', 'SOUMEN SARKAR',
  '2025-03-07', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-07T10:18:00.000Z', '2025-03-07T10:18:00.000Z'
),
(
  'histcob_pay_2ca5a834d2f80bf7', 'treatment', '5th Payment', '5th Payment', 'histcob_c749d0f312ac3e9b', '7319344840', 'Cooch Behar', 'SOUMEN SARKAR',
  '2025-03-10', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-10T10:19:00.000Z', '2025-03-10T10:19:00.000Z'
),
(
  'histcob_pay_d0bbaeecfde1d023', 'treatment', '6th Payment', '6th Payment', 'histcob_c749d0f312ac3e9b', '7319344840', 'Cooch Behar', 'SOUMEN SARKAR',
  '2025-03-17', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-17T10:20:00.000Z', '2025-03-17T10:20:00.000Z'
),
(
  'histcob_pay_c0c7cb3bab78af37', 'treatment', '7th Payment', '7th Payment', 'histcob_c749d0f312ac3e9b', '7319344840', 'Cooch Behar', 'SOUMEN SARKAR',
  '2025-03-24', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-24T10:21:00.000Z', '2025-03-24T10:21:00.000Z'
),
(
  'histcob_pay_b7806ff6e2a16366', 'treatment', '8th Payment', '8th Payment', 'histcob_c749d0f312ac3e9b', '7319344840', 'Cooch Behar', 'SOUMEN SARKAR',
  '2025-04-02', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-02T10:22:00.000Z', '2025-04-02T10:22:00.000Z'
),
(
  'histcob_pay_23f96490716d6673', 'treatment', '9th Payment', '9th Payment', 'histcob_c749d0f312ac3e9b', '7319344840', 'Cooch Behar', 'SOUMEN SARKAR',
  '2025-06-06', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-06T10:23:00.000Z', '2025-06-06T10:23:00.000Z'
),
(
  'histcob_pay_5f4bd4897b3d916b', 'treatment', 'Advance', 'Advance', 'histcob_1f616c2d57329ea7', '9749171722', 'Cooch Behar', 'BHARAT ROY',
  '2025-02-28', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-28T10:15:00.000Z', '2025-02-28T10:15:00.000Z'
),
(
  'histcob_pay_265da05d6a9b2dd0', 'treatment', '2nd Payment', '2nd Payment', 'histcob_1f616c2d57329ea7', '9749171722', 'Cooch Behar', 'BHARAT ROY',
  '2025-03-03', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-03T10:16:00.000Z', '2025-03-03T10:16:00.000Z'
),
(
  'histcob_pay_43d9d9c3d732aa58', 'treatment', 'Advance', 'Advance', 'histcob_e635b95314fb8661', '9832147783', 'Cooch Behar', 'ESMITA PARVEEN',
  '2025-02-28', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-28T10:15:00.000Z', '2025-02-28T10:15:00.000Z'
),
(
  'histcob_pay_7a1febf01e970b95', 'treatment', '2nd Payment', '2nd Payment', 'histcob_e635b95314fb8661', '9832147783', 'Cooch Behar', 'ESMITA PARVEEN',
  '2025-03-03', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-03T10:16:00.000Z', '2025-03-03T10:16:00.000Z'
),
(
  'histcob_pay_05dbd8727e5c21fa', 'treatment', '3rd Payment', '3rd Payment', 'histcob_e635b95314fb8661', '9832147783', 'Cooch Behar', 'ESMITA PARVEEN',
  '2025-03-07', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-07T10:17:00.000Z', '2025-03-07T10:17:00.000Z'
),
(
  'histcob_pay_18701f8004e1e3d6', 'treatment', '4th Payment', '4th Payment', 'histcob_e635b95314fb8661', '9832147783', 'Cooch Behar', 'ESMITA PARVEEN',
  '2025-03-10', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-10T10:18:00.000Z', '2025-03-10T10:18:00.000Z'
),
(
  'histcob_pay_1c3e71b5b520f4f8', 'treatment', '5th Payment', '5th Payment', 'histcob_e635b95314fb8661', '9832147783', 'Cooch Behar', 'ESMITA PARVEEN',
  '2025-03-14', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-14T10:19:00.000Z', '2025-03-14T10:19:00.000Z'
),
(
  'histcob_pay_7fe6d9f0ae98bae4', 'treatment', '6th Payment', '6th Payment', 'histcob_e635b95314fb8661', '9832147783', 'Cooch Behar', 'ESMITA PARVEEN',
  '2025-03-17', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-17T10:20:00.000Z', '2025-03-17T10:20:00.000Z'
),
(
  'histcob_pay_478032ec23102bb1', 'treatment', '7th Payment', '7th Payment', 'histcob_e635b95314fb8661', '9832147783', 'Cooch Behar', 'ESMITA PARVEEN',
  '2025-03-21', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-21T10:21:00.000Z', '2025-03-21T10:21:00.000Z'
),
(
  'histcob_pay_5016ae2dc9e591a4', 'treatment', '8th Payment', '8th Payment', 'histcob_e635b95314fb8661', '9832147783', 'Cooch Behar', 'ESMITA PARVEEN',
  '2025-03-24', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-24T10:22:00.000Z', '2025-03-24T10:22:00.000Z'
),
(
  'histcob_pay_850f45bfaee560d4', 'treatment', '9th Payment', '9th Payment', 'histcob_e635b95314fb8661', '9832147783', 'Cooch Behar', 'ESMITA PARVEEN',
  '2025-04-04', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-04T10:23:00.000Z', '2025-04-04T10:23:00.000Z'
),
(
  'histcob_pay_a3583a0bc923cde8', 'treatment', '10th Payment', '10th Payment', 'histcob_e635b95314fb8661', '9832147783', 'Cooch Behar', 'ESMITA PARVEEN',
  '2025-04-07', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-07T10:24:00.000Z', '2025-04-07T10:24:00.000Z'
),
(
  'histcob_pay_83e1284a70f9a5db', 'treatment', '11th Payment', '11th Payment', 'histcob_e635b95314fb8661', '9832147783', 'Cooch Behar', 'ESMITA PARVEEN',
  '2025-04-14', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-14T10:25:00.000Z', '2025-04-14T10:25:00.000Z'
),
(
  'histcob_pay_d736a4a5f804acc8', 'treatment', '12th Payment', '12th Payment', 'histcob_e635b95314fb8661', '9832147783', 'Cooch Behar', 'ESMITA PARVEEN',
  '2025-04-21', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-21T10:26:00.000Z', '2025-04-21T10:26:00.000Z'
),
(
  'histcob_pay_6c9ae1263f95e9f8', 'treatment', '13th Payment', '13th Payment', 'histcob_e635b95314fb8661', '9832147783', 'Cooch Behar', 'ESMITA PARVEEN',
  '2025-05-05', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-05T10:27:00.000Z', '2025-05-05T10:27:00.000Z'
),
(
  'histcob_pay_6335d9b9e96098ce', 'treatment', '14th Payment', '14th Payment', 'histcob_e635b95314fb8661', '9832147783', 'Cooch Behar', 'ESMITA PARVEEN',
  '2025-08-18', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-18T10:28:00.000Z', '2025-08-18T10:28:00.000Z'
),
(
  'histcob_pay_a52d84fa1b59f7f1', 'treatment', 'Advance', 'Advance', 'histcob_aa10b76aad4142d1', '9854454132', 'Cooch Behar', 'NUR MAHAMMAD',
  '2025-03-07', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-07T10:15:00.000Z', '2025-03-07T10:15:00.000Z'
),
(
  'histcob_pay_2a6cf3a50df6835f', 'treatment', '2nd Payment', '2nd Payment', 'histcob_aa10b76aad4142d1', '9854454132', 'Cooch Behar', 'NUR MAHAMMAD',
  '2025-03-14', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-14T10:16:00.000Z', '2025-03-14T10:16:00.000Z'
),
(
  'histcob_pay_b308ac39b510a4c2', 'treatment', '3rd Payment', '3rd Payment', 'histcob_aa10b76aad4142d1', '9854454132', 'Cooch Behar', 'NUR MAHAMMAD',
  '2025-03-17', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-17T10:17:00.000Z', '2025-03-17T10:17:00.000Z'
),
(
  'histcob_pay_aec5959400567964', 'treatment', '4th Payment', '4th Payment', 'histcob_aa10b76aad4142d1', '9854454132', 'Cooch Behar', 'NUR MAHAMMAD',
  '2025-03-21', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-21T10:18:00.000Z', '2025-03-21T10:18:00.000Z'
),
(
  'histcob_pay_4e41f02cb40658df', 'treatment', '5th Payment', '5th Payment', 'histcob_aa10b76aad4142d1', '9854454132', 'Cooch Behar', 'NUR MAHAMMAD',
  '2025-03-24', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-24T10:19:00.000Z', '2025-03-24T10:19:00.000Z'
),
(
  'histcob_pay_eca2b86b577f0035', 'treatment', '6th Payment', '6th Payment', 'histcob_aa10b76aad4142d1', '9854454132', 'Cooch Behar', 'NUR MAHAMMAD',
  '2025-03-28', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-28T10:20:00.000Z', '2025-03-28T10:20:00.000Z'
),
(
  'histcob_pay_c08f552da309f207', 'treatment', '7th Payment', '7th Payment', 'histcob_aa10b76aad4142d1', '9854454132', 'Cooch Behar', 'NUR MAHAMMAD',
  '2025-04-02', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-02T10:21:00.000Z', '2025-04-02T10:21:00.000Z'
),
(
  'histcob_pay_e7ddcf96f83f03cd', 'treatment', '8th Payment', '8th Payment', 'histcob_aa10b76aad4142d1', '9854454132', 'Cooch Behar', 'NUR MAHAMMAD',
  '2025-04-11', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-11T10:22:00.000Z', '2025-04-11T10:22:00.000Z'
),
(
  'histcob_pay_0a2f3cdeb8807ed2', 'treatment', '9th Payment', '9th Payment', 'histcob_aa10b76aad4142d1', '9854454132', 'Cooch Behar', 'NUR MAHAMMAD',
  '2025-04-21', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-21T10:23:00.000Z', '2025-04-21T10:23:00.000Z'
),
(
  'histcob_pay_45e573287dcf201c', 'treatment', '10th Payment', '10th Payment', 'histcob_aa10b76aad4142d1', '9854454132', 'Cooch Behar', 'NUR MAHAMMAD',
  '2025-05-09', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-09T10:24:00.000Z', '2025-05-09T10:24:00.000Z'
),
(
  'histcob_pay_a6cf0966e149c642', 'treatment', 'Advance', 'Advance', 'histcob_9cf8638006b3febb', '9387771476', 'Cooch Behar', 'SABINA KHATUN',
  '2025-03-07', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-07T10:15:00.000Z', '2025-03-07T10:15:00.000Z'
),
(
  'histcob_pay_f25ab772b72732d0', 'treatment', '2nd Payment', '2nd Payment', 'histcob_9cf8638006b3febb', '9387771476', 'Cooch Behar', 'SABINA KHATUN',
  '2025-03-10', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-10T10:16:00.000Z', '2025-03-10T10:16:00.000Z'
),
(
  'histcob_pay_2fe322ca7e5dd051', 'treatment', '3rd Payment', '3rd Payment', 'histcob_9cf8638006b3febb', '9387771476', 'Cooch Behar', 'SABINA KHATUN',
  '2025-03-17', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-17T10:17:00.000Z', '2025-03-17T10:17:00.000Z'
),
(
  'histcob_pay_9410badd4ae21e53', 'treatment', '4th Payment', '4th Payment', 'histcob_9cf8638006b3febb', '9387771476', 'Cooch Behar', 'SABINA KHATUN',
  '2025-03-24', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-24T10:18:00.000Z', '2025-03-24T10:18:00.000Z'
),
(
  'histcob_pay_04e849d908df9bda', 'treatment', '5th Payment', '5th Payment', 'histcob_9cf8638006b3febb', '9387771476', 'Cooch Behar', 'SABINA KHATUN',
  '2025-04-04', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-04T10:19:00.000Z', '2025-04-04T10:19:00.000Z'
),
(
  'histcob_pay_e6cfc8835d571be7', 'treatment', 'Advance', 'Advance', 'histcob_0fff6a18f0ea083e', '8942024490', 'Cooch Behar', 'RAKIBUL ISLAM',
  '2025-03-10', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-10T10:15:00.000Z', '2025-03-10T10:15:00.000Z'
),
(
  'histcob_pay_160bf76866560943', 'treatment', '2nd Payment', '2nd Payment', 'histcob_0fff6a18f0ea083e', '8942024490', 'Cooch Behar', 'RAKIBUL ISLAM',
  '2025-03-17', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-17T10:16:00.000Z', '2025-03-17T10:16:00.000Z'
),
(
  'histcob_pay_d753c1a66322da70', 'treatment', '3rd Payment', '3rd Payment', 'histcob_0fff6a18f0ea083e', '8942024490', 'Cooch Behar', 'RAKIBUL ISLAM',
  '2025-03-21', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-21T10:17:00.000Z', '2025-03-21T10:17:00.000Z'
),
(
  'histcob_pay_3cf84200190946f0', 'treatment', '4th Payment', '4th Payment', 'histcob_0fff6a18f0ea083e', '8942024490', 'Cooch Behar', 'RAKIBUL ISLAM',
  '2025-03-24', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-24T10:18:00.000Z', '2025-03-24T10:18:00.000Z'
),
(
  'histcob_pay_8f2599b9e9abcc07', 'treatment', '5th Payment', '5th Payment', 'histcob_0fff6a18f0ea083e', '8942024490', 'Cooch Behar', 'RAKIBUL ISLAM',
  '2025-03-28', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-28T10:19:00.000Z', '2025-03-28T10:19:00.000Z'
),
(
  'histcob_pay_4fcc4b2050d9c213', 'treatment', '6th Payment', '6th Payment', 'histcob_0fff6a18f0ea083e', '8942024490', 'Cooch Behar', 'RAKIBUL ISLAM',
  '2025-04-04', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-04T10:20:00.000Z', '2025-04-04T10:20:00.000Z'
),
(
  'histcob_pay_71ff5ec7ccd599ed', 'treatment', '7th Payment', '7th Payment', 'histcob_0fff6a18f0ea083e', '8942024490', 'Cooch Behar', 'RAKIBUL ISLAM',
  '2025-04-11', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-11T10:21:00.000Z', '2025-04-11T10:21:00.000Z'
),
(
  'histcob_pay_7c138cd2dceec9fb', 'treatment', '8th Payment', '8th Payment', 'histcob_0fff6a18f0ea083e', '8942024490', 'Cooch Behar', 'RAKIBUL ISLAM',
  '2025-04-18', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-18T10:22:00.000Z', '2025-04-18T10:22:00.000Z'
),
(
  'histcob_pay_97f18583d24711e2', 'treatment', '9th Payment', '9th Payment', 'histcob_0fff6a18f0ea083e', '8942024490', 'Cooch Behar', 'RAKIBUL ISLAM',
  '2025-04-25', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-25T10:23:00.000Z', '2025-04-25T10:23:00.000Z'
),
(
  'histcob_pay_ab5f3d17ac505718', 'treatment', '10th Payment', '10th Payment', 'histcob_0fff6a18f0ea083e', '8942024490', 'Cooch Behar', 'RAKIBUL ISLAM',
  '2025-05-02', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-02T10:24:00.000Z', '2025-05-02T10:24:00.000Z'
),
(
  'histcob_pay_e16150f718b4775c', 'treatment', '11th Payment', '11th Payment', 'histcob_0fff6a18f0ea083e', '8942024490', 'Cooch Behar', 'RAKIBUL ISLAM',
  '2025-05-09', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-09T10:25:00.000Z', '2025-05-09T10:25:00.000Z'
),
(
  'histcob_pay_294a5f960e03f83d', 'treatment', '12th Payment', '12th Payment', 'histcob_0fff6a18f0ea083e', '8942024490', 'Cooch Behar', 'RAKIBUL ISLAM',
  '2025-05-19', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-19T10:26:00.000Z', '2025-05-19T10:26:00.000Z'
),
(
  'histcob_pay_aa01a0983d572917', 'treatment', '13th Payment', '13th Payment', 'histcob_0fff6a18f0ea083e', '8942024490', 'Cooch Behar', 'RAKIBUL ISLAM',
  '2025-06-02', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-02T10:27:00.000Z', '2025-06-02T10:27:00.000Z'
),
(
  'histcob_pay_d5acd1a40a2876f2', 'treatment', 'Advance', 'Advance', 'histcob_911d8a53c915351e', '9382016981', 'Cooch Behar', 'HAREN DAS',
  '2025-03-14', '1100', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-14T10:15:00.000Z', '2025-03-14T10:15:00.000Z'
),
(
  'histcob_pay_5c1b447ec51566e2', 'treatment', '2nd Payment', '2nd Payment', 'histcob_911d8a53c915351e', '9382016981', 'Cooch Behar', 'HAREN DAS',
  '2025-03-17', '800', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-17T10:16:00.000Z', '2025-03-17T10:16:00.000Z'
),
(
  'histcob_pay_78c7079b87d77d6c', 'treatment', '3rd Payment', '3rd Payment', 'histcob_911d8a53c915351e', '9382016981', 'Cooch Behar', 'HAREN DAS',
  '2025-03-21', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-21T10:17:00.000Z', '2025-03-21T10:17:00.000Z'
),
(
  'histcob_pay_cfd77f265be3f58b', 'treatment', '4th Payment', '4th Payment', 'histcob_911d8a53c915351e', '9382016981', 'Cooch Behar', 'HAREN DAS',
  '2025-03-24', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-24T10:18:00.000Z', '2025-03-24T10:18:00.000Z'
),
(
  'histcob_pay_026e2c44bc5f6e22', 'treatment', '5th Payment', '5th Payment', 'histcob_911d8a53c915351e', '9382016981', 'Cooch Behar', 'HAREN DAS',
  '2025-03-28', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-28T10:19:00.000Z', '2025-03-28T10:19:00.000Z'
),
(
  'histcob_pay_2a34c4dc2811e998', 'treatment', '6th Payment', '6th Payment', 'histcob_911d8a53c915351e', '9382016981', 'Cooch Behar', 'HAREN DAS',
  '2025-04-02', '1200', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-02T10:20:00.000Z', '2025-04-02T10:20:00.000Z'
),
(
  'histcob_pay_d9bee96f2ef121de', 'treatment', '7th Payment', '7th Payment', 'histcob_911d8a53c915351e', '9382016981', 'Cooch Behar', 'HAREN DAS',
  '2025-04-11', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-11T10:21:00.000Z', '2025-04-11T10:21:00.000Z'
),
(
  'histcob_pay_7d69bd01c5eed65f', 'treatment', '8th Payment', '8th Payment', 'histcob_911d8a53c915351e', '9382016981', 'Cooch Behar', 'HAREN DAS',
  '2025-04-14', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-14T10:22:00.000Z', '2025-04-14T10:22:00.000Z'
),
(
  'histcob_pay_01beac3066293903', 'treatment', '9th Payment', '9th Payment', 'histcob_911d8a53c915351e', '9382016981', 'Cooch Behar', 'HAREN DAS',
  '2025-04-18', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-18T10:23:00.000Z', '2025-04-18T10:23:00.000Z'
),
(
  'histcob_pay_94f78c54f7344447', 'treatment', '10th Payment', '10th Payment', 'histcob_911d8a53c915351e', '9382016981', 'Cooch Behar', 'HAREN DAS',
  '2025-04-25', '700', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-25T10:24:00.000Z', '2025-04-25T10:24:00.000Z'
),
(
  'histcob_pay_f05a85acfb0b73c0', 'treatment', '11th Payment', '11th Payment', 'histcob_911d8a53c915351e', '9382016981', 'Cooch Behar', 'HAREN DAS',
  '2025-04-28', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-28T10:25:00.000Z', '2025-04-28T10:25:00.000Z'
),
(
  'histcob_pay_f6e1bc0dcf85ad6d', 'treatment', '12th Payment', '12th Payment', 'histcob_911d8a53c915351e', '9382016981', 'Cooch Behar', 'HAREN DAS',
  '2025-05-02', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-02T10:26:00.000Z', '2025-05-02T10:26:00.000Z'
),
(
  'histcob_pay_19645bdbb0ecaa01', 'treatment', '13th Payment', '13th Payment', 'histcob_911d8a53c915351e', '9382016981', 'Cooch Behar', 'HAREN DAS',
  '2025-05-05', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-05T10:27:00.000Z', '2025-05-05T10:27:00.000Z'
),
(
  'histcob_pay_fe859c262d8263e1', 'treatment', '14th Payment', '14th Payment', 'histcob_911d8a53c915351e', '9382016981', 'Cooch Behar', 'HAREN DAS',
  '2025-05-09', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-09T10:28:00.000Z', '2025-05-09T10:28:00.000Z'
),
(
  'histcob_pay_176a770965033483', 'treatment', '15th Payment', '15th Payment', 'histcob_911d8a53c915351e', '9382016981', 'Cooch Behar', 'HAREN DAS',
  '2025-05-30', '7200', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-30T10:29:00.000Z', '2025-05-30T10:29:00.000Z'
),
(
  'histcob_pay_5d4a84f58a9afe6f', 'treatment', 'Advance', 'Advance', 'histcob_81c3477fac6a1da5', '8900136977', 'Cooch Behar', 'UJJAL PAUL',
  '2025-03-31', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-31T10:15:00.000Z', '2025-03-31T10:15:00.000Z'
),
(
  'histcob_pay_b2e16c8b3157505b', 'treatment', '2nd Payment', '2nd Payment', 'histcob_81c3477fac6a1da5', '8900136977', 'Cooch Behar', 'UJJAL PAUL',
  '2025-04-07', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-07T10:16:00.000Z', '2025-04-07T10:16:00.000Z'
),
(
  'histcob_pay_441a2ba2c1f8f875', 'treatment', '3rd Payment', '3rd Payment', 'histcob_81c3477fac6a1da5', '8900136977', 'Cooch Behar', 'UJJAL PAUL',
  '2025-04-11', '7000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-11T10:17:00.000Z', '2025-04-11T10:17:00.000Z'
),
(
  'histcob_pay_5a4b0c7f6a71aaad', 'treatment', '4th Payment', '4th Payment', 'histcob_81c3477fac6a1da5', '8900136977', 'Cooch Behar', 'UJJAL PAUL',
  '2025-04-18', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-18T10:18:00.000Z', '2025-04-18T10:18:00.000Z'
),
(
  'histcob_pay_30fd6d6060cc757d', 'treatment', '5th Payment', '5th Payment', 'histcob_81c3477fac6a1da5', '8900136977', 'Cooch Behar', 'UJJAL PAUL',
  '2025-04-21', '8000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-21T10:19:00.000Z', '2025-04-21T10:19:00.000Z'
),
(
  'histcob_pay_238149d0b53891ba', 'treatment', 'Advance', 'Advance', 'histcob_c799f6566e807b83', '9382191134', 'Cooch Behar', 'BIMALA SHARMA',
  '2025-03-28', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-28T10:15:00.000Z', '2025-03-28T10:15:00.000Z'
),
(
  'histcob_pay_1574fbd0e3077ebb', 'treatment', '2nd Payment', '2nd Payment', 'histcob_c799f6566e807b83', '9382191134', 'Cooch Behar', 'BIMALA SHARMA',
  '2025-03-31', '2600', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-31T10:16:00.000Z', '2025-03-31T10:16:00.000Z'
),
(
  'histcob_pay_4186c61c6e170fe7', 'treatment', '3rd Payment', '3rd Payment', 'histcob_c799f6566e807b83', '9382191134', 'Cooch Behar', 'BIMALA SHARMA',
  '2025-04-04', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-04T10:17:00.000Z', '2025-04-04T10:17:00.000Z'
),
(
  'histcob_pay_9f6c608533d818b5', 'treatment', '4th Payment', '4th Payment', 'histcob_c799f6566e807b83', '9382191134', 'Cooch Behar', 'BIMALA SHARMA',
  '2025-04-11', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-11T10:18:00.000Z', '2025-04-11T10:18:00.000Z'
),
(
  'histcob_pay_19f1ae50204adeae', 'treatment', '5th Payment', '5th Payment', 'histcob_c799f6566e807b83', '9382191134', 'Cooch Behar', 'BIMALA SHARMA',
  '2025-04-18', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-18T10:19:00.000Z', '2025-04-18T10:19:00.000Z'
),
(
  'histcob_pay_61535489ff07e429', 'treatment', '6th Payment', '6th Payment', 'histcob_c799f6566e807b83', '9382191134', 'Cooch Behar', 'BIMALA SHARMA',
  '2025-05-09', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-09T10:20:00.000Z', '2025-05-09T10:20:00.000Z'
),
(
  'histcob_pay_25fed3a088d3ac9e', 'treatment', 'Advance', 'Advance', 'histcob_e797b13bf8165f88', '6296201505', 'Cooch Behar', 'SANJAY BARMAN',
  '2025-03-31', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-31T10:15:00.000Z', '2025-03-31T10:15:00.000Z'
),
(
  'histcob_pay_177197c1f88818b1', 'treatment', '2nd Payment', '2nd Payment', 'histcob_e797b13bf8165f88', '6296201505', 'Cooch Behar', 'SANJAY BARMAN',
  '2025-04-07', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-07T10:16:00.000Z', '2025-04-07T10:16:00.000Z'
),
(
  'histcob_pay_c2748c81aed2ec51', 'treatment', 'Advance', 'Advance', 'histcob_89c6fa39fde9d991', '7892748445', 'Cooch Behar', 'BISWAJIT BARMAN',
  '2025-04-04', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-04T10:15:00.000Z', '2025-04-04T10:15:00.000Z'
),
(
  'histcob_pay_d6a40177969e99cd', 'treatment', '2nd Payment', '2nd Payment', 'histcob_89c6fa39fde9d991', '7892748445', 'Cooch Behar', 'BISWAJIT BARMAN',
  '2025-04-07', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-07T10:16:00.000Z', '2025-04-07T10:16:00.000Z'
),
(
  'histcob_pay_bc290c7d251bed74', 'treatment', '3rd Payment', '3rd Payment', 'histcob_89c6fa39fde9d991', '7892748445', 'Cooch Behar', 'BISWAJIT BARMAN',
  '2025-04-11', '7000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-11T10:17:00.000Z', '2025-04-11T10:17:00.000Z'
),
(
  'histcob_pay_ea63fca10fce16c7', 'treatment', '4th Payment', '4th Payment', 'histcob_89c6fa39fde9d991', '7892748445', 'Cooch Behar', 'BISWAJIT BARMAN',
  '2025-04-21', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-21T10:18:00.000Z', '2025-04-21T10:18:00.000Z'
),
(
  'histcob_pay_071c9c36e8284d60', 'treatment', '5th Payment', '5th Payment', 'histcob_89c6fa39fde9d991', '7892748445', 'Cooch Behar', 'BISWAJIT BARMAN',
  '2025-05-02', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-02T10:19:00.000Z', '2025-05-02T10:19:00.000Z'
),
(
  'histcob_pay_6a834312b82076e6', 'treatment', '6th Payment', '6th Payment', 'histcob_89c6fa39fde9d991', '7892748445', 'Cooch Behar', 'BISWAJIT BARMAN',
  '2025-05-09', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-09T10:20:00.000Z', '2025-05-09T10:20:00.000Z'
),
(
  'histcob_pay_0fe34b2f5e970992', 'treatment', 'Advance', 'Advance', 'histcob_b4f7dc9ea7262ef1', '8101663797', 'Cooch Behar', 'UTPAL MODAK',
  '2025-04-04', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-04T10:15:00.000Z', '2025-04-04T10:15:00.000Z'
),
(
  'histcob_pay_cbe83ba508a2027e', 'treatment', '2nd Payment', '2nd Payment', 'histcob_b4f7dc9ea7262ef1', '8101663797', 'Cooch Behar', 'UTPAL MODAK',
  '2025-04-07', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-07T10:16:00.000Z', '2025-04-07T10:16:00.000Z'
),
(
  'histcob_pay_e45229b129fadd16', 'treatment', '3rd Payment', '3rd Payment', 'histcob_b4f7dc9ea7262ef1', '8101663797', 'Cooch Behar', 'UTPAL MODAK',
  '2025-04-11', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-11T10:17:00.000Z', '2025-04-11T10:17:00.000Z'
),
(
  'histcob_pay_77d52b697c0fe039', 'treatment', '4th Payment', '4th Payment', 'histcob_b4f7dc9ea7262ef1', '8101663797', 'Cooch Behar', 'UTPAL MODAK',
  '2025-04-14', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-14T10:18:00.000Z', '2025-04-14T10:18:00.000Z'
),
(
  'histcob_pay_13edf7e77f20c128', 'treatment', '5th Payment', '5th Payment', 'histcob_b4f7dc9ea7262ef1', '8101663797', 'Cooch Behar', 'UTPAL MODAK',
  '2025-04-18', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-18T10:19:00.000Z', '2025-04-18T10:19:00.000Z'
),
(
  'histcob_pay_cd471bc2e8646f37', 'treatment', '6th Payment', '6th Payment', 'histcob_b4f7dc9ea7262ef1', '8101663797', 'Cooch Behar', 'UTPAL MODAK',
  '2025-04-21', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-21T10:20:00.000Z', '2025-04-21T10:20:00.000Z'
),
(
  'histcob_pay_788f4386eed42cae', 'treatment', '7th Payment', '7th Payment', 'histcob_b4f7dc9ea7262ef1', '8101663797', 'Cooch Behar', 'UTPAL MODAK',
  '2025-04-25', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-25T10:21:00.000Z', '2025-04-25T10:21:00.000Z'
),
(
  'histcob_pay_ed650030b73db7d8', 'treatment', '8th Payment', '8th Payment', 'histcob_b4f7dc9ea7262ef1', '8101663797', 'Cooch Behar', 'UTPAL MODAK',
  '2025-04-28', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-28T10:22:00.000Z', '2025-04-28T10:22:00.000Z'
),
(
  'histcob_pay_a041bba8ea9a83a1', 'treatment', '9th Payment', '9th Payment', 'histcob_b4f7dc9ea7262ef1', '8101663797', 'Cooch Behar', 'UTPAL MODAK',
  '2025-05-02', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-02T10:23:00.000Z', '2025-05-02T10:23:00.000Z'
),
(
  'histcob_pay_14df795386f698e5', 'treatment', '10th Payment', '10th Payment', 'histcob_b4f7dc9ea7262ef1', '8101663797', 'Cooch Behar', 'UTPAL MODAK',
  '2025-05-09', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-09T10:24:00.000Z', '2025-05-09T10:24:00.000Z'
),
(
  'histcob_pay_5063ec8e96b0fa99', 'treatment', '11th Payment', '11th Payment', 'histcob_b4f7dc9ea7262ef1', '8101663797', 'Cooch Behar', 'UTPAL MODAK',
  '2025-05-12', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-12T10:25:00.000Z', '2025-05-12T10:25:00.000Z'
),
(
  'histcob_pay_e314ed131391e469', 'treatment', '12th Payment', '12th Payment', 'histcob_b4f7dc9ea7262ef1', '8101663797', 'Cooch Behar', 'UTPAL MODAK',
  '2025-05-23', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-23T10:26:00.000Z', '2025-05-23T10:26:00.000Z'
),
(
  'histcob_pay_3a8618c7ce993db3', 'treatment', '13th Payment', '13th Payment', 'histcob_b4f7dc9ea7262ef1', '8101663797', 'Cooch Behar', 'UTPAL MODAK',
  '2025-06-02', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-02T10:27:00.000Z', '2025-06-02T10:27:00.000Z'
),
(
  'histcob_pay_9600b161267110de', 'treatment', '14th Payment', '14th Payment', 'histcob_b4f7dc9ea7262ef1', '8101663797', 'Cooch Behar', 'UTPAL MODAK',
  '2025-06-13', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-13T10:28:00.000Z', '2025-06-13T10:28:00.000Z'
),
(
  'histcob_pay_39945b81d4e3bcb0', 'treatment', 'Advance', 'Advance', 'histcob_5b04fd24f306ac08', '7586817093', 'Cooch Behar', 'RATAN SARKAR',
  '2025-04-07', '6500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-07T10:15:00.000Z', '2025-04-07T10:15:00.000Z'
),
(
  'histcob_pay_b84111c2888e7f22', 'treatment', '2nd Payment', '2nd Payment', 'histcob_5b04fd24f306ac08', '7586817093', 'Cooch Behar', 'RATAN SARKAR',
  '2025-04-11', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-11T10:16:00.000Z', '2025-04-11T10:16:00.000Z'
),
(
  'histcob_pay_642d0109d8cc0973', 'treatment', '3rd Payment', '3rd Payment', 'histcob_5b04fd24f306ac08', '7586817093', 'Cooch Behar', 'RATAN SARKAR',
  '2025-04-14', '2500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-14T10:17:00.000Z', '2025-04-14T10:17:00.000Z'
),
(
  'histcob_pay_d19edf718263fb3d', 'treatment', '4th Payment', '4th Payment', 'histcob_5b04fd24f306ac08', '7586817093', 'Cooch Behar', 'RATAN SARKAR',
  '2025-04-21', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-21T10:18:00.000Z', '2025-04-21T10:18:00.000Z'
),
(
  'histcob_pay_23306b77dc068d12', 'treatment', '5th Payment', '5th Payment', 'histcob_5b04fd24f306ac08', '7586817093', 'Cooch Behar', 'RATAN SARKAR',
  '2025-04-25', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-25T10:19:00.000Z', '2025-04-25T10:19:00.000Z'
),
(
  'histcob_pay_6eebacf0bf5c8c7c', 'treatment', '6th Payment', '6th Payment', 'histcob_5b04fd24f306ac08', '7586817093', 'Cooch Behar', 'RATAN SARKAR',
  '2025-05-05', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-05T10:20:00.000Z', '2025-05-05T10:20:00.000Z'
),
(
  'histcob_pay_6b8fc1acc0471406', 'treatment', '7th Payment', '7th Payment', 'histcob_5b04fd24f306ac08', '7586817093', 'Cooch Behar', 'RATAN SARKAR',
  '2025-05-19', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-19T10:21:00.000Z', '2025-05-19T10:21:00.000Z'
),
(
  'histcob_pay_1446843db41d8cb1', 'treatment', '8th Payment', '8th Payment', 'histcob_5b04fd24f306ac08', '7586817093', 'Cooch Behar', 'RATAN SARKAR',
  '2025-06-02', '2500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-02T10:22:00.000Z', '2025-06-02T10:22:00.000Z'
),
(
  'histcob_pay_09fa41d57a6577f5', 'treatment', '9th Payment', '9th Payment', 'histcob_5b04fd24f306ac08', '7586817093', 'Cooch Behar', 'RATAN SARKAR',
  '2025-06-23', '12000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-23T10:23:00.000Z', '2025-06-23T10:23:00.000Z'
),
(
  'histcob_pay_e216d35edae39d10', 'treatment', 'Advance', 'Advance', 'histcob_4c64e7be243f0111', '9749663105', 'Cooch Behar', 'RAJDEEP BARMAN',
  '2025-04-07', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-07T10:15:00.000Z', '2025-04-07T10:15:00.000Z'
),
(
  'histcob_pay_46906c27b47919c9', 'treatment', '2nd Payment', '2nd Payment', 'histcob_4c64e7be243f0111', '9749663105', 'Cooch Behar', 'RAJDEEP BARMAN',
  '2025-04-11', '700', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-11T10:16:00.000Z', '2025-04-11T10:16:00.000Z'
),
(
  'histcob_pay_df09a9bd3d56847e', 'treatment', '3rd Payment', '3rd Payment', 'histcob_4c64e7be243f0111', '9749663105', 'Cooch Behar', 'RAJDEEP BARMAN',
  '2025-04-21', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-21T10:17:00.000Z', '2025-04-21T10:17:00.000Z'
),
(
  'histcob_pay_2ee94984481dca0d', 'treatment', '4th Payment', '4th Payment', 'histcob_4c64e7be243f0111', '9749663105', 'Cooch Behar', 'RAJDEEP BARMAN',
  '2025-05-05', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-05T10:18:00.000Z', '2025-05-05T10:18:00.000Z'
),
(
  'histcob_pay_f2c0b572eb947758', 'treatment', '5th Payment', '5th Payment', 'histcob_4c64e7be243f0111', '9749663105', 'Cooch Behar', 'RAJDEEP BARMAN',
  '2025-05-19', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-19T10:19:00.000Z', '2025-05-19T10:19:00.000Z'
),
(
  'histcob_pay_d1da704a8a3ff390', 'treatment', 'Advance', 'Advance', 'histcob_64302ba618230674', '9907668749', 'Cooch Behar', 'AJIR MIYA',
  '2025-04-07', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-07T10:15:00.000Z', '2025-04-07T10:15:00.000Z'
),
(
  'histcob_pay_88137f2ab0719884', 'treatment', '2nd Payment', '2nd Payment', 'histcob_64302ba618230674', '9907668749', 'Cooch Behar', 'AJIR MIYA',
  '2025-04-11', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-11T10:16:00.000Z', '2025-04-11T10:16:00.000Z'
),
(
  'histcob_pay_b273db68f5cb5a28', 'treatment', '3rd Payment', '3rd Payment', 'histcob_64302ba618230674', '9907668749', 'Cooch Behar', 'AJIR MIYA',
  '2025-04-14', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-14T10:17:00.000Z', '2025-04-14T10:17:00.000Z'
),
(
  'histcob_pay_4df3f800b1e8847a', 'treatment', '4th Payment', '4th Payment', 'histcob_64302ba618230674', '9907668749', 'Cooch Behar', 'AJIR MIYA',
  '2025-04-18', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-18T10:18:00.000Z', '2025-04-18T10:18:00.000Z'
),
(
  'histcob_pay_695e9da9b1e3db80', 'treatment', '5th Payment', '5th Payment', 'histcob_64302ba618230674', '9907668749', 'Cooch Behar', 'AJIR MIYA',
  '2025-04-25', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-25T10:19:00.000Z', '2025-04-25T10:19:00.000Z'
),
(
  'histcob_pay_32d0ae456026d92a', 'treatment', '6th Payment', '6th Payment', 'histcob_64302ba618230674', '9907668749', 'Cooch Behar', 'AJIR MIYA',
  '2025-04-28', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-28T10:20:00.000Z', '2025-04-28T10:20:00.000Z'
),
(
  'histcob_pay_eaaebd299c4f00cb', 'treatment', '7th Payment', '7th Payment', 'histcob_64302ba618230674', '9907668749', 'Cooch Behar', 'AJIR MIYA',
  '2025-05-05', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-05T10:21:00.000Z', '2025-05-05T10:21:00.000Z'
),
(
  'histcob_pay_4d654f6aed099d4d', 'treatment', '8th Payment', '8th Payment', 'histcob_64302ba618230674', '9907668749', 'Cooch Behar', 'AJIR MIYA',
  '2025-05-12', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-12T10:22:00.000Z', '2025-05-12T10:22:00.000Z'
),
(
  'histcob_pay_9bb57ad2db40bb6d', 'treatment', '9th Payment', '9th Payment', 'histcob_64302ba618230674', '9907668749', 'Cooch Behar', 'AJIR MIYA',
  '2025-05-19', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-19T10:23:00.000Z', '2025-05-19T10:23:00.000Z'
),
(
  'histcob_pay_28c70b1a85c34d3a', 'treatment', '10th Payment', '10th Payment', 'histcob_64302ba618230674', '9907668749', 'Cooch Behar', 'AJIR MIYA',
  '2025-05-26', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-26T10:24:00.000Z', '2025-05-26T10:24:00.000Z'
),
(
  'histcob_pay_204f89e1fa927b3c', 'treatment', '11th Payment', '11th Payment', 'histcob_64302ba618230674', '9907668749', 'Cooch Behar', 'AJIR MIYA',
  '2025-06-02', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-02T10:25:00.000Z', '2025-06-02T10:25:00.000Z'
),
(
  'histcob_pay_2d2b072f672db657', 'treatment', '12th Payment', '12th Payment', 'histcob_64302ba618230674', '9907668749', 'Cooch Behar', 'AJIR MIYA',
  '2025-06-09', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-09T10:26:00.000Z', '2025-06-09T10:26:00.000Z'
),
(
  'histcob_pay_c435da2d10d8d6f0', 'treatment', '13th Payment', '13th Payment', 'histcob_64302ba618230674', '9907668749', 'Cooch Behar', 'AJIR MIYA',
  '2025-06-23', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-23T10:27:00.000Z', '2025-06-23T10:27:00.000Z'
),
(
  'histcob_pay_a89ff21e07b00bc5', 'treatment', '14th Payment', '14th Payment', 'histcob_64302ba618230674', '9907668749', 'Cooch Behar', 'AJIR MIYA',
  '2025-07-04', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-04T10:28:00.000Z', '2025-07-04T10:28:00.000Z'
),
(
  'histcob_pay_570b2adb8604a90b', 'treatment', 'Advance', 'Advance', 'histcob_ab3cb548c471e903', '9144244564', 'Cooch Behar', 'SUBRATA DAS',
  '2025-04-11', '900', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-11T10:15:00.000Z', '2025-04-11T10:15:00.000Z'
),
(
  'histcob_pay_aa941cd92f1c84ec', 'treatment', '2nd Payment', '2nd Payment', 'histcob_ab3cb548c471e903', '9144244564', 'Cooch Behar', 'SUBRATA DAS',
  '2025-04-14', '1600', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-14T10:16:00.000Z', '2025-04-14T10:16:00.000Z'
),
(
  'histcob_pay_c4d404846269be1f', 'treatment', '3rd Payment', '3rd Payment', 'histcob_ab3cb548c471e903', '9144244564', 'Cooch Behar', 'SUBRATA DAS',
  '2025-04-18', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-18T10:17:00.000Z', '2025-04-18T10:17:00.000Z'
),
(
  'histcob_pay_aaad1b294892b7dd', 'treatment', '4th Payment', '4th Payment', 'histcob_ab3cb548c471e903', '9144244564', 'Cooch Behar', 'SUBRATA DAS',
  '2025-04-21', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-21T10:18:00.000Z', '2025-04-21T10:18:00.000Z'
),
(
  'histcob_pay_d00741c96f51fbab', 'treatment', '5th Payment', '5th Payment', 'histcob_ab3cb548c471e903', '9144244564', 'Cooch Behar', 'SUBRATA DAS',
  '2025-04-25', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-25T10:19:00.000Z', '2025-04-25T10:19:00.000Z'
),
(
  'histcob_pay_fcaeef58c6f8a209', 'treatment', '6th Payment', '6th Payment', 'histcob_ab3cb548c471e903', '9144244564', 'Cooch Behar', 'SUBRATA DAS',
  '2025-05-02', '6500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-02T10:20:00.000Z', '2025-05-02T10:20:00.000Z'
),
(
  'histcob_pay_1bf8f9e7ed75f037', 'treatment', '7th Payment', '7th Payment', 'histcob_ab3cb548c471e903', '9144244564', 'Cooch Behar', 'SUBRATA DAS',
  '2025-06-06', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-06T10:21:00.000Z', '2025-06-06T10:21:00.000Z'
),
(
  'histcob_pay_00f41329c39a24c1', 'treatment', '8th Payment', '8th Payment', 'histcob_ab3cb548c471e903', '9144244564', 'Cooch Behar', 'SUBRATA DAS',
  '2025-06-13', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-13T10:22:00.000Z', '2025-06-13T10:22:00.000Z'
),
(
  'histcob_pay_1098e761819f7ab8', 'treatment', '9th Payment', '9th Payment', 'histcob_ab3cb548c471e903', '9144244564', 'Cooch Behar', 'SUBRATA DAS',
  '2025-06-23', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-23T10:23:00.000Z', '2025-06-23T10:23:00.000Z'
),
(
  'histcob_pay_e8db37bef6315cff', 'treatment', 'Advance', 'Advance', 'histcob_45f31e18bd977b43', '9775616759', 'Cooch Behar', 'NASHIP THAPA',
  '2025-04-14', '14000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-14T10:15:00.000Z', '2025-04-14T10:15:00.000Z'
),
(
  'histcob_pay_ceac579612c8767f', 'treatment', 'Advance', 'Advance', 'histcob_62c53b71072deeda', '6294125833', 'Cooch Behar', 'UTTAM DEY',
  '2025-04-18', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-18T10:15:00.000Z', '2025-04-18T10:15:00.000Z'
),
(
  'histcob_pay_a98be1f82bb6244f', 'treatment', '2nd Payment', '2nd Payment', 'histcob_62c53b71072deeda', '6294125833', 'Cooch Behar', 'UTTAM DEY',
  '2025-04-21', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-21T10:16:00.000Z', '2025-04-21T10:16:00.000Z'
),
(
  'histcob_pay_177c05613ec813c0', 'treatment', '3rd Payment', '3rd Payment', 'histcob_62c53b71072deeda', '6294125833', 'Cooch Behar', 'UTTAM DEY',
  '2035-05-09', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2035-05-09T10:17:00.000Z', '2035-05-09T10:17:00.000Z'
),
(
  'histcob_pay_73d90a864afc965b', 'treatment', '4th Payment', '4th Payment', 'histcob_62c53b71072deeda', '6294125833', 'Cooch Behar', 'UTTAM DEY',
  '2025-05-16', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-16T10:18:00.000Z', '2025-05-16T10:18:00.000Z'
),
(
  'histcob_pay_6e446ccb29e8aebb', 'treatment', '5th Payment', '5th Payment', 'histcob_62c53b71072deeda', '6294125833', 'Cooch Behar', 'UTTAM DEY',
  '2025-05-23', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-23T10:19:00.000Z', '2025-05-23T10:19:00.000Z'
),
(
  'histcob_pay_9f77d3749250ab60', 'treatment', '6th Payment', '6th Payment', 'histcob_62c53b71072deeda', '6294125833', 'Cooch Behar', 'UTTAM DEY',
  '2025-08-12', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-12T10:20:00.000Z', '2025-08-12T10:20:00.000Z'
),
(
  'histcob_pay_0e0273fc1ffe762a', 'treatment', 'Advance', 'Advance', 'histcob_0dfa4608b639d066', '9799419924', 'Cooch Behar', 'MANGLU MODAK',
  '2025-04-21', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-21T10:15:00.000Z', '2025-04-21T10:15:00.000Z'
),
(
  'histcob_pay_2e18e75ee67a26cd', 'treatment', '2nd Payment', '2nd Payment', 'histcob_0dfa4608b639d066', '9799419924', 'Cooch Behar', 'MANGLU MODAK',
  '2025-04-25', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-25T10:16:00.000Z', '2025-04-25T10:16:00.000Z'
),
(
  'histcob_pay_b8720eb3ccbcb152', 'treatment', '3rd Payment', '3rd Payment', 'histcob_0dfa4608b639d066', '9799419924', 'Cooch Behar', 'MANGLU MODAK',
  '2025-04-28', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-28T10:17:00.000Z', '2025-04-28T10:17:00.000Z'
),
(
  'histcob_pay_f60da77a1aa3eb6e', 'treatment', '4th Payment', '4th Payment', 'histcob_0dfa4608b639d066', '9799419924', 'Cooch Behar', 'MANGLU MODAK',
  '2025-05-02', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-02T10:18:00.000Z', '2025-05-02T10:18:00.000Z'
),
(
  'histcob_pay_521d6ef4579dadd6', 'treatment', '5th Payment', '5th Payment', 'histcob_0dfa4608b639d066', '9799419924', 'Cooch Behar', 'MANGLU MODAK',
  '2025-05-09', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-09T10:19:00.000Z', '2025-05-09T10:19:00.000Z'
),
(
  'histcob_pay_41a2192ad34f3801', 'treatment', '6th Payment', '6th Payment', 'histcob_0dfa4608b639d066', '9799419924', 'Cooch Behar', 'MANGLU MODAK',
  '2025-05-12', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-12T10:20:00.000Z', '2025-05-12T10:20:00.000Z'
),
(
  'histcob_pay_a1453df5963cfe3b', 'treatment', '7th Payment', '7th Payment', 'histcob_0dfa4608b639d066', '9799419924', 'Cooch Behar', 'MANGLU MODAK',
  '2025-05-19', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-19T10:21:00.000Z', '2025-05-19T10:21:00.000Z'
),
(
  'histcob_pay_da1618c904d7ecb3', 'treatment', '8th Payment', '8th Payment', 'histcob_0dfa4608b639d066', '9799419924', 'Cooch Behar', 'MANGLU MODAK',
  '2025-05-26', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-26T10:22:00.000Z', '2025-05-26T10:22:00.000Z'
),
(
  'histcob_pay_43d398017c618abd', 'treatment', '9th Payment', '9th Payment', 'histcob_0dfa4608b639d066', '9799419924', 'Cooch Behar', 'MANGLU MODAK',
  '2025-06-06', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-06T10:23:00.000Z', '2025-06-06T10:23:00.000Z'
),
(
  'histcob_pay_bffccb8af3639ac0', 'treatment', '10th Payment', '10th Payment', 'histcob_0dfa4608b639d066', '9799419924', 'Cooch Behar', 'MANGLU MODAK',
  '2025-07-04', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-04T10:24:00.000Z', '2025-07-04T10:24:00.000Z'
),
(
  'histcob_pay_2e234e575224f54d', 'treatment', '11th Payment', '11th Payment', 'histcob_0dfa4608b639d066', '9799419924', 'Cooch Behar', 'MANGLU MODAK',
  '2025-07-11', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-11T10:25:00.000Z', '2025-07-11T10:25:00.000Z'
),
(
  'histcob_pay_65f07e76dee617e6', 'treatment', '12th Payment', '12th Payment', 'histcob_0dfa4608b639d066', '9799419924', 'Cooch Behar', 'MANGLU MODAK',
  '2025-07-14', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-14T10:26:00.000Z', '2025-07-14T10:26:00.000Z'
),
(
  'histcob_pay_2f3d2c1a930c19c2', 'treatment', '13th Payment', '13th Payment', 'histcob_0dfa4608b639d066', '9799419924', 'Cooch Behar', 'MANGLU MODAK',
  '2025-07-18', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-18T10:27:00.000Z', '2025-07-18T10:27:00.000Z'
),
(
  'histcob_pay_b4bb0a588d848a53', 'treatment', '14th Payment', '14th Payment', 'histcob_0dfa4608b639d066', '9799419924', 'Cooch Behar', 'MANGLU MODAK',
  '2025-07-28', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-28T10:28:00.000Z', '2025-07-28T10:28:00.000Z'
),
(
  'histcob_pay_25a6b1ab4005742a', 'treatment', '15th Payment', '15th Payment', 'histcob_0dfa4608b639d066', '9799419924', 'Cooch Behar', 'MANGLU MODAK',
  '2025-08-08', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-08T10:29:00.000Z', '2025-08-08T10:29:00.000Z'
),
(
  'histcob_pay_db85cf3b50f88aa2', 'treatment', 'Advance', 'Advance', 'histcob_134f9320b9f68562', '8172023975', 'Cooch Behar', 'BASANTI ROY',
  '2025-04-21', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-21T10:15:00.000Z', '2025-04-21T10:15:00.000Z'
),
(
  'histcob_pay_0137fdf149b76d3e', 'treatment', '2nd Payment', '2nd Payment', 'histcob_134f9320b9f68562', '8172023975', 'Cooch Behar', 'BASANTI ROY',
  '2025-04-25', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-25T10:16:00.000Z', '2025-04-25T10:16:00.000Z'
),
(
  'histcob_pay_4fa5a864dc64bccc', 'treatment', '3rd Payment', '3rd Payment', 'histcob_134f9320b9f68562', '8172023975', 'Cooch Behar', 'BASANTI ROY',
  '2025-04-28', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-28T10:17:00.000Z', '2025-04-28T10:17:00.000Z'
),
(
  'histcob_pay_1e1485ad81c979cf', 'treatment', '4th Payment', '4th Payment', 'histcob_134f9320b9f68562', '8172023975', 'Cooch Behar', 'BASANTI ROY',
  '2025-05-02', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-02T10:18:00.000Z', '2025-05-02T10:18:00.000Z'
),
(
  'histcob_pay_90b82b49d8794fde', 'treatment', '5th Payment', '5th Payment', 'histcob_134f9320b9f68562', '8172023975', 'Cooch Behar', 'BASANTI ROY',
  '2025-05-05', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-05T10:19:00.000Z', '2025-05-05T10:19:00.000Z'
),
(
  'histcob_pay_f02d5069a4f05e58', 'treatment', '6th Payment', '6th Payment', 'histcob_134f9320b9f68562', '8172023975', 'Cooch Behar', 'BASANTI ROY',
  '2025-05-09', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-09T10:20:00.000Z', '2025-05-09T10:20:00.000Z'
),
(
  'histcob_pay_740b8b01cd7cffda', 'treatment', '7th Payment', '7th Payment', 'histcob_134f9320b9f68562', '8172023975', 'Cooch Behar', 'BASANTI ROY',
  '2015-05-12', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2015-05-12T10:21:00.000Z', '2015-05-12T10:21:00.000Z'
),
(
  'histcob_pay_f333f6195249a25f', 'treatment', '8th Payment', '8th Payment', 'histcob_134f9320b9f68562', '8172023975', 'Cooch Behar', 'BASANTI ROY',
  '2025-05-16', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-16T10:22:00.000Z', '2025-05-16T10:22:00.000Z'
),
(
  'histcob_pay_ed75d71f86313437', 'treatment', '9th Payment', '9th Payment', 'histcob_134f9320b9f68562', '8172023975', 'Cooch Behar', 'BASANTI ROY',
  '2025-05-19', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-19T10:23:00.000Z', '2025-05-19T10:23:00.000Z'
),
(
  'histcob_pay_4dced4c6299e210b', 'treatment', '10th Payment', '10th Payment', 'histcob_134f9320b9f68562', '8172023975', 'Cooch Behar', 'BASANTI ROY',
  '2025-05-23', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-23T10:24:00.000Z', '2025-05-23T10:24:00.000Z'
),
(
  'histcob_pay_d9fe91b885e27823', 'treatment', '11th Payment', '11th Payment', 'histcob_134f9320b9f68562', '8172023975', 'Cooch Behar', 'BASANTI ROY',
  '2025-05-26', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-26T10:25:00.000Z', '2025-05-26T10:25:00.000Z'
),
(
  'histcob_pay_7098e72a0f7e3298', 'treatment', '12th Payment', '12th Payment', 'histcob_134f9320b9f68562', '8172023975', 'Cooch Behar', 'BASANTI ROY',
  '2025-06-16', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-16T10:26:00.000Z', '2025-06-16T10:26:00.000Z'
),
(
  'histcob_pay_f895e37e24ba1cd3', 'treatment', '13th Payment', '13th Payment', 'histcob_134f9320b9f68562', '8172023975', 'Cooch Behar', 'BASANTI ROY',
  '2025-06-09', '5500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-09T10:27:00.000Z', '2025-06-09T10:27:00.000Z'
),
(
  'histcob_pay_dbc80c459f8c97f8', 'treatment', '14th Payment', '14th Payment', 'histcob_134f9320b9f68562', '8172023975', 'Cooch Behar', 'BASANTI ROY',
  '2025-06-23', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-23T10:28:00.000Z', '2025-06-23T10:28:00.000Z'
),
(
  'histcob_pay_8f3c7f2106e5ee53', 'treatment', '15th Payment', '15th Payment', 'histcob_134f9320b9f68562', '8172023975', 'Cooch Behar', 'BASANTI ROY',
  '2025-06-30', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-30T10:29:00.000Z', '2025-06-30T10:29:00.000Z'
),
(
  'histcob_pay_8255fcee48198fd8', 'treatment', 'Advance', 'Advance', 'histcob_bf4af50c6fba0028', '9954216046', 'Cooch Behar', 'ANOWAR HOSSAIN',
  '2025-04-25', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-25T10:15:00.000Z', '2025-04-25T10:15:00.000Z'
),
(
  'histcob_pay_cca7b2c90edeea91', 'treatment', '2nd Payment', '2nd Payment', 'histcob_bf4af50c6fba0028', '9954216046', 'Cooch Behar', 'ANOWAR HOSSAIN',
  '2025-05-02', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-02T10:16:00.000Z', '2025-05-02T10:16:00.000Z'
),
(
  'histcob_pay_69f3fa5093cda66d', 'treatment', 'Advance', 'Advance', 'histcob_43a2b00aba4054b4', '9851509380', 'Cooch Behar', 'SHANKAR DAS',
  '2025-05-12', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-12T10:15:00.000Z', '2025-05-12T10:15:00.000Z'
),
(
  'histcob_pay_e11443fd85436c4d', 'treatment', '2nd Payment', '2nd Payment', 'histcob_43a2b00aba4054b4', '9851509380', 'Cooch Behar', 'SHANKAR DAS',
  '2025-05-16', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-16T10:16:00.000Z', '2025-05-16T10:16:00.000Z'
),
(
  'histcob_pay_3b3fb1abd80a470b', 'treatment', '3rd Payment', '3rd Payment', 'histcob_43a2b00aba4054b4', '9851509380', 'Cooch Behar', 'SHANKAR DAS',
  '2025-05-26', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-26T10:17:00.000Z', '2025-05-26T10:17:00.000Z'
),
(
  'histcob_pay_de426638cdb54f1d', 'treatment', '4th Payment', '4th Payment', 'histcob_43a2b00aba4054b4', '9851509380', 'Cooch Behar', 'SHANKAR DAS',
  '2025-05-19', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-19T10:18:00.000Z', '2025-05-19T10:18:00.000Z'
),
(
  'histcob_pay_f4ed742383eebbf7', 'treatment', '5th Payment', '5th Payment', 'histcob_43a2b00aba4054b4', '9851509380', 'Cooch Behar', 'SHANKAR DAS',
  '2025-06-02', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-02T10:19:00.000Z', '2025-06-02T10:19:00.000Z'
),
(
  'histcob_pay_547b810b1a2be9ab', 'treatment', '6th Payment', '6th Payment', 'histcob_43a2b00aba4054b4', '9851509380', 'Cooch Behar', 'SHANKAR DAS',
  '2025-06-13', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-13T10:20:00.000Z', '2025-06-13T10:20:00.000Z'
),
(
  'histcob_pay_0ea40f4c1f235d41', 'treatment', '7th Payment', '7th Payment', 'histcob_43a2b00aba4054b4', '9851509380', 'Cooch Behar', 'SHANKAR DAS',
  '2025-06-06', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-06T10:21:00.000Z', '2025-06-06T10:21:00.000Z'
),
(
  'histcob_pay_69761a3170a67d84', 'treatment', '8th Payment', '8th Payment', 'histcob_43a2b00aba4054b4', '9851509380', 'Cooch Behar', 'SHANKAR DAS',
  '2025-06-20', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-20T10:22:00.000Z', '2025-06-20T10:22:00.000Z'
),
(
  'histcob_pay_9c7f1a50006016a1', 'treatment', '9th Payment', '9th Payment', 'histcob_43a2b00aba4054b4', '9851509380', 'Cooch Behar', 'SHANKAR DAS',
  '2025-06-27', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-27T10:23:00.000Z', '2025-06-27T10:23:00.000Z'
),
(
  'histcob_pay_6c3219cd9bec66aa', 'treatment', '10th Payment', '10th Payment', 'histcob_43a2b00aba4054b4', '9851509380', 'Cooch Behar', 'SHANKAR DAS',
  '2025-07-04', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-04T10:24:00.000Z', '2025-07-04T10:24:00.000Z'
),
(
  'histcob_pay_12f37ae6756b6f35', 'treatment', '11th Payment', '11th Payment', 'histcob_43a2b00aba4054b4', '9851509380', 'Cooch Behar', 'SHANKAR DAS',
  '2025-07-11', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-11T10:25:00.000Z', '2025-07-11T10:25:00.000Z'
),
(
  'histcob_pay_5314af0f6e800782', 'treatment', '12th Payment', '12th Payment', 'histcob_43a2b00aba4054b4', '9851509380', 'Cooch Behar', 'SHANKAR DAS',
  '2025-08-11', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-11T10:26:00.000Z', '2025-08-11T10:26:00.000Z'
),
(
  'histcob_pay_8fe87edbd572321f', 'treatment', '13th Payment', '13th Payment', 'histcob_43a2b00aba4054b4', '9851509380', 'Cooch Behar', 'SHANKAR DAS',
  '2025-09-08', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-08T10:27:00.000Z', '2025-09-08T10:27:00.000Z'
),
(
  'histcob_pay_e7ebfad593ea7a48', 'treatment', '14th Payment', '14th Payment', 'histcob_43a2b00aba4054b4', '9851509380', 'Cooch Behar', 'SHANKAR DAS',
  '2025-10-31', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-31T10:28:00.000Z', '2025-10-31T10:28:00.000Z'
),
(
  'histcob_pay_72ba201620c1840d', 'treatment', '15th Payment', '15th Payment', 'histcob_43a2b00aba4054b4', '9851509380', 'Cooch Behar', 'SHANKAR DAS',
  '2025-11-03', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-03T10:29:00.000Z', '2025-11-03T10:29:00.000Z'
),
(
  'histcob_pay_f5bc8dc8cd765945', 'treatment', 'Advance', 'Advance', 'histcob_70df6f52b00d623f', '8016093930', 'Cooch Behar', 'PRASANJIT BARMAN',
  '2025-04-28', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-28T10:15:00.000Z', '2025-04-28T10:15:00.000Z'
),
(
  'histcob_pay_0a490ac84b5cc3d3', 'treatment', '2nd Payment', '2nd Payment', 'histcob_70df6f52b00d623f', '8016093930', 'Cooch Behar', 'PRASANJIT BARMAN',
  '2025-05-02', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-02T10:16:00.000Z', '2025-05-02T10:16:00.000Z'
),
(
  'histcob_pay_a44d8d4617c30c92', 'treatment', '3rd Payment', '3rd Payment', 'histcob_70df6f52b00d623f', '8016093930', 'Cooch Behar', 'PRASANJIT BARMAN',
  '2025-05-09', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-09T10:17:00.000Z', '2025-05-09T10:17:00.000Z'
),
(
  'histcob_pay_4fe2a48f214a5107', 'treatment', '4th Payment', '4th Payment', 'histcob_70df6f52b00d623f', '8016093930', 'Cooch Behar', 'PRASANJIT BARMAN',
  '2025-05-16', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-16T10:18:00.000Z', '2025-05-16T10:18:00.000Z'
),
(
  'histcob_pay_709a7ae180008ec3', 'treatment', '5th Payment', '5th Payment', 'histcob_70df6f52b00d623f', '8016093930', 'Cooch Behar', 'PRASANJIT BARMAN',
  '2025-05-23', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-23T10:19:00.000Z', '2025-05-23T10:19:00.000Z'
),
(
  'histcob_pay_8ef8467059233f59', 'treatment', '6th Payment', '6th Payment', 'histcob_70df6f52b00d623f', '8016093930', 'Cooch Behar', 'PRASANJIT BARMAN',
  '2025-05-30', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-30T10:20:00.000Z', '2025-05-30T10:20:00.000Z'
),
(
  'histcob_pay_7fcde4c8b2416dd3', 'treatment', '7th Payment', '7th Payment', 'histcob_70df6f52b00d623f', '8016093930', 'Cooch Behar', 'PRASANJIT BARMAN',
  '2025-06-06', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-06T10:21:00.000Z', '2025-06-06T10:21:00.000Z'
),
(
  'histcob_pay_a901f5d7b06a6dbd', 'treatment', '8th Payment', '8th Payment', 'histcob_70df6f52b00d623f', '8016093930', 'Cooch Behar', 'PRASANJIT BARMAN',
  '2025-06-16', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-16T10:22:00.000Z', '2025-06-16T10:22:00.000Z'
),
(
  'histcob_pay_421ac50adb067853', 'treatment', '9th Payment', '9th Payment', 'histcob_70df6f52b00d623f', '8016093930', 'Cooch Behar', 'PRASANJIT BARMAN',
  '2025-06-27', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-27T10:23:00.000Z', '2025-06-27T10:23:00.000Z'
),
(
  'histcob_pay_78fe58807e5a4ce7', 'treatment', '10th Payment', '10th Payment', 'histcob_70df6f52b00d623f', '8016093930', 'Cooch Behar', 'PRASANJIT BARMAN',
  '2025-07-04', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-04T10:24:00.000Z', '2025-07-04T10:24:00.000Z'
),
(
  'histcob_pay_7eafe9e19e69ed24', 'treatment', 'Advance', 'Advance', 'histcob_9a585a9d836191eb', '9851238886', 'Cooch Behar', 'ANOYARA BIBI',
  '2025-04-28', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-28T10:15:00.000Z', '2025-04-28T10:15:00.000Z'
),
(
  'histcob_pay_3127b939b1d32ed7', 'treatment', '2nd Payment', '2nd Payment', 'histcob_9a585a9d836191eb', '9851238886', 'Cooch Behar', 'ANOYARA BIBI',
  '2025-05-02', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-02T10:16:00.000Z', '2025-05-02T10:16:00.000Z'
),
(
  'histcob_pay_67df8d95a769e892', 'treatment', '3rd Payment', '3rd Payment', 'histcob_9a585a9d836191eb', '9851238886', 'Cooch Behar', 'ANOYARA BIBI',
  '2025-12-08', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-08T10:17:00.000Z', '2025-12-08T10:17:00.000Z'
),
(
  'histcob_pay_d33dc843083ae80c', 'treatment', '4th Payment', '4th Payment', 'histcob_9a585a9d836191eb', '9851238886', 'Cooch Behar', 'ANOYARA BIBI',
  '2025-12-12', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-12T10:18:00.000Z', '2025-12-12T10:18:00.000Z'
),
(
  'histcob_pay_7b2031e3855ed68c', 'treatment', '5th Payment', '5th Payment', 'histcob_9a585a9d836191eb', '9851238886', 'Cooch Behar', 'ANOYARA BIBI',
  '2025-12-15', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-15T10:19:00.000Z', '2025-12-15T10:19:00.000Z'
),
(
  'histcob_pay_e1b0648d089db5a3', 'treatment', '6th Payment', '6th Payment', 'histcob_9a585a9d836191eb', '9851238886', 'Cooch Behar', 'ANOYARA BIBI',
  '2025-12-19', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-19T10:20:00.000Z', '2025-12-19T10:20:00.000Z'
),
(
  'histcob_pay_e756b99b9372fa8a', 'treatment', '7th Payment', '7th Payment', 'histcob_9a585a9d836191eb', '9851238886', 'Cooch Behar', 'ANOYARA BIBI',
  '2025-12-22', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-22T10:21:00.000Z', '2025-12-22T10:21:00.000Z'
),
(
  'histcob_pay_708870ee3783d768', 'treatment', '8th Payment', '8th Payment', 'histcob_9a585a9d836191eb', '9851238886', 'Cooch Behar', 'ANOYARA BIBI',
  '2025-12-29', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-29T10:22:00.000Z', '2025-12-29T10:22:00.000Z'
),
(
  'histcob_pay_f3c467b5cf3c703f', 'treatment', '9th Payment', '9th Payment', 'histcob_9a585a9d836191eb', '9851238886', 'Cooch Behar', 'ANOYARA BIBI',
  '2026-01-06', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-06T10:23:00.000Z', '2026-01-06T10:23:00.000Z'
),
(
  'histcob_pay_e1f8ce658c9083d4', 'treatment', '10th Payment', '10th Payment', 'histcob_9a585a9d836191eb', '9851238886', 'Cooch Behar', 'ANOYARA BIBI',
  '2026-01-12', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-12T10:24:00.000Z', '2026-01-12T10:24:00.000Z'
),
(
  'histcob_pay_e206d972a45ec967', 'treatment', '11th Payment', '11th Payment', 'histcob_9a585a9d836191eb', '9851238886', 'Cooch Behar', 'ANOYARA BIBI',
  '2026-01-23', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-23T10:25:00.000Z', '2026-01-23T10:25:00.000Z'
),
(
  'histcob_pay_8580ac2914789986', 'treatment', '12th Payment', '12th Payment', 'histcob_9a585a9d836191eb', '9851238886', 'Cooch Behar', 'ANOYARA BIBI',
  '2026-02-02', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-02T10:26:00.000Z', '2026-02-02T10:26:00.000Z'
),
(
  'histcob_pay_c89b188d8d105a99', 'treatment', '13th Payment', '13th Payment', 'histcob_9a585a9d836191eb', '9851238886', 'Cooch Behar', 'ANOYARA BIBI',
  '2026-02-09', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-09T10:27:00.000Z', '2026-02-09T10:27:00.000Z'
),
(
  'histcob_pay_b455ce0ce77486dd', 'treatment', 'Advance', 'Advance', 'histcob_d3c33478363844c2', '6003768668', 'Cooch Behar', 'ABDUL KARIM',
  '2025-05-02', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-02T10:15:00.000Z', '2025-05-02T10:15:00.000Z'
),
(
  'histcob_pay_fb69edcea40a7168', 'treatment', 'Advance', 'Advance', 'histcob_683d2e48edd0125e', '6295725190', 'Cooch Behar', 'MANIK BARMAN',
  '2025-05-02', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-02T10:15:00.000Z', '2025-05-02T10:15:00.000Z'
),
(
  'histcob_pay_dd9bfbe41724646a', 'treatment', '2nd Payment', '2nd Payment', 'histcob_683d2e48edd0125e', '6295725190', 'Cooch Behar', 'MANIK BARMAN',
  '2025-05-23', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-23T10:16:00.000Z', '2025-05-23T10:16:00.000Z'
),
(
  'histcob_pay_5ee40bf5b8a6171d', 'treatment', 'Advance', 'Advance', 'histcob_caef3134acb07c4b', '8670753016', 'Cooch Behar', 'MALIK MIA',
  '2025-05-05', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-05T10:15:00.000Z', '2025-05-05T10:15:00.000Z'
),
(
  'histcob_pay_f118eaff1d678c9c', 'treatment', '2nd Payment', '2nd Payment', 'histcob_caef3134acb07c4b', '8670753016', 'Cooch Behar', 'MALIK MIA',
  '2025-05-09', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-09T10:16:00.000Z', '2025-05-09T10:16:00.000Z'
),
(
  'histcob_pay_2be10ce38207a9da', 'treatment', '3rd Payment', '3rd Payment', 'histcob_caef3134acb07c4b', '8670753016', 'Cooch Behar', 'MALIK MIA',
  '2025-05-16', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-16T10:17:00.000Z', '2025-05-16T10:17:00.000Z'
),
(
  'histcob_pay_9a5f53c879ef27c0', 'treatment', '4th Payment', '4th Payment', 'histcob_caef3134acb07c4b', '8670753016', 'Cooch Behar', 'MALIK MIA',
  '2025-06-13', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-13T10:18:00.000Z', '2025-06-13T10:18:00.000Z'
),
(
  'histcob_pay_201d795cc2f55b11', 'treatment', '5th Payment', '5th Payment', 'histcob_caef3134acb07c4b', '8670753016', 'Cooch Behar', 'MALIK MIA',
  '2025-06-23', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-23T10:19:00.000Z', '2025-06-23T10:19:00.000Z'
),
(
  'histcob_pay_da4efdf73e8d6091', 'treatment', '6th Payment', '6th Payment', 'histcob_caef3134acb07c4b', '8670753016', 'Cooch Behar', 'MALIK MIA',
  '2025-05-23', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-23T10:20:00.000Z', '2025-05-23T10:20:00.000Z'
),
(
  'histcob_pay_2dbff6d65d613f39', 'treatment', '7th Payment', '7th Payment', 'histcob_caef3134acb07c4b', '8670753016', 'Cooch Behar', 'MALIK MIA',
  '2025-07-11', '2500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-11T10:21:00.000Z', '2025-07-11T10:21:00.000Z'
),
(
  'histcob_pay_86a89f73766267c7', 'treatment', '8th Payment', '8th Payment', 'histcob_caef3134acb07c4b', '8670753016', 'Cooch Behar', 'MALIK MIA',
  '2025-08-01', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-01T10:22:00.000Z', '2025-08-01T10:22:00.000Z'
),
(
  'histcob_pay_b913e4cdf8224056', 'treatment', '9th Payment', '9th Payment', 'histcob_caef3134acb07c4b', '8670753016', 'Cooch Behar', 'MALIK MIA',
  '2025-10-06', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-06T10:23:00.000Z', '2025-10-06T10:23:00.000Z'
),
(
  'histcob_pay_90e0fa5035554567', 'treatment', '10th Payment', '10th Payment', 'histcob_caef3134acb07c4b', '8670753016', 'Cooch Behar', 'MALIK MIA',
  '2025-10-10', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-10T10:24:00.000Z', '2025-10-10T10:24:00.000Z'
),
(
  'histcob_pay_999ba183a46bd6bf', 'treatment', '11th Payment', '11th Payment', 'histcob_caef3134acb07c4b', '8670753016', 'Cooch Behar', 'MALIK MIA',
  '2025-10-17', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-17T10:25:00.000Z', '2025-10-17T10:25:00.000Z'
),
(
  'histcob_pay_843e6175f32f18a3', 'treatment', '12th Payment', '12th Payment', 'histcob_caef3134acb07c4b', '8670753016', 'Cooch Behar', 'MALIK MIA',
  '2025-10-20', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-20T10:26:00.000Z', '2025-10-20T10:26:00.000Z'
),
(
  'histcob_pay_023647670059c430', 'treatment', '13th Payment', '13th Payment', 'histcob_caef3134acb07c4b', '8670753016', 'Cooch Behar', 'MALIK MIA',
  '2025-10-27', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-27T10:27:00.000Z', '2025-10-27T10:27:00.000Z'
),
(
  'histcob_pay_1a866d1ce5a87a30', 'treatment', '14th Payment', '14th Payment', 'histcob_caef3134acb07c4b', '8670753016', 'Cooch Behar', 'MALIK MIA',
  '2025-11-03', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-03T10:28:00.000Z', '2025-11-03T10:28:00.000Z'
),
(
  'histcob_pay_c2eb1b7a5e4ebcfc', 'treatment', '15th Payment', '15th Payment', 'histcob_caef3134acb07c4b', '8670753016', 'Cooch Behar', 'MALIK MIA',
  '2025-11-10', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-10T10:29:00.000Z', '2025-11-10T10:29:00.000Z'
),
(
  'histcob_pay_8bda3ce48ae276dc', 'treatment', 'Advance', 'Advance', 'histcob_aea95478e4914235', '9851443121', 'Cooch Behar', 'SONEKA ROY',
  '2025-06-02', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-02T10:15:00.000Z', '2025-06-02T10:15:00.000Z'
),
(
  'histcob_pay_c93bc8870573a0a5', 'treatment', 'Advance', 'Advance', 'histcob_8a35824d93529e1e', '7477832695', 'Cooch Behar', 'UJJAL DEY SARKAR',
  '2025-05-09', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-09T10:15:00.000Z', '2025-05-09T10:15:00.000Z'
),
(
  'histcob_pay_467409b0c5a5af68', 'treatment', '2nd Payment', '2nd Payment', 'histcob_8a35824d93529e1e', '7477832695', 'Cooch Behar', 'UJJAL DEY SARKAR',
  '2025-05-12', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-12T10:16:00.000Z', '2025-05-12T10:16:00.000Z'
),
(
  'histcob_pay_30d40e699036ba89', 'treatment', '3rd Payment', '3rd Payment', 'histcob_8a35824d93529e1e', '7477832695', 'Cooch Behar', 'UJJAL DEY SARKAR',
  '2025-05-18', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-18T10:17:00.000Z', '2025-05-18T10:17:00.000Z'
),
(
  'histcob_pay_f73264888bce70e8', 'treatment', '4th Payment', '4th Payment', 'histcob_8a35824d93529e1e', '7477832695', 'Cooch Behar', 'UJJAL DEY SARKAR',
  '2025-05-19', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-19T10:18:00.000Z', '2025-05-19T10:18:00.000Z'
),
(
  'histcob_pay_fa4a32eaf55e41a5', 'treatment', '5th Payment', '5th Payment', 'histcob_8a35824d93529e1e', '7477832695', 'Cooch Behar', 'UJJAL DEY SARKAR',
  '2025-05-23', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-23T10:19:00.000Z', '2025-05-23T10:19:00.000Z'
),
(
  'histcob_pay_ad869b52b74ed2ce', 'treatment', '6th Payment', '6th Payment', 'histcob_8a35824d93529e1e', '7477832695', 'Cooch Behar', 'UJJAL DEY SARKAR',
  '2025-05-26', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-26T10:20:00.000Z', '2025-05-26T10:20:00.000Z'
),
(
  'histcob_pay_3d3ba37a32eaad19', 'treatment', '7th Payment', '7th Payment', 'histcob_8a35824d93529e1e', '7477832695', 'Cooch Behar', 'UJJAL DEY SARKAR',
  '2025-06-02', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-02T10:21:00.000Z', '2025-06-02T10:21:00.000Z'
),
(
  'histcob_pay_a4542a558271f4af', 'treatment', '8th Payment', '8th Payment', 'histcob_8a35824d93529e1e', '7477832695', 'Cooch Behar', 'UJJAL DEY SARKAR',
  '2025-06-09', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-09T10:22:00.000Z', '2025-06-09T10:22:00.000Z'
),
(
  'histcob_pay_368f465c3511493a', 'treatment', '9th Payment', '9th Payment', 'histcob_8a35824d93529e1e', '7477832695', 'Cooch Behar', 'UJJAL DEY SARKAR',
  '2025-06-16', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-16T10:23:00.000Z', '2025-06-16T10:23:00.000Z'
),
(
  'histcob_pay_9d1c528727589e5d', 'treatment', '10th Payment', '10th Payment', 'histcob_8a35824d93529e1e', '7477832695', 'Cooch Behar', 'UJJAL DEY SARKAR',
  '2025-06-23', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-23T10:24:00.000Z', '2025-06-23T10:24:00.000Z'
),
(
  'histcob_pay_00885eb3448a0baf', 'treatment', '11th Payment', '11th Payment', 'histcob_8a35824d93529e1e', '7477832695', 'Cooch Behar', 'UJJAL DEY SARKAR',
  '2025-06-30', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-30T10:25:00.000Z', '2025-06-30T10:25:00.000Z'
),
(
  'histcob_pay_a0c19878c36e1969', 'treatment', '12th Payment', '12th Payment', 'histcob_8a35824d93529e1e', '7477832695', 'Cooch Behar', 'UJJAL DEY SARKAR',
  '2025-07-07', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-07T10:26:00.000Z', '2025-07-07T10:26:00.000Z'
),
(
  'histcob_pay_819ef1a9295fba28', 'treatment', '13th Payment', '13th Payment', 'histcob_8a35824d93529e1e', '7477832695', 'Cooch Behar', 'UJJAL DEY SARKAR',
  '2025-07-21', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-21T10:27:00.000Z', '2025-07-21T10:27:00.000Z'
),
(
  'histcob_pay_c42cc94586581744', 'treatment', '14th Payment', '14th Payment', 'histcob_8a35824d93529e1e', '7477832695', 'Cooch Behar', 'UJJAL DEY SARKAR',
  '2025-07-28', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-28T10:28:00.000Z', '2025-07-28T10:28:00.000Z'
),
(
  'histcob_pay_b33d3ca980027150', 'treatment', '15th Payment', '15th Payment', 'histcob_8a35824d93529e1e', '7477832695', 'Cooch Behar', 'UJJAL DEY SARKAR',
  '2025-08-11', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-11T10:29:00.000Z', '2025-08-11T10:29:00.000Z'
),
(
  'histcob_pay_9d284da297be20cf', 'treatment', 'Advance', 'Advance', 'histcob_139fe83fc83835c9', '7602384437', 'Cooch Behar', 'SATYAJIT SARKAR',
  '2025-05-16', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-16T10:15:00.000Z', '2025-05-16T10:15:00.000Z'
),
(
  'histcob_pay_21f90db322da374d', 'treatment', '2nd Payment', '2nd Payment', 'histcob_139fe83fc83835c9', '7602384437', 'Cooch Behar', 'SATYAJIT SARKAR',
  '2025-05-19', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-19T10:16:00.000Z', '2025-05-19T10:16:00.000Z'
),
(
  'histcob_pay_ec60d86b5e44ac5a', 'treatment', '3rd Payment', '3rd Payment', 'histcob_139fe83fc83835c9', '7602384437', 'Cooch Behar', 'SATYAJIT SARKAR',
  '2025-05-26', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-26T10:17:00.000Z', '2025-05-26T10:17:00.000Z'
),
(
  'histcob_pay_2b4b4f8b6d9520fd', 'treatment', '4th Payment', '4th Payment', 'histcob_139fe83fc83835c9', '7602384437', 'Cooch Behar', 'SATYAJIT SARKAR',
  '2025-06-09', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-09T10:18:00.000Z', '2025-06-09T10:18:00.000Z'
),
(
  'histcob_pay_5058bb0c0cf7d55a', 'treatment', '5th Payment', '5th Payment', 'histcob_139fe83fc83835c9', '7602384437', 'Cooch Behar', 'SATYAJIT SARKAR',
  '2025-06-27', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-27T10:19:00.000Z', '2025-06-27T10:19:00.000Z'
),
(
  'histcob_pay_fdd7bd7b4ac966de', 'treatment', '6th Payment', '6th Payment', 'histcob_139fe83fc83835c9', '7602384437', 'Cooch Behar', 'SATYAJIT SARKAR',
  '2025-07-30', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-30T10:20:00.000Z', '2025-07-30T10:20:00.000Z'
),
(
  'histcob_pay_162992067330e94a', 'treatment', '7th Payment', '7th Payment', 'histcob_139fe83fc83835c9', '7602384437', 'Cooch Behar', 'SATYAJIT SARKAR',
  '2025-07-07', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-07T10:21:00.000Z', '2025-07-07T10:21:00.000Z'
),
(
  'histcob_pay_87d35f5718aed0cd', 'treatment', '8th Payment', '8th Payment', 'histcob_139fe83fc83835c9', '7602384437', 'Cooch Behar', 'SATYAJIT SARKAR',
  '2025-07-14', '7000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-14T10:22:00.000Z', '2025-07-14T10:22:00.000Z'
),
(
  'histcob_pay_a7c48bfaef4efc41', 'treatment', 'Advance', 'Advance', 'histcob_d037ddab6e67d6c8', '9434827298', 'Cooch Behar', 'LAXAM MAHATA',
  '2025-05-16', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-16T10:15:00.000Z', '2025-05-16T10:15:00.000Z'
),
(
  'histcob_pay_dc67f11e6e7a7afc', 'treatment', '2nd Payment', '2nd Payment', 'histcob_d037ddab6e67d6c8', '9434827298', 'Cooch Behar', 'LAXAM MAHATA',
  '2025-05-23', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-23T10:16:00.000Z', '2025-05-23T10:16:00.000Z'
),
(
  'histcob_pay_792a102d1d2e49e8', 'treatment', '3rd Payment', '3rd Payment', 'histcob_d037ddab6e67d6c8', '9434827298', 'Cooch Behar', 'LAXAM MAHATA',
  '2025-05-26', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-26T10:17:00.000Z', '2025-05-26T10:17:00.000Z'
),
(
  'histcob_pay_350f85bfb11723c2', 'treatment', '4th Payment', '4th Payment', 'histcob_d037ddab6e67d6c8', '9434827298', 'Cooch Behar', 'LAXAM MAHATA',
  '2025-06-02', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-02T10:18:00.000Z', '2025-06-02T10:18:00.000Z'
),
(
  'histcob_pay_def36fe6c3cb9eb2', 'treatment', '5th Payment', '5th Payment', 'histcob_d037ddab6e67d6c8', '9434827298', 'Cooch Behar', 'LAXAM MAHATA',
  '2025-07-26', '3500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:19:00.000Z', '2025-07-26T10:19:00.000Z'
),
(
  'histcob_pay_11ab9a97ee2142d5', 'treatment', '6th Payment', '6th Payment', 'histcob_d037ddab6e67d6c8', '9434827298', 'Cooch Behar', 'LAXAM MAHATA',
  '2025-07-28', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-28T10:20:00.000Z', '2025-07-28T10:20:00.000Z'
),
(
  'histcob_pay_3db38fa78d711f8f', 'treatment', '7th Payment', '7th Payment', 'histcob_d037ddab6e67d6c8', '9434827298', 'Cooch Behar', 'LAXAM MAHATA',
  '2025-08-15', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-15T10:21:00.000Z', '2025-08-15T10:21:00.000Z'
),
(
  'histcob_pay_f84d16c6e783477c', 'treatment', 'Advance', 'Advance', 'histcob_ec865f519178d9dc', '8537889405', 'Cooch Behar', 'LOTIFA BIBI',
  '2025-05-19', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-19T10:15:00.000Z', '2025-05-19T10:15:00.000Z'
),
(
  'histcob_pay_2872534340bb2a98', 'treatment', '2nd Payment', '2nd Payment', 'histcob_ec865f519178d9dc', '8537889405', 'Cooch Behar', 'LOTIFA BIBI',
  '2025-05-23', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-23T10:16:00.000Z', '2025-05-23T10:16:00.000Z'
),
(
  'histcob_pay_5dcfd7031f61ecd9', 'treatment', '3rd Payment', '3rd Payment', 'histcob_ec865f519178d9dc', '8537889405', 'Cooch Behar', 'LOTIFA BIBI',
  '2025-05-30', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-30T10:17:00.000Z', '2025-05-30T10:17:00.000Z'
),
(
  'histcob_pay_d94d0b7b3cb83966', 'treatment', '4th Payment', '4th Payment', 'histcob_ec865f519178d9dc', '8537889405', 'Cooch Behar', 'LOTIFA BIBI',
  '2025-06-06', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-06T10:18:00.000Z', '2025-06-06T10:18:00.000Z'
),
(
  'histcob_pay_6a19682231fc1ba8', 'treatment', '5th Payment', '5th Payment', 'histcob_ec865f519178d9dc', '8537889405', 'Cooch Behar', 'LOTIFA BIBI',
  '2025-06-13', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-13T10:19:00.000Z', '2025-06-13T10:19:00.000Z'
),
(
  'histcob_pay_48c87256d9761017', 'treatment', '6th Payment', '6th Payment', 'histcob_ec865f519178d9dc', '8537889405', 'Cooch Behar', 'LOTIFA BIBI',
  '2025-06-20', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-20T10:20:00.000Z', '2025-06-20T10:20:00.000Z'
),
(
  'histcob_pay_d9f71b096718a9f0', 'treatment', '7th Payment', '7th Payment', 'histcob_ec865f519178d9dc', '8537889405', 'Cooch Behar', 'LOTIFA BIBI',
  '2025-06-27', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-27T10:21:00.000Z', '2025-06-27T10:21:00.000Z'
),
(
  'histcob_pay_d1ef2e6078a0acc6', 'treatment', '8th Payment', '8th Payment', 'histcob_ec865f519178d9dc', '8537889405', 'Cooch Behar', 'LOTIFA BIBI',
  '2025-06-30', '8000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-30T10:22:00.000Z', '2025-06-30T10:22:00.000Z'
),
(
  'histcob_pay_68c6c2c42d9c963c', 'treatment', '9th Payment', '9th Payment', 'histcob_ec865f519178d9dc', '8537889405', 'Cooch Behar', 'LOTIFA BIBI',
  '2025-07-07', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-07T10:23:00.000Z', '2025-07-07T10:23:00.000Z'
),
(
  'histcob_pay_1472b763d2dbd4af', 'treatment', '10th Payment', '10th Payment', 'histcob_ec865f519178d9dc', '8537889405', 'Cooch Behar', 'LOTIFA BIBI',
  '2025-07-11', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-11T10:24:00.000Z', '2025-07-11T10:24:00.000Z'
),
(
  'histcob_pay_9a51200a905b8712', 'treatment', '11th Payment', '11th Payment', 'histcob_ec865f519178d9dc', '8537889405', 'Cooch Behar', 'LOTIFA BIBI',
  '2025-07-21', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-21T10:25:00.000Z', '2025-07-21T10:25:00.000Z'
),
(
  'histcob_pay_f978f1f4fb3180ff', 'treatment', '12th Payment', '12th Payment', 'histcob_ec865f519178d9dc', '8537889405', 'Cooch Behar', 'LOTIFA BIBI',
  '2025-05-26', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-26T10:26:00.000Z', '2025-05-26T10:26:00.000Z'
),
(
  'histcob_pay_a84acfd96ba8c267', 'treatment', '13th Payment', '13th Payment', 'histcob_ec865f519178d9dc', '8537889405', 'Cooch Behar', 'LOTIFA BIBI',
  '2025-08-08', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-08T10:27:00.000Z', '2025-08-08T10:27:00.000Z'
),
(
  'histcob_pay_f6245441559c7724', 'treatment', 'Advance', 'Advance', 'histcob_994894b5358c3f40', '7363808369', 'Cooch Behar', 'SUNIL DHAR',
  '2025-05-23', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-23T10:15:00.000Z', '2025-05-23T10:15:00.000Z'
),
(
  'histcob_pay_9b255c70313a56e6', 'treatment', '2nd Payment', '2nd Payment', 'histcob_994894b5358c3f40', '7363808369', 'Cooch Behar', 'SUNIL DHAR',
  '2025-05-23', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet) -- combined/date-unclear in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-23T10:16:00.000Z', '2025-05-23T10:16:00.000Z'
),
(
  'histcob_pay_4d747db81be92df9', 'treatment', '3rd Payment', '3rd Payment', 'histcob_994894b5358c3f40', '7363808369', 'Cooch Behar', 'SUNIL DHAR',
  '2025-05-30', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-30T10:17:00.000Z', '2025-05-30T10:17:00.000Z'
),
(
  'histcob_pay_21a7513965035097', 'treatment', 'Advance', 'Advance', 'histcob_286adb020749e431', '8016142536', 'Cooch Behar', 'BISHNU BARMAN',
  '2025-06-01', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-01T10:15:00.000Z', '2025-06-01T10:15:00.000Z'
),
(
  'histcob_pay_0534fa67d8e4209c', 'treatment', 'Advance', 'Advance', 'histcob_e51604d5a824a97f', '7797051503', 'Cooch Behar', 'KRITIKA DAS',
  '2025-06-02', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-02T10:15:00.000Z', '2025-06-02T10:15:00.000Z'
),
(
  'histcob_pay_8078dc124aab6084', 'treatment', '2nd Payment', '2nd Payment', 'histcob_e51604d5a824a97f', '7797051503', 'Cooch Behar', 'KRITIKA DAS',
  '2025-06-06', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-06T10:16:00.000Z', '2025-06-06T10:16:00.000Z'
),
(
  'histcob_pay_70a166a57d27119e', 'treatment', '3rd Payment', '3rd Payment', 'histcob_e51604d5a824a97f', '7797051503', 'Cooch Behar', 'KRITIKA DAS',
  '2025-06-09', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-09T10:17:00.000Z', '2025-06-09T10:17:00.000Z'
),
(
  'histcob_pay_48ecd111ebc895ea', 'treatment', '4th Payment', '4th Payment', 'histcob_e51604d5a824a97f', '7797051503', 'Cooch Behar', 'KRITIKA DAS',
  '2025-06-13', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-13T10:18:00.000Z', '2025-06-13T10:18:00.000Z'
),
(
  'histcob_pay_5ebb26e17d6611bd', 'treatment', '5th Payment', '5th Payment', 'histcob_e51604d5a824a97f', '7797051503', 'Cooch Behar', 'KRITIKA DAS',
  '2025-06-16', '5500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-16T10:19:00.000Z', '2025-06-16T10:19:00.000Z'
),
(
  'histcob_pay_7a9f6fc4eeb92934', 'treatment', '6th Payment', '6th Payment', 'histcob_e51604d5a824a97f', '7797051503', 'Cooch Behar', 'KRITIKA DAS',
  '2025-06-23', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-23T10:20:00.000Z', '2025-06-23T10:20:00.000Z'
),
(
  'histcob_pay_2acc9c65c3027c41', 'treatment', '7th Payment', '7th Payment', 'histcob_e51604d5a824a97f', '7797051503', 'Cooch Behar', 'KRITIKA DAS',
  '2025-06-27', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-27T10:21:00.000Z', '2025-06-27T10:21:00.000Z'
),
(
  'histcob_pay_8c3bf5354445e85d', 'treatment', '8th Payment', '8th Payment', 'histcob_e51604d5a824a97f', '7797051503', 'Cooch Behar', 'KRITIKA DAS',
  '2025-07-14', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-14T10:22:00.000Z', '2025-07-14T10:22:00.000Z'
),
(
  'histcob_pay_06ae18e7f65b63d7', 'treatment', '9th Payment', '9th Payment', 'histcob_e51604d5a824a97f', '7797051503', 'Cooch Behar', 'KRITIKA DAS',
  '2025-07-21', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-21T10:23:00.000Z', '2025-07-21T10:23:00.000Z'
),
(
  'histcob_pay_5b8f16a2c87d68c2', 'treatment', 'Advance', 'Advance', 'histcob_7195cd872914de85', '7318792629', 'Cooch Behar', 'YAJADIN MIA',
  '2025-06-02', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-02T10:15:00.000Z', '2025-06-02T10:15:00.000Z'
),
(
  'histcob_pay_3a596d0111fafe51', 'treatment', 'Advance', 'Advance', 'histcob_0c3ce75e4f1c0e52', '9395019343', 'Cooch Behar', 'SAHAJAYAN ALI KHAN',
  '2025-06-09', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-09T10:15:00.000Z', '2025-06-09T10:15:00.000Z'
),
(
  'histcob_pay_ca08b0296a8d3252', 'treatment', '2nd Payment', '2nd Payment', 'histcob_0c3ce75e4f1c0e52', '9395019343', 'Cooch Behar', 'SAHAJAYAN ALI KHAN',
  '2025-06-16', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-16T10:16:00.000Z', '2025-06-16T10:16:00.000Z'
),
(
  'histcob_pay_d0f2d6009cf79c91', 'treatment', '3rd Payment', '3rd Payment', 'histcob_0c3ce75e4f1c0e52', '9395019343', 'Cooch Behar', 'SAHAJAYAN ALI KHAN',
  '2025-06-23', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-23T10:17:00.000Z', '2025-06-23T10:17:00.000Z'
),
(
  'histcob_pay_f0726f646b1fd759', 'treatment', '4th Payment', '4th Payment', 'histcob_0c3ce75e4f1c0e52', '9395019343', 'Cooch Behar', 'SAHAJAYAN ALI KHAN',
  '2025-07-11', '3500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-11T10:18:00.000Z', '2025-07-11T10:18:00.000Z'
),
(
  'histcob_pay_31f08a096392bd35', 'treatment', '5th Payment', '5th Payment', 'histcob_0c3ce75e4f1c0e52', '9395019343', 'Cooch Behar', 'SAHAJAYAN ALI KHAN',
  '2025-07-18', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-18T10:19:00.000Z', '2025-07-18T10:19:00.000Z'
),
(
  'histcob_pay_20e053532df44bb2', 'treatment', 'Advance', 'Advance', 'histcob_0484563877f37163', '9085598519', 'Cooch Behar', 'HOSSAIN ALI',
  '2025-06-06', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-06T10:15:00.000Z', '2025-06-06T10:15:00.000Z'
),
(
  'histcob_pay_0a96ddf7c05dfc3b', 'treatment', '2nd Payment', '2nd Payment', 'histcob_0484563877f37163', '9085598519', 'Cooch Behar', 'HOSSAIN ALI',
  '2025-06-13', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-13T10:16:00.000Z', '2025-06-13T10:16:00.000Z'
),
(
  'histcob_pay_2ca029cd33d0ad9e', 'treatment', '3rd Payment', '3rd Payment', 'histcob_0484563877f37163', '9085598519', 'Cooch Behar', 'HOSSAIN ALI',
  '2025-06-20', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-20T10:17:00.000Z', '2025-06-20T10:17:00.000Z'
),
(
  'histcob_pay_12b180118f1f5214', 'treatment', '4th Payment', '4th Payment', 'histcob_0484563877f37163', '9085598519', 'Cooch Behar', 'HOSSAIN ALI',
  '2025-06-27', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-27T10:18:00.000Z', '2025-06-27T10:18:00.000Z'
),
(
  'histcob_pay_aef64a01e3456ab4', 'treatment', '5th Payment', '5th Payment', 'histcob_0484563877f37163', '9085598519', 'Cooch Behar', 'HOSSAIN ALI',
  '2025-07-04', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-04T10:19:00.000Z', '2025-07-04T10:19:00.000Z'
),
(
  'histcob_pay_e4e9b22e2e3aaaea', 'treatment', 'Advance', 'Advance', 'histcob_4e77a7609df270fa', '8900663765', 'Cooch Behar', 'NITYANAND SEN',
  '2025-06-06', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-06T10:15:00.000Z', '2025-06-06T10:15:00.000Z'
),
(
  'histcob_pay_d7fa5ff9eb975222', 'treatment', '2nd Payment', '2nd Payment', 'histcob_4e77a7609df270fa', '8900663765', 'Cooch Behar', 'NITYANAND SEN',
  '2025-06-09', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-09T10:16:00.000Z', '2025-06-09T10:16:00.000Z'
),
(
  'histcob_pay_31e7d38b6f32afcb', 'treatment', '3rd Payment', '3rd Payment', 'histcob_4e77a7609df270fa', '8900663765', 'Cooch Behar', 'NITYANAND SEN',
  '2025-06-13', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-13T10:17:00.000Z', '2025-06-13T10:17:00.000Z'
),
(
  'histcob_pay_20c0a81504a651df', 'treatment', '4th Payment', '4th Payment', 'histcob_4e77a7609df270fa', '8900663765', 'Cooch Behar', 'NITYANAND SEN',
  '2025-06-16', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-16T10:18:00.000Z', '2025-06-16T10:18:00.000Z'
),
(
  'histcob_pay_04bba2b6838b37f3', 'treatment', '5th Payment', '5th Payment', 'histcob_4e77a7609df270fa', '8900663765', 'Cooch Behar', 'NITYANAND SEN',
  '2025-06-20', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-20T10:19:00.000Z', '2025-06-20T10:19:00.000Z'
),
(
  'histcob_pay_af118d60e9db7937', 'treatment', '6th Payment', '6th Payment', 'histcob_4e77a7609df270fa', '8900663765', 'Cooch Behar', 'NITYANAND SEN',
  '2025-06-27', '3800', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-27T10:20:00.000Z', '2025-06-27T10:20:00.000Z'
),
(
  'histcob_pay_2305d2dc7321f27c', 'treatment', 'Advance', 'Advance', 'histcob_60100a5295653f7c', '8927079334', 'Cooch Behar', 'SAHID ALI',
  '2025-06-09', '700', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-09T10:15:00.000Z', '2025-06-09T10:15:00.000Z'
),
(
  'histcob_pay_931ea04aede93c55', 'treatment', '2nd Payment', '2nd Payment', 'histcob_60100a5295653f7c', '8927079334', 'Cooch Behar', 'SAHID ALI',
  '2025-06-13', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-13T10:16:00.000Z', '2025-06-13T10:16:00.000Z'
),
(
  'histcob_pay_b79273b7289038f2', 'treatment', '3rd Payment', '3rd Payment', 'histcob_60100a5295653f7c', '8927079334', 'Cooch Behar', 'SAHID ALI',
  '2025-06-27', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-27T10:17:00.000Z', '2025-06-27T10:17:00.000Z'
),
(
  'histcob_pay_7c427dc4987c97ed', 'treatment', '4th Payment', '4th Payment', 'histcob_60100a5295653f7c', '8927079334', 'Cooch Behar', 'SAHID ALI',
  '2025-07-07', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-07T10:18:00.000Z', '2025-07-07T10:18:00.000Z'
),
(
  'histcob_pay_25b89053f27d48cc', 'treatment', 'Advance', 'Advance', 'histcob_a7d49b77deacd666', '7002028352', 'Cooch Behar', 'RAFIKUL ISLAM',
  '2025-06-16', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-16T10:15:00.000Z', '2025-06-16T10:15:00.000Z'
),
(
  'histcob_pay_8f717c699b37928d', 'treatment', '2nd Payment', '2nd Payment', 'histcob_a7d49b77deacd666', '7002028352', 'Cooch Behar', 'RAFIKUL ISLAM',
  '2025-06-21', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:16:00.000Z', '2025-06-21T10:16:00.000Z'
),
(
  'histcob_pay_c167f1c962125c36', 'treatment', '3rd Payment', '3rd Payment', 'histcob_a7d49b77deacd666', '7002028352', 'Cooch Behar', 'RAFIKUL ISLAM',
  '2025-06-28', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:17:00.000Z', '2025-06-28T10:17:00.000Z'
),
(
  'histcob_pay_81ef6c5c2b6ee6c1', 'treatment', 'Advance', 'Advance', 'histcob_b0e05fedd0b0c853', '9609918668', 'Cooch Behar', 'PRAKASH DAS',
  '2025-06-20', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-20T10:15:00.000Z', '2025-06-20T10:15:00.000Z'
),
(
  'histcob_pay_64724b7091efaf51', 'treatment', '2nd Payment', '2nd Payment', 'histcob_b0e05fedd0b0c853', '9609918668', 'Cooch Behar', 'PRAKASH DAS',
  '2025-07-04', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-04T10:16:00.000Z', '2025-07-04T10:16:00.000Z'
),
(
  'histcob_pay_4a7cc26c25ae2970', 'treatment', '3rd Payment', '3rd Payment', 'histcob_b0e05fedd0b0c853', '9609918668', 'Cooch Behar', 'PRAKASH DAS',
  '2025-07-14', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-14T10:17:00.000Z', '2025-07-14T10:17:00.000Z'
),
(
  'histcob_pay_39e1bc2f013de542', 'treatment', '4th Payment', '4th Payment', 'histcob_b0e05fedd0b0c853', '9609918668', 'Cooch Behar', 'PRAKASH DAS',
  '2025-07-25', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-25T10:18:00.000Z', '2025-07-25T10:18:00.000Z'
),
(
  'histcob_pay_043721b8fed7b3e1', 'treatment', 'Advance', 'Advance', 'histcob_dfa83baa67c08704', '6002253819', 'Cooch Behar', 'YOUSIMUDDIN ALI',
  '2025-06-20', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-20T10:15:00.000Z', '2025-06-20T10:15:00.000Z'
),
(
  'histcob_pay_bb9fa2d2532fd042', 'treatment', '2nd Payment', '2nd Payment', 'histcob_dfa83baa67c08704', '6002253819', 'Cooch Behar', 'YOUSIMUDDIN ALI',
  '2025-06-27', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-27T10:16:00.000Z', '2025-06-27T10:16:00.000Z'
),
(
  'histcob_pay_acc79200633d68fd', 'treatment', '3rd Payment', '3rd Payment', 'histcob_dfa83baa67c08704', '6002253819', 'Cooch Behar', 'YOUSIMUDDIN ALI',
  '2025-07-04', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-04T10:17:00.000Z', '2025-07-04T10:17:00.000Z'
),
(
  'histcob_pay_fde3e8f6c468ffc9', 'treatment', '4th Payment', '4th Payment', 'histcob_dfa83baa67c08704', '6002253819', 'Cooch Behar', 'YOUSIMUDDIN ALI',
  '2025-07-14', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-14T10:18:00.000Z', '2025-07-14T10:18:00.000Z'
),
(
  'histcob_pay_848fc8e44ad13fbb', 'treatment', '5th Payment', '5th Payment', 'histcob_dfa83baa67c08704', '6002253819', 'Cooch Behar', 'YOUSIMUDDIN ALI',
  '2025-08-04', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-04T10:19:00.000Z', '2025-08-04T10:19:00.000Z'
),
(
  'histcob_pay_11de248f7e3d016a', 'treatment', 'Advance', 'Advance', 'histcob_5470e10d3702979c', '7002111915', 'Cooch Behar', 'ABDUL WAD SARKAR',
  '2025-06-20', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-20T10:15:00.000Z', '2025-06-20T10:15:00.000Z'
),
(
  'histcob_pay_93056bd9d0eb7faf', 'treatment', '2nd Payment', '2nd Payment', 'histcob_5470e10d3702979c', '7002111915', 'Cooch Behar', 'ABDUL WAD SARKAR',
  '2025-06-27', '7000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-27T10:16:00.000Z', '2025-06-27T10:16:00.000Z'
),
(
  'histcob_pay_baec1765b0f0f832', 'treatment', '3rd Payment', '3rd Payment', 'histcob_5470e10d3702979c', '7002111915', 'Cooch Behar', 'ABDUL WAD SARKAR',
  '2025-07-04', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-04T10:17:00.000Z', '2025-07-04T10:17:00.000Z'
),
(
  'histcob_pay_d3dd7428c67c0525', 'treatment', '4th Payment', '4th Payment', 'histcob_5470e10d3702979c', '7002111915', 'Cooch Behar', 'ABDUL WAD SARKAR',
  '2025-07-11', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-11T10:18:00.000Z', '2025-07-11T10:18:00.000Z'
),
(
  'histcob_pay_c64f34e27c3490fb', 'treatment', '5th Payment', '5th Payment', 'histcob_5470e10d3702979c', '7002111915', 'Cooch Behar', 'ABDUL WAD SARKAR',
  '2025-07-14', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-14T10:19:00.000Z', '2025-07-14T10:19:00.000Z'
),
(
  'histcob_pay_e191c02f15f967be', 'treatment', '6th Payment', '6th Payment', 'histcob_5470e10d3702979c', '7002111915', 'Cooch Behar', 'ABDUL WAD SARKAR',
  '2025-07-25', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-25T10:20:00.000Z', '2025-07-25T10:20:00.000Z'
),
(
  'histcob_pay_4672d3ab34be30bb', 'treatment', '7th Payment', '7th Payment', 'histcob_5470e10d3702979c', '7002111915', 'Cooch Behar', 'ABDUL WAD SARKAR',
  '2025-07-28', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-28T10:21:00.000Z', '2025-07-28T10:21:00.000Z'
),
(
  'histcob_pay_b2a0699a98f5233b', 'treatment', '8th Payment', '8th Payment', 'histcob_5470e10d3702979c', '7002111915', 'Cooch Behar', 'ABDUL WAD SARKAR',
  '2025-08-04', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-04T10:22:00.000Z', '2025-08-04T10:22:00.000Z'
),
(
  'histcob_pay_21144a264a81a47e', 'treatment', '9th Payment', '9th Payment', 'histcob_5470e10d3702979c', '7002111915', 'Cooch Behar', 'ABDUL WAD SARKAR',
  '2025-08-18', '1009', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-18T10:23:00.000Z', '2025-08-18T10:23:00.000Z'
),
(
  'histcob_pay_d8f172fc1509ce1e', 'treatment', 'Advance', 'Advance', 'histcob_bfb3f79b0752023e', '8116738720', 'Cooch Behar', 'RAHAMAT ALI',
  '2025-06-20', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-20T10:15:00.000Z', '2025-06-20T10:15:00.000Z'
),
(
  'histcob_pay_2a1e891269cd95a2', 'treatment', '2nd Payment', '2nd Payment', 'histcob_bfb3f79b0752023e', '8116738720', 'Cooch Behar', 'RAHAMAT ALI',
  '2025-06-23', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-23T10:16:00.000Z', '2025-06-23T10:16:00.000Z'
),
(
  'histcob_pay_20236aa32c102c90', 'treatment', '3rd Payment', '3rd Payment', 'histcob_bfb3f79b0752023e', '8116738720', 'Cooch Behar', 'RAHAMAT ALI',
  '2025-06-27', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-27T10:17:00.000Z', '2025-06-27T10:17:00.000Z'
),
(
  'histcob_pay_a3863ba6c7303e9e', 'treatment', '4th Payment', '4th Payment', 'histcob_bfb3f79b0752023e', '8116738720', 'Cooch Behar', 'RAHAMAT ALI',
  '2025-06-30', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-30T10:18:00.000Z', '2025-06-30T10:18:00.000Z'
),
(
  'histcob_pay_5ea7180cc3dee1f1', 'treatment', '5th Payment', '5th Payment', 'histcob_bfb3f79b0752023e', '8116738720', 'Cooch Behar', 'RAHAMAT ALI',
  '2025-06-04', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-04T10:19:00.000Z', '2025-06-04T10:19:00.000Z'
),
(
  'histcob_pay_1a7c3c89a7cafe83', 'treatment', '6th Payment', '6th Payment', 'histcob_bfb3f79b0752023e', '8116738720', 'Cooch Behar', 'RAHAMAT ALI',
  '2025-07-18', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-18T10:20:00.000Z', '2025-07-18T10:20:00.000Z'
),
(
  'histcob_pay_65ffa82eb232e18f', 'treatment', '7th Payment', '7th Payment', 'histcob_bfb3f79b0752023e', '8116738720', 'Cooch Behar', 'RAHAMAT ALI',
  '2025-07-25', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-25T10:21:00.000Z', '2025-07-25T10:21:00.000Z'
),
(
  'histcob_pay_e99fc35500fbb466', 'treatment', '8th Payment', '8th Payment', 'histcob_bfb3f79b0752023e', '8116738720', 'Cooch Behar', 'RAHAMAT ALI',
  '2025-08-08', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-08T10:22:00.000Z', '2025-08-08T10:22:00.000Z'
),
(
  'histcob_pay_befdca82ea34c862', 'treatment', 'Advance', 'Advance', 'histcob_901d9838d96b4335', '6000633291', 'Cooch Behar', 'AMINUR ISLAM',
  '2025-06-23', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-23T10:15:00.000Z', '2025-06-23T10:15:00.000Z'
),
(
  'histcob_pay_0e47dc9c746be8ac', 'treatment', '2nd Payment', '2nd Payment', 'histcob_901d9838d96b4335', '6000633291', 'Cooch Behar', 'AMINUR ISLAM',
  '2025-06-30', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-30T10:16:00.000Z', '2025-06-30T10:16:00.000Z'
),
(
  'histcob_pay_6ff3094be9061ae3', 'treatment', '3rd Payment', '3rd Payment', 'histcob_901d9838d96b4335', '6000633291', 'Cooch Behar', 'AMINUR ISLAM',
  '2025-07-14', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-14T10:17:00.000Z', '2025-07-14T10:17:00.000Z'
),
(
  'histcob_pay_d1af75540350d3df', 'treatment', '4th Payment', '4th Payment', 'histcob_901d9838d96b4335', '6000633291', 'Cooch Behar', 'AMINUR ISLAM',
  '2025-07-28', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-28T10:18:00.000Z', '2025-07-28T10:18:00.000Z'
),
(
  'histcob_pay_b6dbb8db8bfd5c28', 'treatment', 'Advance', 'Advance', 'histcob_7b0278288cd7f866', '6901906971', 'Cooch Behar', 'KALPANA BALA BARMAN',
  '2025-06-28', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:15:00.000Z', '2025-06-28T10:15:00.000Z'
),
(
  'histcob_pay_56e56eddd5dc0f29', 'treatment', 'Advance', 'Advance', 'histcob_bcffb4968d179b32', '9547395711', 'Cooch Behar', 'IBRAHIM MIAH',
  '2025-06-30', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-30T10:15:00.000Z', '2025-06-30T10:15:00.000Z'
),
(
  'histcob_pay_a277c8687d2295f7', 'treatment', '2nd Payment', '2nd Payment', 'histcob_bcffb4968d179b32', '9547395711', 'Cooch Behar', 'IBRAHIM MIAH',
  '2025-07-04', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-04T10:16:00.000Z', '2025-07-04T10:16:00.000Z'
),
(
  'histcob_pay_7bd7ab9d48f38061', 'treatment', '3rd Payment', '3rd Payment', 'histcob_bcffb4968d179b32', '9547395711', 'Cooch Behar', 'IBRAHIM MIAH',
  '2025-07-11', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-11T10:17:00.000Z', '2025-07-11T10:17:00.000Z'
),
(
  'histcob_pay_6d2a602902e5a3f9', 'treatment', '4th Payment', '4th Payment', 'histcob_bcffb4968d179b32', '9547395711', 'Cooch Behar', 'IBRAHIM MIAH',
  '2025-07-14', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-14T10:18:00.000Z', '2025-07-14T10:18:00.000Z'
),
(
  'histcob_pay_4761ccf29a115c44', 'treatment', '5th Payment', '5th Payment', 'histcob_bcffb4968d179b32', '9547395711', 'Cooch Behar', 'IBRAHIM MIAH',
  '2025-07-18', '6000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-18T10:19:00.000Z', '2025-07-18T10:19:00.000Z'
),
(
  'histcob_pay_1ddf5f4fc3fb1825', 'treatment', '6th Payment', '6th Payment', 'histcob_bcffb4968d179b32', '9547395711', 'Cooch Behar', 'IBRAHIM MIAH',
  '2025-07-25', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-25T10:20:00.000Z', '2025-07-25T10:20:00.000Z'
),
(
  'histcob_pay_90d2466e0e3ba92c', 'treatment', '7th Payment', '7th Payment', 'histcob_bcffb4968d179b32', '9547395711', 'Cooch Behar', 'IBRAHIM MIAH',
  '2025-07-28', '4500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-28T10:21:00.000Z', '2025-07-28T10:21:00.000Z'
),
(
  'histcob_pay_159764721a146bbf', 'treatment', '8th Payment', '8th Payment', 'histcob_bcffb4968d179b32', '9547395711', 'Cooch Behar', 'IBRAHIM MIAH',
  '2025-08-11', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-11T10:22:00.000Z', '2025-08-11T10:22:00.000Z'
),
(
  'histcob_pay_1ef039b5ae91b0e7', 'treatment', 'Advance', 'Advance', 'histcob_071070e33d702b53', '8453663281', 'Cooch Behar', 'HAFIZUR ISLAM',
  '2025-06-30', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-30T10:15:00.000Z', '2025-06-30T10:15:00.000Z'
),
(
  'histcob_pay_e9279ceecbd965d8', 'treatment', 'Advance', 'Advance', 'histcob_4cf4026e7aa635c7', '9821257313', 'Cooch Behar', 'SWAPAN ADHIKARI',
  '2025-06-30', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-30T10:15:00.000Z', '2025-06-30T10:15:00.000Z'
),
(
  'histcob_pay_b0d41bf0057ced72', 'treatment', '2nd Payment', '2nd Payment', 'histcob_4cf4026e7aa635c7', '9821257313', 'Cooch Behar', 'SWAPAN ADHIKARI',
  '2025-07-07', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-07T10:16:00.000Z', '2025-07-07T10:16:00.000Z'
),
(
  'histcob_pay_a63c9b2a7dc0e21e', 'treatment', '3rd Payment', '3rd Payment', 'histcob_4cf4026e7aa635c7', '9821257313', 'Cooch Behar', 'SWAPAN ADHIKARI',
  '2025-07-14', '2500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-14T10:17:00.000Z', '2025-07-14T10:17:00.000Z'
),
(
  'histcob_pay_35c190e3431f527a', 'treatment', 'Advance', 'Advance', 'histcob_58971ee936826126', '9101565033', 'Cooch Behar', 'ASRAFUL ALAM',
  '2025-06-30', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-30T10:15:00.000Z', '2025-06-30T10:15:00.000Z'
),
(
  'histcob_pay_50ba2bef2f3670b6', 'treatment', '2nd Payment', '2nd Payment', 'histcob_58971ee936826126', '9101565033', 'Cooch Behar', 'ASRAFUL ALAM',
  '2025-07-04', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-04T10:16:00.000Z', '2025-07-04T10:16:00.000Z'
),
(
  'histcob_pay_4f7c21ab176eb6bc', 'treatment', '3rd Payment', '3rd Payment', 'histcob_58971ee936826126', '9101565033', 'Cooch Behar', 'ASRAFUL ALAM',
  '2025-07-18', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-18T10:17:00.000Z', '2025-07-18T10:17:00.000Z'
),
(
  'histcob_pay_2436c174b94e622b', 'treatment', '4th Payment', '4th Payment', 'histcob_58971ee936826126', '9101565033', 'Cooch Behar', 'ASRAFUL ALAM',
  '2025-07-07', '6000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-07T10:18:00.000Z', '2025-07-07T10:18:00.000Z'
),
(
  'histcob_pay_583c885f33700c5c', 'treatment', '5th Payment', '5th Payment', 'histcob_58971ee936826126', '9101565033', 'Cooch Behar', 'ASRAFUL ALAM',
  '2025-07-11', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-11T10:19:00.000Z', '2025-07-11T10:19:00.000Z'
),
(
  'histcob_pay_5b5c29c71cb347ef', 'treatment', '6th Payment', '6th Payment', 'histcob_58971ee936826126', '9101565033', 'Cooch Behar', 'ASRAFUL ALAM',
  '2025-07-14', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-14T10:20:00.000Z', '2025-07-14T10:20:00.000Z'
),
(
  'histcob_pay_fe6a3dfa55a7116e', 'treatment', '7th Payment', '7th Payment', 'histcob_58971ee936826126', '9101565033', 'Cooch Behar', 'ASRAFUL ALAM',
  '2025-07-25', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-25T10:21:00.000Z', '2025-07-25T10:21:00.000Z'
),
(
  'histcob_pay_d722652733d5c731', 'treatment', '8th Payment', '8th Payment', 'histcob_58971ee936826126', '9101565033', 'Cooch Behar', 'ASRAFUL ALAM',
  '2025-07-28', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-28T10:22:00.000Z', '2025-07-28T10:22:00.000Z'
),
(
  'histcob_pay_20d7cca56c69e519', 'treatment', '9th Payment', '9th Payment', 'histcob_58971ee936826126', '9101565033', 'Cooch Behar', 'ASRAFUL ALAM',
  '2025-08-01', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-01T10:23:00.000Z', '2025-08-01T10:23:00.000Z'
),
(
  'histcob_pay_411c7d4c2690707c', 'treatment', '10th Payment', '10th Payment', 'histcob_58971ee936826126', '9101565033', 'Cooch Behar', 'ASRAFUL ALAM',
  '2025-08-04', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-04T10:24:00.000Z', '2025-08-04T10:24:00.000Z'
),
(
  'histcob_pay_32acebb2faaaae48', 'treatment', '11th Payment', '11th Payment', 'histcob_58971ee936826126', '9101565033', 'Cooch Behar', 'ASRAFUL ALAM',
  '2025-08-11', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-11T10:25:00.000Z', '2025-08-11T10:25:00.000Z'
),
(
  'histcob_pay_8a09fc70a4063927', 'treatment', '12th Payment', '12th Payment', 'histcob_58971ee936826126', '9101565033', 'Cooch Behar', 'ASRAFUL ALAM',
  '2025-08-18', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-18T10:26:00.000Z', '2025-08-18T10:26:00.000Z'
),
(
  'histcob_pay_b133c7730ea3c269', 'treatment', '13th Payment', '13th Payment', 'histcob_58971ee936826126', '9101565033', 'Cooch Behar', 'ASRAFUL ALAM',
  '2025-08-25', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-25T10:27:00.000Z', '2025-08-25T10:27:00.000Z'
),
(
  'histcob_pay_74d4c422c6467271', 'treatment', '14th Payment', '14th Payment', 'histcob_58971ee936826126', '9101565033', 'Cooch Behar', 'ASRAFUL ALAM',
  '2025-08-20', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-20T10:28:00.000Z', '2025-08-20T10:28:00.000Z'
),
(
  'histcob_pay_d307fe2844c45134', 'treatment', '15th Payment', '15th Payment', 'histcob_58971ee936826126', '9101565033', 'Cooch Behar', 'ASRAFUL ALAM',
  '2025-09-01', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-01T10:29:00.000Z', '2025-09-01T10:29:00.000Z'
),
(
  'histcob_pay_80afa938a27a7846', 'treatment', 'Advance', 'Advance', 'histcob_8975f81b7aedcaaa', '8016432396', 'Cooch Behar', 'RINA DEY SARKAR',
  '2025-07-01', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-01T10:15:00.000Z', '2025-07-01T10:15:00.000Z'
),
(
  'histcob_pay_172e05478b1cc740', 'treatment', 'Advance', 'Advance', 'histcob_60a79ffb83f93a06', '9678800407', 'Cooch Behar', 'DEEP ACHARY',
  '2025-07-04', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-04T10:15:00.000Z', '2025-07-04T10:15:00.000Z'
),
(
  'histcob_pay_556fcbeb8684660e', 'treatment', '2nd Payment', '2nd Payment', 'histcob_60a79ffb83f93a06', '9678800407', 'Cooch Behar', 'DEEP ACHARY',
  '2025-07-07', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-07T10:16:00.000Z', '2025-07-07T10:16:00.000Z'
),
(
  'histcob_pay_e6dbbccd7ac7fd66', 'treatment', '3rd Payment', '3rd Payment', 'histcob_60a79ffb83f93a06', '9678800407', 'Cooch Behar', 'DEEP ACHARY',
  '2025-07-14', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-14T10:17:00.000Z', '2025-07-14T10:17:00.000Z'
),
(
  'histcob_pay_e57846cde969d8ab', 'treatment', '4th Payment', '4th Payment', 'histcob_60a79ffb83f93a06', '9678800407', 'Cooch Behar', 'DEEP ACHARY',
  '2025-08-08', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-08T10:18:00.000Z', '2025-08-08T10:18:00.000Z'
),
(
  'histcob_pay_edc066bcb8ac9631', 'treatment', '5th Payment', '5th Payment', 'histcob_60a79ffb83f93a06', '9678800407', 'Cooch Behar', 'DEEP ACHARY',
  '2025-08-18', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-18T10:19:00.000Z', '2025-08-18T10:19:00.000Z'
),
(
  'histcob_pay_bae11afb00bdff1d', 'treatment', 'Advance', 'Advance', 'histcob_cab76b76a688ae4f', '8348736026', 'Cooch Behar', 'AMBIYA BIBI',
  '2025-07-05', '20000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:15:00.000Z', '2025-07-05T10:15:00.000Z'
),
(
  'histcob_pay_8b619fa8032b2224', 'treatment', '2nd Payment', '2nd Payment', 'histcob_cab76b76a688ae4f', '8348736026', 'Cooch Behar', 'AMBIYA BIBI',
  '2025-07-12', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-12T10:16:00.000Z', '2025-07-12T10:16:00.000Z'
),
(
  'histcob_pay_840a8ff5fdb36018', 'treatment', '3rd Payment', '3rd Payment', 'histcob_cab76b76a688ae4f', '8348736026', 'Cooch Behar', 'AMBIYA BIBI',
  '2025-07-19', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-19T10:17:00.000Z', '2025-07-19T10:17:00.000Z'
),
(
  'histcob_pay_710842dc69c37663', 'treatment', '4th Payment', '4th Payment', 'histcob_cab76b76a688ae4f', '8348736026', 'Cooch Behar', 'AMBIYA BIBI',
  '2025-07-26', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:18:00.000Z', '2025-07-26T10:18:00.000Z'
),
(
  'histcob_pay_1ee174bd7c10ef22', 'treatment', '5th Payment', '5th Payment', 'histcob_cab76b76a688ae4f', '8348736026', 'Cooch Behar', 'AMBIYA BIBI',
  '2025-08-02', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:19:00.000Z', '2025-08-02T10:19:00.000Z'
),
(
  'histcob_pay_77c5041b42e1cef1', 'treatment', '6th Payment', '6th Payment', 'histcob_cab76b76a688ae4f', '8348736026', 'Cooch Behar', 'AMBIYA BIBI',
  '2025-08-09', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-09T10:20:00.000Z', '2025-08-09T10:20:00.000Z'
),
(
  'histcob_pay_6246f5cebef05dc7', 'treatment', '7th Payment', '7th Payment', 'histcob_cab76b76a688ae4f', '8348736026', 'Cooch Behar', 'AMBIYA BIBI',
  '2025-08-23', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:21:00.000Z', '2025-08-23T10:21:00.000Z'
),
(
  'histcob_pay_71db437c273cfb80', 'treatment', 'Advance', 'Advance', 'histcob_4b5c5e5f9f610f16', '7865086004', 'Cooch Behar', 'ASHOK ROY',
  '2025-07-07', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-07T10:15:00.000Z', '2025-07-07T10:15:00.000Z'
),
(
  'histcob_pay_a46d018ede3895b6', 'treatment', '2nd Payment', '2nd Payment', 'histcob_4b5c5e5f9f610f16', '7865086004', 'Cooch Behar', 'ASHOK ROY',
  '2025-07-14', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-14T10:16:00.000Z', '2025-07-14T10:16:00.000Z'
),
(
  'histcob_pay_7fa5515098a5449e', 'treatment', '3rd Payment', '3rd Payment', 'histcob_4b5c5e5f9f610f16', '7865086004', 'Cooch Behar', 'ASHOK ROY',
  '2025-07-18', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-18T10:17:00.000Z', '2025-07-18T10:17:00.000Z'
),
(
  'histcob_pay_6615dfd721c91978', 'treatment', '4th Payment', '4th Payment', 'histcob_4b5c5e5f9f610f16', '7865086004', 'Cooch Behar', 'ASHOK ROY',
  '2025-07-25', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-25T10:18:00.000Z', '2025-07-25T10:18:00.000Z'
),
(
  'histcob_pay_cd6dea6513889db8', 'treatment', '5th Payment', '5th Payment', 'histcob_4b5c5e5f9f610f16', '7865086004', 'Cooch Behar', 'ASHOK ROY',
  '2025-07-28', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-28T10:19:00.000Z', '2025-07-28T10:19:00.000Z'
),
(
  'histcob_pay_9242ad05b17bd956', 'treatment', '6th Payment', '6th Payment', 'histcob_4b5c5e5f9f610f16', '7865086004', 'Cooch Behar', 'ASHOK ROY',
  '2025-08-01', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-01T10:20:00.000Z', '2025-08-01T10:20:00.000Z'
),
(
  'histcob_pay_b58c7e8f6d521b8e', 'treatment', '7th Payment', '7th Payment', 'histcob_4b5c5e5f9f610f16', '7865086004', 'Cooch Behar', 'ASHOK ROY',
  '2025-08-04', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-04T10:21:00.000Z', '2025-08-04T10:21:00.000Z'
),
(
  'histcob_pay_9c46e528ff3e8a13', 'treatment', '8th Payment', '8th Payment', 'histcob_4b5c5e5f9f610f16', '7865086004', 'Cooch Behar', 'ASHOK ROY',
  '2025-08-08', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-08T10:22:00.000Z', '2025-08-08T10:22:00.000Z'
),
(
  'histcob_pay_8b5b1615ba3ad70f', 'treatment', '9th Payment', '9th Payment', 'histcob_4b5c5e5f9f610f16', '7865086004', 'Cooch Behar', 'ASHOK ROY',
  '2025-08-11', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-11T10:23:00.000Z', '2025-08-11T10:23:00.000Z'
),
(
  'histcob_pay_909a73ddeb51ef8c', 'treatment', '10th Payment', '10th Payment', 'histcob_4b5c5e5f9f610f16', '7865086004', 'Cooch Behar', 'ASHOK ROY',
  '2025-08-15', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-15T10:24:00.000Z', '2025-08-15T10:24:00.000Z'
),
(
  'histcob_pay_2e9835e2096282f6', 'treatment', '11th Payment', '11th Payment', 'histcob_4b5c5e5f9f610f16', '7865086004', 'Cooch Behar', 'ASHOK ROY',
  '2025-08-18', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-18T10:25:00.000Z', '2025-08-18T10:25:00.000Z'
),
(
  'histcob_pay_59940d969c4b578f', 'treatment', '12th Payment', '12th Payment', 'histcob_4b5c5e5f9f610f16', '7865086004', 'Cooch Behar', 'ASHOK ROY',
  '2025-08-22', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-22T10:26:00.000Z', '2025-08-22T10:26:00.000Z'
),
(
  'histcob_pay_174d9a3308534055', 'treatment', '13th Payment', '13th Payment', 'histcob_4b5c5e5f9f610f16', '7865086004', 'Cooch Behar', 'ASHOK ROY',
  '2025-08-25', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-25T10:27:00.000Z', '2025-08-25T10:27:00.000Z'
),
(
  'histcob_pay_4d38c1756bd45861', 'treatment', '14th Payment', '14th Payment', 'histcob_4b5c5e5f9f610f16', '7865086004', 'Cooch Behar', 'ASHOK ROY',
  '2025-08-29', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-29T10:28:00.000Z', '2025-08-29T10:28:00.000Z'
),
(
  'histcob_pay_fa16681780750fdf', 'treatment', '15th Payment', '15th Payment', 'histcob_4b5c5e5f9f610f16', '7865086004', 'Cooch Behar', 'ASHOK ROY',
  '2025-09-08', '14250', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-08T10:29:00.000Z', '2025-09-08T10:29:00.000Z'
),
(
  'histcob_pay_751d829f1d69ad0c', 'treatment', 'Advance', 'Advance', 'histcob_7ed4021862bbb3c7', '9707813942', 'Cooch Behar', 'NOSMINA KHATUN',
  '2025-07-07', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-07T10:15:00.000Z', '2025-07-07T10:15:00.000Z'
),
(
  'histcob_pay_da6dd673910bd019', 'treatment', '2nd Payment', '2nd Payment', 'histcob_7ed4021862bbb3c7', '9707813942', 'Cooch Behar', 'NOSMINA KHATUN',
  '2025-07-17', '3500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-17T10:16:00.000Z', '2025-07-17T10:16:00.000Z'
),
(
  'histcob_pay_a6504fbb05485341', 'treatment', '3rd Payment', '3rd Payment', 'histcob_7ed4021862bbb3c7', '9707813942', 'Cooch Behar', 'NOSMINA KHATUN',
  '2028-08-02', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2028-08-02T10:17:00.000Z', '2028-08-02T10:17:00.000Z'
),
(
  'histcob_pay_67d40569ac510aac', 'treatment', '4th Payment', '4th Payment', 'histcob_7ed4021862bbb3c7', '9707813942', 'Cooch Behar', 'NOSMINA KHATUN',
  '2028-08-08', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2028-08-08T10:18:00.000Z', '2028-08-08T10:18:00.000Z'
),
(
  'histcob_pay_5da3cb4d37fa3cd9', 'treatment', '5th Payment', '5th Payment', 'histcob_7ed4021862bbb3c7', '9707813942', 'Cooch Behar', 'NOSMINA KHATUN',
  '2028-08-22', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2028-08-22T10:19:00.000Z', '2028-08-22T10:19:00.000Z'
),
(
  'histcob_pay_aaef9c890ae7b3f7', 'treatment', '6th Payment', '6th Payment', 'histcob_7ed4021862bbb3c7', '9707813942', 'Cooch Behar', 'NOSMINA KHATUN',
  '2025-08-29', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-29T10:20:00.000Z', '2025-08-29T10:20:00.000Z'
),
(
  'histcob_pay_15c93161d1f0b1e8', 'treatment', '7th Payment', '7th Payment', 'histcob_7ed4021862bbb3c7', '9707813942', 'Cooch Behar', 'NOSMINA KHATUN',
  '2025-09-12', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-12T10:21:00.000Z', '2025-09-12T10:21:00.000Z'
),
(
  'histcob_pay_1e825b1dd74871b4', 'treatment', '8th Payment', '8th Payment', 'histcob_7ed4021862bbb3c7', '9707813942', 'Cooch Behar', 'NOSMINA KHATUN',
  '2025-09-26', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-26T10:22:00.000Z', '2025-09-26T10:22:00.000Z'
),
(
  'histcob_pay_af506b406f81351d', 'treatment', '9th Payment', '9th Payment', 'histcob_7ed4021862bbb3c7', '9707813942', 'Cooch Behar', 'NOSMINA KHATUN',
  '2025-11-21', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-21T10:23:00.000Z', '2025-11-21T10:23:00.000Z'
),
(
  'histcob_pay_8810d605cbaa0bad', 'treatment', 'Advance', 'Advance', 'histcob_c8d5ab9ecf12d08e', '7099900257', 'Cooch Behar', 'SOKINA BAGAM',
  '2025-07-11', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-11T10:15:00.000Z', '2025-07-11T10:15:00.000Z'
),
(
  'histcob_pay_3969f3f63674b950', 'treatment', '2nd Payment', '2nd Payment', 'histcob_c8d5ab9ecf12d08e', '7099900257', 'Cooch Behar', 'SOKINA BAGAM',
  '2025-07-14', '9000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-14T10:16:00.000Z', '2025-07-14T10:16:00.000Z'
),
(
  'histcob_pay_569c6ec65fedb366', 'treatment', '3rd Payment', '3rd Payment', 'histcob_c8d5ab9ecf12d08e', '7099900257', 'Cooch Behar', 'SOKINA BAGAM',
  '2025-07-28', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-28T10:17:00.000Z', '2025-07-28T10:17:00.000Z'
),
(
  'histcob_pay_bddc238671018bc3', 'treatment', '4th Payment', '4th Payment', 'histcob_c8d5ab9ecf12d08e', '7099900257', 'Cooch Behar', 'SOKINA BAGAM',
  '2025-08-08', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-08T10:18:00.000Z', '2025-08-08T10:18:00.000Z'
),
(
  'histcob_pay_94208f3cc05d7e81', 'treatment', 'Advance', 'Advance', 'histcob_3472df284bd95bf1', '8638464445', 'Cooch Behar', 'FAJULUR RAHAMAN',
  '2025-07-11', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-11T10:15:00.000Z', '2025-07-11T10:15:00.000Z'
),
(
  'histcob_pay_5837636faa6045d8', 'treatment', '2nd Payment', '2nd Payment', 'histcob_3472df284bd95bf1', '8638464445', 'Cooch Behar', 'FAJULUR RAHAMAN',
  '2025-07-18', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-18T10:16:00.000Z', '2025-07-18T10:16:00.000Z'
),
(
  'histcob_pay_fd0bcd2a95c48350', 'treatment', 'Advance', 'Advance', 'histcob_2a0cc72600e6daf5', '7636076903', 'Cooch Behar', 'SIR MANGAL CHANDI DAS',
  '2025-07-11', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-11T10:15:00.000Z', '2025-07-11T10:15:00.000Z'
),
(
  'histcob_pay_35fd78919f1e9176', 'treatment', '2nd Payment', '2nd Payment', 'histcob_2a0cc72600e6daf5', '7636076903', 'Cooch Behar', 'SIR MANGAL CHANDI DAS',
  '2025-07-18', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-18T10:16:00.000Z', '2025-07-18T10:16:00.000Z'
),
(
  'histcob_pay_a0336e82f8efba4d', 'treatment', 'Advance', 'Advance', 'histcob_1b220961f27467d2', '9025196675', 'Cooch Behar', 'SOFIKUL RAHAMAN',
  '2025-07-14', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-14T10:15:00.000Z', '2025-07-14T10:15:00.000Z'
),
(
  'histcob_pay_29297089e774f34f', 'treatment', 'Advance', 'Advance', 'histcob_93e263038005c15f', '7099563030', 'Cooch Behar', 'SADDAM HOSSAIN',
  '2025-07-18', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-18T10:15:00.000Z', '2025-07-18T10:15:00.000Z'
),
(
  'histcob_pay_41fa023c2b6bc5aa', 'treatment', '2nd Payment', '2nd Payment', 'histcob_93e263038005c15f', '7099563030', 'Cooch Behar', 'SADDAM HOSSAIN',
  '2025-07-21', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-21T10:16:00.000Z', '2025-07-21T10:16:00.000Z'
),
(
  'histcob_pay_2e2f12c42707d542', 'treatment', '3rd Payment', '3rd Payment', 'histcob_93e263038005c15f', '7099563030', 'Cooch Behar', 'SADDAM HOSSAIN',
  '2025-07-28', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-28T10:17:00.000Z', '2025-07-28T10:17:00.000Z'
),
(
  'histcob_pay_8ebfe2863c3898c1', 'treatment', '4th Payment', '4th Payment', 'histcob_93e263038005c15f', '7099563030', 'Cooch Behar', 'SADDAM HOSSAIN',
  '2025-08-04', '11500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-04T10:18:00.000Z', '2025-08-04T10:18:00.000Z'
),
(
  'histcob_pay_614b71585b9796f0', 'treatment', '5th Payment', '5th Payment', 'histcob_93e263038005c15f', '7099563030', 'Cooch Behar', 'SADDAM HOSSAIN',
  '2025-08-09', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-09T10:19:00.000Z', '2025-08-09T10:19:00.000Z'
),
(
  'histcob_pay_a811ebf5aef85f98', 'treatment', '6th Payment', '6th Payment', 'histcob_93e263038005c15f', '7099563030', 'Cooch Behar', 'SADDAM HOSSAIN',
  '2025-08-15', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-15T10:20:00.000Z', '2025-08-15T10:20:00.000Z'
),
(
  'histcob_pay_8808c958df49f389', 'treatment', '7th Payment', '7th Payment', 'histcob_93e263038005c15f', '7099563030', 'Cooch Behar', 'SADDAM HOSSAIN',
  '2025-08-22', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-22T10:21:00.000Z', '2025-08-22T10:21:00.000Z'
),
(
  'histcob_pay_385c392c2ec32d90', 'treatment', '8th Payment', '8th Payment', 'histcob_93e263038005c15f', '7099563030', 'Cooch Behar', 'SADDAM HOSSAIN',
  '2025-08-29', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-29T10:22:00.000Z', '2025-08-29T10:22:00.000Z'
),
(
  'histcob_pay_549e690d52024b11', 'treatment', '9th Payment', '9th Payment', 'histcob_93e263038005c15f', '7099563030', 'Cooch Behar', 'SADDAM HOSSAIN',
  '2025-09-05', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-05T10:23:00.000Z', '2025-09-05T10:23:00.000Z'
),
(
  'histcob_pay_84de4c708e4a0160', 'treatment', '10th Payment', '10th Payment', 'histcob_93e263038005c15f', '7099563030', 'Cooch Behar', 'SADDAM HOSSAIN',
  '2025-09-15', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-15T10:24:00.000Z', '2025-09-15T10:24:00.000Z'
),
(
  'histcob_pay_f2ace070bba83595', 'treatment', 'Advance', 'Advance', 'histcob_16666ebdf66c5c79', '7896219673', 'Cooch Behar', 'SAKIR AHAMMED',
  '2025-07-16', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-16T10:15:00.000Z', '2025-07-16T10:15:00.000Z'
),
(
  'histcob_pay_3cfe786f6c8b9bb8', 'treatment', '2nd Payment', '2nd Payment', 'histcob_16666ebdf66c5c79', '7896219673', 'Cooch Behar', 'SAKIR AHAMMED',
  '2025-07-18', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-18T10:16:00.000Z', '2025-07-18T10:16:00.000Z'
),
(
  'histcob_pay_43839bb0f3c26581', 'treatment', '3rd Payment', '3rd Payment', 'histcob_16666ebdf66c5c79', '7896219673', 'Cooch Behar', 'SAKIR AHAMMED',
  '2025-07-24', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-24T10:17:00.000Z', '2025-07-24T10:17:00.000Z'
),
(
  'histcob_pay_fcb23dcacf507e63', 'treatment', '4th Payment', '4th Payment', 'histcob_16666ebdf66c5c79', '7896219673', 'Cooch Behar', 'SAKIR AHAMMED',
  '2025-07-28', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-28T10:18:00.000Z', '2025-07-28T10:18:00.000Z'
),
(
  'histcob_pay_3963aac06fff4ad3', 'treatment', '5th Payment', '5th Payment', 'histcob_16666ebdf66c5c79', '7896219673', 'Cooch Behar', 'SAKIR AHAMMED',
  '2025-08-07', '8000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-07T10:19:00.000Z', '2025-08-07T10:19:00.000Z'
),
(
  'histcob_pay_d1acad9fe4a96cce', 'treatment', '6th Payment', '6th Payment', 'histcob_16666ebdf66c5c79', '7896219673', 'Cooch Behar', 'SAKIR AHAMMED',
  '2025-09-05', '4300', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-05T10:20:00.000Z', '2025-09-05T10:20:00.000Z'
),
(
  'histcob_pay_4cf4ed68b0101dab', 'treatment', 'Advance', 'Advance', 'histcob_48e5c27c8968e4ee', '6001155720', 'Cooch Behar', 'IJAJUK HOQUE',
  '2025-07-25', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-25T10:15:00.000Z', '2025-07-25T10:15:00.000Z'
),
(
  'histcob_pay_e296a5e156a8b6bb', 'treatment', 'Advance', 'Advance', 'histcob_e59fdcee4ddd5316', '8972852747', 'Cooch Behar', 'NiRMAL MAJHI',
  '2025-08-02', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:15:00.000Z', '2025-08-02T10:15:00.000Z'
),
(
  'histcob_pay_531a5816e04f1a30', 'treatment', 'Advance', 'Advance', 'histcob_a5045e8d6cb3af2d', '7006246957', 'Cooch Behar', 'NONDALAL NATH',
  '2025-08-04', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-04T10:15:00.000Z', '2025-08-04T10:15:00.000Z'
),
(
  'histcob_pay_76f7098ef5cdfdce', 'treatment', '2nd Payment', '2nd Payment', 'histcob_a5045e8d6cb3af2d', '7006246957', 'Cooch Behar', 'NONDALAL NATH',
  '2025-08-08', '6000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-08T10:16:00.000Z', '2025-08-08T10:16:00.000Z'
),
(
  'histcob_pay_3568760e1da59a39', 'treatment', '3rd Payment', '3rd Payment', 'histcob_a5045e8d6cb3af2d', '7006246957', 'Cooch Behar', 'NONDALAL NATH',
  '2025-08-11', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-11T10:17:00.000Z', '2025-08-11T10:17:00.000Z'
),
(
  'histcob_pay_1365692f032d9513', 'treatment', '4th Payment', '4th Payment', 'histcob_a5045e8d6cb3af2d', '7006246957', 'Cooch Behar', 'NONDALAL NATH',
  '2025-08-18', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-18T10:18:00.000Z', '2025-08-18T10:18:00.000Z'
),
(
  'histcob_pay_c3c55635dcf2ae7a', 'treatment', 'Advance', 'Advance', 'histcob_fab2ced4478e6ed6', '6002555376', 'Cooch Behar', 'MOINUL HOQUE',
  '2025-08-08', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-08T10:15:00.000Z', '2025-08-08T10:15:00.000Z'
),
(
  'histcob_pay_0c240de57dae860d', 'treatment', '2nd Payment', '2nd Payment', 'histcob_fab2ced4478e6ed6', '6002555376', 'Cooch Behar', 'MOINUL HOQUE',
  '2025-08-11', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-11T10:16:00.000Z', '2025-08-11T10:16:00.000Z'
),
(
  'histcob_pay_3deca2e574f6cfcc', 'treatment', '3rd Payment', '3rd Payment', 'histcob_fab2ced4478e6ed6', '6002555376', 'Cooch Behar', 'MOINUL HOQUE',
  '2025-08-18', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-18T10:17:00.000Z', '2025-08-18T10:17:00.000Z'
),
(
  'histcob_pay_8c57782b048f574a', 'treatment', '4th Payment', '4th Payment', 'histcob_fab2ced4478e6ed6', '6002555376', 'Cooch Behar', 'MOINUL HOQUE',
  '2025-08-25', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-25T10:18:00.000Z', '2025-08-25T10:18:00.000Z'
),
(
  'histcob_pay_4cb63f2cf8d795c2', 'treatment', '5th Payment', '5th Payment', 'histcob_fab2ced4478e6ed6', '6002555376', 'Cooch Behar', 'MOINUL HOQUE',
  '2025-09-01', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-01T10:19:00.000Z', '2025-09-01T10:19:00.000Z'
),
(
  'histcob_pay_e50da72b39fa2143', 'treatment', 'Advance', 'Advance', 'histcob_cc45353f1f79ec1c', '9309578296', 'Cooch Behar', 'SANDIP DAS',
  '2025-08-08', '25000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-08T10:15:00.000Z', '2025-08-08T10:15:00.000Z'
),
(
  'histcob_pay_5e760dcb3fe8187e', 'treatment', '2nd Payment', '2nd Payment', 'histcob_cc45353f1f79ec1c', '9309578296', 'Cooch Behar', 'SANDIP DAS',
  '2025-08-15', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-15T10:16:00.000Z', '2025-08-15T10:16:00.000Z'
),
(
  'histcob_pay_2395ed22407f5b04', 'treatment', '3rd Payment', '3rd Payment', 'histcob_cc45353f1f79ec1c', '9309578296', 'Cooch Behar', 'SANDIP DAS',
  '2025-08-25', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-25T10:17:00.000Z', '2025-08-25T10:17:00.000Z'
),
(
  'histcob_pay_0040adc9dfac9c79', 'treatment', 'Advance', 'Advance', 'histcob_b058ddba46aaf6fc', '8116492048', 'Cooch Behar', 'NITYAHARI DAS',
  '2025-08-08', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-08T10:15:00.000Z', '2025-08-08T10:15:00.000Z'
),
(
  'histcob_pay_b1c7b217ebaf752a', 'treatment', '2nd Payment', '2nd Payment', 'histcob_b058ddba46aaf6fc', '8116492048', 'Cooch Behar', 'NITYAHARI DAS',
  '2025-08-11', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-11T10:16:00.000Z', '2025-08-11T10:16:00.000Z'
),
(
  'histcob_pay_c35ad98179fa47bf', 'treatment', '3rd Payment', '3rd Payment', 'histcob_b058ddba46aaf6fc', '8116492048', 'Cooch Behar', 'NITYAHARI DAS',
  '2025-08-15', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-15T10:17:00.000Z', '2025-08-15T10:17:00.000Z'
),
(
  'histcob_pay_549a30d730b178c6', 'treatment', '4th Payment', '4th Payment', 'histcob_b058ddba46aaf6fc', '8116492048', 'Cooch Behar', 'NITYAHARI DAS',
  '2025-08-18', '8000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-18T10:18:00.000Z', '2025-08-18T10:18:00.000Z'
),
(
  'histcob_pay_562f47274e3e63ac', 'treatment', '5th Payment', '5th Payment', 'histcob_b058ddba46aaf6fc', '8116492048', 'Cooch Behar', 'NITYAHARI DAS',
  '2025-08-25', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-25T10:19:00.000Z', '2025-08-25T10:19:00.000Z'
),
(
  'histcob_pay_01fb8ec984321a1a', 'treatment', '6th Payment', '6th Payment', 'histcob_b058ddba46aaf6fc', '8116492048', 'Cooch Behar', 'NITYAHARI DAS',
  '2025-08-29', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-29T10:20:00.000Z', '2025-08-29T10:20:00.000Z'
),
(
  'histcob_pay_8613c0bf9e894b7d', 'treatment', '7th Payment', '7th Payment', 'histcob_b058ddba46aaf6fc', '8116492048', 'Cooch Behar', 'NITYAHARI DAS',
  '2025-09-08', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-08T10:21:00.000Z', '2025-09-08T10:21:00.000Z'
),
(
  'histcob_pay_8a7e08ce6e459fe1', 'treatment', '8th Payment', '8th Payment', 'histcob_b058ddba46aaf6fc', '8116492048', 'Cooch Behar', 'NITYAHARI DAS',
  '2025-09-15', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-15T10:22:00.000Z', '2025-09-15T10:22:00.000Z'
),
(
  'histcob_pay_7fa784a9138a847f', 'treatment', '9th Payment', '9th Payment', 'histcob_b058ddba46aaf6fc', '8116492048', 'Cooch Behar', 'NITYAHARI DAS',
  '2025-09-22', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-22T10:23:00.000Z', '2025-09-22T10:23:00.000Z'
),
(
  'histcob_pay_e4a4a3993aefabbc', 'treatment', '10th Payment', '10th Payment', 'histcob_b058ddba46aaf6fc', '8116492048', 'Cooch Behar', 'NITYAHARI DAS',
  '2025-10-03', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-03T10:24:00.000Z', '2025-10-03T10:24:00.000Z'
),
(
  'histcob_pay_ef408f8bf168d20b', 'treatment', '11th Payment', '11th Payment', 'histcob_b058ddba46aaf6fc', '8116492048', 'Cooch Behar', 'NITYAHARI DAS',
  '2025-10-13', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-13T10:25:00.000Z', '2025-10-13T10:25:00.000Z'
),
(
  'histcob_pay_1afebbe7f97945db', 'treatment', '12th Payment', '12th Payment', 'histcob_b058ddba46aaf6fc', '8116492048', 'Cooch Behar', 'NITYAHARI DAS',
  '2025-10-06', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-06T10:26:00.000Z', '2025-10-06T10:26:00.000Z'
),
(
  'histcob_pay_669dcaaf1e27df3d', 'treatment', '13th Payment', '13th Payment', 'histcob_b058ddba46aaf6fc', '8116492048', 'Cooch Behar', 'NITYAHARI DAS',
  '2025-10-22', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-22T10:27:00.000Z', '2025-10-22T10:27:00.000Z'
),
(
  'histcob_pay_16f8607a27cec652', 'treatment', '14th Payment', '14th Payment', 'histcob_b058ddba46aaf6fc', '8116492048', 'Cooch Behar', 'NITYAHARI DAS',
  '2025-10-31', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-31T10:28:00.000Z', '2025-10-31T10:28:00.000Z'
),
(
  'histcob_pay_f44ca9205ff4b5d4', 'treatment', '15th Payment', '15th Payment', 'histcob_b058ddba46aaf6fc', '8116492048', 'Cooch Behar', 'NITYAHARI DAS',
  '2025-11-03', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-03T10:29:00.000Z', '2025-11-03T10:29:00.000Z'
),
(
  'histcob_pay_1dc34ae141288dc0', 'treatment', '16th Payment', '16th Payment', 'histcob_b058ddba46aaf6fc', '8116492048', 'Cooch Behar', 'NITYAHARI DAS',
  '2025-12-22', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-22T10:30:00.000Z', '2025-12-22T10:30:00.000Z'
),
(
  'histcob_pay_5c8d0f51c7ab0515', 'treatment', '17th Payment', '17th Payment', 'histcob_b058ddba46aaf6fc', '8116492048', 'Cooch Behar', 'NITYAHARI DAS',
  '2025-12-26', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-26T10:31:00.000Z', '2025-12-26T10:31:00.000Z'
),
(
  'histcob_pay_599abdc80661408a', 'treatment', 'Advance', 'Advance', 'histcob_7c3a5085bdb5c5d1', '7810953907', 'Cooch Behar', 'SUJATA DAS',
  '2025-08-25', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-25T10:15:00.000Z', '2025-08-25T10:15:00.000Z'
),
(
  'histcob_pay_116df67c1dbddcc6', 'treatment', '2nd Payment', '2nd Payment', 'histcob_7c3a5085bdb5c5d1', '7810953907', 'Cooch Behar', 'SUJATA DAS',
  '2025-09-05', '200', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-05T10:16:00.000Z', '2025-09-05T10:16:00.000Z'
),
(
  'histcob_pay_963bb7aae6adcddd', 'treatment', '3rd Payment', '3rd Payment', 'histcob_7c3a5085bdb5c5d1', '7810953907', 'Cooch Behar', 'SUJATA DAS',
  '2025-09-19', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-19T10:17:00.000Z', '2025-09-19T10:17:00.000Z'
),
(
  'histcob_pay_c62b31264adbbcfd', 'treatment', 'Advance', 'Advance', 'histcob_c9bd49b3462a4bcd', '9474145911', 'Cooch Behar', 'JONEKA BIBI',
  '2025-08-23', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:15:00.000Z', '2025-08-23T10:15:00.000Z'
),
(
  'histcob_pay_398fd5b0d608fbc1', 'treatment', '2nd Payment', '2nd Payment', 'histcob_c9bd49b3462a4bcd', '9474145911', 'Cooch Behar', 'JONEKA BIBI',
  '2025-08-25', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-25T10:16:00.000Z', '2025-08-25T10:16:00.000Z'
),
(
  'histcob_pay_68962b7196344ffc', 'treatment', '3rd Payment', '3rd Payment', 'histcob_c9bd49b3462a4bcd', '9474145911', 'Cooch Behar', 'JONEKA BIBI',
  '2025-08-30', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:17:00.000Z', '2025-08-30T10:17:00.000Z'
),
(
  'histcob_pay_64f05170f32cf1dc', 'treatment', '4th Payment', '4th Payment', 'histcob_c9bd49b3462a4bcd', '9474145911', 'Cooch Behar', 'JONEKA BIBI',
  '2025-09-01', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-01T10:18:00.000Z', '2025-09-01T10:18:00.000Z'
),
(
  'histcob_pay_f0287c12dd56f585', 'treatment', '5th Payment', '5th Payment', 'histcob_c9bd49b3462a4bcd', '9474145911', 'Cooch Behar', 'JONEKA BIBI',
  '2025-09-08', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-08T10:19:00.000Z', '2025-09-08T10:19:00.000Z'
),
(
  'histcob_pay_94cb7413f9476541', 'treatment', '6th Payment', '6th Payment', 'histcob_c9bd49b3462a4bcd', '9474145911', 'Cooch Behar', 'JONEKA BIBI',
  '2025-09-12', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-12T10:20:00.000Z', '2025-09-12T10:20:00.000Z'
),
(
  'histcob_pay_d1dd85c7b2899fc0', 'treatment', '7th Payment', '7th Payment', 'histcob_c9bd49b3462a4bcd', '9474145911', 'Cooch Behar', 'JONEKA BIBI',
  '2025-09-22', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-22T10:21:00.000Z', '2025-09-22T10:21:00.000Z'
),
(
  'histcob_pay_a5bf27d3f56cd5c6', 'treatment', '8th Payment', '8th Payment', 'histcob_c9bd49b3462a4bcd', '9474145911', 'Cooch Behar', 'JONEKA BIBI',
  '2025-09-29', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-29T10:22:00.000Z', '2025-09-29T10:22:00.000Z'
),
(
  'histcob_pay_84fa63ef07bc095b', 'treatment', '9th Payment', '9th Payment', 'histcob_c9bd49b3462a4bcd', '9474145911', 'Cooch Behar', 'JONEKA BIBI',
  '2025-10-03', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-03T10:23:00.000Z', '2025-10-03T10:23:00.000Z'
),
(
  'histcob_pay_8ea34ae23f0aca3e', 'treatment', '10th Payment', '10th Payment', 'histcob_c9bd49b3462a4bcd', '9474145911', 'Cooch Behar', 'JONEKA BIBI',
  '2025-10-06', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-06T10:24:00.000Z', '2025-10-06T10:24:00.000Z'
),
(
  'histcob_pay_46a7237855378da5', 'treatment', '11th Payment', '11th Payment', 'histcob_c9bd49b3462a4bcd', '9474145911', 'Cooch Behar', 'JONEKA BIBI',
  '2025-10-10', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-10T10:25:00.000Z', '2025-10-10T10:25:00.000Z'
),
(
  'histcob_pay_631e9eeb572587df', 'treatment', '12th Payment', '12th Payment', 'histcob_c9bd49b3462a4bcd', '9474145911', 'Cooch Behar', 'JONEKA BIBI',
  '2025-10-13', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-13T10:26:00.000Z', '2025-10-13T10:26:00.000Z'
),
(
  'histcob_pay_8179b1fd1d2485d4', 'treatment', '13th Payment', '13th Payment', 'histcob_c9bd49b3462a4bcd', '9474145911', 'Cooch Behar', 'JONEKA BIBI',
  '2025-10-20', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-20T10:27:00.000Z', '2025-10-20T10:27:00.000Z'
),
(
  'histcob_pay_3c42ae869b8a28ec', 'treatment', '14th Payment', '14th Payment', 'histcob_c9bd49b3462a4bcd', '9474145911', 'Cooch Behar', 'JONEKA BIBI',
  '2025-10-24', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-24T10:28:00.000Z', '2025-10-24T10:28:00.000Z'
),
(
  'histcob_pay_e264870ffdc8c23e', 'treatment', 'Advance', 'Advance', 'histcob_bcf4561169e031d3', '9564015320', 'Cooch Behar', 'Swapna Sarkar',
  '2025-09-08', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-08T10:15:00.000Z', '2025-09-08T10:15:00.000Z'
),
(
  'histcob_pay_f51ee0ad7b200c18', 'treatment', '2nd Payment', '2nd Payment', 'histcob_bcf4561169e031d3', '9564015320', 'Cooch Behar', 'Swapna Sarkar',
  '2025-09-12', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-12T10:16:00.000Z', '2025-09-12T10:16:00.000Z'
),
(
  'histcob_pay_9259eb02d3dcb647', 'treatment', '3rd Payment', '3rd Payment', 'histcob_bcf4561169e031d3', '9564015320', 'Cooch Behar', 'Swapna Sarkar',
  '2025-09-15', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-15T10:17:00.000Z', '2025-09-15T10:17:00.000Z'
),
(
  'histcob_pay_f7549df7da92c32c', 'treatment', '4th Payment', '4th Payment', 'histcob_bcf4561169e031d3', '9564015320', 'Cooch Behar', 'Swapna Sarkar',
  '2025-09-19', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-19T10:18:00.000Z', '2025-09-19T10:18:00.000Z'
),
(
  'histcob_pay_f897ca3a59f65e62', 'treatment', '5th Payment', '5th Payment', 'histcob_bcf4561169e031d3', '9564015320', 'Cooch Behar', 'Swapna Sarkar',
  '2025-09-26', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-26T10:19:00.000Z', '2025-09-26T10:19:00.000Z'
),
(
  'histcob_pay_b4c5174d01dd4594', 'treatment', '6th Payment', '6th Payment', 'histcob_bcf4561169e031d3', '9564015320', 'Cooch Behar', 'Swapna Sarkar',
  '2025-10-06', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-06T10:20:00.000Z', '2025-10-06T10:20:00.000Z'
),
(
  'histcob_pay_f7530b5834ea8cb8', 'treatment', '7th Payment', '7th Payment', 'histcob_bcf4561169e031d3', '9564015320', 'Cooch Behar', 'Swapna Sarkar',
  '2025-10-10', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-10T10:21:00.000Z', '2025-10-10T10:21:00.000Z'
),
(
  'histcob_pay_2c2a4c184cdf2ed6', 'treatment', '8th Payment', '8th Payment', 'histcob_bcf4561169e031d3', '9564015320', 'Cooch Behar', 'Swapna Sarkar',
  '2025-10-13', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-13T10:22:00.000Z', '2025-10-13T10:22:00.000Z'
),
(
  'histcob_pay_e2497c10001382b4', 'treatment', '9th Payment', '9th Payment', 'histcob_bcf4561169e031d3', '9564015320', 'Cooch Behar', 'Swapna Sarkar',
  '2025-10-17', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-17T10:23:00.000Z', '2025-10-17T10:23:00.000Z'
),
(
  'histcob_pay_3380d7a4a1abea23', 'treatment', '10th Payment', '10th Payment', 'histcob_bcf4561169e031d3', '9564015320', 'Cooch Behar', 'Swapna Sarkar',
  '2025-10-24', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-24T10:24:00.000Z', '2025-10-24T10:24:00.000Z'
),
(
  'histcob_pay_d83078467eaf3cfb', 'treatment', '11th Payment', '11th Payment', 'histcob_bcf4561169e031d3', '9564015320', 'Cooch Behar', 'Swapna Sarkar',
  '2025-11-07', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-07T10:25:00.000Z', '2025-11-07T10:25:00.000Z'
),
(
  'histcob_pay_49f2d1626121055b', 'treatment', '12th Payment', '12th Payment', 'histcob_bcf4561169e031d3', '9564015320', 'Cooch Behar', 'Swapna Sarkar',
  '2025-11-10', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-10T10:26:00.000Z', '2025-11-10T10:26:00.000Z'
),
(
  'histcob_pay_6b0293b19671fe7d', 'treatment', 'Advance', 'Advance', 'histcob_72a20fdbe4302d35', '9864151048', 'Cooch Behar', 'ABDUL KADER',
  '2025-09-15', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-15T10:15:00.000Z', '2025-09-15T10:15:00.000Z'
),
(
  'histcob_pay_b51b9beac2ae4e6d', 'treatment', '2nd Payment', '2nd Payment', 'histcob_72a20fdbe4302d35', '9864151048', 'Cooch Behar', 'ABDUL KADER',
  '2025-09-19', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-19T10:16:00.000Z', '2025-09-19T10:16:00.000Z'
),
(
  'histcob_pay_c33f8fca932bc07d', 'treatment', '3rd Payment', '3rd Payment', 'histcob_72a20fdbe4302d35', '9864151048', 'Cooch Behar', 'ABDUL KADER',
  '2025-09-26', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-26T10:17:00.000Z', '2025-09-26T10:17:00.000Z'
),
(
  'histcob_pay_7753438b5febbac3', 'treatment', '4th Payment', '4th Payment', 'histcob_72a20fdbe4302d35', '9864151048', 'Cooch Behar', 'ABDUL KADER',
  '2025-09-29', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-29T10:18:00.000Z', '2025-09-29T10:18:00.000Z'
),
(
  'histcob_pay_a8f9e3c14e61d58a', 'treatment', '5th Payment', '5th Payment', 'histcob_72a20fdbe4302d35', '9864151048', 'Cooch Behar', 'ABDUL KADER',
  '2025-10-03', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-03T10:19:00.000Z', '2025-10-03T10:19:00.000Z'
),
(
  'histcob_pay_1522b2600078a91e', 'treatment', '6th Payment', '6th Payment', 'histcob_72a20fdbe4302d35', '9864151048', 'Cooch Behar', 'ABDUL KADER',
  '2025-10-10', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-10T10:20:00.000Z', '2025-10-10T10:20:00.000Z'
),
(
  'histcob_pay_a5212a5f953dd592', 'treatment', '7th Payment', '7th Payment', 'histcob_72a20fdbe4302d35', '9864151048', 'Cooch Behar', 'ABDUL KADER',
  '2025-10-13', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-13T10:21:00.000Z', '2025-10-13T10:21:00.000Z'
),
(
  'histcob_pay_3ba1bc8b41043eea', 'treatment', '8th Payment', '8th Payment', 'histcob_72a20fdbe4302d35', '9864151048', 'Cooch Behar', 'ABDUL KADER',
  '2025-09-20', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:22:00.000Z', '2025-09-20T10:22:00.000Z'
),
(
  'histcob_pay_f32b73fffa0bf617', 'treatment', '9th Payment', '9th Payment', 'histcob_72a20fdbe4302d35', '9864151048', 'Cooch Behar', 'ABDUL KADER',
  '2025-10-27', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-27T10:23:00.000Z', '2025-10-27T10:23:00.000Z'
),
(
  'histcob_pay_67bef3ff3d517c4f', 'treatment', '10th Payment', '10th Payment', 'histcob_72a20fdbe4302d35', '9864151048', 'Cooch Behar', 'ABDUL KADER',
  '2025-10-10', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-10T10:24:00.000Z', '2025-10-10T10:24:00.000Z'
),
(
  'histcob_pay_9b3519e9dbf8a572', 'treatment', '11th Payment', '11th Payment', 'histcob_72a20fdbe4302d35', '9864151048', 'Cooch Behar', 'ABDUL KADER',
  '2025-10-17', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-17T10:25:00.000Z', '2025-10-17T10:25:00.000Z'
),
(
  'histcob_pay_07350dedf50d747a', 'treatment', '12th Payment', '12th Payment', 'histcob_72a20fdbe4302d35', '9864151048', 'Cooch Behar', 'ABDUL KADER',
  '2025-10-24', '3500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-24T10:26:00.000Z', '2025-10-24T10:26:00.000Z'
),
(
  'histcob_pay_d9c58befb715f0d0', 'treatment', '13th Payment', '13th Payment', 'histcob_72a20fdbe4302d35', '9864151048', 'Cooch Behar', 'ABDUL KADER',
  '2025-12-01', '2500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-01T10:27:00.000Z', '2025-12-01T10:27:00.000Z'
),
(
  'histcob_pay_d127a41ab235cd27', 'treatment', 'Advance', 'Advance', 'histcob_96abd224c4382a71', '7076916258', 'Cooch Behar', 'Dilip Barman',
  '2025-09-15', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-15T10:15:00.000Z', '2025-09-15T10:15:00.000Z'
),
(
  'histcob_pay_3ab395bd47ce69b6', 'treatment', '2nd Payment', '2nd Payment', 'histcob_96abd224c4382a71', '7076916258', 'Cooch Behar', 'Dilip Barman',
  '2025-09-19', '20000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-19T10:16:00.000Z', '2025-09-19T10:16:00.000Z'
),
(
  'histcob_pay_11230c313ee068c4', 'treatment', '3rd Payment', '3rd Payment', 'histcob_96abd224c4382a71', '7076916258', 'Cooch Behar', 'Dilip Barman',
  '2025-09-22', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-22T10:17:00.000Z', '2025-09-22T10:17:00.000Z'
),
(
  'histcob_pay_9c8141b0307725f3', 'treatment', '4th Payment', '4th Payment', 'histcob_96abd224c4382a71', '7076916258', 'Cooch Behar', 'Dilip Barman',
  '2025-09-26', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-26T10:18:00.000Z', '2025-09-26T10:18:00.000Z'
),
(
  'histcob_pay_80faa469920ca8cb', 'treatment', '5th Payment', '5th Payment', 'histcob_96abd224c4382a71', '7076916258', 'Cooch Behar', 'Dilip Barman',
  '2025-09-29', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-29T10:19:00.000Z', '2025-09-29T10:19:00.000Z'
),
(
  'histcob_pay_992eeaae32d9db78', 'treatment', '6th Payment', '6th Payment', 'histcob_96abd224c4382a71', '7076916258', 'Cooch Behar', 'Dilip Barman',
  '2025-10-03', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-03T10:20:00.000Z', '2025-10-03T10:20:00.000Z'
),
(
  'histcob_pay_e515634b1fd1adaa', 'treatment', '7th Payment', '7th Payment', 'histcob_96abd224c4382a71', '7076916258', 'Cooch Behar', 'Dilip Barman',
  '2025-10-06', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-06T10:21:00.000Z', '2025-10-06T10:21:00.000Z'
),
(
  'histcob_pay_398a55a7ea0772b2', 'treatment', '8th Payment', '8th Payment', 'histcob_96abd224c4382a71', '7076916258', 'Cooch Behar', 'Dilip Barman',
  '2025-10-10', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-10T10:22:00.000Z', '2025-10-10T10:22:00.000Z'
),
(
  'histcob_pay_7ad6f762ada9b006', 'treatment', '9th Payment', '9th Payment', 'histcob_96abd224c4382a71', '7076916258', 'Cooch Behar', 'Dilip Barman',
  '2025-10-13', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-13T10:23:00.000Z', '2025-10-13T10:23:00.000Z'
),
(
  'histcob_pay_59531e984b272cfb', 'treatment', '10th Payment', '10th Payment', 'histcob_96abd224c4382a71', '7076916258', 'Cooch Behar', 'Dilip Barman',
  '2025-10-17', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-17T10:24:00.000Z', '2025-10-17T10:24:00.000Z'
),
(
  'histcob_pay_a4b65b2401145c6c', 'treatment', '11th Payment', '11th Payment', 'histcob_96abd224c4382a71', '7076916258', 'Cooch Behar', 'Dilip Barman',
  '2025-10-20', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-20T10:25:00.000Z', '2025-10-20T10:25:00.000Z'
),
(
  'histcob_pay_13a825c11f26f8fb', 'treatment', '12th Payment', '12th Payment', 'histcob_96abd224c4382a71', '7076916258', 'Cooch Behar', 'Dilip Barman',
  '2025-10-24', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-24T10:26:00.000Z', '2025-10-24T10:26:00.000Z'
),
(
  'histcob_pay_345127fd66cf9feb', 'treatment', '13th Payment', '13th Payment', 'histcob_96abd224c4382a71', '7076916258', 'Cooch Behar', 'Dilip Barman',
  '2025-10-31', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-31T10:27:00.000Z', '2025-10-31T10:27:00.000Z'
),
(
  'histcob_pay_2e576f0862521cbd', 'treatment', '14th Payment', '14th Payment', 'histcob_96abd224c4382a71', '7076916258', 'Cooch Behar', 'Dilip Barman',
  '2025-11-03', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-03T10:28:00.000Z', '2025-11-03T10:28:00.000Z'
),
(
  'histcob_pay_226b7f03ef7ad605', 'treatment', '15th Payment', '15th Payment', 'histcob_96abd224c4382a71', '7076916258', 'Cooch Behar', 'Dilip Barman',
  '2025-11-10', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-10T10:29:00.000Z', '2025-11-10T10:29:00.000Z'
),
(
  'histcob_pay_cfebc743e9520ed0', 'treatment', 'Advance', 'Advance', 'histcob_5daf9ec32112b024', '8638342633', 'Cooch Behar', 'HAIDAR ALI',
  '2025-09-15', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-15T10:15:00.000Z', '2025-09-15T10:15:00.000Z'
),
(
  'histcob_pay_ae0f04c63b05dd62', 'treatment', '2nd Payment', '2nd Payment', 'histcob_5daf9ec32112b024', '8638342633', 'Cooch Behar', 'HAIDAR ALI',
  '2025-09-19', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-19T10:16:00.000Z', '2025-09-19T10:16:00.000Z'
),
(
  'histcob_pay_ea3cc0aa0eb847dc', 'treatment', '3rd Payment', '3rd Payment', 'histcob_5daf9ec32112b024', '8638342633', 'Cooch Behar', 'HAIDAR ALI',
  '2025-09-22', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-22T10:17:00.000Z', '2025-09-22T10:17:00.000Z'
),
(
  'histcob_pay_955126650c18b0f5', 'treatment', '4th Payment', '4th Payment', 'histcob_5daf9ec32112b024', '8638342633', 'Cooch Behar', 'HAIDAR ALI',
  '2025-09-29', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-29T10:18:00.000Z', '2025-09-29T10:18:00.000Z'
),
(
  'histcob_pay_995d8e5a4f631d8c', 'treatment', '5th Payment', '5th Payment', 'histcob_5daf9ec32112b024', '8638342633', 'Cooch Behar', 'HAIDAR ALI',
  '2025-10-06', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-06T10:19:00.000Z', '2025-10-06T10:19:00.000Z'
),
(
  'histcob_pay_3c4d6029a70cbf27', 'treatment', '6th Payment', '6th Payment', 'histcob_5daf9ec32112b024', '8638342633', 'Cooch Behar', 'HAIDAR ALI',
  '2025-10-10', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-10T10:20:00.000Z', '2025-10-10T10:20:00.000Z'
),
(
  'histcob_pay_e972582d70ee6de4', 'treatment', '7th Payment', '7th Payment', 'histcob_5daf9ec32112b024', '8638342633', 'Cooch Behar', 'HAIDAR ALI',
  '2025-10-13', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-13T10:21:00.000Z', '2025-10-13T10:21:00.000Z'
),
(
  'histcob_pay_6986e5c50f286c47', 'treatment', '8th Payment', '8th Payment', 'histcob_5daf9ec32112b024', '8638342633', 'Cooch Behar', 'HAIDAR ALI',
  '2025-10-20', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-20T10:22:00.000Z', '2025-10-20T10:22:00.000Z'
),
(
  'histcob_pay_99d3d08629b02715', 'treatment', '9th Payment', '9th Payment', 'histcob_5daf9ec32112b024', '8638342633', 'Cooch Behar', 'HAIDAR ALI',
  '2025-10-27', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-27T10:23:00.000Z', '2025-10-27T10:23:00.000Z'
),
(
  'histcob_pay_f8fc970a54362000', 'treatment', '10th Payment', '10th Payment', 'histcob_5daf9ec32112b024', '8638342633', 'Cooch Behar', 'HAIDAR ALI',
  '2025-11-07', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-07T10:24:00.000Z', '2025-11-07T10:24:00.000Z'
),
(
  'histcob_pay_bac5d77488be29ed', 'treatment', 'Advance', 'Advance', 'histcob_f6e3c118955fc251', '7864060845', 'Cooch Behar', 'Jamiran Bibi',
  '2025-10-03', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-03T10:15:00.000Z', '2025-10-03T10:15:00.000Z'
),
(
  'histcob_pay_f053f19eecaf9372', 'treatment', '2nd Payment', '2nd Payment', 'histcob_f6e3c118955fc251', '7864060845', 'Cooch Behar', 'Jamiran Bibi',
  '2025-10-06', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-06T10:16:00.000Z', '2025-10-06T10:16:00.000Z'
),
(
  'histcob_pay_481e6a04f163851e', 'treatment', '3rd Payment', '3rd Payment', 'histcob_f6e3c118955fc251', '7864060845', 'Cooch Behar', 'Jamiran Bibi',
  '2025-10-10', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-10T10:17:00.000Z', '2025-10-10T10:17:00.000Z'
),
(
  'histcob_pay_8e60243df0f949cd', 'treatment', '4th Payment', '4th Payment', 'histcob_f6e3c118955fc251', '7864060845', 'Cooch Behar', 'Jamiran Bibi',
  '2025-01-17', '3500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-17T10:18:00.000Z', '2025-01-17T10:18:00.000Z'
),
(
  'histcob_pay_7a1a442486f3ff2e', 'treatment', '5th Payment', '5th Payment', 'histcob_f6e3c118955fc251', '7864060845', 'Cooch Behar', 'Jamiran Bibi',
  '2025-10-20', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-20T10:19:00.000Z', '2025-10-20T10:19:00.000Z'
),
(
  'histcob_pay_163c71e9490a3eaa', 'treatment', '6th Payment', '6th Payment', 'histcob_f6e3c118955fc251', '7864060845', 'Cooch Behar', 'Jamiran Bibi',
  '2025-10-24', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-24T10:20:00.000Z', '2025-10-24T10:20:00.000Z'
),
(
  'histcob_pay_7704f87c9a238b22', 'treatment', '7th Payment', '7th Payment', 'histcob_f6e3c118955fc251', '7864060845', 'Cooch Behar', 'Jamiran Bibi',
  '2025-10-27', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-27T10:21:00.000Z', '2025-10-27T10:21:00.000Z'
),
(
  'histcob_pay_096a53f562ff52ed', 'treatment', '8th Payment', '8th Payment', 'histcob_f6e3c118955fc251', '7864060845', 'Cooch Behar', 'Jamiran Bibi',
  '2025-10-31', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-31T10:22:00.000Z', '2025-10-31T10:22:00.000Z'
),
(
  'histcob_pay_ae0c67c97f4d1257', 'treatment', '9th Payment', '9th Payment', 'histcob_f6e3c118955fc251', '7864060845', 'Cooch Behar', 'Jamiran Bibi',
  '2025-11-03', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-03T10:23:00.000Z', '2025-11-03T10:23:00.000Z'
),
(
  'histcob_pay_442423e5fb0346ee', 'treatment', '10th Payment', '10th Payment', 'histcob_f6e3c118955fc251', '7864060845', 'Cooch Behar', 'Jamiran Bibi',
  '2025-11-10', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-10T10:24:00.000Z', '2025-11-10T10:24:00.000Z'
),
(
  'histcob_pay_9e58e8b7222cce59', 'treatment', '11th Payment', '11th Payment', 'histcob_f6e3c118955fc251', '7864060845', 'Cooch Behar', 'Jamiran Bibi',
  '2025-11-17', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-17T10:25:00.000Z', '2025-11-17T10:25:00.000Z'
),
(
  'histcob_pay_6222df8db399ccd2', 'treatment', '12th Payment', '12th Payment', 'histcob_f6e3c118955fc251', '7864060845', 'Cooch Behar', 'Jamiran Bibi',
  '2025-11-24', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-24T10:26:00.000Z', '2025-11-24T10:26:00.000Z'
),
(
  'histcob_pay_be2149ed6750f5bb', 'treatment', '13th Payment', '13th Payment', 'histcob_f6e3c118955fc251', '7864060845', 'Cooch Behar', 'Jamiran Bibi',
  '2025-12-01', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-01T10:27:00.000Z', '2025-12-01T10:27:00.000Z'
),
(
  'histcob_pay_48c79eac4ee67dcd', 'treatment', '14th Payment', '14th Payment', 'histcob_f6e3c118955fc251', '7864060845', 'Cooch Behar', 'Jamiran Bibi',
  '2025-12-08', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-08T10:28:00.000Z', '2025-12-08T10:28:00.000Z'
),
(
  'histcob_pay_3306b8d2549c2359', 'treatment', 'Advance', 'Advance', 'histcob_bd2af8792bd4e4c6', '7029578077', 'Cooch Behar', 'HIMANI BARMAN',
  '2025-10-06', '8000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-06T10:15:00.000Z', '2025-10-06T10:15:00.000Z'
),
(
  'histcob_pay_8ee9255621a6324b', 'treatment', '2nd Payment', '2nd Payment', 'histcob_bd2af8792bd4e4c6', '7029578077', 'Cooch Behar', 'HIMANI BARMAN',
  '2025-10-10', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-10T10:16:00.000Z', '2025-10-10T10:16:00.000Z'
),
(
  'histcob_pay_e49a6323a339cadc', 'treatment', '3rd Payment', '3rd Payment', 'histcob_bd2af8792bd4e4c6', '7029578077', 'Cooch Behar', 'HIMANI BARMAN',
  '2025-10-13', '3500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-13T10:17:00.000Z', '2025-10-13T10:17:00.000Z'
),
(
  'histcob_pay_385ac3ebf1117f43', 'treatment', '4th Payment', '4th Payment', 'histcob_bd2af8792bd4e4c6', '7029578077', 'Cooch Behar', 'HIMANI BARMAN',
  '2025-10-17', '3500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-17T10:18:00.000Z', '2025-10-17T10:18:00.000Z'
),
(
  'histcob_pay_ee223f92d86e4dcc', 'treatment', '5th Payment', '5th Payment', 'histcob_bd2af8792bd4e4c6', '7029578077', 'Cooch Behar', 'HIMANI BARMAN',
  '2025-10-20', '2500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-20T10:19:00.000Z', '2025-10-20T10:19:00.000Z'
),
(
  'histcob_pay_434ed6368dbadfdb', 'treatment', '6th Payment', '6th Payment', 'histcob_bd2af8792bd4e4c6', '7029578077', 'Cooch Behar', 'HIMANI BARMAN',
  '2025-10-24', '4500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-24T10:20:00.000Z', '2025-10-24T10:20:00.000Z'
),
(
  'histcob_pay_6855a03aedf91bbe', 'treatment', '7th Payment', '7th Payment', 'histcob_bd2af8792bd4e4c6', '7029578077', 'Cooch Behar', 'HIMANI BARMAN',
  '2025-10-27', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-27T10:21:00.000Z', '2025-10-27T10:21:00.000Z'
),
(
  'histcob_pay_ce4ecc7943d8b34f', 'treatment', '8th Payment', '8th Payment', 'histcob_bd2af8792bd4e4c6', '7029578077', 'Cooch Behar', 'HIMANI BARMAN',
  '2025-10-31', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-31T10:22:00.000Z', '2025-10-31T10:22:00.000Z'
),
(
  'histcob_pay_30a95dce9ff7167f', 'treatment', '9th Payment', '9th Payment', 'histcob_bd2af8792bd4e4c6', '7029578077', 'Cooch Behar', 'HIMANI BARMAN',
  '2025-11-03', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-03T10:23:00.000Z', '2025-11-03T10:23:00.000Z'
),
(
  'histcob_pay_33935b81504dbc9c', 'treatment', '10th Payment', '10th Payment', 'histcob_bd2af8792bd4e4c6', '7029578077', 'Cooch Behar', 'HIMANI BARMAN',
  '2025-11-07', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-07T10:24:00.000Z', '2025-11-07T10:24:00.000Z'
),
(
  'histcob_pay_6be850c75a3232c0', 'treatment', '11th Payment', '11th Payment', 'histcob_bd2af8792bd4e4c6', '7029578077', 'Cooch Behar', 'HIMANI BARMAN',
  '2025-11-10', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-10T10:25:00.000Z', '2025-11-10T10:25:00.000Z'
),
(
  'histcob_pay_da4c7b5a285b37af', 'treatment', '12th Payment', '12th Payment', 'histcob_bd2af8792bd4e4c6', '7029578077', 'Cooch Behar', 'HIMANI BARMAN',
  '2025-11-14', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-14T10:26:00.000Z', '2025-11-14T10:26:00.000Z'
),
(
  'histcob_pay_5d2e5ea7cf726937', 'treatment', '13th Payment', '13th Payment', 'histcob_bd2af8792bd4e4c6', '7029578077', 'Cooch Behar', 'HIMANI BARMAN',
  '2025-11-17', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-17T10:27:00.000Z', '2025-11-17T10:27:00.000Z'
),
(
  'histcob_pay_d8afa64ae69d3799', 'treatment', '14th Payment', '14th Payment', 'histcob_bd2af8792bd4e4c6', '7029578077', 'Cooch Behar', 'HIMANI BARMAN',
  '2025-11-21', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-21T10:28:00.000Z', '2025-11-21T10:28:00.000Z'
),
(
  'histcob_pay_1e17955242ab5be9', 'treatment', '15th Payment', '15th Payment', 'histcob_bd2af8792bd4e4c6', '7029578077', 'Cooch Behar', 'HIMANI BARMAN',
  '2025-11-24', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-24T10:29:00.000Z', '2025-11-24T10:29:00.000Z'
),
(
  'histcob_pay_4d84add0ad56bb56', 'treatment', '16th Payment', '16th Payment', 'histcob_bd2af8792bd4e4c6', '7029578077', 'Cooch Behar', 'HIMANI BARMAN',
  '2026-01-19', '100', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-19T10:30:00.000Z', '2026-01-19T10:30:00.000Z'
),
(
  'histcob_pay_dd0b7268a34e50de', 'treatment', '17th Payment', '17th Payment', 'histcob_bd2af8792bd4e4c6', '7029578077', 'Cooch Behar', 'HIMANI BARMAN',
  '2026-02-13', '400', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-13T10:31:00.000Z', '2026-02-13T10:31:00.000Z'
),
(
  'histcob_pay_ed6d259f83dc131a', 'treatment', 'Advance', 'Advance', 'histcob_83244ad1b9913bfe', '9895655656', 'Cooch Behar', 'KHOGENDRA BARMAN',
  '2025-10-10', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-10T10:15:00.000Z', '2025-10-10T10:15:00.000Z'
),
(
  'histcob_pay_8e592aeb858bbc43', 'treatment', '2nd Payment', '2nd Payment', 'histcob_83244ad1b9913bfe', '9895655656', 'Cooch Behar', 'KHOGENDRA BARMAN',
  '2025-10-13', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-13T10:16:00.000Z', '2025-10-13T10:16:00.000Z'
),
(
  'histcob_pay_a4bed94da6a6a084', 'treatment', '3rd Payment', '3rd Payment', 'histcob_83244ad1b9913bfe', '9895655656', 'Cooch Behar', 'KHOGENDRA BARMAN',
  '2025-10-24', '2500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-24T10:17:00.000Z', '2025-10-24T10:17:00.000Z'
),
(
  'histcob_pay_9078ceff562908e7', 'treatment', '4th Payment', '4th Payment', 'histcob_83244ad1b9913bfe', '9895655656', 'Cooch Behar', 'KHOGENDRA BARMAN',
  '2025-11-28', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-28T10:18:00.000Z', '2025-11-28T10:18:00.000Z'
),
(
  'histcob_pay_1499a51294cf0dbc', 'treatment', '5th Payment', '5th Payment', 'histcob_83244ad1b9913bfe', '9895655656', 'Cooch Behar', 'KHOGENDRA BARMAN',
  '2025-12-01', '2500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-01T10:19:00.000Z', '2025-12-01T10:19:00.000Z'
),
(
  'histcob_pay_7576b7b318b669cf', 'treatment', 'Advance', 'Advance', 'histcob_6cf03d81db0e78b1', '6000092308', 'Cooch Behar', 'KABIR AHAMMED',
  '2025-10-10', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-10T10:15:00.000Z', '2025-10-10T10:15:00.000Z'
),
(
  'histcob_pay_0ccedee8f1a4d690', 'treatment', '2nd Payment', '2nd Payment', 'histcob_6cf03d81db0e78b1', '6000092308', 'Cooch Behar', 'KABIR AHAMMED',
  '2025-10-13', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-13T10:16:00.000Z', '2025-10-13T10:16:00.000Z'
),
(
  'histcob_pay_308097f2d5528247', 'treatment', '3rd Payment', '3rd Payment', 'histcob_6cf03d81db0e78b1', '6000092308', 'Cooch Behar', 'KABIR AHAMMED',
  '2025-10-17', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-17T10:17:00.000Z', '2025-10-17T10:17:00.000Z'
),
(
  'histcob_pay_b3ce3a48ac0c4d6e', 'treatment', '4th Payment', '4th Payment', 'histcob_6cf03d81db0e78b1', '6000092308', 'Cooch Behar', 'KABIR AHAMMED',
  '2025-10-20', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-20T10:18:00.000Z', '2025-10-20T10:18:00.000Z'
),
(
  'histcob_pay_46757a67d5d157e0', 'treatment', '5th Payment', '5th Payment', 'histcob_6cf03d81db0e78b1', '6000092308', 'Cooch Behar', 'KABIR AHAMMED',
  '2025-10-24', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-24T10:19:00.000Z', '2025-10-24T10:19:00.000Z'
),
(
  'histcob_pay_1158b694daa4a0bd', 'treatment', '6th Payment', '6th Payment', 'histcob_6cf03d81db0e78b1', '6000092308', 'Cooch Behar', 'KABIR AHAMMED',
  '2025-10-27', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-27T10:20:00.000Z', '2025-10-27T10:20:00.000Z'
),
(
  'histcob_pay_1420f016fae40216', 'treatment', '7th Payment', '7th Payment', 'histcob_6cf03d81db0e78b1', '6000092308', 'Cooch Behar', 'KABIR AHAMMED',
  '2025-10-31', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-31T10:21:00.000Z', '2025-10-31T10:21:00.000Z'
),
(
  'histcob_pay_1ab0c2fb23e8bf97', 'treatment', '8th Payment', '8th Payment', 'histcob_6cf03d81db0e78b1', '6000092308', 'Cooch Behar', 'KABIR AHAMMED',
  '2025-11-03', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-03T10:22:00.000Z', '2025-11-03T10:22:00.000Z'
),
(
  'histcob_pay_54a498c1932f4d7e', 'treatment', '9th Payment', '9th Payment', 'histcob_6cf03d81db0e78b1', '6000092308', 'Cooch Behar', 'KABIR AHAMMED',
  '2025-11-07', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-07T10:23:00.000Z', '2025-11-07T10:23:00.000Z'
),
(
  'histcob_pay_6fe8eecc4ccfa952', 'treatment', '10th Payment', '10th Payment', 'histcob_6cf03d81db0e78b1', '6000092308', 'Cooch Behar', 'KABIR AHAMMED',
  '2025-11-14', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-14T10:24:00.000Z', '2025-11-14T10:24:00.000Z'
),
(
  'histcob_pay_9330436b415fbd51', 'treatment', '11th Payment', '11th Payment', 'histcob_6cf03d81db0e78b1', '6000092308', 'Cooch Behar', 'KABIR AHAMMED',
  '2025-11-17', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-17T10:25:00.000Z', '2025-11-17T10:25:00.000Z'
),
(
  'histcob_pay_5c76f04e0b9faa73', 'treatment', 'Advance', 'Advance', 'histcob_4a5176c4cc59ad47', '9593682252', 'Cooch Behar', 'RATAN SARKAR',
  '2025-10-17', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-17T10:15:00.000Z', '2025-10-17T10:15:00.000Z'
),
(
  'histcob_pay_6869fc1477540e02', 'treatment', '2nd Payment', '2nd Payment', 'histcob_4a5176c4cc59ad47', '9593682252', 'Cooch Behar', 'RATAN SARKAR',
  '2025-10-24', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-24T10:16:00.000Z', '2025-10-24T10:16:00.000Z'
),
(
  'histcob_pay_98a7c09fd20d544f', 'treatment', '3rd Payment', '3rd Payment', 'histcob_4a5176c4cc59ad47', '9593682252', 'Cooch Behar', 'RATAN SARKAR',
  '2025-11-31', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-31T10:17:00.000Z', '2025-11-31T10:17:00.000Z'
),
(
  'histcob_pay_6229d9f1ea1d59fa', 'treatment', '4th Payment', '4th Payment', 'histcob_4a5176c4cc59ad47', '9593682252', 'Cooch Behar', 'RATAN SARKAR',
  '2025-11-07', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-07T10:18:00.000Z', '2025-11-07T10:18:00.000Z'
),
(
  'histcob_pay_6833900a26ba4b86', 'treatment', '5th Payment', '5th Payment', 'histcob_4a5176c4cc59ad47', '9593682252', 'Cooch Behar', 'RATAN SARKAR',
  '2025-11-14', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-14T10:19:00.000Z', '2025-11-14T10:19:00.000Z'
),
(
  'histcob_pay_f978233d902c6643', 'treatment', '6th Payment', '6th Payment', 'histcob_4a5176c4cc59ad47', '9593682252', 'Cooch Behar', 'RATAN SARKAR',
  '2025-11-21', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-21T10:20:00.000Z', '2025-11-21T10:20:00.000Z'
),
(
  'histcob_pay_e345a213eae2c333', 'treatment', '7th Payment', '7th Payment', 'histcob_4a5176c4cc59ad47', '9593682252', 'Cooch Behar', 'RATAN SARKAR',
  '2025-12-01', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-01T10:21:00.000Z', '2025-12-01T10:21:00.000Z'
),
(
  'histcob_pay_be2b6a971933457d', 'treatment', '8th Payment', '8th Payment', 'histcob_4a5176c4cc59ad47', '9593682252', 'Cooch Behar', 'RATAN SARKAR',
  '2025-12-08', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-08T10:22:00.000Z', '2025-12-08T10:22:00.000Z'
),
(
  'histcob_pay_ce7687bb6c90aa12', 'treatment', '9th Payment', '9th Payment', 'histcob_4a5176c4cc59ad47', '9593682252', 'Cooch Behar', 'RATAN SARKAR',
  '2025-12-22', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-22T10:23:00.000Z', '2025-12-22T10:23:00.000Z'
),
(
  'histcob_pay_86ecc30df0dd7861', 'treatment', '10th Payment', '10th Payment', 'histcob_4a5176c4cc59ad47', '9593682252', 'Cooch Behar', 'RATAN SARKAR',
  '2026-01-16', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-16T10:24:00.000Z', '2026-01-16T10:24:00.000Z'
),
(
  'histcob_pay_5c70cb72242e0e5d', 'treatment', 'Advance', 'Advance', 'histcob_60e343857b64d688', '9679992827', 'Cooch Behar', 'JANNATUL HOSSAIN',
  '2025-10-27', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-27T10:15:00.000Z', '2025-10-27T10:15:00.000Z'
),
(
  'histcob_pay_11d1db4d4e208f68', 'treatment', '2nd Payment', '2nd Payment', 'histcob_60e343857b64d688', '9679992827', 'Cooch Behar', 'JANNATUL HOSSAIN',
  '2025-10-24', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-24T10:16:00.000Z', '2025-10-24T10:16:00.000Z'
),
(
  'histcob_pay_30d2c80144231bd8', 'treatment', '3rd Payment', '3rd Payment', 'histcob_60e343857b64d688', '9679992827', 'Cooch Behar', 'JANNATUL HOSSAIN',
  '2025-10-31', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-31T10:17:00.000Z', '2025-10-31T10:17:00.000Z'
),
(
  'histcob_pay_4d49b140de838e52', 'treatment', '4th Payment', '4th Payment', 'histcob_60e343857b64d688', '9679992827', 'Cooch Behar', 'JANNATUL HOSSAIN',
  '2025-11-07', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-07T10:18:00.000Z', '2025-11-07T10:18:00.000Z'
),
(
  'histcob_pay_4cd30edf6523a8ad', 'treatment', '5th Payment', '5th Payment', 'histcob_60e343857b64d688', '9679992827', 'Cooch Behar', 'JANNATUL HOSSAIN',
  '2025-11-14', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-14T10:19:00.000Z', '2025-11-14T10:19:00.000Z'
),
(
  'histcob_pay_ce2f0fbeefa9afe9', 'treatment', '6th Payment', '6th Payment', 'histcob_60e343857b64d688', '9679992827', 'Cooch Behar', 'JANNATUL HOSSAIN',
  '2025-11-21', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-21T10:20:00.000Z', '2025-11-21T10:20:00.000Z'
),
(
  'histcob_pay_642441d84cf4aa3f', 'treatment', '7th Payment', '7th Payment', 'histcob_60e343857b64d688', '9679992827', 'Cooch Behar', 'JANNATUL HOSSAIN',
  '2025-11-28', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-28T10:21:00.000Z', '2025-11-28T10:21:00.000Z'
),
(
  'histcob_pay_b8680050d6cb5816', 'treatment', '8th Payment', '8th Payment', 'histcob_60e343857b64d688', '9679992827', 'Cooch Behar', 'JANNATUL HOSSAIN',
  '2025-12-05', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-05T10:22:00.000Z', '2025-12-05T10:22:00.000Z'
),
(
  'histcob_pay_6d0c77a9fbb2dbe0', 'treatment', '9th Payment', '9th Payment', 'histcob_60e343857b64d688', '9679992827', 'Cooch Behar', 'JANNATUL HOSSAIN',
  '2025-12-26', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-26T10:23:00.000Z', '2025-12-26T10:23:00.000Z'
),
(
  'histcob_pay_263e02c590524b2b', 'treatment', '10th Payment', '10th Payment', 'histcob_60e343857b64d688', '9679992827', 'Cooch Behar', 'JANNATUL HOSSAIN',
  '2026-01-19', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-19T10:24:00.000Z', '2026-01-19T10:24:00.000Z'
),
(
  'histcob_pay_2b992a49ab7af3d0', 'treatment', 'Advance', 'Advance', 'histcob_cc172fd73af0e776', '9635055340', 'Cooch Behar', 'SAHERA BIBI',
  '2025-10-31', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-31T10:15:00.000Z', '2025-10-31T10:15:00.000Z'
),
(
  'histcob_pay_eb981be622638f03', 'treatment', '2nd Payment', '2nd Payment', 'histcob_cc172fd73af0e776', '9635055340', 'Cooch Behar', 'SAHERA BIBI',
  '2025-11-03', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-03T10:16:00.000Z', '2025-11-03T10:16:00.000Z'
),
(
  'histcob_pay_afe223492c678548', 'treatment', '3rd Payment', '3rd Payment', 'histcob_cc172fd73af0e776', '9635055340', 'Cooch Behar', 'SAHERA BIBI',
  '2025-11-07', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-07T10:17:00.000Z', '2025-11-07T10:17:00.000Z'
),
(
  'histcob_pay_bc466d188bd79c2f', 'treatment', '4th Payment', '4th Payment', 'histcob_cc172fd73af0e776', '9635055340', 'Cooch Behar', 'SAHERA BIBI',
  '2025-11-10', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-10T10:18:00.000Z', '2025-11-10T10:18:00.000Z'
),
(
  'histcob_pay_62c12e74e404fc2b', 'treatment', '5th Payment', '5th Payment', 'histcob_cc172fd73af0e776', '9635055340', 'Cooch Behar', 'SAHERA BIBI',
  '2025-11-14', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-14T10:19:00.000Z', '2025-11-14T10:19:00.000Z'
),
(
  'histcob_pay_3771b5a512bc28fd', 'treatment', '6th Payment', '6th Payment', 'histcob_cc172fd73af0e776', '9635055340', 'Cooch Behar', 'SAHERA BIBI',
  '2025-11-21', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-21T10:20:00.000Z', '2025-11-21T10:20:00.000Z'
),
(
  'histcob_pay_82f66fc3ada2e916', 'treatment', '7th Payment', '7th Payment', 'histcob_cc172fd73af0e776', '9635055340', 'Cooch Behar', 'SAHERA BIBI',
  '2025-11-28', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-28T10:21:00.000Z', '2025-11-28T10:21:00.000Z'
),
(
  'histcob_pay_c4d27e519ffc56bf', 'treatment', '8th Payment', '8th Payment', 'histcob_cc172fd73af0e776', '9635055340', 'Cooch Behar', 'SAHERA BIBI',
  '2025-12-12', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-12T10:22:00.000Z', '2025-12-12T10:22:00.000Z'
),
(
  'histcob_pay_2c4cfe45bb71f839', 'treatment', '9th Payment', '9th Payment', 'histcob_cc172fd73af0e776', '9635055340', 'Cooch Behar', 'SAHERA BIBI',
  '2025-12-15', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-15T10:23:00.000Z', '2025-12-15T10:23:00.000Z'
),
(
  'histcob_pay_8660fd0d066c3564', 'treatment', '10th Payment', '10th Payment', 'histcob_cc172fd73af0e776', '9635055340', 'Cooch Behar', 'SAHERA BIBI',
  '2025-12-26', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-26T10:24:00.000Z', '2025-12-26T10:24:00.000Z'
),
(
  'histcob_pay_6853b8511d904342', 'treatment', '11th Payment', '11th Payment', 'histcob_cc172fd73af0e776', '9635055340', 'Cooch Behar', 'SAHERA BIBI',
  '2026-01-30', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-30T10:25:00.000Z', '2026-01-30T10:25:00.000Z'
),
(
  'histcob_pay_42d167a09d68fe63', 'treatment', 'Advance', 'Advance', 'histcob_2c62eb023bb4094b', '7908386978', 'Cooch Behar', 'Soma Das',
  '2025-11-03', '4500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-03T10:15:00.000Z', '2025-11-03T10:15:00.000Z'
),
(
  'histcob_pay_51eb3783e8d7a317', 'treatment', '2nd Payment', '2nd Payment', 'histcob_2c62eb023bb4094b', '7908386978', 'Cooch Behar', 'Soma Das',
  '2025-11-07', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-07T10:16:00.000Z', '2025-11-07T10:16:00.000Z'
),
(
  'histcob_pay_b79efe8c73f0e4c3', 'treatment', '3rd Payment', '3rd Payment', 'histcob_2c62eb023bb4094b', '7908386978', 'Cooch Behar', 'Soma Das',
  '2025-11-10', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-10T10:17:00.000Z', '2025-11-10T10:17:00.000Z'
),
(
  'histcob_pay_64b1b4ce5ea3e645', 'treatment', '4th Payment', '4th Payment', 'histcob_2c62eb023bb4094b', '7908386978', 'Cooch Behar', 'Soma Das',
  '2025-11-14', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-14T10:18:00.000Z', '2025-11-14T10:18:00.000Z'
),
(
  'histcob_pay_30a25d99b17512fa', 'treatment', '5th Payment', '5th Payment', 'histcob_2c62eb023bb4094b', '7908386978', 'Cooch Behar', 'Soma Das',
  '2025-11-17', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-17T10:19:00.000Z', '2025-11-17T10:19:00.000Z'
),
(
  'histcob_pay_125b7efb817fc592', 'treatment', '6th Payment', '6th Payment', 'histcob_2c62eb023bb4094b', '7908386978', 'Cooch Behar', 'Soma Das',
  '2025-11-24', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-24T10:20:00.000Z', '2025-11-24T10:20:00.000Z'
),
(
  'histcob_pay_85000ec99b05803a', 'treatment', '7th Payment', '7th Payment', 'histcob_2c62eb023bb4094b', '7908386978', 'Cooch Behar', 'Soma Das',
  '2025-12-01', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-01T10:21:00.000Z', '2025-12-01T10:21:00.000Z'
),
(
  'histcob_pay_b2b2c4e5594385df', 'treatment', '8th Payment', '8th Payment', 'histcob_2c62eb023bb4094b', '7908386978', 'Cooch Behar', 'Soma Das',
  '2025-12-12', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-12T10:22:00.000Z', '2025-12-12T10:22:00.000Z'
),
(
  'histcob_pay_75edf8a78cf065fc', 'treatment', '9th Payment', '9th Payment', 'histcob_2c62eb023bb4094b', '7908386978', 'Cooch Behar', 'Soma Das',
  '2025-12-19', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-19T10:23:00.000Z', '2025-12-19T10:23:00.000Z'
),
(
  'histcob_pay_b03a73a52de10265', 'treatment', '10th Payment', '10th Payment', 'histcob_2c62eb023bb4094b', '7908386978', 'Cooch Behar', 'Soma Das',
  '2025-12-29', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-29T10:24:00.000Z', '2025-12-29T10:24:00.000Z'
),
(
  'histcob_pay_a2f50354ce00b072', 'treatment', '11th Payment', '11th Payment', 'histcob_2c62eb023bb4094b', '7908386978', 'Cooch Behar', 'Soma Das',
  '2026-01-09', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-09T10:25:00.000Z', '2026-01-09T10:25:00.000Z'
),
(
  'histcob_pay_dae25cd082c793b3', 'treatment', '12th Payment', '12th Payment', 'histcob_2c62eb023bb4094b', '7908386978', 'Cooch Behar', 'Soma Das',
  '2026-02-27', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-27T10:26:00.000Z', '2026-02-27T10:26:00.000Z'
),
(
  'histcob_pay_a4be89c03936e9e9', 'treatment', 'Advance', 'Advance', 'histcob_c21ffa9773038b6d', '7086480986', 'Cooch Behar', 'RAHIM BADSHA',
  '2025-11-05', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-05T10:15:00.000Z', '2025-11-05T10:15:00.000Z'
),
(
  'histcob_pay_3cec9338729eebf1', 'treatment', '2nd Payment', '2nd Payment', 'histcob_c21ffa9773038b6d', '7086480986', 'Cooch Behar', 'RAHIM BADSHA',
  '2025-11-10', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-10T10:16:00.000Z', '2025-11-10T10:16:00.000Z'
),
(
  'histcob_pay_b5229c08d49f6b1e', 'treatment', '3rd Payment', '3rd Payment', 'histcob_c21ffa9773038b6d', '7086480986', 'Cooch Behar', 'RAHIM BADSHA',
  '2025-11-17', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-17T10:17:00.000Z', '2025-11-17T10:17:00.000Z'
),
(
  'histcob_pay_68d136429bdfce09', 'treatment', '4th Payment', '4th Payment', 'histcob_c21ffa9773038b6d', '7086480986', 'Cooch Behar', 'RAHIM BADSHA',
  '2025-11-21', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-21T10:18:00.000Z', '2025-11-21T10:18:00.000Z'
),
(
  'histcob_pay_384436dcee8ce680', 'treatment', 'Advance', 'Advance', 'histcob_856933f4588b3c65', '8653755936', 'Cooch Behar', 'Biswasnath Ghosh',
  '2025-11-07', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-07T10:15:00.000Z', '2025-11-07T10:15:00.000Z'
),
(
  'histcob_pay_10d5b759c9cc8bda', 'treatment', '2nd Payment', '2nd Payment', 'histcob_856933f4588b3c65', '8653755936', 'Cooch Behar', 'Biswasnath Ghosh',
  '2025-11-10', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-10T10:16:00.000Z', '2025-11-10T10:16:00.000Z'
),
(
  'histcob_pay_7e9f2a06b82f5b9b', 'treatment', '3rd Payment', '3rd Payment', 'histcob_856933f4588b3c65', '8653755936', 'Cooch Behar', 'Biswasnath Ghosh',
  '2025-11-14', '6000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-14T10:17:00.000Z', '2025-11-14T10:17:00.000Z'
),
(
  'histcob_pay_925f497a01e4df7b', 'treatment', '4th Payment', '4th Payment', 'histcob_856933f4588b3c65', '8653755936', 'Cooch Behar', 'Biswasnath Ghosh',
  '2025-11-17', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-17T10:18:00.000Z', '2025-11-17T10:18:00.000Z'
),
(
  'histcob_pay_5a1d9e5e3bdd2772', 'treatment', '5th Payment', '5th Payment', 'histcob_856933f4588b3c65', '8653755936', 'Cooch Behar', 'Biswasnath Ghosh',
  '2025-11-21', '7000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-21T10:19:00.000Z', '2025-11-21T10:19:00.000Z'
),
(
  'histcob_pay_d4ccdcfd5bf67b39', 'treatment', '6th Payment', '6th Payment', 'histcob_856933f4588b3c65', '8653755936', 'Cooch Behar', 'Biswasnath Ghosh',
  '2025-11-28', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-28T10:20:00.000Z', '2025-11-28T10:20:00.000Z'
),
(
  'histcob_pay_c21fec1e181cd572', 'treatment', '7th Payment', '7th Payment', 'histcob_856933f4588b3c65', '8653755936', 'Cooch Behar', 'Biswasnath Ghosh',
  '2025-12-01', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-01T10:21:00.000Z', '2025-12-01T10:21:00.000Z'
),
(
  'histcob_pay_4e90aaf38832ebab', 'treatment', '8th Payment', '8th Payment', 'histcob_856933f4588b3c65', '8653755936', 'Cooch Behar', 'Biswasnath Ghosh',
  '2025-12-05', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-05T10:22:00.000Z', '2025-12-05T10:22:00.000Z'
),
(
  'histcob_pay_0cdd9f635d3f5908', 'treatment', '9th Payment', '9th Payment', 'histcob_856933f4588b3c65', '8653755936', 'Cooch Behar', 'Biswasnath Ghosh',
  '2025-12-08', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-08T10:23:00.000Z', '2025-12-08T10:23:00.000Z'
),
(
  'histcob_pay_31566b08b397b0ee', 'treatment', '10th Payment', '10th Payment', 'histcob_856933f4588b3c65', '8653755936', 'Cooch Behar', 'Biswasnath Ghosh',
  '2025-12-12', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-12T10:24:00.000Z', '2025-12-12T10:24:00.000Z'
),
(
  'histcob_pay_e948d71039478afa', 'treatment', '11th Payment', '11th Payment', 'histcob_856933f4588b3c65', '8653755936', 'Cooch Behar', 'Biswasnath Ghosh',
  '2025-12-15', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-15T10:25:00.000Z', '2025-12-15T10:25:00.000Z'
),
(
  'histcob_pay_3aa272c1a1c36c28', 'treatment', '12th Payment', '12th Payment', 'histcob_856933f4588b3c65', '8653755936', 'Cooch Behar', 'Biswasnath Ghosh',
  '2025-12-22', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-22T10:26:00.000Z', '2025-12-22T10:26:00.000Z'
),
(
  'histcob_pay_99998f9fb318d1e2', 'treatment', 'Advance', 'Advance', 'histcob_8675fd0218e3725e', '9395202505', 'Cooch Behar', 'NUR KALAM',
  '2025-11-14', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-14T10:15:00.000Z', '2025-11-14T10:15:00.000Z'
),
(
  'histcob_pay_e60b5c9f38f2b511', 'treatment', '2nd Payment', '2nd Payment', 'histcob_8675fd0218e3725e', '9395202505', 'Cooch Behar', 'NUR KALAM',
  '2025-11-17', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-17T10:16:00.000Z', '2025-11-17T10:16:00.000Z'
),
(
  'histcob_pay_50e59baa73b94010', 'treatment', 'Advance', 'Advance', 'histcob_84b9282c5fcf5ee1', '9957718621', 'Cooch Behar', 'ABDUL RAHIM MANDAL',
  '2025-11-17', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-17T10:15:00.000Z', '2025-11-17T10:15:00.000Z'
),
(
  'histcob_pay_80f366e64876070f', 'treatment', '2nd Payment', '2nd Payment', 'histcob_84b9282c5fcf5ee1', '9957718621', 'Cooch Behar', 'ABDUL RAHIM MANDAL',
  '2025-11-21', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-21T10:16:00.000Z', '2025-11-21T10:16:00.000Z'
),
(
  'histcob_pay_d944cb29d6e5ee2a', 'treatment', '3rd Payment', '3rd Payment', 'histcob_84b9282c5fcf5ee1', '9957718621', 'Cooch Behar', 'ABDUL RAHIM MANDAL',
  '2025-11-24', '15000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-24T10:17:00.000Z', '2025-11-24T10:17:00.000Z'
),
(
  'histcob_pay_8baab101eead4203', 'treatment', '4th Payment', '4th Payment', 'histcob_84b9282c5fcf5ee1', '9957718621', 'Cooch Behar', 'ABDUL RAHIM MANDAL',
  '2025-12-01', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-01T10:18:00.000Z', '2025-12-01T10:18:00.000Z'
),
(
  'histcob_pay_62f50e7d8155e14e', 'treatment', 'Advance', 'Advance', 'histcob_bbec894742bc5a3b', '9957414247', 'Cooch Behar', 'MIJANUR SAKH',
  '2025-11-17', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-17T10:15:00.000Z', '2025-11-17T10:15:00.000Z'
),
(
  'histcob_pay_09929e328634aa28', 'treatment', 'Advance', 'Advance', 'histcob_f56850588e91329c', '8016432607', 'Cooch Behar', 'RUMPA DAS',
  '2025-11-21', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-21T10:15:00.000Z', '2025-11-21T10:15:00.000Z'
),
(
  'histcob_pay_e95e5652d8dedb17', 'treatment', '2nd Payment', '2nd Payment', 'histcob_f56850588e91329c', '8016432607', 'Cooch Behar', 'RUMPA DAS',
  '2025-11-24', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-24T10:16:00.000Z', '2025-11-24T10:16:00.000Z'
),
(
  'histcob_pay_8a1dd80be7a77849', 'treatment', '3rd Payment', '3rd Payment', 'histcob_f56850588e91329c', '8016432607', 'Cooch Behar', 'RUMPA DAS',
  '2025-11-28', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-28T10:17:00.000Z', '2025-11-28T10:17:00.000Z'
),
(
  'histcob_pay_3ba711479a1823f4', 'treatment', '4th Payment', '4th Payment', 'histcob_f56850588e91329c', '8016432607', 'Cooch Behar', 'RUMPA DAS',
  '2025-12-01', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-01T10:18:00.000Z', '2025-12-01T10:18:00.000Z'
),
(
  'histcob_pay_af46e67e0fcf45cc', 'treatment', '5th Payment', '5th Payment', 'histcob_f56850588e91329c', '8016432607', 'Cooch Behar', 'RUMPA DAS',
  '2025-12-05', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-05T10:19:00.000Z', '2025-12-05T10:19:00.000Z'
),
(
  'histcob_pay_00b94651b6592694', 'treatment', '6th Payment', '6th Payment', 'histcob_f56850588e91329c', '8016432607', 'Cooch Behar', 'RUMPA DAS',
  '2025-12-12', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-12T10:20:00.000Z', '2025-12-12T10:20:00.000Z'
),
(
  'histcob_pay_5734374c2254a06c', 'treatment', '7th Payment', '7th Payment', 'histcob_f56850588e91329c', '8016432607', 'Cooch Behar', 'RUMPA DAS',
  '2025-12-08', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-08T10:21:00.000Z', '2025-12-08T10:21:00.000Z'
),
(
  'histcob_pay_9b6f9cc082fb6542', 'treatment', '8th Payment', '8th Payment', 'histcob_f56850588e91329c', '8016432607', 'Cooch Behar', 'RUMPA DAS',
  '2025-12-15', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-15T10:22:00.000Z', '2025-12-15T10:22:00.000Z'
),
(
  'histcob_pay_b24f56ed492d487c', 'treatment', '9th Payment', '9th Payment', 'histcob_f56850588e91329c', '8016432607', 'Cooch Behar', 'RUMPA DAS',
  '2025-12-22', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-22T10:23:00.000Z', '2025-12-22T10:23:00.000Z'
),
(
  'histcob_pay_11c83b4e82d56202', 'treatment', '10th Payment', '10th Payment', 'histcob_f56850588e91329c', '8016432607', 'Cooch Behar', 'RUMPA DAS',
  '2025-12-26', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-26T10:24:00.000Z', '2025-12-26T10:24:00.000Z'
),
(
  'histcob_pay_0f93ff4f69d4776c', 'treatment', '11th Payment', '11th Payment', 'histcob_f56850588e91329c', '8016432607', 'Cooch Behar', 'RUMPA DAS',
  '2026-01-03', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-03T10:25:00.000Z', '2026-01-03T10:25:00.000Z'
),
(
  'histcob_pay_e8d2a0e001e7cb5a', 'treatment', '12th Payment', '12th Payment', 'histcob_f56850588e91329c', '8016432607', 'Cooch Behar', 'RUMPA DAS',
  '2026-01-12', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-12T10:26:00.000Z', '2026-01-12T10:26:00.000Z'
),
(
  'histcob_pay_605413ee89c4083d', 'treatment', '13th Payment', '13th Payment', 'histcob_f56850588e91329c', '8016432607', 'Cooch Behar', 'RUMPA DAS',
  '2026-01-16', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-16T10:27:00.000Z', '2026-01-16T10:27:00.000Z'
),
(
  'histcob_pay_fc7b747254b0146b', 'treatment', '14th Payment', '14th Payment', 'histcob_f56850588e91329c', '8016432607', 'Cooch Behar', 'RUMPA DAS',
  '2026-02-02', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-02T10:28:00.000Z', '2026-02-02T10:28:00.000Z'
),
(
  'histcob_pay_35c1567d0f859839', 'treatment', '15th Payment', '15th Payment', 'histcob_f56850588e91329c', '8016432607', 'Cooch Behar', 'RUMPA DAS',
  '2026-02-16', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-16T10:29:00.000Z', '2026-02-16T10:29:00.000Z'
),
(
  'histcob_pay_d7bcd7d6334f0f00', 'treatment', 'Advance', 'Advance', 'histcob_88fd122e254c4d11', '9647802520', 'Cooch Behar', 'Bijli Bibi',
  '2025-11-26', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-26T10:15:00.000Z', '2025-11-26T10:15:00.000Z'
),
(
  'histcob_pay_314064f3aa4dbb8d', 'treatment', '2nd Payment', '2nd Payment', 'histcob_88fd122e254c4d11', '9647802520', 'Cooch Behar', 'Bijli Bibi',
  '2025-11-28', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-28T10:16:00.000Z', '2025-11-28T10:16:00.000Z'
),
(
  'histcob_pay_dde19127b610e6ec', 'treatment', '3rd Payment', '3rd Payment', 'histcob_88fd122e254c4d11', '9647802520', 'Cooch Behar', 'Bijli Bibi',
  '2025-12-01', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-01T10:17:00.000Z', '2025-12-01T10:17:00.000Z'
),
(
  'histcob_pay_f11720bfde163530', 'treatment', '4th Payment', '4th Payment', 'histcob_88fd122e254c4d11', '9647802520', 'Cooch Behar', 'Bijli Bibi',
  '2025-12-05', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-05T10:18:00.000Z', '2025-12-05T10:18:00.000Z'
),
(
  'histcob_pay_1e96689942a28927', 'treatment', '5th Payment', '5th Payment', 'histcob_88fd122e254c4d11', '9647802520', 'Cooch Behar', 'Bijli Bibi',
  '2025-12-08', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-08T10:19:00.000Z', '2025-12-08T10:19:00.000Z'
),
(
  'histcob_pay_752245901a1d0f7e', 'treatment', '6th Payment', '6th Payment', 'histcob_88fd122e254c4d11', '9647802520', 'Cooch Behar', 'Bijli Bibi',
  '2025-12-12', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-12T10:20:00.000Z', '2025-12-12T10:20:00.000Z'
),
(
  'histcob_pay_b3c38490526077fd', 'treatment', '7th Payment', '7th Payment', 'histcob_88fd122e254c4d11', '9647802520', 'Cooch Behar', 'Bijli Bibi',
  '2025-12-15', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-15T10:21:00.000Z', '2025-12-15T10:21:00.000Z'
),
(
  'histcob_pay_b6a0ea576d6403db', 'treatment', '8th Payment', '8th Payment', 'histcob_88fd122e254c4d11', '9647802520', 'Cooch Behar', 'Bijli Bibi',
  '2025-12-15', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-15T10:22:00.000Z', '2025-12-15T10:22:00.000Z'
),
(
  'histcob_pay_7237101fa5fd5f4a', 'treatment', '9th Payment', '9th Payment', 'histcob_88fd122e254c4d11', '9647802520', 'Cooch Behar', 'Bijli Bibi',
  '2026-01-30', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-30T10:23:00.000Z', '2026-01-30T10:23:00.000Z'
),
(
  'histcob_pay_982ec52c01f7a2e5', 'treatment', '10th Payment', '10th Payment', 'histcob_88fd122e254c4d11', '9647802520', 'Cooch Behar', 'Bijli Bibi',
  '2026-02-02', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-02T10:24:00.000Z', '2026-02-02T10:24:00.000Z'
),
(
  'histcob_pay_6268ad3698d4146b', 'treatment', '11th Payment', '11th Payment', 'histcob_88fd122e254c4d11', '9647802520', 'Cooch Behar', 'Bijli Bibi',
  '2026-02-13', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-13T10:25:00.000Z', '2026-02-13T10:25:00.000Z'
),
(
  'histcob_pay_846d6e9060558eb7', 'treatment', 'Advance', 'Advance', 'histcob_9b9e66964d52daee', '7478233316', 'Cooch Behar', 'MONIRUL HOSSAIN',
  '2025-12-19', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-19T10:15:00.000Z', '2025-12-19T10:15:00.000Z'
),
(
  'histcob_pay_614fb13ca07264c3', 'treatment', '2nd Payment', '2nd Payment', 'histcob_9b9e66964d52daee', '7478233316', 'Cooch Behar', 'MONIRUL HOSSAIN',
  '2025-12-22', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-22T10:16:00.000Z', '2025-12-22T10:16:00.000Z'
),
(
  'histcob_pay_f7938178031d4536', 'treatment', '3rd Payment', '3rd Payment', 'histcob_9b9e66964d52daee', '7478233316', 'Cooch Behar', 'MONIRUL HOSSAIN',
  '2025-12-29', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-29T10:17:00.000Z', '2025-12-29T10:17:00.000Z'
),
(
  'histcob_pay_b3a93162309f3847', 'treatment', '4th Payment', '4th Payment', 'histcob_9b9e66964d52daee', '7478233316', 'Cooch Behar', 'MONIRUL HOSSAIN',
  '2026-01-06', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-06T10:18:00.000Z', '2026-01-06T10:18:00.000Z'
),
(
  'histcob_pay_270223a6a0492822', 'treatment', '5th Payment', '5th Payment', 'histcob_9b9e66964d52daee', '7478233316', 'Cooch Behar', 'MONIRUL HOSSAIN',
  '2026-01-12', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-12T10:19:00.000Z', '2026-01-12T10:19:00.000Z'
),
(
  'histcob_pay_6fff7c4a40e5e5ad', 'treatment', '6th Payment', '6th Payment', 'histcob_9b9e66964d52daee', '7478233316', 'Cooch Behar', 'MONIRUL HOSSAIN',
  '2026-01-19', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-19T10:20:00.000Z', '2026-01-19T10:20:00.000Z'
),
(
  'histcob_pay_bf4f8a017f149020', 'treatment', '7th Payment', '7th Payment', 'histcob_9b9e66964d52daee', '7478233316', 'Cooch Behar', 'MONIRUL HOSSAIN',
  '2026-01-26', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-26T10:21:00.000Z', '2026-01-26T10:21:00.000Z'
),
(
  'histcob_pay_71f659d3ebbac474', 'treatment', '8th Payment', '8th Payment', 'histcob_9b9e66964d52daee', '7478233316', 'Cooch Behar', 'MONIRUL HOSSAIN',
  '2026-02-02', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-02T10:22:00.000Z', '2026-02-02T10:22:00.000Z'
),
(
  'histcob_pay_253b818082c4367d', 'treatment', '9th Payment', '9th Payment', 'histcob_9b9e66964d52daee', '7478233316', 'Cooch Behar', 'MONIRUL HOSSAIN',
  '2026-02-09', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-09T10:23:00.000Z', '2026-02-09T10:23:00.000Z'
),
(
  'histcob_pay_27eee49c2111e897', 'treatment', '10th Payment', '10th Payment', 'histcob_9b9e66964d52daee', '7478233316', 'Cooch Behar', 'MONIRUL HOSSAIN',
  '2026-02-16', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-16T10:24:00.000Z', '2026-02-16T10:24:00.000Z'
),
(
  'histcob_pay_0bf4a2b4b3838e2e', 'treatment', '11th Payment', '11th Payment', 'histcob_9b9e66964d52daee', '7478233316', 'Cooch Behar', 'MONIRUL HOSSAIN',
  '2026-02-23', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-23T10:25:00.000Z', '2026-02-23T10:25:00.000Z'
),
(
  'histcob_pay_d8d6020ae0e0f01e', 'treatment', '12th Payment', '12th Payment', 'histcob_9b9e66964d52daee', '7478233316', 'Cooch Behar', 'MONIRUL HOSSAIN',
  '2026-03-02', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-03-02T10:26:00.000Z', '2026-03-02T10:26:00.000Z'
),
(
  'histcob_pay_61acd13b2f7a2e10', 'treatment', '13th Payment', '13th Payment', 'histcob_9b9e66964d52daee', '7478233316', 'Cooch Behar', 'MONIRUL HOSSAIN',
  '2026-03-09', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-03-09T10:27:00.000Z', '2026-03-09T10:27:00.000Z'
),
(
  'histcob_pay_70e358cfbef2e6b0', 'treatment', '14th Payment', '14th Payment', 'histcob_9b9e66964d52daee', '7478233316', 'Cooch Behar', 'MONIRUL HOSSAIN',
  '2026-03-16', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-03-16T10:28:00.000Z', '2026-03-16T10:28:00.000Z'
),
(
  'histcob_pay_df98545c6588fd6c', 'treatment', '15th Payment', '15th Payment', 'histcob_9b9e66964d52daee', '7478233316', 'Cooch Behar', 'MONIRUL HOSSAIN',
  '2026-03-23', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-03-23T10:29:00.000Z', '2026-03-23T10:29:00.000Z'
),
(
  'histcob_pay_53812f722b68812a', 'treatment', '16th Payment', '16th Payment', 'histcob_9b9e66964d52daee', '7478233316', 'Cooch Behar', 'MONIRUL HOSSAIN',
  '2026-03-30', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-03-30T10:30:00.000Z', '2026-03-30T10:30:00.000Z'
),
(
  'histcob_pay_c9b0042a4bb18f8f', 'treatment', 'Advance', 'Advance', 'histcob_1bef335d59ff38f9', '8372835069', 'Cooch Behar', 'MOFIJUL HOQUE',
  '2025-12-19', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-19T10:15:00.000Z', '2025-12-19T10:15:00.000Z'
),
(
  'histcob_pay_1f920204c27ff178', 'treatment', '2nd Payment', '2nd Payment', 'histcob_1bef335d59ff38f9', '8372835069', 'Cooch Behar', 'MOFIJUL HOQUE',
  '2025-12-22', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-22T10:16:00.000Z', '2025-12-22T10:16:00.000Z'
),
(
  'histcob_pay_840aaf432239b557', 'treatment', '3rd Payment', '3rd Payment', 'histcob_1bef335d59ff38f9', '8372835069', 'Cooch Behar', 'MOFIJUL HOQUE',
  '2025-12-26', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-26T10:17:00.000Z', '2025-12-26T10:17:00.000Z'
),
(
  'histcob_pay_a99968994eafa2db', 'treatment', '4th Payment', '4th Payment', 'histcob_1bef335d59ff38f9', '8372835069', 'Cooch Behar', 'MOFIJUL HOQUE',
  '2026-01-06', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-06T10:18:00.000Z', '2026-01-06T10:18:00.000Z'
),
(
  'histcob_pay_8eac7a46fe619e2a', 'treatment', 'Advance', 'Advance', 'histcob_523bba61ee37cc56', '7683038396', 'Cooch Behar', 'SUKBILASH ROY SHINGHO',
  '2025-12-15', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-15T10:15:00.000Z', '2025-12-15T10:15:00.000Z'
),
(
  'histcob_pay_d3def8da12464e0d', 'treatment', '2nd Payment', '2nd Payment', 'histcob_523bba61ee37cc56', '7683038396', 'Cooch Behar', 'SUKBILASH ROY SHINGHO',
  '2025-12-19', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-19T10:16:00.000Z', '2025-12-19T10:16:00.000Z'
),
(
  'histcob_pay_8c73706e32b78741', 'treatment', '3rd Payment', '3rd Payment', 'histcob_523bba61ee37cc56', '7683038396', 'Cooch Behar', 'SUKBILASH ROY SHINGHO',
  '2025-12-22', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-22T10:17:00.000Z', '2025-12-22T10:17:00.000Z'
),
(
  'histcob_pay_10837e5e2e82dfe4', 'treatment', '4th Payment', '4th Payment', 'histcob_523bba61ee37cc56', '7683038396', 'Cooch Behar', 'SUKBILASH ROY SHINGHO',
  '2025-12-26', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-26T10:18:00.000Z', '2025-12-26T10:18:00.000Z'
),
(
  'histcob_pay_6e2f684b2f93aee2', 'treatment', '5th Payment', '5th Payment', 'histcob_523bba61ee37cc56', '7683038396', 'Cooch Behar', 'SUKBILASH ROY SHINGHO',
  '2025-12-29', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-29T10:19:00.000Z', '2025-12-29T10:19:00.000Z'
),
(
  'histcob_pay_554e17da07c8e03e', 'treatment', '6th Payment', '6th Payment', 'histcob_523bba61ee37cc56', '7683038396', 'Cooch Behar', 'SUKBILASH ROY SHINGHO',
  '2026-01-03', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-03T10:20:00.000Z', '2026-01-03T10:20:00.000Z'
),
(
  'histcob_pay_af1814a54dfa3bfe', 'treatment', '7th Payment', '7th Payment', 'histcob_523bba61ee37cc56', '7683038396', 'Cooch Behar', 'SUKBILASH ROY SHINGHO',
  '2026-01-06', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-06T10:21:00.000Z', '2026-01-06T10:21:00.000Z'
),
(
  'histcob_pay_e1bbc86d381ecad0', 'treatment', '8th Payment', '8th Payment', 'histcob_523bba61ee37cc56', '7683038396', 'Cooch Behar', 'SUKBILASH ROY SHINGHO',
  '2026-01-09', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-09T10:22:00.000Z', '2026-01-09T10:22:00.000Z'
),
(
  'histcob_pay_7ae810fa3cab598f', 'treatment', '9th Payment', '9th Payment', 'histcob_523bba61ee37cc56', '7683038396', 'Cooch Behar', 'SUKBILASH ROY SHINGHO',
  '2026-01-12', '6000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-12T10:23:00.000Z', '2026-01-12T10:23:00.000Z'
),
(
  'histcob_pay_1ad7329f8c3a5041', 'treatment', '10th Payment', '10th Payment', 'histcob_523bba61ee37cc56', '7683038396', 'Cooch Behar', 'SUKBILASH ROY SHINGHO',
  '2026-01-16', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-16T10:24:00.000Z', '2026-01-16T10:24:00.000Z'
),
(
  'histcob_pay_27b29b29d057ec92', 'treatment', '11th Payment', '11th Payment', 'histcob_523bba61ee37cc56', '7683038396', 'Cooch Behar', 'SUKBILASH ROY SHINGHO',
  '2026-01-19', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-19T10:25:00.000Z', '2026-01-19T10:25:00.000Z'
),
(
  'histcob_pay_8105a438f99310fe', 'treatment', '12th Payment', '12th Payment', 'histcob_523bba61ee37cc56', '7683038396', 'Cooch Behar', 'SUKBILASH ROY SHINGHO',
  '2026-01-23', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-23T10:26:00.000Z', '2026-01-23T10:26:00.000Z'
),
(
  'histcob_pay_5c8f1525d0a4094f', 'treatment', '13th Payment', '13th Payment', 'histcob_523bba61ee37cc56', '7683038396', 'Cooch Behar', 'SUKBILASH ROY SHINGHO',
  '2026-01-26', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-26T10:27:00.000Z', '2026-01-26T10:27:00.000Z'
),
(
  'histcob_pay_b9c8deb402659e7c', 'treatment', '14th Payment', '14th Payment', 'histcob_523bba61ee37cc56', '7683038396', 'Cooch Behar', 'SUKBILASH ROY SHINGHO',
  '2026-02-02', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-02T10:28:00.000Z', '2026-02-02T10:28:00.000Z'
),
(
  'histcob_pay_a041547e3d336ecf', 'treatment', '15th Payment', '15th Payment', 'histcob_523bba61ee37cc56', '7683038396', 'Cooch Behar', 'SUKBILASH ROY SHINGHO',
  '2026-02-06', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-06T10:29:00.000Z', '2026-02-06T10:29:00.000Z'
),
(
  'histcob_pay_3655fbccb16c5fdf', 'treatment', '16th Payment', '16th Payment', 'histcob_523bba61ee37cc56', '7683038396', 'Cooch Behar', 'SUKBILASH ROY SHINGHO',
  '2026-02-13', '7000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-13T10:30:00.000Z', '2026-02-13T10:30:00.000Z'
),
(
  'histcob_pay_80a8831ba60b997f', 'treatment', 'Advance', 'Advance', 'histcob_6f27c83300edc047', '9382762795', 'Cooch Behar', 'BADAL SAHA',
  '2025-12-22', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-22T10:15:00.000Z', '2025-12-22T10:15:00.000Z'
),
(
  'histcob_pay_cfb91afad07ce2cf', 'treatment', '2nd Payment', '2nd Payment', 'histcob_6f27c83300edc047', '9382762795', 'Cooch Behar', 'BADAL SAHA',
  '2025-12-26', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-26T10:16:00.000Z', '2025-12-26T10:16:00.000Z'
),
(
  'histcob_pay_644be6e197ce280a', 'treatment', '3rd Payment', '3rd Payment', 'histcob_6f27c83300edc047', '9382762795', 'Cooch Behar', 'BADAL SAHA',
  '2025-12-29', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-29T10:17:00.000Z', '2025-12-29T10:17:00.000Z'
),
(
  'histcob_pay_8cf98d416eb79d9d', 'treatment', '4th Payment', '4th Payment', 'histcob_6f27c83300edc047', '9382762795', 'Cooch Behar', 'BADAL SAHA',
  '2026-01-06', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-06T10:18:00.000Z', '2026-01-06T10:18:00.000Z'
),
(
  'histcob_pay_d44848d6dede6455', 'treatment', '5th Payment', '5th Payment', 'histcob_6f27c83300edc047', '9382762795', 'Cooch Behar', 'BADAL SAHA',
  '2026-01-09', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-09T10:19:00.000Z', '2026-01-09T10:19:00.000Z'
),
(
  'histcob_pay_285225a9847debfe', 'treatment', '6th Payment', '6th Payment', 'histcob_6f27c83300edc047', '9382762795', 'Cooch Behar', 'BADAL SAHA',
  '2027-01-12', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2027-01-12T10:20:00.000Z', '2027-01-12T10:20:00.000Z'
),
(
  'histcob_pay_2e580d30e641bc1c', 'treatment', '7th Payment', '7th Payment', 'histcob_6f27c83300edc047', '9382762795', 'Cooch Behar', 'BADAL SAHA',
  '2026-01-16', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-16T10:21:00.000Z', '2026-01-16T10:21:00.000Z'
),
(
  'histcob_pay_3e73e10dddfe3ccb', 'treatment', '8th Payment', '8th Payment', 'histcob_6f27c83300edc047', '9382762795', 'Cooch Behar', 'BADAL SAHA',
  '2026-01-26', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-26T10:22:00.000Z', '2026-01-26T10:22:00.000Z'
),
(
  'histcob_pay_c0f724899596ad3f', 'treatment', '9th Payment', '9th Payment', 'histcob_6f27c83300edc047', '9382762795', 'Cooch Behar', 'BADAL SAHA',
  '2026-01-30', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-30T10:23:00.000Z', '2026-01-30T10:23:00.000Z'
),
(
  'histcob_pay_c98f69a88d796889', 'treatment', '10th Payment', '10th Payment', 'histcob_6f27c83300edc047', '9382762795', 'Cooch Behar', 'BADAL SAHA',
  '2026-02-02', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-02T10:24:00.000Z', '2026-02-02T10:24:00.000Z'
),
(
  'histcob_pay_7b45343855058e1a', 'treatment', '11th Payment', '11th Payment', 'histcob_6f27c83300edc047', '9382762795', 'Cooch Behar', 'BADAL SAHA',
  '2026-02-06', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-06T10:25:00.000Z', '2026-02-06T10:25:00.000Z'
),
(
  'histcob_pay_bc8dcf5ed3ec8d41', 'treatment', '12th Payment', '12th Payment', 'histcob_6f27c83300edc047', '9382762795', 'Cooch Behar', 'BADAL SAHA',
  '2026-05-01', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-05-01T10:26:00.000Z', '2026-05-01T10:26:00.000Z'
),
(
  'histcob_pay_97d8c8f86bb17262', 'treatment', 'Advance', 'Advance', 'histcob_80710fdfcdd97f8b', '9091566634', 'Cooch Behar', 'SANATAN DAS',
  '2025-12-22', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-22T10:15:00.000Z', '2025-12-22T10:15:00.000Z'
),
(
  'histcob_pay_608411957d078be5', 'treatment', '2nd Payment', '2nd Payment', 'histcob_80710fdfcdd97f8b', '9091566634', 'Cooch Behar', 'SANATAN DAS',
  '2025-12-26', '6000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-26T10:16:00.000Z', '2025-12-26T10:16:00.000Z'
),
(
  'histcob_pay_3bb48f32214deb45', 'treatment', '3rd Payment', '3rd Payment', 'histcob_80710fdfcdd97f8b', '9091566634', 'Cooch Behar', 'SANATAN DAS',
  '2025-12-29', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-29T10:17:00.000Z', '2025-12-29T10:17:00.000Z'
),
(
  'histcob_pay_b7666008d2248e39', 'treatment', '4th Payment', '4th Payment', 'histcob_80710fdfcdd97f8b', '9091566634', 'Cooch Behar', 'SANATAN DAS',
  '2026-01-03', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-03T10:18:00.000Z', '2026-01-03T10:18:00.000Z'
),
(
  'histcob_pay_ede9fe45e005cace', 'treatment', '5th Payment', '5th Payment', 'histcob_80710fdfcdd97f8b', '9091566634', 'Cooch Behar', 'SANATAN DAS',
  '2026-01-06', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-06T10:19:00.000Z', '2026-01-06T10:19:00.000Z'
),
(
  'histcob_pay_8e6209d2a9be6a2d', 'treatment', '6th Payment', '6th Payment', 'histcob_80710fdfcdd97f8b', '9091566634', 'Cooch Behar', 'SANATAN DAS',
  '2026-01-09', '1500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-09T10:20:00.000Z', '2026-01-09T10:20:00.000Z'
),
(
  'histcob_pay_888110e01a1f73f3', 'treatment', '7th Payment', '7th Payment', 'histcob_80710fdfcdd97f8b', '9091566634', 'Cooch Behar', 'SANATAN DAS',
  '2026-01-12', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-12T10:21:00.000Z', '2026-01-12T10:21:00.000Z'
),
(
  'histcob_pay_8a4bd4e461cc8f26', 'treatment', '8th Payment', '8th Payment', 'histcob_80710fdfcdd97f8b', '9091566634', 'Cooch Behar', 'SANATAN DAS',
  '2026-01-16', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-16T10:22:00.000Z', '2026-01-16T10:22:00.000Z'
),
(
  'histcob_pay_f1f41ccde363f31e', 'treatment', '9th Payment', '9th Payment', 'histcob_80710fdfcdd97f8b', '9091566634', 'Cooch Behar', 'SANATAN DAS',
  '2026-01-23', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-23T10:23:00.000Z', '2026-01-23T10:23:00.000Z'
),
(
  'histcob_pay_1ae22194cb2d8198', 'treatment', '10th Payment', '10th Payment', 'histcob_80710fdfcdd97f8b', '9091566634', 'Cooch Behar', 'SANATAN DAS',
  '2026-01-26', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-26T10:24:00.000Z', '2026-01-26T10:24:00.000Z'
),
(
  'histcob_pay_559b26d22ef676ff', 'treatment', '11th Payment', '11th Payment', 'histcob_80710fdfcdd97f8b', '9091566634', 'Cooch Behar', 'SANATAN DAS',
  '2026-02-02', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-02T10:25:00.000Z', '2026-02-02T10:25:00.000Z'
),
(
  'histcob_pay_717fd316d1b5d3c5', 'treatment', '12th Payment', '12th Payment', 'histcob_80710fdfcdd97f8b', '9091566634', 'Cooch Behar', 'SANATAN DAS',
  '2026-02-09', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-09T10:26:00.000Z', '2026-02-09T10:26:00.000Z'
),
(
  'histcob_pay_ad53b9d328cd7c0b', 'treatment', '13th Payment', '13th Payment', 'histcob_80710fdfcdd97f8b', '9091566634', 'Cooch Behar', 'SANATAN DAS',
  '2026-02-16', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-16T10:27:00.000Z', '2026-02-16T10:27:00.000Z'
),
(
  'histcob_pay_4a86c976f8750a4a', 'treatment', '14th Payment', '14th Payment', 'histcob_80710fdfcdd97f8b', '9091566634', 'Cooch Behar', 'SANATAN DAS',
  '2026-03-06', '500', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-03-06T10:28:00.000Z', '2026-03-06T10:28:00.000Z'
),
(
  'histcob_pay_da48be2e859fd2db', 'treatment', 'Advance', 'Advance', 'histcob_b35a4216f585623d', '7586986796', 'Cooch Behar', 'AMIT MAJUMDAR',
  '2025-12-22', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-22T10:15:00.000Z', '2025-12-22T10:15:00.000Z'
),
(
  'histcob_pay_3766ffb4f5a2ee82', 'treatment', '2nd Payment', '2nd Payment', 'histcob_b35a4216f585623d', '7586986796', 'Cooch Behar', 'AMIT MAJUMDAR',
  '2025-12-26', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-26T10:16:00.000Z', '2025-12-26T10:16:00.000Z'
),
(
  'histcob_pay_ccc447836d7777cb', 'treatment', '3rd Payment', '3rd Payment', 'histcob_b35a4216f585623d', '7586986796', 'Cooch Behar', 'AMIT MAJUMDAR',
  '2025-12-29', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-29T10:17:00.000Z', '2025-12-29T10:17:00.000Z'
),
(
  'histcob_pay_227ba8047a01b371', 'treatment', '4th Payment', '4th Payment', 'histcob_b35a4216f585623d', '7586986796', 'Cooch Behar', 'AMIT MAJUMDAR',
  '2025-12-30', '5000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-30T10:18:00.000Z', '2025-12-30T10:18:00.000Z'
),
(
  'histcob_pay_24e6413c3535daff', 'treatment', '5th Payment', '5th Payment', 'histcob_b35a4216f585623d', '7586986796', 'Cooch Behar', 'AMIT MAJUMDAR',
  '2026-01-03', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-03T10:19:00.000Z', '2026-01-03T10:19:00.000Z'
),
(
  'histcob_pay_76208bb53f4e076e', 'treatment', '6th Payment', '6th Payment', 'histcob_b35a4216f585623d', '7586986796', 'Cooch Behar', 'AMIT MAJUMDAR',
  '2026-01-06', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-06T10:20:00.000Z', '2026-01-06T10:20:00.000Z'
),
(
  'histcob_pay_27857cfa82259c15', 'treatment', '7th Payment', '7th Payment', 'histcob_b35a4216f585623d', '7586986796', 'Cooch Behar', 'AMIT MAJUMDAR',
  '2026-01-09', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-09T10:21:00.000Z', '2026-01-09T10:21:00.000Z'
),
(
  'histcob_pay_b958314e47bf1958', 'treatment', 'Advance', 'Advance', 'histcob_e8ceaad56e8e9283', '9832844964', 'Cooch Behar', 'RAJIB AHAMMED',
  '2025-12-26', '10000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-26T10:15:00.000Z', '2025-12-26T10:15:00.000Z'
),
(
  'histcob_pay_10c0e4310788b8c9', 'treatment', '2nd Payment', '2nd Payment', 'histcob_e8ceaad56e8e9283', '9832844964', 'Cooch Behar', 'RAJIB AHAMMED',
  '2025-12-29', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-29T10:16:00.000Z', '2025-12-29T10:16:00.000Z'
),
(
  'histcob_pay_b71b5a5a86284099', 'treatment', '3rd Payment', '3rd Payment', 'histcob_e8ceaad56e8e9283', '9832844964', 'Cooch Behar', 'RAJIB AHAMMED',
  '2026-01-03', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-03T10:17:00.000Z', '2026-01-03T10:17:00.000Z'
),
(
  'histcob_pay_cc5ffac7041df1fa', 'treatment', '4th Payment', '4th Payment', 'histcob_e8ceaad56e8e9283', '9832844964', 'Cooch Behar', 'RAJIB AHAMMED',
  '2026-01-06', '4000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-06T10:18:00.000Z', '2026-01-06T10:18:00.000Z'
),
(
  'histcob_pay_efd915591334363f', 'treatment', '5th Payment', '5th Payment', 'histcob_e8ceaad56e8e9283', '9832844964', 'Cooch Behar', 'RAJIB AHAMMED',
  '2026-01-09', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-09T10:19:00.000Z', '2026-01-09T10:19:00.000Z'
),
(
  'histcob_pay_124fe96e2c4b837a', 'treatment', '6th Payment', '6th Payment', 'histcob_e8ceaad56e8e9283', '9832844964', 'Cooch Behar', 'RAJIB AHAMMED',
  '2026-01-12', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-12T10:20:00.000Z', '2026-01-12T10:20:00.000Z'
),
(
  'histcob_pay_94971eb75a0bcf4c', 'treatment', '7th Payment', '7th Payment', 'histcob_e8ceaad56e8e9283', '9832844964', 'Cooch Behar', 'RAJIB AHAMMED',
  '2026-01-16', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-16T10:21:00.000Z', '2026-01-16T10:21:00.000Z'
),
(
  'histcob_pay_b3585e79b5269720', 'treatment', '8th Payment', '8th Payment', 'histcob_e8ceaad56e8e9283', '9832844964', 'Cooch Behar', 'RAJIB AHAMMED',
  '2026-01-19', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-19T10:22:00.000Z', '2026-01-19T10:22:00.000Z'
),
(
  'histcob_pay_10e940d9c4cc312e', 'treatment', '9th Payment', '9th Payment', 'histcob_e8ceaad56e8e9283', '9832844964', 'Cooch Behar', 'RAJIB AHAMMED',
  '2026-01-23', '1000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-23T10:23:00.000Z', '2026-01-23T10:23:00.000Z'
),
(
  'histcob_pay_5f919fa0cc520a8e', 'treatment', '10th Payment', '10th Payment', 'histcob_e8ceaad56e8e9283', '9832844964', 'Cooch Behar', 'RAJIB AHAMMED',
  '2026-01-26', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-26T10:24:00.000Z', '2026-01-26T10:24:00.000Z'
),
(
  'histcob_pay_43a727602ffda2c2', 'treatment', '11th Payment', '11th Payment', 'histcob_e8ceaad56e8e9283', '9832844964', 'Cooch Behar', 'RAJIB AHAMMED',
  '2026-01-30', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-30T10:25:00.000Z', '2026-01-30T10:25:00.000Z'
),
(
  'histcob_pay_5681bcbbcf36bb50', 'treatment', '12th Payment', '12th Payment', 'histcob_e8ceaad56e8e9283', '9832844964', 'Cooch Behar', 'RAJIB AHAMMED',
  '2026-02-02', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-02T10:26:00.000Z', '2026-02-02T10:26:00.000Z'
),
(
  'histcob_pay_731e93b7d6eac776', 'treatment', '13th Payment', '13th Payment', 'histcob_e8ceaad56e8e9283', '9832844964', 'Cooch Behar', 'RAJIB AHAMMED',
  '2026-02-06', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-06T10:27:00.000Z', '2026-02-06T10:27:00.000Z'
),
(
  'histcob_pay_a13132d175e686dc', 'treatment', '14th Payment', '14th Payment', 'histcob_e8ceaad56e8e9283', '9832844964', 'Cooch Behar', 'RAJIB AHAMMED',
  '2026-02-09', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-09T10:28:00.000Z', '2026-02-09T10:28:00.000Z'
),
(
  'histcob_pay_82fb07b20adb0d05', 'treatment', '15th Payment', '15th Payment', 'histcob_e8ceaad56e8e9283', '9832844964', 'Cooch Behar', 'RAJIB AHAMMED',
  '2026-02-13', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-13T10:29:00.000Z', '2026-02-13T10:29:00.000Z'
),
(
  'histcob_pay_4838ab0c675745be', 'treatment', '16th Payment', '16th Payment', 'histcob_e8ceaad56e8e9283', '9832844964', 'Cooch Behar', 'RAJIB AHAMMED',
  '2026-02-16', '3000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-16T10:30:00.000Z', '2026-02-16T10:30:00.000Z'
),
(
  'histcob_pay_6302c8232bec2da4', 'treatment', '17th Payment', '17th Payment', 'histcob_e8ceaad56e8e9283', '9832844964', 'Cooch Behar', 'RAJIB AHAMMED',
  '2026-02-20', '2000', 'CASH', 'Historical import (Cooch Behar 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-20T10:31:00.000Z', '2026-02-20T10:31:00.000Z'
);
