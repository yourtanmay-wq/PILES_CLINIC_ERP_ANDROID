-- ═══════════════════════════════════════════════════════════════════════════
-- V1465 (১৪.০৯.২০২৬) — শুধুমাত্র পড়া (READ-ONLY), কিছুই বদলায়/মোছে না।
--
-- কোচবিহার স্টাফের প্রশ্ন (DURDARS PAL নিজে বলেছেন): তাঁর ট্রিটমেন্ট তো
-- ফালাকাটা ব্রাঞ্চে হয়েছে, অথচ RMP "TK BISWAS" (Cooch Behar)-এর Due
-- তালিকায় তাঁর নামে ₹200 বাকি দেখাচ্ছে (40% of ₹500)। এটা ঠিক TAPOSHI
-- BARMAN-এর (তালিকা ৪৯৭, ১২.০৯) একই ধরনের বাগ কিনা — একই RMP-মোবাইলের
-- একাধিক ব্রাঞ্চ-রেকর্ড থাকলে ভুল ব্রাঞ্চেরটা বেছে নেওয়া — সেটাই এই SQL
-- সরাসরি ডেটা দেখে যাচাই করবে, আন্দাজ নয়।
-- ═══════════════════════════════════════════════════════════════════════════

-- ① DURDARS PAL নামে (বানান একটু আলাদা হতে পারে) রোগীর সারি — আসল branch কোনটা।
select id, name, mobile, branch, "patientId", "registrationDate", "createdAt"
from public.patients
where name ilike '%durdas%' or name ilike '%durdars%' or name ilike '%durga%pal%'
order by "createdAt" desc;

-- ② "TK BISWAS" মোবাইল +918001080080-এর নামে RMP-রেকর্ড ক'টা আছে, কোন কোন ব্রাঞ্চে।
select id, name, mobile, branch, status, "createdAt"
from public.doctor_visits
where mobile like '%8001080080'
order by "createdAt";

-- ③ DURDARS PAL-এর কমিশন-সারিটা কোন RMP-id-তে বাঁধা, কত হার, কবে/কে বসিয়েছে।
select c.id, c.patient_row_id, c.patient_code, c.patient_name, c.patient_mobile,
       c.treatment_branch, c.rmp_id, c.rmp_name, c.commission_mode, c.commission_value,
       c.set_on, c.set_by, c.updated_at
from fin.rmp_patient_commissions c
where c.patient_name ilike '%durdas%' or c.patient_name ilike '%durdars%'
   or c.patient_mobile in (select mobile from public.patients where name ilike '%durdas%' or name ilike '%durdars%');

-- ④ ওই কমিশন-সারিটা কবে/কীভাবে/কার দ্বারা তৈরি হলো — audit trail।
select a.*
from fin.rmp_commission_audit a
where a.entity_id in (
  select c.id::text from fin.rmp_patient_commissions c
  where c.patient_name ilike '%durdas%' or c.patient_name ilike '%durdars%'
)
order by a.id;
