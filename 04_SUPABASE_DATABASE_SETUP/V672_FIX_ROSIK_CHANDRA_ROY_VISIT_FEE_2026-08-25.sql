-- =====================================================================
-- V672 (সংশোধনী) — ROSIK CHANDRA ROY-র হারিয়ে যাওয়া Visit Fee ফিরিয়ে বসানো
-- =====================================================================
-- এই রোগী আজ (25.08.2026) জলপাইগুড়িতে রেজিস্টার হয়েছিলেন, ৳400 Visit Fee
-- নেওয়া হয়েছিল — কিন্তু ওয়েবের একটা বাগের কারণে (এখন V672-এ ঠিক করা
-- হয়েছে) পেমেন্ট-সারিটা কখনো তৈরিই হয়নি। তাই "Return Fees" কাজ করছিল
-- না। এই SQL সেই একটা মিসিং সারি ফিরিয়ে বসায়।
--
-- চালানোর নিয়ম: Supabase → SQL Editor → New query → পুরো ফাইল পেস্ট → Run।
-- "Success. No rows returned" দেখলেই হয়ে গেছে — এরপর "Return Fees"
-- আবার চেষ্টা করলে কাজ করবে।
-- =====================================================================

insert into public.payments
  (id, "payType", "payLabel", "paymentLabel", "patientId", "patientCode",
   mobile, branch, name, date, amount, mode, remarks, "receivedBy", "createdBy", "updatedAt")
select
  'pay_' || replace(gen_random_uuid()::text, '-', ''),
  'visit_fee', 'Visit Fee', 'Visit Fee',
  p.id, p."patientId",
  p.mobile, p.branch, p.name,
  '2026-08-25', 400, 'CASH',
  'Visit Fee (V672 সংশোধনী — মূল সারি বাগে হারিয়ে গিয়েছিল)',
  '', '', now()::text
from public.patients p
where p."patientId" = 'JPE-25082026-003'
-- ⛔ নিরাপত্তা: যদি ইতিমধ্যে এই রোগীর কোনো visit_fee সারি থেকে থাকে
-- (যেমন এই SQL দুবার চালানো হয়), তাহলে আবার বসবে না — ডুপ্লিকেট নয়।
and not exists (
  select 1 from public.payments x
  where x."patientId" = p.id and x."payType" = 'visit_fee'
);
