# 🔒 LOCK NOTE — V194 (৩০.০৭.২০২৬ রাত) · খাতার সারি **B181**

> **এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।**

**ভার্সন:** `versionCode 194` · `versionName 1.94` · পর্দায় **V194**
⛔ **কোনো SQL লাগবে না।**

---

## TK কী রিপোর্ট করেছেন

> *"KNE-KISHAN5, +916207841890 — এই নম্বর দিয়ে লগইন করলে সম্পূর্ণ প্রজেক্ট ব্যবহার যখন করবে কোনো বাংলা আসতে বারণ করেছিলাম, তাহলে এখনো কেন অনেক জায়গাতে বাংলা আসছে?"*

## আসল কারণ (দুটো, কোড ধরে, আন্দাজ নয়)

### ১) পপ-আপের নিজের আলাদা উইন্ডো

`NoBengali.hookApp()` পুরো অ্যাপে সঠিকভাবে বসানো ছিল (প্রতিটা Activity-র নিজের পর্দা ঠিকই ঢাকা পড়ে)। কিন্তু **AlertDialog/পপ-আপের নিজের আলাদা window** থাকে — Activity-র সাধারণ পাহারা সেখানে পৌঁছায় না। শুধু `PremiumAlert.paint(dialog)` ডাকলেই `NoBengali.installDialog()` চালু হয়ে ওই পপ-আপকে ঢেকে দেয়।

পুরো প্রজেক্ট মিলিয়ে দেখা হয়েছে — **১২৮টা `AlertDialog.Builder` ব্যবহারের মধ্যে ১৫টায় এই ডাকটাই ছিল না।**

### ২) `FieldError.validate()`-এর কেন্দ্রীয় ফাঁক

Name/Amount ইত্যাদি ঘর ফাঁকা থাকলে যে সতর্কবার্তা Toast-এ দেখায় ("Title দিন", "Patient নাম দিন", "সঠিক Amount দিন" ইত্যাদি) — এই ফাংশন সরাসরি বাংলা টেক্সট ফেরত দিত, কোনো পাহারা ছাড়াই। Toast-এরও নিজের আলাদা উইন্ডো, তাই সেখানেও পর্দার পাহারা পৌঁছায় না। **এই একটামাত্র ফাংশন প্রজেক্টের ৭টা জায়গায়** ব্যবহার হয় (AppointmentActivity, BriefingActivity, PatientTimelineActivity ×২, PaymentActivity ×২, MedicinePaymentActivity), তাই একই ফাঁক ৭ বার দেখা যেত।

## ⚠️ নিজের একটা সম্ভাব্য গুরুতর ভুল ধরা পড়েছে ও তখনই সারানো হয়েছে

`FieldError.validate()` ঠিক করার প্রথম চেষ্টায় `return NoBengali.s(firstMsg)` লিখেছিলাম। কিন্তু `firstMsg` `null` হলে (মানে যাচাই **পাশ** করেছে, কোনো সমস্যা নেই) — `NoBengali.s(null)` খালি স্ট্রিং (`""`) ফেরায়, `null` নয়! প্রজেক্টের সব ব্যবহারকারী `if (vmsg != null) { Toast...; return }` প্যাটার্নে চেক করে — `""` ফেরত গেলে সেটাও `!= null` সত্যি হয়ে যেত, তাই **প্রতিটা সফল সেভও ভুল করে আটকে যেত।**

এটা নিজে ধরে সঙ্গে সঙ্গে সারানো হয়েছে — এখন `firstMsg?.let { NoBengali.s(it) }`, যা `firstMsg` `null` হলে `null`ই ফেরায়।

## সমাধান

১. **`FieldError.validate()`** — কেন্দ্রীয়ভাবে ঠিক করা হয়েছে, একটাই জায়গায় বদলে ৭টা ব্যবহারকারী একসাথে উপকৃত হয়েছে।
২. **১৫টা AlertDialog-এ `PremiumAlert.paint()` যোগ:**
   - `BriefingActivity.kt` — ১টা (Post Notice)
   - `ChamberAttendanceActivity.kt` — ৩টা (Payment, Edit Amount, Request Edit)
   - `PatientTimelineActivity.kt` — ৩টা (Edit Patient, Take Action, Add Referral Income, Edit Payment — মোট ৪টা আসলে)
   - `FollowUpActivity.kt` — ১টা (Edit Payment)
   - `EnquiryActivity.kt` — ১টা (Duplicate Mobile — বাংলা এখন নেই, ধারাবাহিকতার জন্য)
   - `ReportCardActivity.kt` — ২টা (Request Edit, Estimated Amount)
   - `ReportsActivity.kt` — ১টা (Staff/Branch Detail)
   - `FollowCalendarActivity.kt` — ১টা (Update Remark — বাংলা এখন নেই, ধারাবাহিকতার জন্য)

## দ্বিতীয়বার যাচাই (TK-এর নিয়ম — আন্দাজে না, সত্যতা যাচাই করে)

কাজ শেষে **চওড়া উইন্ডোতে আবার পুরো প্রজেক্ট স্ক্যান** করা হয়েছে। এতে আরও ৮টা সন্দেহজনক জায়গা এসেছিল (BriefingActivity, PatientTimelineActivity ×২, FollowUpActivity ×৩, PaymentActivity, ReportCardActivity)। **প্রতিটা হাতে-হাতে সরাসরি কোডে গিয়ে, দূরত্ব মেপে মিলিয়ে দেখা হয়েছে** — সবকটাই স্ক্যানের নিজের ভুল (ছোট/চওড়া উইন্ডোর কারণে ভুল মিলে যাওয়া — false positive), সত্যিকারের ফাঁক নয়।

## ⛔ যা ছোঁয়া হয়নি

- কোনো ডায়ালগের নিজের কাজ (Save/Cancel/validation logic) এক অক্ষরও বদলায়নি — শুধু `PremiumAlert.paint()` ডাকা যোগ হয়েছে (বিশুদ্ধ সংযোজন)।
- অন্য কোনো স্টাফের ফোনে (বাংলা-বন্ধ তালিকায় নেই এমন) **কিছুই বদলায়নি** — `NoBengali.active()` মিথ্যা হলে `paint()`/`s()` কিছুই করে না।

## যাচাই (কাজের পরে আবার, TK-এর নিয়ম)

- নিজের হাতে ব্র্যাকেট গোনা — **৯টা ফাইলেই পাশ**
- পাহারাদার `tk_guard.py` — **১৭টা যাচাই, সব পাশ**
- আগের অনুমোদিত কাজের যাচাই — **৫৯/৫৯ পাশ**
- ভার্সন চার জায়গায় এক — ZIP · `versionCode 194` · `versionName 1.94` · পর্দায় **V194**

**ফাইল:** `FieldError.kt` · `BriefingActivity.kt` · `ChamberAttendanceActivity.kt` · `PatientTimelineActivity.kt` · `FollowUpActivity.kt` · `EnquiryActivity.kt` · `ReportCardActivity.kt` · `ReportsActivity.kt` · `FollowCalendarActivity.kt` · `build.gradle.kts` · `DashboardActivity.kt` · `app.js` (শুধু ভার্সনের লেখা)

---

## 🔴 যা এখনো বাকি

- TK-এর লাইভ টেস্ট — **KNE-KISHAN5 দিয়ে লগইন করে প্রতিটা পপ-আপ ঘুরে দেখতে হবে।**
- **B148 — RLS** (⛔ TK-এর অনুমতি ছাড়া নিষেধ) · `03_NETLIFY_READY` Netlify-তে আপলোড (V184) · সপ্তাহখানেক পরে Supabase-এর খরচ দেখা।
