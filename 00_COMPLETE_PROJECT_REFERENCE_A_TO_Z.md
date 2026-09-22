# PILES CLINIC APP — সম্পূর্ণ প্রজেক্ট রেফারেন্স (A থেকে Z)

**এই ফাইলটা স্থায়ী, প্রতিটা ভবিষ্যৎ ডেলিভারিতে থাকবে ও নতুন কাজ হলে
আপডেট হবে। ফাইল বড় হলেও অসুবিধা নেই — সম্পূর্ণতাই এখানে সবচেয়ে
জরুরি।**

সর্বশেষ হালনাগাদ: 2026-07-16, V87।

---

# ০. সবচেয়ে জরুরি — TK-এর অনুমতি ছাড়া কিছুই বদলানো যাবে না

**এই ফাইল সহ পুরো প্রজেক্ট পড়ার আগে, প্রতিটা AI/ডেভেলপারকে এই
নিয়মগুলো মানতে হবে (TK-এর সরাসরি, বারবার দেওয়া নির্দেশ):**

1. TK-এর স্পষ্ট অনুমতি ছাড়া **কোনো কোড, ডিজাইন, ফিচার, ওয়ার্কফ্লো
   বদলানো/মোছা যাবে না**
2. শুধুমাত্র **প্রকৃত বাগ বা Dead Button** থাকলে সেটা অনুমতি ছাড়াও
   ঠিক করা যাবে — কিন্তু "উন্নতি"/"সুন্দর করা" এই যুক্তিতে কিছু
   বদলানো যাবে **না**
3. কোনো কাজ করতে গিয়ে কোথাও ঝুঁকি থাকলে, কাজ শুরুর **আগেই** TK-কে
   জানাতে হবে — TK রাজি হলে তবেই কাজ করতে হবে, নাহলে না
4. TK যা করতে বলবেন ঠিক **শুধু ততটুকুই** করতে হবে — অতিরিক্ত/
   অননুমোদিত কিছু বদলানো যাবে না
5. প্রতিটা ডেলিভারির আগে **সম্পূর্ণ প্রজেক্ট diff করে যাচাই** করতে
   হবে যে শুধু ইচ্ছাকৃতভাবে বদলানো ফাইলগুলোই বদলেছে, আর কিছু না
6. `10_FUTURE_PLANS/` ফোল্ডারের নিয়ম: TK-এর কাছে ল্যাপটপ না থাকা
   পর্যন্ত নতুন যেকোনো ফিচার শুধু এই ফোল্ডারে কোড আকারে লেখা থাকবে,
   আসল অ্যাপে বসবে না, যতক্ষণ না TK নিজে এক এক করে বেছে বলেন
7. এই নিয়মগুলো আর বিস্তারিত ব্যাখ্যা `00_PROJECT_STATE_MASTER_NOTE.md`
   ফাইলের একদম শুরুর সেকশনেও (হুবহু) লেখা আছে — ওটাও পড়তে হবে

---

# ১. প্রজেক্টটা কী

**Piles Clinic ERP** — TK Biswas / MAA AYURVED PILES CLINIC-এর ৫টা
ব্রাঞ্চের (Kishanganj, Jalpaiguri, Cooch Behar, Falakata, Birpara)
জন্য বানানো সম্পূর্ণ ক্লিনিক ম্যানেজমেন্ট Android অ্যাপ। Enquiry থেকে
Registration, ডাক্তারের চেকআপ, প্রেসক্রিপশন, পেমেন্ট, ফলো-আপ, প্রিন্ট
— পুরো জার্নি একটা অ্যাপে।

**টেকনোলজি:** Kotlin (Android নেটিভ) + XML (লেআউট) + Supabase
(ক্লাউড ডাটাবেস, ফ্রি প্ল্যান)।

**ইতিহাস:** প্রথমে ওয়েব-অ্যাপ (Netlify-তে, `03_NETLIFY_READY`
ফোল্ডারে সংরক্ষিত) → স্লো লাগায় সম্পূর্ণ নেটিভ Android অ্যাপ হিসেবে
আবার বানানো হয়েছে (`02_ANDROID_SOURCE_CODE`)।

---

# ২. উপরের লেভেলের ফোল্ডার-কাঠামো

| ফোল্ডার | কী আছে |
|---|---|
| `00_READ_ME_FIRST` | Android Studio-তে খোলার গাইড |
| `01_MASTER_LOCK_BOOK_SOURCE_OF_TRUTH` | প্রজেক্টের "সংবিধান" — সবচেয়ে গুরুত্বপূর্ণ নিয়ম |
| `02_ANDROID_SOURCE_CODE` | **আসল অ্যাপের কোড** (ভাগ ৩-৭ দেখুন) |
| `03_NETLIFY_READY` | পুরনো ওয়েব-ভার্সন (ইতিহাস, ব্যবহার হয় না) |
| `04_SUPABASE_DATABASE_SETUP` | সার্ভারের টেবিল বানানোর SQL |
| `05_APK_AAB_BUILD_NOTES` | APK বানানোর নোট |
| `06_TEST_CHECKLISTS` | টেস্ট চেকলিস্ট |
| `07_RELEASE_NOTES_VERSION_HISTORY` | পুরনো ভার্সনের ইতিহাস |
| `08_ASSETS_BACKUP` | ছবি/আইকনের ব্যাকআপ |
| `09_ORIGINAL_UPLOADED_FILES` | একদম শুরুর মূল ফাইল |
| `10_FUTURE_PLANS` | **ল্যাপটপ না থাকা পর্যন্ত** নতুন ফিচারের কোড (এখনো আসল অ্যাপে বসেনি) |

**সবচেয়ে গুরুত্বপূর্ণ দুটো ফাইল, একদম উপরে:**
- `00_PROJECT_STATE_MASTER_NOTE.md` — প্রতিটা সেশনে কী কাজ হয়েছে
  তার ইতিহাস (লগ)
- এই ফাইল (`00_COMPLETE_PROJECT_REFERENCE_A_TO_Z.md`) — সম্পূর্ণ
  প্রজেক্টের গঠন/সূত্র/নিয়ম একসাথে

---

# ৩. আসল কোড — ৫টা প্যাকেজ

`02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/`

| প্যাকেজ | ফাইল সংখ্যা | কাজ |
|---|---|---|
| `native` | ~৮০টা | প্রধান অংশ — প্রায় সব স্ক্রিন |
| `clinical` | ১৬টা | ডাক্তারের চেকআপ, প্রেসক্রিপশন, ওষুধ |
| `print` | ৯টা | PDF/প্রিন্ট |
| `security` | ৭টা | সেটিংস, ব্যাকআপ, ক্র্যাশ-সুরক্ষা |
| `data` | ২৫টা | পুরনো "Phase 5" চেষ্টা — আংশিক ব্যবহৃত (ভাগ ৯ দেখুন) |

মোট এখন পর্যন্ত ~১৩৪টা Kotlin ফাইল, ~১৫৭টা XML লেআউট ফাইল।

---

# ৪. `native` প্যাকেজের প্রতিটা ফিচার

**লগইন/সেশন:** `LoginActivity.kt`, `NativeSession.kt`

**ড্যাশবোর্ড:** `DashboardActivity.kt`, `BottomNav.kt` (নিচের নেভ বার
— এখানেই V77-এর রিলায়েবিলিটি-রিট্রাই বসানো আছে)

**Enquiry:** `EnquiryActivity.kt`, `EnquiryModel.kt`,
`EnquiryRepository.kt`

**Registration:** `RegistrationActivity.kt`,
`RegistrationRepository.kt`

**Follow-up (Enquiry/Visit/Patient ৩ ট্যাব):** `FollowUpActivity.kt`,
`FollowUpAdapter.kt`, `FollowUpModel.kt`, `FollowUpRepository.kt`

**Payment:** `PaymentActivity.kt`, `PaymentModel.kt`,
`PaymentRepository.kt`, `CollectionAdapter.kt`, `PaymentRingView.kt`

**Chamber Attendance (দৈনিক হাজিরা+কালেকশন বোর্ড):**
`ChamberAttendanceActivity.kt`, `ChamberAttendanceAdapter.kt`,
`ChamberAttendanceRepository.kt`

**Doctor Visit/RMP:** `DoctorVisitActivity.kt`,
`DoctorVisitAdapter.kt`, `DoctorVisitModel.kt`,
`DoctorVisitRepository.kt`

**Doctor Queue:** `DoctorQueueActivity.kt`, `DoctorQueueAdapter.kt`,
`DoctorQueueModel.kt`, `DoctorQueueRepository.kt`

**সার্চ/ড্রাফট/ট্র্যাশ:** `GlobalSearchActivity.kt`,
`DraftActivity.kt`, `DraftListActivity.kt`, `DraftCardAdapter.kt`,
`DraftRepository.kt`, `TrashBinActivity.kt`, `TrashAdapter.kt`,
`TrashRepository.kt`

**রোগীর সম্পূর্ণ ইতিহাস (View All/Full Journey):**
`PatientTimelineActivity.kt`, `PatientTimelineRepository.kt`,
`TimelineAdapter.kt`

**রিপোর্ট (Master-only, ব্রাঞ্চ-ওয়াইজ তুলনা-সহ):**
`ReportsActivity.kt`, `ReportsRepository.kt`

**ছবি:** `PatientPhotoActivity.kt`, `PatientPhotoRepository.kt`,
`UserPhotoActivity.kt`, `UserPhotoStore.kt`, `PhotoUtils.kt`

**পাসওয়ার্ড/ব্যাকআপ/এক্সপোর্ট:** `PasswordCenterActivity.kt`,
`PasswordCenterAdapter.kt`, `PasswordCenterRepository.kt`,
`CloudBackup.kt`, `CloudPasswordCheck.kt`, `ExportDataActivity.kt`,
`CsvExportHelper.kt`

**অন্যান্য:** `AppointmentActivity.kt`, `BriefingActivity.kt` +
Adapter/Model/Repository, `MedicinePaymentActivity.kt`,
`FollowCalendarActivity.kt`, `DiseaseCatalog.kt`,
`DiseaseDetailActivity.kt`, `MoreMenuActivity.kt` (এখানেই ভার্সন
নম্বর দেখা যায়, V85 থেকে), `StaffDirectory.kt`,
`PublicSiteActivity.kt`, `CallReminderScheduler.kt`,
`CallReminderWorker.kt` (দৈনিক কল-রিমাইন্ডার নোটিফিকেশন, V86 থেকে
নাম-সহ)

**সাহায্যকারী ফাইল (সবার পেছনে কাজ করে):** `SupabaseClient.kt`
(**সবচেয়ে গুরুত্বপূর্ণ** — Supabase-এর সাথে একমাত্র যোগাযোগের
জায়গা), `LocalWorkflowStore.kt` (ফোনের অফলাইন ক্যাশ), `JsonExt.kt`,
`PatientModel.kt`, `PatientIdGenerator.kt`, `TripleTapEdit.kt`,
`SpinnerLock.kt`, `MobileInput.kt`, `FormStar.kt`

---

# ৫. `clinical` প্যাকেজ

`ClinicalModulesActivity.kt`, `DoctorCheckupActivity.kt` (৭-ধাপের
চেকআপ ফর্ম), `PrescriptionActivity.kt` + Adapter,
`MedicineSlipActivity.kt`, `MedicinePickerDialog.kt`,
`InvestigationAdviceActivity.kt`, `InvestigationCategoryActivity.kt`,
`InvestigationAdapter.kt`, `DietChartActivity.kt`,
`PatientClinicalHistoryActivity.kt`, `HistoryAdapter.kt`,
`ClinicalModels.kt`, `ClinicalRepository.kt`,
`ClinicalCloudRepository.kt`, `RoleSession.kt`

---

# ৬. `print` প্যাকেজ

- **`ClinicPdfBuilder.kt` — 🔒 OWNER LOCKED।** এই ফাইল বদলাতে
  প্রতিবার TK-এর সরাসরি লিখিত অনুমতি লাগবে।
- `PrintCenterActivity.kt`, `PrintPreviewActivity.kt`,
  `PdfPrintDocumentAdapter.kt`, `PrintDocumentModel.kt`,
  `PrintMappers.kt`, `PrintMappersCloud.kt`, `BranchInfo.kt`,
  `QrCodeGenerator.kt`

---

# ৭. `security` প্যাকেজ

`SettingsActivity.kt` (Backup/Restore/Auto-Sync — V87-এ Excel/CSV
ব্যাকআপ যোগ হয়েছে), `AppSettings.kt`, `BackupManager.kt`,
`CrashHandler.kt`, `SecurityGuard.kt`, `SessionTimeoutManager.kt`

---

# ৮. মূল ওয়ার্কফ্লো: Enquiry → Visit → Patient → Treatment

```
Enquiry (stage="Inquiry")
    │  Registration করলে:
    │   ├─ পুরনো Enquiry বন্ধ হয়ে যায় (closeSourceEnquiry)
    │   └─ নতুন stage="Patient" রেকর্ড তৈরি হয় (UI-তে "Visit" ট্যাব)
    ▼
Visit (stage="Patient", UI-তে "Visit")
    │  Advance Payment দিলে:
    │   └─ stage="Treatment"-এ প্রোমোট হয়
    ▼
Patient/Treatment (stage="Treatment", UI-তে "Patient")
    │  ডাক্তার দেখানো, প্রেসক্রিপশন, ওষুধ, টেস্ট, ডায়েট চার্ট...
    ▼
Follow-up (Next Follow-up Date অনুযায়ী চলতেই থাকে)
```

**মনে রাখার মতো:** কোডের ভেতরের নাম (stage="Patient"/"Treatment")
আর স্ক্রিনের লেখা (Visit/Patient) একটু উল্টো — পুরনো ওয়েব-অ্যাপ
থেকেই এমন, ইচ্ছাকৃতভাবে বদলানো হয়নি।

---

# ৯. `data` প্যাকেজ — আংশিক ব্যবহৃত পুরনো কোড (সংশোধিত তথ্য, V86/V87)

**আগে ভুলবশত বলা হয়েছিল এটা সম্পূর্ণ অব্যবহৃত — সেটা ভুল ছিল,
সংশোধন করা হলো:**

- এই প্যাকেজে একটা পুরনো "Phase 5" Room ডাটাবেস + WorkManager
  সিঙ্ক-ব্যবস্থা আছে
- `security/BackupManager.kt` (Menu → Backup-এর একটা অংশ) এখনো এটা
  ব্যবহার করে — **তাই এই প্যাকেজ মোছা যাবে না, মুছলে কম্পাইলই হবে না**
- **কিন্তু এই ব্যবস্থার ভুল আছে:** এটা যে টেবিলের নাম খোঁজে
  (`registrations`, `follow_ups`) তা আসল টেবিলের নামের
  (`patients`, `followups`) সাথে মেলে না — তাই Registration/Follow-up
  ডেটা এই ব্যবস্থায় কখনো সিঙ্ক হয় না। শুধু Enquiry/Payment (নাম
  মিলে যায় বলে) হয়তো ঠিকমতো সিঙ্ক হয়
- **আসল, নির্ভরযোগ্য Backup/Restore অন্য জায়গায়** —
  `SettingsActivity.kt`-এর নিজস্ব `exportCloudJson()`/
  `doCloudJsonRestore()` ফাংশন, যেটা সঠিক টেবিলের নাম দিয়ে সরাসরি
  `SupabaseClient` ব্যবহার করে। **"Backup Now" চাপলে এটাও স্বয়ংক্রিয়ভাবে
  চলে** — এটাই আসল, বিশ্বাসযোগ্য ব্যাকআপ (বিস্তারিত মাস্টার নোটের
  সেকশন ৪১-এ)

---

# ১০. সূত্র/হিসাব (Formula) — কোড পড়ে যাচাই করা

### Patient ID
সূত্র: **`[ব্রাঞ্চ-কোড]-[ddMMyyyy]-[৩-ডিজিট সিরিয়াল]`**
(যেমন `KNE-16072026-003`)। ব্রাঞ্চ-কোড: Kishanganj→KNE,
Jalpaiguri→JPE, Cooch Behar→COB, Falakata→FLK, Birpara→BIR।

### Bill / Paid / Due
সূত্র: **`Due = Bill - Paid`** (কখনো ঋণাত্মক হয় না, সর্বনিম্ন ০)।

### Total Bill Amount লক
সূত্র: **`billLocked = (Bill > 0)`** — একবার বসলে ৩-ট্যাপ ছাড়া
বদলানো যায় না।

### Registration Fee vs Advance Payment
সবসময় সম্পূর্ণ আলাদা — কখনো একসাথে যোগ/গোলানো হয় না।

### Chamber Attendance-এর সংখ্যা
- Expected = আজ Next Follow-up যাদের
- Arrived = আজ Enquiry/Registration/Payment (বা ম্যানুয়াল "Arrived"
  মার্ক) হয়েছে যাদের
- **No-show = Expected AND NOT Arrived**

### Enquiry কল-গণনা
প্রতিদিন সর্বোচ্চ ১ বার বাড়ে, **সর্বোচ্চ সীমা ৫**।

### Doctor Referral Income
Paid/Due সরাসরি লেখা হয় (স্বয়ংক্রিয় বিয়োগ নেই)। Referred Patients
সংখ্যা = "Referred By" নাম/মোবাইল মিলে যাওয়া রোগীর সংখ্যা।

### "Marked Arrived" (Chamber Attendance-এর Search ফিচার)
নতুন টেবিল না — "payments" টেবিলেই ₹0-এর বিশেষ সারি
(`payType="attendance_mark"`), কোনো টোটালে যোগ হয় না।

### Payment History-র গণনা
"X টি পেমেন্ট" গণনায় "Marked Arrived" ধরা হয় না (তালিকায় দেখা যায়,
গোনায় না)।

---

# ১১. কার্যপ্রবাহ (Working Flow)

### ডুপ্লিকেট (একই রোগী দুইবার) ধরার নিয়ম
মোবাইল নম্বর দিয়ে Enquiry আর Patient — দুই টেবিলেই আগে খোঁজা হয়।
পাওয়া গেলে নতুন এন্ট্রি না বানিয়ে পুরনোটাই "Update Existing"
(Enquiry-তে "restoreAndMove" — ব্রাঞ্চ বদলালেও নতুন এন্ট্রি হয় না)।

### ৩-ট্যাপ নিয়ম কোথায় কোথায়
Total Bill Amount এডিট, Advance/2nd Payment এডিট, Call Received
By/Branch (Enquiry ফর্মে), "Marked Arrived" ডিলিট (Payment History-তে
এডিট-ডায়ালগের ভেতরে), Chamber Attendance-এর Close (Treatment খালি
থাকলে), Search-এ ভুল "Arrived" আনডু করা।

### ব্রাঞ্চ-ভিত্তিক দেখার নিয়ম
Master সব ব্রাঞ্চ দেখেন + ফিল্টার করতে পারেন। Staff শুধু নিজের
ব্রাঞ্চ দেখেন, কোনো বাছাই অপশনই নেই।

### অফলাইন-প্রথম সেভ + স্বয়ংক্রিয় রিট্রাই (V77-V84-এ ঠিক করা)
সেভ চাপলে সাথে সাথে ফোনে সেভ হয় → ব্যাকগ্রাউন্ডে একবার Supabase-এ
পাঠানোর চেষ্টা → ব্যর্থ হলে queue-তে জমা → স্টাফ যেকোনো স্ক্রিন
খুললেই (`BottomNav.wire()` দিয়ে) আবার চেষ্টা, যতক্ষণ না পৌঁছায়।
Enquiry, Registration, Advance Payment, Follow-up-এর Remark/Status —
সব জায়গায় একই ব্যবস্থা।

### Call-ahead
ভবিষ্যতের তারিখ বাছলে সেদিনের Expected তালিকায় শুধু 📞 CALL বোতাম
(Payment/Treatment বোতাম না)।

### Search করে "Arrived" মার্ক করা
নাম/মোবাইল/Patient ID (কমপক্ষে ৩ অক্ষর) দিয়ে সার্ভার থেকেই ফিল্টার
করে খোঁজা → "✅ Arrived" এক-চাপে মার্ক → ভুল হলে "↩️ Undo" (৩-ট্যাপ)
সাথে সাথেই, অথবা পরে Payment History-তে গিয়ে ৩-ট্যাপ এডিট→Delete।

### Close Chamber
Arrived রোগীর Treatment খালি থাকলে ১ম/২য় বার আটকায় (Treatment বক্স
খুলে যায়), ৩য় বারে জোর করে সেভ+শেয়ার হয়।

---

# ১২. অ্যাপ ভার্সন নম্বর (V85 থেকে)

Menu স্ক্রিনের নিচে "App Version: V##" দেখা যায় —
`app/build.gradle.kts`-এর `versionName` থেকে স্বয়ংক্রিয়ভাবে বসে।
**নিয়ম:** প্রতিটা ভবিষ্যৎ ডেলিভারিতে ZIP-এর নাম (V88, V89...) আর
`build.gradle.kts`-এর `versionCode`/`versionName` — দুটোই একসাথে
বদলাতে হবে।

**V89 (2026-07-17):** Follow-up Calendar পপ-আপে কল বাটন + এডিটযোগ্য
রিমার্ক (তারিখসহ, ৫-কল গার্ডসহ) + কল-পরবর্তী রিমাইন্ডার (permission
ছাড়া); Doctor Queue-এর CHECK-UP/SUMMARY/Print বাটন-রং বাগ ফিক্স
(DoctorVisitAdapter-এ যে বাগ আগে ফিক্স হয়েছিল ঠিক সেটাই); CHECK-UP
ও SUMMARY এখন আলাদা টার্গেটে যায়; "Doctor Visit" → "Doctor Checkup"
নাম বদল; Print Center রিডিজাইন (Diet Chart "Other"-এ সরানো, ছোট
কমপ্যাক্ট কার্ড/রো, প্রপার ভেক্টর আইকন — Ayurvedic mortar+plant,
Allopathic capsule, blood test tube, diet-chart bowl — একই আইকন
Clinical Document পপ-আপেও বসানো)। বিস্তারিত: মাস্টার নোটের সেকশন ৪৩।

---

# ১৩. খোলা প্রস্তাবনা — এখনো TK-এর সিদ্ধান্তের অপেক্ষায়

1. Master অ্যাকাউন্টের "জরুরি অবস্থা" পরিকল্পনা (দ্বিতীয় ব্যক্তির
   কাছে পাসওয়ার্ড রাখা)
2. নতুন APK ছড়ানোর আগে একজনের ফোনে প্রথমে টেস্ট
3. স্টাফ চলে গেলে পাসওয়ার্ড বদলানোর চেকলিস্ট
4. মাসিক ডেটা এক্সপোর্ট (বাড়তি কপি রাখা)
5. ~~Menu-তে ভার্সন নম্বর~~ ✅ করা হয়ে গেছে (V85)
6. স্টাফদের সমস্যা রিপোর্ট করার নির্দিষ্ট ধরন
7. পুরনো Netlify সাইটের বিলিং/রিনিউয়াল মনে রাখা
8. স্টাফের ফোন হারালে পাসওয়ার্ড বদলানোর নিয়ম
9. একই রোগীকে দুইবার "Arrived" মার্ক করা আটকানো (প্রস্তাব দেওয়া
   হয়েছিল, TK এখনো হ্যাঁ/না বলেননি)
10. পুরনো "Restore local database (device backup)" অপশন সরানো/
    স্পষ্ট করে লেখা (V87-এ চিহ্নিত হয়েছে, এখনো করা হয়নি)

**কোনো AI/ডেভেলপার এই তালিকার কোনো আইটেম নিয়ে TK না বলা পর্যন্ত
নিজে থেকে কিছু করবে না।**

---

# ১৪. এই ফাইল কীভাবে ব্যবহার করবেন (AI/ডেভেলপারের জন্য)

- নতুন কোনো সেশনে কাজ শুরুর আগে **এই ফাইল আর
  `00_PROJECT_STATE_MASTER_NOTE.md`** দুটোই প্রথমে পড়ুন
- প্রতিবার প্রজেক্টে কোনো real change হলে (নতুন ফিচার, বাগ-ফিক্স,
  ফাইল যোগ/বিয়োগ) — **এই ফাইলের প্রাসঙ্গিক সেকশন আপডেট করুন**
  (নতুন ফাইল থাকলে ভাগ ৩-৯-এ যোগ করুন, নতুন সূত্র/ফ্লো থাকলে ভাগ
  ১০-১১-এ যোগ করুন)
- এই ফাইল **কখনো ছোট করবেন না** — শুধু যোগ করুন, পুরনো তথ্য মুছবেন
  না (যদি না কিছু সত্যিই ভুল প্রমাণিত হয়, তখন "সংশোধিত" লিখে
  ঠিক করুন, ভাগ ৯-এর মতো)
