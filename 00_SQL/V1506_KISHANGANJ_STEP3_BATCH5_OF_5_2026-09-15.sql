-- কিশানগঞ্জ ধাপ ৩ -- ব্যাচ 5/5 (রোগী 217-269, মোট 53)
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
  'hist_93eee3dc1eefbb54', 'KNE-03082024-001', '2024-08-03', '2024-08-03', '2024-08-03',
  'ARSHAD ALAM', '9878832913', 'Kishanganj', '25', 'Male',
  'TABAL VITA, PIPRITHAN, THAKURGANJ, KISHANGANJ', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-03T10:00:00.000Z', '2024-08-03T10:00:00.000Z'
),
(
  'hist_fb7478af8bfb31c4', 'KNE-03082024-002', '2024-08-03', '2024-08-03', '2024-08-03',
  'NEHA JASWAL', '6393953152', 'Kishanganj', '26', 'Female',
  'KANKI, CHAKULIA, UTTAR DINAJPUR', 'Piles', '16000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-03T10:00:00.000Z', '2024-08-03T10:00:00.000Z'
),
(
  'hist_ece94b310696c47a', 'KNE-05082024-001', '2024-08-05', '2024-08-05', '2024-08-05',
  'BADIRUL ISLAM', '6299574923', 'Kishanganj', '21', 'Male',
  'MAKHAN POKHAR, TAIABPUR, POTHIA, KISHANGANJ', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-05T10:00:00.000Z', '2024-08-05T10:00:00.000Z'
),
(
  'hist_79b4471d0baecd3e', 'KNE-05082024-002', '2024-08-05', '2024-08-05', '2024-08-05',
  'ABDUL RASID', '9358251073', 'Kishanganj', '32', 'Male',
  'PANJIPARA, PANJIPARA, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '16000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-05T10:00:00.000Z', '2024-08-05T10:00:00.000Z'
),
(
  'hist_839fdfb96e7e9cf0', 'KNE-07082024-001', '2024-08-07', '2024-08-07', '2024-08-07',
  'PUJA DEY', '8509486949', 'Kishanganj', '22', 'Female',
  'DALKHOLA, UTTAR DINAJPUR', 'Piles', '16000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-07T10:00:00.000Z', '2024-08-07T10:00:00.000Z'
),
(
  'hist_30408d1b81a46458', 'KNE-12082024-001', '2024-08-12', '2024-08-12', '2024-08-12',
  'SANTANA DEVI', '9932574075', 'Kishanganj', '40', 'Female',
  'BASTA, KOPALHATI, HARISH CHANDRAPUR, MALDA', 'Fistula', '71500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-12T10:00:00.000Z', '2024-08-12T10:00:00.000Z'
),
(
  'hist_19dae83a110f2c5c', 'KNE-13082024-001', '2024-08-13', '2024-08-13', '2024-08-13',
  'ENABUL HOWK', '9883029473', 'Kishanganj', '35', 'Male',
  'CHAKULIA, UTTAR DINAJPUR', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-13T10:00:00.000Z', '2024-08-13T10:00:00.000Z'
),
(
  'hist_bb1583eb4e259bc9', 'KNE-20082024-001', '2024-08-20', '2024-08-20', '2024-08-20',
  'NUR MOHAMMAD', '7091224917', 'Kishanganj', '76', 'Male',
  'CHANDAR KOLA, ANGAR HAAT, PURNIA', 'Piles', '21000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-20T10:00:00.000Z', '2024-08-20T10:00:00.000Z'
),
(
  'hist_da3521fa50da631a', 'KNE-21082024-001', '2024-08-21', '2024-08-21', '2024-08-21',
  'MUNNA MANJUR', '9547649726', 'Kishanganj', '35', 'Male',
  'NANGOLA, NANGOLA, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-21T10:00:00.000Z', '2024-08-21T10:00:00.000Z'
),
(
  'hist_b1c8c30b3d1c887e', 'KNE-23082024-001', '2024-08-23', '2024-08-23', '2024-08-23',
  'SURAYA KHATOON', '7478705917', 'Kishanganj', '35', 'Female',
  'SAHAPUR, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-23T10:00:00.000Z', '2024-08-23T10:00:00.000Z'
),
(
  'hist_b364b99d520d34ca', 'KNE-26082024-001', '2024-08-26', '2024-08-26', '2024-08-26',
  'PRIYANKA BISWAS', '7063661502', 'Kishanganj', '15', 'Female',
  'MAJLISHPUR, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '19000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-26T10:00:00.000Z', '2024-08-26T10:00:00.000Z'
),
(
  'hist_3ab161c95eb342a1', 'KNE-28082024-001', '2024-08-28', '2024-08-28', '2024-08-28',
  'MANJU ROY', '8101614067', 'Kishanganj', '25', 'Female',
  'BIJULIA, CHAKULIA, UTTAR DINAJPUR', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-28T10:00:00.000Z', '2024-08-28T10:00:00.000Z'
),
(
  'hist_bc5715566d57f743', 'KNE-05082024-003', '2024-08-05', '2024-08-05', '2024-08-05',
  'NAJMUL HOWK', '9065215454', 'Kishanganj', '25', 'Male',
  'MAKHAN POKHAR, TAYABPUR, POTIA, KISHANGANJ', 'Piles', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-05T10:00:00.000Z', '2024-08-05T10:00:00.000Z'
),
(
  'hist_57ab3ceba8a0a580', 'KNE-01092024-001', '2024-09-01', '2024-09-01', '2024-09-01',
  'MD YASIM', '7905499800', 'Kishanganj', '34', 'Male',
  'PANJIPARA, PANJIPARA, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-01T10:00:00.000Z', '2024-09-01T10:00:00.000Z'
),
(
  'hist_fd6029f3fa954b2e', 'KNE-05092024-001', '2024-09-05', '2024-09-05', '2024-09-05',
  'MUJAHID ALAM', '9798656219', 'Kishanganj', '24', 'Male',
  'SINGHIMARI, RAIPUR, PAHARKATTA, KISHANGANJ', 'Piles', '18000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-05T10:00:00.000Z', '2024-09-05T10:00:00.000Z'
),
(
  'hist_bc1348d7c3dc6ad9', 'KNE-12092024-001', '2024-09-12', '2024-09-12', '2024-09-12',
  'MANIK KUMAR DAS', '9933558267', 'Kishanganj', '30', 'Male',
  'RANIGANJ, CHAKULIA, UTTAR DINAJPUR', 'Fistula', '40000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-12T10:00:00.000Z', '2024-09-12T10:00:00.000Z'
),
(
  'hist_6a6e158cd584a0b9', 'KNE-16092024-001', '2024-09-16', '2024-09-16', '2024-09-16',
  'NAFIZ ALAM', '7004460126', 'Kishanganj', '35', 'Male',
  'KISHANGANJ, KISHANGANJ', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-16T10:00:00.000Z', '2024-09-16T10:00:00.000Z'
),
(
  'hist_ed8d70892417f222', 'KNE-16092024-002', '2024-09-16', '2024-09-16', '2024-09-16',
  'RAKESH KUMAR', '6205532335', 'Kishanganj', '25', 'Male',
  'BEGUSARAI', 'Piles', '19000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-16T10:00:00.000Z', '2024-09-16T10:00:00.000Z'
),
(
  'hist_1a98f17b56da5e1d', 'KNE-18092024-001', '2024-09-18', '2024-09-18', '2024-09-18',
  'WAHID ALAM', '6202705961', 'Kishanganj', '36', 'Male',
  'GARAMAR, CHATTARGACH, PAHARKATTA, KISHANGANJ', 'Hydrocele', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-18T10:00:00.000Z', '2024-09-18T10:00:00.000Z'
),
(
  'hist_58eb36e8e9b7f9a4', 'KNE-24092024-001', '2024-09-24', '2024-09-24', '2024-09-24',
  'TAJBUL HOWK', '8371080689', 'Kishanganj', '34', 'Male',
  'AMULIYA, CHAKULIA, UTTAR DINAJPUR', 'Piles', '31500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-24T10:00:00.000Z', '2024-09-24T10:00:00.000Z'
),
(
  'hist_128a233fb16a8253', 'KNE-30092024-001', '2024-09-30', '2024-09-30', '2024-09-30',
  'TAJBUL HOWK', '9064909069', 'Kishanganj', '38', 'Male',
  'KONAKAM HAAT, AMULIYA, CHAKULIA, UTTAR DINAJPUR', 'Piles', '21500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-30T10:00:00.000Z', '2024-09-30T10:00:00.000Z'
),
(
  'hist_0c6d79d3ee3c3871', 'KNE-02102024-001', '2024-10-02', '2024-10-02', '2024-10-02',
  'SHAMIM AKTER', '8825544079', 'Kishanganj', '28', 'Female',
  'MOTIHARPUR, AHILGAON, JALALGARH, PURNIA', 'Fistula', '32000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-02T10:00:00.000Z', '2024-10-02T10:00:00.000Z'
),
(
  'hist_52a5e431960f22ea', 'KNE-02102024-002', '2024-10-02', '2024-10-02', '2024-10-02',
  'SAGARIKA DAS', '7001502725', 'Kishanganj', '21', 'Female',
  'MALKUNDA, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '23500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-02T10:00:00.000Z', '2024-10-02T10:00:00.000Z'
),
(
  'hist_390f542502f17f39', 'KNE-02102024-003', '2024-10-02', '2024-10-02', '2024-10-02',
  'RINA DEVI', '9852947840', 'Kishanganj', '53', 'Female',
  'DUMURIA, KISHANGANJ, KISH', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-02T10:00:00.000Z', '2024-10-02T10:00:00.000Z'
),
(
  'hist_985a6f7518bd3b94', 'KNE-08102024-001', '2024-10-08', '2024-10-08', '2024-10-08',
  'ISRAT KHATOON', '8294314745', 'Kishanganj', '26', 'Female',
  'DHANERA, ADAMPUR, BALRAMPUR, KATIHAR', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-08T10:00:00.000Z', '2024-10-08T10:00:00.000Z'
),
(
  'hist_b028967b1979a719', 'KNE-08102024-002', '2024-10-08', '2024-10-08', '2024-10-08',
  'NILAM KUMARI', '8789381453', 'Kishanganj', '32', 'Female',
  'POLICE LINE, KISHANGANJ, KISHANGANJ', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-08T10:00:00.000Z', '2024-10-08T10:00:00.000Z'
),
(
  'hist_ebeeb49dab36abdf', 'KNE-17102024-001', '2024-10-17', '2024-10-17', '2024-10-17',
  'ALINAZ', '9229252663', 'Kishanganj', '2.5', 'Male',
  'SINGHARI, SONTHA, KOCHADAMAN, KISHANGANJ', 'Piles', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-17T10:00:00.000Z', '2024-10-17T10:00:00.000Z'
),
(
  'hist_b4bb5348c43c3b1f', 'KNE-19102024-001', '2024-10-19', '2024-10-19', '2024-10-19',
  'MD SAMIM ULLAH', '9382173705', 'Kishanganj', '25', 'Male',
  'SOLPARA, SOLPARA, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-19T10:00:00.000Z', '2024-10-19T10:00:00.000Z'
),
(
  'hist_88b16d0b9fae91ac', 'KNE-19102024-002', '2024-10-19', '2024-10-19', '2024-10-19',
  'MUMTAZIR ALAM', '7352971890', 'Kishanganj', '28', 'Male',
  'MASTAN CHOWK, PATKOI HAAT, KOCHADHAMAN, KISHANGANJ', 'Piles', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-19T10:00:00.000Z', '2024-10-19T10:00:00.000Z'
),
(
  'hist_ffa418ac2bdeddf7', 'KNE-20102024-001', '2024-10-20', '2024-10-20', '2024-10-20',
  'TANVEER ALAM', '9631955398', 'Kishanganj', '27', 'Male',
  'LAXMIPUR, PADRASA TOLA, KISHANGANJ', 'Piles', '34000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-20T10:00:00.000Z', '2024-10-20T10:00:00.000Z'
),
(
  'hist_1260fd65e66f6239', 'KNE-23102024-001', '2024-10-23', '2024-10-23', '2024-10-23',
  'DHANNO DEVI', '8283085614', 'Kishanganj', '55', 'Female',
  'CHAINPUR, KUTTIGOLA, ANGAR, KISHANGANJ', 'Piles', '54000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-23T10:00:00.000Z', '2024-10-23T10:00:00.000Z'
),
(
  'hist_6652182fd94d71d5', 'KNE-31102024-001', '2024-10-31', '2024-10-31', '2024-10-31',
  'AMAN KUMAR JHA', '9508884012', 'Kishanganj', '20', 'Male',
  'MOTIBAGH, KISHANGANJ', 'Piles', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-31T10:00:00.000Z', '2024-10-31T10:00:00.000Z'
),
(
  'hist_9ccf482ec2b3bab4', 'KNE-10112024-001', '2024-11-10', '2024-11-10', '2024-11-10',
  'KANCHANA KUMARI', '7004037114', 'Kishanganj', '30', 'Female',
  'HOSPITAL ROAD, KISHANGANJ, KISHANGANJ', 'Piles', '22000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-10T10:00:00.000Z', '2024-11-10T10:00:00.000Z'
),
(
  'hist_4db5ebddda08e93b', 'KNE-10112024-002', '2024-11-10', '2024-11-10', '2024-11-10',
  'HASIBUL RAHMAN', '8509685971', 'Kishanganj', '28', 'Male',
  'KANKI, KANKI, CHAKULIA, UTTAR DINAJPUR', 'Piles', '24000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-10T10:00:00.000Z', '2024-11-10T10:00:00.000Z'
),
(
  'hist_7b53f4e2299482b9', 'KNE-11112024-001', '2024-11-11', '2024-11-11', '2024-11-11',
  'RAMA SANKAR PASWAN', '9798933610', 'Kishanganj', '34', 'Male',
  'MAHENGAON, KISHANGANJ, KISHANGANJ', 'Piles', '12000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-11T10:00:00.000Z', '2024-11-11T10:00:00.000Z'
),
(
  'hist_1af770eef6f363ae', 'KNE-13112024-001', '2024-11-13', '2024-11-13', '2024-11-13',
  'SADDAM HOSSAIN', '8169353984', 'Kishanganj', '30', 'Male',
  'GANNA BARI, KALA NAGIN, ISLAMPUR, UTTAR DINAJPUR', 'Piles', '21000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-13T10:00:00.000Z', '2024-11-13T10:00:00.000Z'
),
(
  'hist_aeb2891553f934fc', 'KNE-14112024-001', '2024-11-14', '2024-11-14', '2024-11-14',
  'GAFFAR ALAM', '8945064648', 'Kishanganj', '25', 'Male',
  'GOAGAON, GOALPOKHAR, UTTAR DINAJPUR', 'Fistula', '50000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-14T10:00:00.000Z', '2024-11-14T10:00:00.000Z'
),
(
  'hist_abf8449b9391aaa7', 'KNE-15112024-001', '2024-11-15', '2024-11-15', '2024-11-15',
  'ANWAR ALAM', '7074474859', 'Kishanganj', '12', 'Male',
  'GOPALPUR, HALDIBARI, MADARGACHI, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-15T10:00:00.000Z', '2024-11-15T10:00:00.000Z'
),
(
  'hist_64b9a2ef17e8d777', 'KNE-16112024-001', '2024-11-16', '2024-11-16', '2024-11-16',
  'MD AFRAZ', '9031546677', 'Kishanganj', '34', 'Male',
  'GOPALPUR, NATWA PARA, BAHADURGANJ, KISHANGANJ', 'Fistula', '50000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-16T10:00:00.000Z', '2024-11-16T10:00:00.000Z'
),
(
  'hist_386178a425e1e62c', 'KNE-16112024-002', '2024-11-16', '2024-11-16', '2024-11-16',
  'NUR ALAM', '8250944968', 'Kishanganj', '27', 'Male',
  'DALAL BASTI, DIMRULLA, ISLAMPUR, UTTAR DINAJPUR', 'Piles', '31000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-16T10:00:00.000Z', '2024-11-16T10:00:00.000Z'
),
(
  'hist_96a6d4474fae8663', 'KNE-16112024-003', '2024-11-16', '2024-11-16', '2024-11-16',
  'FRJANA BEGAM', '9593764670', 'Kishanganj', '30', 'Female',
  'KONAKAMAT, AMULIA, CHAKULIA, UTTAR DINAJPUR', 'Piles', '50000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-16T10:00:00.000Z', '2024-11-16T10:00:00.000Z'
),
(
  'hist_429a0761ef6171b2', 'KNE-16112024-004', '2024-11-16', '2024-11-16', '2024-11-16',
  'SEEBA KHATOON', '9932560305', 'Kishanganj', '22', 'Female',
  'SITVITA, PANJIPARA, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-16T10:00:00.000Z', '2024-11-16T10:00:00.000Z'
),
(
  'hist_21e5ada821ed5313', 'KNE-18112024-001', '2024-11-18', '2024-11-18', '2024-11-18',
  'MDSALIM', '8101951752', 'Kishanganj', '20', 'Male',
  'MAJLISHPUR, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-18T10:00:00.000Z', '2024-11-18T10:00:00.000Z'
),
(
  'hist_df3dd215de1da414', 'KNE-18112024-002', '2024-11-18', '2024-11-18', '2024-11-18',
  'SAMA BEGAM', '9641261236', 'Kishanganj', '28', 'Female',
  'PANJIPARA, PANJIPARA, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '24000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-18T10:00:00.000Z', '2024-11-18T10:00:00.000Z'
),
(
  'hist_d0f91a7a40635e64', 'KNE-19112024-001', '2024-11-19', '2024-11-19', '2024-11-19',
  'BIKRAM RAJWAN', '7357816622', 'Kishanganj', '24', 'Male',
  'GAMHARIA, MODHA, KOCHADAMAN, KISHANGANJ', 'Piles', '35000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-19T10:00:00.000Z', '2024-11-19T10:00:00.000Z'
),
(
  'hist_9739b75a6f667d01', 'KNE-24112024-001', '2024-11-24', '2024-11-24', '2024-11-24',
  'MD YASIM', '8851700288', 'Kishanganj', '29', 'Male',
  'BAISAPATTI, KISHANGANJ', 'Piles', '23000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-24T10:00:00.000Z', '2024-11-24T10:00:00.000Z'
),
(
  'hist_c3568c5e2f47caa0', 'KNE-02122024-001', '2024-12-02', '2024-12-02', '2024-12-02',
  'CHAYTANNA HASTA', '9860862163', 'Kishanganj', '18', 'Male',
  'SARSA, LALGANJ, DALKHOLA, UTTAR DINAJPUR', 'Fistula', '44000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-02T10:00:00.000Z', '2024-12-02T10:00:00.000Z'
),
(
  'hist_f52e5ad594e4f48d', 'KNE-04122024-001', '2024-12-04', '2024-12-04', '2024-12-04',
  'SUJAY KUMAR', '9798907615', 'Kishanganj', '20', 'Male',
  'JANTA HAAT, KANAIYABARI, KOCHADAMAN, KISHANGANJ', 'Piles', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-04T10:00:00.000Z', '2024-12-04T10:00:00.000Z'
),
(
  'hist_aac71e6fa4b95e8f', 'KNE-05122024-001', '2024-12-05', '2024-12-05', '2024-12-05',
  'NASIMA KHATOON', '7478679122', 'Kishanganj', '35', 'Female',
  'KHAGAR NAYA BASTI, POKHRIA, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '28000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-05T10:00:00.000Z', '2024-12-05T10:00:00.000Z'
),
(
  'hist_5e60a61f9fe351b9', 'KNE-11122024-001', '2024-12-11', '2024-12-11', '2024-12-11',
  'RAJKUMAR', '7667424208', 'Kishanganj', '21', 'Male',
  'NAYABASTI, KHAGAR, GOALPOKHAR, UTTAR DINAJPUR', 'Fistula', '45000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-11T10:00:00.000Z', '2024-12-11T10:00:00.000Z'
),
(
  'hist_b1514b253e70065b', 'KNE-14122024-001', '2024-12-14', '2024-12-14', '2024-12-14',
  'MAKSUD', '9932680291', 'Kishanganj', '20', 'Male',
  'MAHENGAON, KISHANGANJ', 'Piles', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-14T10:00:00.000Z', '2024-12-14T10:00:00.000Z'
),
(
  'hist_7925631b39d3eb8c', 'KNE-23122024-001', '2024-12-23', '2024-12-23', '2024-12-23',
  'SAHANSHA ALAM', '8371891170', 'Kishanganj', '35', 'Male',
  'TALCHAPWA, KONIYA VITA, GOALPOKHAR, UTTAR DINAJPUR', 'Fistula', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-23T10:00:00.000Z', '2024-12-23T10:00:00.000Z'
),
(
  'hist_9682cf0abf73ab6f', 'KNE-28122024-001', '2024-12-28', '2024-12-28', '2024-12-28',
  'KAMRUL HODA', '7021809395', 'Kishanganj', '40', 'Male',
  'TALCHAPWA, KONIYA VITA, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-28T10:00:00.000Z', '2024-12-28T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'hist_pay_eff5bc2588c885c8', 'treatment', 'Advance', 'Advance', 'hist_93eee3dc1eefbb54', '9878832913', 'Kishanganj', 'ARSHAD ALAM',
  '2024-08-03', '4500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-03T10:15:00.000Z', '2024-08-03T10:15:00.000Z'
),
(
  'hist_pay_ab9a1f804a5b7f82', 'treatment', 'Advance', 'Advance', 'hist_fb7478af8bfb31c4', '6393953152', 'Kishanganj', 'NEHA JASWAL',
  '2024-08-03', '14500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-03T10:15:00.000Z', '2024-08-03T10:15:00.000Z'
),
(
  'hist_pay_ea12c5f4bd70cde6', 'treatment', 'Advance', 'Advance', 'hist_ece94b310696c47a', '6299574923', 'Kishanganj', 'BADIRUL ISLAM',
  '2024-08-05', '20000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-05T10:15:00.000Z', '2024-08-05T10:15:00.000Z'
),
(
  'hist_pay_79ecad0c831d0708', 'treatment', 'Advance', 'Advance', 'hist_79b4471d0baecd3e', '9358251073', 'Kishanganj', 'ABDUL RASID',
  '2024-08-05', '15000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-05T10:15:00.000Z', '2024-08-05T10:15:00.000Z'
),
(
  'hist_pay_e8742d2731c58c35', 'treatment', 'Advance', 'Advance', 'hist_839fdfb96e7e9cf0', '8509486949', 'Kishanganj', 'PUJA DEY',
  '2024-08-07', '15000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-07T10:15:00.000Z', '2024-08-07T10:15:00.000Z'
),
(
  'hist_pay_36edcdea4f028195', 'treatment', 'Advance', 'Advance', 'hist_30408d1b81a46458', '9932574075', 'Kishanganj', 'SANTANA DEVI',
  '2024-08-12', '58000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-12T10:15:00.000Z', '2024-08-12T10:15:00.000Z'
),
(
  'hist_pay_7e24a8d0f0b061f0', 'treatment', 'Advance', 'Advance', 'hist_19dae83a110f2c5c', '9883029473', 'Kishanganj', 'ENABUL HOWK',
  '2024-08-13', '15000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-13T10:15:00.000Z', '2024-08-13T10:15:00.000Z'
),
(
  'hist_pay_743f6b39da295a76', 'treatment', 'Advance', 'Advance', 'hist_bb1583eb4e259bc9', '7091224917', 'Kishanganj', 'NUR MOHAMMAD',
  '2024-08-20', '21100', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-20T10:15:00.000Z', '2024-08-20T10:15:00.000Z'
),
(
  'hist_pay_3f268a8456b2cccc', 'treatment', 'Advance', 'Advance', 'hist_da3521fa50da631a', '9547649726', 'Kishanganj', 'MUNNA MANJUR',
  '2024-08-21', '30000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-21T10:15:00.000Z', '2024-08-21T10:15:00.000Z'
),
(
  'hist_pay_79cde34246681d63', 'treatment', 'Advance', 'Advance', 'hist_b1c8c30b3d1c887e', '7478705917', 'Kishanganj', 'SURAYA KHATOON',
  '2024-08-23', '35000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-23T10:15:00.000Z', '2024-08-23T10:15:00.000Z'
),
(
  'hist_pay_1af7c5a9d93dd153', 'treatment', 'Advance', 'Advance', 'hist_b364b99d520d34ca', '7063661502', 'Kishanganj', 'PRIYANKA BISWAS',
  '2024-08-26', '10000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-26T10:15:00.000Z', '2024-08-26T10:15:00.000Z'
),
(
  'hist_pay_415b592dd6fccbbf', 'treatment', 'Advance', 'Advance', 'hist_3ab161c95eb342a1', '8101614067', 'Kishanganj', 'MANJU ROY',
  '2024-08-28', '21000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-28T10:15:00.000Z', '2024-08-28T10:15:00.000Z'
),
(
  'hist_pay_64dba1573e510eb9', 'treatment', 'Advance', 'Advance', 'hist_bc5715566d57f743', '9065215454', 'Kishanganj', 'NAJMUL HOWK',
  '2024-08-05', '8000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-08-05T10:15:00.000Z', '2024-08-05T10:15:00.000Z'
),
(
  'hist_pay_415bc05f0877a218', 'treatment', 'Advance', 'Advance', 'hist_57ab3ceba8a0a580', '7905499800', 'Kishanganj', 'MD YASIM',
  '2024-09-01', '40000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-01T10:15:00.000Z', '2024-09-01T10:15:00.000Z'
),
(
  'hist_pay_9d6a42f7845201d2', 'treatment', 'Advance', 'Advance', 'hist_fd6029f3fa954b2e', '9798656219', 'Kishanganj', 'MUJAHID ALAM',
  '2024-09-05', '18200', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-05T10:15:00.000Z', '2024-09-05T10:15:00.000Z'
),
(
  'hist_pay_875775eebd6f9697', 'treatment', 'Advance', 'Advance', 'hist_bc1348d7c3dc6ad9', '9933558267', 'Kishanganj', 'MANIK KUMAR DAS',
  '2024-09-12', '34500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-12T10:15:00.000Z', '2024-09-12T10:15:00.000Z'
),
(
  'hist_pay_5a5eb9bb15207722', 'treatment', 'Advance', 'Advance', 'hist_6a6e158cd584a0b9', '7004460126', 'Kishanganj', 'NAFIZ ALAM',
  '2024-09-16', '15000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-16T10:15:00.000Z', '2024-09-16T10:15:00.000Z'
),
(
  'hist_pay_b22c8f805bba12c0', 'treatment', 'Advance', 'Advance', 'hist_ed8d70892417f222', '6205532335', 'Kishanganj', 'RAKESH KUMAR',
  '2024-09-16', '19000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-16T10:15:00.000Z', '2024-09-16T10:15:00.000Z'
),
(
  'hist_pay_9e5ca820c949da06', 'treatment', 'Advance', 'Advance', 'hist_1a98f17b56da5e1d', '6202705961', 'Kishanganj', 'WAHID ALAM',
  '2024-09-18', '9000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-18T10:15:00.000Z', '2024-09-18T10:15:00.000Z'
),
(
  'hist_pay_682b4391d52842c5', 'treatment', 'Advance', 'Advance', 'hist_58eb36e8e9b7f9a4', '8371080689', 'Kishanganj', 'TAJBUL HOWK',
  '2024-09-24', '23500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-24T10:15:00.000Z', '2024-09-24T10:15:00.000Z'
),
(
  'hist_pay_66d8e575ec5e9773', 'treatment', 'Advance', 'Advance', 'hist_128a233fb16a8253', '9064909069', 'Kishanganj', 'TAJBUL HOWK',
  '2024-09-30', '21500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-09-30T10:15:00.000Z', '2024-09-30T10:15:00.000Z'
),
(
  'hist_pay_7121c3fff377567c', 'treatment', 'Advance', 'Advance', 'hist_0c6d79d3ee3c3871', '8825544079', 'Kishanganj', 'SHAMIM AKTER',
  '2024-10-02', '24000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-02T10:15:00.000Z', '2024-10-02T10:15:00.000Z'
),
(
  'hist_pay_dc3898fda19415cb', 'treatment', 'Advance', 'Advance', 'hist_52a5e431960f22ea', '7001502725', 'Kishanganj', 'SAGARIKA DAS',
  '2024-10-02', '19500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-02T10:15:00.000Z', '2024-10-02T10:15:00.000Z'
),
(
  'hist_pay_8fc6401795bb1b85', 'treatment', 'Advance', 'Advance', 'hist_390f542502f17f39', '9852947840', 'Kishanganj', 'RINA DEVI',
  '2024-10-02', '14200', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-02T10:15:00.000Z', '2024-10-02T10:15:00.000Z'
),
(
  'hist_pay_706bd9dfedf61f08', 'treatment', 'Advance', 'Advance', 'hist_985a6f7518bd3b94', '8294314745', 'Kishanganj', 'ISRAT KHATOON',
  '2024-10-08', '15000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-08T10:15:00.000Z', '2024-10-08T10:15:00.000Z'
),
(
  'hist_pay_b2cac2f180d17039', 'treatment', 'Advance', 'Advance', 'hist_b028967b1979a719', '8789381453', 'Kishanganj', 'NILAM KUMARI',
  '2024-10-08', '8000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-08T10:15:00.000Z', '2024-10-08T10:15:00.000Z'
),
(
  'hist_pay_7e9166ff530e0c81', 'treatment', 'Advance', 'Advance', 'hist_ebeeb49dab36abdf', '9229252663', 'Kishanganj', 'ALINAZ',
  '2024-10-17', '21500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-17T10:15:00.000Z', '2024-10-17T10:15:00.000Z'
),
(
  'hist_pay_ab3b3d9ee6b8fb0c', 'treatment', 'Advance', 'Advance', 'hist_b4bb5348c43c3b1f', '9382173705', 'Kishanganj', 'MD SAMIM ULLAH',
  '2024-10-19', '15000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-19T10:15:00.000Z', '2024-10-19T10:15:00.000Z'
),
(
  'hist_pay_d50f7787ef77f503', 'treatment', 'Advance', 'Advance', 'hist_88b16d0b9fae91ac', '7352971890', 'Kishanganj', 'MUMTAZIR ALAM',
  '2024-10-19', '15000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-19T10:15:00.000Z', '2024-10-19T10:15:00.000Z'
),
(
  'hist_pay_20d0344c4ad0dc5a', 'treatment', 'Advance', 'Advance', 'hist_ffa418ac2bdeddf7', '9631955398', 'Kishanganj', 'TANVEER ALAM',
  '2024-10-20', '31000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-20T10:15:00.000Z', '2024-10-20T10:15:00.000Z'
),
(
  'hist_pay_4f5ec4c4f6cdcc41', 'treatment', 'Advance', 'Advance', 'hist_1260fd65e66f6239', '8283085614', 'Kishanganj', 'DHANNO DEVI',
  '2024-10-23', '54000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-23T10:15:00.000Z', '2024-10-23T10:15:00.000Z'
),
(
  'hist_pay_97763ecf753dce3b', 'treatment', 'Advance', 'Advance', 'hist_6652182fd94d71d5', '9508884012', 'Kishanganj', 'AMAN KUMAR JHA',
  '2024-10-31', '25000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-10-31T10:15:00.000Z', '2024-10-31T10:15:00.000Z'
),
(
  'hist_pay_840fa1155f027811', 'treatment', 'Advance', 'Advance', 'hist_9ccf482ec2b3bab4', '7004037114', 'Kishanganj', 'KANCHANA KUMARI',
  '2024-11-10', '10000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-10T10:15:00.000Z', '2024-11-10T10:15:00.000Z'
),
(
  'hist_pay_7491ea84d754a5a8', 'treatment', 'Advance', 'Advance', 'hist_4db5ebddda08e93b', '8509685971', 'Kishanganj', 'HASIBUL RAHMAN',
  '2024-11-10', '22000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-10T10:15:00.000Z', '2024-11-10T10:15:00.000Z'
),
(
  'hist_pay_924741c21fcb68f8', 'treatment', 'Advance', 'Advance', 'hist_7b53f4e2299482b9', '9798933610', 'Kishanganj', 'RAMA SANKAR PASWAN',
  '2024-11-11', '9000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-11T10:15:00.000Z', '2024-11-11T10:15:00.000Z'
),
(
  'hist_pay_9e2ee96cea7fa9bd', 'treatment', 'Advance', 'Advance', 'hist_1af770eef6f363ae', '8169353984', 'Kishanganj', 'SADDAM HOSSAIN',
  '2024-11-13', '10100', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-13T10:15:00.000Z', '2024-11-13T10:15:00.000Z'
),
(
  'hist_pay_ca98cf0723d072ec', 'treatment', 'Advance', 'Advance', 'hist_aeb2891553f934fc', '8945064648', 'Kishanganj', 'GAFFAR ALAM',
  '2024-11-14', '39200', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-14T10:15:00.000Z', '2024-11-14T10:15:00.000Z'
),
(
  'hist_pay_4eea8ee6cf1d70e8', 'treatment', 'Advance', 'Advance', 'hist_abf8449b9391aaa7', '7074474859', 'Kishanganj', 'ANWAR ALAM',
  '2024-11-15', '14000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-15T10:15:00.000Z', '2024-11-15T10:15:00.000Z'
),
(
  'hist_pay_0104a9d190e00ec6', 'treatment', 'Advance', 'Advance', 'hist_64b9a2ef17e8d777', '9031546677', 'Kishanganj', 'MD AFRAZ',
  '2024-11-16', '26000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-16T10:15:00.000Z', '2024-11-16T10:15:00.000Z'
),
(
  'hist_pay_dc30aa46ba414b0d', 'treatment', 'Advance', 'Advance', 'hist_386178a425e1e62c', '8250944968', 'Kishanganj', 'NUR ALAM',
  '2024-11-16', '14500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-16T10:15:00.000Z', '2024-11-16T10:15:00.000Z'
),
(
  'hist_pay_e9e91b06a6e7da60', 'treatment', 'Advance', 'Advance', 'hist_96a6d4474fae8663', '9593764670', 'Kishanganj', 'FRJANA BEGAM',
  '2024-11-16', '32000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-16T10:15:00.000Z', '2024-11-16T10:15:00.000Z'
),
(
  'hist_pay_8880fbb83dc33102', 'treatment', 'Advance', 'Advance', 'hist_429a0761ef6171b2', '9932560305', 'Kishanganj', 'SEEBA KHATOON',
  '2024-11-16', '14000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-16T10:15:00.000Z', '2024-11-16T10:15:00.000Z'
),
(
  'hist_pay_1a43cc531793cd7c', 'treatment', 'Advance', 'Advance', 'hist_21e5ada821ed5313', '8101951752', 'Kishanganj', 'MDSALIM',
  '2024-11-18', '15000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-18T10:15:00.000Z', '2024-11-18T10:15:00.000Z'
),
(
  'hist_pay_8d5e49386608c261', 'treatment', 'Advance', 'Advance', 'hist_df3dd215de1da414', '9641261236', 'Kishanganj', 'SAMA BEGAM',
  '2024-11-18', '18000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-18T10:15:00.000Z', '2024-11-18T10:15:00.000Z'
),
(
  'hist_pay_ae28cf8e26eeac2c', 'treatment', 'Advance', 'Advance', 'hist_d0f91a7a40635e64', '7357816622', 'Kishanganj', 'BIKRAM RAJWAN',
  '2024-11-19', '35000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-19T10:15:00.000Z', '2024-11-19T10:15:00.000Z'
),
(
  'hist_pay_6f957b5053eb5f98', 'treatment', 'Advance', 'Advance', 'hist_9739b75a6f667d01', '8851700288', 'Kishanganj', 'MD YASIM',
  '2024-11-24', '14500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-11-24T10:15:00.000Z', '2024-11-24T10:15:00.000Z'
),
(
  'hist_pay_bc4ff3f2c72a52a2', 'treatment', 'Advance', 'Advance', 'hist_c3568c5e2f47caa0', '9860862163', 'Kishanganj', 'CHAYTANNA HASTA',
  '2024-12-02', '44000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-02T10:15:00.000Z', '2024-12-02T10:15:00.000Z'
),
(
  'hist_pay_ae47e1cc5a32d0ed', 'treatment', 'Advance', 'Advance', 'hist_f52e5ad594e4f48d', '9798907615', 'Kishanganj', 'SUJAY KUMAR',
  '2024-12-04', '19000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-04T10:15:00.000Z', '2024-12-04T10:15:00.000Z'
),
(
  'hist_pay_1bce2f6b831dbb9e', 'treatment', 'Advance', 'Advance', 'hist_aac71e6fa4b95e8f', '7478679122', 'Kishanganj', 'NASIMA KHATOON',
  '2024-12-05', '21500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-05T10:15:00.000Z', '2024-12-05T10:15:00.000Z'
),
(
  'hist_pay_08f6936eaac0df38', 'treatment', 'Advance', 'Advance', 'hist_5e60a61f9fe351b9', '7667424208', 'Kishanganj', 'RAJKUMAR',
  '2024-12-11', '28000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-11T10:15:00.000Z', '2024-12-11T10:15:00.000Z'
),
(
  'hist_pay_bff2e11a955bb6ef', 'treatment', 'Advance', 'Advance', 'hist_b1514b253e70065b', '9932680291', 'Kishanganj', 'MAKSUD',
  '2024-12-14', '5500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-14T10:15:00.000Z', '2024-12-14T10:15:00.000Z'
),
(
  'hist_pay_0263878839cc9a78', 'treatment', 'Advance', 'Advance', 'hist_7925631b39d3eb8c', '8371891170', 'Kishanganj', 'SAHANSHA ALAM',
  '2024-12-23', '18600', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-23T10:15:00.000Z', '2024-12-23T10:15:00.000Z'
),
(
  'hist_pay_17518069228f155d', 'treatment', 'Advance', 'Advance', 'hist_9682cf0abf73ab6f', '7021809395', 'Kishanganj', 'KAMRUL HODA',
  '2024-12-28', '17600', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2024-12-28T10:15:00.000Z', '2024-12-28T10:15:00.000Z'
);
