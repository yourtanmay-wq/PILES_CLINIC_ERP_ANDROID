-- 🔵 শুধু-পড়ার (READ-ONLY) যাচাই SQL — কিছু বদলায়/মোছে না, শুধু তুলনা করে দেখায়।
-- TK-র পাঠানো "2026_PATIENTS_DETAILS" শিটের জলপাইগুড়ি রোগীদের TOTAL PAID
-- বনাম অ্যাপের payments টেবিলের আসল হিসাব — কোন রোগীর সিটে টাকা দেখাচ্ছে
-- কিন্তু অ্যাপে নেই/কম, সেটাই এই SQL বার করে দেখাবে।
-- ব্যবহার: Supabase SQL Editor-এ চালিয়ে ফলাফলের ছবি/CSV Claude-কে পাঠান।
with sheet(name, mobile, sheet_paid) as (
  values
    ('SANDIP MANDAL','7074918856',20500),
    ('BASANTI ROY','9749643218',22000),
    ('MANAB BARMAN','8590586665',2000),
    ('BASUDEV SARKAR','9242812350',21000),
    ('BIPLAB ROY','6296458182',22000),
    ('NUR JABAN HAQUE','8327591672',18000),
    ('PAYEL SING','7551859520',16000),
    ('PRASANTA ROY','8972425857',1000),
    ('MANAS PATRA','6294480775',30500),
    ('KANAILAL SARKAR','8700197386',1500),
    ('PAKHIZA BEGAM','7908993409',30000),
    ('MUNMUN PARVIN','9547012730',3000),
    ('DOLI ROY','7029059150',1000),
    ('SUPABI DEBNATH','9734171525',28000),
    ('NUR JAMAL ALAM','6282233149',25000),
    ('TAPASI BHAR','7047710867',31000),
    ('ASHOK DEY','9641062348',11000),
    ('GOPAL DEY','9641062348',21000),
    ('GOURANGA SONAR','9932592801',13000),
    ('ASMA KHATUN','9932228280',24500),
    ('TUMPA BEGAM','8967887701',16500),
    ('BEAUTY ROY','9832124730',17500),
    ('AMARJIT YEADAV','7607790460',13000),
    ('SOMA DEY','7679938656',22500),
    ('JANAPRIYA ROY','8972960119',21500),
    ('DIPALI SARKAR','9932630899',2500),
    ('FAJAL MAHAMMAD','8670506333',1000),
    ('KHAGEN BHAGAT','9800850916',23000),
    ('RAJDIP SHIL','9563322399',5100),
    ('TALHA JUBER','8617347925',15000),
    ('SANTA ROY','8509909177',13000),
    ('MUNMUN BAL','9641069609',19000),
    ('NILIMA SARKAR','7602156900',14000),
    ('ANJANA ROY','7478028215',32000),
    ('MANOJ MANDAL','9609308963',28000),
    ('BIBHASH ROY','8075783008',30500),
    ('PUJA ROY','8388957809',18000),
    ('JIBON MONDAL','7074251242',24500),
    ('GOPAL BARUI','9832649861',17000),
    ('PRASANJIT ROY','7864089323',21900),
    ('SAJAL SARKAR','9635015302',50000),
    ('GOPI KAKAR','6294049558',16000),
    ('SWAPAN SARKAR','9679840240',22000),
    ('HARIKANTO ROY','8944064856',30000),
    ('CHAYAN ROY','9749546998',52500),
    ('BIJAY MANDAL','6296314235',30000),
    ('PINTU ROY','7797790802',5000),
    ('ANITA BISWAS','6295527967',7000),
    ('YAGASH KOLI','9545869782',23000),
    ('GANESH CH ROY','9832620422',20000),
    ('PRASRNJIT BISWAS','9064597174',14500),
    ('RAJ ROUTH','7479037293',36160),
    ('NIRMAL ROY','9064633214',35561),
    ('TAJRUL RAHAMAN','9800825329',43000)
),
app_paid as (
  select mobile,
         sum(nullif(regexp_replace(coalesce(amount,''), '[^0-9.]', '', 'g'), '')::numeric) as app_paid
  from public.payments
  where branch ilike 'Jalpaiguri%'
  group by mobile
)
select
  s.name,
  s.mobile,
  s.sheet_paid,
  coalesce(a.app_paid, 0) as app_paid,
  s.sheet_paid - coalesce(a.app_paid, 0) as difference
from sheet s
left join app_paid a on a.mobile = s.mobile
where s.sheet_paid - coalesce(a.app_paid, 0) > 500
order by difference desc;
