-- কিশানগঞ্জ ধাপ ৩ -- ব্যাচ 4/5 (রোগী 163-216, মোট 54)
-- ধাপ ২-এর ১০ জনের নিয়মই এখানে। MD ANWAR (8051606386) বাদ (শিটের BILL
-- ঘরেই ভাঙা লেখা)। sex নাম দেখে আন্দাজ। SABIR ALAM নতুন সারি হিসেবেই
-- (লাইভ MD TAHSIR থেকে আলাদা মানুষ বলে TK আগেই নিশ্চিত করেছেন)।

insert into public.patients (
  id, "patientId", date, "registrationDate", "visitDate",
  name, mobile, branch, age, sex,
  address, disease, bill,
  stage, queue, "doctorComplete",
  "createdBy", "registeredBy", "createdAt", "updatedAt"
) values
(
  'hist_f6bdcde62fee7de3', 'KNE-03022024-001', '2024-02-03', '2024-02-03', '2024-02-03',
  'ANKUSH KUMAR', '9548414688', 'Kishanganj', '32', 'Male',
  'BSF CAMP, KHAGRA, KISHANGANJ', 'Other', '17500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-03T10:00:00.000Z', '2024-02-03T10:00:00.000Z'
),
(
  'hist_d4d9f7677d737794', 'KNE-08022024-001', '2024-02-08', '2024-02-08', '2024-02-08',
  'ROSHNI KUMARI', '9341811967', 'Kishanganj', '21', 'Female',
  'KISHANGANJ BAZAR, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-08T10:00:00.000Z', '2024-02-08T10:00:00.000Z'
),
(
  'hist_ab1e5290d9ccdf49', 'KNE-09022024-001', '2024-02-09', '2024-02-09', '2024-02-09',
  'ANANDA SARKAR', '7477796599', 'Kishanganj', '48', 'Male',
  'BSF CAMP, KHAGRA, KISHANGANJ', 'Piles, Fissure', '18500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-09T10:00:00.000Z', '2024-02-09T10:00:00.000Z'
),
(
  'hist_4ee343a07d916d2b', 'KNE-15022024-001', '2024-02-15', '2024-02-15', '2024-02-15',
  'RUKSANA BEGAM', '9800326267', 'Kishanganj', '35', 'Female',
  'SARASWAT, KOIMARI, KISHANGANJ, KISHANGANJ', 'Other', '22500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-15T10:00:00.000Z', '2024-02-15T10:00:00.000Z'
),
(
  'hist_eee2c6562ea7c927', 'KNE-16022024-001', '2024-02-16', '2024-02-16', '2024-02-16',
  'MD TARIQUE ANWAR', '9641228522', 'Kishanganj', '29', 'Male',
  'KAMAT, AMALIA, CHAKULIA, UTTAR DINAJPUR', 'Fistula', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-16T10:00:00.000Z', '2024-02-16T10:00:00.000Z'
),
(
  'hist_e0fd9578a59e0a3f', 'KNE-22022024-001', '2024-02-22', '2024-02-22', '2024-02-22',
  'SUBRATA BISWAS', '8837484884', 'Kishanganj', '34', 'Male',
  'BSF CAMP, KHAGRA, KISHANGANJ', 'Piles', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-22T10:00:00.000Z', '2024-02-22T10:00:00.000Z'
),
(
  'hist_27ca7a4db130265b', 'KNE-24022024-001', '2024-02-24', '2024-02-24', '2024-02-24',
  'AJAY KR MONDAL', '6294678950', 'Kishanganj', '', 'Male',
  'KANKI, NIJAMPUR, UTTAR DINAJPUR', 'Piles, Fissure', '26000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-24T10:00:00.000Z', '2024-02-24T10:00:00.000Z'
),
(
  'hist_ba6d0bd40ba2de46', 'KNE-15032024-001', '2024-03-15', '2024-03-15', '2024-03-15',
  'NURSHED ALAM', '9797535686', 'Kishanganj', '30', 'Male',
  'KANAIYABARI, NISHANDRA, BISHUNPUR, KISHANGANJ', 'Piles', '34000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-15T10:00:00.000Z', '2024-03-15T10:00:00.000Z'
),
(
  'hist_1d25647ccc08ae50', 'KNE-16032024-001', '2024-03-16', '2024-03-16', '2024-03-16',
  'SANGITA SINGH', '8967786752', 'Kishanganj', '40', 'Female',
  'BSF CAMP, KHAGRA, KISHANGANJ, KISHANGANJ', 'Piles', '17500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-16T10:00:00.000Z', '2024-03-16T10:00:00.000Z'
),
(
  'hist_44e9d56cc4294ea5', 'KNE-20032024-001', '2024-03-20', '2024-03-20', '2024-03-20',
  'TANWEER ALAM', '7601992021', 'Kishanganj', '26', 'Male',
  'PANISHAL, SINGHIA, KISHANGANJ, KISHANGANJ', 'Other', '14000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-20T10:00:00.000Z', '2024-03-20T10:00:00.000Z'
),
(
  'hist_20c3995173df4c3d', 'KNE-20032024-002', '2024-03-20', '2024-03-20', '2024-03-20',
  'SIRIKUL ALAM', '8509898792', 'Kishanganj', '10', 'Male',
  'BALANCHA, CHAKULIA, CHAKULIA, UTTAR DINAJPUR', 'Piles', '17000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-20T10:00:00.000Z', '2024-03-20T10:00:00.000Z'
),
(
  'hist_114cda8482ae9ecc', 'KNE-21032024-001', '2024-03-21', '2024-03-21', '2024-03-21',
  'SAHEDA KHATOON', '9608611304', 'Kishanganj', '29', 'Female',
  'GACHPARA, GACHPARA, KISHANGANJ, KISHANGANJ', 'Piles', '1600',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-21T10:00:00.000Z', '2024-03-21T10:00:00.000Z'
),
(
  'hist_22b090351341f452', 'KNE-23032024-001', '2024-03-23', '2024-03-23', '2024-03-23',
  'MD ALAM', '8695722505', 'Kishanganj', '32', 'Male',
  'SATBARI, HATKHOLA', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-23T10:00:00.000Z', '2024-03-23T10:00:00.000Z'
),
(
  'hist_bb1a94e05e41f30e', 'KNE-23032024-002', '2024-03-23', '2024-03-23', '2024-03-23',
  'MIR TAUFIQ', '8809259608', 'Kishanganj', '21', 'Male',
  'KALAMALA, BELWA, BELWA, KISHANGANJ', 'Piles, Fissure', '21000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-23T10:00:00.000Z', '2024-03-23T10:00:00.000Z'
),
(
  'hist_747042391a9dc26d', 'KNE-28032024-001', '2024-03-28', '2024-03-28', '2024-03-28',
  'SANAT THAKUR', '8809017397', 'Kishanganj', '29', 'Male',
  'CHOND, BARSOI', 'Piles', '23500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-28T10:00:00.000Z', '2024-03-28T10:00:00.000Z'
),
(
  'hist_6b567cda26c5113e', 'KNE-01042024-001', '2024-04-01', '2024-04-01', '2024-04-01',
  'APISAR ALAM', '9523986880', 'Kishanganj', '27', 'Male',
  'BISHUNPUR, KANAIYABARI, KISHANGANJ', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-01T10:00:00.000Z', '2024-04-01T10:00:00.000Z'
),
(
  'hist_b801dd5db91c68c3', 'KNE-04042024-001', '2024-04-04', '2024-04-04', '2024-04-04',
  'RASAMOY BALA', '9647826939', 'Kishanganj', '38', 'Female',
  'HATDUPUKHUR, SAHAPUR, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-04T10:00:00.000Z', '2024-04-04T10:00:00.000Z'
),
(
  'hist_6933a939d7ced6f0', 'KNE-06042024-001', '2024-04-06', '2024-04-06', '2024-04-06',
  'ABAR ALAM', '6354972664', 'Kishanganj', '36', 'Male',
  'TULSIA KHARIBASTI, BAHADURGANJ, BAHADURGANJ, KISHANGANJ', 'Piles', '16000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-06T10:00:00.000Z', '2024-04-06T10:00:00.000Z'
),
(
  'hist_d4cbb0c8c433b40b', 'KNE-10042024-001', '2024-04-10', '2024-04-10', '2024-04-10',
  'NIRMAL HEMRAO', '7384037086', 'Kishanganj', '22', 'Male',
  'CHOWKA, GALIA, CHAKULIA, UTTAR DINAJPUR', 'Fistula', '43000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-10T10:00:00.000Z', '2024-04-10T10:00:00.000Z'
),
(
  'hist_004f66bbb8040fcb', 'KNE-12042024-001', '2024-04-12', '2024-04-12', '2024-04-12',
  'MD SOURAV', '7763933435', 'Kishanganj', '43', 'Male',
  'KHAYAVITA, NIJAGACHI, THAKURGANJ, KISHANGANJ', 'Fistula', '90000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-12T10:00:00.000Z', '2024-04-12T10:00:00.000Z'
),
(
  'hist_2e61903ed6547bcf', 'KNE-19042024-001', '2024-04-19', '2024-04-19', '2024-04-19',
  'SAKILA', '7367902448', 'Kishanganj', '18', 'Female',
  'MALDUAR, KISHANGANG, KISHANGANJ, KISHANGANJ', 'Piles', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-19T10:00:00.000Z', '2024-04-19T10:00:00.000Z'
),
(
  'hist_71084cb54f1a6f33', 'KNE-22042024-001', '2024-04-22', '2024-04-22', '2024-04-22',
  'NITISH SINGH', '7908686830', 'Kishanganj', '25', 'Male',
  'LADHI, CHAKULIA, CHAKULIYA, UTTAR DINAJPUR', 'Piles', '26000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-22T10:00:00.000Z', '2024-04-22T10:00:00.000Z'
),
(
  'hist_5f3c8f775d247b53', 'KNE-23042024-001', '2024-04-23', '2024-04-23', '2024-04-23',
  'LALCHAND', '7652816853', 'Kishanganj', '40', 'Male',
  'SINGHA CHANDRA, BELGHARIA, KOCHADHAMAN, KISHANGANJ', 'Piles, Fistula', '24000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-23T10:00:00.000Z', '2024-04-23T10:00:00.000Z'
),
(
  'hist_aa80aeaa7c78b20b', 'KNE-24042024-001', '2024-04-24', '2024-04-24', '2024-04-24',
  'MD RAFIQ', '8076714940', 'Kishanganj', '52', 'Male',
  'MAHIDURPUR CHOWK, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Fistula', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-24T10:00:00.000Z', '2024-04-24T10:00:00.000Z'
),
(
  'hist_dbfe2ab900ea6d14', 'KNE-25042024-001', '2024-04-25', '2024-04-25', '2024-04-25',
  'MITHUN MONDAL', '8800310256', 'Kishanganj', '31', 'Male',
  'BHAGAYA, BHAGAYA, MEHARMA, JHARKHAND', 'Other', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-25T10:00:00.000Z', '2024-04-25T10:00:00.000Z'
),
(
  'hist_4d3549ade9a1713e', 'KNE-26042024-001', '2024-04-26', '2024-04-26', '2024-04-26',
  'KANAYA CHOWHAN', '9471649220', 'Kishanganj', '27', 'Male',
  'TAUSA, KISHANGANJ, KISHANGANJ', 'Piles', '16000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-26T10:00:00.000Z', '2024-04-26T10:00:00.000Z'
),
(
  'hist_6d2162d736066773', 'KNE-30042024-001', '2024-04-30', '2024-04-30', '2024-04-30',
  'REHANA KHATOON', '9262334736', 'Kishanganj', '33', 'Female',
  'CHATTAR GACH, CHATTARGACH, POTIA, KISHANGANJ', 'Piles', '16000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-30T10:00:00.000Z', '2024-04-30T10:00:00.000Z'
),
(
  'hist_aa163fd70ae4c05f', 'KNE-06052024-001', '2024-05-06', '2024-05-06', '2024-05-06',
  'MUBASARA KHATOON', '9382272425', 'Kishanganj', '9', 'Female',
  'SONAPUR, SONAPUR, POTIA, KISHANGANJ', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-06T10:00:00.000Z', '2024-05-06T10:00:00.000Z'
),
(
  'hist_11351dd76bc92908', 'KNE-13052024-001', '2024-05-13', '2024-05-13', '2024-05-13',
  'PUJA YADAV', '9472687467', 'Kishanganj', '26', 'Female',
  'LOHARPATTI, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '17000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-13T10:00:00.000Z', '2024-05-13T10:00:00.000Z'
),
(
  'hist_5a1f4812ec3292b0', 'KNE-29052024-001', '2024-05-29', '2024-05-29', '2024-05-29',
  'ABDUS SAMAD', '8984742260', 'Kishanganj', '24', 'Male',
  'CHAKULIA, CHAKULIA, CHAKULIA, UTTAR DINAJPUR', 'Piles', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-29T10:00:00.000Z', '2024-05-29T10:00:00.000Z'
),
(
  'hist_6b9529bbb77adb53', 'KNE-30052024-001', '2024-05-30', '2024-05-30', '2024-05-30',
  'ARUP SARKAR', '7602569446', 'Kishanganj', '35', 'Male',
  'RAMKRISHNAPUR, NIJAMPUR, CHAKULIA, UTTAR DINAJPUR', 'Piles, Fissure', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-30T10:00:00.000Z', '2024-05-30T10:00:00.000Z'
),
(
  'hist_ef7b0fc9cf18128d', 'KNE-25052024-001', '2024-05-25', '2024-05-25', '2024-05-25',
  'RIBA SAHA', '8789416050', 'Kishanganj', '30', 'Female',
  'ROLLBHAG, KISHANGANJ, KISHANGANJ', 'Piles', '17000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-25T10:00:00.000Z', '2024-05-25T10:00:00.000Z'
),
(
  'hist_bb1135cc9537527a', 'KNE-03062024-001', '2024-06-03', '2024-06-03', '2024-06-03',
  'BHAVANI SADA', '9883702582', 'Kishanganj', '22', 'Female',
  'JANTA HAAT, JANTA HAAT, CHAKULIA, UTTAR DINAJPUR', 'Piles', '24400',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-03T10:00:00.000Z', '2024-06-03T10:00:00.000Z'
),
(
  'hist_b550e5cf853bd3ae', 'KNE-06062024-001', '2024-06-06', '2024-06-06', '2024-06-06',
  'MD ADIL', '9641813429', 'Kishanganj', '32', 'Male',
  'MONORA, CHAKULIA, UTTAR DINAJPUR', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-06T10:00:00.000Z', '2024-06-06T10:00:00.000Z'
),
(
  'hist_5fb830db6ea369fe', 'KNE-07062024-001', '2024-06-07', '2024-06-07', '2024-06-07',
  'NIRUPAMA DAS', '8617476937', 'Kishanganj', '22', 'Female',
  'MALKUNDA, GOALPOKHAR, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '18500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-07T10:00:00.000Z', '2024-06-07T10:00:00.000Z'
),
(
  'hist_ca3822ead4143c5a', 'KNE-10062024-001', '2024-06-10', '2024-06-10', '2024-06-10',
  'TAIMUR ALAM', '6296499910', 'Kishanganj', '24', 'Male',
  'GOALIN, GOALPOKHAR, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '21000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-10T10:00:00.000Z', '2024-06-10T10:00:00.000Z'
),
(
  'hist_8dcaba98aa0aaa0f', 'KNE-16062024-001', '2024-06-16', '2024-06-16', '2024-06-16',
  'BINA BALU', '9593628178', 'Kishanganj', '46', 'Female',
  'LADHI, CHAKULIA, CHAKULIA, UTTAR DINAJPUR', 'Piles', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-16T10:00:00.000Z', '2024-06-16T10:00:00.000Z'
),
(
  'hist_9f6bd86abcb3980a', 'KNE-21062024-001', '2024-06-21', '2024-06-21', '2024-06-21',
  'SAHADAT ALI', '8356029099', 'Kishanganj', '24', 'Male',
  'MILIKBASTI, GORUKHAL, POTIA, KISHANGANJ', 'Piles', '28000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-21T10:00:00.000Z', '2024-06-21T10:00:00.000Z'
),
(
  'hist_179908a2312e0636', 'KNE-25062024-001', '2024-06-25', '2024-06-25', '2024-06-25',
  'HASMAN REZA', '9547292780', 'Kishanganj', '24', 'Male',
  'MAJLISHPUR, JINTAPUR, GOALPOKHAR, UTTAR DINAJPUR', 'Fistula', '23000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-25T10:00:00.000Z', '2024-06-25T10:00:00.000Z'
),
(
  'hist_4ffa6bd23f746f87', 'KNE-26062024-001', '2024-06-26', '2024-06-26', '2024-06-26',
  'MAHIDUL RAHMAN', '6394304312', 'Kishanganj', '45', 'Male',
  'BOCHA GHARI, GOTI, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-26T10:00:00.000Z', '2024-06-26T10:00:00.000Z'
),
(
  'hist_5632f41e97f32cc3', 'KNE-26062024-002', '2024-06-26', '2024-06-26', '2024-06-26',
  'ANARUL HOWK', '9775947813', 'Kishanganj', '30', 'Male',
  'DHARAMPUR, GENNA BARI, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-26T10:00:00.000Z', '2024-06-26T10:00:00.000Z'
),
(
  'hist_38c0d82fcb495af5', 'KNE-28062024-001', '2024-06-28', '2024-06-28', '2024-06-28',
  'SABINA PARVIN', '7667234486', 'Kishanganj', '24', 'Female',
  'BAGUL BARI, BAGAL BARI, KOCHADAMAN', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-28T10:00:00.000Z', '2024-06-28T10:00:00.000Z'
),
(
  'hist_b4982ac77e157ba9', 'KNE-01072024-001', '2024-07-01', '2024-07-01', '2024-07-01',
  'MD SALMAN', '8002220242', 'Kishanganj', '32', 'Male',
  'JIAPOKHAR, JIAPOKHAR, JIAPOKHAR, KISHANGANJ', 'Piles', '22500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-01T10:00:00.000Z', '2024-07-01T10:00:00.000Z'
),
(
  'hist_1b7d7fa7efcc5951', 'KNE-03072024-001', '2024-07-03', '2024-07-03', '2024-07-03',
  'ALMARA', '8597115652', 'Kishanganj', '38', 'Male',
  'KHAGAR NAYA BASTI, KHAGAR, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-03T10:00:00.000Z', '2024-07-03T10:00:00.000Z'
),
(
  'hist_d1546ef676fce2d6', 'KNE-05072024-001', '2024-07-05', '2024-07-05', '2024-07-05',
  'SABANA PARVIN', '9735917197', 'Kishanganj', '41', 'Female',
  'ISLAMPUR', 'Piles', '12000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-05T10:00:00.000Z', '2024-07-05T10:00:00.000Z'
),
(
  'hist_aa29d9cc79536d74', 'KNE-11072024-001', '2024-07-11', '2024-07-11', '2024-07-11',
  'ABDUL MATIN', '9576893249', 'Kishanganj', '24', 'Male',
  'MASTAN CHOWK, BAHGAL BARI, KOCHADAMAN, KISHANGANJ', 'Piles', '23000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-11T10:00:00.000Z', '2024-07-11T10:00:00.000Z'
),
(
  'hist_b442e6e5c4894275', 'KNE-11072024-002', '2024-07-11', '2024-07-11', '2024-07-11',
  'NAVED ALAM', '6207382674', 'Kishanganj', '36', 'Male',
  'MAHAR MARI, BHAGAL BARI, KOCHADAMAN, KISHANGANJ', 'Hydrocele', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-11T10:00:00.000Z', '2024-07-11T10:00:00.000Z'
),
(
  'hist_e20506dc1d027f99', 'KNE-11072024-003', '2024-07-11', '2024-07-11', '2024-07-11',
  'SHIBNATH SINGH', '9593947676', 'Kishanganj', '40', 'Male',
  'LADHI, GORHA, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-11T10:00:00.000Z', '2024-07-11T10:00:00.000Z'
),
(
  'hist_c41d28ec8eec9e62', 'KNE-13072024-001', '2024-07-13', '2024-07-13', '2024-07-13',
  'AKTARI BEGAM', '7585848382', 'Kishanganj', '28', 'Female',
  'MAJLISHPUR, DIGHLI, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-13T10:00:00.000Z', '2024-07-13T10:00:00.000Z'
),
(
  'hist_24041428d2225b3a', 'KNE-13072024-002', '2024-07-13', '2024-07-13', '2024-07-13',
  'AJIM UDDIN', '9142496208', 'Kishanganj', '36', 'Male',
  'HATH PARA, HATWAR, KISHANGANJ, KISHANGANJ', 'Piles', '28000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-13T10:00:00.000Z', '2024-07-13T10:00:00.000Z'
),
(
  'hist_fd692f7e932d2402', 'KNE-17072024-001', '2024-07-17', '2024-07-17', '2024-07-17',
  'RABI KUMAR SINGH', '7319790327', 'Kishanganj', '22', 'Male',
  'ANGAR HAAT, PURNIA', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-17T10:00:00.000Z', '2024-07-17T10:00:00.000Z'
),
(
  'hist_4c5ef326280e4b5f', 'KNE-20072024-001', '2024-07-20', '2024-07-20', '2024-07-20',
  'ASIF', '9911320326', 'Kishanganj', '42', 'Male',
  'SARA DIGHI, PANASHI, KISHANGANJ, KISHANGANJ', 'Piles', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-20T10:00:00.000Z', '2024-07-20T10:00:00.000Z'
),
(
  'hist_d7cc31f2e3d67749', 'KNE-21072024-001', '2024-07-21', '2024-07-21', '2024-07-21',
  'DILFIROJ', '8882947464', 'Kishanganj', '32', 'Male',
  'BASTA, BOALMARA, THAKURGANJ, KISHANGANJ', 'Piles', '24000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-21T10:00:00.000Z', '2024-07-21T10:00:00.000Z'
),
(
  'hist_603b6f6924746ed6', 'KNE-21072024-002', '2024-07-21', '2024-07-21', '2024-07-21',
  'MILI KHATOON', '6202646730', 'Kishanganj', '48', 'Female',
  'KHAGRA, KISHANGANJ, KISHANGANJ', 'Piles', '21000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-21T10:00:00.000Z', '2024-07-21T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'hist_pay_af38482eda49e32f', 'treatment', 'Advance', 'Advance', 'hist_f6bdcde62fee7de3', '9548414688', 'Kishanganj', 'ANKUSH KUMAR',
  '2024-02-03', '12000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-03T10:15:00.000Z', '2024-02-03T10:15:00.000Z'
),
(
  'hist_pay_407195e19e73eca0', 'treatment', 'Advance', 'Advance', 'hist_d4d9f7677d737794', '9341811967', 'Kishanganj', 'ROSHNI KUMARI',
  '2024-02-08', '14000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-08T10:15:00.000Z', '2024-02-08T10:15:00.000Z'
),
(
  'hist_pay_a57edaa8a378b212', 'treatment', 'Advance', 'Advance', 'hist_ab1e5290d9ccdf49', '7477796599', 'Kishanganj', 'ANANDA SARKAR',
  '2024-02-09', '14000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-09T10:15:00.000Z', '2024-02-09T10:15:00.000Z'
),
(
  'hist_pay_25fb5477f66b43c8', 'treatment', 'Advance', 'Advance', 'hist_4ee343a07d916d2b', '9800326267', 'Kishanganj', 'RUKSANA BEGAM',
  '2024-02-15', '13000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-15T10:15:00.000Z', '2024-02-15T10:15:00.000Z'
),
(
  'hist_pay_37c62e49a9de538c', 'treatment', 'Advance', 'Advance', 'hist_eee2c6562ea7c927', '9641228522', 'Kishanganj', 'MD TARIQUE ANWAR',
  '2024-02-16', '23000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-16T10:15:00.000Z', '2024-02-16T10:15:00.000Z'
),
(
  'hist_pay_b294d53394eb6343', 'treatment', 'Advance', 'Advance', 'hist_e0fd9578a59e0a3f', '8837484884', 'Kishanganj', 'SUBRATA BISWAS',
  '2024-02-22', '10000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-22T10:15:00.000Z', '2024-02-22T10:15:00.000Z'
),
(
  'hist_pay_0c0792afc1223db3', 'treatment', 'Advance', 'Advance', 'hist_27ca7a4db130265b', '6294678950', 'Kishanganj', 'AJAY KR MONDAL',
  '2024-02-24', '21300', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-02-24T10:15:00.000Z', '2024-02-24T10:15:00.000Z'
),
(
  'hist_pay_4ba57a9afb598380', 'treatment', 'Advance', 'Advance', 'hist_ba6d0bd40ba2de46', '9797535686', 'Kishanganj', 'NURSHED ALAM',
  '2024-03-15', '22500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-15T10:15:00.000Z', '2024-03-15T10:15:00.000Z'
),
(
  'hist_pay_f662e184851b7d0d', 'treatment', 'Advance', 'Advance', 'hist_1d25647ccc08ae50', '8967786752', 'Kishanganj', 'SANGITA SINGH',
  '2024-03-16', '12000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-16T10:15:00.000Z', '2024-03-16T10:15:00.000Z'
),
(
  'hist_pay_210224c627de323d', 'treatment', 'Advance', 'Advance', 'hist_44e9d56cc4294ea5', '7601992021', 'Kishanganj', 'TANWEER ALAM',
  '2024-03-20', '12000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-20T10:15:00.000Z', '2024-03-20T10:15:00.000Z'
),
(
  'hist_pay_5c3cf500423c0c6a', 'treatment', 'Advance', 'Advance', 'hist_20c3995173df4c3d', '8509898792', 'Kishanganj', 'SIRIKUL ALAM',
  '2024-03-20', '10000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-20T10:15:00.000Z', '2024-03-20T10:15:00.000Z'
),
(
  'hist_pay_5b8097b6cca62e47', 'treatment', 'Advance', 'Advance', 'hist_114cda8482ae9ecc', '9608611304', 'Kishanganj', 'SAHEDA KHATOON',
  '2024-03-21', '11500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-21T10:15:00.000Z', '2024-03-21T10:15:00.000Z'
),
(
  'hist_pay_6a6a1af5b329d3ae', 'treatment', 'Advance', 'Advance', 'hist_22b090351341f452', '8695722505', 'Kishanganj', 'MD ALAM',
  '2024-03-23', '12500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-23T10:15:00.000Z', '2024-03-23T10:15:00.000Z'
),
(
  'hist_pay_e75036dd65d8eedb', 'treatment', 'Advance', 'Advance', 'hist_bb1a94e05e41f30e', '8809259608', 'Kishanganj', 'MIR TAUFIQ',
  '2024-03-23', '18000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-23T10:15:00.000Z', '2024-03-23T10:15:00.000Z'
),
(
  'hist_pay_8308c66134462b76', 'treatment', 'Advance', 'Advance', 'hist_747042391a9dc26d', '8809017397', 'Kishanganj', 'SANAT THAKUR',
  '2024-03-28', '23500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-03-28T10:15:00.000Z', '2024-03-28T10:15:00.000Z'
),
(
  'hist_pay_f6d3c7c51b532187', 'treatment', 'Advance', 'Advance', 'hist_6b567cda26c5113e', '9523986880', 'Kishanganj', 'APISAR ALAM',
  '2024-04-01', '11000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-01T10:15:00.000Z', '2024-04-01T10:15:00.000Z'
),
(
  'hist_pay_9e1c43944cf7746a', 'treatment', 'Advance', 'Advance', 'hist_b801dd5db91c68c3', '9647826939', 'Kishanganj', 'RASAMOY BALA',
  '2024-04-04', '20000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-04T10:15:00.000Z', '2024-04-04T10:15:00.000Z'
),
(
  'hist_pay_87135c73dc6e8d9f', 'treatment', 'Advance', 'Advance', 'hist_6933a939d7ced6f0', '6354972664', 'Kishanganj', 'ABAR ALAM',
  '2024-04-06', '16000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-06T10:15:00.000Z', '2024-04-06T10:15:00.000Z'
),
(
  'hist_pay_4b932e773fad9c0d', 'treatment', 'Advance', 'Advance', 'hist_d4cbb0c8c433b40b', '7384037086', 'Kishanganj', 'NIRMAL HEMRAO',
  '2024-04-10', '43000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-10T10:15:00.000Z', '2024-04-10T10:15:00.000Z'
),
(
  'hist_pay_a10682ad85b0d274', 'treatment', 'Advance', 'Advance', 'hist_004f66bbb8040fcb', '7763933435', 'Kishanganj', 'MD SOURAV',
  '2024-04-12', '42000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-12T10:15:00.000Z', '2024-04-12T10:15:00.000Z'
),
(
  'hist_pay_a7593c5d45195a66', 'treatment', 'Advance', 'Advance', 'hist_2e61903ed6547bcf', '7367902448', 'Kishanganj', 'SAKILA',
  '2024-04-19', '9500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-19T10:15:00.000Z', '2024-04-19T10:15:00.000Z'
),
(
  'hist_pay_8ef7000853d7c55e', 'treatment', 'Advance', 'Advance', 'hist_71084cb54f1a6f33', '7908686830', 'Kishanganj', 'NITISH SINGH',
  '2024-04-22', '26000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-22T10:15:00.000Z', '2024-04-22T10:15:00.000Z'
),
(
  'hist_pay_bef57309be7ac10d', 'treatment', 'Advance', 'Advance', 'hist_5f3c8f775d247b53', '7652816853', 'Kishanganj', 'LALCHAND',
  '2024-04-23', '19000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-23T10:15:00.000Z', '2024-04-23T10:15:00.000Z'
),
(
  'hist_pay_92c0d42d4f4e869f', 'treatment', 'Advance', 'Advance', 'hist_aa80aeaa7c78b20b', '8076714940', 'Kishanganj', 'MD RAFIQ',
  '2024-04-24', '23000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-24T10:15:00.000Z', '2024-04-24T10:15:00.000Z'
),
(
  'hist_pay_e52f4610fe0c0da1', 'treatment', 'Advance', 'Advance', 'hist_dbfe2ab900ea6d14', '8800310256', 'Kishanganj', 'MITHUN MONDAL',
  '2024-04-25', '35000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-25T10:15:00.000Z', '2024-04-25T10:15:00.000Z'
),
(
  'hist_pay_975a98d4d1734885', 'treatment', 'Advance', 'Advance', 'hist_4d3549ade9a1713e', '9471649220', 'Kishanganj', 'KANAYA CHOWHAN',
  '2024-04-26', '8000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-26T10:15:00.000Z', '2024-04-26T10:15:00.000Z'
),
(
  'hist_pay_8e4f935fd8befc1d', 'treatment', 'Advance', 'Advance', 'hist_6d2162d736066773', '9262334736', 'Kishanganj', 'REHANA KHATOON',
  '2024-04-30', '16000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-04-30T10:15:00.000Z', '2024-04-30T10:15:00.000Z'
),
(
  'hist_pay_84ea2f0bb69dd0dd', 'treatment', 'Advance', 'Advance', 'hist_aa163fd70ae4c05f', '9382272425', 'Kishanganj', 'MUBASARA KHATOON',
  '2024-05-06', '11000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-06T10:15:00.000Z', '2024-05-06T10:15:00.000Z'
),
(
  'hist_pay_86976a266049035c', 'treatment', 'Advance', 'Advance', 'hist_11351dd76bc92908', '9472687467', 'Kishanganj', 'PUJA YADAV',
  '2024-05-13', '15300', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-13T10:15:00.000Z', '2024-05-13T10:15:00.000Z'
),
(
  'hist_pay_a6ab1446749dc5e9', 'treatment', 'Advance', 'Advance', 'hist_5a1f4812ec3292b0', '8984742260', 'Kishanganj', 'ABDUS SAMAD',
  '2024-05-29', '16000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-29T10:15:00.000Z', '2024-05-29T10:15:00.000Z'
),
(
  'hist_pay_321674ec9f44f1cc', 'treatment', 'Advance', 'Advance', 'hist_6b9529bbb77adb53', '7602569446', 'Kishanganj', 'ARUP SARKAR',
  '2024-05-30', '15000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-30T10:15:00.000Z', '2024-05-30T10:15:00.000Z'
),
(
  'hist_pay_e59b051f9d7d89f9', 'treatment', 'Advance', 'Advance', 'hist_ef7b0fc9cf18128d', '8789416050', 'Kishanganj', 'RIBA SAHA',
  '2024-05-25', '17000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-05-25T10:15:00.000Z', '2024-05-25T10:15:00.000Z'
),
(
  'hist_pay_89fab59f544a1c47', 'treatment', 'Advance', 'Advance', 'hist_bb1135cc9537527a', '9883702582', 'Kishanganj', 'BHAVANI SADA',
  '2024-06-03', '18300', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-03T10:15:00.000Z', '2024-06-03T10:15:00.000Z'
),
(
  'hist_pay_9eb241785a136d27', 'treatment', 'Advance', 'Advance', 'hist_b550e5cf853bd3ae', '9641813429', 'Kishanganj', 'MD ADIL',
  '2024-06-06', '20000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-06T10:15:00.000Z', '2024-06-06T10:15:00.000Z'
),
(
  'hist_pay_8d9f07afd3eabc7c', 'treatment', 'Advance', 'Advance', 'hist_5fb830db6ea369fe', '8617476937', 'Kishanganj', 'NIRUPAMA DAS',
  '2024-06-07', '18500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-07T10:15:00.000Z', '2024-06-07T10:15:00.000Z'
),
(
  'hist_pay_aebf6cf109fe8491', 'treatment', 'Advance', 'Advance', 'hist_ca3822ead4143c5a', '6296499910', 'Kishanganj', 'TAIMUR ALAM',
  '2024-06-10', '5100', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-10T10:15:00.000Z', '2024-06-10T10:15:00.000Z'
),
(
  'hist_pay_8d46defaf9d2c25e', 'treatment', 'Advance', 'Advance', 'hist_8dcaba98aa0aaa0f', '9593628178', 'Kishanganj', 'BINA BALU',
  '2024-06-16', '18000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-16T10:15:00.000Z', '2024-06-16T10:15:00.000Z'
),
(
  'hist_pay_e49d9f4d367ee4a9', 'treatment', 'Advance', 'Advance', 'hist_9f6bd86abcb3980a', '8356029099', 'Kishanganj', 'SAHADAT ALI',
  '2024-06-21', '15000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-21T10:15:00.000Z', '2024-06-21T10:15:00.000Z'
),
(
  'hist_pay_2f5e01e6a53c2713', 'treatment', 'Advance', 'Advance', 'hist_179908a2312e0636', '9547292780', 'Kishanganj', 'HASMAN REZA',
  '2024-06-25', '13000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-25T10:15:00.000Z', '2024-06-25T10:15:00.000Z'
),
(
  'hist_pay_ff2fac6df3ba39aa', 'treatment', 'Advance', 'Advance', 'hist_4ffa6bd23f746f87', '6394304312', 'Kishanganj', 'MAHIDUL RAHMAN',
  '2024-06-26', '10000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-26T10:15:00.000Z', '2024-06-26T10:15:00.000Z'
),
(
  'hist_pay_d1778b46d70ee482', 'treatment', 'Advance', 'Advance', 'hist_5632f41e97f32cc3', '9775947813', 'Kishanganj', 'ANARUL HOWK',
  '2024-06-26', '15000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-26T10:15:00.000Z', '2024-06-26T10:15:00.000Z'
),
(
  'hist_pay_f7e93f070fc416ac', 'treatment', 'Advance', 'Advance', 'hist_38c0d82fcb495af5', '7667234486', 'Kishanganj', 'SABINA PARVIN',
  '2024-06-28', '14100', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-06-28T10:15:00.000Z', '2024-06-28T10:15:00.000Z'
),
(
  'hist_pay_43723a243adf83a7', 'treatment', 'Advance', 'Advance', 'hist_b4982ac77e157ba9', '8002220242', 'Kishanganj', 'MD SALMAN',
  '2024-07-01', '22500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-01T10:15:00.000Z', '2024-07-01T10:15:00.000Z'
),
(
  'hist_pay_e11644f87213cd6f', 'treatment', 'Advance', 'Advance', 'hist_1b7d7fa7efcc5951', '8597115652', 'Kishanganj', 'ALMARA',
  '2024-07-03', '19000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-03T10:15:00.000Z', '2024-07-03T10:15:00.000Z'
),
(
  'hist_pay_ca1dabab633f6220', 'treatment', 'Advance', 'Advance', 'hist_d1546ef676fce2d6', '9735917197', 'Kishanganj', 'SABANA PARVIN',
  '2024-07-05', '3000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-05T10:15:00.000Z', '2024-07-05T10:15:00.000Z'
),
(
  'hist_pay_221561d6cfed3d07', 'treatment', 'Advance', 'Advance', 'hist_aa29d9cc79536d74', '9576893249', 'Kishanganj', 'ABDUL MATIN',
  '2024-07-11', '15000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-11T10:15:00.000Z', '2024-07-11T10:15:00.000Z'
),
(
  'hist_pay_b83d559b39d863d4', 'treatment', 'Advance', 'Advance', 'hist_b442e6e5c4894275', '6207382674', 'Kishanganj', 'NAVED ALAM',
  '2024-07-11', '9000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-11T10:15:00.000Z', '2024-07-11T10:15:00.000Z'
),
(
  'hist_pay_29d0e1205f62e34f', 'treatment', 'Advance', 'Advance', 'hist_e20506dc1d027f99', '9593947676', 'Kishanganj', 'SHIBNATH SINGH',
  '2024-07-11', '12000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-11T10:15:00.000Z', '2024-07-11T10:15:00.000Z'
),
(
  'hist_pay_48501df428596d8c', 'treatment', 'Advance', 'Advance', 'hist_c41d28ec8eec9e62', '7585848382', 'Kishanganj', 'AKTARI BEGAM',
  '2024-07-13', '15000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-13T10:15:00.000Z', '2024-07-13T10:15:00.000Z'
),
(
  'hist_pay_bef767ecdbb28920', 'treatment', 'Advance', 'Advance', 'hist_24041428d2225b3a', '9142496208', 'Kishanganj', 'AJIM UDDIN',
  '2024-07-13', '28000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-13T10:15:00.000Z', '2024-07-13T10:15:00.000Z'
),
(
  'hist_pay_dfea1bf6c74177f4', 'treatment', 'Advance', 'Advance', 'hist_fd692f7e932d2402', '7319790327', 'Kishanganj', 'RABI KUMAR SINGH',
  '2024-07-17', '14000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-17T10:15:00.000Z', '2024-07-17T10:15:00.000Z'
),
(
  'hist_pay_de8126621fd6019a', 'treatment', 'Advance', 'Advance', 'hist_4c5ef326280e4b5f', '9911320326', 'Kishanganj', 'ASIF',
  '2024-07-20', '10500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-20T10:15:00.000Z', '2024-07-20T10:15:00.000Z'
),
(
  'hist_pay_cba50247acaf8ec1', 'treatment', 'Advance', 'Advance', 'hist_d7cc31f2e3d67749', '8882947464', 'Kishanganj', 'DILFIROJ',
  '2024-07-21', '22000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-21T10:15:00.000Z', '2024-07-21T10:15:00.000Z'
),
(
  'hist_pay_6266f7b7973d5808', 'treatment', 'Advance', 'Advance', 'hist_603b6f6924746ed6', '6202646730', 'Kishanganj', 'MILI KHATOON',
  '2024-07-21', '11000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-07-21T10:15:00.000Z', '2024-07-21T10:15:00.000Z'
);
