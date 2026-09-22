-- জলপাইগুড়ি ব্রাঞ্চ -- ২০২৩-২৪ পুরনো শিট (ALL_PAYTENTS_DETAILS) -- 61 জন
-- ৬৬ জন বিল-করা রোগীর মধ্যে ৫ জন বাদ (মোবাইল নেই/ভুল), ১টা ডুপ্লিকেট RIMPA DAS সারি বাদ
-- (TK নিশ্চিত করেছেন মোবাইল-ওয়ালা সারিটাই আসল), TAPAN SEN-এর TOTAL PAID শিটে ভুল
-- ছিল (১১৪১২৮), TK নিশ্চিত করেছেন আসল টাকা ৭৩০০০ (কিস্তির যোগফল)।
-- লাইভ-ডুপ্লিকেট-চেক (V1507) TK চালিয়ে দেখেছেন -- কেউ আগে থেকে নেই।

insert into public.patients (
  id, "patientId", date, "registrationDate", "visitDate",
  name, mobile, "altMobile", branch, age, sex,
  address, disease, bill,
  stage, queue, "doctorComplete",
  "createdBy", "registeredBy", "createdAt", "updatedAt"
) values
(
  'histjpe23_ea809004a83c473f', 'JPE-14022023-001', '2023-02-14', '2023-02-14', '2023-02-14',
  'MANAB ROY', '6296502696', '', 'Jalpaiguri', '25', 'Male',
  'DOMOHONI, MOYNAGURI, JALPAIGURI', 'Fistula', '21000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-14T10:00:00.000Z', '2023-02-14T10:00:00.000Z'
),
(
  'histjpe23_d4431cf402558445', 'JPE-14022023-002', '2023-02-14', '2023-02-14', '2023-02-14',
  'NIPU DAS', '8900753335', '', 'Jalpaiguri', '60', 'Male',
  'RAIKATPARA, RAIKARPARA, MOYNAGURI, JALPAIGURI', 'Piles', '8500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-14T10:00:00.000Z', '2023-02-14T10:00:00.000Z'
),
(
  'histjpe23_d218f2f0cd916098', 'JPE-18022023-001', '2023-02-18', '2023-02-18', '2023-02-18',
  'ASHA RANI ROY', '9475936679', '', 'Jalpaiguri', '66', 'Female',
  'PANDAPARA, KOTWALI, JALPAIGURI', 'Piles', '10500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-18T10:00:00.000Z', '2023-02-18T10:00:00.000Z'
),
(
  'histjpe23_1cc93a2f2d88e467', 'JPE-18022023-002', '2023-02-18', '2023-02-18', '2023-02-18',
  'JYOTI MONDAL', '9861748456', '', 'Jalpaiguri', '20', 'Female',
  'KASHIPUR, HABRA, HABRA, 24 PARGANAS NORTH', 'Piles', '11500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-18T10:00:00.000Z', '2023-02-18T10:00:00.000Z'
),
(
  'histjpe23_477e4d7fe4a55f95', 'JPE-21032023-001', '2023-03-21', '2023-03-21', '2023-03-21',
  'JHARNA DUTTA', '8927430017', '', 'Jalpaiguri', '47', 'Female',
  'MOYNAGURI, JALPAIGURI', 'Piles', '17500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-03-21T10:00:00.000Z', '2023-03-21T10:00:00.000Z'
),
(
  'histjpe23_d4b95509ebf30343', 'JPE-23032023-001', '2023-03-23', '2023-03-23', '2023-03-23',
  'ANANTA ROY', '9064923126', '', 'Jalpaiguri', '58', 'Male',
  'DUS DARGA, KAJIPARA, KOTWALI, JALPAIGURI', 'Hydrocele', '11500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-03-23T10:00:00.000Z', '2023-03-23T10:00:00.000Z'
),
(
  'histjpe23_5318e83b8512fae1', 'JPE-28032023-001', '2023-03-28', '2023-03-28', '2023-03-28',
  'SIBAK ROY', '9832163379', '', 'Jalpaiguri', '30', 'Male',
  'NAYAPARA, DANGAPARA, KOTWALI, JALPAIGURI', 'Fistula', '33000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-03-28T10:00:00.000Z', '2023-03-28T10:00:00.000Z'
),
(
  'histjpe23_ffe71c94786ce76b', 'JPE-10042023-001', '2023-04-10', '2023-04-10', '2023-04-10',
  'ARINDAM DAS', '8158849361', '', 'Jalpaiguri', '35', 'Male',
  'RAIKAT PARA, KOTWALI, JALPAIGURI', 'Piles', '13000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-10T10:00:00.000Z', '2023-04-10T10:00:00.000Z'
),
(
  'histjpe23_00240e6d20990101', 'JPE-15042023-001', '2023-04-15', '2023-04-15', '2023-04-15',
  'ABBAS ALI', '8943269630', '', 'Jalpaiguri', '36', 'Male',
  'SARKAR PARA, BAHADUR, KOTWALI, JALPAIGURI', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-15T10:00:00.000Z', '2023-04-15T10:00:00.000Z'
),
(
  'histjpe23_8738da4bfe8b873a', 'JPE-15042023-002', '2023-04-15', '2023-04-15', '2023-04-15',
  'NAJRUL ISLAM', '9641326486', '', 'Jalpaiguri', '35', 'Male',
  'BELACOBA, DANGAPARA, KOTWALI, JALPAIGURI', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-15T10:00:00.000Z', '2023-04-15T10:00:00.000Z'
),
(
  'histjpe23_a23ba1e68d76abe6', 'JPE-23042023-001', '2023-04-23', '2023-04-23', '2023-04-23',
  'SATISH ROY', '9083529422', '', 'Jalpaiguri', '43', 'Male',
  'JAMIDAR PARA, PAHARPUR, KOTWALI, JALPAIGURI', 'Piles', '12000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-23T10:00:00.000Z', '2023-04-23T10:00:00.000Z'
),
(
  'histjpe23_66c5eaf7ad211e80', 'JPE-29042023-001', '2023-04-29', '2023-04-29', '2023-04-29',
  'GOPAL MONDAL', '9101508244', '', 'Jalpaiguri', '51', 'Male',
  'RANIHAAT, RANIHAAT, MEKLIGANJ, COOCHBEHAR', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-29T10:00:00.000Z', '2023-04-29T10:00:00.000Z'
),
(
  'histjpe23_1425054735569f8b', 'JPE-02052023-001', '2023-05-02', '2023-05-02', '2023-05-02',
  'SWAPAN DAS', '7063517519', '', 'Jalpaiguri', '30', 'Male',
  'TAKIMARI, MILANPALLY, JALPAIGURI', 'Other', '6000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-02T10:00:00.000Z', '2023-05-02T10:00:00.000Z'
),
(
  'histjpe23_17b963ee3cc29775', 'JPE-06052023-001', '2023-05-06', '2023-05-06', '2023-05-06',
  'DIPAK KAR', '7602364418', '', 'Jalpaiguri', '41', 'Male',
  'MOHIT NAGAR, JALPAIGURI', 'Gupt Rog', '10500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-06T10:00:00.000Z', '2023-05-06T10:00:00.000Z'
),
(
  'histjpe23_8ece27893a1547bb', 'JPE-15052023-001', '2023-05-15', '2023-05-15', '2023-05-15',
  'ROBIN SARKAR', '7501993851', '', 'Jalpaiguri', '32', 'Male',
  'TAKIMARI, MILAN PALLY, JALPAIGURI', 'Piles', '15900',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-15T10:00:00.000Z', '2023-05-15T10:00:00.000Z'
),
(
  'histjpe23_50f8a10c5e186861', 'JPE-30052023-001', '2023-05-30', '2023-05-30', '2023-05-30',
  'BABLU BARMAN', '8436774353', '', 'Jalpaiguri', '35', 'Male',
  'RANINAGAR, JALPAIGURI', 'Fistula', '34500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-30T10:00:00.000Z', '2023-05-30T10:00:00.000Z'
),
(
  'histjpe23_1fe348291239448e', 'JPE-13062023-001', '2023-06-13', '2023-06-13', '2023-06-13',
  'RAMONI ROY', '8016343352', '', 'Jalpaiguri', '32', 'Male',
  'NAJIRPUR, JALPAIGURI', 'Piles', '17000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-13T10:00:00.000Z', '2023-06-13T10:00:00.000Z'
),
(
  'histjpe23_d36da34d5f25e9c9', 'JPE-25072023-001', '2023-07-25', '2023-07-25', '2023-07-25',
  'JHUTAN DAS', '6296067371', '', 'Jalpaiguri', '27', 'Male',
  'DAKSHIN CHARMANI, JALPAIGURI', 'Piles, Fistula', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-25T10:00:00.000Z', '2023-07-25T10:00:00.000Z'
),
(
  'histjpe23_a3ac2691723e00cb', 'JPE-21082023-001', '2023-08-21', '2023-08-21', '2023-08-21',
  'TAPAN BARMAN', '8670039287', '', 'Jalpaiguri', '26', 'Male',
  'JALPAIGURI', 'Fistula', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-21T10:00:00.000Z', '2023-08-21T10:00:00.000Z'
),
(
  'histjpe23_47547184bd60949f', 'JPE-26082023-001', '2023-08-26', '2023-08-26', '2023-08-26',
  'TARUN CHAKRABORTY', '8927268631', '', 'Jalpaiguri', '49', 'Male',
  '3NO GUMTI, JALPAIGURI', 'Piles', '17000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-26T10:00:00.000Z', '2023-08-26T10:00:00.000Z'
),
(
  'histjpe23_8c52ac23a71e4bde', 'JPE-06092023-001', '2023-09-06', '2023-09-06', '2023-09-06',
  'SUNITA AGARWAL', '9093738744', '', 'Jalpaiguri', '57', 'Male',
  'BARNIS, JALPAIGURI', 'Piles', '36000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-06T10:00:00.000Z', '2023-09-06T10:00:00.000Z'
),
(
  'histjpe23_72226e28afe23a19', 'JPE-09092023-001', '2023-09-09', '2023-09-09', '2023-09-09',
  'SARIFUL ISLAM', '9679475270', '', 'Jalpaiguri', '31', 'Male',
  '', 'Hydrocele', '14000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-09T10:00:00.000Z', '2023-09-09T10:00:00.000Z'
),
(
  'histjpe23_1f2e78ff21cfb588', 'JPE-12092023-001', '2023-09-12', '2023-09-12', '2023-09-12',
  'FIROZ ALI', '9547130190', '', 'Jalpaiguri', '26', 'Male',
  '', 'Piles', '16000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-12T10:00:00.000Z', '2023-09-12T10:00:00.000Z'
),
(
  'histjpe23_77a13d58dde71e79', 'JPE-27102023-001', '2023-10-27', '2023-10-27', '2023-10-27',
  'RUBEL RAHMAN', '6238876495', '', 'Jalpaiguri', '27', 'Male',
  'GHARAMARI, JALPAIGURI', 'Piles', '16000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-27T10:00:00.000Z', '2023-10-27T10:00:00.000Z'
),
(
  'histjpe23_9809a6406eaed5ea', 'JPE-06012024-001', '2024-01-06', '2024-01-06', '2024-01-06',
  'TARIKUL', '9167470535', '', 'Jalpaiguri', '50', 'Male',
  'NAMAJIPARA, PANDAPARA, JALPAIGURI', 'Piles', '13000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-06T10:00:00.000Z', '2024-01-06T10:00:00.000Z'
),
(
  'histjpe23_87a5e0bdc71e134b', 'JPE-12012024-001', '2024-01-12', '2024-01-12', '2024-01-12',
  'BILASH BARMAN', '9126083759', '', 'Jalpaiguri', '38', 'Male',
  'MATIGARA, SILIGURI', 'Piles', '16000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-12T10:00:00.000Z', '2024-01-12T10:00:00.000Z'
),
(
  'histjpe23_fdb434b0439d5a31', 'JPE-19012024-001', '2024-01-19', '2024-01-19', '2024-01-19',
  'MONIRUL ISLAM', '9749682654', '', 'Jalpaiguri', '20', 'Male',
  'ISLAMABAD, MADARIHAAT, ALIPURDUAR', 'Piles', '14000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-19T10:00:00.000Z', '2024-01-19T10:00:00.000Z'
),
(
  'histjpe23_96b779687f9e0ee0', 'JPE-21012024-001', '2024-01-21', '2024-01-21', '2024-01-21',
  'KRISHNA SARKAR', '8101609319', '', 'Jalpaiguri', '40', 'Male',
  'SUKURPARA, BAHADUR', 'Fistula', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-21T10:00:00.000Z', '2024-01-21T10:00:00.000Z'
),
(
  'histjpe23_a2f7df0bdcd867ed', 'JPE-02022024-001', '2024-02-02', '2024-02-02', '2024-02-02',
  'SAHIRUL HOWK', '8945887917', '', 'Jalpaiguri', '27', 'Male',
  'RAJGANJ, KHALPARA', 'Piles', '16500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-02T10:00:00.000Z', '2024-02-02T10:00:00.000Z'
),
(
  'histjpe23_a4ce5e32f4cd9579', 'JPE-02022024-002', '2024-02-02', '2024-02-02', '2024-02-02',
  'ABDUL SALAM', '8927514421', '', 'Jalpaiguri', '29', 'Male',
  'RAJGANJ, KHALPARA', 'Piles', '15400',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-02T10:00:00.000Z', '2024-02-02T10:00:00.000Z'
),
(
  'histjpe23_f4caf9937e3d2e53', 'JPE-17022024-001', '2024-02-17', '2024-02-17', '2024-02-17',
  'RIMPA DAS', '9674747857', '', 'Jalpaiguri', '33', 'Female',
  'RESEASPARA, JALPAIGURI', 'Piles', '13000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-17T10:00:00.000Z', '2024-02-17T10:00:00.000Z'
),
(
  'histjpe23_add6da410596222a', 'JPE-26022024-001', '2024-02-26', '2024-02-26', '2024-02-26',
  'NAYAN PAUL', '9832039824', '', 'Jalpaiguri', '33', 'Male',
  'MOHIT NAGAR', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-26T10:00:00.000Z', '2024-02-26T10:00:00.000Z'
),
(
  'histjpe23_51d6be82cf3606d7', 'JPE-01032024-001', '2024-03-01', '2024-03-01', '2024-03-01',
  'BISWAJIT SEN', '8016259482', '', 'Jalpaiguri', '', 'Male',
  'JALPAIGURI', 'Piles', '16000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-01T10:00:00.000Z', '2024-03-01T10:00:00.000Z'
),
(
  'histjpe23_01b17b9acaba153a', 'JPE-10042024-001', '2024-04-10', '2024-04-10', '2024-04-10',
  'MD ABU IMRAN', '8491090593', '', 'Jalpaiguri', '38', 'Male',
  'MEKHLIGANJ, JALPAIGURI', 'Piles', '16000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-10T10:00:00.000Z', '2024-04-10T10:00:00.000Z'
),
(
  'histjpe23_efbbdb0d8f6fecba', 'JPE-27042024-001', '2024-04-27', '2024-04-27', '2024-04-27',
  'REKHA DAS', '9883280951', '', 'Jalpaiguri', '23', 'Female',
  'MOHIT NAGAR', 'Piles', '19700',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-27T10:00:00.000Z', '2024-04-27T10:00:00.000Z'
),
(
  'histjpe23_3748d7a96589b39b', 'JPE-28042024-001', '2024-04-28', '2024-04-28', '2024-04-28',
  'RAMPRASAD ROY', '9800384561', '', 'Jalpaiguri', '22', 'Male',
  'MACHOPRA, DHAPGANJ', 'Piles', '21000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-28T10:00:00.000Z', '2024-04-28T10:00:00.000Z'
),
(
  'histjpe23_bd2521f2af1d669a', 'JPE-03052024-001', '2024-05-03', '2024-05-03', '2024-05-03',
  'ANIMESH ROY', '9382100434', '', 'Jalpaiguri', '24', 'Male',
  'TALMA', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-03T10:00:00.000Z', '2024-05-03T10:00:00.000Z'
),
(
  'histjpe23_54b5c2381cf3ab78', 'JPE-11052024-001', '2024-05-11', '2024-05-11', '2024-05-11',
  'SALAYA KHATOON', '9547486785', '', 'Jalpaiguri', '60', 'Female',
  'MADARIPUR, ALIPURDUAR', 'Piles', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-11T10:00:00.000Z', '2024-05-11T10:00:00.000Z'
),
(
  'histjpe23_b0db34a89b649d74', 'JPE-14052024-001', '2024-05-14', '2024-05-14', '2024-05-14',
  'AMITAV ROY', '8927978542', '', 'Jalpaiguri', '27', 'Male',
  'SANAR BARI, RAJGANJ', 'Fistula', '24000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-14T10:00:00.000Z', '2024-05-14T10:00:00.000Z'
),
(
  'histjpe23_6ef2aa3e371a0e44', 'JPE-21052024-001', '2024-05-21', '2024-05-21', '2024-05-21',
  'ALOK ROY', '8295192467', '', 'Jalpaiguri', '30', 'Male',
  'MANDALGHAT', 'Piles', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-21T10:00:00.000Z', '2024-05-21T10:00:00.000Z'
),
(
  'histjpe23_6b21bc7ed0fa7ec0', 'JPE-18062024-001', '2024-06-18', '2024-06-18', '2024-06-18',
  'JAGJIT MANDAL', '7001505012', '', 'Jalpaiguri', '36', 'Male',
  'SARKARPARA, RANGDHAMALI', 'Piles', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-18T10:00:00.000Z', '2024-06-18T10:00:00.000Z'
),
(
  'histjpe23_5f1d64bb9b179203', 'JPE-21062024-001', '2024-06-21', '2024-06-21', '2024-06-21',
  'MUKUL ROY', '8250604619', '', 'Jalpaiguri', '37', 'Male',
  'BAHADUR, SARAIMARI', 'Piles, Fistula', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-21T10:00:00.000Z', '2024-06-21T10:00:00.000Z'
),
(
  'histjpe23_2cb38499dcc5afe7', 'JPE-14072024-001', '2024-07-14', '2024-07-14', '2024-07-14',
  'MAMTA BALA', '9832178703', '', 'Jalpaiguri', '50', 'Female',
  'VIVEKANANDA PALLY, JALPAIGURI', 'Piles', '11000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-14T10:00:00.000Z', '2024-07-14T10:00:00.000Z'
),
(
  'histjpe23_4a7283979f7f11a5', 'JPE-16072024-001', '2024-07-16', '2024-07-16', '2024-07-16',
  'SATNA BASFO', '7583953177', '', 'Jalpaiguri', '35', 'Male',
  'JAYANTI PARA', 'Piles', '16000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-16T10:00:00.000Z', '2024-07-16T10:00:00.000Z'
),
(
  'histjpe23_c0a66004d22a8c82', 'JPE-27072024-001', '2024-07-27', '2024-07-27', '2024-07-27',
  'SUSHMITA SARKAR', '6296698321', '', 'Jalpaiguri', '30', 'Male',
  'BOLMARI, MOYNAGURI', 'Piles', '17000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-27T10:00:00.000Z', '2024-07-27T10:00:00.000Z'
),
(
  'histjpe23_3a510830de81815a', 'JPE-27072024-002', '2024-07-27', '2024-07-27', '2024-07-27',
  'RAJINA BEGAM', '7076960587', '', 'Jalpaiguri', '31', 'Female',
  'SOVAR HAAT, GARAL BARI', 'Piles', '13000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-27T10:00:00.000Z', '2024-07-27T10:00:00.000Z'
),
(
  'histjpe23_f0c9ec122a29ce40', 'JPE-10082024-001', '2024-08-10', '2024-08-10', '2024-08-10',
  'SWAPAN PANDIT', '8653744848', '', 'Jalpaiguri', '55', 'Male',
  'FATAPUKUR, RAJGANJ', 'Piles', '17500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-10T10:00:00.000Z', '2024-08-10T10:00:00.000Z'
),
(
  'histjpe23_43f5e4751142b312', 'JPE-12082024-001', '2024-08-12', '2024-08-12', '2024-08-12',
  'SOHEL AHMED', '8906507436', '', 'Jalpaiguri', '29', 'Male',
  'BAHADUR', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-12T10:00:00.000Z', '2024-08-12T10:00:00.000Z'
),
(
  'histjpe23_4c2011a40e558846', 'JPE-16082024-001', '2024-08-16', '2024-08-16', '2024-08-16',
  'SUBHAS ROY', '8016882184', '', 'Jalpaiguri', '50', 'Male',
  'DANGA PARA, PANDAPARA', 'Fistula', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-16T10:00:00.000Z', '2024-08-16T10:00:00.000Z'
),
(
  'histjpe23_3f660f999fa75a30', 'JPE-24082024-001', '2024-08-24', '2024-08-24', '2024-08-24',
  'ARUNA KHATOON', '7047624676', '', 'Jalpaiguri', '30', 'Female',
  'PRADHAN PARA, RAJGANJ', 'Piles', '24000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-24T10:00:00.000Z', '2024-08-24T10:00:00.000Z'
),
(
  'histjpe23_1b93df595edc5de8', 'JPE-28082024-001', '2024-08-28', '2024-08-28', '2024-08-28',
  'RINKU NINUYA', '8391809951', '', 'Jalpaiguri', '38', 'Male',
  'DOMOHANI', 'Fistula', '39000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-28T10:00:00.000Z', '2024-08-28T10:00:00.000Z'
),
(
  'histjpe23_24622147ffbaa48a', 'JPE-01092024-001', '2024-09-01', '2024-09-01', '2024-09-01',
  'POONAM BASU', '9339469563', '', 'Jalpaiguri', '30', 'Male',
  'RAIL COLONY, HALDIBARI', 'Piles', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-01T10:00:00.000Z', '2024-09-01T10:00:00.000Z'
),
(
  'histjpe23_0aeee475cb05a185', 'JPE-08092024-001', '2024-09-08', '2024-09-08', '2024-09-08',
  'MANABUL HOWK', '9832660311', '', 'Jalpaiguri', '37', 'Male',
  'NARSINPUR, FALAKATA', 'Piles', '24000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-08T10:00:00.000Z', '2024-09-08T10:00:00.000Z'
),
(
  'histjpe23_0a001b9ae3c551c7', 'JPE-09092024-001', '2024-09-09', '2024-09-09', '2024-09-09',
  'MANARUL ISLAM', '9325139698', '', 'Jalpaiguri', '33', 'Male',
  'UTTAR PADAMATI', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-09T10:00:00.000Z', '2024-09-09T10:00:00.000Z'
),
(
  'histjpe23_2b1eac58a1b9d3d7', 'JPE-09092024-002', '2024-09-09', '2024-09-09', '2024-09-09',
  'TAPAN SEN', '7866038920', '', 'Jalpaiguri', '34', 'Male',
  'SENPARA, JALPAIGURI', 'Fistula', '130000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-09T10:00:00.000Z', '2024-09-09T10:00:00.000Z'
),
(
  'histjpe23_6311b4310b2e44dc', 'JPE-30092024-001', '2024-09-30', '2024-09-30', '2024-09-30',
  'KONIKA SARKAR', '8101032976', '', 'Jalpaiguri', '32', 'Male',
  'MOHIT NAGAR', 'Piles', '21000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-30T10:00:00.000Z', '2024-09-30T10:00:00.000Z'
),
(
  'histjpe23_2ee6a402693b5daf', 'JPE-10112024-001', '2024-11-10', '2024-11-10', '2024-11-10',
  'MONMAHTO SARKAR', '8927746336', '', 'Jalpaiguri', '50', 'Male',
  'NJP, NJP, NJP', 'Piles', '21000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-10T10:00:00.000Z', '2024-11-10T10:00:00.000Z'
),
(
  'histjpe23_fd9ceb7ad637b5f7', 'JPE-14112024-001', '2024-11-14', '2024-11-14', '2024-11-14',
  'FERDOS RAHMAN', '8617486585', '', 'Jalpaiguri', '23', 'Male',
  'DHUPGURI, DHUPGURI', 'Piles', '16000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-14T10:00:00.000Z', '2024-11-14T10:00:00.000Z'
),
(
  'histjpe23_fff93fda04f97186', 'JPE-18112024-001', '2024-11-18', '2024-11-18', '2024-11-18',
  'PRIYANKA ROY', '8250545395', '', 'Jalpaiguri', '28', 'Male',
  'PAHARPUR, JALPAIGURI', 'Piles', '17000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-18T10:00:00.000Z', '2024-11-18T10:00:00.000Z'
),
(
  'histjpe23_ff8713a527f55eed', 'JPE-15122024-001', '2024-12-15', '2024-12-15', '2024-12-15',
  'DEBASISH ROY', '6295890602', '', 'Jalpaiguri', '43', 'Male',
  'RANINAGAR', 'Hydrocele', '10000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-15T10:00:00.000Z', '2024-12-15T10:00:00.000Z'
),
(
  'histjpe23_71c44d38079eb790', 'JPE-21122024-001', '2024-12-21', '2024-12-21', '2024-12-21',
  'MANOJ ROY', '7872339778', '', 'Jalpaiguri', '39', 'Male',
  'MALBAZAR', 'Hydrocele', '14000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-21T10:00:00.000Z', '2024-12-21T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'histjpe23_pay_2387d218e11cab56', 'treatment', 'Advance', 'Advance', 'histjpe23_ea809004a83c473f', '6296502696', 'Jalpaiguri', 'MANAB ROY',
  '2023-02-14', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-14T10:15:00.000Z', '2023-02-14T10:15:00.000Z'
),
(
  'histjpe23_pay_b31bf9a0047812e0', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_ea809004a83c473f', '6296502696', 'Jalpaiguri', 'MANAB ROY',
  '2023-02-14', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-14T10:16:00.000Z', '2023-02-14T10:16:00.000Z'
),
(
  'histjpe23_pay_65576807bbe03466', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_ea809004a83c473f', '6296502696', 'Jalpaiguri', 'MANAB ROY',
  '2023-02-17', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-17T10:17:00.000Z', '2023-02-17T10:17:00.000Z'
),
(
  'histjpe23_pay_04c3b1711e477d31', 'treatment', '4th Payment', '4th Payment', 'histjpe23_ea809004a83c473f', '6296502696', 'Jalpaiguri', 'MANAB ROY',
  '2023-02-25', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-25T10:18:00.000Z', '2023-02-25T10:18:00.000Z'
),
(
  'histjpe23_pay_0487f5ec04c8de2c', 'treatment', '5th Payment', '5th Payment', 'histjpe23_ea809004a83c473f', '6296502696', 'Jalpaiguri', 'MANAB ROY',
  '2023-02-28', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-28T10:19:00.000Z', '2023-02-28T10:19:00.000Z'
),
(
  'histjpe23_pay_0742205ece1deef6', 'treatment', '6th Payment', '6th Payment', 'histjpe23_ea809004a83c473f', '6296502696', 'Jalpaiguri', 'MANAB ROY',
  '2023-03-06', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-03-06T10:20:00.000Z', '2023-03-06T10:20:00.000Z'
),
(
  'histjpe23_pay_746855652cb052e1', 'treatment', '7th Payment', '7th Payment', 'histjpe23_ea809004a83c473f', '6296502696', 'Jalpaiguri', 'MANAB ROY',
  '2023-03-11', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-03-11T10:21:00.000Z', '2023-03-11T10:21:00.000Z'
),
(
  'histjpe23_pay_cf03f9f530b56cf7', 'treatment', '8th Payment', '8th Payment', 'histjpe23_ea809004a83c473f', '6296502696', 'Jalpaiguri', 'MANAB ROY',
  '2023-03-12', '100', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-03-12T10:22:00.000Z', '2023-03-12T10:22:00.000Z'
),
(
  'histjpe23_pay_cffe19b16db97b8d', 'treatment', '9th Payment', '9th Payment', 'histjpe23_ea809004a83c473f', '6296502696', 'Jalpaiguri', 'MANAB ROY',
  '2023-03-14', '1400', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-03-14T10:23:00.000Z', '2023-03-14T10:23:00.000Z'
),
(
  'histjpe23_pay_373178fb77cef9bd', 'treatment', 'Advance', 'Advance', 'histjpe23_d4431cf402558445', '8900753335', 'Jalpaiguri', 'NIPU DAS',
  '2023-02-14', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-14T10:15:00.000Z', '2023-02-14T10:15:00.000Z'
),
(
  'histjpe23_pay_5e39f089d4ca48fe', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_d4431cf402558445', '8900753335', 'Jalpaiguri', 'NIPU DAS',
  '2023-02-18', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-18T10:16:00.000Z', '2023-02-18T10:16:00.000Z'
),
(
  'histjpe23_pay_b46af4fa75ecf06a', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_d4431cf402558445', '8900753335', 'Jalpaiguri', 'NIPU DAS',
  '2023-02-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-21T10:17:00.000Z', '2023-02-21T10:17:00.000Z'
),
(
  'histjpe23_pay_83a4db606bf5499d', 'treatment', '4th Payment', '4th Payment', 'histjpe23_d4431cf402558445', '8900753335', 'Jalpaiguri', 'NIPU DAS',
  '2023-02-25', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-25T10:18:00.000Z', '2023-02-25T10:18:00.000Z'
),
(
  'histjpe23_pay_2a51b82591d8fbce', 'treatment', '5th Payment', '5th Payment', 'histjpe23_d4431cf402558445', '8900753335', 'Jalpaiguri', 'NIPU DAS',
  '2023-02-27', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-27T10:19:00.000Z', '2023-02-27T10:19:00.000Z'
),
(
  'histjpe23_pay_58f331f331c7125f', 'treatment', '6th Payment', '6th Payment', 'histjpe23_d4431cf402558445', '8900753335', 'Jalpaiguri', 'NIPU DAS',
  '2023-03-04', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-03-04T10:20:00.000Z', '2023-03-04T10:20:00.000Z'
),
(
  'histjpe23_pay_b87e0fa85a0db8ee', 'treatment', '7th Payment', '7th Payment', 'histjpe23_d4431cf402558445', '8900753335', 'Jalpaiguri', 'NIPU DAS',
  '2023-03-11', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-03-11T10:21:00.000Z', '2023-03-11T10:21:00.000Z'
),
(
  'histjpe23_pay_1622cf6448a7fc36', 'treatment', 'Advance', 'Advance', 'histjpe23_d218f2f0cd916098', '9475936679', 'Jalpaiguri', 'ASHA RANI ROY',
  '2023-02-18', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-18T10:15:00.000Z', '2023-02-18T10:15:00.000Z'
),
(
  'histjpe23_pay_c4003efd897a3f2f', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_d218f2f0cd916098', '9475936679', 'Jalpaiguri', 'ASHA RANI ROY',
  '2023-02-21', '9000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-21T10:16:00.000Z', '2023-02-21T10:16:00.000Z'
),
(
  'histjpe23_pay_d63d2b4290aee32e', 'treatment', 'Advance', 'Advance', 'histjpe23_1cc93a2f2d88e467', '9861748456', 'Jalpaiguri', 'JYOTI MONDAL',
  '2023-02-18', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-18T10:15:00.000Z', '2023-02-18T10:15:00.000Z'
),
(
  'histjpe23_pay_21e45f36aa41987f', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_1cc93a2f2d88e467', '9861748456', 'Jalpaiguri', 'JYOTI MONDAL',
  '2023-02-21', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-21T10:16:00.000Z', '2023-02-21T10:16:00.000Z'
),
(
  'histjpe23_pay_bf7940aca1fbd837', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_1cc93a2f2d88e467', '9861748456', 'Jalpaiguri', 'JYOTI MONDAL',
  '2023-02-28', '2500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-28T10:17:00.000Z', '2023-02-28T10:17:00.000Z'
),
(
  'histjpe23_pay_e0e9c8e90c9ed61a', 'treatment', '4th Payment', '4th Payment', 'histjpe23_1cc93a2f2d88e467', '9861748456', 'Jalpaiguri', 'JYOTI MONDAL',
  '2023-03-11', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-03-11T10:18:00.000Z', '2023-03-11T10:18:00.000Z'
),
(
  'histjpe23_pay_18577e0af86b869b', 'treatment', 'Advance', 'Advance', 'histjpe23_477e4d7fe4a55f95', '8927430017', 'Jalpaiguri', 'JHARNA DUTTA',
  '2023-03-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-03-21T10:15:00.000Z', '2023-03-21T10:15:00.000Z'
),
(
  'histjpe23_pay_18fee688650cfc6f', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_477e4d7fe4a55f95', '8927430017', 'Jalpaiguri', 'JHARNA DUTTA',
  '2023-03-28', '3500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-03-28T10:16:00.000Z', '2023-03-28T10:16:00.000Z'
),
(
  'histjpe23_pay_4846fdef33631fa6', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_477e4d7fe4a55f95', '8927430017', 'Jalpaiguri', 'JHARNA DUTTA',
  '2023-04-04', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-04T10:17:00.000Z', '2023-04-04T10:17:00.000Z'
),
(
  'histjpe23_pay_979883d8d0ef15de', 'treatment', '4th Payment', '4th Payment', 'histjpe23_477e4d7fe4a55f95', '8927430017', 'Jalpaiguri', 'JHARNA DUTTA',
  '2023-04-11', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-11T10:18:00.000Z', '2023-04-11T10:18:00.000Z'
),
(
  'histjpe23_pay_9994d283c9501836', 'treatment', '5th Payment', '5th Payment', 'histjpe23_477e4d7fe4a55f95', '8927430017', 'Jalpaiguri', 'JHARNA DUTTA',
  '2023-04-18', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-18T10:19:00.000Z', '2023-04-18T10:19:00.000Z'
),
(
  'histjpe23_pay_b64a860155ef979f', 'treatment', '6th Payment', '6th Payment', 'histjpe23_477e4d7fe4a55f95', '8927430017', 'Jalpaiguri', 'JHARNA DUTTA',
  '2023-05-08', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-08T10:20:00.000Z', '2023-05-08T10:20:00.000Z'
),
(
  'histjpe23_pay_413c6c4eab601fad', 'treatment', 'Advance', 'Advance', 'histjpe23_d4b95509ebf30343', '9064923126', 'Jalpaiguri', 'ANANTA ROY',
  '2023-04-23', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-23T10:15:00.000Z', '2023-04-23T10:15:00.000Z'
),
(
  'histjpe23_pay_b8f8d3034ad4c07c', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_d4b95509ebf30343', '9064923126', 'Jalpaiguri', 'ANANTA ROY',
  '2023-05-02', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-02T10:16:00.000Z', '2023-05-02T10:16:00.000Z'
),
(
  'histjpe23_pay_8a47c10781fdfe2a', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_d4b95509ebf30343', '9064923126', 'Jalpaiguri', 'ANANTA ROY',
  '2023-05-09', '4000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-09T10:17:00.000Z', '2023-05-09T10:17:00.000Z'
),
(
  'histjpe23_pay_fbc56ff7262fb3d3', 'treatment', 'Advance', 'Advance', 'histjpe23_5318e83b8512fae1', '9832163379', 'Jalpaiguri', 'SIBAK ROY',
  '2023-04-01', '3500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-01T10:15:00.000Z', '2023-04-01T10:15:00.000Z'
),
(
  'histjpe23_pay_bc56c8f6e0ea21c3', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_5318e83b8512fae1', '9832163379', 'Jalpaiguri', 'SIBAK ROY',
  '2023-04-04', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-04T10:16:00.000Z', '2023-04-04T10:16:00.000Z'
),
(
  'histjpe23_pay_c6746385b1693617', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_5318e83b8512fae1', '9832163379', 'Jalpaiguri', 'SIBAK ROY',
  '2023-04-08', '4000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-08T10:17:00.000Z', '2023-04-08T10:17:00.000Z'
),
(
  'histjpe23_pay_274461942e83920d', 'treatment', '4th Payment', '4th Payment', 'histjpe23_5318e83b8512fae1', '9832163379', 'Jalpaiguri', 'SIBAK ROY',
  '2023-04-11', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-11T10:18:00.000Z', '2023-04-11T10:18:00.000Z'
),
(
  'histjpe23_pay_cbe5a9231daa4a2d', 'treatment', '5th Payment', '5th Payment', 'histjpe23_5318e83b8512fae1', '9832163379', 'Jalpaiguri', 'SIBAK ROY',
  '2023-04-15', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-15T10:19:00.000Z', '2023-04-15T10:19:00.000Z'
),
(
  'histjpe23_pay_40e5b0e51c1e1ce3', 'treatment', '6th Payment', '6th Payment', 'histjpe23_5318e83b8512fae1', '9832163379', 'Jalpaiguri', 'SIBAK ROY',
  '2023-04-18', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-18T10:20:00.000Z', '2023-04-18T10:20:00.000Z'
),
(
  'histjpe23_pay_aa64d8a6d4c127a5', 'treatment', '7th Payment', '7th Payment', 'histjpe23_5318e83b8512fae1', '9832163379', 'Jalpaiguri', 'SIBAK ROY',
  '2023-04-22', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-22T10:21:00.000Z', '2023-04-22T10:21:00.000Z'
),
(
  'histjpe23_pay_977f1a61c1ea94a1', 'treatment', '8th Payment', '8th Payment', 'histjpe23_5318e83b8512fae1', '9832163379', 'Jalpaiguri', 'SIBAK ROY',
  '2023-04-25', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-25T10:22:00.000Z', '2023-04-25T10:22:00.000Z'
),
(
  'histjpe23_pay_fbba04bdc5d17da5', 'treatment', '9th Payment', '9th Payment', 'histjpe23_5318e83b8512fae1', '9832163379', 'Jalpaiguri', 'SIBAK ROY',
  '2023-04-29', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-29T10:23:00.000Z', '2023-04-29T10:23:00.000Z'
),
(
  'histjpe23_pay_fa6881006ecac652', 'treatment', '10th Payment', '10th Payment', 'histjpe23_5318e83b8512fae1', '9832163379', 'Jalpaiguri', 'SIBAK ROY',
  '2023-05-09', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-09T10:24:00.000Z', '2023-05-09T10:24:00.000Z'
),
(
  'histjpe23_pay_5f2d016070abf66b', 'treatment', 'Advance', 'Advance', 'histjpe23_ffe71c94786ce76b', '8158849361', 'Jalpaiguri', 'ARINDAM DAS',
  '2023-04-10', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-10T10:15:00.000Z', '2023-04-10T10:15:00.000Z'
),
(
  'histjpe23_pay_4fbb37eac80c84b0', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_ffe71c94786ce76b', '8158849361', 'Jalpaiguri', 'ARINDAM DAS',
  '2023-04-15', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-15T10:16:00.000Z', '2023-04-15T10:16:00.000Z'
),
(
  'histjpe23_pay_dac02fea793b759b', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_ffe71c94786ce76b', '8158849361', 'Jalpaiguri', 'ARINDAM DAS',
  '2023-04-24', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-24T10:17:00.000Z', '2023-04-24T10:17:00.000Z'
),
(
  'histjpe23_pay_da478a10d24e6418', 'treatment', '4th Payment', '4th Payment', 'histjpe23_ffe71c94786ce76b', '8158849361', 'Jalpaiguri', 'ARINDAM DAS',
  '2023-04-29', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-29T10:18:00.000Z', '2023-04-29T10:18:00.000Z'
),
(
  'histjpe23_pay_76218bd8efe2c4b5', 'treatment', '5th Payment', '5th Payment', 'histjpe23_ffe71c94786ce76b', '8158849361', 'Jalpaiguri', 'ARINDAM DAS',
  '2023-05-09', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-09T10:19:00.000Z', '2023-05-09T10:19:00.000Z'
),
(
  'histjpe23_pay_2646340363856776', 'treatment', '6th Payment', '6th Payment', 'histjpe23_ffe71c94786ce76b', '8158849361', 'Jalpaiguri', 'ARINDAM DAS',
  '2023-05-14', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-14T10:20:00.000Z', '2023-05-14T10:20:00.000Z'
),
(
  'histjpe23_pay_a203e0a4a99202e5', 'treatment', 'Advance', 'Advance', 'histjpe23_00240e6d20990101', '8943269630', 'Jalpaiguri', 'ABBAS ALI',
  '2023-04-15', '5300', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-15T10:15:00.000Z', '2023-04-15T10:15:00.000Z'
),
(
  'histjpe23_pay_a45abbdb0e774744', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_00240e6d20990101', '8943269630', 'Jalpaiguri', 'ABBAS ALI',
  '2023-04-18', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-18T10:16:00.000Z', '2023-04-18T10:16:00.000Z'
),
(
  'histjpe23_pay_55cd838ef1dddc05', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_00240e6d20990101', '8943269630', 'Jalpaiguri', 'ABBAS ALI',
  '2023-04-22', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-22T10:17:00.000Z', '2023-04-22T10:17:00.000Z'
),
(
  'histjpe23_pay_cd16a25643217372', 'treatment', '4th Payment', '4th Payment', 'histjpe23_00240e6d20990101', '8943269630', 'Jalpaiguri', 'ABBAS ALI',
  '2023-04-25', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-25T10:18:00.000Z', '2023-04-25T10:18:00.000Z'
),
(
  'histjpe23_pay_556c5554c14c187b', 'treatment', '5th Payment', '5th Payment', 'histjpe23_00240e6d20990101', '8943269630', 'Jalpaiguri', 'ABBAS ALI',
  '2023-05-02', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-02T10:19:00.000Z', '2023-05-02T10:19:00.000Z'
),
(
  'histjpe23_pay_a8418062fceb9a7c', 'treatment', '6th Payment', '6th Payment', 'histjpe23_00240e6d20990101', '8943269630', 'Jalpaiguri', 'ABBAS ALI',
  '2023-05-15', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-15T10:20:00.000Z', '2023-05-15T10:20:00.000Z'
),
(
  'histjpe23_pay_20620cbc9b36f6ad', 'treatment', '7th Payment', '7th Payment', 'histjpe23_00240e6d20990101', '8943269630', 'Jalpaiguri', 'ABBAS ALI',
  '2023-05-30', '700', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-30T10:21:00.000Z', '2023-05-30T10:21:00.000Z'
),
(
  'histjpe23_pay_93d3e8baa04d1fb1', 'treatment', 'Advance', 'Advance', 'histjpe23_8738da4bfe8b873a', '9641326486', 'Jalpaiguri', 'NAJRUL ISLAM',
  '2023-04-16', '2500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-16T10:15:00.000Z', '2023-04-16T10:15:00.000Z'
),
(
  'histjpe23_pay_bfd8b7d7943f47b4', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_8738da4bfe8b873a', '9641326486', 'Jalpaiguri', 'NAJRUL ISLAM',
  '2023-04-23', '2500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-23T10:16:00.000Z', '2023-04-23T10:16:00.000Z'
),
(
  'histjpe23_pay_6f470fb22273e94e', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_8738da4bfe8b873a', '9641326486', 'Jalpaiguri', 'NAJRUL ISLAM',
  '2023-05-01', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-01T10:17:00.000Z', '2023-05-01T10:17:00.000Z'
),
(
  'histjpe23_pay_4d6bdbda358f565b', 'treatment', '4th Payment', '4th Payment', 'histjpe23_8738da4bfe8b873a', '9641326486', 'Jalpaiguri', 'NAJRUL ISLAM',
  '2023-05-06', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-06T10:18:00.000Z', '2023-05-06T10:18:00.000Z'
),
(
  'histjpe23_pay_04448668d3ba39be', 'treatment', '5th Payment', '5th Payment', 'histjpe23_8738da4bfe8b873a', '9641326486', 'Jalpaiguri', 'NAJRUL ISLAM',
  '2023-05-09', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-09T10:19:00.000Z', '2023-05-09T10:19:00.000Z'
),
(
  'histjpe23_pay_eb5c03baad057aa3', 'treatment', '6th Payment', '6th Payment', 'histjpe23_8738da4bfe8b873a', '9641326486', 'Jalpaiguri', 'NAJRUL ISLAM',
  '2023-05-15', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-15T10:20:00.000Z', '2023-05-15T10:20:00.000Z'
),
(
  'histjpe23_pay_0e8d43340b19fcce', 'treatment', '7th Payment', '7th Payment', 'histjpe23_8738da4bfe8b873a', '9641326486', 'Jalpaiguri', 'NAJRUL ISLAM',
  '2023-06-05', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-05T10:21:00.000Z', '2023-06-05T10:21:00.000Z'
),
(
  'histjpe23_pay_5aea35d7484d84ed', 'treatment', '8th Payment', '8th Payment', 'histjpe23_8738da4bfe8b873a', '9641326486', 'Jalpaiguri', 'NAJRUL ISLAM',
  '2023-06-13', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-13T10:22:00.000Z', '2023-06-13T10:22:00.000Z'
),
(
  'histjpe23_pay_4565e677a7697f70', 'treatment', 'Advance', 'Advance', 'histjpe23_a23ba1e68d76abe6', '9083529422', 'Jalpaiguri', 'SATISH ROY',
  '2023-04-04', '6000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-04T10:15:00.000Z', '2023-04-04T10:15:00.000Z'
),
(
  'histjpe23_pay_a89d0af919096aec', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_a23ba1e68d76abe6', '9083529422', 'Jalpaiguri', 'SATISH ROY',
  '2023-04-18', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-18T10:16:00.000Z', '2023-04-18T10:16:00.000Z'
),
(
  'histjpe23_pay_b531469501f72a41', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_a23ba1e68d76abe6', '9083529422', 'Jalpaiguri', 'SATISH ROY',
  '2023-04-25', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-25T10:17:00.000Z', '2023-04-25T10:17:00.000Z'
),
(
  'histjpe23_pay_a7fa809659968445', 'treatment', 'Advance', 'Advance', 'histjpe23_66c5eaf7ad211e80', '9101508244', 'Jalpaiguri', 'GOPAL MONDAL',
  '2023-04-29', '2500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-29T10:15:00.000Z', '2023-04-29T10:15:00.000Z'
),
(
  'histjpe23_pay_12a912317d5e03e4', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_66c5eaf7ad211e80', '9101508244', 'Jalpaiguri', 'GOPAL MONDAL',
  '2023-05-02', '2500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-02T10:16:00.000Z', '2023-05-02T10:16:00.000Z'
),
(
  'histjpe23_pay_7d1af67cddcbde28', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_66c5eaf7ad211e80', '9101508244', 'Jalpaiguri', 'GOPAL MONDAL',
  '2023-05-06', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-06T10:17:00.000Z', '2023-05-06T10:17:00.000Z'
),
(
  'histjpe23_pay_ab97d7364e4bd891', 'treatment', '4th Payment', '4th Payment', 'histjpe23_66c5eaf7ad211e80', '9101508244', 'Jalpaiguri', 'GOPAL MONDAL',
  '2023-05-09', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-09T10:18:00.000Z', '2023-05-09T10:18:00.000Z'
),
(
  'histjpe23_pay_3ffbdfcc0c22d6b8', 'treatment', '5th Payment', '5th Payment', 'histjpe23_66c5eaf7ad211e80', '9101508244', 'Jalpaiguri', 'GOPAL MONDAL',
  '2023-05-23', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-23T10:19:00.000Z', '2023-05-23T10:19:00.000Z'
),
(
  'histjpe23_pay_53046cecb2cb9b06', 'treatment', 'Advance', 'Advance', 'histjpe23_1425054735569f8b', '7063517519', 'Jalpaiguri', 'SWAPAN DAS',
  '2023-05-02', '1200', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-02T10:15:00.000Z', '2023-05-02T10:15:00.000Z'
),
(
  'histjpe23_pay_14da7288d6c1a300', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_1425054735569f8b', '7063517519', 'Jalpaiguri', 'SWAPAN DAS',
  '2023-05-09', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-09T10:16:00.000Z', '2023-05-09T10:16:00.000Z'
),
(
  'histjpe23_pay_58907a2ff84c83be', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_1425054735569f8b', '7063517519', 'Jalpaiguri', 'SWAPAN DAS',
  '2023-05-16', '1200', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-16T10:17:00.000Z', '2023-05-16T10:17:00.000Z'
),
(
  'histjpe23_pay_6935a2fad29e6209', 'treatment', '4th Payment', '4th Payment', 'histjpe23_1425054735569f8b', '7063517519', 'Jalpaiguri', 'SWAPAN DAS',
  '2023-05-23', '1200', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-23T10:18:00.000Z', '2023-05-23T10:18:00.000Z'
),
(
  'histjpe23_pay_3fe7046bd45d1506', 'treatment', '5th Payment', '5th Payment', 'histjpe23_1425054735569f8b', '7063517519', 'Jalpaiguri', 'SWAPAN DAS',
  '2023-05-27', '1200', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-27T10:19:00.000Z', '2023-05-27T10:19:00.000Z'
),
(
  'histjpe23_pay_6761ba6a14c867d2', 'treatment', 'Advance', 'Advance', 'histjpe23_17b963ee3cc29775', '7602364418', 'Jalpaiguri', 'DIPAK KAR',
  '2023-05-06', '10500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-06T10:15:00.000Z', '2023-05-06T10:15:00.000Z'
),
(
  'histjpe23_pay_85fe25a7c0c09aa2', 'treatment', 'Advance', 'Advance', 'histjpe23_8ece27893a1547bb', '7501993851', 'Jalpaiguri', 'ROBIN SARKAR',
  '2023-05-15', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-15T10:15:00.000Z', '2023-05-15T10:15:00.000Z'
),
(
  'histjpe23_pay_58b56d7e9a6f3591', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_8ece27893a1547bb', '7501993851', 'Jalpaiguri', 'ROBIN SARKAR',
  '2023-05-22', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-22T10:16:00.000Z', '2023-05-22T10:16:00.000Z'
),
(
  'histjpe23_pay_571101fac879982c', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_8ece27893a1547bb', '7501993851', 'Jalpaiguri', 'ROBIN SARKAR',
  '2023-05-30', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-30T10:17:00.000Z', '2023-05-30T10:17:00.000Z'
),
(
  'histjpe23_pay_34a0a97c7c63b15d', 'treatment', '4th Payment', '4th Payment', 'histjpe23_8ece27893a1547bb', '7501993851', 'Jalpaiguri', 'ROBIN SARKAR',
  '2023-06-05', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-05T10:18:00.000Z', '2023-06-05T10:18:00.000Z'
),
(
  'histjpe23_pay_cfd0eb720013c085', 'treatment', '5th Payment', '5th Payment', 'histjpe23_8ece27893a1547bb', '7501993851', 'Jalpaiguri', 'ROBIN SARKAR',
  '2023-06-12', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-12T10:19:00.000Z', '2023-06-12T10:19:00.000Z'
),
(
  'histjpe23_pay_fb6d21bbe5faa9a5', 'treatment', '6th Payment', '6th Payment', 'histjpe23_8ece27893a1547bb', '7501993851', 'Jalpaiguri', 'ROBIN SARKAR',
  '2023-06-24', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-24T10:20:00.000Z', '2023-06-24T10:20:00.000Z'
),
(
  'histjpe23_pay_6300389e027196f8', 'treatment', 'Advance', 'Advance', 'histjpe23_50f8a10c5e186861', '8436774353', 'Jalpaiguri', 'BABLU BARMAN',
  '2023-05-30', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-30T10:15:00.000Z', '2023-05-30T10:15:00.000Z'
),
(
  'histjpe23_pay_3bb5d0261fe5a8e2', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_50f8a10c5e186861', '8436774353', 'Jalpaiguri', 'BABLU BARMAN',
  '2023-06-06', '10000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-06T10:16:00.000Z', '2023-06-06T10:16:00.000Z'
),
(
  'histjpe23_pay_adbf856a7ea64c40', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_50f8a10c5e186861', '8436774353', 'Jalpaiguri', 'BABLU BARMAN',
  '2023-06-13', '4000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-13T10:17:00.000Z', '2023-06-13T10:17:00.000Z'
),
(
  'histjpe23_pay_ca2ff6f75e81259a', 'treatment', '4th Payment', '4th Payment', 'histjpe23_50f8a10c5e186861', '8436774353', 'Jalpaiguri', 'BABLU BARMAN',
  '2023-06-17', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-17T10:18:00.000Z', '2023-06-17T10:18:00.000Z'
),
(
  'histjpe23_pay_af551b20179165e3', 'treatment', '5th Payment', '5th Payment', 'histjpe23_50f8a10c5e186861', '8436774353', 'Jalpaiguri', 'BABLU BARMAN',
  '2023-06-20', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-20T10:19:00.000Z', '2023-06-20T10:19:00.000Z'
),
(
  'histjpe23_pay_5aff15b81c2847ec', 'treatment', '6th Payment', '6th Payment', 'histjpe23_50f8a10c5e186861', '8436774353', 'Jalpaiguri', 'BABLU BARMAN',
  '2023-06-24', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-24T10:20:00.000Z', '2023-06-24T10:20:00.000Z'
),
(
  'histjpe23_pay_60517591e3239864', 'treatment', '7th Payment', '7th Payment', 'histjpe23_50f8a10c5e186861', '8436774353', 'Jalpaiguri', 'BABLU BARMAN',
  '2023-07-11', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-11T10:21:00.000Z', '2023-07-11T10:21:00.000Z'
),
(
  'histjpe23_pay_f4d3372fe3866605', 'treatment', '8th Payment', '8th Payment', 'histjpe23_50f8a10c5e186861', '8436774353', 'Jalpaiguri', 'BABLU BARMAN',
  '2023-07-15', '7500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-15T10:22:00.000Z', '2023-07-15T10:22:00.000Z'
),
(
  'histjpe23_pay_d8fd13b104b9c914', 'treatment', 'Advance', 'Advance', 'histjpe23_1fe348291239448e', '8016343352', 'Jalpaiguri', 'RAMONI ROY',
  '2023-06-13', '6000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-13T10:15:00.000Z', '2023-06-13T10:15:00.000Z'
),
(
  'histjpe23_pay_2935669bddfb4bf7', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_1fe348291239448e', '8016343352', 'Jalpaiguri', 'RAMONI ROY',
  '2023-06-24', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-24T10:16:00.000Z', '2023-06-24T10:16:00.000Z'
),
(
  'histjpe23_pay_030c24d9627acf00', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_1fe348291239448e', '8016343352', 'Jalpaiguri', 'RAMONI ROY',
  '2023-06-26', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-26T10:17:00.000Z', '2023-06-26T10:17:00.000Z'
),
(
  'histjpe23_pay_32082e5080f3cbbf', 'treatment', '4th Payment', '4th Payment', 'histjpe23_1fe348291239448e', '8016343352', 'Jalpaiguri', 'RAMONI ROY',
  '2023-07-02', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-02T10:18:00.000Z', '2023-07-02T10:18:00.000Z'
),
(
  'histjpe23_pay_f19462bf9bebd9b8', 'treatment', '5th Payment', '5th Payment', 'histjpe23_1fe348291239448e', '8016343352', 'Jalpaiguri', 'RAMONI ROY',
  '2023-07-04', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-04T10:19:00.000Z', '2023-07-04T10:19:00.000Z'
),
(
  'histjpe23_pay_641dc18d5fad0d6d', 'treatment', '6th Payment', '6th Payment', 'histjpe23_1fe348291239448e', '8016343352', 'Jalpaiguri', 'RAMONI ROY',
  '2023-07-11', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-11T10:20:00.000Z', '2023-07-11T10:20:00.000Z'
),
(
  'histjpe23_pay_c60dbcd1781e1a9d', 'treatment', '7th Payment', '7th Payment', 'histjpe23_1fe348291239448e', '8016343352', 'Jalpaiguri', 'RAMONI ROY',
  '2023-07-23', '100', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-23T10:21:00.000Z', '2023-07-23T10:21:00.000Z'
),
(
  'histjpe23_pay_3eb443d3f6d6e057', 'treatment', '8th Payment', '8th Payment', 'histjpe23_1fe348291239448e', '8016343352', 'Jalpaiguri', 'RAMONI ROY',
  '2023-08-01', '400', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-01T10:22:00.000Z', '2023-08-01T10:22:00.000Z'
),
(
  'histjpe23_pay_877c8b51be94954a', 'treatment', 'Advance', 'Advance', 'histjpe23_d36da34d5f25e9c9', '6296067371', 'Jalpaiguri', 'JHUTAN DAS',
  '2023-07-25', '7000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-25T10:15:00.000Z', '2023-07-25T10:15:00.000Z'
),
(
  'histjpe23_pay_a2a1102cce37d3e1', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_d36da34d5f25e9c9', '6296067371', 'Jalpaiguri', 'JHUTAN DAS',
  '2023-07-29', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-29T10:16:00.000Z', '2023-07-29T10:16:00.000Z'
),
(
  'histjpe23_pay_ca1d56f8f3045b69', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_d36da34d5f25e9c9', '6296067371', 'Jalpaiguri', 'JHUTAN DAS',
  '2023-08-01', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-01T10:17:00.000Z', '2023-08-01T10:17:00.000Z'
),
(
  'histjpe23_pay_cae4fd39461e5579', 'treatment', '4th Payment', '4th Payment', 'histjpe23_d36da34d5f25e9c9', '6296067371', 'Jalpaiguri', 'JHUTAN DAS',
  '2023-08-05', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-05T10:18:00.000Z', '2023-08-05T10:18:00.000Z'
),
(
  'histjpe23_pay_1df7a8f9a58cbe55', 'treatment', '5th Payment', '5th Payment', 'histjpe23_d36da34d5f25e9c9', '6296067371', 'Jalpaiguri', 'JHUTAN DAS',
  '2023-08-08', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-08T10:19:00.000Z', '2023-08-08T10:19:00.000Z'
),
(
  'histjpe23_pay_03eb2a1c5357823d', 'treatment', '6th Payment', '6th Payment', 'histjpe23_d36da34d5f25e9c9', '6296067371', 'Jalpaiguri', 'JHUTAN DAS',
  '2023-08-11', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-11T10:20:00.000Z', '2023-08-11T10:20:00.000Z'
),
(
  'histjpe23_pay_82c2c2dadff50f04', 'treatment', '7th Payment', '7th Payment', 'histjpe23_d36da34d5f25e9c9', '6296067371', 'Jalpaiguri', 'JHUTAN DAS',
  '2023-08-17', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-17T10:21:00.000Z', '2023-08-17T10:21:00.000Z'
),
(
  'histjpe23_pay_bfe2aa7268063322', 'treatment', '8th Payment', '8th Payment', 'histjpe23_d36da34d5f25e9c9', '6296067371', 'Jalpaiguri', 'JHUTAN DAS',
  '2023-08-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-21T10:22:00.000Z', '2023-08-21T10:22:00.000Z'
),
(
  'histjpe23_pay_c9b5117adc079c3d', 'treatment', '9th Payment', '9th Payment', 'histjpe23_d36da34d5f25e9c9', '6296067371', 'Jalpaiguri', 'JHUTAN DAS',
  '2023-08-01', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-01T10:23:00.000Z', '2023-08-01T10:23:00.000Z'
),
(
  'histjpe23_pay_adac5f344fb94a03', 'treatment', '10th Payment', '10th Payment', 'histjpe23_d36da34d5f25e9c9', '6296067371', 'Jalpaiguri', 'JHUTAN DAS',
  '2023-08-08', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-08T10:24:00.000Z', '2023-08-08T10:24:00.000Z'
),
(
  'histjpe23_pay_7003e2b124296a6d', 'treatment', '11th Payment', '11th Payment', 'histjpe23_d36da34d5f25e9c9', '6296067371', 'Jalpaiguri', 'JHUTAN DAS',
  '2023-08-15', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-15T10:25:00.000Z', '2023-08-15T10:25:00.000Z'
),
(
  'histjpe23_pay_f3d5302b7eaa3b5f', 'treatment', '12th Payment', '12th Payment', 'histjpe23_d36da34d5f25e9c9', '6296067371', 'Jalpaiguri', 'JHUTAN DAS',
  '2023-08-19', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-19T10:26:00.000Z', '2023-08-19T10:26:00.000Z'
),
(
  'histjpe23_pay_3aca2506259d365c', 'treatment', '13th Payment', '13th Payment', 'histjpe23_d36da34d5f25e9c9', '6296067371', 'Jalpaiguri', 'JHUTAN DAS',
  '2023-08-29', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-29T10:27:00.000Z', '2023-08-29T10:27:00.000Z'
),
(
  'histjpe23_pay_78bed9179d702930', 'treatment', '14th Payment', '14th Payment', 'histjpe23_d36da34d5f25e9c9', '6296067371', 'Jalpaiguri', 'JHUTAN DAS',
  '2023-12-01', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-12-01T10:28:00.000Z', '2023-12-01T10:28:00.000Z'
),
(
  'histjpe23_pay_d7528bdbe532dfcd', 'treatment', 'Advance', 'Advance', 'histjpe23_47547184bd60949f', '8927268631', 'Jalpaiguri', 'TARUN CHAKRABORTY',
  '2023-09-30', '10500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-30T10:15:00.000Z', '2023-09-30T10:15:00.000Z'
),
(
  'histjpe23_pay_7fc0d8635f98f8fd', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_47547184bd60949f', '8927268631', 'Jalpaiguri', 'TARUN CHAKRABORTY',
  '2023-10-07', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-07T10:16:00.000Z', '2023-10-07T10:16:00.000Z'
),
(
  'histjpe23_pay_226da857bae241f2', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_47547184bd60949f', '8927268631', 'Jalpaiguri', 'TARUN CHAKRABORTY',
  '2023-10-14', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-14T10:17:00.000Z', '2023-10-14T10:17:00.000Z'
),
(
  'histjpe23_pay_314ad45028b0992f', 'treatment', 'Advance', 'Advance', 'histjpe23_8c52ac23a71e4bde', '9093738744', 'Jalpaiguri', 'SUNITA AGARWAL',
  '2023-09-06', '9000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-06T10:15:00.000Z', '2023-09-06T10:15:00.000Z'
),
(
  'histjpe23_pay_fcea01269c13c772', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_8c52ac23a71e4bde', '9093738744', 'Jalpaiguri', 'SUNITA AGARWAL',
  '2023-09-22', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-22T10:16:00.000Z', '2023-09-22T10:16:00.000Z'
),
(
  'histjpe23_pay_0344c801533f4c62', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_8c52ac23a71e4bde', '9093738744', 'Jalpaiguri', 'SUNITA AGARWAL',
  '2023-09-29', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-29T10:17:00.000Z', '2023-09-29T10:17:00.000Z'
),
(
  'histjpe23_pay_dbf0982c19673b27', 'treatment', '4th Payment', '4th Payment', 'histjpe23_8c52ac23a71e4bde', '9093738744', 'Jalpaiguri', 'SUNITA AGARWAL',
  '2023-10-13', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-13T10:18:00.000Z', '2023-10-13T10:18:00.000Z'
),
(
  'histjpe23_pay_a05079f48ad82854', 'treatment', 'Advance', 'Advance', 'histjpe23_72226e28afe23a19', '9679475270', 'Jalpaiguri', 'SARIFUL ISLAM',
  '2023-09-09', '9000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-09T10:15:00.000Z', '2023-09-09T10:15:00.000Z'
),
(
  'histjpe23_pay_d6827ca7275d5b17', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_72226e28afe23a19', '9679475270', 'Jalpaiguri', 'SARIFUL ISLAM',
  '2023-10-13', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-13T10:16:00.000Z', '2023-10-13T10:16:00.000Z'
),
(
  'histjpe23_pay_8d54f108e3d3a6e6', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_72226e28afe23a19', '9679475270', 'Jalpaiguri', 'SARIFUL ISLAM',
  '2023-10-28', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-28T10:17:00.000Z', '2023-10-28T10:17:00.000Z'
),
(
  'histjpe23_pay_d2789ea006d4dc02', 'treatment', 'Advance', 'Advance', 'histjpe23_1f2e78ff21cfb588', '9547130190', 'Jalpaiguri', 'FIROZ ALI',
  '2023-09-12', '8000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-12T10:15:00.000Z', '2023-09-12T10:15:00.000Z'
),
(
  'histjpe23_pay_2b9a8e23272e489e', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_1f2e78ff21cfb588', '9547130190', 'Jalpaiguri', 'FIROZ ALI',
  '2023-10-06', '100', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-06T10:16:00.000Z', '2023-10-06T10:16:00.000Z'
),
(
  'histjpe23_pay_5c70d80331f3e3bd', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_1f2e78ff21cfb588', '9547130190', 'Jalpaiguri', 'FIROZ ALI',
  '2023-10-07', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-07T10:17:00.000Z', '2023-10-07T10:17:00.000Z'
),
(
  'histjpe23_pay_24778a58f08a5b50', 'treatment', '4th Payment', '4th Payment', 'histjpe23_1f2e78ff21cfb588', '9547130190', 'Jalpaiguri', 'FIROZ ALI',
  '2023-10-09', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-09T10:18:00.000Z', '2023-10-09T10:18:00.000Z'
),
(
  'histjpe23_pay_4f2b01bb88f79484', 'treatment', '5th Payment', '5th Payment', 'histjpe23_1f2e78ff21cfb588', '9547130190', 'Jalpaiguri', 'FIROZ ALI',
  '2023-10-14', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-14T10:19:00.000Z', '2023-10-14T10:19:00.000Z'
),
(
  'histjpe23_pay_b3a3c02c58884431', 'treatment', '6th Payment', '6th Payment', 'histjpe23_1f2e78ff21cfb588', '9547130190', 'Jalpaiguri', 'FIROZ ALI',
  '2024-03-27', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-27T10:20:00.000Z', '2024-03-27T10:20:00.000Z'
),
(
  'histjpe23_pay_01d9b7cc381b070e', 'treatment', 'Advance', 'Advance', 'histjpe23_77a13d58dde71e79', '6238876495', 'Jalpaiguri', 'RUBEL RAHMAN',
  '2023-10-27', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-27T10:15:00.000Z', '2023-10-27T10:15:00.000Z'
),
(
  'histjpe23_pay_38b188d870e1dd15', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_77a13d58dde71e79', '6238876495', 'Jalpaiguri', 'RUBEL RAHMAN',
  '2023-11-04', '4500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-11-04T10:16:00.000Z', '2023-11-04T10:16:00.000Z'
),
(
  'histjpe23_pay_5d72f65ba0677235', 'treatment', 'Advance', 'Advance', 'histjpe23_9809a6406eaed5ea', '9167470535', 'Jalpaiguri', 'TARIKUL',
  '2024-01-06', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-06T10:15:00.000Z', '2024-01-06T10:15:00.000Z'
),
(
  'histjpe23_pay_024a5cf42f379924', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_9809a6406eaed5ea', '9167470535', 'Jalpaiguri', 'TARIKUL',
  '2024-01-08', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-08T10:16:00.000Z', '2024-01-08T10:16:00.000Z'
),
(
  'histjpe23_pay_90f02df5c2e5c27d', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_9809a6406eaed5ea', '9167470535', 'Jalpaiguri', 'TARIKUL',
  '2024-01-12', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-12T10:17:00.000Z', '2024-01-12T10:17:00.000Z'
),
(
  'histjpe23_pay_e7e38d7e1bbdcf69', 'treatment', '4th Payment', '4th Payment', 'histjpe23_9809a6406eaed5ea', '9167470535', 'Jalpaiguri', 'TARIKUL',
  '2024-01-22', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-22T10:18:00.000Z', '2024-01-22T10:18:00.000Z'
),
(
  'histjpe23_pay_cfdec30e36441aca', 'treatment', 'Advance', 'Advance', 'histjpe23_87a5e0bdc71e134b', '9126083759', 'Jalpaiguri', 'BILASH BARMAN',
  '2024-01-12', '4000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-12T10:15:00.000Z', '2024-01-12T10:15:00.000Z'
),
(
  'histjpe23_pay_e82dae6a53a831ac', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_87a5e0bdc71e134b', '9126083759', 'Jalpaiguri', 'BILASH BARMAN',
  '2024-01-15', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-15T10:16:00.000Z', '2024-01-15T10:16:00.000Z'
),
(
  'histjpe23_pay_e954c7c5cf6c4803', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_87a5e0bdc71e134b', '9126083759', 'Jalpaiguri', 'BILASH BARMAN',
  '2024-01-27', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-27T10:17:00.000Z', '2024-01-27T10:17:00.000Z'
),
(
  'histjpe23_pay_efbe26d8d36f075c', 'treatment', '4th Payment', '4th Payment', 'histjpe23_87a5e0bdc71e134b', '9126083759', 'Jalpaiguri', 'BILASH BARMAN',
  '2024-02-05', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-05T10:18:00.000Z', '2024-02-05T10:18:00.000Z'
),
(
  'histjpe23_pay_021aa2d4b39f4b04', 'treatment', 'Advance', 'Advance', 'histjpe23_fdb434b0439d5a31', '9749682654', 'Jalpaiguri', 'MONIRUL ISLAM',
  '2024-01-19', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-19T10:15:00.000Z', '2024-01-19T10:15:00.000Z'
),
(
  'histjpe23_pay_12a7e0ab923d1fae', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_fdb434b0439d5a31', '9749682654', 'Jalpaiguri', 'MONIRUL ISLAM',
  '2024-01-22', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-22T10:16:00.000Z', '2024-01-22T10:16:00.000Z'
),
(
  'histjpe23_pay_11324c302972742f', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_fdb434b0439d5a31', '9749682654', 'Jalpaiguri', 'MONIRUL ISLAM',
  '2024-01-27', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-27T10:17:00.000Z', '2024-01-27T10:17:00.000Z'
),
(
  'histjpe23_pay_9092a744ae8fc16f', 'treatment', '4th Payment', '4th Payment', 'histjpe23_fdb434b0439d5a31', '9749682654', 'Jalpaiguri', 'MONIRUL ISLAM',
  '2024-02-13', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-13T10:18:00.000Z', '2024-02-13T10:18:00.000Z'
),
(
  'histjpe23_pay_58e6ed39bd204db2', 'treatment', '5th Payment', '5th Payment', 'histjpe23_fdb434b0439d5a31', '9749682654', 'Jalpaiguri', 'MONIRUL ISLAM',
  '2024-02-17', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-17T10:19:00.000Z', '2024-02-17T10:19:00.000Z'
),
(
  'histjpe23_pay_b09f8c1c810ed45b', 'treatment', '6th Payment', '6th Payment', 'histjpe23_fdb434b0439d5a31', '9749682654', 'Jalpaiguri', 'MONIRUL ISLAM',
  '2024-03-02', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-02T10:20:00.000Z', '2024-03-02T10:20:00.000Z'
),
(
  'histjpe23_pay_76f12d5c746f78e4', 'treatment', 'Advance', 'Advance', 'histjpe23_96b779687f9e0ee0', '8101609319', 'Jalpaiguri', 'KRISHNA SARKAR',
  '2024-10-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-21T10:15:00.000Z', '2024-10-21T10:15:00.000Z'
),
(
  'histjpe23_pay_b848994a2ffb1c82', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_96b779687f9e0ee0', '8101609319', 'Jalpaiguri', 'KRISHNA SARKAR',
  '2024-10-25', '4000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-25T10:16:00.000Z', '2024-10-25T10:16:00.000Z'
),
(
  'histjpe23_pay_450f28783fa69ffe', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_96b779687f9e0ee0', '8101609319', 'Jalpaiguri', 'KRISHNA SARKAR',
  '2024-10-28', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-28T10:17:00.000Z', '2024-10-28T10:17:00.000Z'
),
(
  'histjpe23_pay_a9b9d010350b6a0c', 'treatment', '4th Payment', '4th Payment', 'histjpe23_96b779687f9e0ee0', '8101609319', 'Jalpaiguri', 'KRISHNA SARKAR',
  '2024-11-11', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-11T10:18:00.000Z', '2024-11-11T10:18:00.000Z'
),
(
  'histjpe23_pay_692cbeb65afe09bb', 'treatment', '5th Payment', '5th Payment', 'histjpe23_96b779687f9e0ee0', '8101609319', 'Jalpaiguri', 'KRISHNA SARKAR',
  '2024-11-16', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-16T10:19:00.000Z', '2024-11-16T10:19:00.000Z'
),
(
  'histjpe23_pay_2bb3a8544984cd4c', 'treatment', '6th Payment', '6th Payment', 'histjpe23_96b779687f9e0ee0', '8101609319', 'Jalpaiguri', 'KRISHNA SARKAR',
  '2024-11-18', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-18T10:20:00.000Z', '2024-11-18T10:20:00.000Z'
),
(
  'histjpe23_pay_9517b04d4f0b19f1', 'treatment', '7th Payment', '7th Payment', 'histjpe23_96b779687f9e0ee0', '8101609319', 'Jalpaiguri', 'KRISHNA SARKAR',
  '2024-11-23', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-23T10:21:00.000Z', '2024-11-23T10:21:00.000Z'
),
(
  'histjpe23_pay_3a5b222147f5dc58', 'treatment', '8th Payment', '8th Payment', 'histjpe23_96b779687f9e0ee0', '8101609319', 'Jalpaiguri', 'KRISHNA SARKAR',
  '2024-12-11', '4000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-11T10:22:00.000Z', '2024-12-11T10:22:00.000Z'
),
(
  'histjpe23_pay_21647c008491faa1', 'treatment', '9th Payment', '9th Payment', 'histjpe23_96b779687f9e0ee0', '8101609319', 'Jalpaiguri', 'KRISHNA SARKAR',
  '2024-12-13', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-13T10:23:00.000Z', '2024-12-13T10:23:00.000Z'
),
(
  'histjpe23_pay_340c1fd9d0a5fa86', 'treatment', '10th Payment', '10th Payment', 'histjpe23_96b779687f9e0ee0', '8101609319', 'Jalpaiguri', 'KRISHNA SARKAR',
  '2025-01-18', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-18T10:24:00.000Z', '2025-01-18T10:24:00.000Z'
),
(
  'histjpe23_pay_e6618685f6b246e8', 'treatment', '11th Payment', '11th Payment', 'histjpe23_96b779687f9e0ee0', '8101609319', 'Jalpaiguri', 'KRISHNA SARKAR',
  '2025-02-01', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-01T10:25:00.000Z', '2025-02-01T10:25:00.000Z'
),
(
  'histjpe23_pay_d98c7dd49a91d69c', 'treatment', 'Advance', 'Advance', 'histjpe23_a2f7df0bdcd867ed', '8945887917', 'Jalpaiguri', 'SAHIRUL HOWK',
  '2024-02-02', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-02T10:15:00.000Z', '2024-02-02T10:15:00.000Z'
),
(
  'histjpe23_pay_dea7e9e7c16afb60', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_a2f7df0bdcd867ed', '8945887917', 'Jalpaiguri', 'SAHIRUL HOWK',
  '2024-02-05', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-05T10:16:00.000Z', '2024-02-05T10:16:00.000Z'
),
(
  'histjpe23_pay_1f601f7071017e3f', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_a2f7df0bdcd867ed', '8945887917', 'Jalpaiguri', 'SAHIRUL HOWK',
  '2024-04-12', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-12T10:17:00.000Z', '2024-04-12T10:17:00.000Z'
),
(
  'histjpe23_pay_9c0a53e819acf384', 'treatment', '4th Payment', '4th Payment', 'histjpe23_a2f7df0bdcd867ed', '8945887917', 'Jalpaiguri', 'SAHIRUL HOWK',
  '2024-04-20', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-20T10:18:00.000Z', '2024-04-20T10:18:00.000Z'
),
(
  'histjpe23_pay_0273184627ca13bb', 'treatment', '5th Payment', '5th Payment', 'histjpe23_a2f7df0bdcd867ed', '8945887917', 'Jalpaiguri', 'SAHIRUL HOWK',
  '2024-04-27', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-27T10:19:00.000Z', '2024-04-27T10:19:00.000Z'
),
(
  'histjpe23_pay_dbce7abb979dcfe3', 'treatment', '6th Payment', '6th Payment', 'histjpe23_a2f7df0bdcd867ed', '8945887917', 'Jalpaiguri', 'SAHIRUL HOWK',
  '2024-05-04', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-04T10:20:00.000Z', '2024-05-04T10:20:00.000Z'
),
(
  'histjpe23_pay_5c2a4c8d2aeccd92', 'treatment', 'Advance', 'Advance', 'histjpe23_a4ce5e32f4cd9579', '8927514421', 'Jalpaiguri', 'ABDUL SALAM',
  '2024-02-05', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-05T10:15:00.000Z', '2024-02-05T10:15:00.000Z'
),
(
  'histjpe23_pay_238a076fc34aff8e', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_a4ce5e32f4cd9579', '8927514421', 'Jalpaiguri', 'ABDUL SALAM',
  '2024-02-06', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-06T10:16:00.000Z', '2024-02-06T10:16:00.000Z'
),
(
  'histjpe23_pay_4b9ab1ffeae276fb', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_a4ce5e32f4cd9579', '8927514421', 'Jalpaiguri', 'ABDUL SALAM',
  '2024-02-26', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-26T10:17:00.000Z', '2024-02-26T10:17:00.000Z'
),
(
  'histjpe23_pay_6ccbe2265f84d4d4', 'treatment', '4th Payment', '4th Payment', 'histjpe23_a4ce5e32f4cd9579', '8927514421', 'Jalpaiguri', 'ABDUL SALAM',
  '2024-03-01', '4000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-01T10:18:00.000Z', '2024-03-01T10:18:00.000Z'
),
(
  'histjpe23_pay_2bcb41e5e5b1352c', 'treatment', '5th Payment', '5th Payment', 'histjpe23_a4ce5e32f4cd9579', '8927514421', 'Jalpaiguri', 'ABDUL SALAM',
  '2024-03-04', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-04T10:19:00.000Z', '2024-03-04T10:19:00.000Z'
),
(
  'histjpe23_pay_e3298299417558a3', 'treatment', '6th Payment', '6th Payment', 'histjpe23_a4ce5e32f4cd9579', '8927514421', 'Jalpaiguri', 'ABDUL SALAM',
  '2024-03-08', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-08T10:20:00.000Z', '2024-03-08T10:20:00.000Z'
),
(
  'histjpe23_pay_a04024e821028683', 'treatment', 'Advance', 'Advance', 'histjpe23_f4caf9937e3d2e53', '9674747857', 'Jalpaiguri', 'RIMPA DAS',
  '2024-02-17', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-17T10:15:00.000Z', '2024-02-17T10:15:00.000Z'
),
(
  'histjpe23_pay_0f7a5204a0e23195', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_f4caf9937e3d2e53', '9674747857', 'Jalpaiguri', 'RIMPA DAS',
  '2024-02-19', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-19T10:16:00.000Z', '2024-02-19T10:16:00.000Z'
),
(
  'histjpe23_pay_0957c8e035575868', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_f4caf9937e3d2e53', '9674747857', 'Jalpaiguri', 'RIMPA DAS',
  '2024-03-04', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-04T10:17:00.000Z', '2024-03-04T10:17:00.000Z'
),
(
  'histjpe23_pay_a232157cb55ae2a6', 'treatment', '4th Payment', '4th Payment', 'histjpe23_f4caf9937e3d2e53', '9674747857', 'Jalpaiguri', 'RIMPA DAS',
  '2024-03-08', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-08T10:18:00.000Z', '2024-03-08T10:18:00.000Z'
),
(
  'histjpe23_pay_ea00c7c7231e29ac', 'treatment', 'Advance', 'Advance', 'histjpe23_add6da410596222a', '9832039824', 'Jalpaiguri', 'NAYAN PAUL',
  '2024-02-26', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-26T10:15:00.000Z', '2024-02-26T10:15:00.000Z'
),
(
  'histjpe23_pay_f347fa6fdbeb1b82', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_add6da410596222a', '9832039824', 'Jalpaiguri', 'NAYAN PAUL',
  '2024-03-04', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-04T10:16:00.000Z', '2024-03-04T10:16:00.000Z'
),
(
  'histjpe23_pay_e64a9b695d04860f', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_add6da410596222a', '9832039824', 'Jalpaiguri', 'NAYAN PAUL',
  '2024-03-09', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-09T10:17:00.000Z', '2024-03-09T10:17:00.000Z'
),
(
  'histjpe23_pay_b365c6d53ca612b4', 'treatment', '4th Payment', '4th Payment', 'histjpe23_add6da410596222a', '9832039824', 'Jalpaiguri', 'NAYAN PAUL',
  '2024-03-11', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-11T10:18:00.000Z', '2024-03-11T10:18:00.000Z'
),
(
  'histjpe23_pay_b2ba46ea7b0ece23', 'treatment', '5th Payment', '5th Payment', 'histjpe23_add6da410596222a', '9832039824', 'Jalpaiguri', 'NAYAN PAUL',
  '2024-03-18', '4000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-18T10:19:00.000Z', '2024-03-18T10:19:00.000Z'
),
(
  'histjpe23_pay_48a4d4024bf69d10', 'treatment', 'Advance', 'Advance', 'histjpe23_51d6be82cf3606d7', '8016259482', 'Jalpaiguri', 'BISWAJIT SEN',
  '2024-03-03', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-03T10:15:00.000Z', '2024-03-03T10:15:00.000Z'
),
(
  'histjpe23_pay_eae898be8d674c38', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_51d6be82cf3606d7', '8016259482', 'Jalpaiguri', 'BISWAJIT SEN',
  '2024-03-08', '3500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-08T10:16:00.000Z', '2024-03-08T10:16:00.000Z'
),
(
  'histjpe23_pay_84ef322c1b24f5e1', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_51d6be82cf3606d7', '8016259482', 'Jalpaiguri', 'BISWAJIT SEN',
  '2024-03-11', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-11T10:17:00.000Z', '2024-03-11T10:17:00.000Z'
),
(
  'histjpe23_pay_6614db4ab7810932', 'treatment', '4th Payment', '4th Payment', 'histjpe23_51d6be82cf3606d7', '8016259482', 'Jalpaiguri', 'BISWAJIT SEN',
  '2024-03-04', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-04T10:18:00.000Z', '2024-03-04T10:18:00.000Z'
),
(
  'histjpe23_pay_71a3aad11433fae6', 'treatment', '5th Payment', '5th Payment', 'histjpe23_51d6be82cf3606d7', '8016259482', 'Jalpaiguri', 'BISWAJIT SEN',
  '2024-03-15', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-15T10:19:00.000Z', '2024-03-15T10:19:00.000Z'
),
(
  'histjpe23_pay_1cf6202aaefbf625', 'treatment', '6th Payment', '6th Payment', 'histjpe23_51d6be82cf3606d7', '8016259482', 'Jalpaiguri', 'BISWAJIT SEN',
  '2024-03-18', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-18T10:20:00.000Z', '2024-03-18T10:20:00.000Z'
),
(
  'histjpe23_pay_5ca2560d71d48ec9', 'treatment', '7th Payment', '7th Payment', 'histjpe23_51d6be82cf3606d7', '8016259482', 'Jalpaiguri', 'BISWAJIT SEN',
  '2024-03-22', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-22T10:21:00.000Z', '2024-03-22T10:21:00.000Z'
),
(
  'histjpe23_pay_b306396f5ceb8002', 'treatment', '8th Payment', '8th Payment', 'histjpe23_51d6be82cf3606d7', '8016259482', 'Jalpaiguri', 'BISWAJIT SEN',
  '2024-03-29', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-29T10:22:00.000Z', '2024-03-29T10:22:00.000Z'
),
(
  'histjpe23_pay_31fa21d446ef734d', 'treatment', '9th Payment', '9th Payment', 'histjpe23_51d6be82cf3606d7', '8016259482', 'Jalpaiguri', 'BISWAJIT SEN',
  '2024-04-05', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-05T10:23:00.000Z', '2024-04-05T10:23:00.000Z'
),
(
  'histjpe23_pay_55344ddb9a5d1cae', 'treatment', 'Advance', 'Advance', 'histjpe23_01b17b9acaba153a', '8491090593', 'Jalpaiguri', 'MD ABU IMRAN',
  '2024-04-10', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-10T10:15:00.000Z', '2024-04-10T10:15:00.000Z'
),
(
  'histjpe23_pay_423c6a7e11a61cdc', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_01b17b9acaba153a', '8491090593', 'Jalpaiguri', 'MD ABU IMRAN',
  '2024-04-20', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-20T10:16:00.000Z', '2024-04-20T10:16:00.000Z'
),
(
  'histjpe23_pay_b3e7c67b117cee09', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_01b17b9acaba153a', '8491090593', 'Jalpaiguri', 'MD ABU IMRAN',
  '2024-04-27', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-27T10:17:00.000Z', '2024-04-27T10:17:00.000Z'
),
(
  'histjpe23_pay_7c3f2ad92c6319ca', 'treatment', '4th Payment', '4th Payment', 'histjpe23_01b17b9acaba153a', '8491090593', 'Jalpaiguri', 'MD ABU IMRAN',
  '2024-04-28', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-28T10:18:00.000Z', '2024-04-28T10:18:00.000Z'
),
(
  'histjpe23_pay_f266f853d02ac383', 'treatment', '5th Payment', '5th Payment', 'histjpe23_01b17b9acaba153a', '8491090593', 'Jalpaiguri', 'MD ABU IMRAN',
  '2024-12-20', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-20T10:19:00.000Z', '2024-12-20T10:19:00.000Z'
),
(
  'histjpe23_pay_4c9150fd83b87d32', 'treatment', 'Advance', 'Advance', 'histjpe23_efbbdb0d8f6fecba', '9883280951', 'Jalpaiguri', 'REKHA DAS',
  '2024-04-27', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-27T10:15:00.000Z', '2024-04-27T10:15:00.000Z'
),
(
  'histjpe23_pay_541eb43d6f586375', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_efbbdb0d8f6fecba', '9883280951', 'Jalpaiguri', 'REKHA DAS',
  '2024-05-03', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-03T10:16:00.000Z', '2024-05-03T10:16:00.000Z'
),
(
  'histjpe23_pay_8ca6f73ddf5db606', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_efbbdb0d8f6fecba', '9883280951', 'Jalpaiguri', 'REKHA DAS',
  '2024-05-06', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-06T10:17:00.000Z', '2024-05-06T10:17:00.000Z'
),
(
  'histjpe23_pay_3d442b95e325a9ec', 'treatment', '4th Payment', '4th Payment', 'histjpe23_efbbdb0d8f6fecba', '9883280951', 'Jalpaiguri', 'REKHA DAS',
  '2024-05-13', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-13T10:18:00.000Z', '2024-05-13T10:18:00.000Z'
),
(
  'histjpe23_pay_7b1369df16105312', 'treatment', '5th Payment', '5th Payment', 'histjpe23_efbbdb0d8f6fecba', '9883280951', 'Jalpaiguri', 'REKHA DAS',
  '2024-05-25', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-25T10:19:00.000Z', '2024-05-25T10:19:00.000Z'
),
(
  'histjpe23_pay_ec21548c7f4b4bc6', 'treatment', '6th Payment', '6th Payment', 'histjpe23_efbbdb0d8f6fecba', '9883280951', 'Jalpaiguri', 'REKHA DAS',
  '2024-06-01', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-01T10:20:00.000Z', '2024-06-01T10:20:00.000Z'
),
(
  'histjpe23_pay_97d25fdcf0b90f4b', 'treatment', 'Advance', 'Advance', 'histjpe23_3748d7a96589b39b', '9800384561', 'Jalpaiguri', 'RAMPRASAD ROY',
  '2024-04-28', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-28T10:15:00.000Z', '2024-04-28T10:15:00.000Z'
),
(
  'histjpe23_pay_6602d6d74677da8b', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_3748d7a96589b39b', '9800384561', 'Jalpaiguri', 'RAMPRASAD ROY',
  '2024-05-04', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-04T10:16:00.000Z', '2024-05-04T10:16:00.000Z'
),
(
  'histjpe23_pay_5d36b251fdf39175', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_3748d7a96589b39b', '9800384561', 'Jalpaiguri', 'RAMPRASAD ROY',
  '2024-06-03', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-03T10:17:00.000Z', '2024-06-03T10:17:00.000Z'
),
(
  'histjpe23_pay_7f64d9804224c9e6', 'treatment', '4th Payment', '4th Payment', 'histjpe23_3748d7a96589b39b', '9800384561', 'Jalpaiguri', 'RAMPRASAD ROY',
  '2024-06-08', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-08T10:18:00.000Z', '2024-06-08T10:18:00.000Z'
),
(
  'histjpe23_pay_b6cda65cf30860a1', 'treatment', '5th Payment', '5th Payment', 'histjpe23_3748d7a96589b39b', '9800384561', 'Jalpaiguri', 'RAMPRASAD ROY',
  '2024-06-10', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-10T10:19:00.000Z', '2024-06-10T10:19:00.000Z'
),
(
  'histjpe23_pay_6dfa62e35f3dad56', 'treatment', '6th Payment', '6th Payment', 'histjpe23_3748d7a96589b39b', '9800384561', 'Jalpaiguri', 'RAMPRASAD ROY',
  '2024-06-15', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-15T10:20:00.000Z', '2024-06-15T10:20:00.000Z'
),
(
  'histjpe23_pay_fc877ac1bfd7ee9c', 'treatment', '7th Payment', '7th Payment', 'histjpe23_3748d7a96589b39b', '9800384561', 'Jalpaiguri', 'RAMPRASAD ROY',
  '2024-06-17', '3500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-17T10:21:00.000Z', '2024-06-17T10:21:00.000Z'
),
(
  'histjpe23_pay_866673d2a666149e', 'treatment', '8th Payment', '8th Payment', 'histjpe23_3748d7a96589b39b', '9800384561', 'Jalpaiguri', 'RAMPRASAD ROY',
  '2024-06-22', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-22T10:22:00.000Z', '2024-06-22T10:22:00.000Z'
),
(
  'histjpe23_pay_9e77faac04895722', 'treatment', '9th Payment', '9th Payment', 'histjpe23_3748d7a96589b39b', '9800384561', 'Jalpaiguri', 'RAMPRASAD ROY',
  '2024-06-24', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-24T10:23:00.000Z', '2024-06-24T10:23:00.000Z'
),
(
  'histjpe23_pay_46c6aeb88ebd4d71', 'treatment', '10th Payment', '10th Payment', 'histjpe23_3748d7a96589b39b', '9800384561', 'Jalpaiguri', 'RAMPRASAD ROY',
  '2024-06-29', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-29T10:24:00.000Z', '2024-06-29T10:24:00.000Z'
),
(
  'histjpe23_pay_9b36b4b545688a59', 'treatment', '11th Payment', '11th Payment', 'histjpe23_3748d7a96589b39b', '9800384561', 'Jalpaiguri', 'RAMPRASAD ROY',
  '2024-07-22', '2500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-22T10:25:00.000Z', '2024-07-22T10:25:00.000Z'
),
(
  'histjpe23_pay_dbe50a3f6ea3bb23', 'treatment', '12th Payment', '12th Payment', 'histjpe23_3748d7a96589b39b', '9800384561', 'Jalpaiguri', 'RAMPRASAD ROY',
  '2024-08-31', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-31T10:26:00.000Z', '2024-08-31T10:26:00.000Z'
),
(
  'histjpe23_pay_aeb453ea7a1d5c3e', 'treatment', '13th Payment', '13th Payment', 'histjpe23_3748d7a96589b39b', '9800384561', 'Jalpaiguri', 'RAMPRASAD ROY',
  '2024-09-11', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-11T10:27:00.000Z', '2024-09-11T10:27:00.000Z'
),
(
  'histjpe23_pay_39b7aa9cd29f0a54', 'treatment', 'Advance', 'Advance', 'histjpe23_bd2521f2af1d669a', '9382100434', 'Jalpaiguri', 'ANIMESH ROY',
  '2024-06-03', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-03T10:15:00.000Z', '2024-06-03T10:15:00.000Z'
),
(
  'histjpe23_pay_beeee0f919a67112', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_bd2521f2af1d669a', '9382100434', 'Jalpaiguri', 'ANIMESH ROY',
  '2024-06-08', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-08T10:16:00.000Z', '2024-06-08T10:16:00.000Z'
),
(
  'histjpe23_pay_d79142fc13ef82a2', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_bd2521f2af1d669a', '9382100434', 'Jalpaiguri', 'ANIMESH ROY',
  '2024-06-10', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-10T10:17:00.000Z', '2024-06-10T10:17:00.000Z'
),
(
  'histjpe23_pay_6d605f4c81ccd3b1', 'treatment', '4th Payment', '4th Payment', 'histjpe23_bd2521f2af1d669a', '9382100434', 'Jalpaiguri', 'ANIMESH ROY',
  '2024-06-15', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-15T10:18:00.000Z', '2024-06-15T10:18:00.000Z'
),
(
  'histjpe23_pay_4e25a8e34c746cd9', 'treatment', '5th Payment', '5th Payment', 'histjpe23_bd2521f2af1d669a', '9382100434', 'Jalpaiguri', 'ANIMESH ROY',
  '2024-06-17', '3500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-17T10:19:00.000Z', '2024-06-17T10:19:00.000Z'
),
(
  'histjpe23_pay_ac3548fa0259869f', 'treatment', '6th Payment', '6th Payment', 'histjpe23_bd2521f2af1d669a', '9382100434', 'Jalpaiguri', 'ANIMESH ROY',
  '2024-06-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-21T10:20:00.000Z', '2024-06-21T10:20:00.000Z'
),
(
  'histjpe23_pay_f2c617586d26b575', 'treatment', '7th Payment', '7th Payment', 'histjpe23_bd2521f2af1d669a', '9382100434', 'Jalpaiguri', 'ANIMESH ROY',
  '2024-06-24', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-24T10:21:00.000Z', '2024-06-24T10:21:00.000Z'
),
(
  'histjpe23_pay_43535e16c76b6e10', 'treatment', '8th Payment', '8th Payment', 'histjpe23_bd2521f2af1d669a', '9382100434', 'Jalpaiguri', 'ANIMESH ROY',
  '2024-06-29', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-29T10:22:00.000Z', '2024-06-29T10:22:00.000Z'
),
(
  'histjpe23_pay_d4edcd6fc34be6b3', 'treatment', '9th Payment', '9th Payment', 'histjpe23_bd2521f2af1d669a', '9382100434', 'Jalpaiguri', 'ANIMESH ROY',
  '2024-07-01', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-01T10:23:00.000Z', '2024-07-01T10:23:00.000Z'
),
(
  'histjpe23_pay_510d5c51b1b40c8c', 'treatment', '10th Payment', '10th Payment', 'histjpe23_bd2521f2af1d669a', '9382100434', 'Jalpaiguri', 'ANIMESH ROY',
  '2024-07-06', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-06T10:24:00.000Z', '2024-07-06T10:24:00.000Z'
),
(
  'histjpe23_pay_79b4c61a16dcf6ed', 'treatment', '11th Payment', '11th Payment', 'histjpe23_bd2521f2af1d669a', '9382100434', 'Jalpaiguri', 'ANIMESH ROY',
  '2024-07-08', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-08T10:25:00.000Z', '2024-07-08T10:25:00.000Z'
),
(
  'histjpe23_pay_6bf188b0c2f61ac1', 'treatment', '12th Payment', '12th Payment', 'histjpe23_bd2521f2af1d669a', '9382100434', 'Jalpaiguri', 'ANIMESH ROY',
  '2024-07-13', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-13T10:26:00.000Z', '2024-07-13T10:26:00.000Z'
),
(
  'histjpe23_pay_de7253d8ab46a410', 'treatment', '13th Payment', '13th Payment', 'histjpe23_bd2521f2af1d669a', '9382100434', 'Jalpaiguri', 'ANIMESH ROY',
  '2024-07-22', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-22T10:27:00.000Z', '2024-07-22T10:27:00.000Z'
),
(
  'histjpe23_pay_60bcaf02a4a6f4d1', 'treatment', '14th Payment', '14th Payment', 'histjpe23_bd2521f2af1d669a', '9382100434', 'Jalpaiguri', 'ANIMESH ROY',
  '2024-07-27', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-27T10:28:00.000Z', '2024-07-27T10:28:00.000Z'
),
(
  'histjpe23_pay_232a23e16c96b8b2', 'treatment', 'Advance', 'Advance', 'histjpe23_54b5c2381cf3ab78', '9547486785', 'Jalpaiguri', 'SALAYA KHATOON',
  '2024-05-11', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-11T10:15:00.000Z', '2024-05-11T10:15:00.000Z'
),
(
  'histjpe23_pay_f7c013a23fa06043', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_54b5c2381cf3ab78', '9547486785', 'Jalpaiguri', 'SALAYA KHATOON',
  '2024-05-14', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-14T10:16:00.000Z', '2024-05-14T10:16:00.000Z'
),
(
  'histjpe23_pay_4469caf97760b4e5', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_54b5c2381cf3ab78', '9547486785', 'Jalpaiguri', 'SALAYA KHATOON',
  '2024-05-15', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-15T10:17:00.000Z', '2024-05-15T10:17:00.000Z'
),
(
  'histjpe23_pay_1499ce596e06517d', 'treatment', '4th Payment', '4th Payment', 'histjpe23_54b5c2381cf3ab78', '9547486785', 'Jalpaiguri', 'SALAYA KHATOON',
  '2024-05-25', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-25T10:18:00.000Z', '2024-05-25T10:18:00.000Z'
),
(
  'histjpe23_pay_b2898905adf03763', 'treatment', '5th Payment', '5th Payment', 'histjpe23_54b5c2381cf3ab78', '9547486785', 'Jalpaiguri', 'SALAYA KHATOON',
  '2024-06-01', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-01T10:19:00.000Z', '2024-06-01T10:19:00.000Z'
),
(
  'histjpe23_pay_12e42ad490a6cc4d', 'treatment', '6th Payment', '6th Payment', 'histjpe23_54b5c2381cf3ab78', '9547486785', 'Jalpaiguri', 'SALAYA KHATOON',
  '2024-06-09', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-09T10:20:00.000Z', '2024-06-09T10:20:00.000Z'
),
(
  'histjpe23_pay_d0de562b07aec247', 'treatment', 'Advance', 'Advance', 'histjpe23_b0db34a89b649d74', '8927978542', 'Jalpaiguri', 'AMITAV ROY',
  '2024-05-14', '4000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-14T10:15:00.000Z', '2024-05-14T10:15:00.000Z'
),
(
  'histjpe23_pay_924962ded5c21353', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_b0db34a89b649d74', '8927978542', 'Jalpaiguri', 'AMITAV ROY',
  '2024-05-18', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-18T10:16:00.000Z', '2024-05-18T10:16:00.000Z'
),
(
  'histjpe23_pay_639d9ebbba89e4a1', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_b0db34a89b649d74', '8927978542', 'Jalpaiguri', 'AMITAV ROY',
  '2024-05-20', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-20T10:17:00.000Z', '2024-05-20T10:17:00.000Z'
),
(
  'histjpe23_pay_5cfca703321fab68', 'treatment', '4th Payment', '4th Payment', 'histjpe23_b0db34a89b649d74', '8927978542', 'Jalpaiguri', 'AMITAV ROY',
  '2024-05-23', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-23T10:18:00.000Z', '2024-05-23T10:18:00.000Z'
),
(
  'histjpe23_pay_c1486de019e205f7', 'treatment', '5th Payment', '5th Payment', 'histjpe23_b0db34a89b649d74', '8927978542', 'Jalpaiguri', 'AMITAV ROY',
  '2024-05-27', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-27T10:19:00.000Z', '2024-05-27T10:19:00.000Z'
),
(
  'histjpe23_pay_12b425a437fb663d', 'treatment', '6th Payment', '6th Payment', 'histjpe23_b0db34a89b649d74', '8927978542', 'Jalpaiguri', 'AMITAV ROY',
  '2024-05-31', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-31T10:20:00.000Z', '2024-05-31T10:20:00.000Z'
),
(
  'histjpe23_pay_26b25529fe5527c7', 'treatment', '7th Payment', '7th Payment', 'histjpe23_b0db34a89b649d74', '8927978542', 'Jalpaiguri', 'AMITAV ROY',
  '2024-06-03', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-03T10:21:00.000Z', '2024-06-03T10:21:00.000Z'
),
(
  'histjpe23_pay_721095e5e6289312', 'treatment', '8th Payment', '8th Payment', 'histjpe23_b0db34a89b649d74', '8927978542', 'Jalpaiguri', 'AMITAV ROY',
  '2024-06-08', '4500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-08T10:22:00.000Z', '2024-06-08T10:22:00.000Z'
),
(
  'histjpe23_pay_691003506b5e2e50', 'treatment', 'Advance', 'Advance', 'histjpe23_6ef2aa3e371a0e44', '8295192467', 'Jalpaiguri', 'ALOK ROY',
  '2024-05-21', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-21T10:15:00.000Z', '2024-05-21T10:15:00.000Z'
),
(
  'histjpe23_pay_f0968f04567ab79d', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_6ef2aa3e371a0e44', '8295192467', 'Jalpaiguri', 'ALOK ROY',
  '2024-05-25', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-25T10:16:00.000Z', '2024-05-25T10:16:00.000Z'
),
(
  'histjpe23_pay_f24d36599719de08', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_6ef2aa3e371a0e44', '8295192467', 'Jalpaiguri', 'ALOK ROY',
  '2024-05-27', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-27T10:17:00.000Z', '2024-05-27T10:17:00.000Z'
),
(
  'histjpe23_pay_d6a91025d344b035', 'treatment', 'Advance', 'Advance', 'histjpe23_6b21bc7ed0fa7ec0', '7001505012', 'Jalpaiguri', 'JAGJIT MANDAL',
  '2024-06-18', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-18T10:15:00.000Z', '2024-06-18T10:15:00.000Z'
),
(
  'histjpe23_pay_3cc2528f094aaaa9', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_6b21bc7ed0fa7ec0', '7001505012', 'Jalpaiguri', 'JAGJIT MANDAL',
  '2024-06-23', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-23T10:16:00.000Z', '2024-06-23T10:16:00.000Z'
),
(
  'histjpe23_pay_4f9f8746a469f719', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_6b21bc7ed0fa7ec0', '7001505012', 'Jalpaiguri', 'JAGJIT MANDAL',
  '2024-06-30', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-30T10:17:00.000Z', '2024-06-30T10:17:00.000Z'
),
(
  'histjpe23_pay_6d420816cbc96565', 'treatment', '4th Payment', '4th Payment', 'histjpe23_6b21bc7ed0fa7ec0', '7001505012', 'Jalpaiguri', 'JAGJIT MANDAL',
  '2024-07-14', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-14T10:18:00.000Z', '2024-07-14T10:18:00.000Z'
),
(
  'histjpe23_pay_32854f13ab3328a4', 'treatment', 'Advance', 'Advance', 'histjpe23_5f1d64bb9b179203', '8250604619', 'Jalpaiguri', 'MUKUL ROY',
  '2024-07-09', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-09T10:15:00.000Z', '2024-07-09T10:15:00.000Z'
),
(
  'histjpe23_pay_0555f92fb27f52fa', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_5f1d64bb9b179203', '8250604619', 'Jalpaiguri', 'MUKUL ROY',
  '2024-07-13', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-13T10:16:00.000Z', '2024-07-13T10:16:00.000Z'
),
(
  'histjpe23_pay_fa1d8ec78df19ccb', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_5f1d64bb9b179203', '8250604619', 'Jalpaiguri', 'MUKUL ROY',
  '2024-07-17', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-17T10:17:00.000Z', '2024-07-17T10:17:00.000Z'
),
(
  'histjpe23_pay_272658e261bf7895', 'treatment', '4th Payment', '4th Payment', 'histjpe23_5f1d64bb9b179203', '8250604619', 'Jalpaiguri', 'MUKUL ROY',
  '2024-08-03', '4000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-03T10:18:00.000Z', '2024-08-03T10:18:00.000Z'
),
(
  'histjpe23_pay_ad975db25a460793', 'treatment', '5th Payment', '5th Payment', 'histjpe23_5f1d64bb9b179203', '8250604619', 'Jalpaiguri', 'MUKUL ROY',
  '2024-08-16', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-16T10:19:00.000Z', '2024-08-16T10:19:00.000Z'
),
(
  'histjpe23_pay_97d5ee440865ac74', 'treatment', '6th Payment', '6th Payment', 'histjpe23_5f1d64bb9b179203', '8250604619', 'Jalpaiguri', 'MUKUL ROY',
  '2024-09-23', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-23T10:20:00.000Z', '2024-09-23T10:20:00.000Z'
),
(
  'histjpe23_pay_36d282c577c4b97b', 'treatment', 'Advance', 'Advance', 'histjpe23_2cb38499dcc5afe7', '9832178703', 'Jalpaiguri', 'MAMTA BALA',
  '2024-08-10', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-10T10:15:00.000Z', '2024-08-10T10:15:00.000Z'
),
(
  'histjpe23_pay_7fa84b0bfe070fe8', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_2cb38499dcc5afe7', '9832178703', 'Jalpaiguri', 'MAMTA BALA',
  '2024-08-11', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-11T10:16:00.000Z', '2024-08-11T10:16:00.000Z'
),
(
  'histjpe23_pay_998a12b54f49d4ca', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_2cb38499dcc5afe7', '9832178703', 'Jalpaiguri', 'MAMTA BALA',
  '2024-08-18', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-18T10:17:00.000Z', '2024-08-18T10:17:00.000Z'
),
(
  'histjpe23_pay_861afb029cb835f2', 'treatment', '4th Payment', '4th Payment', 'histjpe23_2cb38499dcc5afe7', '9832178703', 'Jalpaiguri', 'MAMTA BALA',
  '2024-09-08', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-08T10:18:00.000Z', '2024-09-08T10:18:00.000Z'
),
(
  'histjpe23_pay_5b816898a6157b6d', 'treatment', '5th Payment', '5th Payment', 'histjpe23_2cb38499dcc5afe7', '9832178703', 'Jalpaiguri', 'MAMTA BALA',
  '2024-09-13', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-13T10:19:00.000Z', '2024-09-13T10:19:00.000Z'
),
(
  'histjpe23_pay_f2e4d59c30fa9e35', 'treatment', 'Advance', 'Advance', 'histjpe23_4a7283979f7f11a5', '7583953177', 'Jalpaiguri', 'SATNA BASFO',
  '2024-07-16', '4000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-16T10:15:00.000Z', '2024-07-16T10:15:00.000Z'
),
(
  'histjpe23_pay_da7eb3815c52502d', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_4a7283979f7f11a5', '7583953177', 'Jalpaiguri', 'SATNA BASFO',
  '2024-07-27', '6000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-27T10:16:00.000Z', '2024-07-27T10:16:00.000Z'
),
(
  'histjpe23_pay_ea2d98f771e819b8', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_4a7283979f7f11a5', '7583953177', 'Jalpaiguri', 'SATNA BASFO',
  '2024-07-29', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-29T10:17:00.000Z', '2024-07-29T10:17:00.000Z'
),
(
  'histjpe23_pay_4848cf2116fbf0d2', 'treatment', '4th Payment', '4th Payment', 'histjpe23_4a7283979f7f11a5', '7583953177', 'Jalpaiguri', 'SATNA BASFO',
  '2024-08-12', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-12T10:18:00.000Z', '2024-08-12T10:18:00.000Z'
),
(
  'histjpe23_pay_b6f602aa5d8326fe', 'treatment', 'Advance', 'Advance', 'histjpe23_c0a66004d22a8c82', '6296698321', 'Jalpaiguri', 'SUSHMITA SARKAR',
  '2024-07-27', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-27T10:15:00.000Z', '2024-07-27T10:15:00.000Z'
),
(
  'histjpe23_pay_df3e5e4ccb8cb6d2', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_c0a66004d22a8c82', '6296698321', 'Jalpaiguri', 'SUSHMITA SARKAR',
  '2024-08-03', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-03T10:16:00.000Z', '2024-08-03T10:16:00.000Z'
),
(
  'histjpe23_pay_343b403d254554c0', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_c0a66004d22a8c82', '6296698321', 'Jalpaiguri', 'SUSHMITA SARKAR',
  '2024-08-10', '3500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-10T10:17:00.000Z', '2024-08-10T10:17:00.000Z'
),
(
  'histjpe23_pay_ecc94f79fc4545c9', 'treatment', '4th Payment', '4th Payment', 'histjpe23_c0a66004d22a8c82', '6296698321', 'Jalpaiguri', 'SUSHMITA SARKAR',
  '2024-08-31', '4000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-31T10:18:00.000Z', '2024-08-31T10:18:00.000Z'
),
(
  'histjpe23_pay_844441ec1e9b4f50', 'treatment', '5th Payment', '5th Payment', 'histjpe23_c0a66004d22a8c82', '6296698321', 'Jalpaiguri', 'SUSHMITA SARKAR',
  '2024-09-28', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-28T10:19:00.000Z', '2024-09-28T10:19:00.000Z'
),
(
  'histjpe23_pay_7df1d542a47af3f6', 'treatment', 'Advance', 'Advance', 'histjpe23_3a510830de81815a', '7076960587', 'Jalpaiguri', 'RAJINA BEGAM',
  '2024-07-27', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-27T10:15:00.000Z', '2024-07-27T10:15:00.000Z'
),
(
  'histjpe23_pay_f54e5f531fb0f5f7', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_3a510830de81815a', '7076960587', 'Jalpaiguri', 'RAJINA BEGAM',
  '2024-08-03', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-03T10:16:00.000Z', '2024-08-03T10:16:00.000Z'
),
(
  'histjpe23_pay_f70b30a7e562e3ec', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_3a510830de81815a', '7076960587', 'Jalpaiguri', 'RAJINA BEGAM',
  '2024-08-09', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-09T10:17:00.000Z', '2024-08-09T10:17:00.000Z'
),
(
  'histjpe23_pay_23939b180d601418', 'treatment', '4th Payment', '4th Payment', 'histjpe23_3a510830de81815a', '7076960587', 'Jalpaiguri', 'RAJINA BEGAM',
  '2024-08-17', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-17T10:18:00.000Z', '2024-08-17T10:18:00.000Z'
),
(
  'histjpe23_pay_f5974511746d51d8', 'treatment', '5th Payment', '5th Payment', 'histjpe23_3a510830de81815a', '7076960587', 'Jalpaiguri', 'RAJINA BEGAM',
  '2024-08-24', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-24T10:19:00.000Z', '2024-08-24T10:19:00.000Z'
),
(
  'histjpe23_pay_73cf534b528b5e51', 'treatment', '6th Payment', '6th Payment', 'histjpe23_3a510830de81815a', '7076960587', 'Jalpaiguri', 'RAJINA BEGAM',
  '2024-09-03', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-03T10:20:00.000Z', '2024-09-03T10:20:00.000Z'
),
(
  'histjpe23_pay_0277bc60c174b875', 'treatment', '7th Payment', '7th Payment', 'histjpe23_3a510830de81815a', '7076960587', 'Jalpaiguri', 'RAJINA BEGAM',
  '2024-09-06', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-06T10:21:00.000Z', '2024-09-06T10:21:00.000Z'
),
(
  'histjpe23_pay_cfe744e9c0c351c1', 'treatment', '8th Payment', '8th Payment', 'histjpe23_3a510830de81815a', '7076960587', 'Jalpaiguri', 'RAJINA BEGAM',
  '2024-09-23', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-23T10:22:00.000Z', '2024-09-23T10:22:00.000Z'
),
(
  'histjpe23_pay_dfc5d0b2789937e7', 'treatment', '9th Payment', '9th Payment', 'histjpe23_3a510830de81815a', '7076960587', 'Jalpaiguri', 'RAJINA BEGAM',
  '2024-09-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-21T10:23:00.000Z', '2024-09-21T10:23:00.000Z'
),
(
  'histjpe23_pay_69333082def94168', 'treatment', 'Advance', 'Advance', 'histjpe23_f0c9ec122a29ce40', '8653744848', 'Jalpaiguri', 'SWAPAN PANDIT',
  '2024-08-10', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-10T10:15:00.000Z', '2024-08-10T10:15:00.000Z'
),
(
  'histjpe23_pay_7fdf4fbe307ec635', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_f0c9ec122a29ce40', '8653744848', 'Jalpaiguri', 'SWAPAN PANDIT',
  '2024-08-12', '3500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-12T10:16:00.000Z', '2024-08-12T10:16:00.000Z'
),
(
  'histjpe23_pay_e08dd627237bcdca', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_f0c9ec122a29ce40', '8653744848', 'Jalpaiguri', 'SWAPAN PANDIT',
  '2024-08-18', '3500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-18T10:17:00.000Z', '2024-08-18T10:17:00.000Z'
),
(
  'histjpe23_pay_1290cc8bab0de729', 'treatment', '4th Payment', '4th Payment', 'histjpe23_f0c9ec122a29ce40', '8653744848', 'Jalpaiguri', 'SWAPAN PANDIT',
  '2024-08-24', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-24T10:18:00.000Z', '2024-08-24T10:18:00.000Z'
),
(
  'histjpe23_pay_4b97d4aedf0745e5', 'treatment', '5th Payment', '5th Payment', 'histjpe23_f0c9ec122a29ce40', '8653744848', 'Jalpaiguri', 'SWAPAN PANDIT',
  '2024-08-28', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-28T10:19:00.000Z', '2024-08-28T10:19:00.000Z'
),
(
  'histjpe23_pay_9e13877f2b306c2a', 'treatment', '6th Payment', '6th Payment', 'histjpe23_f0c9ec122a29ce40', '8653744848', 'Jalpaiguri', 'SWAPAN PANDIT',
  '2024-09-01', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-01T10:20:00.000Z', '2024-09-01T10:20:00.000Z'
),
(
  'histjpe23_pay_f8cfcbe37971ed4c', 'treatment', '7th Payment', '7th Payment', 'histjpe23_f0c9ec122a29ce40', '8653744848', 'Jalpaiguri', 'SWAPAN PANDIT',
  '2024-09-13', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-13T10:21:00.000Z', '2024-09-13T10:21:00.000Z'
),
(
  'histjpe23_pay_f5ccd6f13339413f', 'treatment', 'Advance', 'Advance', 'histjpe23_43f5e4751142b312', '8906507436', 'Jalpaiguri', 'SOHEL AHMED',
  '2024-08-24', '2500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-24T10:15:00.000Z', '2024-08-24T10:15:00.000Z'
),
(
  'histjpe23_pay_d6bf711a98424be5', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_43f5e4751142b312', '8906507436', 'Jalpaiguri', 'SOHEL AHMED',
  '2024-09-02', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-02T10:16:00.000Z', '2024-09-02T10:16:00.000Z'
),
(
  'histjpe23_pay_ef2c1f8390cf5b7c', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_43f5e4751142b312', '8906507436', 'Jalpaiguri', 'SOHEL AHMED',
  '2024-09-09', '4000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-09T10:17:00.000Z', '2024-09-09T10:17:00.000Z'
),
(
  'histjpe23_pay_5cb194350a0cdef7', 'treatment', '4th Payment', '4th Payment', 'histjpe23_43f5e4751142b312', '8906507436', 'Jalpaiguri', 'SOHEL AHMED',
  '2024-09-15', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-15T10:18:00.000Z', '2024-09-15T10:18:00.000Z'
),
(
  'histjpe23_pay_88ff0bf29ee8cef5', 'treatment', 'Advance', 'Advance', 'histjpe23_4c2011a40e558846', '8016882184', 'Jalpaiguri', 'SUBHAS ROY',
  '2024-08-16', '4000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-16T10:15:00.000Z', '2024-08-16T10:15:00.000Z'
),
(
  'histjpe23_pay_a1704f6956bfbb6a', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_4c2011a40e558846', '8016882184', 'Jalpaiguri', 'SUBHAS ROY',
  '2024-09-01', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-01T10:16:00.000Z', '2024-09-01T10:16:00.000Z'
),
(
  'histjpe23_pay_5b56cc80d2f2364d', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_4c2011a40e558846', '8016882184', 'Jalpaiguri', 'SUBHAS ROY',
  '2024-09-08', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-08T10:17:00.000Z', '2024-09-08T10:17:00.000Z'
),
(
  'histjpe23_pay_04d895add0813221', 'treatment', '4th Payment', '4th Payment', 'histjpe23_4c2011a40e558846', '8016882184', 'Jalpaiguri', 'SUBHAS ROY',
  '2024-09-15', '4000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-15T10:18:00.000Z', '2024-09-15T10:18:00.000Z'
),
(
  'histjpe23_pay_2360f60f9457844b', 'treatment', '5th Payment', '5th Payment', 'histjpe23_4c2011a40e558846', '8016882184', 'Jalpaiguri', 'SUBHAS ROY',
  '2024-09-29', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-29T10:19:00.000Z', '2024-09-29T10:19:00.000Z'
),
(
  'histjpe23_pay_779b6c6e7c38fe0f', 'treatment', '6th Payment', '6th Payment', 'histjpe23_4c2011a40e558846', '8016882184', 'Jalpaiguri', 'SUBHAS ROY',
  '2024-10-28', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-28T10:20:00.000Z', '2024-10-28T10:20:00.000Z'
),
(
  'histjpe23_pay_1c308a6fbfe7d29a', 'treatment', 'Advance', 'Advance', 'histjpe23_3f660f999fa75a30', '7047624676', 'Jalpaiguri', 'ARUNA KHATOON',
  '2024-08-24', '8000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-24T10:15:00.000Z', '2024-08-24T10:15:00.000Z'
),
(
  'histjpe23_pay_8767ecf0ec1cc4e2', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_3f660f999fa75a30', '7047624676', 'Jalpaiguri', 'ARUNA KHATOON',
  '2024-08-31', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-31T10:16:00.000Z', '2024-08-31T10:16:00.000Z'
),
(
  'histjpe23_pay_4a5a96be6315d273', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_3f660f999fa75a30', '7047624676', 'Jalpaiguri', 'ARUNA KHATOON',
  '2024-09-06', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-06T10:17:00.000Z', '2024-09-06T10:17:00.000Z'
),
(
  'histjpe23_pay_49938ed6b0297ebb', 'treatment', '4th Payment', '4th Payment', 'histjpe23_3f660f999fa75a30', '7047624676', 'Jalpaiguri', 'ARUNA KHATOON',
  '2024-09-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-21T10:18:00.000Z', '2024-09-21T10:18:00.000Z'
),
(
  'histjpe23_pay_11a9331eba634502', 'treatment', '5th Payment', '5th Payment', 'histjpe23_3f660f999fa75a30', '7047624676', 'Jalpaiguri', 'ARUNA KHATOON',
  '2024-09-28', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-28T10:19:00.000Z', '2024-09-28T10:19:00.000Z'
),
(
  'histjpe23_pay_62243ca4645ffd90', 'treatment', '6th Payment', '6th Payment', 'histjpe23_3f660f999fa75a30', '7047624676', 'Jalpaiguri', 'ARUNA KHATOON',
  '2024-10-05', '3500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-05T10:20:00.000Z', '2024-10-05T10:20:00.000Z'
),
(
  'histjpe23_pay_ddb0ec448466d15e', 'treatment', '7th Payment', '7th Payment', 'histjpe23_3f660f999fa75a30', '7047624676', 'Jalpaiguri', 'ARUNA KHATOON',
  '2024-10-19', '2500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-19T10:21:00.000Z', '2024-10-19T10:21:00.000Z'
),
(
  'histjpe23_pay_7a32f7d8770ab1d3', 'treatment', '8th Payment', '8th Payment', 'histjpe23_3f660f999fa75a30', '7047624676', 'Jalpaiguri', 'ARUNA KHATOON',
  '2024-10-27', '2500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-27T10:22:00.000Z', '2024-10-27T10:22:00.000Z'
),
(
  'histjpe23_pay_0735e80be1d2afda', 'treatment', 'Advance', 'Advance', 'histjpe23_1b93df595edc5de8', '8391809951', 'Jalpaiguri', 'RINKU NINUYA',
  '2024-08-28', '15000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-28T10:15:00.000Z', '2024-08-28T10:15:00.000Z'
),
(
  'histjpe23_pay_9c72f24b9a264e59', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_1b93df595edc5de8', '8391809951', 'Jalpaiguri', 'RINKU NINUYA',
  '2024-09-01', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-01T10:16:00.000Z', '2024-09-01T10:16:00.000Z'
),
(
  'histjpe23_pay_5cfd74ead694f02a', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_1b93df595edc5de8', '8391809951', 'Jalpaiguri', 'RINKU NINUYA',
  '2024-09-09', '4000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-09T10:17:00.000Z', '2024-09-09T10:17:00.000Z'
),
(
  'histjpe23_pay_19c92ccc2307e78f', 'treatment', '4th Payment', '4th Payment', 'histjpe23_1b93df595edc5de8', '8391809951', 'Jalpaiguri', 'RINKU NINUYA',
  '2024-09-14', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-14T10:18:00.000Z', '2024-09-14T10:18:00.000Z'
),
(
  'histjpe23_pay_657ee2a8d098cbf9', 'treatment', '5th Payment', '5th Payment', 'histjpe23_1b93df595edc5de8', '8391809951', 'Jalpaiguri', 'RINKU NINUYA',
  '2024-09-20', '7500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-20T10:19:00.000Z', '2024-09-20T10:19:00.000Z'
),
(
  'histjpe23_pay_291c4a505ca18823', 'treatment', '6th Payment', '6th Payment', 'histjpe23_1b93df595edc5de8', '8391809951', 'Jalpaiguri', 'RINKU NINUYA',
  '2024-10-01', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-01T10:20:00.000Z', '2024-10-01T10:20:00.000Z'
),
(
  'histjpe23_pay_11eb19d3819ca890', 'treatment', '7th Payment', '7th Payment', 'histjpe23_1b93df595edc5de8', '8391809951', 'Jalpaiguri', 'RINKU NINUYA',
  '2024-10-04', '50', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-04T10:21:00.000Z', '2024-10-04T10:21:00.000Z'
),
(
  'histjpe23_pay_ca352b187cfcca84', 'treatment', '8th Payment', '8th Payment', 'histjpe23_1b93df595edc5de8', '8391809951', 'Jalpaiguri', 'RINKU NINUYA',
  '2024-10-19', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-19T10:22:00.000Z', '2024-10-19T10:22:00.000Z'
),
(
  'histjpe23_pay_f6c368fa41c7155e', 'treatment', 'Advance', 'Advance', 'histjpe23_24622147ffbaa48a', '9339469563', 'Jalpaiguri', 'POONAM BASU',
  '2024-09-01', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-01T10:15:00.000Z', '2024-09-01T10:15:00.000Z'
),
(
  'histjpe23_pay_2c2df2c6b7fbf835', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_24622147ffbaa48a', '9339469563', 'Jalpaiguri', 'POONAM BASU',
  '2024-09-09', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-09T10:16:00.000Z', '2024-09-09T10:16:00.000Z'
),
(
  'histjpe23_pay_72be10dcc7769634', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_24622147ffbaa48a', '9339469563', 'Jalpaiguri', 'POONAM BASU',
  '2024-09-16', '4000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-16T10:17:00.000Z', '2024-09-16T10:17:00.000Z'
),
(
  'histjpe23_pay_40facfa8554b2ebe', 'treatment', 'Advance', 'Advance', 'histjpe23_0aeee475cb05a185', '9832660311', 'Jalpaiguri', 'MANABUL HOWK',
  '2024-09-08', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-08T10:15:00.000Z', '2024-09-08T10:15:00.000Z'
),
(
  'histjpe23_pay_2c3df8db3e86c8d8', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_0aeee475cb05a185', '9832660311', 'Jalpaiguri', 'MANABUL HOWK',
  '2024-09-13', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-13T10:16:00.000Z', '2024-09-13T10:16:00.000Z'
),
(
  'histjpe23_pay_15fd0dfaad11c16e', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_0aeee475cb05a185', '9832660311', 'Jalpaiguri', 'MANABUL HOWK',
  '2024-09-16', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-16T10:17:00.000Z', '2024-09-16T10:17:00.000Z'
),
(
  'histjpe23_pay_c7806510c749b89b', 'treatment', '4th Payment', '4th Payment', 'histjpe23_0aeee475cb05a185', '9832660311', 'Jalpaiguri', 'MANABUL HOWK',
  '2024-09-19', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-19T10:18:00.000Z', '2024-09-19T10:18:00.000Z'
),
(
  'histjpe23_pay_7e5d283c927f1926', 'treatment', '5th Payment', '5th Payment', 'histjpe23_0aeee475cb05a185', '9832660311', 'Jalpaiguri', 'MANABUL HOWK',
  '2024-09-23', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-23T10:19:00.000Z', '2024-09-23T10:19:00.000Z'
),
(
  'histjpe23_pay_c78d6950ff7ae6fa', 'treatment', '6th Payment', '6th Payment', 'histjpe23_0aeee475cb05a185', '9832660311', 'Jalpaiguri', 'MANABUL HOWK',
  '2024-09-29', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-29T10:20:00.000Z', '2024-09-29T10:20:00.000Z'
),
(
  'histjpe23_pay_13c6d5196fd52d8a', 'treatment', 'Advance', 'Advance', 'histjpe23_0a001b9ae3c551c7', '9325139698', 'Jalpaiguri', 'MANARUL ISLAM',
  '2024-09-09', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-09T10:15:00.000Z', '2024-09-09T10:15:00.000Z'
),
(
  'histjpe23_pay_86804a68c9bc9c03', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_0a001b9ae3c551c7', '9325139698', 'Jalpaiguri', 'MANARUL ISLAM',
  '2024-09-13', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-13T10:16:00.000Z', '2024-09-13T10:16:00.000Z'
),
(
  'histjpe23_pay_1ad3018d527407b7', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_0a001b9ae3c551c7', '9325139698', 'Jalpaiguri', 'MANARUL ISLAM',
  '2024-09-16', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-16T10:17:00.000Z', '2024-09-16T10:17:00.000Z'
),
(
  'histjpe23_pay_537e69215c578b51', 'treatment', '4th Payment', '4th Payment', 'histjpe23_0a001b9ae3c551c7', '9325139698', 'Jalpaiguri', 'MANARUL ISLAM',
  '2024-09-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-21T10:18:00.000Z', '2024-09-21T10:18:00.000Z'
),
(
  'histjpe23_pay_b310e886ceaba5ab', 'treatment', 'Advance', 'Advance', 'histjpe23_2b1eac58a1b9d3d7', '7866038920', 'Jalpaiguri', 'TAPAN SEN',
  '2024-09-09', '1600', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-09T10:15:00.000Z', '2024-09-09T10:15:00.000Z'
),
(
  'histjpe23_pay_ab7d9faf817e2404', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_2b1eac58a1b9d3d7', '7866038920', 'Jalpaiguri', 'TAPAN SEN',
  '2024-09-13', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-13T10:16:00.000Z', '2024-09-13T10:16:00.000Z'
),
(
  'histjpe23_pay_fc8846a5b2358f5a', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_2b1eac58a1b9d3d7', '7866038920', 'Jalpaiguri', 'TAPAN SEN',
  '2024-09-16', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-16T10:17:00.000Z', '2024-09-16T10:17:00.000Z'
),
(
  'histjpe23_pay_2b312eb0f00da5e9', 'treatment', '4th Payment', '4th Payment', 'histjpe23_2b1eac58a1b9d3d7', '7866038920', 'Jalpaiguri', 'TAPAN SEN',
  '2024-09-19', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-19T10:18:00.000Z', '2024-09-19T10:18:00.000Z'
),
(
  'histjpe23_pay_43bd340e722f496d', 'treatment', '5th Payment', '5th Payment', 'histjpe23_2b1eac58a1b9d3d7', '7866038920', 'Jalpaiguri', 'TAPAN SEN',
  '2024-09-23', '7000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-23T10:19:00.000Z', '2024-09-23T10:19:00.000Z'
),
(
  'histjpe23_pay_0e6d0e6abbbc4867', 'treatment', '6th Payment', '6th Payment', 'histjpe23_2b1eac58a1b9d3d7', '7866038920', 'Jalpaiguri', 'TAPAN SEN',
  '2024-09-30', '20000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-30T10:20:00.000Z', '2024-09-30T10:20:00.000Z'
),
(
  'histjpe23_pay_5e2d955eb2541221', 'treatment', '7th Payment', '7th Payment', 'histjpe23_2b1eac58a1b9d3d7', '7866038920', 'Jalpaiguri', 'TAPAN SEN',
  '2024-10-05', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-05T10:21:00.000Z', '2024-10-05T10:21:00.000Z'
),
(
  'histjpe23_pay_d550c0caf1feff02', 'treatment', '8th Payment', '8th Payment', 'histjpe23_2b1eac58a1b9d3d7', '7866038920', 'Jalpaiguri', 'TAPAN SEN',
  '2024-10-07', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-07T10:22:00.000Z', '2024-10-07T10:22:00.000Z'
),
(
  'histjpe23_pay_d3e6f546ed6b8e31', 'treatment', '9th Payment', '9th Payment', 'histjpe23_2b1eac58a1b9d3d7', '7866038920', 'Jalpaiguri', 'TAPAN SEN',
  '2024-10-11', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-11T10:23:00.000Z', '2024-10-11T10:23:00.000Z'
),
(
  'histjpe23_pay_54ceca15850ab53c', 'treatment', '10th Payment', '10th Payment', 'histjpe23_2b1eac58a1b9d3d7', '7866038920', 'Jalpaiguri', 'TAPAN SEN',
  '2024-10-21', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-21T10:24:00.000Z', '2024-10-21T10:24:00.000Z'
),
(
  'histjpe23_pay_36b4654d705a1c2d', 'treatment', '11th Payment', '11th Payment', 'histjpe23_2b1eac58a1b9d3d7', '7866038920', 'Jalpaiguri', 'TAPAN SEN',
  '2024-10-28', '4400', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-28T10:25:00.000Z', '2024-10-28T10:25:00.000Z'
),
(
  'histjpe23_pay_976010411b5086ed', 'treatment', '12th Payment', '12th Payment', 'histjpe23_2b1eac58a1b9d3d7', '7866038920', 'Jalpaiguri', 'TAPAN SEN',
  '2024-11-04', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-04T10:26:00.000Z', '2024-11-04T10:26:00.000Z'
),
(
  'histjpe23_pay_f84e5a40c13bfbdb', 'treatment', '13th Payment', '13th Payment', 'histjpe23_2b1eac58a1b9d3d7', '7866038920', 'Jalpaiguri', 'TAPAN SEN',
  '2024-11-09', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-09T10:27:00.000Z', '2024-11-09T10:27:00.000Z'
),
(
  'histjpe23_pay_c288746a92356475', 'treatment', '14th Payment', '14th Payment', 'histjpe23_2b1eac58a1b9d3d7', '7866038920', 'Jalpaiguri', 'TAPAN SEN',
  '2024-09-15', '5500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-15T10:28:00.000Z', '2024-09-15T10:28:00.000Z'
),
(
  'histjpe23_pay_ee80494d0c68fa20', 'treatment', '15th Payment', '15th Payment', 'histjpe23_2b1eac58a1b9d3d7', '7866038920', 'Jalpaiguri', 'TAPAN SEN',
  '2024-12-02', '4500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-02T10:29:00.000Z', '2024-12-02T10:29:00.000Z'
),
(
  'histjpe23_pay_aaadd52994f00dc2', 'treatment', 'Advance', 'Advance', 'histjpe23_6311b4310b2e44dc', '8101032976', 'Jalpaiguri', 'KONIKA SARKAR',
  '2024-09-30', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-30T10:15:00.000Z', '2024-09-30T10:15:00.000Z'
),
(
  'histjpe23_pay_9e7dcfad852a6698', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_6311b4310b2e44dc', '8101032976', 'Jalpaiguri', 'KONIKA SARKAR',
  '2024-10-04', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-04T10:16:00.000Z', '2024-10-04T10:16:00.000Z'
),
(
  'histjpe23_pay_51889d10738eb68e', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_6311b4310b2e44dc', '8101032976', 'Jalpaiguri', 'KONIKA SARKAR',
  '2024-10-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-21T10:17:00.000Z', '2024-10-21T10:17:00.000Z'
),
(
  'histjpe23_pay_0b728c363d4b131d', 'treatment', '4th Payment', '4th Payment', 'histjpe23_6311b4310b2e44dc', '8101032976', 'Jalpaiguri', 'KONIKA SARKAR',
  '2024-10-27', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-27T10:18:00.000Z', '2024-10-27T10:18:00.000Z'
),
(
  'histjpe23_pay_e5fc29644d71c8f6', 'treatment', '5th Payment', '5th Payment', 'histjpe23_6311b4310b2e44dc', '8101032976', 'Jalpaiguri', 'KONIKA SARKAR',
  '2024-11-09', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-09T10:19:00.000Z', '2024-11-09T10:19:00.000Z'
),
(
  'histjpe23_pay_efb6ca1ede10ff16', 'treatment', '6th Payment', '6th Payment', 'histjpe23_6311b4310b2e44dc', '8101032976', 'Jalpaiguri', 'KONIKA SARKAR',
  '2025-01-03', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-03T10:20:00.000Z', '2025-01-03T10:20:00.000Z'
),
(
  'histjpe23_pay_00e8fce5ebbb439a', 'treatment', '7th Payment', '7th Payment', 'histjpe23_6311b4310b2e44dc', '8101032976', 'Jalpaiguri', 'KONIKA SARKAR',
  '2025-01-06', '100', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-06T10:21:00.000Z', '2025-01-06T10:21:00.000Z'
),
(
  'histjpe23_pay_2786000cadbad6fd', 'treatment', '8th Payment', '8th Payment', 'histjpe23_6311b4310b2e44dc', '8101032976', 'Jalpaiguri', 'KONIKA SARKAR',
  '2025-01-13', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-13T10:22:00.000Z', '2025-01-13T10:22:00.000Z'
),
(
  'histjpe23_pay_c924613d8fe7053b', 'treatment', 'Advance', 'Advance', 'histjpe23_2ee6a402693b5daf', '8927746336', 'Jalpaiguri', 'MONMAHTO SARKAR',
  '2024-11-10', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-10T10:15:00.000Z', '2024-11-10T10:15:00.000Z'
),
(
  'histjpe23_pay_b055d3669681c4b5', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_2ee6a402693b5daf', '8927746336', 'Jalpaiguri', 'MONMAHTO SARKAR',
  '2024-11-17', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-17T10:16:00.000Z', '2024-11-17T10:16:00.000Z'
),
(
  'histjpe23_pay_435b2bf4a09116df', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_2ee6a402693b5daf', '8927746336', 'Jalpaiguri', 'MONMAHTO SARKAR',
  '2024-12-01', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-01T10:17:00.000Z', '2024-12-01T10:17:00.000Z'
),
(
  'histjpe23_pay_7da877e4d043715b', 'treatment', '4th Payment', '4th Payment', 'histjpe23_2ee6a402693b5daf', '8927746336', 'Jalpaiguri', 'MONMAHTO SARKAR',
  '2024-12-15', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-15T10:18:00.000Z', '2024-12-15T10:18:00.000Z'
),
(
  'histjpe23_pay_0cab6be9902e5e4b', 'treatment', '5th Payment', '5th Payment', 'histjpe23_2ee6a402693b5daf', '8927746336', 'Jalpaiguri', 'MONMAHTO SARKAR',
  '2024-12-27', '5000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-27T10:19:00.000Z', '2024-12-27T10:19:00.000Z'
),
(
  'histjpe23_pay_c4a727b1ed6480f6', 'treatment', 'Advance', 'Advance', 'histjpe23_fd9ceb7ad637b5f7', '8617486585', 'Jalpaiguri', 'FERDOS RAHMAN',
  '2024-11-17', '7000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-17T10:15:00.000Z', '2024-11-17T10:15:00.000Z'
),
(
  'histjpe23_pay_3ac6d0bb5f2bd898', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_fd9ceb7ad637b5f7', '8617486585', 'Jalpaiguri', 'FERDOS RAHMAN',
  '2024-12-01', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-01T10:16:00.000Z', '2024-12-01T10:16:00.000Z'
),
(
  'histjpe23_pay_a28fde753399ad52', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_fd9ceb7ad637b5f7', '8617486585', 'Jalpaiguri', 'FERDOS RAHMAN',
  '2024-12-08', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-08T10:17:00.000Z', '2024-12-08T10:17:00.000Z'
),
(
  'histjpe23_pay_028378700d1b0b0c', 'treatment', '4th Payment', '4th Payment', 'histjpe23_fd9ceb7ad637b5f7', '8617486585', 'Jalpaiguri', 'FERDOS RAHMAN',
  '2024-12-15', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-15T10:18:00.000Z', '2024-12-15T10:18:00.000Z'
),
(
  'histjpe23_pay_73a8fd38829b93bf', 'treatment', '5th Payment', '5th Payment', 'histjpe23_fd9ceb7ad637b5f7', '8617486585', 'Jalpaiguri', 'FERDOS RAHMAN',
  '2024-12-22', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-22T10:19:00.000Z', '2024-12-22T10:19:00.000Z'
),
(
  'histjpe23_pay_f53edd011ac0b4d4', 'treatment', '6th Payment', '6th Payment', 'histjpe23_fd9ceb7ad637b5f7', '8617486585', 'Jalpaiguri', 'FERDOS RAHMAN',
  '2024-12-27', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-27T10:20:00.000Z', '2024-12-27T10:20:00.000Z'
),
(
  'histjpe23_pay_d1fc65cfd1805ea3', 'treatment', 'Advance', 'Advance', 'histjpe23_fff93fda04f97186', '8250545395', 'Jalpaiguri', 'PRIYANKA ROY',
  '2024-11-23', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-23T10:15:00.000Z', '2024-11-23T10:15:00.000Z'
),
(
  'histjpe23_pay_58326285d223af49', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_fff93fda04f97186', '8250545395', 'Jalpaiguri', 'PRIYANKA ROY',
  '2024-12-02', '1500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-02T10:16:00.000Z', '2024-12-02T10:16:00.000Z'
),
(
  'histjpe23_pay_dea28ecde0f3381e', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_fff93fda04f97186', '8250545395', 'Jalpaiguri', 'PRIYANKA ROY',
  '2024-12-07', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-07T10:17:00.000Z', '2024-12-07T10:17:00.000Z'
),
(
  'histjpe23_pay_19e07e4e4a15e6d2', 'treatment', 'Advance', 'Advance', 'histjpe23_ff8713a527f55eed', '6295890602', 'Jalpaiguri', 'DEBASISH ROY',
  '2024-12-15', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-15T10:15:00.000Z', '2024-12-15T10:15:00.000Z'
),
(
  'histjpe23_pay_1c6da50df80b3693', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_ff8713a527f55eed', '6295890602', 'Jalpaiguri', 'DEBASISH ROY',
  '2024-12-21', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-21T10:16:00.000Z', '2024-12-21T10:16:00.000Z'
),
(
  'histjpe23_pay_bc0e552d280973ea', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_ff8713a527f55eed', '6295890602', 'Jalpaiguri', 'DEBASISH ROY',
  '2024-12-29', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-29T10:17:00.000Z', '2024-12-29T10:17:00.000Z'
),
(
  'histjpe23_pay_54a8b1e7eb51ce6d', 'treatment', '4th Payment', '4th Payment', 'histjpe23_ff8713a527f55eed', '6295890602', 'Jalpaiguri', 'DEBASISH ROY',
  '2025-01-04', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-04T10:18:00.000Z', '2025-01-04T10:18:00.000Z'
),
(
  'histjpe23_pay_e91bceefd879121e', 'treatment', 'Advance', 'Advance', 'histjpe23_71c44d38079eb790', '7872339778', 'Jalpaiguri', 'MANOJ ROY',
  '2024-12-21', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-21T10:15:00.000Z', '2024-12-21T10:15:00.000Z'
),
(
  'histjpe23_pay_5afe02e325fe9d59', 'treatment', '2nd Payment', '2nd Payment', 'histjpe23_71c44d38079eb790', '7872339778', 'Jalpaiguri', 'MANOJ ROY',
  '2024-12-28', '2000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-28T10:16:00.000Z', '2024-12-28T10:16:00.000Z'
),
(
  'histjpe23_pay_71ee1927a28121ec', 'treatment', '3rd Payment', '3rd Payment', 'histjpe23_71c44d38079eb790', '7872339778', 'Jalpaiguri', 'MANOJ ROY',
  '2025-01-04', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-04T10:17:00.000Z', '2025-01-04T10:17:00.000Z'
),
(
  'histjpe23_pay_bd03073b6888a6fe', 'treatment', '4th Payment', '4th Payment', 'histjpe23_71c44d38079eb790', '7872339778', 'Jalpaiguri', 'MANOJ ROY',
  '2025-01-11', '3000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-11T10:18:00.000Z', '2025-01-11T10:18:00.000Z'
),
(
  'histjpe23_pay_f07818237ce99285', 'treatment', '5th Payment', '5th Payment', 'histjpe23_71c44d38079eb790', '7872339778', 'Jalpaiguri', 'MANOJ ROY',
  '2025-01-18', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-18T10:19:00.000Z', '2025-01-18T10:19:00.000Z'
),
(
  'histjpe23_pay_fb11c181979a237e', 'treatment', '6th Payment', '6th Payment', 'histjpe23_71c44d38079eb790', '7872339778', 'Jalpaiguri', 'MANOJ ROY',
  '2025-02-01', '1000', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-01T10:20:00.000Z', '2025-02-01T10:20:00.000Z'
),
(
  'histjpe23_pay_4d00f7f417c1ed65', 'treatment', '7th Payment', '7th Payment', 'histjpe23_71c44d38079eb790', '7872339778', 'Jalpaiguri', 'MANOJ ROY',
  '2025-03-01', '500', 'CASH', 'Historical import (Jalpaiguri 2023-24 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-01T10:21:00.000Z', '2025-03-01T10:21:00.000Z'
);
