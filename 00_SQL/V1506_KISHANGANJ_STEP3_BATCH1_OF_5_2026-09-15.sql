-- কিশানগঞ্জ ধাপ ৩ -- ব্যাচ 1/5 (রোগী 1-54, মোট 54)
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
  'hist_a97b8f283ebd84b2', 'KNE-18052017-001', '2017-05-18', '2017-05-18', '2017-05-18',
  'HASIBU RAHMAN', '9635233441', 'Kishanganj', '35', 'Male',
  'BAGESARE, SUJALE, ISLAMPUR, UTTAR DINAJPUR', 'Hydrocele', '6000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-05-18T10:00:00.000Z', '2017-05-18T10:00:00.000Z'
),
(
  'hist_200d777b3e138933', 'KNE-22052017-001', '2017-05-22', '2017-05-22', '2017-05-22',
  'MRS SARAVAN', '7321855372', 'Kishanganj', '45', 'Female',
  'GERABARI, KUTTIKHOLA, KUTTIKHOLA, PURNIA', 'Piles', '3500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-05-22T10:00:00.000Z', '2017-05-22T10:00:00.000Z'
),
(
  'hist_8af2f04e46db61a1', 'KNE-22052017-002', '2017-05-22', '2017-05-22', '2017-05-22',
  'MD AJRAU', '9932949261', 'Kishanganj', '52', 'Male',
  'PANJIPARA, PANJIPARA, GOALPUKHUR, UTTAR DINAJPUR', 'Fistula', '8000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-05-22T10:00:00.000Z', '2017-05-22T10:00:00.000Z'
),
(
  'hist_a19432aa6b29ada0', 'KNE-23052017-001', '2017-05-23', '2017-05-23', '2017-05-23',
  'KHAKRU ROY', '7602142028', 'Kishanganj', '70', 'Male',
  'KURELA, KURELA, GOALPUKHUR, UTTAR DINAJPUR', 'Fistula', '5500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-05-23T10:00:00.000Z', '2017-05-23T10:00:00.000Z'
),
(
  'hist_4d2ff910a3176c76', 'KNE-25052017-001', '2017-05-25', '2017-05-25', '2017-05-25',
  'SANAVAM', '9734140116', 'Kishanganj', '50', 'Male',
  'SIRSI, CHAKULIA, CHAKULIA, UTTAR DINAJPUR', 'Piles', '7000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-05-25T10:00:00.000Z', '2017-05-25T10:00:00.000Z'
),
(
  'hist_9d98c9d69e359f4a', 'KNE-02062017-001', '2017-06-02', '2017-06-02', '2017-06-02',
  'SARFARAJ ALAM', '9931701565', 'Kishanganj', '22', 'Male',
  'SIRSI, PURNIA, PURNIA', 'Piles', '6000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-06-02T10:00:00.000Z', '2017-06-02T10:00:00.000Z'
),
(
  'hist_cbc0017fb3b48dbd', 'KNE-13062017-001', '2017-06-13', '2017-06-13', '2017-06-13',
  'DUIP PANDEY', '7321014368', 'Kishanganj', '18', 'Male',
  'JHARKHAND', 'Hydrocele', '3500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-06-13T10:00:00.000Z', '2017-06-13T10:00:00.000Z'
),
(
  'hist_00d130f48678aadb', 'KNE-19062017-001', '2017-06-19', '2017-06-19', '2017-06-19',
  'AMAR BISWAS', '9733436753', 'Kishanganj', '39', 'Male',
  'NIJAMPUR, NIJAMPUR, CHAKULIA, UTTAR DINAJPUR', 'Piles', '5500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-06-19T10:00:00.000Z', '2017-06-19T10:00:00.000Z'
),
(
  'hist_874433b81472b758', 'KNE-12082017-001', '2017-08-12', '2017-08-12', '2017-08-12',
  'HASAN ALI', '8759413116', 'Kishanganj', '50', 'Male',
  'MALDUAR, BARAMALIERPUR, GAJAL, UTTAR DINAJPUR', 'Piles', '7500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-08-12T10:00:00.000Z', '2017-08-12T10:00:00.000Z'
),
(
  'hist_9c6b230186db4888', 'KNE-05092017-001', '2017-09-05', '2017-09-05', '2017-09-05',
  'RADHA RANI DAS', '9932521411', 'Kishanganj', '57', 'Female',
  'RADHA RITA COLONY, SHREE KRISHNAPUR, ISLAMPUR, UTTAR DINAJPUR', 'Piles', '5500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-09-05T10:00:00.000Z', '2017-09-05T10:00:00.000Z'
),
(
  'hist_4ab6ec619b0fdfe3', 'KNE-12092017-001', '2017-09-12', '2017-09-12', '2017-09-12',
  'TAL BINDA SINGH', '9470867082', 'Kishanganj', '42', 'Male',
  'DHARAMGANJ, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Hydrocele', '4500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-09-12T10:00:00.000Z', '2017-09-12T10:00:00.000Z'
),
(
  'hist_e36acac271123c68', 'KNE-07092017-001', '2017-09-07', '2017-09-07', '2017-09-07',
  'SUTTAN', '9102420111', 'Kishanganj', '40', 'Male',
  'KISHANGANJ, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Gupt Rog', '5000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-09-07T10:00:00.000Z', '2017-09-07T10:00:00.000Z'
),
(
  'hist_e5aeb3927ea85499', 'KNE-10092017-001', '2017-09-10', '2017-09-10', '2017-09-10',
  'LAKSHMAN SINGH', '8409874627', 'Kishanganj', '25', 'Male',
  'PANJIPARA BSF CAMP, PANJIPARA, PANJIPARA, UTTAR DINAJPUR', 'Piles', '6000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-09-10T10:00:00.000Z', '2017-09-10T10:00:00.000Z'
),
(
  'hist_61de18f8a646943f', 'KNE-12092017-002', '2017-09-12', '2017-09-12', '2017-09-12',
  'ASARU ROY', '8348775559', 'Kishanganj', '22', 'Male',
  'CHAKULIA, CHAKULIA, CHAKULIA, UTTAR DINAJPUR', 'Piles', '7000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-09-12T10:00:00.000Z', '2017-09-12T10:00:00.000Z'
),
(
  'hist_680095451acff051', 'KNE-14092017-001', '2017-09-14', '2017-09-14', '2017-09-14',
  'ABDUR RAHIM', '7063373792', 'Kishanganj', '42', 'Male',
  'JHAPATOL, SINGIA, PAHARKATTA, KISHANGANJ', 'Hydrocele', '6500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-09-14T10:00:00.000Z', '2017-09-14T10:00:00.000Z'
),
(
  'hist_6c0d15fdb52314dd', 'KNE-15092017-001', '2017-09-15', '2017-09-15', '2017-09-15',
  'RAM NARAYAN RAM', '9475104523', 'Kishanganj', '40', 'Male',
  'RAIGANJ, RAIGANJ, RAIGANJ, UTTAR DINAJPUR', 'Other', '3500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-09-15T10:00:00.000Z', '2017-09-15T10:00:00.000Z'
),
(
  'hist_678f5d8430d7b1f4', 'KNE-15092017-002', '2017-09-15', '2017-09-15', '2017-09-15',
  'SENBOL HAWK', '9547117843', 'Kishanganj', '38', 'Male',
  'NANERPUR, DUMONAL, ISLAMPUR, UTTAR DINAJPUR', 'Hydrocele', '6500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-09-15T10:00:00.000Z', '2017-09-15T10:00:00.000Z'
),
(
  'hist_bf2a7ff213d5259c', 'KNE-09102017-001', '2017-10-09', '2017-10-09', '2017-10-09',
  'BAPPA DAS', '9775915433', 'Kishanganj', '23', 'Male',
  'KALIARA', 'Gupt Rog', '2600',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-10-09T10:00:00.000Z', '2017-10-09T10:00:00.000Z'
),
(
  'hist_7c18d9ccc4355152', 'KNE-04102017-001', '2017-10-04', '2017-10-04', '2017-10-04',
  'MD MAHABUD', '9733175908', 'Kishanganj', '37', 'Male',
  'DHOKABARI, DHORKA, GOLERPUR, UTTAR DINAJPUR', 'Piles', '8000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-10-04T10:00:00.000Z', '2017-10-04T10:00:00.000Z'
),
(
  'hist_9a4bb5fecd8840eb', 'KNE-05102017-001', '2017-10-05', '2017-10-05', '2017-10-05',
  'AILL HASHAN', '9570573925', 'Kishanganj', '28', 'Male',
  'LINE CHOAR, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '7000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-10-05T10:00:00.000Z', '2017-10-05T10:00:00.000Z'
),
(
  'hist_538a8454564cfc2e', 'KNE-06112017-001', '2017-11-06', '2017-11-06', '2017-11-06',
  'LAXMAN SHING', '7477388234', 'Kishanganj', '44', 'Male',
  'PANJIPARA BSF CAMP, PANJIPARA BSF CAMP, PANJIPARA BSF CAMP, UTTAR DINAJPUR', 'Piles', '8000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-11-06T10:00:00.000Z', '2017-11-06T10:00:00.000Z'
),
(
  'hist_48774119500bb261', 'KNE-10102017-001', '2017-10-10', '2017-10-10', '2017-10-10',
  'MD HAIB ALAM', '9933921206', 'Kishanganj', '24', 'Male',
  'SHAIKARPUR, LALGANJ, CHAULA, UTTAR DINAJPUR', 'Hydrocele', '5000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-10-10T10:00:00.000Z', '2017-10-10T10:00:00.000Z'
),
(
  'hist_71d4822aecf929b9', 'KNE-23102017-001', '2017-10-23', '2017-10-23', '2017-10-23',
  'JETENDAR KUMAR', '8757684190', 'Kishanganj', '21', 'Male',
  'DHONSONA, DHONSONA, DHONSONA, KISHANGANJ', 'Piles', '9700',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-10-23T10:00:00.000Z', '2017-10-23T10:00:00.000Z'
),
(
  'hist_53143defb1ee98ff', 'KNE-04112017-001', '2017-11-04', '2017-11-04', '2017-11-04',
  'LAXIM PROSHAD', '8392592565', 'Kishanganj', '51', 'Male',
  'DIGHALVAI, DIGHALVAI, DIGHALVAI, KISHANGANJ', 'Gupt Rog', '3000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-11-04T10:00:00.000Z', '2017-11-04T10:00:00.000Z'
),
(
  'hist_e295d6a7cc7c3701', 'KNE-08112017-001', '2017-11-08', '2017-11-08', '2017-11-08',
  'AVIRAM MANDY', '8436903797', 'Kishanganj', '21', 'Male',
  'AMLIYA, AMLIYA, CHAKULIA, UTTAR DINAJPUR', 'Gupt Rog', '0',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-11-08T10:00:00.000Z', '2017-11-08T10:00:00.000Z'
),
(
  'hist_f453594c22be98a4', 'KNE-27112017-001', '2017-11-27', '2017-11-27', '2017-11-27',
  'S NAWAZ', '7762955210', 'Kishanganj', '25', 'Male',
  'CHURIPATTI, CHURIPATTI, CHURIPATTI, KISHANGANJ', 'Hydrocele', '4600',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-11-27T10:00:00.000Z', '2017-11-27T10:00:00.000Z'
),
(
  'hist_e76f5c1716b88545', 'KNE-27112017-002', '2017-11-27', '2017-11-27', '2017-11-27',
  'ABDUL HAKIM', '9733738922', 'Kishanganj', '35', 'Male',
  'BARRAN, CHAKULIA, CHAKULIA, UTTAR DINAJPUR', 'Gupt Rog', '3600',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-11-27T10:00:00.000Z', '2017-11-27T10:00:00.000Z'
),
(
  'hist_b79cf826c76a7c5e', 'KNE-28112017-001', '2017-11-28', '2017-11-28', '2017-11-28',
  'RP SAWAN', '7070932121', 'Kishanganj', '50', 'Male',
  'KISHANGANJ, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Hydrocele', '6000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-11-28T10:00:00.000Z', '2017-11-28T10:00:00.000Z'
),
(
  'hist_1ce621af290b9e6a', 'KNE-05122017-001', '2017-12-05', '2017-12-05', '2017-12-05',
  'ALI HOSEN', '8972432385', 'Kishanganj', '74', 'Male',
  'FUKIRDANGI, ISLAMPUR, ISLAMPUR, UTTAR DINAJPUR', 'Piles', '5800',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-12-05T10:00:00.000Z', '2017-12-05T10:00:00.000Z'
),
(
  'hist_293228aff1f775fc', 'KNE-19122017-001', '2017-12-19', '2017-12-19', '2017-12-19',
  'MD TAKIR HUSEN', '7831098259', 'Kishanganj', '33', 'Male',
  'KANKI, KANKI, KANKI, UTTAR DINAJPUR', 'Piles', '9600',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-12-19T10:00:00.000Z', '2017-12-19T10:00:00.000Z'
),
(
  'hist_255f046d5bd726bd', 'KNE-05122017-002', '2017-12-05', '2017-12-05', '2017-12-05',
  'TETAR PASWAN', '9631032188', 'Kishanganj', '35', 'Male',
  'MAHAIRGOOIN, KHOTO, KISHANGANJ, KISHANGANJ', 'Fistula', '30000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-12-05T10:00:00.000Z', '2017-12-05T10:00:00.000Z'
),
(
  'hist_f0700697edc6cb50', 'KNE-15012018-001', '2018-01-15', '2018-01-15', '2018-01-15',
  'MD USMAN', '9264161874', 'Kishanganj', '57', 'Male',
  'LAHIAKANDAR, MEMERGANJ, MEMERGANJ, KISHANGANJ', 'Hydrocele', '11800',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-01-15T10:00:00.000Z', '2018-01-15T10:00:00.000Z'
),
(
  'hist_7dc696d008c279c2', 'KNE-06022018-001', '2018-02-06', '2018-02-06', '2018-02-06',
  'GHURAN PASWAN', '7319544339', 'Kishanganj', '58', 'Male',
  'KANKI, KANKI, UTTAR DINAJPUR', 'Hydrocele', '4200',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-02-06T10:00:00.000Z', '2018-02-06T10:00:00.000Z'
),
(
  'hist_2f7d12f0f780b0c4', 'KNE-19022018-001', '2018-02-19', '2018-02-19', '2018-02-19',
  'RAHAMAT ULLESS', '9973257425', 'Kishanganj', '36', 'Male',
  'KAHAPUR, SINGSIA, KISHANGANJ, KISHANGANJ', 'Hydrocele', '5000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-02-19T10:00:00.000Z', '2018-02-19T10:00:00.000Z'
),
(
  'hist_9b20aad2fb30a74e', 'KNE-20022018-001', '2018-02-20', '2018-02-20', '2018-02-20',
  'FULLCHAN YADEVE', '8002001178', 'Kishanganj', '70', 'Male',
  'ISLAMPUR, ISLAMPUR, ISLAMPUR, UTTAR DINAJPUR', 'Piles', '20000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-02-20T10:00:00.000Z', '2018-02-20T10:00:00.000Z'
),
(
  'hist_56047e330063a5e3', 'KNE-01032018-001', '2018-03-01', '2018-03-01', '2018-03-01',
  'ASMARA KHATUN', '8348144033', 'Kishanganj', '', 'Female',
  'PARTHABARI, VAVARA, CHAKULIA, UTTAR DINAJPUR', 'Piles', '8000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-03-01T10:00:00.000Z', '2018-03-01T10:00:00.000Z'
),
(
  'hist_8ad8dc8aca851657', 'KNE-05032018-001', '2018-03-05', '2018-03-05', '2018-03-05',
  'SANJAY', '9534568245', 'Kishanganj', '36', 'Male',
  'KHARAGARAM, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Hydrocele', '4500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-03-05T10:00:00.000Z', '2018-03-05T10:00:00.000Z'
),
(
  'hist_81ab8ba0115a9307', 'KNE-12032018-001', '2018-03-12', '2018-03-12', '2018-03-12',
  'SANOJ ROY', '6200477620', 'Kishanganj', '30', 'Male',
  'KANKI, KANKI, KANKI, UTTAR DINAJPUR', 'Hydrocele', '3500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-03-12T10:00:00.000Z', '2018-03-12T10:00:00.000Z'
),
(
  'hist_005579f5f2d3a4f9', 'KNE-12032018-002', '2018-03-12', '2018-03-12', '2018-03-12',
  'KHURSHED', '7356948784', 'Kishanganj', '22', 'Male',
  'NIZAMPUR, CHAKULIA, CHAKULIA, UTTAR DINAJPUR', 'Piles', '8500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-03-12T10:00:00.000Z', '2018-03-12T10:00:00.000Z'
),
(
  'hist_939ee05a47670777', 'KNE-14042018-001', '2018-04-14', '2018-04-14', '2018-04-14',
  'NARESH KUMAR', '7602968409', 'Kishanganj', '46', 'Male',
  'SHERPUR, SHERPUR, SHERPUR, UTTAR DINAJPUR', 'Piles', '8000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-04-14T10:00:00.000Z', '2018-04-14T10:00:00.000Z'
),
(
  'hist_85502f7467e93c98', 'KNE-16042018-001', '2018-04-16', '2018-04-16', '2018-04-16',
  'SRAWAN KUMAR', '7783877278', 'Kishanganj', '33', 'Male',
  'PURAMA KHAGAR, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '9500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-04-16T10:00:00.000Z', '2018-04-16T10:00:00.000Z'
),
(
  'hist_ece501bc6fb14ca1', 'KNE-10042018-001', '2018-04-10', '2018-04-10', '2018-04-10',
  'POBITRO BISWAS', '9609050488', 'Kishanganj', '', 'Male',
  'ISLAMPUR, ISLAMPUR, ISLAMPUR, UTTAR DINAJPUR', 'Piles', '5000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-04-10T10:00:00.000Z', '2018-04-10T10:00:00.000Z'
),
(
  'hist_322fb585684e4024', 'KNE-26032018-001', '2018-03-26', '2018-03-26', '2018-03-26',
  'AEMIRA', '7870104590', 'Kishanganj', '16', 'Male',
  'CHURIPATTI, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '6000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-03-26T10:00:00.000Z', '2018-03-26T10:00:00.000Z'
),
(
  'hist_665b56a718ba25b5', 'KNE-01032018-002', '2018-03-01', '2018-03-01', '2018-03-01',
  'RAKESH CHAIOM', '8409966529', 'Kishanganj', '47', 'Male',
  'LAINE PARIA, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '7500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-03-01T10:00:00.000Z', '2018-03-01T10:00:00.000Z'
),
(
  'hist_8c748a5f455fb91a', 'KNE-23042018-001', '2018-04-23', '2018-04-23', '2018-04-23',
  'NAJER HASHAN', '7063129888', 'Kishanganj', '55', 'Male',
  'SHENATHA, DHARMPUR, GOLPUKHER, UTTAR DINAJPUR', 'Hydrocele', '7500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-04-23T10:00:00.000Z', '2018-04-23T10:00:00.000Z'
),
(
  'hist_aeb1789154107caf', 'KNE-09052018-001', '2018-05-09', '2018-05-09', '2018-05-09',
  'MANEJIB ALAM', '7761003671', 'Kishanganj', '', 'Male',
  'KHARE BASTE, HAITHER, KISHANGANJ, KISHANGANJ', 'Piles', '8500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-05-09T10:00:00.000Z', '2018-05-09T10:00:00.000Z'
),
(
  'hist_7583a9c61c15613d', 'KNE-19052018-001', '2018-05-19', '2018-05-19', '2018-05-19',
  'SHERPAL SINGH', '9877727267', 'Kishanganj', '51', 'Male',
  'PANJIPARA BSF CAMP, PANJIPARA BSF CAMP, PANJIPARA BSF CAMP, UTTAR DINAJPUR', 'Hydrocele', '9500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-05-19T10:00:00.000Z', '2018-05-19T10:00:00.000Z'
),
(
  'hist_4e12da8d023dbafa', 'KNE-21052018-001', '2018-05-21', '2018-05-21', '2018-05-21',
  'JAMAIL AKTHER', '8789997428', 'Kishanganj', '50', 'Male',
  'KUMASHUM, HALAIM, HALAIM, UTTAR DINAJPUR', 'Piles', '12000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-05-21T10:00:00.000Z', '2018-05-21T10:00:00.000Z'
),
(
  'hist_d3822a0f87ed469c', 'KNE-24052018-001', '2018-05-24', '2018-05-24', '2018-05-24',
  'RIEDO MOEDOY', '8170813773', 'Kishanganj', '46', 'Male',
  'PANJIPARA, PANJIPARA, GOLPUKHER, UTTAR DINAJPUR', 'Piles', '8500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-05-24T10:00:00.000Z', '2018-05-24T10:00:00.000Z'
),
(
  'hist_9f8ebaac5e1649cc', 'KNE-23052018-001', '2018-05-23', '2018-05-23', '2018-05-23',
  'ABDUL SATTAR', '7870741257', 'Kishanganj', '28', 'Male',
  'HALIMCHOK, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Hydrocele', '9000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-05-23T10:00:00.000Z', '2018-05-23T10:00:00.000Z'
),
(
  'hist_9cef16602a565568', 'KNE-27052018-001', '2018-05-27', '2018-05-27', '2018-05-27',
  'MD MUMATZ', '7654462407', 'Kishanganj', '', 'Male',
  'CHURIPATTI, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '12500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-05-27T10:00:00.000Z', '2018-05-27T10:00:00.000Z'
),
(
  'hist_b25891c463870b76', 'KNE-29052018-001', '2018-05-29', '2018-05-29', '2018-05-29',
  'NOOR ZAMAL', '8158836975', 'Kishanganj', '27', 'Male',
  'DAKHEMPUKURE, DHEROMPUR, GOLPUKHUR, UTTAR DINAJPUR', 'Piles', '28000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-05-29T10:00:00.000Z', '2018-05-29T10:00:00.000Z'
),
(
  'hist_46156363082ed886', 'KNE-27062018-001', '2018-06-27', '2018-06-27', '2018-06-27',
  'JUMED ALAM', '7602897286', 'Kishanganj', '9', 'Male',
  'FULLEREY, KHEREMARY, PHUITIA, KISHANGANJ', 'Hydrocele', '4000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-06-27T10:00:00.000Z', '2018-06-27T10:00:00.000Z'
),
(
  'hist_5cd778d080ec97ce', 'KNE-07072018-001', '2018-07-07', '2018-07-07', '2018-07-07',
  'GHALAM RABANIE', '7797811931', 'Kishanganj', '24', 'Male',
  'SHAPUR, HOTI, GOLPUKUR, UTTAR DINAJPUR', 'Piles', '8000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-07-07T10:00:00.000Z', '2018-07-07T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'hist_pay_777d9628ebeadf29', 'treatment', 'Advance', 'Advance', 'hist_a97b8f283ebd84b2', '9635233441', 'Kishanganj', 'HASIBU RAHMAN',
  '2017-05-18', '4700', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-05-18T10:15:00.000Z', '2017-05-18T10:15:00.000Z'
),
(
  'hist_pay_f9438db642e2c706', 'treatment', 'Advance', 'Advance', 'hist_200d777b3e138933', '7321855372', 'Kishanganj', 'MRS SARAVAN',
  '2017-05-22', '3500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-05-22T10:15:00.000Z', '2017-05-22T10:15:00.000Z'
),
(
  'hist_pay_cf2daf204decfb93', 'treatment', 'Advance', 'Advance', 'hist_8af2f04e46db61a1', '9932949261', 'Kishanganj', 'MD AJRAU',
  '2017-05-22', '7400', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-05-22T10:15:00.000Z', '2017-05-22T10:15:00.000Z'
),
(
  'hist_pay_5177b429e6b0ba2d', 'treatment', 'Advance', 'Advance', 'hist_a19432aa6b29ada0', '7602142028', 'Kishanganj', 'KHAKRU ROY',
  '2017-05-23', '5900', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-05-23T10:15:00.000Z', '2017-05-23T10:15:00.000Z'
),
(
  'hist_pay_d474afec44bcd62c', 'treatment', 'Advance', 'Advance', 'hist_4d2ff910a3176c76', '9734140116', 'Kishanganj', 'SANAVAM',
  '2017-05-25', '6700', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-05-25T10:15:00.000Z', '2017-05-25T10:15:00.000Z'
),
(
  'hist_pay_9e7c73374cbafbe8', 'treatment', 'Advance', 'Advance', 'hist_9d98c9d69e359f4a', '9931701565', 'Kishanganj', 'SARFARAJ ALAM',
  '2017-06-02', '5500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-06-02T10:15:00.000Z', '2017-06-02T10:15:00.000Z'
),
(
  'hist_pay_c65fa0568c36943b', 'treatment', 'Advance', 'Advance', 'hist_cbc0017fb3b48dbd', '7321014368', 'Kishanganj', 'DUIP PANDEY',
  '2017-06-13', '3500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-06-13T10:15:00.000Z', '2017-06-13T10:15:00.000Z'
),
(
  'hist_pay_6eadcc12365da021', 'treatment', 'Advance', 'Advance', 'hist_00d130f48678aadb', '9733436753', 'Kishanganj', 'AMAR BISWAS',
  '2017-06-19', '5000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-06-19T10:15:00.000Z', '2017-06-19T10:15:00.000Z'
),
(
  'hist_pay_210f59172a9f7e91', 'treatment', 'Advance', 'Advance', 'hist_874433b81472b758', '8759413116', 'Kishanganj', 'HASAN ALI',
  '2017-08-12', '6500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-08-12T10:15:00.000Z', '2017-08-12T10:15:00.000Z'
),
(
  'hist_pay_de8ed16739b0d1dd', 'treatment', 'Advance', 'Advance', 'hist_9c6b230186db4888', '9932521411', 'Kishanganj', 'RADHA RANI DAS',
  '2017-09-05', '5500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-09-05T10:15:00.000Z', '2017-09-05T10:15:00.000Z'
),
(
  'hist_pay_a3f8d1a3a2f1ef16', 'treatment', 'Advance', 'Advance', 'hist_4ab6ec619b0fdfe3', '9470867082', 'Kishanganj', 'TAL BINDA SINGH',
  '2017-09-12', '3000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-09-12T10:15:00.000Z', '2017-09-12T10:15:00.000Z'
),
(
  'hist_pay_bde46bc178d5342a', 'treatment', 'Advance', 'Advance', 'hist_e36acac271123c68', '9102420111', 'Kishanganj', 'SUTTAN',
  '2017-09-07', '4500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-09-07T10:15:00.000Z', '2017-09-07T10:15:00.000Z'
),
(
  'hist_pay_0a42ba69c658b2fb', 'treatment', 'Advance', 'Advance', 'hist_e5aeb3927ea85499', '8409874627', 'Kishanganj', 'LAKSHMAN SINGH',
  '2017-09-10', '6000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-09-10T10:15:00.000Z', '2017-09-10T10:15:00.000Z'
),
(
  'hist_pay_60be141be66b8b55', 'treatment', 'Advance', 'Advance', 'hist_61de18f8a646943f', '8348775559', 'Kishanganj', 'ASARU ROY',
  '2017-09-12', '7000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-09-12T10:15:00.000Z', '2017-09-12T10:15:00.000Z'
),
(
  'hist_pay_bdbacaf477d7485d', 'treatment', 'Advance', 'Advance', 'hist_680095451acff051', '7063373792', 'Kishanganj', 'ABDUR RAHIM',
  '2017-09-14', '900', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-09-14T10:15:00.000Z', '2017-09-14T10:15:00.000Z'
),
(
  'hist_pay_10abaa239133905f', 'treatment', 'Advance', 'Advance', 'hist_6c0d15fdb52314dd', '9475104523', 'Kishanganj', 'RAM NARAYAN RAM',
  '2017-09-15', '1000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-09-15T10:15:00.000Z', '2017-09-15T10:15:00.000Z'
),
(
  'hist_pay_44919b4b15c2c415', 'treatment', 'Advance', 'Advance', 'hist_678f5d8430d7b1f4', '9547117843', 'Kishanganj', 'SENBOL HAWK',
  '2017-09-15', '5500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-09-15T10:15:00.000Z', '2017-09-15T10:15:00.000Z'
),
(
  'hist_pay_9588743c73690d98', 'treatment', 'Advance', 'Advance', 'hist_bf2a7ff213d5259c', '9775915433', 'Kishanganj', 'BAPPA DAS',
  '2017-10-09', '500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-10-09T10:15:00.000Z', '2017-10-09T10:15:00.000Z'
),
(
  'hist_pay_2705b1c189d3ff58', 'treatment', 'Advance', 'Advance', 'hist_7c18d9ccc4355152', '9733175908', 'Kishanganj', 'MD MAHABUD',
  '2017-10-04', '8000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-10-04T10:15:00.000Z', '2017-10-04T10:15:00.000Z'
),
(
  'hist_pay_09262f61979f09d1', 'treatment', 'Advance', 'Advance', 'hist_9a4bb5fecd8840eb', '9570573925', 'Kishanganj', 'AILL HASHAN',
  '2017-10-05', '6000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-10-05T10:15:00.000Z', '2017-10-05T10:15:00.000Z'
),
(
  'hist_pay_a3bc2c0a29b74b5e', 'treatment', 'Advance', 'Advance', 'hist_538a8454564cfc2e', '7477388234', 'Kishanganj', 'LAXMAN SHING',
  '2017-11-06', '8000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-11-06T10:15:00.000Z', '2017-11-06T10:15:00.000Z'
),
(
  'hist_pay_ab51f0cee1b4b649', 'treatment', 'Advance', 'Advance', 'hist_48774119500bb261', '9933921206', 'Kishanganj', 'MD HAIB ALAM',
  '2017-10-10', '3400', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-10-10T10:15:00.000Z', '2017-10-10T10:15:00.000Z'
),
(
  'hist_pay_ca26732e0d8c9d8f', 'treatment', 'Advance', 'Advance', 'hist_71d4822aecf929b9', '8757684190', 'Kishanganj', 'JETENDAR KUMAR',
  '2017-10-23', '3200', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-10-23T10:15:00.000Z', '2017-10-23T10:15:00.000Z'
),
(
  'hist_pay_08c912f78c4242e8', 'treatment', 'Advance', 'Advance', 'hist_53143defb1ee98ff', '8392592565', 'Kishanganj', 'LAXIM PROSHAD',
  '2017-11-04', '2200', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-11-04T10:15:00.000Z', '2017-11-04T10:15:00.000Z'
),
(
  'hist_pay_9ab1a2ca6580acab', 'treatment', 'Advance', 'Advance', 'hist_e295d6a7cc7c3701', '8436903797', 'Kishanganj', 'AVIRAM MANDY',
  '2017-11-08', '2700', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-11-08T10:15:00.000Z', '2017-11-08T10:15:00.000Z'
),
(
  'hist_pay_64d2d1a8546fbd46', 'treatment', 'Advance', 'Advance', 'hist_f453594c22be98a4', '7762955210', 'Kishanganj', 'S NAWAZ',
  '2017-11-27', '1500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-11-27T10:15:00.000Z', '2017-11-27T10:15:00.000Z'
),
(
  'hist_pay_25c446b282f80a70', 'treatment', 'Advance', 'Advance', 'hist_e76f5c1716b88545', '9733738922', 'Kishanganj', 'ABDUL HAKIM',
  '2017-11-27', '1200', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-11-27T10:15:00.000Z', '2017-11-27T10:15:00.000Z'
),
(
  'hist_pay_6477ffbe41fb5563', 'treatment', 'Advance', 'Advance', 'hist_b79cf826c76a7c5e', '7070932121', 'Kishanganj', 'RP SAWAN',
  '2017-11-28', '4700', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-11-28T10:15:00.000Z', '2017-11-28T10:15:00.000Z'
),
(
  'hist_pay_ad15245669e1d256', 'treatment', 'Advance', 'Advance', 'hist_1ce621af290b9e6a', '8972432385', 'Kishanganj', 'ALI HOSEN',
  '2017-12-05', '4000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-12-05T10:15:00.000Z', '2017-12-05T10:15:00.000Z'
),
(
  'hist_pay_3f2bc6dadef0f791', 'treatment', 'Advance', 'Advance', 'hist_293228aff1f775fc', '7831098259', 'Kishanganj', 'MD TAKIR HUSEN',
  '2017-12-19', '9300', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-12-19T10:15:00.000Z', '2017-12-19T10:15:00.000Z'
),
(
  'hist_pay_b8be3388f233a075', 'treatment', 'Advance', 'Advance', 'hist_255f046d5bd726bd', '9631032188', 'Kishanganj', 'TETAR PASWAN',
  '2017-12-05', '28700', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-12-05T10:15:00.000Z', '2017-12-05T10:15:00.000Z'
),
(
  'hist_pay_6fb624cf451a2718', 'treatment', 'Advance', 'Advance', 'hist_f0700697edc6cb50', '9264161874', 'Kishanganj', 'MD USMAN',
  '2018-01-15', '11000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-01-15T10:15:00.000Z', '2018-01-15T10:15:00.000Z'
),
(
  'hist_pay_939a7df5eeeb61d1', 'treatment', 'Advance', 'Advance', 'hist_7dc696d008c279c2', '7319544339', 'Kishanganj', 'GHURAN PASWAN',
  '2018-02-06', '4100', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-02-06T10:15:00.000Z', '2018-02-06T10:15:00.000Z'
),
(
  'hist_pay_a4b258b904b95c4f', 'treatment', 'Advance', 'Advance', 'hist_2f7d12f0f780b0c4', '9973257425', 'Kishanganj', 'RAHAMAT ULLESS',
  '2018-02-19', '5000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-02-19T10:15:00.000Z', '2018-02-19T10:15:00.000Z'
),
(
  'hist_pay_adf440fe485b142c', 'treatment', 'Advance', 'Advance', 'hist_9b20aad2fb30a74e', '8002001178', 'Kishanganj', 'FULLCHAN YADEVE',
  '2018-02-20', '10000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-02-20T10:15:00.000Z', '2018-02-20T10:15:00.000Z'
),
(
  'hist_pay_66a37af92c33aec6', 'treatment', 'Advance', 'Advance', 'hist_56047e330063a5e3', '8348144033', 'Kishanganj', 'ASMARA KHATUN',
  '2018-03-01', '7000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-03-01T10:15:00.000Z', '2018-03-01T10:15:00.000Z'
),
(
  'hist_pay_9a88d4785823e5ce', 'treatment', 'Advance', 'Advance', 'hist_8ad8dc8aca851657', '9534568245', 'Kishanganj', 'SANJAY',
  '2018-03-05', '4500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-03-05T10:15:00.000Z', '2018-03-05T10:15:00.000Z'
),
(
  'hist_pay_f7547c5a995a7033', 'treatment', 'Advance', 'Advance', 'hist_81ab8ba0115a9307', '6200477620', 'Kishanganj', 'SANOJ ROY',
  '2018-03-12', '3200', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-03-12T10:15:00.000Z', '2018-03-12T10:15:00.000Z'
),
(
  'hist_pay_cd7fbc2e034ad46e', 'treatment', 'Advance', 'Advance', 'hist_005579f5f2d3a4f9', '7356948784', 'Kishanganj', 'KHURSHED',
  '2018-03-12', '8200', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-03-12T10:15:00.000Z', '2018-03-12T10:15:00.000Z'
),
(
  'hist_pay_df8a0102b060a9de', 'treatment', 'Advance', 'Advance', 'hist_939ee05a47670777', '7602968409', 'Kishanganj', 'NARESH KUMAR',
  '2018-04-14', '6000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-04-14T10:15:00.000Z', '2018-04-14T10:15:00.000Z'
),
(
  'hist_pay_0d9c12d0bd43c56b', 'treatment', 'Advance', 'Advance', 'hist_85502f7467e93c98', '7783877278', 'Kishanganj', 'SRAWAN KUMAR',
  '2018-04-16', '9500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-04-16T10:15:00.000Z', '2018-04-16T10:15:00.000Z'
),
(
  'hist_pay_2586b2b9b780cc9c', 'treatment', 'Advance', 'Advance', 'hist_ece501bc6fb14ca1', '9609050488', 'Kishanganj', 'POBITRO BISWAS',
  '2018-04-10', '5000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-04-10T10:15:00.000Z', '2018-04-10T10:15:00.000Z'
),
(
  'hist_pay_20597a71fac804d3', 'treatment', 'Advance', 'Advance', 'hist_322fb585684e4024', '7870104590', 'Kishanganj', 'AEMIRA',
  '2018-03-26', '5000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-03-26T10:15:00.000Z', '2018-03-26T10:15:00.000Z'
),
(
  'hist_pay_8378deb2f78a87ae', 'treatment', 'Advance', 'Advance', 'hist_665b56a718ba25b5', '8409966529', 'Kishanganj', 'RAKESH CHAIOM',
  '2018-03-01', '7200', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-03-01T10:15:00.000Z', '2018-03-01T10:15:00.000Z'
),
(
  'hist_pay_4a039976ada662d9', 'treatment', 'Advance', 'Advance', 'hist_8c748a5f455fb91a', '7063129888', 'Kishanganj', 'NAJER HASHAN',
  '2018-04-23', '3500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-04-23T10:15:00.000Z', '2018-04-23T10:15:00.000Z'
),
(
  'hist_pay_1e6b5c2f1f79f9e4', 'treatment', 'Advance', 'Advance', 'hist_aeb1789154107caf', '7761003671', 'Kishanganj', 'MANEJIB ALAM',
  '2018-05-09', '8300', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-05-09T10:15:00.000Z', '2018-05-09T10:15:00.000Z'
),
(
  'hist_pay_28a99c9f1ce47316', 'treatment', 'Advance', 'Advance', 'hist_7583a9c61c15613d', '9877727267', 'Kishanganj', 'SHERPAL SINGH',
  '2018-05-19', '9500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-05-19T10:15:00.000Z', '2018-05-19T10:15:00.000Z'
),
(
  'hist_pay_8c289faca712f2b1', 'treatment', 'Advance', 'Advance', 'hist_4e12da8d023dbafa', '8789997428', 'Kishanganj', 'JAMAIL AKTHER',
  '2018-05-21', '11500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-05-21T10:15:00.000Z', '2018-05-21T10:15:00.000Z'
),
(
  'hist_pay_18215b98f68bb7f9', 'treatment', 'Advance', 'Advance', 'hist_d3822a0f87ed469c', '8170813773', 'Kishanganj', 'RIEDO MOEDOY',
  '2018-05-24', '7500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-05-24T10:15:00.000Z', '2018-05-24T10:15:00.000Z'
),
(
  'hist_pay_d5e967c8aae5e3a6', 'treatment', 'Advance', 'Advance', 'hist_9f8ebaac5e1649cc', '7870741257', 'Kishanganj', 'ABDUL SATTAR',
  '2018-05-23', '9000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-05-23T10:15:00.000Z', '2018-05-23T10:15:00.000Z'
),
(
  'hist_pay_40b5c31696705efb', 'treatment', 'Advance', 'Advance', 'hist_9cef16602a565568', '7654462407', 'Kishanganj', 'MD MUMATZ',
  '2018-05-27', '9500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-05-27T10:15:00.000Z', '2018-05-27T10:15:00.000Z'
),
(
  'hist_pay_e7d3e5685ba55c98', 'treatment', 'Advance', 'Advance', 'hist_b25891c463870b76', '8158836975', 'Kishanganj', 'NOOR ZAMAL',
  '2018-05-29', '6000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-05-29T10:15:00.000Z', '2018-05-29T10:15:00.000Z'
),
(
  'hist_pay_612c96998d7c55af', 'treatment', 'Advance', 'Advance', 'hist_46156363082ed886', '7602897286', 'Kishanganj', 'JUMED ALAM',
  '2018-06-27', '3500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-06-27T10:15:00.000Z', '2018-06-27T10:15:00.000Z'
),
(
  'hist_pay_4f60302d917adb97', 'treatment', 'Advance', 'Advance', 'hist_5cd778d080ec97ce', '7797811931', 'Kishanganj', 'GHALAM RABANIE',
  '2018-07-07', '8000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2018-07-07T10:15:00.000Z', '2018-07-07T10:15:00.000Z'
);
