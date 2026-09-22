# DEEP PRODUCTION AUDIT — PILES CLINIC APP
তারিখ: 2026-07-10 · কোনো অনুমান নয়, সব কোড থেকে যাচাই করা।

> গুরুত্বপূর্ণ সততা: এই পরিবেশে অ্যাপ **build বা ফোনে চালানো যায় না**। তাই কোথাও
> "Real Device Test Complete" বা "Production Ready" লেখা হয়নি। যেখানে আসল ফোনে
> পরীক্ষা লাগবে সেখানে **MANUAL DEVICE TEST REQUIRED** লেখা আছে।

---

## A. Exact Build Result
**Debug APK / Release APK / AAB — কোনোটাই এই পরিবেশে build করা যায়নি।**
কারণ (হুবহু):
- `gradle` কমান্ড নেই, `javac` নেই, Kotlin compiler নেই (শুধু JRE `java` আছে)।
- Android SDK নেই (`ANDROID_HOME` খালি, কোনো sdk ডিরেক্টরি নেই)।
- ইন্টারনেট বন্ধ (HTTP 403 host_not_allowed) — Gradle distribution ও dependency
  ডাউনলোড অসম্ভব।
- প্রজেক্টে **gradle wrapper অসম্পূর্ণ**: `gradlew`, `gradlew.bat`,
  `gradle/wrapper/gradle-wrapper.jar` — তিনটিই MISSING (শুধু
  `gradle-wrapper.properties` আছে, gradle 8.5 নির্দেশ করে)।
- `local.properties` MISSING (sdk.dir + SUPABASE_URL/ANON_KEY দরকার)।

**MANUAL DEVICE TEST REQUIRED:** Android Studio-তে প্রজেক্ট খুললে সেটি wrapper
নিজে regenerate করে; Debug APK build করে দেখতে হবে।

---

## B. Critical Bug List
1. **[FIXED] Clinical ডেটা স্থায়ীভাবে সেভ হচ্ছিল না।** `ClinicalRepository`
   ছিল in-memory/session-only (নিজের কমেন্টেই স্বীকৃত), অ্যাপ বন্ধ করলে
   Prescription/Investigation/Diet/Checkup মুছে যেত, যদিও "saved" টোস্ট দেখাত।
   **Root-cause fix:** নতুন `ClinicalCloudRepository` — প্রতিটি রেকর্ড এখন
   Supabase `medical` টেবিলে patient-এর সাথে যুক্ত করে স্থায়ীভাবে সেভ হয়
   (৪টি মডিউলেই wired)।
2. **[FIXED] Prescription persist-এ ভুল field নাম** (`it.dose`/`it.days`) —
   MedicineEntry-তে ওই নাম নেই; বিল্ডে compile error হতো। ঠিক করা হয়েছে
   (`it.dosage`/`it.frequency`/`it.duration`)।

> এ ছাড়া কোড থেকে যন্ত্র-যাচাইযোগ্য অন্য কোনো critical (crash/compile-break)
> পাওয়া যায়নি। বাকি compile-স্তরের নিশ্চয়তা → **MANUAL DEVICE TEST REQUIRED**।

---

## C. High Priority Bug List
1. **[FIXED] Payment mode-এ "UPI" ছিল।** এখন শুধু **CASH / ONLINE**
   (PaymentActivity input + summary, RegistrationActivity, payment layout label)।
   Summary-তে "ONLINE" = CASH ছাড়া সব (পুরনো UPI ডেটাও এতে গোনা হয়)।
2. **[FIXED] Auto-logout / Session Timeout।** `SessionTimeoutManager` idle হলে
   session clear করত। এখন নিষ্ক্রিয় — **login manual logout পর্যন্ত থাকবে**।
3. **[FIXED] অপ্রয়োজনীয় exported Activity (৫টি)।** MainActivity,
   ClinicalModulesActivity, SyncStatusActivity, PrintCenterActivity,
   SettingsActivity — সব `exported="false"` করা হয়েছে। শুধু LoginActivity
   (launcher) `exported="true"`।
4. **[FIXED-partial] Supabase config একাধিক জায়গায়।** Native দিকে এখন এক
   source: `SupabaseClient.URL/KEY` (CloudPasswordCheck এখন সেটাই ব্যবহার করে)।
   *বাকি:* data-layer `SupabaseConfig.kt` (BuildConfig/local.properties নির্ভর) ও
   web `config.js` আলাদা প্রক্রিয়া — এদের একীভূত করতে local.properties সেট করা
   দরকার (build-time), যেটি **MANUAL** ধাপ (নাহলে build ভাঙার ঝুঁকি)।

---

## D. Medium / Low Priority List
1. **নতুন native স্ক্রিনগুলো Online-only** (DoctorQueue, Briefing, Calendar,
   Draft, Reports, Trash, Password, Patient Photo, Clinical cloud save)। এগুলো
   সরাসরি Supabase পড়ে/লেখে — Room offline cache নেই। **ইন্টারনেট না থাকলে
   এগুলো খালি দেখাবে বা "check connection" বলবে।** মূল Enquiry/Registration/
   Payment অবশ্য Room + offline sync ব্যবহার করে। (Medium)
2. **Draft ও Reports একবারে 5000 সারি পড়ে** (৪টি টেবিল)। ছোট ক্লিনিকে ঠিক আছে;
   ডেটা অনেক বড় হলে ধীর হতে পারে। কাজটি background thread-এ, UI জমে না। (Low)
3. **RoleSession-এ ডিফল্ট "Demo Patient" / "P-0001" / DOCTOR** — Doctor Queue
   থেকে খুললে আসল রোগীর তথ্য বসে যায়; কিন্তু রোগী ছাড়া সরাসরি খুললে Demo দেখাবে।
   (Low — fallback placeholder)
4. **পুরনো কিছু Print Center কার্ড এখনো Room পড়ে** (Registration ইত্যাদি),
   অথচ live ডেটা Supabase-এ। নতুন কার্ড (Payment রসিদ/Doctor Visit/Blood Test)
   Supabase পড়ে। পুরনোগুলো ফোনে ফাঁকা প্রিন্ট দিতে পারে। (Medium)
   → **MANUAL DEVICE TEST REQUIRED** (আসল ডেটায় যাচাই)।
5. `openWebViewModule` ফাংশন আর ব্যবহার হয় না (dead code, ক্ষতিকর নয়)।

---

## E. Native vs WebView Module List
**Native (Kotlin) — সব প্রধান ও সহায়ক কাজ:**
- Login, Dashboard — native/LoginActivity, native/DashboardActivity
- Enquiry, Registration, Payment, Follow Up — native/*Activity (Room + sync)
- Doctor Visit — native/DoctorVisitActivity
- Doctor Queue — native/DoctorQueueActivity
- Briefing — native/BriefingActivity
- Follow-up Calendar — native/FollowCalendarActivity
- Password Center — native/PasswordCenterActivity
- Patient Photo / My Photo — native/PatientPhotoActivity, native/UserPhotoActivity
- Draft / Reports / Trash — native/DraftActivity, ReportsActivity, TrashBinActivity
- More menu — native/MoreMenuActivity
- Print Center — print/PrintCenterActivity
- Clinical (Checkup/Prescription/Investigation/Diet/History) — clinical/*Activity
- Settings/Backup — security/SettingsActivity

**WebView (এখন স্বাভাবিক পথে অগম্য):**
- `MainActivity` + `assets/www/app.js`, `config.js` — কোডে রয়ে গেছে, কিন্তু
  Dashboard-এর কোনো বোতাম আর এটি খোলে না (Menu এখন native)। exported=false করা।

---

## F. Demo / Placeholder Module List
- `clinical/RoleSession.kt` — ডিফল্ট "Demo Patient"/"P-0001"/DOCTOR (fallback;
  extras দিয়ে খুললে override হয়)।
- `clinical/ClinicalRepository.kt` — hardcoded ওষুধ/টেস্ট/ডায়েট টেমপ্লেট তালিকা
  (এগুলো ইচ্ছাকৃত ডিফল্ট, doctor সম্পাদনা করে)। **ডেটা এখন Supabase-এ সেভ হয়**
  (আর in-memory-only নয়)।
- অন্য কোথাও "coming soon"/dead button/blank placeholder screen পাওয়া যায়নি।

---

## G. Fixed Files List (এই অডিটে)
- native/PaymentActivity.kt — CASH/ONLINE
- native/RegistrationActivity.kt — CASH/ONLINE
- res/layout/activity_payment.xml — "UPI" → "ONLINE"
- security/SessionTimeoutManager.kt — auto-logout নিষ্ক্রিয়
- AndroidManifest.xml — ৫টি activity exported=false
- native/CloudPasswordCheck.kt — SupabaseClient কে single source
- native/SupabaseClient.kt — deleteById যোগ (আগের ধাপে Trash-এর জন্য)
- clinical/ClinicalCloudRepository.kt — নতুন (Supabase medical persist)
- clinical/DoctorCheckupActivity.kt — Supabase persist + imports
- clinical/DietChartActivity.kt — Supabase persist + imports
- clinical/InvestigationAdviceActivity.kt — Supabase persist + imports
- clinical/PrescriptionActivity.kt — Supabase persist + imports + field-name fix

---

## H. Remaining Manual Test Checklist (verify লাগবে)
- [ ] Android Studio-তে Gradle sync + Debug APK build (wrapper regenerate)
- [ ] `local.properties`-এ sdk.dir + SUPABASE_URL + SUPABASE_ANON_KEY বসানো
- [ ] Supabase-এ `medical`, `briefings`, `usercredentials`, `trash` টেবিল আছে কিনা
- [ ] Clinical save → Supabase `medical`-এ সত্যিই row ঢুকছে কিনা
- [ ] পুরনো Print কার্ড (Registration) আসল ডেটায় প্রিন্ট দেয় কিনা
- [ ] Payment: CASH/ONLINE ছাড়া কিছু দেখায় না তো
- [ ] অ্যাপ idle রেখে ফিরে এলে logout হয় না তো (থাকা উচিত)
- [ ] ইন্টারনেট বন্ধ করে online-only স্ক্রিনগুলোর আচরণ

---

## I. Real Android Phone-এ ধাপে ধাপে টেস্ট (মানুষকে করতে হবে)
1. Android Studio-তে প্রজেক্ট খুলুন → Gradle sync হতে দিন (wrapper তৈরি হবে)।
2. `local.properties` বানিয়ে sdk.dir + Supabase URL/Key দিন।
3. Build → "Build APK(s)" → Debug APK তৈরি করুন। Error এলে লেখা কপি করে পাঠান।
4. APK ফোনে নিন (USB/WhatsApp)। "Install unknown apps" একবার চালু করুন।
5. ইনস্টল করে খুলুন → Login করুন (master নম্বর দিয়ে)।
6. একে একে চাপুন: Enquiry, Registration, Payment (CASH/ONLINE দেখুন),
   Doctor Queue, একজন রোগীতে Check-up/Prescription/Diet/Investigation Save →
   অ্যাপ বন্ধ করে আবার খুলে History-তে ওগুলো থাকে কিনা দেখুন (Supabase persist)।
7. Briefing পোস্ট/seen/reply/delete, Calendar, Users(পাসওয়ার্ড), Patient Photo,
   My Photo, Draft, Reports, Trash — প্রতিটি খুলে দেখুন।
8. অ্যাপ কিছুক্ষণ ফেলে রেখে ফিরে আসুন → logout হয়নি নিশ্চিত করুন।
9. যেখানেই ক্র্যাশ/ফাঁকা/ধীর — স্ক্রিনের নামসহ পাঠান, ঠিক করা হবে।

---

## J. APK/AAB build না হওয়ার Exact Reason
উপরে **A**-তে বিস্তারিত: এই স্যান্ডবক্সে gradle/SDK/compiler নেই, ইন্টারনেট বন্ধ,
এবং gradle wrapper (gradlew, gradlew.bat, gradle-wrapper.jar) + local.properties
অনুপস্থিত। তাই command-line/এখানে build অসম্ভব। Android Studio এগুলো regenerate
করতে পারে — সেখানে build করাই একমাত্র পথ।

---

## K. Production Readiness (সৎ অনুমান)
কোনো build log / emulator / device / Play Console evidence নেই, তাই **"Production
Ready" বলা যাচ্ছে না।**

- **কোড সম্পূর্ণতা (feature parity):** ~সম্পূর্ণ — সব ফিচার native, clinical
  persist ঠিক, requested সব fix (payment/session/exported/config) প্রয়োগ করা।
- **Build-verified:** 0% (এখানে build হয়নি)।
- **Device-verified:** 0% (**MANUAL DEVICE TEST REQUIRED**)।

**সার্বিক (সৎ):** কোড-প্রস্তুতি উঁচু, কিন্তু যেহেতu একবারও build/run হয়নি,
প্রকৃত Production Readiness **নিশ্চিতভাবে বলা যায় না** — প্রথম সফল Debug build ও
উপরের device checklist পাস করার পরই শতাংশে কথা বলা অর্থবহ হবে। তার আগে পর্যন্ত:
**Build-pending, not production-verified.**

---

## পরিশিষ্ট: WebView সম্পূর্ণ অপসারণ (Call-Hierarchy বিশ্লেষণের ভিত্তিতে)

**বিশ্লেষণে প্রমাণিত:** আসল WebView API (`android.webkit`) ছিল মাত্র ২টি ফাইলে —
`MainActivity.kt` ও `bridge/AndroidBridge.kt`। MainActivity-কে ডাকত শুধু
DashboardActivity-র `openWebViewModule()`, যা Menu native করার পর আর কল হতো না
(০ caller)। AndroidBridge ব্যবহার করত শুধু MainActivity।

**Safe-removed (কোনো সক্রিয় রেফারেন্স ছিল না):**
- `MainActivity.kt` (মুছে ফেলা)
- `bridge/AndroidBridge.kt` + খালি `bridge/` ফোল্ডার (মুছে ফেলা)
- `res/layout/activity_main.xml` (শুধু MainActivity ব্যবহার করত — মুছে ফেলা)
- DashboardActivity-র `openWebViewModule()` + `import MainActivity` (মুছে ফেলা)
- AndroidManifest-এ MainActivity-র `<activity>` block (মুছে ফেলা)

**রাখা বাধ্যতামূলক (কারণসহ):**
- `assets/www/` ফোল্ডার — কারণ `assets/www/assets/*.jpg` ব্রাঞ্চ-লোগো, যা **native
  প্রিন্ট** (`ClinicPdfBuilder` → `BranchInfo.logoAssetPath`) সক্রিয়ভাবে পড়ে।
  পুরো www মুছলে প্রিন্টের লোগো ভাঙত। বাকি web ফাইল (app.js/config.js/index.html)
  এখন নিষ্ক্রিয় (কেউ লোড করে না), কিন্তু লোগো-ফোল্ডারের ঝুঁকি এড়াতে www অক্ষত রাখা।

**অপসারণের পর সম্পূর্ণ পুনঃবিশ্লেষণ (সব পাস):**
- কোথাও `android.webkit`/WebView API নেই ✅
- MainActivity/AndroidBridge-এর কোনো সক্রিয় কোড-রেফারেন্স নেই (শুধু ব্যাখ্যা-কমেন্ট) ✅
- Manifest বৈধ XML, launcher = `.native.LoginActivity` (১টি) ✅
- Login → DashboardActivity (native) route অক্ষত ✅
- DashboardActivity ব্র্যাকেট ব্যালান্সড ✅
- লোগো ছবি অক্ষত (প্রিন্ট ভাঙবে না) ✅
- `NativeSession.toHandoffParam()` এখন unused (শুধু warning, build ভাঙে না; NativeSession
  একটি critical ফাইল বলে ঝুঁকি এড়াতে রাখা হয়েছে)

**ফল:** অ্যাপে এখন একটিও executable WebView কোড নেই — আক্ষরিক অর্থে সব native।
তবে আগের মতোই: এটি এখনো **build হয়নি**, তাই প্রথম Debug build-ই চূড়ান্ত প্রমাণ।
