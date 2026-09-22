# V378 — Deleted Referral Income আর ফিরে আসবে না

তারিখ: 14.08.2026 (IST)

- Live proof: ₹10,000 Delete হওয়ার পরে পুরনো queued Referral snapshot Cloud-এ replay হয়ে এন্ট্রিটি ফিরিয়ে এনেছিল।
- Delete-এর আগে ও সফল Delete-এর পরে একই RMP row-এর শুধু `referralPayments`, `referralPaid`, `referralDue` পুরনো queued fields বাদ হয়।
- একই row-এর Call History, Remark, Date বা অন্য pending field অক্ষত থাকে।
- Delete Cloud-এ ব্যর্থ হলে নতুন সঠিক deleted-state retry queue-তে থাকে।
- সফল হলে ফোনের local display note-তেও নতুন deleted-state বসে।
- একই সুরক্ষা Referral Income Edit-এর ক্ষেত্রেও আছে, যাতে পুরনো snapshot পরে edited amount/status ফিরিয়ে না দেয়।
- ডিজাইন, Web, Database schema এবং অন্য workflow অপরিবর্তিত।
