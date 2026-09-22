-- V1386: PKB-কে জলপাইগুড়ি ব্রাঞ্চে RMP হিসেবে যোগ করা (TK অনুমতি: "হ্যাঁ, বানিয়ে দিন")
-- অ্যাপের "Add Doctor/RMP" ফর্ম যা যা লিখে ঠিক তাই — শুধু SQL দিয়ে, কোনো নতুন নিয়ম নয়।
-- নাম/নম্বর কোচবিহারের PKB-র রেকর্ড (dv_91f6cdf6cff84218b82a0bbc74021b9e)-এর সাথে হুবহু মিলিয়ে।

insert into public.doctor_visits
  ("id","name","mobile","area","remarks","date","branch",
   "lastCallDate","nextCallDate","callStatus","status",
   "callHistory","referralPayments","referralPaid","referralDue",
   "createdBy","createdAt","updatedAt")
select
  'dv_5b5ab2ff39454839a0a4c3f2fe776c15', 'PKB', '+919242009205', '', '',
  to_char(now() at time zone 'utc', 'YYYY-MM-DD'), 'Jalpaiguri',
  '', to_char((now() at time zone 'utc') + interval '30 days', 'YYYY-MM-DD'),
  'Pending', 'Active',
  '[]'::jsonb, '[]'::jsonb, '0', '0',
  'TK_DIRECT',
  to_char(now() at time zone 'utc', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
  to_char(now() at time zone 'utc', 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
where not exists (
  select 1 from public.doctor_visits
  where lower(trim("name")) = 'pkb' and lower(trim("branch")) = 'jalpaiguri'
);

-- যাচাই — এই সারিটা তৈরি হলো কিনা দেখুন
select id, name, mobile, branch, status from public.doctor_visits
where lower(trim("name")) = 'pkb' and lower(trim("branch")) = 'jalpaiguri';
