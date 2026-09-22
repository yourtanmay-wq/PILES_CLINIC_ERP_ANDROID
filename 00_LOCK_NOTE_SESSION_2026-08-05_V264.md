# V264 LOCK NOTE (চূড়ান্ত — B456 থেকে B468 পর্যন্ত সব)

Base: V263 FINAL (uploaded PILES_CLINIC_APP_V263_FINAL_66.zip)।

🔒 এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না,
কোনো working flow খারাপ করা যাবে না, অ্যাপ স্লো করা যাবে না।

## এই ডেলিভারিতে যা হয়েছে (B456 → B468, খাতায় বিস্তারিত)

1. **Doctor Note — ৫টা ধাপই TK-এর নম্বর-ধরে চূড়ান্ত (B456–B462):** টুলবার
   কম্প্যাক্ট, "PATIENT'S DETAILS" বার বাদ, প্রতিটা ধাপের সেকশন-হেডার বাদ,
   লেবেল পাশাপাশি/উপর-নিচে (নম্বর-ধরে TK-এর সিদ্ধান্ত), Fistula Opening/
   Bleeding চেকবক্সে বাংলা নতুন লাইনে, Investigations-এ বাংলা বাদ, তিনটে
   Spinner (Grade/Patient Decision/Acute-Chronic)-এ প্রমাণিত প্রিমিয়াম
   পপ-আপ, কিছু বক্সের খালি-উচ্চতা কমানো।
2. **Backdate Payment Permissions (B463):** স্টাফ-মোবাইল টাইপ করার বদলে
   তালিকা থেকে বাছার ব্যবস্থা।
3. **নতুন "Dialer" (B464):** ☰ মেনু, নম্বর টাইপ/পেস্ট করে কল — Enquiry-তে
   মিললে সেখানেই কল-গোনা বাড়ে, না মিললে নতুন `dialer_calls` লগে। Daily
   Report-এর "App Calls"-এ স্বয়ংক্রিয় গোনা।
4. **Work Notebook IN/OUT TIME (B465):** IN TIME-এ Master-নোটিফিকেশন +
   জোরপূর্বক WhatsApp (দুই পথেই), OUT TIME-এ "Are you sure" বাদ, বাংলা,
   কাস্টম-কারণ বক্স, OUT TIME-এও জোরপূর্বক WhatsApp।
5. **Staff Photos (B466):** তালিকা থেকে ডাক্তার বাদ, শুধু স্টাফ।
6. **Briefing/Messaging (B467):** সবাই নোটিশ পাঠাতে পারবেন, ১০-মিনিট-পরপর
   জোরপূর্বক নোটিফিকেশন (স্নুজ-সহ), মেসেজের নম্বরে ট্যাপ করলে সরাসরি কল,
   কল ফেরত এলে Enquiry/Patient-এ অ্যাকশন বা নতুন Enquiry-সাজেশন।
7. **দ্বিতীয়বার সততার সাথে সম্পূর্ণ সেশন যাচাই (B468):** ১টা প্রকৃত ফাঁক
   (EnquiryActivity prefill-এর ক্রম) পাওয়া গেছে ও ঠিক করা হয়েছে।

## ⛔ কোনো ঝুঁকি ছাড়াই যা যা অক্ষত রাখা হয়েছে

- Doctor Note-এর কোনো সেভ-লজিক/checkbox-tag/id বদলায়নি — শুধু দেখানোর
  চেহারা।
- Delete/Approve-Refund/Approve-Reopen — এখনো Master-only-ই।
- `FollowUpRepository.updateRemark()`/`CallChooser`/`BriefingRepository`-র
  ভিতরে এক অক্ষরও ছোঁয়া হয়নি — শুধু নতুন কল-সাইট।

## 🔴 TK-কে করতে হবে

1. `04_SUPABASE_DATABASE_SETUP/V264_DIALER_CALLS_2026-08-05.sql` Supabase-এ RUN।
2. Doctor Note ৫টা ধাপ, Dialer, Work Notebook IN/OUT TIME, Briefing — সব
   লাইভ টেস্ট।
