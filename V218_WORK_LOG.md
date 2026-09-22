# V218_WORK_LOG.md (আগে V217_WORK_LOG.md নামে শুরু হয়েছিল, একই সেশনেই V218-এ ফাইনাল)

**Base:** PILES_CLINIC_APP_V216_FINAL → **V218 (versionCode 218 / 2.18)**। তারিখ: 31.07.2026 IST। Owner: TK BISWAS।
**উপলক্ষ:** TK-এর "Master Fix Order §14" (Staff Same-Day Reject/Delete) + Refund audit + কম্পিউটারেও Refund/Delete (Design Proof পাশ করার পরে) + একাধিক দফা "সম্পূর্ণ অডিট করুন" নির্দেশ।
**Environment:** আগের সেশনগুলোর মতোই — Claude (Cowork) cloud container, আসল Android device/Gradle build/live Supabase নেই। তাই static check (bracket/brace balance + node --check) করা হয়েছে; device/build test TK-এর।

## সত্যিই code-এ করা হয়েছে (static check pass)

| # | কাজ | ফাইল | প্রমাণ |
|---|-----|------|--------|
| 1 | Refund cloud-save ব্যর্থ হলে "Success" দেখানোর বাগ ঠিক — `saveRefund` এখন `RefundResult` (আসল cloud-status) ফেরত দেয়, আগের `return ok \|\| context != null` সরানো হয়েছে | PaymentRepository.kt, PaymentModel.kt, PaymentActivity.kt | brace-balance ✅ |
| 2 | জমার চেয়ে বেশি Refund বন্ধ — screen-স্তরে তাৎক্ষণিক চেক + `saveRefund`-এর ভিতরে pending refund-সহ চূড়ান্ত পাহারা | PaymentRepository.kt, PaymentActivity.kt | brace-balance ✅ |
| 3 | Refund-এর Paid/Due হিসাব Timeline ও Report Card-এ সঠিক — নতুন `paidEffect` ফিল্ড (approved refund বিয়োগ, pending/rejected অপ্রভাবিত); Refund row 3-tap Edit দিয়ে বদলানো বন্ধ | PatientTimelineRepository.kt, ReportCardActivity.kt, PatientTimelineActivity.kt | brace-balance ✅ |
| 4 | Staff Same-Day Reject (Master Fix Order §14, item 1) — আজ/গতকাল + নিজের ব্রাঞ্চ + চেম্বার-বন্ধ-না-থাকলে সঙ্গে সঙ্গে; পুরনো হলে Master-কে ঘন্টায় জানানো | PatientTimelineActivity.kt | brace-balance ✅ |
| 5 | Staff Same-Day Full Patient/Enquiry Delete → Trash (Master Fix Order §14, item 2) — একই নিয়ম; সবসময় Trash-এ (Permanent নয়), Master Restore করতে পারবেন; Master-এর জন্য অপরিবর্তিত | PatientTimelineActivity.kt (reuses existing `DeletePermission.canDeleteEntryNow` ও `TrashHelper.moveToTrashWithFollowupCascade`) | brace-balance ✅ |
| 6 | CHECK-UP Loading কমানো — একই রোগীর ঠিকানা/বয়স/লিঙ্গ ৫ মিনিটের ভিতরে দ্বিতীয়বার খুললে নেট-কল লাগে না | AddressTagRepository.kt (নতুন `fetchDemographicsCached`), DoctorQueueActivity.kt | brace-balance ✅ |
| 7 | CHECK-UP Back → তালিকার স্ক্রল-জায়গা ধরে রাখা | DoctorQueueActivity.kt (`renderRows`) | brace-balance ✅ |
| 8 | Follow-up Scroll Position ধরে রাখা (একই কৌশল) | FollowUpActivity.kt (`buildRows`) | brace-balance ✅ |
| 9 | Website Password — hash থাকলে (PBKDF2, Web Crypto) সেটা দিয়েই আগে যাচাই; hash না থাকলে আগের plaintext পথ অক্ষত | 03_NETLIFY_READY/app.js, assets/www/app.js (নতুন `cloudPasswordHashForMobile`, `pbkdf2VerifyWeb`) | node --check ✅ দুই ফাইলেই |

## যা এখনো Manual/বাকি (সৎভাবে)

- **Refund-এর ৫টা নতুন Supabase কলাম:** `V217_REFUND_SQL_COPY_PASTE.sql` ফাইলে আলাদা করে রাখা হয়েছে (আগে থেকেই V215_SAFE_MIGRATION-এ ছিল) — TK না চালালে Refund সেভ Supabase-এ silently ব্যর্থ হবে। **একবারই চালাতে হবে, যদি আগে না চালানো থাকে।**
- **item 10, Notification (FCM push):** কোডের মধ্যে যতটা সম্ভব (drop-in source `10_FUTURE_PLANS/fcm_push_ready/`) আগে থেকেই আছে, কিন্তু **আসল push পাঠাতে Firebase Console-এ প্রজেক্ট বানিয়ে `google-services.json` লাগবে — এটা সম্পূর্ণ TK-কে বাইরে থেকে করতে হবে, কোনো Code দিয়ে সম্ভব না।** এখনকার in-app ঘন্টা/bell ব্যবস্থা (Refund/Delete/Reject-এর সব অনুরোধ) আগের মতোই কাজ করে।
- **App.js দুই কপির পুরনো ফারাক (নতুন আবিষ্কার):** `03_NETLIFY_READY/app.js` ও ফোনের ভিতরের `assets/www/app.js` — এই সেশনের আগে থেকেই প্রায় ৩৬০০ লাইনের ফারাক আছে (ফোনের কপি পুরনো)। এটা এই সেশনে তৈরি হয়নি, শুধু ধরা পড়েছে। বড় আকারে মেলানো এই সেশনে করা হয়নি (ঝুঁকি বেশি, TK-এর আলাদা অনুমতি দরকার) — শুধু password-fix দুটো কপিতেই আলাদাভাবে বসানো হয়েছে।
- **বাস্তব Build/Test:** আগের সব সেশনের মতোই — device/Android Studio build TK-কেই করতে হবে।

## 🔴 Self-Audit (31.07.2026, একই সেশন, TK-এর নির্দেশে — "আন্দাজে কথা না বলে সম্পূর্ণ অডিট করুন")

**কর্তা:** Cloud AI (Claude)
**তারিখ-সময়:** 31.07.2026, রাত (IST)

উপরের ৯টা কাজ আবার নিজে খুলে পড়ে যাচাই করার সময় **২টা সত্যিকারের বাগ ধরা পড়েছে নিজের V217 কাজেই** — এগুলো TK-কে না জানিয়ে চাপা দেওয়া হয়নি, এখানেই লেখা হলো:

| # | সমস্যা | ফাইল | কী ছিল ভুল | ফিক্স | Test |
|---|--------|------|-----------|-------|------|
| 1 | Reject-এর Same-Day তারিখ ভুল উৎস থেকে নিচ্ছিল | PatientTimelineActivity.kt (`confirmStatusChange`) | Visit-stage (রেজিস্টার্ড কিন্তু Treatment নয়) রোগীর Reject-এ ভুল করে `currentEnquiryDate` (অনেক পুরনো হতে পারে) ব্যবহার হচ্ছিল, `currentRegistrationDate`-এর বদলে — ফলে আজকের Visit-ও "পুরনো" ধরে অকারণে Master-অনুমতি চাইত | এখন ঠিক `isRegistered`-এর একই নিয়মে তারিখ বাছা হয় | brace-balance ✅ পাশ, static — device-এ TK-কে যাচাই করতে হবে |
| 2 | Thread-safety ঝুঁকি | AddressTagRepository.kt (নতুন cache) | সাধারণ `HashMap` ব্যবহার হচ্ছিল, যেটা একাধিক CHECK-UP প্রায় একসাথে খুললে (আলাদা IO-thread থেকে) ক্র্যাশ/ভুল ডেটার ঝুঁকি তৈরি করত | `ConcurrentHashMap`-এ বদলানো হলো | brace-balance ✅ পাশ |
| 3 | 🔴 Report Card cache-first paint-এ PAID ভুল ₹0 দেখাত — **শুধু Refund থাকা রোগীর নয়, সবার** | TimelineCache.kt, ReportCardActivity.kt | `ReportCardActivity`-এর নতুন `paidTotal`/`paidThisDay` হিসাব `it.paidEffect` থেকে আসে, কিন্তু `TimelineCache` (V216 §10-এর cache-first ব্যবস্থা, ফোনে আগের দেখা রিপোর্ট সঙ্গে সঙ্গে দেখায়) এই নতুন ফিল্ডটা সেভ/লোড করত না — তাই cache থেকে প্রথম paint-এ সব সময় `paidEffect=0.0` (default) পেত, PAID বাক্স মুহূর্তের জন্য ₹0 দেখাত, কিছুক্ষণ পরে fresh fetch এসে ঠিক হয়ে যেত | `TimelineCache.save()`/`load()`-এ `paidEffect` যোগ করা হলো — cache-এও এখন সঠিক signed মান থাকে | brace-balance ✅ পাশ। ⚠️ **এই একটা bug সবচেয়ে গুরুত্বপূর্ণ ছিল — এটাই বেশি ব্যবহারকারীকে প্রভাবিত করত।** |
| 4 | 🔴🔴 **Screen ও Print-এ ভিন্ন PAID/DUE** — সবচেয়ে গুরুতর, রোগীর হাতে যাওয়া কাগজ | ReportCardPrinter.kt | `ReportCardActivity.kt` (স্ক্রিন) ঠিক হয়েছিল, কিন্তু ছাপার জন্য আলাদা ফাইল `ReportCardPrinter.kt`-এ **হুবহু একই পুরনো বাগ** (unsigned `paymentAmount` দিয়ে যোগ, approved refund বিয়োগ হত না) রয়ে গিয়েছিল — স্ক্রিনে এক PAID, প্রিন্টে আরেক PAID দেখাতে পারত | `paidTotal`/`paidThisDay`-কেও `paidEffect`-এ বদলানো হলো, ঠিক ReportCardActivity.kt-এর নিয়মে | brace-balance ✅ পাশ। **এটা দ্বিতীয় দফা গভীর অডিটে ধরা পড়েছে — "প্রজেক্টের অন্য কোথাও একই দুর্বলতা আছে কিনা" এই নিয়ম মেনে খোঁজার সময়।** |

**চারটে বাগই V217-এর নিজের নতুন কোডে ছিল (Refund ফিচারের সাথে যুক্ত), আগের কোনো Approved কাজ/ডিজাইন/workflow ছোঁয়নি বা ভাঙেনি। এই ধরনের আরও লুকানো জায়গা আছে কিনা যাচাই করা হয়েছে (Chamber Board, Today's Collection, Payment Collection — এগুলো V216 §13 থেকেই refund-aware ছিল, এই সেশনে ছোঁয়া হয়নি, ভাঙেওনি)।**

### 🔴🔴 পঞ্চম বাগ — প্রজেক্টের নিজস্ব `00_GUARD/tk_guard.py` চালিয়ে ধরা পড়েছে

TK-এর কড়া নির্দেশে (তৃতীয় দফা, "নিজে আবার সম্পূর্ণ যাচাই করুন") এই সেশনে **প্রথমবার প্রজেক্টের নিজস্ব স্বয়ংক্রিয় Guard টুল সরাসরি চালানো হলো** (আগে শুধু হাতে/স্ক্রিপ্টে bracket-count করা হচ্ছিল, যেটা এই Guard-এর মতো ব্যাপক নয়)। Guard একটা লক করা নিয়ম ভাঙা ধরেছে:

**[৯.১৪] No-Bengali guard:** স্টাফের কিছু পর্দায় (বিশেষ ফোনে) বাংলা সম্পূর্ণ বন্ধ থাকার নিয়ম আছে (`NoBengali.kt`)। নিচের Bengali UI-টেক্সট ধরা পড়েছে:

| ফাইল | কী ছিল | কার তৈরি |
|---|---|---|
| PatientTimelineActivity.kt | নতুন Reject-অনুমতি dialog + 3-tap-block টোস্ট (৫টা স্ট্রিং) | 🆕 **V217-এ আমার নিজের তৈরি নতুন বাগ** |
| PaymentActivity.kt | Refund বোতাম/টাইটেল/সাফল্য-বার্তা (৩টা স্ট্রিং) | ⚠️ **V216 থেকেই ছিল (§13) — আমি তৈরি করিনি, কিন্তু এই ফাইল ছুঁয়েছি বলে এখন ধরা পড়ল ও ঠিক করা হলো** |
| PaymentModel.kt | Refund-এর label "Refund / টাকা ফেরত" | ⚠️ **V216 থেকেই ছিল (§13), একই কারণে ঠিক করা হলো** |

**ফিক্স:** সবগুলো স্ট্রিং ইংরেজিতে বদলানো হয়েছে (নতুন কোনো Bengali-to-English ম্যাপ যোগ না করে, সরাসরি ইংরেজি লেখা হয়েছে — প্রজেক্টের সাধারণ নিয়ম "সব UI text English-only" মেনে)।

**Test:** `python3 00_GUARD/tk_guard.py .` (মেশিন-মোড) ও `python3 00_GUARD/tk_guard.py --release .` — দুটোই এখন **✅ সব যাচাই পাশ** দেখাচ্ছে, একটাও ❌ নেই।

**⚠️ সৎ স্বীকারোক্তি:** এই Guard টুল থাকা সত্ত্বেও আগে থেকেই ছিল (আগের সেশনের নোটে উল্লেখ আছে), কিন্তু এই সেশনের শুরুতে আমি নিজে থেকে এটা না চালিয়ে শুধু নিজের ছোট bracket-check স্ক্রিপ্ট চালিয়েছিলাম — যেটা এই বাংলা-নিয়ম ধরতে পারে না। TK কড়াভাবে "আবার সম্পূর্ণ যাচাই করুন" বলার পরে প্রজেক্টের নিজস্ব টুল খুঁজে বের করে চালানো হয়েছে, তখনই ধরা পড়েছে। এটা প্রথমেই করা উচিত ছিল।

### 🔴🔴🔴 ষষ্ঠ বাগ — চতুর্থ দফা অডিটে ধরা পড়েছে (সবচেয়ে সূক্ষ্ম)

**সমস্যা:** `saveRefund`-এর নতুন "pending refund-ও যোগ করে ধরা" পাহারা (§B216, item 2-এর অংশ) `pendingRefundSumForPatient(patient.patientId)` ডেকেছিল — কিন্তু `payments` টেবিলের `"patientId"` কলামে আসলে `patient.id` (DB-এর নিজের row-id) লেখা হয় (`buildRefundRow()` দেখুন), `patient.patientId` (মানুষ-পড়া কোড, যেমন KNE-31072026-001) নয় — সেটা যায় আলাদা `"patientCode"` কলামে। ফলে এই ফাংশন **কখনো কোনো সারি খুঁজেই পেত না**, চুপচাপ সবসময় ০ ফেরত দিত — অর্থাৎ "একই টাকায় দুটো আলাদা pending refund request" আটকানোর পাহারাটা বাস্তবে কাজই করত না (মূল `amount > patient.paid` চেকটা তখনও ঠিকই কাজ করত, তাই একক বড় over-refund তখনও আটকাত — শুধু "দুটো ছোট request একসাথে জমার বেশি" এই বিশেষ ফাঁকটা খোলা ছিল)।

**ফিক্স:** `pendingRefundSumForPatient(patient.id)` — সঠিক row-id দিয়ে ডাকা হচ্ছে এখন। ফাংশনের docstring-এও এই id/patientId নামের ফাঁদ স্পষ্ট করে লেখা হয়েছে, যাতে ভবিষ্যতে কেউ আবার একই ভুল না করে।

**ফাইল:** PaymentRepository.kt। **Test:** brace-balance ✅, `tk_guard.py` ✅।

### 📋 সপ্তম আবিষ্কার — লক করা নিয়মের (B98/B111/B112) সাথে সম্পর্ক

`00_GUARD/tk_guard.py`-এর নিজস্ব `LOCKED_RULES` তালিকায় B98/B111/B112 (29.07.2026-এ TK-এর লক করা নিয়ম — "ডিলিট শুধু Master") আছে। এই সেশনের B240 (Staff Same-Day Reject/Delete) **এই লক করা নিয়মের একটা আংশিক, TK-অনুমোদিত ব্যতিক্রম** — TK নিজেই স্পষ্ট বলেছেন "এই নিয়ম B98-এর সাধারণ Delete Rule-এর পরিবর্তে শুধু Same-Day বিশেষ নিয়ম হিসেবে কাজ করবে"। যাতে ভবিষ্যতে কোনো সেশন বিভ্রান্ত না হয় (বা ভুলে B240 বাতিল করে পুরনো B98-এ ফিরিয়ে না দেয়), **guard tool-এর নিজের `LOCKED_RULES` তালিকায় B240 নতুন এন্ট্রি হিসেবে যোগ করা হলো** (তারিখ-সময় সহ, B98-এর এন্ট্রি মোছা/দুর্বল না করে) — এখন থেকে B98 আর B240 একসাথে পড়তে হবে। **ফাইল:** `00_GUARD/tk_guard.py`।

**যা আরও যাচাই করা হয়েছে (কোনো সমস্যা পাওয়া যায়নি):**
- Refund dialog-এ যে `patient.paid` দিয়ে over-refund আটকানো হয় — সেটা `findPatientByMobile()`-এর refund-aware (approved refund বিয়োগ করা) হিসাব থেকেই আসে, নিশ্চিত হওয়া গেছে।
- Same-Day Delete/Reject-এর নতুন `DeletePermission.canDeleteEntryNow` কল — Payment/Chamber-এ আগে থেকেই ব্যবহৃত হওয়া একই ফাংশন, নতুন করে কোনো ভিন্ন লজিক লেখা হয়নি।
- DoctorQueue/Follow-up স্ক্রল-restore কোড — `layoutManager` null/অন্য টাইপ হলেও নিরাপদে কিছু না করেই এড়িয়ে যায় (`as?` safe cast), ক্র্যাশের ঝুঁকি নেই।

**এখনো device/live test দিয়ে যাচাই করা হয়নি (Pending, TK-এর কাজ):** উপরের সবগুলো fix-ই Android Studio build ও ফোনে test করে দেখতে হবে — এই environment-এ সেটা সম্ভব নয়, আগেও বলা হয়েছে।


- ৯টা পরিবর্তিত/নতুন Kotlin ফাইল bracket/brace-balance — সব ✅ PASS।
- `03_NETLIFY_READY/app.js` ও `assets/www/app.js` — দুই ফাইলেই `node --check` ✅ PASS।
- নতুন কোনো জায়গায় `kotlinx.coroutines.async/launch` fully-qualified প্যাটার্ন (V209 RED ALERT বাগ) নেই — সব জায়গায় আগে থেকে থাকা `import` দিয়ে unqualified।
- versionCode/versionName 216→217, web cache-buster v216→v217 (index.html)।
- ROLLBACK_V216/ (এই ভার্সনের ঠিক আগের কপি) ও ROLLBACK_V215/ রাখা হয়েছে; ROLLBACK_V214/ সরানো হয়েছে (শুধু শেষ দুটো রাখার নিয়ম অনুযায়ী)।
- কোনো Design/Layout/Color/Button/Workflow বদলানো হয়নি — শুধু বাগ-ফিক্স ও অনুমতি-নিয়ম।
