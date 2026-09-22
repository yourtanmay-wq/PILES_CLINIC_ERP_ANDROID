-- WARNING: SAMPLE ONLY -- Step 2: first 10 patients (Kishanganj, 2017
-- historical Google Sheet). Only after TK reviews and approves this in the
-- app will the rest of the patients be imported.
-- NOTE: createdBy/registeredBy = 'HISTORICAL IMPORT' -- honestly marked;
-- the real original staff name is not known, so no one's name was guessed.
-- NOTE: sex (Male/Female) was not in the sheet -- guessed from name
-- (TK's instruction); if wrong it will show up in these 10 and be fixed.
-- NOTE: stage='Treatment Running' but queue='false' and doctorComplete=
-- 'true' -- so these will NOT show up in today's live Doctor Queue or
-- Follow-up call list.

insert into public.patients (
  id, "patientId", date, "registrationDate", "visitDate",
  name, mobile, branch, age, sex,
  address, disease, bill,
  stage, queue, "doctorComplete",
  "createdBy", "registeredBy", "createdAt", "updatedAt"
) values
(
  'hist_03a36d699ab568ea', 'KNE-06042017-001', '2017-04-06', '2017-04-06', '2017-04-06',
  'MD ARBAJ', '7763926891', 'Kishanganj', '', 'Male',
  'RATUA, PURNIA', 'Hydrocele', '3000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-04-06T10:00:00.000Z', '2017-04-06T10:00:00.000Z'
),
(
  'hist_63a05c723ce4f2aa', 'KNE-12042017-001', '2017-04-12', '2017-04-12', '2017-04-12',
  'ALADIN ANSARI', '9006717887', 'Kishanganj', '39', 'Male',
  'KHAGRA, KHAGRA, KISHANGANJ, KISHANGANJ', 'Piles', '5500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-04-12T10:00:00.000Z', '2017-04-12T10:00:00.000Z'
),
(
  'hist_76d9ea5e692703a1', 'KNE-17042017-001', '2017-04-17', '2017-04-17', '2017-04-17',
  'MAJRUL ISLAM', '9732900921', 'Kishanganj', '25', 'Male',
  'FULBARI, CHAKULIYA, CHAKULIYA, UTTAR DINAJPUR', 'Hydrocele', '4000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-04-17T10:00:00.000Z', '2017-04-17T10:00:00.000Z'
),
(
  'hist_02472e099b6fe404', 'KNE-18042017-001', '2017-04-18', '2017-04-18', '2017-04-18',
  'RAHMAT ALAM', '9934252301', 'Kishanganj', '26', 'Male',
  'SHIRSI, RATUA, RATUA, PURNIA', 'Piles', '5500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-04-18T10:00:00.000Z', '2017-04-18T10:00:00.000Z'
),
(
  'hist_228bd6c77b79636b', 'KNE-18042017-002', '2017-04-18', '2017-04-18', '2017-04-18',
  'MD IQBAL', '9608699408', 'Kishanganj', '', 'Male',
  'PANE, PANE, KISHANGANJ, KISHANGANJ', 'Hydrocele', '4000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-04-18T10:00:00.000Z', '2017-04-18T10:00:00.000Z'
),
(
  'hist_883f795ee7ea5880', 'KNE-26042017-001', '2017-04-26', '2017-04-26', '2017-04-26',
  'TAUSIF REJA', '7859431634', 'Kishanganj', '17', 'Male',
  'UTTAR DINAJPUR', 'Piles', '5500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-04-26T10:00:00.000Z', '2017-04-26T10:00:00.000Z'
),
(
  'hist_bf0f62ee77eb740f', 'KNE-30042017-001', '2017-04-30', '2017-04-30', '2017-04-30',
  'SURESH KUMAR', '9769742799', 'Kishanganj', '26', 'Male',
  'DUMURIA, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Hydrocele', '4000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-04-30T10:00:00.000Z', '2017-04-30T10:00:00.000Z'
),
(
  'hist_9a63e52bd8aedcd9', 'KNE-10052017-001', '2017-05-10', '2017-05-10', '2017-05-10',
  'MUNNA ALAM', '9661842124', 'Kishanganj', '40', 'Male',
  'KISHANGANJ, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '3500',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-05-10T10:00:00.000Z', '2017-05-10T10:00:00.000Z'
),
(
  'hist_795f6e9ee754f992', 'KNE-13052017-001', '2017-05-13', '2017-05-13', '2017-05-13',
  'TAPAN KUMAR DAS', '9775961893', 'Kishanganj', '42', 'Male',
  'RUIDASHA, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Piles', '4000',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-05-13T10:00:00.000Z', '2017-05-13T10:00:00.000Z'
),
(
  'hist_db6319096da7e065', 'KNE-13052017-002', '2017-05-13', '2017-05-13', '2017-05-13',
  'AHMAD HOSSAIN', '9470853827', 'Kishanganj', '55', 'Male',
  'TAUSA, KISHANGANJ, KISHANGANJ, KISHANGANJ', 'Hydrocele', '4200',
  'Treatment Running', 'false', 'true',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-05-13T10:00:00.000Z', '2017-05-13T10:00:00.000Z'
);

insert into public.payments (
  id, "payType", "payLabel", "paymentLabel", "patientId", mobile, branch, name,
  date, amount, mode, remarks,
  "createdBy", "receivedBy", "createdAt", "updatedAt"
) values
(
  'hist_pay_8f65e225212f63fd', 'treatment', 'Advance', 'Advance', 'hist_03a36d699ab568ea', '7763926891', 'Kishanganj', 'MD ARBAJ',
  '2017-04-06', '1500', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-04-06T10:15:00.000Z', '2017-04-06T10:15:00.000Z'
),
(
  'hist_pay_a5ba457592e59787', 'treatment', 'Advance', 'Advance', 'hist_63a05c723ce4f2aa', '9006717887', 'Kishanganj', 'ALADIN ANSARI',
  '2017-04-12', '5210', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-04-12T10:15:00.000Z', '2017-04-12T10:15:00.000Z'
),
(
  'hist_pay_c3c09c5efede3801', 'treatment', 'Advance', 'Advance', 'hist_76d9ea5e692703a1', '9732900921', 'Kishanganj', 'MAJRUL ISLAM',
  '2017-04-17', '4000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-04-17T10:15:00.000Z', '2017-04-17T10:15:00.000Z'
),
(
  'hist_pay_e1caff9bd058d5e0', 'treatment', 'Advance', 'Advance', 'hist_02472e099b6fe404', '9934252301', 'Kishanganj', 'RAHMAT ALAM',
  '2017-04-18', '3700', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-04-18T10:15:00.000Z', '2017-04-18T10:15:00.000Z'
),
(
  'hist_pay_06db9bfb2b4c4578', 'treatment', 'Advance', 'Advance', 'hist_228bd6c77b79636b', '9608699408', 'Kishanganj', 'MD IQBAL',
  '2017-04-18', '3700', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-04-18T10:15:00.000Z', '2017-04-18T10:15:00.000Z'
),
(
  'hist_pay_9bf4e8292c93b158', 'treatment', 'Advance', 'Advance', 'hist_883f795ee7ea5880', '7859431634', 'Kishanganj', 'TAUSIF REJA',
  '2017-04-26', '5100', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-04-26T10:15:00.000Z', '2017-04-26T10:15:00.000Z'
),
(
  'hist_pay_e1f019855388bf7a', 'treatment', 'Advance', 'Advance', 'hist_bf0f62ee77eb740f', '9769742799', 'Kishanganj', 'SURESH KUMAR',
  '2017-04-30', '3700', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-04-30T10:15:00.000Z', '2017-04-30T10:15:00.000Z'
),
(
  'hist_pay_4f78dda8e2783922', 'treatment', 'Advance', 'Advance', 'hist_9a63e52bd8aedcd9', '9661842124', 'Kishanganj', 'MUNNA ALAM',
  '2017-05-10', '2900', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-05-10T10:15:00.000Z', '2017-05-10T10:15:00.000Z'
),
(
  'hist_pay_2fbabac7dce4eb40', 'treatment', 'Advance', 'Advance', 'hist_795f6e9ee754f992', '9775961893', 'Kishanganj', 'TAPAN KUMAR DAS',
  '2017-05-13', '2000', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-05-13T10:15:00.000Z', '2017-05-13T10:15:00.000Z'
),
(
  'hist_pay_24b07af769a62756', 'treatment', 'Advance', 'Advance', 'hist_db6319096da7e065', '9470853827', 'Kishanganj', 'AHMAD HOSSAIN',
  '2017-05-13', '4200', 'CASH', 'Historical import (Google Sheet, pre-app) -- combined, no per-installment date in sheet',
  'HISTORICAL IMPORT', 'HISTORICAL IMPORT', '2017-05-13T10:15:00.000Z', '2017-05-13T10:15:00.000Z'
);
