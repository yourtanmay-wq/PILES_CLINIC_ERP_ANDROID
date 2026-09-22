-- ═══════════════════════════════════════════════════════════════════════════
-- V1375 (১২.০৯.২০২৬) — TK-প্রশ্ন যাচাই: SAIF RAZA (+918292909251, KNE-
-- 11032026-003)-এর Timeline-এ "Visit Fee ₹400" আর "Registration / Visit
-- ₹400" — দুটো আলাদা সারি, দুটোতেই টাকা। আসল payments টেবিলে কী আছে সেটাই
-- এই SQL সরাসরি দেখাবে (আন্দাজ নয়)।
-- ⛔ শুধু পড়া, কিছু বদলায় না। Supabase → SQL Editor → New query → Run।
-- ═══════════════════════════════════════════════════════════════════════════

select
  x.id, x."patientId", x."payType", x.amount, x.remarks,
  x."refundApprovalStatus", x."refundOfPaymentId",
  x.date, x."createdAt", x."updatedAt", x."receivedBy"
from public.payments x
where x.mobile like '%8292909251'
order by x."createdAt";

-- একই মোবাইলে patients টেবিলে ক'টা রোগীর সারি আছে (ডুপ্লিকেট রেজিস্ট্রেশন
-- হয়ে থাকলে এখানেই দেখা যাবে)।
select id, name, "patientId", branch, "registrationDate", "createdAt", "updatedAt"
from public.patients
where mobile like '%8292909251'
order by "createdAt";
