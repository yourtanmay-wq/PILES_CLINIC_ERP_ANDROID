# STATIC CODE ANALYSIS REPORT — PILES CLINIC APP
Senior Android Code Auditor · Static analysis only · তারিখ: 2026-07-10

> এই রিপোর্ট **শুধু static code analysis**। কোনো APK build, emulator বা real
> device test করা হয়নি এবং দাবিও করা হয়নি। যেখানে static analysis থেকে নিশ্চিত
> করা যায় না, সেখানে **"Cannot be verified by static code analysis"** লেখা।
> প্রতিটি সিদ্ধান্তের সাথে file + code evidence দেওয়া।

---

## ১. সারসংক্ষেপ টেবিল

| Category | Status | Evidence |
|----------|--------|----------|
| Native Architecture | **PASS** | `grep android.webkit` পুরো `app/src/main/java`-তে ০ ফল। MainActivity.kt ও bridge/AndroidBridge.kt মুছে ফেলা। |
| Active WebView | **PASS (none)** | কোনো `WebView`/`loadUrl`/`WebViewClient` কোড নেই; MainActivity-র কোনো `::class` রেফারেন্স নেই (শুধু কমেন্ট)। |
| Room Database | **PASS (mapping)** | `AppDatabase.kt` @Database-এ ৪ entity + ৪ DAO; সব Entity-তে tableName; Converters রেজিস্টার্ড। |
| Supabase | **PASS-with-notes** | native creds এখন এক জায়গায় (`SupabaseClient.kt`); মেথড ব্যবহার সঙ্গতিপূর্ণ। নোট: `mobile=eq.+91…` URL-এ `+` এনকোডিং runtime-এ যাচাইযোগ্য নয়। |
| Navigation | **FAIL (1 dead screen)** | `SyncStatusActivity` manifest-এ আছে কিন্তু কোনো `SyncStatusActivity::class` navigation নেই → unreachable। বাকি ২৭টি Activity reachable। |
| Business Logic | **FAIL (duplication)** | দুটি সমান্তরাল ডেটা-স্তর ও দুটি `EnquiryRepository` ক্লাস (`data/repository` ও `native`)। UI native-টা ব্যবহার করে; Room-স্তর UI write পায় না। |
| Performance Risks | **WARN** | `DraftRepository`/`ReportsRepository` একবারে 5000 সারি × ৪ টেবিল; `PatientPhotoRepository` লুপে ক্রমিক `updateById`। (background thread-এ, তবু ভারী) |
| Security Risks | **WARN** | anon/publishable key hardcoded (`SupabaseClient.kt`); exported কেবল launcher। প্রকৃত সুরক্ষা Supabase RLS-নির্ভর → static-এ যাচাই করা যায় না। |
| Build Verification | **NOT VERIFIED** | এই পরিবেশে gradle/SDK/compiler নেই, নেট বন্ধ, gradle wrapper অসম্পূর্ণ। Cannot be verified by static code analysis. |
| Real Device Test | **NOT VERIFIED** | AI হিসেবে ফোন/emulator চালানো যায় না। Cannot be verified by static code analysis. |

---

## ২. বিস্তারিত Finding (File · Class · Function · Root Cause · Fix)

### F1 — Unreachable dead screen: SyncStatusActivity  [Navigation: FAIL]
- **File:** `sync/SyncStatusActivity.kt` · **Class:** SyncStatusActivity
- **Evidence:** পুরো `app/src/main/java`-তে `SyncStatusActivity::class` বা এটিকে
  `startActivity`-তে ব্যবহারের কোনো লাইন নেই; শুধু `PrintCenterActivity.kt`-এর
  একটি কমেন্টে নাম আছে, আর manifest লাইন 196-এ রেজিস্ট্রেশন। exported=false।
- **Root Cause:** স্ক্রিনটি তৈরি হয়েছিল কিন্তু কোনো native মেনু/বোতাম থেকে আর
  লিঙ্ক করা হয়নি (WebView যুগের অবশিষ্ট)।
- **Fix Recommendation:** হয় `SettingsActivity`/`MoreMenuActivity`-তে একটি
  "Sync Status" বোতাম যোগ করে reachable করুন, অথবা ফাইল + manifest এন্ট্রি
  সরিয়ে দিন। (নিরাপদে সরানো যায়, কারণ কেউ ডাকে না।)

### F2 — Duplicate parallel data architecture  [Business Logic: FAIL]
- **Files/Classes:**
  - Room path: `data/local/*` (AppDatabase, EnquiryEntity…), `data/repository/EnquiryRepository`, `data/repository/SyncManager`, `data/sync/SyncWorker`.
  - Direct-Supabase path: `native/EnquiryRepository`, `native/EnquiryModel`, `native/SupabaseClient`.
- **Evidence:**
  - দুটি ভিন্ন প্যাকেজে একই নামের ক্লাস: `data/repository/EnquiryRepository.kt` **এবং** `native/EnquiryRepository.kt`।
  - `native/EnquiryActivity.kt:38,51` → `repository = EnquiryRepository(this)` (native-টা, সরাসরি Supabase)।
  - `PilesClinicApplication.kt:32` → `SyncScheduler.schedulePeriodic(this)` (Room→Supabase sync চলে)।
  - কিন্তু native UI (Enquiry/Registration/Payment/FollowUp) Room DAO-তে **write করে না** — তারা SupabaseClient-এ সরাসরি লেখে।
- **Root Cause:** অ্যাপটি আগের Room+sync ডিজাইন থেকে সরাসরি-Supabase native
  ডিজাইনে স্থানান্তরিত; পুরনো স্তর রয়ে গেছে। ফলে Room offline-first স্তর UI-র
  লেখা পায় না (SyncWorker কেবল Supabase→Room pull করে, print/backup তা পড়ে)।
- **Fix Recommendation (owner-decision, build ছাড়া নিরাপদে করা যায় না):** একটি পথ
  বেছে নিন — (ক) native write-গুলো Room-এর মধ্য দিয়ে চালিয়ে সত্যিকারের
  offline-first পুনরুদ্ধার করুন, অথবা (খ) ব্যবহার-না-হওয়া Room+sync স্তর সরিয়ে
  একক direct-Supabase রাখুন। এখানে ইচ্ছাকৃতভাবে রিমুভ করা হয়নি (নির্দেশ #13:
  business logic অযথা redesign নয়) — কারণসহ রিপোর্ট করা হলো।

### F3 — পুরনো Print কার্ড Room পড়ে, native data Supabase-এ  [Business Logic/WARN]
- **File:** `print/PrintCenterActivity.kt:41` · **Function:** onCreate/init
- **Evidence:** `registrationRepo = RegistrationRepository(AppDatabase.getInstance(this))`
  — Room থেকে registration পড়ে; কিন্তু native RegistrationActivity Supabase-এ লেখে,
  Room-এ নয়।
- **Root Cause:** F2-এর ফল — print পুরনো Room স্তরে, ডেটা নতুন Supabase স্তরে।
- **Fix Recommendation:** পুরনো print কার্ডগুলো SupabaseClient থেকে পড়াতে হবে
  (নতুন Payment/DoctorVisit/BloodTest কার্ড যেভাবে পড়ে)। → **Cannot be verified
  by static code analysis** যে ফোনে ফাঁকা প্রিন্ট দেবে কিনা; কোড-পথে ঝুঁকি আছে।

### F4 — Session-only / in-memory clinical data (আগে) — FIXED
- **File:** `clinical/ClinicalRepository.kt` (in-memory) → **Fix:** নতুন
  `clinical/ClinicalCloudRepository.kt` + ৪টি clinical Activity-তে Supabase
  `medical` টেবিলে persist।
- **Evidence:** `grep saveMedical` → DoctorCheckup/Diet/Investigation/Prescription
  চারটিতেই কল আছে।
- **অবশিষ্ট:** in-memory working-copy (print handoff-এর জন্য) থেকে যায়, কিন্তু
  ডেটা এখন স্থায়ীভাবেও সেভ হয়।

### F5 — Placeholder/Demo fallback: RoleSession  [Placeholder]
- **File:** `clinical/RoleSession.kt` · **Object:** RoleSession
- **Evidence:** ডিফল্ট `currentPatientName = "Demo Patient"`, `currentPatientId
  = "P-0001"`, ডিফল্ট role DOCTOR। Doctor Queue থেকে extras দিয়ে খুললে override
  হয়।
- **Root Cause:** clinical স্ক্রিন patient ছাড়া সরাসরি খুললে fallback দরকার ছিল।
- **Fix Recommendation:** patient না থাকলে খালি স্ট্রিং + একটি "রোগী নির্বাচন
  করুন" বার্তা দেখান; অথবা patient ছাড়া clinical স্ক্রিন খোলা আটকান।

### F6 — Hardcoded credentials  [Security: WARN]
- **File:** `native/SupabaseClient.kt` · **Object:** SupabaseClient
- **Evidence:** `URL="https://…supabase.co"`, `KEY="sb_publishable_…"` সরাসরি সোর্সে।
- **Root Cause:** anon/publishable key ক্লায়েন্টে থাকতেই হয় (ডিজাইন)। তবে এটি
  hardcoded, এবং data-layer `SupabaseConfig.kt` BuildConfig ব্যবহার করে — দুই ভিন্ন
  প্রক্রিয়া।
- **Fix Recommendation:** anon key ঠিক আছে যদি Supabase **RLS** সঠিক থাকে। →
  **RLS আসলে সক্রিয় কিনা static analysis-এ যাচাই করা যায় না (Cannot be verified
  by static code analysis)।** পুরো একীকরণ করতে সব উৎস BuildConfig-এ আনুন
  (local.properties দরকার — build-time)।

### F7 — Performance: full-list & সিরিয়াল কল  [Performance: WARN]
- **Files/Functions:**
  - `native/DraftRepository.load()` লাইন 57-60: ৪টি টেবিল, প্রতিটি `,5000` limit।
  - `native/ReportsRepository.load()` লাইন 43-45: ৩টি টেবিল, `,5000`।
  - `native/PatientPhotoRepository.savePhoto()` লাইন 49-55: followups-এর উপর লুপে
    ক্রমিক `updateById`।
- **Evidence:** উপরের লাইনগুলোতে সরাসরি দেখা যায়।
- **Root Cause:** aggregate/mirror করতে ক্লায়েন্টে সব সারি আনা।
- **Fix Recommendation:** সার্ভার-সাইড count/aggregate (PostgREST) ব্যবহার করুন;
  photo mirror ব্যাচে বা একটি RPC-তে করুন। → ফোনে প্রকৃত গতি **Cannot be verified
  by static code analysis**।

### F8 — Offline behaviour of native screens  [Architecture note]
- **Evidence:** সব `native/*Repository` কেবল `SupabaseClient` (HTTP) ব্যবহার করে;
  এদের কোনো Room fallback নেই (F2)।
- **প্রভাব:** ইন্টারনেট ছাড়া DoctorQueue/Briefing/Calendar/Draft/Reports/Trash/
  Password/Photo/clinical-save খালি/ব্যর্থ হবে। মূল Enquiry/Registration/Payment/
  FollowUp-এর জন্য Room entity আছে, কিন্তু UI direct-Supabase পথ ব্যবহার করে (F2),
  তাই কার্যত এগুলোও online-নির্ভর। → প্রকৃত অফলাইন আচরণ **Cannot be verified by
  static code analysis** (runtime দরকার)।

---

## ৩. যা static analysis-এ PASS প্রমাণিত
- **Native architecture:** কোনো `android.webkit`/WebView কোড নেই (grep evidence)।
- **Room mapping:** ৪ entity ↔ ৪ DAO ↔ @Database ↔ Converters সঙ্গতিপূর্ণ।
- **Manifest:** বৈধ XML; একক launcher `.native.LoginActivity`; ২৭/২৮ Activity
  reachable (SyncStatusActivity বাদে)।
- **Single native Supabase source:** creds কেবল `SupabaseClient.kt`-এ।
- **exported:** launcher ছাড়া সব `exported="false"`।
- **Brace/paren balance:** সব সম্পাদিত/নতুন ফাইলে সুষম (আলাদা যাচাই)।

## ৪. যা static analysis-এ যাচাই করা যায় না (NOT VERIFIED)
- APK/AAB build সফল হবে কিনা — **Cannot be verified by static code analysis**
  (build পরিবেশ নেই)।
- Emulator/Real device চালানো, প্রকৃত গতি, অফলাইন আচরণ, প্রিন্ট আউটপুট —
  **Cannot be verified by static code analysis**।
- Supabase RLS/টেবিল স্কিমা বাস্তবে মিলছে কিনা — **Cannot be verified by static
  code analysis** (সার্ভার-সাইড)।

---

## ৫. অগ্রাধিকার (static-প্রমাণিত)
1. **F2** — সমান্তরাল ডেটা-স্তর: owner সিদ্ধান্ত নিন (offline-first ফেরান বা Room
   স্তর সরান)। প্রিন্ট/ডেটা সঙ্গতির মূল কারণ।
2. **F3** — পুরনো print কার্ড Supabase-এ আনুন।
3. **F1** — SyncStatusActivity reachable করুন বা সরান।
4. **F5** — RoleSession-এর Demo fallback পরিষ্কার করুন।
5. **F7** — বড় ডেটায় aggregate সার্ভার-সাইডে নিন।

> চূড়ান্ত কথা: static-প্রমাণিত সব কাঠামোগত জিনিস ঠিক (native, Room mapping,
> manifest, single WebView-free)। কিন্তু build/device evidence নেই — তাই
> "Production Ready" বলা হয়নি। প্রথম Debug build-ই পরবর্তী ধাপ।
