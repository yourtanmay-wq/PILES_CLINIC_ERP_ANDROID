-- ═══════════════════════════════════════════════════════════════════════════
-- V1466 (১৪.০৯.২০২৬) — শুধুমাত্র পড়া (READ-ONLY), কিছুই বদলায়/মোছে না।
-- TK-র সরাসরি প্রশ্নগুলোর উত্তর একসাথে বার করার জন্য।
-- ═══════════════════════════════════════════════════════════════════════════

-- ① "TK BISWAS" (মোবাইল +918001080080) নামে RMP-রেকর্ড ক'টা, কোন কোন ব্রাঞ্চে।
select id, name, mobile, branch, status, "createdAt"
from public.doctor_visits
where mobile like '%8001080080'
order by "createdAt";

-- ② ওই প্রতিটা RMP-id-র নিজের সাধারণ ডিফল্ট হার (branch-নির্দিষ্ট নয়)।
select rmp_id, rmp_name, rmp_mobile, commission_mode, commission_value, updated_by, updated_at
from fin.rmp_commission_defaults
where rmp_mobile like '%8001080080';

-- ③ প্রতিটা ব্রাঞ্চ-ভিত্তিক ডিফল্ট হার — এখানেই যদি Falakata-র জন্য ৬০% জমা
--    থাকে সেটা দেখা যাবে, আর কোন rmp_id-র সাথে বাঁধা তাও।
select rmp_id, branch, rmp_name, rmp_mobile, commission_mode, commission_value, effective_from, updated_by, updated_at
from fin.rmp_commission_branch_defaults
where rmp_mobile like '%8001080080'
order by branch;

-- ④ DURDARS PAL (FLK-12092026-001)-এর নিজের রোগীর সারি।
select id, name, mobile, branch, "patientId", "registrationDate", "createdAt"
from public.patients
where "patientId" = 'FLK-12092026-001' or mobile like '%9635099181';

-- ⑤ DURDARS PAL-এর সব টাকার সারি — ৫০০ টাকা কোন ব্রাঞ্চে/কবে জমা হয়েছে তা এখানেই।
select id, "patientId", branch, "payType", amount, date, "createdAt", "receivedBy"
from public.payments
where mobile like '%9635099181'
order by "createdAt";
