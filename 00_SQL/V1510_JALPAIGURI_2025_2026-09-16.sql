-- জলপাইগুড়ি ব্রাঞ্চ -- ২০২৫ শিট (2025_PAYTENTS_DETAILS) -- 125 জন
-- ১৩৪ জন বিল-করা রোগীর মধ্যে ৯ জন বাদ (৫ জন শিট-করাপশন/টাকা গরমিল, ৩ জন
-- মোবাইল ভুল/নেই, ১ জন JAGABANDHU ROY -- ভুয়া মোবাইল "0000000000") --
-- TK নিশ্চিত করেছেন, বাদ-তালিকা খাতায় লেখা আছে। বাকি ১২৫ জন।
-- লাইভ-ডুপ্লিকেট-চেক (V1509) TK চালিয়ে দেখেছেন -- কেউ আগে থেকে নেই।
-- ৩ জোড়া রোগীর মোবাইল একই (পরিবারের ফোন শেয়ার, ভিন্ন মানুষ, ভিন্ন তারিখ/বিল) --
-- Kishanganj প্রকল্পেও একই প্যাটার্ন পাওয়া গিয়েছিল, স্বাভাবিক।

insert into public.patients (
  id, "patientId", date, "registrationDate", "visitDate",
  name, mobile, "altMobile", branch, age, sex,
  address, disease, bill,
  stage, queue, "doctorComplete",
  "createdBy", "registeredBy", "createdAt", "updatedAt"
) values
(
  'histjpe25_b6f3eac44515256f', 'JPE-05012025-001', '2025-01-05', '2025-01-05', '2025-01-05',
  'MITHUN BARMAN', '8207218447', '', 'Jalpaiguri', '23', 'Male',
  'MATHAVANGA MORE, JATAMARI, SHITALKUCHI, COOCHBIHAR', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-05T10:00:00.000Z', '2025-01-05T10:00:00.000Z'
),
(
  'histjpe25_7fb80024970ca366', 'JPE-05012025-002', '2025-01-05', '2025-01-05', '2025-01-05',
  'DEV ROY SARKAR', '7384437061', '', 'Jalpaiguri', '5', 'Male',
  'Uttat khal para, Changmari, Malbazar, Jalpaiguri', 'Piles', '16000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-05T10:00:00.000Z', '2025-01-05T10:00:00.000Z'
),
(
  'histjpe25_3be9b4406b363cd8', 'JPE-11012025-001', '2025-01-11', '2025-01-11', '2025-01-11',
  'MOLIN CHANDRA ROY', '9547814633', '', 'Jalpaiguri', '48', 'Male',
  'Gageleghata, Barovatiya, kotwali, Jalpaiguri', 'Piles', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-11T10:00:00.000Z', '2025-01-11T10:00:00.000Z'
),
(
  'histjpe25_ea61bdf8f9fe7678', 'JPE-11012025-002', '2025-01-11', '2025-01-11', '2025-01-11',
  'ARUN CHANDRA ROY', '9932293816', '', 'Jalpaiguri', '40', 'Male',
  'subhas palli, police line, Jalpaiguri, kotwali, Jalpaiguri', 'Fistula', '3200',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-11T10:00:00.000Z', '2025-01-11T10:00:00.000Z'
),
(
  'histjpe25_c622aad011516cbf', 'JPE-11012025-003', '2025-01-11', '2025-01-11', '2025-01-11',
  'KHARGA NARAYAN ROY', '7679849423', '', 'Jalpaiguri', '66', 'Male',
  'UTTAR MATIALI, LATAGURI, KRANTI, Jalpaiguri', 'Piles', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-11T10:00:00.000Z', '2025-01-11T10:00:00.000Z'
),
(
  'histjpe25_a71995fc5b0d9a24', 'JPE-12012025-001', '2025-01-12', '2025-01-12', '2025-01-12',
  'S.B CHAKRABORTY', '7475895957', '', 'Jalpaiguri', '79', 'Male',
  'Raikatpara, Dinbazar, Kotwali, Jalpaiguri', 'Piles', '9500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-12T10:00:00.000Z', '2025-01-12T10:00:00.000Z'
),
(
  'histjpe25_3cf34828bdb49163', 'JPE-12012025-002', '2025-01-12', '2025-01-12', '2025-01-12',
  'RAJKUMAR ROY', '9641779180', '', 'Jalpaiguri', '40', 'Male',
  'RANGDHAMALI, PATKATA, KOTWALI, JALPAIGURI', 'Hydrocele', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-12T10:00:00.000Z', '2025-01-12T10:00:00.000Z'
),
(
  'histjpe25_c997cae960222503', 'JPE-13012025-001', '2025-01-13', '2025-01-13', '2025-01-13',
  'RAJ RASWA', '8670661208', '', 'Jalpaiguri', '41', 'Male',
  'Shiposomiti para, Jalpaiguri, Kotwali, Jalpaiguri', 'Piles', '17000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-13T10:00:00.000Z', '2025-01-13T10:00:00.000Z'
),
(
  'histjpe25_063c3ce920aaa8b5', 'JPE-13012025-002', '2025-01-13', '2025-01-13', '2025-01-13',
  'RINTU BASAK', '8967434707', '', 'Jalpaiguri', '36', 'Male',
  'Maynaguri, Maynaguri, Maynaguri, Jalpaiguri', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-13T10:00:00.000Z', '2025-01-13T10:00:00.000Z'
),
(
  'histjpe25_800f788b5dcc4c07', 'JPE-18012025-001', '2025-01-18', '2025-01-18', '2025-01-18',
  'AMBIKA', '7432985537', '', 'Jalpaiguri', '20', 'Female',
  'Mohitnagar, Japaiguri, Mohitnagar, Jalpaiguri', 'Piles', '10000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-18T10:00:00.000Z', '2025-01-18T10:00:00.000Z'
),
(
  'histjpe25_c84f6673d2c0e34c', 'JPE-19012025-001', '2025-01-19', '2025-01-19', '2025-01-19',
  'HARI', '6296394987', '', 'Jalpaiguri', '26', 'Male',
  'Barnish, Maynaguri, Barnish, Jalpaiguri', 'Gupt Rog', '8000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-19T10:00:00.000Z', '2025-01-19T10:00:00.000Z'
),
(
  'histjpe25_1c79929bf2ad6c20', 'JPE-25012025-001', '2025-01-25', '2025-01-25', '2025-01-25',
  'BINOD KUMAR MAHATO', '9851514795', '', 'Jalpaiguri', '30', 'Male',
  'Champasari, Pradhan nagar, ptadhan nagar, Darjeeling', 'Piles', '44000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-25T10:00:00.000Z', '2025-01-25T10:00:00.000Z'
),
(
  'histjpe25_3e0b778099e0916e', 'JPE-25012025-002', '2025-01-25', '2025-01-25', '2025-01-25',
  'PRASANTA BASAK', '7047761706', '', 'Jalpaiguri', '34', 'Male',
  'Bagan bari, Jorpakri, Maynaguri, Jalpaiguri', 'Other', '70025',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-25T10:00:00.000Z', '2025-01-25T10:00:00.000Z'
),
(
  'histjpe25_0aa2c2a2a2dd5026', 'JPE-25012025-003', '2025-01-25', '2025-01-25', '2025-01-25',
  'PALLABI MAJUMDA', '7432837172', '', 'Jalpaiguri', '25', 'Female',
  'Talma hat, Debi Takurbari, Kotwali, Jalpaiguri', 'Piles', '13000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-25T10:00:00.000Z', '2025-01-25T10:00:00.000Z'
),
(
  'histjpe25_0944c60e522e4881', 'JPE-01022025-001', '2025-02-01', '2025-02-01', '2025-02-01',
  'SAMINA YASMIN', '7063830935', '', 'Jalpaiguri', '22', 'Male',
  'Futkibari, Goralbari, kotwali, Jalpaiguri', 'Piles', '12000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-01T10:00:00.000Z', '2025-02-01T10:00:00.000Z'
),
(
  'histjpe25_3a7e65fe170e2fc3', 'JPE-01022025-002', '2025-02-01', '2025-02-01', '2025-02-01',
  'SAHJAHAN ISLAM', '7001705669', '', 'Jalpaiguri', '27', 'Male',
  'Chaulhati, Chaulhati, Rajgonj, Jalpaiguri', 'Fistula', '100000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-01T10:00:00.000Z', '2025-02-01T10:00:00.000Z'
),
(
  'histjpe25_3b4b599f477d42e3', 'JPE-01022025-003', '2025-02-01', '2025-02-01', '2025-02-01',
  'JAYANTA ROY', '9475629465', '', 'Jalpaiguri', '23', 'Male',
  'Satkura, Satkura, kotwali, Jalpaiguri', 'Gupt Rog', '6500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-01T10:00:00.000Z', '2025-02-01T10:00:00.000Z'
),
(
  'histjpe25_43f6075c058285c2', 'JPE-01022025-004', '2025-02-01', '2025-02-01', '2025-02-01',
  'SHIBANI MANDAL', '8918486092', '', 'Jalpaiguri', '57', 'Male',
  'Milon pally, Gajol doba, Njp, Jalpaiguri', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-01T10:00:00.000Z', '2025-02-01T10:00:00.000Z'
),
(
  'histjpe25_5d972f64b9659447', 'JPE-02022025-001', '2025-02-02', '2025-02-02', '2025-02-02',
  'BHARAT SHING', '9434824470', '', 'Jalpaiguri', '52', 'Male',
  'Chalsa, Chalsa, Meteli, Jalpaiguri', 'Piles', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-02T10:00:00.000Z', '2025-02-02T10:00:00.000Z'
),
(
  'histjpe25_ad0d0a0a4e780ea8', 'JPE-04022025-001', '2025-02-04', '2025-02-04', '2025-02-04',
  'RASNA KHATUN', '9883960266', '', 'Jalpaiguri', '28', 'Female',
  'Chaulhati, Chaulhati, Rajgonj, Jalpaiguri', 'Piles, Fistula', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-04T10:00:00.000Z', '2025-02-04T10:00:00.000Z'
),
(
  'histjpe25_445e6c375f1222cb', 'JPE-05022025-001', '2025-02-05', '2025-02-05', '2025-02-05',
  'TAPASHI SARKAR', '7718416138', '', 'Jalpaiguri', '43', 'Male',
  'Jamidar palli, Jalpaiguri, kotwali, Jalpaiguri', 'Piles', '14000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-05T10:00:00.000Z', '2025-02-05T10:00:00.000Z'
),
(
  'histjpe25_f9b43756b1af6166', 'JPE-05022025-002', '2025-02-05', '2025-02-05', '2025-02-05',
  'JAGOBONDHU ROY', '9547178350', '', 'Jalpaiguri', '80', 'Male',
  'ULLABARI, MAYNAGURI, MAYNAGURI, JALPAIGURI', 'Piles', '65000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-05T10:00:00.000Z', '2025-02-05T10:00:00.000Z'
),
(
  'histjpe25_1541f293a97cd404', 'JPE-05022025-003', '2025-02-05', '2025-02-05', '2025-02-05',
  'SRIBASH ROY', '7364842184', '', 'Jalpaiguri', '30', 'Male',
  'Berubari, Berubari, Kotwali, Jalpaiguri', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-05T10:00:00.000Z', '2025-02-05T10:00:00.000Z'
),
(
  'histjpe25_e151f66bba6f43ce', 'JPE-08022025-001', '2025-02-08', '2025-02-08', '2025-02-08',
  'HIMALAYA ROY', '9933709754', '', 'Jalpaiguri', '20', 'Male',
  'Kuchlibari, Mekhligonj, Mekhligonj, Jalpaiguri', 'Fistula', '51000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-08T10:00:00.000Z', '2025-02-08T10:00:00.000Z'
),
(
  'histjpe25_2187fb2a1cb1b97a', 'JPE-22022025-001', '2025-02-22', '2025-02-22', '2025-02-22',
  'SUDHANTA SARKAR', '6295599811', '', 'Jalpaiguri', '40', 'Male',
  'JALPAIGURI, JALPAIGURI, JALPAIGURI, JALPAIGURI', 'Piles', '16500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-22T10:00:00.000Z', '2025-02-22T10:00:00.000Z'
),
(
  'histjpe25_743a44bb59233ff6', 'JPE-22022025-002', '2025-02-22', '2025-02-22', '2025-02-22',
  'KALATU ROY', '9800487355', '', 'Jalpaiguri', '54', 'Male',
  'HODAR DANGA', 'Fissure', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-22T10:00:00.000Z', '2025-02-22T10:00:00.000Z'
),
(
  'histjpe25_3a07c9cc9537cf4c', 'JPE-01032025-001', '2025-03-01', '2025-03-01', '2025-03-01',
  'RANJIT ROY', '8637899217', '', 'Jalpaiguri', '36', 'Male',
  'TALMA HAT, DEBI THAKUR BARI, KOTWALI, JALPAIGURI', 'Piles', '24000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-01T10:00:00.000Z', '2025-03-01T10:00:00.000Z'
),
(
  'histjpe25_aeda5572fcc34a4e', 'JPE-01032025-002', '2025-03-01', '2025-03-01', '2025-03-01',
  'BIRENDRANATH ROY', '7001806924', '', 'Jalpaiguri', '48', 'Male',
  'BOLBARI, BOLBARI, MAYNAGURI, JALPAIGURI', 'Piles', '19500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-01T10:00:00.000Z', '2025-03-01T10:00:00.000Z'
),
(
  'histjpe25_84ee510823ebd271', 'JPE-01032025-003', '2025-03-01', '2025-03-01', '2025-03-01',
  'DALIYA BARMAN', '8972261311', '', 'Jalpaiguri', '32', 'Male',
  'BELABOBA, BELAKOBA, RAJGANJ, JALPAIGURI', 'Fistula', '50000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-01T10:00:00.000Z', '2025-03-01T10:00:00.000Z'
),
(
  'histjpe25_4dacfbc0505e16f7', 'JPE-04032025-001', '2025-03-04', '2025-03-04', '2025-03-04',
  'JAHANARA BEGAM', '8927539671', '', 'Jalpaiguri', '32', 'Female',
  'BARO GHARIA, GORALBARI, KOTWALI, JALPAIGURI', 'Piles, Fissure', '33000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-04T10:00:00.000Z', '2025-03-04T10:00:00.000Z'
),
(
  'histjpe25_48286f7c735f5706', 'JPE-08032025-001', '2025-03-08', '2025-03-08', '2025-03-08',
  'ABBU BAKKAR SIDDIK', '7029582842', '', 'Jalpaiguri', '27', 'Male',
  'PASCHIM BALA BARI, NAKURGANJ, RAJGANJ, JALPAIGURI', 'Piles', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-08T10:00:00.000Z', '2025-03-08T10:00:00.000Z'
),
(
  'histjpe25_878beb3953207bda', 'JPE-08032025-002', '2025-03-08', '2025-03-08', '2025-03-08',
  'DIPALI ROY', '9365670832', '', 'Jalpaiguri', '35', 'Female',
  'JALPAIGURI, JALPAIGURI, JALPAIGURI, JALPAIGURI', 'Piles', '28000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-08T10:00:00.000Z', '2025-03-08T10:00:00.000Z'
),
(
  'histjpe25_9881a84ab0e580bb', 'JPE-15032025-001', '2025-03-15', '2025-03-15', '2025-03-15',
  'MONTOSH MODAK', '7718156997', '', 'Jalpaiguri', '29', 'Male',
  'RANINAGAR, RANINAGAR, KOTWALI, JALPAIGURI', 'Piles', '18500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-15T10:00:00.000Z', '2025-03-15T10:00:00.000Z'
),
(
  'histjpe25_8dc12c9d6f0c964e', 'JPE-18032025-001', '2025-03-18', '2025-03-18', '2025-03-18',
  'MRINMOY ROY', '9832304557', '', 'Jalpaiguri', '12', 'Male',
  'NORTH BENGAL, SHIKARPUR, KOTWALI, JALPAIGURI', 'Piles', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-18T10:00:00.000Z', '2025-03-18T10:00:00.000Z'
),
(
  'histjpe25_fd940e9be5f276ec', 'JPE-29032025-001', '2025-03-29', '2025-03-29', '2025-03-29',
  'GOPAL BISWAS', '9641126416', '', 'Jalpaiguri', '54', 'Male',
  'RAJBARI PARA, B.D.O OFFICE, KOTWALI, JALPAIGURI', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-29T10:00:00.000Z', '2025-03-29T10:00:00.000Z'
),
(
  'histjpe25_b1a69617aeffd9b9', 'JPE-29032025-002', '2025-03-29', '2025-03-29', '2025-03-29',
  'BISWAS RAM BHAGAT', '9547196494', '', 'Jalpaiguri', '38', 'Male',
  'SATKURA, SATKURA, HALDIBARI, COOCHBEHAR', 'Piles', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-29T10:00:00.000Z', '2025-03-29T10:00:00.000Z'
),
(
  'histjpe25_4dbe3e778b894893', 'JPE-29032025-003', '2025-03-29', '2025-03-29', '2025-03-29',
  'RAHUL ROY', '7063528848', '', 'Jalpaiguri', '', 'Male',
  'RAJGANJ, RAJGANJ, KOTWALI, JALPAIGURI', 'Piles', '26000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-29T10:00:00.000Z', '2025-03-29T10:00:00.000Z'
),
(
  'histjpe25_da68e1bd12d369c1', 'JPE-29032025-004', '2025-03-29', '2025-03-29', '2025-03-29',
  'MUNNA KUMAR', '8617378476', '', 'Jalpaiguri', '', 'Male',
  '', 'Hydrocele', '22500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-29T10:00:00.000Z', '2025-03-29T10:00:00.000Z'
),
(
  'histjpe25_5fba273d50acf335', 'JPE-05042025-001', '2025-04-05', '2025-04-05', '2025-04-05',
  'PRADIP ROY', '7719171801', '', 'Jalpaiguri', '', 'Male',
  'ADOR PARA, PANDA PARA, KOTWALI, JALPAIGURI', 'Piles', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-05T10:00:00.000Z', '2025-04-05T10:00:00.000Z'
),
(
  'histjpe25_9eab93e421a0d6ca', 'JPE-06042025-001', '2025-04-06', '2025-04-06', '2025-04-06',
  'SARASWATI BASAK', '6295445862', '', 'Jalpaiguri', '37', 'Male',
  'TALMA HAT, RAJGANJ, RAJGANJ, JALPAIGURI', 'Piles', '10000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-06T10:00:00.000Z', '2025-04-06T10:00:00.000Z'
),
(
  'histjpe25_ffdd061a84992df8', 'JPE-08042025-001', '2025-04-08', '2025-04-08', '2025-04-08',
  'ABU BAKKAR SIDDIK', '7909197887', '', 'Jalpaiguri', '31', 'Male',
  'JALPAIGURI', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-08T10:00:00.000Z', '2025-04-08T10:00:00.000Z'
),
(
  'histjpe25_c2e80e41e89e1fd0', 'JPE-15042025-001', '2025-04-15', '2025-04-15', '2025-04-15',
  'MANIKUL HOWK', '9832876576', '', 'Jalpaiguri', '36', 'Male',
  'Kestopur, Dalimgonj, kaliyaganj, Uttar Dinajpur', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-15T10:00:00.000Z', '2025-04-15T10:00:00.000Z'
),
(
  'histjpe25_ab321cb877151c91', 'JPE-18042025-001', '2025-04-18', '2025-04-18', '2025-04-18',
  'RIVA ROY', '8145773324', '', 'Jalpaiguri', '24', 'Female',
  'JALPAIGURI', 'Piles', '18500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-18T10:00:00.000Z', '2025-04-18T10:00:00.000Z'
),
(
  'histjpe25_acd84078c03817ca', 'JPE-19042025-001', '2025-04-19', '2025-04-19', '2025-04-19',
  'ABHIJIT BISWAS', '8101954623', '', 'Jalpaiguri', '50', 'Male',
  '4NO GHUMTI, JALPAIGURI, KOTWALI, JALPAIGURI', 'Piles', '17600',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-19T10:00:00.000Z', '2025-04-19T10:00:00.000Z'
),
(
  'histjpe25_48aa1a0086e8258f', 'JPE-22042025-001', '2025-04-22', '2025-04-22', '2025-04-22',
  'MANOTOSH ROY', '9563175056', '', 'Jalpaiguri', '28', 'Male',
  'k', 'Fistula', '13000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-22T10:00:00.000Z', '2025-04-22T10:00:00.000Z'
),
(
  'histjpe25_9767a049edb5caed', 'JPE-22042025-002', '2025-04-22', '2025-04-22', '2025-04-22',
  'SUMAN BISWAS', '7602376461', '', 'Jalpaiguri', '26', 'Male',
  '', 'Other', '28000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-22T10:00:00.000Z', '2025-04-22T10:00:00.000Z'
),
(
  'histjpe25_bd67c87b8c2eed59', 'JPE-26042025-001', '2025-04-26', '2025-04-26', '2025-04-26',
  'RAJU SARKAR', '8597456404', '', 'Jalpaiguri', '26', 'Male',
  'Kasiyabari, kashyapari, haldibadi, JALPAIGURI', 'Piles', '28000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-26T10:00:00.000Z', '2025-04-26T10:00:00.000Z'
),
(
  'histjpe25_a5786277fb9d9113', 'JPE-30042025-001', '2025-04-30', '2025-04-30', '2025-04-30',
  'MD MOINUDDIN', '7679872605', '', 'Jalpaiguri', '40', 'Male',
  'surjosen, VAKTINAGAR, VAKTINAGAR, JALPAIGURI', 'Piles', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-30T10:00:00.000Z', '2025-04-30T10:00:00.000Z'
),
(
  'histjpe25_c1b3638e1ccb1ff9', 'JPE-03052025-001', '2025-05-03', '2025-05-03', '2025-05-03',
  'MIRAJUL HOQUE', '9064471790', '', 'Jalpaiguri', '36', 'Male',
  'NEWTWON PARA, JALPAIGURI, KOTWALI, JALPAIGURI', 'Piles', '22500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-03T10:00:00.000Z', '2025-05-03T10:00:00.000Z'
),
(
  'histjpe25_c5158671fca734c3', 'JPE-03052025-002', '2025-05-03', '2025-05-03', '2025-05-03',
  'TUMPA ROY', '7557824144', '', 'Jalpaiguri', '30', 'Male',
  'DHAPGONJ, DHAPGONJ, KOTWALI, JALPAIGURI', 'Piles', '18500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-03T10:00:00.000Z', '2025-05-03T10:00:00.000Z'
),
(
  'histjpe25_55273a9356bbb70c', 'JPE-06052025-001', '2025-05-06', '2025-05-06', '2025-05-06',
  'SAMBHU ROY', '8250583177', '', 'Jalpaiguri', '28', 'Male',
  'PANDA PARA, PANDA PARA, KOTWALI, JALPAIGURI', 'Hydrocele', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-06T10:00:00.000Z', '2025-05-06T10:00:00.000Z'
),
(
  'histjpe25_00ce16be5ac5f4ac', 'JPE-06052025-002', '2025-05-06', '2025-05-06', '2025-05-06',
  'RATAN DAS', '9932274032', '', 'Jalpaiguri', '38', 'Male',
  'DEBNAGAR, DEBNAGAR, KOTWALI, JALPAIGURI', 'Piles', '43000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-06T10:00:00.000Z', '2025-05-06T10:00:00.000Z'
),
(
  'histjpe25_2ec685a669b9537a', 'JPE-10052025-001', '2025-05-10', '2025-05-10', '2025-05-10',
  'SAJAHAN ALAM ALI', '9679364669', '', 'Jalpaiguri', '9', 'Male',
  'DHUPGURI, DHUPGURI, DHUPGURI, JALPAIGURI', 'Piles', '21000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:00:00.000Z', '2025-05-10T10:00:00.000Z'
),
(
  'histjpe25_945926a76c21e48d', 'JPE-10052025-002', '2025-05-10', '2025-05-10', '2025-05-10',
  'NIPUN SAHA', '9679872378', '', 'Jalpaiguri', '36', 'Male',
  'MAYNAGURI, MAYNAGURI, MAYNAGURI, JALPAIGURI', 'Piles, Fistula', '35625',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:00:00.000Z', '2025-05-10T10:00:00.000Z'
),
(
  'histjpe25_b15c771150a65f7f', 'JPE-10052025-003', '2025-05-10', '2025-05-10', '2025-05-10',
  'SUMAN SAHA', '8617389224', '', 'Jalpaiguri', '30', 'Male',
  'CHARAKDANGI, PANGA SAHEBBARI, KOTWALI, JALPAIGURI', 'Piles', '22800',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:00:00.000Z', '2025-05-10T10:00:00.000Z'
),
(
  'histjpe25_fd3325790c83f1fd', 'JPE-10052025-004', '2025-05-10', '2025-05-10', '2025-05-10',
  'RUPESH KUMAR', '9641077632', '', 'Jalpaiguri', '38', 'Male',
  'BIR PARA, BIR PARA, BIRPARA, JALPAIGURI', 'Fistula', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:00:00.000Z', '2025-05-10T10:00:00.000Z'
),
(
  'histjpe25_00ff122cefd103bc', 'JPE-10052025-005', '2025-05-10', '2025-05-10', '2025-05-10',
  'SURAJ SAHA', '9883377003', '', 'Jalpaiguri', '25', 'Male',
  '', 'Hydrocele', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:00:00.000Z', '2025-05-10T10:00:00.000Z'
),
(
  'histjpe25_81f64612d1aec5b9', 'JPE-13052025-001', '2025-05-13', '2025-05-13', '2025-05-13',
  'GOLOK SARKAR', '8927425993', '', 'Jalpaiguri', '50', 'Male',
  'MAYNAGURI, JALPAIGURI', 'Piles', '24000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-13T10:00:00.000Z', '2025-05-13T10:00:00.000Z'
),
(
  'histjpe25_63c02bb1d326a36e', 'JPE-17052025-001', '2025-05-17', '2025-05-17', '2025-05-17',
  'SHIPEN ROY', '9635284001', '', 'Jalpaiguri', '60', 'Male',
  'BODAGONJ', 'Piles', '27200',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:00:00.000Z', '2025-05-17T10:00:00.000Z'
),
(
  'histjpe25_644de92704e7b28c', 'JPE-18052025-001', '2025-05-18', '2025-05-18', '2025-05-18',
  'BABAN DAS', '9832026423', '', 'Jalpaiguri', '41', 'Male',
  'DANGA PARA, PANDA PARA, KOTWALI, JALPAIGURI', 'Piles', '38000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-18T10:00:00.000Z', '2025-05-18T10:00:00.000Z'
),
(
  'histjpe25_cf5a27df3cedfc70', 'JPE-24052025-001', '2025-05-24', '2025-05-24', '2025-05-24',
  'SHIBU ROY', '9933472497', '', 'Jalpaiguri', '37', 'Male',
  '', 'Gupt Rog', '19400',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-24T10:00:00.000Z', '2025-05-24T10:00:00.000Z'
),
(
  'histjpe25_3e46e10f0f8720d6', 'JPE-27052025-001', '2025-05-27', '2025-05-27', '2025-05-27',
  'SARAT ROY', '9734938612', '', 'Jalpaiguri', '38', 'Male',
  'BODAGANJ, BAROPATIA, KOTWALI, JALPAIGURI', 'Piles', '25500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-27T10:00:00.000Z', '2025-05-27T10:00:00.000Z'
),
(
  'histjpe25_46e86ceee3f7f1ee', 'JPE-29052025-001', '2025-05-29', '2025-05-29', '2025-05-29',
  'MAYA BARMAN', '7866811699', '', 'Jalpaiguri', '43', 'Female',
  'RAGBARI PARA, B.D.O OFFICE, KOTWALI, JALPAIGURI', 'Piles', '28000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-29T10:00:00.000Z', '2025-05-29T10:00:00.000Z'
),
(
  'histjpe25_ef1ee5dc6aa7ceae', 'JPE-31052025-001', '2025-05-31', '2025-05-31', '2025-05-31',
  'JOYESNA SARKAR', '9734938612', '', 'Jalpaiguri', '35', 'Female',
  'KASIYABARI, KASIYABARI, HALDIBARI, COOCHBEHAR', 'Piles', '23500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-31T10:00:00.000Z', '2025-05-31T10:00:00.000Z'
),
(
  'histjpe25_0735ca7c271fc057', 'JPE-31052025-002', '2025-05-31', '2025-05-31', '2025-05-31',
  'NAMITA ROY', '8509221401', '', 'Jalpaiguri', '40', 'Female',
  'BERUBARI, SRIRAM PARA, KOTWALI, JALPAIGURI', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-31T10:00:00.000Z', '2025-05-31T10:00:00.000Z'
),
(
  'histjpe25_e402a891f4cceb61', 'JPE-07062025-001', '2025-06-07', '2025-06-07', '2025-06-07',
  'RIJIYA SULTANA', '9434606517', '', 'Jalpaiguri', '43', 'Male',
  'NEW CERCULAR ROAD, JALPAIGURI, JALPAIGURI, JALPAIGURI', 'Piles', '27500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-07T10:00:00.000Z', '2025-06-07T10:00:00.000Z'
),
(
  'histjpe25_13ae1cbccea083f8', 'JPE-07062025-002', '2025-06-07', '2025-06-07', '2025-06-07',
  'HARIPADO MANDAL', '7718667453', '', 'Jalpaiguri', '45', 'Male',
  'KURSAMARI, PASCHIM MALLICK PARA, DHUPGURI, JALPAIGURI', 'Hydrocele', '19200',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-07T10:00:00.000Z', '2025-06-07T10:00:00.000Z'
),
(
  'histjpe25_d90936700977397f', 'JPE-10062025-001', '2025-06-10', '2025-06-10', '2025-06-10',
  'DINONATH PAL', '8972684145', '', 'Jalpaiguri', '48', 'Male',
  'RAJGANJ, RAJGANJ, RAJGANJ, JALPAIGURI', 'Piles', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-10T10:00:00.000Z', '2025-06-10T10:00:00.000Z'
),
(
  'histjpe25_8c30c64f2a774709', 'JPE-17062025-001', '2025-06-17', '2025-06-17', '2025-06-17',
  'SONATAN ROY', '9002967240', '', 'Jalpaiguri', '38', 'Male',
  'MUNSI PARA, KADOBARI, KOTWALI, JALPAIGURI', 'Piles', '19000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-17T10:00:00.000Z', '2025-06-17T10:00:00.000Z'
),
(
  'histjpe25_a1c7444afd7bd77d', 'JPE-21062025-001', '2025-06-21', '2025-06-21', '2025-06-21',
  'JOYDIP MANDAL', '9474454671', '', 'Jalpaiguri', '22', 'Male',
  'MEKLIGANJ, MEKLIGANJ, MEKLIGANJ, COOCHBEHAR', 'Piles', '21500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:00:00.000Z', '2025-06-21T10:00:00.000Z'
),
(
  'histjpe25_630178ce7b25f645', 'JPE-24062025-001', '2025-06-24', '2025-06-24', '2025-06-24',
  'MITHUN RISHI', '7679263894', '', 'Jalpaiguri', '32', 'Male',
  'BANIYA PARA, KADOBARI, KOTWALI, JALPAIGURI', 'Piles', '17600',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-24T10:00:00.000Z', '2025-06-24T10:00:00.000Z'
),
(
  'histjpe25_e473f2273ccbcc04', 'JPE-28062025-001', '2025-06-28', '2025-06-28', '2025-06-28',
  'PROFULLA BOSAK', '7797442776', '', 'Jalpaiguri', '40', 'Male',
  'JORPAKRI, JORPAKRI, MOYNAGURI, JALPAIGURI', 'Piles', '26000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:00:00.000Z', '2025-06-28T10:00:00.000Z'
),
(
  'histjpe25_8381d6374a636c25', 'JPE-28062025-002', '2025-06-28', '2025-06-28', '2025-06-28',
  'SOURAV SEN GUPTA', '7797852870', '', 'Jalpaiguri', '33', 'Male',
  'KADAMTALA, JALPAIGURI, KOTWALI, JALPAIGURI', 'Piles', '26500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:00:00.000Z', '2025-06-28T10:00:00.000Z'
),
(
  'histjpe25_f08131122668bf4b', 'JPE-08072025-001', '2025-07-08', '2025-07-08', '2025-07-08',
  'ASHIS MINJ ORAN', '7679263894', '', 'Jalpaiguri', '38', 'Male',
  'VAGAT PUR, NAGRAKATA, NAGRAKATA, JALPAIGURI', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-08T10:00:00.000Z', '2025-07-08T10:00:00.000Z'
),
(
  'histjpe25_1d75d2a7db80e74d', 'JPE-08072025-002', '2025-07-08', '2025-07-08', '2025-07-08',
  'ROMICHA KHATUN', '7074945102', '', 'Jalpaiguri', '55', 'Female',
  'CHAULHATI, CHAULHATI, RAJGANJ, JALPAIGURI', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-08T10:00:00.000Z', '2025-07-08T10:00:00.000Z'
),
(
  'histjpe25_fa50f9ef3d4ed579', 'JPE-12072025-001', '2025-07-12', '2025-07-12', '2025-07-12',
  'SHYAMALI ROY', '8250448488', '', 'Jalpaiguri', '38', 'Male',
  'BASUSOVA, CHAPADANGA, KOTWALI, JALPAIGURI', 'Piles', '17000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-12T10:00:00.000Z', '2025-07-12T10:00:00.000Z'
),
(
  'histjpe25_d189ca719783ec26', 'JPE-22072025-001', '2025-07-22', '2025-07-22', '2025-07-22',
  'ROKEYA KHATUN', '9911624524', '', 'Jalpaiguri', '31', 'Female',
  'SANTI PARA, UTTARKANYA, VAKTINAGAR, JALPAIGURI', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-22T10:00:00.000Z', '2025-07-22T10:00:00.000Z'
),
(
  'histjpe25_5405916cad7287b8', 'JPE-29072025-001', '2025-07-29', '2025-07-29', '2025-07-29',
  'KRISHNA MOHAN ROY', '7585991625', '', 'Jalpaiguri', '45', 'Female',
  'RAJAR HAT, PUTIMARI, MAYNAGURI, JALPAIGURI', 'Fistula', '45000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-29T10:00:00.000Z', '2025-07-29T10:00:00.000Z'
),
(
  'histjpe25_7cf2f9820031cf86', 'JPE-02082025-001', '2025-08-02', '2025-08-02', '2025-08-02',
  'KRISHNAPADA MANDAL', '8327477184', '', 'Jalpaiguri', '38', 'Male',
  'FALAKATA, FALAKATA, FALAKATA, ALIPURDUAR', 'Fissure', '38000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:00:00.000Z', '2025-08-02T10:00:00.000Z'
),
(
  'histjpe25_a0d7e4a04164cbfa', 'JPE-02082025-002', '2025-08-02', '2025-08-02', '2025-08-02',
  'BIKRAM ROY', '7679666423', '', 'Jalpaiguri', '50', 'Male',
  '+      JORPAKRI, JORPAKRI, MAYNAGURI, JALPAIGURI', 'Hydrocele', '23000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:00:00.000Z', '2025-08-02T10:00:00.000Z'
),
(
  'histjpe25_1f37f89c7b4ca12e', 'JPE-02082025-003', '2025-08-02', '2025-08-02', '2025-08-02',
  'SARAT CH ROY', '9832380655', '', 'Jalpaiguri', '51', 'Male',
  'SILIGURI, SEVOK ROAD, VAKTI NAGAR, JALPAIGURI', 'Piles', '46000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:00:00.000Z', '2025-08-02T10:00:00.000Z'
),
(
  'histjpe25_7fb6f899d94a50b8', 'JPE-02082025-004', '2025-08-02', '2025-08-02', '2025-08-02',
  'SANTASH RAM BHAGAT', '8116710061', '', 'Jalpaiguri', '46', 'Male',
  'SATKURA, SATKURA, KATWALI, JALPAIGURI', 'Piles', '27000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:00:00.000Z', '2025-08-02T10:00:00.000Z'
),
(
  'histjpe25_630587155bde0668', 'JPE-05082025-001', '2025-08-05', '2025-08-05', '2025-08-05',
  'SHEAK HASAN', '9907561162', '', 'Jalpaiguri', '29', 'Male',
  'MAYNAGURI, MAYNAGURI,BAGHJAN, MAYNAGURI, JALPAIGURI', 'Piles, Other', '17500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-05T10:00:00.000Z', '2025-08-05T10:00:00.000Z'
),
(
  'histjpe25_a69ff2f187b047f4', 'JPE-09082025-001', '2025-08-09', '2025-08-09', '2025-08-09',
  'NIRANJAN ROY', '6238749763', '', 'Jalpaiguri', '40', 'Male',
  'NATUN PARA, DHUBRI BARI, HALDIBARI, JALPAIGURI', 'Piles', '23000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-09T10:00:00.000Z', '2025-08-09T10:00:00.000Z'
),
(
  'histjpe25_041fc465383ca449', 'JPE-12082025-001', '2025-08-12', '2025-08-12', '2025-08-12',
  'SAHEBUL ISLAM', '7548083702', '', 'Jalpaiguri', '27', 'Male',
  'AKRIGOS, BOLORAM, VAKTINAGAR, JALPAIGURI', 'Piles', '31000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-12T10:00:00.000Z', '2025-08-12T10:00:00.000Z'
),
(
  'histjpe25_1b3f726cb3f85a6b', 'JPE-19082025-001', '2025-08-19', '2025-08-19', '2025-08-19',
  'JAHANGIR ALAM', '9832774511', '', 'Jalpaiguri', '30', 'Male',
  'DHUPGURI, PATKIDAHA, DHUPGURI, JALPAIGURI', 'Piles', '36000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-19T10:00:00.000Z', '2025-08-19T10:00:00.000Z'
),
(
  'histjpe25_9884ba31c1fc84b4', 'JPE-19082025-002', '2025-08-19', '2025-08-19', '2025-08-19',
  'KAMALA DAS', '8388975355', '', 'Jalpaiguri', '30', 'Male',
  'BELACOBA, PRACHNA NAGAR, RAJGANG, JALPAIGURI', 'Piles', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-19T10:00:00.000Z', '2025-08-19T10:00:00.000Z'
),
(
  'histjpe25_7869bed96fb2b219', 'JPE-23082025-001', '2025-08-23', '2025-08-23', '2025-08-23',
  'KAYU MALI', '7699983914', '', 'Jalpaiguri', '34', 'Male',
  'CHAPA GURI, GOPAL BAGAN, MADARI HAT, ALIPURDUAR', 'Piles', '34000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:00:00.000Z', '2025-08-23T10:00:00.000Z'
),
(
  'histjpe25_134f162f63c28f5b', 'JPE-23082025-002', '2025-08-23', '2025-08-23', '2025-08-23',
  'SUMITA ROY', '8972515634', '', 'Jalpaiguri', '42', 'Male',
  'LALBAZAR PARA, BERUBARI, KOTWALI, JALPAIGURI', 'Piles', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:00:00.000Z', '2025-08-23T10:00:00.000Z'
),
(
  'histjpe25_06a1b4de8e604826', 'JPE-30082025-001', '2025-08-30', '2025-08-30', '2025-08-30',
  'JAHIRUL HAQUE', '7363946034', '', 'Jalpaiguri', '30', 'Male',
  'DAS DARGA, KARJI PARA, KOTWALI, JALPAIGURI', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:00:00.000Z', '2025-08-30T10:00:00.000Z'
),
(
  'histjpe25_9865902f66c4b52d', 'JPE-31082025-001', '2025-08-31', '2025-08-31', '2025-08-31',
  'PRANAB ROY', '6296125142', '', 'Jalpaiguri', '27', 'Male',
  'SINGIMARI, BERUBARI, KOTWALI, JALPAIGURI', 'Piles', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-31T10:00:00.000Z', '2025-08-31T10:00:00.000Z'
),
(
  'histjpe25_edada66a87ac2fd9', 'JPE-06092025-001', '2025-09-06', '2025-09-06', '2025-09-06',
  'SHUVANKAR TANTRA', '8967918639', '', 'Jalpaiguri', '32', 'Male',
  'BALA PARA, PAHARPUR, KOTWALI, JALPAIGURI', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:00:00.000Z', '2025-09-06T10:00:00.000Z'
),
(
  'histjpe25_6a057120b715b80b', 'JPE-09092025-001', '2025-09-09', '2025-09-09', '2025-09-09',
  'PURNADEB BISWAS', '8848653776', '', 'Jalpaiguri', '31', 'Male',
  'GAJOLDOBA, GAJOLDOBA, MALBAZAR, JALPAIGURI', 'Fistula', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-09T10:00:00.000Z', '2025-09-09T10:00:00.000Z'
),
(
  'histjpe25_46d11867a65bb608', 'JPE-16092025-001', '2025-09-16', '2025-09-16', '2025-09-16',
  'SULOCHINI PASOYAN', '7478866361', '', 'Jalpaiguri', '25', 'Male',
  'SANTIPARA, JALPAIGURI, KOTWALI, JALPAIGURI', 'Piles', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-16T10:00:00.000Z', '2025-09-16T10:00:00.000Z'
),
(
  'histjpe25_eff5729a10c56847', 'JPE-20092025-001', '2025-09-20', '2025-09-20', '2025-09-20',
  'PARUL SARKAR', '7384033340', '', 'Jalpaiguri', '42', 'Male',
  'HALDIBARI, HALDIBARI, HALDIBARI, COOCHBEHAR', 'Piles', '17850',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:00:00.000Z', '2025-09-20T10:00:00.000Z'
),
(
  'histjpe25_38688ee2c9a42884', 'JPE-20092025-002', '2025-09-20', '2025-09-20', '2025-09-20',
  'BIRJINIA KANDULNA', '7076908674', '', 'Jalpaiguri', '30', 'Male',
  'MORA HAT, BINNAGURI, BANARHAT, JALPAIGURI', 'Piles', '18500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:00:00.000Z', '2025-09-20T10:00:00.000Z'
),
(
  'histjpe25_3a834c910a10b202', 'JPE-22092025-001', '2025-09-22', '2025-09-22', '2025-09-22',
  'SANDIPA KUNDU', '8670828932', '', 'Jalpaiguri', '42', 'Male',
  'UTTAR RAIKOTPARA, DINBAJAR, KOTWALI, JALPAIGURI', 'Piles', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-22T10:00:00.000Z', '2025-09-22T10:00:00.000Z'
),
(
  'histjpe25_342ddd36501ab44b', 'JPE-23092025-001', '2025-09-23', '2025-09-23', '2025-09-23',
  'ANNAPURNA ROY', '9475808989', '', 'Jalpaiguri', '56', 'Male',
  'RAIKAT PARA, DINBAJAR, KOTWALI, JALPAIGURI', 'Piles', '23000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-23T10:00:00.000Z', '2025-09-23T10:00:00.000Z'
),
(
  'histjpe25_79ef0db3eddcc11d', 'JPE-23092025-002', '2025-09-23', '2025-09-23', '2025-09-23',
  'TUMPA BEGAM', '8927836474', '', 'Jalpaiguri', '25', 'Female',
  'BALAKOPA SONARBARI, PRASANNA NAGAR, RAJGANG, JALPAIGURI', 'Piles', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-23T10:00:00.000Z', '2025-09-23T10:00:00.000Z'
),
(
  'histjpe25_cb500526f68b7ddd', 'JPE-27092025-001', '2025-09-27', '2025-09-27', '2025-09-27',
  'CHANDAN KUNDU', '9832371611', '', 'Jalpaiguri', '38', 'Male',
  'BELAKOBA, PROSANNA NAGAR, RAJGONGE, JALPAIGURI', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:00:00.000Z', '2025-09-27T10:00:00.000Z'
),
(
  'histjpe25_8af18d14e95579f1', 'JPE-04102025-001', '2025-10-04', '2025-10-04', '2025-10-04',
  'SONALY ROY', '8597910258', '', 'Jalpaiguri', '32', 'Male',
  'VOTPATTI, VOTPATTI, MAYNAGURI, JALPAIGURI', 'Piles', '28000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:00:00.000Z', '2025-10-04T10:00:00.000Z'
),
(
  'histjpe25_690a64c9821640f8', 'JPE-07102025-001', '2025-10-07', '2025-10-07', '2025-10-07',
  'BIPLAB SARKAR', '7905763228', '', 'Jalpaiguri', '41', 'Male',
  'UTTAR RAYKAT PARA, DINBAZAR, KOTWALI, JALPAIGURI', 'Piles', '45000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-07T10:00:00.000Z', '2025-10-07T10:00:00.000Z'
),
(
  'histjpe25_eb5e4f634f9123f3', 'JPE-18102025-001', '2025-10-18', '2025-10-18', '2025-10-18',
  'MALEKA PARVIN', '9932427384', '', 'Jalpaiguri', '49', 'Female',
  'RANIRHAT, JABRAMALI, MAYNAGURI, JALPAIGURI', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:00:00.000Z', '2025-10-18T10:00:00.000Z'
),
(
  'histjpe25_779ff1e5ccf6c397', 'JPE-18102025-002', '2025-10-18', '2025-10-18', '2025-10-18',
  'SNIGKDHA BHATTACHARYA', '9046288011', '', 'Jalpaiguri', '40', 'Male',
  'JALPAIGURI, JALPAIGURI, KOTWALI, JALPAIGURI', 'Piles', '24000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:00:00.000Z', '2025-10-18T10:00:00.000Z'
),
(
  'histjpe25_b81ffad462a77034', 'JPE-18102025-003', '2025-10-18', '2025-10-18', '2025-10-18',
  'MADHAV CH. SARKAR', '8927555801', '', 'Jalpaiguri', '65', 'Male',
  'SDA COMPLEX, DENGUAJHAR, KOTWALI, JALPAIGURI', 'Fistula', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:00:00.000Z', '2025-10-18T10:00:00.000Z'
),
(
  'histjpe25_a9f3335142fceb55', 'JPE-25102025-001', '2025-10-25', '2025-10-25', '2025-10-25',
  'SUKUMAR PAHARI', '8116124449', '', 'Jalpaiguri', '52', 'Male',
  'SATKURA, SATKURA, KOTWALI, JALPAIGURI', 'Piles', '21000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-25T10:00:00.000Z', '2025-10-25T10:00:00.000Z'
),
(
  'histjpe25_9b59497c40762b8e', 'JPE-28102025-001', '2025-10-28', '2025-10-28', '2025-10-28',
  'RAJIB BISWAS', '6296182593', '', 'Jalpaiguri', '37', 'Male',
  'JURAN PARA, RAJGANJ, RAJGANJ, JALPAIGURI', 'Piles', '45000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-28T10:00:00.000Z', '2025-10-28T10:00:00.000Z'
),
(
  'histjpe25_195023c787324188', 'JPE-15112025-001', '2025-11-15', '2025-11-15', '2025-11-15',
  'SACHIN ROY', '9800244353', '', 'Jalpaiguri', '70', 'Male',
  'SUKANTA NAGAR, HAKIMPARA, KOTWALI, JALPAIGURI', 'Piles', '15540',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:00:00.000Z', '2025-11-15T10:00:00.000Z'
),
(
  'histjpe25_e8f0ac0c609bd420', 'JPE-23112025-001', '2025-11-23', '2025-11-23', '2025-11-23',
  'PRATIVA DHAR', '7001203085', '', 'Jalpaiguri', '43', 'Male',
  'BOUBAZAR, JALPAIGURI, KOTWALI, JALPAIGURI', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-23T10:00:00.000Z', '2025-11-23T10:00:00.000Z'
),
(
  'histjpe25_04c58395cda59057', 'JPE-25112025-001', '2025-11-25', '2025-11-25', '2025-11-25',
  'MONIRUL ALAM', '9749984663', '', 'Jalpaiguri', '26', 'Male',
  'MOUTH PARA, DENGUAJHAR, KOTWALI, JALPAIGURI', 'Piles', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-25T10:00:00.000Z', '2025-11-25T10:00:00.000Z'
),
(
  'histjpe25_b45bad482a5a7a92', 'JPE-29112025-001', '2025-11-29', '2025-11-29', '2025-11-29',
  'BISHU ROY', '8101805878', '', 'Jalpaiguri', '41', 'Male',
  'BAMAN PARA, KHARIA, KOTWALI, JALPAIGURI', 'Fistula', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-29T10:00:00.000Z', '2025-11-29T10:00:00.000Z'
),
(
  'histjpe25_7fbc3f5f6aad0086', 'JPE-02122025-001', '2025-12-02', '2025-12-02', '2025-12-02',
  'PAPON ROY', '8388939713', '', 'Jalpaiguri', '30', 'Male',
  'PATKATA, RANGDHANALY, KOTWALI, JALPAIGURI', 'Piles', '24000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-02T10:00:00.000Z', '2025-12-02T10:00:00.000Z'
),
(
  'histjpe25_a9ea47611e0cd849', 'JPE-02122025-002', '2025-12-02', '2025-12-02', '2025-12-02',
  'UTTAM MANDAL', '8921170584', '', 'Jalpaiguri', '31', 'Male',
  'JHAJHANGI, CHURABHANDAR, MAYNAGURI, JALPAIGURI', 'Piles', '36000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-02T10:00:00.000Z', '2025-12-02T10:00:00.000Z'
),
(
  'histjpe25_9ef43bb18c91163c', 'JPE-06122025-001', '2025-12-06', '2025-12-06', '2025-12-06',
  'ANJU BEGAM', '7679693511', '', 'Jalpaiguri', '26', 'Female',
  'CHANGRABANDHYA, CHANGRABANDHYA, MEKHLIGANG, COOCHBEHAR', 'Fistula', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-06T10:00:00.000Z', '2025-12-06T10:00:00.000Z'
),
(
  'histjpe25_32cb0178bd9e6b7b', 'JPE-08122025-001', '2025-12-08', '2025-12-08', '2025-12-08',
  'MD UJIR', '9832236466', '', 'Jalpaiguri', '60', 'Male',
  'BELAKOBA, PRASANNA NAGAR, RAJGANG, JALPAIGURI', 'Fistula', '28000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-08T10:00:00.000Z', '2025-12-08T10:00:00.000Z'
),
(
  'histjpe25_25d9a6b992a29637', 'JPE-12122025-001', '2025-12-12', '2025-12-12', '2025-12-12',
  'INDU HR', '8695590721', '', 'Jalpaiguri', '27', 'Male',
  'KALCHINI, KALCHINI, KALCHINI, ALIPURDUAR', 'Piles', '24000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-12T10:00:00.000Z', '2025-12-12T10:00:00.000Z'
),
(
  'histjpe25_89a203fdeeb49755', 'JPE-13122025-001', '2025-12-13', '2025-12-13', '2025-12-13',
  'NARAYAN ROY', '9401865077', '', 'Jalpaiguri', '19', 'Male',
  'SIPAI PARA, UTTAR DHANTALA, KOTWALI, JALPAIGURI', 'Piles', '24000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:00:00.000Z', '2025-12-13T10:00:00.000Z'
),
(
  'histjpe25_52d0ff504833fc95', 'JPE-13122025-002', '2025-12-13', '2025-12-13', '2025-12-13',
  'NIRANJAN MANDAL', '9378149491', '', 'Jalpaiguri', '62', 'Male',
  'INDRA COLONY, DENGUAJHAR, KOTWALI, JALPAIGURI', 'Piles', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:00:00.000Z', '2025-12-13T10:00:00.000Z'
),
(
  'histjpe25_d162c7cdf458f6d7', 'JPE-13122025-003', '2025-12-13', '2025-12-13', '2025-12-13',
  'SANJIT ROY', '9832765330', '', 'Jalpaiguri', '31', 'Male',
  'TALMA, 7 KHAMAR, KOTWALI, JALPAIGURI', 'Piles', '28000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:00:00.000Z', '2025-12-13T10:00:00.000Z'
),
(
  'histjpe25_407d3550090ef7c6', 'JPE-16122025-001', '2025-12-16', '2025-12-16', '2025-12-16',
  'TAPAS MANDAL', '6295821544', '', 'Jalpaiguri', '25', 'Male',
  'JHAJHANGI, VANGAMARI, MAYNAGURI, JALPAIGURI', 'Piles, Fissure', '27000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-16T10:00:00.000Z', '2025-12-16T10:00:00.000Z'
),
(
  'histjpe25_da1dcaa4abc35624', 'JPE-27122025-001', '2025-12-27', '2025-12-27', '2025-12-27',
  'SAHADAT ALI', '9635465400', '', 'Jalpaiguri', '42', 'Male',
  'GORAL BARI, GORAL BARI, KOTWALI, JALPAIGURI', 'Piles', '27300',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:00:00.000Z', '2025-12-27T10:00:00.000Z'
),
(
  'histjpe25_6a0d0900bbd7f13c', 'JPE-27122025-002', '2025-12-27', '2025-12-27', '2025-12-27',
  'BIDHAN CH. SING', '8116734967', '', 'Jalpaiguri', '52', 'Male',
  'SANTI PARA, JALPAIGURI, KOTWALI, JALPAIGURI', 'Piles', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:00:00.000Z', '2025-12-27T10:00:00.000Z'
),
(
  'histjpe25_49c9b42a2a9b8203', 'JPE-27122025-003', '2025-12-27', '2025-12-27', '2025-12-27',
  'ANIKA ROY', '8348640351', '', 'Jalpaiguri', '42', 'Female',
  'MOHIT NAGAR PASCHIM PARA, MOHIT NAGAR, KOTWALI, JALPAIGURI', 'Piles', '15500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:00:00.000Z', '2025-12-27T10:00:00.000Z'
),
(
  'histjpe25_0dd2b2c2bdc293e8', 'JPE-27122025-004', '2025-12-27', '2025-12-27', '2025-12-27',
  'RINA BARMAN', '8348640351', '', 'Jalpaiguri', '48', 'Female',
  'KALI BARI, JALPAIGURI, KOTWALI, JALPAIGURI', 'Piles', '26000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:00:00.000Z', '2025-12-27T10:00:00.000Z'
),
(
  'histjpe25_05590ea482349912', 'JPE-31122025-001', '2025-12-31', '2025-12-31', '2025-12-31',
  'MINA SARKAR', '9647231089', '', 'Jalpaiguri', '40', 'Female',
  'KASIABARU, BORO HALDIBARI, HAL DIBARI, COOCHBEHAR', 'Piles', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-31T10:00:00.000Z', '2025-12-31T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'histjpe25_pay_8aec06da8b849f3a', 'treatment', 'Advance', 'Advance', 'histjpe25_b6f3eac44515256f', '8207218447', 'Jalpaiguri', 'MITHUN BARMAN',
  '2025-01-05', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-05T10:15:00.000Z', '2025-01-05T10:15:00.000Z'
),
(
  'histjpe25_pay_b2974b2c2dfb36e5', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_b6f3eac44515256f', '8207218447', 'Jalpaiguri', 'MITHUN BARMAN',
  '2025-02-25', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-25T10:16:00.000Z', '2025-02-25T10:16:00.000Z'
),
(
  'histjpe25_pay_c78a685064e634c2', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_b6f3eac44515256f', '8207218447', 'Jalpaiguri', 'MITHUN BARMAN',
  '2025-03-01', '10000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-01T10:17:00.000Z', '2025-03-01T10:17:00.000Z'
),
(
  'histjpe25_pay_75a7cb0387eca53c', 'treatment', '4th Payment', '4th Payment', 'histjpe25_b6f3eac44515256f', '8207218447', 'Jalpaiguri', 'MITHUN BARMAN',
  '2025-03-11', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-11T10:18:00.000Z', '2025-03-11T10:18:00.000Z'
),
(
  'histjpe25_pay_c8aa827235f0c1da', 'treatment', '5th Payment', '5th Payment', 'histjpe25_b6f3eac44515256f', '8207218447', 'Jalpaiguri', 'MITHUN BARMAN',
  '2025-02-15', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-15T10:19:00.000Z', '2025-02-15T10:19:00.000Z'
),
(
  'histjpe25_pay_609b9f123f0441a2', 'treatment', 'Advance', 'Advance', 'histjpe25_7fb80024970ca366', '7384437061', 'Jalpaiguri', 'DEV ROY SARKAR',
  '2025-01-11', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-11T10:15:00.000Z', '2025-01-11T10:15:00.000Z'
),
(
  'histjpe25_pay_1f7c4079cbedd636', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_7fb80024970ca366', '7384437061', 'Jalpaiguri', 'DEV ROY SARKAR',
  '2025-01-25', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-25T10:16:00.000Z', '2025-01-25T10:16:00.000Z'
),
(
  'histjpe25_pay_9185dee6b8b33291', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_7fb80024970ca366', '7384437061', 'Jalpaiguri', 'DEV ROY SARKAR',
  '2025-02-08', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-08T10:17:00.000Z', '2025-02-08T10:17:00.000Z'
),
(
  'histjpe25_pay_2dd99ce0c15f0458', 'treatment', '4th Payment', '4th Payment', 'histjpe25_7fb80024970ca366', '7384437061', 'Jalpaiguri', 'DEV ROY SARKAR',
  '2025-02-15', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-15T10:18:00.000Z', '2025-02-15T10:18:00.000Z'
),
(
  'histjpe25_pay_e0a98f0aa8890f73', 'treatment', '5th Payment', '5th Payment', 'histjpe25_7fb80024970ca366', '7384437061', 'Jalpaiguri', 'DEV ROY SARKAR',
  '2025-02-22', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-22T10:19:00.000Z', '2025-02-22T10:19:00.000Z'
),
(
  'histjpe25_pay_3c7e2819a8afee3d', 'treatment', '6th Payment', '6th Payment', 'histjpe25_7fb80024970ca366', '7384437061', 'Jalpaiguri', 'DEV ROY SARKAR',
  '2025-03-08', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-08T10:20:00.000Z', '2025-03-08T10:20:00.000Z'
),
(
  'histjpe25_pay_4b4eb272644b0bc5', 'treatment', '7th Payment', '7th Payment', 'histjpe25_7fb80024970ca366', '7384437061', 'Jalpaiguri', 'DEV ROY SARKAR',
  '2025-04-19', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-19T10:21:00.000Z', '2025-04-19T10:21:00.000Z'
),
(
  'histjpe25_pay_3b5e6a18b439fafd', 'treatment', '8th Payment', '8th Payment', 'histjpe25_7fb80024970ca366', '7384437061', 'Jalpaiguri', 'DEV ROY SARKAR',
  '2025-05-17', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:22:00.000Z', '2025-05-17T10:22:00.000Z'
),
(
  'histjpe25_pay_45e6e11e987f4921', 'treatment', '9th Payment', '9th Payment', 'histjpe25_7fb80024970ca366', '7384437061', 'Jalpaiguri', 'DEV ROY SARKAR',
  '2025-06-07', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-07T10:23:00.000Z', '2025-06-07T10:23:00.000Z'
),
(
  'histjpe25_pay_ef37af20a2bfac22', 'treatment', 'Advance', 'Advance', 'histjpe25_3be9b4406b363cd8', '9547814633', 'Jalpaiguri', 'MOLIN CHANDRA ROY',
  '2025-01-11', '7000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-11T10:15:00.000Z', '2025-01-11T10:15:00.000Z'
),
(
  'histjpe25_pay_1cdee25b5d0f20c8', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_3be9b4406b363cd8', '9547814633', 'Jalpaiguri', 'MOLIN CHANDRA ROY',
  '2025-01-25', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-25T10:16:00.000Z', '2025-01-25T10:16:00.000Z'
),
(
  'histjpe25_pay_a31de25bd96b2334', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_3be9b4406b363cd8', '9547814633', 'Jalpaiguri', 'MOLIN CHANDRA ROY',
  '2025-01-21', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-21T10:17:00.000Z', '2025-01-21T10:17:00.000Z'
),
(
  'histjpe25_pay_cdca3d805495dd34', 'treatment', '4th Payment', '4th Payment', 'histjpe25_3be9b4406b363cd8', '9547814633', 'Jalpaiguri', 'MOLIN CHANDRA ROY',
  '2025-01-27', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-27T10:18:00.000Z', '2025-01-27T10:18:00.000Z'
),
(
  'histjpe25_pay_dc31c2dd87cc54fc', 'treatment', '5th Payment', '5th Payment', 'histjpe25_3be9b4406b363cd8', '9547814633', 'Jalpaiguri', 'MOLIN CHANDRA ROY',
  '2025-02-01', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-01T10:19:00.000Z', '2025-02-01T10:19:00.000Z'
),
(
  'histjpe25_pay_c1fb5ceaaffa739c', 'treatment', '6th Payment', '6th Payment', 'histjpe25_3be9b4406b363cd8', '9547814633', 'Jalpaiguri', 'MOLIN CHANDRA ROY',
  '2025-02-08', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-08T10:20:00.000Z', '2025-02-08T10:20:00.000Z'
),
(
  'histjpe25_pay_c41a573f479f48ac', 'treatment', '7th Payment', '7th Payment', 'histjpe25_3be9b4406b363cd8', '9547814633', 'Jalpaiguri', 'MOLIN CHANDRA ROY',
  '2025-01-17', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-17T10:21:00.000Z', '2025-01-17T10:21:00.000Z'
),
(
  'histjpe25_pay_af4951670c446add', 'treatment', 'Advance', 'Advance', 'histjpe25_c622aad011516cbf', '7679849423', 'Jalpaiguri', 'KHARGA NARAYAN ROY',
  '2025-11-18', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-18T10:15:00.000Z', '2025-11-18T10:15:00.000Z'
),
(
  'histjpe25_pay_bc79d571c51e635c', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_c622aad011516cbf', '7679849423', 'Jalpaiguri', 'KHARGA NARAYAN ROY',
  '2025-11-20', '350', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-20T10:16:00.000Z', '2025-11-20T10:16:00.000Z'
),
(
  'histjpe25_pay_5fea4e43a334be54', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_c622aad011516cbf', '7679849423', 'Jalpaiguri', 'KHARGA NARAYAN ROY',
  '2025-11-25', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-25T10:17:00.000Z', '2025-11-25T10:17:00.000Z'
),
(
  'histjpe25_pay_b7b44ceb9675849a', 'treatment', '4th Payment', '4th Payment', 'histjpe25_c622aad011516cbf', '7679849423', 'Jalpaiguri', 'KHARGA NARAYAN ROY',
  '2025-02-01', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-01T10:18:00.000Z', '2025-02-01T10:18:00.000Z'
),
(
  'histjpe25_pay_ba83fab7067b2de3', 'treatment', '5th Payment', '5th Payment', 'histjpe25_c622aad011516cbf', '7679849423', 'Jalpaiguri', 'KHARGA NARAYAN ROY',
  '2025-02-08', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-08T10:19:00.000Z', '2025-02-08T10:19:00.000Z'
),
(
  'histjpe25_pay_245cf6f33e97589d', 'treatment', '6th Payment', '6th Payment', 'histjpe25_c622aad011516cbf', '7679849423', 'Jalpaiguri', 'KHARGA NARAYAN ROY',
  '2025-02-15', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-15T10:20:00.000Z', '2025-02-15T10:20:00.000Z'
),
(
  'histjpe25_pay_39c65337523ca1aa', 'treatment', '7th Payment', '7th Payment', 'histjpe25_c622aad011516cbf', '7679849423', 'Jalpaiguri', 'KHARGA NARAYAN ROY',
  '2025-02-22', '800', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-22T10:21:00.000Z', '2025-02-22T10:21:00.000Z'
),
(
  'histjpe25_pay_049e8b5738f82521', 'treatment', '8th Payment', '8th Payment', 'histjpe25_c622aad011516cbf', '7679849423', 'Jalpaiguri', 'KHARGA NARAYAN ROY',
  '2025-03-01', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-01T10:22:00.000Z', '2025-03-01T10:22:00.000Z'
),
(
  'histjpe25_pay_186ac0c689d4c096', 'treatment', 'Advance', 'Advance', 'histjpe25_a71995fc5b0d9a24', '7475895957', 'Jalpaiguri', 'S.B CHAKRABORTY',
  '2025-01-26', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-26T10:15:00.000Z', '2025-01-26T10:15:00.000Z'
),
(
  'histjpe25_pay_807169265be43326', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_a71995fc5b0d9a24', '7475895957', 'Jalpaiguri', 'S.B CHAKRABORTY',
  '2025-02-01', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-01T10:16:00.000Z', '2025-02-01T10:16:00.000Z'
),
(
  'histjpe25_pay_176316aaf434e415', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_a71995fc5b0d9a24', '7475895957', 'Jalpaiguri', 'S.B CHAKRABORTY',
  '2025-02-08', '2500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-08T10:17:00.000Z', '2025-02-08T10:17:00.000Z'
),
(
  'histjpe25_pay_c21b0ae25b9fe2e8', 'treatment', '4th Payment', '4th Payment', 'histjpe25_a71995fc5b0d9a24', '7475895957', 'Jalpaiguri', 'S.B CHAKRABORTY',
  '2025-02-11', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-11T10:18:00.000Z', '2025-02-11T10:18:00.000Z'
),
(
  'histjpe25_pay_758a0f9ebae8b8d1', 'treatment', '5th Payment', '5th Payment', 'histjpe25_a71995fc5b0d9a24', '7475895957', 'Jalpaiguri', 'S.B CHAKRABORTY',
  '2025-02-25', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-25T10:19:00.000Z', '2025-02-25T10:19:00.000Z'
),
(
  'histjpe25_pay_db6167866139f01f', 'treatment', 'Advance', 'Advance', 'histjpe25_3cf34828bdb49163', '9641779180', 'Jalpaiguri', 'RAJKUMAR ROY',
  '2025-05-11', '4500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-11T10:15:00.000Z', '2025-05-11T10:15:00.000Z'
),
(
  'histjpe25_pay_057f5fb2e495dd97', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_3cf34828bdb49163', '9641779180', 'Jalpaiguri', 'RAJKUMAR ROY',
  '2025-05-17', '4500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:16:00.000Z', '2025-05-17T10:16:00.000Z'
),
(
  'histjpe25_pay_c01f061f4165bb7c', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_3cf34828bdb49163', '9641779180', 'Jalpaiguri', 'RAJKUMAR ROY',
  '2025-05-24', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-24T10:17:00.000Z', '2025-05-24T10:17:00.000Z'
),
(
  'histjpe25_pay_9ffa038b8e6fbf48', 'treatment', 'Advance', 'Advance', 'histjpe25_0aa2c2a2a2dd5026', '7432837172', 'Jalpaiguri', 'PALLABI MAJUMDA',
  '2025-01-25', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-01-25T10:15:00.000Z', '2025-01-25T10:15:00.000Z'
),
(
  'histjpe25_pay_241f7fdb191e3909', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_0aa2c2a2a2dd5026', '7432837172', 'Jalpaiguri', 'PALLABI MAJUMDA',
  '2025-02-01', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-01T10:16:00.000Z', '2025-02-01T10:16:00.000Z'
),
(
  'histjpe25_pay_ef899d5aff615e2b', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_0aa2c2a2a2dd5026', '7432837172', 'Jalpaiguri', 'PALLABI MAJUMDA',
  '2025-02-08', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-08T10:17:00.000Z', '2025-02-08T10:17:00.000Z'
),
(
  'histjpe25_pay_b2127133f7d43ea3', 'treatment', '4th Payment', '4th Payment', 'histjpe25_0aa2c2a2a2dd5026', '7432837172', 'Jalpaiguri', 'PALLABI MAJUMDA',
  '2025-02-13', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-13T10:18:00.000Z', '2025-02-13T10:18:00.000Z'
),
(
  'histjpe25_pay_80e6ada4b5135750', 'treatment', 'Advance', 'Advance', 'histjpe25_0944c60e522e4881', '7063830935', 'Jalpaiguri', 'SAMINA YASMIN',
  '2025-02-01', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-01T10:15:00.000Z', '2025-02-01T10:15:00.000Z'
),
(
  'histjpe25_pay_a5765a14f976a22c', 'treatment', 'Advance', 'Advance', 'histjpe25_3a7e65fe170e2fc3', '7001705669', 'Jalpaiguri', 'SAHJAHAN ISLAM',
  '2025-02-01', '150', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-01T10:15:00.000Z', '2025-02-01T10:15:00.000Z'
),
(
  'histjpe25_pay_2e723d0cdf3ba81b', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_3a7e65fe170e2fc3', '7001705669', 'Jalpaiguri', 'SAHJAHAN ISLAM',
  '2025-02-02', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-02T10:16:00.000Z', '2025-02-02T10:16:00.000Z'
),
(
  'histjpe25_pay_cf1a4634f27104ab', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_3a7e65fe170e2fc3', '7001705669', 'Jalpaiguri', 'SAHJAHAN ISLAM',
  '2025-02-04', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-04T10:17:00.000Z', '2025-02-04T10:17:00.000Z'
),
(
  'histjpe25_pay_d044a4d2f5200d72', 'treatment', '4th Payment', '4th Payment', 'histjpe25_3a7e65fe170e2fc3', '7001705669', 'Jalpaiguri', 'SAHJAHAN ISLAM',
  '2025-02-08', '700', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-08T10:18:00.000Z', '2025-02-08T10:18:00.000Z'
),
(
  'histjpe25_pay_af67be355b4ec383', 'treatment', '5th Payment', '5th Payment', 'histjpe25_3a7e65fe170e2fc3', '7001705669', 'Jalpaiguri', 'SAHJAHAN ISLAM',
  '2025-02-16', '1100', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-16T10:19:00.000Z', '2025-02-16T10:19:00.000Z'
),
(
  'histjpe25_pay_0968ca8b73d6ed21', 'treatment', '6th Payment', '6th Payment', 'histjpe25_3a7e65fe170e2fc3', '7001705669', 'Jalpaiguri', 'SAHJAHAN ISLAM',
  '2025-02-18', '1700', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-18T10:20:00.000Z', '2025-02-18T10:20:00.000Z'
),
(
  'histjpe25_pay_84487d36b4d83c9d', 'treatment', '7th Payment', '7th Payment', 'histjpe25_3a7e65fe170e2fc3', '7001705669', 'Jalpaiguri', 'SAHJAHAN ISLAM',
  '2025-02-25', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-25T10:21:00.000Z', '2025-02-25T10:21:00.000Z'
),
(
  'histjpe25_pay_845b4d6c14ad988c', 'treatment', '8th Payment', '8th Payment', 'histjpe25_3a7e65fe170e2fc3', '7001705669', 'Jalpaiguri', 'SAHJAHAN ISLAM',
  '2025-03-02', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-02T10:22:00.000Z', '2025-03-02T10:22:00.000Z'
),
(
  'histjpe25_pay_7e86efe53b68b2d7', 'treatment', '9th Payment', '9th Payment', 'histjpe25_3a7e65fe170e2fc3', '7001705669', 'Jalpaiguri', 'SAHJAHAN ISLAM',
  '2025-03-09', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-09T10:23:00.000Z', '2025-03-09T10:23:00.000Z'
),
(
  'histjpe25_pay_3361179cd221a241', 'treatment', 'Advance', 'Advance', 'histjpe25_43f6075c058285c2', '8918486092', 'Jalpaiguri', 'SHIBANI MANDAL',
  '2025-02-01', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-01T10:15:00.000Z', '2025-02-01T10:15:00.000Z'
),
(
  'histjpe25_pay_31b005710edfa5a7', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_43f6075c058285c2', '8918486092', 'Jalpaiguri', 'SHIBANI MANDAL',
  '2025-02-08', '8000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-08T10:16:00.000Z', '2025-02-08T10:16:00.000Z'
),
(
  'histjpe25_pay_153ee37c518dc81b', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_43f6075c058285c2', '8918486092', 'Jalpaiguri', 'SHIBANI MANDAL',
  '2025-02-22', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-22T10:17:00.000Z', '2025-02-22T10:17:00.000Z'
),
(
  'histjpe25_pay_59eb6f5c29a3a795', 'treatment', '4th Payment', '4th Payment', 'histjpe25_43f6075c058285c2', '8918486092', 'Jalpaiguri', 'SHIBANI MANDAL',
  '2025-03-01', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-01T10:18:00.000Z', '2025-03-01T10:18:00.000Z'
),
(
  'histjpe25_pay_656b2c2cf0974db7', 'treatment', '5th Payment', '5th Payment', 'histjpe25_43f6075c058285c2', '8918486092', 'Jalpaiguri', 'SHIBANI MANDAL',
  '2025-03-29', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-29T10:19:00.000Z', '2025-03-29T10:19:00.000Z'
),
(
  'histjpe25_pay_26ff59d08ab194dc', 'treatment', 'Advance', 'Advance', 'histjpe25_5d972f64b9659447', '9434824470', 'Jalpaiguri', 'BHARAT SHING',
  '2025-02-02', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-02T10:15:00.000Z', '2025-02-02T10:15:00.000Z'
),
(
  'histjpe25_pay_af154c3f4534cac5', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_5d972f64b9659447', '9434824470', 'Jalpaiguri', 'BHARAT SHING',
  '2025-02-08', '7000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-08T10:16:00.000Z', '2025-02-08T10:16:00.000Z'
),
(
  'histjpe25_pay_cf84d28d6a1398fb', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_5d972f64b9659447', '9434824470', 'Jalpaiguri', 'BHARAT SHING',
  '2025-02-11', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-11T10:17:00.000Z', '2025-02-11T10:17:00.000Z'
),
(
  'histjpe25_pay_53d5ef1c7338ab58', 'treatment', '4th Payment', '4th Payment', 'histjpe25_5d972f64b9659447', '9434824470', 'Jalpaiguri', 'BHARAT SHING',
  '2025-02-22', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-22T10:18:00.000Z', '2025-02-22T10:18:00.000Z'
),
(
  'histjpe25_pay_8881c8e00e9b295a', 'treatment', '5th Payment', '5th Payment', 'histjpe25_5d972f64b9659447', '9434824470', 'Jalpaiguri', 'BHARAT SHING',
  '2025-03-01', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-01T10:19:00.000Z', '2025-03-01T10:19:00.000Z'
),
(
  'histjpe25_pay_1ef893c6687ac537', 'treatment', 'Advance', 'Advance', 'histjpe25_ad0d0a0a4e780ea8', '9883960266', 'Jalpaiguri', 'RASNA KHATUN',
  '2025-02-04', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-04T10:15:00.000Z', '2025-02-04T10:15:00.000Z'
),
(
  'histjpe25_pay_323163b4891a3c77', 'treatment', 'Advance', 'Advance', 'histjpe25_f9b43756b1af6166', '9547178350', 'Jalpaiguri', 'JAGOBONDHU ROY',
  '2025-03-16', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-16T10:15:00.000Z', '2025-03-16T10:15:00.000Z'
),
(
  'histjpe25_pay_bb2a669b040c6d6a', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_f9b43756b1af6166', '9547178350', 'Jalpaiguri', 'JAGOBONDHU ROY',
  '2025-03-28', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-28T10:16:00.000Z', '2025-03-28T10:16:00.000Z'
),
(
  'histjpe25_pay_c99b4f1c746a1e1d', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_f9b43756b1af6166', '9547178350', 'Jalpaiguri', 'JAGOBONDHU ROY',
  '2025-04-01', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-01T10:17:00.000Z', '2025-04-01T10:17:00.000Z'
),
(
  'histjpe25_pay_6d343a9e87e5b4e6', 'treatment', '4th Payment', '4th Payment', 'histjpe25_f9b43756b1af6166', '9547178350', 'Jalpaiguri', 'JAGOBONDHU ROY',
  '2025-04-10', '800', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-10T10:18:00.000Z', '2025-04-10T10:18:00.000Z'
),
(
  'histjpe25_pay_eec3fff18fb1c13b', 'treatment', '5th Payment', '5th Payment', 'histjpe25_f9b43756b1af6166', '9547178350', 'Jalpaiguri', 'JAGOBONDHU ROY',
  '2025-05-17', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:19:00.000Z', '2025-05-17T10:19:00.000Z'
),
(
  'histjpe25_pay_d48e1d2542f8128c', 'treatment', '6th Payment', '6th Payment', 'histjpe25_f9b43756b1af6166', '9547178350', 'Jalpaiguri', 'JAGOBONDHU ROY',
  '2025-06-07', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-07T10:20:00.000Z', '2025-06-07T10:20:00.000Z'
),
(
  'histjpe25_pay_c659487be1619305', 'treatment', 'Advance', 'Advance', 'histjpe25_e151f66bba6f43ce', '9933709754', 'Jalpaiguri', 'HIMALAYA ROY',
  '2025-02-08', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-08T10:15:00.000Z', '2025-02-08T10:15:00.000Z'
),
(
  'histjpe25_pay_655bd0b6fb490666', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_e151f66bba6f43ce', '9933709754', 'Jalpaiguri', 'HIMALAYA ROY',
  '2025-02-08', '10000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-08T10:16:00.000Z', '2025-02-08T10:16:00.000Z'
),
(
  'histjpe25_pay_a4ea3b6a62aaaafd', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_e151f66bba6f43ce', '9933709754', 'Jalpaiguri', 'HIMALAYA ROY',
  '2025-02-11', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-11T10:17:00.000Z', '2025-02-11T10:17:00.000Z'
),
(
  'histjpe25_pay_31bbdb70f0fd3a64', 'treatment', '4th Payment', '4th Payment', 'histjpe25_e151f66bba6f43ce', '9933709754', 'Jalpaiguri', 'HIMALAYA ROY',
  '2025-02-15', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-15T10:18:00.000Z', '2025-02-15T10:18:00.000Z'
),
(
  'histjpe25_pay_51e60b6631f86e53', 'treatment', '5th Payment', '5th Payment', 'histjpe25_e151f66bba6f43ce', '9933709754', 'Jalpaiguri', 'HIMALAYA ROY',
  '2025-02-22', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-22T10:19:00.000Z', '2025-02-22T10:19:00.000Z'
),
(
  'histjpe25_pay_d2843ecf9b72b367', 'treatment', '6th Payment', '6th Payment', 'histjpe25_e151f66bba6f43ce', '9933709754', 'Jalpaiguri', 'HIMALAYA ROY',
  '2025-03-01', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-01T10:20:00.000Z', '2025-03-01T10:20:00.000Z'
),
(
  'histjpe25_pay_3274f0a09b390d5c', 'treatment', '7th Payment', '7th Payment', 'histjpe25_e151f66bba6f43ce', '9933709754', 'Jalpaiguri', 'HIMALAYA ROY',
  '2025-03-18', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-18T10:21:00.000Z', '2025-03-18T10:21:00.000Z'
),
(
  'histjpe25_pay_f24f4c94e1e53d44', 'treatment', '8th Payment', '8th Payment', 'histjpe25_e151f66bba6f43ce', '9933709754', 'Jalpaiguri', 'HIMALAYA ROY',
  '2025-03-25', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-25T10:22:00.000Z', '2025-03-25T10:22:00.000Z'
),
(
  'histjpe25_pay_bbe0ba4e6da56a16', 'treatment', '9th Payment', '9th Payment', 'histjpe25_e151f66bba6f43ce', '9933709754', 'Jalpaiguri', 'HIMALAYA ROY',
  '2025-05-06', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-06T10:23:00.000Z', '2025-05-06T10:23:00.000Z'
),
(
  'histjpe25_pay_4835f0ccb0339712', 'treatment', 'Advance', 'Advance', 'histjpe25_2187fb2a1cb1b97a', '6295599811', 'Jalpaiguri', 'SUDHANTA SARKAR',
  '2025-02-22', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-22T10:15:00.000Z', '2025-02-22T10:15:00.000Z'
),
(
  'histjpe25_pay_42a60d7b2a7acca6', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_2187fb2a1cb1b97a', '6295599811', 'Jalpaiguri', 'SUDHANTA SARKAR',
  '2025-03-01', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-01T10:16:00.000Z', '2025-03-01T10:16:00.000Z'
),
(
  'histjpe25_pay_046389b1e4c0a2b3', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_2187fb2a1cb1b97a', '6295599811', 'Jalpaiguri', 'SUDHANTA SARKAR',
  '2025-03-08', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-08T10:17:00.000Z', '2025-03-08T10:17:00.000Z'
),
(
  'histjpe25_pay_a7b81d4a0ebec931', 'treatment', '4th Payment', '4th Payment', 'histjpe25_2187fb2a1cb1b97a', '6295599811', 'Jalpaiguri', 'SUDHANTA SARKAR',
  '2025-03-15', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-15T10:18:00.000Z', '2025-03-15T10:18:00.000Z'
),
(
  'histjpe25_pay_da8c8bd654c7f6eb', 'treatment', '5th Payment', '5th Payment', 'histjpe25_2187fb2a1cb1b97a', '6295599811', 'Jalpaiguri', 'SUDHANTA SARKAR',
  '2025-03-22', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-22T10:19:00.000Z', '2025-03-22T10:19:00.000Z'
),
(
  'histjpe25_pay_1d696c1fb5494d76', 'treatment', '6th Payment', '6th Payment', 'histjpe25_2187fb2a1cb1b97a', '6295599811', 'Jalpaiguri', 'SUDHANTA SARKAR',
  '2025-03-29', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-29T10:20:00.000Z', '2025-03-29T10:20:00.000Z'
),
(
  'histjpe25_pay_e97a6aff1c9943ee', 'treatment', 'Advance', 'Advance', 'histjpe25_743a44bb59233ff6', '9800487355', 'Jalpaiguri', 'KALATU ROY',
  '2025-02-22', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-22T10:15:00.000Z', '2025-02-22T10:15:00.000Z'
),
(
  'histjpe25_pay_5069607b2493bd05', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_743a44bb59233ff6', '9800487355', 'Jalpaiguri', 'KALATU ROY',
  '2025-03-25', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-25T10:16:00.000Z', '2025-03-25T10:16:00.000Z'
),
(
  'histjpe25_pay_cd5b5f995fab612d', 'treatment', 'Advance', 'Advance', 'histjpe25_3a07c9cc9537cf4c', '8637899217', 'Jalpaiguri', 'RANJIT ROY',
  '2025-03-01', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-01T10:15:00.000Z', '2025-03-01T10:15:00.000Z'
),
(
  'histjpe25_pay_6f6f82dfaa757711', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_3a07c9cc9537cf4c', '8637899217', 'Jalpaiguri', 'RANJIT ROY',
  '2025-03-04', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-04T10:16:00.000Z', '2025-03-04T10:16:00.000Z'
),
(
  'histjpe25_pay_418d3311405791c1', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_3a07c9cc9537cf4c', '8637899217', 'Jalpaiguri', 'RANJIT ROY',
  '2025-03-08', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-08T10:17:00.000Z', '2025-03-08T10:17:00.000Z'
),
(
  'histjpe25_pay_f53c4d47dcdc1426', 'treatment', '4th Payment', '4th Payment', 'histjpe25_3a07c9cc9537cf4c', '8637899217', 'Jalpaiguri', 'RANJIT ROY',
  '2025-03-11', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-11T10:18:00.000Z', '2025-03-11T10:18:00.000Z'
),
(
  'histjpe25_pay_27f7295d6fee3b50', 'treatment', '5th Payment', '5th Payment', 'histjpe25_3a07c9cc9537cf4c', '8637899217', 'Jalpaiguri', 'RANJIT ROY',
  '2025-03-18', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-18T10:19:00.000Z', '2025-03-18T10:19:00.000Z'
),
(
  'histjpe25_pay_f45bd9b0fb54287b', 'treatment', '6th Payment', '6th Payment', 'histjpe25_3a07c9cc9537cf4c', '8637899217', 'Jalpaiguri', 'RANJIT ROY',
  '2025-03-22', '9500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-22T10:20:00.000Z', '2025-03-22T10:20:00.000Z'
),
(
  'histjpe25_pay_a7bddf0aea32a518', 'treatment', 'Advance', 'Advance', 'histjpe25_aeda5572fcc34a4e', '7001806924', 'Jalpaiguri', 'BIRENDRANATH ROY',
  '2025-03-08', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-08T10:15:00.000Z', '2025-03-08T10:15:00.000Z'
),
(
  'histjpe25_pay_383577d9fb18e05c', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_aeda5572fcc34a4e', '7001806924', 'Jalpaiguri', 'BIRENDRANATH ROY',
  '2025-03-11', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-11T10:16:00.000Z', '2025-03-11T10:16:00.000Z'
),
(
  'histjpe25_pay_f01f0f83f6d1dc44', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_aeda5572fcc34a4e', '7001806924', 'Jalpaiguri', 'BIRENDRANATH ROY',
  '2025-03-22', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-22T10:17:00.000Z', '2025-03-22T10:17:00.000Z'
),
(
  'histjpe25_pay_8b5a5b10e38cc179', 'treatment', '4th Payment', '4th Payment', 'histjpe25_aeda5572fcc34a4e', '7001806924', 'Jalpaiguri', 'BIRENDRANATH ROY',
  '2025-04-01', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-01T10:18:00.000Z', '2025-04-01T10:18:00.000Z'
),
(
  'histjpe25_pay_dea916a7789d15da', 'treatment', '5th Payment', '5th Payment', 'histjpe25_aeda5572fcc34a4e', '7001806924', 'Jalpaiguri', 'BIRENDRANATH ROY',
  '2025-03-29', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-29T10:19:00.000Z', '2025-03-29T10:19:00.000Z'
),
(
  'histjpe25_pay_2a255c7affc7862a', 'treatment', '6th Payment', '6th Payment', 'histjpe25_aeda5572fcc34a4e', '7001806924', 'Jalpaiguri', 'BIRENDRANATH ROY',
  '2025-04-05', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-05T10:20:00.000Z', '2025-04-05T10:20:00.000Z'
),
(
  'histjpe25_pay_2749c0769667eebb', 'treatment', '7th Payment', '7th Payment', 'histjpe25_aeda5572fcc34a4e', '7001806924', 'Jalpaiguri', 'BIRENDRANATH ROY',
  '2025-04-08', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-08T10:21:00.000Z', '2025-04-08T10:21:00.000Z'
),
(
  'histjpe25_pay_5e074b8a17a69eba', 'treatment', '8th Payment', '8th Payment', 'histjpe25_aeda5572fcc34a4e', '7001806924', 'Jalpaiguri', 'BIRENDRANATH ROY',
  '2025-04-12', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-12T10:22:00.000Z', '2025-04-12T10:22:00.000Z'
),
(
  'histjpe25_pay_7a0160b5a954e277', 'treatment', '9th Payment', '9th Payment', 'histjpe25_aeda5572fcc34a4e', '7001806924', 'Jalpaiguri', 'BIRENDRANATH ROY',
  '2025-04-19', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-19T10:23:00.000Z', '2025-04-19T10:23:00.000Z'
),
(
  'histjpe25_pay_56f476ea15ad7a08', 'treatment', '10th Payment', '10th Payment', 'histjpe25_aeda5572fcc34a4e', '7001806924', 'Jalpaiguri', 'BIRENDRANATH ROY',
  '2025-04-22', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-22T10:24:00.000Z', '2025-04-22T10:24:00.000Z'
),
(
  'histjpe25_pay_73a92f4531c23039', 'treatment', '11th Payment', '11th Payment', 'histjpe25_aeda5572fcc34a4e', '7001806924', 'Jalpaiguri', 'BIRENDRANATH ROY',
  '2025-04-26', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-26T10:25:00.000Z', '2025-04-26T10:25:00.000Z'
),
(
  'histjpe25_pay_a2b1d721799faa90', 'treatment', '12th Payment', '12th Payment', 'histjpe25_aeda5572fcc34a4e', '7001806924', 'Jalpaiguri', 'BIRENDRANATH ROY',
  '2025-04-29', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-29T10:26:00.000Z', '2025-04-29T10:26:00.000Z'
),
(
  'histjpe25_pay_a440f69c4945cbc8', 'treatment', '13th Payment', '13th Payment', 'histjpe25_aeda5572fcc34a4e', '7001806924', 'Jalpaiguri', 'BIRENDRANATH ROY',
  '2025-04-06', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-06T10:27:00.000Z', '2025-04-06T10:27:00.000Z'
),
(
  'histjpe25_pay_ed97879c34c5fcbb', 'treatment', '14th Payment', '14th Payment', 'histjpe25_aeda5572fcc34a4e', '7001806924', 'Jalpaiguri', 'BIRENDRANATH ROY',
  '2025-05-17', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:28:00.000Z', '2025-05-17T10:28:00.000Z'
),
(
  'histjpe25_pay_2390faaa1ffed89e', 'treatment', 'Advance', 'Advance', 'histjpe25_84ee510823ebd271', '8972261311', 'Jalpaiguri', 'DALIYA BARMAN',
  '2025-03-16', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-16T10:15:00.000Z', '2025-03-16T10:15:00.000Z'
),
(
  'histjpe25_pay_8dc1aed47a4ae71c', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_84ee510823ebd271', '8972261311', 'Jalpaiguri', 'DALIYA BARMAN',
  '2025-03-25', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-25T10:16:00.000Z', '2025-03-25T10:16:00.000Z'
),
(
  'histjpe25_pay_ac39285f87ca84c1', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_84ee510823ebd271', '8972261311', 'Jalpaiguri', 'DALIYA BARMAN',
  '2025-04-01', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-01T10:17:00.000Z', '2025-04-01T10:17:00.000Z'
),
(
  'histjpe25_pay_a00c1a4d80d621df', 'treatment', '4th Payment', '4th Payment', 'histjpe25_84ee510823ebd271', '8972261311', 'Jalpaiguri', 'DALIYA BARMAN',
  '2025-04-05', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-05T10:18:00.000Z', '2025-04-05T10:18:00.000Z'
),
(
  'histjpe25_pay_de18b85c4243a90c', 'treatment', '5th Payment', '5th Payment', 'histjpe25_84ee510823ebd271', '8972261311', 'Jalpaiguri', 'DALIYA BARMAN',
  '2025-04-12', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-12T10:19:00.000Z', '2025-04-12T10:19:00.000Z'
),
(
  'histjpe25_pay_8496d2d54595e6bd', 'treatment', '6th Payment', '6th Payment', 'histjpe25_84ee510823ebd271', '8972261311', 'Jalpaiguri', 'DALIYA BARMAN',
  '2025-04-29', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-29T10:20:00.000Z', '2025-04-29T10:20:00.000Z'
),
(
  'histjpe25_pay_978fa305c7147d9e', 'treatment', '7th Payment', '7th Payment', 'histjpe25_84ee510823ebd271', '8972261311', 'Jalpaiguri', 'DALIYA BARMAN',
  '2025-05-03', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-03T10:21:00.000Z', '2025-05-03T10:21:00.000Z'
),
(
  'histjpe25_pay_877e72c47c510f53', 'treatment', '8th Payment', '8th Payment', 'histjpe25_84ee510823ebd271', '8972261311', 'Jalpaiguri', 'DALIYA BARMAN',
  '2025-05-10', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:22:00.000Z', '2025-05-10T10:22:00.000Z'
),
(
  'histjpe25_pay_cacd33cd1874ff06', 'treatment', '9th Payment', '9th Payment', 'histjpe25_84ee510823ebd271', '8972261311', 'Jalpaiguri', 'DALIYA BARMAN',
  '2025-05-17', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:23:00.000Z', '2025-05-17T10:23:00.000Z'
),
(
  'histjpe25_pay_3a7c1ead4284c636', 'treatment', '10th Payment', '10th Payment', 'histjpe25_84ee510823ebd271', '8972261311', 'Jalpaiguri', 'DALIYA BARMAN',
  '2025-05-27', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-27T10:24:00.000Z', '2025-05-27T10:24:00.000Z'
),
(
  'histjpe25_pay_0168a32e5fbe6e83', 'treatment', '11th Payment', '11th Payment', 'histjpe25_84ee510823ebd271', '8972261311', 'Jalpaiguri', 'DALIYA BARMAN',
  '2025-05-07', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-07T10:25:00.000Z', '2025-05-07T10:25:00.000Z'
),
(
  'histjpe25_pay_d52e79e5c322de92', 'treatment', '12th Payment', '12th Payment', 'histjpe25_84ee510823ebd271', '8972261311', 'Jalpaiguri', 'DALIYA BARMAN',
  '2025-06-17', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-17T10:26:00.000Z', '2025-06-17T10:26:00.000Z'
),
(
  'histjpe25_pay_398ce5b9c66ad5bd', 'treatment', '13th Payment', '13th Payment', 'histjpe25_84ee510823ebd271', '8972261311', 'Jalpaiguri', 'DALIYA BARMAN',
  '2025-06-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:27:00.000Z', '2025-06-21T10:27:00.000Z'
),
(
  'histjpe25_pay_8abe602c36916837', 'treatment', '14th Payment', '14th Payment', 'histjpe25_84ee510823ebd271', '8972261311', 'Jalpaiguri', 'DALIYA BARMAN',
  '2025-07-12', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-12T10:28:00.000Z', '2025-07-12T10:28:00.000Z'
),
(
  'histjpe25_pay_d3c2633da7078fb4', 'treatment', '15th Payment', '15th Payment', 'histjpe25_84ee510823ebd271', '8972261311', 'Jalpaiguri', 'DALIYA BARMAN',
  '2025-07-19', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-19T10:29:00.000Z', '2025-07-19T10:29:00.000Z'
),
(
  'histjpe25_pay_d854b4663b5b254d', 'treatment', 'Advance', 'Advance', 'histjpe25_4dacfbc0505e16f7', '8927539671', 'Jalpaiguri', 'JAHANARA BEGAM',
  '2025-03-04', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-04T10:15:00.000Z', '2025-03-04T10:15:00.000Z'
),
(
  'histjpe25_pay_4b4eb6f1c98bba31', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_4dacfbc0505e16f7', '8927539671', 'Jalpaiguri', 'JAHANARA BEGAM',
  '2025-03-09', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-09T10:16:00.000Z', '2025-03-09T10:16:00.000Z'
),
(
  'histjpe25_pay_be043651418668f9', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_4dacfbc0505e16f7', '8927539671', 'Jalpaiguri', 'JAHANARA BEGAM',
  '2025-03-16', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-16T10:17:00.000Z', '2025-03-16T10:17:00.000Z'
),
(
  'histjpe25_pay_498e83e9c5eaa71c', 'treatment', 'Advance', 'Advance', 'histjpe25_48286f7c735f5706', '7029582842', 'Jalpaiguri', 'ABBU BAKKAR SIDDIK',
  '2025-03-08', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-08T10:15:00.000Z', '2025-03-08T10:15:00.000Z'
),
(
  'histjpe25_pay_9e7149fe2c705b2c', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_48286f7c735f5706', '7029582842', 'Jalpaiguri', 'ABBU BAKKAR SIDDIK',
  '2025-03-15', '4500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-15T10:16:00.000Z', '2025-03-15T10:16:00.000Z'
),
(
  'histjpe25_pay_658a0ae2a90559e9', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_48286f7c735f5706', '7029582842', 'Jalpaiguri', 'ABBU BAKKAR SIDDIK',
  '2025-03-18', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-18T10:17:00.000Z', '2025-03-18T10:17:00.000Z'
),
(
  'histjpe25_pay_e8ed008225ecd069', 'treatment', '4th Payment', '4th Payment', 'histjpe25_48286f7c735f5706', '7029582842', 'Jalpaiguri', 'ABBU BAKKAR SIDDIK',
  '2025-03-22', '2500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-22T10:18:00.000Z', '2025-03-22T10:18:00.000Z'
),
(
  'histjpe25_pay_a03a9d35a9788afd', 'treatment', '5th Payment', '5th Payment', 'histjpe25_48286f7c735f5706', '7029582842', 'Jalpaiguri', 'ABBU BAKKAR SIDDIK',
  '2025-03-29', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-29T10:19:00.000Z', '2025-03-29T10:19:00.000Z'
),
(
  'histjpe25_pay_dcc48256e97664f4', 'treatment', 'Advance', 'Advance', 'histjpe25_878beb3953207bda', '9365670832', 'Jalpaiguri', 'DIPALI ROY',
  '2025-03-08', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-08T10:15:00.000Z', '2025-03-08T10:15:00.000Z'
),
(
  'histjpe25_pay_55416a6533030df7', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_878beb3953207bda', '9365670832', 'Jalpaiguri', 'DIPALI ROY',
  '2025-03-09', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-09T10:16:00.000Z', '2025-03-09T10:16:00.000Z'
),
(
  'histjpe25_pay_d3e1294d89d61a16', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_878beb3953207bda', '9365670832', 'Jalpaiguri', 'DIPALI ROY',
  '2025-03-18', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-18T10:17:00.000Z', '2025-03-18T10:17:00.000Z'
),
(
  'histjpe25_pay_a8e1a6f03be0d76c', 'treatment', 'Advance', 'Advance', 'histjpe25_9881a84ab0e580bb', '7718156997', 'Jalpaiguri', 'MONTOSH MODAK',
  '2025-03-22', '8000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-22T10:15:00.000Z', '2025-03-22T10:15:00.000Z'
),
(
  'histjpe25_pay_2b3de8868fdff30e', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_9881a84ab0e580bb', '7718156997', 'Jalpaiguri', 'MONTOSH MODAK',
  '2025-04-08', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-08T10:16:00.000Z', '2025-04-08T10:16:00.000Z'
),
(
  'histjpe25_pay_6733cb212f5d8454', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_9881a84ab0e580bb', '7718156997', 'Jalpaiguri', 'MONTOSH MODAK',
  '2025-04-12', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-12T10:17:00.000Z', '2025-04-12T10:17:00.000Z'
),
(
  'histjpe25_pay_ab7af082577b9b20', 'treatment', '4th Payment', '4th Payment', 'histjpe25_9881a84ab0e580bb', '7718156997', 'Jalpaiguri', 'MONTOSH MODAK',
  '2025-04-26', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-26T10:18:00.000Z', '2025-04-26T10:18:00.000Z'
),
(
  'histjpe25_pay_cb4998d0953e2dba', 'treatment', '5th Payment', '5th Payment', 'histjpe25_9881a84ab0e580bb', '7718156997', 'Jalpaiguri', 'MONTOSH MODAK',
  '2025-05-17', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:19:00.000Z', '2025-05-17T10:19:00.000Z'
),
(
  'histjpe25_pay_f7a04ec9c767e7b2', 'treatment', '6th Payment', '6th Payment', 'histjpe25_9881a84ab0e580bb', '7718156997', 'Jalpaiguri', 'MONTOSH MODAK',
  '2025-05-24', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-24T10:20:00.000Z', '2025-05-24T10:20:00.000Z'
),
(
  'histjpe25_pay_e8c48993882b5d33', 'treatment', 'Advance', 'Advance', 'histjpe25_8dc12c9d6f0c964e', '9832304557', 'Jalpaiguri', 'MRINMOY ROY',
  '2025-03-18', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-18T10:15:00.000Z', '2025-03-18T10:15:00.000Z'
),
(
  'histjpe25_pay_148f5a88b3bb3d2c', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_8dc12c9d6f0c964e', '9832304557', 'Jalpaiguri', 'MRINMOY ROY',
  '2025-03-22', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-22T10:16:00.000Z', '2025-03-22T10:16:00.000Z'
),
(
  'histjpe25_pay_3318900fe1c27747', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_8dc12c9d6f0c964e', '9832304557', 'Jalpaiguri', 'MRINMOY ROY',
  '2025-03-25', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-25T10:17:00.000Z', '2025-03-25T10:17:00.000Z'
),
(
  'histjpe25_pay_80f5891271ffc617', 'treatment', '4th Payment', '4th Payment', 'histjpe25_8dc12c9d6f0c964e', '9832304557', 'Jalpaiguri', 'MRINMOY ROY',
  '2025-03-29', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-29T10:18:00.000Z', '2025-03-29T10:18:00.000Z'
),
(
  'histjpe25_pay_c83f5b64a56f1063', 'treatment', '5th Payment', '5th Payment', 'histjpe25_8dc12c9d6f0c964e', '9832304557', 'Jalpaiguri', 'MRINMOY ROY',
  '2025-04-01', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-01T10:19:00.000Z', '2025-04-01T10:19:00.000Z'
),
(
  'histjpe25_pay_1e2015502f691fbf', 'treatment', '6th Payment', '6th Payment', 'histjpe25_8dc12c9d6f0c964e', '9832304557', 'Jalpaiguri', 'MRINMOY ROY',
  '2025-04-05', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-05T10:20:00.000Z', '2025-04-05T10:20:00.000Z'
),
(
  'histjpe25_pay_53d1c6e4f5b8b153', 'treatment', '7th Payment', '7th Payment', 'histjpe25_8dc12c9d6f0c964e', '9832304557', 'Jalpaiguri', 'MRINMOY ROY',
  '2025-04-12', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-12T10:21:00.000Z', '2025-04-12T10:21:00.000Z'
),
(
  'histjpe25_pay_f4b28f2d8de88bdb', 'treatment', '8th Payment', '8th Payment', 'histjpe25_8dc12c9d6f0c964e', '9832304557', 'Jalpaiguri', 'MRINMOY ROY',
  '2025-04-26', '2500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-26T10:22:00.000Z', '2025-04-26T10:22:00.000Z'
),
(
  'histjpe25_pay_e931143ff59394f7', 'treatment', '9th Payment', '9th Payment', 'histjpe25_8dc12c9d6f0c964e', '9832304557', 'Jalpaiguri', 'MRINMOY ROY',
  '2025-05-03', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-03T10:23:00.000Z', '2025-05-03T10:23:00.000Z'
),
(
  'histjpe25_pay_a79505a01984e35a', 'treatment', 'Advance', 'Advance', 'histjpe25_fd940e9be5f276ec', '9641126416', 'Jalpaiguri', 'GOPAL BISWAS',
  '2025-03-29', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-29T10:15:00.000Z', '2025-03-29T10:15:00.000Z'
),
(
  'histjpe25_pay_f4c9665681320ac1', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_fd940e9be5f276ec', '9641126416', 'Jalpaiguri', 'GOPAL BISWAS',
  '2025-04-01', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-01T10:16:00.000Z', '2025-04-01T10:16:00.000Z'
),
(
  'histjpe25_pay_e832f9838c25c311', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_fd940e9be5f276ec', '9641126416', 'Jalpaiguri', 'GOPAL BISWAS',
  '2025-04-12', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-12T10:17:00.000Z', '2025-04-12T10:17:00.000Z'
),
(
  'histjpe25_pay_c5efa9b702c2a98f', 'treatment', '4th Payment', '4th Payment', 'histjpe25_fd940e9be5f276ec', '9641126416', 'Jalpaiguri', 'GOPAL BISWAS',
  '2025-04-03', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-03T10:18:00.000Z', '2025-04-03T10:18:00.000Z'
),
(
  'histjpe25_pay_d42c9d528e69d11d', 'treatment', '5th Payment', '5th Payment', 'histjpe25_fd940e9be5f276ec', '9641126416', 'Jalpaiguri', 'GOPAL BISWAS',
  '2025-05-03', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-03T10:19:00.000Z', '2025-05-03T10:19:00.000Z'
),
(
  'histjpe25_pay_5682a58b5e53dd25', 'treatment', 'Advance', 'Advance', 'histjpe25_b1a69617aeffd9b9', '9547196494', 'Jalpaiguri', 'BISWAS RAM BHAGAT',
  '2025-03-29', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-29T10:15:00.000Z', '2025-03-29T10:15:00.000Z'
),
(
  'histjpe25_pay_177c9dda48230853', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_b1a69617aeffd9b9', '9547196494', 'Jalpaiguri', 'BISWAS RAM BHAGAT',
  '2025-04-01', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-01T10:16:00.000Z', '2025-04-01T10:16:00.000Z'
),
(
  'histjpe25_pay_93176b63b752e0cd', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_b1a69617aeffd9b9', '9547196494', 'Jalpaiguri', 'BISWAS RAM BHAGAT',
  '2025-04-12', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-12T10:17:00.000Z', '2025-04-12T10:17:00.000Z'
),
(
  'histjpe25_pay_055ce8e7202e8e6e', 'treatment', '4th Payment', '4th Payment', 'histjpe25_b1a69617aeffd9b9', '9547196494', 'Jalpaiguri', 'BISWAS RAM BHAGAT',
  '2025-04-19', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-19T10:18:00.000Z', '2025-04-19T10:18:00.000Z'
),
(
  'histjpe25_pay_c464dc970f6d88fb', 'treatment', '5th Payment', '5th Payment', 'histjpe25_b1a69617aeffd9b9', '9547196494', 'Jalpaiguri', 'BISWAS RAM BHAGAT',
  '2025-04-26', '5500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-26T10:19:00.000Z', '2025-04-26T10:19:00.000Z'
),
(
  'histjpe25_pay_121ffe1faef975b9', 'treatment', '6th Payment', '6th Payment', 'histjpe25_b1a69617aeffd9b9', '9547196494', 'Jalpaiguri', 'BISWAS RAM BHAGAT',
  '2025-05-03', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-03T10:20:00.000Z', '2025-05-03T10:20:00.000Z'
),
(
  'histjpe25_pay_a9baf48a52299580', 'treatment', '7th Payment', '7th Payment', 'histjpe25_b1a69617aeffd9b9', '9547196494', 'Jalpaiguri', 'BISWAS RAM BHAGAT',
  '2025-05-10', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:21:00.000Z', '2025-05-10T10:21:00.000Z'
),
(
  'histjpe25_pay_ecd127f679f8a40b', 'treatment', '8th Payment', '8th Payment', 'histjpe25_b1a69617aeffd9b9', '9547196494', 'Jalpaiguri', 'BISWAS RAM BHAGAT',
  '2025-05-17', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:22:00.000Z', '2025-05-17T10:22:00.000Z'
),
(
  'histjpe25_pay_75470fcd36d48365', 'treatment', '9th Payment', '9th Payment', 'histjpe25_b1a69617aeffd9b9', '9547196494', 'Jalpaiguri', 'BISWAS RAM BHAGAT',
  '2025-04-05', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-05T10:23:00.000Z', '2025-04-05T10:23:00.000Z'
),
(
  'histjpe25_pay_b8f4153bc506cf47', 'treatment', 'Advance', 'Advance', 'histjpe25_4dbe3e778b894893', '7063528848', 'Jalpaiguri', 'RAHUL ROY',
  '2025-04-05', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-05T10:15:00.000Z', '2025-04-05T10:15:00.000Z'
),
(
  'histjpe25_pay_5292e3cf9cc78df0', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_4dbe3e778b894893', '7063528848', 'Jalpaiguri', 'RAHUL ROY',
  '2025-04-08', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-08T10:16:00.000Z', '2025-04-08T10:16:00.000Z'
),
(
  'histjpe25_pay_51dd71bc5387fc4b', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_4dbe3e778b894893', '7063528848', 'Jalpaiguri', 'RAHUL ROY',
  '2025-04-12', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-12T10:17:00.000Z', '2025-04-12T10:17:00.000Z'
),
(
  'histjpe25_pay_dfea29f0c3343639', 'treatment', '4th Payment', '4th Payment', 'histjpe25_4dbe3e778b894893', '7063528848', 'Jalpaiguri', 'RAHUL ROY',
  '2025-04-19', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-19T10:18:00.000Z', '2025-04-19T10:18:00.000Z'
),
(
  'histjpe25_pay_f1f235dfca535659', 'treatment', '5th Payment', '5th Payment', 'histjpe25_4dbe3e778b894893', '7063528848', 'Jalpaiguri', 'RAHUL ROY',
  '2025-04-22', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-22T10:19:00.000Z', '2025-04-22T10:19:00.000Z'
),
(
  'histjpe25_pay_4696b38a181e263c', 'treatment', '6th Payment', '6th Payment', 'histjpe25_4dbe3e778b894893', '7063528848', 'Jalpaiguri', 'RAHUL ROY',
  '2025-04-26', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-26T10:20:00.000Z', '2025-04-26T10:20:00.000Z'
),
(
  'histjpe25_pay_8bb3d3627eca8f16', 'treatment', '7th Payment', '7th Payment', 'histjpe25_4dbe3e778b894893', '7063528848', 'Jalpaiguri', 'RAHUL ROY',
  '2025-05-03', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-03T10:21:00.000Z', '2025-05-03T10:21:00.000Z'
),
(
  'histjpe25_pay_152ed7717939056d', 'treatment', '8th Payment', '8th Payment', 'histjpe25_4dbe3e778b894893', '7063528848', 'Jalpaiguri', 'RAHUL ROY',
  '2025-05-24', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-24T10:22:00.000Z', '2025-05-24T10:22:00.000Z'
),
(
  'histjpe25_pay_68f806a5a2e2c55d', 'treatment', 'Advance', 'Advance', 'histjpe25_da68e1bd12d369c1', '8617378476', 'Jalpaiguri', 'MUNNA KUMAR',
  '2025-03-29', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-03-29T10:15:00.000Z', '2025-03-29T10:15:00.000Z'
),
(
  'histjpe25_pay_29f3059a73d383d2', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_da68e1bd12d369c1', '8617378476', 'Jalpaiguri', 'MUNNA KUMAR',
  '2025-04-05', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-05T10:16:00.000Z', '2025-04-05T10:16:00.000Z'
),
(
  'histjpe25_pay_697a157af438dedd', 'treatment', 'Advance', 'Advance', 'histjpe25_5fba273d50acf335', '7719171801', 'Jalpaiguri', 'PRADIP ROY',
  '2025-04-05', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-05T10:15:00.000Z', '2025-04-05T10:15:00.000Z'
),
(
  'histjpe25_pay_5c6ed9e47f7187b0', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_5fba273d50acf335', '7719171801', 'Jalpaiguri', 'PRADIP ROY',
  '2025-04-08', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-08T10:16:00.000Z', '2025-04-08T10:16:00.000Z'
),
(
  'histjpe25_pay_f7ca684653a2dd0e', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_5fba273d50acf335', '7719171801', 'Jalpaiguri', 'PRADIP ROY',
  '2025-04-12', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-12T10:17:00.000Z', '2025-04-12T10:17:00.000Z'
),
(
  'histjpe25_pay_0c89a8f62d115170', 'treatment', '4th Payment', '4th Payment', 'histjpe25_5fba273d50acf335', '7719171801', 'Jalpaiguri', 'PRADIP ROY',
  '2025-04-19', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-19T10:18:00.000Z', '2025-04-19T10:18:00.000Z'
),
(
  'histjpe25_pay_13ede2f1fb948152', 'treatment', '5th Payment', '5th Payment', 'histjpe25_5fba273d50acf335', '7719171801', 'Jalpaiguri', 'PRADIP ROY',
  '2025-04-22', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-22T10:19:00.000Z', '2025-04-22T10:19:00.000Z'
),
(
  'histjpe25_pay_4fb63e6339039145', 'treatment', '6th Payment', '6th Payment', 'histjpe25_5fba273d50acf335', '7719171801', 'Jalpaiguri', 'PRADIP ROY',
  '2025-04-26', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-26T10:20:00.000Z', '2025-04-26T10:20:00.000Z'
),
(
  'histjpe25_pay_03c83cd0f9f25610', 'treatment', '7th Payment', '7th Payment', 'histjpe25_5fba273d50acf335', '7719171801', 'Jalpaiguri', 'PRADIP ROY',
  '2025-04-29', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-29T10:21:00.000Z', '2025-04-29T10:21:00.000Z'
),
(
  'histjpe25_pay_1aaa0da6c6bfa4fc', 'treatment', '8th Payment', '8th Payment', 'histjpe25_5fba273d50acf335', '7719171801', 'Jalpaiguri', 'PRADIP ROY',
  '2025-05-06', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-06T10:22:00.000Z', '2025-05-06T10:22:00.000Z'
),
(
  'histjpe25_pay_1c22157ed0995f6f', 'treatment', '9th Payment', '9th Payment', 'histjpe25_5fba273d50acf335', '7719171801', 'Jalpaiguri', 'PRADIP ROY',
  '2025-05-10', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:23:00.000Z', '2025-05-10T10:23:00.000Z'
),
(
  'histjpe25_pay_55add0539b144640', 'treatment', '10th Payment', '10th Payment', 'histjpe25_5fba273d50acf335', '7719171801', 'Jalpaiguri', 'PRADIP ROY',
  '2025-05-20', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-20T10:24:00.000Z', '2025-05-20T10:24:00.000Z'
),
(
  'histjpe25_pay_1e8ec15c1064ec14', 'treatment', 'Advance', 'Advance', 'histjpe25_9eab93e421a0d6ca', '6295445862', 'Jalpaiguri', 'SARASWATI BASAK',
  '2025-04-06', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-06T10:15:00.000Z', '2025-04-06T10:15:00.000Z'
),
(
  'histjpe25_pay_cd5b5f712a1b8d98', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_9eab93e421a0d6ca', '6295445862', 'Jalpaiguri', 'SARASWATI BASAK',
  '2025-04-06', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-06T10:16:00.000Z', '2025-04-06T10:16:00.000Z'
),
(
  'histjpe25_pay_74b205f3478ce1ca', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_9eab93e421a0d6ca', '6295445862', 'Jalpaiguri', 'SARASWATI BASAK',
  '2025-04-13', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-13T10:17:00.000Z', '2025-04-13T10:17:00.000Z'
),
(
  'histjpe25_pay_62723648683d3d4f', 'treatment', '4th Payment', '4th Payment', 'histjpe25_9eab93e421a0d6ca', '6295445862', 'Jalpaiguri', 'SARASWATI BASAK',
  '2025-04-21', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-21T10:18:00.000Z', '2025-04-21T10:18:00.000Z'
),
(
  'histjpe25_pay_9cf1d856e8afc19e', 'treatment', '5th Payment', '5th Payment', 'histjpe25_9eab93e421a0d6ca', '6295445862', 'Jalpaiguri', 'SARASWATI BASAK',
  '2025-05-03', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-03T10:19:00.000Z', '2025-05-03T10:19:00.000Z'
),
(
  'histjpe25_pay_d5c09f0b25880558', 'treatment', '6th Payment', '6th Payment', 'histjpe25_9eab93e421a0d6ca', '6295445862', 'Jalpaiguri', 'SARASWATI BASAK',
  '2025-05-13', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-13T10:20:00.000Z', '2025-05-13T10:20:00.000Z'
),
(
  'histjpe25_pay_4fa5a4026fe0a11b', 'treatment', '7th Payment', '7th Payment', 'histjpe25_9eab93e421a0d6ca', '6295445862', 'Jalpaiguri', 'SARASWATI BASAK',
  '2025-05-20', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-20T10:21:00.000Z', '2025-05-20T10:21:00.000Z'
),
(
  'histjpe25_pay_53137d17f4302ea7', 'treatment', 'Advance', 'Advance', 'histjpe25_ffdd061a84992df8', '7909197887', 'Jalpaiguri', 'ABU BAKKAR SIDDIK',
  '2025-04-08', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-08T10:15:00.000Z', '2025-04-08T10:15:00.000Z'
),
(
  'histjpe25_pay_bc689fe5925d5ec9', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_ffdd061a84992df8', '7909197887', 'Jalpaiguri', 'ABU BAKKAR SIDDIK',
  '2025-04-26', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-26T10:16:00.000Z', '2025-04-26T10:16:00.000Z'
),
(
  'histjpe25_pay_dbab614286a048cf', 'treatment', 'Advance', 'Advance', 'histjpe25_ab321cb877151c91', '8145773324', 'Jalpaiguri', 'RIVA ROY',
  '2025-04-19', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-19T10:15:00.000Z', '2025-04-19T10:15:00.000Z'
),
(
  'histjpe25_pay_34814b86b1d693f8', 'treatment', 'Advance', 'Advance', 'histjpe25_acd84078c03817ca', '8101954623', 'Jalpaiguri', 'ABHIJIT BISWAS',
  '2025-04-19', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-19T10:15:00.000Z', '2025-04-19T10:15:00.000Z'
),
(
  'histjpe25_pay_9f15b11859577310', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_acd84078c03817ca', '8101954623', 'Jalpaiguri', 'ABHIJIT BISWAS',
  '2025-04-22', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-22T10:16:00.000Z', '2025-04-22T10:16:00.000Z'
),
(
  'histjpe25_pay_b0c8becf9190718f', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_acd84078c03817ca', '8101954623', 'Jalpaiguri', 'ABHIJIT BISWAS',
  '2025-04-26', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-26T10:17:00.000Z', '2025-04-26T10:17:00.000Z'
),
(
  'histjpe25_pay_812b7870987ac574', 'treatment', '4th Payment', '4th Payment', 'histjpe25_acd84078c03817ca', '8101954623', 'Jalpaiguri', 'ABHIJIT BISWAS',
  '2025-04-29', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-29T10:18:00.000Z', '2025-04-29T10:18:00.000Z'
),
(
  'histjpe25_pay_ac4a5ba1136802e8', 'treatment', '5th Payment', '5th Payment', 'histjpe25_acd84078c03817ca', '8101954623', 'Jalpaiguri', 'ABHIJIT BISWAS',
  '2025-05-03', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-03T10:19:00.000Z', '2025-05-03T10:19:00.000Z'
),
(
  'histjpe25_pay_b0e2c2b6d5a5016e', 'treatment', '6th Payment', '6th Payment', 'histjpe25_acd84078c03817ca', '8101954623', 'Jalpaiguri', 'ABHIJIT BISWAS',
  '2025-05-06', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-06T10:20:00.000Z', '2025-05-06T10:20:00.000Z'
),
(
  'histjpe25_pay_b1c804f193684bae', 'treatment', '7th Payment', '7th Payment', 'histjpe25_acd84078c03817ca', '8101954623', 'Jalpaiguri', 'ABHIJIT BISWAS',
  '2025-05-10', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:21:00.000Z', '2025-05-10T10:21:00.000Z'
),
(
  'histjpe25_pay_85a09b99b989dfb0', 'treatment', '8th Payment', '8th Payment', 'histjpe25_acd84078c03817ca', '8101954623', 'Jalpaiguri', 'ABHIJIT BISWAS',
  '2025-05-13', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-13T10:22:00.000Z', '2025-05-13T10:22:00.000Z'
),
(
  'histjpe25_pay_2439360cec13ae9c', 'treatment', 'Advance', 'Advance', 'histjpe25_48aa1a0086e8258f', '9563175056', 'Jalpaiguri', 'MANOTOSH ROY',
  '2025-04-22', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-22T10:15:00.000Z', '2025-04-22T10:15:00.000Z'
),
(
  'histjpe25_pay_df3099a053e8ca4d', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_48aa1a0086e8258f', '9563175056', 'Jalpaiguri', 'MANOTOSH ROY',
  '2025-04-26', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-26T10:16:00.000Z', '2025-04-26T10:16:00.000Z'
),
(
  'histjpe25_pay_535aaa3a83d94904', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_48aa1a0086e8258f', '9563175056', 'Jalpaiguri', 'MANOTOSH ROY',
  '2025-05-03', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-03T10:17:00.000Z', '2025-05-03T10:17:00.000Z'
),
(
  'histjpe25_pay_9128d3ab468bb79a', 'treatment', '4th Payment', '4th Payment', 'histjpe25_48aa1a0086e8258f', '9563175056', 'Jalpaiguri', 'MANOTOSH ROY',
  '2025-05-06', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-06T10:18:00.000Z', '2025-05-06T10:18:00.000Z'
),
(
  'histjpe25_pay_b127ec2680ee520c', 'treatment', '5th Payment', '5th Payment', 'histjpe25_48aa1a0086e8258f', '9563175056', 'Jalpaiguri', 'MANOTOSH ROY',
  '2025-05-10', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:19:00.000Z', '2025-05-10T10:19:00.000Z'
),
(
  'histjpe25_pay_b3248f6488592904', 'treatment', '6th Payment', '6th Payment', 'histjpe25_48aa1a0086e8258f', '9563175056', 'Jalpaiguri', 'MANOTOSH ROY',
  '2025-05-13', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-13T10:20:00.000Z', '2025-05-13T10:20:00.000Z'
),
(
  'histjpe25_pay_ce56a235c45f7fe3', 'treatment', '7th Payment', '7th Payment', 'histjpe25_48aa1a0086e8258f', '9563175056', 'Jalpaiguri', 'MANOTOSH ROY',
  '2025-05-17', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:21:00.000Z', '2025-05-17T10:21:00.000Z'
),
(
  'histjpe25_pay_e188b782211b995a', 'treatment', '8th Payment', '8th Payment', 'histjpe25_48aa1a0086e8258f', '9563175056', 'Jalpaiguri', 'MANOTOSH ROY',
  '2025-05-20', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-20T10:22:00.000Z', '2025-05-20T10:22:00.000Z'
),
(
  'histjpe25_pay_7384390e01ddf410', 'treatment', '9th Payment', '9th Payment', 'histjpe25_48aa1a0086e8258f', '9563175056', 'Jalpaiguri', 'MANOTOSH ROY',
  '2025-05-27', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-27T10:23:00.000Z', '2025-05-27T10:23:00.000Z'
),
(
  'histjpe25_pay_3627a40df3fa2c6f', 'treatment', '10th Payment', '10th Payment', 'histjpe25_48aa1a0086e8258f', '9563175056', 'Jalpaiguri', 'MANOTOSH ROY',
  '2025-05-29', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-29T10:24:00.000Z', '2025-05-29T10:24:00.000Z'
),
(
  'histjpe25_pay_3ccd04c686f3f62f', 'treatment', 'Advance', 'Advance', 'histjpe25_9767a049edb5caed', '7602376461', 'Jalpaiguri', 'SUMAN BISWAS',
  '2025-04-22', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-22T10:15:00.000Z', '2025-04-22T10:15:00.000Z'
),
(
  'histjpe25_pay_45d431ba2007259b', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_9767a049edb5caed', '7602376461', 'Jalpaiguri', 'SUMAN BISWAS',
  '2025-04-26', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-26T10:16:00.000Z', '2025-04-26T10:16:00.000Z'
),
(
  'histjpe25_pay_83b0eb46061b4eb0', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_9767a049edb5caed', '7602376461', 'Jalpaiguri', 'SUMAN BISWAS',
  '2025-04-29', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-29T10:17:00.000Z', '2025-04-29T10:17:00.000Z'
),
(
  'histjpe25_pay_e28e135a9210c746', 'treatment', '4th Payment', '4th Payment', 'histjpe25_9767a049edb5caed', '7602376461', 'Jalpaiguri', 'SUMAN BISWAS',
  '2025-05-03', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-03T10:18:00.000Z', '2025-05-03T10:18:00.000Z'
),
(
  'histjpe25_pay_539f18815362ba00', 'treatment', '5th Payment', '5th Payment', 'histjpe25_9767a049edb5caed', '7602376461', 'Jalpaiguri', 'SUMAN BISWAS',
  '2025-05-06', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-06T10:19:00.000Z', '2025-05-06T10:19:00.000Z'
),
(
  'histjpe25_pay_cad3dd9ef3379d02', 'treatment', '6th Payment', '6th Payment', 'histjpe25_9767a049edb5caed', '7602376461', 'Jalpaiguri', 'SUMAN BISWAS',
  '2025-05-13', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-13T10:20:00.000Z', '2025-05-13T10:20:00.000Z'
),
(
  'histjpe25_pay_234edf41234634b2', 'treatment', '7th Payment', '7th Payment', 'histjpe25_9767a049edb5caed', '7602376461', 'Jalpaiguri', 'SUMAN BISWAS',
  '2025-05-31', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-31T10:21:00.000Z', '2025-05-31T10:21:00.000Z'
),
(
  'histjpe25_pay_1ac6cb20cf3c4a23', 'treatment', '8th Payment', '8th Payment', 'histjpe25_9767a049edb5caed', '7602376461', 'Jalpaiguri', 'SUMAN BISWAS',
  '2025-06-07', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-07T10:22:00.000Z', '2025-06-07T10:22:00.000Z'
),
(
  'histjpe25_pay_3ca27752ca3bdbae', 'treatment', '9th Payment', '9th Payment', 'histjpe25_9767a049edb5caed', '7602376461', 'Jalpaiguri', 'SUMAN BISWAS',
  '2025-05-17', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:23:00.000Z', '2025-05-17T10:23:00.000Z'
),
(
  'histjpe25_pay_de0e21a48bc5c2db', 'treatment', '10th Payment', '10th Payment', 'histjpe25_9767a049edb5caed', '7602376461', 'Jalpaiguri', 'SUMAN BISWAS',
  '2025-08-16', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:24:00.000Z', '2025-08-16T10:24:00.000Z'
),
(
  'histjpe25_pay_d71a1cf48894c004', 'treatment', 'Advance', 'Advance', 'histjpe25_bd67c87b8c2eed59', '8597456404', 'Jalpaiguri', 'RAJU SARKAR',
  '2025-09-26', '3500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-26T10:15:00.000Z', '2025-09-26T10:15:00.000Z'
),
(
  'histjpe25_pay_bd29da4bd66b4e91', 'treatment', 'Advance', 'Advance', 'histjpe25_a5786277fb9d9113', '7679872605', 'Jalpaiguri', 'MD MOINUDDIN',
  '2025-04-30', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-30T10:15:00.000Z', '2025-04-30T10:15:00.000Z'
),
(
  'histjpe25_pay_6bea2d25db7f7d32', 'treatment', 'Advance', 'Advance', 'histjpe25_c1b3638e1ccb1ff9', '9064471790', 'Jalpaiguri', 'MIRAJUL HOQUE',
  '2025-05-06', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-06T10:15:00.000Z', '2025-05-06T10:15:00.000Z'
),
(
  'histjpe25_pay_d4192fefd7702a61', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_c1b3638e1ccb1ff9', '9064471790', 'Jalpaiguri', 'MIRAJUL HOQUE',
  '2025-05-10', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:16:00.000Z', '2025-05-10T10:16:00.000Z'
),
(
  'histjpe25_pay_21dbb8a39388045d', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_c1b3638e1ccb1ff9', '9064471790', 'Jalpaiguri', 'MIRAJUL HOQUE',
  '2025-05-17', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:17:00.000Z', '2025-05-17T10:17:00.000Z'
),
(
  'histjpe25_pay_6fe950764d0dfeb9', 'treatment', '4th Payment', '4th Payment', 'histjpe25_c1b3638e1ccb1ff9', '9064471790', 'Jalpaiguri', 'MIRAJUL HOQUE',
  '2025-05-24', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-24T10:18:00.000Z', '2025-05-24T10:18:00.000Z'
),
(
  'histjpe25_pay_dec621e0c8103fe2', 'treatment', '5th Payment', '5th Payment', 'histjpe25_c1b3638e1ccb1ff9', '9064471790', 'Jalpaiguri', 'MIRAJUL HOQUE',
  '2025-05-31', '6000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-31T10:19:00.000Z', '2025-05-31T10:19:00.000Z'
),
(
  'histjpe25_pay_4fd91fde689e7cc1', 'treatment', '6th Payment', '6th Payment', 'histjpe25_c1b3638e1ccb1ff9', '9064471790', 'Jalpaiguri', 'MIRAJUL HOQUE',
  '2025-06-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:20:00.000Z', '2025-06-21T10:20:00.000Z'
),
(
  'histjpe25_pay_bb901d69139d5ed4', 'treatment', 'Advance', 'Advance', 'histjpe25_c5158671fca734c3', '7557824144', 'Jalpaiguri', 'TUMPA ROY',
  '2025-05-06', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-06T10:15:00.000Z', '2025-05-06T10:15:00.000Z'
),
(
  'histjpe25_pay_0092b09633407301', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_c5158671fca734c3', '7557824144', 'Jalpaiguri', 'TUMPA ROY',
  '2025-05-10', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:16:00.000Z', '2025-05-10T10:16:00.000Z'
),
(
  'histjpe25_pay_8853e12b92fb5893', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_c5158671fca734c3', '7557824144', 'Jalpaiguri', 'TUMPA ROY',
  '2026-05-24', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-05-24T10:17:00.000Z', '2026-05-24T10:17:00.000Z'
),
(
  'histjpe25_pay_fab582e41e1f718b', 'treatment', '4th Payment', '4th Payment', 'histjpe25_c5158671fca734c3', '7557824144', 'Jalpaiguri', 'TUMPA ROY',
  '2025-05-27', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-27T10:18:00.000Z', '2025-05-27T10:18:00.000Z'
),
(
  'histjpe25_pay_516d3aec577265d4', 'treatment', '5th Payment', '5th Payment', 'histjpe25_c5158671fca734c3', '7557824144', 'Jalpaiguri', 'TUMPA ROY',
  '2025-05-31', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-31T10:19:00.000Z', '2025-05-31T10:19:00.000Z'
),
(
  'histjpe25_pay_677596ee73a5460c', 'treatment', '6th Payment', '6th Payment', 'histjpe25_c5158671fca734c3', '7557824144', 'Jalpaiguri', 'TUMPA ROY',
  '2025-06-07', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-07T10:20:00.000Z', '2025-06-07T10:20:00.000Z'
),
(
  'histjpe25_pay_a770e4faf2aeacca', 'treatment', '7th Payment', '7th Payment', 'histjpe25_c5158671fca734c3', '7557824144', 'Jalpaiguri', 'TUMPA ROY',
  '2025-06-10', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-10T10:21:00.000Z', '2025-06-10T10:21:00.000Z'
),
(
  'histjpe25_pay_22d4adaae37e539e', 'treatment', '8th Payment', '8th Payment', 'histjpe25_c5158671fca734c3', '7557824144', 'Jalpaiguri', 'TUMPA ROY',
  '2025-06-14', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-14T10:22:00.000Z', '2025-06-14T10:22:00.000Z'
),
(
  'histjpe25_pay_fd050feacb48521a', 'treatment', '9th Payment', '9th Payment', 'histjpe25_c5158671fca734c3', '7557824144', 'Jalpaiguri', 'TUMPA ROY',
  '2025-06-21', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:23:00.000Z', '2025-06-21T10:23:00.000Z'
),
(
  'histjpe25_pay_0ac196ccb5833580', 'treatment', '10th Payment', '10th Payment', 'histjpe25_c5158671fca734c3', '7557824144', 'Jalpaiguri', 'TUMPA ROY',
  '2025-06-28', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:24:00.000Z', '2025-06-28T10:24:00.000Z'
),
(
  'histjpe25_pay_02c7c727c3b6d15f', 'treatment', 'Advance', 'Advance', 'histjpe25_00ce16be5ac5f4ac', '9932274032', 'Jalpaiguri', 'RATAN DAS',
  '2025-05-10', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:15:00.000Z', '2025-05-10T10:15:00.000Z'
),
(
  'histjpe25_pay_3cdff26dd697280a', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_00ce16be5ac5f4ac', '9932274032', 'Jalpaiguri', 'RATAN DAS',
  '2025-05-13', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-13T10:16:00.000Z', '2025-05-13T10:16:00.000Z'
),
(
  'histjpe25_pay_bd76f96b545dfab3', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_00ce16be5ac5f4ac', '9932274032', 'Jalpaiguri', 'RATAN DAS',
  '2025-05-20', '2500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-20T10:17:00.000Z', '2025-05-20T10:17:00.000Z'
),
(
  'histjpe25_pay_7beae3345f8d18d6', 'treatment', '4th Payment', '4th Payment', 'histjpe25_00ce16be5ac5f4ac', '9932274032', 'Jalpaiguri', 'RATAN DAS',
  '2025-05-24', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-24T10:18:00.000Z', '2025-05-24T10:18:00.000Z'
),
(
  'histjpe25_pay_c2a004dd2e66e472', 'treatment', '5th Payment', '5th Payment', 'histjpe25_00ce16be5ac5f4ac', '9932274032', 'Jalpaiguri', 'RATAN DAS',
  '2025-05-27', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-27T10:19:00.000Z', '2025-05-27T10:19:00.000Z'
),
(
  'histjpe25_pay_a936014b6e302c52', 'treatment', '6th Payment', '6th Payment', 'histjpe25_00ce16be5ac5f4ac', '9932274032', 'Jalpaiguri', 'RATAN DAS',
  '2025-05-31', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-31T10:20:00.000Z', '2025-05-31T10:20:00.000Z'
),
(
  'histjpe25_pay_ac8c0b48d29dc8a4', 'treatment', '7th Payment', '7th Payment', 'histjpe25_00ce16be5ac5f4ac', '9932274032', 'Jalpaiguri', 'RATAN DAS',
  '2025-05-17', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:21:00.000Z', '2025-05-17T10:21:00.000Z'
),
(
  'histjpe25_pay_fcf1e1922b528fe3', 'treatment', '8th Payment', '8th Payment', 'histjpe25_00ce16be5ac5f4ac', '9932274032', 'Jalpaiguri', 'RATAN DAS',
  '2025-06-03', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-03T10:22:00.000Z', '2025-06-03T10:22:00.000Z'
),
(
  'histjpe25_pay_d2ab7322089e4d2b', 'treatment', '9th Payment', '9th Payment', 'histjpe25_00ce16be5ac5f4ac', '9932274032', 'Jalpaiguri', 'RATAN DAS',
  '2025-06-07', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-07T10:23:00.000Z', '2025-06-07T10:23:00.000Z'
),
(
  'histjpe25_pay_c841cf22bc48c0a7', 'treatment', '10th Payment', '10th Payment', 'histjpe25_00ce16be5ac5f4ac', '9932274032', 'Jalpaiguri', 'RATAN DAS',
  '2025-06-17', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-17T10:24:00.000Z', '2025-06-17T10:24:00.000Z'
),
(
  'histjpe25_pay_bb3eb13838bcf090', 'treatment', '11th Payment', '11th Payment', 'histjpe25_00ce16be5ac5f4ac', '9932274032', 'Jalpaiguri', 'RATAN DAS',
  '2025-06-24', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-24T10:25:00.000Z', '2025-06-24T10:25:00.000Z'
),
(
  'histjpe25_pay_e872372dc78a45b5', 'treatment', 'Advance', 'Advance', 'histjpe25_2ec685a669b9537a', '9679364669', 'Jalpaiguri', 'SAJAHAN ALAM ALI',
  '2025-05-10', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:15:00.000Z', '2025-05-10T10:15:00.000Z'
),
(
  'histjpe25_pay_70b1a399bbe39a4c', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_2ec685a669b9537a', '9679364669', 'Jalpaiguri', 'SAJAHAN ALAM ALI',
  '2025-05-17', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:16:00.000Z', '2025-05-17T10:16:00.000Z'
),
(
  'histjpe25_pay_227cc304d3aa0b30', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_2ec685a669b9537a', '9679364669', 'Jalpaiguri', 'SAJAHAN ALAM ALI',
  '2025-05-25', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-25T10:17:00.000Z', '2025-05-25T10:17:00.000Z'
),
(
  'histjpe25_pay_e3c784aa258afebb', 'treatment', '4th Payment', '4th Payment', 'histjpe25_2ec685a669b9537a', '9679364669', 'Jalpaiguri', 'SAJAHAN ALAM ALI',
  '2025-05-31', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-31T10:18:00.000Z', '2025-05-31T10:18:00.000Z'
),
(
  'histjpe25_pay_022d4f4e0c9a7b15', 'treatment', '5th Payment', '5th Payment', 'histjpe25_2ec685a669b9537a', '9679364669', 'Jalpaiguri', 'SAJAHAN ALAM ALI',
  '2025-06-07', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-07T10:19:00.000Z', '2025-06-07T10:19:00.000Z'
),
(
  'histjpe25_pay_e8383777d0e3cae1', 'treatment', '6th Payment', '6th Payment', 'histjpe25_2ec685a669b9537a', '9679364669', 'Jalpaiguri', 'SAJAHAN ALAM ALI',
  '2025-06-10', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-10T10:20:00.000Z', '2025-06-10T10:20:00.000Z'
),
(
  'histjpe25_pay_b5b62c027644ab96', 'treatment', '7th Payment', '7th Payment', 'histjpe25_2ec685a669b9537a', '9679364669', 'Jalpaiguri', 'SAJAHAN ALAM ALI',
  '2025-06-17', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-17T10:21:00.000Z', '2025-06-17T10:21:00.000Z'
),
(
  'histjpe25_pay_6432f97feafac9e1', 'treatment', '8th Payment', '8th Payment', 'histjpe25_2ec685a669b9537a', '9679364669', 'Jalpaiguri', 'SAJAHAN ALAM ALI',
  '2025-06-21', '2500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:22:00.000Z', '2025-06-21T10:22:00.000Z'
),
(
  'histjpe25_pay_1014f65040630cff', 'treatment', 'Advance', 'Advance', 'histjpe25_945926a76c21e48d', '9679872378', 'Jalpaiguri', 'NIPUN SAHA',
  '2025-05-10', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:15:00.000Z', '2025-05-10T10:15:00.000Z'
),
(
  'histjpe25_pay_86fe10ebb58b72d0', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_945926a76c21e48d', '9679872378', 'Jalpaiguri', 'NIPUN SAHA',
  '2025-05-13', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-13T10:16:00.000Z', '2025-05-13T10:16:00.000Z'
),
(
  'histjpe25_pay_48839d1809f7262f', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_945926a76c21e48d', '9679872378', 'Jalpaiguri', 'NIPUN SAHA',
  '2025-05-20', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-20T10:17:00.000Z', '2025-05-20T10:17:00.000Z'
),
(
  'histjpe25_pay_4b659331c7ffc0f5', 'treatment', '4th Payment', '4th Payment', 'histjpe25_945926a76c21e48d', '9679872378', 'Jalpaiguri', 'NIPUN SAHA',
  '2025-05-24', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-24T10:18:00.000Z', '2025-05-24T10:18:00.000Z'
),
(
  'histjpe25_pay_7960b121b2ad6fce', 'treatment', '5th Payment', '5th Payment', 'histjpe25_945926a76c21e48d', '9679872378', 'Jalpaiguri', 'NIPUN SAHA',
  '2025-05-17', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:19:00.000Z', '2025-05-17T10:19:00.000Z'
),
(
  'histjpe25_pay_ff9de593e3151ff5', 'treatment', '6th Payment', '6th Payment', 'histjpe25_945926a76c21e48d', '9679872378', 'Jalpaiguri', 'NIPUN SAHA',
  '2025-05-18', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-18T10:20:00.000Z', '2025-05-18T10:20:00.000Z'
),
(
  'histjpe25_pay_9d490ac5cc37c834', 'treatment', '7th Payment', '7th Payment', 'histjpe25_945926a76c21e48d', '9679872378', 'Jalpaiguri', 'NIPUN SAHA',
  '2025-05-27', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-27T10:21:00.000Z', '2025-05-27T10:21:00.000Z'
),
(
  'histjpe25_pay_bfd4e8d72a401dd9', 'treatment', '8th Payment', '8th Payment', 'histjpe25_945926a76c21e48d', '9679872378', 'Jalpaiguri', 'NIPUN SAHA',
  '2025-08-02', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:22:00.000Z', '2025-08-02T10:22:00.000Z'
),
(
  'histjpe25_pay_cb2fa51257eca9e9', 'treatment', '9th Payment', '9th Payment', 'histjpe25_945926a76c21e48d', '9679872378', 'Jalpaiguri', 'NIPUN SAHA',
  '2025-08-16', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:23:00.000Z', '2025-08-16T10:23:00.000Z'
),
(
  'histjpe25_pay_62eb3d8b3fd77471', 'treatment', 'Advance', 'Advance', 'histjpe25_b15c771150a65f7f', '8617389224', 'Jalpaiguri', 'SUMAN SAHA',
  '2025-05-10', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:15:00.000Z', '2025-05-10T10:15:00.000Z'
),
(
  'histjpe25_pay_d84a9f165b099771', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_b15c771150a65f7f', '8617389224', 'Jalpaiguri', 'SUMAN SAHA',
  '2025-05-13', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-13T10:16:00.000Z', '2025-05-13T10:16:00.000Z'
),
(
  'histjpe25_pay_6ca709f471d5c2d1', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_b15c771150a65f7f', '8617389224', 'Jalpaiguri', 'SUMAN SAHA',
  '2025-05-17', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:17:00.000Z', '2025-05-17T10:17:00.000Z'
),
(
  'histjpe25_pay_1ac93621f46e9c61', 'treatment', 'Advance', 'Advance', 'histjpe25_fd3325790c83f1fd', '9641077632', 'Jalpaiguri', 'RUPESH KUMAR',
  '2025-05-17', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:15:00.000Z', '2025-05-17T10:15:00.000Z'
),
(
  'histjpe25_pay_63298d9b6d57c187', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_fd3325790c83f1fd', '9641077632', 'Jalpaiguri', 'RUPESH KUMAR',
  '2025-05-20', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-20T10:16:00.000Z', '2025-05-20T10:16:00.000Z'
),
(
  'histjpe25_pay_1ece1ba33f757548', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_fd3325790c83f1fd', '9641077632', 'Jalpaiguri', 'RUPESH KUMAR',
  '2025-05-24', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-24T10:17:00.000Z', '2025-05-24T10:17:00.000Z'
),
(
  'histjpe25_pay_62e27f219fcc87bd', 'treatment', '4th Payment', '4th Payment', 'histjpe25_fd3325790c83f1fd', '9641077632', 'Jalpaiguri', 'RUPESH KUMAR',
  '2025-05-27', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-27T10:18:00.000Z', '2025-05-27T10:18:00.000Z'
),
(
  'histjpe25_pay_319638a9d32c0881', 'treatment', '5th Payment', '5th Payment', 'histjpe25_fd3325790c83f1fd', '9641077632', 'Jalpaiguri', 'RUPESH KUMAR',
  '2025-05-31', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-31T10:19:00.000Z', '2025-05-31T10:19:00.000Z'
),
(
  'histjpe25_pay_c32e1eaae1b1468e', 'treatment', '6th Payment', '6th Payment', 'histjpe25_fd3325790c83f1fd', '9641077632', 'Jalpaiguri', 'RUPESH KUMAR',
  '2025-06-03', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-03T10:20:00.000Z', '2025-06-03T10:20:00.000Z'
),
(
  'histjpe25_pay_89f240b629508680', 'treatment', '7th Payment', '7th Payment', 'histjpe25_fd3325790c83f1fd', '9641077632', 'Jalpaiguri', 'RUPESH KUMAR',
  '2025-06-14', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-14T10:21:00.000Z', '2025-06-14T10:21:00.000Z'
),
(
  'histjpe25_pay_89297c89426eeb99', 'treatment', '8th Payment', '8th Payment', 'histjpe25_fd3325790c83f1fd', '9641077632', 'Jalpaiguri', 'RUPESH KUMAR',
  '2025-06-21', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:22:00.000Z', '2025-06-21T10:22:00.000Z'
),
(
  'histjpe25_pay_6de71ad1bd37120e', 'treatment', '9th Payment', '9th Payment', 'histjpe25_fd3325790c83f1fd', '9641077632', 'Jalpaiguri', 'RUPESH KUMAR',
  '2025-06-28', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:23:00.000Z', '2025-06-28T10:23:00.000Z'
),
(
  'histjpe25_pay_227fbd1981d95913', 'treatment', 'Advance', 'Advance', 'histjpe25_00ff122cefd103bc', '9883377003', 'Jalpaiguri', 'SURAJ SAHA',
  '2025-05-10', '550', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:15:00.000Z', '2025-05-10T10:15:00.000Z'
),
(
  'histjpe25_pay_f906d64219290c61', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_00ff122cefd103bc', '9883377003', 'Jalpaiguri', 'SURAJ SAHA',
  '2025-05-17', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-17T10:16:00.000Z', '2025-05-17T10:16:00.000Z'
),
(
  'histjpe25_pay_b1b5a8704ea25896', 'treatment', 'Advance', 'Advance', 'histjpe25_81f64612d1aec5b9', '8927425993', 'Jalpaiguri', 'GOLOK SARKAR',
  '2025-05-13', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-13T10:15:00.000Z', '2025-05-13T10:15:00.000Z'
),
(
  'histjpe25_pay_e119fdc814f42589', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_81f64612d1aec5b9', '8927425993', 'Jalpaiguri', 'GOLOK SARKAR',
  '2025-05-20', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-20T10:16:00.000Z', '2025-05-20T10:16:00.000Z'
),
(
  'histjpe25_pay_94682c69f3e0a5f8', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_81f64612d1aec5b9', '8927425993', 'Jalpaiguri', 'GOLOK SARKAR',
  '2025-05-24', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-24T10:17:00.000Z', '2025-05-24T10:17:00.000Z'
),
(
  'histjpe25_pay_aeb56e895ed7bbc4', 'treatment', '4th Payment', '4th Payment', 'histjpe25_81f64612d1aec5b9', '8927425993', 'Jalpaiguri', 'GOLOK SARKAR',
  '2025-05-31', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-31T10:18:00.000Z', '2025-05-31T10:18:00.000Z'
),
(
  'histjpe25_pay_d52def264a66be6d', 'treatment', '5th Payment', '5th Payment', 'histjpe25_81f64612d1aec5b9', '8927425993', 'Jalpaiguri', 'GOLOK SARKAR',
  '2025-06-07', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-07T10:19:00.000Z', '2025-06-07T10:19:00.000Z'
),
(
  'histjpe25_pay_c49261c06d19d518', 'treatment', '6th Payment', '6th Payment', 'histjpe25_81f64612d1aec5b9', '8927425993', 'Jalpaiguri', 'GOLOK SARKAR',
  '2025-06-14', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-14T10:20:00.000Z', '2025-06-14T10:20:00.000Z'
),
(
  'histjpe25_pay_00595540a56b6fa4', 'treatment', '7th Payment', '7th Payment', 'histjpe25_81f64612d1aec5b9', '8927425993', 'Jalpaiguri', 'GOLOK SARKAR',
  '2025-06-21', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:21:00.000Z', '2025-06-21T10:21:00.000Z'
),
(
  'histjpe25_pay_3b77faaeb536cb4e', 'treatment', 'Advance', 'Advance', 'histjpe25_63c02bb1d326a36e', '9635284001', 'Jalpaiguri', 'SHIPEN ROY',
  '2025-05-20', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-20T10:15:00.000Z', '2025-05-20T10:15:00.000Z'
),
(
  'histjpe25_pay_46c0d4cf0016a4f2', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_63c02bb1d326a36e', '9635284001', 'Jalpaiguri', 'SHIPEN ROY',
  '2025-05-27', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-27T10:16:00.000Z', '2025-05-27T10:16:00.000Z'
),
(
  'histjpe25_pay_1a3a3036802ea802', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_63c02bb1d326a36e', '9635284001', 'Jalpaiguri', 'SHIPEN ROY',
  '2025-05-31', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-31T10:17:00.000Z', '2025-05-31T10:17:00.000Z'
),
(
  'histjpe25_pay_f57a922221e147a9', 'treatment', '4th Payment', '4th Payment', 'histjpe25_63c02bb1d326a36e', '9635284001', 'Jalpaiguri', 'SHIPEN ROY',
  '2025-06-07', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-07T10:18:00.000Z', '2025-06-07T10:18:00.000Z'
),
(
  'histjpe25_pay_d2a7c0790ddd741f', 'treatment', 'Advance', 'Advance', 'histjpe25_644de92704e7b28c', '9832026423', 'Jalpaiguri', 'BABAN DAS',
  '2025-05-18', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-18T10:15:00.000Z', '2025-05-18T10:15:00.000Z'
),
(
  'histjpe25_pay_aea0d1e543037c0f', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_644de92704e7b28c', '9832026423', 'Jalpaiguri', 'BABAN DAS',
  '2025-05-20', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-20T10:16:00.000Z', '2025-05-20T10:16:00.000Z'
),
(
  'histjpe25_pay_e052c0c3a7b509f0', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_644de92704e7b28c', '9832026423', 'Jalpaiguri', 'BABAN DAS',
  '2025-05-24', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-24T10:17:00.000Z', '2025-05-24T10:17:00.000Z'
),
(
  'histjpe25_pay_a74e1bf450a03fc0', 'treatment', '4th Payment', '4th Payment', 'histjpe25_644de92704e7b28c', '9832026423', 'Jalpaiguri', 'BABAN DAS',
  '2025-05-27', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-27T10:18:00.000Z', '2025-05-27T10:18:00.000Z'
),
(
  'histjpe25_pay_d0b9435da616d0df', 'treatment', '5th Payment', '5th Payment', 'histjpe25_644de92704e7b28c', '9832026423', 'Jalpaiguri', 'BABAN DAS',
  '2025-05-31', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-31T10:19:00.000Z', '2025-05-31T10:19:00.000Z'
),
(
  'histjpe25_pay_37f1d317d050e107', 'treatment', '6th Payment', '6th Payment', 'histjpe25_644de92704e7b28c', '9832026423', 'Jalpaiguri', 'BABAN DAS',
  '2025-06-10', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-10T10:20:00.000Z', '2025-06-10T10:20:00.000Z'
),
(
  'histjpe25_pay_fdc5a617795c1eba', 'treatment', '7th Payment', '7th Payment', 'histjpe25_644de92704e7b28c', '9832026423', 'Jalpaiguri', 'BABAN DAS',
  '2025-06-14', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-14T10:21:00.000Z', '2025-06-14T10:21:00.000Z'
),
(
  'histjpe25_pay_846317bc277d65f4', 'treatment', '8th Payment', '8th Payment', 'histjpe25_644de92704e7b28c', '9832026423', 'Jalpaiguri', 'BABAN DAS',
  '2025-06-17', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-17T10:22:00.000Z', '2025-06-17T10:22:00.000Z'
),
(
  'histjpe25_pay_e74b231a48ef588e', 'treatment', '9th Payment', '9th Payment', 'histjpe25_644de92704e7b28c', '9832026423', 'Jalpaiguri', 'BABAN DAS',
  '2025-06-21', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:23:00.000Z', '2025-06-21T10:23:00.000Z'
),
(
  'histjpe25_pay_e6f2fe20b568e117', 'treatment', '10th Payment', '10th Payment', 'histjpe25_644de92704e7b28c', '9832026423', 'Jalpaiguri', 'BABAN DAS',
  '2025-06-28', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:24:00.000Z', '2025-06-28T10:24:00.000Z'
),
(
  'histjpe25_pay_c37a6e24eb675fe5', 'treatment', '11th Payment', '11th Payment', 'histjpe25_644de92704e7b28c', '9832026423', 'Jalpaiguri', 'BABAN DAS',
  '2025-07-05', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:25:00.000Z', '2025-07-05T10:25:00.000Z'
),
(
  'histjpe25_pay_d1a59f7944a5c8ca', 'treatment', '12th Payment', '12th Payment', 'histjpe25_644de92704e7b28c', '9832026423', 'Jalpaiguri', 'BABAN DAS',
  '2025-07-08', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-08T10:26:00.000Z', '2025-07-08T10:26:00.000Z'
),
(
  'histjpe25_pay_6f7049fd97adfbda', 'treatment', '13th Payment', '13th Payment', 'histjpe25_644de92704e7b28c', '9832026423', 'Jalpaiguri', 'BABAN DAS',
  '2025-07-19', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-19T10:27:00.000Z', '2025-07-19T10:27:00.000Z'
),
(
  'histjpe25_pay_4881297fac84ebd6', 'treatment', '14th Payment', '14th Payment', 'histjpe25_644de92704e7b28c', '9832026423', 'Jalpaiguri', 'BABAN DAS',
  '2025-07-29', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-29T10:28:00.000Z', '2025-07-29T10:28:00.000Z'
),
(
  'histjpe25_pay_7168f5caecc1719e', 'treatment', '15th Payment', '15th Payment', 'histjpe25_644de92704e7b28c', '9832026423', 'Jalpaiguri', 'BABAN DAS',
  '2025-08-02', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:29:00.000Z', '2025-08-02T10:29:00.000Z'
),
(
  'histjpe25_pay_608716c4c8039232', 'treatment', 'Advance', 'Advance', 'histjpe25_cf5a27df3cedfc70', '9933472497', 'Jalpaiguri', 'SHIBU ROY',
  '2025-05-31', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-31T10:15:00.000Z', '2025-05-31T10:15:00.000Z'
),
(
  'histjpe25_pay_5ee0fe702f47ea82', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_cf5a27df3cedfc70', '9933472497', 'Jalpaiguri', 'SHIBU ROY',
  '2025-06-03', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-03T10:16:00.000Z', '2025-06-03T10:16:00.000Z'
),
(
  'histjpe25_pay_ff878791f9895611', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_cf5a27df3cedfc70', '9933472497', 'Jalpaiguri', 'SHIBU ROY',
  '2025-06-17', '7000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-17T10:17:00.000Z', '2025-06-17T10:17:00.000Z'
),
(
  'histjpe25_pay_94677d3063e71859', 'treatment', 'Advance', 'Advance', 'histjpe25_3e46e10f0f8720d6', '9734938612', 'Jalpaiguri', 'SARAT ROY',
  '2025-05-27', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-27T10:15:00.000Z', '2025-05-27T10:15:00.000Z'
),
(
  'histjpe25_pay_987a4b7961523249', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_3e46e10f0f8720d6', '9734938612', 'Jalpaiguri', 'SARAT ROY',
  '2025-05-31', '1700', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-31T10:16:00.000Z', '2025-05-31T10:16:00.000Z'
),
(
  'histjpe25_pay_ba8dbe295fa59c91', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_3e46e10f0f8720d6', '9734938612', 'Jalpaiguri', 'SARAT ROY',
  '2025-06-03', '7000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-03T10:17:00.000Z', '2025-06-03T10:17:00.000Z'
),
(
  'histjpe25_pay_68f1cb38cc309d73', 'treatment', '4th Payment', '4th Payment', 'histjpe25_3e46e10f0f8720d6', '9734938612', 'Jalpaiguri', 'SARAT ROY',
  '2025-06-07', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-07T10:18:00.000Z', '2025-06-07T10:18:00.000Z'
),
(
  'histjpe25_pay_e1cb6f34a54cf98e', 'treatment', '5th Payment', '5th Payment', 'histjpe25_3e46e10f0f8720d6', '9734938612', 'Jalpaiguri', 'SARAT ROY',
  '2025-06-10', '1200', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-10T10:19:00.000Z', '2025-06-10T10:19:00.000Z'
),
(
  'histjpe25_pay_b9eb8b68ac584c88', 'treatment', '6th Payment', '6th Payment', 'histjpe25_3e46e10f0f8720d6', '9734938612', 'Jalpaiguri', 'SARAT ROY',
  '2025-06-14', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-14T10:20:00.000Z', '2025-06-14T10:20:00.000Z'
),
(
  'histjpe25_pay_c4a0103a355cb77b', 'treatment', '7th Payment', '7th Payment', 'histjpe25_3e46e10f0f8720d6', '9734938612', 'Jalpaiguri', 'SARAT ROY',
  '2025-06-21', '1100', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:21:00.000Z', '2025-06-21T10:21:00.000Z'
),
(
  'histjpe25_pay_dec3a46b6044129c', 'treatment', '8th Payment', '8th Payment', 'histjpe25_3e46e10f0f8720d6', '9734938612', 'Jalpaiguri', 'SARAT ROY',
  '2025-06-28', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:22:00.000Z', '2025-06-28T10:22:00.000Z'
),
(
  'histjpe25_pay_6e1eace87ee438a6', 'treatment', '9th Payment', '9th Payment', 'histjpe25_3e46e10f0f8720d6', '9734938612', 'Jalpaiguri', 'SARAT ROY',
  '2025-07-05', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:23:00.000Z', '2025-07-05T10:23:00.000Z'
),
(
  'histjpe25_pay_993b243a7b12a92c', 'treatment', '10th Payment', '10th Payment', 'histjpe25_3e46e10f0f8720d6', '9734938612', 'Jalpaiguri', 'SARAT ROY',
  '2025-08-02', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:24:00.000Z', '2025-08-02T10:24:00.000Z'
),
(
  'histjpe25_pay_72f8a38fbceb13d4', 'treatment', '11th Payment', '11th Payment', 'histjpe25_3e46e10f0f8720d6', '9734938612', 'Jalpaiguri', 'SARAT ROY',
  '2025-08-23', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:25:00.000Z', '2025-08-23T10:25:00.000Z'
),
(
  'histjpe25_pay_0dbe3e1a76218a17', 'treatment', 'Advance', 'Advance', 'histjpe25_46e86ceee3f7f1ee', '7866811699', 'Jalpaiguri', 'MAYA BARMAN',
  '2025-04-01', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-01T10:15:00.000Z', '2025-04-01T10:15:00.000Z'
),
(
  'histjpe25_pay_8b03c2df8f509e07', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_46e86ceee3f7f1ee', '7866811699', 'Jalpaiguri', 'MAYA BARMAN',
  '2025-04-05', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-05T10:16:00.000Z', '2025-04-05T10:16:00.000Z'
),
(
  'histjpe25_pay_4b1777d113dd2867', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_46e86ceee3f7f1ee', '7866811699', 'Jalpaiguri', 'MAYA BARMAN',
  '2025-04-08', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-08T10:17:00.000Z', '2025-04-08T10:17:00.000Z'
),
(
  'histjpe25_pay_c867525d30b16011', 'treatment', '4th Payment', '4th Payment', 'histjpe25_46e86ceee3f7f1ee', '7866811699', 'Jalpaiguri', 'MAYA BARMAN',
  '2025-04-12', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-12T10:18:00.000Z', '2025-04-12T10:18:00.000Z'
),
(
  'histjpe25_pay_2b513c2643b73122', 'treatment', '5th Payment', '5th Payment', 'histjpe25_46e86ceee3f7f1ee', '7866811699', 'Jalpaiguri', 'MAYA BARMAN',
  '2025-04-19', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-19T10:19:00.000Z', '2025-04-19T10:19:00.000Z'
),
(
  'histjpe25_pay_6bddb4e7cc6c2f65', 'treatment', '6th Payment', '6th Payment', 'histjpe25_46e86ceee3f7f1ee', '7866811699', 'Jalpaiguri', 'MAYA BARMAN',
  '2025-04-26', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-04-26T10:20:00.000Z', '2025-04-26T10:20:00.000Z'
),
(
  'histjpe25_pay_8f662b673d9b8c32', 'treatment', '7th Payment', '7th Payment', 'histjpe25_46e86ceee3f7f1ee', '7866811699', 'Jalpaiguri', 'MAYA BARMAN',
  '2025-05-06', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-06T10:21:00.000Z', '2025-05-06T10:21:00.000Z'
),
(
  'histjpe25_pay_832e486e18200ffc', 'treatment', '8th Payment', '8th Payment', 'histjpe25_46e86ceee3f7f1ee', '7866811699', 'Jalpaiguri', 'MAYA BARMAN',
  '2025-05-10', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-10T10:22:00.000Z', '2025-05-10T10:22:00.000Z'
),
(
  'histjpe25_pay_0ac82f081f326167', 'treatment', '9th Payment', '9th Payment', 'histjpe25_46e86ceee3f7f1ee', '7866811699', 'Jalpaiguri', 'MAYA BARMAN',
  '2025-05-20', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-20T10:23:00.000Z', '2025-05-20T10:23:00.000Z'
),
(
  'histjpe25_pay_f6f6fbcc730524b7', 'treatment', '10th Payment', '10th Payment', 'histjpe25_46e86ceee3f7f1ee', '7866811699', 'Jalpaiguri', 'MAYA BARMAN',
  '2025-05-24', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-24T10:24:00.000Z', '2025-05-24T10:24:00.000Z'
),
(
  'histjpe25_pay_5d9240dee6c6453a', 'treatment', 'Advance', 'Advance', 'histjpe25_ef1ee5dc6aa7ceae', '9734938612', 'Jalpaiguri', 'JOYESNA SARKAR',
  '2025-05-31', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-31T10:15:00.000Z', '2025-05-31T10:15:00.000Z'
),
(
  'histjpe25_pay_47147637572b56c7', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_ef1ee5dc6aa7ceae', '9734938612', 'Jalpaiguri', 'JOYESNA SARKAR',
  '2025-06-03', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-03T10:16:00.000Z', '2025-06-03T10:16:00.000Z'
),
(
  'histjpe25_pay_6dd7646531fe2e0c', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_ef1ee5dc6aa7ceae', '9734938612', 'Jalpaiguri', 'JOYESNA SARKAR',
  '2025-06-07', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-07T10:17:00.000Z', '2025-06-07T10:17:00.000Z'
),
(
  'histjpe25_pay_fc2d7451f01390f9', 'treatment', '4th Payment', '4th Payment', 'histjpe25_ef1ee5dc6aa7ceae', '9734938612', 'Jalpaiguri', 'JOYESNA SARKAR',
  '2025-06-10', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-10T10:18:00.000Z', '2025-06-10T10:18:00.000Z'
),
(
  'histjpe25_pay_90f70ea36aa0f743', 'treatment', '5th Payment', '5th Payment', 'histjpe25_ef1ee5dc6aa7ceae', '9734938612', 'Jalpaiguri', 'JOYESNA SARKAR',
  '2025-06-14', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-14T10:19:00.000Z', '2025-06-14T10:19:00.000Z'
),
(
  'histjpe25_pay_c61c67c92dbff6f7', 'treatment', '6th Payment', '6th Payment', 'histjpe25_ef1ee5dc6aa7ceae', '9734938612', 'Jalpaiguri', 'JOYESNA SARKAR',
  '2025-06-21', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:20:00.000Z', '2025-06-21T10:20:00.000Z'
),
(
  'histjpe25_pay_d4029dc5da354725', 'treatment', '7th Payment', '7th Payment', 'histjpe25_ef1ee5dc6aa7ceae', '9734938612', 'Jalpaiguri', 'JOYESNA SARKAR',
  '2025-06-28', '2500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:21:00.000Z', '2025-06-28T10:21:00.000Z'
),
(
  'histjpe25_pay_11761c7897c8f566', 'treatment', '8th Payment', '8th Payment', 'histjpe25_ef1ee5dc6aa7ceae', '9734938612', 'Jalpaiguri', 'JOYESNA SARKAR',
  '2025-07-05', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:22:00.000Z', '2025-07-05T10:22:00.000Z'
),
(
  'histjpe25_pay_ac57e00cbe0789fd', 'treatment', '9th Payment', '9th Payment', 'histjpe25_ef1ee5dc6aa7ceae', '9734938612', 'Jalpaiguri', 'JOYESNA SARKAR',
  '2025-07-19', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-19T10:23:00.000Z', '2025-07-19T10:23:00.000Z'
),
(
  'histjpe25_pay_59481fc07b1fecb8', 'treatment', '10th Payment', '10th Payment', 'histjpe25_ef1ee5dc6aa7ceae', '9734938612', 'Jalpaiguri', 'JOYESNA SARKAR',
  '2025-10-04', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:24:00.000Z', '2025-10-04T10:24:00.000Z'
),
(
  'histjpe25_pay_d332b7494796ca7a', 'treatment', 'Advance', 'Advance', 'histjpe25_0735ca7c271fc057', '8509221401', 'Jalpaiguri', 'NAMITA ROY',
  '2025-05-31', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-05-31T10:15:00.000Z', '2025-05-31T10:15:00.000Z'
),
(
  'histjpe25_pay_2825735597f51899', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_0735ca7c271fc057', '8509221401', 'Jalpaiguri', 'NAMITA ROY',
  '2025-06-03', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-03T10:16:00.000Z', '2025-06-03T10:16:00.000Z'
),
(
  'histjpe25_pay_e5c6c5f9578b4753', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_0735ca7c271fc057', '8509221401', 'Jalpaiguri', 'NAMITA ROY',
  '2025-06-07', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-07T10:17:00.000Z', '2025-06-07T10:17:00.000Z'
),
(
  'histjpe25_pay_17475910cb062f36', 'treatment', '4th Payment', '4th Payment', 'histjpe25_0735ca7c271fc057', '8509221401', 'Jalpaiguri', 'NAMITA ROY',
  '2025-06-14', '7000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-14T10:18:00.000Z', '2025-06-14T10:18:00.000Z'
),
(
  'histjpe25_pay_5e3f066f42d90668', 'treatment', '5th Payment', '5th Payment', 'histjpe25_0735ca7c271fc057', '8509221401', 'Jalpaiguri', 'NAMITA ROY',
  '2025-06-17', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-17T10:19:00.000Z', '2025-06-17T10:19:00.000Z'
),
(
  'histjpe25_pay_2b41f4e3da510ad3', 'treatment', '6th Payment', '6th Payment', 'histjpe25_0735ca7c271fc057', '8509221401', 'Jalpaiguri', 'NAMITA ROY',
  '2025-06-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:20:00.000Z', '2025-06-21T10:20:00.000Z'
),
(
  'histjpe25_pay_7ee62496eff9117f', 'treatment', '7th Payment', '7th Payment', 'histjpe25_0735ca7c271fc057', '8509221401', 'Jalpaiguri', 'NAMITA ROY',
  '2025-07-05', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:21:00.000Z', '2025-07-05T10:21:00.000Z'
),
(
  'histjpe25_pay_73eb518cb8788048', 'treatment', '8th Payment', '8th Payment', 'histjpe25_0735ca7c271fc057', '8509221401', 'Jalpaiguri', 'NAMITA ROY',
  '2025-07-12', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-12T10:22:00.000Z', '2025-07-12T10:22:00.000Z'
),
(
  'histjpe25_pay_4bb6cdb20caee869', 'treatment', '9th Payment', '9th Payment', 'histjpe25_0735ca7c271fc057', '8509221401', 'Jalpaiguri', 'NAMITA ROY',
  '2025-07-29', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-29T10:23:00.000Z', '2025-07-29T10:23:00.000Z'
),
(
  'histjpe25_pay_cd9c7b637b33456f', 'treatment', '10th Payment', '10th Payment', 'histjpe25_0735ca7c271fc057', '8509221401', 'Jalpaiguri', 'NAMITA ROY',
  '2025-08-02', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:24:00.000Z', '2025-08-02T10:24:00.000Z'
),
(
  'histjpe25_pay_df5547beb0176a03', 'treatment', 'Advance', 'Advance', 'histjpe25_e402a891f4cceb61', '9434606517', 'Jalpaiguri', 'RIJIYA SULTANA',
  '2025-06-07', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-07T10:15:00.000Z', '2025-06-07T10:15:00.000Z'
),
(
  'histjpe25_pay_3dcbc5e0d73bad8e', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_e402a891f4cceb61', '9434606517', 'Jalpaiguri', 'RIJIYA SULTANA',
  '2025-06-10', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-10T10:16:00.000Z', '2025-06-10T10:16:00.000Z'
),
(
  'histjpe25_pay_e686f2655809468a', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_e402a891f4cceb61', '9434606517', 'Jalpaiguri', 'RIJIYA SULTANA',
  '2025-06-21', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:17:00.000Z', '2025-06-21T10:17:00.000Z'
),
(
  'histjpe25_pay_f93555487b23a9c6', 'treatment', '4th Payment', '4th Payment', 'histjpe25_e402a891f4cceb61', '9434606517', 'Jalpaiguri', 'RIJIYA SULTANA',
  '2025-06-24', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-24T10:18:00.000Z', '2025-06-24T10:18:00.000Z'
),
(
  'histjpe25_pay_5e250d27555fde29', 'treatment', '5th Payment', '5th Payment', 'histjpe25_e402a891f4cceb61', '9434606517', 'Jalpaiguri', 'RIJIYA SULTANA',
  '2025-06-28', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:19:00.000Z', '2025-06-28T10:19:00.000Z'
),
(
  'histjpe25_pay_811ff3cc8c96fbb9', 'treatment', '6th Payment', '6th Payment', 'histjpe25_e402a891f4cceb61', '9434606517', 'Jalpaiguri', 'RIJIYA SULTANA',
  '2025-07-05', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:20:00.000Z', '2025-07-05T10:20:00.000Z'
),
(
  'histjpe25_pay_1add68556c371a50', 'treatment', '7th Payment', '7th Payment', 'histjpe25_e402a891f4cceb61', '9434606517', 'Jalpaiguri', 'RIJIYA SULTANA',
  '2025-07-12', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-12T10:21:00.000Z', '2025-07-12T10:21:00.000Z'
),
(
  'histjpe25_pay_92524fd5c18a2839', 'treatment', '8th Payment', '8th Payment', 'histjpe25_e402a891f4cceb61', '9434606517', 'Jalpaiguri', 'RIJIYA SULTANA',
  '2025-07-19', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-19T10:22:00.000Z', '2025-07-19T10:22:00.000Z'
),
(
  'histjpe25_pay_416bba67ce3192b5', 'treatment', '9th Payment', '9th Payment', 'histjpe25_e402a891f4cceb61', '9434606517', 'Jalpaiguri', 'RIJIYA SULTANA',
  '2025-07-26', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:23:00.000Z', '2025-07-26T10:23:00.000Z'
),
(
  'histjpe25_pay_5bda0a1f24f419ff', 'treatment', '10th Payment', '10th Payment', 'histjpe25_e402a891f4cceb61', '9434606517', 'Jalpaiguri', 'RIJIYA SULTANA',
  '2025-08-02', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:24:00.000Z', '2025-08-02T10:24:00.000Z'
),
(
  'histjpe25_pay_424c78d8236c3cfa', 'treatment', '11th Payment', '11th Payment', 'histjpe25_e402a891f4cceb61', '9434606517', 'Jalpaiguri', 'RIJIYA SULTANA',
  '2025-08-16', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:25:00.000Z', '2025-08-16T10:25:00.000Z'
),
(
  'histjpe25_pay_59a99bc3c2b8ff5d', 'treatment', '12th Payment', '12th Payment', 'histjpe25_e402a891f4cceb61', '9434606517', 'Jalpaiguri', 'RIJIYA SULTANA',
  '2025-11-01', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-01T10:26:00.000Z', '2025-11-01T10:26:00.000Z'
),
(
  'histjpe25_pay_8c64a74153a394b5', 'treatment', 'Advance', 'Advance', 'histjpe25_13ae1cbccea083f8', '7718667453', 'Jalpaiguri', 'HARIPADO MANDAL',
  '2025-06-17', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-17T10:15:00.000Z', '2025-06-17T10:15:00.000Z'
),
(
  'histjpe25_pay_e4443cb2a45cc3fb', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_13ae1cbccea083f8', '7718667453', 'Jalpaiguri', 'HARIPADO MANDAL',
  '2025-06-28', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:16:00.000Z', '2025-06-28T10:16:00.000Z'
),
(
  'histjpe25_pay_f507534f56274bfe', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_13ae1cbccea083f8', '7718667453', 'Jalpaiguri', 'HARIPADO MANDAL',
  '2025-07-05', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:17:00.000Z', '2025-07-05T10:17:00.000Z'
),
(
  'histjpe25_pay_36c6570e9cbeaa92', 'treatment', '4th Payment', '4th Payment', 'histjpe25_13ae1cbccea083f8', '7718667453', 'Jalpaiguri', 'HARIPADO MANDAL',
  '2025-07-12', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-12T10:18:00.000Z', '2025-07-12T10:18:00.000Z'
),
(
  'histjpe25_pay_79dae48a7c61bfc9', 'treatment', '5th Payment', '5th Payment', 'histjpe25_13ae1cbccea083f8', '7718667453', 'Jalpaiguri', 'HARIPADO MANDAL',
  '2025-07-19', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-19T10:19:00.000Z', '2025-07-19T10:19:00.000Z'
),
(
  'histjpe25_pay_167ca661db1e366c', 'treatment', '6th Payment', '6th Payment', 'histjpe25_13ae1cbccea083f8', '7718667453', 'Jalpaiguri', 'HARIPADO MANDAL',
  '2025-07-26', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:20:00.000Z', '2025-07-26T10:20:00.000Z'
),
(
  'histjpe25_pay_ffdb521507d10d12', 'treatment', '7th Payment', '7th Payment', 'histjpe25_13ae1cbccea083f8', '7718667453', 'Jalpaiguri', 'HARIPADO MANDAL',
  '2025-08-09', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-09T10:21:00.000Z', '2025-08-09T10:21:00.000Z'
),
(
  'histjpe25_pay_4ea4f76c829e55f6', 'treatment', 'Advance', 'Advance', 'histjpe25_d90936700977397f', '8972684145', 'Jalpaiguri', 'DINONATH PAL',
  '2025-06-10', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-10T10:15:00.000Z', '2025-06-10T10:15:00.000Z'
),
(
  'histjpe25_pay_374ffcc570d14e88', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_d90936700977397f', '8972684145', 'Jalpaiguri', 'DINONATH PAL',
  '2025-06-21', '2500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:16:00.000Z', '2025-06-21T10:16:00.000Z'
),
(
  'histjpe25_pay_25127bfb200e2532', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_d90936700977397f', '8972684145', 'Jalpaiguri', 'DINONATH PAL',
  '2025-06-28', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:17:00.000Z', '2025-06-28T10:17:00.000Z'
),
(
  'histjpe25_pay_22da99143d37b513', 'treatment', '4th Payment', '4th Payment', 'histjpe25_d90936700977397f', '8972684145', 'Jalpaiguri', 'DINONATH PAL',
  '2025-07-05', '7000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:18:00.000Z', '2025-07-05T10:18:00.000Z'
),
(
  'histjpe25_pay_3814ace9979ef345', 'treatment', '5th Payment', '5th Payment', 'histjpe25_d90936700977397f', '8972684145', 'Jalpaiguri', 'DINONATH PAL',
  '2025-07-12', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-12T10:19:00.000Z', '2025-07-12T10:19:00.000Z'
),
(
  'histjpe25_pay_9ced1a988261dd42', 'treatment', '6th Payment', '6th Payment', 'histjpe25_d90936700977397f', '8972684145', 'Jalpaiguri', 'DINONATH PAL',
  '2025-07-19', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-19T10:20:00.000Z', '2025-07-19T10:20:00.000Z'
),
(
  'histjpe25_pay_620af53ac977add6', 'treatment', 'Advance', 'Advance', 'histjpe25_8c30c64f2a774709', '9002967240', 'Jalpaiguri', 'SONATAN ROY',
  '2025-06-17', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-17T10:15:00.000Z', '2025-06-17T10:15:00.000Z'
),
(
  'histjpe25_pay_84e9124990d466b0', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_8c30c64f2a774709', '9002967240', 'Jalpaiguri', 'SONATAN ROY',
  '2025-06-21', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-21T10:16:00.000Z', '2025-06-21T10:16:00.000Z'
),
(
  'histjpe25_pay_420172a32af40467', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_8c30c64f2a774709', '9002967240', 'Jalpaiguri', 'SONATAN ROY',
  '2025-06-28', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:17:00.000Z', '2025-06-28T10:17:00.000Z'
),
(
  'histjpe25_pay_2390c3d50f345279', 'treatment', '4th Payment', '4th Payment', 'histjpe25_8c30c64f2a774709', '9002967240', 'Jalpaiguri', 'SONATAN ROY',
  '2025-07-05', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:18:00.000Z', '2025-07-05T10:18:00.000Z'
),
(
  'histjpe25_pay_815a586584038598', 'treatment', '5th Payment', '5th Payment', 'histjpe25_8c30c64f2a774709', '9002967240', 'Jalpaiguri', 'SONATAN ROY',
  '2025-07-08', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-08T10:19:00.000Z', '2025-07-08T10:19:00.000Z'
),
(
  'histjpe25_pay_aca79a6e0bd2b28f', 'treatment', '6th Payment', '6th Payment', 'histjpe25_8c30c64f2a774709', '9002967240', 'Jalpaiguri', 'SONATAN ROY',
  '2025-07-15', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-15T10:20:00.000Z', '2025-07-15T10:20:00.000Z'
),
(
  'histjpe25_pay_26957157c0f2b61c', 'treatment', '7th Payment', '7th Payment', 'histjpe25_8c30c64f2a774709', '9002967240', 'Jalpaiguri', 'SONATAN ROY',
  '2025-07-22', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-22T10:21:00.000Z', '2025-07-22T10:21:00.000Z'
),
(
  'histjpe25_pay_1e20d4fc8582c094', 'treatment', '8th Payment', '8th Payment', 'histjpe25_8c30c64f2a774709', '9002967240', 'Jalpaiguri', 'SONATAN ROY',
  '2025-07-26', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:22:00.000Z', '2025-07-26T10:22:00.000Z'
),
(
  'histjpe25_pay_3b90d3e71e46dd2f', 'treatment', 'Advance', 'Advance', 'histjpe25_a1c7444afd7bd77d', '9474454671', 'Jalpaiguri', 'JOYDIP MANDAL',
  '2057-06-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2057-06-21T10:15:00.000Z', '2057-06-21T10:15:00.000Z'
),
(
  'histjpe25_pay_58d630ff1d96a3e3', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_a1c7444afd7bd77d', '9474454671', 'Jalpaiguri', 'JOYDIP MANDAL',
  '2025-06-24', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-24T10:16:00.000Z', '2025-06-24T10:16:00.000Z'
),
(
  'histjpe25_pay_980c452508b7b708', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_a1c7444afd7bd77d', '9474454671', 'Jalpaiguri', 'JOYDIP MANDAL',
  '2025-06-28', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:17:00.000Z', '2025-06-28T10:17:00.000Z'
),
(
  'histjpe25_pay_601a53dacb89c40a', 'treatment', '4th Payment', '4th Payment', 'histjpe25_a1c7444afd7bd77d', '9474454671', 'Jalpaiguri', 'JOYDIP MANDAL',
  '2025-07-05', '4500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:18:00.000Z', '2025-07-05T10:18:00.000Z'
),
(
  'histjpe25_pay_7ffc6195a1aba8b0', 'treatment', '5th Payment', '5th Payment', 'histjpe25_a1c7444afd7bd77d', '9474454671', 'Jalpaiguri', 'JOYDIP MANDAL',
  '2025-07-12', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-12T10:19:00.000Z', '2025-07-12T10:19:00.000Z'
),
(
  'histjpe25_pay_2fd78ec414eac4ce', 'treatment', '6th Payment', '6th Payment', 'histjpe25_a1c7444afd7bd77d', '9474454671', 'Jalpaiguri', 'JOYDIP MANDAL',
  '2025-07-19', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-19T10:20:00.000Z', '2025-07-19T10:20:00.000Z'
),
(
  'histjpe25_pay_d09dde7ec2ed509b', 'treatment', '7th Payment', '7th Payment', 'histjpe25_a1c7444afd7bd77d', '9474454671', 'Jalpaiguri', 'JOYDIP MANDAL',
  '2025-07-26', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:21:00.000Z', '2025-07-26T10:21:00.000Z'
),
(
  'histjpe25_pay_a70e6258c9c345e4', 'treatment', 'Advance', 'Advance', 'histjpe25_630178ce7b25f645', '7679263894', 'Jalpaiguri', 'MITHUN RISHI',
  '2025-06-24', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-24T10:15:00.000Z', '2025-06-24T10:15:00.000Z'
),
(
  'histjpe25_pay_524d0459affcd24a', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_630178ce7b25f645', '7679263894', 'Jalpaiguri', 'MITHUN RISHI',
  '2025-07-05', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:16:00.000Z', '2025-07-05T10:16:00.000Z'
),
(
  'histjpe25_pay_fe82608b209b2edc', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_630178ce7b25f645', '7679263894', 'Jalpaiguri', 'MITHUN RISHI',
  '2025-07-08', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-08T10:17:00.000Z', '2025-07-08T10:17:00.000Z'
),
(
  'histjpe25_pay_6f86ca7f079b0da2', 'treatment', '4th Payment', '4th Payment', 'histjpe25_630178ce7b25f645', '7679263894', 'Jalpaiguri', 'MITHUN RISHI',
  '2025-07-12', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-12T10:18:00.000Z', '2025-07-12T10:18:00.000Z'
),
(
  'histjpe25_pay_18076ae2adb8d4d8', 'treatment', '5th Payment', '5th Payment', 'histjpe25_630178ce7b25f645', '7679263894', 'Jalpaiguri', 'MITHUN RISHI',
  '2025-07-15', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-15T10:19:00.000Z', '2025-07-15T10:19:00.000Z'
),
(
  'histjpe25_pay_ae7b7031a6e4a0b2', 'treatment', '6th Payment', '6th Payment', 'histjpe25_630178ce7b25f645', '7679263894', 'Jalpaiguri', 'MITHUN RISHI',
  '2025-07-22', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-22T10:20:00.000Z', '2025-07-22T10:20:00.000Z'
),
(
  'histjpe25_pay_061c298de5a992e1', 'treatment', '7th Payment', '7th Payment', 'histjpe25_630178ce7b25f645', '7679263894', 'Jalpaiguri', 'MITHUN RISHI',
  '2025-08-02', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:21:00.000Z', '2025-08-02T10:21:00.000Z'
),
(
  'histjpe25_pay_a984cb07628aeb84', 'treatment', '8th Payment', '8th Payment', 'histjpe25_630178ce7b25f645', '7679263894', 'Jalpaiguri', 'MITHUN RISHI',
  '2025-08-23', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:22:00.000Z', '2025-08-23T10:22:00.000Z'
),
(
  'histjpe25_pay_646472c5f90eb1e5', 'treatment', 'Advance', 'Advance', 'histjpe25_e473f2273ccbcc04', '7797442776', 'Jalpaiguri', 'PROFULLA BOSAK',
  '2025-06-28', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:15:00.000Z', '2025-06-28T10:15:00.000Z'
),
(
  'histjpe25_pay_45177d3389dd2654', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_e473f2273ccbcc04', '7797442776', 'Jalpaiguri', 'PROFULLA BOSAK',
  '2025-07-05', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:16:00.000Z', '2025-07-05T10:16:00.000Z'
),
(
  'histjpe25_pay_6f6a55d20e8b5d06', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_e473f2273ccbcc04', '7797442776', 'Jalpaiguri', 'PROFULLA BOSAK',
  '2025-07-08', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-08T10:17:00.000Z', '2025-07-08T10:17:00.000Z'
),
(
  'histjpe25_pay_c3f3c23290cf4b94', 'treatment', '4th Payment', '4th Payment', 'histjpe25_e473f2273ccbcc04', '7797442776', 'Jalpaiguri', 'PROFULLA BOSAK',
  '2025-07-12', '2500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-12T10:18:00.000Z', '2025-07-12T10:18:00.000Z'
),
(
  'histjpe25_pay_3f2c1f4632eba3b7', 'treatment', '5th Payment', '5th Payment', 'histjpe25_e473f2273ccbcc04', '7797442776', 'Jalpaiguri', 'PROFULLA BOSAK',
  '2025-07-19', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-19T10:19:00.000Z', '2025-07-19T10:19:00.000Z'
),
(
  'histjpe25_pay_ce137b0d50c559b1', 'treatment', '6th Payment', '6th Payment', 'histjpe25_e473f2273ccbcc04', '7797442776', 'Jalpaiguri', 'PROFULLA BOSAK',
  '2025-07-22', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-22T10:20:00.000Z', '2025-07-22T10:20:00.000Z'
),
(
  'histjpe25_pay_dde94fbd40529640', 'treatment', '7th Payment', '7th Payment', 'histjpe25_e473f2273ccbcc04', '7797442776', 'Jalpaiguri', 'PROFULLA BOSAK',
  '2025-07-26', '2500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:21:00.000Z', '2025-07-26T10:21:00.000Z'
),
(
  'histjpe25_pay_744a23f1af019b15', 'treatment', '8th Payment', '8th Payment', 'histjpe25_e473f2273ccbcc04', '7797442776', 'Jalpaiguri', 'PROFULLA BOSAK',
  '2025-07-29', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-29T10:22:00.000Z', '2025-07-29T10:22:00.000Z'
),
(
  'histjpe25_pay_68f8162d02ca3814', 'treatment', '9th Payment', '9th Payment', 'histjpe25_e473f2273ccbcc04', '7797442776', 'Jalpaiguri', 'PROFULLA BOSAK',
  '2025-08-02', '3500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:23:00.000Z', '2025-08-02T10:23:00.000Z'
),
(
  'histjpe25_pay_e1d0946e74f6de84', 'treatment', 'Advance', 'Advance', 'histjpe25_8381d6374a636c25', '7797852870', 'Jalpaiguri', 'SOURAV SEN GUPTA',
  '2025-06-28', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-06-28T10:15:00.000Z', '2025-06-28T10:15:00.000Z'
),
(
  'histjpe25_pay_d72a377b680a970e', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_8381d6374a636c25', '7797852870', 'Jalpaiguri', 'SOURAV SEN GUPTA',
  '2025-07-05', '15000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-05T10:16:00.000Z', '2025-07-05T10:16:00.000Z'
),
(
  'histjpe25_pay_ad370c73dda1d454', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_8381d6374a636c25', '7797852870', 'Jalpaiguri', 'SOURAV SEN GUPTA',
  '2025-07-12', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-12T10:17:00.000Z', '2025-07-12T10:17:00.000Z'
),
(
  'histjpe25_pay_e04fba67ff1a8aac', 'treatment', '4th Payment', '4th Payment', 'histjpe25_8381d6374a636c25', '7797852870', 'Jalpaiguri', 'SOURAV SEN GUPTA',
  '2025-07-19', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-19T10:18:00.000Z', '2025-07-19T10:18:00.000Z'
),
(
  'histjpe25_pay_6279b7213cb68b45', 'treatment', '5th Payment', '5th Payment', 'histjpe25_8381d6374a636c25', '7797852870', 'Jalpaiguri', 'SOURAV SEN GUPTA',
  '2025-07-26', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:19:00.000Z', '2025-07-26T10:19:00.000Z'
),
(
  'histjpe25_pay_909ad991fd0aad6e', 'treatment', '6th Payment', '6th Payment', 'histjpe25_8381d6374a636c25', '7797852870', 'Jalpaiguri', 'SOURAV SEN GUPTA',
  '2025-08-02', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:20:00.000Z', '2025-08-02T10:20:00.000Z'
),
(
  'histjpe25_pay_5ac0bbf0094a2be5', 'treatment', '7th Payment', '7th Payment', 'histjpe25_8381d6374a636c25', '7797852870', 'Jalpaiguri', 'SOURAV SEN GUPTA',
  '2025-08-09', '2500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-09T10:21:00.000Z', '2025-08-09T10:21:00.000Z'
),
(
  'histjpe25_pay_27d97943d21c0c7f', 'treatment', 'Advance', 'Advance', 'histjpe25_f08131122668bf4b', '7679263894', 'Jalpaiguri', 'ASHIS MINJ ORAN',
  '2025-07-06', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-06T10:15:00.000Z', '2025-07-06T10:15:00.000Z'
),
(
  'histjpe25_pay_1b0cc650a6b398bc', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_f08131122668bf4b', '7679263894', 'Jalpaiguri', 'ASHIS MINJ ORAN',
  '2025-07-08', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-08T10:16:00.000Z', '2025-07-08T10:16:00.000Z'
),
(
  'histjpe25_pay_67909df9e95b8be8', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_f08131122668bf4b', '7679263894', 'Jalpaiguri', 'ASHIS MINJ ORAN',
  '2025-07-12', '7000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-12T10:17:00.000Z', '2025-07-12T10:17:00.000Z'
),
(
  'histjpe25_pay_d6e093d9ef55225f', 'treatment', '4th Payment', '4th Payment', 'histjpe25_f08131122668bf4b', '7679263894', 'Jalpaiguri', 'ASHIS MINJ ORAN',
  '2025-07-19', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-19T10:18:00.000Z', '2025-07-19T10:18:00.000Z'
),
(
  'histjpe25_pay_27e48c110e5287e0', 'treatment', '5th Payment', '5th Payment', 'histjpe25_f08131122668bf4b', '7679263894', 'Jalpaiguri', 'ASHIS MINJ ORAN',
  '2025-07-26', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:19:00.000Z', '2025-07-26T10:19:00.000Z'
),
(
  'histjpe25_pay_dd8744e7cba098d5', 'treatment', '6th Payment', '6th Payment', 'histjpe25_f08131122668bf4b', '7679263894', 'Jalpaiguri', 'ASHIS MINJ ORAN',
  '2025-08-09', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-09T10:20:00.000Z', '2025-08-09T10:20:00.000Z'
),
(
  'histjpe25_pay_125172a03338a42a', 'treatment', '7th Payment', '7th Payment', 'histjpe25_f08131122668bf4b', '7679263894', 'Jalpaiguri', 'ASHIS MINJ ORAN',
  '2025-08-23', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:21:00.000Z', '2025-08-23T10:21:00.000Z'
),
(
  'histjpe25_pay_219a51a8251c0544', 'treatment', '8th Payment', '8th Payment', 'histjpe25_f08131122668bf4b', '7679263894', 'Jalpaiguri', 'ASHIS MINJ ORAN',
  '2025-08-30', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:22:00.000Z', '2025-08-30T10:22:00.000Z'
),
(
  'histjpe25_pay_adc5d04970e969aa', 'treatment', '9th Payment', '9th Payment', 'histjpe25_f08131122668bf4b', '7679263894', 'Jalpaiguri', 'ASHIS MINJ ORAN',
  '2025-09-20', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:23:00.000Z', '2025-09-20T10:23:00.000Z'
),
(
  'histjpe25_pay_5638ce62d7e03538', 'treatment', '10th Payment', '10th Payment', 'histjpe25_f08131122668bf4b', '7679263894', 'Jalpaiguri', 'ASHIS MINJ ORAN',
  '2025-10-04', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:24:00.000Z', '2025-10-04T10:24:00.000Z'
),
(
  'histjpe25_pay_a9700d6265f97de9', 'treatment', 'Advance', 'Advance', 'histjpe25_1d75d2a7db80e74d', '7074945102', 'Jalpaiguri', 'ROMICHA KHATUN',
  '2025-07-08', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-08T10:15:00.000Z', '2025-07-08T10:15:00.000Z'
),
(
  'histjpe25_pay_04fdb2bc050a02fb', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_1d75d2a7db80e74d', '7074945102', 'Jalpaiguri', 'ROMICHA KHATUN',
  '2025-07-13', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-13T10:16:00.000Z', '2025-07-13T10:16:00.000Z'
),
(
  'histjpe25_pay_3ac608234a872194', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_1d75d2a7db80e74d', '7074945102', 'Jalpaiguri', 'ROMICHA KHATUN',
  '2025-07-20', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-20T10:17:00.000Z', '2025-07-20T10:17:00.000Z'
),
(
  'histjpe25_pay_fd557448aef82491', 'treatment', '4th Payment', '4th Payment', 'histjpe25_1d75d2a7db80e74d', '7074945102', 'Jalpaiguri', 'ROMICHA KHATUN',
  '2025-07-27', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-27T10:18:00.000Z', '2025-07-27T10:18:00.000Z'
),
(
  'histjpe25_pay_ab4054f5f2a8e769', 'treatment', 'Advance', 'Advance', 'histjpe25_fa50f9ef3d4ed579', '8250448488', 'Jalpaiguri', 'SHYAMALI ROY',
  '2025-07-12', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-12T10:15:00.000Z', '2025-07-12T10:15:00.000Z'
),
(
  'histjpe25_pay_4cf467ded83e6102', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_fa50f9ef3d4ed579', '8250448488', 'Jalpaiguri', 'SHYAMALI ROY',
  '2025-07-15', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-15T10:16:00.000Z', '2025-07-15T10:16:00.000Z'
),
(
  'histjpe25_pay_8c5788745012f429', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_fa50f9ef3d4ed579', '8250448488', 'Jalpaiguri', 'SHYAMALI ROY',
  '2025-07-19', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-19T10:17:00.000Z', '2025-07-19T10:17:00.000Z'
),
(
  'histjpe25_pay_3454ccd48526dffd', 'treatment', '4th Payment', '4th Payment', 'histjpe25_fa50f9ef3d4ed579', '8250448488', 'Jalpaiguri', 'SHYAMALI ROY',
  '2025-07-26', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:18:00.000Z', '2025-07-26T10:18:00.000Z'
),
(
  'histjpe25_pay_b0c111b032669489', 'treatment', '5th Payment', '5th Payment', 'histjpe25_fa50f9ef3d4ed579', '8250448488', 'Jalpaiguri', 'SHYAMALI ROY',
  '2025-08-02', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:19:00.000Z', '2025-08-02T10:19:00.000Z'
),
(
  'histjpe25_pay_cfff0d7e542b035b', 'treatment', '6th Payment', '6th Payment', 'histjpe25_fa50f9ef3d4ed579', '8250448488', 'Jalpaiguri', 'SHYAMALI ROY',
  '2025-08-23', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:20:00.000Z', '2025-08-23T10:20:00.000Z'
),
(
  'histjpe25_pay_a464dfceed60434b', 'treatment', '7th Payment', '7th Payment', 'histjpe25_fa50f9ef3d4ed579', '8250448488', 'Jalpaiguri', 'SHYAMALI ROY',
  '2025-08-30', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:21:00.000Z', '2025-08-30T10:21:00.000Z'
),
(
  'histjpe25_pay_26250f9772678afc', 'treatment', '8th Payment', '8th Payment', 'histjpe25_fa50f9ef3d4ed579', '8250448488', 'Jalpaiguri', 'SHYAMALI ROY',
  '2025-07-22', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-22T10:22:00.000Z', '2025-07-22T10:22:00.000Z'
),
(
  'histjpe25_pay_67f5afaa50fb65c0', 'treatment', 'Advance', 'Advance', 'histjpe25_d189ca719783ec26', '9911624524', 'Jalpaiguri', 'ROKEYA KHATUN',
  '2025-07-22', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-22T10:15:00.000Z', '2025-07-22T10:15:00.000Z'
),
(
  'histjpe25_pay_54331959078ca85b', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_d189ca719783ec26', '9911624524', 'Jalpaiguri', 'ROKEYA KHATUN',
  '2025-07-26', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-26T10:16:00.000Z', '2025-07-26T10:16:00.000Z'
),
(
  'histjpe25_pay_64ceae7819e2d2b9', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_d189ca719783ec26', '9911624524', 'Jalpaiguri', 'ROKEYA KHATUN',
  '2025-07-29', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-29T10:17:00.000Z', '2025-07-29T10:17:00.000Z'
),
(
  'histjpe25_pay_f779305216735f9f', 'treatment', '4th Payment', '4th Payment', 'histjpe25_d189ca719783ec26', '9911624524', 'Jalpaiguri', 'ROKEYA KHATUN',
  '2025-08-02', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:18:00.000Z', '2025-08-02T10:18:00.000Z'
),
(
  'histjpe25_pay_000ff5fc28f2fcaa', 'treatment', '5th Payment', '5th Payment', 'histjpe25_d189ca719783ec26', '9911624524', 'Jalpaiguri', 'ROKEYA KHATUN',
  '2025-08-05', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-05T10:19:00.000Z', '2025-08-05T10:19:00.000Z'
),
(
  'histjpe25_pay_0301dca2cb781613', 'treatment', '6th Payment', '6th Payment', 'histjpe25_d189ca719783ec26', '9911624524', 'Jalpaiguri', 'ROKEYA KHATUN',
  '2025-08-09', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-09T10:20:00.000Z', '2025-08-09T10:20:00.000Z'
),
(
  'histjpe25_pay_07b8996ae0134d5c', 'treatment', '7th Payment', '7th Payment', 'histjpe25_d189ca719783ec26', '9911624524', 'Jalpaiguri', 'ROKEYA KHATUN',
  '2025-08-16', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:21:00.000Z', '2025-08-16T10:21:00.000Z'
),
(
  'histjpe25_pay_447acc8735ad61be', 'treatment', '8th Payment', '8th Payment', 'histjpe25_d189ca719783ec26', '9911624524', 'Jalpaiguri', 'ROKEYA KHATUN',
  '2025-08-23', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:22:00.000Z', '2025-08-23T10:22:00.000Z'
),
(
  'histjpe25_pay_bdedc667f4934e76', 'treatment', '9th Payment', '9th Payment', 'histjpe25_d189ca719783ec26', '9911624524', 'Jalpaiguri', 'ROKEYA KHATUN',
  '2025-08-30', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:23:00.000Z', '2025-08-30T10:23:00.000Z'
),
(
  'histjpe25_pay_d9ef5cc1eef0fb55', 'treatment', 'Advance', 'Advance', 'histjpe25_5405916cad7287b8', '7585991625', 'Jalpaiguri', 'KRISHNA MOHAN ROY',
  '2025-07-29', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-07-29T10:15:00.000Z', '2025-07-29T10:15:00.000Z'
),
(
  'histjpe25_pay_e9988d70b1c4c07c', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_5405916cad7287b8', '7585991625', 'Jalpaiguri', 'KRISHNA MOHAN ROY',
  '2025-08-05', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-05T10:16:00.000Z', '2025-08-05T10:16:00.000Z'
),
(
  'histjpe25_pay_77781389dfbc6519', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_5405916cad7287b8', '7585991625', 'Jalpaiguri', 'KRISHNA MOHAN ROY',
  '2025-08-09', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-09T10:17:00.000Z', '2025-08-09T10:17:00.000Z'
),
(
  'histjpe25_pay_26cd6387964716d8', 'treatment', '4th Payment', '4th Payment', 'histjpe25_5405916cad7287b8', '7585991625', 'Jalpaiguri', 'KRISHNA MOHAN ROY',
  '2025-08-12', '2500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-12T10:18:00.000Z', '2025-08-12T10:18:00.000Z'
),
(
  'histjpe25_pay_cbe7c67c6c6fbd1e', 'treatment', '5th Payment', '5th Payment', 'histjpe25_5405916cad7287b8', '7585991625', 'Jalpaiguri', 'KRISHNA MOHAN ROY',
  '2025-08-16', '2500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:19:00.000Z', '2025-08-16T10:19:00.000Z'
),
(
  'histjpe25_pay_7168ecc12dff205d', 'treatment', '6th Payment', '6th Payment', 'histjpe25_5405916cad7287b8', '7585991625', 'Jalpaiguri', 'KRISHNA MOHAN ROY',
  '2025-08-19', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-19T10:20:00.000Z', '2025-08-19T10:20:00.000Z'
),
(
  'histjpe25_pay_fb9495429c0fb047', 'treatment', '7th Payment', '7th Payment', 'histjpe25_5405916cad7287b8', '7585991625', 'Jalpaiguri', 'KRISHNA MOHAN ROY',
  '2025-08-23', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:21:00.000Z', '2025-08-23T10:21:00.000Z'
),
(
  'histjpe25_pay_f928466a3d2503b3', 'treatment', '8th Payment', '8th Payment', 'histjpe25_5405916cad7287b8', '7585991625', 'Jalpaiguri', 'KRISHNA MOHAN ROY',
  '2025-08-30', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:22:00.000Z', '2025-08-30T10:22:00.000Z'
),
(
  'histjpe25_pay_675cf46175226eb6', 'treatment', '9th Payment', '9th Payment', 'histjpe25_5405916cad7287b8', '7585991625', 'Jalpaiguri', 'KRISHNA MOHAN ROY',
  '2025-09-02', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-02T10:23:00.000Z', '2025-09-02T10:23:00.000Z'
),
(
  'histjpe25_pay_b77d0bd92c3483eb', 'treatment', '10th Payment', '10th Payment', 'histjpe25_5405916cad7287b8', '7585991625', 'Jalpaiguri', 'KRISHNA MOHAN ROY',
  '2025-09-09', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-09T10:24:00.000Z', '2025-09-09T10:24:00.000Z'
),
(
  'histjpe25_pay_c4f402ea857e57a3', 'treatment', '11th Payment', '11th Payment', 'histjpe25_5405916cad7287b8', '7585991625', 'Jalpaiguri', 'KRISHNA MOHAN ROY',
  '2025-09-23', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-23T10:25:00.000Z', '2025-09-23T10:25:00.000Z'
),
(
  'histjpe25_pay_492836305ea62489', 'treatment', '12th Payment', '12th Payment', 'histjpe25_5405916cad7287b8', '7585991625', 'Jalpaiguri', 'KRISHNA MOHAN ROY',
  '2025-08-02', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:26:00.000Z', '2025-08-02T10:26:00.000Z'
),
(
  'histjpe25_pay_f41dfcfd7f6f1d26', 'treatment', 'Advance', 'Advance', 'histjpe25_7cf2f9820031cf86', '8327477184', 'Jalpaiguri', 'KRISHNAPADA MANDAL',
  '2025-08-05', '10000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-05T10:15:00.000Z', '2025-08-05T10:15:00.000Z'
),
(
  'histjpe25_pay_c50858eceeef4547', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_7cf2f9820031cf86', '8327477184', 'Jalpaiguri', 'KRISHNAPADA MANDAL',
  '2025-08-16', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:16:00.000Z', '2025-08-16T10:16:00.000Z'
),
(
  'histjpe25_pay_5adb400c1b353d62', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_7cf2f9820031cf86', '8327477184', 'Jalpaiguri', 'KRISHNAPADA MANDAL',
  '2025-08-23', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:17:00.000Z', '2025-08-23T10:17:00.000Z'
),
(
  'histjpe25_pay_00b9e885b981347f', 'treatment', '4th Payment', '4th Payment', 'histjpe25_7cf2f9820031cf86', '8327477184', 'Jalpaiguri', 'KRISHNAPADA MANDAL',
  '2025-08-30', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:18:00.000Z', '2025-08-30T10:18:00.000Z'
),
(
  'histjpe25_pay_d8543a665259c476', 'treatment', '5th Payment', '5th Payment', 'histjpe25_7cf2f9820031cf86', '8327477184', 'Jalpaiguri', 'KRISHNAPADA MANDAL',
  '2025-09-13', '10000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:19:00.000Z', '2025-09-13T10:19:00.000Z'
),
(
  'histjpe25_pay_c1334bc2df90d818', 'treatment', 'Advance', 'Advance', 'histjpe25_a0d7e4a04164cbfa', '7679666423', 'Jalpaiguri', 'BIKRAM ROY',
  '2025-08-02', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:15:00.000Z', '2025-08-02T10:15:00.000Z'
),
(
  'histjpe25_pay_97944ee3efc59ca9', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_a0d7e4a04164cbfa', '7679666423', 'Jalpaiguri', 'BIKRAM ROY',
  '2025-08-09', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-09T10:16:00.000Z', '2025-08-09T10:16:00.000Z'
),
(
  'histjpe25_pay_bf6653dcc5c6cbce', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_a0d7e4a04164cbfa', '7679666423', 'Jalpaiguri', 'BIKRAM ROY',
  '2025-08-16', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:17:00.000Z', '2025-08-16T10:17:00.000Z'
),
(
  'histjpe25_pay_076b76c0aeabd6c2', 'treatment', '4th Payment', '4th Payment', 'histjpe25_a0d7e4a04164cbfa', '7679666423', 'Jalpaiguri', 'BIKRAM ROY',
  '2025-08-23', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:18:00.000Z', '2025-08-23T10:18:00.000Z'
),
(
  'histjpe25_pay_3872d7f447bd3bf7', 'treatment', '5th Payment', '5th Payment', 'histjpe25_a0d7e4a04164cbfa', '7679666423', 'Jalpaiguri', 'BIKRAM ROY',
  '2025-08-30', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:19:00.000Z', '2025-08-30T10:19:00.000Z'
),
(
  'histjpe25_pay_599e79d0e97a24b4', 'treatment', '6th Payment', '6th Payment', 'histjpe25_a0d7e4a04164cbfa', '7679666423', 'Jalpaiguri', 'BIKRAM ROY',
  '2025-09-06', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:20:00.000Z', '2025-09-06T10:20:00.000Z'
),
(
  'histjpe25_pay_3b7004e91616517b', 'treatment', '7th Payment', '7th Payment', 'histjpe25_a0d7e4a04164cbfa', '7679666423', 'Jalpaiguri', 'BIKRAM ROY',
  '2025-09-20', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:21:00.000Z', '2025-09-20T10:21:00.000Z'
),
(
  'histjpe25_pay_9595b30702ef6b40', 'treatment', '8th Payment', '8th Payment', 'histjpe25_a0d7e4a04164cbfa', '7679666423', 'Jalpaiguri', 'BIKRAM ROY',
  '2025-09-27', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:22:00.000Z', '2025-09-27T10:22:00.000Z'
),
(
  'histjpe25_pay_461d0aee573a88a4', 'treatment', '9th Payment', '9th Payment', 'histjpe25_a0d7e4a04164cbfa', '7679666423', 'Jalpaiguri', 'BIKRAM ROY',
  '2025-10-04', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:23:00.000Z', '2025-10-04T10:23:00.000Z'
),
(
  'histjpe25_pay_b6e5fd4dbaadd346', 'treatment', 'Advance', 'Advance', 'histjpe25_1f37f89c7b4ca12e', '9832380655', 'Jalpaiguri', 'SARAT CH ROY',
  '2025-08-02', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:15:00.000Z', '2025-08-02T10:15:00.000Z'
),
(
  'histjpe25_pay_a0e2bc75ec9ac181', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_1f37f89c7b4ca12e', '9832380655', 'Jalpaiguri', 'SARAT CH ROY',
  '2025-08-05', '10000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-05T10:16:00.000Z', '2025-08-05T10:16:00.000Z'
),
(
  'histjpe25_pay_a64194eff116d1be', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_1f37f89c7b4ca12e', '9832380655', 'Jalpaiguri', 'SARAT CH ROY',
  '2025-08-09', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-09T10:17:00.000Z', '2025-08-09T10:17:00.000Z'
),
(
  'histjpe25_pay_c759f0fcfb6dbaa7', 'treatment', '4th Payment', '4th Payment', 'histjpe25_1f37f89c7b4ca12e', '9832380655', 'Jalpaiguri', 'SARAT CH ROY',
  '2025-08-12', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-12T10:18:00.000Z', '2025-08-12T10:18:00.000Z'
),
(
  'histjpe25_pay_48cf6700ffc84321', 'treatment', '5th Payment', '5th Payment', 'histjpe25_1f37f89c7b4ca12e', '9832380655', 'Jalpaiguri', 'SARAT CH ROY',
  '2025-08-16', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:19:00.000Z', '2025-08-16T10:19:00.000Z'
),
(
  'histjpe25_pay_10ea4480ad98209d', 'treatment', '6th Payment', '6th Payment', 'histjpe25_1f37f89c7b4ca12e', '9832380655', 'Jalpaiguri', 'SARAT CH ROY',
  '2025-08-23', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:20:00.000Z', '2025-08-23T10:20:00.000Z'
),
(
  'histjpe25_pay_8e140255f9b9d3a1', 'treatment', '7th Payment', '7th Payment', 'histjpe25_1f37f89c7b4ca12e', '9832380655', 'Jalpaiguri', 'SARAT CH ROY',
  '2025-08-30', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:21:00.000Z', '2025-08-30T10:21:00.000Z'
),
(
  'histjpe25_pay_0323aa8e8f87f041', 'treatment', '8th Payment', '8th Payment', 'histjpe25_1f37f89c7b4ca12e', '9832380655', 'Jalpaiguri', 'SARAT CH ROY',
  '2025-09-02', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-02T10:22:00.000Z', '2025-09-02T10:22:00.000Z'
),
(
  'histjpe25_pay_b337c29327684495', 'treatment', '9th Payment', '9th Payment', 'histjpe25_1f37f89c7b4ca12e', '9832380655', 'Jalpaiguri', 'SARAT CH ROY',
  '2025-09-04', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-04T10:23:00.000Z', '2025-09-04T10:23:00.000Z'
),
(
  'histjpe25_pay_e5ef950db853afd8', 'treatment', 'Advance', 'Advance', 'histjpe25_7fb6f899d94a50b8', '8116710061', 'Jalpaiguri', 'SANTASH RAM BHAGAT',
  '2025-08-02', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-02T10:15:00.000Z', '2025-08-02T10:15:00.000Z'
),
(
  'histjpe25_pay_2d7f8174eca7a814', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_7fb6f899d94a50b8', '8116710061', 'Jalpaiguri', 'SANTASH RAM BHAGAT',
  '2025-08-05', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-05T10:16:00.000Z', '2025-08-05T10:16:00.000Z'
),
(
  'histjpe25_pay_a2fb597923460780', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_7fb6f899d94a50b8', '8116710061', 'Jalpaiguri', 'SANTASH RAM BHAGAT',
  '2025-08-09', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-09T10:17:00.000Z', '2025-08-09T10:17:00.000Z'
),
(
  'histjpe25_pay_3ad07debea2a80df', 'treatment', '4th Payment', '4th Payment', 'histjpe25_7fb6f899d94a50b8', '8116710061', 'Jalpaiguri', 'SANTASH RAM BHAGAT',
  '2025-08-12', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-12T10:18:00.000Z', '2025-08-12T10:18:00.000Z'
),
(
  'histjpe25_pay_4b74b0f4940e79c9', 'treatment', '5th Payment', '5th Payment', 'histjpe25_7fb6f899d94a50b8', '8116710061', 'Jalpaiguri', 'SANTASH RAM BHAGAT',
  '2025-08-16', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:19:00.000Z', '2025-08-16T10:19:00.000Z'
),
(
  'histjpe25_pay_5b9a33b6f1991c46', 'treatment', '6th Payment', '6th Payment', 'histjpe25_7fb6f899d94a50b8', '8116710061', 'Jalpaiguri', 'SANTASH RAM BHAGAT',
  '2028-08-19', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2028-08-19T10:20:00.000Z', '2028-08-19T10:20:00.000Z'
),
(
  'histjpe25_pay_8a954ef5d099da3b', 'treatment', '7th Payment', '7th Payment', 'histjpe25_7fb6f899d94a50b8', '8116710061', 'Jalpaiguri', 'SANTASH RAM BHAGAT',
  '2025-08-23', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:21:00.000Z', '2025-08-23T10:21:00.000Z'
),
(
  'histjpe25_pay_c17873d5b332cd22', 'treatment', '8th Payment', '8th Payment', 'histjpe25_7fb6f899d94a50b8', '8116710061', 'Jalpaiguri', 'SANTASH RAM BHAGAT',
  '2025-08-26', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-26T10:22:00.000Z', '2025-08-26T10:22:00.000Z'
),
(
  'histjpe25_pay_870bf48f5ee6ab61', 'treatment', '9th Payment', '9th Payment', 'histjpe25_7fb6f899d94a50b8', '8116710061', 'Jalpaiguri', 'SANTASH RAM BHAGAT',
  '2025-08-30', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:23:00.000Z', '2025-08-30T10:23:00.000Z'
),
(
  'histjpe25_pay_1916d25758100514', 'treatment', '10th Payment', '10th Payment', 'histjpe25_7fb6f899d94a50b8', '8116710061', 'Jalpaiguri', 'SANTASH RAM BHAGAT',
  '2025-09-02', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-02T10:24:00.000Z', '2025-09-02T10:24:00.000Z'
),
(
  'histjpe25_pay_caa3460470f4b69e', 'treatment', '11th Payment', '11th Payment', 'histjpe25_7fb6f899d94a50b8', '8116710061', 'Jalpaiguri', 'SANTASH RAM BHAGAT',
  '2025-09-06', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:25:00.000Z', '2025-09-06T10:25:00.000Z'
),
(
  'histjpe25_pay_0be17d9a5f7a4e94', 'treatment', '12th Payment', '12th Payment', 'histjpe25_7fb6f899d94a50b8', '8116710061', 'Jalpaiguri', 'SANTASH RAM BHAGAT',
  '2025-09-09', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-09T10:26:00.000Z', '2025-09-09T10:26:00.000Z'
),
(
  'histjpe25_pay_4140b00dc9d25cb8', 'treatment', 'Advance', 'Advance', 'histjpe25_630587155bde0668', '9907561162', 'Jalpaiguri', 'SHEAK HASAN',
  '2025-08-12', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-12T10:15:00.000Z', '2025-08-12T10:15:00.000Z'
),
(
  'histjpe25_pay_0a2766ad8428d8d5', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_630587155bde0668', '9907561162', 'Jalpaiguri', 'SHEAK HASAN',
  '2025-08-16', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:16:00.000Z', '2025-08-16T10:16:00.000Z'
),
(
  'histjpe25_pay_437c612715059a0d', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_630587155bde0668', '9907561162', 'Jalpaiguri', 'SHEAK HASAN',
  '2025-08-23', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:17:00.000Z', '2025-08-23T10:17:00.000Z'
),
(
  'histjpe25_pay_ddc6f98349f446db', 'treatment', '4th Payment', '4th Payment', 'histjpe25_630587155bde0668', '9907561162', 'Jalpaiguri', 'SHEAK HASAN',
  '2025-09-06', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:18:00.000Z', '2025-09-06T10:18:00.000Z'
),
(
  'histjpe25_pay_4aa39f425a9aac95', 'treatment', 'Advance', 'Advance', 'histjpe25_a69ff2f187b047f4', '6238749763', 'Jalpaiguri', 'NIRANJAN ROY',
  '2025-08-09', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-09T10:15:00.000Z', '2025-08-09T10:15:00.000Z'
),
(
  'histjpe25_pay_d692092316ee42da', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_a69ff2f187b047f4', '6238749763', 'Jalpaiguri', 'NIRANJAN ROY',
  '2025-08-12', '7000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-12T10:16:00.000Z', '2025-08-12T10:16:00.000Z'
),
(
  'histjpe25_pay_79dafb2b8503242e', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_a69ff2f187b047f4', '6238749763', 'Jalpaiguri', 'NIRANJAN ROY',
  '2025-08-16', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:17:00.000Z', '2025-08-16T10:17:00.000Z'
),
(
  'histjpe25_pay_5df52026e87a91a2', 'treatment', '4th Payment', '4th Payment', 'histjpe25_a69ff2f187b047f4', '6238749763', 'Jalpaiguri', 'NIRANJAN ROY',
  '2025-08-19', '6000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-19T10:18:00.000Z', '2025-08-19T10:18:00.000Z'
),
(
  'histjpe25_pay_1052131fba6f0d48', 'treatment', '5th Payment', '5th Payment', 'histjpe25_a69ff2f187b047f4', '6238749763', 'Jalpaiguri', 'NIRANJAN ROY',
  '2025-08-23', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:19:00.000Z', '2025-08-23T10:19:00.000Z'
),
(
  'histjpe25_pay_c20339ea47044535', 'treatment', 'Advance', 'Advance', 'histjpe25_041fc465383ca449', '7548083702', 'Jalpaiguri', 'SAHEBUL ISLAM',
  '2025-08-12', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-12T10:15:00.000Z', '2025-08-12T10:15:00.000Z'
),
(
  'histjpe25_pay_b451f6d06184a333', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_041fc465383ca449', '7548083702', 'Jalpaiguri', 'SAHEBUL ISLAM',
  '2025-08-16', '10000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-16T10:16:00.000Z', '2025-08-16T10:16:00.000Z'
),
(
  'histjpe25_pay_ef9a05ed0624548d', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_041fc465383ca449', '7548083702', 'Jalpaiguri', 'SAHEBUL ISLAM',
  '2025-08-19', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-19T10:17:00.000Z', '2025-08-19T10:17:00.000Z'
),
(
  'histjpe25_pay_607c4e3319d3a09b', 'treatment', '4th Payment', '4th Payment', 'histjpe25_041fc465383ca449', '7548083702', 'Jalpaiguri', 'SAHEBUL ISLAM',
  '2025-08-23', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:18:00.000Z', '2025-08-23T10:18:00.000Z'
),
(
  'histjpe25_pay_fb82b0975a9071b0', 'treatment', '5th Payment', '5th Payment', 'histjpe25_041fc465383ca449', '7548083702', 'Jalpaiguri', 'SAHEBUL ISLAM',
  '2025-08-26', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-26T10:19:00.000Z', '2025-08-26T10:19:00.000Z'
),
(
  'histjpe25_pay_b18635d6d8cde226', 'treatment', '6th Payment', '6th Payment', 'histjpe25_041fc465383ca449', '7548083702', 'Jalpaiguri', 'SAHEBUL ISLAM',
  '2025-08-30', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:20:00.000Z', '2025-08-30T10:20:00.000Z'
),
(
  'histjpe25_pay_9e720f23eb0825df', 'treatment', '7th Payment', '7th Payment', 'histjpe25_041fc465383ca449', '7548083702', 'Jalpaiguri', 'SAHEBUL ISLAM',
  '2025-09-02', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-02T10:21:00.000Z', '2025-09-02T10:21:00.000Z'
),
(
  'histjpe25_pay_d0ef6f5a1ad49097', 'treatment', '8th Payment', '8th Payment', 'histjpe25_041fc465383ca449', '7548083702', 'Jalpaiguri', 'SAHEBUL ISLAM',
  '2025-09-09', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-09T10:22:00.000Z', '2025-09-09T10:22:00.000Z'
),
(
  'histjpe25_pay_f8742fec1baa35c2', 'treatment', '9th Payment', '9th Payment', 'histjpe25_041fc465383ca449', '7548083702', 'Jalpaiguri', 'SAHEBUL ISLAM',
  '2025-09-16', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-16T10:23:00.000Z', '2025-09-16T10:23:00.000Z'
),
(
  'histjpe25_pay_a57f93b16fec15c2', 'treatment', '10th Payment', '10th Payment', 'histjpe25_041fc465383ca449', '7548083702', 'Jalpaiguri', 'SAHEBUL ISLAM',
  '2025-09-23', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-23T10:24:00.000Z', '2025-09-23T10:24:00.000Z'
),
(
  'histjpe25_pay_688750fa515c9e8c', 'treatment', 'Advance', 'Advance', 'histjpe25_1b3f726cb3f85a6b', '9832774511', 'Jalpaiguri', 'JAHANGIR ALAM',
  '2025-08-19', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-19T10:15:00.000Z', '2025-08-19T10:15:00.000Z'
),
(
  'histjpe25_pay_237e517b9b445773', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_1b3f726cb3f85a6b', '9832774511', 'Jalpaiguri', 'JAHANGIR ALAM',
  '2025-08-23', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:16:00.000Z', '2025-08-23T10:16:00.000Z'
),
(
  'histjpe25_pay_a03c12e6d52c6310', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_1b3f726cb3f85a6b', '9832774511', 'Jalpaiguri', 'JAHANGIR ALAM',
  '2025-08-26', '6500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-26T10:17:00.000Z', '2025-08-26T10:17:00.000Z'
),
(
  'histjpe25_pay_fd76a4bd1126f607', 'treatment', '4th Payment', '4th Payment', 'histjpe25_1b3f726cb3f85a6b', '9832774511', 'Jalpaiguri', 'JAHANGIR ALAM',
  '2025-08-30', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:18:00.000Z', '2025-08-30T10:18:00.000Z'
),
(
  'histjpe25_pay_341ab6188a291078', 'treatment', '5th Payment', '5th Payment', 'histjpe25_1b3f726cb3f85a6b', '9832774511', 'Jalpaiguri', 'JAHANGIR ALAM',
  '2025-09-02', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-02T10:19:00.000Z', '2025-09-02T10:19:00.000Z'
),
(
  'histjpe25_pay_d61bf0095d5c7f7a', 'treatment', '6th Payment', '6th Payment', 'histjpe25_1b3f726cb3f85a6b', '9832774511', 'Jalpaiguri', 'JAHANGIR ALAM',
  '2025-09-06', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:20:00.000Z', '2025-09-06T10:20:00.000Z'
),
(
  'histjpe25_pay_dc9b241e00e6f60e', 'treatment', '7th Payment', '7th Payment', 'histjpe25_1b3f726cb3f85a6b', '9832774511', 'Jalpaiguri', 'JAHANGIR ALAM',
  '2025-09-09', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-09T10:21:00.000Z', '2025-09-09T10:21:00.000Z'
),
(
  'histjpe25_pay_a6c7df7a6ff0d0f9', 'treatment', '8th Payment', '8th Payment', 'histjpe25_1b3f726cb3f85a6b', '9832774511', 'Jalpaiguri', 'JAHANGIR ALAM',
  '2025-09-13', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:22:00.000Z', '2025-09-13T10:22:00.000Z'
),
(
  'histjpe25_pay_ab5a61702d56b857', 'treatment', '9th Payment', '9th Payment', 'histjpe25_1b3f726cb3f85a6b', '9832774511', 'Jalpaiguri', 'JAHANGIR ALAM',
  '2025-09-20', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:23:00.000Z', '2025-09-20T10:23:00.000Z'
),
(
  'histjpe25_pay_605dfad91e5f0467', 'treatment', 'Advance', 'Advance', 'histjpe25_9884ba31c1fc84b4', '8388975355', 'Jalpaiguri', 'KAMALA DAS',
  '2025-08-19', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-19T10:15:00.000Z', '2025-08-19T10:15:00.000Z'
),
(
  'histjpe25_pay_8a2323238e3c5fad', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_9884ba31c1fc84b4', '8388975355', 'Jalpaiguri', 'KAMALA DAS',
  '2025-08-23', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:16:00.000Z', '2025-08-23T10:16:00.000Z'
),
(
  'histjpe25_pay_dd0a067d5bd31f9b', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_9884ba31c1fc84b4', '8388975355', 'Jalpaiguri', 'KAMALA DAS',
  '2025-08-26', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-26T10:17:00.000Z', '2025-08-26T10:17:00.000Z'
),
(
  'histjpe25_pay_60c4c6c17fff023d', 'treatment', '4th Payment', '4th Payment', 'histjpe25_9884ba31c1fc84b4', '8388975355', 'Jalpaiguri', 'KAMALA DAS',
  '2025-08-30', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:18:00.000Z', '2025-08-30T10:18:00.000Z'
),
(
  'histjpe25_pay_fd792a09d3e01a22', 'treatment', '5th Payment', '5th Payment', 'histjpe25_9884ba31c1fc84b4', '8388975355', 'Jalpaiguri', 'KAMALA DAS',
  '2025-09-02', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-02T10:19:00.000Z', '2025-09-02T10:19:00.000Z'
),
(
  'histjpe25_pay_541216a57fd3a5c8', 'treatment', '6th Payment', '6th Payment', 'histjpe25_9884ba31c1fc84b4', '8388975355', 'Jalpaiguri', 'KAMALA DAS',
  '2025-09-06', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:20:00.000Z', '2025-09-06T10:20:00.000Z'
),
(
  'histjpe25_pay_5be62507ea8fce66', 'treatment', '7th Payment', '7th Payment', 'histjpe25_9884ba31c1fc84b4', '8388975355', 'Jalpaiguri', 'KAMALA DAS',
  '2025-09-13', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:21:00.000Z', '2025-09-13T10:21:00.000Z'
),
(
  'histjpe25_pay_8eab2ab843282f19', 'treatment', '8th Payment', '8th Payment', 'histjpe25_9884ba31c1fc84b4', '8388975355', 'Jalpaiguri', 'KAMALA DAS',
  '2025-09-20', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:22:00.000Z', '2025-09-20T10:22:00.000Z'
),
(
  'histjpe25_pay_39bf4170bcc07058', 'treatment', 'Advance', 'Advance', 'histjpe25_7869bed96fb2b219', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-08-23', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-23T10:15:00.000Z', '2025-08-23T10:15:00.000Z'
),
(
  'histjpe25_pay_18ccc63ebde70468', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_7869bed96fb2b219', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-08-26', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-26T10:16:00.000Z', '2025-08-26T10:16:00.000Z'
),
(
  'histjpe25_pay_3534f405236fd921', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_7869bed96fb2b219', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-08-30', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:17:00.000Z', '2025-08-30T10:17:00.000Z'
),
(
  'histjpe25_pay_6968705adfe82a0f', 'treatment', '4th Payment', '4th Payment', 'histjpe25_7869bed96fb2b219', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-09-02', '2500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-02T10:18:00.000Z', '2025-09-02T10:18:00.000Z'
),
(
  'histjpe25_pay_dd2740cdae6ddb8f', 'treatment', '5th Payment', '5th Payment', 'histjpe25_7869bed96fb2b219', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-09-06', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:19:00.000Z', '2025-09-06T10:19:00.000Z'
),
(
  'histjpe25_pay_45b6f4a08c92ea88', 'treatment', '6th Payment', '6th Payment', 'histjpe25_7869bed96fb2b219', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-09-09', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-09T10:20:00.000Z', '2025-09-09T10:20:00.000Z'
),
(
  'histjpe25_pay_4997f4abe4574692', 'treatment', '7th Payment', '7th Payment', 'histjpe25_7869bed96fb2b219', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-09-13', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:21:00.000Z', '2025-09-13T10:21:00.000Z'
),
(
  'histjpe25_pay_26135d4dcfb93c2d', 'treatment', '8th Payment', '8th Payment', 'histjpe25_7869bed96fb2b219', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-09-20', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:22:00.000Z', '2025-09-20T10:22:00.000Z'
),
(
  'histjpe25_pay_4302da77888ba2de', 'treatment', '9th Payment', '9th Payment', 'histjpe25_7869bed96fb2b219', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-10-04', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:23:00.000Z', '2025-10-04T10:23:00.000Z'
),
(
  'histjpe25_pay_0286567e49b2c075', 'treatment', '10th Payment', '10th Payment', 'histjpe25_7869bed96fb2b219', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-10-11', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:24:00.000Z', '2025-10-11T10:24:00.000Z'
),
(
  'histjpe25_pay_095b97d22cf30b0e', 'treatment', '11th Payment', '11th Payment', 'histjpe25_7869bed96fb2b219', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-11-08', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:25:00.000Z', '2025-11-08T10:25:00.000Z'
),
(
  'histjpe25_pay_b4e74c255811e67a', 'treatment', '12th Payment', '12th Payment', 'histjpe25_7869bed96fb2b219', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-11-15', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:26:00.000Z', '2025-11-15T10:26:00.000Z'
),
(
  'histjpe25_pay_610b27441f7685c6', 'treatment', '13th Payment', '13th Payment', 'histjpe25_7869bed96fb2b219', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-11-22', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-22T10:27:00.000Z', '2025-11-22T10:27:00.000Z'
),
(
  'histjpe25_pay_9306f6112b076694', 'treatment', '14th Payment', '14th Payment', 'histjpe25_7869bed96fb2b219', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-11-29', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-29T10:28:00.000Z', '2025-11-29T10:28:00.000Z'
),
(
  'histjpe25_pay_96aa7b55e89de8f4', 'treatment', '15th Payment', '15th Payment', 'histjpe25_7869bed96fb2b219', '7699983914', 'Jalpaiguri', 'KAYU MALI',
  '2025-12-06', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-06T10:29:00.000Z', '2025-12-06T10:29:00.000Z'
),
(
  'histjpe25_pay_51f2362de653dea4', 'treatment', 'Advance', 'Advance', 'histjpe25_134f162f63c28f5b', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-08-26', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-26T10:15:00.000Z', '2025-08-26T10:15:00.000Z'
),
(
  'histjpe25_pay_b6b525575cd14294', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_134f162f63c28f5b', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-09-02', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-02T10:16:00.000Z', '2025-09-02T10:16:00.000Z'
),
(
  'histjpe25_pay_e3b6008924eb6ab9', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_134f162f63c28f5b', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-09-06', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:17:00.000Z', '2025-09-06T10:17:00.000Z'
),
(
  'histjpe25_pay_10b425a2e7dca51d', 'treatment', '4th Payment', '4th Payment', 'histjpe25_134f162f63c28f5b', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-09-09', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-09T10:18:00.000Z', '2025-09-09T10:18:00.000Z'
),
(
  'histjpe25_pay_251209dcd2b2c40e', 'treatment', '5th Payment', '5th Payment', 'histjpe25_134f162f63c28f5b', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-09-13', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:19:00.000Z', '2025-09-13T10:19:00.000Z'
),
(
  'histjpe25_pay_38594cc7e5458ecc', 'treatment', '6th Payment', '6th Payment', 'histjpe25_134f162f63c28f5b', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-09-27', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:20:00.000Z', '2025-09-27T10:20:00.000Z'
),
(
  'histjpe25_pay_765b9f59ea1a58fe', 'treatment', '7th Payment', '7th Payment', 'histjpe25_134f162f63c28f5b', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-10-07', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-07T10:21:00.000Z', '2025-10-07T10:21:00.000Z'
),
(
  'histjpe25_pay_2bf193af6555106b', 'treatment', '8th Payment', '8th Payment', 'histjpe25_134f162f63c28f5b', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-10-11', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:22:00.000Z', '2025-10-11T10:22:00.000Z'
),
(
  'histjpe25_pay_2ecd3849f352e96c', 'treatment', '9th Payment', '9th Payment', 'histjpe25_134f162f63c28f5b', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-10-14', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-14T10:23:00.000Z', '2025-10-14T10:23:00.000Z'
),
(
  'histjpe25_pay_fbe7c36db3895205', 'treatment', '10th Payment', '10th Payment', 'histjpe25_134f162f63c28f5b', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-10-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-21T10:24:00.000Z', '2025-10-21T10:24:00.000Z'
),
(
  'histjpe25_pay_395c3a2661b34f5e', 'treatment', '11th Payment', '11th Payment', 'histjpe25_134f162f63c28f5b', '8972515634', 'Jalpaiguri', 'SUMITA ROY',
  '2025-11-15', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:25:00.000Z', '2025-11-15T10:25:00.000Z'
),
(
  'histjpe25_pay_11eff443a283852a', 'treatment', 'Advance', 'Advance', 'histjpe25_06a1b4de8e604826', '7363946034', 'Jalpaiguri', 'JAHIRUL HAQUE',
  '2025-08-30', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-30T10:15:00.000Z', '2025-08-30T10:15:00.000Z'
),
(
  'histjpe25_pay_a9c31e5da01a0c77', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_06a1b4de8e604826', '7363946034', 'Jalpaiguri', 'JAHIRUL HAQUE',
  '2025-09-02', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-02T10:16:00.000Z', '2025-09-02T10:16:00.000Z'
),
(
  'histjpe25_pay_4f8c0656a1003d34', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_06a1b4de8e604826', '7363946034', 'Jalpaiguri', 'JAHIRUL HAQUE',
  '2025-09-09', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-09T10:17:00.000Z', '2025-09-09T10:17:00.000Z'
),
(
  'histjpe25_pay_4228a88ab7c9d620', 'treatment', 'Advance', 'Advance', 'histjpe25_9865902f66c4b52d', '6296125142', 'Jalpaiguri', 'PRANAB ROY',
  '2025-08-31', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-08-31T10:15:00.000Z', '2025-08-31T10:15:00.000Z'
),
(
  'histjpe25_pay_94131d191d947e5b', 'treatment', 'Advance', 'Advance', 'histjpe25_edada66a87ac2fd9', '8967918639', 'Jalpaiguri', 'SHUVANKAR TANTRA',
  '2025-09-06', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-06T10:15:00.000Z', '2025-09-06T10:15:00.000Z'
),
(
  'histjpe25_pay_e15469db05738aab', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_edada66a87ac2fd9', '8967918639', 'Jalpaiguri', 'SHUVANKAR TANTRA',
  '2025-09-09', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-09T10:16:00.000Z', '2025-09-09T10:16:00.000Z'
),
(
  'histjpe25_pay_ab171be8e1e857c3', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_edada66a87ac2fd9', '8967918639', 'Jalpaiguri', 'SHUVANKAR TANTRA',
  '2025-09-13', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-13T10:17:00.000Z', '2025-09-13T10:17:00.000Z'
),
(
  'histjpe25_pay_323961be22a03a93', 'treatment', '4th Payment', '4th Payment', 'histjpe25_edada66a87ac2fd9', '8967918639', 'Jalpaiguri', 'SHUVANKAR TANTRA',
  '2025-09-16', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-16T10:18:00.000Z', '2025-09-16T10:18:00.000Z'
),
(
  'histjpe25_pay_8ceeecf294ecb86d', 'treatment', '5th Payment', '5th Payment', 'histjpe25_edada66a87ac2fd9', '8967918639', 'Jalpaiguri', 'SHUVANKAR TANTRA',
  '2025-09-20', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:19:00.000Z', '2025-09-20T10:19:00.000Z'
),
(
  'histjpe25_pay_1afba7676440d3d8', 'treatment', '6th Payment', '6th Payment', 'histjpe25_edada66a87ac2fd9', '8967918639', 'Jalpaiguri', 'SHUVANKAR TANTRA',
  '2025-09-23', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-23T10:20:00.000Z', '2025-09-23T10:20:00.000Z'
),
(
  'histjpe25_pay_a6e9fa26a7729f59', 'treatment', '7th Payment', '7th Payment', 'histjpe25_edada66a87ac2fd9', '8967918639', 'Jalpaiguri', 'SHUVANKAR TANTRA',
  '2025-10-07', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-07T10:21:00.000Z', '2025-10-07T10:21:00.000Z'
),
(
  'histjpe25_pay_939a51e3283ae76e', 'treatment', '8th Payment', '8th Payment', 'histjpe25_edada66a87ac2fd9', '8967918639', 'Jalpaiguri', 'SHUVANKAR TANTRA',
  '2025-10-11', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:22:00.000Z', '2025-10-11T10:22:00.000Z'
),
(
  'histjpe25_pay_0c665b3c5528a05a', 'treatment', '9th Payment', '9th Payment', 'histjpe25_edada66a87ac2fd9', '8967918639', 'Jalpaiguri', 'SHUVANKAR TANTRA',
  '2025-10-18', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:23:00.000Z', '2025-10-18T10:23:00.000Z'
),
(
  'histjpe25_pay_9cc56133ed5ed447', 'treatment', '10th Payment', '10th Payment', 'histjpe25_edada66a87ac2fd9', '8967918639', 'Jalpaiguri', 'SHUVANKAR TANTRA',
  '2025-10-25', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-25T10:24:00.000Z', '2025-10-25T10:24:00.000Z'
),
(
  'histjpe25_pay_2d89501f9b2447ca', 'treatment', '11th Payment', '11th Payment', 'histjpe25_edada66a87ac2fd9', '8967918639', 'Jalpaiguri', 'SHUVANKAR TANTRA',
  '2025-10-28', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-28T10:25:00.000Z', '2025-10-28T10:25:00.000Z'
),
(
  'histjpe25_pay_c56995fe6eb5d9c0', 'treatment', '12th Payment', '12th Payment', 'histjpe25_edada66a87ac2fd9', '8967918639', 'Jalpaiguri', 'SHUVANKAR TANTRA',
  '2025-11-18', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-18T10:26:00.000Z', '2025-11-18T10:26:00.000Z'
),
(
  'histjpe25_pay_ba8263aadf1f96db', 'treatment', '13th Payment', '13th Payment', 'histjpe25_edada66a87ac2fd9', '8967918639', 'Jalpaiguri', 'SHUVANKAR TANTRA',
  '2026-03-14', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-03-14T10:27:00.000Z', '2026-03-14T10:27:00.000Z'
),
(
  'histjpe25_pay_b5aa476dbb933a2d', 'treatment', '14th Payment', '14th Payment', 'histjpe25_edada66a87ac2fd9', '8967918639', 'Jalpaiguri', 'SHUVANKAR TANTRA',
  '2026-03-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-03-21T10:28:00.000Z', '2026-03-21T10:28:00.000Z'
),
(
  'histjpe25_pay_5a4897fab4efd1d6', 'treatment', 'Advance', 'Advance', 'histjpe25_6a057120b715b80b', '8848653776', 'Jalpaiguri', 'PURNADEB BISWAS',
  '2025-09-09', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-09T10:15:00.000Z', '2025-09-09T10:15:00.000Z'
),
(
  'histjpe25_pay_542b1950c8eab6d8', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_6a057120b715b80b', '8848653776', 'Jalpaiguri', 'PURNADEB BISWAS',
  '2025-09-20', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:16:00.000Z', '2025-09-20T10:16:00.000Z'
),
(
  'histjpe25_pay_6abb67d79e497e23', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_6a057120b715b80b', '8848653776', 'Jalpaiguri', 'PURNADEB BISWAS',
  '2025-09-27', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:17:00.000Z', '2025-09-27T10:17:00.000Z'
),
(
  'histjpe25_pay_d3e5b8942739ba94', 'treatment', '4th Payment', '4th Payment', 'histjpe25_6a057120b715b80b', '8848653776', 'Jalpaiguri', 'PURNADEB BISWAS',
  '2002-10-04', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2002-10-04T10:18:00.000Z', '2002-10-04T10:18:00.000Z'
),
(
  'histjpe25_pay_dca6903ffd14b524', 'treatment', '5th Payment', '5th Payment', 'histjpe25_6a057120b715b80b', '8848653776', 'Jalpaiguri', 'PURNADEB BISWAS',
  '2025-10-14', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-14T10:19:00.000Z', '2025-10-14T10:19:00.000Z'
),
(
  'histjpe25_pay_a2af6847ecf97622', 'treatment', '6th Payment', '6th Payment', 'histjpe25_6a057120b715b80b', '8848653776', 'Jalpaiguri', 'PURNADEB BISWAS',
  '2025-11-08', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:20:00.000Z', '2025-11-08T10:20:00.000Z'
),
(
  'histjpe25_pay_cf50dd56541d6c54', 'treatment', 'Advance', 'Advance', 'histjpe25_46d11867a65bb608', '7478866361', 'Jalpaiguri', 'SULOCHINI PASOYAN',
  '2025-09-20', '3500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:15:00.000Z', '2025-09-20T10:15:00.000Z'
),
(
  'histjpe25_pay_6d4da6734a311a58', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_46d11867a65bb608', '7478866361', 'Jalpaiguri', 'SULOCHINI PASOYAN',
  '2025-09-23', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-23T10:16:00.000Z', '2025-09-23T10:16:00.000Z'
),
(
  'histjpe25_pay_48b24b5dfe4311b1', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_46d11867a65bb608', '7478866361', 'Jalpaiguri', 'SULOCHINI PASOYAN',
  '2025-09-27', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:17:00.000Z', '2025-09-27T10:17:00.000Z'
),
(
  'histjpe25_pay_1f6437f1691dc886', 'treatment', '4th Payment', '4th Payment', 'histjpe25_46d11867a65bb608', '7478866361', 'Jalpaiguri', 'SULOCHINI PASOYAN',
  '2025-10-07', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-07T10:18:00.000Z', '2025-10-07T10:18:00.000Z'
),
(
  'histjpe25_pay_1eb216ea38a5a96d', 'treatment', '5th Payment', '5th Payment', 'histjpe25_46d11867a65bb608', '7478866361', 'Jalpaiguri', 'SULOCHINI PASOYAN',
  '2025-10-11', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:19:00.000Z', '2025-10-11T10:19:00.000Z'
),
(
  'histjpe25_pay_3fc9ee4cb65cbd88', 'treatment', '6th Payment', '6th Payment', 'histjpe25_46d11867a65bb608', '7478866361', 'Jalpaiguri', 'SULOCHINI PASOYAN',
  '2025-10-18', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:20:00.000Z', '2025-10-18T10:20:00.000Z'
),
(
  'histjpe25_pay_14e67acf8fdada72', 'treatment', '7th Payment', '7th Payment', 'histjpe25_46d11867a65bb608', '7478866361', 'Jalpaiguri', 'SULOCHINI PASOYAN',
  '2025-10-25', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-25T10:21:00.000Z', '2025-10-25T10:21:00.000Z'
),
(
  'histjpe25_pay_f6bb15706a9bd8b2', 'treatment', '8th Payment', '8th Payment', 'histjpe25_46d11867a65bb608', '7478866361', 'Jalpaiguri', 'SULOCHINI PASOYAN',
  '2025-11-01', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-01T10:22:00.000Z', '2025-11-01T10:22:00.000Z'
),
(
  'histjpe25_pay_4ed6534e71a71c78', 'treatment', '9th Payment', '9th Payment', 'histjpe25_46d11867a65bb608', '7478866361', 'Jalpaiguri', 'SULOCHINI PASOYAN',
  '2025-11-04', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-04T10:23:00.000Z', '2025-11-04T10:23:00.000Z'
),
(
  'histjpe25_pay_1ba3634ca727b48f', 'treatment', '10th Payment', '10th Payment', 'histjpe25_46d11867a65bb608', '7478866361', 'Jalpaiguri', 'SULOCHINI PASOYAN',
  '2025-11-08', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:24:00.000Z', '2025-11-08T10:24:00.000Z'
),
(
  'histjpe25_pay_72244384ad8e2cdc', 'treatment', '11th Payment', '11th Payment', 'histjpe25_46d11867a65bb608', '7478866361', 'Jalpaiguri', 'SULOCHINI PASOYAN',
  '2025-11-11', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-11T10:25:00.000Z', '2025-11-11T10:25:00.000Z'
),
(
  'histjpe25_pay_c6e5c0a974a99f2a', 'treatment', '12th Payment', '12th Payment', 'histjpe25_46d11867a65bb608', '7478866361', 'Jalpaiguri', 'SULOCHINI PASOYAN',
  '2025-11-15', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:26:00.000Z', '2025-11-15T10:26:00.000Z'
),
(
  'histjpe25_pay_5393c69c08512cbe', 'treatment', '13th Payment', '13th Payment', 'histjpe25_46d11867a65bb608', '7478866361', 'Jalpaiguri', 'SULOCHINI PASOYAN',
  '2025-11-18', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-18T10:27:00.000Z', '2025-11-18T10:27:00.000Z'
),
(
  'histjpe25_pay_ae314fa9f3a1ae03', 'treatment', '14th Payment', '14th Payment', 'histjpe25_46d11867a65bb608', '7478866361', 'Jalpaiguri', 'SULOCHINI PASOYAN',
  '2025-11-22', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-22T10:28:00.000Z', '2025-11-22T10:28:00.000Z'
),
(
  'histjpe25_pay_65e297bc827d2b74', 'treatment', '15th Payment', '15th Payment', 'histjpe25_46d11867a65bb608', '7478866361', 'Jalpaiguri', 'SULOCHINI PASOYAN',
  '2025-12-02', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-02T10:29:00.000Z', '2025-12-02T10:29:00.000Z'
),
(
  'histjpe25_pay_928e6604aed5faf9', 'treatment', 'Advance', 'Advance', 'histjpe25_eff5729a10c56847', '7384033340', 'Jalpaiguri', 'PARUL SARKAR',
  '2025-09-20', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:15:00.000Z', '2025-09-20T10:15:00.000Z'
),
(
  'histjpe25_pay_ffa6cec940f162b0', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_eff5729a10c56847', '7384033340', 'Jalpaiguri', 'PARUL SARKAR',
  '2025-09-23', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-23T10:16:00.000Z', '2025-09-23T10:16:00.000Z'
),
(
  'histjpe25_pay_9b413455de42256f', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_eff5729a10c56847', '7384033340', 'Jalpaiguri', 'PARUL SARKAR',
  '2025-09-27', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:17:00.000Z', '2025-09-27T10:17:00.000Z'
),
(
  'histjpe25_pay_67524de6c0b4783e', 'treatment', '4th Payment', '4th Payment', 'histjpe25_eff5729a10c56847', '7384033340', 'Jalpaiguri', 'PARUL SARKAR',
  '2025-10-04', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:18:00.000Z', '2025-10-04T10:18:00.000Z'
),
(
  'histjpe25_pay_7e3f83a49ac51cb5', 'treatment', '5th Payment', '5th Payment', 'histjpe25_eff5729a10c56847', '7384033340', 'Jalpaiguri', 'PARUL SARKAR',
  '2025-10-14', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-14T10:19:00.000Z', '2025-10-14T10:19:00.000Z'
),
(
  'histjpe25_pay_b748f0d8e0c714f7', 'treatment', '6th Payment', '6th Payment', 'histjpe25_eff5729a10c56847', '7384033340', 'Jalpaiguri', 'PARUL SARKAR',
  '2025-11-11', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-11T10:20:00.000Z', '2025-11-11T10:20:00.000Z'
),
(
  'histjpe25_pay_e8c7449644254640', 'treatment', '7th Payment', '7th Payment', 'histjpe25_eff5729a10c56847', '7384033340', 'Jalpaiguri', 'PARUL SARKAR',
  '2025-11-25', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-25T10:21:00.000Z', '2025-11-25T10:21:00.000Z'
),
(
  'histjpe25_pay_4344dea5f784edd0', 'treatment', '8th Payment', '8th Payment', 'histjpe25_eff5729a10c56847', '7384033340', 'Jalpaiguri', 'PARUL SARKAR',
  '2025-12-13', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:22:00.000Z', '2025-12-13T10:22:00.000Z'
),
(
  'histjpe25_pay_8c5e2fca69137348', 'treatment', 'Advance', 'Advance', 'histjpe25_38688ee2c9a42884', '7076908674', 'Jalpaiguri', 'BIRJINIA KANDULNA',
  '2025-09-20', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-20T10:15:00.000Z', '2025-09-20T10:15:00.000Z'
),
(
  'histjpe25_pay_2df0407e9c0961c9', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_38688ee2c9a42884', '7076908674', 'Jalpaiguri', 'BIRJINIA KANDULNA',
  '2025-09-23', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-23T10:16:00.000Z', '2025-09-23T10:16:00.000Z'
),
(
  'histjpe25_pay_d1ed6737a4fab01d', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_38688ee2c9a42884', '7076908674', 'Jalpaiguri', 'BIRJINIA KANDULNA',
  '2025-09-27', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:17:00.000Z', '2025-09-27T10:17:00.000Z'
),
(
  'histjpe25_pay_6c765c8baaa2e17f', 'treatment', '4th Payment', '4th Payment', 'histjpe25_38688ee2c9a42884', '7076908674', 'Jalpaiguri', 'BIRJINIA KANDULNA',
  '2025-10-07', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-07T10:18:00.000Z', '2025-10-07T10:18:00.000Z'
),
(
  'histjpe25_pay_2312ad890016be92', 'treatment', '5th Payment', '5th Payment', 'histjpe25_38688ee2c9a42884', '7076908674', 'Jalpaiguri', 'BIRJINIA KANDULNA',
  '2025-10-11', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:19:00.000Z', '2025-10-11T10:19:00.000Z'
),
(
  'histjpe25_pay_ecf6447476f856a5', 'treatment', '6th Payment', '6th Payment', 'histjpe25_38688ee2c9a42884', '7076908674', 'Jalpaiguri', 'BIRJINIA KANDULNA',
  '2025-10-18', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:20:00.000Z', '2025-10-18T10:20:00.000Z'
),
(
  'histjpe25_pay_9d3f1d48ce9c8453', 'treatment', '7th Payment', '7th Payment', 'histjpe25_38688ee2c9a42884', '7076908674', 'Jalpaiguri', 'BIRJINIA KANDULNA',
  '2025-10-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-21T10:21:00.000Z', '2025-10-21T10:21:00.000Z'
),
(
  'histjpe25_pay_252b616c8ecaccb8', 'treatment', '8th Payment', '8th Payment', 'histjpe25_38688ee2c9a42884', '7076908674', 'Jalpaiguri', 'BIRJINIA KANDULNA',
  '2025-10-28', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-28T10:22:00.000Z', '2025-10-28T10:22:00.000Z'
),
(
  'histjpe25_pay_22abbe524e72eb5b', 'treatment', '9th Payment', '9th Payment', 'histjpe25_38688ee2c9a42884', '7076908674', 'Jalpaiguri', 'BIRJINIA KANDULNA',
  '2025-11-04', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-04T10:23:00.000Z', '2025-11-04T10:23:00.000Z'
),
(
  'histjpe25_pay_7d9f4c54053809c3', 'treatment', '10th Payment', '10th Payment', 'histjpe25_38688ee2c9a42884', '7076908674', 'Jalpaiguri', 'BIRJINIA KANDULNA',
  '2025-11-08', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:24:00.000Z', '2025-11-08T10:24:00.000Z'
),
(
  'histjpe25_pay_420f1e5e64b7dd4a', 'treatment', '11th Payment', '11th Payment', 'histjpe25_38688ee2c9a42884', '7076908674', 'Jalpaiguri', 'BIRJINIA KANDULNA',
  '2025-11-15', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:25:00.000Z', '2025-11-15T10:25:00.000Z'
),
(
  'histjpe25_pay_e28f31577f9bba28', 'treatment', '12th Payment', '12th Payment', 'histjpe25_38688ee2c9a42884', '7076908674', 'Jalpaiguri', 'BIRJINIA KANDULNA',
  '2025-11-22', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-22T10:26:00.000Z', '2025-11-22T10:26:00.000Z'
),
(
  'histjpe25_pay_0ce15aa1ec897471', 'treatment', '13th Payment', '13th Payment', 'histjpe25_38688ee2c9a42884', '7076908674', 'Jalpaiguri', 'BIRJINIA KANDULNA',
  '2025-11-29', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-29T10:27:00.000Z', '2025-11-29T10:27:00.000Z'
),
(
  'histjpe25_pay_ee70c0007f2d8b58', 'treatment', 'Advance', 'Advance', 'histjpe25_3a834c910a10b202', '8670828932', 'Jalpaiguri', 'SANDIPA KUNDU',
  '2025-09-22', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-22T10:15:00.000Z', '2025-09-22T10:15:00.000Z'
),
(
  'histjpe25_pay_59791de8666938a3', 'treatment', 'Advance', 'Advance', 'histjpe25_342ddd36501ab44b', '9475808989', 'Jalpaiguri', 'ANNAPURNA ROY',
  '2025-09-23', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-23T10:15:00.000Z', '2025-09-23T10:15:00.000Z'
),
(
  'histjpe25_pay_146634504aa5dd62', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_342ddd36501ab44b', '9475808989', 'Jalpaiguri', 'ANNAPURNA ROY',
  '2025-09-27', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:16:00.000Z', '2025-09-27T10:16:00.000Z'
),
(
  'histjpe25_pay_f9e3e189c6b4ed7a', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_342ddd36501ab44b', '9475808989', 'Jalpaiguri', 'ANNAPURNA ROY',
  '2025-10-04', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:17:00.000Z', '2025-10-04T10:17:00.000Z'
),
(
  'histjpe25_pay_85104eb7ef751d5e', 'treatment', '4th Payment', '4th Payment', 'histjpe25_342ddd36501ab44b', '9475808989', 'Jalpaiguri', 'ANNAPURNA ROY',
  '2025-10-07', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-07T10:18:00.000Z', '2025-10-07T10:18:00.000Z'
),
(
  'histjpe25_pay_921900bbce5cf85e', 'treatment', '5th Payment', '5th Payment', 'histjpe25_342ddd36501ab44b', '9475808989', 'Jalpaiguri', 'ANNAPURNA ROY',
  '2025-10-11', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:19:00.000Z', '2025-10-11T10:19:00.000Z'
),
(
  'histjpe25_pay_5475108c599d3b91', 'treatment', '6th Payment', '6th Payment', 'histjpe25_342ddd36501ab44b', '9475808989', 'Jalpaiguri', 'ANNAPURNA ROY',
  '2025-10-14', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-14T10:20:00.000Z', '2025-10-14T10:20:00.000Z'
),
(
  'histjpe25_pay_023f191858724c2c', 'treatment', '7th Payment', '7th Payment', 'histjpe25_342ddd36501ab44b', '9475808989', 'Jalpaiguri', 'ANNAPURNA ROY',
  '2025-10-18', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:21:00.000Z', '2025-10-18T10:21:00.000Z'
),
(
  'histjpe25_pay_2178bc50a3a18664', 'treatment', '8th Payment', '8th Payment', 'histjpe25_342ddd36501ab44b', '9475808989', 'Jalpaiguri', 'ANNAPURNA ROY',
  '2025-10-25', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-25T10:22:00.000Z', '2025-10-25T10:22:00.000Z'
),
(
  'histjpe25_pay_e72310528535d46c', 'treatment', '9th Payment', '9th Payment', 'histjpe25_342ddd36501ab44b', '9475808989', 'Jalpaiguri', 'ANNAPURNA ROY',
  '2025-10-28', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-28T10:23:00.000Z', '2025-10-28T10:23:00.000Z'
),
(
  'histjpe25_pay_cae0ac07239e8095', 'treatment', '10th Payment', '10th Payment', 'histjpe25_342ddd36501ab44b', '9475808989', 'Jalpaiguri', 'ANNAPURNA ROY',
  '2025-11-01', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-01T10:24:00.000Z', '2025-11-01T10:24:00.000Z'
),
(
  'histjpe25_pay_9ba7fec19632fa89', 'treatment', '11th Payment', '11th Payment', 'histjpe25_342ddd36501ab44b', '9475808989', 'Jalpaiguri', 'ANNAPURNA ROY',
  '2025-11-04', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-04T10:25:00.000Z', '2025-11-04T10:25:00.000Z'
),
(
  'histjpe25_pay_e8b7d614c54529e2', 'treatment', '12th Payment', '12th Payment', 'histjpe25_342ddd36501ab44b', '9475808989', 'Jalpaiguri', 'ANNAPURNA ROY',
  '2025-11-08', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:26:00.000Z', '2025-11-08T10:26:00.000Z'
),
(
  'histjpe25_pay_f3b4b8c4f007bb78', 'treatment', '13th Payment', '13th Payment', 'histjpe25_342ddd36501ab44b', '9475808989', 'Jalpaiguri', 'ANNAPURNA ROY',
  '2025-11-11', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-11T10:27:00.000Z', '2025-11-11T10:27:00.000Z'
),
(
  'histjpe25_pay_81c427fe5732611b', 'treatment', '14th Payment', '14th Payment', 'histjpe25_342ddd36501ab44b', '9475808989', 'Jalpaiguri', 'ANNAPURNA ROY',
  '2025-11-15', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:28:00.000Z', '2025-11-15T10:28:00.000Z'
),
(
  'histjpe25_pay_d1f2e792ce8abd96', 'treatment', '15th Payment', '15th Payment', 'histjpe25_342ddd36501ab44b', '9475808989', 'Jalpaiguri', 'ANNAPURNA ROY',
  '2025-11-22', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-22T10:29:00.000Z', '2025-11-22T10:29:00.000Z'
),
(
  'histjpe25_pay_1107f437c045bfc2', 'treatment', 'Advance', 'Advance', 'histjpe25_79ef0db3eddcc11d', '8927836474', 'Jalpaiguri', 'TUMPA BEGAM',
  '2025-09-23', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-23T10:15:00.000Z', '2025-09-23T10:15:00.000Z'
),
(
  'histjpe25_pay_462a8e07160c0b86', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_79ef0db3eddcc11d', '8927836474', 'Jalpaiguri', 'TUMPA BEGAM',
  '2025-09-27', '7000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:16:00.000Z', '2025-09-27T10:16:00.000Z'
),
(
  'histjpe25_pay_613b14c7e209c421', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_79ef0db3eddcc11d', '8927836474', 'Jalpaiguri', 'TUMPA BEGAM',
  '2025-10-04', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:17:00.000Z', '2025-10-04T10:17:00.000Z'
),
(
  'histjpe25_pay_08a477d5fa54998a', 'treatment', '4th Payment', '4th Payment', 'histjpe25_79ef0db3eddcc11d', '8927836474', 'Jalpaiguri', 'TUMPA BEGAM',
  '2025-10-07', '4500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-07T10:18:00.000Z', '2025-10-07T10:18:00.000Z'
),
(
  'histjpe25_pay_895d805e4d9f460d', 'treatment', '5th Payment', '5th Payment', 'histjpe25_79ef0db3eddcc11d', '8927836474', 'Jalpaiguri', 'TUMPA BEGAM',
  '2025-10-11', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:19:00.000Z', '2025-10-11T10:19:00.000Z'
),
(
  'histjpe25_pay_5657c4f1facbd8fc', 'treatment', '6th Payment', '6th Payment', 'histjpe25_79ef0db3eddcc11d', '8927836474', 'Jalpaiguri', 'TUMPA BEGAM',
  '2025-10-14', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-14T10:20:00.000Z', '2025-10-14T10:20:00.000Z'
),
(
  'histjpe25_pay_39d0c3a7d5373342', 'treatment', '7th Payment', '7th Payment', 'histjpe25_79ef0db3eddcc11d', '8927836474', 'Jalpaiguri', 'TUMPA BEGAM',
  '2025-10-18', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:21:00.000Z', '2025-10-18T10:21:00.000Z'
),
(
  'histjpe25_pay_ed289dd9c3374b5e', 'treatment', '8th Payment', '8th Payment', 'histjpe25_79ef0db3eddcc11d', '8927836474', 'Jalpaiguri', 'TUMPA BEGAM',
  '2025-10-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-21T10:22:00.000Z', '2025-10-21T10:22:00.000Z'
),
(
  'histjpe25_pay_acc05884f79a9089', 'treatment', '9th Payment', '9th Payment', 'histjpe25_79ef0db3eddcc11d', '8927836474', 'Jalpaiguri', 'TUMPA BEGAM',
  '2025-10-25', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-25T10:23:00.000Z', '2025-10-25T10:23:00.000Z'
),
(
  'histjpe25_pay_b4f96169b7cf6527', 'treatment', 'Advance', 'Advance', 'histjpe25_cb500526f68b7ddd', '9832371611', 'Jalpaiguri', 'CHANDAN KUNDU',
  '2025-09-27', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-09-27T10:15:00.000Z', '2025-09-27T10:15:00.000Z'
),
(
  'histjpe25_pay_1bdbd34ee387ff8a', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_cb500526f68b7ddd', '9832371611', 'Jalpaiguri', 'CHANDAN KUNDU',
  '2025-10-04', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:16:00.000Z', '2025-10-04T10:16:00.000Z'
),
(
  'histjpe25_pay_be6428bd34b3bea7', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_cb500526f68b7ddd', '9832371611', 'Jalpaiguri', 'CHANDAN KUNDU',
  '2025-10-07', '7000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-07T10:17:00.000Z', '2025-10-07T10:17:00.000Z'
),
(
  'histjpe25_pay_d6568fabdff1a69a', 'treatment', '4th Payment', '4th Payment', 'histjpe25_cb500526f68b7ddd', '9832371611', 'Jalpaiguri', 'CHANDAN KUNDU',
  '2025-10-11', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:18:00.000Z', '2025-10-11T10:18:00.000Z'
),
(
  'histjpe25_pay_4b61ddba3aa1234d', 'treatment', '5th Payment', '5th Payment', 'histjpe25_cb500526f68b7ddd', '9832371611', 'Jalpaiguri', 'CHANDAN KUNDU',
  '2025-10-18', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:19:00.000Z', '2025-10-18T10:19:00.000Z'
),
(
  'histjpe25_pay_9abe03bf8d5c7840', 'treatment', '6th Payment', '6th Payment', 'histjpe25_cb500526f68b7ddd', '9832371611', 'Jalpaiguri', 'CHANDAN KUNDU',
  '2025-10-21', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-21T10:20:00.000Z', '2025-10-21T10:20:00.000Z'
),
(
  'histjpe25_pay_39117d59272e1072', 'treatment', '7th Payment', '7th Payment', 'histjpe25_cb500526f68b7ddd', '9832371611', 'Jalpaiguri', 'CHANDAN KUNDU',
  '2025-10-25', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-25T10:21:00.000Z', '2025-10-25T10:21:00.000Z'
),
(
  'histjpe25_pay_980239357ee25fcd', 'treatment', '8th Payment', '8th Payment', 'histjpe25_cb500526f68b7ddd', '9832371611', 'Jalpaiguri', 'CHANDAN KUNDU',
  '2025-10-28', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-28T10:22:00.000Z', '2025-10-28T10:22:00.000Z'
),
(
  'histjpe25_pay_299d7a395ccc22a0', 'treatment', '9th Payment', '9th Payment', 'histjpe25_cb500526f68b7ddd', '9832371611', 'Jalpaiguri', 'CHANDAN KUNDU',
  '2025-11-01', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-01T10:23:00.000Z', '2025-11-01T10:23:00.000Z'
),
(
  'histjpe25_pay_e1c7ad9cda63d977', 'treatment', '10th Payment', '10th Payment', 'histjpe25_cb500526f68b7ddd', '9832371611', 'Jalpaiguri', 'CHANDAN KUNDU',
  '2025-11-08', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:24:00.000Z', '2025-11-08T10:24:00.000Z'
),
(
  'histjpe25_pay_06da84710e2da8fb', 'treatment', '11th Payment', '11th Payment', 'histjpe25_cb500526f68b7ddd', '9832371611', 'Jalpaiguri', 'CHANDAN KUNDU',
  '2025-11-11', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-11T10:25:00.000Z', '2025-11-11T10:25:00.000Z'
),
(
  'histjpe25_pay_2cd5debc8dd9a1b0', 'treatment', 'Advance', 'Advance', 'histjpe25_8af18d14e95579f1', '8597910258', 'Jalpaiguri', 'SONALY ROY',
  '2025-10-04', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-04T10:15:00.000Z', '2025-10-04T10:15:00.000Z'
),
(
  'histjpe25_pay_c9d1b4d2a17b8da5', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_8af18d14e95579f1', '8597910258', 'Jalpaiguri', 'SONALY ROY',
  '2025-10-07', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-07T10:16:00.000Z', '2025-10-07T10:16:00.000Z'
),
(
  'histjpe25_pay_ac00358cfe47b5c8', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_8af18d14e95579f1', '8597910258', 'Jalpaiguri', 'SONALY ROY',
  '2025-10-11', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:17:00.000Z', '2025-10-11T10:17:00.000Z'
),
(
  'histjpe25_pay_0f2ed6a155a14a6d', 'treatment', '4th Payment', '4th Payment', 'histjpe25_8af18d14e95579f1', '8597910258', 'Jalpaiguri', 'SONALY ROY',
  '2025-10-14', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-14T10:18:00.000Z', '2025-10-14T10:18:00.000Z'
),
(
  'histjpe25_pay_2ebc452c5c34a4a3', 'treatment', '5th Payment', '5th Payment', 'histjpe25_8af18d14e95579f1', '8597910258', 'Jalpaiguri', 'SONALY ROY',
  '2025-10-18', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:19:00.000Z', '2025-10-18T10:19:00.000Z'
),
(
  'histjpe25_pay_e95f672dac99132e', 'treatment', '6th Payment', '6th Payment', 'histjpe25_8af18d14e95579f1', '8597910258', 'Jalpaiguri', 'SONALY ROY',
  '2025-10-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-21T10:20:00.000Z', '2025-10-21T10:20:00.000Z'
),
(
  'histjpe25_pay_1197becd9144ac5a', 'treatment', '7th Payment', '7th Payment', 'histjpe25_8af18d14e95579f1', '8597910258', 'Jalpaiguri', 'SONALY ROY',
  '2025-10-25', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-25T10:21:00.000Z', '2025-10-25T10:21:00.000Z'
),
(
  'histjpe25_pay_587d29ec07f1b786', 'treatment', '8th Payment', '8th Payment', 'histjpe25_8af18d14e95579f1', '8597910258', 'Jalpaiguri', 'SONALY ROY',
  '2025-11-01', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-01T10:22:00.000Z', '2025-11-01T10:22:00.000Z'
),
(
  'histjpe25_pay_e8fbec118f45cf04', 'treatment', '9th Payment', '9th Payment', 'histjpe25_8af18d14e95579f1', '8597910258', 'Jalpaiguri', 'SONALY ROY',
  '2025-11-15', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:23:00.000Z', '2025-11-15T10:23:00.000Z'
),
(
  'histjpe25_pay_af4e40a51d20bea8', 'treatment', '10th Payment', '10th Payment', 'histjpe25_8af18d14e95579f1', '8597910258', 'Jalpaiguri', 'SONALY ROY',
  '2025-11-29', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-29T10:24:00.000Z', '2025-11-29T10:24:00.000Z'
),
(
  'histjpe25_pay_85e42f48fa7d56b0', 'treatment', '11th Payment', '11th Payment', 'histjpe25_8af18d14e95579f1', '8597910258', 'Jalpaiguri', 'SONALY ROY',
  '2026-01-10', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-10T10:25:00.000Z', '2026-01-10T10:25:00.000Z'
),
(
  'histjpe25_pay_b5a7cc3ab5eae8bb', 'treatment', '12th Payment', '12th Payment', 'histjpe25_8af18d14e95579f1', '8597910258', 'Jalpaiguri', 'SONALY ROY',
  '2026-01-17', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-17T10:26:00.000Z', '2026-01-17T10:26:00.000Z'
),
(
  'histjpe25_pay_d9589d212e626746', 'treatment', 'Advance', 'Advance', 'histjpe25_690a64c9821640f8', '7905763228', 'Jalpaiguri', 'BIPLAB SARKAR',
  '2025-10-11', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-11T10:15:00.000Z', '2025-10-11T10:15:00.000Z'
),
(
  'histjpe25_pay_b8d0fba55aa35410', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_690a64c9821640f8', '7905763228', 'Jalpaiguri', 'BIPLAB SARKAR',
  '2025-10-14', '14000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-14T10:16:00.000Z', '2025-10-14T10:16:00.000Z'
),
(
  'histjpe25_pay_587811b921611af1', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_690a64c9821640f8', '7905763228', 'Jalpaiguri', 'BIPLAB SARKAR',
  '2025-11-01', '15000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-01T10:17:00.000Z', '2025-11-01T10:17:00.000Z'
),
(
  'histjpe25_pay_c7f30faf206298e8', 'treatment', '4th Payment', '4th Payment', 'histjpe25_690a64c9821640f8', '7905763228', 'Jalpaiguri', 'BIPLAB SARKAR',
  '2025-11-15', '10000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:18:00.000Z', '2025-11-15T10:18:00.000Z'
),
(
  'histjpe25_pay_0c5590a9301ee46e', 'treatment', 'Advance', 'Advance', 'histjpe25_eb5e4f634f9123f3', '9932427384', 'Jalpaiguri', 'MALEKA PARVIN',
  '2025-10-21', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-21T10:15:00.000Z', '2025-10-21T10:15:00.000Z'
),
(
  'histjpe25_pay_798f52aedc6f2f86', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_eb5e4f634f9123f3', '9932427384', 'Jalpaiguri', 'MALEKA PARVIN',
  '2025-10-25', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-25T10:16:00.000Z', '2025-10-25T10:16:00.000Z'
),
(
  'histjpe25_pay_380ec475fc720cc0', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_eb5e4f634f9123f3', '9932427384', 'Jalpaiguri', 'MALEKA PARVIN',
  '2025-10-28', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-28T10:17:00.000Z', '2025-10-28T10:17:00.000Z'
),
(
  'histjpe25_pay_e1c24d215e79486b', 'treatment', '4th Payment', '4th Payment', 'histjpe25_eb5e4f634f9123f3', '9932427384', 'Jalpaiguri', 'MALEKA PARVIN',
  '2025-11-01', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-01T10:18:00.000Z', '2025-11-01T10:18:00.000Z'
),
(
  'histjpe25_pay_bb26c2237b48e604', 'treatment', '5th Payment', '5th Payment', 'histjpe25_eb5e4f634f9123f3', '9932427384', 'Jalpaiguri', 'MALEKA PARVIN',
  '2025-11-08', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:19:00.000Z', '2025-11-08T10:19:00.000Z'
),
(
  'histjpe25_pay_d094586950783d9c', 'treatment', '6th Payment', '6th Payment', 'histjpe25_eb5e4f634f9123f3', '9932427384', 'Jalpaiguri', 'MALEKA PARVIN',
  '2025-11-11', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-11T10:20:00.000Z', '2025-11-11T10:20:00.000Z'
),
(
  'histjpe25_pay_58f2dc3aac6b7769', 'treatment', '7th Payment', '7th Payment', 'histjpe25_eb5e4f634f9123f3', '9932427384', 'Jalpaiguri', 'MALEKA PARVIN',
  '2025-11-18', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-18T10:21:00.000Z', '2025-11-18T10:21:00.000Z'
),
(
  'histjpe25_pay_42406f04c048dca1', 'treatment', '8th Payment', '8th Payment', 'histjpe25_eb5e4f634f9123f3', '9932427384', 'Jalpaiguri', 'MALEKA PARVIN',
  '2025-12-02', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-02T10:22:00.000Z', '2025-12-02T10:22:00.000Z'
),
(
  'histjpe25_pay_bf57b6e059715988', 'treatment', '9th Payment', '9th Payment', 'histjpe25_eb5e4f634f9123f3', '9932427384', 'Jalpaiguri', 'MALEKA PARVIN',
  '2025-12-20', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-20T10:23:00.000Z', '2025-12-20T10:23:00.000Z'
),
(
  'histjpe25_pay_a849e7d685d41e46', 'treatment', 'Advance', 'Advance', 'histjpe25_779ff1e5ccf6c397', '9046288011', 'Jalpaiguri', 'SNIGKDHA BHATTACHARYA',
  '2025-10-18', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:15:00.000Z', '2025-10-18T10:15:00.000Z'
),
(
  'histjpe25_pay_806b88b4db3c0fe5', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_779ff1e5ccf6c397', '9046288011', 'Jalpaiguri', 'SNIGKDHA BHATTACHARYA',
  '2025-10-25', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-25T10:16:00.000Z', '2025-10-25T10:16:00.000Z'
),
(
  'histjpe25_pay_79ebf186e8347f6a', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_779ff1e5ccf6c397', '9046288011', 'Jalpaiguri', 'SNIGKDHA BHATTACHARYA',
  '2025-10-28', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-28T10:17:00.000Z', '2025-10-28T10:17:00.000Z'
),
(
  'histjpe25_pay_1ee8cdc18c97d837', 'treatment', '4th Payment', '4th Payment', 'histjpe25_779ff1e5ccf6c397', '9046288011', 'Jalpaiguri', 'SNIGKDHA BHATTACHARYA',
  '2025-11-01', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-01T10:18:00.000Z', '2025-11-01T10:18:00.000Z'
),
(
  'histjpe25_pay_81268ddb01a55ca5', 'treatment', '5th Payment', '5th Payment', 'histjpe25_779ff1e5ccf6c397', '9046288011', 'Jalpaiguri', 'SNIGKDHA BHATTACHARYA',
  '2025-11-04', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-04T10:19:00.000Z', '2025-11-04T10:19:00.000Z'
),
(
  'histjpe25_pay_c9a8958fa990267f', 'treatment', '6th Payment', '6th Payment', 'histjpe25_779ff1e5ccf6c397', '9046288011', 'Jalpaiguri', 'SNIGKDHA BHATTACHARYA',
  '2025-11-11', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-11T10:20:00.000Z', '2025-11-11T10:20:00.000Z'
),
(
  'histjpe25_pay_b8bc8142037043c3', 'treatment', '7th Payment', '7th Payment', 'histjpe25_779ff1e5ccf6c397', '9046288011', 'Jalpaiguri', 'SNIGKDHA BHATTACHARYA',
  '2025-11-15', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:21:00.000Z', '2025-11-15T10:21:00.000Z'
),
(
  'histjpe25_pay_815e6f9b794302b5', 'treatment', '8th Payment', '8th Payment', 'histjpe25_779ff1e5ccf6c397', '9046288011', 'Jalpaiguri', 'SNIGKDHA BHATTACHARYA',
  '2025-11-18', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-18T10:22:00.000Z', '2025-11-18T10:22:00.000Z'
),
(
  'histjpe25_pay_b0c71b7d8c261fd9', 'treatment', '9th Payment', '9th Payment', 'histjpe25_779ff1e5ccf6c397', '9046288011', 'Jalpaiguri', 'SNIGKDHA BHATTACHARYA',
  '2025-11-22', '2500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-22T10:23:00.000Z', '2025-11-22T10:23:00.000Z'
),
(
  'histjpe25_pay_85af91813458e593', 'treatment', '10th Payment', '10th Payment', 'histjpe25_779ff1e5ccf6c397', '9046288011', 'Jalpaiguri', 'SNIGKDHA BHATTACHARYA',
  '2025-12-13', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:24:00.000Z', '2025-12-13T10:24:00.000Z'
),
(
  'histjpe25_pay_9c24e7d67f5e1b71', 'treatment', '11th Payment', '11th Payment', 'histjpe25_779ff1e5ccf6c397', '9046288011', 'Jalpaiguri', 'SNIGKDHA BHATTACHARYA',
  '2025-12-23', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-23T10:25:00.000Z', '2025-12-23T10:25:00.000Z'
),
(
  'histjpe25_pay_c27df300ce552e63', 'treatment', 'Advance', 'Advance', 'histjpe25_b81ffad462a77034', '8927555801', 'Jalpaiguri', 'MADHAV CH. SARKAR',
  '2025-10-18', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:15:00.000Z', '2025-10-18T10:15:00.000Z'
),
(
  'histjpe25_pay_f63a977c0c738c6d', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_b81ffad462a77034', '8927555801', 'Jalpaiguri', 'MADHAV CH. SARKAR',
  '2025-10-18', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-18T10:16:00.000Z', '2025-10-18T10:16:00.000Z'
),
(
  'histjpe25_pay_b3a2681906c50a9f', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_b81ffad462a77034', '8927555801', 'Jalpaiguri', 'MADHAV CH. SARKAR',
  '2025-10-21', '6000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-21T10:17:00.000Z', '2025-10-21T10:17:00.000Z'
),
(
  'histjpe25_pay_5ec3a7dbee86017f', 'treatment', '4th Payment', '4th Payment', 'histjpe25_b81ffad462a77034', '8927555801', 'Jalpaiguri', 'MADHAV CH. SARKAR',
  '2025-10-25', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-25T10:18:00.000Z', '2025-10-25T10:18:00.000Z'
),
(
  'histjpe25_pay_508f377507b33c33', 'treatment', '5th Payment', '5th Payment', 'histjpe25_b81ffad462a77034', '8927555801', 'Jalpaiguri', 'MADHAV CH. SARKAR',
  '2025-11-01', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-01T10:19:00.000Z', '2025-11-01T10:19:00.000Z'
),
(
  'histjpe25_pay_fdffd8e2f09705da', 'treatment', '6th Payment', '6th Payment', 'histjpe25_b81ffad462a77034', '8927555801', 'Jalpaiguri', 'MADHAV CH. SARKAR',
  '2025-11-04', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-04T10:20:00.000Z', '2025-11-04T10:20:00.000Z'
),
(
  'histjpe25_pay_ecf64089c5f87c75', 'treatment', '7th Payment', '7th Payment', 'histjpe25_b81ffad462a77034', '8927555801', 'Jalpaiguri', 'MADHAV CH. SARKAR',
  '2025-11-08', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:21:00.000Z', '2025-11-08T10:21:00.000Z'
),
(
  'histjpe25_pay_101fa33096eaa51d', 'treatment', '8th Payment', '8th Payment', 'histjpe25_b81ffad462a77034', '8927555801', 'Jalpaiguri', 'MADHAV CH. SARKAR',
  '2025-11-11', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-11T10:22:00.000Z', '2025-11-11T10:22:00.000Z'
),
(
  'histjpe25_pay_2fc74db3e14d4229', 'treatment', '9th Payment', '9th Payment', 'histjpe25_b81ffad462a77034', '8927555801', 'Jalpaiguri', 'MADHAV CH. SARKAR',
  '2025-11-22', '10000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-22T10:23:00.000Z', '2025-11-22T10:23:00.000Z'
),
(
  'histjpe25_pay_643cbd2f911e2314', 'treatment', 'Advance', 'Advance', 'histjpe25_a9f3335142fceb55', '8116124449', 'Jalpaiguri', 'SUKUMAR PAHARI',
  '2025-11-01', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-01T10:15:00.000Z', '2025-11-01T10:15:00.000Z'
),
(
  'histjpe25_pay_687799abbd57d49c', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_a9f3335142fceb55', '8116124449', 'Jalpaiguri', 'SUKUMAR PAHARI',
  '2025-11-04', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-04T10:16:00.000Z', '2025-11-04T10:16:00.000Z'
),
(
  'histjpe25_pay_0150c55b929ca8ae', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_a9f3335142fceb55', '8116124449', 'Jalpaiguri', 'SUKUMAR PAHARI',
  '2025-11-08', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:17:00.000Z', '2025-11-08T10:17:00.000Z'
),
(
  'histjpe25_pay_26b11ebb9b7d58c5', 'treatment', '4th Payment', '4th Payment', 'histjpe25_a9f3335142fceb55', '8116124449', 'Jalpaiguri', 'SUKUMAR PAHARI',
  '2025-11-11', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-11T10:18:00.000Z', '2025-11-11T10:18:00.000Z'
),
(
  'histjpe25_pay_924a2b743a01f5d0', 'treatment', '5th Payment', '5th Payment', 'histjpe25_a9f3335142fceb55', '8116124449', 'Jalpaiguri', 'SUKUMAR PAHARI',
  '2025-11-15', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:19:00.000Z', '2025-11-15T10:19:00.000Z'
),
(
  'histjpe25_pay_5ff018b2ea1ebf3f', 'treatment', '6th Payment', '6th Payment', 'histjpe25_a9f3335142fceb55', '8116124449', 'Jalpaiguri', 'SUKUMAR PAHARI',
  '2025-11-18', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-18T10:20:00.000Z', '2025-11-18T10:20:00.000Z'
),
(
  'histjpe25_pay_cdc6430fedd4863e', 'treatment', '7th Payment', '7th Payment', 'histjpe25_a9f3335142fceb55', '8116124449', 'Jalpaiguri', 'SUKUMAR PAHARI',
  '2025-11-22', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-22T10:21:00.000Z', '2025-11-22T10:21:00.000Z'
),
(
  'histjpe25_pay_dea24b6bd4b0a7d4', 'treatment', '8th Payment', '8th Payment', 'histjpe25_a9f3335142fceb55', '8116124449', 'Jalpaiguri', 'SUKUMAR PAHARI',
  '2025-12-06', '4500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-06T10:22:00.000Z', '2025-12-06T10:22:00.000Z'
),
(
  'histjpe25_pay_584d2dc056ce29ef', 'treatment', '9th Payment', '9th Payment', 'histjpe25_a9f3335142fceb55', '8116124449', 'Jalpaiguri', 'SUKUMAR PAHARI',
  '2025-12-20', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-20T10:23:00.000Z', '2025-12-20T10:23:00.000Z'
),
(
  'histjpe25_pay_53ffe5f23b1fa160', 'treatment', 'Advance', 'Advance', 'histjpe25_9b59497c40762b8e', '6296182593', 'Jalpaiguri', 'RAJIB BISWAS',
  '2025-10-28', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-10-28T10:15:00.000Z', '2025-10-28T10:15:00.000Z'
),
(
  'histjpe25_pay_f7b372288cf0b99a', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_9b59497c40762b8e', '6296182593', 'Jalpaiguri', 'RAJIB BISWAS',
  '2025-11-01', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-01T10:16:00.000Z', '2025-11-01T10:16:00.000Z'
),
(
  'histjpe25_pay_72059d7f19de28de', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_9b59497c40762b8e', '6296182593', 'Jalpaiguri', 'RAJIB BISWAS',
  '2025-11-04', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-04T10:17:00.000Z', '2025-11-04T10:17:00.000Z'
),
(
  'histjpe25_pay_fef6849efb5cda3c', 'treatment', '4th Payment', '4th Payment', 'histjpe25_9b59497c40762b8e', '6296182593', 'Jalpaiguri', 'RAJIB BISWAS',
  '2025-11-08', '12000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-08T10:18:00.000Z', '2025-11-08T10:18:00.000Z'
),
(
  'histjpe25_pay_7c4cfa17dd52e27f', 'treatment', '5th Payment', '5th Payment', 'histjpe25_9b59497c40762b8e', '6296182593', 'Jalpaiguri', 'RAJIB BISWAS',
  '2025-11-11', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-11T10:19:00.000Z', '2025-11-11T10:19:00.000Z'
),
(
  'histjpe25_pay_1ce8af154b98b299', 'treatment', '6th Payment', '6th Payment', 'histjpe25_9b59497c40762b8e', '6296182593', 'Jalpaiguri', 'RAJIB BISWAS',
  '2025-11-15', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:20:00.000Z', '2025-11-15T10:20:00.000Z'
),
(
  'histjpe25_pay_c329a106591f140b', 'treatment', '7th Payment', '7th Payment', 'histjpe25_9b59497c40762b8e', '6296182593', 'Jalpaiguri', 'RAJIB BISWAS',
  '2025-11-22', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-22T10:21:00.000Z', '2025-11-22T10:21:00.000Z'
),
(
  'histjpe25_pay_836e83b8c709863e', 'treatment', '8th Payment', '8th Payment', 'histjpe25_9b59497c40762b8e', '6296182593', 'Jalpaiguri', 'RAJIB BISWAS',
  '2025-11-29', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-29T10:22:00.000Z', '2025-11-29T10:22:00.000Z'
),
(
  'histjpe25_pay_2e4d9ae6fc8b6e5e', 'treatment', '9th Payment', '9th Payment', 'histjpe25_9b59497c40762b8e', '6296182593', 'Jalpaiguri', 'RAJIB BISWAS',
  '2025-12-13', '10000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:23:00.000Z', '2025-12-13T10:23:00.000Z'
),
(
  'histjpe25_pay_42ef60973af26e1e', 'treatment', 'Advance', 'Advance', 'histjpe25_195023c787324188', '9800244353', 'Jalpaiguri', 'SACHIN ROY',
  '2025-11-15', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-15T10:15:00.000Z', '2025-11-15T10:15:00.000Z'
),
(
  'histjpe25_pay_68b1b1ad91aef2e0', 'treatment', 'Advance', 'Advance', 'histjpe25_e8f0ac0c609bd420', '7001203085', 'Jalpaiguri', 'PRATIVA DHAR',
  '2025-11-23', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-23T10:15:00.000Z', '2025-11-23T10:15:00.000Z'
),
(
  'histjpe25_pay_b7599108d821064d', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_e8f0ac0c609bd420', '7001203085', 'Jalpaiguri', 'PRATIVA DHAR',
  '2025-11-25', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-25T10:16:00.000Z', '2025-11-25T10:16:00.000Z'
),
(
  'histjpe25_pay_558adb47e0d08b49', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_e8f0ac0c609bd420', '7001203085', 'Jalpaiguri', 'PRATIVA DHAR',
  '2025-11-29', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-29T10:17:00.000Z', '2025-11-29T10:17:00.000Z'
),
(
  'histjpe25_pay_cb46c815684dd3c2', 'treatment', '4th Payment', '4th Payment', 'histjpe25_e8f0ac0c609bd420', '7001203085', 'Jalpaiguri', 'PRATIVA DHAR',
  '2025-12-02', '2500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-02T10:18:00.000Z', '2025-12-02T10:18:00.000Z'
),
(
  'histjpe25_pay_ed55b33c95364f08', 'treatment', '5th Payment', '5th Payment', 'histjpe25_e8f0ac0c609bd420', '7001203085', 'Jalpaiguri', 'PRATIVA DHAR',
  '2025-12-06', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-06T10:19:00.000Z', '2025-12-06T10:19:00.000Z'
),
(
  'histjpe25_pay_c8881576707fea5d', 'treatment', '6th Payment', '6th Payment', 'histjpe25_e8f0ac0c609bd420', '7001203085', 'Jalpaiguri', 'PRATIVA DHAR',
  '2025-12-09', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-09T10:20:00.000Z', '2025-12-09T10:20:00.000Z'
),
(
  'histjpe25_pay_37078ba28924b84f', 'treatment', '7th Payment', '7th Payment', 'histjpe25_e8f0ac0c609bd420', '7001203085', 'Jalpaiguri', 'PRATIVA DHAR',
  '2025-12-13', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:21:00.000Z', '2025-12-13T10:21:00.000Z'
),
(
  'histjpe25_pay_c550462a39f50ad6', 'treatment', '8th Payment', '8th Payment', 'histjpe25_e8f0ac0c609bd420', '7001203085', 'Jalpaiguri', 'PRATIVA DHAR',
  '2026-01-02', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-02T10:22:00.000Z', '2026-01-02T10:22:00.000Z'
),
(
  'histjpe25_pay_7f21796bbb3515ee', 'treatment', '9th Payment', '9th Payment', 'histjpe25_e8f0ac0c609bd420', '7001203085', 'Jalpaiguri', 'PRATIVA DHAR',
  '2026-01-17', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-17T10:23:00.000Z', '2026-01-17T10:23:00.000Z'
),
(
  'histjpe25_pay_e362ed2a9f247d5a', 'treatment', 'Advance', 'Advance', 'histjpe25_04c58395cda59057', '9749984663', 'Jalpaiguri', 'MONIRUL ALAM',
  '2025-11-25', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-25T10:15:00.000Z', '2025-11-25T10:15:00.000Z'
),
(
  'histjpe25_pay_b815d285dbb4b553', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_04c58395cda59057', '9749984663', 'Jalpaiguri', 'MONIRUL ALAM',
  '2025-11-29', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-29T10:16:00.000Z', '2025-11-29T10:16:00.000Z'
),
(
  'histjpe25_pay_1e6385db957ae70f', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_04c58395cda59057', '9749984663', 'Jalpaiguri', 'MONIRUL ALAM',
  '2025-12-06', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-06T10:17:00.000Z', '2025-12-06T10:17:00.000Z'
),
(
  'histjpe25_pay_99541d707d09bacc', 'treatment', '4th Payment', '4th Payment', 'histjpe25_04c58395cda59057', '9749984663', 'Jalpaiguri', 'MONIRUL ALAM',
  '2025-12-09', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-09T10:18:00.000Z', '2025-12-09T10:18:00.000Z'
),
(
  'histjpe25_pay_954a4ac04e0e97bc', 'treatment', '5th Payment', '5th Payment', 'histjpe25_04c58395cda59057', '9749984663', 'Jalpaiguri', 'MONIRUL ALAM',
  '2025-12-13', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:19:00.000Z', '2025-12-13T10:19:00.000Z'
),
(
  'histjpe25_pay_b4dc95dd7f34ef44', 'treatment', '6th Payment', '6th Payment', 'histjpe25_04c58395cda59057', '9749984663', 'Jalpaiguri', 'MONIRUL ALAM',
  '2025-12-20', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-20T10:20:00.000Z', '2025-12-20T10:20:00.000Z'
),
(
  'histjpe25_pay_21e32dac5d0bac8a', 'treatment', '7th Payment', '7th Payment', 'histjpe25_04c58395cda59057', '9749984663', 'Jalpaiguri', 'MONIRUL ALAM',
  '2025-12-23', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-23T10:21:00.000Z', '2025-12-23T10:21:00.000Z'
),
(
  'histjpe25_pay_518d106e29637344', 'treatment', '8th Payment', '8th Payment', 'histjpe25_04c58395cda59057', '9749984663', 'Jalpaiguri', 'MONIRUL ALAM',
  '2025-12-27', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:22:00.000Z', '2025-12-27T10:22:00.000Z'
),
(
  'histjpe25_pay_0974107c0c78671b', 'treatment', '9th Payment', '9th Payment', 'histjpe25_04c58395cda59057', '9749984663', 'Jalpaiguri', 'MONIRUL ALAM',
  '2026-01-13', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-13T10:23:00.000Z', '2026-01-13T10:23:00.000Z'
),
(
  'histjpe25_pay_072a1b4efd43a346', 'treatment', '10th Payment', '10th Payment', 'histjpe25_04c58395cda59057', '9749984663', 'Jalpaiguri', 'MONIRUL ALAM',
  '2026-01-31', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-31T10:24:00.000Z', '2026-01-31T10:24:00.000Z'
),
(
  'histjpe25_pay_c5bf1777ecd26fc7', 'treatment', 'Advance', 'Advance', 'histjpe25_b45bad482a5a7a92', '8101805878', 'Jalpaiguri', 'BISHU ROY',
  '2025-11-28', '6000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-28T10:15:00.000Z', '2025-11-28T10:15:00.000Z'
),
(
  'histjpe25_pay_9ca69553e0b019e8', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_b45bad482a5a7a92', '8101805878', 'Jalpaiguri', 'BISHU ROY',
  '2025-11-29', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-11-29T10:16:00.000Z', '2025-11-29T10:16:00.000Z'
),
(
  'histjpe25_pay_f131e9079b4d2f00', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_b45bad482a5a7a92', '8101805878', 'Jalpaiguri', 'BISHU ROY',
  '2025-12-02', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-02T10:17:00.000Z', '2025-12-02T10:17:00.000Z'
),
(
  'histjpe25_pay_fff7f741be0a208d', 'treatment', '4th Payment', '4th Payment', 'histjpe25_b45bad482a5a7a92', '8101805878', 'Jalpaiguri', 'BISHU ROY',
  '2025-12-06', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-06T10:18:00.000Z', '2025-12-06T10:18:00.000Z'
),
(
  'histjpe25_pay_cb136bad95a3e0cb', 'treatment', '5th Payment', '5th Payment', 'histjpe25_b45bad482a5a7a92', '8101805878', 'Jalpaiguri', 'BISHU ROY',
  '2025-12-09', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-09T10:19:00.000Z', '2025-12-09T10:19:00.000Z'
),
(
  'histjpe25_pay_d5556a223ad6fb60', 'treatment', '6th Payment', '6th Payment', 'histjpe25_b45bad482a5a7a92', '8101805878', 'Jalpaiguri', 'BISHU ROY',
  '2025-12-13', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:20:00.000Z', '2025-12-13T10:20:00.000Z'
),
(
  'histjpe25_pay_93ff45f3f0d2fa32', 'treatment', '7th Payment', '7th Payment', 'histjpe25_b45bad482a5a7a92', '8101805878', 'Jalpaiguri', 'BISHU ROY',
  '2025-12-16', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-16T10:21:00.000Z', '2025-12-16T10:21:00.000Z'
),
(
  'histjpe25_pay_a8ec9ccc90c26cfe', 'treatment', '8th Payment', '8th Payment', 'histjpe25_b45bad482a5a7a92', '8101805878', 'Jalpaiguri', 'BISHU ROY',
  '2025-12-20', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-20T10:22:00.000Z', '2025-12-20T10:22:00.000Z'
),
(
  'histjpe25_pay_28cd258479e42f05', 'treatment', '9th Payment', '9th Payment', 'histjpe25_b45bad482a5a7a92', '8101805878', 'Jalpaiguri', 'BISHU ROY',
  '2025-12-23', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-23T10:23:00.000Z', '2025-12-23T10:23:00.000Z'
),
(
  'histjpe25_pay_5e5ba1459f89b872', 'treatment', '10th Payment', '10th Payment', 'histjpe25_b45bad482a5a7a92', '8101805878', 'Jalpaiguri', 'BISHU ROY',
  '2025-12-27', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:24:00.000Z', '2025-12-27T10:24:00.000Z'
),
(
  'histjpe25_pay_9ef8d9ba01c61bc0', 'treatment', 'Advance', 'Advance', 'histjpe25_7fbc3f5f6aad0086', '8388939713', 'Jalpaiguri', 'PAPON ROY',
  '2025-12-02', '600', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-02T10:15:00.000Z', '2025-12-02T10:15:00.000Z'
),
(
  'histjpe25_pay_ec840fea94bf8558', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_7fbc3f5f6aad0086', '8388939713', 'Jalpaiguri', 'PAPON ROY',
  '2025-12-06', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-06T10:16:00.000Z', '2025-12-06T10:16:00.000Z'
),
(
  'histjpe25_pay_b660c858351da1ab', 'treatment', 'Advance', 'Advance', 'histjpe25_a9ea47611e0cd849', '8921170584', 'Jalpaiguri', 'UTTAM MANDAL',
  '2025-12-02', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-02T10:15:00.000Z', '2025-12-02T10:15:00.000Z'
),
(
  'histjpe25_pay_2b1a205cb01edd5e', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_a9ea47611e0cd849', '8921170584', 'Jalpaiguri', 'UTTAM MANDAL',
  '2025-12-06', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-06T10:16:00.000Z', '2025-12-06T10:16:00.000Z'
),
(
  'histjpe25_pay_d36c3f664c0246cf', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_a9ea47611e0cd849', '8921170584', 'Jalpaiguri', 'UTTAM MANDAL',
  '2025-12-09', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-09T10:17:00.000Z', '2025-12-09T10:17:00.000Z'
),
(
  'histjpe25_pay_ff32e640179534dd', 'treatment', '4th Payment', '4th Payment', 'histjpe25_a9ea47611e0cd849', '8921170584', 'Jalpaiguri', 'UTTAM MANDAL',
  '2025-12-13', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:18:00.000Z', '2025-12-13T10:18:00.000Z'
),
(
  'histjpe25_pay_f111e804ef8073de', 'treatment', '5th Payment', '5th Payment', 'histjpe25_a9ea47611e0cd849', '8921170584', 'Jalpaiguri', 'UTTAM MANDAL',
  '2025-12-16', '8000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-16T10:19:00.000Z', '2025-12-16T10:19:00.000Z'
),
(
  'histjpe25_pay_c953e12916cab37e', 'treatment', '6th Payment', '6th Payment', 'histjpe25_a9ea47611e0cd849', '8921170584', 'Jalpaiguri', 'UTTAM MANDAL',
  '2025-12-20', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-20T10:20:00.000Z', '2025-12-20T10:20:00.000Z'
),
(
  'histjpe25_pay_beae334f6bfc9a89', 'treatment', '7th Payment', '7th Payment', 'histjpe25_a9ea47611e0cd849', '8921170584', 'Jalpaiguri', 'UTTAM MANDAL',
  '2025-12-23', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-23T10:21:00.000Z', '2025-12-23T10:21:00.000Z'
),
(
  'histjpe25_pay_ebe6ee6dfab2ff23', 'treatment', '8th Payment', '8th Payment', 'histjpe25_a9ea47611e0cd849', '8921170584', 'Jalpaiguri', 'UTTAM MANDAL',
  '2025-12-27', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:22:00.000Z', '2025-12-27T10:22:00.000Z'
),
(
  'histjpe25_pay_d8a954c1c2eceb46', 'treatment', '9th Payment', '9th Payment', 'histjpe25_a9ea47611e0cd849', '8921170584', 'Jalpaiguri', 'UTTAM MANDAL',
  '2026-01-02', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-02T10:23:00.000Z', '2026-01-02T10:23:00.000Z'
),
(
  'histjpe25_pay_56ef3033b654c926', 'treatment', 'Advance', 'Advance', 'histjpe25_9ef43bb18c91163c', '7679693511', 'Jalpaiguri', 'ANJU BEGAM',
  '2025-12-09', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-09T10:15:00.000Z', '2025-12-09T10:15:00.000Z'
),
(
  'histjpe25_pay_0b9c9fcd79c9aa8a', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_9ef43bb18c91163c', '7679693511', 'Jalpaiguri', 'ANJU BEGAM',
  '2025-12-13', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:16:00.000Z', '2025-12-13T10:16:00.000Z'
),
(
  'histjpe25_pay_ea91d6a6e553051e', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_9ef43bb18c91163c', '7679693511', 'Jalpaiguri', 'ANJU BEGAM',
  '2025-12-16', '10000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-16T10:17:00.000Z', '2025-12-16T10:17:00.000Z'
),
(
  'histjpe25_pay_b7c268d07fa18f0c', 'treatment', '4th Payment', '4th Payment', 'histjpe25_9ef43bb18c91163c', '7679693511', 'Jalpaiguri', 'ANJU BEGAM',
  '2025-12-20', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-20T10:18:00.000Z', '2025-12-20T10:18:00.000Z'
),
(
  'histjpe25_pay_288e9a796fb5587d', 'treatment', '5th Payment', '5th Payment', 'histjpe25_9ef43bb18c91163c', '7679693511', 'Jalpaiguri', 'ANJU BEGAM',
  '2025-12-23', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-23T10:19:00.000Z', '2025-12-23T10:19:00.000Z'
),
(
  'histjpe25_pay_041b27415c921431', 'treatment', '6th Payment', '6th Payment', 'histjpe25_9ef43bb18c91163c', '7679693511', 'Jalpaiguri', 'ANJU BEGAM',
  '2026-01-02', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-02T10:20:00.000Z', '2026-01-02T10:20:00.000Z'
),
(
  'histjpe25_pay_04b2c6a2226bae10', 'treatment', '7th Payment', '7th Payment', 'histjpe25_9ef43bb18c91163c', '7679693511', 'Jalpaiguri', 'ANJU BEGAM',
  '2026-01-13', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-13T10:21:00.000Z', '2026-01-13T10:21:00.000Z'
),
(
  'histjpe25_pay_cb5f88e009ffbaac', 'treatment', 'Advance', 'Advance', 'histjpe25_32cb0178bd9e6b7b', '9832236466', 'Jalpaiguri', 'MD UJIR',
  '2025-12-08', '900', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-08T10:15:00.000Z', '2025-12-08T10:15:00.000Z'
),
(
  'histjpe25_pay_3ccbff794748a4e6', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_32cb0178bd9e6b7b', '9832236466', 'Jalpaiguri', 'MD UJIR',
  '2025-12-09', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-09T10:16:00.000Z', '2025-12-09T10:16:00.000Z'
),
(
  'histjpe25_pay_b102e2b1b13b189d', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_32cb0178bd9e6b7b', '9832236466', 'Jalpaiguri', 'MD UJIR',
  '2025-12-16', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-16T10:17:00.000Z', '2025-12-16T10:17:00.000Z'
),
(
  'histjpe25_pay_4d470802ca7758d9', 'treatment', '4th Payment', '4th Payment', 'histjpe25_32cb0178bd9e6b7b', '9832236466', 'Jalpaiguri', 'MD UJIR',
  '2025-12-20', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-20T10:18:00.000Z', '2025-12-20T10:18:00.000Z'
),
(
  'histjpe25_pay_fdc2d3b55699d02f', 'treatment', '5th Payment', '5th Payment', 'histjpe25_32cb0178bd9e6b7b', '9832236466', 'Jalpaiguri', 'MD UJIR',
  '2025-12-23', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-23T10:19:00.000Z', '2025-12-23T10:19:00.000Z'
),
(
  'histjpe25_pay_e96327a679ba7a2d', 'treatment', '6th Payment', '6th Payment', 'histjpe25_32cb0178bd9e6b7b', '9832236466', 'Jalpaiguri', 'MD UJIR',
  '2025-12-27', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:20:00.000Z', '2025-12-27T10:20:00.000Z'
),
(
  'histjpe25_pay_0213277f19f96449', 'treatment', '7th Payment', '7th Payment', 'histjpe25_32cb0178bd9e6b7b', '9832236466', 'Jalpaiguri', 'MD UJIR',
  '2025-12-30', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-30T10:21:00.000Z', '2025-12-30T10:21:00.000Z'
),
(
  'histjpe25_pay_9d99efee3ae31961', 'treatment', '8th Payment', '8th Payment', 'histjpe25_32cb0178bd9e6b7b', '9832236466', 'Jalpaiguri', 'MD UJIR',
  '2026-01-02', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-02T10:22:00.000Z', '2026-01-02T10:22:00.000Z'
),
(
  'histjpe25_pay_a335ce343b89f8bd', 'treatment', '9th Payment', '9th Payment', 'histjpe25_32cb0178bd9e6b7b', '9832236466', 'Jalpaiguri', 'MD UJIR',
  '2026-01-05', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-05T10:23:00.000Z', '2026-01-05T10:23:00.000Z'
),
(
  'histjpe25_pay_24c508fda8a14116', 'treatment', '10th Payment', '10th Payment', 'histjpe25_32cb0178bd9e6b7b', '9832236466', 'Jalpaiguri', 'MD UJIR',
  '2026-01-10', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-10T10:24:00.000Z', '2026-01-10T10:24:00.000Z'
),
(
  'histjpe25_pay_28b42856a11fd6aa', 'treatment', '11th Payment', '11th Payment', 'histjpe25_32cb0178bd9e6b7b', '9832236466', 'Jalpaiguri', 'MD UJIR',
  '2026-01-13', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-13T10:25:00.000Z', '2026-01-13T10:25:00.000Z'
),
(
  'histjpe25_pay_bb4a956bf641203d', 'treatment', '12th Payment', '12th Payment', 'histjpe25_32cb0178bd9e6b7b', '9832236466', 'Jalpaiguri', 'MD UJIR',
  '2026-01-17', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-17T10:26:00.000Z', '2026-01-17T10:26:00.000Z'
),
(
  'histjpe25_pay_29fac7428119951e', 'treatment', 'Advance', 'Advance', 'histjpe25_25d9a6b992a29637', '8695590721', 'Jalpaiguri', 'INDU HR',
  '2025-12-12', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-12T10:15:00.000Z', '2025-12-12T10:15:00.000Z'
),
(
  'histjpe25_pay_6ef59e471a7910aa', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_25d9a6b992a29637', '8695590721', 'Jalpaiguri', 'INDU HR',
  '2025-12-20', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-20T10:16:00.000Z', '2025-12-20T10:16:00.000Z'
),
(
  'histjpe25_pay_c985cc646e392c3e', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_25d9a6b992a29637', '8695590721', 'Jalpaiguri', 'INDU HR',
  '2025-12-30', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-30T10:17:00.000Z', '2025-12-30T10:17:00.000Z'
),
(
  'histjpe25_pay_fe6e0889d02f9f42', 'treatment', '4th Payment', '4th Payment', 'histjpe25_25d9a6b992a29637', '8695590721', 'Jalpaiguri', 'INDU HR',
  '2026-01-02', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-02T10:18:00.000Z', '2026-01-02T10:18:00.000Z'
),
(
  'histjpe25_pay_0203247d74d13002', 'treatment', '5th Payment', '5th Payment', 'histjpe25_25d9a6b992a29637', '8695590721', 'Jalpaiguri', 'INDU HR',
  '2026-01-10', '4500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-10T10:19:00.000Z', '2026-01-10T10:19:00.000Z'
),
(
  'histjpe25_pay_db4d45f0b78da30e', 'treatment', '6th Payment', '6th Payment', 'histjpe25_25d9a6b992a29637', '8695590721', 'Jalpaiguri', 'INDU HR',
  '2026-01-17', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-17T10:20:00.000Z', '2026-01-17T10:20:00.000Z'
),
(
  'histjpe25_pay_661ee6dba0ce88c4', 'treatment', '7th Payment', '7th Payment', 'histjpe25_25d9a6b992a29637', '8695590721', 'Jalpaiguri', 'INDU HR',
  '2026-01-24', '3500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-24T10:21:00.000Z', '2026-01-24T10:21:00.000Z'
),
(
  'histjpe25_pay_543496896bba62d5', 'treatment', 'Advance', 'Advance', 'histjpe25_89a203fdeeb49755', '9401865077', 'Jalpaiguri', 'NARAYAN ROY',
  '2025-12-13', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:15:00.000Z', '2025-12-13T10:15:00.000Z'
),
(
  'histjpe25_pay_035b9962d1abf696', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_89a203fdeeb49755', '9401865077', 'Jalpaiguri', 'NARAYAN ROY',
  '2025-12-16', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-16T10:16:00.000Z', '2025-12-16T10:16:00.000Z'
),
(
  'histjpe25_pay_435539c084c4f457', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_89a203fdeeb49755', '9401865077', 'Jalpaiguri', 'NARAYAN ROY',
  '2025-12-20', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-20T10:17:00.000Z', '2025-12-20T10:17:00.000Z'
),
(
  'histjpe25_pay_cdbb21d11c3b4864', 'treatment', '4th Payment', '4th Payment', 'histjpe25_89a203fdeeb49755', '9401865077', 'Jalpaiguri', 'NARAYAN ROY',
  '2025-12-23', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-23T10:18:00.000Z', '2025-12-23T10:18:00.000Z'
),
(
  'histjpe25_pay_7af227655b82fb4f', 'treatment', '5th Payment', '5th Payment', 'histjpe25_89a203fdeeb49755', '9401865077', 'Jalpaiguri', 'NARAYAN ROY',
  '2025-12-27', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:19:00.000Z', '2025-12-27T10:19:00.000Z'
),
(
  'histjpe25_pay_afb29041d5a1f867', 'treatment', '6th Payment', '6th Payment', 'histjpe25_89a203fdeeb49755', '9401865077', 'Jalpaiguri', 'NARAYAN ROY',
  '2026-01-24', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-24T10:20:00.000Z', '2026-01-24T10:20:00.000Z'
),
(
  'histjpe25_pay_60293cae38d170f2', 'treatment', '7th Payment', '7th Payment', 'histjpe25_89a203fdeeb49755', '9401865077', 'Jalpaiguri', 'NARAYAN ROY',
  '2026-01-05', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-05T10:21:00.000Z', '2026-01-05T10:21:00.000Z'
),
(
  'histjpe25_pay_d4c9464ae00151c6', 'treatment', 'Advance', 'Advance', 'histjpe25_52d0ff504833fc95', '9378149491', 'Jalpaiguri', 'NIRANJAN MANDAL',
  '2025-12-13', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:15:00.000Z', '2025-12-13T10:15:00.000Z'
),
(
  'histjpe25_pay_b35ba05ba2fefabd', 'treatment', 'Advance', 'Advance', 'histjpe25_d162c7cdf458f6d7', '9832765330', 'Jalpaiguri', 'SANJIT ROY',
  '2025-12-13', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-13T10:15:00.000Z', '2025-12-13T10:15:00.000Z'
),
(
  'histjpe25_pay_2e9ee7965f0052e6', 'treatment', 'Advance', 'Advance', 'histjpe25_407d3550090ef7c6', '6295821544', 'Jalpaiguri', 'TAPAS MANDAL',
  '2025-12-16', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-16T10:15:00.000Z', '2025-12-16T10:15:00.000Z'
),
(
  'histjpe25_pay_dd5351009ac9b696', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_407d3550090ef7c6', '6295821544', 'Jalpaiguri', 'TAPAS MANDAL',
  '2025-12-20', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-20T10:16:00.000Z', '2025-12-20T10:16:00.000Z'
),
(
  'histjpe25_pay_d9675c257ed9b99d', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_407d3550090ef7c6', '6295821544', 'Jalpaiguri', 'TAPAS MANDAL',
  '2025-12-23', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-23T10:17:00.000Z', '2025-12-23T10:17:00.000Z'
),
(
  'histjpe25_pay_de20151053ea5ecd', 'treatment', '4th Payment', '4th Payment', 'histjpe25_407d3550090ef7c6', '6295821544', 'Jalpaiguri', 'TAPAS MANDAL',
  '2025-12-27', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:18:00.000Z', '2025-12-27T10:18:00.000Z'
),
(
  'histjpe25_pay_9db9ec5511c62dbb', 'treatment', '5th Payment', '5th Payment', 'histjpe25_407d3550090ef7c6', '6295821544', 'Jalpaiguri', 'TAPAS MANDAL',
  '2025-12-30', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-30T10:19:00.000Z', '2025-12-30T10:19:00.000Z'
),
(
  'histjpe25_pay_2469e353001d5563', 'treatment', '6th Payment', '6th Payment', 'histjpe25_407d3550090ef7c6', '6295821544', 'Jalpaiguri', 'TAPAS MANDAL',
  '2026-01-02', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-02T10:20:00.000Z', '2026-01-02T10:20:00.000Z'
),
(
  'histjpe25_pay_881c2ceb849354ce', 'treatment', '7th Payment', '7th Payment', 'histjpe25_407d3550090ef7c6', '6295821544', 'Jalpaiguri', 'TAPAS MANDAL',
  '2026-01-10', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-10T10:21:00.000Z', '2026-01-10T10:21:00.000Z'
),
(
  'histjpe25_pay_c7234bd0a906871e', 'treatment', 'Advance', 'Advance', 'histjpe25_da1dcaa4abc35624', '9635465400', 'Jalpaiguri', 'SAHADAT ALI',
  '2025-12-27', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:15:00.000Z', '2025-12-27T10:15:00.000Z'
),
(
  'histjpe25_pay_eb776bfa5c340c44', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_da1dcaa4abc35624', '9635465400', 'Jalpaiguri', 'SAHADAT ALI',
  '2025-12-30', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-30T10:16:00.000Z', '2025-12-30T10:16:00.000Z'
),
(
  'histjpe25_pay_96f6ba71aced9308', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_da1dcaa4abc35624', '9635465400', 'Jalpaiguri', 'SAHADAT ALI',
  '2026-01-02', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-02T10:17:00.000Z', '2026-01-02T10:17:00.000Z'
),
(
  'histjpe25_pay_1abc9fcf546dffda', 'treatment', '4th Payment', '4th Payment', 'histjpe25_da1dcaa4abc35624', '9635465400', 'Jalpaiguri', 'SAHADAT ALI',
  '2026-01-05', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-05T10:18:00.000Z', '2026-01-05T10:18:00.000Z'
),
(
  'histjpe25_pay_2d7f02ac8343bd5a', 'treatment', '5th Payment', '5th Payment', 'histjpe25_da1dcaa4abc35624', '9635465400', 'Jalpaiguri', 'SAHADAT ALI',
  '2026-01-10', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-10T10:19:00.000Z', '2026-01-10T10:19:00.000Z'
),
(
  'histjpe25_pay_245f7145f8b95344', 'treatment', '6th Payment', '6th Payment', 'histjpe25_da1dcaa4abc35624', '9635465400', 'Jalpaiguri', 'SAHADAT ALI',
  '2026-01-17', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-17T10:20:00.000Z', '2026-01-17T10:20:00.000Z'
),
(
  'histjpe25_pay_2bc4104f1f33d600', 'treatment', '7th Payment', '7th Payment', 'histjpe25_da1dcaa4abc35624', '9635465400', 'Jalpaiguri', 'SAHADAT ALI',
  '2026-01-20', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-20T10:21:00.000Z', '2026-01-20T10:21:00.000Z'
),
(
  'histjpe25_pay_b9d836fc7744ee7f', 'treatment', '8th Payment', '8th Payment', 'histjpe25_da1dcaa4abc35624', '9635465400', 'Jalpaiguri', 'SAHADAT ALI',
  '2026-01-24', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-24T10:22:00.000Z', '2026-01-24T10:22:00.000Z'
),
(
  'histjpe25_pay_45a8159de007fb53', 'treatment', '9th Payment', '9th Payment', 'histjpe25_da1dcaa4abc35624', '9635465400', 'Jalpaiguri', 'SAHADAT ALI',
  '2026-02-07', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-07T10:23:00.000Z', '2026-02-07T10:23:00.000Z'
),
(
  'histjpe25_pay_e7f1ec722678d7de', 'treatment', '10th Payment', '10th Payment', 'histjpe25_da1dcaa4abc35624', '9635465400', 'Jalpaiguri', 'SAHADAT ALI',
  '2026-02-21', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-21T10:24:00.000Z', '2026-02-21T10:24:00.000Z'
),
(
  'histjpe25_pay_7d99d9a891d1f488', 'treatment', 'Advance', 'Advance', 'histjpe25_6a0d0900bbd7f13c', '8116734967', 'Jalpaiguri', 'BIDHAN CH. SING',
  '2025-12-27', '15000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:15:00.000Z', '2025-12-27T10:15:00.000Z'
),
(
  'histjpe25_pay_84e6cb40ca527a98', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_6a0d0900bbd7f13c', '8116734967', 'Jalpaiguri', 'BIDHAN CH. SING',
  '2026-01-02', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-02T10:16:00.000Z', '2026-01-02T10:16:00.000Z'
),
(
  'histjpe25_pay_f52f28dec07e8986', 'treatment', 'Advance', 'Advance', 'histjpe25_49c9b42a2a9b8203', '8348640351', 'Jalpaiguri', 'ANIKA ROY',
  '2025-12-30', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-30T10:15:00.000Z', '2025-12-30T10:15:00.000Z'
),
(
  'histjpe25_pay_e05b4e7de05ab56f', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_49c9b42a2a9b8203', '8348640351', 'Jalpaiguri', 'ANIKA ROY',
  '2026-01-05', '4000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-05T10:16:00.000Z', '2026-01-05T10:16:00.000Z'
),
(
  'histjpe25_pay_0a175fc13dd2dbd1', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_49c9b42a2a9b8203', '8348640351', 'Jalpaiguri', 'ANIKA ROY',
  '2026-01-13', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-13T10:17:00.000Z', '2026-01-13T10:17:00.000Z'
),
(
  'histjpe25_pay_71eacd4b6375537b', 'treatment', '4th Payment', '4th Payment', 'histjpe25_49c9b42a2a9b8203', '8348640351', 'Jalpaiguri', 'ANIKA ROY',
  '2026-01-20', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-20T10:18:00.000Z', '2026-01-20T10:18:00.000Z'
),
(
  'histjpe25_pay_c78c26cd5edd930a', 'treatment', '5th Payment', '5th Payment', 'histjpe25_49c9b42a2a9b8203', '8348640351', 'Jalpaiguri', 'ANIKA ROY',
  '2026-01-27', '1500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-27T10:19:00.000Z', '2026-01-27T10:19:00.000Z'
),
(
  'histjpe25_pay_290593be910225be', 'treatment', 'Advance', 'Advance', 'histjpe25_0dd2b2c2bdc293e8', '8348640351', 'Jalpaiguri', 'RINA BARMAN',
  '2025-12-27', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-12-27T10:15:00.000Z', '2025-12-27T10:15:00.000Z'
),
(
  'histjpe25_pay_b9cf1a7e41fceeb7', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_0dd2b2c2bdc293e8', '8348640351', 'Jalpaiguri', 'RINA BARMAN',
  '2026-01-02', '500', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-02T10:16:00.000Z', '2026-01-02T10:16:00.000Z'
),
(
  'histjpe25_pay_63afc118790de2d7', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_0dd2b2c2bdc293e8', '8348640351', 'Jalpaiguri', 'RINA BARMAN',
  '2026-01-10', '10000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-10T10:17:00.000Z', '2026-01-10T10:17:00.000Z'
),
(
  'histjpe25_pay_a08dcd7da620f620', 'treatment', '4th Payment', '4th Payment', 'histjpe25_0dd2b2c2bdc293e8', '8348640351', 'Jalpaiguri', 'RINA BARMAN',
  '2026-01-13', '100', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-13T10:18:00.000Z', '2026-01-13T10:18:00.000Z'
),
(
  'histjpe25_pay_d107f3875f84da8e', 'treatment', '5th Payment', '5th Payment', 'histjpe25_0dd2b2c2bdc293e8', '8348640351', 'Jalpaiguri', 'RINA BARMAN',
  '2026-01-20', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-20T10:19:00.000Z', '2026-01-20T10:19:00.000Z'
),
(
  'histjpe25_pay_cac9f05bf55981ea', 'treatment', '6th Payment', '6th Payment', 'histjpe25_0dd2b2c2bdc293e8', '8348640351', 'Jalpaiguri', 'RINA BARMAN',
  '2026-01-24', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-24T10:20:00.000Z', '2026-01-24T10:20:00.000Z'
),
(
  'histjpe25_pay_15c6ed0f1d6bddff', 'treatment', '7th Payment', '7th Payment', 'histjpe25_0dd2b2c2bdc293e8', '8348640351', 'Jalpaiguri', 'RINA BARMAN',
  '2026-01-27', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-27T10:21:00.000Z', '2026-01-27T10:21:00.000Z'
),
(
  'histjpe25_pay_05f39f88166e7742', 'treatment', '8th Payment', '8th Payment', 'histjpe25_0dd2b2c2bdc293e8', '8348640351', 'Jalpaiguri', 'RINA BARMAN',
  '2025-02-03', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2025-02-03T10:22:00.000Z', '2025-02-03T10:22:00.000Z'
),
(
  'histjpe25_pay_da352b0f7737c887', 'treatment', '9th Payment', '9th Payment', 'histjpe25_0dd2b2c2bdc293e8', '8348640351', 'Jalpaiguri', 'RINA BARMAN',
  '2026-02-03', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-03T10:23:00.000Z', '2026-02-03T10:23:00.000Z'
),
(
  'histjpe25_pay_e28fe6fa12dcd9bd', 'treatment', 'Advance', 'Advance', 'histjpe25_05590ea482349912', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-01-10', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-10T10:15:00.000Z', '2026-01-10T10:15:00.000Z'
),
(
  'histjpe25_pay_4cd2736e0538686f', 'treatment', '2nd Payment', '2nd Payment', 'histjpe25_05590ea482349912', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-01-20', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-20T10:16:00.000Z', '2026-01-20T10:16:00.000Z'
),
(
  'histjpe25_pay_5495c6e709d54056', 'treatment', '3rd Payment', '3rd Payment', 'histjpe25_05590ea482349912', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-01-27', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-01-27T10:17:00.000Z', '2026-01-27T10:17:00.000Z'
),
(
  'histjpe25_pay_bb7923d2c10cd74a', 'treatment', '4th Payment', '4th Payment', 'histjpe25_05590ea482349912', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-02-03', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-03T10:18:00.000Z', '2026-02-03T10:18:00.000Z'
),
(
  'histjpe25_pay_c2b8e80ec34f7250', 'treatment', '5th Payment', '5th Payment', 'histjpe25_05590ea482349912', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-02-07', '2000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-07T10:19:00.000Z', '2026-02-07T10:19:00.000Z'
),
(
  'histjpe25_pay_06debbb35105b82a', 'treatment', '6th Payment', '6th Payment', 'histjpe25_05590ea482349912', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-02-14', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-14T10:20:00.000Z', '2026-02-14T10:20:00.000Z'
),
(
  'histjpe25_pay_6f0d417bb98610cd', 'treatment', '7th Payment', '7th Payment', 'histjpe25_05590ea482349912', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-02-28', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-02-28T10:21:00.000Z', '2026-02-28T10:21:00.000Z'
),
(
  'histjpe25_pay_63786720344d100d', 'treatment', '8th Payment', '8th Payment', 'histjpe25_05590ea482349912', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-03-17', '5000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-03-17T10:22:00.000Z', '2026-03-17T10:22:00.000Z'
),
(
  'histjpe25_pay_52acb7e589a9aa32', 'treatment', '9th Payment', '9th Payment', 'histjpe25_05590ea482349912', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-04-04', '1000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-04-04T10:23:00.000Z', '2026-04-04T10:23:00.000Z'
),
(
  'histjpe25_pay_10ae005009754020', 'treatment', '10th Payment', '10th Payment', 'histjpe25_05590ea482349912', '9647231089', 'Jalpaiguri', 'MINA SARKAR',
  '2026-04-18', '3000', 'CASH', 'Historical import (Jalpaiguri 2025 sheet)',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2026-04-18T10:24:00.000Z', '2026-04-18T10:24:00.000Z'
);
