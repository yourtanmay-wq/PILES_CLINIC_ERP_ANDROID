-- V1616 (২০.০৯.২০২৬, TK-রিপোর্ট, ছবিসহ — DIPANKAR DAS42): Follow-up তালিকায়
-- রোগীর নাম "DIPANKAR DAS42" দেখাচ্ছিল, অথচ তাঁর নিজের পাতায় (Full Journey)
-- ঠিক নাম "DIPANKAR DAS" ছিল।
--
-- আসল কারণ: নাম বদলালে patients-টেবিলের নাম followups/enquiries-টেবিলেও
-- বসিয়ে দেওয়ার নিয়ম (V1130, ০৬.০৯.২০২৬) ফোন ও কম্পিউটার দুটোতেই আছে,
-- কিন্তু ওই নিয়ম বসার **আগে** যাঁদের নাম শোধরানো হয়েছিল (এই রোগীর
-- রেজিস্ট্রেশন ০৩.০৭.২০২৬-এর, নিয়মটা আসার প্রায় দুই মাস আগে), তাঁদের
-- পুরনো followups-সারিতে ভুল নামটা তখনই থেকে গিয়েছিল — কোনো কোড এখন আর এই
-- ভুল করে না, শুধু পুরনো কিছু সারিতে এটা থেকে গেছে।
--
-- এই SQL শুধু **সেই পুরনো রেখে-যাওয়া অমিলগুলো** ঠিক করে —
--   · যেসব মোবাইলে ঠিক ১ জন রোগী আছেন (এক নম্বরে দুজন আলাদা মানুষের
--     কোনো ঝুঁকিই নেই) তাঁদের followups-সারির নামই শুধু ছোঁয়া হয়,
--   · আর নাম সত্যিই আলাদা থাকলে তবেই বদলানো হয় (একই নাম আবার লেখা হয় না)।
-- ⛔ টাকা/তারিখ/অবস্থা/আইডি — কিচ্ছু ছোঁয়া হয় না, শুধু নামের ঘরটা।
-- ⛔ এক নম্বরে একাধিক রোগী থাকলে (শেয়ার-করা নম্বর) সেই মোবাইলের একটা
--    সারিও ছোঁয়া হয় না — ভুল করে অন্য মানুষের নাম বসানোর চেয়ে কিছু না
--    করা নিরাপদ (PatientIdentity.kt-এর নিজের নিয়মের হুবহু একই সাবধানতা)।

begin;

-- ১) আগে দেখে নেওয়া — কতগুলো সারি বদলাবে, কারা কারা (রান করার আগে TK/স্টাফ
--    চাইলে এই SELECT-টা একা চালিয়ে তালিকা দেখে নিতে পারেন):
--
-- select f.id as followup_id, f.name as followup_name_before,
--        p.name as patient_name_correct, p."patientId"
--   from public.followups f
--   join public.patients p
--     on right(regexp_replace(f.mobile,'\D','','g'), 10)
--      = right(regexp_replace(p.mobile,'\D','','g'), 10)
--  where right(regexp_replace(f.mobile,'\D','','g'), 10) in (
--          select right(regexp_replace(mobile,'\D','','g'), 10)
--            from public.patients
--           group by 1 having count(*) = 1
--        )
--    and trim(f.name) <> trim(p.name)
--    and length(right(regexp_replace(f.mobile,'\D','','g'), 10)) = 10;

-- ২) আসল সংশোধন:
with one_patient_per_mobile as (
  select right(regexp_replace(mobile, '\D', '', 'g'), 10) as m10, min(id) as pid
    from public.patients
   where length(right(regexp_replace(mobile, '\D', '', 'g'), 10)) = 10
   group by 1
  having count(*) = 1
)
update public.followups f
   set name = p.name,
       "updatedAt" = to_char(now() at time zone 'utc', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
  from one_patient_per_mobile opm
  join public.patients p on p.id = opm.pid
 where right(regexp_replace(f.mobile, '\D', '', 'g'), 10) = opm.m10
   and trim(f.name) <> trim(p.name);

commit;

-- ৩) যাচাই — এরপর এই তালিকা খালি আসা উচিত (একই নম্বরে আর কোনো নাম-অমিল নেই,
--    শুধু একজন-রোগীর-মোবাইলগুলোতে):
select f.id, f.name as followup_name, p.name as patient_name, p."patientId"
  from public.followups f
  join public.patients p
    on right(regexp_replace(f.mobile,'\D','','g'), 10)
     = right(regexp_replace(p.mobile,'\D','','g'), 10)
 where right(regexp_replace(f.mobile,'\D','','g'), 10) in (
         select right(regexp_replace(mobile,'\D','','g'), 10)
           from public.patients
          group by 1 having count(*) = 1
       )
   and trim(f.name) <> trim(p.name);
