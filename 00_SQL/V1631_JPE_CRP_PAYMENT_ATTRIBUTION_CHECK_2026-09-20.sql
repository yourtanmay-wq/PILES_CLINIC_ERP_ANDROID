-- 🔵 শুধু-পড়ার (READ-ONLY) যাচাই SQL — কিছু বদলায়/মোছে না।
-- JPE-CRP-এর অভিযোগ: পেশেন্ট JPE-24022026-001 আগস্টের ৯ তারিখে চেম্বারে
-- আসেননি, তবু তাঁর নামে ৯.০৮.২০২৬ ৩.১৩ PM-এ ₹1,000 (9th Payment) পেমেন্ট
-- বসানো আছে, যেটা তিনি নেননি বলছেন। এই SQL সেই একটা সারির আসল, পুরো
-- তথ্য বার করে দেখাবে (কে বসিয়েছিলেন, পরে কেউ বদলেছেন কিনা)।
select id, "patientId", mobile, branch, name, date, amount, mode,
       "payType", "payLabel", "paymentLabel", remarks,
       "receivedBy", "createdBy", "createdAt",
       "editedBy", "editedAt", "editHistory", "updatedAt"
from public.payments
where "patientId" = 'JPE-24022026-001'
order by date;
