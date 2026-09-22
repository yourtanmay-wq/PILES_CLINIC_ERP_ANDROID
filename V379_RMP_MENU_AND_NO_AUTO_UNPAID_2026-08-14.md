# V379 — RMP Referral Income Menu + No Automatic Unpaid

তারিখ: 14.08.2026 (IST)

- V378-এর Referral Delete ফিরে না আসার সমাধান অক্ষত।
- Referral Income menu-এর ৫টি route code এবং Supabase function মিলিয়ে যাচাই: Default, Patient Commission/Payment, Summary, Previous Records, Pending Approvals।
- RMP Default ও Patient Commission Save-এর পরে একই Cloud setting পুনরায় পড়ে মিললেই শুধু “saved” দেখাবে।
- Android Patient Timeline Add form: Paid/Unpaid কোনোটিই আগে থেকে selected নয়।
- Android Doctor/RMP Add form: `Select Status` আগে দেখা যাবে; Paid/Unpaid নিজে বাছতে হবে।
- Web Add form: `Select Status`; Paid/Unpaid নিজে বাছতে হবে।
- Status না বাছলে `Select Paid or Unpaid` দেখিয়ে Save বন্ধ থাকবে।
- পুরনো saved record-এর আসল Paid/Unpaid, Edit form, Summary হিসাব বা legacy fallback বদলানো হয়নি।
- অন্য ডিজাইন, Database schema এবং workflow অপরিবর্তিত।
