# V252 LOCK NOTE (চূড়ান্ত — B300 থেকে B303.1 পর্যন্ত সব)

Base: V251 FINAL (uploaded PILES_CLINIC_APP_V251_FINAL.zip).

এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না,
কোনো working flow খারাপ করা যাবে না।

## এই ডেলিভারিতে যা হয়েছে (B300 → B301.2, khata-তে বিস্তারিত)

1. **Work Notebook (B300):** Calculator ও Sheet (row/column totals) সেকশন
   সম্পূর্ণ সরানো হলো (TK-নির্দেশ, শুধু ফোন)। শুধু ওই দুটোতে ব্যবহৃত হেল্পার
   (`Calc`, `buildSheet`, `sheetCells`, `fmt`) — সবই অপ্রয়োজনীয় হয়ে গিয়েছিল
   বলে সরানো হয়েছে।

2. **সবুজ থিম (B300):** `ModuleUi.kt` (শুধু Staff Profile / Work Notebook /
   Income-Expense — এই তিনটে নতুন মডিউল শেয়ার করে, প্রজেক্টের বাকি কোনো
   স্ক্রিন এটা ব্যবহার করে না) — `screen()`/`card()`/`heading()`/`button()`-এ
   Income & Expense-এর সবুজ থিম বসানো হলো (মকআপ TK-approved, "হ্যাঁ" পাওয়া
   গেছে Income & Expense-এর Save/Cancel/Back বোতাম সবুজ হওয়া নিয়েও)।

3. **Refund/টাকা-ফেরত নিয়ম বদল (B301 → B301.1 → B301.2, চূড়ান্ত):**
   TK-এর সাথে ধাপে-ধাপে আলোচনার পর চূড়ান্ত নিয়ম — **সেই ব্রাঞ্চের আজকের
   Chamber বন্ধ (বিদ্যমান "Close Chamber") না হওয়া পর্যন্ত Staff নিজেই
   যেকোনো Refund করতে পারবেন (টাকা যেদিনই জমা হোক), Master-এর অনুমতি
   লাগবে না। Chamber বন্ধ হয়ে গেলে — তখন থেকে Master-এর অনুমতি লাগবে।**
   কোনো সময়-ভিত্তিক ছাড় বা আলাদা "Reopen" বোতাম বানানো হয়নি (TK-এর স্পষ্ট
   নির্দেশ)। বিদ্যমান `ChamberCloseRepository.isClosed()`-ই একমাত্র উৎস —
   নতুন কোনো DB টেবিল/কলাম লাগেনি।
   ⚠️ শুধু Android — ওয়েবে (`app.js`) এখনো পুরনো নিয়ম (সব Staff-রিফান্ড
   Master-approval-এ যায়)।

## অপরিবর্তিত (নিশ্চিত)

- জমার চেয়ে বেশি Refund আটকানোর পাহারা (V217 §13) — এক অক্ষরও বদলায়নি।
- Chamber Close/Chamber Attendance-এর নিজের কোনো লজিক/বোতাম — শুধু তার
  বিদ্যমান স্ট্যাটাস read-only পড়া হয়েছে।
- Check-in/Check-out, Work Entries, Notes, Outside Calls, Reports (Work
  Notebook) — কিছুই বদলায়নি।
- Add Collection/Add Expense/Daily Ledger/Monthly Summary/Ledger Sheet-এর
  আসল হিসাব — কিছু বদলায়নি, শুধু রং।

## ফাইল বদলেছে

`WorkNotebookActivity.kt` · `ModuleUi.kt` · `PaymentModel.kt` ·
`PaymentRepository.kt` · `PaymentActivity.kt` · `build.gradle.kts`
(versionCode 251→252, versionName 2.51→2.52)।

## যাচাই

ব্র্যাকেট-প্যারেন গোনা (Python দিয়ে, প্রতিটা বদলানো ফাইলে) ✅ পাশ ·
`00_GUARD/tk_guard.py --release` **সব ✅ পাশ** (V252)।

🔴 **TK-এর লাইভ টেস্ট বাকি:**
- Staff Profile ও Work Notebook — রং ও Calculator/Sheet বাদ পড়েছে কিনা।
- একটা ব্রাঞ্চে Chamber Close না করে Refund করে "Refund now" (সরাসরি) হচ্ছে
  কিনা, তারপর Chamber Close করে আবার Refund করে "Send refund request"
  (Master-approval) হচ্ছে কিনা — ফোন ও কম্পিউটার দুটোতেই।
- একটা রোগীর Advance সম্পূর্ণ Refund করে দেখুন Patient/Visit/Enquiry কোনো
  কার্ডেই নেই, Draft-এর নতুন "💸 Refunded" ঘরে আছে (ফোন ও কম্পিউটার দুটোতেই)।
  Restore চেপে Patient কার্ডে ফিরছে কিনা, Delete চেপে Trash-এ যাচ্ছে কিনা।
- একটা কম্পিউটার/ফোন থেকে Chamber Close করে, অন্য কম্পিউটার থেকে Draft/
  Payment Delete চেপে দেখুন "Master অনুমতি লাগবে" দেখাচ্ছে কিনা।

## ==================== পরবর্তী কাজ (B301 → B303.1) ====================

উপরের ধাপের পরে TK একই সেশনে আরও কিছু কাজ দিয়েছেন — সংক্ষেপে (khata-তে
B301/B301.1/B301.2/B301.3/B302/B302.1/B302.2/B303/B303.1-এ পূর্ণ বিবরণ):

- **B301.2 (চূড়ান্ত রিফান্ড নিয়ম):** Refund — Master সরাসরি; Staff-এর
  জন্য, সেই ব্রাঞ্চের আজকের Chamber বন্ধ না হওয়া পর্যন্ত সরাসরি (টাকা
  যেদিনই জমা হোক), Chamber বন্ধ হলে Master-এর অনুমতি লাগে। ফোনে
  `PaymentRepository.chamberOpenToday()`।
- **B301.3:** একই নিয়ম ওয়েবেও (`wlv1ChamberOpenTodayFailSafe`) + ফোনে
  অফলাইন/অন্য-ফোনে-বন্ধ হলে ভুল করে auto-approve না হওয়ার নিরাপত্তা
  (cloud-null মানে নিরাপদ দিকেই "বন্ধ" ধরা)।
- **B302 + B302.1:** Advance/Treatment টাকা সম্পূর্ণ Approved-Refund হয়ে
  নেট জমা ঠিক ₹0 হলে — রোগী আর Enquiry/Visit/Patient কার্ডে দেখাবে না,
  Draft-এ নতুন "💸 Refunded" ঘরে (রেকর্ড অক্ষত)। সেখান থেকে হাতে Restore
  (নতুন `patients.refundRestoredBy`) ও Delete (Trash Bin-এ, প্রমাণিত
  Delete-Patient নিয়মেই) দুটোই করা যায়। নতুন টাকা জমা পড়লে স্বয়ংক্রিয়
  ফেরত।
- **B302.2:** উপরের পুরো ফিচার ওয়েবেও (`wlv1RefundedMobilesSet`,
  `draffHome`-এর নতুন Refunded বাকেট, `restoreDraftEntry`-র নতুন শাখা)।
- **B303 + B303.1:** যাচাইয়ে ধরা পড়া পুরনো বাগ — ওয়েবের
  `wlv1ChamberClosedFor()` ভুল id খুঁজত (Chamber বন্ধ কখনো ধরাই পড়ত না,
  সব Draft/Payment Delete বোতামে প্রভাব ফেলত) — id ঠিক + লোকাল-সেভ + Delete-
  চাপার মুহূর্তে live cloud-refresh (`wlv1PullChamberCloseFromCloud`), যাতে
  অন্য ডিভাইস থেকে বন্ধ করা Chamber-ও ধরা পড়ে।

**নতুন SQL প্যাচ (একবার Run করা লাগবে):**
`04_SUPABASE_DATABASE_SETUP/PATCH_2026-08-02_patients_refundRestoredBy.sql`
(শুধু `patients` টেবিলে একটা nullable কলাম যোগ করে, নিরাপদ, বারবার চালানো
যায়)।

⛔ টাকার সীমা/pending-hisab পাহারা (V217 §13), Chamber Close/Attendance-এর
নিজের লজিক, Check-in/Check-out/Work Entries/Notes/Reports — কিছুই বদলায়নি।
⚠️ Draft-Delete বোতামের Chamber-চেক (`wlv1CanDeletePaymentNow`) ওয়েবে
এখনো তালিকা-লোডের সময় নয়, শুধু Delete-চাপার মুহূর্তে তাজা — এটা ইচ্ছাকৃত
(speed রক্ষা করতে), ভুলে বাদ পড়েনি।

