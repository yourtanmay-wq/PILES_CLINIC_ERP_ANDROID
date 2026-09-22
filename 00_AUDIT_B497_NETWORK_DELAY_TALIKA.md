# 🔴🔴 B497 — TK-নির্দেশ (06.08.2026): "কার মোবাইলে যেন বিন্দুমাত্র সময়
দেরি না হয়" — পুরো প্রজেক্টের সম্পূর্ণ তালিকা, কাজ শুরুর আগে

> TK-এর নির্দেশ হুবহু: "যে স্টাফ এনকোয়ারি করবে, যে স্টাফ ভিজিট নেবে,
> যেকোনো কাজই করুক না কেন সম্পূর্ণ প্রজেক্টে — কার মোবাইলে যেন বিন্দুমাত্র
> সময় দেরি না হয়, কঠোরভাবে নির্দেশ। পরবর্তীতে নেট দুর্বল বলে দোহাই দেওয়া
> যাবে না।"

**পদ্ধতি (আন্দাজ না করে):** পুরো `app/src/main/java/` জুড়ে যত জায়গায়
সরাসরি ক্লাউড (Supabase) থেকে ডেটা এনে **সাথে সাথে স্ক্রিনে দেখানো হয়**
সেগুলো `grep` দিয়ে খুঁজে, প্রতিটার আশেপাশের কোড পড়ে যাচাই করা হয়েছে —
কোনটা ইতিমধ্যে Cache-First (নিরাপদ) আর কোনটা এখনো সরাসরি নেটওয়ার্ক-নির্ভর
(ঝুঁকিপূর্ণ) তা আলাদা করা হয়েছে।

---

## ✅ ইতিমধ্যে Cache-First (নিরাপদ, হাত দেওয়ার দরকার নেই)

এই ফাইলগুলো ইতিমধ্যে `LocalWorkflowStore` / `CloudReadCache` /
`BackgroundWork` ব্যবহার করে — পাতা খোলার সাথে সাথেই শেষ-জানা তথ্য দেখায়,
পেছনে চুপচাপ ক্লাউড থেকে হালনাগাদ করে:

- `FollowUpActivity.kt` / `FollowUpRepository.kt`
- `EnquiryRepository.kt`
- `RegistrationRepository.kt`
- `PatientTimelineRepository.kt`
- `PaymentRepository.kt`
- `DraftRepository.kt`
- `ChamberAttendanceActivity.kt` / `ChamberAttendanceRepository.kt`
- `DoctorVisitActivity.kt` / `DoctorVisitRepository.kt`
- `DoctorQueueRepository.kt`
- `GlobalSearchActivity.kt`
- `DialerRepository.kt`
- `PatientMessage.kt`
- `ClinicalCloudRepository.kt` (Doctor Note-এর ৩টা ক্লিনিক্যাল স্ক্রিন)
- `BriefingRepository.kt`

---

## 🔴 সরাসরি নেটওয়ার্ক-নির্ভর — এখনো ঝুঁকিপূর্ণ (এক এক করে ঠিক করতে হবে)

### 🟢 ১. `WorkNotebookActivity.kt` — **আজ B496-এ আংশিক ঠিক হয়েছে**
- ✅ IN TIME/OUT TIME (`loadDay()`) — ঠিক হয়েছে (getRowsChecked)
- ✅ New Enquiry/Registration (auto) সংখ্যা (`fetchStats()`-এর enq/reg অংশ) — ঠিক হয়েছে
- 🔴 **এখনো বাকি এই একই ফাইলে:**
  - `App Calls (auto)` — `call_taps` টেবিল থেকে সরাসরি `getRows()` (ব্যর্থ হলে "০")
  - `Total call (auto)` — উপরেরটার উপর নির্ভরশীল
  - Monthly/মাসিক রিপোর্টের `outside_calls_manual` যোগফল, `leaveDays` (ছুটির দিন গোনা) — সরাসরি `getRows()`
  - Collection (টাকা জমার যোগফল, `sumPublic()`) — একই ধরনের সমস্যা থাকতে পারে (যাচাই বাকি)

### 🔴 ২. `IncomeExpenseActivity.kt` — Master-এর টাকার হিসাব (গুরুত্বপূর্ণ, টাকা-সম্পর্কিত)
- দিনের সারাংশ (`showDaySummary()`) — Cash/Online/Total Collection, Expense — সরাসরি `ModuleAuth.getRows()`
- মাসিক শীট (মাসের সব এন্ট্রি তালিকা)
- নির্দিষ্ট দিনের বিস্তারিত পাতা
- ব্রাঞ্চ-ভিত্তিক/তারিখ-রেঞ্জ সারাংশ
- ⚠️ **সবচেয়ে বেশি ঝুঁকি** — ব্যর্থ হলে "₹0" দেখাবে, যেটা "আজ কোনো টাকা আসেনি" বলে ভুল বোঝাতে পারে Master-কে

### 🔴 ৩. `StaffProfileActivity.kt` — স্টাফের বেতন/প্রোফাইল তথ্য
- স্টাফ তালিকা, বেতনের সেটিং (`salary_config`)
- বেতন পেমেন্টের ইতিহাস (`salary_payments`)
- প্রোফাইল লোড (একটা জায়গায় ইতিমধ্যে `getRowsChecked()` ব্যবহার হচ্ছে — বাকিগুলো এখনো পুরনো `getRows()`)

---

## ⚪ এই তালিকার বাইরে (স্বাভাবিক, ঠিক করার দরকার নেই)

- **লেখা/সেভ করার (write) অপারেশন** — এগুলোতে "দেরি" মানে ভিন্ন জিনিস
  (সেভ ব্যর্থ হলে টাকা হারানোর ঝুঁকি, যেটা আলাদা, ইতিমধ্যে Retry-Queue
  (`CloudWriteQueue`) দিয়ে সুরক্ষিত) — এই তালিকা শুধু **দেখানোর (read)**
  দেরি নিয়ে।
- `DeletePermission.kt`-এর `hasOutTimeToday()` — নেটওয়ার্ক ব্যর্থ হলে
  ইচ্ছাকৃতভাবে "অনুমতি লাগবে" ধরে নেয় (নিরাপদ দিক, টাকার সুরক্ষা) — এটা
  বাগ না, স্বাভাবিক সিদ্ধান্ত, ছোঁয়া হবে না।

---

## 📋 করণীয় ক্রম (TK-এর অনুমোদনের অপেক্ষায়)

1. `WorkNotebookActivity.kt`-এর বাকি অংশ (App Calls/Total call/Monthly/Collection)
2. `IncomeExpenseActivity.kt` (টাকার হিসাব — সবচেয়ে গুরুত্বপূর্ণ)
3. `StaffProfileActivity.kt`

**পদ্ধতি প্রতিটাতে একই থাকবে:** Cache-First — পাতা খোলার সাথে সাথেই
ফোনে শেষ-জানা তথ্য (যদি আগে একবার লোড হয়ে থাকে) সাথে সাথে দেখাবে, পেছনে
নিঃশব্দে ক্লাউড থেকে হালনাগাদ যাচাই হবে, বদলালে আপডেট হবে — কখনো
ফাঁকা/জিরো/লোডিং দেখাবে না ব্যর্থতার কারণে।

---

## 🔍 আরও গভীর সম্পূর্ণ-প্রজেক্ট যাচাই (06.08.2026, TK-এর দ্বিতীয় নির্দেশ —
"শুধু এই কটা উদাহরণ না, সম্পূর্ণ প্রজেক্ট যাচাই করুন")

**পদ্ধতি:** `ModuleAuth.getRows/getRowsChecked/countPublic/sumPublic` —
এই চারটে সরাসরি-ক্লাউড-পড়ার ফাংশন সম্পূর্ণ `app/src/main/java/` জুড়ে
আবার নতুন করে খোঁজা হয়েছে (শুধু WorkNotebookActivity-কেন্দ্রিক না)।

**ফলাফল — এই ৪টা ফাংশন সরাসরি ব্যবহার করে এমন ফাইল মোট ৪টাই:**
`WorkNotebookActivity.kt` · `IncomeExpenseActivity.kt` ·
`StaffProfileActivity.kt` · `DeletePermission.kt` (এটা read-only অনুমতি-
চেক, স্বাভাবিকভাবেই নিরাপদ দিক ধরে, ঠিক করার দরকার নেই — উপরে ব্যাখ্যা
করা আছে)। **নতুন কোনো ফাইল পাওয়া যায়নি** — মানে ঝুঁকিপূর্ণ জায়গা এই
৩টাতেই সীমাবদ্ধ (WorkNotebook/IncomeExpense/StaffProfile)।

**অতিরিক্ত যাচাই — Dashboard-এর "📞 X calls pending today" ব্যানার:**
এটা ইতিমধ্যে নিরাপদ `FollowUpRepository.fetchTab()` (Cache-First)
ব্যবহার করে, কিন্তু তার উপরে Dashboard নিজে একটা বাড়তি
`catch(Exception){0}` বসিয়ে রেখেছে — অর্থাৎ কোনো একটা কারণে ব্যর্থ হলে
ব্যানারটাই দেখাবে না (সংখ্যা ভুল দেখাবে না, শুধু লুকিয়ে যাবে)। এটা
টাকা/গণনার ভুল না, শুধু একটা সহায়ক ব্যানার সাময়িক না-দেখানো — অগ্রাধিকার
কম, তবু তালিকায় নোট করা হলো।

**উপসংহার:** ৩টা ফাইলের বাইরে প্রজেক্টে এই ধরনের ঝুঁকি আর পাওয়া যায়নি
(সরাসরি ক্লাউড-পড়া ফাংশনের হুবহু ব্যবহার-জায়গা ধরে)। Dashboard-এর
ব্যানারটা বাড়তি (৪ নম্বর, কম অগ্রাধিকার) হিসেবে যোগ করা হলো।

## 📋 চূড়ান্ত করণীয় ক্রম
1. `WorkNotebookActivity.kt` (বাকি অংশ)
2. `IncomeExpenseActivity.kt` (টাকার হিসাব — সবচেয়ে গুরুত্বপূর্ণ)
3. `StaffProfileActivity.kt`
4. `DashboardActivity.kt`-এর কল-ব্যানার (কম অগ্রাধিকার, সময় থাকলে)

---

## 🔴🔴 তৃতীয়, সবচেয়ে গুরুত্বপূর্ণ যাচাই (06.08.2026, TK-এর তৃতীয়বার
স্পষ্ট করে বলার পরে — "যে কাজ করবে তার ফোনে সাথে সাথে, যাদের দেখা দরকার
তাদের ফোনেও দ্রুত, সব ব্রাঞ্চেই, শুধু Enquiry না — সম্পূর্ণ প্রজেক্ট")

**আসল, গভীর কারণ পাওয়া গেছে (আগের "নিরাপদ ১৪টা ফাইল"-এর দাবি ভুল
ছিল, সংশোধন করা হলো):**

`FollowUpRepository.fetchTab()` — Enquiry/Visit/Patient তিনটে তালিকা,
Dashboard-এর কল-গণনা, আরও ১২টা ফাইলের ভিত্তি — এই ফাংশনটা **সরাসরি,
থেমে থেকে (`runBlocking`) ক্লাউডের উত্তরের অপেক্ষা করে**, তারপর ফলাফল
দেয়। ফোনের নিজের জমানো তথ্য (`LocalWorkflowStore`) থাকলেও প্রথমে
দেখানো হয় না — এটাই দেরির আসল কারণ। এই একই ফাংশন ব্যবহার করে এমন
ফাইল ১৪টা:

`FollowUpActivity.kt` · `DashboardActivity.kt` · `AppointmentActivity.kt`
· `ChamberAttendanceActivity.kt` · `DialerRepository.kt` ·
`FollowCalendarActivity.kt` · `PatientTimelineActivity.kt` ·
`PaymentRepository.kt` · `BottomNav.kt` · `PendingSyncStatus.kt` ·
`SyncWorker.kt` · `BackgroundRefreshWorker.kt` · `CallReminderWorker.kt`

**সমাধানের পথ (প্রজেক্টে আগে থেকেই থাকা ফাংশন ব্যবহার করে, নতুন কিছু
আবিষ্কার না করে — ঝুঁকি কম রাখতে):** `LocalWorkflowStore.rowsForStage()`
— এটা ফোনের জমানো তথ্য **সাথে সাথে, নেটওয়ার্ক ছাড়াই** পড়ে। প্রতিটা
স্ক্রিনে এখন থেকে: (১) পাতা খোলার সাথে সাথেই এই ফাংশন দিয়ে যা জমানো
আছে তাই দেখানো হবে (শূন্য অপেক্ষা), (২) তার ঠিক পরপরই পেছনে
`fetchTab()` (ক্লাউড) ডাকা হবে, উত্তর এলে তালিকা নিঃশব্দে হালনাগাদ
হবে (অন্য ব্রাঞ্চ/অন্য স্টাফের নতুন কাজও যোগ হবে)। **সব ব্রাঞ্চেই
সমানভাবে** — কোনো ব্রাঞ্চ আলাদা নিয়মে চলবে না।

**ঝুঁকি:** `fetchTab()`-এর ভিতরের লজিক (কোন সারি কার কাছে দৃশ্যমান,
টাকার হিসাব, ব্রাঞ্চ-ফিল্টার) **একটুও বদলানো হবে না** — শুধু প্রতিটা
ক্যালিং-স্ক্রিনে "আগে local, পরে cloud" এই ক্রমটা যোগ হবে। Supabase
ফ্রি-প্ল্যান অনুরোধ-সংখ্যা বাড়বে না (একই fetchTab() কল, শুধু সময়
এগিয়ে আনা)।

**করণীয় ক্রম (একটা একটা করে, প্রতিটার পরে TK-কে ফলাফল দেখানো হবে):**
1. `FollowUpActivity.kt` — Enquiry/Visit/Patient তিনটে তালিকার মূল পাতা (সর্বোচ্চ অগ্রাধিকার)
2. `DashboardActivity.kt` — কল-ব্যানার
3. `PatientTimelineActivity.kt`
4. `ChamberAttendanceActivity.kt`
5. `AppointmentActivity.kt` / `FollowCalendarActivity.kt`
6. `DialerRepository.kt` (Contacts ট্যাব)
7. `PaymentRepository.kt`
8. বাকি সহায়ক ফাইল (`BottomNav`, Worker-গুলো — এগুলো ব্যাকগ্রাউন্ড/নোটিফিকেশন, সরাসরি স্ক্রিনে দেখানো না, অগ্রাধিকার সবার নিচে)

---

## ✏️ সংশোধন (06.08.2026, একই দিনে, কাজ শুরু করতে গিয়ে ধরা পড়েছে)

উপরের তালিকায় ভুল ছিল — **প্রতিটা ফাইল হাতে খুলে সত্যিই যাচাই করার পরে**
দেখা গেছে কিছু ফাইলে ইতিমধ্যে সঠিক প্যাটার্ন (আগে local instant, পরে
cloud) আছে, নিচের র‍্যাংক-করা তালিকাই এখন সঠিক, চূড়ান্ত:

**✅ ইতিমধ্যে সঠিক (হাতে-কোড দেখে নিশ্চিত হওয়া, আর ছোঁয়া হবে না):**
- `FollowUpActivity.kt` — `repository.loadCachedTab()` দিয়ে সাথে সাথে দেখায়
- `ChamberAttendanceActivity.kt` — `ChamberAttendanceRepository.loadCachedBoard()`
- `PaymentRepository.kt` — নিজস্ব local-cache ব্যবস্থা আগে থেকেই আছে
- `FollowCalendarActivity.kt` — একই প্যাটার্ন আছে

**🔴 সত্যিই এখনো বাকি (হাতে দেখে নিশ্চিত):**
1. `DashboardActivity.kt` — কল-ব্যানার সরাসরি `fetchTab()`, local-cache নেই
2. `PatientTimelineActivity.kt` — সরাসরি নেটওয়ার্ক, local-cache নেই
3. `AppointmentActivity.kt` — সরাসরি নেটওয়ার্ক, local-cache নেই
4. `DialerRepository.kt` (Contacts ট্যাব, `fetchContacts()`) — local-cache নেই

**+ আগের তালিকার ৩টা:** Work Notebook (বাকি অংশ) · Income-Expense ·
Staff Profile।

এখন এই **৭টা** নিয়েই এক এক করে কাজ শুরু হচ্ছে — DashboardActivity.kt
দিয়ে প্রথমে।


