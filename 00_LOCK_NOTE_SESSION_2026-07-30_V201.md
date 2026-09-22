# 🔒 LOCK NOTE — V201 (৩০.০৭.২০২৬ রাত)

**ভার্সন:** `versionCode 201` · `versionName 2.01` · পর্দায় **V201** · খাতার সারি **B193**

---

## ✅ SQL চালানো হয়ে গেছে

TK-এর ফটো-প্রুফ (30.07.2026 রাত ৬.২৮): Supabase SQL Editor-এ চালিয়ে **"Success. No rows returned"** — নিশ্চিত।

```
alter table public.doctor_visits
  add column if not exists "expectedPatientDate" text;
```

- নতুন কলাম `doctor_visits.expectedPatientDate` এখন লাইভ ডেটাবেসে আছে
- পুরনো কোনো টেবিল/কলাম/সারি ছোঁয়া হয়নি
- **EXPECTED ফিচার এখন সম্পূর্ণ কাজ করবে** — আর কোনো SQL বাকি নেই

---

## ⛔ সবার আগে — স্থায়ী নিয়ম
> **এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।**

---

## TK-এর নির্দেশ ও আলোচনার সারাংশ

স্টাফ বুঝতে পারছিলেন না কোন RMP-কে এই মাসে কল করা হয়েছে, কোনটা বাকি। কয়েক দফা আলোচনার মাধ্যমে TK-এর সঙ্গে নিচের প্ল্যান নিশ্চিত হয়:

1. **ALL RMP** — ব্রাঞ্চ-ভিত্তিক সব ডাক্তারের নম্বর
2. **PENDING** — প্রতি ইংরেজি মাসে ডাক্তারকে একবার কল করতে হয়; না করা হলে এখানে দেখাবে
3. **CALLED** — এই মাসে যাদের একবার কল করা হয়ে গেছে
4. **EXPECTED** — কল করার সময় ডাক্তার যদি তারিখ বলেন (পেশেন্ট আসতে পারে), সেই নম্বর+তারিখ এখানে যাবে, আর স্টাফের ঘন্টায় "আজ কল করুন" নোটিফিকেশন আসবে

TK স্পষ্ট করে দেন: পুরনো Today Call/Overdue Call বক্স বাদ দিয়ে **শুধু এই ৪টাই** থাকবে। মাস গোনা হবে ১ তারিখ থেকে মাসের শেষ তারিখ পর্যন্ত।

⚠️ **গুরুত্বপূর্ণ পার্থক্য যা TK-কে স্পষ্ট করে জানানো হয়েছে ও তিনি বুঝে নিয়েছেন:** আগে "Pending" মানে ছিল "Overdue Call" (নেক্সট-কল-তারিখ পার হয়ে যাওয়া)। নতুন "Pending"-এর অর্থ সম্পূর্ণ আলাদা — "এই মাসে একবারও কল হয়নি"। TK নিজে নিশ্চিত করেছেন এই নতুন অর্থই চান, পুরনো Overdue ধারণা আর কোনো বক্সে থাকবে না।

## ✅ কী করা হলো

### ১. চারটে বক্স (activity_doctorvisit.xml + DoctorVisitActivity.kt)
পুরনো ৪টা ভিউ-আইডি (`statToday`/`statPending`/`statCalled`/`statAll`) **পুনর্ব্যবহার** করা হয়েছে — নতুন কোনো আইডি/বাইন্ডিং যোগ করতে হয়নি, তাই ভাঙার ঝুঁকি নেই:
- `statToday` → এখন **EXPECTED** (নতুন গোলাপি রং — `bg_dv_stat_expected.xml` / `bg_dv_stat_expected_selected.xml`)
- `statPending` → এখন **PENDING** (নতুন সংজ্ঞা, রং অপরিবর্তিত কমলা)
- `statCalled` → **CALLED** (রং/লজিক অপরিবর্তিত, শুধু লেবেল)
- `statAll` → **ALL RMP** (রং/লজিক অপরিবর্তিত, শুধু লেবেল)

### ২. PENDING/CALLED-এর লজিক (DoctorVisitModel.kt, DoctorVisitActivity.kt)
নতুন হেল্পার `DoctorVisitModel.isThisMonth(dateStr)` — একটা তারিখ এই ইংরেজি ক্যালেন্ডার মাসে পড়ে কিনা (ISO তারিখের প্রথম ৭ অক্ষর "yyyy-MM" মিলিয়ে)। বিদ্যমান `calledThisMonth()` ফাংশন (যেটা আগে থেকেই ছিল, `lastCallDate` ধরে) অপরিবর্তিত রইল; PENDING এখন তারই বিপরীত (`!calledThisMonth(it)`)।
⛔ **কোনো নতুন কলাম লাগেনি এই দুটোর জন্য** — `lastCallDate` আগে থেকেই ছিল।

### ৩. EXPECTED (নতুন — SQL কলাম লাগে)
- **Log Call ফর্মে** (DoctorVisitActivity.showLogCallDialog) নতুন ঐচ্ছিক ঘর "Expected Patient Date" — Next Call Date-এর ঠিক নিচে, একই ধাঁচে; পাশে "✕ Clear" বোতাম। আগে থেকে সেট করা থাকলে প্রি-ফিল দেখায়।
- **DoctorVisitModel.buildCallUpdateFields()** ও **DoctorVisitRepository.logCall()** — নতুন `expectedPatientDate` প্যারামিটার (ডিফল্ট ফাঁকা, পুরনো আচরণ অক্ষত)।
- **দুটো Log Call পাথই** (মূল ফর্ম + কার্ডের রিমার্ক-বাক্স কুইক-এডিট) সাবধানে `expectedPatientDate` **সংরক্ষণ করে** — না ছুঁলে কখনো মুছে যাবে না (Next Call Date-এর মতোই একই সুরক্ষা-নিয়ম)।
- **কার্ডে দেখানো** (item_doctor_card.xml + DoctorVisitAdapter.kt) — নতুন `tvExpected` লাইন, গোলাপি রঙে "🤞 Expected patient: [তারিখ]", ফাঁকা হলে সম্পূর্ণ লুকানো (tvReferralSummary-র একই প্যাটার্ন)।
- **EXPECTED ট্যাব** — `expectedPatientDate` সেট করা সব ডাক্তার, তারিখ-ক্রমে সাজানো (কাছেরটা আগে)। তারিখ অতীত/আজ/ভবিষ্যৎ যাই হোক, TK নিজে "Clear" না করা পর্যন্ত এখানেই থাকেন — কিছুই হারায় না।

### ৪. ঘন্টা-নোটিফিকেশন (BellCounter.kt, DoctorVisitRepository.kt)
নতুন `DoctorVisitRepository.fetchExpectedTodayCount(branchFilter)` — আজকের তারিখে expectedPatientDate থাকা ডাক্তারের সংখ্যা (সস্তা count-only অনুরোধ, `PaymentRepository.fetchPendingBackdateCount()`-এর হুবহু একই প্যাটার্ন)। `BellCounter.count()`-এ যোগ — সব রোলের জন্য (কল স্টাফরাই করেন), স্টাফ/ফিল্ড অফিসার শুধু নিজের ব্রাঞ্চ দেখেন, Master সব ব্রাঞ্চ।

## ⛔ যা ছোঁয়া হয়নি
Chamber Attendance · Payment · Follow-up · Enquiry/Registration · Referral Income · Doctor delete/edit — প্রজেক্টের অন্য কোনো স্ক্রিন বা ফাংশন ছোঁয়া হয়নি। Log Call ফর্মের Remarks/Next Call Date/callHistory-এর পুরনো নিয়ম অক্ষত।

## 🔍 যাচাই
- পাহারাদার (`tk_guard.py`) **১৭/১৭ পাশ** (186 Kotlin ফাইল, 231 XML ফাইল)
- YACHAI স্ক্রিপ্ট **৫৯/৫৯ পাশ**
- ভার্সন দুই জায়গায় এক — `build.gradle.kts` (201/2.01) ও `DashboardActivity.kt` (V201)
- প্রতিটা নতুন/পরিবর্তিত ফাংশনের স্কোপ হাতে যাচাই (এই সেশনেই আগে দুটো একই ধরনের ভুল ধরা পড়েছিল — `dp()` স্থানীয় ফাংশন ভুলবশত ব্যবহার হচ্ছিল, ধরে ঠিক করা হয়েছে)
- `logCall()`-এর দুটো কল-সাইটই (মূল ফর্ম + রিমার্ক-বাক্স কুইক-এডিট) হাতে মিলিয়ে দেখা হয়েছে — কোনোটাতেই `expectedPatientDate` ভুলবশত মুছে যাওয়ার ঝুঁকি নেই

## 🔴 এখনো বাকি
- **TK-এর লাইভ টেস্ট** — ৪টা বক্স, Log Call-এ নতুন ঘর, কার্ডে ব্যাজ, ঘন্টা-নোটিফিকেশন
- **B190 — ঝুঁকিসহ লোডিং-ফিক্স** — পরের সেশনে আলোচনা করে ফাইনাল হবে
- **B148 — RLS** ⛔ TK-এর অনুমতি ছাড়া নিষেধ
- `03_NETLIFY_READY` Netlify-তে আপলোড (TK-এর কাজ)
