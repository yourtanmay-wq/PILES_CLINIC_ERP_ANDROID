-- আজকের ইমপোর্টে ১৪ জন সত্যিকারের ডুপ্লিকেট (একই মানুষ লাইভে আগে থেকেই আছেন,
-- '+91' ফরম্যাটের কারণে ডুপ্লিকেট-চেকে ধরা পড়েনি) -- এরা সবাই bill=0/Enquiry
-- ব্যাচের (V1528/V1534/V1535), কোনো payments সারি নেই, তাই সরাসরি মুছে ফেলাই যথেষ্ট।

delete from public.patients where id = 'histkne25nb_2081c0fac14b1b04' and "patientId" = 'KNE-10032025-002' and name = 'MUKTAR ALAM';
delete from public.patients where id = 'histkne25nb_56a4769bb9dd7bbc' and "patientId" = 'KNE-19092025-001' and name = 'SADDAM HUSAIN';
delete from public.patients where id = 'histkne25nb_8bd0445617b666b8' and "patientId" = 'KNE-15102025-001' and name = 'MANGAL SINGH';
delete from public.patients where id = 'histkne25nb_31cf586d84b92462' and "patientId" = 'KNE-26112025-001' and name = 'NAIMUL HOUK';
delete from public.patients where id = 'histkne25nb_e12b95a85393c513' and "patientId" = 'KNE-16122025-001' and name = 'AKHTAR RAZA';
delete from public.patients where id = 'histkne25nb_3c4f791c70f65fd0' and "patientId" = 'KNE-17122025-002' and name = 'KRISHNA BISWAS';
delete from public.patients where id = 'histcob25nb_abcf7dc97bc51904' and "patientId" = 'COB-22072025-001' and name = 'DULAUDDIN MIYA';
delete from public.patients where id = 'histcob25nb_78062a2a6937838c' and "patientId" = 'COB-27102025-003' and name = 'RABIUL ISLAM';
delete from public.patients where id = 'histcob25nb_0ecd54169af46f48' and "patientId" = 'COB-01122025-001' and name = 'ARJUN KUMAR ROY';
delete from public.patients where id = 'histjpe25nb_99c20aab118cbab7' and "patientId" = 'JPE-29042025-002' and name = 'MANIK ROY';
delete from public.patients where id = 'histjpe25nb_7f7e27889063fd64' and "patientId" = 'JPE-02062025-001' and name = 'GANESH CH ROY';
delete from public.patients where id = 'histjpe25nb_3d77c0450476dd5f' and "patientId" = 'JPE-06062025-002' and name = 'PRASENJIT BISWAS';
delete from public.patients where id = 'histjpe25nb_2000006b4ae33bef' and "patientId" = 'JPE-27062025-001' and name = 'SNITDHA DATTA';
delete from public.patients where id = 'histjpe25nb_c0ad2412ba6457bf' and "patientId" = 'JPE-26092025-001' and name = 'NOOR JABAN HOQUE';
