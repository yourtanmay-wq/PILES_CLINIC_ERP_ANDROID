-- ═══════════════════════════════════════════════════════════════════════════
-- V1369 (১২.০৯.২০২৬) — TK-নির্দেশ: AMIT GOLDAR-এর পাঠানো সব রোগীর তালিকা,
-- প্রতিজনের সর্বমোট জমা (ভিজিট ফি + রেজিস্ট্রেশন + ট্রিটমেন্ট + মেডিসিন,
-- অনুমোদিত রিফান্ড বাদ দিয়ে) — Google Sheet বানানোর জন্য এই ফলটাই লাগবে।
-- ⛔ শুধু পড়া, কিছু বদলায় না। Supabase → SQL Editor → New query → Run।
-- ফল CSV করে (Export) TK-কে পাঠিয়ে দিতে হবে, বা স্ক্রিনশট/কপি করলেও চলবে।
-- ═══════════════════════════════════════════════════════════════════════════

with amit as (
  select lower(trim(coalesce(name,''))) as nm,
         right(regexp_replace(coalesce(mobile,''),'[^0-9]','','g'),10) as mb
  from public.doctor_visits
  where lower(trim(coalesce(name,''))) like '%amit%goldar%'
),
matched_patients as (
  select distinct p.id, p.name, p.mobile,
         greatest(0, fin.rmp_safe_number(p."bill") - fin.rmp_safe_number(p."discount")) as bill
  from public.patients p, amit a
  where lower(trim(coalesce(p."refBy",''))) = a.nm
     or lower(trim(coalesce(p."refDoctor",''))) = a.nm
     or right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10) = a.mb
)
select mp.name as patient_name, mp.mobile, mp.bill,
       coalesce((
         select sum(case
           when lower(coalesce(x."payType",'')) not in ('refund','attendance_mark') then fin.rmp_safe_number(x."amount")
           when lower(coalesce(x."payType",'')) = 'refund' and lower(coalesce(x."refundApprovalStatus",'')) = 'approved' then -fin.rmp_safe_number(x."amount")
           else 0 end)
         from public.payments x where x."patientId" = mp.id
       ),0) as total_paid
from matched_patients mp
order by total_paid desc, mp.name;
