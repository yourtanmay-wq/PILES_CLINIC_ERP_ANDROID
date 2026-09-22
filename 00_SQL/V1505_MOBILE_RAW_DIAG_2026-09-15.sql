-- শুধু SELECT — কিছু মোছে না।
-- ASHOK DEY / GOPAL DEY দুজনের মোবাইল ঘরের আসল (raw, অপরিবর্তিত) মান —
-- দুজনকে এক নম্বরের মনে হলেও কেন তালিকায় আলাদা সারি হলো তা দেখতে।
select "patientId", name, mobile as raw_mobile, length(mobile) as raw_len,
       right(regexp_replace(coalesce(mobile,''),'\D','','g'),10) as normalized_last10,
       length(regexp_replace(coalesce(mobile,''),'\D','','g')) as digits_only_len
  from public.patients
 where "patientId" in ('JPE-24022026-005','JPE-24022026-006');
