-- ═══════════════════════════════════════════════════════════════════════════
-- V1370 (১২.০৯.২০২৬) — TK-নির্দেশ: V1369-এর তালিকা থেকে Visit Fee বাদ দিয়ে
-- প্রতিজনের সর্বমোট জমা (রেজিস্ট্রেশন + ট্রিটমেন্ট + মেডিসিন, Visit Fee
-- আর তার approved রিফান্ড দুটোই বাদ, বাকি অনুমোদিত রিফান্ড আগের মতোই বাদ)।
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
),
is_visitfee as (
  select x.id,
         (lower(coalesce(x."payType",'')) in ('visit_fee','visitfee')
          or lower(coalesce(x."remarks",'')) like '%visit fee%') as vf
  from public.payments x
)
select mp.name as patient_name, mp.mobile, mp.bill,
       coalesce((
         select sum(case
           when iv.vf then 0
           when lower(coalesce(x."payType",'')) = 'refund' then
             case when exists (
               select 1 from public.payments o
               join is_visitfee ivo on ivo.id = o.id
               where o.id = x."refundOfPaymentId" and ivo.vf
             ) then 0
             when lower(coalesce(x."refundApprovalStatus",'')) = 'approved' then -fin.rmp_safe_number(x."amount")
             else 0 end
           when lower(coalesce(x."payType",'')) = 'attendance_mark' then 0
           else fin.rmp_safe_number(x."amount") end)
         from public.payments x
         join is_visitfee iv on iv.id = x.id
         where x."patientId" = mp.id
       ),0) as total_paid_excl_visitfee
from matched_patients mp
order by total_paid_excl_visitfee desc, mp.name;
