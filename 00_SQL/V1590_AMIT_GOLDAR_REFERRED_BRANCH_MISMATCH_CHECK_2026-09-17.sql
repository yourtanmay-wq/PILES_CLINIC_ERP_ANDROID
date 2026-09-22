-- 🔍 READ-ONLY — শুধু দেখার জন্য, কোনো টাকা/ডেটা বদলাবে না
-- V1589-এ প্রমাণ হয়েছে ৫০০০-এর সীমা কারণ না (মোট রোগীই ২১৯৪)।
-- নতুন সন্দেহ: রোগী দেখানোর সার্ভার-ফাংশন রোগীর ব্রাঞ্চ AMIT GOLDAR-এর নিজের
-- ব্রাঞ্চের (Kishanganj) সাথে না মিললে বাদ দেয়। এই কোয়েরি ৫৭ জনের মধ্যে
-- কার ব্রাঞ্চ Kishanganj নয় তা দেখাবে — সেটাই হয়তো ৫৭-৫৬=১ জনের আসল কারণ।
with rmp as (
  select id, "name", "mobile", coalesce("branch",'') as rmp_branch
  from public.doctor_visits
  where right(regexp_replace(coalesce("mobile",''),'\D','','g'),10) = '9046366596'
  limit 1
)
select p.id, p."name", p."mobile", coalesce(p."branch",'(ফাঁকা)') as patient_branch,
       r.rmp_branch as amit_goldar_branch,
       coalesce(p."refBy",'') as "refBy", coalesce(p."refDoctor",'') as "refDoctor",
       coalesce(p."refDoctorMobile",'') as "refDoctorMobile"
from public.patients p, rmp r
where (
    lower(trim(coalesce(p."refBy",'')))     = lower(trim(r."name"))
 or lower(trim(coalesce(p."refDoctor",'')))  = lower(trim(r."name"))
 or right(regexp_replace(coalesce(p."refDoctorMobile",''),'\D','','g'),10)
    = right(regexp_replace(coalesce(r."mobile",''),'\D','','g'),10)
)
order by patient_branch;
