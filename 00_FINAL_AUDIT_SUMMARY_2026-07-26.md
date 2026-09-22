# 🔎 FINAL AUDIT SUMMARY — মালিকের ১০-দফা Technical তালিকা
### তারিখ: ২৬.০৭.২০২৬ · Base: `PILES_CLINIC_APP_V131_FINAL.zip`

> **এই Audit-এ কোনো Code পরিবর্তন করা হয়নি** — শুধু Version V132 ছাড়া।
> Build/Test এখানে চালানো যায় না, তাই যেখানে চালানো হয়নি সেখানে স্পষ্ট **NOT TESTED** লেখা আছে।

---

## ০) আগে একটা সংশোধন ও একটা ফেরত

**ক) আমার আগের ভুল:** আমি বলেছিলাম "SQL ফাইল দেওয়া হয়নি"। **ভুল ছিল** — SQL ZIP-এর ভিতরেই আছে: `04_SUPABASE_DATABASE_SETUP/` ফোল্ডারে **৬টা** ফাইল। সেগুলো ধরেই দফা ৮ যাচাই করা হয়েছে।

**খ) ফেরত নেওয়া হয়েছে:** আগের উত্তরে আমি `SyncWorker.kt` বদলে ফেলেছিলাম। আপনার নির্দেশে সেটা **V131-এর মূল অবস্থায় ফিরিয়ে দেওয়া হয়েছে** (byte-for-byte মিলিয়ে দেখা হয়েছে)।

**এখন V131 থেকে আলাদা মাত্র ১টা ফাইল:** `app/build.gradle.kts` (versionCode 132, versionName "V132")।

**Rollback copy:** `PILES_CLINIC_APP_V131_FINAL.zip` — আপলোড করা মূল ফাইল, অক্ষত।

---

## ১) সংক্ষিপ্ত ফলাফল টেবিল

| দফা | বিষয় | রায় |
|---|---|---|
| ১ | Version V132 | ✅ **PASS** (করা হয়েছে) |
| ২ | পুরনো/নতুন Sync দ্বন্দ্ব | ⚠️ **সমস্যা আছে** — বন্ধ করা নিরাপদ, তবে একটা শর্তে (নিচে) |
| ৩ | ভুল Success / Duplicate | ✅ **PASS** — কোডে আগে থেকেই ঠিক |
| ৪ | Registration ও Payment | ❌ **সমস্যা আছে** — Registration-এ একটা আসল ঝুঁকি পাওয়া গেছে (দফা ৮ দেখুন) |
| ৫ | Patient Edit-এর Linked Data | ❌ **সমস্যা আছে** — payments / medical / doctor_visits আপডেট হয় না |
| ৬ | Delete স্থায়ী হওয়া | ❌ **সমস্যা আছে** — মোছা রেকর্ড ফিরে আসতে পারে |
| ৭ | Branch Data | ✅ **PASS** |
| ৮ | Database ও Code মেলানো | ⚠️ **নিশ্চিত নয়** — একটা কলাম অনুপস্থিত মনে হচ্ছে, আপনাকে Supabase-এ দেখতে হবে |
| ৯ | Patient ID Duplicate | ❌ **সমস্যা আছে** — নিরাপদ সমাধান নিচে লেখা |
| ১০ | Patient Photo | ✅ **PASS** — 600px করলেও 100–150 KB-এর অনেক নিচে থাকবে (মেপে দেখা) |

---

## ২) দফা ২ — পুরনো Sync (আপনার ৩টা প্রশ্নের উত্তর)

### প্রশ্ন ২.১ — Offline Pending Sync / Retry / Registration / Payment / Follow-up / Delete ক্ষতিগ্রস্ত হবে কি?

**Registration, Payment, Follow-up, Delete, Offline Pending — না, ক্ষতি হবে না।**
কারণ ওগুলোর কোনোটাই পুরনো `SyncManager` ব্যবহার করে না। ওরা সবাই চলে ৮টা `flushPending()` দিয়ে, যেগুলো ওই একই `SyncWorker`-এর ভিতরে **আগেই** চলে যায় — পুরনো অংশটা তার পরে, সম্পূর্ণ আলাদাভাবে চলে।

**Retry — এখানে একটা সূক্ষ্ম নির্ভরতা আছে, এটাই একমাত্র সাবধানতা:**
এখন পুরনো `SyncManager` ব্যর্থ হলে Worker `Result.retry()` দেয় → WorkManager পুরো Worker আবার চালায় → ফলে `flushPending()`-ও **বাড়তি একটা সুযোগ** পেয়ে যায়। শুধু পুরনো অংশটা কেটে `Result.success()` দিলে ওই **আকস্মিক বাড়তি retry-টা চলে যাবে**।
👉 **নিরাপদ সমাধান:** `Result.success()` না দিয়ে — "কোনো queue-তে এখনো কিছু বাকি থাকলে `Result.retry()`, নইলে `Result.success()`"। তাহলে retry আগের চেয়ে **আরও নির্ভুল** হবে, দুর্বল নয়।
⚠️ এর জন্য ৮টা রিপোজিটরিতে একটা করে ছোট `pendingCount()` দরকার — এখন শুধু `EnquiryRepository`-তে আছে।

### প্রশ্ন ২.২ — সত্যিই কোনো Screen/Repository ব্যবহার করে না? — Exact reference list

**ক) `SyncManager` — পুরো প্রজেক্টে মোট ৩টা reference:**
| ফাইল | লাইন | কী |
|---|---|---|
| `data/repository/SyncManager.kt` | 39 | ক্লাসের নিজের সংজ্ঞা |
| `data/sync/SyncWorker.kt` | 7 | `import` |
| `data/sync/SyncWorker.kt` | 36 | একমাত্র জায়গা যেখানে চালানো হয় |

➡️ **কোনো Activity, Adapter বা Repository এটা ব্যবহার করে না।**

**খ) Room ডেটাবেস (`AppDatabase` + ৪টা DAO):**
| ফাইল | লাইন | কী |
|---|---|---|
| `data/local/AppDatabase.kt` | 20–35 | নিজের সংজ্ঞা |
| `data/repository/SyncManager.kt` | 108,125,142,159,178,194,210,223 | একমাত্র ব্যবহারকারী |
| `data/sync/SyncWorker.kt` | 34 | `getInstance` |
| `security/BackupManager.kt` | 23, 51 | ফাইল-কপি ব্যাকআপ |

➡️ **Enquiry / Registration / Payment / Follow-up / Chamber / Draft / Timeline — একটাও Room ছোঁয় না।** সব চলে `LocalWorkflowStore` (ফোনের নিজের সঞ্চয়) + `SupabaseClient` (ক্লাউড) দিয়ে।

**গ) `SyncStatusHolder` / `SyncState` (পুরনো Sync যেটা আপডেট করে):**
| ফাইল | লাইন |
|---|---|
| `data/repository/SyncState.kt` | 6–18 (সংজ্ঞা) |
| `data/repository/SyncManager.kt` | 61, 98, 100 (একমাত্র লেখক) |

➡️ **কোনো স্ক্রিন এটা পড়ে না / দেখায় না** — তাই বন্ধ করলে পর্দায় কিছুই বদলাবে না।

**ঘ) Settings-এর "Backup Now" — ৩টা কাজ করে:**
1. `BackupManager.backupNow()` → Room ফাইলের কপি (পুরনো),
2. `exportCloudJson()` → **সরাসরি Supabase থেকে ৭টা টেবিলের আসল JSON ব্যাকআপ** (কোডের নিজের মন্তব্য: "the reliable Restore source"),
3. `exportBackupCsv()` → সরাসরি Supabase থেকে CSV।
➡️ আসল ব্যাকআপ (২ ও ৩) Room-এর উপর **একেবারেই নির্ভর করে না**।

### প্রশ্ন ২.৩ — বন্ধ করার আগে/পরে ঠিক কোন ফাইল ও লাইন বদলাবে?

**মাত্র ১টা ফাইল: `.../data/sync/SyncWorker.kt` — মাত্র ৯টা লাইন (৩৩–৪১)।**

**আগে (V131, এখন যেমন আছে):**
```
33        return try {
34            val db = AppDatabase.getInstance(applicationContext)
35            val sessionManager = SessionManager(applicationContext)
36            val syncManager = SyncManager(applicationContext, db, sessionManager)
37            val summary = syncManager.syncAll()
38            if (summary.errors.isEmpty()) Result.success() else Result.retry()
39        } catch (e: Exception) {
40            Result.retry()
41        }
```

**পরে (প্রস্তাব):** উপরের ৯ লাইনের বদলে — "সব queue খালি হলে `Result.success()`, কিছু বাকি থাকলে `Result.retry()`"।

**যা ছোঁয়া হবে না:** ২৩–৩১ লাইনের ৮টা `flushPending()` — অক্ষর-অক্ষর অপরিবর্তিত।
**কোনো ফাইল মুছব না** — `SyncManager.kt`, `AppDatabase.kt`, DAO, `SyncState.kt` সব জায়গামতো থাকবে, শুধু আর ডাকা হবে না। এক লাইনে ফেরত আনা যায়।

**লাভ:** প্রতি ১৫ মিনিটে **এবং প্রতিটা Save-এর পরে** Supabase থেকে ৪টা আস্ত টেবিল নামানো বন্ধ হবে — পাঁচ ব্রাঞ্চের প্রতিটা ফোনে। ফ্রি-প্ল্যানের কোটা, মোবাইল ডেটা ও ব্যাটারির বড় সাশ্রয়।

---

## ৩) দফা ৩ — ভুল Success / Duplicate → ✅ PASS

**Code evidence:**
- `EnquiryRepository.kt:124–148` — আগে **লোকালে** সেভ (`LocalWorkflowStore`), তারপর `queuePending()`, তারপর একবার background চেষ্টা। ক্লাউডে না গেলে সারি **pending-এ থেকে যায়**, হারায় না।
- `EnquiryRepository.kt:203–214` — `queuePending()` একই `table + id` আগে queue-তে থাকলে পুরনোটা **বাদ দিয়ে** নতুনটা রাখে → queue-তে ডুপ্লিকেট জমে না।
- `EnquiryModel.kt:44,75` · `PatientModel.kt:65,114,150` · `PaymentModel.kt:176,208,233` — প্রতিটা সারির নিজস্ব **স্থায়ী UUID id** (`enq_…`, `pat_…`, `pay_…`), সেভের সময়েই তৈরি।
- `SupabaseClient.kt:44` — ক্লাউডে লেখা হয় `Prefer: resolution=merge-duplicates` দিয়ে, অর্থাৎ **একই id বারবার পাঠালেও নতুন সারি তৈরি হয় না**, পুরনোটাই আপডেট হয়।

➡️ "Internet না থাকলে Pending" ✅ · "Internet ফিরলে একবারই Sync, Duplicate নয়" ✅
**NOT TESTED:** আসল ফোনে নেট বন্ধ করে পরীক্ষা এখানে করা যায়নি।

---

## ৪) দফা ৪ — Registration ও Payment → ❌ সমস্যা আছে

**Payment অংশ ✅:** একজন রোগীর পেমেন্টে কোনো **hard limit নেই** (যাচাই করা), আর Paid/Due-র হিসাব সব স্ক্রিনে একই নিয়মে গোনা হয় (`visit_fee` ও `attendance_mark` বাদ) — `PatientTimelineRepository`, `ReportCardActivity`, `DraftRepository`, `FollowUpRepository`, `DoctorVisitActivity`, `ReportsRepository` — সব মিলিয়ে দেখা হয়েছে।

**Registration অংশ ❌:** দফা ৮-এ পাওয়া কলাম-সমস্যার কারণে **patients সারি ক্লাউডে না-ও পৌঁছাতে পারে** (নিচে বিস্তারিত)। সেক্ষেত্রে ফোনে সেভ দেখাবে, কিন্তু ক্লাউডে যাবে না — ঠিক "অর্ধেক Save"।

---

## ৫) দফা ৫ — Patient Edit-এর Linked Data → ❌ সমস্যা আছে

**Code evidence:**
- `PatientTimelineActivity.kt:290–301` — মোবাইল বদলালে আপডেট হয় শুধু **`followups`** ও **`enquiries`**।
- `FollowUpActivity.kt:1695–1712` — আপডেট হয় শুধু **`patients`** ও **`enquiries`**।

**যেগুলো কোথাওই আপডেট হয় না:** `payments` · `medical` (Prescription / Medicine Slip / Blood Test / Diet) · `doctor_visits`।

**ফল:** পুরনো নম্বরে তোলা যে পেমেন্টে `patientId` নেই (যেমন Chamber থেকে নেওয়া), মোবাইল বদলালে সেগুলো **পুরনো নম্বরের নিচে পড়ে থাকতে পারে**। যেগুলোতে `patientId` আছে সেগুলো ঠিক থাকে (`PatientTimelineRepository.kt:252` patientId দিয়েও খোঁজে)।

---

## ৬) দফা ৬ — Delete → ❌ সমস্যা আছে

**Code evidence — `TrashHelper.kt:45–66` (`moveToTrash`):**
1. `trash` টেবিলে সারি লেখে → 2. `SupabaseClient.deleteById(table, id)` দিয়ে **ক্লাউড** থেকে মোছে।
**যা করে না:** ফোনের `LocalWorkflowStore` ক্যাশ থেকে মোছে না, আর **pending queue** থেকেও মোছে না।

**ফল:** অফলাইনে সেভ হওয়া কোনো সারি (যেটা এখনো queue-তে) মুছে দিলে, পরের `flushPending()` সেটা আবার Supabase-এ **ফিরিয়ে দেবে** — মোছা রেকর্ড ফিরে আসবে। ঠিক যেটা আপনি লিখেছেন।

`LocalWorkflowStore`-এ শুধু `removePayment(id)` (161) ও `removeEnquiry(id)` (219) আছে; followups/patients-এর জন্য নেই, আর delete-এর পথ এগুলোর কোনোটাই ডাকে না।

⚠️ **ঝুঁকি:** ঠিক করতে **৮টা আলাদা pending queue**-তে হাত দিতে হবে (`piles_clinic_enquiry_pending`, `..._registration_pending`, `..._payment_pending`, `..._followup_pending`, `..._followup_heal_pending`, `..._chamber_pending`, `..._medical_pending`, `..._briefing_pending`, `..._generic_update_pending`) — এটাই অ্যাপের সবচেয়ে স্পর্শকাতর অংশ। **Trash/Restore-এর নিয়ম বদলাব না**, শুধু মোছার সময় ক্যাশ ও queue পরিষ্কার হবে।

---

## ৭) দফা ৭ — Branch Data → ✅ PASS

**Code evidence:** রিপোজিটরিগুলোতে ব্রাঞ্চ-ফিল্টার **৯৪ জায়গায়** বসানো। role-এর তুলনা সর্বত্র একই ছোট-হাতের লেখায় (`"master"`, `"staff"`, `"doctor"`, `"field"`) — `StaffDirectory.kt`-এ role হাতে লেখা, কোথাও বড়-ছোট হাতের গোলমাল নেই। Master-এর "All Branch" ও ব্রাঞ্চ বাছাই আলাদাভাবে আছে (যেমন `ChamberAttendanceActivity.kt:167`)।
**কিছু পরিবর্তন করা হয়নি** — আপনার নির্দেশ ছিল permission না ছোঁয়ার।

---

## ৮) দফা ৮ — Database ও Code মেলানো → ⚠️ একটা জিনিস আপনাকে দেখতে হবে

**যা মেলানো হয়েছে** (`04_SUPABASE_DATABASE_SETUP/`-এর ৬টা SQL ফাইল বনাম পুরো Kotlin কোড):

✅ কোড যেসব কলামে **সার্ভারে ফিল্টার** করে — `branch, date, id, kind, mobile, patientId, payType, registrationDate, stage, status` — **সবগুলোই SQL-এ আছে**।
✅ **sort** যেসব কলামে হয় — `updatedAt`, `name` — **আছে**।
✅ `enquiries` (১৭টি), `payments` (৪ ধরনের সারি), `payment_backdate_requests` — **প্রতিটা লেখা ফিল্ড SQL-এ আছে**।

### ❌ একটা সম্ভাব্য গরমিল — `patients` টেবিল ও `timeType`

| | |
|---|---|
| **কোড কী করে** | `PatientModel.kt:96` — Registration-এর সময় `patients` সারিতে **`timeType`** লেখে |
| **SQL কী বলে** | `timeType` কলাম **`enquiries`**-এ আছে (`PILES_CLINIC_DB_SETUP.sql:13`) এবং **`followups`**-এ আছে (`PATCH_2026-07-23_followups_timeType.sql:27`) — কিন্তু **`patients`-এ কোথাও যোগ করা হয়নি** |
| **যদি সত্যিই না থাকে** | Supabase অচেনা কলামসহ পুরো সারিটাই **বাতিল** করে → `upsert` ব্যর্থ → patients সারি pending queue-তে আটকে থাকে, বারবার চেষ্টা করেও কখনো ঢোকে না। ফোনে "Save হয়েছে" দেখাবে, **ক্লাউডে যাবে না** |

**🔴 আপনার কাছে একটাই অনুরোধ:** Supabase-এ গিয়ে `patients` টেবিলে **`timeType`** নামে কলাম আছে কিনা একবার দেখুন।
- **থাকলে** → কোনো সমস্যা নেই, দফা ৮ সম্পূর্ণ PASS।
- **না থাকলে** → এটাই দফা ২ ও ৪-এ আপনার লেখা "Data হারিয়ে যাওয়া / অর্ধেক Save"-এর আসল কারণ হতে পারে। সমাধান একটাই নিরাপদ লাইন (কিছু মোছে না, শুধু যোগ করে):
  `alter table public.patients add column if not exists "timeType" text;`

**আপনার নির্দেশ মেনে কোনো SQL পরিবর্তন করা হয়নি।**

---

## ৯) দফা ৯ — Patient ID Duplicate → সবচেয়ে নিরাপদ পদ্ধতি

**সমস্যাটা কোথায় (`PatientIdGenerator.kt:36–47`):** সিরিয়াল বের হয় "আগে পড়ো, তারপর লেখো" পদ্ধতিতে — Supabase-এ ওই ব্রাঞ্চ+তারিখের সব ID পড়ে সবচেয়ে বড়টার সাথে ১ যোগ করে। দুই ফোন একই সেকেন্ডে করলে দুজনেই একই সংখ্যা পড়বে → **একই ID**।

আপনি ঠিকই বলেছেন — মাসে ১০০–১৫০টা বাড়তি read কিছুই না। কোটার অজুহাতে এটা বাদ দেওয়া উচিত নয়।

### প্রস্তাবিত সমাধান — সবচেয়ে নিরাপদ, দুই স্তরে

**স্তর ১ (আসল রক্ষাকবচ, ডেটাবেসে):** `patients.patientId`-এ একটা **unique index**। এটাই একমাত্র ১০০% নিশ্চিত উপায় — দুটো ফোন যত দ্রুতই চেষ্টা করুক, ডেটাবেস দ্বিতীয়টাকে **নিজেই আটকে দেবে**। এটা "safe additive migration", কিছু মোছে না:
```sql
create unique index if not exists patients_patientid_uniq
  on public.patients ("patientId") where "patientId" is not null;
```
⚠️ চালানোর আগে দেখতে হবে **এখনই কোনো ডুপ্লিকেট আছে কিনা** — থাকলে index তৈরি হবে না। তাই আগে একটা "খুঁজে দেখার" query চালাতে হবে (কিছু বদলায় না):
```sql
select "patientId", count(*) from public.patients
where "patientId" is not null and "patientId" <> ''
group by "patientId" having count(*) > 1;
```

**স্তর ২ (অ্যাপে, তাৎক্ষণিক ভদ্র আচরণ):** সেভ ব্যর্থ হলে — অর্থাৎ ডেটাবেস "এই ID আগেই আছে" বলল — অ্যাপ **নিজে থেকে পরের সিরিয়ালটা নিয়ে আবার চেষ্টা করবে**, সর্বোচ্চ ৩ বার। স্টাফ কিছু টেরও পাবেন না, শুধু ID এক ঘর এগিয়ে যাবে।

**কেন এই দুইয়ে মিলে সবচেয়ে নিরাপদ:** শুধু স্তর ২ দিলে "প্রায় নিশ্চিত" হয়, ১০০% নয় (দুই ফোন একই মুহূর্তে হলে দুটোই সফল ভাবতে পারে)। শুধু স্তর ১ দিলে ডুপ্লিকেট আটকাবে ঠিকই, কিন্তু স্টাফ একটা ব্যর্থতা দেখবেন। **দুটো একসাথে = ডুপ্লিকেট অসম্ভব, আর স্টাফের চোখে সব স্বাভাবিক।**
**পুরনো কোনো Patient ID বদলানো হবে না।**

---

## ১০) দফা ১০ — Patient Photo → ✅ 600px সম্ভব, ফাইল ৩–৪ গুণ হবে না

**এখন কী আছে:** `PhotoUtils.kt:17` — `maxSide = 400`, `quality = 70`।
⚠️ **এবং একই কাজের একটা দ্বিতীয় কপিও আছে:** `PatientPhotoActivity.kt:158–175` (নিজের ভিতরে আলাদা 400/70 লেখা)। বদলালে **দুটোই** বদলাতে হবে, নইলে দুই স্ক্রিনে দুই মাপের ছবি হবে।

**মেপে দেখা হয়েছে** (একটা ঘন-বিস্তারিত ছবির উপর, ফোনের JPEG-ও প্রায় একই আচরণ করে):

| সেটিং | JPEG ফাইল | ডেটাবেসে (base64) |
|---|---|---|
| **এখনকার — 400px, quality 70** | ~22 KB | ~29 KB |
| 600px, quality 70 | ~37 KB | ~49 KB |
| 600px, quality 80 | ~46 KB | ~61 KB |
| **600px, quality 85** ← প্রস্তাব | ~54 KB | ~72 KB |
| 600px, quality 90 | ~68 KB | ~91 KB |

**উত্তর:** হ্যাঁ — **600px রেখে মুখ পরিষ্কার রাখা যায় এবং তাও ১০০ KB-এর নিচেই থাকে**, আপনার ১০০–১৫০ KB সীমার অনেক নিচে। ৩–৪ গুণ বড় হওয়ার দরকার নেই; আনুমানিক **২.৫ গুণ** (২৯ KB → ৭২ KB)।

**আমার সৎ পরামর্শ:** 400px-এ মুখ চেনা গেলেও **প্রিন্টে বা বড় করে দেখলে** ৪০০ ছোট পড়ে। তাই **600px / quality 85** যুক্তিসঙ্গত — তবে এটা আপনার ছবি দেখে সিদ্ধান্ত নেওয়ার বিষয়। **পুরনো কোনো ছবিতে হাত দেওয়া হবে না** — নতুন তোলা ছবিতেই শুধু নতুন মাপ লাগবে।

---

## ১১) PASS / FAIL / NOT TESTED — চূড়ান্ত

| বিষয় | ফল |
|---|---|
| ১৫০টা Kotlin ফাইলের গঠন যাচাই | **PASS** |
| ২০২টা XML-এর গঠন যাচাই | **PASS** |
| সব রিসোর্স রেফারেন্স | **PASS** |
| ৩৯টা Activity ম্যানিফেস্টে | **PASS** |
| Web `app.js` / CSS / ১৬৩টা বোতাম | **PASS** |
| স্থির মাপের ঘরে লেখা ধরা (৫৬টা) | **PASS** |
| SQL বনাম কোডের কলাম মেলানো | **PASS**, একটি ছাড়া (`patients.timeType`) |
| Android Studio-তে Build | **NOT TESTED** (এখানে build হয় না) |
| ফোনে লাইভ টেস্ট | **NOT TESTED** |
| অফলাইন / নেট-ফেরা পরীক্ষা | **NOT TESTED** |
| Live Supabase-এর আসল schema | **NOT TESTED** (এখান থেকে ইন্টারনেট বন্ধ) |

---

## ১২) ঘোষণা

এই Audit-এ **Version V132 ছাড়া অ্যাপের কোনো Code, Design, Layout, রং, Navigation, Working Flow, Permission বা Data নিয়ম পরিবর্তন করা হয়নি।** কোনো SQL চালানো বা বদলানো হয়নি। কোনো ফাইল মোছা হয়নি। `.git`, পুরনো ZIP, Lock Note — সব অক্ষত।

**পরের ধাপ আপনার:** উপরের ❌ ও ⚠️ চিহ্নিত কাজগুলোর মধ্যে কোনটা করব, একটা একটা করে বলুন।

---

**🔒 LOCK NOTE:** এই ফাইলে যা আছে তা ধ্বংস করা যাবে না, কোনো ডিজাইন মালিককে না জানিয়ে বদলানো যাবে না, কোনো working flow খারাপ করা যাবে না, অ্যাপ স্লো করা যাবে না।
