-- কিশানগঞ্জ ধাপ ৩ -- ব্যাচ 3/5 (রোগী 109-162, মোট 54)
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
  'hist_4acb3346d641a897', 'KNE-23022023-001', '2023-02-23', '2023-02-23', '2023-02-23',
  'KHUSHI PARVIN', '7479464369', 'Kishanganj', '', 'Female',
  'TEGHARIA, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-23T10:00:00.000Z', '2023-02-23T10:00:00.000Z'
),
(
  'hist_71d7791a4c7a77e3', 'KNE-18022023-002', '2023-02-18', '2023-02-18', '2023-02-18',
  'MD GULAM RASUL', '9892602746', 'Kishanganj', '41', 'Male',
  'SURJAPUR RIGUF, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Hydrocele', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-18T10:00:00.000Z', '2023-02-18T10:00:00.000Z'
),
(
  'hist_4cd9034909259d03', 'KNE-02032023-001', '2023-03-02', '2023-03-02', '2023-03-02',
  'ALAUDDIN', '9813548240', 'Kishanganj', '48', 'Male',
  'BEDBARI, BEDBARI, GOALPOKHAR, UTTAR DINAJPUR', 'Fistula', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-03-02T10:00:00.000Z', '2023-03-02T10:00:00.000Z'
),
(
  'hist_04bb29db5d24e135', 'KNE-17032023-001', '2023-03-17', '2023-03-17', '2023-03-17',
  'ANJERI HAUQUE', '8695316683', 'Kishanganj', '', 'Male',
  'CHAKULIA, CHAKULIA, CHAKULIA, UTTAR DINAJPUR', 'Piles', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-03-17T10:00:00.000Z', '2023-03-17T10:00:00.000Z'
),
(
  'hist_57accee24988aaf2', 'KNE-03042023-001', '2023-04-03', '2023-04-03', '2023-04-03',
  'SHAMMD', '9641251960', 'Kishanganj', '24', 'Male',
  'GOPALPUR, MADHARGACHHI, KARANDIGHI, UTTAR DINAJPUR', 'Fistula', '8000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-03T10:00:00.000Z', '2023-04-03T10:00:00.000Z'
),
(
  'hist_d24180b48887ac67', 'KNE-28042023-001', '2023-04-28', '2023-04-28', '2023-04-28',
  'KRISHNA MARDI', '7679083949', 'Kishanganj', '23', 'Male',
  'DAKSHIN AMBARI, GOALPOKHAR, GOALPOKHAR, UTTAR DINAJPUR', 'Other', '12000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-28T10:00:00.000Z', '2023-04-28T10:00:00.000Z'
),
(
  'hist_f06f7f4b15d4cc7a', 'KNE-30052023-001', '2023-05-30', '2023-05-30', '2023-05-30',
  'TOUSIF REZA', '9832965511', 'Kishanganj', '28', 'Male',
  'KALUGAON, GOAGAON, GOALPOKHAR, UTTAR DINAJPUR', 'Other', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-30T10:00:00.000Z', '2023-05-30T10:00:00.000Z'
),
(
  'hist_713dc682abfe6bda', 'KNE-10062023-001', '2023-06-10', '2023-06-10', '2023-06-10',
  'MAHOSIN REZA', '6005211622', 'Kishanganj', '25', 'Male',
  '', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-10T10:00:00.000Z', '2023-06-10T10:00:00.000Z'
),
(
  'hist_8e06aaa8f373e6f1', 'KNE-21062023-001', '2023-06-21', '2023-06-21', '2023-06-21',
  'GULABI KHATOON', '7479311228', 'Kishanganj', '18', 'Female',
  'HALALPUR, RAIGANJ, UTTAR DINAJPUR', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-21T10:00:00.000Z', '2023-06-21T10:00:00.000Z'
),
(
  'hist_845cd67915d66acd', 'KNE-24062023-001', '2023-06-24', '2023-06-24', '2023-06-24',
  'MAHABUB ALAM', '9771572400', 'Kishanganj', '35', 'Male',
  'KADAMPUR', 'Piles', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-24T10:00:00.000Z', '2023-06-24T10:00:00.000Z'
),
(
  'hist_df92a37f68950201', 'KNE-04072023-001', '2023-07-04', '2023-07-04', '2023-07-04',
  'BENJAMIN', '8409762336', 'Kishanganj', '60', 'Male',
  'HALIM CHOWK, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Other', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-04T10:00:00.000Z', '2023-07-04T10:00:00.000Z'
),
(
  'hist_26aca1e89e7d6f2a', 'KNE-24072023-001', '2023-07-24', '2023-07-24', '2023-07-24',
  'SACHIN KR DAS', '8825122217', 'Kishanganj', '28', 'Male',
  'FOOL BASTI, CHAKLA GHAT, KISHANGANJ', 'Piles', '12000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-24T10:00:00.000Z', '2023-07-24T10:00:00.000Z'
),
(
  'hist_ab30931a74373ea8', 'KNE-25072023-001', '2023-07-25', '2023-07-25', '2023-07-25',
  'ABDUL KUDDUS', '7765097034', 'Kishanganj', '60', 'Male',
  'JANTA KANYA BARI, KOCHADAMAN, KISHANGANJ', 'Fistula', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-25T10:00:00.000Z', '2023-07-25T10:00:00.000Z'
),
(
  'hist_8e5f17d3ad94d7ab', 'KNE-31072023-001', '2023-07-31', '2023-07-31', '2023-07-31',
  'RENUKA KHATOON', '7865813715', 'Kishanganj', '25', 'Female',
  'BHAGALPUR, GOAGAON, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '17000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-31T10:00:00.000Z', '2023-07-31T10:00:00.000Z'
),
(
  'hist_76e908be1c699fa8', 'KNE-31072023-002', '2023-07-31', '2023-07-31', '2023-07-31',
  'MAIFUL ALAM', '7979984065', 'Kishanganj', '26', 'Male',
  'SOUDAGAR PATTI, KISHANGANJ, KISHANGANJ', 'Hydrocele', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-31T10:00:00.000Z', '2023-07-31T10:00:00.000Z'
),
(
  'hist_41f5e17646dc1d1f', 'KNE-01082023-001', '2023-08-01', '2023-08-01', '2023-08-01',
  'MD NAJIM', '6203548064', 'Kishanganj', '32', 'Male',
  'BAHADURGANJ, KISHANGANJ', 'Fistula', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-01T10:00:00.000Z', '2023-08-01T10:00:00.000Z'
),
(
  'hist_2d520cc5ff257185', 'KNE-02082023-001', '2023-08-02', '2023-08-02', '2023-08-02',
  'ASHOK DAS', '9907786900', 'Kishanganj', '42', 'Male',
  'HARIYA DIGHI, DOMOHONA, KANAN DIGHI, UTTAR DINAJPUR', 'Fistula', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-02T10:00:00.000Z', '2023-08-02T10:00:00.000Z'
),
(
  'hist_2c58137d7c6986a3', 'KNE-03082023-001', '2023-08-03', '2023-08-03', '2023-08-03',
  'ALI HOSSAIN', '8637574643', 'Kishanganj', '30', 'Male',
  'BAKSHA BARI, BARBILLA, GOALPOKHAR, UTTAR DINAJPUR', 'Hydrocele', '12000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-03T10:00:00.000Z', '2023-08-03T10:00:00.000Z'
),
(
  'hist_3135aa51afc640d2', 'KNE-09082023-001', '2023-08-09', '2023-08-09', '2023-08-09',
  'ANJUMARA BEGAM', '9733264458', 'Kishanganj', '', 'Female',
  'TESAR, DOOR, GOAGAON, UTTAR DINAJPUR', 'Piles', '12000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-09T10:00:00.000Z', '2023-08-09T10:00:00.000Z'
),
(
  'hist_2dfc208b865fcf67', 'KNE-17082023-001', '2023-08-17', '2023-08-17', '2023-08-17',
  'JULFIKAR ALAM', '9679890167', 'Kishanganj', '26', 'Male',
  'KAMARSHAL, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Fistula', '42000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-17T10:00:00.000Z', '2023-08-17T10:00:00.000Z'
),
(
  'hist_b730ad8fd740c867', 'KNE-18082023-001', '2023-08-18', '2023-08-18', '2023-08-18',
  'SHANAWAZ', '6289028319', 'Kishanganj', '20', 'Male',
  'ALUDDIN, DIMTI, UTTAR DINAJPUR', 'Other', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-18T10:00:00.000Z', '2023-08-18T10:00:00.000Z'
),
(
  'hist_27218731928c3d94', 'KNE-19082023-001', '2023-08-19', '2023-08-19', '2023-08-19',
  'AMIR HOSSAIN', '9382309358', 'Kishanganj', '23', 'Male',
  'SETHUL, VARNA, CHAKULIA, UTTAR DINAJPUR', 'Fistula', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-19T10:00:00.000Z', '2023-08-19T10:00:00.000Z'
),
(
  'hist_1ea34eb0d21b4651', 'KNE-22082023-001', '2023-08-22', '2023-08-22', '2023-08-22',
  'GOPAL SINGH', '6296448639', 'Kishanganj', '42', 'Male',
  'DHARAMPUR, DHARAMPUR, GOALPOKHAR, UTTAR DINAJPUR', 'Fistula', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-22T10:00:00.000Z', '2023-08-22T10:00:00.000Z'
),
(
  'hist_b5705b67ee70234e', 'KNE-23082023-001', '2023-08-23', '2023-08-23', '2023-08-23',
  'MUSTIK ALAM', '9608035424', 'Kishanganj', '38', 'Male',
  'DAULA, HATWAR, KISHANGANJ, KISHANGANJ', 'Piles', '23000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-23T10:00:00.000Z', '2023-08-23T10:00:00.000Z'
),
(
  'hist_d4235b789843bd4e', 'KNE-23082023-002', '2023-08-23', '2023-08-23', '2023-08-23',
  'HANDELU SINGH', '6296448639', 'Kishanganj', '45', 'Male',
  'DHARAMPUR, DHARAMPUR, UTTAR DINAJPUR, UTTAR DINAJPUR', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-23T10:00:00.000Z', '2023-08-23T10:00:00.000Z'
),
(
  'hist_2d9dedaf2dff2805', 'KNE-25082023-001', '2023-08-25', '2023-08-25', '2023-08-25',
  'BIBI MASADA', '8709470499', 'Kishanganj', '', 'Female',
  'BIJULIA, THAKURBARI, CHAKULIA, UTTAR DINAJPUR', 'Piles', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-25T10:00:00.000Z', '2023-08-25T10:00:00.000Z'
),
(
  'hist_84d4b696edc9bf10', 'KNE-30082023-001', '2023-08-30', '2023-08-30', '2023-08-30',
  'KURBAN ALI', '9547636850', 'Kishanganj', '16', 'Male',
  'BISHNUPUR, BISHNUPUR, BHADURGANJ, KISHANGANJ', 'Other', '16000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-30T10:00:00.000Z', '2023-08-30T10:00:00.000Z'
),
(
  'hist_1ec99b9b2a42e8f4', 'KNE-06092023-001', '2023-09-06', '2023-09-06', '2023-09-06',
  'JASMINE NILA', '9593290398', 'Kishanganj', '24', 'Female',
  'BIPRIT CHOWK, BAHAN, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '16500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-06T10:00:00.000Z', '2023-09-06T10:00:00.000Z'
),
(
  'hist_2415df59c3e3d68e', 'KNE-13092023-001', '2023-09-13', '2023-09-13', '2023-09-13',
  'LAITUN', '8670907255', 'Kishanganj', '42', 'Male',
  'GOTI NAGAR, LODHAN, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-13T10:00:00.000Z', '2023-09-13T10:00:00.000Z'
),
(
  'hist_c2ac0a15a8ae8b60', 'KNE-16092023-001', '2023-09-16', '2023-09-16', '2023-09-16',
  'MG AKBAR', '9734174935', 'Kishanganj', '24', 'Male',
  'MILIK BASTI, KOIMARI, POTIA, KISHANGANJ', 'Fistula', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-16T10:00:00.000Z', '2023-09-16T10:00:00.000Z'
),
(
  'hist_fd86c7363bb3f031', 'KNE-18092023-001', '2023-09-18', '2023-09-18', '2023-09-18',
  'CHANO DEVI', '9056254061', 'Kishanganj', '26', 'Female',
  'BIJRA, MAJGAMA PIPLA, ANGAR, KISHANGANJ', 'Piles', '16000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-18T10:00:00.000Z', '2023-09-18T10:00:00.000Z'
),
(
  'hist_f03a157b1827484f', 'KNE-20092023-001', '2023-09-20', '2023-09-20', '2023-09-20',
  'SIKERU NISHA', '9931401130', 'Kishanganj', '36', 'Female',
  'BAKSHA, CHATTAR GACH, KISHANGANJ, KISHANGANJ', 'Piles', '12000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-20T10:00:00.000Z', '2023-09-20T10:00:00.000Z'
),
(
  'hist_aa2044d3a0746240', 'KNE-29092023-001', '2023-09-29', '2023-09-29', '2023-09-29',
  'NIAMUDDIN', '8159826879', 'Kishanganj', '35', 'Male',
  'MILIK BASTI, GORUKHAL, POTIA, KISHANGANJ', 'Fistula', '26000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-29T10:00:00.000Z', '2023-09-29T10:00:00.000Z'
),
(
  'hist_508432cacdc02e5c', 'KNE-30092023-001', '2023-09-30', '2023-09-30', '2023-09-30',
  'MD TASLIM UDDIN', '8436886253', 'Kishanganj', '32', 'Male',
  'NAYA BASTI, KHAGAR, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '16500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-30T10:00:00.000Z', '2023-09-30T10:00:00.000Z'
),
(
  'hist_bb576e7f7fbf6a0c', 'KNE-03102023-001', '2023-10-03', '2023-10-03', '2023-10-03',
  'ABDUS SALAM', '8509325964', 'Kishanganj', '33', 'Male',
  'MATIKUNDA, MATIKUNDA, ISLAMPUR, UTTAR DINAJPUR', 'Fistula', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-03T10:00:00.000Z', '2023-10-03T10:00:00.000Z'
),
(
  'hist_6a0e2f02c278dfba', 'KNE-04102023-001', '2023-10-04', '2023-10-04', '2023-10-04',
  'RUJI KHATOON', '8972910452', 'Kishanganj', '19', 'Female',
  'DANGI BASTI, KOIMARI, POTIA, KISHANGANJ', 'Piles', '12000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-04T10:00:00.000Z', '2023-10-04T10:00:00.000Z'
),
(
  'hist_5c00ebcabeb27e59', 'KNE-04102023-002', '2023-10-04', '2023-10-04', '2023-10-04',
  'MD KAMALUDDIN', '7365802345', 'Kishanganj', '22', 'Male',
  'BOCHA GARI, GOTI, GOTI, UTTAR DINAJPUR', 'Piles', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-04T10:00:00.000Z', '2023-10-04T10:00:00.000Z'
),
(
  'hist_695074626a72fea1', 'KNE-05102023-001', '2023-10-05', '2023-10-05', '2023-10-05',
  'SHANKAR', '8653312228', 'Kishanganj', '25', 'Male',
  'MALDUAR, MAJLISHPUR, GOALPOKHAR, UTTAR DINAJPUR', 'Other', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-05T10:00:00.000Z', '2023-10-05T10:00:00.000Z'
),
(
  'hist_ffed82780bba8405', 'KNE-05102023-002', '2023-10-05', '2023-10-05', '2023-10-05',
  'MD ASHRAF', '8104922270', 'Kishanganj', '30', 'Male',
  'TELTA, BALRAMPUR, BALRAMPUR, KATIHAR', 'Piles', '16500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-05T10:00:00.000Z', '2023-10-05T10:00:00.000Z'
),
(
  'hist_129b52718716a214', 'KNE-07102023-001', '2023-10-07', '2023-10-07', '2023-10-07',
  'RABIUL ISLAM', '6296735367', 'Kishanganj', '45', 'Male',
  'BOCHAGARI, GOTI, GOALPOKHAR, UTTAR DINAJPUR', 'Hydrocele', '16000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-07T10:00:00.000Z', '2023-10-07T10:00:00.000Z'
),
(
  'hist_cd537804971be465', 'KNE-19102023-001', '2023-10-19', '2023-10-19', '2023-10-19',
  'SARFARAJ ALAM', '9932908825', 'Kishanganj', '30', 'Male',
  'SIMULIA, SIMULIA, DALKHOLA, UTTAR DINAJPUR', 'Other', '26630',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-19T10:00:00.000Z', '2023-10-19T10:00:00.000Z'
),
(
  'hist_1e54d883cfc3da4c', 'KNE-11102023-001', '2023-10-11', '2023-10-11', '2023-10-11',
  'SUFAL SARAN', '7431837740', 'Kishanganj', '23', 'Male',
  'GHARDHAPPA, CHAKULIA, CHAKULIA, UTTAR DINAJPUR', 'Hydrocele', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-11T10:00:00.000Z', '2023-10-11T10:00:00.000Z'
),
(
  'hist_44f4477fcb5b41c3', 'KNE-12102023-001', '2023-10-12', '2023-10-12', '2023-10-12',
  'MOYMANA BABY', '7497311228', 'Kishanganj', '40', 'Female',
  'HALAL PUR, PUSVARIA, RAJ, UTTAR DINAJPUR', 'Piles', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-12T10:00:00.000Z', '2023-10-12T10:00:00.000Z'
),
(
  'hist_d0b14e8ba53e4aa1', 'KNE-14102023-001', '2023-10-14', '2023-10-14', '2023-10-14',
  'MONOWAR HOSSAIN', '9064566203', 'Kishanganj', '16', 'Male',
  'DIMTI GOABARI, 5 DIMTI, ISLAMPUR, UTTAR DINAJPUR', 'Fistula', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-14T10:00:00.000Z', '2023-10-14T10:00:00.000Z'
),
(
  'hist_4f73c9a0c909fa3a', 'KNE-10112023-001', '2023-11-10', '2023-11-10', '2023-11-10',
  'SABNAM BEGAM', '8210509264', 'Kishanganj', '21', 'Female',
  'OJHAPOKHAR', 'Piles', '13000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-11-10T10:00:00.000Z', '2023-11-10T10:00:00.000Z'
),
(
  'hist_e147a791cc6f1061', 'KNE-06122023-001', '2023-12-06', '2023-12-06', '2023-12-06',
  'MERI DOMIKA', '9933449672', 'Kishanganj', '31', 'Female',
  'DEBIJORA', 'Piles', '27000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-12-06T10:00:00.000Z', '2023-12-06T10:00:00.000Z'
),
(
  'hist_433f5f25eae4be6b', 'KNE-17122023-001', '2023-12-17', '2023-12-17', '2023-12-17',
  'GANAPATI B.S.F.', '9596336875', 'Kishanganj', '32', 'Male',
  'BSF CAMP, KHAGRA, KISHANGANJ', 'Fistula', '38000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-12-17T10:00:00.000Z', '2023-12-17T10:00:00.000Z'
),
(
  'hist_441864a2bac0eac6', 'KNE-29122023-001', '2023-12-29', '2023-12-29', '2023-12-29',
  'SAFIK ANSARI', '8759880452', 'Kishanganj', '40', 'Male',
  'MALHARA, SURJAPUR, KISHANGANJ', 'Hydrocele', '21000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-12-29T10:00:00.000Z', '2023-12-29T10:00:00.000Z'
),
(
  'hist_2cb1ffd3774e9003', 'KNE-09012024-001', '2024-01-09', '2024-01-09', '2024-01-09',
  'AISUDDIN SHEK', '9004954989', 'Kishanganj', '48', 'Male',
  'SIRNIA, GOAGAON, GOALPOKHAR, UTTAR DINAJPUR', 'Fistula', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-09T10:00:00.000Z', '2024-01-09T10:00:00.000Z'
),
(
  'hist_34b1c3dc41cb67ea', 'KNE-11012024-001', '2024-01-11', '2024-01-11', '2024-01-11',
  'SAMERUL HAUQUE', '6297639498', 'Kishanganj', '26', 'Male',
  'MILIK BASTI, GORUKHAL, UTTAR DINAJPUR', 'Piles', '26000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-11T10:00:00.000Z', '2024-01-11T10:00:00.000Z'
),
(
  'hist_5b8ff302942d1d2d', 'KNE-12012024-001', '2024-01-12', '2024-01-12', '2024-01-12',
  'AKSHAY KR MONDAL', '9905967148', 'Kishanganj', '20', 'Male',
  'CHICHORA, BERI, BEBIGANJ, UTTAR DINAJPUR', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-12T10:00:00.000Z', '2024-01-12T10:00:00.000Z'
),
(
  'hist_6cb93866a8d3f176', 'KNE-13012024-001', '2024-01-13', '2024-01-13', '2024-01-13',
  'TAHSIP RAJA', '8927035235', 'Kishanganj', '21', 'Male',
  'PATUYA, GORHA, GOALPOKHAR, UTTAR DINAJPUR', 'Other', '12000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-13T10:00:00.000Z', '2024-01-13T10:00:00.000Z'
),
(
  'hist_0c1cc64fb9903426', 'KNE-24012024-001', '2024-01-24', '2024-01-24', '2024-01-24',
  'RENUKA KHATOON', '7865813715', 'Kishanganj', '25', 'Female',
  'BHAGBANPUR, GOAGAON, GOALPOKHAR, UTTAR DINAJPUR', 'Fistula', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-24T10:00:00.000Z', '2024-01-24T10:00:00.000Z'
),
(
  'hist_8fa03226558f2f9c', 'KNE-29012024-001', '2024-01-29', '2024-01-29', '2024-01-29',
  'ZAKIR HOSSAIN', '9800518555', 'Kishanganj', '', 'Male',
  'CHURAKUTTI, GORHA, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-29T10:00:00.000Z', '2024-01-29T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'hist_pay_f84c9fc708c9a322', 'treatment', 'Advance', 'Advance', 'hist_4acb3346d641a897', '7479464369', 'Kishanganj', 'KHUSHI PARVIN',
  '2023-02-23', '8000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-23T10:15:00.000Z', '2023-02-23T10:15:00.000Z'
),
(
  'hist_pay_74c5517e75a0b5d6', 'treatment', 'Advance', 'Advance', 'hist_71d7791a4c7a77e3', '9892602746', 'Kishanganj', 'MD GULAM RASUL',
  '2023-02-18', '15000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-18T10:15:00.000Z', '2023-02-18T10:15:00.000Z'
),
(
  'hist_pay_6004f51c86d5fe9b', 'treatment', 'Advance', 'Advance', 'hist_4cd9034909259d03', '9813548240', 'Kishanganj', 'ALAUDDIN',
  '2023-03-02', '10000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-03-02T10:15:00.000Z', '2023-03-02T10:15:00.000Z'
),
(
  'hist_pay_c4465b5d6f59ab29', 'treatment', 'Advance', 'Advance', 'hist_04bb29db5d24e135', '8695316683', 'Kishanganj', 'ANJERI HAUQUE',
  '2023-03-17', '11000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-03-17T10:15:00.000Z', '2023-03-17T10:15:00.000Z'
),
(
  'hist_pay_9ae285af616db46e', 'treatment', 'Advance', 'Advance', 'hist_57accee24988aaf2', '9641251960', 'Kishanganj', 'SHAMMD',
  '2023-04-03', '52000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-03T10:15:00.000Z', '2023-04-03T10:15:00.000Z'
),
(
  'hist_pay_2fc13d6e19caeeb3', 'treatment', 'Advance', 'Advance', 'hist_d24180b48887ac67', '7679083949', 'Kishanganj', 'KRISHNA MARDI',
  '2023-04-28', '9000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-04-28T10:15:00.000Z', '2023-04-28T10:15:00.000Z'
),
(
  'hist_pay_2a61ace65f4a8de0', 'treatment', 'Advance', 'Advance', 'hist_f06f7f4b15d4cc7a', '9832965511', 'Kishanganj', 'TOUSIF REZA',
  '2023-05-30', '8000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-05-30T10:15:00.000Z', '2023-05-30T10:15:00.000Z'
),
(
  'hist_pay_1baf3bf1f92279d0', 'treatment', 'Advance', 'Advance', 'hist_713dc682abfe6bda', '6005211622', 'Kishanganj', 'MAHOSIN REZA',
  '2023-06-10', '14500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-10T10:15:00.000Z', '2023-06-10T10:15:00.000Z'
),
(
  'hist_pay_2ee285fa7c6e1fa1', 'treatment', 'Advance', 'Advance', 'hist_8e06aaa8f373e6f1', '7479311228', 'Kishanganj', 'GULABI KHATOON',
  '2023-06-21', '5000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-21T10:15:00.000Z', '2023-06-21T10:15:00.000Z'
),
(
  'hist_pay_c0eb933e1aa85dfd', 'treatment', 'Advance', 'Advance', 'hist_845cd67915d66acd', '9771572400', 'Kishanganj', 'MAHABUB ALAM',
  '2023-06-24', '3500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-06-24T10:15:00.000Z', '2023-06-24T10:15:00.000Z'
),
(
  'hist_pay_0951d58cce475f10', 'treatment', 'Advance', 'Advance', 'hist_df92a37f68950201', '8409762336', 'Kishanganj', 'BENJAMIN',
  '2023-07-04', '10000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-04T10:15:00.000Z', '2023-07-04T10:15:00.000Z'
),
(
  'hist_pay_087355afeeb00914', 'treatment', 'Advance', 'Advance', 'hist_26aca1e89e7d6f2a', '8825122217', 'Kishanganj', 'SACHIN KR DAS',
  '2023-07-24', '11000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-24T10:15:00.000Z', '2023-07-24T10:15:00.000Z'
),
(
  'hist_pay_818543300179f91a', 'treatment', 'Advance', 'Advance', 'hist_ab30931a74373ea8', '7765097034', 'Kishanganj', 'ABDUL KUDDUS',
  '2023-07-25', '13000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-25T10:15:00.000Z', '2023-07-25T10:15:00.000Z'
),
(
  'hist_pay_a90080ac38dd6bbf', 'treatment', 'Advance', 'Advance', 'hist_8e5f17d3ad94d7ab', '7865813715', 'Kishanganj', 'RENUKA KHATOON',
  '2023-07-31', '14500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-31T10:15:00.000Z', '2023-07-31T10:15:00.000Z'
),
(
  'hist_pay_db8f372d5a4dd10b', 'treatment', 'Advance', 'Advance', 'hist_76e908be1c699fa8', '7979984065', 'Kishanganj', 'MAIFUL ALAM',
  '2023-07-31', '8000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-07-31T10:15:00.000Z', '2023-07-31T10:15:00.000Z'
),
(
  'hist_pay_ad135240775cd859', 'treatment', 'Advance', 'Advance', 'hist_41f5e17646dc1d1f', '6203548064', 'Kishanganj', 'MD NAJIM',
  '2023-08-01', '19900', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-01T10:15:00.000Z', '2023-08-01T10:15:00.000Z'
),
(
  'hist_pay_7b7689cea601933c', 'treatment', 'Advance', 'Advance', 'hist_2d520cc5ff257185', '9907786900', 'Kishanganj', 'ASHOK DAS',
  '2023-08-02', '11000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-02T10:15:00.000Z', '2023-08-02T10:15:00.000Z'
),
(
  'hist_pay_f3f861059499b133', 'treatment', 'Advance', 'Advance', 'hist_2c58137d7c6986a3', '8637574643', 'Kishanganj', 'ALI HOSSAIN',
  '2023-08-03', '9000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-03T10:15:00.000Z', '2023-08-03T10:15:00.000Z'
),
(
  'hist_pay_3b95ba4574c928f7', 'treatment', 'Advance', 'Advance', 'hist_3135aa51afc640d2', '9733264458', 'Kishanganj', 'ANJUMARA BEGAM',
  '2023-08-09', '11200', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-09T10:15:00.000Z', '2023-08-09T10:15:00.000Z'
),
(
  'hist_pay_4d98b8255929a2f9', 'treatment', 'Advance', 'Advance', 'hist_2dfc208b865fcf67', '9679890167', 'Kishanganj', 'JULFIKAR ALAM',
  '2023-08-17', '25000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-17T10:15:00.000Z', '2023-08-17T10:15:00.000Z'
),
(
  'hist_pay_bd72964089604d4a', 'treatment', 'Advance', 'Advance', 'hist_b730ad8fd740c867', '6289028319', 'Kishanganj', 'SHANAWAZ',
  '2023-08-18', '13000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-18T10:15:00.000Z', '2023-08-18T10:15:00.000Z'
),
(
  'hist_pay_698a60225b9d4e49', 'treatment', 'Advance', 'Advance', 'hist_27218731928c3d94', '9382309358', 'Kishanganj', 'AMIR HOSSAIN',
  '2023-08-19', '7000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-19T10:15:00.000Z', '2023-08-19T10:15:00.000Z'
),
(
  'hist_pay_1ad810d938905d39', 'treatment', 'Advance', 'Advance', 'hist_1ea34eb0d21b4651', '6296448639', 'Kishanganj', 'GOPAL SINGH',
  '2023-08-22', '22000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-22T10:15:00.000Z', '2023-08-22T10:15:00.000Z'
),
(
  'hist_pay_6f5f7999853524fd', 'treatment', 'Advance', 'Advance', 'hist_b5705b67ee70234e', '9608035424', 'Kishanganj', 'MUSTIK ALAM',
  '2023-08-23', '15000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-23T10:15:00.000Z', '2023-08-23T10:15:00.000Z'
),
(
  'hist_pay_41971b30c2c463f7', 'treatment', 'Advance', 'Advance', 'hist_d4235b789843bd4e', '6296448639', 'Kishanganj', 'HANDELU SINGH',
  '2023-08-23', '15500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-23T10:15:00.000Z', '2023-08-23T10:15:00.000Z'
),
(
  'hist_pay_8b1f171b62cf2a7e', 'treatment', 'Advance', 'Advance', 'hist_2d9dedaf2dff2805', '8709470499', 'Kishanganj', 'BIBI MASADA',
  '2023-08-25', '6000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-25T10:15:00.000Z', '2023-08-25T10:15:00.000Z'
),
(
  'hist_pay_009db1f6ff3e08c6', 'treatment', 'Advance', 'Advance', 'hist_84d4b696edc9bf10', '9547636850', 'Kishanganj', 'KURBAN ALI',
  '2023-08-30', '12300', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-08-30T10:15:00.000Z', '2023-08-30T10:15:00.000Z'
),
(
  'hist_pay_81bff218e160f05f', 'treatment', 'Advance', 'Advance', 'hist_1ec99b9b2a42e8f4', '9593290398', 'Kishanganj', 'JASMINE NILA',
  '2023-09-06', '10000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-06T10:15:00.000Z', '2023-09-06T10:15:00.000Z'
),
(
  'hist_pay_72c96381e000ffd9', 'treatment', 'Advance', 'Advance', 'hist_2415df59c3e3d68e', '8670907255', 'Kishanganj', 'LAITUN',
  '2023-09-13', '19000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-13T10:15:00.000Z', '2023-09-13T10:15:00.000Z'
),
(
  'hist_pay_f6728ec385776e3c', 'treatment', 'Advance', 'Advance', 'hist_c2ac0a15a8ae8b60', '9734174935', 'Kishanganj', 'MG AKBAR',
  '2023-09-16', '32500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-16T10:15:00.000Z', '2023-09-16T10:15:00.000Z'
),
(
  'hist_pay_5607b12faa23b842', 'treatment', 'Advance', 'Advance', 'hist_fd86c7363bb3f031', '9056254061', 'Kishanganj', 'CHANO DEVI',
  '2023-09-18', '15500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-18T10:15:00.000Z', '2023-09-18T10:15:00.000Z'
),
(
  'hist_pay_7cf1f3c61969579f', 'treatment', 'Advance', 'Advance', 'hist_f03a157b1827484f', '9931401130', 'Kishanganj', 'SIKERU NISHA',
  '2023-09-20', '8500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-20T10:15:00.000Z', '2023-09-20T10:15:00.000Z'
),
(
  'hist_pay_409026ea84202b62', 'treatment', 'Advance', 'Advance', 'hist_aa2044d3a0746240', '8159826879', 'Kishanganj', 'NIAMUDDIN',
  '2023-09-29', '16000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-29T10:15:00.000Z', '2023-09-29T10:15:00.000Z'
),
(
  'hist_pay_b17622f01074f662', 'treatment', 'Advance', 'Advance', 'hist_508432cacdc02e5c', '8436886253', 'Kishanganj', 'MD TASLIM UDDIN',
  '2023-09-30', '9000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-09-30T10:15:00.000Z', '2023-09-30T10:15:00.000Z'
),
(
  'hist_pay_4b3e94481d1aeca8', 'treatment', 'Advance', 'Advance', 'hist_bb576e7f7fbf6a0c', '8509325964', 'Kishanganj', 'ABDUS SALAM',
  '2023-10-03', '27000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-03T10:15:00.000Z', '2023-10-03T10:15:00.000Z'
),
(
  'hist_pay_75b919030443abd8', 'treatment', 'Advance', 'Advance', 'hist_6a0e2f02c278dfba', '8972910452', 'Kishanganj', 'RUJI KHATOON',
  '2023-10-04', '10500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-04T10:15:00.000Z', '2023-10-04T10:15:00.000Z'
),
(
  'hist_pay_8f4df1810e578c46', 'treatment', 'Advance', 'Advance', 'hist_5c00ebcabeb27e59', '7365802345', 'Kishanganj', 'MD KAMALUDDIN',
  '2023-10-04', '12000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-04T10:15:00.000Z', '2023-10-04T10:15:00.000Z'
),
(
  'hist_pay_1651e338dd85a2e6', 'treatment', 'Advance', 'Advance', 'hist_695074626a72fea1', '8653312228', 'Kishanganj', 'SHANKAR',
  '2023-10-05', '17000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-05T10:15:00.000Z', '2023-10-05T10:15:00.000Z'
),
(
  'hist_pay_01236ffedcbd78bc', 'treatment', 'Advance', 'Advance', 'hist_ffed82780bba8405', '8104922270', 'Kishanganj', 'MD ASHRAF',
  '2023-10-05', '9500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-05T10:15:00.000Z', '2023-10-05T10:15:00.000Z'
),
(
  'hist_pay_d423457cf444c367', 'treatment', 'Advance', 'Advance', 'hist_129b52718716a214', '6296735367', 'Kishanganj', 'RABIUL ISLAM',
  '2023-10-07', '11000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-07T10:15:00.000Z', '2023-10-07T10:15:00.000Z'
),
(
  'hist_pay_e3fc8f8786d9b9ed', 'treatment', 'Advance', 'Advance', 'hist_cd537804971be465', '9932908825', 'Kishanganj', 'SARFARAJ ALAM',
  '2023-10-19', '21000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-19T10:15:00.000Z', '2023-10-19T10:15:00.000Z'
),
(
  'hist_pay_9c432e729ab91a59', 'treatment', 'Advance', 'Advance', 'hist_1e54d883cfc3da4c', '7431837740', 'Kishanganj', 'SUFAL SARAN',
  '2023-10-11', '14000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-11T10:15:00.000Z', '2023-10-11T10:15:00.000Z'
),
(
  'hist_pay_60fa083a2b2fa93d', 'treatment', 'Advance', 'Advance', 'hist_44f4477fcb5b41c3', '7497311228', 'Kishanganj', 'MOYMANA BABY',
  '2023-10-12', '10500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-12T10:15:00.000Z', '2023-10-12T10:15:00.000Z'
),
(
  'hist_pay_656bf72d686986c7', 'treatment', 'Advance', 'Advance', 'hist_d0b14e8ba53e4aa1', '9064566203', 'Kishanganj', 'MONOWAR HOSSAIN',
  '2023-10-14', '13200', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-10-14T10:15:00.000Z', '2023-10-14T10:15:00.000Z'
),
(
  'hist_pay_7ca54397d2665b94', 'treatment', 'Advance', 'Advance', 'hist_4f73c9a0c909fa3a', '8210509264', 'Kishanganj', 'SABNAM BEGAM',
  '2023-11-10', '11500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-11-10T10:15:00.000Z', '2023-11-10T10:15:00.000Z'
),
(
  'hist_pay_f9451769c1151a55', 'treatment', 'Advance', 'Advance', 'hist_e147a791cc6f1061', '9933449672', 'Kishanganj', 'MERI DOMIKA',
  '2023-12-06', '7000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-12-06T10:15:00.000Z', '2023-12-06T10:15:00.000Z'
),
(
  'hist_pay_ee46aadd38c66288', 'treatment', 'Advance', 'Advance', 'hist_433f5f25eae4be6b', '9596336875', 'Kishanganj', 'GANAPATI B.S.F.',
  '2023-12-17', '31000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-12-17T10:15:00.000Z', '2023-12-17T10:15:00.000Z'
),
(
  'hist_pay_88e8a7e9d39f10d6', 'treatment', 'Advance', 'Advance', 'hist_441864a2bac0eac6', '8759880452', 'Kishanganj', 'SAFIK ANSARI',
  '2023-12-29', '18000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-12-29T10:15:00.000Z', '2023-12-29T10:15:00.000Z'
),
(
  'hist_pay_d45bfc3437248b69', 'treatment', 'Advance', 'Advance', 'hist_2cb1ffd3774e9003', '9004954989', 'Kishanganj', 'AISUDDIN SHEK',
  '2024-01-09', '12500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-09T10:15:00.000Z', '2024-01-09T10:15:00.000Z'
),
(
  'hist_pay_460d1fa978c14216', 'treatment', 'Advance', 'Advance', 'hist_34b1c3dc41cb67ea', '6297639498', 'Kishanganj', 'SAMERUL HAUQUE',
  '2024-01-11', '20000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-11T10:15:00.000Z', '2024-01-11T10:15:00.000Z'
),
(
  'hist_pay_c643520442e105c5', 'treatment', 'Advance', 'Advance', 'hist_5b8ff302942d1d2d', '9905967148', 'Kishanganj', 'AKSHAY KR MONDAL',
  '2024-01-12', '15000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-12T10:15:00.000Z', '2024-01-12T10:15:00.000Z'
),
(
  'hist_pay_fb392dcc93d85dbc', 'treatment', 'Advance', 'Advance', 'hist_6cb93866a8d3f176', '8927035235', 'Kishanganj', 'TAHSIP RAJA',
  '2024-01-13', '4000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-13T10:15:00.000Z', '2024-01-13T10:15:00.000Z'
),
(
  'hist_pay_6b526094b9b11ece', 'treatment', 'Advance', 'Advance', 'hist_0c1cc64fb9903426', '7865813715', 'Kishanganj', 'RENUKA KHATOON',
  '2024-01-24', '20000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-24T10:15:00.000Z', '2024-01-24T10:15:00.000Z'
),
(
  'hist_pay_ab0b281c7a637881', 'treatment', 'Advance', 'Advance', 'hist_8fa03226558f2f9c', '9800518555', 'Kishanganj', 'ZAKIR HOSSAIN',
  '2024-01-29', '6000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-01-29T10:15:00.000Z', '2024-01-29T10:15:00.000Z'
);
