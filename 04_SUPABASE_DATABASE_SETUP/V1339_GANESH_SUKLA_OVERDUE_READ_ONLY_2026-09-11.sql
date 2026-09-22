-- ⛔ শুধু দেখার SQL — একটাও সারি লেখে/বদলায়/মোছে না।
-- TK-এর অভিযোগ: Ganesh Chandra Roy ও Sukla Roy-কে বারবার "No more calls"-এ
-- পাঠানো হয়েছে (staff-রা কনফার্ম করেছেন), তবু Follow-up-এর "Overdue" ট্যাবে
-- এখনো দেখাচ্ছে। এই কোয়েরি দুটো সন্দেহভাজন কারণ একসাথে দেখায়:
--   ১) একই রোগীর নামে একাধিক সারি (duplicate) আছে কিনা, আর থাকলে কোনটায়
--      noMoreCalls=true বসেছে আর কোনটা এখনো false।
--   ২) যেটাতে noMoreCalls=true হওয়ার কথা, সেটা আদৌ সত্যিই ডেটাবেসে বসেছে
--      কিনা (লেখাটাই ব্যর্থ হয়ে থাকতে পারে)।

SELECT
  id, "refId", mobile, name, stage, status,
  "noMoreCalls", "nextFollow", "lastRemark", "lastCallDate",
  "callCount", "updatedAt", "createdAt", branch
FROM public.followups
WHERE name ILIKE '%ganesh%roy%'
   OR name ILIKE '%sukla%roy%'
ORDER BY name, "updatedAt" DESC;
