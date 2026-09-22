# 🔒 LOCK NOTE — V191 (৩০.০৭.২০২৬ রাত) · খাতার সারি **B175 · B176**

> **এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন TK-কে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না।**

**ভার্সন:** `versionCode 191` · `versionName 1.91` · পর্দায় **V191**
⛔ **কোনো SQL লাগবে না।**

---

## TK কী রিপোর্ট করেছেন (দুটো, ছবি-প্রুফসহ)

১. Prescription-এ "Patient ID: pat_9711468691" দেখাচ্ছে — Timeline-এ যেটা "KNE-30072026-001" হিসেবে দেখায়। *"Patient ID তো প্রজেক্টের শুরু থেকেই ফাইনাল ছিল, তাহলে প্রেসক্রিপশনে নেই কেন? সিস্টেমেই বা নেই কেন? এরকম সমস্যা আর কোথায় কোথায় আছে খুঁজে দেখুন।"*
২. Medicine Picker-এ লিস্ট থেকে মেডিসিন বেছে তারপর "+ Add" দিয়ে আলাদা মেডিসিন টাইপ করলে, প্রেসক্রিপশনে শুধু টাইপ-করাটাই থাকে — লিস্টের বাছাই হারিয়ে যায়।

---

## B175 — Patient ID

### আসল কারণ (কোড ধরে, আন্দাজ নয়)

`RoleSession.currentPatientId` একটাই ঘর **দুটো ভিন্ন কাজে** ব্যবহার হত:
১. `medical` টেবিলে প্রেসক্রিপশন সেভ/খোঁজার **আসল চাবি** (রোগীর সারির raw আইডি, যেমন "pat_9711468691")
২. ছাপার কাগজে দেখানো **মানুষ-পড়া-যায়** আইডি ("KNE-30072026-001")

বেশিরভাগ জায়গায় raw আইডিটাই এই ঘরে বসত (এটাই সঠিক, কারণ `medical` টেবিলের লিংক এই আইডি ধরেই কাজ করে) — তাই ছাপাতেও সেটাই দেখাত।

### সমাধান

নতুন আলাদা ঘর `RoleSession.currentPatientDisplayId` + `displayId()` ফাংশন — **শুধু দেখানোর জন্য**।

⛔ **`currentPatientId` (raw আইডি) এক অক্ষরও বদলানো হয়নি** — এটাই সবচেয়ে জরুরি ছিল, এটা ভাঙলে Prescription/Medicine Slip/Blood Test/Diet Chart সবকিছুর `medical` টেবিলের লিংক ভেঙে যেত এবং প্রেসক্রিপশন হারিয়ে যেত।

### কোথায় কোথায় ঠিক করা হলো

**SET-এর দিক (৬টা জায়গা — `RoleSession.applyFrom()`-এ `patientDisplayId` পাঠানো, যেখানে মানুষ-পড়া-যায় কোড এমনিতেই লোড করা ছিল, বাড়তি ক্লাউড-কল ছাড়াই):**
- `PatientTimelineActivity` (Take Action মেনু) — `currentPatientCode`
- `FollowUpActivity` (তিনটে জায়গা — Prescription/Diet Chart tag, Blood Test, Take Action) — `item.patientId`
- `ChamberAttendanceActivity` (Clinical মেনু) — `row.patientId`
- `GlobalSearchActivity` — `hit.patientId`
- `DoctorQueueActivity` → `ClinicalModulesActivity` — নতুন `EXTRA_PATIENT_DISPLAY_ID` Intent extra দিয়ে

**READ-এর দিক (৮টা জায়গা — `RoleSession.currentPatientId` সরাসরি পড়ার বদলে `RoleSession.displayId()`):**
- `DietChartActivity` (হেডার সাবটাইটেল + শেয়ার-টেক্সট)
- `ClinicalModulesActivity` (হেডার লাইন)
- `InvestigationAdviceActivity` (হেডার সাবটাইটেল)
- `MedicinePickerDialog` (**TK যে লাইনটা ছবিতে দাগ দিয়ে দেখিয়েছেন** — "pat_9711468691 · Piles")
- `MedicineSlipActivity` (হেডার + শেয়ার-টেক্সট)
- `PrescriptionActivity` (শেয়ার-টেক্সট)
- `DoctorCheckupActivity` (`bindPatientHeader()`-এর "Reg ID:" লাইন — নিচের ক্লাউড-খোঁজাও একই ভ্যারিয়েবল ব্যবহার করে, যেটা আগে থেকেই মানুষ-পড়া-যায় কোড প্রথমে খোঁজে তারপর raw আইডি খোঁজে, তাই এই বদলে খোঁজাটা বরং আরও সঠিক হলো)

**অক্ষত রাখা হয়েছে (সেভ/খোঁজার আসল চাবি, বদলালে প্রেসক্রিপশন হারিয়ে যেত):**
- `saveMedical()`-এর সব ডাক (PrescriptionActivity, DoctorCheckupActivity, DietChartActivity, InvestigationAdviceActivity)
- `PatientClinicalHistoryActivity`-এর ক্যাশ-চাবি ও ক্লাউড-খোঁজা

### 📌 যা এখনো বাকি (TK-কে জানানো, অনুমতি ছাড়া করা হয়নি)

আরও তিনটে জায়গায় (`ChamberAttendanceActivity`, `GlobalSearchActivity`, `DoctorQueueActivity`) address/age/sex এখনো লোড হয় না — এই তথ্য ঠিক করতে **নতুন একটা ক্লাউড-কল** লাগবে প্রতিটাতে (এটা সারি B174-এর সেই একই বাকি অংশ, নতুন কিছু নয়)।

---

## B176 — লিস্ট + কাস্টম মেডিসিন কম্বাইন

### আসল কারণ (কোড ধরে, আন্দাজ নয়)

একই পর্দায় **দুই রকম "কমিট" নিয়ম** চলত:
- তালিকার চেকবক্স-বাছাই `currentPrescription`-এ লেখাই হত **না**, যতক্ষণ না এই পর্দার নিজের **"Save"** বোতাম চাপা হত।
- কিন্তু **"+ Add"** (বাইরের ওষুধ টাইপ করে যোগ করা) নিজের মতো **সঙ্গে সঙ্গেই** লিখে ফেলত।

তাই স্টাফ বক্স টিক দিয়ে "+ Add"-এ গিয়ে একটা ওষুধ টাইপ করে "Add" চাপলে (এটাই সম্পূর্ণ কাজ মনে হয়), চেকবক্সের বাছাই তখনও অপেক্ষমাণ থেকে যেত — এই পর্দার নিজের "Save" আলাদা করে না চাপলে সেগুলো কখনো লেখাই হত না।

### সমাধান

নতুন `commitSelectedToList()` — "+ Add" চাপার **সঙ্গে সঙ্গেই**, বাইরের ওষুধ যোগ করার ছোট পর্দা খোলার **আগে**, তখন পর্যন্ত টিক দেওয়া সবকিছু আগে থেকেই লিখে ফেলা হচ্ছে। তাই স্টাফ এরপর যা-ই করুন, তালিকার বাছাই আর হারায় না।

⛔ Type (Tab/Cap/Syp/...) কোনো ওষুধে না বসানো থাকলে আগের মতোই আটকানো হয় (নীরবে কিছু বসানো হয় না)।
⛔ একই নাম দুবার সারি হয়ে যাওয়ার পুরনো পাহারা (খাতার সারি B22) অক্ষত — বারবার commit চললেও পুরনো সারিই update হয়, নতুন সারি তৈরি হয় না।

### একই ফাইল, তিন জায়গায় সমাধান

`MedicinePickerDialog.showPicker()` — **Prescription, Medicine Slip, Print Center-এর Walk-in** — তিন জায়গাতেই শেয়ার করা একটাই ফাইল। তাই এই একটা ফাইল ঠিক করায় তিন জায়গাতেই সমাধান হয়ে গেছে, আলাদা করে ছুঁতে হয়নি।

---

## যাচাই (কাজের পরে আবার, TK-এর নিয়ম)

- নিজের হাতে ব্র্যাকেট গোনা — **সব ফাইলেই পাশ**
- প্রতিটা নতুন লাইনের API/টাইপ হাতে মিলিয়ে দেখা — **পাশ**
- পাহারাদার `tk_guard.py` — **১৭টা যাচাই, সব পাশ**
- আগের অনুমোদিত কাজের যাচাই — **৫৯/৫৯ পাশ**
- ভার্সন চার জায়গায় এক — ZIP · `versionCode 191` · `versionName 1.91` · পর্দায় **V191**

**ফাইল:** `RoleSession.kt` · `PrintMappers.kt` · `PatientTimelineActivity.kt` · `FollowUpActivity.kt` · `ChamberAttendanceActivity.kt` · `GlobalSearchActivity.kt` · `ClinicalModulesActivity.kt` · `DoctorQueueActivity.kt` · `DietChartActivity.kt` · `InvestigationAdviceActivity.kt` · `MedicinePickerDialog.kt` · `MedicineSlipActivity.kt` · `PrescriptionActivity.kt` · `DoctorCheckupActivity.kt` · `build.gradle.kts` · `DashboardActivity.kt` · `app.js` (শুধু ভার্সনের লেখা)

---

## 🔴 যা এখনো বাকি

- TK-এর লাইভ টেস্ট — NOOR ALAM-এর Prescription আবার দেখতে হবে (Patient ID + মেডিসিন কম্বাইন দুটোই)।
- **সিদ্ধান্ত দরকার:** Chamber/Global Search/Doctor Queue-তে address/age/sex ফিক্সের জন্য নতুন ক্লাউড-কল চাই কিনা।
- **B148 — RLS** (⛔ TK-এর অনুমতি ছাড়া নিষেধ) · `03_NETLIFY_READY` Netlify-তে আপলোড (V184) · সপ্তাহখানেক পরে Supabase-এর খরচ দেখা।
