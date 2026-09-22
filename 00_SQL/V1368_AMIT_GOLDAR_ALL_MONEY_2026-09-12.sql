-- ═══════════════════════════════════════════════════════════════════════════
-- V1368 (১২.০৯.২০২৬) — AMIT GOLDAR (কিশানগঞ্জ)-এর রোগীদের মোট জমা (ভিজিট ফি
-- + রেজিস্ট্রেশন + মেডিসিন + ট্রিটমেন্ট, সব মিলিয়ে) — কমিশন শুধু ট্রিটমেন্ট-
-- টাকার উপরেই হয় (TK-র নিজের পুরনো নিয়ম), তাই আগের ফলে দেখা ₹2,54,100
-- (শুধু ট্রিটমেন্ট) আর স্টাফের বলা ₹2,90,000 (সম্ভবত মোট জমা)-এর মধ্যে
-- ফারাকটা ভিজিট ফি/রেজিস্ট্রেশন/মেডিসিন টাকা কিনা, এটা সেটাই মেলাবে।
-- ⛔ শুধু পড়া, কিছু বদলায় না। Supabase → SQL Editor → New query → Run।
-- ═══════════════════════════════════════════════════════════════════════════

with amit as (
  select lower(trim(coalesce(name,''))) as nm,
         right(regexp_replace(coalesce(mobile,''),'[^0-9]','','g'),10) as mb
  from public.doctor_visits
  where lower(trim(coalesce(name,''))) like '%amit%goldar%'
),
matched_patients as (
  select distinct p.id, p.mobile
  from public.patients p, amit a
  where lower(trim(coalesce(p."refBy",''))) = a.nm
     or lower(trim(coalesce(p."refDoctor",''))) = a.nm
     or right(regexp_replace(coalesce(p."refDoctorMobile",''),'[^0-9]','','g'),10) = a.mb
)
select
  coalesce(sum(case when lower(coalesce(x."payType",'')) in ('treatment','') then fin.rmp_safe_number(x."amount") else 0 end),0) as treatment_only,
  coalesce(sum(case when lower(coalesce(x."payType",'')) in ('visit_fee','visitfee','registration') then fin.rmp_safe_number(x."amount") else 0 end),0) as visit_or_registration,
  coalesce(sum(case when lower(coalesce(x."payType",'')) = 'medicine' then fin.rmp_safe_number(x."amount") else 0 end),0) as medicine,
  coalesce(sum(case when lower(coalesce(x."payType",'')) not in ('treatment','','visit_fee','visitfee','registration','medicine','refund','attendance_mark') then fin.rmp_safe_number(x."amount") else 0 end),0) as other_type,
  coalesce(sum(case when lower(coalesce(x."payType",'')) = 'refund' and lower(coalesce(x."refundApprovalStatus",'')) = 'approved' then -fin.rmp_safe_number(x."amount") else 0 end),0) as approved_refund,
  coalesce(sum(case when lower(coalesce(x."payType",'')) not in ('refund','attendance_mark') then fin.rmp_safe_number(x."amount")
                     when lower(coalesce(x."payType",'')) = 'refund' and lower(coalesce(x."refundApprovalStatus",'')) = 'approved' then -fin.rmp_safe_number(x."amount")
                     else 0 end),0) as grand_total_all_money
from public.payments x
join matched_patients mp on mp.id = x."patientId";
