-- কিশানগঞ্জ ধাপ ৩ -- ব্যাচ 2/5 (রোগী 55-108, মোট 54)
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
  'hist_41b8c6a74fd38de2', 'KNE-07072018-002', '2018-07-07', '2018-07-07', '2018-07-07',
  'SAJAN ALAM', '9199969938', 'Kishanganj', '35', 'Male',
  'KHAREBOSTE, HATWAR, KISHANGANJ, KISHANGANJ', 'Piles', '7500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-07-07T10:00:00.000Z', '2018-07-07T10:00:00.000Z'
),
(
  'hist_6836bc4fd4b30094', 'KNE-16072018-001', '2018-07-16', '2018-07-16', '2018-07-16',
  'SONKOR KUMAR DAS', '6296694140', 'Kishanganj', '35', 'Male',
  'THAKURBARI, THAKURBARI, CHAKULIA, UTTAR DINAJPUR', 'Piles', '14000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-07-16T10:00:00.000Z', '2018-07-16T10:00:00.000Z'
),
(
  'hist_3daf80cca1382cfd', 'KNE-28072018-001', '2018-07-28', '2018-07-28', '2018-07-28',
  'SANJIT KUMAR', '7295887937', 'Kishanganj', '18', 'Male',
  'MANEGOLY, CHULLA, KISHANGANJ, KISHANGANJ', 'Hydrocele', '5000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-07-28T10:00:00.000Z', '2018-07-28T10:00:00.000Z'
),
(
  'hist_39225bc892520e38', 'KNE-30072018-001', '2018-07-30', '2018-07-30', '2018-07-30',
  'SHDAM HOHAN', '9113109709', 'Kishanganj', '25', 'Male',
  'RAMPUR, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Hydrocele', '4500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-07-30T10:00:00.000Z', '2018-07-30T10:00:00.000Z'
),
(
  'hist_5712806cfc871656', 'KNE-25082018-001', '2018-08-25', '2018-08-25', '2018-08-25',
  'RIBENDOR KUMAR SING', '9430663012', 'Kishanganj', '24', 'Male',
  'DHARAMGANJ, BALBARIE, CHACADAMAN, KISHANGANJ', 'Hydrocele', '4500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-08-25T10:00:00.000Z', '2018-08-25T10:00:00.000Z'
),
(
  'hist_1e9650b2a7a70500', 'KNE-27082018-001', '2018-08-27', '2018-08-27', '2018-08-27',
  'MD MEHERUL', '7250554512', 'Kishanganj', '', 'Male',
  'SHEGANJ, TILE, BALARAMPUR, KATHIAR', 'Hydrocele', '12000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-08-27T10:00:00.000Z', '2018-08-27T10:00:00.000Z'
),
(
  'hist_b5053ec665b25b15', 'KNE-05092018-001', '2018-09-05', '2018-09-05', '2018-09-05',
  'JARENAI KHATON', '9162508840', 'Kishanganj', '', 'Female',
  'AHAMIGARM, PUTEGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '7000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-09-05T10:00:00.000Z', '2018-09-05T10:00:00.000Z'
),
(
  'hist_1999ae4428aa48a3', 'KNE-28052018-001', '2018-05-28', '2018-05-28', '2018-05-28',
  'AZHER AHOML', '8597906626', 'Kishanganj', '', 'Male',
  'SUSIA, CHULLAI, MONORA, UTTAR DINAJPUR', 'Piles', '9000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-05-28T10:00:00.000Z', '2018-05-28T10:00:00.000Z'
),
(
  'hist_6fc60d86a1e9dc6d', 'KNE-02082018-002', '2018-08-02', '2018-08-02', '2018-08-02',
  'MD HANIB', '7364830234', 'Kishanganj', '45', 'Male',
  'HAITOR, HAITOR, CHULLAI, UTTAR DINAJPUR', 'Hydrocele', '5000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-08-02T10:00:00.000Z', '2018-08-02T10:00:00.000Z'
),
(
  'hist_6ca5b448f0efbb75', 'KNE-22102018-001', '2018-10-22', '2018-10-22', '2018-10-22',
  'KILICHE', '7808988110', 'Kishanganj', '35', 'Male',
  'PARITE, PARITE, BARIHATE, BAKKA', 'Hydrocele', '5000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-10-22T10:00:00.000Z', '2018-10-22T10:00:00.000Z'
),
(
  'hist_a38a13fc8f1c0a18', 'KNE-28102018-001', '2018-10-28', '2018-10-28', '2018-10-28',
  'NURFAR ALAM', '7076537421', 'Kishanganj', '17', 'Male',
  'CHER CHER, GOLPUKHER, GOALPOKHAR, UTTAR DINAJPUR', 'Piles', '8500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-10-28T10:00:00.000Z', '2018-10-28T10:00:00.000Z'
),
(
  'hist_5e8c4d9e34fceea4', 'KNE-31102018-001', '2018-10-31', '2018-10-31', '2018-10-31',
  'RAJU DAS', '7355203959', 'Kishanganj', '32', 'Male',
  'DANGCEA, PANJIPARA, GOALPOKHAR, UTTAR DINAJPUR', 'Hydrocele', '5000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-10-31T10:00:00.000Z', '2018-10-31T10:00:00.000Z'
),
(
  'hist_f8963ed94768e833', 'KNE-20112018-001', '2018-11-20', '2018-11-20', '2018-11-20',
  'ABU KALAM', '9734083627', 'Kishanganj', '', 'Male',
  'CHALLAIA, CHALLAIA, CHALLAIA, UTTAR DINAJPUR', 'Hydrocele', '5500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-11-20T10:00:00.000Z', '2018-11-20T10:00:00.000Z'
),
(
  'hist_1c4fd279461ab817', 'KNE-05122018-001', '2018-12-05', '2018-12-05', '2018-12-05',
  'ROMON THAPA', '9887059614', 'Kishanganj', '31', 'Male',
  'PANJIPARA, PANJIPARA, PANJIPARA, UTTAR DINAJPUR', 'Piles', '9500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-12-05T10:00:00.000Z', '2018-12-05T10:00:00.000Z'
),
(
  'hist_23aeb15b02997e35', 'KNE-20122018-001', '2018-12-20', '2018-12-20', '2018-12-20',
  'SANTOSH', '9795951186', 'Kishanganj', '35', 'Male',
  'KISHANGANJ, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Hydrocele', '4500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-12-20T10:00:00.000Z', '2018-12-20T10:00:00.000Z'
),
(
  'hist_01522ad2effe0473', 'KNE-25122018-001', '2018-12-25', '2018-12-25', '2018-12-25',
  'SHEKEL', '7585927616', 'Kishanganj', '40', 'Male',
  'DALKHOLA, DALKHOLA, DALKHOLA, UTTAR DINAJPUR', 'Hydrocele', '5000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-12-25T10:00:00.000Z', '2018-12-25T10:00:00.000Z'
),
(
  'hist_5611139a794f79ea', 'KNE-06022019-001', '2019-02-06', '2019-02-06', '2019-02-06',
  'MUJIB', '8709233878', 'Kishanganj', '', 'Male',
  'PALIBADI, PIPLA, PAHARKATTA, KISHANGANJ', 'Hydrocele', '10000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2019-02-06T10:00:00.000Z', '2019-02-06T10:00:00.000Z'
),
(
  'hist_a6538e52944e7471', 'KNE-07032019-001', '2019-03-07', '2019-03-07', '2019-03-07',
  'MD ASINIF', '7739686171', 'Kishanganj', '47', 'Male',
  'KISHANGANJ, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Hydrocele', '6000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2019-03-07T10:00:00.000Z', '2019-03-07T10:00:00.000Z'
),
(
  'hist_17ce60c43e0c35d4', 'KNE-25032019-001', '2019-03-25', '2019-03-25', '2019-03-25',
  'SUNILKUMAR', '9914735506', 'Kishanganj', '26', 'Male',
  'MAJLISHPUR, MAJLISHPUR, GOALPOKHAR, UTTAR DINAJPUR', 'Hydrocele', '6500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2019-03-25T10:00:00.000Z', '2019-03-25T10:00:00.000Z'
),
(
  'hist_c87df86d1be2482a', 'KNE-24042019-001', '2019-04-24', '2019-04-24', '2019-04-24',
  'AIKEB', '7986897472', 'Kishanganj', '26', 'Male',
  'PITIKETI, PITIKETI HAYATI, KOCHADHAMAN, KISHANGANJ', 'Piles', '9000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2019-04-24T10:00:00.000Z', '2019-04-24T10:00:00.000Z'
),
(
  'hist_50059656d3013ccd', 'KNE-09052019-001', '2019-05-09', '2019-05-09', '2019-05-09',
  'RAJESH KUMAR', '9735468335', 'Kishanganj', '48', 'Male',
  'KISHANGANJ, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '10500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2019-05-09T10:00:00.000Z', '2019-05-09T10:00:00.000Z'
),
(
  'hist_fbc61014191ad495', 'KNE-03072019-001', '2019-07-03', '2019-07-03', '2019-07-03',
  'MENOHAR', '8967722462', 'Kishanganj', '38', 'Male',
  'ADAMA, KANINDHA, KANINDHA, UTTAR DINAJPUR', 'Hydrocele', '4500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2019-07-03T10:00:00.000Z', '2019-07-03T10:00:00.000Z'
),
(
  'hist_d64c9daf79eafd8f', 'KNE-15072019-001', '2019-07-15', '2019-07-15', '2019-07-15',
  'HAMOTO DAS', '6296694140', 'Kishanganj', '60', 'Male',
  'THAKURBARI, THAKURBARI, CHULLAI, UTTAR DINAJPUR', 'Piles', '12000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2019-07-15T10:00:00.000Z', '2019-07-15T10:00:00.000Z'
),
(
  'hist_ff3d6dfd7f022d90', 'KNE-24072019-001', '2019-07-24', '2019-07-24', '2019-07-24',
  'JERINA KHATUN', '9635700947', 'Kishanganj', '18', 'Female',
  'DHEILYA, HAITOR, KISHANGANJ, KISHANGANJ', 'Piles', '8500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2019-07-24T10:00:00.000Z', '2019-07-24T10:00:00.000Z'
),
(
  'hist_33dd48fe653beace', 'KNE-15092021-001', '2021-09-15', '2021-09-15', '2021-09-15',
  'ASHRAF ALAM', '6200145751', 'Kishanganj', '28', 'Male',
  'DAULA, KHARIBASTI, KISHANGANJ, KISHANGANJ', 'Hydrocele', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2021-09-15T10:00:00.000Z', '2021-09-15T10:00:00.000Z'
),
(
  'hist_8e088a3e99cb638f', 'KNE-15092021-002', '2021-09-15', '2021-09-15', '2021-09-15',
  'MONIR ALAM', '9749588537', 'Kishanganj', '3', 'Male',
  'MONI DANGI, GOLPUKHER, GOLPUKHER, UTTAR DINAJPUR', 'Hydrocele', '9500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2021-09-15T10:00:00.000Z', '2021-09-15T10:00:00.000Z'
),
(
  'hist_54b53479e55757be', 'KNE-26092021-001', '2021-09-26', '2021-09-26', '2021-09-26',
  'BIMA DEVI', '7479755983', 'Kishanganj', '', 'Female',
  'PHOOLBARI, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2021-09-26T10:00:00.000Z', '2021-09-26T10:00:00.000Z'
),
(
  'hist_b07992867a904d0a', 'KNE-02102021-001', '2021-10-02', '2021-10-02', '2021-10-02',
  'ATAUR RAHMAN', '8509699486', 'Kishanganj', '40', 'Male',
  'CHANDPUR, CHANDPUR, GOALPOKHAR, UTTAR DINAJPUR', 'Hydrocele', '6000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2021-10-02T10:00:00.000Z', '2021-10-02T10:00:00.000Z'
),
(
  'hist_3e4790212fe21a50', 'KNE-04122021-001', '2021-12-04', '2021-12-04', '2021-12-04',
  'PRAMOD KR DAS', '7070321482', 'Kishanganj', '', 'Male',
  'KACHARA PATTI, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Fistula', '13000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2021-12-04T10:00:00.000Z', '2021-12-04T10:00:00.000Z'
),
(
  'hist_d3ffa51bfdaa4e43', 'KNE-07022022-001', '2022-02-07', '2022-02-07', '2022-02-07',
  'SUJIT KR DAS', '9470766350', 'Kishanganj', '20', 'Male',
  'FARINGOLA, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '9500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-02-07T10:00:00.000Z', '2022-02-07T10:00:00.000Z'
),
(
  'hist_4426054a182a6c4f', 'KNE-24032022-001', '2022-03-24', '2022-03-24', '2022-03-24',
  'MUSARAF ALAM', '8709579467', 'Kishanganj', '25', 'Male',
  'DOHAPARA, GOALPOKHAR, UTTAR DINAJPUR', 'Other', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-03-24T10:00:00.000Z', '2022-03-24T10:00:00.000Z'
),
(
  'hist_b1ee5f5a0c34736d', 'KNE-13032022-001', '2022-03-13', '2022-03-13', '2022-03-13',
  'SAHEB MAHATO', '9612139380', 'Kishanganj', '34', 'Male',
  'DHARAMGANJ, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Fistula', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-03-13T10:00:00.000Z', '2022-03-13T10:00:00.000Z'
),
(
  'hist_492280821f406290', 'KNE-03042022-001', '2022-04-03', '2022-04-03', '2022-04-03',
  'ANISUR RAHMAN', '9735917074', 'Kishanganj', '45', 'Male',
  'RAJBARI, LALKURI, GOALPOKHAR, UTTAR DINAJPUR', 'Hydrocele', '10500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-04-03T10:00:00.000Z', '2022-04-03T10:00:00.000Z'
),
(
  'hist_e0357a4d210a405b', 'KNE-24042022-001', '2022-04-24', '2022-04-24', '2022-04-24',
  'SAID ANWAR', '8084751487', 'Kishanganj', '28', 'Male',
  'KHANABARI, SARAIKURI, POWAKHALI, KISHANGANJ', 'Fistula', '17000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-04-24T10:00:00.000Z', '2022-04-24T10:00:00.000Z'
),
(
  'hist_10634f129e8adeef', 'KNE-25042022-001', '2022-04-25', '2022-04-25', '2022-04-25',
  'SADHAN BISWAS', '7602262929', 'Kishanganj', '24', 'Male',
  'DANGIPARA, GHATBARI, GOALPOKHAR, UTTAR DINAJPUR', 'Fissure', '12000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-04-25T10:00:00.000Z', '2022-04-25T10:00:00.000Z'
),
(
  'hist_567a8de5d18bfd53', 'KNE-04042022-001', '2022-04-04', '2022-04-04', '2022-04-04',
  'RASTUKHA', '8757587368', 'Kishanganj', '32', 'Male',
  'BAHIRAGOLA, GACHHPARA, KISHANGANJ, KISHANGANJ', 'Hydrocele', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-04-04T10:00:00.000Z', '2022-04-04T10:00:00.000Z'
),
(
  'hist_02aaa60b44405748', 'KNE-03042022-002', '2022-04-03', '2022-04-03', '2022-04-03',
  'ANISUR RAHMAN', '9735917074', 'Kishanganj', '45', 'Male',
  'RAJBARI, LALKURI, GOALPOKHAR, UTTAR DINAJPUR', 'Hydrocele', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-04-03T10:00:00.000Z', '2022-04-03T10:00:00.000Z'
),
(
  'hist_603014968ae1ff2c', 'KNE-07052022-001', '2022-05-07', '2022-05-07', '2022-05-07',
  'BIKI GHOSH', '6203109755', 'Kishanganj', '1.5', 'Male',
  'GARDHAPPA, PANJIPARA, GOALPOKHAR, UTTAR DINAJPUR', 'Hydrocele', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-05-07T10:00:00.000Z', '2022-05-07T10:00:00.000Z'
),
(
  'hist_351e057cd4e100d7', 'KNE-10052022-001', '2022-05-10', '2022-05-10', '2022-05-10',
  'MD MASRIQUL', '8676922172', 'Kishanganj', '24', 'Male',
  'SINGI MOLLI, BELWA, KISHANGANJ, KISHANGANJ', 'Hydrocele', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-05-10T10:00:00.000Z', '2022-05-10T10:00:00.000Z'
),
(
  'hist_d3b65ceed1b273d9', 'KNE-02062022-001', '2022-06-02', '2022-06-02', '2022-06-02',
  'RUPA KUMARI', '8709454680', 'Kishanganj', '25', 'Female',
  'DHARAMGANJ, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Fissure', '14000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-06-02T10:00:00.000Z', '2022-06-02T10:00:00.000Z'
),
(
  'hist_3f4957597322be95', 'KNE-13062022-001', '2022-06-13', '2022-06-13', '2022-06-13',
  'MD LAL', '7718289900', 'Kishanganj', '32', 'Male',
  'NAJIPUR, GOAGAON, GOAGAONG, UTTAR DINAJPUR', 'Piles', '11500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-06-13T10:00:00.000Z', '2022-06-13T10:00:00.000Z'
),
(
  'hist_e81accd0a39718e9', 'KNE-19072022-001', '2022-07-19', '2022-07-19', '2022-07-19',
  'MD FAIZUR RAHMAN', '9733113549', 'Kishanganj', '37', 'Male',
  'ARANI, KHIKITOLA, CHAKULIA, UTTAR DINAJPUR', 'Fistula', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-07-19T10:00:00.000Z', '2022-07-19T10:00:00.000Z'
),
(
  'hist_f83cd1c0020e11e0', 'KNE-13082022-001', '2022-08-13', '2022-08-13', '2022-08-13',
  'GOUTAM YADAR', '9534272089', 'Kishanganj', '55', 'Male',
  'DILAWARGANJ, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Fistula', '24000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-08-13T10:00:00.000Z', '2022-08-13T10:00:00.000Z'
),
(
  'hist_d96d726de0bbde46', 'KNE-06092022-001', '2022-09-06', '2022-09-06', '2022-09-06',
  'GULABUR RAHMAN', '9733303763', 'Kishanganj', '70', 'Male',
  'BIJULIA, THAKURBARI, CHAKULIA, UTTAR DINAJPUR', 'Piles', '14500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-09-06T10:00:00.000Z', '2022-09-06T10:00:00.000Z'
),
(
  'hist_2a9b48f02f3bce29', 'KNE-19102022-001', '2022-10-19', '2022-10-19', '2022-10-19',
  'NOOR HASAN', '9733107882', 'Kishanganj', '8', 'Male',
  'DHARAMPUR, GOALPOKHAR, GOALPOKHAR, UTTAR DINAJPUR', 'Fistula', '13000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-10-19T10:00:00.000Z', '2022-10-19T10:00:00.000Z'
),
(
  'hist_3421c0b418649709', 'KNE-12112022-001', '2022-11-12', '2022-11-12', '2022-11-12',
  'SABIR ALAM', '8436603741', 'Kishanganj', '', 'Male',
  '', 'Piles', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-11-12T10:00:00.000Z', '2022-11-12T10:00:00.000Z'
),
(
  'hist_6fb89959c7dbc4d4', 'KNE-29112022-001', '2022-11-29', '2022-11-29', '2022-11-29',
  'MD MUJAHIR HOSSAIN', '9501823198', 'Kishanganj', '60', 'Male',
  'KAIAL, CHAKULIA, CHAKULIA, UTTAR DINAJPUR', 'Hydrocele', '9500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-11-29T10:00:00.000Z', '2022-11-29T10:00:00.000Z'
),
(
  'hist_5c5023185b42d29c', 'KNE-20122022-001', '2022-12-20', '2022-12-20', '2022-12-20',
  'MD JAKIR', '9304173739', 'Kishanganj', '53', 'Male',
  'BHOLAGACHH, KOIMARI, POTHIA, KISHANGANJ', 'Fistula', '25000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-12-20T10:00:00.000Z', '2022-12-20T10:00:00.000Z'
),
(
  'hist_9ef12b7b68d44bea', 'KNE-15122022-001', '2022-12-15', '2022-12-15', '2022-12-15',
  'TARI ANWAR', '9934330899', 'Kishanganj', '38', 'Male',
  'BISHNUPUR, BISHNUPUR, KOCHADAMAN, UTTAR DINAJPUR', 'Hydrocele', '10000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-12-15T10:00:00.000Z', '2022-12-15T10:00:00.000Z'
),
(
  'hist_5e818a52b4be9974', 'KNE-18112022-001', '2022-11-18', '2022-11-18', '2022-11-18',
  'SABJAD', '6205811780', 'Kishanganj', '30', 'Male',
  'MAHENGAON, HATWAR, KISHANGANJ, KISHANGANJ', 'Gupt Rog', '10000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-11-18T10:00:00.000Z', '2022-11-18T10:00:00.000Z'
),
(
  'hist_f273ca3e5400bafc', 'KNE-03012023-001', '2023-01-03', '2023-01-03', '2023-01-03',
  'AMIR HAMZA', '9907764851', 'Kishanganj', '24', 'Male',
  'LOHAKACHI, THAKURBARI, CHAKULIA, UTTAR DINAJPUR', 'Piles', '8000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-01-03T10:00:00.000Z', '2023-01-03T10:00:00.000Z'
),
(
  'hist_43ec5c57afc9a26b', 'KNE-29122022-001', '2022-12-29', '2022-12-29', '2022-12-29',
  'MANJERI BEGAM', '7250543513', 'Kishanganj', '26', 'Female',
  'FARINGOLA GOLA, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '10000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-12-29T10:00:00.000Z', '2022-12-29T10:00:00.000Z'
),
(
  'hist_a21c96deb9005bb4', 'KNE-17012023-001', '2023-01-17', '2023-01-17', '2023-01-17',
  'MD ANIS', '6207269410', 'Kishanganj', '28', 'Male',
  'TELIVITA, POWAKHALI, POWAKHALI, KISHANGANJ', 'Piles', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-01-17T10:00:00.000Z', '2023-01-17T10:00:00.000Z'
),
(
  'hist_e901199aa5d0f245', 'KNE-18022023-001', '2023-02-18', '2023-02-18', '2023-02-18',
  'RANJIT CHOWDHURY', '7864975681', 'Kishanganj', '40', 'Male',
  'ASHURAGARH, ASHURAGARH, DALKHOLA, UTTAR DINAJPUR', 'Hydrocele', '15000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-18T10:00:00.000Z', '2023-02-18T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'hist_pay_8e95dbd5a8bf3c55', 'treatment', 'Advance', 'Advance', 'hist_41b8c6a74fd38de2', '9199969938', 'Kishanganj', 'SAJAN ALAM',
  '2018-07-07', '7000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-07-07T10:15:00.000Z', '2018-07-07T10:15:00.000Z'
),
(
  'hist_pay_a0c097646eacf130', 'treatment', 'Advance', 'Advance', 'hist_6836bc4fd4b30094', '6296694140', 'Kishanganj', 'SONKOR KUMAR DAS',
  '2018-07-16', '12500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-07-16T10:15:00.000Z', '2018-07-16T10:15:00.000Z'
),
(
  'hist_pay_75fdba2df609d98b', 'treatment', 'Advance', 'Advance', 'hist_3daf80cca1382cfd', '7295887937', 'Kishanganj', 'SANJIT KUMAR',
  '2018-07-28', '5000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-07-28T10:15:00.000Z', '2018-07-28T10:15:00.000Z'
),
(
  'hist_pay_0e5e6541145316fb', 'treatment', 'Advance', 'Advance', 'hist_39225bc892520e38', '9113109709', 'Kishanganj', 'SHDAM HOHAN',
  '2018-07-30', '4100', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-07-30T10:15:00.000Z', '2018-07-30T10:15:00.000Z'
),
(
  'hist_pay_44cc146a6e194922', 'treatment', 'Advance', 'Advance', 'hist_5712806cfc871656', '9430663012', 'Kishanganj', 'RIBENDOR KUMAR SING',
  '2018-08-25', '3000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-08-25T10:15:00.000Z', '2018-08-25T10:15:00.000Z'
),
(
  'hist_pay_069e25e14fb12889', 'treatment', 'Advance', 'Advance', 'hist_1e9650b2a7a70500', '7250554512', 'Kishanganj', 'MD MEHERUL',
  '2018-08-27', '11500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-08-27T10:15:00.000Z', '2018-08-27T10:15:00.000Z'
),
(
  'hist_pay_51fb1d0875900c97', 'treatment', 'Advance', 'Advance', 'hist_b5053ec665b25b15', '9162508840', 'Kishanganj', 'JARENAI KHATON',
  '2018-09-05', '7000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-09-05T10:15:00.000Z', '2018-09-05T10:15:00.000Z'
),
(
  'hist_pay_3e5aeecae13bb954', 'treatment', 'Advance', 'Advance', 'hist_1999ae4428aa48a3', '8597906626', 'Kishanganj', 'AZHER AHOML',
  '2018-05-28', '9000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-05-28T10:15:00.000Z', '2018-05-28T10:15:00.000Z'
),
(
  'hist_pay_60d2fda20797e069', 'treatment', 'Advance', 'Advance', 'hist_6fc60d86a1e9dc6d', '7364830234', 'Kishanganj', 'MD HANIB',
  '2018-08-02', '5000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-08-02T10:15:00.000Z', '2018-08-02T10:15:00.000Z'
),
(
  'hist_pay_491af2dc73fe89c8', 'treatment', 'Advance', 'Advance', 'hist_6ca5b448f0efbb75', '7808988110', 'Kishanganj', 'KILICHE',
  '2018-10-22', '5000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-10-22T10:15:00.000Z', '2018-10-22T10:15:00.000Z'
),
(
  'hist_pay_e1c0c524f0007703', 'treatment', 'Advance', 'Advance', 'hist_a38a13fc8f1c0a18', '7076537421', 'Kishanganj', 'NURFAR ALAM',
  '2018-10-28', '8200', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-10-28T10:15:00.000Z', '2018-10-28T10:15:00.000Z'
),
(
  'hist_pay_fa8081305aa4e682', 'treatment', 'Advance', 'Advance', 'hist_5e8c4d9e34fceea4', '7355203959', 'Kishanganj', 'RAJU DAS',
  '2018-10-31', '5000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-10-31T10:15:00.000Z', '2018-10-31T10:15:00.000Z'
),
(
  'hist_pay_3f77dbfcaed5a3f5', 'treatment', 'Advance', 'Advance', 'hist_f8963ed94768e833', '9734083627', 'Kishanganj', 'ABU KALAM',
  '2018-11-20', '5500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-11-20T10:15:00.000Z', '2018-11-20T10:15:00.000Z'
),
(
  'hist_pay_93932f85106380d0', 'treatment', 'Advance', 'Advance', 'hist_1c4fd279461ab817', '9887059614', 'Kishanganj', 'ROMON THAPA',
  '2018-12-05', '9500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-12-05T10:15:00.000Z', '2018-12-05T10:15:00.000Z'
),
(
  'hist_pay_84e532954133e03f', 'treatment', 'Advance', 'Advance', 'hist_23aeb15b02997e35', '9795951186', 'Kishanganj', 'SANTOSH',
  '2018-12-20', '4500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-12-20T10:15:00.000Z', '2018-12-20T10:15:00.000Z'
),
(
  'hist_pay_527a5ada90bcf33f', 'treatment', 'Advance', 'Advance', 'hist_01522ad2effe0473', '7585927616', 'Kishanganj', 'SHEKEL',
  '2018-12-25', '5000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-12-25T10:15:00.000Z', '2018-12-25T10:15:00.000Z'
),
(
  'hist_pay_a75773b9015c70af', 'treatment', 'Advance', 'Advance', 'hist_5611139a794f79ea', '8709233878', 'Kishanganj', 'MUJIB',
  '2019-02-06', '7600', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2019-02-06T10:15:00.000Z', '2019-02-06T10:15:00.000Z'
),
(
  'hist_pay_f569653992a284f5', 'treatment', 'Advance', 'Advance', 'hist_a6538e52944e7471', '7739686171', 'Kishanganj', 'MD ASINIF',
  '2019-03-07', '4100', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2019-03-07T10:15:00.000Z', '2019-03-07T10:15:00.000Z'
),
(
  'hist_pay_4c876a9d385c4fcf', 'treatment', 'Advance', 'Advance', 'hist_17ce60c43e0c35d4', '9914735506', 'Kishanganj', 'SUNILKUMAR',
  '2019-03-25', '6500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2019-03-25T10:15:00.000Z', '2019-03-25T10:15:00.000Z'
),
(
  'hist_pay_ed58b817468b581c', 'treatment', 'Advance', 'Advance', 'hist_c87df86d1be2482a', '7986897472', 'Kishanganj', 'AIKEB',
  '2019-04-24', '8000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2019-04-24T10:15:00.000Z', '2019-04-24T10:15:00.000Z'
),
(
  'hist_pay_7a6ff82469713185', 'treatment', 'Advance', 'Advance', 'hist_50059656d3013ccd', '9735468335', 'Kishanganj', 'RAJESH KUMAR',
  '2019-05-09', '10000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2019-05-09T10:15:00.000Z', '2019-05-09T10:15:00.000Z'
),
(
  'hist_pay_d5210c492e87fb90', 'treatment', 'Advance', 'Advance', 'hist_fbc61014191ad495', '8967722462', 'Kishanganj', 'MENOHAR',
  '2019-07-03', '4400', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2019-07-03T10:15:00.000Z', '2019-07-03T10:15:00.000Z'
),
(
  'hist_pay_1dbbe4cfc58444f1', 'treatment', 'Advance', 'Advance', 'hist_d64c9daf79eafd8f', '6296694140', 'Kishanganj', 'HAMOTO DAS',
  '2019-07-15', '9800', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2019-07-15T10:15:00.000Z', '2019-07-15T10:15:00.000Z'
),
(
  'hist_pay_fdaa2ded1917cae2', 'treatment', 'Advance', 'Advance', 'hist_ff3d6dfd7f022d90', '9635700947', 'Kishanganj', 'JERINA KHATUN',
  '2019-07-24', '4000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2019-07-24T10:15:00.000Z', '2019-07-24T10:15:00.000Z'
),
(
  'hist_pay_b13611c38bfa9795', 'treatment', 'Advance', 'Advance', 'hist_33dd48fe653beace', '6200145751', 'Kishanganj', 'ASHRAF ALAM',
  '2021-09-15', '5200', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2021-09-15T10:15:00.000Z', '2021-09-15T10:15:00.000Z'
),
(
  'hist_pay_f312d4e8035d9839', 'treatment', 'Advance', 'Advance', 'hist_8e088a3e99cb638f', '9749588537', 'Kishanganj', 'MONIR ALAM',
  '2021-09-15', '7800', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2021-09-15T10:15:00.000Z', '2021-09-15T10:15:00.000Z'
),
(
  'hist_pay_d8584f1b5dc003b7', 'treatment', 'Advance', 'Advance', 'hist_54b53479e55757be', '7479755983', 'Kishanganj', 'BIMA DEVI',
  '2021-09-26', '5380', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2021-09-26T10:15:00.000Z', '2021-09-26T10:15:00.000Z'
),
(
  'hist_pay_b4c4a580d81140d6', 'treatment', 'Advance', 'Advance', 'hist_b07992867a904d0a', '8509699486', 'Kishanganj', 'ATAUR RAHMAN',
  '2021-10-02', '4200', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2021-10-02T10:15:00.000Z', '2021-10-02T10:15:00.000Z'
),
(
  'hist_pay_36143ab81ec4b918', 'treatment', 'Advance', 'Advance', 'hist_3e4790212fe21a50', '7070321482', 'Kishanganj', 'PRAMOD KR DAS',
  '2021-12-04', '10100', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2021-12-04T10:15:00.000Z', '2021-12-04T10:15:00.000Z'
),
(
  'hist_pay_08775e3237513c2d', 'treatment', 'Advance', 'Advance', 'hist_d3ffa51bfdaa4e43', '9470766350', 'Kishanganj', 'SUJIT KR DAS',
  '2022-02-07', '8500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-02-07T10:15:00.000Z', '2022-02-07T10:15:00.000Z'
),
(
  'hist_pay_74a243e345b9c4a1', 'treatment', 'Advance', 'Advance', 'hist_b1ee5f5a0c34736d', '9612139380', 'Kishanganj', 'SAHEB MAHATO',
  '2022-03-13', '10100', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-03-13T10:15:00.000Z', '2022-03-13T10:15:00.000Z'
),
(
  'hist_pay_9bdcd833730eced9', 'treatment', 'Advance', 'Advance', 'hist_492280821f406290', '9735917074', 'Kishanganj', 'ANISUR RAHMAN',
  '2022-04-03', '9500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-04-03T10:15:00.000Z', '2022-04-03T10:15:00.000Z'
),
(
  'hist_pay_16e38ac5ff944e1f', 'treatment', 'Advance', 'Advance', 'hist_e0357a4d210a405b', '8084751487', 'Kishanganj', 'SAID ANWAR',
  '2022-04-24', '16000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-04-24T10:15:00.000Z', '2022-04-24T10:15:00.000Z'
),
(
  'hist_pay_d3053f645e5b1a9f', 'treatment', 'Advance', 'Advance', 'hist_10634f129e8adeef', '7602262929', 'Kishanganj', 'SADHAN BISWAS',
  '2022-04-25', '6000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-04-25T10:15:00.000Z', '2022-04-25T10:15:00.000Z'
),
(
  'hist_pay_21d3982b2f7523c7', 'treatment', 'Advance', 'Advance', 'hist_567a8de5d18bfd53', '8757587368', 'Kishanganj', 'RASTUKHA',
  '2022-04-04', '5000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-04-04T10:15:00.000Z', '2022-04-04T10:15:00.000Z'
),
(
  'hist_pay_ebe4690ebe880bdb', 'treatment', 'Advance', 'Advance', 'hist_603014968ae1ff2c', '6203109755', 'Kishanganj', 'BIKI GHOSH',
  '2022-05-07', '7000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-05-07T10:15:00.000Z', '2022-05-07T10:15:00.000Z'
),
(
  'hist_pay_488600f47f77ac08', 'treatment', 'Advance', 'Advance', 'hist_351e057cd4e100d7', '8676922172', 'Kishanganj', 'MD MASRIQUL',
  '2022-05-10', '10000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-05-10T10:15:00.000Z', '2022-05-10T10:15:00.000Z'
),
(
  'hist_pay_e6bbd027b3df0a98', 'treatment', 'Advance', 'Advance', 'hist_d3b65ceed1b273d9', '8709454680', 'Kishanganj', 'RUPA KUMARI',
  '2022-06-02', '9000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-06-02T10:15:00.000Z', '2022-06-02T10:15:00.000Z'
),
(
  'hist_pay_3a91d8abee2ccabe', 'treatment', 'Advance', 'Advance', 'hist_3f4957597322be95', '7718289900', 'Kishanganj', 'MD LAL',
  '2022-06-13', '5000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-06-13T10:15:00.000Z', '2022-06-13T10:15:00.000Z'
),
(
  'hist_pay_453bdd378e11ce2f', 'treatment', 'Advance', 'Advance', 'hist_e81accd0a39718e9', '9733113549', 'Kishanganj', 'MD FAIZUR RAHMAN',
  '2022-07-19', '31500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-07-19T10:15:00.000Z', '2022-07-19T10:15:00.000Z'
),
(
  'hist_pay_89ef01ce35ea4fa5', 'treatment', 'Advance', 'Advance', 'hist_f83cd1c0020e11e0', '9534272089', 'Kishanganj', 'GOUTAM YADAR',
  '2022-08-13', '22700', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-08-13T10:15:00.000Z', '2022-08-13T10:15:00.000Z'
),
(
  'hist_pay_faa2572d7f7104cc', 'treatment', 'Advance', 'Advance', 'hist_d96d726de0bbde46', '9733303763', 'Kishanganj', 'GULABUR RAHMAN',
  '2022-09-06', '10000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-09-06T10:15:00.000Z', '2022-09-06T10:15:00.000Z'
),
(
  'hist_pay_c189345b28e7d6da', 'treatment', 'Advance', 'Advance', 'hist_2a9b48f02f3bce29', '9733107882', 'Kishanganj', 'NOOR HASAN',
  '2022-10-19', '11000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-10-19T10:15:00.000Z', '2022-10-19T10:15:00.000Z'
),
(
  'hist_pay_af068861085c5d68', 'treatment', 'Advance', 'Advance', 'hist_3421c0b418649709', '8436603741', 'Kishanganj', 'SABIR ALAM',
  '2022-11-12', '11000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-11-12T10:15:00.000Z', '2022-11-12T10:15:00.000Z'
),
(
  'hist_pay_54be8a4139c938e5', 'treatment', 'Advance', 'Advance', 'hist_6fb89959c7dbc4d4', '9501823198', 'Kishanganj', 'MD MUJAHIR HOSSAIN',
  '2022-11-29', '6550', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-11-29T10:15:00.000Z', '2022-11-29T10:15:00.000Z'
),
(
  'hist_pay_77f7a065635825a8', 'treatment', 'Advance', 'Advance', 'hist_5c5023185b42d29c', '9304173739', 'Kishanganj', 'MD JAKIR',
  '2022-12-20', '22800', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-12-20T10:15:00.000Z', '2022-12-20T10:15:00.000Z'
),
(
  'hist_pay_a78c28f6ec9eae76', 'treatment', 'Advance', 'Advance', 'hist_9ef12b7b68d44bea', '9934330899', 'Kishanganj', 'TARI ANWAR',
  '2022-12-15', '7000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-12-15T10:15:00.000Z', '2022-12-15T10:15:00.000Z'
),
(
  'hist_pay_f1fca9e5c47b7e4f', 'treatment', 'Advance', 'Advance', 'hist_5e818a52b4be9974', '6205811780', 'Kishanganj', 'SABJAD',
  '2022-11-18', '19000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-11-18T10:15:00.000Z', '2022-11-18T10:15:00.000Z'
),
(
  'hist_pay_d5c4f3c7f9fe6ca0', 'treatment', 'Advance', 'Advance', 'hist_f273ca3e5400bafc', '9907764851', 'Kishanganj', 'AMIR HAMZA',
  '2023-01-03', '2500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-01-03T10:15:00.000Z', '2023-01-03T10:15:00.000Z'
),
(
  'hist_pay_2445dfa8996fe107', 'treatment', 'Advance', 'Advance', 'hist_43ec5c57afc9a26b', '7250543513', 'Kishanganj', 'MANJERI BEGAM',
  '2022-12-29', '6000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2022-12-29T10:15:00.000Z', '2022-12-29T10:15:00.000Z'
),
(
  'hist_pay_56ba2d84d3e9b859', 'treatment', 'Advance', 'Advance', 'hist_a21c96deb9005bb4', '6207269410', 'Kishanganj', 'MD ANIS',
  '2023-01-17', '10500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-01-17T10:15:00.000Z', '2023-01-17T10:15:00.000Z'
),
(
  'hist_pay_8875a70ff02d54e3', 'treatment', 'Advance', 'Advance', 'hist_e901199aa5d0f245', '7864975681', 'Kishanganj', 'RANJIT CHOWDHURY',
  '2023-02-18', '14000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2023-02-18T10:15:00.000Z', '2023-02-18T10:15:00.000Z'
);
